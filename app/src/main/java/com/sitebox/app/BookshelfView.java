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
 private final Bitmap artwork;
 BookshelfView(Context context,Bitmap bitmap){
  super(context);artwork=bitmap;setClipChildren(false);setClipToPadding(false);
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
  cabinet.draw(canvas);drawRoomBooks(canvas);
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
 @Override protected void onMeasure(int widthSpec,int heightSpec){
  int width=MeasureSpec.getSize(widthSpec);
  // The cabinet is wide enough for seven proportionate books in every cubby.
  int height=Math.round(width*CabinetGeometry.ASPECT);
  setMeasuredDimension(width,height);
  cabinet.measure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(height,MeasureSpec.EXACTLY));
  for(int i=1;i<getChildCount();i++){
   View child=getChildAt(i);int tag=(Integer)child.getTag();float[] r=LibraryRules.cell(tag%9);
   int cw=tag<9?Math.round(width*r[2])-Math.round(width*r[0]):Math.round(width*.175f);
   int ch=tag<9?Math.round(height*r[3])-Math.round(height*r[1]):Math.round(height*.038f);
   child.measure(MeasureSpec.makeMeasureSpec(cw,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(ch,MeasureSpec.EXACTLY));
  }
 }
 @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
  int w=right-left,h=bottom-top;cabinet.layout(0,0,w,h);
  for(int i=1;i<getChildCount();i++){
   View child=getChildAt(i);int tag=(Integer)child.getTag();int cell=tag%9;
   float[] r=LibraryRules.cell(cell);
   if(tag<9)child.layout(Math.round(w*r[0]),Math.round(h*r[1]),Math.round(w*r[2]),Math.round(h*r[3]));
   else {
    int pw=Math.round(w*.175f),ph=Math.round(h*.038f);
    int cx=Math.round(w*(r[0]+r[2])*.5f),cy=Math.round(h*(r[3]+.012f));
    child.layout(cx-pw/2,cy-ph/2,cx+pw/2,cy+ph/2);
   }
  }
 }
}
