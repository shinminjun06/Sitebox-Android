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
    @Test public void photographicCabinetFollowsTheMeasuredLeftWall(){
        ActivityController<MainActivity> lifecycle=Robolectric.buildActivity(MainActivity.class).create();
        try{
            MainActivity a=lifecycle.get();a.setStudyMotion(false);
            for(int[] size:new int[][]{{591,1206},{926,428}}){
                if(a.sceneCollapsed)a.expandFromScene(false);layout(a,size[0],size[1]);a.collapseToScene(false);layout(a,size[0],size[1]);
                float[] front=new float[8],side=new float[8],roof=new float[8];a.miniature.copyRenderedCorners(front);a.miniature.copyRenderedBodyCorners(side,roof);
                assertEquals("Near and far posts stay upright",front[0],front[6],.1f);assertEquals(front[2],front[4],.1f);
                assertTrue("The left-wall cabinet recedes upward toward the rear corner",front[7]>front[5]+1);
                float[] transform=new float[9];a.backdrop.getImageMatrix().getValues(transform);
                float[] source={.035f,.385f,.225f,.384f,.225f,.590f,.035f,.670f};
                for(int i=0;i<8;i+=2){assertEquals(transform[2]+source[i]*a.sceneBackdrop.getWidth()*transform[0],front[i],.5f);assertEquals(transform[5]+source[i+1]*a.sceneBackdrop.getHeight()*transform[4],front[i+1],.5f);}

            }
        }finally{lifecycle.destroy();}
    }
    @Test public void contactShadowsTouchTheOpaqueFeetInsteadOfBitmapBounds(){
        ActivityController<MainActivity> lifecycle=Robolectric.buildActivity(MainActivity.class).create();
        try{
            MainActivity a=lifecycle.get();a.setStudyMotion(false);layout(a,591,1206);a.collapseToScene(false);layout(a,591,1206);
            ProjectedCabinetView v=a.miniature;
            Bitmap furniture=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(furniture));
            float[] floor=new float[8],contacts=new float[4];v.groundedFootprint(floor);a.sceneShadow.contactCenters(contacts);
            for(int foot=0;foot<2;foot++){
                float x=floor[foot==0?6:4];
                int min=Math.round(x),max=Math.round(x),bottom=-1;
                for(int y=furniture.getHeight()-1;y>=0&&bottom<0;y--)for(int px=min;px<=max;px++)if(Color.alpha(furniture.getPixel(px,y))>220){bottom=y;break;}
                assertTrue("Opaque furniture foot must be present",bottom>=0);
                float visibleBottom=v.getY()+(bottom+.5f)*v.getScaleY();
                assertEquals("Shadow must meet the visible foot "+foot,visibleBottom,contacts[2+foot],1.5f);
            }
            furniture.recycle();
        }finally{lifecycle.destroy();}
    }
    @Test public void groundOcclusionHasDepthBehindThePhotographedFeet(){
        ActivityController<MainActivity> lifecycle=Robolectric.buildActivity(MainActivity.class).create();
        try{
            MainActivity a=lifecycle.get();a.setStudyMotion(false);layout(a,591,1206);a.collapseToScene(false);layout(a,591,1206);
            ProjectedCabinetView v=a.miniature;float[] floor=new float[8];v.groundedFootprint(floor);
            Bitmap furniture=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(furniture));
            assertTrue("Ground occlusion stays behind the two visible photographed feet",floor[1]<floor[7]&&floor[3]<floor[5]);
            assertTrue("The footprint has depth across the floor",floor[0]>floor[6]&&floor[2]>floor[4]);
            furniture.recycle();
        }finally{lifecycle.destroy();}
    }
    void layout(MainActivity a,int w,int h){a.stage.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));a.stage.layout(0,0,w,h);a.updateSceneBackdrop();}
}
