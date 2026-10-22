package com.sitebox.app;
public class SceneProjectionTest {
 static void convex(float[] q){float sign=0;for(int i=0;i<4;i++){int j=(i+1)%4,k=(i+2)%4;float cross=(q[2*j]-q[2*i])*(q[2*k+1]-q[2*j+1])-(q[2*k]-q[2*j])*(q[2*j+1]-q[2*i+1]);if(Math.abs(cross)<.0001f||sign!=0&&Math.signum(cross)!=sign)throw new AssertionError("nonconvex visible face");sign=Math.signum(cross);}}
 static void near(float a,float b){if(Math.abs(a-b)>.01f)throw new AssertionError(a+" != "+b);}
 public static void main(String[] args){int checks=0;
 SceneTheme room=SceneTheme.find("study");
 for(float[] viewport:new float[][]{{1080,2400},{2400,1080},{800,900},{320,480},{480,320},{1080,1080}}){
 boolean wide=viewport[0]>viewport[1];float iw=wide?1672:854,ih=wide?941:1842;SceneProjection p=SceneProjection.find(room.id,wide);
 float[] bounds=room.sceneBounds(viewport[0],viewport[1],iw,ih,0,30,0,30),t=room.imageTransform(viewport[0],viewport[1],iw,ih);
 float vx=(t[1]+iw*t[0]*p.cameraX-bounds[0])/(bounds[2]-bounds[0]),vy=(t[2]+ih*t[0]*p.cameraY-bounds[1])/(bounds[3]-bounds[1]),ux=(t[1]+iw*t[0]*p.widthVanishingX-bounds[0])/(bounds[2]-bounds[0]);
 for(float f:new float[]{0,.25f,.5f,1}){float[] a=new float[8],s=new float[8],r=new float[8];p.front(320,416,f,vx,vy,ux,a);p.body(320,416,f,vx,vy,ux,a,s,r);
 for(float[] face:new float[][]{a,s,r})for(int i=0;i<8;i++){if(!Float.isFinite(face[i])||face[i]<-.01f||face[i]>(i%2==0?320.01f:416.01f))throw new AssertionError("face outside view at fraction "+f+" viewport "+viewport[0]+"x"+viewport[1]+" coordinate "+i+"="+face[i]+" front "+java.util.Arrays.toString(a)+" roof "+java.util.Arrays.toString(r));checks++;}
 convex(a);near(a[0],a[6]);near(a[2],a[4]);near(Math.max(a[5],a[7]),416);near(r[4],a[2]);near(r[5],a[3]);near(r[6],a[0]);near(r[7],a[1]);
 if(f>0){
  if(p.leftVisible(vx,vy,f)||p.rightVisible(vx,vy,f))convex(s);if(p.topVisible(vx,vy,f))convex(r);
  RoomPerspectiveTest.ray(r[0],r[1],r[6],r[7],vx*320,vy*416);RoomPerspectiveTest.ray(r[2],r[3],r[4],r[5],vx*320,vy*416);
  if(f==1){RoomPerspectiveTest.ray(a[0],a[1],a[2],a[3],ux*320,vy*416);RoomPerspectiveTest.ray(r[0],r[1],r[2],r[3],ux*320,vy*416);}
 }
 near(SceneProjection.contact(a[6],a[4],.05f,320)*1.04f*320-.02f*320,a[6]+.05f*(a[4]-a[6]));
 if(f==0){float[] identity={0,0,320,0,320,416,0,416};for(int i=0;i<8;i++)near(a[i],identity[i]);}
 }}
 System.out.println("SceneProjection: "+checks+" bounded face checks plus two-point perspective and identity checks passed");}
}
