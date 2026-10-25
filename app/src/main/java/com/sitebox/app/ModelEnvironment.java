package com.sitebox.app;

import android.graphics.*;
import com.google.android.filament.*;
import com.google.android.filament.android.TextureHelper;
import com.google.android.filament.gltfio.UbershaderProvider;
import java.util.ArrayList;
import java.util.Random;

/** Actual aperture, outdoor geometry and glass; no photographed interior or baked window frame. */
final class ModelEnvironment {
 private final Engine engine;private final ArrayList<ModelGpuMesh> meshes=new ArrayList<>();private final ArrayList<MaterialInstance> materials=new ArrayList<>();
 private final MaterialInstance sky,roofs,windows,clouds,glass,bulb;
 private final Texture rain,snow;private final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
 final float exteriorDepth;final int exteriorTriangles;
 ModelEnvironment(Engine e,Scene scene,UbershaderProvider provider,MaterialInstance plaster){
  engine=e;MaterialInstance wall=MaterialInstance.duplicate(plaster,"SiteBox wall with actual window opening");materials.add(wall);wall.setParameter("metallicFactor",0f);wall.setParameter("roughnessFactor",.94f);
  add(scene,wall,EnvironmentGeometry.wall(),true,4);
  MaterialInstance town=material(provider,"SiteBox Japanese neighborhood",false,true,false,false);EnvironmentGeometry.Town outside=EnvironmentGeometry.town();
  add(scene,town,outside.solid,false,4);exteriorDepth=outside.solid.max[2]-outside.solid.min[2];exteriorTriangles=outside.solid.indices.length/3+outside.roofs.indices.length/3;
  roofs=material(provider,"SiteBox roof tiles and winter snow",false,true,false,false);add(scene,roofs,outside.roofs,false,4);
  windows=material(provider,"SiteBox outside window lights",false,true,true,false);add(scene,windows,outside.windows,false,4);
  sky=material(provider,"SiteBox sky dome",false,true,true,false);sky.setDepthWrite(false);add(scene,sky,EnvironmentGeometry.sky(),false,0);
  clouds=material(provider,"SiteBox volumetric cloud shapes",false,true,false,true);add(scene,clouds,outside.clouds,false,3);
  glass=material(provider,"SiteBox weather on actual glass",true,false,false,true);glass.setParameter("roughnessFactor",.18f);rain=weatherTexture(false);snow=weatherTexture(true);add(scene,glass,EnvironmentGeometry.glass(),false,6);
  bulb=material(provider,"SiteBox desk bulb",false,true,true,false);ModelMesh.Builder light=new ModelMesh.Builder();light.ellipsoid(-.36f,1.305f,-1.16f,.024f,.018f,.024f,new float[]{1,1,1},16,10);add(scene,bulb,light.build(),false,4);
  setAtmosphere(1,0,0,true);
 }
 private MaterialInstance material(UbershaderProvider p,String name,boolean tex,boolean colors,boolean unlit,boolean blend){MaterialInstance m=ModelMaterials.create(engine,p,name,tex,colors,unlit,blend);materials.add(m);return m;}
 private void add(Scene s,MaterialInstance material,ModelMesh.Data data,boolean shadows,int priority){ModelGpuMesh mesh=new ModelGpuMesh(engine,s,material,shadows,priority);mesh.upload(data);meshes.add(mesh);}
 private Texture weatherTexture(boolean winter){
  Bitmap bitmap=Bitmap.createBitmap(768,512,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(bitmap);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);Random r=new Random(winter?33:17);
  if(winter){for(int i=0;i<220;i++){float x=r.nextFloat()*768,y=350+r.nextFloat()*162,rad=4+r.nextFloat()*19;p.setShader(new RadialGradient(x,y,rad,new int[]{0xdde8f1f3,0x007e9dac},null,Shader.TileMode.CLAMP));c.drawCircle(x,y,rad,p);}}
  else for(int i=0;i<290;i++){float x=r.nextFloat()*768,y=r.nextFloat()*512,rad=1+r.nextFloat()*3.8f;p.setShader(new RadialGradient(x,y,rad,new int[]{0x88d9eaf0,0x113d6470,0x00b9cbd4},new float[]{0,.7f,1},Shader.TileMode.CLAMP));c.drawOval(x-rad,y-rad*1.5f,x+rad,y+rad*1.5f,p);if(i%5==0){p.setShader(null);p.setColor(0x225c8ba3);p.setStrokeWidth(.65f);c.drawLine(x,y,x-1,y+13+r.nextFloat()*28,p);}}
  Texture t=new Texture.Builder().width(768).height(512).levels(0xff).usage(Texture.Usage.DEFAULT|Texture.Usage.GEN_MIPMAPPABLE).sampler(Texture.Sampler.SAMPLER_2D).format(Texture.InternalFormat.SRGB8_A8).build(engine);
  TextureHelper.setBitmap(engine,t,0,bitmap,handler,bitmap::recycle);t.generateMipmaps(engine);return t;
 }
 void setAtmosphere(float day,float cloud,int precipitation,boolean lamp){
  float night=1-day,overcast=1-cloud*.45f;
  sky.setParameter("baseColorFactor",(.035f+.965f*day)*overcast,(.06f+.94f*day)*overcast,(.16f+.84f*day)*overcast,1);
  clouds.setParameter("baseColorFactor",1-cloud*.35f,1-cloud*.32f,1-cloud*.27f,.45f+.55f*cloud);
  if(precipitation==2)roofs.setParameter("baseColorFactor",.92f,.96f,1f,1f);else roofs.setParameter("baseColorFactor",.16f,.19f,.23f,1f);
  windows.setParameter("baseColorFactor",.10f+.85f*night,.22f+.41f*night,.31f-.07f*night,1);
  glass.setParameter("baseColorMap",precipitation==2?snow:rain,new TextureSampler(TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,TextureSampler.MagFilter.LINEAR,TextureSampler.WrapMode.CLAMP_TO_EDGE));
  glass.setParameter("baseColorFactor",.8f,.9f,1f,precipitation==0?0:precipitation==1?.42f:.8f);
  bulb.setParameter("baseColorFactor",lamp?1f:.035f,lamp?.66f:.03f,lamp?.28f:.025f,1f);
 }
 void releaseRetired(){for(ModelGpuMesh m:meshes)m.releaseRetired();}
 void dispose(){for(ModelGpuMesh m:meshes)m.dispose();for(MaterialInstance m:materials)engine.destroyMaterialInstance(m);engine.destroyTexture(rain);engine.destroyTexture(snow);}
}
