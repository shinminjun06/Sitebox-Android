package com.sitebox.app;

import android.app.AlertDialog;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class AutomaticSyncTest {
    MainActivity activity;
    ActivityController<MainActivity> lifecycle;
    BackupController controller;
    String email="reader@example.com";
    @Before public void setup() throws Exception {
        lifecycle=Robolectric.buildActivity(MainActivity.class).create();
        activity=lifecycle.get();controller=activity.backups;
        activity.getPreferences(0).edit().clear().commit();
        activity.getSharedPreferences("sitebox.sync",0).edit().clear().commit();
        activity.load();
        controller.show();
        Object drive=GoogleAccountTest.get(controller,"drive");
        ((ThreadPoolExecutor)GoogleAccountTest.get(drive,"worker")).shutdownNow();
        GoogleDriveBackup.Listener listener=(GoogleDriveBackup.Listener)GoogleAccountTest.get(drive,"listener");
        listener.onIdentity(email,"Reader ("+email+")");
    }
    @After public void close(){controller.close();lifecycle.destroy();}
    String state(String... names)throws Exception{
        JSONArray sites=new JSONArray();for(String n:names)sites.put(new JSONObject().put("name",n).put("url","https://example.com/"+n).put("category",activity.categories[0]).put("notes","").put("fav",false));
        return BackupCodec.encode(sites,activity.categories,1,2,"study",false);
    }
    void receive(String json)throws Exception{
        GoogleAccountTest.call(controller,"receiveRemote",new Class<?>[]{String.class,String.class},json,email);
        long deadline=System.nanoTime()+10_000_000_000L;
        while((Boolean)GoogleAccountTest.get(controller,"syncing")||(Boolean)GoogleAccountTest.get(controller,"storageBusy")){
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            if(System.nanoTime()>deadline)fail("Automatic sync did not finish");
            Thread.sleep(10);
        }
    }
    @Test public void remoteLibraryRestoresWithoutConfirmationAndSurvivesReload()throws Exception{
        String remote=state("book","memo");receive(remote);
        assertTrue(SyncPlanner.same(remote,controller.snapshot()));
        assertEquals(email,activity.getPreferences(0).getString("syncOwner",""));
        activity.load();assertEquals(2,activity.sites.length());
        assertEquals(1,activity.shelfThemeIndex);assertEquals(2,activity.bookThemeIndex);assertFalse(activity.motionEnabled);
        assertEquals("Google 계정 · 자동 동기화",Shadows.shadowOf((AlertDialog)ShadowDialog.getLatestDialog()).getTitle().toString());
    }
    @Test public void deletingRemoteBookDoesNotResurrectItOnNextSync()throws Exception{
        receive(state("book","memo"));receive(state("book"));
        assertEquals(1,activity.sites.length());
        assertEquals("book",activity.sites.getJSONObject(0).getString("name"));
    }
    @Test public void driveFailureDoesNotErasePersistedGoogleIdentity()throws Exception{
        Object drive=GoogleAccountTest.get(controller,"drive");
        GoogleDriveBackup.Listener listener=(GoogleDriveBackup.Listener)GoogleAccountTest.get(drive,"listener");
        listener.onChanged("Google Drive API 설정 필요 (HTTP 403)","",false);listener.onFailure();
        BackupController recreated=new BackupController(activity);
        try{assertEquals(email,GoogleAccountTest.get(recreated,"syncedAccount"));assertTrue(GoogleAccountTest.get(recreated,"account").toString().contains(email));}
        finally{recreated.close();}
    }
    @Test public void accountUiOffersOnlyGoogleConnectionAndNoManualBackupButtons()throws Exception{
        AlertDialog dialog=(AlertDialog)ShadowDialog.getLatestDialog();
        List<String> labels=new ArrayList<>();collect(dialog.getWindow().getDecorView(),labels);
        assertTrue(labels.contains("Google 계정 다시 연결"));
        assertTrue(labels.contains("Google 계정 연결 해제"));
        for(String forbidden:new String[]{"Google Drive에 백업","Drive 최근 백업 불러오기","백업 파일 불러오기","파일로 저장 · Google Drive 선택 가능","복원 전 기기 사본 불러오기"})assertFalse(forbidden,labels.contains(forbidden));
    }
    @Test public void anotherAccountNeverUploadsPreviousOwnersBooks()throws Exception{
        receive(state("private"));
        email="another@example.com";
        receive(state("other"));
        assertEquals(1,activity.sites.length());assertEquals("other",activity.sites.getJSONObject(0).getString("name"));
        email="reader@example.com";receive(state("private"));
        assertEquals("private",activity.sites.getJSONObject(0).getString("name"));
    }
    @Test public void signedOutScreenOffersGoogleLoginOnly()throws Exception{
        Object drive=GoogleAccountTest.get(controller,"drive");
        GoogleDriveBackup.Listener listener=(GoogleDriveBackup.Listener)GoogleAccountTest.get(drive,"listener");
        listener.onDisconnected();
        List<String> labels=new ArrayList<>();collect(ShadowDialog.getLatestDialog().getWindow().getDecorView(),labels);
        assertTrue(labels.contains("Google로 로그인"));
        assertFalse(labels.contains("Google 계정 다시 연결"));
    }
    void collect(View v,List<String> labels){if(v instanceof TextView)labels.add(((TextView)v).getText().toString());if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)collect(((ViewGroup)v).getChildAt(i),labels);}
}
