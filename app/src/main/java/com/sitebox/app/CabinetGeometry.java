package com.sitebox.app;

/** Measured front-sprite openings; shared by native children and projected hits. */
final class CabinetGeometry {
 static final float ASPECT=1.30f;
 private static final float[] X={.066f,.368f,.670f};
 private static final float[] Y={.042f,.323f,.617f};
 private CabinetGeometry(){}
 static float[] cell(int index){
  if(index<0||index>=9)throw new IllegalArgumentException("cell");
  return new float[]{X[index%3],Y[index/3],X[index%3]+.264f,Y[index/3]+.258f};
 }
 static int hit(float x,float y){
  for(int i=0;i<9;i++){float[] b=cell(i);if(x>=b[0]&&x<b[2]&&y>=b[1]&&y<b[3])return i;}
  return -1;
 }
 // Openings measured in the rectified photo; the near column is partly off-camera.
 private static final float[] PHOTO_LEFT={.015f,.195f,.600f},PHOTO_RIGHT={.160f,.558f,.966f};
 private static final float[] PHOTO_TOP={.022f,.345f,.646f},PHOTO_BASE={.310f,.590f,.925f};
 static float[] photoCell(int index){if(index<0||index>=9)throw new IllegalArgumentException("cell");int col=index%3,row=index/3;return new float[]{PHOTO_LEFT[col],PHOTO_TOP[row],PHOTO_RIGHT[col],PHOTO_BASE[row]+(col-1)*.006f};}
}
