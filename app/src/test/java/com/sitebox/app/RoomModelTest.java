package com.sitebox.app;
import org.junit.Test;
import static org.junit.Assert.*;
public class RoomModelTest {
 @Test public void editorProjectionMatchesTheLiveNativeCabinetOnEveryAspect(){
  for(int[] size:new int[][]{{360,800},{591,1206},{1080,1920},{1920,1080},{320,480}}){
   float w=size[0],h=size[1],cw=Math.min(w*.72f,(h-150)/RoomModelGeometry.ASPECT),ch=cw*RoomModelGeometry.ASPECT;
   RoomCameraRig rig=new RoomCameraRig();rig.update(w,h,(w-cw)/2,95,(w+cw)/2,95+ch,0);
   float[] q=new float[8];rig.shelfQuad(q);
   float[] expected={(w-cw)/2,95,(w+cw)/2,95,(w+cw)/2,95+ch,(w-cw)/2,95+ch};
   assertArrayEquals("Native books and same model frame must share the camera",expected,q,.02f);
  }
 }
 @Test public void roomShelfStaysVisibleAndWorldProjectionIsReversible(){
  RoomCameraRig rig=new RoomCameraRig();float[] q=new float[8];
  for(int[] size:new int[][]{{360,800},{591,1206},{1920,1080}}){
   rig.update(size[0],size[1],40,100,size[0]-40,size[1]-70,1);rig.shelfQuad(q);
   for(int i=0;i<8;i+=2){assertTrue("Shelf must be inside the room viewport",q[i]>=0&&q[i]<=size[0]);assertTrue(q[i+1]>=0&&q[i+1]<=size[1]);}
   float[] before=q.clone();rig.update(size[0],size[1],40,100,size[0]-40,size[1]-70,.4f);rig.update(size[0],size[1],40,100,size[0]-40,size[1]-70,1);rig.shelfQuad(q);assertArrayEquals(before,q,0);
  }
 }
 @Test public void shelfFinishPreservesOriginalAndOffersDistinctWoodColors(){
  assertArrayEquals(new float[]{1,1,1},RoomModelGeometry.finishTint(0),0);
  float[] dark=RoomModelGeometry.finishTint(1),light=RoomModelGeometry.finishTint(2);
  for(int i=0;i<3;i++){assertTrue(dark[i]>0&&dark[i]<1);assertTrue(light[i]>1&&Float.isFinite(light[i]));}
  assertArrayEquals(RoomModelGeometry.finishTint(0),RoomModelGeometry.finishTint(-4),0);
 }
 @Test public void roomCameraSeesThroughTheOpenFrontWithoutBlankBorders(){
  for(int[] size:new int[][]{{360,800},{591,1206},{1080,1920},{1920,1080}}){
   RoomCameraRig rig=new RoomCameraRig();rig.update(size[0],size[1],40,100,size[0]-40,size[1]-70,1);assertTrue(rig.eye[2]>RoomModelGeometry.FRONT);
   if(rig.eye[2]>2.3){float[] q=new float[8];rig.project(-2.25,3,2.3,q,0);rig.project(2.25,3,2.3,q,2);rig.project(2.25,0,2.3,q,4);rig.project(-2.25,0,2.3,q,6);
    for(int corner=0;corner<4;corner++){float x=(corner&1)==0?0:size[0],y=(corner&2)==0?0:size[1];for(int edge=0;edge<4;edge++){int a=edge*2,b=((edge+1)%4)*2;assertTrue("Every view ray must enter the supplied room opening",(q[b]-q[a])*(y-q[a+1])-(q[b+1]-q[a+1])*(x-q[a])>=-.1f);}}
   }
  }
 }
 @Test public void authoredForegroundTableRemainsVisibleFromTheSeatedPortraitView(){
  for(int[] size:new int[][]{{360,800},{591,1206},{1080,1920}}){RoomCameraRig rig=new RoomCameraRig();rig.update(size[0],size[1],40,100,size[0]-40,size[1]-70,1);float[] p=new float[2];
   rig.project(-.65,.4625,1.575,p,0);assertTrue("Authored coffee-table back edge must be inside the portrait viewport",p[0]>0&&p[0]<size[0]&&p[1]>size[1]*.65f&&p[1]<size[1]*.95f);
  }
 }
 @Test public void lampHitAreaContainsTheActualGlbShadeOnEveryAspect(){
  RoomCameraRig rig=new RoomCameraRig();float[] bounds=new float[4],p=new float[2];
  for(int[] size:new int[][]{{591,1206},{1920,1080}}){rig.update(size[0],size[1],40,100,size[0]-40,size[1]-50,1);rig.lampBounds(bounds);
   for(int i=0;i<8;i++){rig.project((i&1)==0?-.55:-.17,(i&2)==0?1.32:1.50,(i&4)==0?-1.35:-.97,p,0);assertTrue("Actual shade is tappable",p[0]>=bounds[0]-.02f&&p[0]<=bounds[2]+.02f&&p[1]>=bounds[1]-.02f&&p[1]<=bounds[3]+.02f);}}
 }
 @Test public void portraitRoomIncludesTheAuthoredBedAlongsideTheCabinet(){
  for(int[] size:new int[][]{{360,800},{591,1206},{1080,1920}}){RoomCameraRig rig=new RoomCameraRig();rig.update(size[0],size[1],40,100,size[0]-40,size[1]-70,1);float[] p=new float[2];
   for(double[] point:new double[][]{{1.78,.665,-.85},{1.34,.665,0}}){rig.project(point[0],point[1],point[2],p,0);assertTrue("The authored bed must be recognizable in the portrait room",p[0]>0&&p[0]<size[0]&&p[1]>0&&p[1]<size[1]);}
  }
 }
 @Test public void nineActualModelOpeningsNeverTouchFrameOrOneAnother(){
  for(int i=0;i<9;i++){float[] r=RoomModelGeometry.cell(i);assertTrue(r[0]>0&&r[1]>0&&r[2]<1&&r[3]<1);assertTrue(r[0]<r[2]&&r[1]<r[3]);assertEquals(i,RoomModelGeometry.hit((r[0]+r[2])/2,(r[1]+r[3])/2));}
 }
}
