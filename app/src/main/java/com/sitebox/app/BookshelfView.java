package com.sitebox.app;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/** A single furniture sprite behind nine identical fractional interaction regions. */
final class BookshelfView extends FrameLayout {
 private final ImageView cabinet;
 private boolean modelFrame;
 void setModelFrame(){modelFrame=true;cabinet.setVisibility(GONE);setBackgroundColor(Color.TRANSPARENT);requestLayout();}
 boolean usesModelFrame(){return modelFrame;}
 private float[] opening(int i){return modelFrame?RoomModelGeometry.cell(i):LibraryRules.cell(i);}
 private final Bitmap artwork;
 private final RectF[] roomCells=new RectF[9];
 BookshelfView(Context context,Bitmap bitmap){
  super(context);artwork=bitmap;setClipChildren(false);setClipToPadding(false);for(int i=0;i<9;i++)roomCells[i]=new RectF();
  cabinet=new ImageView(context);cabinet.setScaleType(ImageView.ScaleType.FIT_XY);
  if(bitmap!=null)cabinet.setImageBitmap(bitmap);
  else {setBackgroundColor(0xffad8056);}
  addView(cabinet,new LayoutParams(-1,-1));
 }
 Bitmap artwork(){return artwork;}
 void setFinish(int index){
  if(index==0){cabinet.clearColorFilter();return;}
  ColorMatrix color=new ColorMatrix();
  if(index==1)color.set(new float[]{.60f,0,0,0,0,0,.56f,0,0,0,0,0,.55f,0,0,0,0,0,1,0});
  else color.set(new float[]{.80f,0,0,0,42,0,.84f,0,0,43,0,0,.89f,0,42,0,0,0,1,0});
  cabinet.setColorFilter(new ColorMatrixColorFilter(color));
 }
 /** Draw the same actual frame/books at full canvas resolution, including during travel. */
 void drawProjected(Canvas canvas,float labels){
  drawFrame(canvas);drawRoomBooks(canvas);drawCategoryLabels(canvas,labels);
 }
 void drawFrame(Canvas canvas){if(!modelFrame)cabinet.draw(canvas);}
 void drawCategoryLabels(Canvas canvas,float labels){
  if(labels<=0)return;
  int layer=canvas.saveLayerAlpha(0,0,getWidth(),getHeight(),Math.round(255*labels));
  for(int i=1;i<getChildCount();i++){
   View child=getChildAt(i);if(!(child.getTag() instanceof Integer)||(Integer)child.getTag()<9)continue;
   int save=canvas.save();canvas.translate(child.getLeft()-child.getScrollX(),child.getTop()-child.getScrollY());child.draw(canvas);canvas.restoreToCount(save);
  }
  canvas.restoreToCount(layer);
 }
 /** Render actual book children onto room furniture; category controls belong to the expanded UI. */
 void drawRoomBooks(Canvas canvas){
  for(int i=1;i<getChildCount();i++){
   View child=getChildAt(i);if(!(child.getTag() instanceof Integer)||(Integer)child.getTag()>=9)continue;
   int save=canvas.save();canvas.translate(child.getLeft(),child.getTop());child.draw(canvas);canvas.restoreToCount(save);
  }
 }
 /** Real book children fitted to the measured openings in the reference photograph. */
 void drawPhotoBooks(Canvas canvas,float progress){
  for(int i=1;i<getChildCount();i++){
   View child=getChildAt(i);if(!(child.getTag() instanceof Integer)||(Integer)child.getTag()>=9)continue;RectF room=roomCells[(Integer)child.getTag()];
   float left=child.getLeft()+(room.left-child.getLeft())*progress,top=child.getTop()+(room.top-child.getTop())*progress;
   float width=child.getWidth()+(room.width()-child.getWidth())*progress,height=child.getHeight()+(room.height()-child.getHeight())*progress;
   if(child.getWidth()==0||child.getHeight()==0)continue;int save=canvas.save();canvas.translate(left,top);canvas.scale(width/child.getWidth(),height/child.getHeight());child.draw(canvas);canvas.restoreToCount(save);
  }
 }
 @Override protected void onMeasure(int widthSpec,int heightSpec){
  int width=MeasureSpec.getSize(widthSpec);
  // The cabinet is wide enough for seven proportionate books in every cubby.
  int height=Math.round(width*(modelFrame?RoomModelGeometry.ASPECT:CabinetGeometry.ASPECT));
  setMeasuredDimension(width,height);
  cabinet.measure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(height,MeasureSpec.EXACTLY));
  for(int i=1;i<getChildCount();i++){
   View child=getChildAt(i);int tag=(Integer)child.getTag();float[] r=opening(tag%9);
   int cw=tag<9?Math.round(width*r[2])-Math.round(width*r[0]):Math.round(width*.175f);
   int ch=tag<9?Math.round(height*r[3])-Math.round(height*r[1]):Math.round(height*(modelFrame?.025f:.038f));
   child.measure(MeasureSpec.makeMeasureSpec(cw,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(ch,MeasureSpec.EXACTLY));
  }
 }
 @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
  int w=right-left,h=bottom-top;cabinet.layout(0,0,w,h);
  for(int cell=0;cell<9;cell++){float[] r=CabinetGeometry.photoCell(cell);roomCells[cell].set(w*r[0],h*r[1],w*r[2],h*r[3]);}
  for(int i=1;i<getChildCount();i++){
   View child=getChildAt(i);int tag=(Integer)child.getTag();int cell=tag%9;
   float[] r=opening(cell);
   if(tag<9)child.layout(Math.round(w*r[0]),Math.round(h*r[1]),Math.round(w*r[2]),Math.round(h*r[3]));
   else {
    int pw=Math.round(w*.175f),ph=Math.round(h*(modelFrame?.025f:.038f));
    int cx=Math.round(w*(r[0]+r[2])*.5f),cy=Math.round(h*(r[3]+.012f));
    child.layout(cx-pw/2,cy-ph/2,cx+pw/2,cy+ph/2);
   }
  }
 }
}
