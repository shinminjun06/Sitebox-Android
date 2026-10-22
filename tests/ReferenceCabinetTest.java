package com.sitebox.app;
/** Cabinet silhouette measured from the user's 591 × 1206 target room photograph. */
public class ReferenceCabinetTest {
 public static void main(String[] args){
  SceneTheme room=SceneTheme.find("study");SceneProjection p=SceneProjection.find("study",false);
  float[] b=room.sceneBounds(591,1206,854,1842,0,0,0,0),t=room.imageTransform(591,1206,854,1842);
  float w=b[2]-b[0],h=b[3]-b[1],vx=(t[1]+854*t[0]*p.cameraX-b[0])/w,vy=(t[2]+1842*t[0]*p.cameraY-b[1])/h,ux=(t[1]+854*t[0]*p.widthVanishingX-b[0])/w;
  float[] front=new float[8];p.front(w,h,1,vx,vy,ux,front);
  float[] target={155,530,334,533,334,773,155,766};
  for(int i=0;i<8;i++){
   float actual=front[i]+b[i%2];if(Math.abs(actual-target[i])>7)throw new AssertionError("Portrait silhouette misses target corner "+i+": "+actual+" vs "+target[i]);
  }
  float baseSlope=(front[5]-front[7])/(front[4]-front[6]);
  if(baseSlope>.035f)throw new AssertionError("Portrait base leans too much for the target photograph: slope="+baseSlope);
  System.out.println("PASS: portrait cabinet silhouette stays within seven pixels of the target photograph");
 }
}
