package com.sitebox.app;

import android.view.View;
import java.util.ArrayList;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class LampSwitchTest {
    ActivityController<MainActivity> lifecycle;
    MainActivity activity;
    @Before public void setup(){
        lifecycle=Robolectric.buildActivity(MainActivity.class).create();activity=lifecycle.get();
        activity.applyBackgroundTheme("study");layout(1080,1920);
    }
    @After public void close(){lifecycle.destroy();}
    void layout(int width,int height){
        activity.stage.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
        activity.stage.layout(0,0,width,height);activity.updateSceneBackdrop();
    }
    View target(String label){
        ArrayList<View> hits=new ArrayList<>();activity.stage.findViewsWithText(hits,label,View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
        assertEquals("A lamp switch must be accessible by its current action",1,hits.size());return hits.get(0);
    }
    @Test public void collapsedLampTapTurnsOffAndSecondTapTurnsOn()throws Exception{
        activity.collapseToScene(false);assertTrue(activity.sceneCollapsed);
        View lamp=target("조명 끄기");assertEquals(View.VISIBLE,lamp.getVisibility());
        lamp.performClick();assertTrue(activity.sceneCollapsed);
        assertFalse(activity.getPreferences(0).getBoolean("studyLampOn",true));
        assertFalse((Boolean)GoogleAccountTest.get(activity.studyLive,"lampEnabled"));
        target("조명 켜기").performClick();
        assertTrue(activity.getPreferences(0).getBoolean("studyLampOn",false));
        assertTrue((Boolean)GoogleAccountTest.get(activity.studyLive,"lampEnabled"));
    }
    @Test public void lampTargetIsHiddenAfterExpandingBookshelf(){
        activity.collapseToScene(false);View lamp=target("조명 끄기");
        activity.expandFromScene(false);assertEquals(View.GONE,lamp.getVisibility());
    }
    @Test public void removedThemeFallsBackToStudyAndKeepsLampSwitch(){
        activity.collapseToScene(false);View lamp=target("조명 끄기");
        activity.applyBackgroundTheme("classroom");assertEquals("study",activity.backgroundThemeId);assertEquals(View.VISIBLE,lamp.getVisibility());
    }
    @Test public void turningOffLampSurvivesActivityRecreation()throws Exception{
        activity.collapseToScene(false);target("조명 끄기").performClick();
        lifecycle.recreate();activity=lifecycle.get();layout(1080,1920);
        assertFalse((Boolean)GoogleAccountTest.get(activity.studyLive,"lampEnabled"));
        assertEquals("조명 켜기",target("조명 켜기").getContentDescription());
    }
    @Test public void compactCabinetLeavesClearSpaceBesideLamp(){
        activity.collapseToScene(false);View lamp=target("조명 끄기");
        float cabinetLeft=activity.miniature.getX();
        assertTrue("Lamp and cabinet need a visible gap",cabinetLeft>=lamp.getX()+lamp.getLayoutParams().width+activity.dp(20));
        assertTrue("The room needs more breathing space",activity.miniature.getLayoutParams().height*activity.miniature.getScaleY()<activity.stage.getHeight()*.25f);
    }
    @Test public void landscapeLampRemainsTappableWithoutOpeningCabinet(){
        layout(1920,1080);activity.collapseToScene(false);View lamp=target("조명 끄기");
        assertEquals(View.VISIBLE,lamp.getVisibility());
        assertTrue(lamp.getLayoutParams().width>=activity.dp(48));assertTrue(lamp.getLayoutParams().height>=activity.dp(48));
        lamp.performClick();assertTrue(activity.sceneCollapsed);
        assertEquals("조명 켜기",lamp.getContentDescription());
    }
}
