"""Validate shipped artwork and metadata without regenerating textures."""
import json
from pathlib import Path
from PIL import Image
root=Path('app/src/main/assets/themes')
count=0
for kind,keys in [('shelves',['oak','walnut','white_oak']),('books',['classic','pastel','modern'])]:
 for key in keys:
  directory=root/kind/key
  config=json.loads((directory/'theme.json').read_text())
  for name in [config['image']] if kind=='shelves' else config['images']:
   path=directory/name
   assert path.resolve().is_relative_to(directory.resolve())
   im=Image.open(path);im.load()
   assert max(im.size)<=2048 and im.mode=='RGBA', (path,im.size,im.mode)
   assert im.getchannel('A').getextrema()[1]>200
   count+=1
assert count==15
print('PASS: 15 raster assets, 6 theme manifests; dimensions, format, alpha, paths')

for name,wide in [('portrait',False),('landscape',True)]:
 path=Path('app/src/main/assets/scenes')/('classroom_'+name+'.webp')
 with Image.open(path) as room:
  room.load()
  assert room.format=='WEBP' and room.mode=='RGB',path
  assert (room.width>room.height)==wide and max(room.size)<=2048,(path,room.size)
  assert path.stat().st_size<500000,path
print('PASS: portrait and landscape classroom backgrounds; orientation, decode, memory and file-size bounds')

scene_hashes=set()
for key in ['library','bedroom','greenhouse','cafe','tatami','attic','lab','wizard','seaside','train']:
 path=Path('app/src/main/assets/scenes')/(key+'.webp')
 with Image.open(path) as room:
  room.load()
  assert room.format=='WEBP' and room.mode=='RGB' and room.height>room.width,(key,room.size)
  assert max(room.size)<=2048 and path.stat().st_size<500000,path
 import hashlib
 scene_hashes.add(hashlib.sha256(path.read_bytes()).hexdigest())
assert len(scene_hashes)==10,'Each concept needs distinct artwork'
print('PASS: ten unique portrait background concepts; bounded WebP assets')

# Independent full-grain timber surfaces: decode every shipped finish, no alpha or sprite strip.
surfaces=[]
for key in ['oak','walnut','white_oak']:
 path=root/'shelves'/key/'surface.webp'
 with Image.open(path) as surface:
  surface.load()
  assert surface.format=='WEBP' and surface.mode=='RGB' and surface.size==(1024,1024),(path,surface.mode,surface.size)
  assert path.stat().st_size<500000,path
  assert surface.getextrema()[0][1]-surface.getextrema()[0][0]>20,path
 surfaces.append(path)
print(f'PASS: {len(surfaces)} square RGB wood surfaces decoded; bounded file size and full-grain contrast')

# Theme 1 plates are shelf-free backgrounds, bounded independently of furniture.
for orientation,wide in [('portrait',False),('landscape',True)]:
 p=Path('app/src/main/assets/scenes')/('study_'+orientation+'.webp')
 with Image.open(p) as im:
  im.load();assert im.mode=='RGB' and im.format=='WEBP'
  assert (im.width>im.height)==wide and max(im.size)<=1920
  assert im.width*im.height*4<=8*1024*1024 and p.stat().st_size<500000
print('PASS: theme 1 portrait and landscape plates fit room cache and APK size budget')
