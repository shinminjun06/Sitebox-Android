package com.sitebox.app;
public class SceneProjectionTest {
 public static void main(String[] args){
  int checks=0;
  for(SceneTheme room:SceneTheme.ALL){
   SceneProjection pose=SceneProjection.find(room.id);
   if(pose.top<pose.rise)throw new AssertionError(room.id+" camera-above profile constraints");checks++;
   for(float f:new float[]{0,.25f,.5f,1}){
    float[] p=new float[8];pose.front(320,416,f,p);
    for(int i=0;i<8;i++){if(p[i]<0||p[i]>(i%2==0?320:416))throw new AssertionError(room.id+" bounded");checks++;}
    for(int i=0;i<4;i++){int j=(i+1)%4,k=(i+2)%4;float cross=(p[j*2]-p[i*2])*(p[k*2+1]-p[j*2+1])-(p[j*2+1]-p[i*2+1])*(p[k*2]-p[j*2]);if(cross<=0)throw new AssertionError(room.id+" convex positive area");checks++;}
    float[] side=new float[8],roof=new float[8];pose.body(320,416,f,p,side,roof);
    for(float[] face:new float[][]{side,roof})for(int i=0;i<8;i++){if(face[i]<-.001f||face[i]>(i%2==0?320.001f:416.001f))throw new AssertionError(room.id+" body bounded");checks++;}
    if(f>0){
     int near=pose.depth>0?0:2,far=pose.depth>0?2:0,nearBottom=pose.depth>0?6:4,farBottom=pose.depth>0?4:6;
     if(!(p[far+1]<p[near+1]&&p[farBottom+1]<p[nearBottom+1]&&p[farBottom+1]-p[far+1]<p[nearBottom+1]-p[near+1]))throw new AssertionError(room.id+" receding front edge rises and shortens above floor");checks++;
     if(!(side[7]-side[1]<p[nearBottom+1]-p[near+1]))throw new AssertionError(room.id+" receding side edge shortens");checks++;
     for(float[] face:new float[][]{side,roof}){float sign=0;for(int i=0;i<4;i++){int j=(i+1)%4,k=(i+2)%4;float cross=(face[j*2]-face[i*2])*(face[k*2+1]-face[j*2+1])-(face[j*2+1]-face[i*2+1])*(face[k*2]-face[j*2]);if(cross==0||(sign!=0&&Math.signum(cross)!=sign))throw new AssertionError(room.id+" convex physical face");sign=Math.signum(cross);checks++;}}
    }
    if(Math.max(p[5],p[7])!=416)throw new AssertionError(room.id+" floor contact");checks++;
    if(f==0){float[] identity={0,0,320,0,320,416,0,416};for(int i=0;i<8;i++)if(p[i]!=identity[i])throw new AssertionError("identity");checks++;}
   }
  }
  if(SceneProjection.find("missing")!=SceneProjection.find("classroom"))throw new AssertionError("fallback");
  System.out.println("SceneProjection: "+checks+" checks passed");
 }
}
