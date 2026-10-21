package com.sitebox.app;
import android.app.*;
import android.content.*;
import android.os.*;
import android.widget.TextView;
import android.view.*;
import org.json.*;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

final class BackupChecks {
 final SmokeInstrumentation test;final MainActivity activity;
 BackupChecks(SmokeInstrumentation test){this.test=test;activity=test.activity;}
 interface Ready{boolean get()throws Exception;}
 void waitFor(Ready ready)throws Exception{long end=SystemClock.elapsedRealtime()+5000;while(!ready.get()){if(SystemClock.elapsedRealtime()>end)throw new AssertionError("Backup UI timeout");SystemClock.sleep(30);}test.waitForIdleSync();}
 void idle()throws Exception{Field f=BackupController.class.getDeclaredField("storageBusy");f.setAccessible(true);waitFor(()->{boolean[] busy={true};test.main(()->{try{busy[0]=f.getBoolean(activity.backups);}catch(Exception e){throw new RuntimeException(e);}});return !busy[0];});}
 View find(View v,String text){if(v instanceof TextView&&text.contentEquals(((TextView)v).getText()))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View hit=find(((ViewGroup)v).getChildAt(i),text);if(hit!=null)return hit;}return null;}
 void prefs(Map<String,?> state){SharedPreferences.Editor e=activity.getPreferences(0).edit().clear();for(Map.Entry<String,?> x:state.entrySet()){Object v=x.getValue();String k=x.getKey();if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof Float)e.putFloat(k,(Float)v);}if(!e.commit())throw new AssertionError("Test cleanup prefs");}
 void reject(String json)throws Exception{try{BackupCodec.decode(json);throw new AssertionError("Invalid backup accepted");}catch(JSONException expected){}}
 void run()throws Exception{
  final BackupController controller=activity.backups;final String original=controller.snapshot();final BackupCodec.Snapshot base=BackupCodec.decode(original);final Map<String,?> before=activity.getPreferences(0).getAll();
  File rollback=new File(activity.getFilesDir(),"before-restore.json");byte[] oldRollback=rollback.exists()?java.nio.file.Files.readAllBytes(rollback.toPath()):null;
  final String filter=activity.filter;final int page=activity.page;final boolean expanded=activity.searchExpanded;
  try{
   JSONObject invalid=new JSONObject(original);invalid.put("version",999);reject(invalid.toString());reject(original.substring(0,original.length()-1));reject(original+" garbage");reject("{unquoted:1}");
   invalid=new JSONObject(original);invalid.put("motion","true");reject(invalid.toString());
   invalid=new JSONObject(original);invalid.getJSONArray("sites").getJSONObject(0).put("url","javascript:alert(1)");reject(invalid.toString());
   for(String padded:new String[]{" "+base.categories[0],base.categories[0]+" "}){
    invalid=new JSONObject(original);invalid.getJSONArray("categories").put(0,padded);
    JSONArray sites=invalid.getJSONArray("sites");for(int i=0;i<sites.length();i++)if(base.categories[0].equals(sites.getJSONObject(i).getString("category")))sites.getJSONObject(i).put("category",padded);
    reject(invalid.toString());
   }
   test.check(BackupCodec.decode(original).sites.toString().equals(base.sites.toString()),"codec rejects padded category labels and preserves valid fixture");
   test.check(controller.snapshot()!=null&&base.sites.length()==32,"backup codec validates native fixture and rejects malformed imports");
   // A stalled pipe must respond to cancellation without waiting for a provider write/EOF.
   ParcelFileDescriptor[] pipe=ParcelFileDescriptor.createPipe();CancellationSignal cancel=new CancellationSignal();AtomicReference<Throwable> failure=new AtomicReference<>();
   Thread blocked=new Thread(()->{try{BackupController.transfer(pipe[0],null,cancel);failure.set(new AssertionError("Cancelled pipe succeeded"));}catch(OperationCanceledException expected){}catch(Throwable e){failure.set(e);}});blocked.start();SystemClock.sleep(120);cancel.cancel();blocked.join(1500);boolean stopped=!blocked.isAlive();pipe[0].close();pipe[1].close();test.check(stopped&&failure.get()==null,"stalled document provider cancels within bounded poll");
   test.main(controller::show);test.snapshot("backup-settings.png");
   // Close settings through back before using synthetic Android document results.
   test.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);test.waitForIdleSync();
   test.main(()->controller.onActivityResult(BackupController.EXPORT,Activity.RESULT_CANCELED,null));test.check(before.equals(activity.getPreferences(0).getAll()),"cancelled picker preserves preferences");
   Intent document=new Intent().setData(BackupTestProvider.DOCUMENT);
   test.main(()->controller.onActivityResult(BackupController.EXPORT,Activity.RESULT_OK,document));idle();
   String saved;try(InputStream in=activity.getContentResolver().openInputStream(BackupTestProvider.DOCUMENT)){saved=new String(in.readAllBytes(),StandardCharsets.UTF_8);}
   test.check(BackupCodec.decode(saved).sites.toString().equals(base.sites.toString()),"native document URI export round trip");
   try(OutputStream out=activity.getContentResolver().openOutputStream(BackupTestProvider.DOCUMENT,"wt")){out.write("broken".getBytes(StandardCharsets.UTF_8));}
   test.main(()->controller.onActivityResult(BackupController.IMPORT,Activity.RESULT_OK,document));idle();test.check(before.equals(activity.getPreferences(0).getAll()),"malformed native import leaves preferences untouched");
   JSONObject malformed=new JSONObject(original);malformed.getJSONArray("sites").getJSONObject(0).put("name","UTF8_MARKER");
   byte[] corrupt=malformed.toString().getBytes(StandardCharsets.UTF_8);int marker=malformed.toString().indexOf("UTF8_MARKER");
   int byteOffset=malformed.toString().substring(0,marker).getBytes(StandardCharsets.UTF_8).length;corrupt[byteOffset]=(byte)0xff;
   try(OutputStream out=activity.getContentResolver().openOutputStream(BackupTestProvider.DOCUMENT,"wt")){out.write(corrupt);}
   test.main(()->controller.onActivityResult(BackupController.IMPORT,Activity.RESULT_OK,document));idle();
   android.view.accessibility.AccessibilityNodeInfo root=test.getUiAutomation().getRootInActiveWindow();
   test.check(root!=null&&root.findAccessibilityNodeInfosByText("백업 복원 확인").isEmpty()&&before.equals(activity.getPreferences(0).getAll())&&activity.sites.toString().equals(base.sites.toString()),"invalid UTF-8 native import is rejected without preview or library changes");
   JSONArray books=new JSONArray();JSONObject book=new JSONObject().put("name","한글 메모 📚").put("url","https://example.com/saved").put("category",base.categories[0]).put("fav",true).put("notes","첫 줄\n둘째 줄").put("sortOrder",Long.MAX_VALUE).put("custom","preserved");books.put(book);
   String replacement=BackupCodec.encode(books,base.categories,2,1,"wizard",false);BackupCodec.Snapshot restored=BackupCodec.decode(replacement);
   test.check(restored.sites.getJSONObject(0).getLong("sortOrder")==Long.MAX_VALUE&&restored.sites.getJSONObject(0).getString("custom").equals("preserved"),"backup retains Korean metadata and full sort order range");
   // Validate/read remains separate from explicit confirmation. Cancel via back, then restore explicitly.
   test.main(()->controller.validateAndPreview(replacement,"기기 문서 테스트"));idle();test.snapshot("backup-preview.png");test.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);test.waitForIdleSync();test.check(before.equals(activity.getPreferences(0).getAll()),"cancelled restore preview leaves data unchanged");
   test.main(()->controller.restore(restored));idle();
   BackupCodec.Snapshot actual=BackupCodec.decode(controller.snapshot());test.check(actual.sites.toString().equals(books.toString())&&actual.shelfTheme==2&&actual.bookTheme==1&&actual.backgroundTheme.equals("wizard")&&!actual.motion,"confirmed restore updates every field");
   test.main(activity::load);test.check(activity.sites.toString().equals(books.toString()),"restored library persists through preferences reload");
   BackupCodec.Snapshot safety=BackupCodec.decode(new String(java.nio.file.Files.readAllBytes(rollback.toPath()),StandardCharsets.UTF_8));test.check(safety.sites.toString().equals(base.sites.toString()),"restore saves original atomic rollback copy");
   test.checks.append("PASS Google backup codec and local restore verified\nUNTESTED Google account consent and live Drive transfer: no configured cloud account in CI\n");
  }finally{
   idle();test.main(()->{prefs(before);activity.load();activity.motionEnabled=base.motion;activity.search.setText(filter);activity.setSearchExpanded(expanded);activity.page=page;activity.populate();activity.updateSceneBackdrop();prefs(before);});
   if(oldRollback==null)rollback.delete();else java.nio.file.Files.write(rollback.toPath(),oldRollback);
   activity.getContentResolver().delete(BackupTestProvider.DOCUMENT,null,null);
  }
 }
}
