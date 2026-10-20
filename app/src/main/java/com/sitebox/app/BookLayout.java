package com.sitebox.app;

/** Geometry shared by actual book drawing and the scene transition. */
final class BookLayout {
 static final int CAPACITY=7;
 static final float SOURCE_SPINE_FRACTION=.76f;
 static final float TITLE_TOP=.31f,TITLE_BOTTOM=.72f;
 private BookLayout(){}
 static float spineRight(float left,float right){return right-(right-left)*.035f;}
 // A portrait bitmap crops the floor out in near-square foldable/multiwindow views.
 static boolean wideScene(float width,float height){return width>=height*.8f;}
 static float[] sceneBounds(float width,float height,float imageWidth,float imageHeight,float insetLeft,float insetTop,float insetRight,float insetBottom){
  float padding=Math.min(width,height)*.025f;
  float availableWidth=Math.max(1,width-insetLeft-insetRight-2*padding);
  float availableHeight=Math.max(1,height-insetTop-insetBottom-2*padding);
  float imageScale=Math.max(width/imageWidth,height/imageHeight);
  float xOffset=(width-imageWidth*imageScale)*.5f,yOffset=(height-imageHeight*imageScale)*.5f;
  boolean landscape=imageWidth>imageHeight;
  // Match the actual sill and floor in each orientation's independently composed room.
  float h=Math.min(imageHeight*imageScale*(landscape?.38f:.31f),Math.min(availableHeight*.62f,availableWidth*1.3f*.70f));
  float w=h/1.3f;
  float center=xOffset+imageWidth*imageScale*.52f;
  float left=Math.max(insetLeft+padding,Math.min(center-w*.5f,width-insetRight-padding-w));
  float bottom=Math.max(insetTop+padding+h,Math.min(yOffset+imageHeight*imageScale*(landscape?.84f:.80f),height-insetBottom-padding));
  return new float[]{left,bottom-h,left+w,bottom};
 }
}
