package com.sitebox.app;

/** Image-space cabinet poses. Coordinates stay inside the original grounded envelope.
 * Positive depth exposes the left side; negative exposes the right. Small rising
 * receding edges follow the room's horizontal architecture, never its sloping roof. */
final class SceneProjection {
 static final SceneProjection[] ALL={
  new SceneProjection("classroom",.075f,.025f,.012f),
  new SceneProjection("library",-.09f,.027f,.016f),
  new SceneProjection("bedroom",.085f,.031f,.015f),
  new SceneProjection("greenhouse",.045f,.023f,.008f),
  new SceneProjection("cafe",.10f,.032f,.018f),
  new SceneProjection("tatami",-.035f,.028f,.006f),
  new SceneProjection("attic",.08f,.035f,.014f),
  new SceneProjection("lab",-.065f,.024f,.010f),
  new SceneProjection("wizard",-.08f,.030f,.014f),
  new SceneProjection("seaside",.065f,.030f,.012f),
  new SceneProjection("train",.11f,.026f,.020f)
 };
 final String id;final float depth,top,rise;
 private SceneProjection(String id,float depth,float top,float rise){this.id=id;this.depth=depth;this.top=top;this.rise=rise;}
 static SceneProjection find(String id){for(SceneProjection p:ALL)if(p.id.equals(id))return p;return ALL[0];}
 /** Clockwise TL, TR, BR, BL, in local view pixels; caller owns the output. */
 void front(float w,float h,float fraction,float[] out){
  float f=Math.max(0,Math.min(1,fraction)),d=Math.abs(depth)*f*w,t=top*f*h,r=rise*f*h;
  boolean left=depth>=0;
  out[0]=left?d:0;out[1]=t+(left?0:r);
  out[2]=left?w:w-d;out[3]=t+(left?r:0);
  out[4]=out[2];out[5]=left?h-r:h;
  out[6]=out[0];out[7]=left?h:h-r;
 }
 /** Back top corners and bottom corner of the exposed physical side. */
 void body(float w,float h,float fraction,float[] front,float[] side,float[] roof){
  float f=Math.max(0,Math.min(1,fraction)),r=rise*f*h;
  boolean left=depth>=0;int edge=left?0:2,bottom=left?6:4;
  float backX=left?0:w,backY=Math.max(0,front[edge+1]-top*f*h);
  side[0]=backX;side[1]=backY;side[2]=front[edge];side[3]=front[edge+1];side[4]=front[bottom];side[5]=front[bottom+1];side[6]=backX;side[7]=h-r;
  roof[0]=left?backX:front[0];roof[1]=left?backY:Math.max(0,front[1]-top*f*h);
  roof[2]=left?front[2]:backX;roof[3]=left?Math.max(0,front[3]-top*f*h):backY;
  roof[4]=front[2];roof[5]=front[3];roof[6]=front[0];roof[7]=front[1];
 }
}
