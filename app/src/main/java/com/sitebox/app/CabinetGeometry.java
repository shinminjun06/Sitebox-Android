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
}
