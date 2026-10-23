package com.sitebox.app;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.widget.FrameLayout;

/** Coordinates scene layers, the reversible timeline, and the existing native editor. */
final class RoomExperience {
 final RoomTransition transition;
 private final MainActivity a;
 private Bitmap day,night,nightOff,foregroundDay,foregroundNight,outsideDay,outsideNight;
 private final Matrix roomMatrix=new Matrix(),skyMatrix=new Matrix(),foregroundMatrix=new Matrix(),outsideMatrix=new Matrix();
 private final Path panes=new Path();
 private final float[] room=new float[3],sky=new float[3],front=new float[3],quad=new float[8],mapped=new float[8],floor=new float[8];
 private boolean reduced,disposed;
 private boolean pendingRequest,pendingRoom,pendingAnimate;
 private int sceneWidth,sceneHeight;
 private float clouds;
 RoomExperience(MainActivity activity){
  a=activity;
  ActivityManager manager=(ActivityManager)a.getSystemService(Context.ACTIVITY_SERVICE);
  reduced=a.getPreferences(0).getBoolean("roomReducedEffects",manager!=null&&manager.isLowRamDevice());
  transition=new RoomTransition(this::render);
  a.studyLive.setStudentRoom(true);a.studyLive.setVisibility(View.VISIBLE);a.studyLive.setFrameListener(this::updateLighting);
 }
 boolean reduced(){return reduced;}
 void setReduced(boolean value){reduced=value;a.getPreferences(0).edit().putBoolean("roomReducedEffects",value).apply();a.studyLive.setReducedEffects(value);relayout();}
 void bind(){
  if(a.currentCabinet==null)return;
  a.miniature.setContent(a.currentCabinet);
  a.currentCabinet.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(!disposed&&a.currentCabinet==v)relayout();});
  a.currentCabinet.post(()->{if(!disposed)relayout();});
 }
 void relayout(){
  if(disposed||a.stage.getWidth()==0||a.stage.getHeight()==0)return;
  if(day==null){
   day=a.assets.get(RoomScene.ROOT+"room_day.webp");night=a.assets.get(RoomScene.ROOT+"room_night.webp");nightOff=a.assets.get(RoomScene.ROOT+"room_night_off.webp");
   foregroundDay=a.assets.get(RoomScene.ROOT+"foreground_day.webp");foregroundNight=a.assets.get(RoomScene.ROOT+"foreground_night.webp");
   if(day==null){day=a.assets.scene(false);night=a.assets.get("scenes/study_night_portrait.webp");}
   outsideDay=a.assets.get(RoomScene.ROOT+"outside_day.webp");outsideNight=a.assets.get(RoomScene.ROOT+"outside_night.webp");
   a.backdrop.setPlates(day,night,nightOff);a.backdrop.setShell(a.assets.get(RoomScene.ROOT+"room_shell.webp"));a.backdrop.setDayOff(a.assets.get(RoomScene.ROOT+"room_day_off.webp"));a.outsideLayer.setPlates(outsideDay,null,null);a.studyLive.setWindowTextures(outsideDay,outsideNight);a.roomForeground.setPlates(foregroundDay,foregroundNight,null);
   a.sceneBackdrop=day;
  }
  if(day==null)return;
  if(sceneWidth!=a.stage.getWidth()||sceneHeight!=a.stage.getHeight()){
   sceneWidth=a.stage.getWidth();sceneHeight=a.stage.getHeight();
   RoomScene.transform(sceneWidth,sceneHeight,day.getWidth(),day.getHeight(),transition.progress(),RoomScene.ROOM,reduced,room);
   a.studyLive.setScene(day,night,false,room);a.studyLive.setReducedEffects(reduced);
  }
  render(transition.progress(),transition.running(),transition.target()==1);
  if(pendingRequest&&a.currentCabinet!=null&&a.currentCabinet.getWidth()>0)request(pendingRoom,pendingAnimate);
 }
 void request(boolean room,boolean animate){
  if(disposed||a.currentCabinet==null||a.dragSource!=null)return;
  // populate() replaces the live View before Android has measured it. Preserve
  // the latest intent until bind()/layout can supply valid shared geometry.
  if(a.currentCabinet.getWidth()==0||a.stage.getWidth()==0||a.stage.getHeight()==0){pendingRequest=true;pendingRoom=room;pendingAnimate=animate;return;}
  pendingRequest=false;
  if(room)((android.view.inputmethod.InputMethodManager)a.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(a.stage.getWindowToken(),0);
  a.backgroundOnly=false;a.revealOverlay.setVisibility(View.GONE);a.controls.setVisibility(View.VISIBLE);
  if(a.miniature.content()!=a.currentCabinet)bind();
  transition.request(room?1:0,animate);
 }
 private void render(float p,boolean running,boolean towardRoom){
  if(disposed||day==null||a.currentCabinet==null||a.currentCabinet.getWidth()==0)return;
  a.sceneCollapsed=towardRoom;a.sceneAnimating=running;
  float w=a.stage.getWidth(),h=a.stage.getHeight(),iw=day.getWidth(),ih=day.getHeight();
  RectF editorBounds=a.cabinetBounds();
  RoomScene.camera(w,h,iw,ih,p,RoomScene.ROOM,reduced,editorBounds.left,editorBounds.top,editorBounds.right,editorBounds.bottom,room);
  RoomScene.camera(w,h,iw,ih,p,RoomScene.OUTSIDE,reduced,editorBounds.left,editorBounds.top,editorBounds.right,editorBounds.bottom,sky);
  RoomScene.camera(w,h,iw,ih,p,RoomScene.FOREGROUND,reduced,editorBounds.left,editorBounds.top,editorBounds.right,editorBounds.bottom,front);RoomScene.coverOutside(iw,ih,room,sky);
  matrix(roomMatrix,room);matrix(skyMatrix,sky);matrix(foregroundMatrix,front);
  panes.rewind();
  for(float[] pane:RoomScene.GLASS){RoomScene.map(pane,iw,ih,room,mapped);panes.moveTo(mapped[0],mapped[1]);for(int i=2;i<8;i+=2)panes.lineTo(mapped[i],mapped[i+1]);panes.close();}
  a.backdrop.setGeometry(roomMatrix,skyMatrix,panes);
  if(outsideDay!=null){float[] b=RoomScene.SKY;outsideMatrix.setScale(iw*(b[2]-b[0])*sky[0]/outsideDay.getWidth(),ih*(b[3]-b[1])*sky[0]/outsideDay.getHeight());outsideMatrix.postTranslate(sky[1]+iw*b[0]*sky[0],sky[2]+ih*b[1]*sky[0]);a.outsideLayer.setGeometry(outsideMatrix,skyMatrix,panes);a.outsideLayer.setRoomClip(roomMatrix,iw,ih);}
  // Foreground may have its own decode resolution, but retains the same normalized canvas.
  if(foregroundDay!=null){foregroundMatrix.setScale(iw*front[0]/foregroundDay.getWidth(),ih*front[0]/foregroundDay.getHeight());foregroundMatrix.postTranslate(front[1],front[2]);}
  a.roomForeground.setGeometry(foregroundMatrix,skyMatrix,panes);
  a.studyLive.setRoomTransforms(room,sky);
  RoomScene.transform(w,h,iw,ih,1,RoomScene.ROOM,reduced,mapped);RoomScene.map(RoomScene.CABINET,iw,ih,mapped,quad);
  a.miniature.setPose(editorBounds,quad);a.miniature.setProjectionFraction(p);
  boolean editor=p<.0001f&&!running;
  a.currentCabinet.setVisibility(editor?View.VISIBLE:View.INVISIBLE);
  a.controls.setVisibility(p>=.9999f&&!running?View.INVISIBLE:View.VISIBLE);
  a.controls.setAlpha(Math.max(0,1-p*2));
  a.controls.setImportantForAccessibility(editor?View.IMPORTANT_FOR_ACCESSIBILITY_AUTO:View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
  a.miniature.setVisibility(editor?View.GONE:View.VISIBLE);a.miniature.setEnabled(!editor);
  a.miniature.setImportantForAccessibility(editor?View.IMPORTANT_FOR_ACCESSIBILITY_NO:View.IMPORTANT_FOR_ACCESSIBILITY_YES);
  a.roomForeground.setAlpha(p);
  a.roomLayer.setAlpha(.72f+.28f*p);
  positionShadow();a.sceneShadow.setAlpha(p*p);a.sceneShadow.setVisibility(p>0?View.VISIBLE:View.GONE);
  a.updateLampSwitch();updateLighting();
 }
 private static void matrix(Matrix m,float[] t){m.setScale(t[0],t[0]);m.postTranslate(t[1],t[2]);}
 private void positionShadow(){
  a.miniature.groundedFootprint(floor);
  float minX=Float.MAX_VALUE,minY=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,maxY=-Float.MAX_VALUE;
  for(int i=0;i<8;i+=2){minX=Math.min(minX,floor[i]);maxX=Math.max(maxX,floor[i]);minY=Math.min(minY,floor[i+1]);maxY=Math.max(maxY,floor[i+1]);}
  float width=Math.max(1,floor[4]-floor[6]),padding=Math.max(a.dp(6),width*.23f),left=minX-padding,top=minY-padding*.45f;
  int sw=(int)Math.ceil(maxX-minX+padding*2),sh=(int)Math.ceil(maxY-minY+padding*1.8f);
  for(int i=0;i<8;i+=2){floor[i]-=left;floor[i+1]-=top;}
  if(a.sceneShadow.getLayoutParams().width!=sw||a.sceneShadow.getLayoutParams().height!=sh)a.sceneShadow.setLayoutParams(new FrameLayout.LayoutParams(sw,sh));
  a.sceneShadow.setFloor(floor,width);a.sceneShadow.setX(left);a.sceneShadow.setY(top);
 }
 void updateLighting(){
  if(disposed)return;
  float daylight=a.studyLive.getDaylight();
  a.roomLayer.setBackgroundColor(Color.rgb(Math.round(17+(84-17)*daylight),Math.round(24+(75-24)*daylight),Math.round(36+(60-36)*daylight)));
  a.backdrop.setLighting(daylight,clouds,a.studyLampOn);a.roomForeground.setLighting(daylight,clouds,a.studyLampOn);
  a.miniature.setAmbientLight(daylight*(1-.35f*clouds));a.sceneShadow.setAmbientLight(daylight*(1-.35f*clouds));
 }
 void setClouds(float value){clouds=value;updateLighting();}
 void lampBounds(float[] out){
  if(day==null)return;out[0]=room[1]+RoomScene.LAMP[0]*day.getWidth()*room[0];out[1]=room[2]+RoomScene.LAMP[1]*day.getHeight()*room[0];out[2]=room[1]+RoomScene.LAMP[2]*day.getWidth()*room[0];out[3]=room[2]+RoomScene.LAMP[3]*day.getHeight()*room[0];
 }
 void roomBounds(float[] out){RoomScene.bounds(quad,out);}
 void dispose(){disposed=true;transition.dispose();a.studyLive.setFrameListener(null);a.miniature.setContent(null);day=night=nightOff=foregroundDay=foregroundNight=outsideDay=outsideNight=null;}
}
