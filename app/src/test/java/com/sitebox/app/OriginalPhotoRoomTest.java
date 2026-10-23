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
@Config(sdk=28,qualifiers="w591dp-h1206dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class OriginalPhotoRoomTest {
 @Test public void originalPhotoKeepsItsDeskAndWallWithoutFurnitureOverlays(){
  ActivityController<MainActivity> life=Robolectric.buildActivity(MainActivity.class).create();
  try{
   MainActivity a=life.get();a.setStudyMotion(false);RoomExperienceTest.layout(a,591,1206);long t=1700000000000L;a.setAmbientPreview(new WeatherState(20,0,0,0,t,0,0,t,true,0,1),t);a.collapseToScene(false);
   Bitmap reference=a.assets.get("themes/rooms/student/room_reference.jpg");assertSame("Room must use the user's unchanged photo",reference,a.sceneBackdrop);
   assertEquals("No separately generated foreground desk",View.GONE,a.roomForeground.getVisibility());assertEquals("Keep the photo's own floor shadows",View.GONE,a.sceneShadow.getVisibility());
   Bitmap overlay=Bitmap.createBitmap(591,1206,Bitmap.Config.ARGB_8888);a.miniature.draw(new Canvas(overlay));float[] front=new float[8];a.miniature.copyRenderedCorners(front);int ink=0;for(int y=0;y<1206;y+=3)for(int x=0;x<591;x+=3)if(Color.alpha(overlay.getPixel(x,y))>0){ink++;assertTrue("Only the original shelf region may be edited",x>=front[0]-2&&x<=front[2]+2&&y>=front[1]-2&&y<=front[7]+2);}assertTrue("The photo shelf must display actual book data",ink>100);float[] floor={reference.getWidth()*.012f,reference.getHeight()*.695f};a.backdrop.getImageMatrix().mapPoints(floor);assertEquals("Shelf edit must stop above the original pot and floor",0,Color.alpha(overlay.getPixel(Math.round(floor[0]),Math.round(floor[1]))));overlay.recycle();
   Bitmap backdrop=Bitmap.createBitmap(591,1206,Bitmap.Config.ARGB_8888);a.backdrop.draw(new Canvas(backdrop));
   for(float[] point:new float[][]{{.24f,.13f},{.24f,.37f},{.50f,.48f},{.48f,.90f},{.55f,.76f}}){float[] p={reference.getWidth()*point[0],reference.getHeight()*point[1]};int expected=reference.getPixel((int)p[0],(int)p[1]);a.backdrop.getImageMatrix().mapPoints(p);int actual=backdrop.getPixel(Math.round(p[0]),Math.round(p[1]));assertTrue("Desk, wall and floor must come from the source photograph",Math.abs(Color.red(expected)-Color.red(actual))+Math.abs(Color.green(expected)-Color.green(actual))+Math.abs(Color.blue(expected)-Color.blue(actual))<35);}
   backdrop.recycle();
  }finally{life.destroy();}
 }
}
