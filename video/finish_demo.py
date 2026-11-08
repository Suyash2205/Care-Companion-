"""
finish_demo.py — re-render only the closing scene of the demonstration film and
re-assemble, reusing the scene clips already rendered in video/build/.

Used after a text change confined to one scene, so that a two-word correction
does not cost a twenty-five minute full re-render.

    python video/finish_demo.py d14
"""
import os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(ROOT, "docs"))

import make_ad as A
import make_demo as M

WORK = A.WORK
OUT = M.OUT
only = sys.argv[1] if len(sys.argv) > 1 else None

scenes = M.build_layers()           # regenerates every layer PNG from source
plate = os.path.join(WORK, "plate.mp4")
plate_dur = A.duration(plate)

clips, durs, wavs = [], [], []
for i, s in enumerate(scenes):
    clip = os.path.join(WORK, s["name"] + "_v.mp4")
    wav = os.path.join(WORK, s["name"] + ".wav")
    if only and s["name"] != only and os.path.exists(clip) and os.path.exists(wav):
        dur = A.duration(clip)
        print(f"  {s['name']}  {dur:5.2f}s  (reused)")
    else:
        clip, dur, wav = A.build_scene(s, plate, plate_dur, i)
        print(f"  {s['name']}  {dur:5.2f}s  (re-rendered)")
    clips.append(clip); durs.append(dur); wavs.append((wav, s["lead"]))

M.assemble(clips, durs, wavs)
