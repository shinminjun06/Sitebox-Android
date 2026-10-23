package com.sitebox.app;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class SyncPlannerTest {
    final String[] categories={"공부","개발","AI","게임","영상","쇼핑","커뮤니티","생활","기타"};
    String state(String... names)throws Exception{
        JSONArray books=new JSONArray();for(String n:names)books.put(new JSONObject().put("name",n).put("url","https://example.com/"+n).put("category",categories[0]).put("notes","").put("fav",false));
        return BackupCodec.encode(books,categories,0,0,"study",true);
    }
    String edit(String original,String key,Object value)throws Exception{
        JSONObject o=new JSONObject(original);o.getJSONArray("sites").getJSONObject(0).put(key,value);return o.toString();
    }
    @Test public void emptyCloudKeepsOfflineLibrary()throws Exception{
        String local=state("offline");assertTrue(SyncPlanner.same(local,SyncPlanner.merge(null,local,null)));
    }
    @Test public void initialConnectionMergesLocalAndCloudBooks()throws Exception{
        BackupCodec.Snapshot s=BackupCodec.decode(SyncPlanner.merge(null,state("offline"),state("cloud")));
        assertEquals(2,s.bookCount);
    }
    @Test public void offlineDeletionIsNotResurrected()throws Exception{
        String base=state("book");assertTrue(SyncPlanner.same(state(),SyncPlanner.merge(base,state(),base)));
    }
    @Test public void remoteDeletionIsNotResurrected()throws Exception{
        String base=state("book");assertTrue(SyncPlanner.same(state(),SyncPlanner.merge(base,base,state())));
    }
    @Test public void simultaneousEditsKeepBothVersions()throws Exception{
        String base=state("book");BackupCodec.Snapshot s=BackupCodec.decode(SyncPlanner.merge(base,edit(base,"notes","offline memo"),edit(base,"notes","cloud memo")));
        assertEquals(2,s.bookCount);
        assertEquals("offline memo",s.sites.getJSONObject(0).getString("notes"));
        assertEquals("cloud memo",s.sites.getJSONObject(1).getString("notes"));
        assertFalse(s.sites.getJSONObject(0).optString("syncId").isEmpty());
    }
    @Test public void timestampsDoNotCauseRepeatedUploads()throws Exception{
        String base=state("book");JSONObject other=new JSONObject(base);other.put("createdAt",1);
        assertTrue(SyncPlanner.same(base,other.toString()));
    }
}
