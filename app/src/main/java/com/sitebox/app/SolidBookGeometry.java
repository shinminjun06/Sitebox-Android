package com.sitebox.app;
import java.util.List;
/** Closed hardcover volumes with a curved, textured spine and recessed page block. */
final class SolidBookGeometry {
 static final class Book {
  final float left,bottom,right,top,front,depth;final float[] uv,color;
  Book(float l,float b,float r,float t,float f,float d,float[] uv,float[] color){left=l;bottom=b;right=r;top=t;front=f;depth=d;this.uv=uv;this.color=color;}
 }
 static final class Result {final ModelMesh.Data body,spines;final int books;Result(ModelMesh.Data b,ModelMesh.Data s,int n){body=b;spines=s;books=n;}}
 static Result build(List<Book> books){ModelMesh.Builder body=new ModelMesh.Builder(),spines=new ModelMesh.Builder();for(Book b:books)add(b,body,spines);return new Result(body.build(),spines.build(),books.size());}
 private static void add(Book b,ModelMesh.Builder body,ModelMesh.Builder spine){
  float width=b.right-b.left,cover=Math.min(.0018f,width*.075f),back=b.front-b.depth,bind=b.front-.014f;
  body.box(b.left,b.bottom,back,b.left+cover,b.top,bind,b.color);body.box(b.right-cover,b.bottom,back,b.right,b.top,bind,b.color);
  float paperBottom=b.bottom+.004f,paperTop=b.top-.004f;float[] ivory=ModelMesh.rgb(0xffe5dbc1),rule=ModelMesh.rgb(0xffc8bea8);
  body.box(b.left+cover,paperBottom,back+.009f,b.right-cover,paperTop,bind-.003f,ivory);
  // Groups of page edges are shallow closed ribs, not an exposed photograph.
  for(int i=1;i<11;i++){float x=b.left+cover+(width-2*cover)*i/11f;body.box(x,paperTop,back+.009f,x+.00018f,paperTop+.00012f,bind-.003f,rule);}
  int segments=8;float[][] profile=new float[segments+3][];
  for(int i=0;i<=segments;i++){float u=i/(float)segments;profile[i]=ModelMesh.p(b.left+width*u,0,b.front-.008f*(2*u-1)*(2*u-1));}
  profile[segments+1]=ModelMesh.p(b.right,0,bind);profile[segments+2]=ModelMesh.p(b.left,0,bind);
  float[] white={1,1,1};
  for(int i=0;i<segments;i++){
   float u=i/(float)segments,v=(i+1)/(float)segments;
   float nx=.032f*(2*u-1)/width,nn=(float)Math.sqrt(1+nx*nx),mx=.032f*(2*v-1)/width,mn=(float)Math.sqrt(1+mx*mx);
   int a=spine.vertex(profile[i][0],b.bottom,profile[i][2],nx/nn,0,1/nn,b.uv[0]+(b.uv[2]-b.uv[0])*u,b.uv[3],white);
   int c=spine.vertex(profile[i+1][0],b.top,profile[i+1][2],mx/mn,0,1/mn,b.uv[0]+(b.uv[2]-b.uv[0])*v,b.uv[1],white);
   int d=spine.vertex(profile[i][0],b.top,profile[i][2],nx/nn,0,1/nn,b.uv[0]+(b.uv[2]-b.uv[0])*u,b.uv[1],white);
   int e=spine.vertex(profile[i+1][0],b.bottom,profile[i+1][2],mx/mn,0,1/mn,b.uv[0]+(b.uv[2]-b.uv[0])*v,b.uv[3],white);spine.triangle(a,e,c);spine.triangle(a,c,d);
  }
  float cx=(b.left+b.right)/2,cz=(bind+b.front)/2;
  for(int i=0;i<profile.length;i++){
   float[] a=profile[i],c=profile[(i+1)%profile.length];
   body.tri(ModelMesh.p(cx,b.top,cz),ModelMesh.p(a[0],b.top,a[2]),ModelMesh.p(c[0],b.top,c[2]),b.color);
   body.tri(ModelMesh.p(cx,b.bottom,cz),ModelMesh.p(c[0],b.bottom,c[2]),ModelMesh.p(a[0],b.bottom,a[2]),b.color);
   if(i>=segments)body.quad(ModelMesh.p(a[0],b.bottom,a[2]),ModelMesh.p(c[0],b.bottom,c[2]),ModelMesh.p(c[0],b.top,c[2]),ModelMesh.p(a[0],b.top,a[2]),b.color);
  }
 }
}
