#!/usr/bin/env python3
"""Build the approved Kino dispenser OBJ and its Minecraft weathered textures."""

from __future__ import annotations

import importlib.util
import math
import random
import shutil
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/sgjdestiny_dhd"
MODEL_DIR = ASSETS / "models/block"
TEXTURE_DIR = ASSETS / "textures/block/kino_dispenser"
GAME_OBJ = MODEL_DIR / "kino_dispenser.obj"
GAME_MTL = MODEL_DIR / "kino_dispenser.mtl"
PREVIEW_OBJ = ROOT / "blockbench/kino_dispenser_weathered_preview_v5.obj"
PREVIEW_MTL = ROOT / "blockbench/kino_dispenser_weathered_preview_v5.mtl"
PREVIEW_TEXTURE_DIR = ROOT / "blockbench"


MATERIALS = {
    # name: (base, variation, rust amount, scratch amount)
    "dark_iron": ((31, 27, 24), 8, 0.25, 0.32),
    "blackened_metal": ((21, 19, 17), 7, 0.20, 0.28),
    "rim_metal": ((112, 106, 91), 9, 0.30, 0.40),
    "aged_panel": ((91, 87, 74), 9, 0.34, 0.38),
    "rusted_metal": ((62, 39, 31), 10, 0.58, 0.34),
    "cavity": ((7, 7, 7), 2, 0.00, 0.02),
    "pipe": ((49, 38, 33), 8, 0.42, 0.34),
    "pipe_band": ((87, 79, 68), 9, 0.32, 0.42),
    "bolt": ((67, 64, 57), 8, 0.38, 0.46),
    "display": ((4, 15, 14), 3, 0.00, 0.08),
    "control_button": ((70, 67, 59), 7, 0.24, 0.34),
    "status_light": ((117, 108, 77), 5, 0.05, 0.12),
    "cable": ((15, 14, 13), 4, 0.02, 0.14),
    "kino_shell": ((79, 76, 68), 8, 0.20, 0.32),
    "kino_seam": ((48, 46, 41), 7, 0.22, 0.34),
    "kino_lens": ((8, 41, 45), 5, 0.02, 0.10),
    "kino_camera": ((31, 33, 31), 6, 0.06, 0.20),
    "kino_brass": ((73, 65, 52), 7, 0.16, 0.28),
}


def load_preview_module():
    path = ROOT / "tools/generate_kino_dispenser_preview.py"
    spec = importlib.util.spec_from_file_location("kino_preview", path)
    if spec is None or spec.loader is None:
        raise RuntimeError("Unable to load preview generator")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def clamp(value: float) -> int:
    return max(0, min(255, round(value)))


def weathered_texture(name: str, base: tuple[int, int, int], variation: int,
                      rust_amount: float, scratch_amount: float, size: int = 128) -> Image.Image:
    rng = random.Random(f"kino-dispenser:{name}")
    fine = Image.new("L", (size, size))
    fine.putdata([rng.randrange(256) for _ in range(size * size)])
    fine = fine.filter(ImageFilter.GaussianBlur(0.45))

    image = Image.new("RGB", (size, size))
    pixels = image.load()
    fine_px = fine.load()
    for y in range(size):
        # Fine brushed-metal grain; broad cloudy stains looked like rotten wood
        # once a texture was stretched across a Minecraft OBJ face.
        grain = 1.4 * math.sin(y * 0.75) + 0.8 * math.sin(y * 0.17)
        for x in range(size):
            noise = (fine_px[x, y] - 128) / 128 * variation + grain
            # Polished wear collects along exposed panel and pipe edges. This
            # baked response remains visible in Minecraft even without PBR.
            edge_distance = min(x, y, size - 1 - x, size - 1 - y)
            edge_wear = max(0.0, (7.0 - edge_distance) / 7.0)
            edge_lift = edge_wear * (16.0 if name not in {"cavity", "cable", "display"} else 1.5)
            pixels[x, y] = tuple(clamp(channel + noise + edge_lift) for channel in base)

    image = image.convert("RGBA")

    # Soft, irregular oxidation stains sit under the sharper rust. Keeping the
    # stains small and edge-biased avoids the muddy/wood-grain look of v1.
    if name not in {"cavity", "cable", "display", "status_light", "kino_lens"}:
        tarnish = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(tarnish)
        for _ in range(10 + round(26 * rust_amount)):
            side = rng.randrange(4)
            if side == 0:
                x, y = rng.randrange(size), rng.randrange(0, 22)
            elif side == 1:
                x, y = rng.randrange(size), rng.randrange(size - 22, size)
            elif side == 2:
                x, y = rng.randrange(0, 22), rng.randrange(size)
            else:
                x, y = rng.randrange(size - 22, size), rng.randrange(size)
            rx, ry = rng.randrange(5, 16), rng.randrange(3, 11)
            color = rng.choice([(26, 22, 19), (42, 32, 25), (39, 41, 31)])
            draw.ellipse((x - rx, y - ry, x + rx, y + ry),
                         fill=(*color, rng.randrange(48, 92)))
        tarnish = tarnish.filter(ImageFilter.GaussianBlur(3.2))
        image = Image.alpha_composite(image, tarnish)

    if rust_amount:
        rust = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(rust)
        for _ in range(3 + round(30 * rust_amount)):
            if rng.random() < 0.72:
                side = rng.randrange(4)
                x = rng.randrange(size) if side < 2 else rng.randrange(0, 8) if side == 2 else rng.randrange(size - 8, size)
                y = rng.randrange(0, 8) if side == 0 else rng.randrange(size - 8, size) if side == 1 else rng.randrange(size)
            else:
                x, y = rng.randrange(size), rng.randrange(size)
            rx, ry = rng.randrange(1, 6), rng.randrange(1, 4)
            color = rng.choice([(100, 47, 22), (126, 61, 25), (76, 42, 25)])
            draw.ellipse((x - rx, y - ry, x + rx, y + ry),
                         fill=(*color, rng.randrange(55, 115)))
        rust = rust.filter(ImageFilter.GaussianBlur(0.65))
        image = Image.alpha_composite(image, rust)

    # Fine pale scratches and dark pits sell worn metal under Minecraft light.
    marks = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(marks)
    for _ in range(round(34 * scratch_amount)):
        x, y = rng.randrange(size), rng.randrange(size)
        length = rng.randrange(3, 14)
        shade = rng.choice([(190, 181, 150, 45), (14, 11, 9, 62)])
        draw.line((x, y, x + length, y + rng.choice((-1, 0, 1))), fill=shade, width=1)
    for _ in range(round(28 * rust_amount)):
        x, y = rng.randrange(size), rng.randrange(size)
        draw.point((x, y), fill=(8, 6, 5, rng.randrange(80, 160)))
    image = Image.alpha_composite(image, marks)

    if name == "display":
        draw = ImageDraw.Draw(image)
        for y in range(3, size, 5):
            draw.line((0, y, size, y), fill=(10, 32, 28, 80), width=1)
    return image.convert("RGBA")


def write_textures() -> None:
    TEXTURE_DIR.mkdir(parents=True, exist_ok=True)
    for name, (base, variation, rust, scratches) in MATERIALS.items():
        weathered_texture(name, base, variation, rust, scratches).save(TEXTURE_DIR / f"{name}.png")


def write_preview_specular_maps() -> None:
    """Write standard MTL specular maps for a genuinely metallic preview."""
    PREVIEW_TEXTURE_DIR.mkdir(parents=True, exist_ok=True)
    nonmetal = {"cavity", "cable", "display", "status_light", "kino_lens"}
    rng = random.Random("kino-dispenser:specular")
    size = 128
    for name in MATERIALS:
        base = 22 if name in nonmetal else (112 if name in {"aged_panel", "rim_metal", "pipe_band", "bolt", "kino_seam", "kino_brass"} else 84)
        image = Image.new("RGB", (size, size))
        pixels = image.load()
        for y in range(size):
            for x in range(size):
                edge = min(x, y, size - 1 - x, size - 1 - y)
                worn = max(0.0, (8.0 - edge) / 8.0)
                value = clamp(base + worn * (48 if name not in nonmetal else 4) + rng.randrange(-10, 11))
                pixels[x, y] = (value, value, value)
        image.filter(ImageFilter.GaussianBlur(0.35)).save(
            PREVIEW_TEXTURE_DIR / f"kino_dispenser_v5_{name}_specular.png")


def normal(points: list[tuple[float, float, float]]) -> tuple[float, float, float]:
    a = points[0]
    for i in range(1, len(points) - 1):
        b, c = points[i], points[i + 1]
        ab = (b[0] - a[0], b[1] - a[1], b[2] - a[2])
        ac = (c[0] - a[0], c[1] - a[1], c[2] - a[2])
        cross = (ab[1] * ac[2] - ab[2] * ac[1],
                 ab[2] * ac[0] - ab[0] * ac[2],
                 ab[0] * ac[1] - ab[1] * ac[0])
        if sum(value * value for value in cross) > 1e-12:
            return cross
    return (0.0, 0.0, 1.0)


def project_uv(points: list[tuple[float, float, float]]) -> list[tuple[float, float]]:
    """Project one polygon into its own atlas-safe 0..1 texture area.

    Forge stitches block textures into a shared atlas. UVs outside the assigned
    sprite's 0..1 range therefore sample neighboring block textures instead of
    repeating this material. Normalizing each independently emitted polygon
    keeps every lookup inside the material's sprite. A one-pixel inset also
    prevents mip filtering from bleeding across the sprite boundary.
    """
    nx, ny, nz = (abs(value) for value in normal(points))
    if nx >= ny and nx >= nz:
        projected = [(z, y) for _, y, z in points]
    elif ny >= nz:
        projected = [(x, z) for x, _, z in points]
    else:
        projected = [(x, y) for x, y, _ in points]

    min_u = min(u for u, _ in projected)
    max_u = max(u for u, _ in projected)
    min_v = min(v for _, v in projected)
    max_v = max(v for _, v in projected)
    span_u = max(max_u - min_u, 1e-9)
    span_v = max(max_v - min_v, 1e-9)
    padding = 1.0 / 128.0
    usable = 1.0 - 2.0 * padding
    return [
        (padding + ((u - min_u) / span_u) * usable,
         padding + ((v - min_v) / span_v) * usable)
        for u, v in projected
    ]


def write_game_obj(source: Path) -> None:
    raw_vertices: list[tuple[float, float, float]] = []
    records: list[tuple[str, object]] = []
    material = "aged_panel"
    for line in source.read_text(encoding="utf-8").splitlines():
        if line.startswith("v "):
            x, y, z = map(float, line.split()[1:4])
            # Minecraft block OBJ origin is a lower corner; the approved model
            # was authored around its center line.
            raw_vertices.append((x + 0.5, y, z + 0.5))
        elif line.startswith("usemtl "):
            material = line.split(maxsplit=1)[1]
        elif line.startswith("f "):
            indices = [int(token.split("/")[0]) for token in line.split()[1:]]
            records.append((material, indices))

    texcoords: list[tuple[float, float]] = []
    output_faces: list[tuple[str, list[int], list[int]]] = []
    for face_material, indices in records:
        points = [raw_vertices[index - 1] for index in indices]
        uvs = project_uv(points)
        uv_indices = []
        for uv in uvs:
            texcoords.append(uv)
            uv_indices.append(len(texcoords))
        output_faces.append((face_material, indices, uv_indices))

    lines = ["# Approved six-block Kino dispenser", "mtllib kino_dispenser.mtl"]
    lines.extend(f"v {x:.6f} {y:.6f} {z:.6f}" for x, y, z in raw_vertices)
    lines.extend(f"vt {u:.6f} {v:.6f}" for u, v in texcoords)
    current = None
    for face_material, indices, uv_indices in output_faces:
        if face_material != current:
            lines.append(f"usemtl {face_material}")
            current = face_material
        lines.append("f " + " ".join(f"{v}/{vt}" for v, vt in zip(indices, uv_indices)))
        # Several decorative surfaces are intentionally thin. Emit the reverse
        # winding too so Forge's back-face culling cannot make them disappear
        # when the dispenser is viewed from behind.
        lines.append("f " + " ".join(
            f"{v}/{vt}" for v, vt in zip(reversed(indices), reversed(uv_indices))))
    GAME_OBJ.write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_mtl() -> None:
    lines = ["# UV-mapped weathered Kino dispenser materials"]
    for name in MATERIALS:
        lines.extend(["", f"newmtl {name}", "Kd 1 1 1", "Ka 0 0 0",
                      f"map_Kd sgjdestiny_dhd:block/kino_dispenser/{name}"])
    GAME_MTL.write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_local_preview() -> None:
    """Create a textured OBJ package that Windows 3D Viewer can load."""
    PREVIEW_TEXTURE_DIR.mkdir(parents=True, exist_ok=True)
    for texture in TEXTURE_DIR.glob("*.png"):
        shutil.copy2(texture, PREVIEW_TEXTURE_DIR / f"kino_dispenser_v5_{texture.name}")
    write_preview_specular_maps()

    obj_text = GAME_OBJ.read_text(encoding="utf-8").replace(
        "mtllib kino_dispenser.mtl", f"mtllib {PREVIEW_MTL.name}", 1)
    PREVIEW_OBJ.write_text(obj_text, encoding="utf-8")

    lines = ["# Local textured approval preview"]
    for name in MATERIALS:
        nonmetal = name in {"cavity", "cable", "display", "status_light", "kino_lens"}
        specular = "0.06 0.06 0.06" if nonmetal else "0.28 0.27 0.25"
        shininess = "12" if nonmetal else "36"
        lines.extend(["", f"newmtl {name}", "Kd 1 1 1", "Ka 0.03 0.03 0.03",
                      f"Ks {specular}", f"Ns {shininess}", "Ni 1.46", "illum 2",
                      f"map_Kd kino_dispenser_v5_{name}.png",
                      f"map_Ks kino_dispenser_v5_{name}_specular.png"])
    PREVIEW_MTL.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> None:
    preview = load_preview_module()
    preview.build()
    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    write_textures()
    write_game_obj(preview.OBJ)
    write_mtl()
    write_local_preview()
    print(GAME_OBJ)


if __name__ == "__main__":
    main()
