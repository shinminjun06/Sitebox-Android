package com.sitebox.app;
import java.util.*;
public class LibraryRulesTest {
 static void eq(Object a,Object b){if(!a.equals(b))throw new AssertionError(a+" != "+b);}
 public static void main(String[] args){
  eq(LibraryRules.move(Arrays.asList(0,1,2,3),0,2,false),Arrays.asList(1,0,2,3));
  eq(LibraryRules.move(Arrays.asList(0,1,2,3),0,3,true),Arrays.asList(1,2,3,0));
  eq(LibraryRules.move(Arrays.asList(0,1,2),1,1,false),Arrays.asList(0,1,2));
  eq(LibraryRules.move(Arrays.asList(1,2),0,1,false),Arrays.asList(0,1,2));
  eq(LibraryRules.move(Collections.emptyList(),7,-1,true),Arrays.asList(7));
  eq(LibraryRules.webUrl("example.com"),"https://example.com");
  eq(LibraryRules.webUrl("HTTPS://example.com/a?q=x"),"HTTPS://example.com/a?q=x");
  for(String bad:new String[]{"javascript:alert(1)","file:///tmp/x","https://","https://a b","intent://x","content://x"})eq(LibraryRules.webUrl(bad),"");
  float previous=0;for(int row=0;row<3;row++){
   float[] r=LibraryRules.cell(row*3);float height=r[3]-r[1];
   if(row>0&&Math.abs(height-previous)>.00001)throw new AssertionError("unequal rows");previous=height;
  }
  System.out.println("PASS: ordering, self-drop, cross-category, hidden-target, URLs, equal rows");
 }
}
