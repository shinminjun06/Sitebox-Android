package com.sitebox.app;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class SolidBookGeometryTest {
 @Test public void coversAndPagesHaveClosedDepthInsteadOfASpineQuad(){
  SolidBookGeometry.Book b=new SolidBookGeometry.Book(-2.14f,.749f,-2.105f,1.29f,-.774f,.29f,new float[]{.1f,.2f,.2f,.5f},new float[]{.18f,.05f,.03f});
  SolidBookGeometry.Result r=SolidBookGeometry.build(Arrays.asList(b));
  assertEquals(1,r.books);assertTrue(r.body.indices.length>120);assertTrue(r.spines.indices.length>6);
  assertTrue(r.body.max[2]-r.body.min[2]>.25f);assertTrue(r.body.min[2]>-1.18f);assertEquals(b.bottom,r.body.min[1],.00001f);
  HashMap<String,Integer> edges=new HashMap<>();check(r.body,edges);check(r.spines,edges);
  for(Map.Entry<String,Integer> e:edges.entrySet())assertEquals("Closed solids have no unmatched boundary: "+e.getKey(),0,e.getValue()%2);
  for(int v=0;v<r.spines.vertices.length;v+=15){assertTrue(r.spines.vertices[v+7]>=.1f&&r.spines.vertices[v+7]<=.2f);assertTrue(r.spines.vertices[v+8]>=.2f&&r.spines.vertices[v+8]<=.5f);}
 }
 @Test public void emptyDataCreatesNoDecorativeFakeBooks(){SolidBookGeometry.Result r=SolidBookGeometry.build(Collections.emptyList());assertEquals(0,r.books);assertEquals(0,r.body.indices.length);assertEquals(0,r.spines.indices.length);}
 @Test public void measuredSupportMatchesTheActualFrontShelfLips(){assertEquals(1.4385f,RoomModelGeometry.shelfTop(0),.00001f);assertEquals(.7485f,RoomModelGeometry.shelfTop(4),.00001f);assertEquals(.0585f,RoomModelGeometry.shelfTop(8),.00001f);}
 @Test public void nativeHitCellsAndSolidBookBasesShareTheSupport(){for(int i=0;i<9;i++){float[] r=RoomModelGeometry.cell(i);assertEquals(RoomModelGeometry.shelfTop(i)+.0005f,RoomModelGeometry.TOP-r[3]*(RoomModelGeometry.TOP-RoomModelGeometry.BOTTOM),.00001f);}}
 @Test public void draggedSolidBookDimsCoverPagesAndSpineTogether(){
  float[] uv={.1f,.2f,.2f,.5f},c={.2f,.1f,.05f};
  SolidBookGeometry.Result normal=SolidBookGeometry.build(Arrays.asList(new SolidBookGeometry.Book(-2,.75f,-1.96f,1.3f,-.774f,.3f,uv,c)));
  SolidBookGeometry.Result dragged=SolidBookGeometry.build(Arrays.asList(new SolidBookGeometry.Book(-2,.75f,-1.96f,1.3f,-.774f,.3f,uv,c,.25f)));
  for(ModelMesh.Data[] pair:new ModelMesh.Data[][]{{normal.body,dragged.body},{normal.spines,dragged.spines}}){assertEquals(pair[0].indices.length,pair[1].indices.length);for(int i=0;i<pair[0].vertices.length;i+=15)for(int k=11;k<14;k++)assertEquals(pair[0].vertices[i+k]*.25f,pair[1].vertices[i+k],.000001f);}
 }
 static String point(ModelMesh.Data d,int i){int p=i*15;return Math.round(d.vertices[p]*1000000)+","+Math.round(d.vertices[p+1]*1000000)+","+Math.round(d.vertices[p+2]*1000000);}
 static void check(ModelMesh.Data d,Map<String,Integer> edges){
  for(float f:d.vertices)assertTrue(Float.isFinite(f));
  for(int i=0;i<d.indices.length;i+=3){int a=d.indices[i],b=d.indices[i+1],c=d.indices[i+2];assertTrue(a>=0&&c<d.vertices.length/15&&b<d.vertices.length/15);
   for(int[] e:new int[][]{{a,b},{b,c},{c,a}}){String x=point(d,e[0]),y=point(d,e[1]);String key=x.compareTo(y)<0?x+":"+y:y+":"+x;edges.put(key,edges.getOrDefault(key,0)+1);}
   float[] n=ModelMesh.normal(d.vertices,a*15);int ai=a*15,bi=b*15,ci=c*15;float x=d.vertices[bi]-d.vertices[ai],y=d.vertices[bi+1]-d.vertices[ai+1],z=d.vertices[bi+2]-d.vertices[ai+2],u=d.vertices[ci]-d.vertices[ai],v=d.vertices[ci+1]-d.vertices[ai+1],w=d.vertices[ci+2]-d.vertices[ai+2];
   assertTrue("Normals point out of the rendered surface",(y*w-z*v)*n[0]+(z*u-x*w)*n[1]+(x*v-y*u)*n[2]>0);
  }
 }
}
