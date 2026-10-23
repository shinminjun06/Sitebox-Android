package com.sitebox.app;

import android.content.Context;
import android.graphics.*;
import android.widget.ImageView;

/** Cached photographic plane. Foreground is true alpha, with no room baked behind it. */
final class RoomPlateView extends ImageView {
 private final boolean foreground;
 private Bitmap day,night,off,dayOff,shell;
 private final Matrix sky=new Matrix(),registered=new Matrix();
 private final Paint photo=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG),ambient=new Paint();
 private final Path glass=new Path();
 private final RectF lampRegion=new RectF();
 private final Paint lampMask=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Matrix maskMatrix=new Matrix();
 private final Paint shellMask=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 private final RectF roomClip=new RectF();
 private boolean clipped;
 private boolean referencePhoto;
 private final ColorMatrix tone=new ColorMatrix();
 private final Paint lampShade=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Paint warmLight=new Paint(Paint.ANTI_ALIAS_FLAG);
 private float daylight=1,clouds;
 private boolean lamp=true;
 RoomPlateView(Context context,boolean foreground){super(context);this.foreground=foreground;setScaleType(ScaleType.MATRIX);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setClickable(false);}
 void setReferencePhoto(boolean value){referencePhoto=value;updateTone();invalidate();}
 void setPlates(Bitmap day,Bitmap night,Bitmap off){this.day=day;this.night=night;this.off=off;setImageBitmap(day);if(day!=null){lampShade.setShader(new RadialGradient(day.getWidth()*.425f,day.getHeight()*.382f,day.getWidth()*.038f,new int[]{0xcc15171b,0x8815171b,0x0015171b},new float[]{0,.45f,1},Shader.TileMode.CLAMP));warmLight.setShader(new RadialGradient(0,0,1,new int[]{0x66ffc479,0x24ffc479,0x00ffc479},new float[]{0,.5f,1},Shader.TileMode.CLAMP));}invalidate();}
 void setShell(Bitmap bitmap){shell=bitmap;shellMask.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));invalidate();}
 void setRoomClip(Matrix room,float width,float height){clipped=true;roomClip.set(0,0,width,height);room.mapRect(roomClip);}
 void setDayOff(Bitmap bitmap){
  dayOff=bitmap;if(day==null)return;
  lampRegion.set(day.getWidth()*.26f,day.getHeight()*.29f,day.getWidth()*.57f,day.getHeight()*.53f);
  RadialGradient gradient=new RadialGradient(0,0,1,new int[]{0xffffffff,0xffffffff,0x00ffffff},new float[]{0,.66f,1},Shader.TileMode.CLAMP);
  maskMatrix.setScale(day.getWidth()*.155f,day.getHeight()*.12f);maskMatrix.postTranslate(day.getWidth()*.415f,day.getHeight()*.41f);gradient.setLocalMatrix(maskMatrix);
  lampMask.setShader(gradient);lampMask.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
 }
 void setLighting(float daylight,float clouds,boolean lamp){if(Math.abs(this.daylight-daylight)<.001f&&Math.abs(this.clouds-clouds)<.001f&&this.lamp==lamp)return;this.daylight=daylight;this.clouds=clouds;this.lamp=lamp;if(referencePhoto)updateTone();invalidate();}
 private void updateTone(){
  if(!referencePhoto)return;referenceTone(daylight,clouds,1,tone);float n=1-daylight;
  photo.setColorFilter(n<.001f&&clouds<.001f?null:new ColorMatrixColorFilter(tone));
 }
 static void referenceTone(float daylight,float clouds,float amount,ColorMatrix out){float n=1-daylight,b=1-.65f*n-.12f*clouds*daylight;out.set(new float[]{1+(b*(1-.22f*n)-1)*amount,0,0,0,3*n*amount,0,1+(b*(1-.08f*n)-1)*amount,0,0,5*n*amount,0,0,1+(b*(1+.15f*n)-1)*amount,0,15*n*amount,0,0,0,1,0});}
 void setGeometry(Matrix plane,Matrix sky,Path panes){setImageMatrix(plane);this.sky.set(sky);glass.set(panes);invalidate();}
 private void image(Canvas c,Bitmap bitmap,Matrix matrix,int alpha){
  if(bitmap==null||bitmap.isRecycled())return;
  registered.setScale(day.getWidth()/(float)bitmap.getWidth(),day.getHeight()/(float)bitmap.getHeight());registered.postConcat(matrix);
  photo.setAlpha(alpha);c.drawBitmap(bitmap,registered,photo);
 }
 @Override protected void onDraw(Canvas c){
  if(day==null||day.isRecycled())return;
  Matrix plane=getImageMatrix();
  if(referencePhoto){
   image(c,day,plane,255);
   if(!lamp){int save=c.save();c.concat(plane);c.drawCircle(day.getWidth()*.425f,day.getHeight()*.382f,day.getWidth()*.038f,lampShade);c.restoreToCount(save);}
   else if(daylight<1){int save=c.save();c.concat(plane);c.translate(day.getWidth()*.43f,day.getHeight()*.46f);c.scale(day.getWidth()*.16f,day.getHeight()*.14f);warmLight.setAlpha(Math.round(255*(1-daylight)));c.drawCircle(0,0,1,warmLight);c.restoreToCount(save);}
   return;
  }
  if(!foreground){
   int save=shell==null?c.save():c.saveLayer(0,0,getWidth(),getHeight(),null);
   if(shell==null)c.clipPath(glass,Region.Op.DIFFERENCE);
   image(c,day,plane,255);
   if(!lamp&&dayOff!=null){
    int local=c.save();c.concat(plane);int layer=c.saveLayer(lampRegion,null);
    registered.setScale(day.getWidth()/(float)dayOff.getWidth(),day.getHeight()/(float)dayOff.getHeight());photo.setAlpha(255);c.drawBitmap(dayOff,registered,photo);c.drawRect(lampRegion,lampMask);c.restoreToCount(layer);c.restoreToCount(local);
   }
   image(c,lamp||off==null?night:off,plane,Math.round((1-daylight)*255));
   if(clouds>0&&daylight>0){ambient.setColor(Color.argb(Math.round(45*clouds*daylight),25,37,55));int local=c.save();c.concat(plane);c.drawRect(0,0,day.getWidth(),day.getHeight(),ambient);c.restoreToCount(local);}
   if(shell!=null){registered.setScale(day.getWidth()/(float)shell.getWidth(),day.getHeight()/(float)shell.getHeight());registered.postConcat(plane);c.drawBitmap(shell,registered,shellMask);}
   c.restoreToCount(save);
  }else {int save=c.save();if(clipped)c.clipRect(roomClip);image(c,day,plane,255);image(c,night,plane,Math.round((1-daylight)*255));c.restoreToCount(save);}
  photo.setAlpha(255);
 }
}
