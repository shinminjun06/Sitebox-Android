package com.sitebox.app;

/** Stable persisted room IDs and source-image anchors, independent of book/cabinet finishes. */
final class SceneTheme {
 static final String PREFERENCE_KEY="backgroundTheme";
 static final SceneTheme[] ALL={new SceneTheme("study","테마 1 · 햇살 공부방",.43f,.605f,.21f)};
 // Old cloud backups remain readable after removing their room artwork.
 static boolean acceptsBackupId(String id){
  if("study".equals(id))return true;
  for(String legacy:new String[]{"classroom","library","bedroom","greenhouse","cafe","tatami","attic","lab","wizard","seaside","train"})if(legacy.equals(id))return true;
  return false;
 }
 final String id,name;
 final float centerX,feet,height;
 private SceneTheme(String id,String name,float centerX,float feet,float height){this.id=id;this.name=name;this.centerX=centerX;this.feet=feet;this.height=height;}
 static SceneTheme find(String id){for(SceneTheme scene:ALL)if(scene.id.equals(id))return scene;return ALL[0];}
 String assetPath(boolean wide){return "scenes/study_"+(wide?"landscape":"portrait")+".webp";}
 float anchorX(boolean wide){return id.equals("study")&&wide?.40f:centerX;}
 float anchorFeet(boolean wide){return id.equals("study")&&wide?.69f:feet;}
 float anchorHeight(boolean wide){return id.equals("study")&&wide?.34f:height;}
 static String initialTheme(String saved,boolean introduced){return find(saved).id;}
 private static float clamp(float value,float min,float max){return Math.max(min,Math.min(max,value));}
 /** Scale, x translation, y translation used by both ImageView and furniture geometry. */
 float[] imageTransform(float width,float viewportHeight,float imageWidth,float imageHeight){
  float scale=Math.max(width/imageWidth,viewportHeight/imageHeight);
  boolean wide=imageWidth>imageHeight;float cx=anchorX(wide),fy=anchorFeet(wide);
  return new float[]{scale,clamp(width*.5f-imageWidth*scale*cx,width-imageWidth*scale,0),clamp(viewportHeight*.84f-imageHeight*scale*fy,viewportHeight-imageHeight*scale,0)};
 }
 float[] sceneBounds(float width,float viewportHeight,float imageWidth,float imageHeight,float leftInset,float topInset,float rightInset,float bottomInset){
  float padding=Math.min(width,viewportHeight)*.025f;
  float availableWidth=Math.max(1,width-leftInset-rightInset-2*padding),availableHeight=Math.max(1,viewportHeight-topInset-bottomInset-2*padding);
  boolean wide=imageWidth>imageHeight;
  float[] matrix=imageTransform(width,viewportHeight,imageWidth,imageHeight);
  float h=Math.min(imageHeight*matrix[0]*anchorHeight(wide),Math.min(availableHeight*.62f,availableWidth*1.3f*.70f)),w=h/1.3f;
  float center=matrix[1]+imageWidth*matrix[0]*anchorX(wide);
  float left=clamp(center-w*.5f,leftInset+padding,width-rightInset-padding-w);
  float bottom=clamp(matrix[2]+imageHeight*matrix[0]*anchorFeet(wide),topInset+padding+h,viewportHeight-bottomInset-padding);
  return new float[]{left,bottom-h,left+w,bottom};
 }
}
