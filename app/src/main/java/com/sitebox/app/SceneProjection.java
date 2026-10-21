package com.sitebox.app;

/** Perspective cuboid parallel to the room wall. Rear corners converge on the camera horizon. */
final class SceneProjection {
 static final SceneProjection[] ALL={
  new SceneProjection("classroom",.50f,.41f,.10f,.96f),
  new SceneProjection("library",.51f,.46f,.09f,.82f),
  new SceneProjection("bedroom",.46f,.40f,.10f,.94f),
  new SceneProjection("greenhouse",.48f,.49f,.11f,.96f),
  new SceneProjection("cafe",.46f,.45f,.10f,.92f),
  new SceneProjection("tatami",.50f,.45f,.09f,.94f),
  new SceneProjection("attic",.49f,.35f,.10f,.91f),
  new SceneProjection("lab",.54f,.43f,.08f,.93f),
  new SceneProjection("wizard",.54f,.43f,.09f,.73f),
  new SceneProjection("seaside",.50f,.40f,.10f,.97f),
  new SceneProjection("train",.46f,.43f,.10f,.88f)
 };
 final String id; final float cameraX,cameraY,depth,brightness;
 private SceneProjection(String id,float x,float y,float depth,float brightness){this.id=id;cameraX=x;cameraY=y;this.depth=depth;this.brightness=brightness;}
 static SceneProjection find(String id){for(SceneProjection p:ALL)if(p.id.equals(id))return p;return ALL[0];}
 boolean leftVisible(float vx){return vx<0;}
 boolean rightVisible(float vx){return vx>1;}
 boolean topVisible(float vy){return vy<0;}
 // Default only for pure geometry callers; the renderer passes the actual cropped camera.
 void front(float w,float h,float f,float[] out){front(w,h,f,-.25f,-.5f,out);}
 void front(float w,float h,float f,float vx,float vy,float[] out){
  f=Math.max(0,Math.min(1,f));float k=1/(1+depth*f);
  float bx=vx*(1-k),br=k+vx*(1-k),by=vy*(1-k);
  float minX=Math.min(0,bx),maxX=Math.max(1,br),minY=Math.min(0,by);
  float scale=Math.min(1/(maxX-minX),1/(1-minY)),sx=w*scale,sy=h*scale;
  float padding=(w-(maxX-minX)*sx)*.5f;
  out[0]=padding-minX*sx;out[1]=h-sy;out[2]=padding+(1-minX)*sx;out[3]=out[1];out[4]=out[2];out[5]=h;out[6]=out[0];out[7]=h;
 }
 void body(float w,float h,float f,float[] front,float[] side,float[] roof){body(w,h,f,-.25f,-.5f,front,side,roof);}
 void body(float w,float h,float f,float vx,float vy,float[] a,float[] side,float[] roof){
  f=Math.max(0,Math.min(1,f));float k=1/(1+depth*f),bx=vx*(1-k),br=k+vx*(1-k),by=vy*(1-k);
  float minX=Math.min(0,bx),maxX=Math.max(1,br),minY=Math.min(0,by);
  float scale=Math.min(1/(maxX-minX),1/(1-minY)),sx=w*scale,sy=h*scale;
  float padding=(w-(maxX-minX)*sx)*.5f,originY=h-sy;
  boolean left=leftVisible(vx);float x=left?0:1;
  // Body ends above the transparent sprite feet, never as a full-height side plank.
  float bottom=.94f,fx=padding+(x-minX)*sx,fy=originY+bottom*sy;
  float rx=padding+(k*x+vx*(1-k)-minX)*sx,rt=originY+by*sy,rb=originY+(k*bottom+vy*(1-k))*sy;
  side[0]=rx;side[1]=rt;side[2]=fx;side[3]=a[1];side[4]=fx;side[5]=fy;side[6]=rx;side[7]=rb;
  roof[0]=padding+(bx-minX)*sx;roof[1]=originY+by*sy;roof[2]=padding+(br-minX)*sx;roof[3]=roof[1];roof[4]=a[2];roof[5]=a[3];roof[6]=a[0];roof[7]=a[1];
 }
}
