package com.sitebox.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;

/** Versioned, bounded backup validation. Never edits the caller's library. */
final class BackupCodec {
 static final int MAX_BYTES=2*1024*1024;
 private BackupCodec() {}
 static final class Snapshot {
  final JSONArray sites; final String[] categories;
  final int shelfTheme,bookTheme,bookCount; final String backgroundTheme;
  final boolean motion; final long createdAt;
  Snapshot(JSONObject o,JSONArray s,String[] c)throws JSONException{
   sites=s;categories=c;shelfTheme=(int)integer(o,"shelfTheme");bookTheme=(int)integer(o,"bookTheme");
   bookCount=s.length();backgroundTheme=SceneTheme.find(string(o,"backgroundTheme")).id;motion=(Boolean)o.get("motion");createdAt=integer(o,"createdAt");
  }
 }
 static String encode(JSONArray sites,String[] categories,int shelf,int book,String background,boolean motion)throws JSONException{
  if(sites==null||categories==null)throw invalid();
  JSONObject o=new JSONObject();o.put("format","sitebox-backup");o.put("version",1);o.put("createdAt",System.currentTimeMillis());
  o.put("bookCount",sites.length());o.put("sites",sites);JSONArray names=new JSONArray();for(String c:categories)names.put(c);
  o.put("categories",names);o.put("shelfTheme",shelf);o.put("bookTheme",book);o.put("backgroundTheme",background);o.put("motion",motion);
  String json=o.toString();if(json==null)throw invalid();decode(json);return json;
 }
 static Snapshot decode(String json)throws JSONException{
  if(json==null||json.getBytes(StandardCharsets.UTF_8).length>MAX_BYTES)throw invalid();
  new StrictJson(json).validate();JSONObject o=new JSONObject(json);
  if(!"sitebox-backup".equals(string(o,"format"))||integer(o,"version")!=1||integer(o,"createdAt")<0)throw invalid();
  Object raw=o.get("sites"),cats=o.get("categories");if(!(raw instanceof JSONArray)||!(cats instanceof JSONArray))throw invalid();
  JSONArray sites=(JSONArray)raw,names=(JSONArray)cats;if(sites.length()>10000||names.length()!=9||integer(o,"bookCount")!=sites.length())throw invalid();
  Set<String> known=new HashSet<>(),folded=new HashSet<>();String[] categories=new String[9];
  for(int i=0;i<9;i++){Object n=names.get(i);if(!(n instanceof String)||((String)n).trim().isEmpty()||!n.equals(((String)n).trim())||!known.add((String)n)||!folded.add(((String)n).toLowerCase(Locale.ROOT)))throw invalid();categories[i]=(String)n;}
  for(int i=0;i<sites.length();i++){
   Object value=sites.get(i);if(!(value instanceof JSONObject))throw invalid();JSONObject site=(JSONObject)value;
   if(string(site,"name").trim().isEmpty()||!known.contains(string(site,"category")))throw invalid();
   String url=string(site,"url");try{URI u=new URI(url);if(!("http".equalsIgnoreCase(u.getScheme())||"https".equalsIgnoreCase(u.getScheme()))||u.getRawAuthority()==null||LibraryRules.webUrl(url).isEmpty())throw invalid();}catch(java.net.URISyntaxException e){throw invalid();}
   if(site.has("fav")&&!(site.get("fav") instanceof Boolean))throw invalid();
   if(site.has("notes"))string(site,"notes");if(site.has("sortOrder"))integer(site,"sortOrder");
  }
  long shelf=integer(o,"shelfTheme"),book=integer(o,"bookTheme");if(shelf<0||shelf>2||book<0||book>2||!(o.get("motion") instanceof Boolean))throw invalid();
  String background=string(o,"backgroundTheme");if(!SceneTheme.acceptsBackupId(background))throw invalid();
  return new Snapshot(o,sites,categories);
 }
 private static String string(JSONObject o,String k)throws JSONException{Object v=o.get(k);if(!(v instanceof String))throw invalid();return (String)v;}
 private static long integer(JSONObject o,String k)throws JSONException{Object v=o.get(k);if(!(v instanceof Integer)&&!(v instanceof Long))throw invalid();return ((Number)v).longValue();}
 private static JSONException invalid(){return new JSONException("올바른 SiteBox 백업 파일이 아니에요.");}
 /** Android's JSONTokener accepts comments, unquoted keys and trailing data; this gate does not. */
 private static final class StrictJson {
  final String s;int p;StrictJson(String s){this.s=s;}
  void validate()throws JSONException{space();value(0);space();if(p!=s.length())throw invalid();}
  void space(){while(p<s.length()&&(s.charAt(p)==' '||s.charAt(p)=='\n'||s.charAt(p)=='\r'||s.charAt(p)=='\t'))p++;}
  char peek(){return p<s.length()?s.charAt(p):'\0';}
  void expect(char c)throws JSONException{space();if(peek()!=c)throw invalid();p++;}
  void value(int depth)throws JSONException{
   if(depth>64)throw invalid();space();char c=peek();
   if(c=='{'){p++;space();Set<String> keys=new HashSet<>();if(peek()=='}'){p++;return;}while(true){space();int start=p;quoted();String key=new JSONArray("["+s.substring(start,p)+"]").getString(0);if(!keys.add(key))throw invalid();expect(':');value(depth+1);space();if(peek()=='}'){p++;return;}expect(',');}}
   else if(c=='['){p++;space();if(peek()==']'){p++;return;}while(true){value(depth+1);space();if(peek()==']'){p++;return;}expect(',');}}
   else if(c=='"')quoted();else if(s.startsWith("true",p))p+=4;else if(s.startsWith("false",p))p+=5;else if(s.startsWith("null",p))p+=4;else number();
  }
  void quoted()throws JSONException{if(peek()!='"')throw invalid();p++;while(p<s.length()){char c=s.charAt(p++);if(c=='"')return;if(c<32)throw invalid();if(c=='\\'){if(p>=s.length())throw invalid();char esc=s.charAt(p++);if(esc=='u'){for(int i=0;i<4;i++){if(p>=s.length()||Character.digit(s.charAt(p++),16)<0)throw invalid();}}else if("\"\\/bfnrt".indexOf(esc)<0)throw invalid();}}throw invalid();}
  void number()throws JSONException{int start=p;if(peek()=='-')p++;if(peek()=='0')p++;else{if(peek()<'1'||peek()>'9')throw invalid();while(peek()>='0'&&peek()<='9')p++;}if(peek()=='.'){p++;digits();}if(peek()=='e'||peek()=='E'){p++;if(peek()=='+'||peek()=='-')p++;digits();}if(p==start)throw invalid();}
  void digits()throws JSONException{int start=p;while(peek()>='0'&&peek()<='9')p++;if(start==p)throw invalid();}
 }
}
