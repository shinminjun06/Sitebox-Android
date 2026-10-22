package com.sitebox.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class StudyRoomTest {
    static final String[] LEGACY={"classroom","library","bedroom","greenhouse","cafe","tatami","attic","lab","wizard","seaside","train"};
    @Test public void oldRoomPreferencesMigrateWithoutChangingLibrary()throws Exception{
        SharedPreferences prefs=RuntimeEnvironment.getApplication().getSharedPreferences("MainActivity",Context.MODE_PRIVATE);
        String books="[{\"name\":\"내 책\",\"url\":\"https://example.com\",\"category\":\"소설\",\"notes\":\"메모\"}]";
        for(String id:LEGACY){
            prefs.edit().clear().putString("backgroundTheme",id).putBoolean("studyThemeIntroduced",true).putString("sites",books).putInt("shelfTheme",2).putInt("bookTheme",1).commit();
            ActivityController<MainActivity> lifecycle=Robolectric.buildActivity(MainActivity.class).create();
            try{MainActivity a=lifecycle.get();assertEquals("study",a.backgroundThemeId);assertEquals("study",a.getPreferences(0).getString("backgroundTheme",""));assertEquals(new JSONArray(books).toString(),a.sites.toString());assertEquals(2,a.shelfThemeIndex);assertEquals(1,a.bookThemeIndex);}finally{lifecycle.destroy();}
        }
    }
    @Test public void oldCloudRoomsRestoreBooksAndNotes()throws Exception{
        String[] categories={"소설","시","역사","과학","예술","여행","개발","경제","기타"};
        JSONArray books=new JSONArray().put(new JSONObject().put("name","책").put("url","https://example.com").put("category","소설").put("notes","잃지 않을 메모"));
        JSONObject backup=new JSONObject(BackupCodec.encode(books,categories,2,1,"study",false));
        for(String id:LEGACY){backup.put("backgroundTheme",id);BackupCodec.Snapshot restored=BackupCodec.decode(backup.toString());assertEquals("study",restored.backgroundTheme);assertEquals(books.toString(),restored.sites.toString());assertEquals(2,restored.shelfTheme);assertEquals(1,restored.bookTheme);assertFalse(restored.motion);}
    }
    @Test public void packagedRoomsContainOnlyStudyArtwork()throws Exception{
        assertEquals(1,SceneTheme.ALL.length);assertEquals("study",SceneTheme.ALL[0].id);
        for(String asset:RuntimeEnvironment.getApplication().getAssets().list("scenes"))assertTrue("Removed room still packaged: "+asset,asset.startsWith("study_"));
    }
}
