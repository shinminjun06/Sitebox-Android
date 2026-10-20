package com.sitebox.app;

import android.content.Context;
import android.graphics.*;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;
import org.json.JSONObject;

/** Draws the spine separately from the very thin paper edge; live text stays on leather. */
class BookArtworkView extends View {
 final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 final TextPaint textPaint=new TextPaint(Paint.ANTI_ALIAS_FLAG);
 final Rect source=new Rect(),paperSource=new Rect();
 final RectF spine=new RectF(),paper=new RectF();
 final JSONObject item;final Bitmap artwork;final int style,variant;
 String displayTitle="";boolean upright;
 float titleX,titleY,titleHeight,textSize;
 BookArtworkView(Context context,JSONObject data,Bitmap bitmap,int theme){
  super(context);item=data;artwork=bitmap;style=theme;variant=Math.floorMod(data.optString("name").hashCode(),4);
  if(bitmap!=null){int split=Math.round(bitmap.getWidth()*BookLayout.SOURCE_SPINE_FRACTION);source.set(0,0,split,bitmap.getHeight());paperSource.set(split,0,bitmap.getWidth(),bitmap.getHeight());}
  setContentDescription(data.optString("name")+(data.optBoolean("fav")?", 즐겨찾기":""));
 }
 @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight){
  super.onSizeChanged(width,height,oldWidth,oldHeight);
  float gap=Math.max(.5f,getResources().getDisplayMetrics().density*.35f);
  float left=gap,right=Math.max(left+1,width-gap);
  float top=height*(.08f+variant*.02f),bottom=height-1;
  spine.set(left,top,BookLayout.spineRight(left,right),bottom);paper.set(spine.right,top,right,bottom);
  titleX=spine.centerX();titleY=top+spine.height()*(BookLayout.TITLE_TOP+BookLayout.TITLE_BOTTOM)*.5f;
  titleHeight=spine.height()*(BookLayout.TITLE_BOTTOM-BookLayout.TITLE_TOP);
  // Width caps prevent letters touching the foil border even at large system font sizes.
  textSize=Math.min(10*getResources().getDisplayMetrics().scaledDensity,spine.width()*.66f);
  textPaint.setTypeface(Typeface.create(style==2?"sans-serif-medium":"serif",Typeface.NORMAL));
  textPaint.setTextSize(textSize);textPaint.setTextAlign(Paint.Align.CENTER);
  boolean light=style==1||(style==0&&variant==2)||(style==2&&variant>=2);
  textPaint.setColor(light?0xff302a24:0xffffebbd);
  String title=item.optString("name","Site");upright=title.matches(".*[\\p{IsHangul}\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}].*");
  if(upright){
   int max=Math.max(1,(int)(titleHeight/(textSize*1.16f)));
   String compact=title.replaceAll("\\s+","");
   int count=compact.codePointCount(0,compact.length());
   displayTitle=count>max?compact.substring(0,compact.offsetByCodePoints(0,Math.max(0,max-1)))+"…":compact;
  }else displayTitle=TextUtils.ellipsize(title,textPaint,titleHeight,TextUtils.TruncateAt.END).toString();
 }
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);if(getWidth()<1||getHeight()<1)return;
  if(artwork!=null){canvas.drawBitmap(artwork,source,spine,paint);canvas.drawBitmap(artwork,paperSource,paper,paint);}
  else {paint.setColor(0xff253d47);canvas.drawRoundRect(spine,2,2,paint);}
  canvas.save();canvas.clipRect(spine.left+spine.width()*.12f,spine.top+spine.height()*BookLayout.TITLE_TOP,spine.right-spine.width()*.12f,spine.top+spine.height()*BookLayout.TITLE_BOTTOM);
  if(upright){
   int count=displayTitle.codePointCount(0,displayTitle.length());float step=textSize*1.16f;
   float y=titleY-(count-1)*step*.5f-(textPaint.ascent()+textPaint.descent())*.5f;
   for(int i=0;i<displayTitle.length();){int cp=displayTitle.codePointAt(i);int next=i+Character.charCount(cp);canvas.drawText(displayTitle,i,next,titleX,y,textPaint);y+=step;i=next;}
  }else {canvas.rotate(-90,titleX,titleY);canvas.drawText(displayTitle,titleX,titleY-(textPaint.ascent()+textPaint.descent())*.5f,textPaint);}
  canvas.restore();
  if(item.optBoolean("fav")){paint.setTypeface(Typeface.DEFAULT);paint.setColor(0xffd7ad55);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(Math.min(spine.width()*.72f,11*getResources().getDisplayMetrics().density));canvas.drawText("★",titleX,spine.top+spine.height()*.22f,paint);}
 }
}
