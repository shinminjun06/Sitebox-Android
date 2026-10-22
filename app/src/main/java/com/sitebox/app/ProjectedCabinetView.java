package com.sitebox.app;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.view.View;

/** Lightweight 2.5D live snapshot. The expanded BookshelfView is never transformed.
 * All paths/matrices are cached; no bitmap decode or allocation occurs in onDraw. */
final class ProjectedCabinetView extends ImageView {
 private final Matrix frontMatrix=new Matrix(),sideMatrix=new Matrix(),topMatrix=new Matrix();
 private final Path sidePath=new Path(),topPath=new Path();
 private final Paint texturePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG),shade=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final float[] source=new float[8],textureSource=new float[8],topSource=new float[8];
 private final float[] corners=new float[8],side=new float[8],roof=new float[8];
 private boolean lampEnabled=true;
 void setLampEnabled(boolean enabled){if(lampEnabled==enabled)return;lampEnabled=enabled;rebuild();invalidateRoom();}
 private Bitmap roomDecor;
 private Bitmap roomSnapshot;
 private final Paint roomPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 private final RectF roomBounds=new RectF();
 private boolean naturalWood;
 void setRoomSnapshot(Bitmap bitmap,boolean natural){roomSnapshot=bitmap;naturalWood=natural;rebuild();invalidate();}
 private final RectF decorBounds=new RectF();
 private final Paint decorPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 void setRoomDecor(Bitmap bitmap){roomDecor=bitmap;rebuild();invalidateRoom();}
 private void invalidateRoom(){invalidate();if(getParent() instanceof View)((View)getParent()).invalidate();}
 private Bitmap wood;private float ambientLight=1;private float fraction,vpX=-.25f,vpY=-.5f,widthVP=-16;
 private final ColorMatrix lighting=new ColorMatrix();
 private SceneProjection profile=SceneProjection.ALL[0];
 ProjectedCabinetView(Context context){super(context);setScaleType(ScaleType.FIT_XY);}
 void setAmbientLight(float value){float next=Math.max(0,Math.min(1,value));if(Math.abs(next-ambientLight)<.001f)return;ambientLight=next;rebuild();invalidateRoom();}
 void setRoom(SceneProjection value,Bitmap cabinet){profile=value;wood=cabinet;rebuild();}
 void setCamera(float viewportWidth,float viewportHeight,float imageWidth,float imageHeight,float[] bounds){
  profile=SceneProjection.find(profile.id,imageWidth>imageHeight);
  float[] transform=SceneTheme.find(profile.id).imageTransform(viewportWidth,viewportHeight,imageWidth,imageHeight);
  vpX=(transform[1]+imageWidth*transform[0]*profile.cameraX-bounds[0])/(bounds[2]-bounds[0]);
  vpY=(transform[2]+imageHeight*transform[0]*profile.cameraY-bounds[1])/(bounds[3]-bounds[1]);
  widthVP=(transform[1]+imageWidth*transform[0]*profile.widthVanishingX-bounds[0])/(bounds[2]-bounds[0]);rebuild();
 }
 boolean sideVisible(){return profile.leftVisible(vpX,vpY,fraction)||profile.rightVisible(vpX,vpY,fraction);}
 boolean topVisible(){return profile.topVisible(vpX,vpY,fraction);}
 void roomCamera(float[] out){out[0]=vpX*getWidth();out[1]=vpY*getHeight();}
 void groundedCorners(float[] out){profile.front(Math.max(1,getLayoutParams().width),Math.max(1,getLayoutParams().height),1,vpX,vpY,widthVP,out);}
 void expectedCorners(float[] out){profile.front(getWidth(),getHeight(),fraction,vpX,vpY,widthVP,out);}
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
  rect(source,0,0,w,h);profile.front(w,h,fraction,vpX,vpY,widthVP,corners);profile.body(w,h,fraction,vpX,vpY,widthVP,corners,side,roof);
  float roomBrightness=.96f*(.48f+.52f*ambientLight);
  float b=1+(roomBrightness-1)*fraction,contrast=1-.04f*fraction;
  float warm=.008f+.020f*(1-ambientLight);
  if(!lampEnabled){b*=1-.20f*fraction;warm=-.035f*(1-ambientLight);}
  lighting.set(new float[]{b*contrast+warm*fraction,0,0,0,4*fraction,0,b*contrast,0,0,3*fraction,0,0,b*contrast-warm*fraction,0,3*fraction,0,0,0,1,0});
  ColorMatrix saturation=new ColorMatrix();saturation.setSaturation(1-.02f*fraction);lighting.postConcat(saturation);
  ColorMatrixColorFilter filter=fraction==0?null:new ColorMatrixColorFilter(lighting);texturePaint.setColorFilter(filter);decorPaint.setColorFilter(filter);roomPaint.setColorFilter(filter);setColorFilter(filter);
  if(naturalWood){ColorMatrix timber=new ColorMatrix(new float[]{.72f,0,0,0,0,0,.60f,0,0,0,0,0,.48f,0,0,0,0,0,1,0});timber.postConcat(lighting);texturePaint.setColorFilter(new ColorMatrixColorFilter(timber));}
  roomBounds.set(0,0,w,h);
  if(roomDecor!=null){float dw=w*.90f;decorBounds.set(w*.05f,2-dw*roomDecor.getHeight()/roomDecor.getWidth(),w*.95f,2);}
  frontMatrix.setPolyToPoly(source,0,corners,0,4);quad(sidePath,side);quad(topPath,roof);
  if(wood!=null){
   // Sample a plank-width strip so a square wood texture is not squeezed into the narrow side or roof.
   rect(textureSource,wood.getWidth()*.44f,0,wood.getWidth()*.56f,wood.getHeight());
   rect(topSource,0,wood.getHeight()*.44f,wood.getWidth(),wood.getHeight()*.56f);
   sideMatrix.setPolyToPoly(textureSource,0,side,0,4);topMatrix.setPolyToPoly(topSource,0,roof,0,4);
  }
 }
 @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);rebuild();}
 private void face(Canvas canvas,Path path,Matrix matrix,int alpha){
  int save=canvas.save();canvas.clipPath(path);canvas.drawBitmap(wood,matrix,texturePaint);shade.setColor(Color.argb(Math.round(alpha*fraction),0,0,0));canvas.drawPath(path,shade);canvas.restoreToCount(save);
 }
 @Override protected void onDraw(Canvas canvas){
  Drawable drawable=getDrawable();if(drawable==null)return;
  if(fraction>0&&wood!=null){if(sideVisible())face(canvas,sidePath,sideMatrix,34);if(topVisible())face(canvas,topPath,topMatrix,14);}
  int save=canvas.save();canvas.concat(frontMatrix);
  if(roomSnapshot==null||fraction==0)super.onDraw(canvas);
  else {
   if(fraction<1){drawable.setAlpha(Math.round(255*(1-fraction)));super.onDraw(canvas);drawable.setAlpha(255);}
   roomPaint.setAlpha(Math.round(255*fraction));canvas.drawBitmap(roomSnapshot,null,roomBounds,roomPaint);
  }
  // The same live furniture transform and lighting keep props attached during collapse/rotation.
  // The parent allows this small decoration region above the cabinet, without adding a hit target.
  if(roomDecor!=null&&fraction>0){decorPaint.setAlpha(Math.round(255*fraction*fraction*fraction));canvas.drawBitmap(roomDecor,null,decorBounds,decorPaint);}
  canvas.restoreToCount(save);
 }
}
