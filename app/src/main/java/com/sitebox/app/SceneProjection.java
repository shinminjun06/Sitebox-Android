package com.sitebox.app;

/** One metric camera for boards, openings, books and floor contacts. Room axes
 * follow the beam/baseboard and side-wall floor. Vertical posts stay upright. */
final class SceneProjection {
 private static final SceneProjection PORTRAIT=new SceneProjection(854,1842,300,46,534,14,300,1073,612,1106,260,1080,310,10);
 private static final SceneProjection LANDSCAPE=new SceneProjection(1672,941,453,39,877,7,454,621,1155,666,400,636,580,30);
 static final SceneProjection[] ALL={PORTRAIT};
 static final float CABINET_WIDTH=.85f,CABINET_HEIGHT=1.02f,CABINET_DEPTH=.28f;
 private static final float EYE_HEIGHT=1.45f;
 final String id="study";
 final float cameraX,cameraY,widthVanishingX,depth,brightness=.82f;
 final float imageWidth,imageHeight;
 private final double cx,horizon,focal,ux,uz,vx,vz,originX,originZ;
 private final float minX,minY,boxWidth,boxHeight;
 private SceneProjection(float iw,float ih,float x1,float y1,float x2,float y2,float x3,float y3,float x4,float y4,float sideX,float sideY,float rearX,float clearance){
  imageWidth=iw;imageHeight=ih;
  double m1=(y2-y1)/(x2-x1),m2=(y4-y3)/(x4-x3);
  double widthVP=(y3-m2*x3-y1+m1*x1)/(m1-m2);
  horizon=y1+m1*(widthVP-x1);
  double depthVP=x3+(horizon-y3)*(sideX-x3)/(sideY-y3);
  cx=iw*.5;focal=Math.sqrt(-(widthVP-cx)*(depthVP-cx));
  double uLength=Math.hypot(cx-widthVP,focal),vLength=Math.hypot(depthVP-cx,focal);
  ux=(cx-widthVP)/uLength;uz=-focal/uLength;
  vx=(depthVP-cx)/vLength;vz=focal/vLength;
  double rearY=y3+m2*(rearX-x3)+clearance;
  double rearZ=focal*EYE_HEIGHT/(rearY-horizon);
  originX=(rearX-cx)*rearZ/focal-CABINET_DEPTH*vx;
  originZ=rearZ-CABINET_DEPTH*vz;
  cameraX=(float)(depthVP/iw);cameraY=(float)(horizon/ih);widthVanishingX=(float)(widthVP/iw);
  depth=(float)(CABINET_DEPTH*vz/(originZ+CABINET_WIDTH*uz));
  float l=Float.MAX_VALUE,t=Float.MAX_VALUE,r=-Float.MAX_VALUE,b=-Float.MAX_VALUE;
  for(int x=0;x<2;x++)for(int y=0;y<2;y++)for(int z=0;z<2;z++){
   float px=(float)sourceX(x,z),py=(float)sourceY(x,y,z);
   l=Math.min(l,px);t=Math.min(t,py);r=Math.max(r,px);b=Math.max(b,py);
  }
  minX=l;minY=t;boxWidth=r-l;boxHeight=b-t;
 }
 private double distance(float x,float z){return originZ+x*CABINET_WIDTH*uz+z*CABINET_DEPTH*vz;}
 private double sourceX(float x,float z){return cx+focal*(originX+x*CABINET_WIDTH*ux+z*CABINET_DEPTH*vx)/distance(x,z);}
 private double sourceY(float x,float y,float z){return horizon+focal*(EYE_HEIGHT-(1-y)*CABINET_HEIGHT)/distance(x,z);}
 /** Source-image bounds of the solid cabinet, excluding the plant and picture. */
 float[] imageBounds(){return new float[]{minX/imageWidth,minY/imageHeight,(minX+boxWidth)/imageWidth,(minY+boxHeight)/imageHeight};}
 void point(float w,float h,float fraction,float x,float y,float z,float[] out,int offset){
  float f=Math.max(0,Math.min(1,fraction));
  float px=(float)((sourceX(x,z)-minX)/boxWidth),py=(float)((sourceY(x,y,z)-minY)/boxHeight);
  out[offset]=w*(x+(px-x)*f);out[offset+1]=h*(y+(py-y)*f);
 }
 static SceneProjection find(String id){return PORTRAIT;}
 static SceneProjection find(String id,boolean wide){return wide?LANDSCAPE:PORTRAIT;}
 float lightDirection(){return -.12f;}
 boolean leftVisible(float ignored){return false;}
 boolean rightVisible(float ignored){return true;}
 boolean topVisible(float ignored){return true;}
 boolean leftVisible(float x,float y,float f){return false;}
 boolean rightVisible(float x,float y,float f){return f>0;}
 boolean topVisible(float x,float y,float f){return f>0;}
 static float contact(float left,float right,float fraction,float width){return ((left+(right-left)*fraction)/width+.02f)/1.04f;}
 void front(float w,float h,float f,float[] out){
  point(w,h,f,0,0,0,out,0);point(w,h,f,1,0,0,out,2);
  point(w,h,f,1,1,0,out,4);point(w,h,f,0,1,0,out,6);
 }
 void front(float w,float h,float f,float vx,float vy,float[] out){front(w,h,f,out);}
 void front(float w,float h,float f,float vx,float vy,float ux,float[] out){front(w,h,f,out);}
 void body(float w,float h,float f,float[] front,float[] side,float[] roof){
  point(w,h,f,1,0,1,side,0);point(w,h,f,1,0,0,side,2);
  point(w,h,f,1,.955f,0,side,4);point(w,h,f,1,.955f,1,side,6);
  point(w,h,f,0,0,1,roof,0);point(w,h,f,1,0,1,roof,2);
  point(w,h,f,1,0,0,roof,4);point(w,h,f,0,0,0,roof,6);
 }
 void body(float w,float h,float f,float vx,float vy,float[] a,float[] side,float[] roof){body(w,h,f,a,side,roof);}
 void body(float w,float h,float f,float vx,float vy,float ux,float[] a,float[] side,float[] roof){body(w,h,f,a,side,roof);}
 void footprint(float w,float h,float[] out){
  point(w,h,1,.0225f,1,.94f,out,0);point(w,h,1,.9775f,1,.94f,out,2);
  point(w,h,1,.9775f,1,.06f,out,4);point(w,h,1,.0225f,1,.06f,out,6);
 }
}
