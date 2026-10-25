package com.sitebox.app;

import android.app.ActivityManager;
import android.graphics.RectF;
import android.graphics.Color;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

/** One real model and camera throughout room, approach and native book editing. */
final class RoomExperience {
 final RoomTransition transition;
 final ModelRoomView model;
 final RoomCameraRig rig=new RoomCameraRig();
 private final MainActivity a;
 private final float[] quad=new float[8];
 private boolean reduced,disposed,pendingRequest,pendingRoom,pendingAnimate;
 private float daylight=1,clouds;
 private int precipitation;
 RoomExperience(MainActivity activity){
  a=activity;ActivityManager am=(ActivityManager)a.getSystemService(android.content.Context.ACTIVITY_SERVICE);
  reduced=a.getPreferences(0).getBoolean("roomReducedEffects",am!=null&&am.isLowRamDevice());
  transition=new RoomTransition(this::render);
  a.studyLive.setVisibility(View.GONE);a.studyLive.setActive(false);a.backdrop.setVisibility(View.GONE);a.outsideLayer.setVisibility(View.GONE);a.roomForeground.setVisibility(View.GONE);
  a.roomLayer.removeAllViews();model=new ModelRoomView(a);model.setReduced(reduced);a.roomLayer.addView(model,new FrameLayout.LayoutParams(-1,-1));
  TextView status=a.label("3D 방을 준비하고 있어요…",15,0xffffedca);status.setGravity(android.view.Gravity.CENTER);status.setPadding(a.dp(24),a.dp(24),a.dp(24),a.dp(24));status.setBackgroundColor(0xcc242a29);a.roomLayer.addView(status,new FrameLayout.LayoutParams(-1,-1));
  model.setPresentationListener(this::relayout);
  model.setStatusListener(message->{if(message==null)status.setVisibility(View.GONE);else{status.setText(message);status.setVisibility(View.VISIBLE);}relayout();});
  a.stage.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(r-l!=or-ol||b-t!=ob-ot)relayout();});
 }
 boolean reduced(){return reduced;}
 void setReduced(boolean value){reduced=value;a.getPreferences(0).edit().putBoolean("roomReducedEffects",value).apply();model.setReduced(value);}
 void bind(){
  if(a.currentCabinet==null)return;model.setFinish(a.shelfThemeIndex);model.setBooks(a.currentCabinet);a.miniature.setContent(a.currentCabinet);
  a.currentCabinet.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(!disposed&&v==a.currentCabinet)relayout();});
  a.currentCabinet.post(()->{if(!disposed)relayout();});
 }
 void relayout(){
  if(disposed||a.stage.getWidth()==0||a.stage.getHeight()==0)return;
  render(transition.progress(),transition.running(),transition.target()==1);
  if(pendingRequest&&a.currentCabinet!=null&&a.currentCabinet.getWidth()>0)request(pendingRoom,pendingAnimate);
 }
 void request(boolean room,boolean animate){
  if(disposed||a.currentCabinet==null||a.dragSource!=null)return;
  if(a.currentCabinet.getWidth()==0||a.stage.getWidth()==0||a.stage.getHeight()==0){pendingRequest=true;pendingRoom=room;pendingAnimate=animate;return;}
  pendingRequest=false;if(room)((android.view.inputmethod.InputMethodManager)a.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(a.stage.getWindowToken(),0);
  a.backgroundOnly=false;a.revealOverlay.setVisibility(View.GONE);if(a.miniature.content()!=a.currentCabinet)bind();transition.request(room?1:0,animate);
 }
 private void render(float p,boolean moving,boolean towardRoom){
  if(disposed||a.currentCabinet==null||a.currentCabinet.getWidth()==0||a.stage.getWidth()==0)return;
  a.sceneCollapsed=towardRoom;RectF bounds=a.cabinetBounds();
  rig.update(a.stage.getWidth(),a.stage.getHeight(),bounds.left,bounds.top,bounds.right,bounds.bottom,p);rig.shelfQuad(quad);
  model.setPose(rig,p);a.miniature.setModelPose(quad,p);
  a.sceneAnimating=moving||!model.poseReady();
  boolean editor=p<.0001f&&!a.sceneAnimating;
  a.currentCabinet.setVisibility(editor?View.VISIBLE:View.INVISIBLE);
  a.controls.setVisibility(p>=.9999f&&!a.sceneAnimating?View.INVISIBLE:View.VISIBLE);a.controls.setAlpha(Math.max(0,1-p*2));a.controls.setBackgroundColor(Color.TRANSPARENT);
  a.controls.setImportantForAccessibility(editor?View.IMPORTANT_FOR_ACCESSIBILITY_AUTO:View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
  a.miniature.setVisibility(editor?View.GONE:View.VISIBLE);a.miniature.setEnabled(!editor);
  a.miniature.setImportantForAccessibility(editor?View.IMPORTANT_FOR_ACCESSIBILITY_NO:View.IMPORTANT_FOR_ACCESSIBILITY_YES);
  a.sceneShadow.setVisibility(View.GONE);a.roomForeground.setVisibility(View.GONE);a.updateLampSwitch();updateLighting();
 }
 void setWeather(WeatherState state,long now){daylight=state.daylight(now);clouds=state.cloudiness();precipitation=state.precipitation();updateLighting();}
 void setClouds(float value){clouds=value;updateLighting();}
 void updateLighting(){if(!disposed){model.setWeatherImages(a.motionEnabled);model.setAtmosphere(daylight,clouds,precipitation,a.studyLampOn);a.miniature.setModelLighting(daylight,clouds,a.studyLampOn);}}
 void lampBounds(float[] out){rig.lampBounds(out);}
 void roomBounds(float[] out){RoomScene.bounds(quad,out);}
 void dispose(){disposed=true;transition.dispose();a.miniature.setContent(null);model.dispose();}
}
