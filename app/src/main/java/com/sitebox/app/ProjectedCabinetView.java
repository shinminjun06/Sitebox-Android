package com.sitebox.app;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import java.util.ArrayList;

/** Live wooden cabinet. All boards and books use the same room camera; only the
 * expanded editor uses a flat sprite. Paths and texture transforms are cached. */
final class ProjectedCabinetView extends ImageView {
 private final float[] source=new float[8],corners=new float[8],side=new float[8],roof=new float[8];
 private final Matrix frontMatrix=new Matrix(),decorMatrix=new Matrix();
 private final Paint decorPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 private final RectF decorBounds=new RectF();
 private final Path[] openings=new Path[9];
 private final ArrayList<Face> faces=new ArrayList<>();
 private final ColorMatrix lighting=new ColorMatrix();
 private Bitmap wood,joinery,roomBooks,roomDecor;
 private boolean naturalWood,lampEnabled=true;
 private float ambientLight=1,fraction,vpX,vpY;
 private SceneProjection profile=SceneProjection.ALL[0];
 private static final float[] LEFT={.045f,.357667f,.670334f},RIGHT={.329667f,.642334f,.955f};
 private static final float[] TOP={.030f,.330f,.630f},BOTTOM={.310f,.610f,.910f};

 ProjectedCabinetView(Context context){
  super(context);setScaleType(ScaleType.FIT_XY);
  for(int i=0;i<9;i++)openings[i]=new Path();
  // Four real supports. The exposed rear foot is part of the same closed model.
  leg(0,.045f,.88f,1);leg(.955f,1,.88f,1);
  add(-1,-1,1.02f,1,0,1,1,0,0,1,.955f,0,1,.955f,1);
  for(int cell=0;cell<9;cell++){
   float l=LEFT[cell%3],r=RIGHT[cell%3],t=TOP[cell/3],b=BOTTOM[cell/3];
   add(cell,-1,.34f,l,t,1,r,t,1,r,b,1,l,b,1); // back
   add(cell,-1,.48f,l,t,0,l,t,1,l,b,1,l,b,0); // left inside
   add(cell,-1,.57f,r,t,1,r,t,0,r,b,0,r,b,1); // right inside
   add(cell,-1,.39f,l,t,0,r,t,0,r,t,1,l,t,1); // ceiling
   add(cell,-1,.69f,l,b,1,r,b,1,r,b,0,l,b,0); // shelf surface
   add(cell,cell,1,l,t,.08f,r,t,.08f,r,b,.08f,l,b,.08f);
  }
  // Front joinery. Posts, dividers and shelf lips are not painted into a bitmap.
  front(0,.03f,.045f,.91f,.88f);front(.955f,.03f,1,.91f,.88f);
  front(RIGHT[0],.03f,LEFT[1],.91f,.88f);front(RIGHT[1],.03f,LEFT[2],.91f,.88f);
  front(.045f,.31f,.955f,.33f,.86f);front(.045f,.61f,.955f,.63f,.86f);
  front(0,0,1,.03f,.92f);front(0,.91f,1,.955f,.82f);
  // Small bevels catch the same window light as the large top, rather than gold labels.
  front(0,0,1,.003f,1.03f);front(.045f,.31f,.955f,.313f,1.01f);
  front(.045f,.61f,.955f,.613f,1.01f);front(0,.91f,1,.914f,.98f);
  add(-1,-1,.99f,0,0,1,1,0,1,1,0,0,0,0,0);
  leg(0,.045f,0,.12f);leg(.955f,1,0,.12f);
 }
 private void leg(float l,float r,float back,float front){
  add(-1,-1,.69f,r,.95f,front,r,.95f,back,r,1,back,r,1,front);
  add(-1,-1,.76f,l,.95f,back,r,.95f,back,r,1,back,l,1,back);
 }
 private void front(float l,float t,float r,float b,float shade){add(-1,-1,shade,l,t,0,r,t,0,r,b,0,l,b,0);}
 private void add(int clip,int book,float shade,float... vertices){faces.add(new Face(clip,book,shade,vertices));}
 void setRoomSnapshot(Bitmap bitmap,boolean natural){roomBooks=bitmap;naturalWood=natural;rebuild();invalidateRoom();}
 void setRoomDecor(Bitmap bitmap){roomDecor=bitmap;rebuild();invalidateRoom();}
 void setRoomJoinery(Bitmap bitmap){joinery=bitmap;rebuild();invalidateRoom();}
 void setLampEnabled(boolean enabled){if(lampEnabled==enabled)return;lampEnabled=enabled;rebuild();invalidateRoom();}
 void setAmbientLight(float value){float next=Math.max(0,Math.min(1,value));if(Math.abs(next-ambientLight)<.001f)return;ambientLight=next;rebuild();invalidateRoom();}
 void setRoom(SceneProjection value,Bitmap texture){profile=value;wood=texture;rebuild();invalidateRoom();}
 void setCamera(float viewportWidth,float viewportHeight,float imageWidth,float imageHeight,float[] bounds){
  profile=SceneProjection.find(profile.id,imageWidth>imageHeight);
  float[] transform=SceneTheme.find(profile.id).imageTransform(viewportWidth,viewportHeight,imageWidth,imageHeight);
  vpX=(transform[1]+imageWidth*transform[0]*profile.cameraX-bounds[0])/(bounds[2]-bounds[0]);
  vpY=(transform[2]+imageHeight*transform[0]*profile.cameraY-bounds[1])/(bounds[3]-bounds[1]);rebuild();invalidateRoom();
 }
 private void invalidateRoom(){invalidate();if(getParent() instanceof View)((View)getParent()).invalidate();}
 boolean sideVisible(){return fraction>0;}
 boolean topVisible(){return fraction>0;}
 void roomCamera(float[] out){out[0]=vpX*getWidth();out[1]=vpY*getHeight();}
 void groundedCorners(float[] out){profile.front(Math.max(1,getLayoutParams().width),Math.max(1,getLayoutParams().height),1,out);}
 void groundedFootprint(float[] out){profile.footprint(Math.max(1,getLayoutParams().width),Math.max(1,getLayoutParams().height),out);}
 void expectedCorners(float[] out){profile.front(getWidth(),getHeight(),fraction,out);}
 SceneProjection room(){return profile;}
 Bitmap sideTexture(){return wood;}
 public float getProjectionFraction(){return fraction;}
 public void setProjectionFraction(float value){fraction=Math.max(0,Math.min(1,value));rebuild();invalidateRoom();}
 void copyRenderedCorners(float[] output){frontMatrix.mapPoints(output,source);}
 void copyRenderedBodyCorners(float[] sideOutput,float[] topOutput){System.arraycopy(side,0,sideOutput,0,8);System.arraycopy(roof,0,topOutput,0,8);}
 private static void quad(Path path,float[] points){path.rewind();path.moveTo(points[0],points[1]);for(int i=2;i<8;i+=2)path.lineTo(points[i],points[i+1]);path.close();}
 private void rebuild(){
  float w=getWidth()>0?getWidth():getLayoutParams()==null?0:getLayoutParams().width,h=getHeight()>0?getHeight():getLayoutParams()==null?0:getLayoutParams().height;
  if(w<=0||h<=0)return;
  source[0]=0;source[1]=0;source[2]=w;source[3]=0;source[4]=w;source[5]=h;source[6]=0;source[7]=h;
  profile.front(w,h,fraction,corners);profile.body(w,h,fraction,corners,side,roof);frontMatrix.setPolyToPoly(source,0,corners,0,4);
  float b=1+(.96f*(.48f+.52f*ambientLight)-1)*fraction,contrast=1-.04f*fraction;
  float warm=.008f*ambientLight,moon=(1-ambientLight)*fraction;
  if(!lampEnabled){b*=1-.20f*fraction;warm=-.016f*(1-ambientLight);}
  lighting.set(new float[]{b*contrast*(1-.17f*moon)+warm*fraction,0,0,0,4*fraction,0,b*contrast,0,0,3*fraction,0,0,b*contrast*(1+.18f*moon)-warm*fraction,0,(3+5*moon)*fraction,0,0,0,1,0});
  ColorMatrix saturation=new ColorMatrix();saturation.setSaturation(1-.02f*fraction);lighting.postConcat(saturation);
  ColorMatrixColorFilter filter=new ColorMatrixColorFilter(lighting);decorPaint.setColorFilter(filter);setColorFilter(fraction==0?null:filter);
  float[] points=new float[8];
  for(int cell=0;cell<9;cell++){
   float l=LEFT[cell%3],r=RIGHT[cell%3],t=TOP[cell/3],bottom=BOTTOM[cell/3];
   profile.point(w,h,fraction,l,t,0,points,0);profile.point(w,h,fraction,r,t,0,points,2);
   profile.point(w,h,fraction,r,bottom,0,points,4);profile.point(w,h,fraction,l,bottom,0,points,6);quad(openings[cell],points);
  }
  for(Face face:faces)face.rebuild(w,h);
  if(roomDecor!=null){
   float decorHeight=w*1.06f*roomDecor.getHeight()/roomDecor.getWidth()/h;
   profile.point(w,h,fraction,-.075f,-decorHeight,.42f,points,0);profile.point(w,h,fraction,.985f,-decorHeight,.42f,points,2);
   profile.point(w,h,fraction,.985f,0,.42f,points,4);profile.point(w,h,fraction,-.075f,0,.42f,points,6);
   float[] rect={0,0,roomDecor.getWidth(),0,roomDecor.getWidth(),roomDecor.getHeight(),0,roomDecor.getHeight()};decorMatrix.setPolyToPoly(rect,0,points,0,4);
   decorBounds.set(0,0,roomDecor.getWidth(),roomDecor.getHeight());
  }
 }
 @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);rebuild();}
 @Override protected void onDraw(Canvas canvas){
  Drawable expanded=getDrawable();if(expanded==null)return;
  if(fraction<1){expanded.setAlpha(Math.round(255*(1-fraction)));super.onDraw(canvas);expanded.setAlpha(255);}
  if(fraction==0||wood==null)return;
  int layer=canvas.saveLayerAlpha(0,0,getWidth(),getHeight(),Math.round(255*fraction));
  for(Face face:faces)face.draw(canvas);canvas.restoreToCount(layer);
  if(roomDecor!=null){int save=canvas.save();canvas.concat(decorMatrix);decorPaint.setAlpha(Math.round(255*fraction*fraction*fraction));canvas.drawBitmap(roomDecor,0,0,decorPaint);canvas.restoreToCount(save);}
 }
 private final class Face {
  final int clip,book;final float shade;final float[] vertices,points=new float[8],uv=new float[8];
  final Path path=new Path();final Matrix matrix=new Matrix();final Rect sample=new Rect();final RectF sampleBounds=new RectF();
  final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG),fill=new Paint(Paint.ANTI_ALIAS_FLAG);
  Face(int clip,int book,float shade,float[] vertices){this.clip=clip;this.book=book;this.shade=shade;this.vertices=vertices;}
  void rebuild(float w,float h){
   for(int i=0;i<4;i++)profile.point(w,h,fraction,vertices[i*3],vertices[i*3+1],vertices[i*3+2],points,i*2);quad(path,points);
   Bitmap bitmap=bitmap();if(bitmap==null)return;
   if(book>=0){float[] cell=LibraryRules.cell(book);sample.set(Math.round(cell[0]*bitmap.getWidth()),Math.round(cell[1]*bitmap.getHeight()),Math.round(cell[2]*bitmap.getWidth()),Math.round(cell[3]*bitmap.getHeight()));}
   else if(naturalWood&&joinery!=null){
    // Reuse photographed timber only as material samples. No baked opening edges
    // or old perspective are projected onto the new solid model.
    boolean front=vertices[2]==vertices[5]&&vertices[2]==vertices[8]&&vertices[2]==vertices[11];
    boolean horizontal=vertices[1]==vertices[4]&&vertices[1]==vertices[7];
    boolean upright=vertices[0]==vertices[3]&&vertices[0]==vertices[6];
    if(front&&vertices[2]<.5f){
     if(vertices[1]>=.95f)sample.set(13,624,32,650);
     else if(vertices[1]>=.91f)sample.set(11,579,501,618);
     else if(length(0,1)<length(1,2))sample.set(169,24,185,575);
     else if(vertices[1]<.03f)sample.set(11,6,501,18);
     else sample.set(35,198,167,212);
    }else if(front)sample.set(54,52,155,194);
    else if(horizontal&&clip>=0){if(vertices[1]==TOP[clip/3])sample.set(55,22,141,44);else sample.set(55,553,141,574);}
    else if(upright&&clip>=0)sample.set(34,49,49,194);
    else sample.set(169,24,185,575);
   }
   else {
    double u=length(0,1),v=length(1,2),ratio=u/Math.max(.0001,v);
    int sw=(int)Math.max(2,Math.min(bitmap.getWidth(),bitmap.getHeight()*ratio)),sh=(int)Math.max(2,Math.min(bitmap.getHeight(),bitmap.getWidth()/ratio));
    int sx=(bitmap.getWidth()-sw)/2,sy=(bitmap.getHeight()-sh)/2;sample.set(sx,sy,sx+sw,sy+sh);
   }
   sampleBounds.set(sample);uv[0]=sample.left;uv[1]=sample.top;uv[2]=sample.right;uv[3]=sample.top;uv[4]=sample.right;uv[5]=sample.bottom;uv[6]=sample.left;uv[7]=sample.bottom;matrix.setPolyToPoly(uv,0,points,0,4);
   ColorMatrix color=new ColorMatrix();
   float strength=shade;
   if(book<0&&naturalWood&&joinery!=null&&clip>=0)strength=.55f+shade*.45f;
   color.setScale(strength,strength,strength,1);color.postConcat(lighting);ColorMatrixColorFilter filter=new ColorMatrixColorFilter(color);paint.setColorFilter(filter);fill.setColorFilter(filter);
   fill.setColor(bitmap.getPixel(bitmap.getWidth()/2,bitmap.getHeight()/2));
  }
  private double length(int a,int b){double x=(vertices[a*3]-vertices[b*3])*SceneProjection.CABINET_WIDTH,y=(vertices[a*3+1]-vertices[b*3+1])*SceneProjection.CABINET_HEIGHT,z=(vertices[a*3+2]-vertices[b*3+2])*SceneProjection.CABINET_DEPTH;return Math.sqrt(x*x+y*y+z*z);}
  private Bitmap bitmap(){return book>=0?roomBooks:naturalWood&&joinery!=null?joinery:wood;}
  void draw(Canvas canvas){
   Bitmap bitmap=bitmap();if(bitmap==null)return;
   int save=canvas.save();if(clip>=0)canvas.clipPath(openings[clip]);
   if(book<0)canvas.drawPath(path,fill);
   canvas.clipPath(path);canvas.concat(matrix);canvas.drawBitmap(bitmap,sample,sampleBounds,paint);canvas.restoreToCount(save);
  }
 }
}
