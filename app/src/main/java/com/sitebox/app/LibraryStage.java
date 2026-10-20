package com.sitebox.app;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.FrameLayout;

/** Receives two fingers before child scrolling/book long presses can consume them. */
final class LibraryStage extends FrameLayout {
 private final ScaleGestureDetector detector;
 private boolean multiTouch,triggered;
 private float accumulated=1;
 LibraryStage(Context context,Runnable pinchIn){
  super(context);
  detector=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
   @Override public boolean onScaleBegin(ScaleGestureDetector gesture){accumulated=1;return true;}
   @Override public boolean onScale(ScaleGestureDetector gesture){
    accumulated*=gesture.getScaleFactor();
    if(!triggered&&accumulated<.76f){triggered=true;pinchIn.run();}
    return true;
   }
  });
  detector.setQuickScaleEnabled(false);
 }
 @Override public boolean dispatchTouchEvent(MotionEvent event){
  if(event.getActionMasked()==MotionEvent.ACTION_DOWN){multiTouch=false;triggered=false;}
  if(event.getPointerCount()>1)multiTouch=true;
  detector.onTouchEvent(event);
  return super.dispatchTouchEvent(event);
 }
 @Override public boolean onInterceptTouchEvent(MotionEvent event){return multiTouch||super.onInterceptTouchEvent(event);}
 @Override public boolean onTouchEvent(MotionEvent event){return multiTouch||super.onTouchEvent(event);}
}
