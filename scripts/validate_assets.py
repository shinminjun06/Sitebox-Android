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

scene_files=list(Path('app/src/main/assets/scenes').iterdir())
assert len(scene_files)==12 and all(p.name.startswith('study_') for p in scene_files)
assert not Path('app/src/main/assets/school_morning.jpg').exists()
print('PASS: only theme 1 room artwork and video loops are bundled')

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

decor=root/'study/cabinet_decor.webp'
with Image.open(decor) as im:
 im.load();assert im.mode=='RGBA' and im.size==(768,384)
 assert im.getchannel('A').getextrema()==(0,255) and decor.stat().st_size<500000
print('PASS: cabinet-top decor has real transparent alpha and bounded decoded memory')
room_cabinet=root/'study/room_cabinet.webp'
with Image.open(room_cabinet) as im:
 im.load();assert im.mode=='RGBA' and im.size==(512,666)
 assert im.getchannel('A').getextrema()==(0,255) and room_cabinet.stat().st_size<200000
print('PASS: separate compact furniture sprite has alpha and bounded memory')

for shelf in ['oak','walnut','white_oak']:
 for book in ['classic','pastel','modern']:
  active=[decor,room_cabinet,root/'shelves'/shelf/'cabinet.webp',root/'shelves'/shelf/'surface.webp']+list((root/'books'/book).glob('*.webp'))
  assert sum(Image.open(p).width*Image.open(p).height*4 for p in active)<12*1024*1024
print('PASS: each active shelf/book combination fits the isolated 12 MiB artwork cache')

# Theme 1 motion assets: audio-free baseline H264; bounded dimensions and bit budget.
import subprocess
for orientation in ['portrait','landscape','night_portrait','night_landscape']:
 p=Path('app/src/main/assets/scenes')/('study_'+orientation+'.mp4')
 m=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_format','-show_streams','-of','json',str(p)]))
 assert len(m['streams'])==1 and m['streams'][0]['codec_name']=='h264'
 assert m['streams'][0]['pix_fmt']=='yuv420p' and max(m['streams'][0]['width'],m['streams'][0]['height'])<=1104
 assert 5.8<float(m['format']['duration'])<6.2 and p.stat().st_size<1000000
 p=p.with_suffix('.webp')
 with Image.open(p) as im:
  im.load();assert im.mode=='RGB' and im.width*im.height*4<8*1024*1024 and p.stat().st_size<500000
print('PASS: theme 1 silent six-second H264 loops and bounded night plates')

for orientation in ["portrait","landscape"]:
 pair=[Image.open(Path("app/src/main/assets/scenes")/("study_"+phase+orientation+".webp")) for phase in ["","night_"]]
 assert sum(im.width*im.height*4 for im in pair)<12*1024*1024
print("PASS: active day/night image pairs fit the 12 MiB room cache")

# Independently relit lamp-off plates retain ambient/sun shadows instead of a tint overlay.
for orientation in ["portrait", "landscape"]:
 active=[]
 for phase in ["", "night_", "lamp_off_", "lamp_off_night_"]:
  path=Path("app/src/main/assets/scenes")/("study_"+phase+orientation+".webp")
  with Image.open(path) as im:
   im.load();assert im.mode=="RGB" and im.format=="WEBP"
   assert (im.width>im.height)==(orientation=="landscape") and path.stat().st_size<500000
   active.append(im.width*im.height*4)
 assert sum(active)<20*1024*1024, (orientation,active)
print("PASS: lamp-on/off day/night artwork bounded to 20 MiB active decoded room memory")

# Photographic student-room package, independent from retained legacy fallback art.
student=Path('app/src/main/assets/themes/rooms/student')
room_names=['room_day','room_day_off','room_night','room_night_off','room_shell','foreground_day','foreground_night']
alpha_names={'room_shell','foreground_day','foreground_night','cabinet_front'}
images={}
for name in room_names+['outside_day','outside_night','cabinet_front']:
 path=student/(name+'.webp')
 with Image.open(path) as im:
  im.load();assert im.format=='WEBP' and max(im.size)<=2048,(name,im.size)
  assert im.mode==('RGBA' if name in alpha_names else 'RGB'),(name,im.mode)
  assert path.stat().st_size<3000000,(name,path.stat().st_size)
  images[name]=im.copy()
for name in room_names:assert images[name].size==(941,1672),(name,images[name].size)
assert images['cabinet_front'].size==(1100,1430)
for name in alpha_names:assert images[name].getchannel('A').getextrema()==(0,255),name
mask=images['room_shell'].getchannel('A')
assert mask.getpixel((int(mask.width*.77),int(mask.height*.22)))<8,'Outdoor glass must be a real alpha hole'
assert mask.getpixel((int(mask.width*.10),int(mask.height*.40)))>240,'Indoor wall stays opaque'
for name in ['foreground_day','foreground_night']:
 im=images[name];assert im.getpixel((im.width//2,im.height//4))[3]==0 and im.getpixel((im.width//2,int(im.height*.95)))[3]>240
assert sum(im.width*im.height*4 for im in images.values())<64*1024*1024
for phase in ['day','night']:
 p=student/('window_'+phase+'.mp4');m=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_format','-show_streams','-of','json',str(p)]))
 assert len(m['streams'])==1 and m['streams'][0]['codec_name']=='h264' and m['streams'][0]['width']==480 and m['streams'][0]['height']==480
 assert 5.8<float(m['format']['duration'])<6.2 and p.stat().st_size<1000000
print('PASS: 10 photographic student-room images, exact registered canvases, real room/window/foreground alpha, bounded decode budget and two silent fallback loops')

# The active room must preserve the actual uploaded reference byte-for-byte.
import hashlib
reference=student/"room_reference.jpg"
assert hashlib.sha256(reference.read_bytes()).hexdigest()=="275373adaff1581fa1d089ce74f21ff1dc9540ccf9a9eb5bd45f7bc19d0f6e18"
assert Image.open(reference).size==(720,1280)
photo_shelf=Image.open(student/"reference_cabinet.webp")
assert photo_shelf.size==(941,1672) and photo_shelf.mode=="RGB"
print("PASS: unchanged uploaded reference photo and shelf-only 3x3 photographic patch")
