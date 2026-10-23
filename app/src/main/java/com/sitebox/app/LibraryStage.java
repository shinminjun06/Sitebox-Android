package com.sitebox.app;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/** Owns a two-finger stream before child scrolling/book long presses consume it. */
final class LibraryStage extends FrameLayout {
 private final Runnable pinchIn;
 private boolean multiTouch,triggered;
 private int first=-1,second=-1;
 private float initialSpan;
 interface HitTest {boolean contains(float x,float y);}
 private java.util.function.BooleanSupplier moving=()->false;
 private HitTest furniture=(x,y)->false;
 private Runnable approach=()->{};
 private boolean blockedStream,moved;
 private float downX,downY;
 void setTransitionGuard(java.util.function.BooleanSupplier moving,HitTest furniture,Runnable approach){this.moving=moving;this.furniture=furniture;this.approach=approach;}
 LibraryStage(Context context,Runnable pinchIn){super(context);this.pinchIn=pinchIn;}
 @Override public boolean dispatchTouchEvent(MotionEvent event){
  int action=event.getActionMasked();
  if(action==MotionEvent.ACTION_DOWN){multiTouch=false;triggered=false;initialSpan=0;first=event.getPointerId(0);second=-1;}
  if(action==MotionEvent.ACTION_POINTER_DOWN&&event.getPointerCount()==2){
   multiTouch=true;first=event.getPointerId(0);second=event.getPointerId(1);initialSpan=span(event);
   // ScrollView may already have requested ownership. Reclaim this multi-touch stream.
   requestDisallowInterceptTouchEvent(false);
  }
  if(action==MotionEvent.ACTION_MOVE&&multiTouch&&!triggered&&initialSpan>48*getResources().getDisplayMetrics().density){
   float current=span(event);
   if(current>0&&current/initialSpan<.76f){triggered=true;pinchIn.run();}
  }
  if(action==MotionEvent.ACTION_POINTER_UP){int lifted=event.getPointerId(event.getActionIndex());if(lifted==first||lifted==second)initialSpan=0;}
  return super.dispatchTouchEvent(event);
 }
 private float span(MotionEvent event){int a=event.findPointerIndex(first),b=event.findPointerIndex(second);return a<0||b<0?0:(float)Math.hypot(event.getX(a)-event.getX(b),event.getY(a)-event.getY(b));}
 @Override public boolean onInterceptTouchEvent(MotionEvent event){
  if(event.getActionMasked()==MotionEvent.ACTION_DOWN){blockedStream=moving.getAsBoolean();downX=event.getX();downY=event.getY();moved=false;}
  return multiTouch||blockedStream||super.onInterceptTouchEvent(event);
 }
 @Override public boolean onTouchEvent(MotionEvent event){
  if(multiTouch)return true;
  if(blockedStream){
   if(event.getActionMasked()==MotionEvent.ACTION_MOVE&&Math.hypot(event.getX()-downX,event.getY()-downY)>android.view.ViewConfiguration.get(getContext()).getScaledTouchSlop())moved=true;
   if(event.getActionMasked()==MotionEvent.ACTION_UP){if(!moved&&furniture.contains(event.getX(),event.getY()))approach.run();blockedStream=false;}
   if(event.getActionMasked()==MotionEvent.ACTION_CANCEL)blockedStream=false;
   return true;
  }
  return super.onTouchEvent(event);
 }
}
