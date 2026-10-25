package com.sitebox.app;
/** A camera, content or lighting revision is ready only after its own complete frame is acquired. */
final class RenderRevision {
 private long wanted=1,displayed,lastTime;private final long[] times=new long[64],revisions=new long[64];private int slot;
 void change(){wanted++;}
 void submit(long timestamp,boolean contentReady){times[slot]=timestamp;revisions[slot]=contentReady?wanted:-1;slot=(slot+1)%times.length;}
 void present(long timestamp){lastTime=timestamp;for(int i=0;i<times.length;i++)if(times[i]==timestamp){displayed=revisions[i];break;}}
 boolean ready(){return wanted==displayed;}
 void reset(){displayed=0;java.util.Arrays.fill(times,0);}
 String describe(){int i=(slot+times.length-1)%times.length;return "revision="+wanted+" displayed="+displayed+" surfaceTime="+lastTime+" submittedTime="+times[i]+" submittedRevision="+revisions[i];}
}
