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
  System.out.println("PASS: seven-book capacity, thin paper edge, scene anchors in portrait/landscape/small screens");
 }
}
