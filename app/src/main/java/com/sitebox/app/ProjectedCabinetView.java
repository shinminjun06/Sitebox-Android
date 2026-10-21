package com.sitebox.app;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/** Lightweight 2.5D live snapshot. The expanded BookshelfView is never transformed.
 * All paths/matrices are cached; no bitmap decode or allocation occurs in onDraw. */
final class ProjectedCabinetView extends ImageView {
 private final Matrix frontMatrix=new Matrix(),sideMatrix=new Matrix(),topMatrix=new Matrix();
 private final Path sidePath=new Path(),topPath=new Path();
 private final Paint texturePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG),shade=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final float[] source=new float[8],textureSource=new float[8],topSource=new float[8];
 private final float[] corners=new float[8],side=new float[8],roof=new float[8];
 private Bitmap wood;private float fraction,vpX=-.25f,vpY=-.5f;
 private final ColorMatrix lighting=new ColorMatrix();
 private SceneProjection profile=SceneProjection.ALL[0];
 ProjectedCabinetView(Context context){super(context);setScaleType(ScaleType.FIT_XY);}
 void setRoom(SceneProjection value,Bitmap cabinet){profile=value;wood=cabinet;rebuild();}
 void setCamera(float viewportWidth,float viewportHeight,float imageWidth,float imageHeight,float[] bounds){
  float[] transform=SceneTheme.find(profile.id).imageTransform(viewportWidth,viewportHeight,imageWidth,imageHeight);
  vpX=(transform[1]+imageWidth*transform[0]*profile.cameraX-bounds[0])/(bounds[2]-bounds[0]);
  vpY=(transform[2]+imageHeight*transform[0]*profile.cameraY-bounds[1])/(bounds[3]-bounds[1]);rebuild();
 }
 boolean sideVisible(){return profile.leftVisible(vpX)||profile.rightVisible(vpX);}
 boolean topVisible(){return profile.topVisible(vpY);}
 void groundedCorners(float[] out){profile.front(Math.max(1,getLayoutParams().width),Math.max(1,getLayoutParams().height),1,vpX,vpY,out);}
 void expectedCorners(float[] out){profile.front(getWidth(),getHeight(),fraction,vpX,vpY,out);}
 SceneProjection room(){return profile;}
 Bitmap sideTexture(){return wood;}
 public float getProjectionFraction(){return fraction;}
 public void setProjectionFraction(float value){fraction=Math.max(0,Math.min(1,value));rebuild();invalidate();}
 void copyRenderedCorners(float[] output){frontMatrix.mapPoints(output,source);}
 void copyRenderedBodyCorners(float[] sideOutput,float[] topOutput){sideMatrix.mapPoints(sideOutput,textureSource);topMatrix.mapPoints(topOutput,topSource);}
 private void quad(Path path,float[] points){path.rewind();path.moveTo(points[0],points[1]);for(int i=2;i<8;i+=2)path.lineTo(points[i],points[i+1]);path.close();}
 private void rect(float[] out,float l,float t,float r,float b){out[0]=l;out[1]=t;out[2]=r;out[3]=t;out[4]=r;out[5]=b;out[6]=l;out[7]=b;}
 private void rebuild(){
  float w=getWidth()>0?getWidth():getLayoutParams()==null?0:getLayoutParams().width,h=getHeight()>0?getHeight():getLayoutParams()==null?0:getLayoutParams().height;
  if(w<=0||h<=0)return;
  rect(source,0,0,w,h);profile.front(w,h,fraction,vpX,vpY,corners);profile.body(w,h,fraction,vpX,vpY,corners,side,roof);
  float b=1+(profile.brightness-1)*fraction,contrast=1-.045f*fraction;float warm=profile.id.equals("wizard")?-.025f:profile.id.equals("lab")?-.01f:.012f;
  lighting.set(new float[]{b*contrast+warm*fraction,0,0,0,3*fraction,0,b*contrast,0,0,3*fraction,0,0,b*contrast-warm*fraction,0,4*fraction,0,0,0,1,0});
  ColorMatrixColorFilter filter=fraction==0?null:new ColorMatrixColorFilter(lighting);texturePaint.setColorFilter(filter);setColorFilter(filter);
  frontMatrix.setPolyToPoly(source,0,corners,0,4);quad(sidePath,side);quad(topPath,roof);
  if(wood!=null){
   // Outer upright is clean uninterrupted timber, outside all nine cubbies.
   rect(textureSource,0,0,wood.getWidth(),wood.getHeight());
   rect(topSource,0,0,wood.getWidth(),wood.getHeight());
   sideMatrix.setPolyToPoly(textureSource,0,side,0,4);topMatrix.setPolyToPoly(topSource,0,roof,0,4);
  }
 }
 @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);rebuild();}
 private void face(Canvas canvas,Path path,Matrix matrix,int alpha){
  int save=canvas.save();canvas.clipPath(path);canvas.drawBitmap(wood,matrix,texturePaint);shade.setColor(Color.argb(Math.round(alpha*fraction),0,0,0));canvas.drawPath(path,shade);canvas.restoreToCount(save);
 }
 @Override protected void onDraw(Canvas canvas){
  Drawable drawable=getDrawable();if(drawable==null)return;
  if(fraction>0&&wood!=null){if(sideVisible())face(canvas,sidePath,sideMatrix,30);if(topVisible())face(canvas,topPath,topMatrix,8);}
  int save=canvas.save();canvas.concat(frontMatrix);super.onDraw(canvas);canvas.restoreToCount(save);
 }
}
