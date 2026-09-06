#!/usr/bin/env python3
"""Quick solid-color review render for the generated vanilla block model."""

import json
import math
from pathlib import Path

import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d.art3d import Poly3DCollection

ROOT = Path(__file__).resolve().parents[1]
MODEL = ROOT / "src/main/resources/assets/sgjdestiny_dhd/models/block/destiny_dhd.json"
OUT = ROOT / "destiny-console-preview.png"

COLORS = {
    "#hull": "#315e55", "#hull_dark": "#172522", "#metal": "#45443f",
    "#bronze": "#8b633b", "#screen": "#050a0d", "#light": "#e5e7bd",
    "#cable": "#151515", "#pedestal": "#392c25", "#dial": "#d6cf9f",
}


def rotate(point, rotation):
    if not rotation:
        return point
    x, y, z = point
    ox, oy, oz = rotation["origin"]
    x, y, z = x - ox, y - oy, z - oz
    a = math.radians(rotation["angle"])
    c, s = math.cos(a), math.sin(a)
    if rotation["axis"] == "x":
        y, z = y * c - z * s, y * s + z * c
    elif rotation["axis"] == "y":
        x, z = x * c + z * s, -x * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    return x + ox, y + oy, z + oz


def faces(element):
    x1, y1, z1 = element["from"]
    x2, y2, z2 = element["to"]
    pts = [(x1,y1,z1),(x2,y1,z1),(x2,y2,z1),(x1,y2,z1),
           (x1,y1,z2),(x2,y1,z2),(x2,y2,z2),(x1,y2,z2)]
    pts = [rotate(p, element.get("rotation")) for p in pts]
    # Plot coordinates are model X, model Z (depth), model Y (height).
    pts = [(x, z, y) for x, y, z in pts]
    return [[pts[i] for i in ids] for ids in
            ((0,1,2,3),(4,5,6,7),(0,1,5,4),(2,3,7,6),(1,2,6,5),(0,3,7,4))]


model = json.loads(MODEL.read_text())
fig = plt.figure(figsize=(18, 7), facecolor="#111")
for index, (elev, azim, title) in enumerate(((12,-90,"operator front"),(24,-58,"operator quarter"),(28,35,"back quarter")), 1):
    ax = fig.add_subplot(1, 3, index, projection="3d")
    ax.set_facecolor("#111")
    for element in model["elements"]:
        tex = element["faces"]["north"]["texture"]
        color = COLORS.get(tex, "#888")
        poly = Poly3DCollection(faces(element), facecolor=color, edgecolor="#0b0b0b", linewidth=.15, alpha=1)
        ax.add_collection3d(poly)
    ax.set_xlim(-18, 34); ax.set_ylim(-8, 23); ax.set_zlim(0, 31)
    ax.set_box_aspect((52, 31, 31))
    ax.view_init(elev=elev, azim=azim)
    ax.set_axis_off(); ax.set_title(title, color="white")
fig.tight_layout()
fig.savefig(OUT, dpi=180, facecolor=fig.get_facecolor(), bbox_inches="tight")
print(OUT)
