#!/usr/bin/env python3
"""Reference-driven alpha.3 geometry for the SGU Destiny dialing console."""

from __future__ import annotations

import json
import math
from pathlib import Path

from PIL import Image, ImageDraw

import generate_destiny_console as base

ROOT = Path(__file__).resolve().parents[1]
C = base.cube

base.BBMODEL_OUT = ROOT / "blockbench/destiny_dialing_console_alpha3.bbmodel"
base.TEXTURES["dial"] = "sgjdestiny_dhd:block/destiny_dial"


def build_geometry() -> None:
    base.elements.clear()

    # Broad prop plinth with a shallow front notch and stepped shoulders.
    C("base_center", (-4.5, 0, 4.0), (20.5, 1.25, 17.0), "pedestal")
    C("base_front_center", (-1.5, 0, 1.2), (17.5, 1.05, 6.0), "metal")
    C("base_left_wing", (-7.0, 0, 5.5), (-3.6, 1.1, 15.8), "pedestal", ("y", -22.5, [-3.8, 1, 8]))
    C("base_right_wing", (19.6, 0, 5.5), (23.0, 1.1, 15.8), "pedestal", ("y", 22.5, [19.8, 1, 8]))
    for x1, x2 in ((-1.0, 2.6), (13.4, 17.0)):
        C("foot_front", (x1, .75, .35), (x2, 1.55, 4.5), "metal")

    # Two thin swept pylons. The paired pieces overlap to read as one curved leg.
    pylon_specs = (
        ("left", -1.8, 22.5),
        ("right", 14.8, -22.5),
    )
    for side, x, angle in pylon_specs:
        C(f"{side}_pylon_lower", (x, 1.0, 7.0), (x + 3.4, 8.0, 11.4), "pedestal",
          ("z", angle, [x + 1.7, 6.5, 9]))
        offset = -1.0 if side == "left" else 1.0
        C(f"{side}_pylon_upper", (x + offset, 6.8, 6.5), (x + 3.4 + offset, 14.8, 11.6), "pedestal",
          ("z", -angle, [x + 1.7, 8.0, 9]))
        C(f"{side}_pylon_front_cap", (x + .25, 2.0, 6.72), (x + 3.15, 13.2, 7.02), "metal")
        for groove in range(3):
            gx = x + .5 + groove * .9
            C(f"{side}_pylon_flute_{groove}", (gx, 2.25, 6.48), (gx + .24, 12.8, 6.72), "bronze")

    # Thin central shell and rear bridge beneath the display.
    C("center_underbody", (-3.2, 13.25, -3.5), (19.2, 15.05, 11.0), "hull_dark")
    C("center_deck", (-3.7, 15.0, -4.2), (19.7, 17.15, 10.8), "hull")
    C("rear_bridge_underbody", (-1.8, 13.6, 8.5), (17.8, 15.1, 15.0), "hull_dark")
    C("rear_bridge_deck", (-2.2, 15.0, 8.0), (18.2, 17.15, 15.4), "hull")

    # Swept wings create the shallow boomerang outline of the screen-used prop.
    wing_specs = (
        ("left_inner", -10.0, -2.0, -1.2, 14.8, -22.5, -2.0),
        ("left_outer", -15.2, -8.2, 1.0, 14.0, -22.5, -8.2),
        ("right_inner", 18.0, 26.0, -1.2, 14.8, 22.5, 18.0),
        ("right_outer", 24.2, 31.2, 1.0, 14.0, 22.5, 24.2),
    )
    for name, x1, x2, z1, z2, angle, ox in wing_specs:
        origin = [ox, 16, 8]
        C(f"{name}_underbody", (x1, 13.55, z1), (x2, 15.05, z2), "hull_dark", ("y", angle, origin))
        C(f"{name}_deck", (x1, 15.0, z1 - .4), (x2, 17.15, z2 + .45), "hull", ("y", angle, origin))

    # Small ribbed blocks trace the front crescent instead of one rectangular fascia.
    for index, x in enumerate([i * 2.15 - 14.5 for i in range(22)]):
        distance = abs(x - 8.0)
        z = -4.9 + min(4.2, distance * .17)
        angle = -22.5 if x < -3 else (22.5 if x > 19 else 0)
        C(f"bumper_rib_{index}", (x, 14.65, z), (x + 2.0, 17.65, z + 1.35), "metal",
          ("y", angle, [8, 16, -2]))
        if index in (1, 5, 9, 12, 16, 20):
            C(f"bumper_clasp_{index}", (x + .58, 15.0, z - .18), (x + 1.32, 17.35, z + 1.52), "bronze",
              ("y", angle, [8, 16, -2]))
    C("left_end_cap", (-16.0, 14.55, 1.0), (-13.8, 17.65, 13.7), "metal", ("y", -22.5, [-14, 16, 7]))
    C("right_end_cap", (29.8, 14.55, 1.0), (32.0, 17.65, 13.7), "metal", ("y", 22.5, [30, 16, 7]))

    # Control wings. Their different button patterns preserve the prop's asymmetry.
    panel_specs = (
        ("left", -13.7, -3.8, -22.5, -3.8),
        ("right", 19.8, 29.7, 22.5, 19.8),
    )
    for side, x1, x2, angle, ox in panel_specs:
        rotation = ("y", angle, [ox, 17, 8])
        C(f"{side}_panel_recess", (x1, 17.08, .7), (x2, 17.42, 13.9), "hull_dark", rotation)
        C(f"{side}_panel_field", (x1 + .55, 17.4, 1.25), (x2 - .55, 17.62, 13.4), "metal", rotation)
        for row in range(4):
            count = 4 if (side == "left" or row != 1) else 3
            for col in range(count):
                px = x1 + 1.0 + col * 2.0
                pz = 2.0 + row * 2.05 + (.28 if (row + col) % 2 else 0)
                texture = "light" if (row * 2 + col) % 3 else "hull"
                C(f"{side}_button_{row}_{col}", (px, 17.62, pz), (px + .82, 17.98, pz + .58), texture,
                  rotation, shade=False)
        lamp_positions = ((1.0, 11.0), (4.2, 11.8), (7.1, 10.9)) if side == "left" else ((.9, 10.8), (4.5, 11.7), (7.3, 10.7))
        for lamp, (dx, dz) in enumerate(lamp_positions):
            C(f"{side}_round_lamp_{lamp}", (x1 + dx, 17.66, dz), (x1 + dx + 1.65, 18.06, dz + 1.45),
              "light", rotation, shade=False)

    # Low keyboard bank tucked beneath and to the left of the main display.
    C("keyboard_recess", (-2.8, 17.08, -.55), (5.6, 17.45, 5.15), "hull_dark")
    C("keyboard_plate", (-2.25, 17.43, .0), (5.05, 17.63, 4.65), "metal")
    for row in range(3):
        for col in range(5):
            px = -1.85 + col * 1.36
            pz = .45 + row * 1.34
            C(f"keyboard_key_{row}_{col}", (px, 17.63, pz), (px + .78, 17.98, pz + .7),
              "hull_dark" if (row + col) % 4 else "light", shade=False)

    # Compact monitor, reclining away from the operator. Its black glass covers only
    # the center opening; wide green bezels remain visible from every front angle.
    screen_rotation = ("x", 22.5, [8.0, 16.5, 5.0])
    C("monitor_back", (-2.0, 16.1, 3.9), (18.0, 26.5, 6.0), "hull_dark", screen_rotation)
    C("monitor_shell", (-1.35, 16.65, 3.15), (17.35, 25.9, 5.05), "hull", screen_rotation)
    C("screen_glass", (1.2, 18.65, 2.65), (15.75, 24.55, 2.94), "screen", screen_rotation, shade=False)
    C("screen_top_bezel", (.35, 24.35, 2.48), (16.55, 25.7, 3.3), "hull", screen_rotation)
    C("screen_bottom_bezel", (.35, 17.05, 2.48), (16.55, 18.85, 3.3), "hull", screen_rotation)
    C("screen_left_bezel", (-.45, 17.9, 2.48), (1.38, 25.05, 3.3), "hull", screen_rotation)
    C("screen_right_bezel", (15.55, 17.9, 2.48), (17.2, 25.05, 3.3), "hull", screen_rotation)
    for inset, texture in ((0.0, "metal"), (.42, "bronze")):
        C("screen_pipe_top", (.75 + inset, 24.15 - inset, 2.12), (16.15 - inset, 24.58 - inset, 2.48), texture, screen_rotation)
        C("screen_pipe_bottom", (.75 + inset, 18.45 + inset, 2.12), (16.15 - inset, 18.88 + inset, 2.48), texture, screen_rotation)
        C("screen_pipe_left", (.75 + inset, 18.6 + inset, 2.12), (1.16 + inset, 24.35 - inset, 2.48), texture, screen_rotation)
        C("screen_pipe_right", (15.74 - inset, 18.6 + inset, 2.12), (16.15 - inset, 24.35 - inset, 2.48), texture, screen_rotation)

    # Signature selector wheel and slotted metal spine overlapping the upper-left.
    C("dial_backing", (-3.0, 21.95, 2.0), (2.9, 27.85, 2.72), "metal", screen_rotation)
    C("dial_face", (-2.62, 22.33, 1.72), (2.52, 27.47, 2.02), "dial", screen_rotation, shade=False)
    C("dial_spine", (-3.0, 16.8, 2.2), (-.2, 22.95, 3.2), "metal", ("z", -22.5, [-1.2, 22, 2.5]))
    for row in range(5):
        C(f"dial_spine_slot_{row}", (-2.5, 17.45 + row * .92, 1.92), (-.75, 17.8 + row * .92, 2.2), "hull_dark")

    # Status crown, lower handrail, and three external right-side leads.
    C("status_crown", (5.1, 25.55, 3.8), (14.9, 27.15, 5.7), "metal", screen_rotation)
    for i in range(7):
        C(f"status_lamp_{i}", (5.65 + i * 1.18, 25.95, 3.28), (6.35 + i * 1.18, 26.42, 3.58),
          "light" if i in (0, 2, 3, 6) else "hull_dark", screen_rotation, shade=False)
    C("monitor_lower_rail", (-.75, 17.15, 2.0), (17.0, 17.72, 2.48), "cable", screen_rotation)
    C("monitor_left_rail", (-.72, 17.5, 2.0), (-.3, 24.0, 2.48), "cable", screen_rotation)
    C("monitor_right_rail", (16.65, 17.5, 2.0), (17.07, 24.0, 2.48), "cable", screen_rotation)
    for i in range(3):
        y = 18.8 + i * 1.18
        C(f"right_socket_{i}", (17.2, y, 3.6), (18.15, y + .58, 4.5), "bronze", screen_rotation)
        C(f"right_lead_{i}", (17.8, y + .12, 3.0 + i * .48), (25.6, y + .5, 3.42 + i * .48), "cable", screen_rotation)
        C(f"right_lead_drop_{i}", (25.2, 17.3, 3.2 + i * .48), (25.68, y + .46, 3.65 + i * .48), "cable")

    C("left_palm_rail", (-12.7, 17.6, -.75), (-4.25, 18.28, -.15), "metal", ("y", -22.5, [-4, 18, 0]))
    C("right_palm_rail", (20.25, 17.6, -.75), (28.7, 18.28, -.15), "metal", ("y", 22.5, [20, 18, 0]))


def build_dial_texture() -> None:
    image = Image.new("RGBA", (32, 32), (13, 26, 33, 255))
    draw = ImageDraw.Draw(image)
    for y in range(32):
        draw.line((0, y, 31, y), fill=(13 + y % 3, 26 + y % 4, 33 + y % 3, 255))
    image.save(base.TEX_OUT / "destiny_dial.png")


def clean_material_textures() -> None:
    # The real prop has broad patinated panels. Remove alpha.2's repeated stripes.
    base.weathered_texture("destiny_hull.png", (43, 78, 69), (105, 129, 104), 11)
    base.weathered_texture("destiny_metal.png", (57, 57, 53), (125, 116, 96), 13)
    base.weathered_texture("destiny_cable.png", (17, 18, 17), (70, 67, 58), 15)


def fix_bbmodel_metadata() -> None:
    data = json.loads(base.BBMODEL_OUT.read_text(encoding="utf-8"))
    data["name"] = "Destiny Dialing Console alpha 3"
    data["visible_box"] = [3, 2, 2]
    base.BBMODEL_OUT.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    build_geometry()
    base.build_textures()
    clean_material_textures()
    build_dial_texture()
    base.write_model_json()
    base.write_bbmodel()
    fix_bbmodel_metadata()
    print(f"Generated alpha.3 with {len(base.elements)} model elements")
