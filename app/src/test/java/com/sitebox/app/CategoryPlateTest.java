package com.sitebox.app;

import android.graphics.*;
import android.view.View;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,qualifiers="w591dp-h1206dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CategoryPlateTest {
 @Test public void categoryInkStaysOnThePlateWhenNativeTextViewScrolls(){
  ActivityController<MainActivity> life=Robolectric.buildActivity(MainActivity.class).create();
  try{
   MainActivity a=life.get();RoomExperienceTest.layout(a,591,1206);a.expandFromScene(false);
   BookshelfView shelf=a.currentCabinet;
   // Single-line TextView can scroll its native Layout. The custom label must
   // still draw inside the visible brass plate when its parent draws the child.
   for(int i=0;i<shelf.getChildCount();i++)if(shelf.getChildAt(i) instanceof TextView)shelf.getChildAt(i).scrollTo(1000,17);
   for(boolean projected:new boolean[]{false,true}){
    Bitmap b=Bitmap.createBitmap(shelf.getWidth(),shelf.getHeight(),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(b);if(projected)shelf.drawProjected(canvas,1);else shelf.draw(canvas);
    for(int i=0;i<shelf.getChildCount();i++){
     View v=shelf.getChildAt(i);if(!(v instanceof TextView))continue;int ink=0;
     for(int y=v.getTop()+v.getHeight()/4;y<v.getTop()+v.getHeight()*3/4;y++)for(int x=v.getLeft()+v.getWidth()/8;x<v.getLeft()+v.getWidth()*7/8;x++){int p=b.getPixel(x,y);if(Color.alpha(p)>220&&Color.red(p)+Color.green(p)+Color.blue(p)<300)ink++;}
     assertTrue("Scrolled category name must remain visible in "+(projected?"projected":"native")+" brass plate",ink>3);
    }
    b.recycle();
   }
  }finally{life.destroy();}
 }
 @Test public void categoryLabelsContainVisibleText(){
  ActivityController<MainActivity> life=Robolectric.buildActivity(MainActivity.class).create();
  try{
   MainActivity a=life.get();RoomExperienceTest.layout(a,591,1206);a.expandFromScene(false);
   for(int i=0;i<a.currentCabinet.getChildCount();i++){
    View v=a.currentCabinet.getChildAt(i);if(!(v instanceof TextView))continue;
    Bitmap b=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));int ink=0;
    for(int y=b.getHeight()/4;y<b.getHeight()*3/4;y++)for(int x=b.getWidth()/8;x<b.getWidth()*7/8;x++){int p=b.getPixel(x,y);if(Color.alpha(p)>220&&Color.red(p)+Color.green(p)+Color.blue(p)<300)ink++;}
    assertTrue("Category name must be visibly drawn inside its plate",ink>3);b.recycle();
   }
  }finally{life.destroy();}
 }
}
