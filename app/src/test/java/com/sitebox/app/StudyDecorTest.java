package com.sitebox.app;

import android.graphics.*;
import android.view.View;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import java.io.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class StudyDecorTest {
    @Test public void decorRendersAboveCompactCabinetWithoutChangingBooks()throws Exception{
        ActivityController<MainActivity> lifecycle=Robolectric.buildActivity(MainActivity.class).create();MainActivity a=lifecycle.get();
        try{
            a.setStudyMotion(false);a.sites=new JSONArray();
            for(int cell=0;cell<9;cell++)for(int i=0;i<3;i++)a.sites.put(new JSONObject().put("name","책 "+cell+"-"+i).put("url","https://example.com/"+cell+"/"+i).put("category",a.categories[cell]));
            a.populate();String library=a.sites.toString();
            for(boolean wide:new boolean[]{false,true})for(boolean night:new boolean[]{false,true}){
                if(a.sceneCollapsed)a.expandFromScene(false);
                int w=wide?926:428,h=wide?428:926;layout(a,w,h);
                long now=1700000000000L;a.setAmbientPreview(new WeatherState(20,0,0,0,now,0,0,now,true,0,night?0:1),now);a.updateSceneBackdrop();a.collapseToScene(false);layout(a,w,h);
                float[] front=new float[8],contacts=new float[4];a.miniature.copyRenderedCorners(front);a.sceneShadow.contactCenters(contacts);
                assertTrue("Low cabinet top must be visible below the eye line",a.miniature.topVisible());
                assertTrue("Front feet must follow room slope",front[5]>front[7]+1);
                assertEquals(a.miniature.getY()+front[7]*a.miniature.getScaleY(),contacts[2],2f);
                assertEquals(a.miniature.getY()+front[5]*a.miniature.getScaleY(),contacts[3],2f);
                Bitmap decor=a.assets.get("themes/study/cabinet_decor.webp");assertNotNull(decor);
                Bitmap with=render(a,w,h);a.miniature.setRoomDecor(null);Bitmap without=render(a,w,h);
                int bottom=(int)a.miniature.getY(),top=Math.max(0,bottom-(int)(a.miniature.getWidth()*a.miniature.getScaleX()*.7f));int changes=0;
                for(int y=top;y<bottom;y++)for(int x=0;x<w;x++)if(with.getPixel(x,y)!=without.getPixel(x,y))changes++;
                assertTrue("Decor above the view must survive parent clipping",changes>100);
                a.miniature.setRoomDecor(decor);save(with,(wide?"landscape":"portrait")+(night?"-night":"-day"));
                a.lampSwitch.performClick();Bitmap off=render(a,w,h);save(off,(wide?"landscape":"portrait")+(night?"-night-off":"-day-off"));a.lampSwitch.performClick();
                assertEquals("Decor must never become decorative fake books",library,a.sites.toString());
                a.expandFromScene(false);assertEquals(View.GONE,a.miniature.getVisibility());assertEquals(library,a.sites.toString());
                with.recycle();without.recycle();off.recycle();
            }
        }finally{lifecycle.destroy();}
    }
    void layout(MainActivity a,int w,int h){a.stage.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));a.stage.layout(0,0,w,h);a.updateSceneBackdrop();}
    Bitmap render(MainActivity a,int w,int h){Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);a.stage.draw(new Canvas(b));return b;}
    void save(Bitmap b,String name)throws Exception{File dir=new File("build/reports/study-reference");dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}}
}
