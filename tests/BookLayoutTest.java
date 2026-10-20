package com.sitebox.app;
public class BookLayoutTest {
 public static void main(String[] args)throws Exception{
  Class<?> layout;
  try{layout=Class.forName("com.sitebox.app.BookLayout");}catch(ClassNotFoundException missing){throw new AssertionError("Seven-book spine layout is missing");}
  int capacity=layout.getDeclaredField("CAPACITY").getInt(null);
  if(capacity!=7)throw new AssertionError("Seven books must fit each cubby");
  java.lang.reflect.Method spine=layout.getDeclaredMethod("spineRight",float.class,float.class);
  float right=(Float)spine.invoke(null,1f,21f);
  if(right<20f||right>21f)throw new AssertionError("Paper edge must occupy at most 5% of width");
  java.lang.reflect.Method scene=layout.getDeclaredMethod("sceneBounds",float.class,float.class,float.class,float.class,float.class,float.class,float.class,float.class);
  for(float[] screen:new float[][]{{720,1560},{1560,720},{320,480}}){
   float[] bounds=(float[])scene.invoke(null,screen[0],screen[1],852f,1846f,0f,24f,0f,24f);
   if(bounds[0]<0||bounds[1]<24||bounds[2]>screen[0]||bounds[3]>screen[1]-24)throw new AssertionError("Miniature must remain visible");
   if(Math.abs((bounds[3]-bounds[1])/(bounds[2]-bounds[0])-1.3f)>.001f)throw new AssertionError("Furniture aspect ratio changes");
  }
  java.lang.reflect.Method wide=layout.getDeclaredMethod("wideScene",float.class,float.class);
  if((Boolean)wide.invoke(null,720f,1560f)||(Boolean)wide.invoke(null,1080f,2400f)||!(Boolean)wide.invoke(null,1200f,1440f)||!(Boolean)wide.invoke(null,1080f,1080f))throw new AssertionError("Room composition must adapt to near-square windows");
  // The cabinet must sit beneath the sill and reach the floor in the shipped room art.
  for(float[] profile:new float[][]{{720,1560,852,1846,.455f,.77f},{1560,720,1672,941,.40f,.81f},{1200,1440,1672,941,.40f,.81f},{1080,1080,1672,941,.40f,.81f}}){
   float w=profile[0],h=profile[1],iw=profile[2],ih=profile[3];
   float scale=Math.max(w/iw,h/ih),offset=(h-ih*scale)*.5f;
   float[] cabinet=(float[])scene.invoke(null,w,h,iw,ih,0f,24f,0f,24f);
   float sill=offset+ih*scale*profile[4],floor=offset+ih*scale*profile[5];
   if(cabinet[1]<sill+8)throw new AssertionError("Cabinet crown overlaps classroom windowsill");
   if(cabinet[3]<floor+8||cabinet[3]>floor+ih*scale*.06f)throw new AssertionError("Cabinet feet must stand on classroom floor, not float on wall");
  }
  System.out.println("PASS: seven-book capacity, thin paper edge, scene anchors in portrait/landscape/small screens");
 }
}
