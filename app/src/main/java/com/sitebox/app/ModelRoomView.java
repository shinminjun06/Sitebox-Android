package com.sitebox.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.view.Choreographer;
import android.view.Surface;
import android.view.TextureView;
import com.google.android.filament.*;
import com.google.android.filament.gltfio.*;
import com.google.android.filament.android.TextureHelper;
import com.google.android.filament.android.DisplayHelper;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.function.Consumer;

/** Native renderer of the supplied room. All Filament calls belong to the UI thread. */
final class ModelRoomView extends TextureView implements TextureView.SurfaceTextureListener,Choreographer.FrameCallback {
 private Engine engine;
 private DisplayHelper displayHelper;
 private int warmup;
 private boolean surfacePresented,modelLoading;
 boolean presented(){return presented;}
 private Renderer renderer;
 private com.google.android.filament.View view;
 private Scene scene;
 private Camera camera;
 private FilamentAsset asset;
 private AssetLoader loader;
 private ResourceLoader resources;
 private UbershaderProvider materials;
 private IndirectLight ambient;
 private SwapChain swap;
 private Surface surface;
 private int cameraEntity,sun,lamp;
 private final HashMap<String,Texture> windows=new HashMap<>();
 private MaterialInstance windowMaterial,ownedWindowMaterial;
 private final java.util.ArrayList<MaterialInstance> shelfMaterials=new java.util.ArrayList<>();
 private final java.util.ArrayList<Float> shelfFactors=new java.util.ArrayList<>();
 private int finish;
 private boolean weatherImages=true;
 int finishMaterialCount(){return shelfMaterials.size();}
 void setWeatherImages(boolean value){if(weatherImages==value)return;weatherImages=value;updateWindow();requestFrame();}
 void setFinish(int value){finish=value;applyFinish();requestFrame();}
 private void applyFinish(){float[] tint=RoomModelGeometry.finishTint(finish);for(int i=0;i<shelfMaterials.size();i++){float f=shelfFactors.get(i);shelfMaterials.get(i).setParameter("baseColorFactor",tint[0]*f,tint[1]*f,tint[2]*f,1);}}

 private boolean active=true,disposed,scheduled,dirty=true,reduced;
 private float daylight=1,clouds,progress=1;
 private int precipitation;
 private boolean lampOn=true;
 private long frames;
 private boolean presented;
 private Bitmap startupProbe;
 private String error,windowKey="day";
 private Consumer<String> status=value->{};
 private RoomCameraRig rig;
 ModelRoomView(Context context){super(context);setOpaque(true);setSurfaceTextureListener(this);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
 void setStatusListener(Consumer<String> listener){status=listener;}
 boolean ready(){return asset!=null&&error==null;}
 long frames(){return frames;}
 int renderableCount(){return asset==null?0:asset.getRenderableEntities().length;}
 String failure(){return error;}
 String windowState(){return windowKey;}
 float lampIntensity(){return engine==null||lamp==0?0:engine.getLightManager().getIntensity(engine.getLightManager().getInstance(lamp));}
 void setPose(RoomCameraRig value,float p){rig=value;progress=p;updateCamera();updateLights();requestFrame();}
 void setReduced(boolean value){reduced=value;if(view!=null)configureQuality();requestFrame();}
 void setActive(boolean value){active=value;if(!value){Choreographer.getInstance().removeFrameCallback(this);scheduled=false;}else requestFrame();}
 void setAtmosphere(float day,float cloud,int rain,boolean light){
  if(Math.abs(day-daylight)<.001f&&Math.abs(cloud-clouds)<.001f&&precipitation==rain&&lampOn==light)return;
  daylight=day;clouds=cloud;precipitation=rain;lampOn=light;updateLights();updateWindow();requestFrame();
 }
 private void initialize(){
  if(engine!=null||disposed)return;
  Filament.init();Gltfio.init();engine=Engine.create(Engine.Backend.OPENGL);renderer=engine.createRenderer();view=engine.createView();scene=engine.createScene();
  cameraEntity=EntityManager.get().create();camera=engine.createCamera(cameraEntity);camera.setExposure(16,1f/125,100);view.setScene(scene);view.setCamera(camera);
  Renderer.ClearOptions clear=new Renderer.ClearOptions();clear.clear=true;clear.discard=false;clear.clearColor=new float[]{.055f,.065f,.08f,1};renderer.setClearOptions(clear);
  view.setPostProcessingEnabled(false);updateCamera();
 }
 private void loadModel(){
  if(disposed||modelLoading||asset!=null||engine==null)return;modelLoading=true;
  try{initializeModel();requestFrame();}
  catch(RuntimeException|LinkageError e){error=e.toString();android.util.Log.e("SiteBox3D","Native room initialization failed",e);status.accept("3D 방을 열지 못했어요.\n"+error);}
 }
 private void initializeModel(){
  configureQuality();view.setPostProcessingEnabled(true);
  materials=new UbershaderProvider(engine);loader=new AssetLoader(engine,materials,EntityManager.get());resources=new ResourceLoader(engine);
  try(InputStream input=getContext().getAssets().open(RoomModelGeometry.ASSET)){
   java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream();byte[] chunk=new byte[8192];int count;while((count=input.read(chunk))!=-1)output.write(chunk,0,count);byte[] bytes=output.toByteArray();ByteBuffer buffer=ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());buffer.put(bytes).flip();asset=loader.createAsset(buffer);
   if(asset==null)throw new IllegalStateException("GLB model could not be parsed");resources.loadResources(asset);
  }catch(Exception e){throw new IllegalStateException("GLB loading failed",e);}
  // Source geometry is Z-up. Do not normalize a room into a unit cube.
  float[] rotation={1,0,0,0, 0,0,-1,0, 0,1,0,0, 0,0,0,1};
  TransformManager tm=engine.getTransformManager();tm.setTransform(tm.getInstance(asset.getRoot()),rotation);
  RenderableManager rm=engine.getRenderableManager();
  for(int entity:asset.getRenderableEntities()){
   String name=asset.getName(entity);if(name==null)name="";
   if(name.startsWith("BOOK_SLOT_")||name.startsWith("Sunbeam_Patch_"))continue;
   int instance=rm.getInstance(entity);
   // The export omits metallicFactor (glTF defaults to 1). Wood, plaster and
   // fabric need diffuse lighting, not a metal response without reflections.
   for(int primitive=0;primitive<rm.getPrimitiveCount(instance);primitive++){
    MaterialInstance material=rm.getMaterialInstanceAt(instance,primitive);
    if(material.getMaterial().hasParameter("metallicFactor"))material.setParameter("metallicFactor",0f);
   }
   // The supplied back wall has no geometric window hole. Let sun enter through
   // the authored window plane instead of blocking the entire interior with it.
   boolean shell=name.startsWith("Wall_")||name.startsWith("Ceiling")||name.startsWith("Window_Neighborhood");
   rm.setCastShadows(instance,!shell);rm.setReceiveShadows(instance,true);rm.setScreenSpaceContactShadows(instance,true);
   scene.addEntity(entity);
   if(name.startsWith("Bookshelf_3x3_")||name.startsWith("Bookshelf_Edge_")||name.startsWith("Bookshelf_Foot_")||name.startsWith("HQ_Shelf_Wood_")||name.startsWith("V4_Bookshelf_")){
    for(int primitive=0;primitive<rm.getPrimitiveCount(instance);primitive++){
     MaterialInstance separate=MaterialInstance.duplicate(rm.getMaterialInstanceAt(instance,primitive),"SiteBox shelf finish");rm.setMaterialInstanceAt(instance,primitive,separate);shelfMaterials.add(separate);shelfFactors.add(name.startsWith("HQ_")?.4f:1f);
    }
   }
   if(name.equals("Window_Neighborhood_Reference")){
    // Outdoors must not become dark when the indoor desk lamp turns off.
    MaterialProvider.MaterialKey key=new MaterialProvider.MaterialKey();key.unlit=true;key.hasBaseColorTexture=true;key.baseColorUV=0;
    ownedWindowMaterial=materials.createMaterialInstance(key,new int[8],"SiteBox outdoor window",null);
    if(ownedWindowMaterial==null)throw new IllegalStateException("Cannot create outdoor material");
    ownedWindowMaterial.setParameter("baseColorFactor",1f,1f,1f,1f);rm.setMaterialInstanceAt(instance,0,ownedWindowMaterial);windowMaterial=ownedWindowMaterial;
   }
  }
  if(windowMaterial==null)throw new IllegalStateException("Model window material is missing");
  sun=EntityManager.get().create();LightManager.ShadowOptions shadow=new LightManager.ShadowOptions();shadow.mapSize=reduced?512:1024;shadow.shadowCascades=1;shadow.normalBias=.01f;shadow.constantBias=.001f;shadow.shadowFar=15;
  new LightManager.Builder(LightManager.Type.DIRECTIONAL).color(1,.88f,.72f).intensity(65000).direction(-.55f,-.85f,.55f).castShadows(true).shadowOptions(shadow).build(engine,sun);scene.addEntity(sun);
  lamp=EntityManager.get().create();new LightManager.Builder(LightManager.Type.POINT).position(-.36f,1.31f,-1.16f).color(1,.66f,.32f).intensity(2400).falloff(3.8f).castShadows(false).build(engine,lamp);scene.addEntity(lamp);
  ambient=new IndirectLight.Builder().irradiance(1,new float[]{1,1,1}).intensity(18000).build(engine);scene.setIndirectLight(ambient);
  android.util.Log.i("SiteBox3D","Loaded supplied room GLB: "+asset.getRenderableEntities().length+" renderables, scene="+scene.getRenderableCount()+", surface="+getWidth()+"x"+getHeight());asset.releaseSourceData();applyFinish();updateCamera();updateLights();updateWindow();
 }
 private void configureQuality(){
  view.setAntiAliasing(com.google.android.filament.View.AntiAliasing.FXAA);
  com.google.android.filament.View.AmbientOcclusionOptions ao=new com.google.android.filament.View.AmbientOcclusionOptions();ao.enabled=!reduced;ao.radius=.35f;ao.power=1.3f;ao.intensity=1;ao.resolution=.5f;view.setAmbientOcclusionOptions(ao);
  view.setScreenSpaceRefractionEnabled(false);
 }
 private void updateCamera(){
  if(camera==null||rig==null||getWidth()==0||getHeight()==0)return;
  camera.setProjection(rig.fov(),rig.aspect,.035,40,Camera.Fov.VERTICAL);
  camera.lookAt(rig.eye[0],rig.eye[1],rig.eye[2],rig.target[0],rig.target[1],rig.target[2],0,1,0);
 }
 private void updateLights(){
  if(engine==null||ambient==null)return;
  float dusk=1-daylight;camera.setExposure(16,1f/125,100+500*dusk*dusk*dusk);
  LightManager lm=engine.getLightManager();int s=lm.getInstance(sun),l=lm.getInstance(lamp);
  lm.setIntensity(s,900+64000*daylight*(1-.72f*clouds));
  lm.setColor(s,.55f+.45f*daylight,.66f+.22f*daylight,1-.28f*daylight);
  lm.setDirection(s,-.55f,-.85f,.55f);lm.setIntensity(l,lampOn?2400:0);
  ambient.setIntensity(1700+16000*daylight*(1-.35f*clouds)+(1-progress)*8000);
 }
 private void updateWindow(){
  if(daylight<.35f)windowKey="night"+(weatherImages&&precipitation==2?"_snow":weatherImages&&precipitation==1?"_rain":"");
  else windowKey=weatherImages&&precipitation==2?"snow":weatherImages&&precipitation==1?"rain":weatherImages&&clouds>.65f?"cloud":"day";
  if(engine==null||windowMaterial==null)return;
  // Day is the original embedded GLB texture. Weather variants preserve its UVs.
  String name="models/window_"+windowKey+".png";
  Texture texture=windows.get(windowKey);
  if(texture==null){
   try(InputStream input=getContext().getAssets().open(name)){
    BitmapFactory.Options opts=new BitmapFactory.Options();opts.inScaled=false;opts.inSampleSize=reduced||getWidth()<1000?2:1;Bitmap bitmap=BitmapFactory.decodeStream(input,null,opts);
    if(bitmap==null)throw new IllegalStateException("Invalid window texture: "+name);
    texture=new Texture.Builder().usage(Texture.Usage.DEFAULT|Texture.Usage.GEN_MIPMAPPABLE).width(bitmap.getWidth()).height(bitmap.getHeight()).levels(0xff).sampler(Texture.Sampler.SAMPLER_2D).format(Texture.InternalFormat.SRGB8_A8).build(engine);
    TextureHelper.setBitmap(engine,texture,0,bitmap);texture.generateMipmaps(engine);windows.put(windowKey,texture);
   }catch(java.io.IOException missing){return;}
  }
  windowMaterial.setParameter("baseColorMap",texture,new TextureSampler(TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,TextureSampler.MagFilter.LINEAR,TextureSampler.WrapMode.CLAMP_TO_EDGE));
  windowMaterial.setParameter("baseColorFactor",1f,1f,1f,1f);
 }
 void requestFrame(){dirty=true;if(active&&!disposed&&!scheduled&&swap!=null){scheduled=true;Choreographer.getInstance().postFrameCallback(this);}}
 @Override public void doFrame(long time){
  scheduled=false;if(!active||disposed||swap==null||view==null)return;
  if(dirty||(asset!=null&&(!presented||warmup<8))){dirty=false;if(renderer.beginFrame(swap,time)){renderer.render(view);renderer.endFrame();frames++;warmup++;invalidate();}else dirty=true;}
  if(dirty||(asset!=null&&(!presented||warmup<8)))requestFrame();
 }
 @Override public void onSurfaceTextureAvailable(SurfaceTexture texture,int width,int height){
  try{texture.setDefaultBufferSize(width,height);initialize();surface=new Surface(texture);swap=engine.createSwapChain(surface);displayHelper=new DisplayHelper(getContext());displayHelper.attach(renderer,getDisplay());resize(width,height);requestFrame();}
  catch(RuntimeException|LinkageError e){error=e.toString();android.util.Log.e("SiteBox3D","Native room initialization failed",e);status.accept("3D 방을 열지 못했어요. 앱을 다시 열어 주세요.\n"+error);}
 }
 private void resize(int width,int height){if(view!=null){view.setViewport(new Viewport(0,0,width,height));updateCamera();engine.flushAndWait();requestFrame();}}
 @Override public void onSurfaceTextureSizeChanged(SurfaceTexture texture,int width,int height){resize(width,height);}
 @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture texture){
  Choreographer.getInstance().removeFrameCallback(this);scheduled=false;presented=false;surfacePresented=false;warmup=0;if(displayHelper!=null){displayHelper.detach();displayHelper=null;}
  if(swap!=null){engine.destroySwapChain(swap);engine.flushAndWait();swap=null;}if(surface!=null){surface.release();surface=null;}return true;
 }
 @Override public void onSurfaceTextureUpdated(SurfaceTexture texture){
  if(!surfacePresented){surfacePresented=true;android.util.Log.i("SiteBox3D","empty room surface GPU frame presented");if(asset==null)post(this::loadModel);}
  else if(!presented&&asset!=null&&frames>1&&hasRoomPixels()){presented=true;startupProbe.recycle();startupProbe=null;status.accept(null);android.util.Log.i("SiteBox3D","room GPU frame presented");}
 }
 private boolean hasRoomPixels(){
  // A queued empty frame can arrive after GLB loading. Keep the loading notice
  // until the TextureView contains geometry, rather than trusting queue timing.
  if(startupProbe==null)startupProbe=Bitmap.createBitmap(16,32,Bitmap.Config.ARGB_8888);
  if(getBitmap(startupProbe)==null)return false;int first=startupProbe.getPixel(2,2);
  for(int y=2;y<30;y+=2)for(int x=1;x<15;x+=2){int p=startupProbe.getPixel(x,y);if(Math.abs(((p>>16)&255)-((first>>16)&255))+Math.abs(((p>>8)&255)-((first>>8)&255))+Math.abs((p&255)-(first&255))>30)return true;}
  return false;
 }
 void dispose(){
  if(disposed)return;disposed=true;active=false;Choreographer.getInstance().removeFrameCallback(this);scheduled=false;if(startupProbe!=null){startupProbe.recycle();startupProbe=null;}
  if(engine==null)return;
  if(displayHelper!=null){displayHelper.detach();displayHelper=null;}
  if(swap!=null){engine.destroySwapChain(swap);engine.flushAndWait();swap=null;}if(surface!=null){surface.release();surface=null;}
  if(resources!=null){resources.asyncCancelLoad();resources.destroy();}if(asset!=null){loader.destroyAsset(asset);asset=null;}for(MaterialInstance material:shelfMaterials)engine.destroyMaterialInstance(material);shelfMaterials.clear();shelfFactors.clear();if(loader!=null)loader.destroy();
  if(ownedWindowMaterial!=null){engine.destroyMaterialInstance(ownedWindowMaterial);ownedWindowMaterial=null;}
  if(materials!=null){materials.destroyMaterials();materials.destroy();}
  for(Texture t:windows.values())engine.destroyTexture(t);windows.clear();
  if(ambient!=null)engine.destroyIndirectLight(ambient);
  for(int entity:new int[]{sun,lamp,cameraEntity})if(entity!=0){engine.destroyEntity(entity);EntityManager.get().destroy(entity);}
  engine.destroyView(view);engine.destroyScene(scene);engine.destroyRenderer(renderer);engine.destroy();engine=null;
 }
}
