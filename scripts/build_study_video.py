"""Build quiet six-second H.264 cinemagraphs from the approved theme plates.
Only the app's glass pane mask exposes the moving pixels. No camera movement.
"""
from pathlib import Path
import subprocess

root=Path(__file__).resolve().parents[1]
for orientation,w,h in [('portrait',512,1104),('landscape',960,540),('night_portrait',512,1104),('night_landscape',960,540)]:
    source=root/'app/src/main/assets/scenes'/('study_'+orientation+'.webp')
    output=source.with_suffix('.mp4')
    # A bounded sub-pixel breeze, periodic in six seconds; no frame-boundary jump.
    dx="1.35*sin(2*PI*T/6)*sin(Y/47)*gt(X,W*0.60)*lt(Y,H*0.48)"
    dy="0.65*sin(2*PI*T/6)*sin(X/61)*gt(X,W*0.60)*lt(Y,H*0.48)"
    rgb=':'.join(c+"='"+c+"(clip(X+"+dx+",0,W-1),clip(Y+"+dy+",0,H-1))'" for c in ['r','g','b'])
    subprocess.run(['ffmpeg','-y','-hide_banner','-loglevel','error','-loop','1','-i',str(source),
        '-vf',f'scale={w}:{h},format=gbrp,geq={rgb},format=yuv420p','-t','6','-r','24',
        '-an','-c:v','libx264','-preset','medium','-crf','24','-profile:v','baseline','-level','3.1',
        '-pix_fmt','yuv420p','-movflags','+faststart',str(output)],check=True)
    print(output.name,output.stat().st_size,flush=True)
