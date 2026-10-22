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
 @Test public void sidePanelClosesTheTransparentMarginBesideTheFrontPosts()throws Exception{
  ActivityController<MainActivity> lifecycle=Robolectric.buildActivity(MainActivity.class).create();
  try{
   MainActivity a=lifecycle.get();a.setStudyMotion(false);
   for(int[] size:new int[][]{{591,1206},{926,428}}){
    if(a.sceneCollapsed)a.expandFromScene(false);layout(a,size[0],size[1]);a.collapseToScene(false);layout(a,size[0],size[1]);
    ProjectedCabinetView v=a.miniature;Bitmap b=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));
    File folder=new File("build/reports/cabinet-panel");folder.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(folder,size[0]+"-panel.png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}
    float[] front=new float[8];v.copyRenderedCorners(front);Matrix m=new Matrix();float[] source={0,0,v.getWidth(),0,v.getWidth(),v.getHeight(),0,v.getHeight()};m.setPolyToPoly(source,0,front,0,4);
    for(float y:new float[]{.15f,.45f,.75f})for(float x:new float[]{.99f,1.005f}){
     float[] point={v.getWidth()*x,v.getHeight()*y};m.mapPoints(point);
     int alpha=Color.alpha(b.getPixel(Math.round(point[0]),Math.round(point[1])));
     assertTrue("Side/front seam must be solid timber, not a transparent gutter: x="+x+" y="+y+" alpha="+alpha,alpha>=250);
    }
    float[] side=new float[8],roof=new float[8];v.copyRenderedBodyCorners(side,roof);
    for(float y:new float[]{.15f,.5f,.85f})for(float x:new float[]{.2f,.5f,.8f}){
     float left=side[0]*(1-y)+side[6]*y,right=side[2]*(1-y)+side[4]*y,top=side[1]*(1-x)+side[3]*x,bottom=side[7]*(1-x)+side[5]*x;
     assertTrue("Side panel interior must be opaque",Color.alpha(b.getPixel(Math.round(left*(1-x)+right*x),Math.round(top*(1-y)+bottom*y)))>=250);
    }
    b.recycle();
   }
  }finally{lifecycle.destroy();}
 }
 void layout(MainActivity a,int w,int h){a.stage.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));a.stage.layout(0,0,w,h);a.updateSceneBackdrop();}
}
