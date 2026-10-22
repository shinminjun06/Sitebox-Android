package com.sitebox.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.*;
import android.util.AtomicFile;
import android.widget.*;
import org.json.JSONArray;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/** Automatic account sync, with local atomic recovery and legacy document import compatibility. */
final class BackupController {
    static final int EXPORT=9131, IMPORT=9132;
    private final MainActivity activity;
    private final GoogleDriveBackup drive;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService timer=Executors.newSingleThreadScheduledExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ArrayList<Button> actions=new ArrayList<>();
    private final AtomicFile rollback;
    private TextView statusView;
    private volatile boolean closed;
    private boolean storageBusy,driveBusy,syncing,foreground,changedDuringSync;
    private final SharedPreferences syncPrefs;
    private final Runnable syncTick=()->startSync(false);
    private String syncedAccount="";
    private Button connectButton,disconnectButton;
    private boolean waitingRemote;
    private String pendingRemote,pendingEmail;
    private final Runnable applyPending=()->{if(waitingRemote)receiveRemote(pendingRemote,pendingEmail);};
    private volatile CancellationSignal cancellation;
    private String driveStatus="Google 계정을 연결하면 서재를 자동으로 불러오고 저장합니다.",account="";

    BackupController(MainActivity activity){
        this.activity=activity;
        syncPrefs=activity.getSharedPreferences("sitebox.sync",0);
        syncedAccount=syncPrefs.getString("email","");account=syncPrefs.getString("label",syncedAccount);
        if(!syncedAccount.isEmpty())driveStatus="저장된 계정 · 연결을 확인하고 있어요.";
        rollback=new AtomicFile(new File(activity.getFilesDir(),"before-restore.json"));
        drive=new GoogleDriveBackup(activity,new GoogleDriveBackup.Listener(){
            public void onChanged(String status,String name,boolean busy){if(closed)return;driveStatus=status;if(name!=null&&!name.isEmpty())account=name;driveBusy=busy;update();}
            public void onIdentity(String email,String label){if(closed)return;syncedAccount=email;account=label;syncPrefs.edit().putString("email",email).putString("label",label).apply();update();}
            public void onRestore(String json,String email){if(!closed)receiveRemote(json,email);}
            public void onSaved(String json){if(!closed)acknowledge(json);}
            public void onFailure(){if(closed)return;syncing=false;update();schedule(60000);}
            public void onDisconnected(){if(closed)return;syncing=false;syncedAccount="";account="";syncPrefs.edit().remove("email").remove("label").apply();main.removeCallbacks(syncTick);update();}
        });
    }
    void saveState(Bundle state){drive.saveState(state);}
    void restoreState(Bundle state){drive.restoreState(state);}
    String snapshot()throws Exception{return BackupCodec.encode(activity.sites,activity.categories,activity.shelfThemeIndex,activity.bookThemeIndex,activity.backgroundThemeId,activity.motionEnabled);}
    void show(){
        LinearLayout content=new LinearLayout(activity);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(activity.dp(20),activity.dp(8),activity.dp(20),activity.dp(12));
        content.addView(activity.label("Google 계정을 연결하면 저장된 서재를 자동으로 불러옵니다. 책·메모·즐겨찾기·순서와 테마를 바꾸면 자동 저장합니다.",13,0xff574532));
        statusView=activity.label("",13,0xff574532);statusView.setTextIsSelectable(true);statusView.setPadding(0,activity.dp(12),0,activity.dp(12));content.addView(statusView);actions.clear();
        connectButton=button(content,"Google 계정 연결",()->startSync(true));
        disconnectButton=button(content,"Google 계정 연결 해제",()->new AlertDialog.Builder(activity).setMessage("자동 동기화를 중지하고 Google Drive 접근 권한을 해제할까요? 기기 데이터와 이미 저장된 서재는 유지됩니다.").setNegativeButton("취소",null).setPositiveButton("연결 해제",(d,w)->drive.disconnect()).show());
        content.addView(activity.label("오프라인에서도 기기에 저장됩니다. 앱을 다시 열면 연결을 확인하고 동기화를 재시도합니다.",12,0xff746650));
        ScrollView scroll=new ScrollView(activity);scroll.addView(content);
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Google 계정 · 자동 동기화").setView(scroll).setPositiveButton("닫기",null).create();
        dialog.setOnDismissListener(d->{statusView=null;connectButton=null;disconnectButton=null;actions.clear();});update();dialog.show();
    }
    private Button button(LinearLayout parent,String title,Runnable action){Button b=new Button(activity);b.setText(title);b.setMinHeight(activity.dp(48));b.setOnClickListener(v->{if(!storageBusy&&!driveBusy&&!syncing)action.run();});parent.addView(b);actions.add(b);return b;}
    private void update(){if(statusView!=null)statusView.setText((account.isEmpty()?"아직 연결되지 않았어요":account)+"\n"+(storageBusy?"서재를 기기에 반영하고 있어요…":driveStatus));for(Button b:actions)b.setEnabled(!storageBusy&&!driveBusy&&!syncing);if(connectButton!=null)connectButton.setText(syncedAccount.isEmpty()?"Google 계정 연결":"Google 계정 다시 연결");if(disconnectButton!=null)disconnectButton.setVisibility(syncedAccount.isEmpty()?android.view.View.GONE:android.view.View.VISIBLE);}
    void resume(){foreground=true;if(waitingRemote){main.removeCallbacks(applyPending);main.postDelayed(applyPending,500);}else schedule(500);}
    void pause(){foreground=false;main.removeCallbacks(syncTick);if(changedDuringSync&&!syncing&&!driveBusy&&!storageBusy)startSync(false);}
    void localChanged(){changedDuringSync=true;if(!closed)schedule(1500);}
    private void schedule(long delay){main.removeCallbacks(syncTick);if(!closed&&foreground&&!syncedAccount.isEmpty())main.postDelayed(syncTick,delay);}
    private void startSync(boolean explicit){
        if(closed||syncing||storageBusy||driveBusy)return;
        if(!explicit&&syncedAccount.isEmpty())return;
        syncing=true;changedDuringSync=false;main.removeCallbacks(syncTick);
        if(explicit&&syncedAccount.isEmpty())drive.connect();else drive.resume(syncedAccount,explicit);
    }
    private AtomicFile baseline(String email)throws Exception{
        byte[] hash=java.security.MessageDigest.getInstance("SHA-256").digest(email.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));StringBuilder name=new StringBuilder("sync-");for(byte v:hash)name.append(String.format(Locale.ROOT,"%02x",v));return new AtomicFile(new File(activity.getFilesDir(),name+".json"));
    }
    private AtomicFile accountCopy(String email)throws Exception{return new AtomicFile(new File(activity.getFilesDir(),baseline(email).getBaseFile().getName()+".device"));}
    private String readSnapshot(AtomicFile file)throws Exception{if(!file.getBaseFile().exists())return null;try(InputStream in=file.openRead()){String json=readBounded(in);BackupCodec.decode(json);return json;}}
    private String baselineRead(String email)throws Exception{return readSnapshot(baseline(email));}
    private static void atomicWrite(AtomicFile file,String json)throws Exception{FileOutputStream out=null;try{out=file.startWrite();out.write(json.getBytes(StandardCharsets.UTF_8));file.finishWrite(out);}catch(Exception e){if(out!=null)file.failWrite(out);throw e;}}
    private void receiveRemote(String remote,String email){
        if(closed)return;
        main.removeCallbacks(applyPending);
        if(!activity.hasWindowFocus()&&statusView==null){waitingRemote=true;pendingRemote=remote;pendingEmail=email;syncing=true;driveStatus="서재를 확인했어요. 편집을 마치면 자동으로 반영합니다.";update();if(foreground)main.postDelayed(applyPending,1000);return;}
        waitingRemote=false;pendingRemote=null;pendingEmail=null;
        syncing=true;final String[] currentCategories=activity.categories.clone();final String owner=activity.getPreferences(0).getString("syncOwner","");final String local,expected=revision();try{local=snapshot();}catch(Exception e){syncFailed();return;}
        work(()->{
            String previous=baselineRead(email);String merged;
            // Store each owner's offline edits separately. Never upload one account's books to another.
            if(!owner.isEmpty()&&!owner.equalsIgnoreCase(email)){
                atomicWrite(accountCopy(owner),local);String ownLocal=readSnapshot(accountCopy(email));
                merged=ownLocal!=null?SyncPlanner.merge(previous,ownLocal,remote):(remote==null?BackupCodec.encode(new JSONArray(),currentCategories,0,0,"study",true):remote);
            }else merged=SyncPlanner.merge(previous,local,remote);
            if(!SyncPlanner.same(local,merged))atomicWrite(rollback,local);
            return merged;
        },value->{
            if(!expected.equals(revision())){receiveRemote(remote,email);return;}
            try{
                String merged=(String)value;BackupCodec.Snapshot state=BackupCodec.decode(merged);
                if(!SyncPlanner.same(local,merged)||!owner.equalsIgnoreCase(email)){if(!writePreferences(state,email)){syncFailed();return;}reload(state);}
                work(()->{if(remote!=null)atomicWrite(baseline(email),remote);return merged;},ready->{try{
                    if(remote==null||!SyncPlanner.same((String)ready,remote)){driveStatus="변경사항을 자동 저장하고 있어요…";update();drive.backup((String)ready);}
                    else completeSync();
                }catch(Exception e){syncFailed();}});
            }catch(Exception e){syncFailed();}
        });
    }
    private void acknowledge(String json){final String email=syncedAccount;work(()->{atomicWrite(baseline(email),json);return json;},value->completeSync());}
    private void completeSync(){syncing=false;driveStatus="자동 동기화 완료 · "+new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date());update();schedule(changedDuringSync?1500:60000);}
    private void syncFailed(){syncing=false;driveStatus="동기화하지 못했어요. 기기 데이터는 유지되며 다음 연결 때 재시도합니다.";update();schedule(60000);}
    private void reload(BackupCodec.Snapshot state){
        boolean collapsed=activity.sceneCollapsed;
        if(collapsed)activity.expandFromScene(false);
        activity.load();activity.motionEnabled=state.motion;activity.populate();activity.updateSceneBackdrop();
        // Re-capture the new cabinet before the first frame; keep the user's scene mode and search.
        if(collapsed){final android.view.View cabinet=activity.currentCabinet;cabinet.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){if(cabinet.getViewTreeObserver().isAlive())cabinet.getViewTreeObserver().removeOnPreDrawListener(this);if(!closed&&activity.currentCabinet==cabinet)activity.collapseToScene(false);return true;}});}
    }

    private void message(String text){if(!closed)Toast.makeText(activity,text,Toast.LENGTH_LONG).show();}
    private void pick(int code){
        Intent intent=new Intent(code==EXPORT?Intent.ACTION_CREATE_DOCUMENT:Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE);
        if(code==EXPORT){intent.setType("application/json");intent.putExtra(Intent.EXTRA_TITLE,"SiteBox-backup-"+new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.ROOT).format(new Date())+".json");intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);}
        else{intent.setType("*/*");intent.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/json","text/plain","application/octet-stream"});intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);}
        try{activity.startActivityForResult(intent,code);}catch(android.content.ActivityNotFoundException e){message("파일 선택기를 사용할 수 없어요.");}
    }
    boolean onActivityResult(int request,int result,Intent data){
        if(drive.onActivityResult(request,result,data))return true;
        if(request!=EXPORT&&request!=IMPORT)return false;
        if(result!=android.app.Activity.RESULT_OK||data==null||data.getData()==null)return true;
        Uri uri=data.getData();if(!"content".equals(uri.getScheme())){message("문서 제공자의 백업 파일을 선택해 주세요.");return true;}
        if(storageBusy||driveBusy){message("진행 중인 작업이 끝난 뒤 다시 시도해 주세요.");return true;}
        if(request==EXPORT){final String json;try{json=snapshot();}catch(Exception e){message("현재 데이터를 백업할 수 없어요.");return true;}
            work(()->{document(uri,json.getBytes(StandardCharsets.UTF_8));return "파일 저장을 마쳤어요. Drive 업로드 상태는 Drive 앱에서 확인할 수 있어요.";},value->message((String)value));
        }else work(()->BackupCodec.decode(decodeUtf8(document(uri,null))),value->preview((BackupCodec.Snapshot)value,"선택한 백업 파일"));
        return true;
    }
    /** Interruptible channels let cancellation stop even stalled pipe providers on every supported API. */
    private byte[] document(Uri uri,byte[] output)throws Exception{
        CancellationSignal signal=cancellation;
        ParcelFileDescriptor descriptor=activity.getContentResolver().openFileDescriptor(uri,output==null?"r":"wt",signal);
        if(descriptor==null)throw new IOException("No document");
        return transfer(descriptor,output,signal);
    }
    static byte[] transfer(ParcelFileDescriptor descriptor,byte[] output,CancellationSignal signal)throws Exception{
        Thread thread=Thread.currentThread();signal.setOnCancelListener(thread::interrupt);
        long deadline=SystemClock.elapsedRealtime()+45000;
        try(FileInputStream source=output==null?new ParcelFileDescriptor.AutoCloseInputStream(descriptor):null;
            FileOutputStream sink=output!=null?new ParcelFileDescriptor.AutoCloseOutputStream(descriptor):null){
            FileChannel channel=source!=null?source.getChannel():sink.getChannel();
            if(output!=null&&output.length>BackupCodec.MAX_BYTES)throw new IOException("Backup too large");
            ByteArrayOutputStream input=new ByteArrayOutputStream();ByteBuffer buffer=output==null?ByteBuffer.allocate(8192):ByteBuffer.wrap(output);
            while(output==null||buffer.hasRemaining()){
                signal.throwIfCanceled();if(thread.isInterrupted()||SystemClock.elapsedRealtime()>=deadline)throw new InterruptedIOException("Document timeout");
                if(output!=null)channel.write(buffer);
                else{int n=channel.read(buffer);if(n<0)break;if(input.size()+n>BackupCodec.MAX_BYTES)throw new IOException("Backup too large");input.write(buffer.array(),0,n);buffer.clear();}
            }
            signal.throwIfCanceled();return input.toByteArray();
        }catch(IOException e){signal.throwIfCanceled();throw e;}
        finally{signal.setOnCancelListener(null);Thread.interrupted();}
    }
    private void readRollback(){work(()->{try(InputStream input=rollback.openRead()){return BackupCodec.decode(readBounded(input));}},value->preview((BackupCodec.Snapshot)value,"복원 전 기기 사본"));}
    void validateAndPreview(String json,String label){work(()->BackupCodec.decode(json),value->preview((BackupCodec.Snapshot)value,label));}
    private void preview(BackupCodec.Snapshot restored,String label){if(closed)return;new AlertDialog.Builder(activity).setTitle("백업 복원 확인").setMessage(label+"\n북마크 "+restored.sites.length()+"개 · 카테고리 9개\n\n현재 기기의 북마크와 서재 설정을 이 백업으로 바꿉니다. 현재 데이터는 기기 안에 복구 사본으로 보관합니다.").setNegativeButton("취소",null).setPositiveButton("복원",(d,w)->restore(restored)).show();}
    private String revision(){JSONArray names=new JSONArray();for(String name:activity.categories)names.put(name);return activity.sites.toString()+names.toString()+":"+activity.shelfThemeIndex+":"+activity.bookThemeIndex+":"+activity.backgroundThemeId+":"+activity.motionEnabled;}
    void restore(BackupCodec.Snapshot restored){
        final String expected=revision(),before;try{before=snapshot();}catch(Exception e){message("현재 데이터의 복구 사본을 만들 수 없어 복원을 중단했어요.");return;}
        work(()->{FileOutputStream out=null;try{out=rollback.startWrite();out.write(before.getBytes(StandardCharsets.UTF_8));rollback.finishWrite(out);}catch(Exception e){if(out!=null)rollback.failWrite(out);throw e;}return restored;},value->{try{
            if(!expected.equals(revision())){message("복원 준비 중 데이터가 바뀌었어요. 백업을 다시 불러와 주세요.");return;}
            if(!writePreferences(restored)){writePreferences(BackupCodec.decode(before));message("기기에 저장하지 못해 복원을 중단했어요.");return;}
            if(activity.sceneCollapsed)activity.expandFromScene(false);activity.load();activity.motionEnabled=restored.motion;activity.page=0;activity.search.setText("");activity.setSearchExpanded(false);activity.populate();activity.updateSceneBackdrop();message("백업을 복원했어요.");
        }catch(Exception e){message("복원하지 못했어요. 복원 전 기기 사본을 확인해 주세요.");}});
    }
    private boolean writePreferences(BackupCodec.Snapshot state){return writePreferences(state,null);}
    private boolean writePreferences(BackupCodec.Snapshot state,String owner){JSONArray names=new JSONArray();for(String name:state.categories)names.put(name);SharedPreferences.Editor editor=activity.getPreferences(0).edit().putString("sites",state.sites.toString()).putString("categories",names.toString()).putInt("shelfTheme",state.shelfTheme).putInt("bookTheme",state.bookTheme).putString("backgroundTheme",state.backgroundTheme).putBoolean("studyThemeIntroduced",true).putBoolean("studyMotion",state.motion);if(owner!=null)editor.putString("syncOwner",owner);return editor.commit();}
    private static String decodeUtf8(byte[] bytes)throws java.nio.charset.CharacterCodingException{return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();}
    private static String readBounded(InputStream input)throws Exception{ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] chunk=new byte[8192];int n;while((n=input.read(chunk))!=-1){if(bytes.size()+n>BackupCodec.MAX_BYTES)throw new IOException("Too large");bytes.write(chunk,0,n);}return decodeUtf8(bytes.toByteArray());}
    private interface Job{Object run()throws Exception;}private interface Result{void accept(Object value);}
    private void work(Job job,Result result){
        if(closed||storageBusy||driveBusy)return;storageBusy=true;update();CancellationSignal signal=new CancellationSignal();cancellation=signal;
        ScheduledFuture<?> timeout=timer.schedule(signal::cancel,45,TimeUnit.SECONDS);
        worker.execute(()->{Object value=null;Exception error=null;try{signal.throwIfCanceled();value=job.run();signal.throwIfCanceled();}catch(Exception e){error=e;}finally{timeout.cancel(false);cancellation=null;}
            final Object answer=value;final Exception failure=error;main.post(()->{if(closed)return;storageBusy=false;update();if(failure==null)result.accept(answer);else{if(syncing)syncFailed();else message("파일을 처리하지 못했어요. 백업 형식·크기와 연결 상태를 확인해 주세요. 기존 데이터는 유지됩니다.");}});});
    }
    void close(){closed=true;pendingRemote=null;pendingEmail=null;waitingRemote=false;CancellationSignal signal=cancellation;if(signal!=null)signal.cancel();worker.shutdownNow();timer.shutdownNow();main.removeCallbacksAndMessages(null);drive.close();}
}
