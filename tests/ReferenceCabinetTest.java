package com.sitebox.app;
/** Keep the target photograph's small floor-standing cabinet placement. Angle is
 * checked independently against the room's structural lines in RoomPerspectiveTest. */
public class ReferenceCabinetTest {
 public static void main(String[] args){
  SceneTheme room=SceneTheme.find("study");SceneProjection p=SceneProjection.find("study",false);
  float[] b=room.sceneBounds(591,1206,854,1842,0,0,0,0),t=room.imageTransform(591,1206,854,1842);
  float w=b[2]-b[0],h=b[3]-b[1],vx=(t[1]+854*t[0]*p.cameraX-b[0])/w,vy=(t[2]+1842*t[0]*p.cameraY-b[1])/h,ux=(t[1]+854*t[0]*p.widthVanishingX-b[0])/w;
  float[] front=new float[8];p.front(w,h,1,vx,vy,ux,front);
  if(b[0]<150||b[2]>380||b[1]<490||b[3]>800)throw new AssertionError("Cabinet must remain in the reference's back-wall floor region");
  if(w<170||w>230||h<210||h>270)throw new AssertionError("Reference cabinet scale must stay small relative to the desk and window");
  if(front[1]+b[1]<500||front[7]+b[1]<740)throw new AssertionError("Cabinet must stand on the floor below the picture");
  System.out.println("PASS: cabinet keeps reference floor placement and scale with wall-calibrated perspective");
 }
}
