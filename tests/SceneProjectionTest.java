package com.sitebox.app;
public class SceneProjectionTest {
 public static void main(String[] args){
  int checks=0;
  for(SceneTheme room:SceneTheme.ALL){
   SceneProjection pose=SceneProjection.find(room.id);
   for(float f:new float[]{0,.25f,.5f,1}){
    float[] p=new float[8];pose.front(320,416,f,p);
    for(int i=0;i<8;i++){if(p[i]<0||p[i]>(i%2==0?320:416))throw new AssertionError(room.id+" bounded");checks++;}
    for(int i=0;i<4;i++){int j=(i+1)%4,k=(i+2)%4;float cross=(p[j*2]-p[i*2])*(p[k*2+1]-p[j*2+1])-(p[j*2+1]-p[i*2+1])*(p[k*2]-p[j*2]);if(cross<=0)throw new AssertionError(room.id+" convex positive area");checks++;}
    float[] side=new float[8],roof=new float[8];pose.body(320,416,f,p,side,roof);
    for(float[] face:new float[][]{side,roof})for(int i=0;i<8;i++){if(face[i]<-.001f||face[i]>(i%2==0?320.001f:416.001f))throw new AssertionError(room.id+" body bounded");checks++;}
    if(Math.max(p[5],p[7])!=416)throw new AssertionError(room.id+" floor contact");checks++;
    if(f==0){float[] identity={0,0,320,0,320,416,0,416};for(int i=0;i<8;i++)if(p[i]!=identity[i])throw new AssertionError("identity");checks++;}
   }
  }
  if(SceneProjection.find("missing")!=SceneProjection.find("classroom"))throw new AssertionError("fallback");
  System.out.println("SceneProjection: "+checks+" checks passed");
 }
}
