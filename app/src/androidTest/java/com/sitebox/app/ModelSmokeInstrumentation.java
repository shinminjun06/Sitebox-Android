package com.sitebox.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import org.json.*;

/** Exercises the real native GPU renderer; host tests cannot stand in for this. */
public final class ModelSmokeInstrumentation extends SmokeInstrumentation {
 private boolean visualOnly;
 @Override public void onCreate(Bundle arguments){visualOnly=arguments!=null&&"true".equals(arguments.getString("visualOnly"));super.onCreate(arguments);}
 int reportedChecks;
 int expectedWidth,expectedHeight;
 final android.app.Application.ActivityLifecycleCallbacks activityTracker=new android.app.Application.ActivityLifecycleCallbacks(){
  public void onActivityResumed(Activity next){if(next instanceof MainActivity)activity=(MainActivity)next;}
  public void onActivityCreated(Activity next,Bundle state){}
  public void onActivityStarted(Activity next){}
  public void onActivityPaused(Activity next){}
  public void onActivityStopped(Activity next){}
  public void onActivitySaveInstanceState(Activity next,Bundle state){}
  public void onActivityDestroyed(Activity next){}
 };
 @Override void phase(String name){super.phase(name);if(checks.length()>reportedChecks){Bundle progress=new Bundle();progress.putString("stream",checks.substring(reportedChecks));sendStatus(1,progress);reportedChecks=checks.length();}}
 @Override public void onStart(){
  if(visualOnly){visualPreview();return;}
  Bundle result=new Bundle();int code=Activity.RESULT_OK;String original=null;
  final android.app.Application application=(android.app.Application)getTargetContext().getApplicationContext();main(()->application.registerActivityLifecycleCallbacks(activityTracker));
  try{
   phase("native GLB startup");activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();shell("am force-stop com.google.android.apps.nexuslauncher");
   original=activity.sites.toString();awaitModel();awaitScene(true);main(()->{
    android.graphics.Bitmap image=activity.roomExperience.model.getBitmap(64,128);check(image!=null,"GPU texture has displayed pixels");
    image.recycle();
   });snapshot("3d-room.png");
   main(()->{check(activity.roomExperience.model.renderableCount()>650,"actual supplied GLB rendered with furniture and walls");check(activity.roomExperience.model.failure()==null,"native renderer reports no initialization failure");check(activity.roomExperience.model.frames()>0,"Filament GPU frame submitted");check(activity.roomExperience.model.finishMaterialCount()>15,"shelf finishes have isolated model materials");});
   phase("native GLB visible and interactive");tapRoomCabinet();main(()->{check(activity.roomExperience.model.gpuBooksReady()&&activity.currentCabinet.usesGpuBooks(),"books and category labels render in the same GPU frame as the model");check(activity.roomExperience.model.poseReady(),"native book input waits for the actual displayed editor camera");});snapshot("3d-editor-empty.png");
   main(()->{check(!activity.header.performLongClick(),"empty long press does not collapse or hide editor");check(!activity.backgroundOnly&&!activity.sceneCollapsed&&activity.controls.getVisibility()==View.VISIBLE,"editor remains visible after long press");
    activity.sites=new JSONArray();for(int i=0;i<70;i++){JSONObject o=new JSONObject();try{o.put("name","실제 책 "+i);o.put("url","https://example.com/"+i);o.put("category",activity.categories[i%9]);o.put("sortOrder",i*1000L);}catch(JSONException e){throw new RuntimeException(e);}activity.sites.put(o);}activity.save();activity.populate();
   });waitForIdleSync();snapshot("3d-editor-books.png");main(()->{check(activity.roomExperience.model.solidBookCount()==63,"actual page contains 63 solid data-driven books");check(activity.roomExperience.model.solidBookTriangles()>7000,"covers pages and curved spines are volumetric meshes");check(activity.roomExperience.model.outsideDepth()>25,"window neighborhood has actual world-space depth");check(activity.roomExperience.model.lampCastsShadows(),"desk spotlight casts physical shadows");});for(int style=0;style<3;style++){final int choice=style;main(()->{activity.shelfThemeIndex=choice;activity.bookThemeIndex=choice;activity.populate();});snapshot("3d-style-"+style+".png");}main(()->{activity.shelfThemeIndex=0;activity.bookThemeIndex=0;activity.populate();});
   main(()->{check(activity.pageSummary.getText().toString().contains("/ 2"),"overflow paginates without overlapping books");for(int i=1;i<activity.currentCabinet.getChildCount();i+=2){android.view.ViewGroup row=(android.view.ViewGroup)activity.currentCabinet.getChildAt(i);for(int b=0;b<row.getChildCount();b++)check(row.getChildAt(b).getRight()<=row.getWidth(),"book stays inside actual model opening");}});
   phase("native book dialogs");crudDialogs();phase("native book drag and sync regression");nativeDrag();main(()->check(activity.sites.optJSONObject(0).optString("category").equals(activity.categories[1]),"native long press moves the real book without retreating"));new BackupChecks(this).run();awaitSystemToasts();
   phase("same model camera gestures");main(()->activity.expandFromScene(false));pinch(false);awaitScene(false);pinch(true);awaitScene(true);snapshot("3d-pinch-room.png");tapRoomCabinet();
   main(()->{activity.collapseToScene(false);check(activity.roomExperience.model.ready(),"same GLB survives editor and room transitions");});snapshot("3d-room-books.png");
   String data=activity.sites.toString();long t=1700000000000L;
   int[] codes={0,0,63,73,3,63,73};String[] states={"day","night","rain","snow","cloud","night_rain","night_snow"};
   for(int mode=0;mode<states.length;mode++){
    final int m=mode;final String state=states[mode];main(()->activity.setAmbientPreview(new WeatherState(m==3||m==6?-2:20,0,codes[m],m>=2?100:0,t,0,0,t,true,0,m==1||m>=5?0:1),t));SystemClock.sleep(1200);snapshot("3d-"+state+".png");main(()->{check(activity.sites.toString().equals(data),"weather change preserves book data");check(activity.roomExperience.model.windowState().equals(state),"actual 3D window material state "+state);});
    if(mode==1){
     final float[] roomLight={0};main(()->roomLight[0]=activity.roomExperience.model.ambientIntensity());tapRoomCabinet();main(()->check(Math.abs(activity.roomExperience.model.ambientIntensity()-roomLight[0])<.01f,"camera approach preserves the real room light instead of bleaching the cabinet"));snapshot("3d-night-editor.png");main(()->activity.collapseToScene(false));snapshot("3d-night-room-return.png");
     final android.graphics.Bitmap[] before={null},after={null};main(()->{check(activity.roomExperience.model.lampIntensity()>0,"native lamp light has positive intensity");before[0]=activity.roomExperience.model.getBitmap(64,128);activity.lampSwitch.performClick();check(!activity.studyLampOn,"room lamp switch turns the real light off");check(activity.roomExperience.model.lampIntensity()==0,"native lamp light intensity becomes zero");});snapshot("3d-night-lamp-off.png");main(()->{after[0]=activity.roomExperience.model.getBitmap(64,128);check(!before[0].sameAs(after[0]),"lamp switch changes actual GPU room lighting");before[0].recycle();after[0].recycle();activity.lampSwitch.performClick();});
    }
   }
   main(()->{activity.clearAmbientPreview();callActivityOnPause(activity);});SystemClock.sleep(250);long frames=activity.roomExperience.model.frames();SystemClock.sleep(350);check(activity.roomExperience.model.frames()==frames,"paused activity submits no native GPU frames");main(()->callActivityOnResume(activity));awaitModel();
   for(int[] size:new int[][]{{720,1560},{1080,1920},{1560,720}}){resizeWindow(size[0],size[1]);main(()->{activity.expandFromScene(false);activity.collapseToScene(false);});snapshot("3d-ratio-"+size[0]+"x"+size[1]+".png");}
   resizeWindow(720,1560);main(()->{activity.load();activity.populate();check(activity.sites.length()>0,"book data survives preferences reload");});
   phase("displayed camera reversal and native Back");tapRoomCabinet();roomReversalAndBack();
   checks.append("PASS actual 3D room, shared camera, native book management, gestures and lifecycle verified\n");
  }catch(Throwable e){code=Activity.RESULT_CANCELED;checks.append("FAIL ").append(e).append('\n');try{snapshot("3d-failure.png");}catch(Throwable ignored){}}
  finally{main(()->application.unregisterActivityLifecycleCallbacks(activityTracker));if(activity!=null&&original!=null){final String saved=original;main(()->{try{activity.sites=new JSONArray(saved);activity.save();}catch(JSONException e){throw new RuntimeException(e);}if(activity.roomExperience.model.presented())activity.finish();});}}
  result.putString("stream",checks.toString());finish(code,result);
 }
 private void visualPreview(){
  Bundle result=new Bundle();int code=Activity.RESULT_OK;java.util.Map<String,?> originalPreferences=null;
  try{
   phase("solid books and window daylight preview");activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));originalPreferences=activity.getPreferences(0).getAll();shell("am force-stop com.google.android.apps.nexuslauncher");awaitModel();awaitScene(true);
   main(()->{activity.sites=new JSONArray();for(int i=0;i<70;i++)try{activity.sites.put(new JSONObject().put("name","입체 서재 "+i).put("url","https://example.com/book/"+i).put("category",activity.categories[i%9]).put("sortOrder",i*1000L));}catch(JSONException e){throw new RuntimeException(e);}activity.save();activity.populate();activity.setAmbientPreview(new WeatherState(23,0,0,0,1700000000000L,0,0,1700000000000L,true,0,1),1700000000000L);});
   snapshot("solid-day-room.png");tapRoomCabinet();snapshot("solid-day-books.png");
   main(()->{check(activity.roomExperience.model.solidBookCount()==63,"preview renders actual solid books");check(activity.roomExperience.model.lampCastsShadows(),"preview lamp has native shadow casting enabled");activity.roomExperience.transition.request(.38f,false);});snapshot("solid-book-depth.png");
   main(()->{activity.collapseToScene(false);activity.setAmbientPreview(new WeatherState(12,0,0,0,1700000000000L,0,0,1700000000000L,true,0,0),1700000000000L);});snapshot("solid-night-room.png");tapRoomCabinet();snapshot("solid-night-books.png");
   main(()->activity.collapseToScene(false));awaitScene(true);main(()->activity.lampSwitch.performClick());snapshot("solid-night-lamp-off.png");checks.append("PASS solid geometry and natural window preview rendered on native GPU\n");
  }catch(Throwable failure){code=Activity.RESULT_CANCELED;checks.append("FAIL ").append(failure).append('\n');try{snapshot("solid-preview-failure.png");}catch(Throwable ignored){}}
  finally{if(activity!=null){final java.util.Map<String,?> saved=originalPreferences;main(()->{if(saved!=null)new BackupChecks(this).prefs(saved);activity.finish();});}}
  result.putString("stream",checks.toString());finish(code,result);
 }
 @Override void awaitScene(boolean collapsed){awaitScene(collapsed,30000);}
 @Override void awaitScene(boolean collapsed,long timeout){
  // swangle can take 10s per submitted frame plus TextureView composition.
  // Still require the actual final GPU pose; never substitute timeline completion.
  try{super.awaitScene(collapsed,Math.max(30000,timeout));}catch(AssertionError failure){
   final String[] state={""};main(()->state[0]=activity.roomExperience.model.presentationState()+" progress="+activity.roomExperience.transition.progress()+" running="+activity.roomExperience.transition.running()+" collapsed="+activity.sceneCollapsed+" moving="+activity.sceneAnimating);
   try{android.graphics.Bitmap bitmap=getUiAutomation().takeScreenshot();if(bitmap!=null){java.io.File folder=new java.io.File(getTargetContext().getExternalFilesDir(null),"qa");folder.mkdirs();try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(folder,"3d-camera-timeout.png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}}catch(Exception ignored){}
   throw new AssertionError(failure.getMessage()+" "+state[0],failure);
  }
 }
 @Override void snapshot(String name){awaitScene(activity.sceneCollapsed);super.snapshot(name);}
 void resizeWindow(int w,int h){expectedWidth=w;expectedHeight=h;shell("wm size "+w+"x"+h);waitForIdleSync();awaitModel();}
 void awaitModel(){long end=SystemClock.uptimeMillis()+60000;while(SystemClock.uptimeMillis()<end){final boolean[] ready={false};main(()->{if(activity.roomExperience.model.failure()!=null)throw new AssertionError(activity.roomExperience.model.failure());if(!activity.isDestroyed()&&activity.roomExperience.model.isShown()&&activity.roomExperience.model.isAvailable()&&(expectedWidth==0||(activity.roomExperience.model.getWidth()==expectedWidth&&activity.roomExperience.model.getHeight()==expectedHeight))&&activity.roomExperience.model.ready()&&activity.roomExperience.model.presented()){android.graphics.Bitmap image=activity.roomExperience.model.getBitmap(64,128);if(image!=null){java.util.HashSet<Integer> colors=new java.util.HashSet<>();for(int y=12;y<116;y+=3)for(int x=3;x<61;x+=3)colors.add(image.getPixel(x,y));image.recycle();ready[0]=colors.size()>60;}}});if(ready[0]){check(true,"actual room pixels are nonuniform, not an empty background");return;}SystemClock.sleep(200);}throw new AssertionError("Native model did not display geometry within 60 seconds; frames="+activity.roomExperience.model.frames()+", available="+activity.roomExperience.model.isAvailable()+", shown="+activity.roomExperience.model.isShown()+", size="+activity.roomExperience.model.getWidth()+"x"+activity.roomExperience.model.getHeight()+", alpha="+activity.roomExperience.model.getAlpha());}
}
