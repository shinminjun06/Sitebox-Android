package com.sitebox.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Looper;
import com.google.android.filament.*;
import com.google.android.filament.android.TextureHelper;
import com.google.android.filament.gltfio.MaterialProvider;
import com.google.android.filament.gltfio.UbershaderProvider;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/** Closed 3D books with live native title artwork, sharing the cabinet camera.
 * Native children remain the input/accessibility model; only their paint is cached.
 */
final class ModelBookLayer {
 private final Engine engine;
 private final Scene scene;
 private final Runnable requestFrame;
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final VertexBuffer vertices;
 private final IndexBuffer indices;
 private final MaterialInstance books,labels,bodyMaterial;
 private final ModelGpuMesh solidBodies,solidSpines;
 private boolean geometryDirty=true;
 private int solidCount,sourceWidth,sourceHeight;
 int solidCount(){return solidCount;}
 int solidTriangles(){return solidBodies.triangleCount+solidSpines.triangleCount;}
 private final int entity;
 private final Bitmap[] bitmaps=new Bitmap[2];
 private final Canvas[] canvases=new Canvas[2];
 private final Texture[] textures=new Texture[2];
 private final java.util.ArrayList<Texture> retired=new java.util.ArrayList<>();
 private BookshelfView content;
 private boolean dirty=true,disposed,ready;
 private int uploads,width,height;
 ModelBookLayer(Engine engine,Scene scene,UbershaderProvider provider,Runnable requestFrame){
  this.engine=engine;this.scene=scene;this.requestFrame=requestFrame;
  books=material(provider,"SiteBox solid book spines",true);
  bodyMaterial=ModelMaterials.create(engine,provider,"SiteBox hardcover boards and pages",false,true,false);
  solidBodies=new ModelGpuMesh(engine,scene,bodyMaterial,true,4);solidSpines=new ModelGpuMesh(engine,scene,books,true,4);
  labels=material(provider,"SiteBox category labels",false);
  vertices=new VertexBuffer.Builder().vertexCount(4).bufferCount(1)
   .attribute(VertexBuffer.VertexAttribute.POSITION,0,VertexBuffer.AttributeType.FLOAT3,0,60)
   .attribute(VertexBuffer.VertexAttribute.TANGENTS,0,VertexBuffer.AttributeType.FLOAT4,12,60)
   .attribute(VertexBuffer.VertexAttribute.UV0,0,VertexBuffer.AttributeType.FLOAT2,28,60)
   .attribute(VertexBuffer.VertexAttribute.UV1,0,VertexBuffer.AttributeType.FLOAT2,36,60)
   .attribute(VertexBuffer.VertexAttribute.COLOR,0,VertexBuffer.AttributeType.FLOAT4,44,60).build(engine);
  FloatBuffer data=ByteBuffer.allocateDirect(4*60).order(ByteOrder.nativeOrder()).asFloatBuffer();
  vertex(data,RoomModelGeometry.LEFT,RoomModelGeometry.TOP,0,0);
  vertex(data,RoomModelGeometry.LEFT,RoomModelGeometry.BOTTOM,0,1);
  vertex(data,RoomModelGeometry.RIGHT,RoomModelGeometry.BOTTOM,1,1);
  vertex(data,RoomModelGeometry.RIGHT,RoomModelGeometry.TOP,1,0);data.flip();vertices.setBufferAt(engine,0,data);
  indices=new IndexBuffer.Builder().indexCount(6).bufferType(IndexBuffer.Builder.IndexType.USHORT).build(engine);
  ShortBuffer order=ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder()).asShortBuffer();order.put(new short[]{0,1,2,0,2,3}).flip();indices.setBuffer(engine,order);
  entity=EntityManager.get().create();
  new RenderableManager.Builder(1).geometry(0,RenderableManager.PrimitiveType.TRIANGLES,vertices,indices).material(0,labels).blendOrder(0,1)
   .boundingBox(new Box((RoomModelGeometry.LEFT+RoomModelGeometry.RIGHT)/2,(RoomModelGeometry.TOP+RoomModelGeometry.BOTTOM)/2,RoomModelGeometry.FRONT,.58f,1.11f,.01f))
   .culling(false).priority(7).castShadows(false).receiveShadows(false).build(engine,entity);
 }
 private MaterialInstance material(UbershaderProvider provider,String name,boolean depth){
  if(depth)return ModelMaterials.create(engine,provider,name,true,false,false);
  MaterialProvider.MaterialKey key=new MaterialProvider.MaterialKey();key.unlit=true;key.doubleSided=true;key.hasBaseColorTexture=true;key.baseColorUV=0;key.alphaMode=2;
  MaterialInstance result=provider.createMaterialInstance(key,new int[8],name,null);
  if(result==null)throw new IllegalStateException("Book material creation failed");
  result.setDepthWrite(false);result.setDepthCulling(depth);result.setParameter("baseColorFactor",1f,1f,1f,1f);return result;
 }
 private static void vertex(FloatBuffer data,float x,float y,float u,float v){data.put(new float[]{x,y,RoomModelGeometry.FRONT,0,0,0,1,u,v,u,v,1,1,1,1});}
 void setContent(BookshelfView value){if(content!=value){if(content!=null)content.setGpuBooks(false);content=value;dirty=true;geometryDirty=true;}if(content!=null&&ready)content.setGpuBooks(true);}
 void invalidate(){dirty=true;requestFrame.run();}
 boolean ready(){return ready;}
 /** Returns without allocating or drawing when native content has not changed. */
 void prepare(boolean reduced){
  if(disposed||uploads!=0||content==null||content.getWidth()==0||content.getHeight()==0)return;
  if(sourceWidth!=content.getWidth()||sourceHeight!=content.getHeight()){sourceWidth=content.getWidth();sourceHeight=content.getHeight();geometryDirty=true;dirty=true;}
  int cap=reduced?1024:2048;float scale=Math.min(1,cap/(float)Math.max(content.getWidth(),content.getHeight()));
  int w=Math.max(1,Math.round(content.getWidth()*scale)),h=Math.max(1,Math.round(content.getHeight()*scale));
  if(w!=width||h!=height){
   width=w;height=h;dirty=true;
   for(int i=0;i<2;i++){
    if(bitmaps[i]!=null)bitmaps[i].recycle();bitmaps[i]=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);canvases[i]=new Canvas(bitmaps[i]);
    Texture previous=textures[i];textures[i]=new Texture.Builder().width(w).height(h).levels(0xff).usage(Texture.Usage.DEFAULT|Texture.Usage.GEN_MIPMAPPABLE).sampler(Texture.Sampler.SAMPLER_2D).format(Texture.InternalFormat.SRGB8_A8).build(engine);
    (i==0?books:labels).setParameter("baseColorMap",textures[i],new TextureSampler(TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,TextureSampler.MagFilter.LINEAR,TextureSampler.WrapMode.CLAMP_TO_EDGE));
    if(previous!=null)retired.add(previous);
   }
  }
  if(!dirty)return;dirty=false;
  if(geometryDirty){updateGeometry();geometryDirty=false;}
  for(int i=0;i<2;i++){
   Canvas canvas=canvases[i];canvas.drawColor(0,PorterDuff.Mode.CLEAR);int save=canvas.save();canvas.scale(w/(float)content.getWidth(),h/(float)content.getHeight());
   if(i==0){fillSpines(canvas);content.drawRoomBooks(canvas);}else content.drawCategoryLabels(canvas,1);canvas.restoreToCount(save);
   uploads++;TextureHelper.setBitmap(engine,textures[i],0,bitmaps[i],handler,()->{uploads--;if(disposed){if(uploads==0)recycle();}else if(dirty)requestFrame.run();});textures[i].generateMipmaps(engine);
  }
  if(!ready){ready=true;scene.addEntity(entity);content.setGpuBooks(true);}
 }
 /** Release only after beginFrame has committed the new sampler bindings and
  * endFrame has queued their draw. Earlier release leaves a stale sampler handle.
  */
 void releaseRetiredTextures(){solidBodies.releaseRetired();solidSpines.releaseRetired();for(Texture t:retired)engine.destroyTexture(t);retired.clear();}
 void setLighting(float day,float cloud,boolean lamp,float progress){
  labels.setParameter("baseColorFactor",1f,1f,1f,Math.max(0,1-progress*4));
 }
 private void fillSpines(Canvas canvas){
  android.graphics.Paint paint=new android.graphics.Paint();
  for(int r=1;r<content.getChildCount();r++){android.view.View rowView=content.getChildAt(r);if(!(rowView.getTag() instanceof Integer)||(Integer)rowView.getTag()>=9||!(rowView instanceof android.view.ViewGroup))continue;
   android.view.ViewGroup row=(android.view.ViewGroup)rowView;for(int i=0;i<row.getChildCount();i++)if(row.getChildAt(i) instanceof BookArtworkView){BookArtworkView book=(BookArtworkView)row.getChildAt(i);paint.setColor(coverColor(book));canvas.drawRect(row.getLeft()+book.getLeft()+book.spine.left,row.getTop()+book.getTop()+book.spine.top,row.getLeft()+book.getLeft()+book.spine.right,row.getTop()+book.getTop()+book.spine.bottom,paint);}}
 }
 private static int coverColor(BookArtworkView b){if(b.artwork!=null)return b.artwork.getPixel(Math.max(0,b.artwork.getWidth()/3),Math.max(0,b.artwork.getHeight()/6))|0xff000000;return 0xff253d47;}
 private void updateGeometry(){
  java.util.ArrayList<SolidBookGeometry.Book> data=new java.util.ArrayList<>();float w=content.getWidth(),h=content.getHeight(),ww=RoomModelGeometry.RIGHT-RoomModelGeometry.LEFT,hh=RoomModelGeometry.TOP-RoomModelGeometry.BOTTOM;
  for(int r=1;r<content.getChildCount();r++){android.view.View rowView=content.getChildAt(r);if(!(rowView.getTag() instanceof Integer)||(Integer)rowView.getTag()>=9||!(rowView instanceof android.view.ViewGroup))continue;
   int cell=(Integer)rowView.getTag();android.view.ViewGroup row=(android.view.ViewGroup)rowView;
   for(int i=0;i<row.getChildCount();i++)if(row.getChildAt(i) instanceof BookArtworkView){
    BookArtworkView b=(BookArtworkView)row.getChildAt(i);float l=row.getLeft()+b.getLeft()+b.spine.left,t=row.getTop()+b.getTop()+b.spine.top,rr=row.getLeft()+b.getLeft()+b.spine.right,bb=row.getTop()+b.getTop()+b.spine.bottom;
    if(rr<=l||bb<=t)continue;float[] uv={(l+.35f)/w,(t+.35f)/h,(rr-.35f)/w,(bb-.35f)/h};
    float depth=.26f+.025f*Math.floorMod(b.item.optString("url").hashCode(),4);
    data.add(new SolidBookGeometry.Book(RoomModelGeometry.LEFT+l/w*ww,RoomModelGeometry.shelfTop(cell)+.0005f,RoomModelGeometry.LEFT+(row.getLeft()+b.getLeft()+b.paper.right)/w*ww,RoomModelGeometry.TOP-t/h*hh,RoomModelGeometry.FRONT,depth,uv,ModelMesh.rgb(coverColor(b))));
   }
  }
  SolidBookGeometry.Result mesh=SolidBookGeometry.build(data);solidCount=mesh.books;solidBodies.upload(mesh.body);solidSpines.upload(mesh.spines);
 }
 private void recycle(){for(int i=0;i<2;i++)if(bitmaps[i]!=null){bitmaps[i].recycle();bitmaps[i]=null;canvases[i]=null;}}
 void dispose(){
  disposed=true;if(content!=null)content.setGpuBooks(false);scene.removeEntity(entity);engine.destroyEntity(entity);EntityManager.get().destroy(entity);
  solidBodies.dispose();solidSpines.dispose();engine.destroyMaterialInstance(bodyMaterial);engine.destroyVertexBuffer(vertices);engine.destroyIndexBuffer(indices);engine.destroyMaterialInstance(books);engine.destroyMaterialInstance(labels);for(Texture t:textures)if(t!=null)engine.destroyTexture(t);releaseRetiredTextures();if(uploads==0)recycle();
 }
}
