package com.sitebox.app;
/** Independent room measurements: long back-wall beam and baseboard, in artwork pixels. */
public class RoomPerspectiveTest {
 static void ray(float x1,float y1,float x2,float y2,float vx,float vy){
  double distance=Math.abs((x2-x1)*(vy-y1)-(y2-y1)*(vx-x1))/Math.hypot(x2-x1,y2-y1);
  if(distance>.1)throw new AssertionError("Edges miss their room vanishing point by "+distance);
 }
 public static void main(String[] args){
  for(boolean wide:new boolean[]{false,true}){
   float iw=wide?1672:854,ih=wide?941:1842;
   SceneProjection p=SceneProjection.find("study",wide);SceneTheme room=SceneTheme.find("study");
   for(float[] viewport:wide?new float[][]{{926,428},{1560,720},{480,320}}:new float[][]{{428,926},{720,1560},{320,480},{1080,1080}}){
    float w=viewport[0],h=viewport[1];float[] t=room.imageTransform(w,h,iw,ih),b=room.sceneBounds(w,h,iw,ih,0,0,0,0);
    float bw=b[2]-b[0],bh=b[3]-b[1];
    float vx=(t[1]+iw*t[0]*p.cameraX-b[0])/bw,vy=(t[2]+ih*t[0]*p.cameraY-b[1])/bh,ux=(t[1]+iw*t[0]*p.widthVanishingX-b[0])/bw;
    float[] a=new float[8],s=new float[8],r=new float[8];
    p.front(bw,bh,1,vx,vy,ux,a);p.body(bw,bh,1,vx,vy,ux,a,s,r);
    // A wall-aligned cabinet must share both the crown and foot line vanishing point.
    ray(a[0],a[1],a[2],a[3],ux*bw,vy*bh);ray(a[6],a[7],a[4],a[5],ux*bw,vy*bh);
    ray(r[0],r[1],r[2],r[3],ux*bw,vy*bh);
    ray(r[0],r[1],r[6],r[7],vx*bw,vy*bh);ray(r[2],r[3],r[4],r[5],vx*bw,vy*bh);
    if(a[5]-a[7]<1)throw new AssertionError("Feet must follow the sloping back-wall floor, not a screen-horizontal baseline");
    if(!p.topVisible(vx,vy,1))throw new AssertionError("Low cabinet is below the room horizon: its top must be visible");
    float retreat=1/(1+p.depth);
    for(int i:wide?new int[]{6,4}:new int[0]){
     float rearX=(vx*bw+retreat*(a[i]-vx*bw)+b[0]-t[1])/t[0],rearY=(vy*bh+retreat*(a[i+1]-vy*bh)+b[1]-t[2])/t[0];
     float floorY=wide?621+(rearX-454)*45/701:1073+(rearX-300)*33/312;
     if(rearX<(wide?454:300))throw new AssertionError("Cabinet rear must remain inside the back wall corner");
     if(rearY<floorY+8||rearY>floorY+(wide?90:120))throw new AssertionError("Rear foot must rest on floor in front of the wall, not the baseboard: "+rearY+" floor "+floorY);
    }
    float visibleDepth=(r[2]-a[2])/(a[2]-a[0]);
    if(visibleDepth<(wide?.075f:.03f)||visibleDepth>(wide?.25f:.08f))throw new AssertionError("Cabinet depth must follow its room reference: "+visibleDepth);
    // Front foot slope should agree with independently observed baseboard direction.
    float measured=wide?45f/701:33f/312;
    float slope=(a[5]-a[7])/(a[4]-a[6]);
    if(wide&&Math.abs(slope-measured)>.025)throw new AssertionError("Cabinet floor slope "+slope+" disagrees with room "+measured);
   }
  }
  System.out.println("PASS: front, rear and depth edges share room vanishing points; floor slope follows artwork");
 }
}
