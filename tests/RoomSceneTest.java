package com.sitebox.app;

public class RoomSceneTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void main(String[] args){int points=0;
  for(int cell=0;cell<9;cell++){
   float[] b=CabinetGeometry.cell(cell);check(CabinetGeometry.hit((b[0]+b[2])*.5f,(b[1]+b[3])*.5f)==cell,"All nine canonical hits");
   check(b[0]>0&&b[1]>0&&b[2]<1&&b[3]<1,"Cells inside the photo frame");
   float measuredFloor=new float[]{.300f,.581f,.875f}[cell/3];check(Math.abs(b[3]-measuredFloor)<.001f,"Book baseline meets measured photo shelf lip");
  }
  check(CabinetGeometry.hit(0,0)==-1&&CabinetGeometry.hit(1,1)==-1,"Outer frame is not a book cell");
  for(float[] viewport:new float[][]{{320,480},{360,800},{428,926},{591,1206},{720,1560},{1080,2400},{1080,1080},{926,428}}){
   float w=viewport[0],h=viewport[1];float[] room=new float[3],outside=new float[3],foreground=new float[3],cabinet=new float[8],sky=new float[4],pane=new float[8];
   for(float progress:new float[]{0,.25f,.5f,.75f,1}){
    RoomScene.transform(w,h,941,1672,progress,RoomScene.ROOM,false,room);RoomScene.transform(w,h,941,1672,progress,RoomScene.OUTSIDE,false,outside);RoomScene.transform(w,h,941,1672,progress,RoomScene.FOREGROUND,false,foreground);
    check(foreground[0]>=room[0]&&room[0]>=outside[0],"Foreground moves most, outside least");
    RoomScene.coverOutside(941,1672,room,outside);
    check(outside[0]<=room[0]+.001,"Outside coverage must not exaggerate the room camera zoom");
    RoomScene.map(RoomScene.SKY,941,1672,outside,sky);
    for(float[] glass:RoomScene.GLASS){RoomScene.map(glass,941,1672,room,pane);for(int i=0;i<8;i+=2){check(pane[i]>=sky[0]-.1f&&pane[i]<=sky[2]+.1f&&pane[i+1]>=sky[1]-.1f&&pane[i+1]<=sky[3]+.1f,"Frame-free sky overscan covers moving window at "+w+"x"+h+" p="+progress);points++;}}
    if(progress==1){RoomScene.map(RoomScene.CABINET,941,1672,room,cabinet);for(int i=0;i<8;i+=2)check(cabinet[i]>=0&&cabinet[i]<w&&cabinet[i+1]>0&&cabinet[i+1]<h,"Visible photographed shelf stays inside the room viewport");}
   }
   RoomScene.transform(w,h,941,1672,0,RoomScene.FOREGROUND,true,foreground);RoomScene.transform(w,h,941,1672,1,RoomScene.ROOM,true,room);
   for(int i=0;i<3;i++)check(Math.abs(foreground[i]-room[i])<.001,"Reduced effects disable plane movement");
  }
  System.out.println("PASS: nine photo shelf baselines, eight aspect ratios, "+points+" parallax coverage points and reduced effects");
 }
}
