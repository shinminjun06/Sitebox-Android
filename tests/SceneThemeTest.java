package com.sitebox.app;
import java.util.HashSet;
public class SceneThemeTest {
 public static void main(String[] args){
  String[] ids={"classroom","library","bedroom","greenhouse","cafe","tatami","attic","lab","wizard","seaside","train"};
  if(SceneTheme.ALL.length!=ids.length)throw new AssertionError("Eleven backgrounds required");
  HashSet<String> unique=new HashSet<>();
  for(int i=0;i<ids.length;i++){
   SceneTheme theme=SceneTheme.ALL[i];
   if(!ids[i].equals(theme.id)||!unique.add(theme.id)||theme.name.isEmpty())throw new AssertionError("Stable catalog IDs and labels required");
   if(SceneTheme.find(theme.id)!=theme)throw new AssertionError("Saved ID must round trip");
   if(i==0)continue;
   for(float[] screen:new float[][]{{720,1560},{1560,720},{1080,1080},{320,480},{480,320},{1200,1440}}){
    float w=screen[0],h=screen[1],iw=852,ih=1846;
    float[] matrix=theme.imageTransform(w,h,iw,ih);
    float s=matrix[0],x=matrix[1],y=matrix[2];
    if(x>.01f||y>.01f||x+iw*s<w-.01f||y+ih*s<h-.01f)throw new AssertionError("Crop must cover viewport");
    float feet=y+ih*s*theme.feet;
    if(feet<24||feet>h-24)throw new AssertionError("Anchor-aware crop must keep floor visible");
    float[] b=theme.sceneBounds(w,h,iw,ih,10,24,10,24);
    if(b[0]<10||b[1]<24||b[2]>w-10||b[3]>h-24)throw new AssertionError("Furniture must respect safe areas");
    if(Math.abs((b[3]-b[1])/(b[2]-b[0])-1.3f)>.001f)throw new AssertionError("Cabinet must retain aspect ratio");
    if(Math.abs(b[3]-feet)>.01f)throw new AssertionError("Furniture and image must share floor transform");
   }
  }
  SceneTheme train=SceneTheme.find("train");
  float rearFloor=SceneProjection.find("train").cameraY+(train.feet-SceneProjection.find("train").cameraY)/(1+SceneProjection.find("train").depth);
  if(train.feet!=.74f||train.height!=.23f||rearFloor<.71f||rearFloor>.72f)throw new AssertionError("Train feet must stand on parquet below baseboard, with rear floor near source .713");
  if(SceneTheme.find(null)!=SceneTheme.ALL[0]||SceneTheme.find("removed-room")!=SceneTheme.ALL[0])throw new AssertionError("Missing and unknown saved IDs must use classroom");
  System.out.println("PASS: stable room IDs, fallback, shared crop/pose geometry and safe areas");
 }
}
