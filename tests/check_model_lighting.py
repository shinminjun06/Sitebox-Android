"""Check actual Android captures, rather than accepting a changed state label."""
from pathlib import Path
import sys
from PIL import Image, ImageStat

root = Path(sys.argv[1])
def mean(name, box):
    with Image.open(root / name) as source:
        w, h = source.size
        region = source.convert('RGB').crop(tuple(round(v * (w if i % 2 == 0 else h)) for i, v in enumerate(box)))
        return ImageStat.Stat(region).mean

day = mean('3d-day.png', (0, .05, 1, .96))
night = mean('3d-night.png', (0, .05, 1, .96))
luma = lambda rgb: .2126 * rgb[0] + .7152 * rgb[1] + .0722 * rgb[2]
ratio = luma(night) / luma(day)
assert .20 < ratio < .85, f'Night must remain readable and visibly darker than day; luma ratio={ratio:.3f}'
on = mean('3d-night.png', (.60, .36, .74, .42))
off = mean('3d-night-lamp-off.png', (.60, .36, .74, .42))
assert max(abs(a - b) for a, b in zip(on, off)) < 3, (on, off)
print(f'PASS captured night room remains readable and darker than day (luma ratio {ratio:.3f})')
print('PASS captured outdoor window stays stable when the indoor lamp turns off')
