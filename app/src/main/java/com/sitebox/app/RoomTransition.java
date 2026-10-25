package com.sitebox.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.animation.DecelerateInterpolator;

/** One reversible timeline. Cancellation invalidates old completion callbacks. */
final class RoomTransition {
 interface Listener {void update(float progress,boolean running,boolean towardRoom);}
 static final long DURATION_MS=550;
 private final Listener listener;
 private ValueAnimator animator;
 private float progress,target;
 private int generation;
 private boolean disposed;
 RoomTransition(Listener listener){this.listener=listener;}
 float progress(){return progress;}
 float target(){return target;}
 boolean running(){return animator!=null;}
 void request(float next,boolean animate){
  if(disposed)return;next=RoomScene.clamp(next);
  if(animate&&animator!=null&&target==next)return;
  int ticket=++generation;if(animator!=null){animator.cancel();animator=null;}target=next;
  if(!animate||Math.abs(progress-next)<.0001f){progress=next;listener.update(progress,false,target==1);return;}
  ValueAnimator active=ValueAnimator.ofFloat(progress,next);animator=active;
  active.setDuration(Math.max(80,Math.round(DURATION_MS*Math.abs(next-progress))));
  active.setInterpolator(new DecelerateInterpolator(1.15f));
  active.addUpdateListener(value->{if(ticket!=generation||disposed)return;progress=(Float)value.getAnimatedValue();listener.update(progress,true,target==1);});
  active.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator value){if(ticket!=generation||disposed)return;animator=null;progress=target;listener.update(progress,false,target==1);}});
  listener.update(progress,true,target==1);active.start();
 }
 void dispose(){disposed=true;generation++;if(animator!=null)animator.cancel();animator=null;}
}
