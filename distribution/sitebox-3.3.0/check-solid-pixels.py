from pathlib import Path
import sys
from PIL import Image,ImageStat,ImageChops
root=Path(sys.argv[1]);on=Image.open(root/'solid-night-room.png').convert('RGB');off=Image.open(root/'solid-night-lamp-off.png').convert('RGB');day=Image.open(root/'solid-day-room.png').convert('RGB');w,h=on.size
luma=lambda c:sum(x*y for x,y in zip(c,(.2126,.7152,.0722)))
def crop(im,box):return im.crop(tuple(round(v*(w if i%2==0 else h)) for i,v in enumerate(box)))
def mean(im,box):return ImageStat.Stat(crop(im,box)).mean
box=(.417,.491,.5,.504);gain=luma(mean(on,box))-luma(mean(off,box));print(f'Measured desk luma gain with lamp on: {gain:.3f}',flush=True);assert gain>5,'Desk must be lit, not just the bulb glyph'
print('PASS native desk spotlight changes desk surface, beyond the bulb alone')
window=(.60,.36,.74,.42);delta=max(abs(a-b) for a,b in zip(mean(on,window),mean(off,window)));assert delta<3,(delta,)
print(f'PASS outdoor window remains independent of desk lamp (mean RGB delta {delta:.3f})')
ratio=luma(mean(on,(0,.05,1,.96)))/luma(mean(day,(0,.05,1,.96)));assert .2<ratio<.85,ratio
print(f'PASS night remains readable and visibly darker than day (luma ratio {ratio:.3f})')
