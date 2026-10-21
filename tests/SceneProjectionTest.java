package com.sitebox.app;
public class SceneProjectionTest {
 static void convex(float[] q){float sign=0;for(int i=0;i<4;i++){int j=(i+1)%4,k=(i+2)%4;float cross=(q[2*j]-q[2*i])*(q[2*k+1]-q[2*j+1])-(q[2*j+1]-q[2*i+1])*(q[2*k]-q[2*j]);if(Math.abs(cross)<.0001f||sign!=0&&Math.signum(cross)!=sign)throw new AssertionError("nonconvex visible face");sign=Math.signum(cross);}}
 static void near(float a,float b){if(Math.abs(a-b)>.001f)throw new AssertionError(a+" != "+b);}
 public static void main(String[] args){int checks=0;
 for(SceneTheme room:SceneTheme.ALL){SceneProjection p=SceneProjection.find(room.id);
 for(float[] screen:new float[][]{{1080,2400,1024,1536},{2400,1080,1536,1024},{800,900,1024,1536}}){
 float[] bounds=room.sceneBounds(screen[0],screen[1],screen[2],screen[3],0,30,0,30),t=room.imageTransform(screen[0],screen[1],screen[2],screen[3]);
 float vx=(t[1]+screen[2]*t[0]*p.cameraX-bounds[0])/(bounds[2]-bounds[0]),vy=(t[2]+screen[3]*t[0]*p.cameraY-bounds[1])/(bounds[3]-bounds[1]);
 for(float f:new float[]{0,.25f,.5f,1}){float[] a=new float[8],s=new float[8],r=new float[8];p.front(320,416,f,vx,vy,a);p.body(320,416,f,vx,vy,a,s,r);
 for(float[] face:new float[][]{a,s,r})for(int i=0;i<8;i++){if(!Float.isFinite(face[i])||face[i]<-.001f||face[i]>(i%2==0?320.001f:416.001f))throw new AssertionError(room.id+" bounded");checks++;}
 convex(a);if(f>0){if(p.leftVisible(vx,vy,f)||p.rightVisible(vx,vy,f))convex(s);if(p.topVisible(vx,vy,f))convex(r);}
 near((a[5]-a[1])/(a[2]-a[0]),416f/320f);near(a[1],a[3]);near(a[5],416);near(a[7],416);near(r[4],a[2]);near(r[7],a[1]);
 if(f>0)near(s[7]-s[1],(416-a[1])*.94f/(1+p.depth*f));
 float k=1/(1+p.depth*f);near(r[0],vx*320+k*(a[0]-vx*320));near(r[1],vy*416+k*(a[1]-vy*416));near(r[2],vx*320+k*(a[2]-vx*320));
 for(int corner=0;corner<2;corner++){float fx=a[corner*2],fy=a[corner*2+1],rx=r[corner*2],ry=r[corner*2+1];near(((rx-vx*320)*(fy-vy*416)-(ry-vy*416)*(fx-vx*320))/(float)Math.hypot(fx-vx*320,fy-vy*416),0);}
 near(SceneProjection.contact(a[0],a[2],.05f,320)*1.04f*320-.02f*320,a[0]+.05f*(a[2]-a[0]));
 if(f==0){float[] identity={0,0,320,0,320,416,0,416};for(int i=0;i<8;i++)near(a[i],identity[i]);}
 }}
 if(p.leftVisible(.5f)||p.rightVisible(.5f)||!p.leftVisible(-.1f)||!p.rightVisible(1.1f)||p.topVisible(.1f)||!p.topVisible(-.1f))throw new AssertionError("visibility");
 }
 SceneProjection edge=SceneProjection.ALL[0];float[] e=new float[8],es=new float[8],er=new float[8];edge.front(320,416,1,-2,.01f,e);edge.body(320,416,1,-2,.01f,e,es,er);if(!edge.topVisible(-2,.01f,1)||!(e[1]>.01f*416))throw new AssertionError("inset roof compares actual front");convex(er);near(er[1],.01f*416+(e[1]-.01f*416)/(1+edge.depth));
 System.out.println("SceneProjection: "+checks+" checks passed");}
}
