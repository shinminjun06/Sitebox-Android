package com.sitebox.app;

import java.util.Random;
import static com.sitebox.app.ModelMesh.p;
/** World-space neighborhood and a real opening matching the supplied GLB window. */
final class EnvironmentGeometry {
 static final float WINDOW_LEFT=-.9f,WINDOW_RIGHT=1.85f,WINDOW_BOTTOM=.97f,WINDOW_TOP=2.67f;
 static ModelMesh.Data wall(){ModelMesh.Builder m=new ModelMesh.Builder();float[] c={1,1,1};m.box(-2.25f,0,-2.355f,WINDOW_LEFT,2.92f,-2.265f,c);m.box(WINDOW_RIGHT,0,-2.355f,2.25f,2.92f,-2.265f,c);m.box(WINDOW_LEFT,0,-2.355f,WINDOW_RIGHT,WINDOW_BOTTOM,-2.265f,c);m.box(WINDOW_LEFT,WINDOW_TOP,-2.355f,WINDOW_RIGHT,2.92f,-2.265f,c);return m.build();}
 static final class Town {final ModelMesh.Data solid,windows,roofs,clouds;Town(ModelMesh.Data s,ModelMesh.Data w,ModelMesh.Data r,ModelMesh.Data c){solid=s;windows=w;roofs=r;clouds=c;}}
 static Town town(){
  ModelMesh.Builder solid=new ModelMesh.Builder(),windows=new ModelMesh.Builder(),roofs=new ModelMesh.Builder(),clouds=new ModelMesh.Builder();Random random=new Random(71025);
  solid.box(-70,-2.5f,-95,70,-2.3f,-6.1f,ModelMesh.rgb(0xff657061));solid.box(-1.8f,-2.29f,-90,1.8f,-2.25f,-6.1f,ModelMesh.rgb(0xff6c7070));
  int[] plaster={0xffd2c8ad,0xffc4baaa,0xffc9c5b8,0xffddd1b7,0xffaeb7b6};int[] tiles={0xff4f5e69,0xff58646d,0xff66676a,0xff745951};
  for(int row=0;row<4;row++)for(int side=0;side<2;side++)for(int offset=0;offset<2;offset++){
   float x=(side==0?-1:1)*(4.5f+offset*5.8f)+random.nextFloat(),z=-13-row*12-offset*2,w=3.7f+random.nextFloat(),d=4.4f+random.nextFloat(),height=2.5f+random.nextFloat()*.6f;
   house(solid,windows,roofs,x,z,w,d,-2.28f,height,ModelMesh.rgb(plaster[(row+offset+side)%plaster.length]),ModelMesh.rgb(tiles[(row*3+offset+side)%tiles.length]),random);
  }
  for(int i=0;i<13;i++){float x=(i%2==0?-1:1)*(2.8f+random.nextFloat()*10),z=-22f-i*4.8f;tree(solid,x,-2.28f,z,3.5f+random.nextFloat()*2,random);}
  // A few near-window branches have genuine parallax instead of photographed leaves.
  tree(solid,12f,-2.28f,-25f,5.2f,random);
  for(int i=0;i<5;i++){
   float z=-10-i*14,x=2.05f;float[] pole=ModelMesh.rgb(0xff77776e);solid.tube(p(x,-2.3f,z),p(x,4.2f,z),.055f,pole,8);solid.box(x-.6f,3.85f,z-.045f,x+.6f,3.95f,z+.045f,pole);
   if(i<4)for(float line:new float[]{-.43f,0,.43f})for(int seg=0;seg<8;seg++){float a=seg/8f,b=(seg+1)/8f;solid.tube(p(x+line,3.9f-.3f*(float)Math.sin(a*Math.PI),z-14*a),p(x+line,3.9f-.3f*(float)Math.sin(b*Math.PI),z-14*b),.008f,ModelMesh.rgb(0xff303a3e),4);}
  }
  for(int j=0;j<9;j++)for(int i=0;i<28;i++){
   float x=-140+i*10,z=-78-j*8;float[] a=hill(x,z),b=hill(x+10,z),c=hill(x+10,z-8),d=hill(x,z-8);float haze=.4f+j*.045f;solid.quad(a,b,c,d,new float[]{.14f+(.45f-.14f)*haze,.24f+(.60f-.24f)*haze,.20f+(.73f-.20f)*haze});
  }
  return new Town(solid.build(),windows.build(),roofs.build(),clouds.build());
 }
 private static float[] hill(float x,float z){return p(x,2.8f+(float)(2.5*Math.sin(x*.035+z*.018)+1.7*Math.sin(x*.071-z*.026)),z);}
 private static void house(ModelMesh.Builder m,ModelMesh.Builder windows,ModelMesh.Builder roofs,float x,float z,float w,float d,float y,float h,float[] plaster,float[] tile,Random random){
  float l=x-w/2,r=x+w/2,back=z-d/2,front=z+d/2,t=y+h,ridge=t+w*.29f;float[] timber=ModelMesh.rgb(0xff64594a),trim=ModelMesh.rgb(0xffa9a595);
  m.box(l,y,back,r,t,front,plaster);m.box(l-.08f,y,back-.08f,r+.08f,y+.17f,front+.08f,ModelMesh.rgb(0xff8c8a7d));
  for(float px:new float[]{l+.05f,r-.05f})m.box(px-.045f,y,front+.005f,px+.045f,t,front+.05f,timber);
  m.tri(p(l,t,front),p(r,t,front),p(x,ridge,front),plaster);m.tri(p(r,t,back),p(l,t,back),p(x,ridge,back),plaster);
  float shade=.60f+.8f*Math.max(tile[0],Math.max(tile[1],tile[2]));tile=new float[]{shade,shade,shade};
  float e=.26f;roofs.quad(p(l-e,t,back-e),p(l-e,t,front+e),p(x,ridge,front+e),p(x,ridge,back-e),tile);
  roofs.quad(p(x,ridge,back-e),p(x,ridge,front+e),p(r+e,t,front+e),p(r+e,t,back-e),tile);
  roofs.box(l-e,t-.09f,back-e,r+e,t,front+e,tile);
  // Raised tile courses and ridge caps break up the large roof planes.
  for(int side=-1;side<=1;side+=2)for(int course=1;course<9;course++){
   float a=course/9f,b=a+.012f,xa=x+side*(w/2+e)*a,xb=x+side*(w/2+e)*b,ya=ridge+(t-ridge)*a+.013f,yb=ridge+(t-ridge)*b+.013f;
   if(side<0)roofs.quad(p(xb,yb,back-e),p(xb,yb,front+e),p(xa,ya,front+e),p(xa,ya,back-e),new float[]{tile[0]*.7f,tile[1]*.7f,tile[2]*.7f});
   else roofs.quad(p(xa,ya,back-e),p(xa,ya,front+e),p(xb,yb,front+e),p(xb,yb,back-e),new float[]{tile[0]*.7f,tile[1]*.7f,tile[2]*.7f});
  }
  for(float rz=back-e;rz<front+e;rz+=.38f){float zz=Math.min(rz+.022f,front+e);float[] seam={tile[0]*.84f,tile[1]*.84f,tile[2]*.84f};
   roofs.quad(p(l-e,t+.008f,rz),p(l-e,t+.008f,zz),p(x,ridge+.008f,zz),p(x,ridge+.008f,rz),seam);
   roofs.quad(p(x,ridge+.008f,rz),p(x,ridge+.008f,zz),p(r+e,t+.008f,zz),p(r+e,t+.008f,rz),seam);}
  for(float rz=back-e;rz<front+e;rz+=.38f)roofs.tube(p(x,ridge+.035f,rz),p(x,ridge+.035f,Math.min(rz+.37f,front+e)),.065f,tile,8);
  for(int floor=0;floor<2;floor++)for(int window=0;window<3;window++){
   float wx=l+.42f+window*(w-.84f)/3,wy=y+.48f+floor*(h*.49f),ww=(w-.95f)/3,wh=h*.31f;
   m.box(wx-.055f,wy-.055f,front+.008f,wx+ww+.055f,wy+wh+.055f,front+.055f,timber);
   windows.quad(p(wx,wy,front+.057f),p(wx+ww,wy,front+.057f),p(wx+ww,wy+wh,front+.057f),p(wx,wy+wh,front+.057f),new float[]{.68f+random.nextFloat()*.32f,.75f,.8f});
   m.box(wx+ww*.48f,wy,front+.06f,wx+ww*.52f,wy+wh,front+.08f,trim);m.box(wx,wy+wh*.49f,front+.06f,wx+ww,wy+wh*.53f,front+.08f,trim);
  }
 }
 private static void tree(ModelMesh.Builder m,float x,float y,float z,float h,Random r){
  float[] bark=ModelMesh.rgb(0xff665443);m.tube(p(x,y,z),p(x+.10f,y+h*.8f,z),.09f,bark,9);
  for(int i=0;i<8;i++){double angle=i*2.399;float px=x+(float)Math.cos(angle)*h*.22f,pz=z+(float)Math.sin(angle)*h*.22f,py=y+h*(.55f+r.nextFloat()*.32f);m.tube(p(x,y+h*.42f,z),p(px,py,pz),.035f,bark,6);for(int leaf=0;leaf<6;leaf++){double az=leaf*2.399+i;float ox=(float)Math.cos(az)*h*.15f,oz=(float)Math.sin(az)*h*.15f,oy=(r.nextFloat()-.5f)*h*.18f;float rad=h*(.075f+r.nextFloat()*.035f);m.ellipsoid(px+ox,py+oy,pz+oz,rad,rad*.85f,rad,ModelMesh.rgb((i+leaf)%3==0?0xff6c8747:(i+leaf)%3==1?0xff819552:0xff547345),8,6);}}
 }
 static ModelMesh.Data sky(){return sky(false);}
 static ModelMesh.Data sky(boolean starPanorama){ModelMesh.Builder b=new ModelMesh.Builder();int slices=48,rings=24;for(int j=0;j<=rings;j++){double lat=Math.PI*j/rings;float y=(float)Math.cos(lat),sr=(float)Math.sin(lat);float t=Math.max(0,y);float[] c={.55f-.4f*t,.72f-.37f*t,.88f-.22f*t};for(int i=0;i<=slices;i++){double a=2*Math.PI*i/slices;float x=sr*(float)Math.cos(a),z=sr*(float)Math.sin(a);b.vertex(x*140,y*140,-35+z*140,-x,-y,-z,i/(float)slices+.5f,j/(float)rings*(starPanorama?.65f:1f),c);}}for(int j=0;j<rings;j++)for(int i=0;i<slices;i++){int a=j*(slices+1)+i,c=a+slices+1;if(j>0)b.triangle(a,c,a+1);if(j<rings-1)b.triangle(a+1,c,c+1);}return b.build();}
 static ModelMesh.Data glass(){ModelMesh.Builder b=new ModelMesh.Builder();b.quad(p(-.89f,.99f,-2.09f),p(1.81f,.99f,-2.09f),p(1.81f,2.64f,-2.09f),p(-.89f,2.64f,-2.09f),new float[]{1,1,1});return b.build();}
}
