package com.sitebox.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.*;
import android.system.*;
import android.util.AtomicFile;
import android.widget.*;
import org.json.JSONArray;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/** Manual backups. Importing and cancelling never change the library. */
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
    private boolean storageBusy,driveBusy;
    private volatile CancellationSignal cancellation;
    private String driveStatus="Google 계정을 연결하거나 파일로 백업할 수 있어요.",account="";

    BackupController(MainActivity activity){
        this.activity=activity;
        rollback=new AtomicFile(new File(activity.getFilesDir(),"before-restore.json"));
        drive=new GoogleDriveBackup(activity,new GoogleDriveBackup.Listener(){
            public void onChanged(String status,String name,boolean busy){if(closed)return;driveStatus=status;account=name==null?"":name;driveBusy=busy;update();}
            public void onRestore(String json,String label){if(!closed)validateAndPreview(json,label);}
        });
    }
    void saveState(Bundle state){drive.saveState(state);}
    void restoreState(Bundle state){drive.restoreState(state);}
    String snapshot()throws Exception{return BackupCodec.encode(activity.sites,activity.categories,activity.shelfThemeIndex,activity.bookThemeIndex,activity.backgroundThemeId,activity.motionEnabled);}
    void show(){
        LinearLayout content=new LinearLayout(activity);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(activity.dp(20),activity.dp(8),activity.dp(20),activity.dp(12));
        content.addView(activity.label("북마크·메모·즐겨찾기·책 순서·카테고리와 서재 테마를 저장합니다. 백업과 복원은 버튼을 누를 때만 실행합니다.",13,0xff574532));
        statusView=activity.label("",13,0xff574532);statusView.setPadding(0,activity.dp(12),0,activity.dp(12));content.addView(statusView);actions.clear();
        button(content,"Google 계정 연결",()->drive.connect());
        button(content,"Google Drive에 백업",()->{try{drive.backup(snapshot());}catch(Exception e){message("현재 데이터를 백업할 수 없어요. 데이터를 확인해 주세요.");}});
        button(content,"Drive 최근 백업 불러오기",()->drive.restoreLatest());
        button(content,"Google 계정 연결 해제",()->new AlertDialog.Builder(activity).setMessage("앱의 Google Drive 접근 권한을 해제할까요? 기기 데이터는 유지됩니다.").setNegativeButton("취소",null).setPositiveButton("연결 해제",(d,w)->drive.disconnect()).show());
        TextView info=activity.label("파일 선택기에서 Google Drive와 사용할 계정을 선택해 저장할 수도 있어요. Drive가 표시되지 않으면 Google Drive 앱의 로그인 상태를 확인해 주세요.",12,0xff746650);info.setPadding(0,activity.dp(12),0,activity.dp(8));content.addView(info);
        button(content,"파일로 저장 · Google Drive 선택 가능",()->pick(EXPORT));
        button(content,"백업 파일 불러오기",()->pick(IMPORT));
        button(content,"복원 전 기기 사본 불러오기",this::readRollback);
        ScrollView scroll=new ScrollView(activity);scroll.addView(content);
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Google 계정 · 백업").setView(scroll).setPositiveButton("닫기",null).create();
        dialog.setOnDismissListener(d->{statusView=null;actions.clear();});update();dialog.show();
    }
    private void button(LinearLayout parent,String title,Runnable action){Button b=new Button(activity);b.setText(title);b.setMinHeight(activity.dp(48));b.setOnClickListener(v->{if(!storageBusy&&!driveBusy)action.run();});parent.addView(b);actions.add(b);}
    private void update(){if(statusView!=null)statusView.setText((account.isEmpty()?"연결된 계정 없음":account)+"\n"+(storageBusy?"백업 파일 처리 중…":driveStatus));for(Button b:actions)b.setEnabled(!storageBusy&&!driveBusy);}
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
        }else work(()->BackupCodec.decode(new String(document(uri,null),StandardCharsets.UTF_8)),value->preview((BackupCodec.Snapshot)value,"선택한 백업 파일"));
        return true;
    }
    /** Nonblocking descriptors + bounded poll let cancellation interrupt even stalled pipe providers. */
    private byte[] document(Uri uri,byte[] output)throws Exception{
        CancellationSignal signal=cancellation;
        try(ParcelFileDescriptor descriptor=activity.getContentResolver().openFileDescriptor(uri,output==null?"r":"wt",signal)){
            if(descriptor==null)throw new IOException("No document");
            return transfer(descriptor,output,signal);
        }
    }
    static byte[] transfer(ParcelFileDescriptor descriptor,byte[] output,CancellationSignal signal)throws Exception{
        FileDescriptor fd=descriptor.getFileDescriptor();int flags=Os.fcntlInt(fd,OsConstants.F_GETFL,0);Os.fcntlInt(fd,OsConstants.F_SETFL,flags|OsConstants.O_NONBLOCK);
        ByteArrayOutputStream input=new ByteArrayOutputStream();byte[] chunk=new byte[8192];int offset=0;
        long deadline=SystemClock.elapsedRealtime()+45000;
        StructPollfd poll=new StructPollfd();poll.fd=fd;poll.events=(short)(output==null?OsConstants.POLLIN:OsConstants.POLLOUT);
        while(output==null||offset<output.length){
            signal.throwIfCanceled();if(Thread.currentThread().isInterrupted()||SystemClock.elapsedRealtime()>=deadline)throw new InterruptedIOException("Document timeout");
            if(Os.poll(new StructPollfd[]{poll},250)==0)continue;
            if((poll.revents&OsConstants.POLLNVAL)!=0)throw new IOException("Closed document");
            try{
                if(output!=null){int n=Os.write(fd,output,offset,Math.min(8192,output.length-offset));if(n<=0)throw new IOException("Write stopped");offset+=n;}
                else{int n=Os.read(fd,chunk,0,chunk.length);if(n==0)break;if(input.size()+n>BackupCodec.MAX_BYTES)throw new IOException("Backup too large");input.write(chunk,0,n);}
            }catch(ErrnoException e){if(e.errno!=OsConstants.EAGAIN&&e.errno!=OsConstants.EINTR)throw e;}
        }
        signal.throwIfCanceled();return input.toByteArray();
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
    private boolean writePreferences(BackupCodec.Snapshot state){JSONArray names=new JSONArray();for(String name:state.categories)names.put(name);return activity.getPreferences(0).edit().putString("sites",state.sites.toString()).putString("categories",names.toString()).putInt("shelfTheme",state.shelfTheme).putInt("bookTheme",state.bookTheme).putString("backgroundTheme",state.backgroundTheme).putBoolean("studyThemeIntroduced",true).putBoolean("studyMotion",state.motion).commit();}
    private static String readBounded(InputStream input)throws Exception{ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] chunk=new byte[8192];int n;while((n=input.read(chunk))!=-1){if(bytes.size()+n>BackupCodec.MAX_BYTES)throw new IOException("Too large");bytes.write(chunk,0,n);}return new String(bytes.toByteArray(),StandardCharsets.UTF_8);}
    private interface Job{Object run()throws Exception;}private interface Result{void accept(Object value);}
    private void work(Job job,Result result){
        if(closed||storageBusy||driveBusy)return;storageBusy=true;update();CancellationSignal signal=new CancellationSignal();cancellation=signal;
        ScheduledFuture<?> timeout=timer.schedule(signal::cancel,45,TimeUnit.SECONDS);
        worker.execute(()->{Object value=null;Exception error=null;try{signal.throwIfCanceled();value=job.run();signal.throwIfCanceled();}catch(Exception e){error=e;}finally{timeout.cancel(false);cancellation=null;}
            final Object answer=value;final Exception failure=error;main.post(()->{if(closed)return;storageBusy=false;update();if(failure==null)result.accept(answer);else message("파일을 처리하지 못했어요. 백업 형식·크기와 연결 상태를 확인해 주세요. 기존 데이터는 유지됩니다.");});});
    }
    void close(){closed=true;CancellationSignal signal=cancellation;if(signal!=null)signal.cancel();worker.shutdownNow();timer.shutdownNow();main.removeCallbacksAndMessages(null);drive.close();}
}
