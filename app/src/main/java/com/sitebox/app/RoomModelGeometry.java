package com.sitebox.app;

/** Source GLB measurements in converted Y-up metres. R1 anchors in GLB are the bottom row. */
final class RoomModelGeometry {
 static final String ASSET="models/student_room.glb";
 static final float LEFT=-2.25f,RIGHT=-1.11f,TOP=2.15f,BOTTOM=-.05f,FRONT=-.774f;
 static final float ASPECT=(TOP-BOTTOM)/(RIGHT-LEFT);
 private static final float[] L={-2.165f,-1.817f,-1.477f},R={-1.883f,-1.543f,-1.195f};
 private static final float[] T={2.055f,1.36f,.67f},B={1.445f,.755f,.065f};
 static float[] finishTint(int index){return index==1?new float[]{.60f,.56f,.55f}:index==2?new float[]{1.5f,1.8f,2f}:new float[]{1,1,1};}
 static int editorWidth(int width,int height,float density){return Math.max(Math.round(96*density),Math.round(Math.min(width*.87f,Math.max(180*density,height-100*density)/ASPECT)));}
 static float[] cell(int i){
  if(i<0||i>8)throw new IllegalArgumentException("cell");int c=i%3,r=i/3;
  return new float[]{(L[c]-LEFT)/(RIGHT-LEFT),(TOP-T[r])/(TOP-BOTTOM),(R[c]-LEFT)/(RIGHT-LEFT),(TOP-B[r])/(TOP-BOTTOM)};
 }
 static int hit(float x,float y){for(int i=0;i<9;i++){float[] r=cell(i);if(x>=r[0]&&x<r[2]&&y>=r[1]&&y<r[3])return i;}return -1;}
}
