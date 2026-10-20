package com.sitebox.app;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/** Platform-independent ordering, URL validation and normalized shelf geometry. */
final class LibraryRules {
 private LibraryRules() {}
 static List<Integer> move(List<Integer> order,int source,int anchor,boolean append){
  ArrayList<Integer> result=new ArrayList<>(order);
  if(source==anchor&&!append)return result;
  result.remove(Integer.valueOf(source));
  int insertion=append?result.size():result.indexOf(anchor);
  if(insertion<0)insertion=result.size();
  result.add(insertion,source);
  return result;
 }
 static String webUrl(String input){
  if(input==null)return "";
  String value=input.trim();
  if(!value.matches("(?i)^[a-z][a-z0-9+.-]*:.*"))value="https://"+value;
  try{
   URI uri=new URI(value);
   String scheme=uri.getScheme();
   if(!"https".equalsIgnoreCase(scheme)&&!"http".equalsIgnoreCase(scheme))return "";
   if(uri.getHost()==null||uri.getHost().isEmpty()||uri.getRawUserInfo()!=null)return "";
   return value;
  }catch(Exception invalid){return "";}
 }
 // Fractional bounds are the inner openings after artwork normalization.
 static float[] cell(int index){
  int row=index/3,col=index%3;
  float x=.075f+col*.300f,y=.075f+row*.272f;
  return new float[]{x,y,x+.260f,y+.245f};
 }
}
