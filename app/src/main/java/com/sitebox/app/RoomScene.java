package com.sitebox.app;

/** Photographic registration in normalized source coordinates, independent of device pixels. */
final class RoomScene {
 static final String ROOT="themes/rooms/student/";
 static final float[] CABINET={.035f,.385f,.225f,.384f,.225f,.590f,.035f,.670f};
 private static final float CABINET_X=(CABINET[0]+CABINET[2]+CABINET[4]+CABINET[6])*.25f;
 private static final float CABINET_Y=(CABINET[1]+CABINET[3]+CABINET[5]+CABINET[7])*.25f;
 private static final float CABINET_HEIGHT=(CABINET[7]-CABINET[1]+CABINET[5]-CABINET[3])*.5f;
 static final float[] LAMP={.340f,.350f,.470f,.407f};
 static final float[] SKY={.360f,.115f,.940f,.455f};
 // Effect coverage is deliberately generous; room_shell alpha preserves the exact
 // mullions, lace, lamp and plant silhouettes above the outdoor layer.
 static final float[][] GLASS={{.350f,.130f,.920f,.130f,.920f,.440f,.350f,.440f}};
 static final int OUTSIDE=0,ROOM=1,FOREGROUND=2;
 private RoomScene(){}
 /** Fit the complete photographic composition. Extreme ratios use an ambient surround. */
 static void transform(float w,float h,float iw,float ih,float progress,int layer,boolean reduced,float[] out){
  camera(w,h,iw,ih,progress,layer,reduced,w*.065f,h*.12f,w*.935f,h*.12f+w*.87f*CabinetGeometry.ASPECT,out);
 }
 static void camera(float w,float h,float iw,float ih,float progress,int layer,boolean reduced,float left,float top,float right,float bottom,float[] out){
  float base=w>=h*.8f?Math.min(w/iw,h/ih):Math.min(Math.max(w/iw,h/ih),w/(iw*.89f)),q=1-clamp(progress);
  float bx=iw*base>w?-(iw*base-w)*.22f:(w-iw*base)*.5f,by=(h-ih*base)*.5f;
  float anchorX=iw*base*CABINET_X,anchorY=ih*base*CABINET_Y;
  float zoomTo=Math.max(1,Math.min(3,(bottom-top)/(ih*base*CABINET_HEIGHT)));
  float depth=reduced?0:layer==OUTSIDE?.12f:layer==ROOM?1:1.18f;
  float zoom=1+(zoomTo-1)*depth*q;
  float dx=((left+right)*.5f-bx-anchorX)*depth*q,dy=((top+bottom)*.5f-by-anchorY)*depth*q;
  out[0]=base*zoom;out[1]=bx+anchorX*(1-zoom)+dx;out[2]=by+anchorY*(1-zoom)+dy;
  if(layer==ROOM){
   if(iw*out[0]>=w)out[1]=Math.max(w-iw*out[0],Math.min(0,out[1]));
   if(ih*out[0]>=h)out[2]=Math.max(h-ih*out[0],Math.min(0,out[2]));
  }
 }
 /** Keep a frame-free outside texture behind every moving pane, including overscan. */
 static void coverOutside(float iw,float ih,float[] room,float[] outside){
  float l=Float.MAX_VALUE,t=Float.MAX_VALUE,r=-Float.MAX_VALUE,b=-Float.MAX_VALUE;
  for(float[] pane:GLASS)for(int i=0;i<8;i+=2){float x=room[1]+pane[i]*iw*room[0],y=room[2]+pane[i+1]*ih*room[0];l=Math.min(l,x);t=Math.min(t,y);r=Math.max(r,x);b=Math.max(b,y);}
  float required=Math.max((r-l)/(iw*(SKY[2]-SKY[0])),(b-t)/(ih*(SKY[3]-SKY[1])));
  outside[0]=Math.max(outside[0],required);
  outside[1]=Math.max(r-iw*SKY[2]*outside[0],Math.min(l-iw*SKY[0]*outside[0],outside[1]));
  outside[2]=Math.max(b-ih*SKY[3]*outside[0],Math.min(t-ih*SKY[1]*outside[0],outside[2]));
 }
 static float clamp(float p){return Math.max(0,Math.min(1,p));}
 static void map(float[] normalized,float iw,float ih,float[] transform,float[] out){
  for(int i=0;i<normalized.length;i+=2){out[i]=transform[1]+normalized[i]*iw*transform[0];out[i+1]=transform[2]+normalized[i+1]*ih*transform[0];}
 }
 static void bounds(float[] quad,float[] out){
  out[0]=out[1]=Float.MAX_VALUE;out[2]=out[3]=-Float.MAX_VALUE;
  for(int i=0;i<quad.length;i+=2){out[0]=Math.min(out[0],quad[i]);out[1]=Math.min(out[1],quad[i+1]);out[2]=Math.max(out[2],quad[i]);out[3]=Math.max(out[3],quad[i+1]);}
 }
}
