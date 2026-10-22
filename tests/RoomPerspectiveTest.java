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
   // Independent intersections of the photographed beam/baseboard and side floor.
   // A self-consistent cabinet can still be at the wrong angle to the room.
   float measuredWidthVP=wide?-3713.633f:-3934.678f;
   float measuredHorizon=wide?353.463f:625.101f;
   float measuredDepthVP=wide?1417.134f:2859.421f;
   if(Math.abs(p.widthVanishingX*iw-measuredWidthVP)>1||Math.abs(p.cameraY*ih-measuredHorizon)>1||Math.abs(p.cameraX*iw-measuredDepthVP)>1)
    throw new AssertionError("Cabinet camera must match the measured room walls, not an arbitrary front slope");
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
    float[] feet=new float[8];p.footprint(bw,bh,feet);
    for(int i:new int[]{0,2}){
     float rearX=(feet[i]+b[0]-t[1])/t[0],rearY=(feet[i+1]+b[1]-t[2])/t[0];
     float floorY=wide?621+(rearX-454)*45/701:1073+(rearX-300)*33/312;
     if(rearX<(wide?454:300))throw new AssertionError("Cabinet rear must remain inside the back wall corner");
     if(rearY<floorY+8||rearY>floorY+(wide?90:120))throw new AssertionError("Rear foot must rest on floor in front of the wall, not the baseboard: "+rearY+" floor "+floorY);
    }
    float visibleDepth=(r[2]-a[2])/(a[2]-a[0]);
    if(visibleDepth<.10f||visibleDepth>.30f)throw new AssertionError("Cabinet needs a physically plausible enclosed side: "+visibleDepth);
    // Front foot slope should agree with independently observed baseboard direction.
    float measured=wide?45f/701:33f/312;
    float slope=(a[5]-a[7])/(a[4]-a[6]);
    if(Math.abs(slope-measured)>.025)throw new AssertionError("Cabinet floor slope "+slope+" disagrees with room "+measured);
    for(float shelfY:new float[]{.03f,.31f,.61f,.91f}){
     float[] line=new float[6];p.point(bw,bh,1,.045f,shelfY,0,line,0);p.point(bw,bh,1,.955f,shelfY,0,line,2);p.point(bw,bh,1,.045f,shelfY,1,line,4);
     ray(line[0],line[1],line[2],line[3],ux*bw,vy*bh);ray(line[0],line[1],line[4],line[5],vx*bw,vy*bh);
    }
   }
  }
  System.out.println("PASS: front, rear and depth edges share room vanishing points; floor slope follows artwork");
 }
}
