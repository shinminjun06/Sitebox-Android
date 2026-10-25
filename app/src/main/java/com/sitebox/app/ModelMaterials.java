package com.sitebox.app;
import com.google.android.filament.*;
import com.google.android.filament.gltfio.*;
/** Small shared glTF-compatible material factory; callers own returned instances. */
final class ModelMaterials {
 static MaterialInstance create(Engine engine,UbershaderProvider provider,String name,boolean texture,boolean colors,boolean unlit){return create(engine,provider,name,texture,colors,unlit,false);}
 static MaterialInstance create(Engine engine,UbershaderProvider provider,String name,boolean texture,boolean colors,boolean unlit,boolean blend){
  MaterialProvider.MaterialKey key=new MaterialProvider.MaterialKey();key.unlit=unlit;key.hasBaseColorTexture=texture;key.baseColorUV=0;key.hasVertexColors=colors;key.doubleSided=blend||unlit;key.alphaMode=blend?2:0;
  MaterialInstance result=provider.createMaterialInstance(key,new int[8],name,null);if(result==null)throw new IllegalStateException("Cannot create "+name);
  result.setParameter("baseColorFactor",1f,1f,1f,1f);if(!unlit){result.setParameter("metallicFactor",0f);result.setParameter("roughnessFactor",.78f);}result.setDepthCulling(true);result.setDepthWrite(!blend);return result;
 }
}
