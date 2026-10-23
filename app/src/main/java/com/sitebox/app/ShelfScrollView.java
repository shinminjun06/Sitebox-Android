package com.sitebox.app;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

/** Update cabinet width before measuring, including the first frame after resize. */
final class ShelfScrollView extends ScrollView {
 ShelfScrollView(Context context){super(context);}
 @Override protected void onMeasure(int widthSpec,int heightSpec){
  int width=Math.round((MeasureSpec.getSize(widthSpec)-getPaddingLeft()-getPaddingRight())*.87f);
  if(getChildCount()>0&&getChildAt(0) instanceof ViewGroup){
   ViewGroup content=(ViewGroup)getChildAt(0);
   for(int i=0;i<content.getChildCount();i++){View child=content.getChildAt(i);ViewGroup.LayoutParams lp=child.getLayoutParams();if(lp.width!=width){lp.width=width;child.forceLayout();}}
  }
  super.onMeasure(widthSpec,heightSpec);
 }
}
