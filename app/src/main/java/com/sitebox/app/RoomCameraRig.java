package com.sitebox.app;

/** One allocation-free camera shared by Filament, native books and hit testing. */
final class RoomCameraRig {
 final double[] eye=new double[3],target=new double[3];
 double tanY,aspect;
 private double width,height,rx,ry,rz,ux,uy,uz,fx,fy,fz;
 private final float[] lampPoint=new float[2];
 void update(float w,float h,float left,float top,float right,float bottom,float room){
  width=Math.max(1,w);height=Math.max(1,h);aspect=width/height;
  double editorZ=1.05,d=editorZ-RoomModelGeometry.FRONT,editorTan=(RoomModelGeometry.TOP-RoomModelGeometry.BOTTOM)/(2*d*Math.max(.01,(bottom-top)/height));
  double cx=(RoomModelGeometry.LEFT+RoomModelGeometry.RIGHT)/2,cy=(RoomModelGeometry.TOP+RoomModelGeometry.BOTTOM)/2;
  double ex=cx-((left+right)/width-1)*d*editorTan*aspect;
  double ey=cy+((top+bottom)/height-1)*d*editorTan;
  // Pull back through the open front in portrait, with a narrower lens. Every
  // view ray still enters the authored room, and the foreground table remains
  // visible. Wide screens use the interior camera to avoid exposing its edges.
  double portrait=Math.max(0,Math.min(1,(1-aspect)/.25));
  double roomTan=.70+(Math.min(.85,.38/aspect)-.70)*portrait;
  double roomZ=2.2+1.7*portrait;
  tanY=editorTan+(roomTan-editorTan)*room;
  eye[0]=ex+(-.8-ex)*room;eye[1]=ey+(1.45-ey)*room;eye[2]=editorZ+(roomZ-editorZ)*room;
  target[0]=ex+(-.7-ex)*room;target[1]=ey+(1.32-ey)*room;target[2]=(editorZ-1)+(-.9-(editorZ-1))*room;
  fx=target[0]-eye[0];fy=target[1]-eye[1];fz=target[2]-eye[2];double n=Math.sqrt(fx*fx+fy*fy+fz*fz);fx/=n;fy/=n;fz/=n;
  n=Math.sqrt(fz*fz+fx*fx);rx=-fz/n;ry=0;rz=fx/n;
  ux=ry*fz-rz*fy;uy=rz*fx-rx*fz;uz=rx*fy-ry*fx;
 }
 void project(double x,double y,double z,float[] out,int offset){
  x-=eye[0];y-=eye[1];z-=eye[2];double depth=x*fx+y*fy+z*fz;
  out[offset]=(float)(width*.5*(1+(x*rx+y*ry+z*rz)/(depth*tanY*aspect)));
  out[offset+1]=(float)(height*.5*(1-(x*ux+y*uy+z*uz)/(depth*tanY)));
 }
 void shelfQuad(float[] out){
  project(RoomModelGeometry.LEFT,RoomModelGeometry.TOP,RoomModelGeometry.FRONT,out,0);
  project(RoomModelGeometry.RIGHT,RoomModelGeometry.TOP,RoomModelGeometry.FRONT,out,2);
  project(RoomModelGeometry.RIGHT,RoomModelGeometry.BOTTOM,RoomModelGeometry.FRONT,out,4);
  project(RoomModelGeometry.LEFT,RoomModelGeometry.BOTTOM,RoomModelGeometry.FRONT,out,6);
 }
 void lampBounds(float[] out){
  out[0]=out[1]=Float.MAX_VALUE;out[2]=out[3]=-Float.MAX_VALUE;
  for(int i=0;i<8;i++){project((i&1)==0?-.55:-.17,(i&2)==0?1.32:1.50,(i&4)==0?-1.35:-.97,lampPoint,0);out[0]=Math.min(out[0],lampPoint[0]);out[1]=Math.min(out[1],lampPoint[1]);out[2]=Math.max(out[2],lampPoint[0]);out[3]=Math.max(out[3],lampPoint[1]);}
 }
 double fov(){return Math.toDegrees(2*Math.atan(tanY));}
}
