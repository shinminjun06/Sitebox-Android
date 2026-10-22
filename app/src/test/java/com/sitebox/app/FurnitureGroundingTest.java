package com.sitebox.app;

import android.graphics.*;
import android.view.View;
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
public class FurnitureGroundingTest {
    @Test public void roomCabinetHasVisibleDepthInsteadOfAFlatWallDecal(){
        ActivityController<MainActivity> lifecycle=Robolectric.buildActivity(MainActivity.class).create();
        try{
            MainActivity a=lifecycle.get();a.setStudyMotion(false);
            for(int[] size:new int[][]{{591,1206},{926,428}}){
                if(a.sceneCollapsed)a.expandFromScene(false);layout(a,size[0],size[1]);a.collapseToScene(false);layout(a,size[0],size[1]);
                float[] front=new float[8],side=new float[8],roof=new float[8];a.miniature.copyRenderedCorners(front);a.miniature.copyRenderedBodyCorners(side,roof);
                float depth=(roof[2]-front[2])/(front[2]-front[0]);
                assertTrue("A real closed side must follow the room camera: "+depth,depth>=.10f);
                assertTrue("Cabinet must not become a deep box: "+depth,depth<=.30f);
            }
        }finally{lifecycle.destroy();}
    }
    @Test public void contactShadowsTouchTheOpaqueFeetInsteadOfBitmapBounds(){
        ActivityController<MainActivity> lifecycle=Robolectric.buildActivity(MainActivity.class).create();
        try{
            MainActivity a=lifecycle.get();a.setStudyMotion(false);layout(a,591,1206);a.collapseToScene(false);layout(a,591,1206);
            ProjectedCabinetView v=a.miniature;
            Bitmap furniture=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(furniture));
            float[] front=new float[8],contacts=new float[4];v.copyRenderedCorners(front);a.sceneShadow.contactCenters(contacts);
            for(int foot=0;foot<2;foot++){
                float x=front[0]+(foot==0?.045f:.955f)*(front[2]-front[0]);
                int min=Math.max(0,(int)x-4),max=Math.min(furniture.getWidth()-1,(int)x+4),bottom=-1;
                for(int y=furniture.getHeight()-1;y>=0&&bottom<0;y--)for(int px=min;px<=max;px++)if(Color.alpha(furniture.getPixel(px,y))>220){bottom=y;break;}
                assertTrue("Opaque furniture foot must be present",bottom>=0);
                float visibleBottom=v.getY()+(bottom+1)*v.getScaleY();
                assertEquals("Shadow must meet the visible foot "+foot,visibleBottom,contacts[2+foot],1f);
            }
            furniture.recycle();
        }finally{lifecycle.destroy();}
    }
    @Test public void visibleBackCornerHasASupportingFootOnTheFloor(){
        ActivityController<MainActivity> lifecycle=Robolectric.buildActivity(MainActivity.class).create();
        try{
            MainActivity a=lifecycle.get();a.setStudyMotion(false);layout(a,591,1206);a.collapseToScene(false);layout(a,591,1206);
            ProjectedCabinetView v=a.miniature;float[] floor=new float[8];v.groundedFootprint(floor);
            Bitmap furniture=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(furniture));
            assertTrue("Exposed rear corner must have an opaque foot reaching its floor point",Color.alpha(furniture.getPixel(Math.round(floor[2]),Math.round(floor[3]-2)))>220);
            furniture.recycle();
        }finally{lifecycle.destroy();}
    }
    void layout(MainActivity a,int w,int h){a.stage.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));a.stage.layout(0,0,w,h);a.updateSceneBackdrop();}
}
