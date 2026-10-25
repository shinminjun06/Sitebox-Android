package com.sitebox.app;
import org.junit.Test;
import static org.junit.Assert.*;
public class RenderRevisionTest {
 @Test public void unchangedCameraCannotAcknowledgeStaleBookGeometry(){RenderRevision r=new RenderRevision();r.change();r.submit(100,false);r.present(100);assertFalse(r.ready());r.submit(200,true);r.present(200);assertTrue(r.ready());r.change();assertFalse(r.ready());r.present(200);assertFalse(r.ready());r.submit(300,true);r.present(300);assertTrue(r.ready());}
 @Test public void latestWeatherWaitsForItsOwnDisplayedFrame(){RenderRevision r=new RenderRevision();r.submit(11,true);r.change();r.submit(12,true);r.present(11);assertFalse(r.ready());r.present(12);assertTrue(r.ready());}
}
