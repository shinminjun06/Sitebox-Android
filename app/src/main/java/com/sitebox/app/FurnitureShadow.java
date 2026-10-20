package com.sitebox.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/** Small, non-interactive contact shadow: no full-screen blur or bitmap allocation. */
final class FurnitureShadow extends View {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 FurnitureShadow(Context context){
  super(context);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
  paint.setShader(new RadialGradient(0,0,1,new int[]{0x70432c15,0x35432c15,0x00432c15},new float[]{0,.45f,1},Shader.TileMode.CLAMP));
 }
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);canvas.save();canvas.translate(getWidth()*.5f,getHeight()*.5f);
  canvas.scale(getWidth()*.5f,getHeight()*.5f);canvas.drawCircle(0,0,1,paint);canvas.restore();
 }
}
