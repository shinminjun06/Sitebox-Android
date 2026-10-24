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
  if(!value.matches("(?i)^https?://.*")){
   boolean hostPort=value.matches("^[^/\\s:]+:[0-9]+([/?#].*)?$");
   if(value.matches("(?i)^[a-z][a-z0-9+.-]*:.*")&&!hostPort)return "";
   value="https://"+value;
  }
  try{
   URI uri=new URI(value);String authority=uri.getRawAuthority();
   if(authority==null||authority.isEmpty()||authority.contains("@"))return "";
   String host=uri.getHost();
   if(host!=null)return uri.getPort()>65535?"":value;
   String unicodeHost=authority,port="";
   int colon=authority.lastIndexOf(':');
   if(colon>=0){unicodeHost=authority.substring(0,colon);port=authority.substring(colon);int number=Integer.parseInt(port.substring(1));if(number<0||number>65535)return "";}
   String ascii=java.net.IDN.toASCII(unicodeHost,java.net.IDN.USE_STD3_ASCII_RULES);
   if(ascii.isEmpty())return "";
   String normalized=uri.getScheme()+"://"+ascii+port+(uri.getRawPath()==null?"":uri.getRawPath())+(uri.getRawQuery()==null?"":"?"+uri.getRawQuery())+(uri.getRawFragment()==null?"":"#"+uri.getRawFragment());
   return new URI(normalized).getHost()==null?"":normalized;
  }catch(Exception invalid){return "";}
 }
 // Fractional bounds are the inner openings after artwork normalization.
 static float[] cell(int index){
  return CabinetGeometry.cell(index);
 }
}
