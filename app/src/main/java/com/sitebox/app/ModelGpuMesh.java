package com.sitebox.app;
import com.google.android.filament.*;
import java.nio.*;
import java.util.ArrayList;
/** Bounded reusable mesh buffers. Replaced buffers retire only after a submitted frame. */
final class ModelGpuMesh {
 private final Engine engine;private final Scene scene;private final MaterialInstance material;private final int entity;
 private VertexBuffer vertices;private IndexBuffer indices;private int vertexCapacity,indexCapacity;private boolean built,inScene;private final boolean shadows;private final int priority;
 private final ArrayList<VertexBuffer> oldVertices=new ArrayList<>();private final ArrayList<IndexBuffer> oldIndices=new ArrayList<>();
 int triangleCount;
 ModelGpuMesh(Engine e,Scene s,MaterialInstance m,boolean shadows,int priority){engine=e;scene=s;material=m;this.shadows=shadows;this.priority=priority;entity=EntityManager.get().create();}
 void upload(ModelMesh.Data data){
  triangleCount=data.indices.length/3;if(data.indices.length==0){if(inScene){scene.removeEntity(entity);inScene=false;}return;}
  int vc=data.vertices.length/15,ic=data.indices.length;
  if(vertices==null||vc>vertexCapacity){if(vertices!=null)oldVertices.add(vertices);vertexCapacity=capacity(vc);vertices=new VertexBuffer.Builder().vertexCount(vertexCapacity).bufferCount(1)
   .attribute(VertexBuffer.VertexAttribute.POSITION,0,VertexBuffer.AttributeType.FLOAT3,0,60).attribute(VertexBuffer.VertexAttribute.TANGENTS,0,VertexBuffer.AttributeType.FLOAT4,12,60)
   .attribute(VertexBuffer.VertexAttribute.UV0,0,VertexBuffer.AttributeType.FLOAT2,28,60).attribute(VertexBuffer.VertexAttribute.UV1,0,VertexBuffer.AttributeType.FLOAT2,36,60).attribute(VertexBuffer.VertexAttribute.COLOR,0,VertexBuffer.AttributeType.FLOAT4,44,60).build(engine);}
  if(indices==null||ic>indexCapacity){if(indices!=null)oldIndices.add(indices);indexCapacity=capacity(ic);indices=new IndexBuffer.Builder().indexCount(indexCapacity).bufferType(IndexBuffer.Builder.IndexType.UINT).build(engine);}
  FloatBuffer v=ByteBuffer.allocateDirect(data.vertices.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();v.put(data.vertices).flip();vertices.setBufferAt(engine,0,v);
  IntBuffer i=ByteBuffer.allocateDirect(data.indices.length*4).order(ByteOrder.nativeOrder()).asIntBuffer();i.put(data.indices).flip();indices.setBuffer(engine,i);
  float[] min=data.min,max=data.max;Box bounds=new Box((min[0]+max[0])/2,(min[1]+max[1])/2,(min[2]+max[2])/2,Math.max(.001f,(max[0]-min[0])/2),Math.max(.001f,(max[1]-min[1])/2),Math.max(.001f,(max[2]-min[2])/2));
  if(!built){new RenderableManager.Builder(1).geometry(0,RenderableManager.PrimitiveType.TRIANGLES,vertices,indices,0,ic).material(0,material).boundingBox(bounds).priority(priority).castShadows(shadows).receiveShadows(shadows).screenSpaceContactShadows(shadows).build(engine,entity);built=true;}
  else{RenderableManager rm=engine.getRenderableManager();int instance=rm.getInstance(entity);rm.setGeometryAt(instance,0,RenderableManager.PrimitiveType.TRIANGLES,vertices,indices,0,ic);rm.setAxisAlignedBoundingBox(instance,bounds);}
  if(!inScene){scene.addEntity(entity);inScene=true;}
 }
 private static int capacity(int value){int n=64;while(n<value)n*=2;return n;}
 void releaseRetired(){for(VertexBuffer v:oldVertices)engine.destroyVertexBuffer(v);for(IndexBuffer i:oldIndices)engine.destroyIndexBuffer(i);oldVertices.clear();oldIndices.clear();}
 void dispose(){if(inScene)scene.removeEntity(entity);engine.destroyEntity(entity);EntityManager.get().destroy(entity);if(vertices!=null)engine.destroyVertexBuffer(vertices);if(indices!=null)engine.destroyIndexBuffer(indices);releaseRetired();}
}
