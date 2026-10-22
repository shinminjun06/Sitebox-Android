package com.sitebox.app;

/** Two-point room perspective, with vertical cabinet posts kept upright. */
final class SceneProjection {
 // Intersections of the back-wall ceiling beam and baseboard, measured on each source plate.
 private static final SceneProjection PORTRAIT=calibrated(854,1842,.78f,300,46,534,14,300,1073,612,1106);
 private static final SceneProjection LANDSCAPE=calibrated(1672,941,.68f,453,39,877,7,454,621,1155,666);
 static final SceneProjection[] ALL={PORTRAIT};
 final String id="study";
 final float cameraX,cameraY,widthVanishingX,depth=.04f,brightness=.82f;
 private SceneProjection(float x,float y,float widthX){cameraX=x;cameraY=y;widthVanishingX=widthX;}
 private static SceneProjection calibrated(float iw,float ih,float depthX,float x1,float y1,float x2,float y2,float x3,float y3,float x4,float y4){
  float m1=(y2-y1)/(x2-x1),m2=(y4-y3)/(x4-x3);
  float x=(y3-m2*x3-y1+m1*x1)/(m1-m2),y=y1+m1*(x-x1);
  return new SceneProjection(depthX,y/ih,x/iw);
 }
 static SceneProjection find(String id){return PORTRAIT;}
 static SceneProjection find(String id,boolean wide){return wide?LANDSCAPE:PORTRAIT;}
 float lightDirection(){return -.12f;}
 private float fit(float f,float vx,float vy){
  float d=depth*Math.max(0,Math.min(1,f));
  return Math.max(.001f,Math.min(1,Math.min(1+d*vy,Math.min(1+2*d*vx,1+2*d*(1-vx)))));
 }
 boolean leftVisible(float vx){return leftVisible(vx,0,0);}
 boolean rightVisible(float vx){return rightVisible(vx,0,0);}
 boolean topVisible(float vy){return vy<0;}
 boolean leftVisible(float vx,float vy,float f){return vx<(1-fit(f,vx,vy))*.5f;}
 boolean rightVisible(float vx,float vy,float f){return vx>(1+fit(f,vx,vy))*.5f;}
 boolean topVisible(float vx,float vy,float f){return vy<1-fit(f,vx,vy);}
 static float contact(float left,float right,float fraction,float width){return ((left+(right-left)*fraction)/width+.02f)/1.04f;}
 void front(float w,float h,float f,float[] out){front(w,h,f,-.25f,-.5f,-16,out);}
 void front(float w,float h,float f,float vx,float vy,float[] out){front(w,h,f,vx,vy,-16,out);}
 void front(float w,float h,float f,float vx,float vy,float ux,float[] out){
  float s=fit(f,vx,vy),left=(w-w*s)*.5f,right=(w+w*s)*.5f,top=h-h*s,u=ux*w,v=vy*h;
  float ql=u<left?(left-u)/(right-u):1,qr=u>right?(u-right)/(u-left):1;
  ql=1+f*(ql-1);qr=1+f*(qr-1);
  // Reserve headroom for the far upper corner as well as the near one during interpolation.
  float q=Math.min(ql,qr);top=Math.max(top,-v*(depth*f+(1-q)/q));
  out[0]=left;out[1]=v+ql*(top-v);out[2]=right;out[3]=v+qr*(top-v);
  out[4]=right;out[5]=v+qr*(h-v);out[6]=left;out[7]=v+ql*(h-v);
 }
 void body(float w,float h,float f,float[] front,float[] side,float[] roof){body(w,h,f,-.25f,-.5f,-16,front,side,roof);}
 void body(float w,float h,float f,float vx,float vy,float[] a,float[] side,float[] roof){body(w,h,f,vx,vy,-16,a,side,roof);}
 void body(float w,float h,float f,float vx,float vy,float ux,float[] a,float[] side,float[] roof){
  float vX=vx*w,vY=vy*h,baseHeight=Math.max(a[7]-a[1],a[5]-a[3]);
  // The two uprights have different camera distances. A single shared retreat factor
  // would make the rear crown converge to a different width vanishing point.
  float kl=1/(1+depth*f*(a[7]-a[1])/baseHeight),kr=1/(1+depth*f*(a[5]-a[3])/baseHeight);
  boolean left=leftVisible(vx,vy,f);float k=left?kl:kr,x=left?a[0]:a[2],top=left?a[1]:a[3],foot=left?a[7]:a[5],bottom=top+.94f*(foot-top);
  side[0]=vX+k*(x-vX);side[1]=vY+k*(top-vY);side[2]=x;side[3]=top;side[4]=x;side[5]=bottom;side[6]=vX+k*(x-vX);side[7]=vY+k*(bottom-vY);
  roof[0]=vX+kl*(a[0]-vX);roof[1]=vY+kl*(a[1]-vY);roof[2]=vX+kr*(a[2]-vX);roof[3]=vY+kr*(a[3]-vY);
  roof[4]=a[2];roof[5]=a[3];roof[6]=a[0];roof[7]=a[1];
 }
}
