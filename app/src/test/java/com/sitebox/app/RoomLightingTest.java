package com.sitebox.app;
import org.junit.Test;
import static org.junit.Assert.*;
public class RoomLightingTest {
 @Test public void sunlightEntersThroughTheRearWindowAndPointsDown(){RoomLighting.State s=RoomLighting.state(1,0,true);assertTrue(s.direction[1]<-.5f);assertTrue(s.direction[2]>.4f);assertTrue(s.sun>40000);assertTrue(s.ambient<s.sun*.12f);}
 @Test public void cloudAndNightReduceDirectLightWithoutErasingReadableAmbient(){RoomLighting.State day=RoomLighting.state(1,0,true),cloud=RoomLighting.state(1,1,true),night=RoomLighting.state(0,0,true);assertTrue(cloud.sun<day.sun*.2f);assertTrue(night.sun<day.sun*.03f);assertTrue(night.ambient>0&&night.iso>day.iso);}
 @Test public void overcastNightOccludesTheStarSkyAndDimsMoonlight(){float[] cloud=RoomLighting.cloudSky(0,1);assertTrue("Cloudy night must cover the clear star panorama",cloud[3]>.99f);assertTrue(cloud[2]>cloud[0]);assertEquals(0,RoomLighting.cloudSky(0,0)[3],0);assertTrue(RoomLighting.state(0,1,true).sun<RoomLighting.state(0,0,true).sun*.2f);}
 @Test public void lampChangesOnlyTheLocalSpotlight(){RoomLighting.State on=RoomLighting.state(.4f,.7f,true),off=RoomLighting.state(.4f,.7f,false);assertEquals(on.sun,off.sun,0);assertEquals(on.ambient,off.ambient,0);assertEquals(on.iso,off.iso,0);assertTrue(on.lamp>0);assertEquals(0,off.lamp,0);}
}
