package com.sitebox.app;

import android.graphics.*;
import android.view.View;
import java.io.File;
import java.io.FileOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class LampRenderingTest {
    Bitmap asset(String name)throws Exception{try(java.io.InputStream in=RuntimeEnvironment.getApplication().getAssets().open("scenes/"+name+".webp")){Bitmap b=BitmapFactory.decodeStream(in);assertNotNull(name,b);return b;}}
    @Test public void switchingOffReplacesIndoorLightingButPreservesSunlight()throws Exception{verifyRelighting(false);}
    @Test public void nightSwitchRemovesWarmReflectionsAndLampShadows()throws Exception{verifyRelighting(true);}
    @Test public void litNightKeepsCoolMoonlightAwayFromTheDeskLamp()throws Exception{
        Bitmap day=asset("study_portrait"),night=asset("study_night_portrait"),off=asset("study_lamp_off_night_portrait");
        StudyLiveView live=new StudyLiveView(RuntimeEnvironment.getApplication());live.setMotionEnabled(false);live.setScene(day,night,false,new float[]{.5f,0,0});live.setLampPlates(null,off);
        long now=1700000000000L;live.setWeather(new WeatherState(20,0,0,0,now,0,0,now,true,0,0),now);
        int w=day.getWidth()/2,h=day.getHeight()/2;live.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));live.layout(0,0,w,h);
        Bitmap on=render(day,live,w,h);int wall=on.getPixel(Math.round(w*.30f),Math.round(h*.30f));
        assertTrue("A lit desk lamp must retain the reference's blue-grey moonlit wall",Color.blue(wall)>Color.red(wall));
        int bulb=on.getPixel(Math.round(w*.035f),Math.round(h*.486f));assertTrue("The warm-white bulb still emits light",Color.red(bulb)>Color.blue(bulb));
        int paper=on.getPixel(Math.round(w*.30f),Math.round(h*.79f));assertTrue("Warm lamp light remains local to the desktop",Color.red(paper)>Color.blue(paper));
        live.release();day.recycle();night.recycle();off.recycle();on.recycle();
    }
    void verifyRelighting(boolean nightTime)throws Exception{
        Bitmap day=asset("study_portrait"),night=asset("study_night_portrait"),offDay=asset("study_lamp_off_portrait"),offNight=asset("study_lamp_off_night_portrait");
        StudyLiveView live=new StudyLiveView(RuntimeEnvironment.getApplication());
        live.setMotionEnabled(false);live.setScene(day,night,false,new float[]{.5f,0,0});live.setLampPlates(offDay,offNight);
        long now=1700000000000L;
        live.setWeather(new WeatherState(20,0,0,0,now,0,0,now,true,0,nightTime?0:1),now);
        int w=day.getWidth()/2,h=day.getHeight()/2;
        live.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));live.layout(0,0,w,h);
        Bitmap on=render(day,live,w,h);live.setLampEnabled(false);Bitmap off=render(day,live,w,h);
        int x=Math.round(w*.035f),y=Math.round(h*.486f);
        assertTrue("Unlit bulb must lose its emission",brightness(off.getPixel(x,y))<brightness(on.getPixel(x,y))*.65f);
        assertEquals("The original window must stay pixel-aligned",on.getPixel(Math.round(w*.75f),Math.round(h*.27f)),off.getPixel(Math.round(w*.75f),Math.round(h*.27f)));
        if(nightTime){
            int lit=on.getPixel(Math.round(w*.30f),Math.round(h*.79f)),unlit=off.getPixel(Math.round(w*.30f),Math.round(h*.79f));
            assertTrue("Lit paper has a warm lamp reflection",Color.red(lit)>Color.blue(lit));
            assertTrue("Unlit paper must reflect cool ambient light rather than dimmed orange",Color.blue(unlit)>Color.red(unlit));
        }
        live.setLampEnabled(true);Bitmap restored=render(day,live,w,h);assertEquals(on.getPixel(x,y),restored.getPixel(x,y));
        save(on,(nightTime?"night":"day")+"-lamp-on");save(off,(nightTime?"night":"day")+"-lamp-off");
        live.release();day.recycle();night.recycle();offDay.recycle();offNight.recycle();on.recycle();off.recycle();restored.recycle();
    }
    @Test public void renderCompactRoomWithCabinetAndChangedContactShadow()throws Exception{
        ActivityController<MainActivity> lifecycle=Robolectric.buildActivity(MainActivity.class).create();
        MainActivity a=lifecycle.get();
        try{
            a.applyBackgroundTheme("study");a.setStudyMotion(false);
            long now=1700000000000L;a.setAmbientPreview(new WeatherState(20,0,0,0,now,0,0,now,true,0,0),now);
            a.stage.measure(View.MeasureSpec.makeMeasureSpec(428,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(926,View.MeasureSpec.EXACTLY));a.stage.layout(0,0,428,926);a.updateSceneBackdrop();
            a.collapseToScene(false);saveStage(a,"room-night-on");a.lampSwitch.performClick();
            assertFalse((Boolean)GoogleAccountTest.get(a.sceneShadow,"lampEnabled"));
            float[] front=new float[8];a.miniature.copyRenderedCorners(front);assertTrue(Math.max(front[2],front[4])<a.lampSwitch.getX());
            saveStage(a,"room-night-off");
        }finally{lifecycle.destroy();}
    }
    void saveStage(MainActivity a,String name)throws Exception{a.stage.measure(View.MeasureSpec.makeMeasureSpec(428,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(926,View.MeasureSpec.EXACTLY));a.stage.layout(0,0,428,926);assertTrue(a.miniature.getWidth()>1);Bitmap b=Bitmap.createBitmap(428,926,Bitmap.Config.ARGB_8888);a.stage.draw(new Canvas(b));save(b,name);b.recycle();}
    void save(Bitmap b,String name)throws Exception{File dir=new File("build/reports/lamp");dir.mkdirs();try(FileOutputStream f=new FileOutputStream(new File(dir,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,f);}}
    Bitmap render(Bitmap day,StudyLiveView live,int w,int h){Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(b);canvas.drawBitmap(day,null,new Rect(0,0,w,h),null);live.draw(canvas);return b;}
    int brightness(int color){return Color.red(color)+Color.green(color)+Color.blue(color);}
}
