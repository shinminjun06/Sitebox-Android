package com.sitebox.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import java.io.InputStream;

/** Immutable artwork (12 MiB), backgrounds (12 MiB), thumbnails (2 MiB), no decode during onDraw. */
final class ThemeAssets {
 private final Context context;
 private final LruCache<String,Bitmap> cache=new LruCache<String,Bitmap>(12*1024*1024){
  @Override protected int sizeOf(String key,Bitmap bitmap){return bitmap.getAllocationByteCount();}
 };
 private final LruCache<String,Bitmap> rooms=new LruCache<String,Bitmap>(12*1024*1024){
  @Override protected int sizeOf(String key,Bitmap bitmap){return bitmap.getAllocationByteCount();}
 };
 private final LruCache<String,Bitmap> previews=new LruCache<String,Bitmap>(2*1024*1024){
  @Override protected int sizeOf(String key,Bitmap bitmap){return bitmap.getAllocationByteCount();}
 };
 private final LruCache<String,Bitmap> student=new LruCache<String,Bitmap>(24*1024*1024){
  @Override protected int sizeOf(String key,Bitmap bitmap){return bitmap.getAllocationByteCount();}
 };
 static final String[] SHELVES={"oak","walnut","white_oak"};
 static final String[] BOOKS={"classic","pastel","modern"};
 ThemeAssets(Context context){this.context=context.getApplicationContext();}
 Bitmap get(String path){
  LruCache<String,Bitmap> selected=path.endsWith("cabinet_front.webp")?cache:path.startsWith(RoomScene.ROOT)?student:path.startsWith("scenes/")?rooms:cache;
  Bitmap existing=selected.get(path);if(existing!=null)return existing;
  try(InputStream stream=context.getAssets().open(path)){
   BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;options.inSampleSize=1;
   if(path.startsWith(RoomScene.ROOT)){
    BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
    try(InputStream header=context.getAssets().open(path)){BitmapFactory.decodeStream(header,null,bounds);}
    int screen=Math.max(context.getResources().getDisplayMetrics().widthPixels,context.getResources().getDisplayMetrics().heightPixels);
    int limit=Math.min(1920,Math.max(1024,Math.round(screen*1.25f)));
    android.app.ActivityManager manager=(android.app.ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
    boolean low=manager!=null&&manager.isLowRamDevice();if(low)limit=Math.min(limit,1024);
    boolean outside=path.contains("outside_");if(outside)limit=Math.min(limit,low?768:1024);
    while(Math.max(bounds.outWidth,bounds.outHeight)/options.inSampleSize>limit*(outside?1:1.3f))options.inSampleSize*=2;
   }
   Bitmap decoded=BitmapFactory.decodeStream(stream,null,options);
   if(decoded!=null)selected.put(path,decoded);
   return decoded;
  }catch(Exception unavailable){return null;}
 }
 Bitmap shelf(int index){return get("themes/shelves/"+SHELVES[index]+"/cabinet.webp");}
 Bitmap surface(int index){Bitmap surface=get("themes/shelves/"+SHELVES[index]+"/surface.webp");return surface!=null?surface:shelf(index);}
 Bitmap book(int index,int variant){return get("themes/books/"+BOOKS[index]+"/"+(variant%4)+".webp");}
 Bitmap scene(boolean landscape){
  return get("scenes/study_"+(landscape?"landscape":"portrait")+".webp");
 }
 Bitmap scene(SceneTheme theme,boolean landscape){
  Bitmap room=get(theme.assetPath(landscape));return room!=null?room:scene(landscape);
 }
 Bitmap scenePreview(SceneTheme theme){
  String path=RoomScene.ROOT+"room_day.webp";Bitmap existing=previews.get(path);if(existing!=null)return existing;
  try(InputStream stream=context.getAssets().open(path)){
   BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;options.inSampleSize=8;
   Bitmap decoded=BitmapFactory.decodeStream(stream,null,options);if(decoded!=null)previews.put(path,decoded);return decoded;
  }catch(Exception unavailable){return null;}
 }
 int cachedRoomBytes(){return rooms.size();}
 int cachedArtworkBytes(){return cache.size();}
 int cachedStudentBytes(){return student.size();}
 void clear(){cache.evictAll();rooms.evictAll();previews.evictAll();student.evictAll();}
}
