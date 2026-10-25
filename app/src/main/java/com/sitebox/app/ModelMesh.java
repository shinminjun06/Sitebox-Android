package com.sitebox.app;

import java.util.Arrays;

/** Engine-independent indexed mesh; position, tangent quaternion, two UVs, linear RGBA. */
final class ModelMesh {
 static final int STRIDE=15;
 static final class Data {
  final float[] vertices,min={Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE},max={-Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE};final int[] indices;
  Data(float[] v,int[] i){vertices=v;indices=i;for(int p=0;p<v.length;p+=STRIDE)for(int a=0;a<3;a++){min[a]=Math.min(min[a],v[p+a]);max[a]=Math.max(max[a],v[p+a]);}if(v.length==0)Arrays.fill(min,0);if(v.length==0)Arrays.fill(max,0);}
 }
 static float[] rgb(int color){return new float[]{linear((color>>16)&255),linear((color>>8)&255),linear(color&255)};}
 private static float linear(int value){float v=value/255f;return v<=.04045f?v/12.92f:(float)Math.pow((v+.055f)/1.055f,2.4);}
 static float[] normal(float[] v,int offset){float x=v[offset+3],y=v[offset+4],z=v[offset+5],w=v[offset+6];return new float[]{2*(x*z+y*w),2*(y*z-x*w),1-2*(x*x+y*y)};}
 static final class Builder {
  float[] vertices=new float[2048];int[] indices=new int[1024];int vc,ic;
  int vertex(float x,float y,float z,float nx,float ny,float nz,float u,float v,float[] color){
   if((vc+1)*STRIDE>vertices.length)vertices=Arrays.copyOf(vertices,vertices.length*2);
   float n=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);nx/=n;ny/=n;nz/=n;
   float qx=-ny,qy=nx,qz=0,qw=1+nz;
   if(qw<.00001f){qx=0;qy=1;qz=0;qw=.0000001f;}
   float q=(float)Math.sqrt(qx*qx+qy*qy+qz*qz+qw*qw);int p=vc*STRIDE;
   float[] data={x,y,z,qx/q,qy/q,qz/q,qw/q,u,v,u,v,color[0],color[1],color[2],color.length>3?color[3]:1};System.arraycopy(data,0,vertices,p,STRIDE);return vc++;
  }
  void triangle(int a,int b,int c){if(ic+3>indices.length)indices=Arrays.copyOf(indices,indices.length*2);indices[ic++]=a;indices[ic++]=b;indices[ic++]=c;}
  void tri(float[] a,float[] b,float[] c,float[] color){float x=b[0]-a[0],y=b[1]-a[1],z=b[2]-a[2],u=c[0]-a[0],v=c[1]-a[1],w=c[2]-a[2];float nx=y*w-z*v,ny=z*u-x*w,nz=x*v-y*u;int i=vertex(a[0],a[1],a[2],nx,ny,nz,0,0,color),j=vertex(b[0],b[1],b[2],nx,ny,nz,1,0,color),k=vertex(c[0],c[1],c[2],nx,ny,nz,.5f,1,color);triangle(i,j,k);}
  void quad(float[] a,float[] b,float[] c,float[] d,float[] color){float x=b[0]-a[0],y=b[1]-a[1],z=b[2]-a[2],u=c[0]-a[0],v=c[1]-a[1],w=c[2]-a[2];float nx=y*w-z*v,ny=z*u-x*w,nz=x*v-y*u;int i=vertex(a[0],a[1],a[2],nx,ny,nz,0,1,color),j=vertex(b[0],b[1],b[2],nx,ny,nz,1,1,color),k=vertex(c[0],c[1],c[2],nx,ny,nz,1,0,color),l=vertex(d[0],d[1],d[2],nx,ny,nz,0,0,color);triangle(i,j,k);triangle(i,k,l);}
  void box(float l,float b,float back,float r,float t,float front,float[] c){
   quad(p(l,b,front),p(r,b,front),p(r,t,front),p(l,t,front),c);quad(p(r,b,back),p(l,b,back),p(l,t,back),p(r,t,back),c);
   quad(p(r,b,front),p(r,b,back),p(r,t,back),p(r,t,front),c);quad(p(l,b,back),p(l,b,front),p(l,t,front),p(l,t,back),c);
   quad(p(l,t,front),p(r,t,front),p(r,t,back),p(l,t,back),c);quad(p(l,b,back),p(r,b,back),p(r,b,front),p(l,b,front),c);
  }
  void ellipsoid(float cx,float cy,float cz,float rx,float ry,float rz,float[] color,int slices,int rings){
   int start=vc;
   for(int j=0;j<=rings;j++){double lat=Math.PI*j/rings;float sy=(float)Math.cos(lat),sr=(float)Math.sin(lat);for(int i=0;i<=slices;i++){double angle=2*Math.PI*i/slices;float sx=sr*(float)Math.cos(angle),sz=sr*(float)Math.sin(angle);vertex(cx+rx*sx,cy+ry*sy,cz+rz*sz,sx/rx,sy/ry,sz/rz,i/(float)slices,j/(float)rings,color);}}
   for(int j=0;j<rings;j++)for(int i=0;i<slices;i++){int a=start+j*(slices+1)+i,b=a+slices+1;if(j>0)triangle(a,a+1,b);if(j<rings-1)triangle(a+1,b+1,b);}
  }
  void tube(float[] a,float[] b,float radius,float[] color,int sides){
   float dx=b[0]-a[0],dy=b[1]-a[1],dz=b[2]-a[2],len=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);if(len<.000001f)return;dx/=len;dy/=len;dz/=len;
   float ux=Math.abs(dy)>.9f?1:-dz,uy=0,uz=Math.abs(dy)>.9f?0:dx,un=(float)Math.sqrt(ux*ux+uz*uz);ux/=un;uz/=un;
   float vx=dy*uz,vy=dz*ux-dx*uz,vz=-dy*ux;
   float[][] ringA=new float[sides][],ringB=new float[sides][];
   for(int i=0;i<sides;i++){float co=(float)Math.cos(i*2*Math.PI/sides),si=(float)Math.sin(i*2*Math.PI/sides),x=(ux*co+vx*si)*radius,y=(uy*co+vy*si)*radius,z=(uz*co+vz*si)*radius;ringA[i]=p(a[0]+x,a[1]+y,a[2]+z);ringB[i]=p(b[0]+x,b[1]+y,b[2]+z);}
   for(int i=0;i<sides;i++){int n=(i+1)%sides;quad(ringA[i],ringA[n],ringB[n],ringB[i],color);tri(a,ringA[n],ringA[i],color);tri(b,ringB[i],ringB[n],color);}
  }
  Data build(){return new Data(Arrays.copyOf(vertices,vc*STRIDE),Arrays.copyOf(indices,ic));}
 }
 static float[] p(float x,float y,float z){return new float[]{x,y,z};}
}
