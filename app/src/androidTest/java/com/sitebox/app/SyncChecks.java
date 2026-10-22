package com.sitebox.app;

import org.json.*;
import android.content.SharedPreferences;
import android.os.SystemClock;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/** Native merge, account persistence, and automatic restore checks; no simulated Google login claim. */
final class SyncChecks {
 static final String[] C={"공부","개발","AI","게임","영상","쇼핑","커뮤니티","생활","기타"};
 static String state(String... names)throws Exception{JSONArray sites=new JSONArray();for(String n:names)sites.put(new JSONObject().put("name",n).put("url","https://example.com/"+n).put("category",C[0]).put("notes","").put("fav",false));return BackupCodec.encode(sites,C,0,0,"study",true);}
 static String edit(String json,String key,Object value)throws Exception{JSONObject o=new JSONObject(json);o.getJSONArray("sites").getJSONObject(0).put(key,value);return o.toString();}
 static void same(SmokeInstrumentation t,String expected,String actual,String label)throws Exception{t.check(SyncPlanner.same(expected,actual),label);}
 static void run(SmokeInstrumentation t)throws Exception{
  String a=state("a"),ab=state("a","b"),empty=state();
  same(t,ab,SyncPlanner.merge(null,empty,ab),"first account connection automatically restores remote books");
  same(t,a,SyncPlanner.merge(null,a,null),"empty cloud retains existing local library");
  same(t,empty,SyncPlanner.merge(a,empty,a),"local deletion persists against unchanged remote");
  same(t,empty,SyncPlanner.merge(a,a,empty),"remote deletion persists against unchanged local");
  String local=edit(a,"notes","local memo"),remote=edit(a,"fav",true);
  BackupCodec.Snapshot conflict=BackupCodec.decode(SyncPlanner.merge(a,local,remote));
  t.check(conflict.bookCount==2,"concurrent changes retain both book versions");
  String merged=SyncPlanner.merge(a,ab,edit(a,"notes","remote memo"));
  t.check(BackupCodec.decode(merged).bookCount==2,"independent addition and remote edit merge");
  JSONObject reordered=new JSONObject(a);reordered.put("createdAt",1);same(t,a,reordered.toString(),"timestamps do not create an upload loop");
  JSONObject renamed=new JSONObject(a);renamed.getJSONArray("categories").put(0,"학습");renamed.getJSONArray("sites").getJSONObject(0).put("category","학습");
  BackupCodec.Snapshot categories=BackupCodec.decode(SyncPlanner.merge(a,renamed.toString(),remote));
  t.check(categories.bookCount==1&&categories.categories[0].equals("학습")&&categories.sites.getJSONObject(0).getBoolean("fav"),"category rename and remote favorite merge without duplicate books");
  MainActivity activity=t.activity;BackupChecks helper=new BackupChecks(t);String original=activity.backups.snapshot();BackupCodec.Snapshot before=BackupCodec.decode(original);Map<String,?> prefs=activity.getPreferences(0).getAll();SharedPreferences sync=activity.getSharedPreferences("sitebox.sync",0);Map<String,?> oldSync=sync.getAll();
  BackupController[] isolated={null,null};
  try{
   t.main(()->sync.edit().clear().commit());
   t.main(()->{isolated[0]=new BackupController(activity);});
   Field driveField=BackupController.class.getDeclaredField("drive");driveField.setAccessible(true);Object drive=driveField.get(isolated[0]);Field listenerField=GoogleDriveBackup.class.getDeclaredField("listener");listenerField.setAccessible(true);GoogleDriveBackup.Listener listener=(GoogleDriveBackup.Listener)listenerField.get(drive);
   t.main(()->{listener.onIdentity("sync-fixture@example.com","동기화 테스트 (sync-fixture@example.com)");listener.onChanged("Drive 연결 실패 (HTTP 403)","",false);listener.onFailure();});
   t.check(sync.getString("email","").equals("sync-fixture@example.com"),"verified account persists despite later Drive HTTP failure");
   t.main(()->isolated[1]=new BackupController(activity));Field account=BackupController.class.getDeclaredField("account");account.setAccessible(true);
   t.check(account.get(isolated[1]).toString().contains("sync-fixture@example.com"),"account label survives controller recreation");
   t.main(()->{activity.sites=new JSONArray();activity.save();});
   Method receive=BackupController.class.getDeclaredMethod("receiveRemote",String.class,String.class);receive.setAccessible(true);
   android.app.AlertDialog[] editing={null};t.main(()->editing[0]=new android.app.AlertDialog.Builder(activity).setTitle("편집 중 테스트").setMessage("원격 반영 대기 검증").create());t.main(()->editing[0].show());t.waitForIdleSync();
   t.main(()->{try{receive.invoke(isolated[0],ab,"sync-fixture@example.com");}catch(Exception e){throw new RuntimeException(e);}});
   t.check(activity.sites.length()==0,"remote restore waits while an editing dialog is open");
   t.main(()->{editing[0].dismiss();isolated[0].resume();});
   long deadline=SystemClock.elapsedRealtime()+7000;
   while(true){int[] count={0};t.main(()->count[0]=activity.sites.length());if(count[0]==2)break;if(SystemClock.elapsedRealtime()>deadline)throw new AssertionError("Auto restore timeout; books="+count[0]);SystemClock.sleep(30);}
   t.check(activity.sites.length()==2,"remote content automatically applies without restore button");
   JSONArray persisted=new JSONArray(activity.getPreferences(0).getString("sites","[]"));
   t.check(persisted.length()==2&&persisted.getJSONObject(1).getString("url").equals("https://example.com/b"),"automatic restore persists parsed book URL (independent of JSON escaping)");
   t.check(!helper.activeWindowHasText("백업 복원 확인"),"automatic restore has no manual confirmation dialog");
   Field syncing=BackupController.class.getDeclaredField("syncing");syncing.setAccessible(true);deadline=SystemClock.elapsedRealtime()+5000;
   while(true){boolean[] pending={true};t.main(()->{try{pending[0]=syncing.getBoolean(isolated[0]);}catch(Exception e){throw new RuntimeException(e);}});if(!pending[0])break;if(SystemClock.elapsedRealtime()>deadline)throw new AssertionError("Sync acknowledgement timeout");SystemClock.sleep(30);}
   t.checks.append("PASS automatic sync merges, deletions, conflicts, account persistence and automatic restore verified\nUNTESTED live Google OAuth consent and Drive transfer: cloud credentials are owner-managed\n");
  }finally{
   t.main(()->{for(BackupController c:isolated)if(c!=null)c.close();helper.prefs(prefs);activity.load();activity.motionEnabled=before.motion;activity.populate();activity.updateSceneBackdrop();});
   SharedPreferences.Editor editor=sync.edit().clear();for(Map.Entry<String,?> x:oldSync.entrySet()){Object v=x.getValue();if(v instanceof String)editor.putString(x.getKey(),(String)v);}editor.commit();
   // Delete fixture baseline only. It must not affect a real user's acknowledged account.
   byte[] hash=java.security.MessageDigest.getInstance("SHA-256").digest("sync-fixture@example.com".getBytes(java.nio.charset.StandardCharsets.UTF_8));StringBuilder name=new StringBuilder("sync-");for(byte v:hash)name.append(String.format(java.util.Locale.ROOT,"%02x",v));new android.util.AtomicFile(new java.io.File(activity.getFilesDir(),name+".json")).delete();
  }
 }
}
