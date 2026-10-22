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
 private final Path sidePath=new Path(),topPath=new Path(),rearLegPath=new Path();
 private final Paint rearLegPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Paint panelPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Paint texturePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG),shade=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final float[] source=new float[8],textureSource=new float[8],topSource=new float[8];
 private final float[] corners=new float[8],side=new float[8],roof=new float[8];
 private final Rect sideSample=new Rect(),topSample=new Rect();
 private final RectF sideSampleBounds=new RectF(),topSampleBounds=new RectF();
 private final float[] panelSeams=new float[8];
 private float joinLeft,joinRight=1,joinTop;
 private boolean lampEnabled=true;
 void setLampEnabled(boolean enabled){if(lampEnabled==enabled)return;lampEnabled=enabled;rebuild();invalidateRoom();}
 private Bitmap roomDecor;
 private Bitmap roomSnapshot;
 private final Paint roomPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 private final RectF roomBounds=new RectF();
 private final float[] footSources={.05f,1,.95f,1};
 private boolean naturalWood;
 void setRoomSnapshot(Bitmap bitmap,boolean natural){
  roomSnapshot=bitmap;naturalWood=natural;
  joinLeft=0;joinRight=1;joinTop=0;
  if(bitmap!=null){
   // Attach the panels to visible timber, not the outside of a transparent sprite.
   int middleY=bitmap.getHeight()/2,edgeLeft=0,edgeRight=bitmap.getWidth()-1,edgeTop=0;
   while(edgeLeft<edgeRight&&Color.alpha(bitmap.getPixel(edgeLeft,middleY))<=220)edgeLeft++;
   while(edgeRight>edgeLeft&&Color.alpha(bitmap.getPixel(edgeRight,middleY))<=220)edgeRight--;
   while(edgeTop<bitmap.getHeight()/5&&Color.alpha(bitmap.getPixel(bitmap.getWidth()/2,edgeTop))<=220)edgeTop++;
   if(edgeRight>edgeLeft){joinLeft=Math.min(.15f,edgeLeft/(float)bitmap.getWidth()+.003f);joinRight=Math.max(.85f,(edgeRight+1f)/bitmap.getWidth()-.003f);joinTop=edgeTop/(float)bitmap.getHeight()+.003f;}
   // Sprite files have transparent padding below their feet. Find actual opaque
   // soles once per snapshot, rather than grounding the rectangular image bounds.
   for(int foot=0;foot<2;foot++){
    int left=foot==0?0:bitmap.getWidth()*4/5,right=foot==0?bitmap.getWidth()/5:bitmap.getWidth();boolean found=false;
    for(int y=bitmap.getHeight()-1;y>=bitmap.getHeight()*4/5&&!found;y--){
     int count=0;float sum=0;
     for(int x=left;x<right;x++)if(Color.alpha(bitmap.getPixel(x,y))>220){count++;sum+=x+.5f;}
     if(count>=2){footSources[foot*2]=sum/count/bitmap.getWidth();footSources[foot*2+1]=(y+1f)/bitmap.getHeight();found=true;}
    }
    if(!found){footSources[foot*2]=foot==0?.05f:.95f;footSources[foot*2+1]=1;}
   }
  }
  rebuild();invalidate();
 }
 private final RectF decorBounds=new RectF();
 private final Paint decorPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 void setRoomDecor(Bitmap bitmap){roomDecor=bitmap;rebuild();invalidateRoom();}
 private void invalidateRoom(){invalidate();if(getParent() instanceof View)((View)getParent()).invalidate();}
 private Bitmap wood;private int woodColor=0xff775536;private float ambientLight=1;private float fraction,vpX=-.25f,vpY=-.5f,widthVP=-16;
 private final ColorMatrix lighting=new ColorMatrix();
 private SceneProjection profile=SceneProjection.ALL[0];
 ProjectedCabinetView(Context context){super(context);setScaleType(ScaleType.FIT_XY);}
 void setAmbientLight(float value){float next=Math.max(0,Math.min(1,value));if(Math.abs(next-ambientLight)<.001f)return;ambientLight=next;rebuild();invalidateRoom();}
 void setRoom(SceneProjection value,Bitmap cabinet){profile=value;wood=cabinet;if(wood!=null){int c=wood.getPixel(wood.getWidth()/2,wood.getHeight()/2);woodColor=Color.rgb(Math.round(Color.red(c)*.75f),Math.round(Color.green(c)*.75f),Math.round(Color.blue(c)*.75f));}rebuild();}
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
 void groundedFootprint(float[] out){
  float w=Math.max(1,getLayoutParams().width),h=Math.max(1,getLayoutParams().height);
  float[] a=new float[8];profile.front(w,h,1,vpX,vpY,widthVP,a);
  Matrix floorMatrix=new Matrix();float[] rectangle={0,0,w,0,w,h,0,h};floorMatrix.setPolyToPoly(rectangle,0,a,0,4);
  // Clockwise floor footprint: rear left, rear right, front right, front left.
  float[] feet={footSources[0]*w,footSources[1]*h,footSources[2]*w,footSources[3]*h};floorMatrix.mapPoints(feet);
  float maxHeight=Math.max(a[7]-a[1],a[5]-a[3]);
  for(int foot=0;foot<2;foot++){
   float across=(feet[foot*2]-a[0])/(a[2]-a[0]);
   float upright=(a[7]-a[1])*(1-across)+(a[5]-a[3])*across;
   float k=1/(1+profile.depth*upright/maxHeight);
   out[foot*2]=vpX*w+k*(feet[foot*2]-vpX*w);out[foot*2+1]=vpY*h+k*(feet[foot*2+1]-vpY*h);
   int front=foot==0?6:4;out[front]=feet[foot*2];out[front+1]=feet[foot*2+1];
  }
 }
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
  float warm=.008f*ambientLight,moon=(1-ambientLight)*fraction;
  if(!lampEnabled){b*=1-.20f*fraction;warm=-.016f*(1-ambientLight);}
  lighting.set(new float[]{b*contrast*(1-.17f*moon)+warm*fraction,0,0,0,4*fraction,0,b*contrast,0,0,3*fraction,0,0,b*contrast*(1+.18f*moon)-warm*fraction,0,(3+5*moon)*fraction,0,0,0,1,0});
  ColorMatrix saturation=new ColorMatrix();saturation.setSaturation(1-.02f*fraction);lighting.postConcat(saturation);
  ColorMatrixColorFilter filter=fraction==0?null:new ColorMatrixColorFilter(lighting);texturePaint.setColorFilter(filter);decorPaint.setColorFilter(filter);roomPaint.setColorFilter(filter);setColorFilter(filter);
  if(naturalWood){ColorMatrix timber=new ColorMatrix(new float[]{.72f,0,0,0,0,0,.60f,0,0,0,0,0,.48f,0,0,0,0,0,1,0});timber.postConcat(lighting);texturePaint.setColorFilter(new ColorMatrixColorFilter(timber));}
  panelPaint.setColor(woodColor);panelPaint.setColorFilter(texturePaint.getColorFilter());
  roomBounds.set(0,0,w,h);
  if(roomDecor!=null){float dw=w*1.06f;decorBounds.set(-w*.075f,2-dw*roomDecor.getHeight()/roomDecor.getWidth(),w*.985f,2);}
  frontMatrix.setPolyToPoly(source,0,corners,0,4);
  boolean leftPanel=profile.leftVisible(vpX,vpY,fraction);
  float seamX=(leftPanel?joinLeft:joinRight)*w;
  panelSeams[0]=seamX;panelSeams[1]=joinTop*h;panelSeams[2]=seamX;panelSeams[3]=h*.94f;
  panelSeams[4]=joinLeft*w;panelSeams[5]=joinTop*h;panelSeams[6]=joinRight*w;panelSeams[7]=joinTop*h;
  frontMatrix.mapPoints(panelSeams);
  // Small overlaps lie beneath the opaque front posts/top board and close the seam.
  sidePath.rewind();sidePath.moveTo(side[0],side[1]);sidePath.lineTo(panelSeams[0],panelSeams[1]);sidePath.lineTo(panelSeams[2],panelSeams[3]);sidePath.lineTo(side[6],side[7]);sidePath.close();
  topPath.rewind();topPath.moveTo(roof[0],roof[1]);topPath.lineTo(roof[2],roof[3]);topPath.lineTo(panelSeams[6],panelSeams[7]);topPath.lineTo(panelSeams[4],panelSeams[5]);topPath.close();
  rearLegPath.rewind();
  if(fraction>0&&sideVisible()){
   int foot=profile.leftVisible(vpX,vpY,fraction)?0:1;
   float[] sole={footSources[foot*2]*w,footSources[foot*2+1]*h};frontMatrix.mapPoints(sole);
   float across=(sole[0]-corners[0])/(corners[2]-corners[0]);
   float leftHeight=corners[7]-corners[1],rightHeight=corners[5]-corners[3];
   float k=1/(1+profile.depth*fraction*(leftHeight*(1-across)+rightHeight*across)/Math.max(leftHeight,rightHeight));
   float x=vpX*w+k*(sole[0]-vpX*w),y=vpY*h+k*(sole[1]-vpY*h),half=w*.012f;
   rearLegPath.addRect(x-half,y-h*.052f,x+half,y,Path.Direction.CW);
   rearLegPaint.setColor(woodColor);rearLegPaint.setColorFilter(texturePaint.getColorFilter());rearLegPaint.setAlpha(Math.round(255*fraction*fraction*fraction));
  }
  if(wood!=null){
   // Sample a plank-width strip so a square wood texture is not squeezed into the narrow side or roof.
   rect(textureSource,wood.getWidth()*.44f,0,wood.getWidth()*.56f,wood.getHeight());
   rect(topSource,0,wood.getHeight()*.44f,wood.getWidth(),wood.getHeight()*.56f);
   sideMatrix.setPolyToPoly(textureSource,0,side,0,4);topMatrix.setPolyToPoly(topSource,0,roof,0,4);
   sideSample.set((int)Math.floor(textureSource[0]),0,(int)Math.ceil(textureSource[4]),wood.getHeight());
   sideSampleBounds.set(textureSource[0],textureSource[1],textureSource[4],textureSource[5]);
   topSample.set(0,(int)Math.floor(topSource[1]),wood.getWidth(),(int)Math.ceil(topSource[5]));
   topSampleBounds.set(topSource[0],topSource[1],topSource[4],topSource[5]);
  }
 }
 @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);rebuild();}
 private void face(Canvas canvas,Path path,Matrix matrix,Rect sample,RectF sampleBounds,int alpha){
  // A solid panel under the texture keeps its coverage independent of filtering and UV edges.
  canvas.drawPath(path,panelPaint);
  int save=canvas.save();canvas.clipPath(path);canvas.concat(matrix);canvas.drawBitmap(wood,sample,sampleBounds,texturePaint);canvas.restoreToCount(save);
  shade.setColor(Color.argb(Math.round(alpha*fraction),0,0,0));canvas.drawPath(path,shade);
 }
 @Override protected void onDraw(Canvas canvas){
  Drawable drawable=getDrawable();if(drawable==null)return;
  if(fraction>0&&wood!=null){if(sideVisible()){canvas.drawPath(rearLegPath,rearLegPaint);face(canvas,sidePath,sideMatrix,sideSample,sideSampleBounds,34);}if(topVisible())face(canvas,topPath,topMatrix,topSample,topSampleBounds,14);}
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
