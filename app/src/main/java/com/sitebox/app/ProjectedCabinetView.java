package com.sitebox.app;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;

/** One photographed frame and live native book children through one homography.
 * No book snapshot, bitmap decode or geometry allocation in onDraw. */
final class ProjectedCabinetView extends ImageView {
 private BookshelfView content;
 private Bitmap decor;
 private final Matrix frontMatrix=new Matrix(),roomMatrix=new Matrix(),inverse=new Matrix();
 private final float[] rectangle=new float[8],corners=new float[8],roomQuad=new float[8],point=new float[2],feet={.04f,.994f,.96f,.994f};
 private final RectF editor=new RectF(),decorBounds=new RectF();
 private final Rect accessibleBounds=new Rect();
 private final int[] location=new int[2];
 private final Paint lightPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG),decorPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 private final ColorMatrix color=new ColorMatrix();
 private float fraction,ambient=1;
 private boolean lampEnabled=true,validPose,pressedInside;
 private boolean photoBackdrop;
 private Bitmap photoFrame,photoOcclusion;
 private final RectF occlusionRegion=new RectF();
 private final Matrix photoFrameMatrix=new Matrix();
 private final float[] sourceFrame=new float[8];
 private final Rect photoRegion=new Rect();
 private final RectF photoRegionF=new RectF();
 private final Paint photoFramePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 private final ColorMatrix photoTone=new ColorMatrix();
 private float photoDaylight=1,photoClouds;
 void setPhotoFrame(Bitmap bitmap){if(photoFrame==bitmap)return;photoFrame=bitmap;cachePhotoLeaves();rebuild();invalidate();}
 /** Preserve photographed leaves in front of live books. Small cached alpha mask,
  * derived only from green foliage at the left edge, never a new room/desk asset. */
 private void cachePhotoLeaves(){
  photoOcclusion=null;if(photoFrame==null)return;
  int width=Math.max(1,Math.round(photoFrame.getWidth()*.13f)),top=Math.round(photoFrame.getHeight()*.165f),height=Math.round(photoFrame.getHeight()*.515f);
  int[] pixels=new int[width*height];photoFrame.getPixels(pixels,0,width,0,top,width,height);
  for(int y=0;y<height;y++)for(int x=0;x<width;x++){
   int i=y*width+x,c=pixels[i];float nx=x/(float)photoFrame.getWidth(),ny=(y+top)/(float)photoFrame.getHeight();
   float leaf=(Color.green(c)-Color.red(c)*.72f)/14f,green=(Color.green(c)-Color.blue(c)*1.35f)/12f;
   float coverage=(nx<.075f||ny>.54f)?Math.max(0,Math.min(1,Math.min(leaf,green))):0;
   pixels[i]=(c&0x00ffffff)|(Math.round(255*coverage)<<24);
  }
  photoOcclusion=Bitmap.createBitmap(pixels,width,height,Bitmap.Config.ARGB_8888);occlusionRegion.set(0,top,width,top+height);
 }
 void setPhotoLighting(float daylight,float clouds){if(Math.abs(photoDaylight-daylight)<.001f&&Math.abs(photoClouds-clouds)<.001f)return;photoDaylight=daylight;photoClouds=clouds;RoomPlateView.referenceTone(daylight,clouds,1,photoTone);photoFramePaint.setColorFilter(daylight>.999f&&clouds<.001f?null:new ColorMatrixColorFilter(photoTone));rebuildLight();invalidate();}
 void setPhotoBackdrop(boolean value){photoBackdrop=value;invalidate();}
 private float downX,downY;
 private final int slop;
 ProjectedCabinetView(Context context){super(context);setScaleType(ScaleType.FIT_XY);slop=ViewConfiguration.get(context).getScaledTouchSlop();setClickable(true);}
 void setContent(BookshelfView view){
  content=view;if(view!=null){setImageBitmap(view.artwork());measureFeet(view.artwork());}else setImageDrawable(null);
  rebuild();invalidate();
 }
 BookshelfView content(){return content;}
 void setPose(RectF front,float[] room){editor.set(front);System.arraycopy(room,0,roomQuad,0,8);validPose=true;rebuild();invalidate();}
 void setRoomDecor(Bitmap bitmap){decor=bitmap;rebuild();invalidate();}
 void setLampEnabled(boolean value){if(lampEnabled==value)return;lampEnabled=value;rebuildLight();invalidate();}
 void setAmbientLight(float value){value=RoomScene.clamp(value);if(Math.abs(ambient-value)<.0001f)return;ambient=value;rebuildLight();invalidate();}
 public float getProjectionFraction(){return fraction;}
 public void setProjectionFraction(float value){fraction=RoomScene.clamp(value);rebuild();invalidate();}
 void copyRenderedCorners(float[] out){frontMatrix.mapPoints(out,rectangle);}
 void expectedCorners(float[] out){System.arraycopy(corners,0,out,0,8);}
 void copyRenderedBodyCorners(float[] side,float[] roof){
  if(content==null)return;float w=content.getWidth(),h=content.getHeight();
  float[] s={w*.96f,h*.03f,w*.985f,h*.03f,w*.985f,h*.94f,w*.96f,h*.94f};
  float[] r={w*.02f,h*.008f,w*.985f,h*.008f,w*.985f,h*.026f,w*.02f,h*.026f};frontMatrix.mapPoints(side,s);frontMatrix.mapPoints(roof,r);
 }
 boolean topVisible(){return !photoBackdrop&&fraction>0;}
 boolean sideVisible(){return !photoBackdrop&&fraction>0;}
 void groundedCorners(float[] out){System.arraycopy(roomQuad,0,out,0,8);}
 void groundedFootprint(float[] out){
  if(content==null)return;float w=content.getWidth(),h=content.getHeight();
  float[] contacts={feet[0]*w,feet[1]*h,feet[2]*w,feet[3]*h};frontMatrix.mapPoints(contacts);
  float depthX=(corners[2]-corners[0])*.10f,depthY=-(corners[7]-corners[1])*.035f;
  out[0]=contacts[0]+depthX;out[1]=contacts[1]+depthY;
  out[2]=contacts[2]+depthX;out[3]=contacts[3]+depthY*.75f;
  out[4]=contacts[2];out[5]=contacts[3];out[6]=contacts[0];out[7]=contacts[1];
 }
 int hitCell(float x,float y){if(!canonical(x,y))return -1;float u=point[0]/content.getWidth(),v=point[1]/content.getHeight();if(photoBackdrop&&photoFrame!=null){for(int i=0;i<9;i++){float[] a=CabinetGeometry.cell(i),b=CabinetGeometry.photoCell(i);if(u>=a[0]+(b[0]-a[0])*fraction&&u<a[2]+(b[2]-a[2])*fraction&&v>=a[1]+(b[1]-a[1])*fraction&&v<a[3]+(b[3]-a[3])*fraction)return i;}return -1;}return CabinetGeometry.hit(u,v);}
 boolean hitFrame(float x,float y){
  if(!canonical(x,y))return false;Bitmap b=content.artwork();if(b==null)return true;
  int px=Math.min(b.getWidth()-1,(int)(point[0]/content.getWidth()*b.getWidth())),py=Math.min(b.getHeight()-1,(int)(point[1]/content.getHeight()*b.getHeight()));
  return Color.alpha(b.getPixel(Math.max(0,px),Math.max(0,py)))>24;
 }
 private boolean canonical(float x,float y){
  if(content==null||!validPose||content.getWidth()==0||content.getHeight()==0)return false;
  point[0]=x;point[1]=y;inverse.mapPoints(point);
  return point[0]>=0&&point[1]>=0&&point[0]<content.getWidth()&&point[1]<content.getHeight();
 }
 private void measureFeet(Bitmap bitmap){
  if(bitmap==null)return;
  for(int foot=0;foot<2;foot++){
   int l=foot==0?0:bitmap.getWidth()*7/8,r=foot==0?bitmap.getWidth()/8:bitmap.getWidth();
   boolean found=false;
   for(int y=bitmap.getHeight()-1;y>bitmap.getHeight()*9/10&&!found;y--){
    int n=0;float x=0;for(int px=l;px<r;px++)if(Color.alpha(bitmap.getPixel(px,y))>220){n++;x+=px+.5f;}
    if(n>1){feet[foot*2]=x/n/bitmap.getWidth();feet[foot*2+1]=(y+.5f)/bitmap.getHeight();found=true;}
   }
  }
 }
 private void rebuild(){
  if(content==null||!validPose||content.getWidth()==0||content.getHeight()==0)return;
  float w=content.getWidth(),h=content.getHeight();
  rectangle[0]=0;rectangle[1]=0;rectangle[2]=w;rectangle[3]=0;rectangle[4]=w;rectangle[5]=h;rectangle[6]=0;rectangle[7]=h;
  for(int i=0;i<8;i+=2){float x=i==0||i==6?editor.left:editor.right,y=i<4?editor.top:editor.bottom;corners[i]=x+(roomQuad[i]-x)*fraction;corners[i+1]=y+(roomQuad[i+1]-y)*fraction;}
  frontMatrix.setPolyToPoly(rectangle,0,corners,0,4);frontMatrix.invert(inverse);roomMatrix.setPolyToPoly(rectangle,0,roomQuad,0,4);
  if(photoFrame!=null){for(int i=0;i<8;i+=2){sourceFrame[i]=RoomScene.PHOTO_FRAME[i]*photoFrame.getWidth();sourceFrame[i+1]=RoomScene.PHOTO_FRAME[i+1]*photoFrame.getHeight();}photoFrameMatrix.setPolyToPoly(sourceFrame,0,rectangle,0,4);photoRegion.set(0,Math.round(photoFrame.getHeight()*.165f),Math.round(photoFrame.getWidth()*.222f),Math.round(photoFrame.getHeight()*.680f));photoRegionF.set(photoRegion);}
  if(decor!=null){float dw=w*1.04f;decorBounds.set(-w*.055f,2-dw*decor.getHeight()/decor.getWidth(),w*.985f,2);}
  rebuildLight();
 }
 private void rebuildLight(){
  if(photoBackdrop){RoomPlateView.referenceTone(photoDaylight,photoClouds,fraction,color);lightPaint.setColorFilter(fraction==0?null:new ColorMatrixColorFilter(color));return;}
  float brightness=1+((.43f+.45f*ambient)*(lampEnabled?1:.90f)-1)*fraction;
  float moon=(1-ambient)*fraction;
  color.set(new float[]{brightness*(1-.10f*moon),0,0,0,3*fraction,0,brightness,0,0,3*fraction,0,0,brightness*(1+.12f*moon),0,5*moon,0,0,0,1,0});
  lightPaint.setColorFilter(fraction==0?null:new ColorMatrixColorFilter(color));
 }
 @Override protected void onDraw(Canvas canvas){
  if(content==null||!validPose)return;
  if(photoBackdrop&&photoFrame==null&&fraction>=.9999f)return;
  int save=canvas.save();canvas.concat(frontMatrix);
  int fade=photoBackdrop&&photoFrame==null?canvas.saveLayerAlpha(0,0,content.getWidth(),content.getHeight(),Math.round(255*(1-fraction))):-1;
  if(photoBackdrop&&photoFrame!=null){
   if(fraction<1){int lit=canvas.saveLayer(0,0,content.getWidth(),content.getHeight(),lightPaint);content.drawFrame(canvas);canvas.restoreToCount(lit);}
   if(fraction>0){int frame=canvas.save();canvas.clipRect(0,0,content.getWidth(),content.getHeight());canvas.concat(photoFrameMatrix);photoFramePaint.setAlpha(Math.round(255*fraction));canvas.drawBitmap(photoFrame,photoRegion,photoRegionF,photoFramePaint);canvas.restoreToCount(frame);}
   int books=canvas.saveLayer(0,0,content.getWidth(),content.getHeight(),lightPaint);content.drawPhotoBooks(canvas,fraction);content.drawCategoryLabels(canvas,Math.max(0,1-fraction*4));canvas.restoreToCount(books);
   if(photoOcclusion!=null&&fraction>0){int leaves=canvas.save();canvas.clipRect(0,0,content.getWidth(),content.getHeight());canvas.concat(photoFrameMatrix);canvas.drawBitmap(photoOcclusion,null,occlusionRegion,photoFramePaint);canvas.restoreToCount(leaves);}
   canvas.restoreToCount(save);return;
  }
  int layer=canvas.saveLayer(-content.getWidth()*.08f,-content.getHeight()*.45f,content.getWidth()*1.03f,content.getHeight(),lightPaint);
  content.drawProjected(canvas,Math.max(0,1-fraction*4));
  if(!photoBackdrop&&decor!=null&&fraction>0){decorPaint.setAlpha(Math.round(255*fraction*fraction*fraction));canvas.drawBitmap(decor,null,decorBounds,decorPaint);}
  canvas.restoreToCount(layer);if(fade>=0)canvas.restoreToCount(fade);canvas.restoreToCount(save);
 }
 @Override public boolean onTouchEvent(MotionEvent event){
  switch(event.getActionMasked()){
   case MotionEvent.ACTION_DOWN:pressedInside=hitFrame(event.getX(),event.getY());downX=event.getX();downY=event.getY();return pressedInside;
   case MotionEvent.ACTION_MOVE:if(Math.hypot(event.getX()-downX,event.getY()-downY)>slop)pressedInside=false;return true;
   case MotionEvent.ACTION_UP:boolean click=pressedInside&&hitFrame(event.getX(),event.getY());pressedInside=false;if(click)performClick();return click;
   case MotionEvent.ACTION_CANCEL:pressedInside=false;return true;
   default:return pressedInside;
  }
 }
 @Override public boolean performClick(){super.performClick();return true;}
 @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){
  super.onInitializeAccessibilityNodeInfo(info);if(!validPose)return;
  float l=Float.MAX_VALUE,t=Float.MAX_VALUE,r=-Float.MAX_VALUE,b=-Float.MAX_VALUE;
  for(int i=0;i<8;i+=2){l=Math.min(l,corners[i]);t=Math.min(t,corners[i+1]);r=Math.max(r,corners[i]);b=Math.max(b,corners[i+1]);}
  accessibleBounds.set((int)Math.floor(l),(int)Math.floor(t),(int)Math.ceil(r),(int)Math.ceil(b));info.setBoundsInParent(accessibleBounds);
  getLocationOnScreen(location);accessibleBounds.offset(location[0],location[1]);info.setBoundsInScreen(accessibleBounds);info.setClassName("android.widget.Button");
 }
}
