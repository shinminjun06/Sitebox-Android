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
 static final String[] SHELVES={"oak","walnut","white_oak"};
 static final String[] BOOKS={"classic","pastel","modern"};
 ThemeAssets(Context context){this.context=context.getApplicationContext();}
 Bitmap get(String path){
  LruCache<String,Bitmap> selected=path.startsWith("scenes/")?rooms:cache;
  Bitmap existing=selected.get(path);if(existing!=null)return existing;
  try(InputStream stream=context.getAssets().open(path)){
   BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;
   Bitmap decoded=BitmapFactory.decodeStream(stream,null,options);
   if(decoded!=null)selected.put(path,decoded);
   return decoded;
  }catch(Exception unavailable){return null;}
 }
 Bitmap shelf(int index){return get("themes/shelves/"+SHELVES[index]+"/cabinet.webp");}
 Bitmap surface(int index){Bitmap surface=get("themes/shelves/"+SHELVES[index]+"/surface.webp");return surface!=null?surface:shelf(index);}
 Bitmap book(int index,int variant){return get("themes/books/"+BOOKS[index]+"/"+(variant%4)+".webp");}
 Bitmap scene(boolean landscape){
  Bitmap room=get("scenes/classroom_"+(landscape?"landscape":"portrait")+".webp");
  return room!=null?room:get("school_morning.jpg");
 }
 Bitmap scene(SceneTheme theme,boolean landscape){
  Bitmap room=get(theme.assetPath(landscape));return room!=null?room:scene(landscape);
 }
 Bitmap scenePreview(SceneTheme theme){
  String path=theme.assetPath(false);Bitmap existing=previews.get(path);if(existing!=null)return existing;
  try(InputStream stream=context.getAssets().open(path)){
   BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;options.inSampleSize=8;
   Bitmap decoded=BitmapFactory.decodeStream(stream,null,options);if(decoded!=null)previews.put(path,decoded);return decoded;
  }catch(Exception unavailable){return null;}
 }
 int cachedRoomBytes(){return rooms.size();}
 int cachedArtworkBytes(){return cache.size();}
 void clear(){cache.evictAll();rooms.evictAll();previews.evictAll();}
}
