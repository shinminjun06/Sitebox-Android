package com.sitebox.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import org.json.*;
import java.nio.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

/** Asset/host contracts only. GPU rendering is exercised on Android in ModelSmokeInstrumentation. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=28) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class OriginalPhotoRoomTest {
 @Test public void suppliedRoomModelHasClosedWallsCabinetAndNineExplicitSlots()throws Exception{
  java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream();try(java.io.InputStream in=RuntimeEnvironment.getApplication().getAssets().open(RoomModelGeometry.ASSET)){byte[] chunk=new byte[8192];int n;while((n=in.read(chunk))!=-1)output.write(chunk,0,n);}
  byte[] bytes=output.toByteArray();ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);assertEquals(0x46546c67,b.getInt());assertEquals(2,b.getInt());assertEquals(bytes.length,b.getInt());int jsonSize=b.getInt();assertEquals(0x4e4f534a,b.getInt());JSONObject glb=new JSONObject(new String(bytes,20,jsonSize,java.nio.charset.StandardCharsets.UTF_8));JSONArray nodes=glb.getJSONArray("nodes");int anchors=0,walls=0,sides=0,backs=0;
  for(int i=0;i<nodes.length();i++){String name=nodes.getJSONObject(i).optString("name");if(name.startsWith("BOOK_SLOT_"))anchors++;if(name.startsWith("Wall_"))walls++;if(name.startsWith("Bookshelf_3x3_Side"))sides++;if(name.startsWith("Bookshelf_3x3_Back"))backs++;}
  assertEquals(9,anchors);assertEquals(3,walls);assertEquals(2,sides);assertEquals(1,backs);assertTrue(glb.getJSONArray("meshes").length()>700);
 }
 @Test public void fullRoomReplacesPhotographicFurnitureWithoutChangingLibrary(){
  ActivityController<MainActivity> life=Robolectric.buildActivity(MainActivity.class).create();try{MainActivity a=life.get();RoomExperienceTest.layout(a,591,1206);String data=a.sites.toString();a.collapseToScene(false);assertEquals(View.GONE,a.backdrop.getVisibility());assertEquals(View.GONE,a.roomForeground.getVisibility());assertEquals(View.GONE,a.studyLive.getVisibility());assertSame(a.roomLayer,a.roomExperience.model.getParent());a.expandFromScene(false);assertEquals(data,a.sites.toString());assertTrue(a.currentCabinet.usesModelFrame());}finally{life.destroy();}
 }
 @Test public void dryCloudsAndNightPrecipitationHaveTheirOwnWindowImages(){
  ModelRoomView model=new ModelRoomView(RuntimeEnvironment.getApplication());
  model.setAtmosphere(1,1,0,true);assertEquals("cloud",model.windowState());
  model.setAtmosphere(0,1,1,true);assertEquals("night_rain",model.windowState());
  model.setAtmosphere(0,1,2,true);assertEquals("night_snow",model.windowState());
  model.setWeatherImages(false);assertEquals("night",model.windowState());model.dispose();
 }
 @Test public void windowVariantsChangeActualImageContentAndKeepUvDimensions()throws Exception{
  Bitmap day=null;for(String state:new String[]{"day","night","rain","snow","cloud","night_rain","night_snow"}){Bitmap image;try(java.io.InputStream in=RuntimeEnvironment.getApplication().getAssets().open("models/window_"+state+".png")){image=BitmapFactory.decodeStream(in);}assertNotNull(image);if(day==null)day=image;else{assertEquals(day.getWidth(),image.getWidth());assertEquals(day.getHeight(),image.getHeight());assertFalse("Actual landscape changes, not just room brightness",day.sameAs(image));image.recycle();}}day.recycle();
 }
}
