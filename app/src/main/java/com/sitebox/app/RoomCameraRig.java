package com.sitebox.app;

/** One allocation-free camera shared by Filament, native books and hit testing. */
final class RoomCameraRig {
 final double[] eye=new double[3],target=new double[3];
 double tanY,aspect;
 private double width,height,rx,ry,rz,ux,uy,uz,fx,fy,fz;
 void update(float w,float h,float left,float top,float right,float bottom,float room){
  width=Math.max(1,w);height=Math.max(1,h);aspect=width/height;
  double editorZ=1.05,d=editorZ-RoomModelGeometry.FRONT,editorTan=(RoomModelGeometry.TOP-RoomModelGeometry.BOTTOM)/(2*d*Math.max(.01,(bottom-top)/height));
  double cx=(RoomModelGeometry.LEFT+RoomModelGeometry.RIGHT)/2,cy=(RoomModelGeometry.TOP+RoomModelGeometry.BOTTOM)/2;
  double ex=cx-((left+right)/width-1)*d*editorTan*aspect;
  double ey=cy+((top+bottom)/height-1)*d*editorTan;
  // Fixed horizontal room FOV in portrait; bounded vertical FOV in landscape.
  double roomTan=Math.max(.70,.54/aspect);
  tanY=editorTan+(roomTan-editorTan)*room;
  eye[0]=ex+(-.8-ex)*room;eye[1]=ey+(1.45-ey)*room;eye[2]=editorZ+(2.2-editorZ)*room;
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
 void lampBounds(float[] out){float[] p=new float[4];project(-.79f,1.43f,-1.75f,p,0);project(-.49f,1.1f,-1.4f,p,2);out[0]=Math.min(p[0],p[2]);out[1]=Math.min(p[1],p[3]);out[2]=Math.max(p[0],p[2]);out[3]=Math.max(p[1],p[3]);}
 double fov(){return Math.toDegrees(2*Math.atan(tanY));}
}
