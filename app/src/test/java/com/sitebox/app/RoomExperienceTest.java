package com.sitebox.app;

import android.view.View;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class RoomExperienceTest {
 @Test public void latestRequestSurvivesUnmeasuredLibraryRebind(){
  ActivityController<MainActivity> life=Robolectric.buildActivity(MainActivity.class).create();
  try{
   MainActivity a=life.get();layout(a,428,926);a.collapseToScene(false);String books=a.sites.toString();
   a.populate();assertEquals(0,a.currentCabinet.getWidth());
   a.expandFromScene(false);a.collapseToScene(false);a.expandFromScene(false);layout(a,428,926);
   assertFalse("Latest approach request must survive a just-rebuilt, unmeasured library",a.sceneCollapsed);
   assertEquals(View.VISIBLE,a.currentCabinet.getVisibility());assertEquals(0,a.roomExperience.transition.progress(),0);
   a.populate();a.collapseToScene(false);layout(a,428,926);
   assertTrue("A queued retreat must apply after measurement",a.sceneCollapsed);assertEquals(1,a.roomExperience.transition.progress(),0);assertEquals(books,a.sites.toString());
  }finally{life.destroy();}
 }
 @Test public void pinchRequestCanReverseAnApproach(){
  ActivityController<MainActivity> life=Robolectric.buildActivity(MainActivity.class).create();
  try{
   MainActivity a=life.get();layout(a,428,926);a.collapseToScene(false);layout(a,428,926);
   a.expandFromScene(true);assertTrue(a.sceneAnimating);assertFalse(a.sceneCollapsed);
   a.collapseToScene(true);
   assertTrue("A pinch-in while approaching must reverse toward the room",a.sceneCollapsed);
  }finally{life.destroy();}
 }
 @Test public void backFromTheEditorReturnsToTheRoom(){
  ActivityController<MainActivity> life=Robolectric.buildActivity(MainActivity.class).create();
  try{MainActivity a=life.get();layout(a,428,926);a.onBackPressed();assertTrue("Back must retreat into the room",a.sceneCollapsed);}
  finally{life.destroy();}
 }
 @Test public void movingCameraDoesNotDeliverTouchesToHiddenToolbar(){
  ActivityController<MainActivity> life=Robolectric.buildActivity(MainActivity.class).create();
  try{
   MainActivity a=life.get();layout(a,428,926);a.collapseToScene(false);a.expandFromScene(true);assertTrue(a.sceneAnimating);
   android.app.AlertDialog before=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
   View add=a.headline.getChildAt(2);int[] at=new int[2],origin=new int[2];add.getLocationOnScreen(at);a.stage.getLocationOnScreen(origin);float x=at[0]-origin[0]+add.getWidth()*.5f,y=at[1]-origin[1]+add.getHeight()*.5f;
   long now=android.os.SystemClock.uptimeMillis();for(int action:new int[]{android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_UP}){android.view.MotionEvent e=android.view.MotionEvent.obtain(now,now,action,x,y,0);a.stage.dispatchTouchEvent(e);e.recycle();}
   assertSame("Toolbar cannot open an edit dialog during camera travel",before,org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog());
  }finally{life.destroy();}
 }
 static void layout(MainActivity a,int w,int h){a.stage.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));a.stage.layout(0,0,w,h);a.updateSceneBackdrop();}
}
