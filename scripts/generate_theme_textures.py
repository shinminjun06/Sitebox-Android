"""Generate bundled, replaceable SiteBox PNG theme assets before Android build."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import math, random

ROOT=Path("app/src/main/assets/themes")
random.seed(9021)
woods={
 "oak":((220,169,107),(108,65,34)),
 "walnut":((112,72,49),(49,29,21)),
 "white_oak":((232,217,188),(156,132,103)),
}
for key,(light,dark) in woods.items():
 w,h=384,768
 im=Image.new("RGB",(w,h))
 px=im.load()
 waves=[random.uniform(0,6.28) for _ in range(22)]
 for x in range(w):
  grain=math.sin(x*.095+math.sin(x*.014)*2.5)*.09+math.sin(x*.42)*.025
  grain+=sum(math.sin(x*(.011+j*.002)+waves[j])* (.008 if j%3 else .015) for j in range(12))
  for y in range(h):
   variation=grain+math.sin(y*.012+x*.045)*.018+random.uniform(-.022,.022)
   shade=max(0,min(1,.73+variation))
   px[x,y]=tuple(int(d+(l-d)*shade) for l,d in zip(light,dark))
 draw=ImageDraw.Draw(im,"RGBA")
 for n in range(40):
  x=random.randrange(w)
  points=[(x+math.sin(y/70+n)*random.uniform(0,3),y) for y in range(0,h,12)]
  draw.line(points,fill=(72,35,19,random.randrange(8,32)),width=random.choice([1,1,2]))
 p=ROOT/"shelves"/key/"frame.png";p.parent.mkdir(parents=True,exist_ok=True);im.save(p,optimize=True)

colors={
 "classic":[(30,48,77),(117,39,50),(215,196,157),(27,73,65),(110,62,38),(37,91,87),(45,65,103),(161,128,91)],
 "pastel":[(145,177,187),(214,162,174),(236,213,169),(148,188,162),(206,174,151),(156,195,187),(167,177,215),(224,196,173)],
 "modern":[(35,46,57),(89,100,111),(223,217,202),(42,77,81),(105,93,85),(77,116,125),(59,72,91),(172,158,138)]
}
names=["navy","burgundy","ivory","green","brown","teal","blue","tan"]
for style,palette in colors.items():
 for name,base in zip(names,palette):
  w,h=144,640
  im=Image.new("RGBA",(w,h))
  pix=im.load()
  for x in range(8,137):
   edge=.65+.35*math.sin(math.pi*(x-8)/129)
   for y in range(10,633):
    n=random.uniform(-7,7)
    v=math.sin(y*.16+x*.09)*2.2
    pix[x,y]=(*[int(max(0,min(255,c*edge+n+v))) for c in base],255)
  d=ImageDraw.Draw(im,"RGBA")
  d.rounded_rectangle((7,9,136,633),radius=8,outline=(23,18,13,220),width=3)
  d.rectangle((123,18,134,626),fill=(245,224,179,220))
  for x in (125,128,131):d.line((x,22,x,623),fill=(150,120,82,90),width=1)
  gild=(232,192,105,240) if style=="classic" else ((249,244,226,220) if style=="pastel" else (190,202,207,220))
  for y in (34,39,48,55,578,585,594,600):
   d.line((17,y,119,y),fill=gild,width=2)
  d.rectangle((20,91,115,548),outline=gild,width=2)
  if style=="classic":
   for y in (123,174,465,518):
    d.polygon([(68,y-18),(84,y),(68,y+18),(52,y)],outline=gild)
    d.ellipse((64,y-4,72,y+4),fill=gild)
   for y in range(225,425,23):
    d.arc((37,y-13,99,y+18),0,180,fill=gild,width=2)
  elif style=="pastel":
   for y in (150,490):
    d.ellipse((49,y-18,87,y+18),outline=gild,width=3)
  else:
   for y in (140,480):d.line((36,y,100,y),fill=gild,width=4)
  p=ROOT/"books"/style/(name+".png");p.parent.mkdir(parents=True,exist_ok=True);im.save(p,optimize=True)
print("Generated 27 PNG textures")
