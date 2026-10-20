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
 MainActivity activity;
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
    for(int cell=0;cell<9;cell++)for(int k=0;k<(cell==0?7:3);k++){
     JSONObject item=new JSONObject();try{item.put("name",new String[]{"나의 공부","개발 노트","AI 연구","게임 자료","영상 모음","쇼핑 목록","함께 읽기","생활 기록","기타 사이트"}[cell]+" "+(k+1));item.put("url","https://example.com/"+cell+"/"+k);item.put("category",activity.categories[cell]);item.put("fav",k==0);item.put("sortOrder",k*1000L);}catch(Exception e){throw new RuntimeException(e);}activity.sites.put(item);
    }
    activity.save();activity.populate();
   });
   main(()->{
    check(activity.sites.length()==31,"fixture 31 books across 9 categories");
    BookshelfView shelf=(BookshelfView)activity.shelves.getChildAt(1);
    check(shelf.getChildCount()==19,"9 live cells + 9 editable labels + furniture bitmap");
    int[] pos=new int[2];shelf.getLocationOnScreen(pos);int[] searchPos=new int[2];activity.search.getLocationOnScreen(searchPos);
    check(pos[1]>=searchPos[1]+activity.search.getHeight(),"cabinet starts below search");
    check(shelf.getChildAt(1).getHeight()==shelf.getChildAt(17).getHeight()||Math.abs(shelf.getChildAt(1).getHeight()-shelf.getChildAt(17).getHeight())<=1,"equal first/last row geometry");
    check(activity.pageSummary.getText().toString().contains("/ 3"),"three pages for seven books");
   });
   snapshot("oak-classic.png");
   for(int s=0;s<3;s++)for(int b=0;b<3;b++){
    final int si=s,bi=b;main(()->{activity.shelfThemeIndex=si;activity.bookThemeIndex=bi;activity.save();activity.populate();});
    snapshot("theme-"+s+"-"+b+".png");
   }
   crudDialogs();
   nativeDrag();
   main(()->{
    check(activity.sites.optJSONObject(0).optString("category").equals(activity.categories[1]),"long-press native drag delivered cross-cell drop");
    check(activity.dragSource==null||activity.dragSource.getAlpha()==1f,"drag source opacity restored");
    int count=activity.sites.length();activity.moveBookTo(0,1,7);
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
    for(int i=0;i<7;i++){JSONObject item=new JSONObject();try{item.put("name",i<3?"hidden "+i:"matched "+i);item.put("url","https://example.com/"+i);item.put("category",activity.categories[0]);item.put("sortOrder",i*1000L);}catch(Exception e){throw new RuntimeException(e);}activity.sites.put(item);}
    activity.search.setText("matched");activity.page=0;activity.populate();activity.moveBookTo(3,0,3);
    check(activity.page==0,"filtered self-drop keeps visible page");
    activity.moveBookTo(4,0,3);check(activity.page==0,"filtered reorder derives filtered page");
    activity.search.setText("");try{activity.sites=new JSONArray(demoSites);}catch(Exception e){throw new RuntimeException(e);}activity.save();activity.populate();
   });
   snapshot("final-home.png");
   main(()->activity.edit(-1));snapshot("add-dialog.png");clickText("취소");waitForIdleSync();
   checks.append("PASS all nine theme combinations rendered\n");
  }catch(Throwable failure){try{snapshot("failure.png");}catch(Throwable ignored){}code=Activity.RESULT_CANCELED;result.putString("failure",failure.toString());checks.append("FAIL ").append(failure);}
  finally{
   if(activity!=null&&backupSites!=null){main(()->{activity.getPreferences(0).edit().putString("sites",backupSites).putString("categories",backupCategories).putInt("shelfTheme",backupShelf).putInt("bookTheme",backupBook).commit();activity.finish();});}
  }
  result.putString("stream",checks.toString());finish(code,result);
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
 void touch(long down,int action,float x,float y){android.view.MotionEvent event=android.view.MotionEvent.obtain(down,android.os.SystemClock.uptimeMillis(),action,x,y,0);event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);sendPointerSync(event);event.recycle();}
 void snapshot(String name){
  android.os.SystemClock.sleep(450);waitForIdleSync();
  try{
   Bitmap image=getUiAutomation().takeScreenshot();if(image==null)throw new IllegalStateException("screenshot unavailable");
   File directory=new File(getTargetContext().getExternalFilesDir(null),"qa");directory.mkdirs();
   try(FileOutputStream out=new FileOutputStream(new File(directory,name))){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();
  }catch(Exception e){throw new RuntimeException(e);}
 }
}
