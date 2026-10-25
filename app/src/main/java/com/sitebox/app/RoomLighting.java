package com.sitebox.app;
/** Lux/lumen parameters independent of camera travel and book contents. */
final class RoomLighting {
 static final class State {final float sun,ambient,lamp,iso;final float[] direction,color;State(float s,float a,float l,float iso,float[] d,float[] c){sun=s;ambient=a;lamp=l;this.iso=iso;direction=d;color=c;}}
 static State state(float day,float cloud,boolean lamp){day=Math.max(0,Math.min(1,day));cloud=Math.max(0,Math.min(1,cloud));float dusk=1-day;
  float x=-.46f,y=-.62f-.18f*day,z=.66f,n=(float)Math.sqrt(x*x+y*y+z*z);
  return new State(650+85000*day*(1-.94f*cloud),1000+9000*day*(1-.18f*cloud),lamp?850:0,200+800*dusk*dusk*dusk,new float[]{x/n,y/n,z/n},new float[]{.53f+.47f*day,.67f+.25f*day,1-.20f*day});
 }
 static float[] skyIrradiance(){return new float[]{.90f,.86f,.80f, .06f,.10f,.16f, -.06f,-.09f,-.13f, .03f,.05f,.08f, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};}
}
