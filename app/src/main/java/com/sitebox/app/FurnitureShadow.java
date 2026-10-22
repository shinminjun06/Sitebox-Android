package com.sitebox.app;
import android.content.Context;
import android.graphics.*;
import android.view.View;
/** Ground-plane occlusion and cast light share the furniture's actual opaque soles. */
final class FurnitureShadow extends View {
 private boolean lampEnabled=true;
 private float ambientLight=1,width;
 private final float[] floor=new float[8];
 private final Path footprint=new Path();
 private final Paint soft=new Paint(Paint.ANTI_ALIAS_FLAG),solid=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final ColorFilter moonShadow=new PorterDuffColorFilter(0xff172034,PorterDuff.Mode.SRC_IN);
 FurnitureShadow(Context context){
  super(context);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setClickable(false);
  soft.setShader(new RadialGradient(0,0,1,new int[]{0xc01e140e,0x601e140e,0x001e140e},new float[]{0,.40f,1},Shader.TileMode.CLAMP));
 }
 void setAmbientLight(float value){ambientLight=Math.max(0,Math.min(1,value));invalidate();}
 void setLampEnabled(boolean enabled){if(lampEnabled==enabled)return;lampEnabled=enabled;invalidate();}
 void setFloor(float[] points,float cabinetWidth){
  System.arraycopy(points,0,floor,0,8);width=cabinetWidth;
  footprint.rewind();footprint.moveTo(floor[0],floor[1]);for(int i=2;i<8;i+=2)footprint.lineTo(floor[i],floor[i+1]);footprint.close();invalidate();
 }
 void contactCenters(float[] out){out[0]=getX()+floor[6];out[1]=getX()+floor[4];if(out.length>=4){out[2]=getY()+floor[7];out[3]=getY()+floor[5];}}
 private void ellipse(Canvas c,float x,float y,float rx,float ry){int save=c.save();c.translate(x,y);c.scale(Math.max(.1f,rx),Math.max(.1f,ry));c.drawCircle(0,0,1,soft);c.restoreToCount(save);}
 @Override protected void onDraw(Canvas c){
  super.onDraw(c);if(width<=0)return;
  float centerX=(floor[4]+floor[6])*.5f,centerY=(floor[5]+floor[7])*.5f;
  soft.setColorFilter(lampEnabled?null:moonShadow);soft.setAlpha(lampEnabled?Math.round(110+55*ambientLight):75);
  // Daylight from the right window casts left and forward; ambient light leaves a softer shadow.
  int save=c.save();c.rotate((float)Math.toDegrees(Math.atan2(floor[5]-floor[7],floor[4]-floor[6])),centerX,centerY);
  ellipse(c,centerX-width*(lampEnabled?.06f:.015f),centerY+width*(lampEnabled?.045f:.015f),width*.65f,width*(lampEnabled?.095f:.05f));c.restoreToCount(save);
  solid.setColor(lampEnabled?0x48251910:0x30141c2d);c.drawPath(footprint,solid);
  soft.setAlpha(lampEnabled?225:175);
  ellipse(c,floor[0],floor[1],width*.035f,Math.max(1,width*.012f));
  ellipse(c,floor[2],floor[3],width*.035f,Math.max(1,width*.012f));
  // A narrow continuous occlusion under the base joins the feet into one solid object.
  soft.setAlpha(lampEnabled?185:130);save=c.save();c.rotate((float)Math.toDegrees(Math.atan2(floor[5]-floor[7],floor[4]-floor[6])),centerX,centerY);
  ellipse(c,centerX,centerY-width*.012f,width*.51f,width*.027f);c.restoreToCount(save);
  soft.setAlpha(lampEnabled?255:205);
  ellipse(c,floor[6],floor[7],width*.050f,Math.max(1,width*.015f));
  ellipse(c,floor[4],floor[5],width*.050f,Math.max(1,width*.015f));
 }
}
