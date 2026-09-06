"""Prepare the accepted Destiny console Blender model for Forge's OBJ loader."""

import os
from pathlib import Path

import bpy
import bmesh


SOURCE = Path(os.environ.get(
    "SGJDESTINY_SOURCE_BLEND",
    Path.home() / "Documents" / "SGJDestinyDHD" / "destiny_console_v3.blend",
))
PROJECT = Path(__file__).resolve().parents[1]
MODEL_DIR = PROJECT / "src" / "main" / "resources" / "assets" / "sgjdestiny_dhd" / "models" / "block"
OBJ_PATH = MODEL_DIR / "destiny_console.obj"
MTL_PATH = MODEL_DIR / "destiny_console.mtl"

WIDTH_SCALE = 0.272
VERTICAL_SCALE = 0.288
DEPTH_SCALE = 0.4
CENTER_X = 0.5
CENTER_Z = 0.5

MATERIAL_TEXTURES = {
    "Aged_Destiny_green": "destiny_hull",
    "Ancient_bronze": "destiny_bronze",
    "Ancient_floor_socket": "destiny_pedestal",
    "Dark_inset_alloy": "destiny_hull_dark",
    "Destiny_dial_blue": "destiny_dial",
    "Destiny_dial_ivory": "destiny_dial_ivory",
    "Inactive_display": "destiny_screen_off",
    "Ivory_controls": "destiny_light",
    "Ribbed_perimeter_metal": "destiny_metal",
    "Worn_green_edge": "destiny_hull",
}


def prepare_meshes():
    meshes = []
    for source_obj in list(bpy.context.scene.objects):
        if source_obj.type not in {"MESH", "CURVE"} or source_obj.name == "Inspection floor":
            continue

        bpy.ops.object.select_all(action="DESELECT")
        source_obj.select_set(True)
        bpy.context.view_layer.objects.active = source_obj
        if source_obj.type == "CURVE":
            source_obj.data.use_fill_caps = True
            bpy.ops.object.convert(target="MESH")
        obj = bpy.context.object

        for modifier in list(obj.modifiers):
            bpy.ops.object.modifier_apply(modifier=modifier.name)

        bpy.context.view_layer.objects.active = obj
        obj.select_set(True)
        bpy.ops.object.mode_set(mode="EDIT")
        bpy.ops.mesh.select_all(action="SELECT")
        bpy.ops.mesh.remove_doubles(threshold=0.00001)
        bpy.ops.mesh.normals_make_consistent(inside=False)
        bpy.ops.mesh.quads_convert_to_tris(quad_method="BEAUTY", ngon_method="BEAUTY")
        bpy.ops.uv.smart_project(angle_limit=1.15192, island_margin=0.02)
        bpy.ops.object.mode_set(mode="OBJECT")

        topology = bmesh.new()
        topology.from_mesh(obj.data)
        non_manifold_edges = sum(1 for edge in topology.edges if not edge.is_manifold)
        topology.free()
        if non_manifold_edges:
            raise RuntimeError(f"{obj.name} has {non_manifold_edges} non-manifold edges")

        obj.select_set(False)
        meshes.append(obj)
    return meshes


def transform_obj_vertices():
    transformed = []
    for line in OBJ_PATH.read_text(encoding="utf-8").splitlines():
        if not line.startswith("v "):
            transformed.append(line)
            continue
        _, x_text, y_text, z_text = line.split()
        x = float(x_text) * WIDTH_SCALE + CENTER_X
        y = float(y_text) * VERTICAL_SCALE
        z = float(z_text) * DEPTH_SCALE + CENTER_Z
        transformed.append(f"v {x:.6f} {y:.6f} {z:.6f}")
    OBJ_PATH.write_text("\n".join(transformed) + "\n", encoding="ascii")


def write_minecraft_mtl():
    lines = ["# Minecraft texture mappings for the accepted Destiny console", ""]
    for material_name, texture_name in MATERIAL_TEXTURES.items():
        lines.extend([
            f"newmtl {material_name}",
            "Kd 1 1 1",
            "Ka 0 0 0",
            f"map_Kd sgjdestiny_dhd:block/{texture_name}",
            "",
        ])
    MTL_PATH.write_text("\n".join(lines), encoding="ascii")


if not SOURCE.is_file():
    raise FileNotFoundError(f"Accepted Blender model not found: {SOURCE}")

MODEL_DIR.mkdir(parents=True, exist_ok=True)
bpy.ops.wm.open_mainfile(filepath=str(SOURCE))
bpy.ops.object.select_all(action="DESELECT")
meshes = prepare_meshes()

for obj in meshes:
    obj.select_set(True)
bpy.context.view_layer.objects.active = meshes[0]

bpy.ops.wm.obj_export(
    filepath=str(OBJ_PATH),
    export_selected_objects=True,
    export_materials=True,
    forward_axis="NEGATIVE_Z",
    up_axis="Y",
)
transform_obj_vertices()
write_minecraft_mtl()
print(f"Prepared Forge OBJ: {OBJ_PATH}")