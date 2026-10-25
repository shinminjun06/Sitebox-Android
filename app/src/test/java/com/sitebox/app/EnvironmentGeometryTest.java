package com.sitebox.app;
import org.junit.Test;
import static org.junit.Assert.*;
public class EnvironmentGeometryTest {
 @Test public void originalWindowIsARealOpeningInAnOtherwiseSolidWall(){ModelMesh.Data wall=EnvironmentGeometry.wall();assertFalse(hit(wall,.475f,1.8f));assertTrue(hit(wall,-1.8f,1.8f));assertTrue(hit(wall,.475f,.3f));assertTrue(hit(wall,.475f,2.85f));assertTrue(hit(wall,2.05f,1.8f));}
 @Test public void neighborhoodHasRealDepthAndBoundedGeometry(){EnvironmentGeometry.Town t=EnvironmentGeometry.town();assertTrue(t.solid.max[2]-t.solid.min[2]>25);assertTrue(t.solid.max[2]<-6);assertTrue(t.solid.indices.length>3000);assertTrue(t.solid.indices.length<300000);for(float f:t.solid.vertices)assertTrue(Float.isFinite(f));assertTrue(t.windows.indices.length>0);}
 @Test public void roundedVegetationFacesOutwardAndSkyFacesInward(){ModelMesh.Builder b=new ModelMesh.Builder();b.ellipsoid(0,0,0,1,2,1,new float[]{1,1,1},12,8);SolidBookGeometryTest.check(b.build(),new java.util.HashMap<>());SolidBookGeometryTest.check(EnvironmentGeometry.sky(),new java.util.HashMap<>());}
 static boolean hit(ModelMesh.Data d,float x,float y){for(int i=0;i<d.indices.length;i+=3){int a=d.indices[i]*15,b=d.indices[i+1]*15,c=d.indices[i+2]*15;float ax=d.vertices[a],ay=d.vertices[a+1],bx=d.vertices[b],by=d.vertices[b+1],cx=d.vertices[c],cy=d.vertices[c+1];float den=(by-cy)*(ax-cx)+(cx-bx)*(ay-cy);if(Math.abs(den)<1e-8)continue;float u=((by-cy)*(x-cx)+(cx-bx)*(y-cy))/den,v=((cy-ay)*(x-cx)+(ax-cx)*(y-cy))/den;if(u>=0&&v>=0&&u+v<=1)return true;}return false;}
}
