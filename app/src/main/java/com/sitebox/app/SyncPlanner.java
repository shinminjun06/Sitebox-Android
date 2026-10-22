package com.sitebox.app;

import org.json.*;
import java.util.*;

/** Three-way merge against the last acknowledged cloud snapshot. No clock-based data loss. */
final class SyncPlanner {
 private SyncPlanner(){}
 static String canonical(Object value)throws JSONException{
  if(value instanceof JSONObject){JSONObject o=(JSONObject)value;ArrayList<String> keys=new ArrayList<>();Iterator<String> it=o.keys();while(it.hasNext())keys.add(it.next());Collections.sort(keys);StringBuilder b=new StringBuilder("{");for(String key:keys)b.append(JSONObject.quote(key)).append(':').append(canonical(o.get(key))).append(',');return b.append('}').toString();}
  if(value instanceof JSONArray){JSONArray a=(JSONArray)value;StringBuilder b=new StringBuilder("[");for(int i=0;i<a.length();i++)b.append(canonical(a.get(i))).append(',');return b.append(']').toString();}
  return value instanceof String?JSONObject.quote((String)value):String.valueOf(value);
 }
 static String content(String json)throws JSONException{JSONObject o=new JSONObject(json);o.remove("createdAt");return canonical(o);}
 static boolean same(String a,String b)throws JSONException{return a==null?b==null:b!=null&&content(a).equals(content(b));}
 private static boolean equal(Object a,Object b)throws JSONException{return a==null?b==null:b!=null&&canonical(a).equals(canonical(b));}
 private static Object choose(Object base,Object local,Object remote)throws JSONException{return equal(local,base)?remote:equal(remote,base)?local:remote;}
 private static LinkedHashMap<String,JSONObject> books(BackupCodec.Snapshot s)throws JSONException{
  LinkedHashMap<String,JSONObject> out=new LinkedHashMap<>();if(s==null)return out;Map<String,Integer> occurrences=new HashMap<>();
  for(int i=0;i<s.sites.length();i++){JSONObject o=new JSONObject(s.sites.getJSONObject(i).toString());String url=o.getString("url");int n=occurrences.getOrDefault(url,0);occurrences.put(url,n+1);String key=o.optString("syncId");if(key.isEmpty())key="legacy:"+url+":"+n;while(out.containsKey(key))key+="_";for(int c=0;c<9;c++)if(s.categories[c].equals(o.getString("category"))){o.put("category",c);break;}out.put(key,o);}
  return out;
 }
 static String merge(String baseJson,String localJson,String remoteJson)throws JSONException{
  BackupCodec.Snapshot local=BackupCodec.decode(localJson);if(remoteJson==null)return localJson;
  BackupCodec.Snapshot remote=BackupCodec.decode(remoteJson),base=baseJson==null?null:BackupCodec.decode(baseJson);
  if(same(localJson,remoteJson)||same(localJson,baseJson))return remoteJson;
  if(same(remoteJson,baseJson))return localJson;
  String[] categories=remote.categories.clone();if(base!=null)for(int i=0;i<9;i++)categories[i]=(String)choose(base.categories[i],local.categories[i],remote.categories[i]);
  // Independently valid renames may collide after a merge. Keep the remote vocabulary in that case.
  if(new HashSet<>(Arrays.asList(categories)).size()!=9)categories=remote.categories.clone();
  HashSet<String> folded=new HashSet<>();for(String c:categories)folded.add(c.toLowerCase(Locale.ROOT));if(folded.size()!=9)categories=remote.categories.clone();
  LinkedHashMap<String,JSONObject> bm=books(base),lm=books(local),rm=books(remote);LinkedHashSet<String> keys=new LinkedHashSet<>(rm.keySet());keys.addAll(lm.keySet());keys.addAll(bm.keySet());JSONArray merged=new JSONArray();
  for(String key:keys){JSONObject b=bm.get(key),l=lm.get(key),r=rm.get(key);JSONObject selected;
   if(equal(l,b))selected=r;else if(equal(r,b)||equal(l,r))selected=l;
   else{selected=r;if(l!=null){JSONObject copy=new JSONObject(l.toString());if(r!=null)copy.put("syncId",UUID.randomUUID().toString());append(merged,copy,categories);}}
   if(selected!=null)append(merged,new JSONObject(selected.toString()),categories);
  }
  int shelf=base==null?remote.shelfTheme:(Integer)choose(base.shelfTheme,local.shelfTheme,remote.shelfTheme);
  int book=base==null?remote.bookTheme:(Integer)choose(base.bookTheme,local.bookTheme,remote.bookTheme);
  String background=base==null?remote.backgroundTheme:(String)choose(base.backgroundTheme,local.backgroundTheme,remote.backgroundTheme);
  boolean motion=base==null?remote.motion:(Boolean)choose(base.motion,local.motion,remote.motion);
  return BackupCodec.encode(merged,categories,shelf,book,background,motion);
 }
 private static void append(JSONArray out,JSONObject book,String[] categories)throws JSONException{book.put("category",categories[book.getInt("category")]);out.put(book);}
}
