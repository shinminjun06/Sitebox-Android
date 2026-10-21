package com.sitebox.app;

/** Stable persisted room IDs and source-image anchors, independent of book/cabinet finishes. */
final class SceneTheme {
 static final String PREFERENCE_KEY="backgroundTheme";
 static final SceneTheme[] ALL={
  new SceneTheme("classroom","햇살 교실",.62f,.69f,.23f),
  new SceneTheme("library","고전 도서관",0.34f,0.69f,0.18f),
  new SceneTheme("bedroom","나만의 방",0.65f,0.62f,0.20f),
  new SceneTheme("greenhouse","유리 온실",0.62f,0.74f,0.17f),
  new SceneTheme("cafe","북카페",0.68f,0.69f,0.22f),
  new SceneTheme("tatami","일본식 다다미방",0.42f,0.68f,0.21f),
  new SceneTheme("attic","다락방",0.68f,0.58f,0.19f),
  new SceneTheme("lab","미래 연구실",0.36f,0.67f,0.21f),
  new SceneTheme("wizard","마법사의 서재",0.36f,0.68f,0.20f),
  new SceneTheme("seaside","바닷가 별장",0.66f,0.66f,0.15f),
  new SceneTheme("train","기차 객실",0.68f,0.74f,0.23f)
 };
 final String id,name;
 final float centerX,feet,height;
 private SceneTheme(String id,String name,float centerX,float feet,float height){this.id=id;this.name=name;this.centerX=centerX;this.feet=feet;this.height=height;}
 static SceneTheme find(String id){for(SceneTheme scene:ALL)if(scene.id.equals(id))return scene;return ALL[0];}
 String assetPath(boolean wide){return "scenes/"+id+(id.equals("classroom")?(wide?"_landscape":"_portrait"):"")+".webp";}
 private static float clamp(float value,float min,float max){return Math.max(min,Math.min(max,value));}
 /** Scale, x translation, y translation used by both ImageView and furniture geometry. */
 float[] imageTransform(float width,float viewportHeight,float imageWidth,float imageHeight){
  float scale=Math.max(width/imageWidth,viewportHeight/imageHeight);
  if(id.equals("classroom"))return new float[]{scale,(width-imageWidth*scale)*.5f,(viewportHeight-imageHeight*scale)*.5f};
  return new float[]{scale,clamp(width*.5f-imageWidth*scale*centerX,width-imageWidth*scale,0),clamp(viewportHeight*.84f-imageHeight*scale*feet,viewportHeight-imageHeight*scale,0)};
 }
 float[] sceneBounds(float width,float viewportHeight,float imageWidth,float imageHeight,float leftInset,float topInset,float rightInset,float bottomInset){
  if(id.equals("classroom"))return BookLayout.sceneBounds(width,viewportHeight,imageWidth,imageHeight,leftInset,topInset,rightInset,bottomInset);
  float padding=Math.min(width,viewportHeight)*.025f;
  float availableWidth=Math.max(1,width-leftInset-rightInset-2*padding),availableHeight=Math.max(1,viewportHeight-topInset-bottomInset-2*padding);
  float[] matrix=imageTransform(width,viewportHeight,imageWidth,imageHeight);
  float h=Math.min(imageHeight*matrix[0]*height,Math.min(availableHeight*.62f,availableWidth*1.3f*.70f)),w=h/1.3f;
  float center=matrix[1]+imageWidth*matrix[0]*centerX;
  float left=clamp(center-w*.5f,leftInset+padding,width-rightInset-padding-w);
  float bottom=clamp(matrix[2]+imageHeight*matrix[0]*feet,topInset+padding+h,viewportHeight-bottomInset-padding);
  return new float[]{left,bottom-h,left+w,bottom};
 }
}
