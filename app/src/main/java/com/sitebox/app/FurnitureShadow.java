package com.sitebox.app;
import android.content.Context;
import android.graphics.*;
import android.view.View;
/** Cached cast gradient and two narrow foot contacts; no large blur. */
final class FurnitureShadow extends View {
 private float leftFoot=.23f,rightFoot=.77f,direction=.06f;
 void setFootprint(float left,float right,float direction){leftFoot=left;rightFoot=right;this.direction=direction;invalidate();}
 void contactCenters(float[] out){out[0]=getX()+leftFoot*getWidth();out[1]=getX()+rightFoot*getWidth();}
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 FurnitureShadow(Context context){super(context);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setClickable(false);paint.setShader(new RadialGradient(0,0,1,new int[]{0x60403932,0x25403932,0x00403932},new float[]{0,.35f,1},Shader.TileMode.CLAMP));}
 private void ellipse(Canvas c,float x,float y,float rx,float ry){int save=c.save();c.translate(x,y);c.scale(rx,ry);c.drawCircle(0,0,1,paint);c.restoreToCount(save);}
 @Override protected void onDraw(Canvas c){super.onDraw(c);ellipse(c,getWidth()*(.5f+direction),getHeight()*.60f,getWidth()*.44f,getHeight()*.40f);ellipse(c,getWidth()*leftFoot,getHeight()*.25f,getWidth()*(.06f/1.04f),getHeight()*.12f);ellipse(c,getWidth()*rightFoot,getHeight()*.25f,getWidth()*(.06f/1.04f),getHeight()*.12f);}
}
