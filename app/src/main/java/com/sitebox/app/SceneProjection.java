package com.sitebox.app;

/** Perspective cuboid parallel to the room wall. Rear corners converge on the camera horizon. */
final class SceneProjection {
 // A shallow window-side perspective matches the near-frontal cabinet in the reference room.
 static final SceneProjection[] ALL={new SceneProjection("study",.69f,.43f,.055f,.82f)};
 final String id; final float cameraX,cameraY,depth,brightness;
 private SceneProjection(String id,float x,float y,float depth,float brightness){this.id=id;cameraX=x;cameraY=y;this.depth=depth;this.brightness=brightness;}
 static SceneProjection find(String id){for(SceneProjection p:ALL)if(p.id.equals(id))return p;return ALL[0];}
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
 void front(float w,float h,float f,float[] out){front(w,h,f,-.25f,-.5f,out);}
 void front(float w,float h,float f,float vx,float vy,float[] out){
  float s=fit(f,vx,vy),left=(w-w*s)*.5f,right=(w+w*s)*.5f,top=h-h*s;
  out[0]=left;out[1]=top;out[2]=right;out[3]=top;out[4]=right;out[5]=h;out[6]=left;out[7]=h;
 }
 void body(float w,float h,float f,float[] front,float[] side,float[] roof){body(w,h,f,-.25f,-.5f,front,side,roof);}
 void body(float w,float h,float f,float vx,float vy,float[] a,float[] side,float[] roof){
  float k=1/(1+depth*Math.max(0,Math.min(1,f))),vX=vx*w,vY=vy*h;
  float x=leftVisible(vx,vy,f)?a[0]:a[2],top=a[1],bottom=top+.94f*(h-top);
  side[0]=vX+k*(x-vX);side[1]=vY+k*(top-vY);side[2]=x;side[3]=top;side[4]=x;side[5]=bottom;side[6]=vX+k*(x-vX);side[7]=vY+k*(bottom-vY);
  roof[0]=vX+k*(a[0]-vX);roof[1]=vY+k*(a[1]-vY);roof[2]=vX+k*(a[2]-vX);roof[3]=roof[1];roof[4]=a[2];roof[5]=a[3];roof[6]=a[0];roof[7]=a[1];
 }
}
