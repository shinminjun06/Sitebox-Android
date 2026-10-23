package com.sitebox.app;

import android.graphics.*;
import android.view.View;
import java.io.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CabinetPanelTest {
 @Test public void photographicFrameHasClosedStilesAndNineSolidBacks()throws Exception{
  ActivityController<MainActivity> lifecycle=Robolectric.buildActivity(MainActivity.class).create();
  try{
   MainActivity a=lifecycle.get();a.setStudyMotion(false);
   for(int[] size:new int[][]{{591,1206},{926,428}}){
    a.expandFromScene(false);layout(a,size[0],size[1]);a.expandFromScene(false);layout(a,size[0],size[1]);
    BookshelfView v=a.currentCabinet;Bitmap b=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));
    File folder=new File("build/reports/cabinet-panel");folder.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(folder,size[0]+"-panel.png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}
    Matrix m=new Matrix();
    // The final design is a photographed alpha frame, with closed wooden backs.
    // Inspect native rendered pixels in every cubby and both outer stiles.
    for(int cell=0;cell<9;cell++){
     float[] r=CabinetGeometry.cell(cell),point={(r[0]+r[2])*.5f*v.getWidth(),(r[1]+r[3])*.5f*v.getHeight()};
     m.mapPoints(point);assertTrue("Photographic cubby backs must be opaque",Color.alpha(b.getPixel((int)point[0],(int)point[1]))>=250);
    }
    for(float x:new float[]{.035f,.965f})for(float y:new float[]{.15f,.45f,.75f}){
     float[] point={v.getWidth()*x,v.getHeight()*y};m.mapPoints(point);
     assertTrue("Photographic outer stiles must be closed",Color.alpha(b.getPixel((int)point[0],(int)point[1]))>=250);
    }
    b.recycle();
   }
  }finally{lifecycle.destroy();}
 }
 void layout(MainActivity a,int w,int h){a.stage.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));a.stage.layout(0,0,w,h);a.updateSceneBackdrop();}
}
