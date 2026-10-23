package com.sitebox.app;

import android.graphics.*;
import android.view.View;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class FurnitureGroundingTest {
 @Test public void hotspotFollowsTheOriginalPhotographedShelf(){
  ActivityController<MainActivity> life=Robolectric.buildActivity(MainActivity.class).create();
  try{
   MainActivity a=life.get();a.setStudyMotion(false);
   for(int[] size:new int[][]{{591,1206},{926,428}}){RoomExperienceTest.layout(a,size[0],size[1]);a.collapseToScene(false);RoomExperienceTest.layout(a,size[0],size[1]);float[] actual=new float[8];a.miniature.copyRenderedCorners(actual);
    // Independently marked crown/feet on the user's 720x1280 photograph.
    float[] expected={0,.170f,.216f,.235f,.216f,.598f,0,.670f};for(int i=0;i<8;i+=2){expected[i]*=a.sceneBackdrop.getWidth();expected[i+1]*=a.sceneBackdrop.getHeight();}a.backdrop.getImageMatrix().mapPoints(expected);assertArrayEquals(expected,actual,.5f);
    assertEquals("Use the source photo's real floor contact",View.GONE,a.sceneShadow.getVisibility());
   }
  }finally{life.destroy();}
 }
 @Test public void libraryChangesKeepTheRoomPhotoIntactAndAppearInTheEditor()throws Exception{
  ActivityController<MainActivity> life=Robolectric.buildActivity(MainActivity.class).create();
  try{
   MainActivity a=life.get();a.setStudyMotion(false);a.sites=new JSONArray();a.populate();RoomExperienceTest.layout(a,591,1206);a.collapseToScene(false);
   Bitmap before=Bitmap.createBitmap(591,1206,Bitmap.Config.ARGB_8888);a.stage.draw(new Canvas(before));
   a.sites.put(new JSONObject().put("name","내 서재의 책").put("url","https://example.com").put("category",a.categories[0]));a.populate();RoomExperienceTest.layout(a,591,1206);
   Bitmap after=Bitmap.createBitmap(591,1206,Bitmap.Config.ARGB_8888);a.stage.draw(new Canvas(after));assertFalse("Actual saved books must appear within the photo shelf",before.sameAs(after));
   a.expandFromScene(false);assertEquals(View.VISIBLE,a.currentCabinet.getVisibility());assertEquals(1,RoomRenderingTest.booksInCell(a,0));before.recycle();after.recycle();
  }finally{life.destroy();}
 }
}
