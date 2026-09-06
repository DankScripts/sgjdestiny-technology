#!/usr/bin/env python3
"""Generate the alpha-1 Destiny console model, editable bbmodel, and textures."""

from __future__ import annotations

import json
import math
import random
import uuid
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
MODEL_OUT = ROOT / "src/main/resources/assets/sgjdestiny_dhd/models/block/destiny_dhd.json"
TEX_OUT = ROOT / "src/main/resources/assets/sgjdestiny_dhd/textures/block"
BBMODEL_OUT = ROOT / "blockbench/destiny_dialing_console_alpha1.bbmodel"

TEXTURES = {
    "hull": "sgjdestiny_dhd:block/destiny_hull",
    "hull_dark": "sgjdestiny_dhd:block/destiny_hull_dark",
    "metal": "sgjdestiny_dhd:block/destiny_metal",
    "bronze": "sgjdestiny_dhd:block/destiny_bronze",
    "screen": "sgjdestiny_dhd:block/destiny_screen_off",
    "light": "sgjdestiny_dhd:block/destiny_light",
    "cable": "sgjdestiny_dhd:block/destiny_cable",
    "pedestal": "sgjdestiny_dhd:block/destiny_pedestal",
}

elements: list[dict] = []


def cube(name, frm, to, texture, rotation=None, shade=True):
    face = {"uv": [0, 0, 16, 16], "texture": f"#{texture}"}
    element = {
        "name": name,
        "from": [round(v, 4) for v in frm],
        "to": [round(v, 4) for v in to],
        "shade": shade,
        "faces": {side: dict(face) for side in ("north", "east", "south", "west", "up", "down")},
    }
    if rotation:
        axis, angle, origin = rotation
        element["rotation"] = {
            "angle": angle,
            "axis": axis,
            "origin": origin,
            "rescale": False,
        }
    elements.append(element)


def build_geometry():
    # Broad floor plinth and twin tapered supports, matching the freestanding prop.
    cube("base_center", (-5, 0, 3), (21, 1.5, 15), "pedestal")
    cube("base_left", (-9, 0, 5), (-5, 1.5, 13), "pedestal")
    cube("base_right", (21, 0, 5), (25, 1.5, 13), "pedestal")
    cube("base_front_left", (-7, 0, 1), (3, 1.2, 5), "metal", ("y", -22.5, [-5, 1, 4]))
    cube("base_front_right", (13, 0, 1), (23, 1.2, 5), "metal", ("y", 22.5, [21, 1, 4]))
    cube("left_leg_lower", (-7.5, 1, 6), (-3.2, 8.2, 11), "pedestal", ("z", -22.5, [-5, 7, 8]))
    cube("left_leg_upper", (-5.7, 6.6, 5.5), (-1.4, 15.2, 10.7), "pedestal", ("z", 22.5, [-4, 8, 8]))
    cube("right_leg_lower", (19.2, 1, 6), (23.5, 8.2, 11), "pedestal", ("z", 22.5, [21, 7, 8]))
    cube("right_leg_upper", (17.4, 6.6, 5.5), (21.7, 15.2, 10.7), "pedestal", ("z", -22.5, [20, 8, 8]))
    for x in (-6.5, -5.25, -4.0, 19.9, 21.15, 22.4):
        cube("leg_flute", (x, 2.0, 5.55), (x + .38, 13.2, 5.9), "bronze")

    # Deep console body: thin work surface over a substantial sculpted underbody.
    cube("body_center", (-5, 12.8, -2), (21, 17.0, 19), "hull_dark")
    cube("deck_center", (-6, 15.3, -3), (22, 18.1, 20), "hull")
    cube("deck_left", (-14, 15.3, -1), (-5, 18.1, 18), "hull")
    cube("deck_right", (21, 15.3, -1), (30, 18.1, 18), "hull")
    cube("left_rounded_tip", (-16, 15.5, 3), (-13, 17.9, 15), "hull")
    cube("right_rounded_tip", (29, 15.5, 3), (32, 17.9, 15), "hull")
    cube("left_underwing", (-13, 13.6, 1), (-5, 15.5, 17), "hull_dark")
    cube("right_underwing", (21, 13.6, 1), (29, 15.5, 17), "hull_dark")

    # Segmented dark-metal perimeter. Axis-aligned steps read cleaner than long rails.
    front_arc = [
        (-13, -4.5, -8, -2.7), (-8, -5.5, -2, -3.6), (-2, -6.0, 5, -4.1),
        (5, -6.2, 11, -4.3), (11, -6.0, 18, -4.1), (18, -5.5, 24, -3.6),
        (24, -4.5, 29, -2.7),
    ]
    for i, (x1, z1, x2, z2) in enumerate(front_arc):
        cube(f"front_bumper_{i}", (x1, 15.0, z1), (x2, 18.7, z2), "metal")
        if i in (0, 2, 4, 6):
            cube(f"front_bronze_clasp_{i}", (x1 + .35, 16.0, z1 - .12), (x1 + 1.5, 17.7, z2 + .12), "bronze")
    cube("left_side_bumper", (-16, 15.0, 2), (-14, 18.7, 16), "metal")
    cube("right_side_bumper", (30, 15.0, 2), (32, 18.7, 16), "metal")
    cube("left_rear_bumper", (-14, 15.0, 17), (-3, 18.7, 20), "metal")
    cube("right_rear_bumper", (19, 15.0, 17), (30, 18.7, 20), "metal")
    for x in (-12.8, -10.2, -7.6, -5.0, 21.0, 23.6, 26.2, 28.8):
        cube("bumper_seam", (x, 15.2, 18.85), (x + .28, 18.5, 20.12), "bronze")

    # Recessed left and right control islands with organized, varied controls.
    for side, x1, x2 in (("left", -13.2, -4.2), ("right", 20.2, 29.2)):
        cube(f"{side}_panel_recess", (x1, 18.05, .2), (x2, 18.42, 15.8), "hull_dark")
        cube(f"{side}_panel_inner", (x1 + .7, 18.4, 1.2), (x2 - .7, 18.62, 14.9), "metal")
        for row in range(4):
            for col in range(3):
                x = x1 + 1.25 + col * 2.25
                z = 2.0 + row * 2.55 + (0.25 if col == 1 else 0)
                key_tex = "light" if (row, col) in ((0, 0), (1, 2), (3, 1)) else "hull"
                cube(f"{side}_key", (x, 18.62, z), (x + 1.15, 19.0, z + .8), key_tex, shade=False)
        cube(f"{side}_round_pad_a", (x1 + 1.0, 18.62, 12.6), (x1 + 3.0, 19.05, 14.6), "light", shade=False)
        cube(f"{side}_round_pad_b", (x2 - 2.8, 18.62, 12.8), (x2 - 1.0, 19.05, 14.6), "light", shade=False)

    # Central keyboard tray, separated from the screen as on the physical prop.
    cube("keyboard_recess", (-3.3, 18.05, 1.0), (4.8, 18.45, 14.8), "hull_dark")
    cube("keyboard_plate", (-2.7, 18.43, 2.0), (4.2, 18.68, 13.8), "metal")
    for row in range(4):
        for col in range(4):
            x = -2.2 + col * 1.55
            z = 2.8 + row * 2.35
            cube("keyboard_key", (x, 18.68, z), (x + .95, 19.02, z + 1.15), "hull")

    # Compact raised monitor: green shell, inset dark glass, layered metal piping.
    screen_rotation = ("x", -22.5, [11, 18, 10])
    cube("monitor_back", (0.0, 17.4, 8.8), (22.0, 29.0, 11.8), "hull_dark", screen_rotation)
    cube("monitor_shell", (1.0, 18.0, 8.1), (21.0, 28.5, 11.0), "hull", screen_rotation)
    cube("screen_glass", (4.1, 20.4, 7.45), (19.1, 26.6, 7.78), "screen", screen_rotation, shade=False)
    cube("screen_top_bezel", (3.2, 26.5, 7.3), (19.8, 28.0, 8.45), "hull", screen_rotation)
    cube("screen_bottom_bezel", (3.2, 18.8, 7.3), (19.8, 20.5, 8.45), "hull", screen_rotation)
    cube("screen_left_bezel", (2.5, 19.6, 7.3), (4.3, 27.2, 8.45), "hull", screen_rotation)
    cube("screen_right_bezel", (18.9, 19.6, 7.3), (20.5, 27.2, 8.45), "hull", screen_rotation)
    cube("screen_top_pipe", (3.1, 27.0, 7.0), (20.0, 27.65, 7.45), "bronze", screen_rotation)
    cube("screen_bottom_pipe", (3.1, 19.35, 7.0), (20.0, 20.0, 7.45), "metal", screen_rotation)
    cube("screen_left_pipe", (2.8, 19.8, 7.0), (3.5, 27.2, 7.45), "metal", screen_rotation)
    cube("screen_right_pipe", (19.6, 19.8, 7.0), (20.3, 27.2, 7.45), "metal", screen_rotation)

    # Signature circular Destiny dial on the upper-left corner.
    dial_cx, dial_cy, dial_z = 2.9, 27.0, 6.8
    for i in range(12):
        angle = i * 30
        rad = math.radians(angle)
        cx = dial_cx + math.cos(rad) * 2.5
        cy = dial_cy + math.sin(rad) * 2.5
        # Vanilla permits 22.5/45-degree element rotation only; stepped teeth sell the circle.
        cube("dial_tooth", (cx - .55, cy - .55, dial_z), (cx + .55, cy + .55, dial_z + 1.0),
             "light" if i % 3 == 0 else "bronze", shade=False)
    cube("dial_outer_plate", (.5, 24.6, 7.25), (5.3, 29.4, 8.1), "metal")
    cube("dial_inner", (1.3, 25.4, 6.45), (4.5, 28.6, 8.35), "bronze")
    cube("dial_hub", (2.0, 26.1, 6.0), (3.8, 27.9, 8.7), "screen")
    cube("dial_spine", (.1, 18.8, 7.0), (2.6, 25.1, 8.8), "metal", ("z", -22.5, [2, 24, 8]))
    for row in range(5):
        cube("dial_spine_vent", (.55, 19.5 + row * 1.05, 6.65), (2.0, 19.9 + row * 1.05, 7.0), "hull_dark")

    # Status hump, lower handrail, side conduits and loose cables.
    cube("top_status_hump", (7.0, 28.2, 9.1), (18.5, 30.0, 12.0), "hull_dark", screen_rotation)
    for i in range(6):
        cube("status_light", (7.7 + i * 1.65, 28.65, 8.55), (8.55 + i * 1.65, 29.2, 8.85),
             "light" if i in (0, 1, 4) else "metal", screen_rotation, shade=False)
    cube("monitor_lower_rail", (1.2, 18.25, 6.6), (21.0, 19.05, 7.15), "cable", screen_rotation)
    cube("left_monitor_cable", (1.0, 19.2, 6.55), (1.6, 25.2, 7.1), "cable", screen_rotation)
    cube("right_monitor_cable", (20.2, 19.2, 6.55), (20.8, 27.0, 7.1), "cable", screen_rotation)
    for i in range(3):
        cube("right_socket", (21.4, 21.0 + i * 1.35, 9.3), (22.6, 21.65 + i * 1.35, 10.5), "bronze")
        cube("right_external_cable", (22.2, 21.2 + i * 1.35, 9.65), (29.6, 21.65 + i * 1.35, 10.1), "cable")
        cube("right_cable_drop", (29.0, 17.8, 10.0 + i * .85), (29.6, 21.65 + i * 1.35, 10.5 + i * .85), "cable")

    # Raised palm rails on both wings.
    cube("left_palm_rail", (-13.0, 18.7, -.4), (-4.4, 19.55, .35), "metal")
    cube("right_palm_rail", (20.4, 18.7, -.4), (29.0, 19.55, .35), "metal")


def weathered_texture(filename, base, fleck, seed, line=None):
    rng = random.Random(seed)
    img = Image.new("RGBA", (32, 32), (*base, 255))
    px = img.load()
    for y in range(32):
        for x in range(32):
            grain = rng.randint(-10, 10)
            edge = -8 if x in (0, 31) or y in (0, 31) else 0
            px[x, y] = tuple(max(0, min(255, c + grain + edge)) for c in base) + (255,)
    draw = ImageDraw.Draw(img)
    for _ in range(28):
        x, y = rng.randrange(32), rng.randrange(32)
        length = rng.randrange(1, 5)
        color = (*fleck, rng.randrange(35, 100))
        draw.line((x, y, min(31, x + length), y), fill=color)
    if line:
        draw.line((0, 7, 31, 7), fill=(*line, 130))
        draw.line((0, 24, 31, 24), fill=(*line, 90))
    img.save(TEX_OUT / filename)


def build_textures():
    TEX_OUT.mkdir(parents=True, exist_ok=True)
    weathered_texture("destiny_hull.png", (43, 78, 69), (105, 129, 104), 11, (89, 116, 96))
    weathered_texture("destiny_hull_dark.png", (20, 34, 32), (61, 76, 66), 12)
    weathered_texture("destiny_metal.png", (57, 57, 53), (125, 116, 96), 13, (27, 27, 25))
    weathered_texture("destiny_bronze.png", (91, 63, 38), (166, 121, 65), 14)
    weathered_texture("destiny_cable.png", (17, 18, 17), (70, 67, 58), 15, (118, 94, 60))
    weathered_texture("destiny_pedestal.png", (37, 29, 25), (88, 66, 46), 16)

    screen = Image.new("RGBA", (32, 32), (5, 10, 13, 255))
    draw = ImageDraw.Draw(screen)
    for y in range(2, 31, 4):
        draw.line((1, y, 30, y), fill=(10, 25, 27, 255))
    draw.rectangle((1, 1, 30, 30), outline=(25, 42, 40, 255))
    screen.save(TEX_OUT / "destiny_screen_off.png")

    light = Image.new("RGBA", (32, 32), (176, 178, 145, 255))
    draw = ImageDraw.Draw(light)
    draw.rectangle((2, 2, 29, 29), fill=(217, 222, 183, 255), outline=(91, 91, 70, 255), width=2)
    draw.rectangle((6, 6, 25, 25), fill=(239, 240, 203, 255))
    light.save(TEX_OUT / "destiny_light.png")


def write_model_json():
    MODEL_OUT.parent.mkdir(parents=True, exist_ok=True)
    model = {
        "credit": "Original Minecraft recreation by DankScripts",
        "ambientocclusion": False,
        "render_type": "cutout",
        "textures": {**TEXTURES, "particle": TEXTURES["hull"]},
        "display": {
            "thirdperson_righthand": {"rotation": [70, 45, 0], "translation": [0, 1.5, 0], "scale": [0.22, 0.22, 0.22]},
            "thirdperson_lefthand": {"rotation": [70, 225, 0], "translation": [0, 1.5, 0], "scale": [0.22, 0.22, 0.22]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 1, 0], "scale": [0.16, 0.16, 0.16]},
            "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 1, 0], "scale": [0.16, 0.16, 0.16]},
            "gui": {"rotation": [30, 225, 0], "translation": [0, -1, 0], "scale": [0.28, 0.28, 0.28]},
            "ground": {"translation": [0, 2, 0], "scale": [0.22, 0.22, 0.22]},
            "fixed": {"rotation": [0, 180, 0], "scale": [0.25, 0.25, 0.25]}
        },
        "elements": elements,
    }
    MODEL_OUT.write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")


def write_bbmodel():
    texture_names = list(TEXTURES)
    texture_index = {name: i for i, name in enumerate(texture_names)}
    bb_elements = []
    outliner = []
    for source in elements:
        uid = str(uuid.uuid4())
        tex_name = source["faces"]["north"]["texture"][1:]
        cube_data = {
            "name": source["name"],
            "box_uv": False,
            "rescale": False,
            "locked": False,
            "light_emission": 0,
            "render_order": "default",
            "allow_mirror_modeling": True,
            "from": source["from"],
            "to": source["to"],
            "autouv": 0,
            "color": texture_index[tex_name],
            "origin": source.get("rotation", {}).get("origin", [8, 8, 8]),
            "faces": {
                side: {"uv": [0, 0, 16, 16], "texture": texture_index[tex_name]}
                for side in ("north", "east", "south", "west", "up", "down")
            },
            "type": "cube",
            "uuid": uid,
        }
        if "rotation" in source:
            rot = source["rotation"]
            vector = [0, 0, 0]
            vector[{"x": 0, "y": 1, "z": 2}[rot["axis"]]] = rot["angle"]
            cube_data["rotation"] = vector
        bb_elements.append(cube_data)
        outliner.append(uid)

    bb_textures = []
    for i, name in enumerate(texture_names):
        path = TEX_OUT / (TEXTURES[name].split("/")[-1] + ".png")
        bb_textures.append({
            "path": str(path.relative_to(ROOT)).replace("\\", "/"),
            "name": path.name,
            "folder": "block",
            "namespace": "sgjdestiny_dhd",
            "id": str(i),
            "particle": name == "hull",
            "render_mode": "default",
            "render_sides": "auto",
            "frame_time": 1,
            "frame_order_type": "loop",
            "frame_interpolate": False,
            "visible": True,
            "internal": False,
            "saved": True,
            "uuid": str(uuid.uuid4()),
            "relative_path": f"block/{path.name}",
        })

    bbmodel = {
        "meta": {
            "format_version": "4.10",
            "model_format": "java_block",
            "box_uv": False,
        },
        "name": "Destiny Dialing Console alpha 1",
        "model_identifier": "sgjdestiny_dhd:destiny_dhd",
        "visible_box": [3, 2, 2],
        "variable_placeholders": "",
        "resolution": {"width": 32, "height": 32},
        "elements": bb_elements,
        "outliner": outliner,
        "textures": bb_textures,
    }
    BBMODEL_OUT.parent.mkdir(parents=True, exist_ok=True)
    BBMODEL_OUT.write_text(json.dumps(bbmodel, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    build_geometry()
    build_textures()
    write_model_json()
    write_bbmodel()
    print(f"Generated {len(elements)} model elements")
