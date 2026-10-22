package com.sitebox.app;

import android.app.Instrumentation;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.json.*;
import java.io.File;
import java.io.FileOutputStream;

/** Device tests with no external test dependency. Restores original user preferences. */
public final class SmokeInstrumentation extends Instrumentation {
 volatile MainActivity activity;
 String backupSites,backupCategories,demoSites,backupBackground;
 boolean hadBackground,hadGestureTip,backupGestureTip,hadMotion,backupMotion;
 int backupShelf,backupBook;
 int recoveredPixelLauncherAnrs;
 StringBuilder checks=new StringBuilder();
 void check(boolean value,String name){if(!value)throw new AssertionError(name);checks.append("PASS ").append(name).append('\n');}
 void main(Runnable action){final Throwable[] failure={null};runOnMainSync(()->{try{action.run();}catch(Throwable error){failure[0]=error;}});waitForIdleSync();if(failure[0] instanceof Error)throw (Error)failure[0];if(failure[0] instanceof RuntimeException)throw (RuntimeException)failure[0];if(failure[0]!=null)throw new RuntimeException(failure[0]);}
 @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
 @Override public void onStart(){
  Bundle result=new Bundle();int code=Activity.RESULT_OK;
  try {
   activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
   SharedPreferences prefs=activity.getPreferences(0);backupSites=prefs.getString("sites","[]");backupCategories=prefs.getString("categories","[]");backupShelf=prefs.getInt("shelfTheme",0);backupBook=prefs.getInt("bookTheme",0);hadGestureTip=prefs.contains("gestureTipSeen");backupGestureTip=prefs.getBoolean("gestureTipSeen",false);hadBackground=prefs.contains("backgroundTheme");backupBackground=prefs.getString("backgroundTheme","classroom");
   hadMotion=prefs.contains("studyMotion");backupMotion=prefs.getBoolean("studyMotion",true);main(()->activity.setStudyMotion(true));
   snapshot("study-first-launch.png");
   main(()->{
    check(activity.getPreferences(0).getBoolean("studyThemeIntroduced",false),"theme 1 introduction recorded once");
    activity.applyBackgroundTheme("classroom");activity.sites=new JSONArray();activity.filter="";activity.page=0;
    for(int cell=0;cell<9;cell++)for(int k=0;k<(cell==0?8:3);k++){
     JSONObject item=new JSONObject();try{item.put("name",new String[]{"나의 공부","개발 노트","AI 연구","게임 자료","영상 모음","쇼핑 목록","함께 읽기","생활 기록","기타 사이트"}[cell]+" "+(k+1));item.put("url","https://example.com/"+cell+"/"+k);item.put("category",activity.categories[cell]);item.put("fav",k==0);item.put("sortOrder",k*1000L);}catch(Exception e){throw new RuntimeException(e);}activity.sites.put(item);
    }
    activity.save();activity.populate();
   });
   main(()->{
    check(activity.sites.length()==32,"fixture 32 books across 9 categories");
    BookshelfView shelf=activity.currentCabinet;
    ViewGroup firstCell=(ViewGroup)shelf.getChildAt(1);check(firstCell.getChildCount()==7,"seven actual book views fit one cubby");
    for(int i=0;i<7;i++){View book=firstCell.getChildAt(i);check(book instanceof BookArtworkView&&book.getRight()<=firstCell.getWidth(),"all seven spines stay inside cell");BookArtworkView art=(BookArtworkView)book;check(Math.abs(art.titleX-art.spine.centerX())<.01f&&art.paper.width()<art.spine.width()*.05f,"title centered on spine and paper side is thin");}
    check(shelf.getChildCount()==19,"9 live cells + 9 editable labels + furniture bitmap");
    int[] pos=new int[2];shelf.getLocationOnScreen(pos);int[] headerPos=new int[2];activity.header.getLocationOnScreen(headerPos);
    check(pos[1]>=headerPos[1]+activity.header.getHeight(),"cabinet starts below compact header");
    check(activity.headline.getHeight()==activity.dp(48),"header is one 48dp row");
    for(int i=1;i<activity.headline.getChildCount();i++){View action=activity.headline.getChildAt(i);check(action.getWidth()>=activity.dp(48)&&action.getHeight()>=activity.dp(48),"header action minimum touch target");}
    check(activity.search.getVisibility()==View.GONE&&!activity.searchExpanded,"search initially collapsed");
    check(Math.abs(shelf.getWidth()-activity.shelfScroll.getWidth()*.87f)<=1,"bookshelf occupies 87 percent of inset viewport");
    check(Math.abs(shelf.getLeft()-(activity.shelves.getWidth()-shelf.getRight()))<=1,"bookshelf has equal side margins");
    check(activity.pagingBar.getVisibility()==View.VISIBLE,"multiple pages show navigation");
    check(shelf.getChildAt(1).getHeight()==shelf.getChildAt(17).getHeight()||Math.abs(shelf.getChildAt(1).getHeight()-shelf.getChildAt(17).getHeight())<=1,"equal first/last row geometry");
    check(activity.pageSummary.getText().toString().contains("/ 2"),"eighth book starts second page after seven visible books");
   });
   snapshot("oak-classic.png");
   new BackupChecks(this).run();
   ambientScenes();
   final String fullFixture=activity.sites.toString();
   main(()->{try{JSONObject single=new JSONObject();single.put("name","naver");single.put("url","https://naver.com");single.put("category",activity.categories[5]);activity.sites=new JSONArray().put(single);activity.populate();}catch(Exception error){throw new RuntimeException(error);}});
   main(()->{check(activity.pagingBar.getVisibility()==View.GONE,"single page hides paging bar");check(activity.pageSummary.getText().toString().contains("1권"),"hidden page summary remains current");check(activity.shelves.getChildCount()==2,"populated shelf has no persistent bottom instructions");});
   snapshot("single-book-naver.png");main(()->{try{activity.sites=new JSONArray(fullFixture);activity.populate();}catch(Exception error){throw new RuntimeException(error);}});
   for(int s=0;s<3;s++)for(int b=0;b<3;b++){
    final int si=s,bi=b;main(()->{activity.shelfThemeIndex=si;activity.bookThemeIndex=bi;activity.save();activity.populate();});
    snapshot("theme-"+s+"-"+b+".png");
   }
   crudDialogs();
   nativeDrag();
   main(()->{
    check(activity.sites.optJSONObject(0).optString("category").equals(activity.categories[1]),"long-press native drag delivered cross-cell drop");
    check(activity.dragSource==null||activity.dragSource.getAlpha()==1f,"drag source opacity restored");
    int count=activity.sites.length();activity.moveBookTo(0,1,8);
    check(activity.sites.length()==count,"move does not lose books");
    check(activity.sites.optJSONObject(0).optString("category").equals(activity.categories[1]),"cross-category native-drop operation");
    activity.moveBookTo(0,1,0);check(activity.sites.optJSONObject(0).optLong("sortOrder")==0,"self-drop preserves order");
    activity.searchToggle.performClick();check(activity.search.getVisibility()==View.VISIBLE&&activity.search.getLayoutParams().height==activity.dp(48),"search expands to 48dp row");
    activity.search.setText("example.com/2/");activity.searchToggle.performClick();check(activity.filter.equals("example.com/2/"),"collapsing search retains query");activity.setSearchExpanded(true);check(activity.pageSummary.getText().toString().contains("3권"),"URL search");
    activity.search.setText(activity.categories[3]);check(activity.pageSummary.getText().toString().contains("3권"),"category search");
    activity.search.setText("개발 노트");check(activity.pageSummary.getText().toString().contains("3권"),"title search");
    activity.search.setText("");activity.shelfThemeIndex=0;activity.bookThemeIndex=0;activity.save();activity.populate();
    activity.load();check(activity.sites.length()==count,"preferences data reload");
    activity.shelfThemeIndex=1;activity.bookThemeIndex=2;activity.save();activity.shelfThemeIndex=0;activity.bookThemeIndex=0;activity.load();
    check(activity.shelfThemeIndex==1&&activity.bookThemeIndex==2,"independent nonmatching theme persistence");
    activity.shelfThemeIndex=0;activity.bookThemeIndex=0;activity.save();activity.populate();
    check(activity.assets.shelf(0)!=null&&activity.assets.book(2,3)!=null,"real raster assets decode");
   });
   main(()->{
    // Hidden books before matched books reproduce the review's filtered pagination defect.
    demoSites=activity.sites.toString();activity.sites=new JSONArray();
    for(int i=0;i<15;i++){JSONObject item=new JSONObject();try{item.put("name",i<7?"hidden "+i:"matched "+i);item.put("url","https://example.com/"+i);item.put("category",activity.categories[0]);item.put("sortOrder",i*1000L);}catch(Exception e){throw new RuntimeException(e);}activity.sites.put(item);}
    activity.search.setText("matched");activity.page=0;activity.populate();activity.moveBookTo(7,0,7);
    check(activity.page==0,"filtered self-drop keeps visible page");
    activity.moveBookTo(8,0,7);check(activity.page==0,"filtered reorder derives filtered page");
    activity.search.setText("");try{activity.sites=new JSONArray(demoSites);}catch(Exception e){throw new RuntimeException(e);}activity.save();activity.populate();
   });
   snapshot("final-home.png");
   main(()->activity.edit(-1));snapshot("add-dialog.png");clickText("취소");waitForIdleSync();
   collapsedSearchRotation();
   renameCategory();
   rotateScreens();
   projectedCabinetThemes();
   pinchCollapseRestore();
   backgroundCollection();
   main(()->{
    View root=((ViewGroup)activity.stage).getChildAt(1);View header=((ViewGroup)root).getChildAt(0);
    check(header.performLongClick()&&activity.backgroundOnly,"classroom-only reveal preserved");
    ((ViewGroup)activity.stage).getChildAt(2).performClick();check(!activity.backgroundOnly,"classroom reveal returns to controls");
    check(activity.assets.get("themes/missing.webp")==null,"missing artwork handled without crash");
   });
   android.os.Debug.MemoryInfo memory=new android.os.Debug.MemoryInfo();android.os.Debug.getMemoryInfo(memory);checks.append("MEMORY totalPssKiB=").append(memory.getTotalPss()).append("\n");
   checks.append("PASS all nine theme combinations rendered\n");
  }catch(Throwable failure){try{snapshot("failure.png");}catch(Throwable ignored){}code=Activity.RESULT_CANCELED;result.putString("failure",failure.toString());checks.append("FAIL ").append(failure);}
  finally{
   if(activity!=null&&backupSites!=null){main(()->{SharedPreferences.Editor restore=activity.getPreferences(0).edit().putString("sites",backupSites).putString("categories",backupCategories).putInt("shelfTheme",backupShelf).putInt("bookTheme",backupBook);if(hadBackground)restore.putString("backgroundTheme",backupBackground);else restore.remove("backgroundTheme");if(hadGestureTip)restore.putBoolean("gestureTipSeen",backupGestureTip);else restore.remove("gestureTipSeen");if(hadMotion)restore.putBoolean("studyMotion",backupMotion);else restore.remove("studyMotion");restore.commit();activity.finish();});}
  }
  checks.append("\nENV recoveredPixelLauncherAnrs=").append(recoveredPixelLauncherAnrs).append("\n");
  result.putString("stream",checks.toString());finish(code,result);
 }
 // Deterministic weather previews isolate native rendering from the live provider.
 void ambientScenes(){
  final String data=activity.sites.toString(),categories=new JSONArray(java.util.Arrays.asList(activity.categories)).toString();
  final String filter=activity.filter,room=activity.backgroundThemeId;
  final int shelf=activity.shelfThemeIndex,book=activity.bookThemeIndex,page=activity.page;
  final boolean motion=activity.motionEnabled,collapsed=activity.sceneCollapsed;
  final long noon=1700000000000L,hour=3600000L;
  final WeatherState clear=new WeatherState(23,3,0,0,noon,noon-6*hour,noon+6*hour,noon,true);
  try{
   main(()->{activity.setStudyMotion(true);activity.setAmbientPreview(clear,noon);activity.applyBackgroundTheme("study");activity.collapseToScene(false);});
   awaitScene(true);awaitStudyVideo("study portrait video");
   awaitDaylight(1);android.os.SystemClock.sleep(2000);awaitStudyVideo("study daylight video");
   main(()->check(!activity.studyLive.isNightVideo()&&activity.studyLive.isVideoVisible(),"study daylight decoder uses visible day video"));snapshot("ambient-day.png");
   assertAmbientLibrary(data,categories,filter,page,shelf,book);
   main(()->activity.setAmbientPreview(clear,noon+10*hour));awaitDaylight(0);
   android.os.SystemClock.sleep(2000);awaitStudyVideo("study night video");
   main(()->check(activity.studyLive.isNightVideo()&&activity.studyLive.isVideoVisible(),"study night decoder uses visible night video"));snapshot("ambient-night.png");
   assertAmbientLibrary(data,categories,filter,page,shelf,book);
   main(()->activity.setAmbientPreview(new WeatherState(15,12,63,95,noon,noon-6*hour,noon+6*hour,noon,true),noon));
   awaitDaylight(1);android.os.SystemClock.sleep(2000);snapshot("ambient-rain.png");
   assertAmbientLibrary(data,categories,filter,page,shelf,book);
   main(()->activity.setAmbientPreview(new WeatherState(-3,8,73,100,noon,noon-6*hour,noon+6*hour,noon,true),noon));
   android.os.SystemClock.sleep(2000);snapshot("ambient-snow.png");
   assertAmbientLibrary(data,categories,filter,page,shelf,book);
   // Instrumentation invokes the actual activity callbacks, balanced even on failure.
   try{
    main(()->callActivityOnPause(activity));android.os.SystemClock.sleep(400);
    final long[] paused={0};main(()->{check(!activity.studyLive.isPlaying(),"study activity pause releases decoder");paused[0]=activity.studyLive.renderedFrames();});
    android.os.SystemClock.sleep(300);main(()->check(activity.studyLive.renderedFrames()==paused[0],"study activity pause stops video frames"));
   }finally{main(()->callActivityOnResume(activity));}
   awaitStudyVideo("study activity resume video");
   main(()->activity.setStudyMotion(false));android.os.SystemClock.sleep(400);
   final long[] stopped={0};main(()->{check(!activity.studyLive.isPlaying(),"study motion off releases decoder");stopped[0]=activity.studyLive.renderedFrames();});
   android.os.SystemClock.sleep(350);main(()->check(activity.studyLive.renderedFrames()==stopped[0],"study motion off freezes actual video frames"));
   main(()->activity.setStudyMotion(true));awaitStudyVideo("study motion restored video");
   main(()->activity.studyLive.failVideoForTest());android.os.SystemClock.sleep(300);
   main(()->check(!activity.studyLive.isPlaying()&&!activity.studyLive.hasVideoFrame()&&activity.backdrop.getDrawable()!=null,"study decoder error keeps static artwork fallback"));
   main(()->activity.applyBackgroundTheme("classroom"));
   main(()->check(activity.backgroundThemeId.equals("study")&&activity.studyLive.getVisibility()==View.VISIBLE,"retired room safely falls back to study"));
   main(()->activity.setStudyMotion(false));main(()->activity.setStudyMotion(true));awaitStudyVideo("study motion restart resets failed decoder");
   java.util.Calendar fallbackNoon=java.util.Calendar.getInstance();fallbackNoon.setTimeInMillis(noon);fallbackNoon.set(java.util.Calendar.HOUR_OF_DAY,12);
   final long fallbackTime=fallbackNoon.getTimeInMillis();
   main(()->activity.setAmbientPreview(WeatherState.empty(),fallbackTime));awaitDaylight(1);
   main(()->check(activity.studyLive.hasVideoFrame(),"missing weather retains offline day and video fallback"));
   assertAmbientLibrary(data,categories,filter,page,shelf,book);
   checks.append("PASS study live ambient and decoder lifecycle verified\n");
  }finally{
   main(()->{activity.clearAmbientPreview();activity.setStudyMotion(motion);activity.applyBackgroundTheme(room);activity.shelfThemeIndex=shelf;activity.bookThemeIndex=book;try{activity.sites=new JSONArray(data);}catch(JSONException error){throw new RuntimeException(error);}activity.search.setText(filter);activity.page=page;activity.save();activity.populate();if(collapsed)activity.collapseToScene(false);else if(activity.sceneCollapsed)activity.expandFromScene(false);});
  }
 }
 void assertAmbientLibrary(String data,String categories,String filter,int page,int shelf,int book){
  main(()->{
   check(activity.sites.toString().equals(data)&&new JSONArray(java.util.Arrays.asList(activity.categories)).toString().equals(categories),"study ambient effects preserve bookmarks and categories");
   check(activity.filter.equals(filter)&&activity.page==page&&activity.shelfThemeIndex==shelf&&activity.bookThemeIndex==book,"study ambient effects preserve filter page and independent styles");
  });
 }
 void awaitDaylight(float expected){
  final float[] daylight={Float.NaN};final long[] times={0,0};final String[] status={""};final Throwable[] failure={null};long startedAt=android.os.SystemClock.uptimeMillis(),deadline=startedAt+1300;
  do{
   times[0]=android.os.SystemClock.uptimeMillis();
   // Read live animation state without waiting for global UI idle after the observation.
   runOnMainSync(()->{try{times[1]=android.os.SystemClock.uptimeMillis();daylight[0]=activity.studyLive.getDaylight();status[0]=activity.studyLive.videoStatusForTest();}catch(Throwable error){failure[0]=error;}});
   if(failure[0] instanceof Error)throw (Error)failure[0];if(failure[0] instanceof RuntimeException)throw (RuntimeException)failure[0];if(failure[0]!=null)throw new RuntimeException(failure[0]);
   if(times[1]<=deadline&&Math.abs(daylight[0]-expected)<.05f){check(true,"study daylight settles to "+expected+" within 1.3 seconds");return;}
   android.os.SystemClock.sleep(25);
  }while(android.os.SystemClock.uptimeMillis()<deadline);
  throw new AssertionError("study daylight transition exceeded 1.3 seconds: expected="+expected+",lastValue="+daylight[0]+",startedAt="+startedAt+",requestedAt="+times[0]+",observedAt="+times[1]+",deadline="+deadline+",reportedAt="+android.os.SystemClock.uptimeMillis()+",status={"+status[0]+"}");
 }
 static final class VideoObservation {
  long requestedAt,observedAt,frames;boolean playing,hasFrame;String status;
  @Override public String toString(){return "requestedAt="+requestedAt+",observedAt="+observedAt+",frames="+frames+",playing="+playing+",hasFrame="+hasFrame+",status={"+status+"}";}
 }
 VideoObservation observeStudyVideo(){
  VideoObservation sample=new VideoObservation();sample.requestedAt=android.os.SystemClock.uptimeMillis();final Throwable[] failure={null};
  // Do not wait for global UI idle after sampling: animation work can make an observation stale.
  runOnMainSync(()->{try{sample.observedAt=android.os.SystemClock.uptimeMillis();sample.frames=activity.studyLive.renderedFrames();sample.playing=activity.studyLive.isPlaying();sample.hasFrame=activity.studyLive.hasVideoFrame();sample.status=activity.studyLive.videoStatusForTest();}catch(Throwable error){failure[0]=error;}});
  if(failure[0] instanceof Error)throw (Error)failure[0];if(failure[0] instanceof RuntimeException)throw (RuntimeException)failure[0];if(failure[0]!=null)throw new RuntimeException(failure[0]);return sample;
 }
 void awaitStudyVideo(String label){
  VideoObservation initial=observeStudyVideo(),observed=initial;long deadline=initial.observedAt+12000;boolean ready=false;
  // Preparing/first display can briefly stall under emulator decoder startup load.
  // Require sustained current-decoder texture updates before testing steady progress.
  do{observed=observeStudyVideo();ready=observed.observedAt<=deadline&&observed.playing&&observed.hasFrame&&observed.frames>=initial.frames+3;if(ready)break;android.os.SystemClock.sleep(50);}while(android.os.SystemClock.uptimeMillis()<deadline);
  if(!ready)throw new AssertionError(label+" no sustained native frames: baseline={"+initial+"},last={"+observed+"},deadline="+deadline+",reportedAt="+android.os.SystemClock.uptimeMillis());
  check(true,label+" prepared and rendered first native video frame");
  VideoObservation baseline=observeStudyVideo();long progressDeadline=baseline.observedAt+1000;boolean advancing=false;
  do{android.os.SystemClock.sleep(50);observed=observeStudyVideo();advancing=observed.observedAt<=progressDeadline&&observed.playing&&observed.frames>=baseline.frames+2;if(advancing)break;}while(android.os.SystemClock.uptimeMillis()<progressDeadline);
  if(!advancing)throw new AssertionError(label+" steady progress stalled: baseline={"+baseline+"},last={"+observed+"},deadline="+progressDeadline+",reportedAt="+android.os.SystemClock.uptimeMillis());
  check(true,label+" decoder advances at least two actual video frames within one second");
 }
 void projectedCabinetThemes(){
  final int shelf=activity.shelfThemeIndex,book=activity.bookThemeIndex,page=activity.page;
  final String data=activity.sites.toString(),filter=activity.filter,room=activity.backgroundThemeId;
  try{
   for(int index=0;index<ThemeAssets.SHELVES.length;index++){
    final int selected=index;
    main(()->{if(activity.sceneCollapsed)activity.expandFromScene(false);activity.shelfThemeIndex=selected;activity.save();activity.populate();});
    main(()->activity.collapseToScene(false));awaitScene(true);
    main(()->{
     checkProjection(SceneTheme.find(room));
     check(activity.getPreferences(0).getInt("shelfTheme",-1)==selected,"projected cabinet finish persists "+selected);
     check(activity.bookThemeIndex==book&&activity.backgroundThemeId.equals(room),"projected cabinet finish independent of book and room "+selected);
     check(activity.sites.toString().equals(data)&&activity.filter.equals(filter)&&activity.page==page,"projected cabinet finish preserves live library "+selected);
    });
    snapshot("projected-cabinet-"+ThemeAssets.SHELVES[index]+".png");
    main(()->{activity.expandFromScene(false);float[] identity=new float[8];activity.miniature.copyRenderedCorners(identity);check(activity.miniature.getProjectionFraction()==0&&Math.abs(identity[0])<.1f&&Math.abs(identity[2]-activity.miniature.getWidth())<.1f,"projected finish restores identity "+selected);check(activity.currentCabinet.getScaleX()==1&&activity.currentCabinet.getScaleY()==1&&activity.currentCabinet.getRotation()==0,"expanded cabinet remains frontal "+selected);});
   }
  }finally{main(()->{if(activity.sceneCollapsed)activity.expandFromScene(false);activity.shelfThemeIndex=shelf;activity.bookThemeIndex=book;activity.save();activity.populate();});}
 }
 void backgroundCollection(){
  final String data=activity.sites.toString();final int shelf=activity.shelfThemeIndex,book=activity.bookThemeIndex;
  main(()->{activity.search.setText("example.com/");activity.page=1;activity.populate();});
  final int page=activity.page;final String filter=activity.filter;
  check(SceneTheme.ALL.length==1,"only the study background remains");
  for(SceneTheme theme:SceneTheme.ALL){
   main(()->activity.showBackgroundThemes());
   clickBackgroundChoice(theme.name);waitForIdleSync();
   if(theme.id.equals("classroom")||theme.id.equals("train"))snapshot("background-dialog-"+theme.id+".png");
   clickText("닫기");waitForIdleSync();
   main(()->{
    check(activity.backgroundThemeId.equals(theme.id),theme.id+" selected through native background dialog");
    check(activity.getPreferences(0).getString(SceneTheme.PREFERENCE_KEY,"").equals(theme.id),theme.id+" selection persisted immediately");
    check(activity.shelfThemeIndex==shelf&&activity.bookThemeIndex==book,theme.id+" background independent of cabinet and book themes");
    check(activity.sites.toString().equals(data)&&activity.page==page&&activity.filter.equals(filter),theme.id+" selection preserves data page and filter");
    activity.backgroundThemeId="not-loaded";activity.load();
    check(activity.backgroundThemeId.equals(theme.id),theme.id+" selection survives settings reload");
    activity.collapseToScene(false);
   });
   awaitScene(true);
   main(()->checkSelectedScene(theme));snapshot("scene-"+theme.id+".png");
   // Exercise the same native tap target for every scene, not only the default classroom.
   clickDescription("작아진 책장 열기");awaitScene(false);
   main(()->{
    check(activity.controls.getVisibility()==View.VISIBLE&&activity.miniature.getVisibility()==View.GONE,theme.id+" miniature tap restores library");
    check(activity.sites.toString().equals(data)&&activity.page==page&&activity.filter.equals(filter),theme.id+" collapse and tap preserve library state");
   });
   snapshot("scene-"+theme.id+"-expanded.png");rotateBackground(theme);
  }
  main(()->{activity.applyBackgroundTheme("classroom");activity.search.setText("");activity.page=0;activity.populate();});
 }
 void clickBackgroundChoice(String name){
  int targetIndex=0,currentIndex=0;for(int i=0;i<SceneTheme.ALL.length;i++){if(SceneTheme.ALL[i].name.equals(name))targetIndex=i;if(SceneTheme.ALL[i].id.equals(activity.backgroundThemeId))currentIndex=i;}
  final int scrollAction=targetIndex<currentIndex?android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD:android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD;
  long deadline=android.os.SystemClock.uptimeMillis()+5000;
  do {
   waitForIdleSync();android.view.accessibility.AccessibilityNodeInfo root=activeAppWindow();
   if(root!=null)for(android.view.accessibility.AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText(name)){
    if(name.contentEquals(node.getText()==null?"":node.getText())){android.view.accessibility.AccessibilityNodeInfo target=node;while(target!=null){if(target.isClickable()&&target.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))return;target=target.getParent();}}
   }
   scrollDialog(root,scrollAction);android.os.SystemClock.sleep(50);
  }while(android.os.SystemClock.uptimeMillis()<deadline);
  throw new AssertionError("background dialog choice: "+name);
 }
 boolean scrollDialog(android.view.accessibility.AccessibilityNodeInfo node,int action){
  if(node==null)return false;
  if(node.isScrollable()&&node.performAction(action))return true;
  for(int i=0;i<node.getChildCount();i++)if(scrollDialog(node.getChild(i),action))return true;
  return false;
 }
 void checkSelectedScene(SceneTheme theme){
  boolean wide=BookLayout.wideScene(activity.stage.getWidth(),activity.stage.getHeight());
  Bitmap expected=activity.assets.get(theme.assetPath(wide));
  check(expected!=null&&activity.sceneBackdrop==expected,theme.id+" correct selected artwork decoded without fallback");
  check(((android.graphics.drawable.BitmapDrawable)activity.backdrop.getDrawable()).getBitmap()==expected,theme.id+" selected artwork displayed");
  check(activity.backgroundThemeId.equals(theme.id)&&activity.sceneCollapsed&&!activity.sceneAnimating,theme.id+" scene state remains selected");
  checkProjection(theme);
  check(activity.assets.get(theme.assetPath(wide))==expected,theme.id+" current room bitmap reused");
  check(activity.assets.cachedRoomBytes()<=12*1024*1024&&activity.assets.cachedArtworkBytes()<=12*1024*1024,theme.id+" room and artwork caches bounded independently");
  if(theme.id.equals("classroom")){checkRoomGrounding();return;}
  float x=activity.miniature.getX(),y=activity.miniature.getY(),w=activity.miniature.getWidth()*activity.miniature.getScaleX(),h=activity.miniature.getHeight()*activity.miniature.getScaleY();
  float vw=activity.stage.getWidth(),vh=activity.stage.getHeight();
  check(w>vw*.12f&&h>vh*.12f&&x>=0&&y>=0&&x+w<=vw+1&&y+h<=vh+1,theme.id+" readable compact cabinet fits viewport");
  // Read the rendered ImageView matrix, rather than calling the geometry method under test.
  float[] sourceAnchor={expected.getWidth()*theme.anchorX(wide),expected.getHeight()*theme.anchorFeet(wide)};
  activity.backdrop.getImageMatrix().mapPoints(sourceAnchor);
  check(Math.abs(y+h-sourceAnchor[1])<vh*.045f,theme.id+" cabinet feet contact the visible artwork floor anchor");
  check(Math.abs(x+w*.5f-sourceAnchor[0])<vw*.045f,theme.id+" cabinet aligns with reserved left or right artwork space");
  check(y>=0&&y+h>vh*.45f&&y+h<vh*.98f,theme.id+" cabinet floor pose remains visible for room camera angle");
  check(activity.sceneShadow.getVisibility()==View.VISIBLE&&activity.sceneShadow.getAlpha()>.99f,theme.id+" visible contact shadow");
 }
 void checkProjection(SceneTheme theme){
  ProjectedCabinetView view=activity.miniature;
  check(view.room()==SceneProjection.find(theme.id,activity.sceneBackdrop.getWidth()>activity.sceneBackdrop.getHeight()),theme.id+" room projection profile applied");
  check(view.sideTexture()!=activity.assets.shelf(activity.shelfThemeIndex),theme.id+" dedicated full-grain surface loaded without fallback");
  check(view.sideTexture()==activity.assets.surface(activity.shelfThemeIndex),theme.id+" physical side uses current theme cabinet bitmap");
  check(Math.abs(view.getProjectionFraction()-1)<.0001f,theme.id+" projection animation settled");
  float[] actual=new float[8],expected=new float[8];view.copyRenderedCorners(actual);view.expectedCorners(expected);
  for(int i=0;i<8;i++)check(Math.abs(actual[i]-expected[i])<.1f,theme.id+" rendered homography corner "+i);
  check(Math.max(actual[5],actual[7])>=view.getHeight()-.1f,theme.id+" projected feet retain floor contact");
  check(Math.abs(actual[0]-actual[6])<.1f&&Math.abs(actual[2]-actual[4])<.1f,theme.id+" cabinet uprights remain vertical");
  check(actual[5]>actual[7]&&view.topVisible(),theme.id+" front follows room floor slope and top is visible below horizon");
  try{
   JSONObject pose=new JSONObject();JSONArray corners=new JSONArray();int[] stageLocation=new int[2];activity.stage.getLocationOnScreen(stageLocation);
   for(int i=0;i<8;i++)corners.put((i%2==0?stageLocation[0]+view.getX():stageLocation[1]+view.getY())+actual[i]*(i%2==0?view.getScaleX():view.getScaleY()));
   pose.put("front",corners);pose.put("depth",view.room().depth);pose.put("sideVisible",view.sideVisible());pose.put("topVisible",view.topVisible());
   float[] side=new float[8],roof=new float[8];view.copyRenderedBodyCorners(side,roof);
   float[] camera=new float[2];view.roomCamera(camera);float k=(roof[0]-camera[0])/(actual[0]-camera[0]);
   check(Math.abs(roof[0]-(camera[0]+k*(actual[0]-camera[0])))<.1f&&Math.abs(roof[1]-(camera[1]+k*(actual[1]-camera[1])))<.1f,theme.id+" rear rays use fixed cropped room camera");
   float[] contacts=new float[4],floor=new float[8];activity.sceneShadow.contactCenters(contacts);view.groundedFootprint(floor);
   check(Math.abs(contacts[0]-(view.getX()+floor[6]*view.getScaleX()))<1&&Math.abs(contacts[1]-(view.getX()+floor[4]*view.getScaleX()))<1&&Math.abs(contacts[2]-(view.getY()+floor[7]*view.getScaleY()))<1&&Math.abs(contacts[3]-(view.getY()+floor[5]*view.getScaleY()))<1,theme.id+" contact shadows align with opaque sprite soles");
   for(int face=0;face<2;face++){float[] points=face==0?side:roof;JSONArray coordinates=new JSONArray();for(int i=0;i<8;i++)coordinates.put((i%2==0?stageLocation[0]+view.getX():stageLocation[1]+view.getY())+points[i]*(i%2==0?view.getScaleX():view.getScaleY()));pose.put(face==0?"side":"top",coordinates);}
   File directory=new File(getTargetContext().getExternalFilesDir(null),"qa");directory.mkdirs();
   String orientation=activity.getResources().getConfiguration().orientation==2?"landscape":"portrait";
   try(FileOutputStream out=new FileOutputStream(new File(directory,"projection-"+theme.id+"-"+orientation+".json"))){out.write(pose.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
  }catch(Exception e){throw new RuntimeException(e);}
 }
 void rotateBackground(SceneTheme theme){
  final String data=activity.sites.toString(),filter=activity.filter;final int page=activity.page;
  android.app.Application application=activity.getApplication();
  android.app.Application.ActivityLifecycleCallbacks listener=new android.app.Application.ActivityLifecycleCallbacks(){
   public void onActivityCreated(Activity a,Bundle b){}public void onActivityStarted(Activity a){}
   public void onActivityResumed(Activity a){if(a instanceof MainActivity)activity=(MainActivity)a;}
   public void onActivityPaused(Activity a){}public void onActivityStopped(Activity a){}
   public void onActivitySaveInstanceState(Activity a,Bundle b){}public void onActivityDestroyed(Activity a){}
  };
  application.registerActivityLifecycleCallbacks(listener);
  try{
   main(()->activity.collapseToScene(false));awaitScene(true);
   for(int orientation:new int[]{android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}){
    MainActivity before=activity;main(()->activity.setRequestedOrientation(orientation));
    long deadline=android.os.SystemClock.uptimeMillis()+8000;
    while(activity==before&&android.os.SystemClock.uptimeMillis()<deadline)android.os.SystemClock.sleep(50);
    check(activity!=before,theme.id+" rotation recreated activity");awaitScene(true);
    if(theme.id.equals("study")){
     main(()->activity.setStudyMotion(true));awaitStudyVideo("study rotated video");
     main(()->check(!before.studyLive.isPlaying(),"study destroyed activity released decoder"));
    }
    main(()->{
     int expectedOrientation=orientation==android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE?android.content.res.Configuration.ORIENTATION_LANDSCAPE:android.content.res.Configuration.ORIENTATION_PORTRAIT;
     check(activity.getResources().getConfiguration().orientation==expectedOrientation,theme.id+" requested orientation active");
     checkSelectedScene(theme);
     check(activity.sites.toString().equals(data)&&activity.filter.equals(filter)&&activity.page==page,theme.id+" rotation preserves library state");
    });
    snapshot("scene-"+theme.id+(orientation==android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE?"-landscape.png":"-portrait.png"));
   }
   clickDescription("작아진 책장 열기");awaitScene(false);
  }finally{application.unregisterActivityLifecycleCallbacks(listener);}
 }
 void renameCategory(){
  String original=activity.categories[0];main(()->activity.manageCategories());android.os.SystemClock.sleep(300);
  java.util.ArrayList<android.view.accessibility.AccessibilityNodeInfo> fields=new java.util.ArrayList<>();collectEdits(activeAppWindow(),fields);
  check(fields.size()==9,"nine editable category fields");setText(fields.get(0),"학습");clickText("저장");waitForIdleSync();
  main(()->{check(activity.categories[0].equals("학습"),"category name edited");for(int i=0;i<activity.sites.length();i++)check(!activity.sites.optJSONObject(i).optString("category").equals(original),"book category references renamed");});
 }
 void collapsedSearchRotation(){
  final String originalSites=activity.sites.toString();
  android.app.Application application=activity.getApplication();
  android.app.Application.ActivityLifecycleCallbacks listener=new android.app.Application.ActivityLifecycleCallbacks(){
   public void onActivityCreated(Activity a,Bundle b){}public void onActivityStarted(Activity a){}
   public void onActivityResumed(Activity a){if(a instanceof MainActivity)activity=(MainActivity)a;}
   public void onActivityPaused(Activity a){}public void onActivityStopped(Activity a){}
   public void onActivitySaveInstanceState(Activity a,Bundle b){}public void onActivityDestroyed(Activity a){}
  };
  application.registerActivityLifecycleCallbacks(listener);
  try{
   main(()->{
    activity.sites=new JSONArray();
    for(int i=0;i<8;i++){JSONObject item=new JSONObject();try{item.put("name","rotation match "+i);item.put("url","https://example.com/rotation/"+i);item.put("category",activity.categories[0]);}catch(Exception e){throw new RuntimeException(e);}activity.sites.put(item);}
    activity.save();activity.search.setText("rotation match");activity.setSearchExpanded(false);activity.page=1;activity.populate();
   });
   for(int orientation:new int[]{android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}){
    MainActivity before=activity;main(()->activity.setRequestedOrientation(orientation));
    long deadline=android.os.SystemClock.uptimeMillis()+8000;
    while(activity==before&&android.os.SystemClock.uptimeMillis()<deadline)android.os.SystemClock.sleep(50);
    check(activity!=before,"collapsed search rotation recreates activity");waitForIdleSync();
    main(()->{
     check(activity.filter.equals("rotation match")&&activity.page==1,"collapsed query and second page survive rotation");
     check(!activity.searchExpanded&&activity.search.getVisibility()==View.GONE,"collapsed search stays collapsed after rotation");
     check(activity.searchToggle.getText().toString().equals("검색 중"),"collapsed search has active filter indicator");
     check(activity.searchToggle.getContentDescription().toString().contains("rotation match"),"search action announces current query");
     check(activity.pageSummary.getText().toString().contains("2 / 2"),"second filtered page remains selected");
    });
   }
   main(()->{activity.search.setText("");check(activity.searchToggle.getText().toString().equals("검색"),"clearing filter resets search indicator");check(activity.searchToggle.getContentDescription().toString().equals("검색 펼치기"),"clearing filter resets search description");});
  }finally{
   application.unregisterActivityLifecycleCallbacks(listener);
   main(()->{try{activity.sites=new JSONArray(originalSites);}catch(Exception e){throw new RuntimeException(e);}activity.search.setText("");activity.page=0;activity.save();activity.populate();});
  }
 }
 void rotateScreens(){
  final boolean originallyCollapsed=activity.sceneCollapsed;
  android.app.Application.ActivityLifecycleCallbacks listener=new android.app.Application.ActivityLifecycleCallbacks(){
   public void onActivityCreated(Activity a,Bundle b){} public void onActivityStarted(Activity a){}
   public void onActivityResumed(Activity a){if(a instanceof MainActivity)activity=(MainActivity)a;}
   public void onActivityPaused(Activity a){} public void onActivityStopped(Activity a){}
   public void onActivitySaveInstanceState(Activity a,Bundle b){} public void onActivityDestroyed(Activity a){}
  };
  activity.getApplication().registerActivityLifecycleCallbacks(listener);
  try {
   final MainActivity before=activity;main(()->{activity.setSearchExpanded(true);activity.search.setText("개발");activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);});
   for(int i=0;i<60&&activity==before;i++)android.os.SystemClock.sleep(100);waitForIdleSync();
   main(()->{check(activity.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE,"landscape rotation");check(activity.filter.equals("개발"),"filter survives Activity recreation");check(activity.searchExpanded&&activity.search.getVisibility()==View.VISIBLE,"expanded search survives rotation");check(Math.abs(activity.currentCabinet.getWidth()-activity.shelfScroll.getWidth()*.87f)<=1,"landscape shelf respects inset viewport width");if(originallyCollapsed){check(activity.sceneCollapsed,"collapsed cabinet survives landscape recreation");checkRoomGrounding();}});
   snapshot(originallyCollapsed?"scene-landscape.png":"landscape-top.png");main(()->activity.shelfScroll.fullScroll(View.FOCUS_DOWN));snapshot("landscape-bottom.png");
   MainActivity landscape=activity;main(()->activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));for(int i=0;i<60&&activity==landscape;i++)android.os.SystemClock.sleep(100);waitForIdleSync();
   main(()->{check(activity.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_PORTRAIT,"portrait restored");if(originallyCollapsed){check(activity.sceneCollapsed,"collapsed cabinet survives portrait recreation");checkRoomGrounding();}});if(originallyCollapsed)snapshot("scene-portrait.png");main(()->{if(originallyCollapsed)activity.expandFromScene(false);activity.search.setText("");activity.setSearchExpanded(false);});snapshot("portrait-restored.png");
  }finally{activity.getApplication().unregisterActivityLifecycleCallbacks(listener);}
 }
 void crudDialogs(){
  final int original=activity.sites.length();
  main(()->activity.edit(-1));android.os.SystemClock.sleep(300);
  java.util.ArrayList<android.view.accessibility.AccessibilityNodeInfo> fields=awaitEditFields(2);
  check(fields.size()==2,"add dialog title and URL fields");
  setText(fields.get(0),"검증 추가 책");setText(fields.get(1),"https://example.com/qa");clickText("저장");waitForIdleSync();
  main(()->check(activity.sites.length()==original+1,"bookmark add through real dialog"));
  main(()->activity.edit(original));android.os.SystemClock.sleep(300);fields=awaitEditFields(2);check(fields.size()==2,"edit dialog title and URL fields");setText(fields.get(0),"검증 수정 책");clickText("저장");waitForIdleSync();
  main(()->{check(activity.sites.optJSONObject(original).optString("name").equals("검증 수정 책"),"bookmark edit through real dialog");activity.showBook(original);});
  android.os.SystemClock.sleep(500);clickText("☆ 즐겨찾기");waitForIdleSync();
  main(()->check(activity.sites.optJSONObject(original).optBoolean("fav"),"favorite toggle through book info"));
  snapshot("book-preview.png");clickText("삭제");android.os.SystemClock.sleep(250);clickText("삭제");waitForIdleSync();
  main(()->check(activity.sites.length()==original,"bookmark delete through confirmation"));
 }
 // This is an emulator-only recovery for the observed external launcher ANR.
 // Unknown system dialogs and all SiteBox ANRs remain visible and fail normally.
 boolean systemDialogPackage(CharSequence name){return "android".contentEquals(name==null?"":name)||"com.android.systemui".contentEquals(name==null?"":name);}
 boolean dismissPixelLauncherAnr(android.view.accessibility.AccessibilityNodeInfo root){
  if(root==null||!systemDialogPackage(root.getPackageName()))return false;
  boolean exactTitle=false;
  for(android.view.accessibility.AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText("Pixel Launcher isn't responding")){
   if("Pixel Launcher isn't responding".contentEquals(node.getText()==null?"":node.getText())&&systemDialogPackage(node.getPackageName())){exactTitle=true;break;}
  }
  if(!exactTitle)return false;
  android.view.accessibility.AccessibilityNodeInfo close=null;
  for(android.view.accessibility.AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText("Close app")){
   if("Close app".contentEquals(node.getText()==null?"":node.getText())&&systemDialogPackage(node.getPackageName())){close=node;break;}
  }
  if(close==null)return false;
  checks.append("ENV observed exact system Pixel Launcher ANR; preserving screenshot before Close app\n");
  snapshot("external-launcher-anr-"+(recoveredPixelLauncherAnrs+1)+".png");
  for(android.view.accessibility.AccessibilityNodeInfo target=close;target!=null;target=target.getParent()){
   if(!systemDialogPackage(target.getPackageName()))break;
   if(target.isClickable()&&target.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)){
    recoveredPixelLauncherAnrs++;checks.append("ENV recovered external Pixel Launcher ANR #").append(recoveredPixelLauncherAnrs).append(" via exact Close app action\n");return true;
   }
  }
  throw new AssertionError("Observed external Pixel Launcher ANR Close app action failed");
 }
 android.view.accessibility.AccessibilityNodeInfo activeAppWindow(){
  long deadline=android.os.SystemClock.uptimeMillis()+5000;
  do{
   android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();
   if(!dismissPixelLauncherAnr(root))return root;
   waitForIdleSync();android.os.SystemClock.sleep(50);
  }while(android.os.SystemClock.uptimeMillis()<deadline);
  throw new AssertionError("Repeated external Pixel Launcher ANRs exceeded 5 second recovery window");
 }
 java.util.ArrayList<android.view.accessibility.AccessibilityNodeInfo> awaitEditFields(int expected){
  long deadline=android.os.SystemClock.uptimeMillis()+5000;
  java.util.ArrayList<android.view.accessibility.AccessibilityNodeInfo> fields=new java.util.ArrayList<>();
  do{waitForIdleSync();fields.clear();collectEdits(activeAppWindow(),fields);if(fields.size()==expected)return fields;android.os.SystemClock.sleep(50);}while(android.os.SystemClock.uptimeMillis()<deadline);
  return fields;
 }
 void collectEdits(android.view.accessibility.AccessibilityNodeInfo node,java.util.List<android.view.accessibility.AccessibilityNodeInfo> found){if(node==null)return;if("android.widget.EditText".contentEquals(node.getClassName()))found.add(node);for(int i=0;i<node.getChildCount();i++)collectEdits(node.getChild(i),found);}
 void setText(android.view.accessibility.AccessibilityNodeInfo node,String text){Bundle args=new Bundle();args.putCharSequence(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text);check(node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT,args),"text field accepts edit");}
 void clickText(String text){
  long deadline=android.os.SystemClock.uptimeMillis()+5000;
  do {waitForIdleSync();android.view.accessibility.AccessibilityNodeInfo root=activeAppWindow();if(root!=null)for(android.view.accessibility.AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText(text)){
   if(text.contentEquals(node.getText()==null?"":node.getText())){android.view.accessibility.AccessibilityNodeInfo target=node;while(target!=null){if(target.isClickable()&&target.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))return;target=target.getParent();}}
  }android.os.SystemClock.sleep(50);}while(android.os.SystemClock.uptimeMillis()<deadline);
  throw new AssertionError("click target: "+text);
 }
 void awaitScene(boolean collapsed){
  long deadline=android.os.SystemClock.uptimeMillis()+5000;final boolean[] ready={false};
  do {main(()->ready[0]=activity.sceneCollapsed==collapsed&&!activity.sceneAnimating&&activity.stage.getWidth()>0&&activity.stage.getHeight()>0);if(ready[0])return;android.os.SystemClock.sleep(50);}while(android.os.SystemClock.uptimeMillis()<deadline);
  throw new AssertionError("scene transition timed out: collapsed="+collapsed);
 }
 void checkRoomGrounding(){checkSelectedScene(SceneTheme.find("study"));}

 void pinchCollapseRestore(){
  final int count=activity.sites.length(),oldPage=activity.page;final String oldFilter=activity.filter;
  pinch(false);awaitScene(false);
  main(()->check(!activity.sceneCollapsed,"outward pinch does not collapse cabinet"));
  pinch(true);awaitScene(true);
  main(()->{
   check(activity.sceneCollapsed&&!activity.sceneAnimating,"inward two-finger gesture collapses cabinet");
   checkProjection(SceneTheme.find(activity.backgroundThemeId));
   checkRoomGrounding();
   check(activity.controls.getVisibility()==View.INVISIBLE,"header and full-size library hidden in scene");
   check(activity.miniature.getVisibility()==View.VISIBLE&&activity.miniature.getScaleX()<.75f,"live cabinet snapshot shrinks into background");
   float x=activity.miniature.getX(),y=activity.miniature.getY();
   check(x>=0&&y>=0&&x+activity.miniature.getWidth()*activity.miniature.getScaleX()<=activity.stage.getWidth()+1&&y+activity.miniature.getHeight()*activity.miniature.getScaleY()<=activity.stage.getHeight()+1,"miniature remains inside screen");
   check(activity.sites.length()==count&&activity.page==oldPage&&activity.filter.equals(oldFilter),"pinch preserves bookmarks page and search");
  });
  snapshot("pinch-collapsed.png");measuredExpand();
  main(()->{check(!activity.sceneCollapsed&&!activity.sceneAnimating,"tap miniature restores full-size cabinet");float[] restored=new float[8];activity.miniature.copyRenderedCorners(restored);check(activity.miniature.getProjectionFraction()==0&&Math.abs(restored[0])<.1f&&Math.abs(restored[2]-activity.miniature.getWidth())<.1f,"expansion restores identity projection");check(activity.sceneShadow.getVisibility()==View.GONE,"contact shadow removed on expansion");check(activity.controls.getVisibility()==View.VISIBLE&&activity.miniature.getVisibility()==View.GONE,"full controls restored without overlay");});
  snapshot("pinch-restored.png");pinch(true);awaitScene(true);rotateScreens();
 }
 void clickDescription(String description){android.view.accessibility.AccessibilityNodeInfo root=activeAppWindow();android.view.accessibility.AccessibilityNodeInfo node=findDescription(root,description);if(node==null||!node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))throw new AssertionError("click description "+description);waitForIdleSync();}
 android.view.accessibility.AccessibilityNodeInfo findDescription(android.view.accessibility.AccessibilityNodeInfo root,String description){if(root==null)return null;if(root.getContentDescription()!=null&&description.contentEquals(root.getContentDescription()))return root;for(int i=0;i<root.getChildCount();i++){android.view.accessibility.AccessibilityNodeInfo found=findDescription(root.getChild(i),description);if(found!=null)return found;}return null;}
 String shell(String command){
  try(android.os.ParcelFileDescriptor descriptor=getUiAutomation().executeShellCommand(command);
      java.io.InputStream input=new android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor);
      java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream()){
   byte[] buffer=new byte[256];int count;while((count=input.read(buffer))!=-1)output.write(buffer,0,count);
   return output.toString("UTF-8").trim();
  }catch(Exception error){throw new RuntimeException(error);}
 }
 void awaitAnimatorScaleOne(){
  if(android.os.Build.VERSION.SDK_INT<26)return;
  final boolean[] ready={false};long deadline=android.os.SystemClock.uptimeMillis()+2000;
  do{runOnMainSync(()->ready[0]=Math.abs(android.animation.ValueAnimator.getDurationScale()-1)<.001f);if(ready[0])return;android.os.SystemClock.sleep(20);}while(android.os.SystemClock.uptimeMillis()<deadline);
  throw new AssertionError("Animator scale setting did not reach app process");
 }
 void measuredExpand(){
  String previous=shell("settings get global animator_duration_scale");
  if(!previous.equals("null")&&!previous.matches("[0-9]+(\\.[0-9]+)?"))throw new AssertionError("Unexpected animator duration scale: "+previous);
  final boolean[] intermediate={false};final float[] last={1};
  android.view.ViewTreeObserver.OnPreDrawListener observer=()->{float fraction=activity.miniature.getProjectionFraction();if(activity.sceneAnimating&&fraction>0&&fraction<1&&fraction<last[0])intermediate[0]=true;last[0]=fraction;return true;};
  try{
   shell("settings put global animator_duration_scale 1");awaitAnimatorScaleOne();
   check(shell("settings get global animator_duration_scale").equals("1"),"temporarily enable measured reverse projection animation");
   main(()->activity.stage.getViewTreeObserver().addOnPreDrawListener(observer));
   clickDescription("작아진 책장 열기");awaitScene(false);
   main(()->{check(intermediate[0],"native accessible tap animates decreasing intermediate projection");check(activity.miniature.getProjectionFraction()==0,"native accessible tap settles projection at identity");});
  }finally{
   main(()->{if(activity.stage.getViewTreeObserver().isAlive())activity.stage.getViewTreeObserver().removeOnPreDrawListener(observer);});
   shell(previous.equals("null")?"settings delete global animator_duration_scale":"settings put global animator_duration_scale "+previous);
  }
 }
 void pinch(boolean inward){
  boolean measure=inward&&!activity.sceneCollapsed;
  if(!measure){pinchMotion(inward);return;}
  String previous=shell("settings get global animator_duration_scale");
  if(!previous.equals("null")&&!previous.matches("[0-9]+(\\.[0-9]+)?"))throw new AssertionError("Unexpected animator duration scale: "+previous);
  final boolean[] intermediate={false};final float[] last={0};final int[] renderedIntermediateFrames={0};
  android.view.ViewTreeObserver.OnPreDrawListener observer=()->{
   float fraction=activity.miniature.getProjectionFraction();
   if(activity.sceneCollapsed&&activity.sceneAnimating&&fraction>0&&fraction<1){renderedIntermediateFrames[0]++;if(fraction>last[0])intermediate[0]=true;}
   last[0]=fraction;return true;
  };
  try{
   shell("settings put global animator_duration_scale 1");awaitAnimatorScaleOne();
   check(shell("settings get global animator_duration_scale").equals("1"),"temporarily enable measured native projection animation");
   main(()->activity.stage.getViewTreeObserver().addOnPreDrawListener(observer));
   pinchMotion(inward);awaitScene(true);
   main(()->{
    checks.append("ENV forward projection intermediate draw frames=").append(renderedIntermediateFrames[0]).append("\n");
    check(intermediate[0],"native pinch animates continuous intermediate projection");
    check(activity.miniature.getProjectionFraction()==1,"native pinch settles projection at room pose");
   });
  }finally{
   main(()->{if(activity.stage.getViewTreeObserver().isAlive())activity.stage.getViewTreeObserver().removeOnPreDrawListener(observer);});
   shell(previous.equals("null")?"settings delete global animator_duration_scale":"settings put global animator_duration_scale "+previous);
  }
 }
 void pinchMotion(boolean inward){
  float width=activity.stage.getWidth(),y=activity.stage.getHeight()*.54f;
  float start=inward?.06f:.38f,end=inward?.38f:.06f;
  long down=android.os.SystemClock.uptimeMillis();touch(down,android.view.MotionEvent.ACTION_DOWN,width*start,y);
  twoFinger(down,android.view.MotionEvent.ACTION_POINTER_DOWN|(1<<android.view.MotionEvent.ACTION_POINTER_INDEX_SHIFT),width*start,width*(1-start),y);
  for(int i=1;i<=24;i++){float f=start+(end-start)*i/24f;twoFinger(down,android.view.MotionEvent.ACTION_MOVE,width*f,width*(1-f),y);android.os.SystemClock.sleep(18);}
  twoFinger(down,android.view.MotionEvent.ACTION_POINTER_UP|(1<<android.view.MotionEvent.ACTION_POINTER_INDEX_SHIFT),width*end,width*(1-end),y);
  touch(down,android.view.MotionEvent.ACTION_UP,width*end,y);waitForIdleSync();
 }
 void twoFinger(long down,int action,float left,float right,float y){
  android.view.MotionEvent.PointerProperties[] properties={new android.view.MotionEvent.PointerProperties(),new android.view.MotionEvent.PointerProperties()};
  android.view.MotionEvent.PointerCoords[] coords={new android.view.MotionEvent.PointerCoords(),new android.view.MotionEvent.PointerCoords()};
  for(int i=0;i<2;i++){properties[i].id=i;properties[i].toolType=android.view.MotionEvent.TOOL_TYPE_FINGER;coords[i].x=i==0?left:right;coords[i].y=y;coords[i].pressure=1;coords[i].size=1;}
  android.view.MotionEvent event=android.view.MotionEvent.obtain(down,android.os.SystemClock.uptimeMillis(),action,2,properties,coords,0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);
  if(!getUiAutomation().injectInputEvent(event,true))throw new AssertionError("two-finger input injection");event.recycle();
 }
 void nativeDrag(){
  final int[] start=new int[2],end=new int[2];
  main(()->{
   BookshelfView furniture=activity.currentCabinet;
   View source=((ViewGroup)furniture.getChildAt(1)).getChildAt(0);
   View destination=((ViewGroup)furniture.getChildAt(3)).getChildAt(0);
   source.getLocationOnScreen(start);destination.getLocationOnScreen(end);
   start[0]+=source.getWidth()/2;start[1]+=source.getHeight()/2;
   end[0]+=destination.getWidth()/2;end[1]+=destination.getHeight()/2;
  });
  long down=android.os.SystemClock.uptimeMillis();
  touch(down,android.view.MotionEvent.ACTION_DOWN,start[0],start[1]);
  android.os.SystemClock.sleep(700);
  for(int i=1;i<=20;i++){touch(down,android.view.MotionEvent.ACTION_MOVE,start[0]+(end[0]-start[0])*i/20f,start[1]+(end[1]-start[1])*i/20f);android.os.SystemClock.sleep(30);}
  touch(down,android.view.MotionEvent.ACTION_UP,end[0],end[1]);android.os.SystemClock.sleep(350);waitForIdleSync();
 }
 void touch(long down,int action,float x,float y){android.view.MotionEvent event=android.view.MotionEvent.obtain(down,android.os.SystemClock.uptimeMillis(),action,x,y,0);event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);if(!getUiAutomation().injectInputEvent(event,true))throw new AssertionError("native touch injection");event.recycle();}
 void snapshot(String name){
  android.os.SystemClock.sleep(450);waitForIdleSync();
  java.util.concurrent.CountDownLatch drawn=new java.util.concurrent.CountDownLatch(1);
  main(()->{activity.stage.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){activity.stage.getViewTreeObserver().removeOnPreDrawListener(this);activity.stage.postOnAnimation(()->drawn.countDown());return true;}});activity.stage.invalidate();});
  try{if(!drawn.await(3,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("screenshot requires a fresh on-screen frame");}catch(InterruptedException interrupted){throw new RuntimeException(interrupted);}
  main(()->{check(!activity.sceneAnimating,"screenshot animation settled");if(activity.sceneCollapsed)check(activity.controls.getVisibility()==View.INVISIBLE&&activity.miniature.isShown()&&activity.miniature.getProjectionFraction()==1,"collapsed screenshot is real visible miniature with controls hidden");});
  try{
   Bitmap image=getUiAutomation().takeScreenshot();if(image==null)throw new IllegalStateException("screenshot unavailable");
   File directory=new File(getTargetContext().getExternalFilesDir(null),"qa");directory.mkdirs();
   try(FileOutputStream out=new FileOutputStream(new File(directory,name))){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();
  }catch(Exception e){throw new RuntimeException(e);}
 }
}
