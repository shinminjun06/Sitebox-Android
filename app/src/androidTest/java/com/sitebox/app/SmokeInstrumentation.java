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
 String backupSites,backupCategories,demoSites;
 int backupShelf,backupBook;
 StringBuilder checks=new StringBuilder();
 void check(boolean value,String name){if(!value)throw new AssertionError(name);checks.append("PASS ").append(name).append('\n');}
 void main(Runnable action){runOnMainSync(action);waitForIdleSync();}
 @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
 @Override public void onStart(){
  Bundle result=new Bundle();int code=Activity.RESULT_OK;
  try {
   activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
   SharedPreferences prefs=activity.getPreferences(0);backupSites=prefs.getString("sites","[]");backupCategories=prefs.getString("categories","[]");backupShelf=prefs.getInt("shelfTheme",0);backupBook=prefs.getInt("bookTheme",0);
   main(()->{
    activity.sites=new JSONArray();activity.filter="";activity.page=0;
    for(int cell=0;cell<9;cell++)for(int k=0;k<(cell==0?8:3);k++){
     JSONObject item=new JSONObject();try{item.put("name",new String[]{"나의 공부","개발 노트","AI 연구","게임 자료","영상 모음","쇼핑 목록","함께 읽기","생활 기록","기타 사이트"}[cell]+" "+(k+1));item.put("url","https://example.com/"+cell+"/"+k);item.put("category",activity.categories[cell]);item.put("fav",k==0);item.put("sortOrder",k*1000L);}catch(Exception e){throw new RuntimeException(e);}activity.sites.put(item);
    }
    activity.save();activity.populate();
   });
   main(()->{
    check(activity.sites.length()==32,"fixture 32 books across 9 categories");
    BookshelfView shelf=(BookshelfView)activity.shelves.getChildAt(1);
    ViewGroup firstCell=(ViewGroup)shelf.getChildAt(1);check(firstCell.getChildCount()==7,"seven actual book views fit one cubby");
    for(int i=0;i<7;i++){View book=firstCell.getChildAt(i);check(book instanceof BookArtworkView&&book.getRight()<=firstCell.getWidth(),"all seven spines stay inside cell");BookArtworkView art=(BookArtworkView)book;check(Math.abs(art.titleX-art.spine.centerX())<.01f&&art.paper.width()<art.spine.width()*.05f,"title centered on spine and paper side is thin");}
    check(shelf.getChildCount()==19,"9 live cells + 9 editable labels + furniture bitmap");
    int[] pos=new int[2];shelf.getLocationOnScreen(pos);int[] searchPos=new int[2];activity.search.getLocationOnScreen(searchPos);
    check(pos[1]>=searchPos[1]+activity.search.getHeight(),"cabinet starts below search");
    check(shelf.getChildAt(1).getHeight()==shelf.getChildAt(17).getHeight()||Math.abs(shelf.getChildAt(1).getHeight()-shelf.getChildAt(17).getHeight())<=1,"equal first/last row geometry");
    check(activity.pageSummary.getText().toString().contains("/ 2"),"eighth book starts second page after seven visible books");
   });
   snapshot("oak-classic.png");
   final String fullFixture=activity.sites.toString();
   main(()->{try{JSONObject single=new JSONObject();single.put("name","naver");single.put("url","https://naver.com");single.put("category",activity.categories[5]);activity.sites=new JSONArray().put(single);activity.populate();}catch(Exception error){throw new RuntimeException(error);}});
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
    activity.search.setText("example.com/2/");check(activity.pageSummary.getText().toString().contains("3권"),"URL search");
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
   renameCategory();
   rotateScreens();
   pinchCollapseRestore();
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
   if(activity!=null&&backupSites!=null){main(()->{activity.getPreferences(0).edit().putString("sites",backupSites).putString("categories",backupCategories).putInt("shelfTheme",backupShelf).putInt("bookTheme",backupBook).commit();activity.finish();});}
  }
  result.putString("stream",checks.toString());finish(code,result);
 }
 void renameCategory(){
  String original=activity.categories[0];main(()->activity.manageCategories());android.os.SystemClock.sleep(300);
  java.util.ArrayList<android.view.accessibility.AccessibilityNodeInfo> fields=new java.util.ArrayList<>();collectEdits(getUiAutomation().getRootInActiveWindow(),fields);
  check(fields.size()==9,"nine editable category fields");setText(fields.get(0),"학습");clickText("저장");waitForIdleSync();
  main(()->{check(activity.categories[0].equals("학습"),"category name edited");for(int i=0;i<activity.sites.length();i++)check(!activity.sites.optJSONObject(i).optString("category").equals(original),"book category references renamed");});
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
   final MainActivity before=activity;main(()->{activity.search.setText("개발");activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);});
   for(int i=0;i<60&&activity==before;i++)android.os.SystemClock.sleep(100);waitForIdleSync();
   main(()->{check(activity.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE,"landscape rotation");check(activity.filter.equals("개발"),"filter survives Activity recreation");if(originallyCollapsed)check(activity.sceneCollapsed,"collapsed cabinet survives landscape recreation");});
   snapshot("landscape-top.png");main(()->activity.shelfScroll.fullScroll(View.FOCUS_DOWN));snapshot("landscape-bottom.png");
   MainActivity landscape=activity;main(()->activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));for(int i=0;i<60&&activity==landscape;i++)android.os.SystemClock.sleep(100);waitForIdleSync();
   main(()->{check(activity.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_PORTRAIT,"portrait restored");if(originallyCollapsed){check(activity.sceneCollapsed,"collapsed cabinet survives portrait recreation");activity.expandFromScene(false);}activity.search.setText("");});snapshot("portrait-restored.png");
  }finally{activity.getApplication().unregisterActivityLifecycleCallbacks(listener);}
 }
 void crudDialogs(){
  final int original=activity.sites.length();
  main(()->activity.edit(-1));android.os.SystemClock.sleep(300);
  java.util.ArrayList<android.view.accessibility.AccessibilityNodeInfo> fields=new java.util.ArrayList<>();
  collectEdits(getUiAutomation().getRootInActiveWindow(),fields);
  check(fields.size()==2,"add dialog title and URL fields");
  setText(fields.get(0),"검증 추가 책");setText(fields.get(1),"https://example.com/qa");clickText("저장");waitForIdleSync();
  main(()->check(activity.sites.length()==original+1,"bookmark add through real dialog"));
  main(()->activity.edit(original));android.os.SystemClock.sleep(300);fields.clear();collectEdits(getUiAutomation().getRootInActiveWindow(),fields);setText(fields.get(0),"검증 수정 책");clickText("저장");waitForIdleSync();
  main(()->{check(activity.sites.optJSONObject(original).optString("name").equals("검증 수정 책"),"bookmark edit through real dialog");activity.showBook(original);});
  android.os.SystemClock.sleep(500);clickText("☆ 즐겨찾기");waitForIdleSync();
  main(()->check(activity.sites.optJSONObject(original).optBoolean("fav"),"favorite toggle through book info"));
  snapshot("book-preview.png");clickText("삭제");android.os.SystemClock.sleep(250);clickText("삭제");waitForIdleSync();
  main(()->check(activity.sites.length()==original,"bookmark delete through confirmation"));
 }
 void collectEdits(android.view.accessibility.AccessibilityNodeInfo node,java.util.List<android.view.accessibility.AccessibilityNodeInfo> found){if(node==null)return;if("android.widget.EditText".contentEquals(node.getClassName()))found.add(node);for(int i=0;i<node.getChildCount();i++)collectEdits(node.getChild(i),found);}
 void setText(android.view.accessibility.AccessibilityNodeInfo node,String text){Bundle args=new Bundle();args.putCharSequence(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text);check(node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT,args),"text field accepts edit");}
 void clickText(String text){android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();if(root!=null)for(android.view.accessibility.AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText(text)){if(text.contentEquals(node.getText())&&node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))return;}throw new AssertionError("click target: "+text);}
 void pinchCollapseRestore(){
  final int count=activity.sites.length(),oldPage=activity.page;final String oldFilter=activity.filter;
  pinch(false);android.os.SystemClock.sleep(500);waitForIdleSync();
  main(()->check(!activity.sceneCollapsed,"outward pinch does not collapse cabinet"));
  pinch(true);android.os.SystemClock.sleep(650);waitForIdleSync();
  main(()->{
   check(activity.sceneCollapsed&&!activity.sceneAnimating,"inward two-finger gesture collapses cabinet");
   check(activity.controls.getVisibility()==View.INVISIBLE,"header and full-size library hidden in scene");
   check(activity.miniature.getVisibility()==View.VISIBLE&&activity.miniature.getScaleX()<.75f,"live cabinet snapshot shrinks into background");
   float x=activity.miniature.getX(),y=activity.miniature.getY();
   check(x>=0&&y>=0&&x+activity.miniature.getWidth()*activity.miniature.getScaleX()<=activity.stage.getWidth()+1&&y+activity.miniature.getHeight()*activity.miniature.getScaleY()<=activity.stage.getHeight()+1,"miniature remains inside screen");
   check(activity.sites.length()==count&&activity.page==oldPage&&activity.filter.equals(oldFilter),"pinch preserves bookmarks page and search");
  });
  snapshot("pinch-collapsed.png");clickDescription("작아진 책장 열기");android.os.SystemClock.sleep(650);waitForIdleSync();
  main(()->{check(!activity.sceneCollapsed&&!activity.sceneAnimating,"tap miniature restores full-size cabinet");check(activity.controls.getVisibility()==View.VISIBLE&&activity.miniature.getVisibility()==View.GONE,"full controls restored without overlay");});
  snapshot("pinch-restored.png");pinch(true);android.os.SystemClock.sleep(650);waitForIdleSync();rotateScreens();
 }
 void clickDescription(String description){android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();android.view.accessibility.AccessibilityNodeInfo node=findDescription(root,description);if(node==null||!node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))throw new AssertionError("click description "+description);waitForIdleSync();}
 android.view.accessibility.AccessibilityNodeInfo findDescription(android.view.accessibility.AccessibilityNodeInfo root,String description){if(root==null)return null;if(root.getContentDescription()!=null&&description.contentEquals(root.getContentDescription()))return root;for(int i=0;i<root.getChildCount();i++){android.view.accessibility.AccessibilityNodeInfo found=findDescription(root.getChild(i),description);if(found!=null)return found;}return null;}
 void pinch(boolean inward){
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
   BookshelfView furniture=(BookshelfView)activity.shelves.getChildAt(1);
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
  try{
   Bitmap image=getUiAutomation().takeScreenshot();if(image==null)throw new IllegalStateException("screenshot unavailable");
   File directory=new File(getTargetContext().getExternalFilesDir(null),"qa");directory.mkdirs();
   try(FileOutputStream out=new FileOutputStream(new File(directory,name))){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();
  }catch(Exception e){throw new RuntimeException(e);}
 }
}
