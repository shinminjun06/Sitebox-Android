package com.sitebox.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.text.TextUtils;
import android.widget.TextView;

/** Native accessible label with cached, explicit text drawing on the timber plate. */
final class CategoryPlateView extends TextView {
 private final TextPaint lettering=new TextPaint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);
 private String displayed="";
 CategoryPlateView(Context context){super(context);}
 private void prepare(){
  if(getWidth()==0||lettering==null)return;
  lettering.set(getPaint());lettering.setColor(getCurrentTextColor());lettering.setTextAlign(Paint.Align.CENTER);
  displayed=TextUtils.ellipsize(getText(),lettering,getWidth()*.84f,TextUtils.TruncateAt.END).toString();
 }
 @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);prepare();}
 @Override protected void onTextChanged(CharSequence text,int start,int before,int count){super.onTextChanged(text,start,before,count);prepare();}
 @Override protected void onDraw(Canvas canvas){
  // TextView centers a single-line native Layout in a very wide scroll space.
  // ViewGroup subtracts that scroll when drawing us; keep custom ink anchored
  // to the visible plate rather than the native Layout's content coordinates.
  canvas.drawText(displayed,getScrollX()+getWidth()*.5f,getScrollY()+getHeight()*.5f-(lettering.ascent()+lettering.descent())*.5f,lettering);
 }
}
