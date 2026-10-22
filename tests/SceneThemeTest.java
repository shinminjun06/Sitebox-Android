package com.sitebox.app;
import java.util.HashSet;
public class SceneThemeTest {
 public static void main(String[] args){
  String[] ids={"study"};
  if(SceneTheme.ALL.length!=ids.length)throw new AssertionError("Only study is selectable");
  HashSet<String> unique=new HashSet<>();
  for(int i=0;i<ids.length;i++){
   SceneTheme theme=SceneTheme.ALL[i];
   if(!ids[i].equals(theme.id)||!unique.add(theme.id)||theme.name.isEmpty())throw new AssertionError("Stable catalog IDs and labels required");
   if(SceneTheme.find(theme.id)!=theme)throw new AssertionError("Saved ID must round trip");
   if(theme.id.equals("classroom"))continue;
   for(float[] screen:new float[][]{{720,1560},{1560,720},{1080,1080},{320,480},{480,320},{1200,1440}}){
    float w=screen[0],h=screen[1],iw=theme.id.equals("study")&&w>h?1672:852,ih=theme.id.equals("study")&&w>h?941:1846;
    float[] matrix=theme.imageTransform(w,h,iw,ih);
    float s=matrix[0],x=matrix[1],y=matrix[2];
    if(x>.01f||y>.01f||x+iw*s<w-.01f||y+ih*s<h-.01f)throw new AssertionError("Crop must cover viewport");
    float feet=y+ih*s*theme.anchorFeet(iw>ih);
    if(feet<24||feet>h-24)throw new AssertionError("Anchor-aware crop must keep floor visible");
    float[] b=theme.sceneBounds(w,h,iw,ih,10,24,10,24);
    if(b[0]<10||b[1]<24||b[2]>w-10||b[3]>h-24)throw new AssertionError("Furniture must respect safe areas");
    if(Math.abs((b[3]-b[1])/(b[2]-b[0])-1.3f)>.001f)throw new AssertionError("Cabinet must retain aspect ratio");
    if(Math.abs(b[3]-feet)>.01f)throw new AssertionError("Furniture and image must share floor transform");
   }
  }
  for(String old:new String[]{null,"classroom","wizard","train","unknown"})for(boolean introduced:new boolean[]{false,true})if(!SceneTheme.initialTheme(old,introduced).equals("study"))throw new AssertionError("Old room choices migrate to study");
  SceneTheme study=SceneTheme.find("study");
  for(boolean wide:new boolean[]{false,true}){
   SceneProjection camera=SceneProjection.find("study",wide);float rear=camera.cameraY+(study.anchorFeet(wide)-camera.cameraY)/(1+camera.depth);
   if(rear<(wide?.70f:.60f)||rear>(wide?.71f:.612f))throw new AssertionError("Rear feet must meet the wall-floor seam");
   float iw=wide?1672:852,ih=wide?941:1846;
   float[] bounds=study.sceneBounds(iw,ih,iw,ih,0,0,0,0);
   float vx=(iw*camera.cameraX-bounds[0])/(bounds[2]-bounds[0]);
   if(!camera.rightVisible(vx,0,1))throw new AssertionError("Room-facing side must remain visible");
  }
  if(SceneTheme.find(null)!=SceneTheme.ALL[0]||SceneTheme.find("removed-room")!=SceneTheme.ALL[0])throw new AssertionError("Missing and unknown saved IDs must use study");
  System.out.println("PASS: stable room IDs, fallback, shared crop/pose geometry and safe areas");
 }
}
