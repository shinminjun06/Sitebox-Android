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
 @Test public void nineActualModelOpeningsNeverTouchFrameOrOneAnother(){
  for(int i=0;i<9;i++){float[] r=RoomModelGeometry.cell(i);assertTrue(r[0]>0&&r[1]>0&&r[2]<1&&r[3]<1);assertTrue(r[0]<r[2]&&r[1]<r[3]);assertEquals(i,RoomModelGeometry.hit((r[0]+r[2])/2,(r[1]+r[3])/2));}
 }
}
