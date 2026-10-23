package com.sitebox.app;

import android.animation.ValueAnimator;
import android.graphics.*;
import android.view.View;
import android.widget.LinearLayout;
import java.io.*;
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
@Config(sdk=28,qualifiers="w591dp-h1206dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class RoomRenderingTest {
 @Test public void nativeFramesAndAllNineProjectedTouchCellsShareTheirCoordinates()throws Exception{
  ActivityController<MainActivity> life=Robolectric.buildActivity(MainActivity.class).create();
  try{
   MainActivity a=life.get();a.setStudyMotion(false);a.sites=new JSONArray();
   for(int c=0;c<9;c++)for(int n=0;n<3;n++)a.sites.put(new JSONObject().put("name","나의 책 "+c+"-"+n).put("url","https://example.com/"+c+"/"+n).put("category",a.categories[c]));
   a.populate();layout(a,591,1206);ambient(a,1);a.collapseToScene(false);layout(a,591,1206);
   assertSame(a.currentCabinet,a.miniature.content());
   for(int[] size:new int[][]{{591,1206},{360,800},{1080,2400},{1080,1080},{926,428}}){
    layout(a,size[0],size[1]);float[] q=new float[8];a.miniature.copyRenderedCorners(q);
    Matrix m=new Matrix();float w=a.currentCabinet.getWidth(),h=a.currentCabinet.getHeight();float[] src={0,0,w,0,w,h,0,h};m.setPolyToPoly(src,0,q,0,4);
    for(int cell=0;cell<9;cell++){
     float[] b=CabinetGeometry.cell(cell),p={(b[0]+b[2])*.5f*w,(b[1]+b[3])*.5f*h};m.mapPoints(p);
     assertTrue("Visible cabinet cell must be inside viewport",p[0]>=0&&p[0]<size[0]&&p[1]>=0&&p[1]<size[1]);
     assertEquals("Inverse touch cell on "+size[0]+"x"+size[1],cell,a.miniature.hitCell(p[0],p[1]));assertTrue(a.miniature.hitFrame(p[0],p[1]));
    }
    assertFalse(a.miniature.hitFrame(0,0));
    android.view.accessibility.AccessibilityNodeInfo node=android.view.accessibility.AccessibilityNodeInfo.obtain();a.miniature.onInitializeAccessibilityNodeInfo(node);Rect accessible=new Rect();node.getBoundsInParent(accessible);assertTrue("Accessible target bounds must match the visible cabinet",accessible.width()<a.stage.getWidth()*.5f);assertTrue(a.miniature.hitFrame(accessible.exactCenterX(),accessible.exactCenterY()));node.recycle();
   }
   layout(a,591,1206);save(a,"room-day");int daySky=sample(a,.77f,.22f);ambient(a,0);save(a,"room-night");int nightSky=sample(a,.77f,.22f);assertTrue("Window must relight with the room",(Color.red(nightSky)+Color.green(nightSky)+Color.blue(nightSky))<(Color.red(daySky)+Color.green(daySky)+Color.blue(daySky))*.7f);a.lampSwitch.performClick();save(a,"room-night-off");a.lampSwitch.performClick();ambient(a,1);
   a.expandFromScene(true);ValueAnimator animator=(ValueAnimator)GoogleAccountTest.get(a.roomExperience.transition,"animator");animator.setCurrentPlayTime(250);save(a,"approach");
   float before=a.roomExperience.transition.progress();float[] old=new float[8],now=new float[8];a.miniature.copyRenderedCorners(old);a.collapseToScene(true);a.miniature.copyRenderedCorners(now);
   assertEquals("Retargeting starts at current progress",before,a.roomExperience.transition.progress(),.00001f);for(int i=0;i<8;i++)assertEquals("No pose jump on reversal",old[i],now[i],.01f);
   a.expandFromScene(false);assertFalse(a.sceneAnimating);assertEquals(View.VISIBLE,a.currentCabinet.getVisibility());assertEquals(View.GONE,a.miniature.getVisibility());save(a,"editor-day");
   for(int i=0;i<a.currentCabinet.getChildCount();i++){View v=a.currentCabinet.getChildAt(i);if(v instanceof android.widget.TextView){android.widget.TextView text=(android.widget.TextView)v;assertNotNull("Category text must be laid out",text.getLayout());}}
   a.collapseToScene(false);layout(a,926,428);save(a,"wide-room");layout(a,360,900);save(a,"tall-room");
  }finally{life.destroy();}
 }
 @Test public void overflowingBooksPageAndRebindWithoutMovingTheRoom()throws Exception{
  ActivityController<MainActivity> life=Robolectric.buildActivity(MainActivity.class).create();
  try{
   MainActivity a=life.get();a.setStudyMotion(false);a.sites=new JSONArray();
   for(int n=0;n<71;n++)a.sites.put(new JSONObject().put("name","책 "+n).put("url","https://example.com/"+n).put("category",a.categories[0]));
   a.populate();layout(a,428,926);a.collapseToScene(false);layout(a,428,926);float[] old=new float[8],now=new float[8];a.miniature.copyRenderedCorners(old);
   assertEquals(7,booksInCell(a,0));a.page=10;a.populate();layout(a,428,926);assertSame(a.currentCabinet,a.miniature.content());assertEquals(1,booksInCell(a,0));assertEquals(71,a.sites.length());
   a.miniature.copyRenderedCorners(now);for(int i=0;i<8;i++)assertEquals(old[i],now[i],.05f);
   BookshelfView frame=a.currentCabinet;ambient(a,0);assertSame("Weather cannot rebuild the library",frame,a.currentCabinet);assertEquals(1,a.roomExperience.transition.progress(),.0001f);
  }finally{life.destroy();}
 }
 @Test public void savedBooksAndReducedEffectsSurviveRecreation()throws Exception{
  ActivityController<MainActivity> life=Robolectric.buildActivity(MainActivity.class).create();
  try{
   MainActivity a=life.get();a.sites=new JSONArray().put(new JSONObject().put("name","유지할 책").put("url","https://example.com").put("category",a.categories[0]).put("notes","메모").put("fav",true).put("sortOrder",123));
   a.save();a.roomExperience.setReduced(true);life.recreate();a=life.get();layout(a,428,926);
   assertEquals("메모",a.sites.getJSONObject(0).getString("notes"));assertTrue(a.sites.getJSONObject(0).getBoolean("fav"));assertTrue(a.roomExperience.reduced());
  }finally{life.destroy();}
 }
 @Test public void lampRelightingPreservesSunlightAndOutsidePixels()throws Exception{
  ActivityController<MainActivity> life=Robolectric.buildActivity(MainActivity.class).create();
  try{
   MainActivity a=life.get();a.setStudyMotion(false);layout(a,591,1206);ambient(a,1);a.collapseToScene(false);layout(a,591,1206);
   int sun=sample(a,.60f,.67f),sky=sample(a,.77f,.22f),bulb=sample(a,.425f,.386f);a.lampSwitch.performClick();
   assertEquals("Daylight shadows stay fixed",sun,sample(a,.60f,.67f));assertEquals("Outside scene stays fixed",sky,sample(a,.77f,.22f));assertNotEquals("The physical bulb must change",bulb,sample(a,.425f,.386f));
   a.lampSwitch.performClick();ambient(a,0);int lit=sample(a,.42f,.65f);a.lampSwitch.performClick();int unlit=sample(a,.42f,.65f);
   assertTrue("Lamp-off removes the warm indoor pool",Color.red(lit)-Color.blue(lit)>Color.red(unlit)-Color.blue(unlit)+10);
  }finally{life.destroy();}
 }
 static int booksInCell(MainActivity a,int cell){
  for(int i=0;i<a.currentCabinet.getChildCount();i++){View v=a.currentCabinet.getChildAt(i);if(Integer.valueOf(cell).equals(v.getTag())){LinearLayout row=(LinearLayout)v;int count=0;for(int j=0;j<row.getChildCount();j++)if(row.getChildAt(j) instanceof BookArtworkView){count++;assertTrue(row.getChildAt(j).getWidth()>0);assertTrue(row.getChildAt(j).getRight()<=row.getWidth());}return count;}}
  throw new AssertionError("Missing book row");
 }
 static void layout(MainActivity a,int w,int h){RoomExperienceTest.layout(a,w,h);}
 static void ambient(MainActivity a,int day){long t=1700000000000L;a.setAmbientPreview(new WeatherState(20,0,0,0,t,0,0,t,true,0,day),t);}
 static int sample(MainActivity a,float x,float y){Bitmap b=Bitmap.createBitmap(a.stage.getWidth(),a.stage.getHeight(),Bitmap.Config.ARGB_8888);a.stage.draw(new Canvas(b));float[] p={x*a.sceneBackdrop.getWidth(),y*a.sceneBackdrop.getHeight()};a.backdrop.getImageMatrix().mapPoints(p);int color=b.getPixel((int)p[0],(int)p[1]);b.recycle();return color;}
 static void save(MainActivity a,String name)throws Exception{File dir=new File("build/reports/room-experience");dir.mkdirs();Bitmap b=Bitmap.createBitmap(a.stage.getWidth(),a.stage.getHeight(),Bitmap.Config.ARGB_8888);a.stage.draw(new Canvas(b));try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
}
