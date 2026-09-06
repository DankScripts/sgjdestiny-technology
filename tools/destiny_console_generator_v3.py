"""Generate an SGU Destiny console silhouette study from the selected prop reference."""

import math
import os
from pathlib import Path

import bpy
from mathutils import Vector


OUT = Path(os.environ.get(
    "SGJDESTINY_OUTPUT_DIR",
    Path.home() / "Documents" / "SGJDestinyDHD",
))
OUT.mkdir(parents=True, exist_ok=True)


def clear_scene():
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)
    for datablocks in (bpy.data.meshes, bpy.data.curves, bpy.data.materials,
                       bpy.data.cameras, bpy.data.lights):
        for block in list(datablocks):
            if block.users == 0:
                datablocks.remove(block)


def material(name, color, metallic=0.0, roughness=0.5):
    value = bpy.data.materials.new(name)
    value.diffuse_color = (*color, 1.0)
    value.use_nodes = True
    bsdf = value.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs["Base Color"].default_value = (*color, 1.0)
    bsdf.inputs["Metallic"].default_value = metallic
    bsdf.inputs["Roughness"].default_value = roughness
    return value


def finish(obj, value, bevel_width=0.04):
    obj.data.materials.append(value)
    if bevel_width:
        modifier = obj.modifiers.new("Soft manufactured edges", "BEVEL")
        modifier.width = bevel_width
        modifier.segments = 3
        modifier.limit_method = "ANGLE"
    return obj


def cube(name, location, scale, value, rotation=(0, 0, 0), bevel_width=0.04):
    bpy.ops.mesh.primitive_cube_add(location=location, rotation=rotation)
    obj = bpy.context.object
    obj.name = name
    obj.scale = scale
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    return finish(obj, value, bevel_width)


def prism_xy(name, points, z0, z1, value, bevel_width=0.04):
    count = len(points)
    vertices = [(x, y, z0) for x, y in points] + [(x, y, z1) for x, y in points]
    faces = [tuple(range(count - 1, -1, -1)), tuple(range(count, 2 * count))]
    for index in range(count):
        following = (index + 1) % count
        faces.append((index, following, count + following, count + index))
    mesh = bpy.data.meshes.new(name + "Mesh")
    mesh.from_pydata(vertices, [], faces)
    mesh.update()
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.collection.objects.link(obj)
    return finish(obj, value, bevel_width)


def prism_yz(name, points, x0, x1, value, bevel_width=0.04):
    count = len(points)
    vertices = [(x0, y, z) for y, z in points] + [(x1, y, z) for y, z in points]
    faces = [tuple(range(count - 1, -1, -1)), tuple(range(count, 2 * count))]
    for index in range(count):
        following = (index + 1) % count
        faces.append((index, following, count + following, count + index))
    mesh = bpy.data.meshes.new(name + "Mesh")
    mesh.from_pydata(vertices, [], faces)
    mesh.update()
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.collection.objects.link(obj)
    return finish(obj, value, bevel_width)


def prism_xz(name, points, y0, y1, value, bevel_width=0.04):
    count = len(points)
    vertices = [(x, y0, z) for x, z in points] + [(x, y1, z) for x, z in points]
    faces = [tuple(range(count - 1, -1, -1)), tuple(range(count, 2 * count))]
    for index in range(count):
        following = (index + 1) % count
        faces.append((index, following, count + following, count + index))
    mesh = bpy.data.meshes.new(name + "Mesh")
    mesh.from_pydata(vertices, [], faces)
    mesh.update()
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.collection.objects.link(obj)
    return finish(obj, value, bevel_width)


def radial_prism_xz(name, center, points, y0, y1, value, bevel_width=0.04):
    count = len(points)
    vertices = [(center[0], y0, center[1]), (center[0], y1, center[1])]
    vertices += [(x, y0, z) for x, z in points]
    vertices += [(x, y1, z) for x, z in points]
    faces = []
    for index in range(count):
        following = (index + 1) % count
        back = 2 + index
        back_following = 2 + following
        front = 2 + count + index
        front_following = 2 + count + following
        faces.append((0, back_following, back))
        faces.append((1, front, front_following))
        faces.append((back, back_following, front_following, front))
    mesh = bpy.data.meshes.new(name + "Mesh")
    mesh.from_pydata(vertices, [], faces)
    mesh.update()
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.collection.objects.link(obj)
    return finish(obj, value, bevel_width)


def cylinder(name, location, radius, depth, value, rotation=(0, 0, 0), vertices=32):
    bpy.ops.mesh.primitive_cylinder_add(vertices=vertices, radius=radius, depth=depth,
                                       location=location, rotation=rotation)
    obj = bpy.context.object
    obj.name = name
    return finish(obj, value, 0.025)


def tube(name, points, radius, value, cyclic=False):
    curve = bpy.data.curves.new(name + "Curve", "CURVE")
    curve.dimensions = "3D"
    curve.resolution_u = 10
    curve.bevel_depth = radius
    curve.bevel_resolution = 3
    spline = curve.splines.new("BEZIER")
    spline.bezier_points.add(len(points) - 1)
    for point, coordinate in zip(spline.bezier_points, points):
        point.co = coordinate
        point.handle_left_type = "AUTO"
        point.handle_right_type = "AUTO"
    spline.use_cyclic_u = cyclic
    obj = bpy.data.objects.new(name, curve)
    bpy.context.collection.objects.link(obj)
    obj.data.materials.append(value)
    return obj


def poly_tube(name, points, radius, value):
    curve = bpy.data.curves.new(name + "Curve", "CURVE")
    curve.dimensions = "3D"
    curve.resolution_u = 2
    curve.bevel_depth = radius
    curve.bevel_resolution = 3
    spline = curve.splines.new("POLY")
    spline.points.add(len(points) - 1)
    for point, coordinate in zip(spline.points, points):
        point.co = (*coordinate, 1.0)
    obj = bpy.data.objects.new(name, curve)
    bpy.context.collection.objects.link(obj)
    obj.data.materials.append(value)
    return obj


def parent_to(objects, parent):
    for obj in objects:
        obj.parent = parent


clear_scene()

GREEN = material("Aged Destiny green", (0.11, 0.30, 0.24), 0.35, 0.40)
GREEN_EDGE = material("Worn green edge", (0.22, 0.43, 0.34), 0.40, 0.35)
DARK = material("Dark inset alloy", (0.035, 0.045, 0.041), 0.70, 0.30)
METAL = material("Ribbed perimeter metal", (0.20, 0.19, 0.16), 0.82, 0.28)
BRONZE = material("Ancient bronze", (0.36, 0.20, 0.075), 0.72, 0.31)
IVORY = material("Ivory controls", (0.72, 0.73, 0.61), 0.18, 0.42)
SCREEN = material("Inactive display", (0.025, 0.035, 0.038), 0.12, 0.16)
BASE = material("Ancient floor socket", (0.20, 0.15, 0.12), 0.48, 0.46)
DIAL = material("Destiny dial blue", (0.055, 0.105, 0.13), 0.50, 0.32)

# Broad floor slab with the prop's distinctive comb-like front edge.
base_outline = [(-2.75, -1.18), (2.75, -1.18), (2.92, 0.72),
                (2.48, 1.10), (-2.48, 1.10), (-2.92, 0.72)]
prism_xy("Heavy floor socket", base_outline, 0.00, 0.42, BASE, 0.08)
for x in (-2.18, -1.78, -1.38, -0.98, -0.58, 0.58, 0.98, 1.38, 1.78, 2.18):
    cube("Floor tooth", (x, -1.34, 0.25), (0.11, 0.28, 0.25), BASE, bevel_width=0.025)
cube("Floor center tooth", (0, -1.40, 0.27), (0.28, 0.34, 0.27), BASE,
     bevel_width=0.025)
for x in (-1.28, 1.28):
    cube("Floor inset", (x, 0.03, 0.435), (0.80, 0.66, 0.015), METAL,
         bevel_width=0.025)

# The table uses two broad pylons swept outward toward the deck. Their front
# profiles are structural shapes, not straight furniture posts.
left_support = [(-2.36, 3.96), (-1.92, 3.96), (-1.58, 0.42),
                (-2.02, 0.42), (-2.18, 1.40)]
right_support = [(1.92, 3.96), (2.36, 3.96), (2.18, 1.40),
                 (2.02, 0.42), (1.58, 0.42)]
prism_xz("Left swept table pylon", left_support, -0.24, 0.22, DARK, 0.055)
prism_xz("Right swept table pylon", right_support, -0.24, 0.22, DARK, 0.055)
left_face = [(-2.24, 3.82), (-2.04, 3.82), (-1.72, 0.58), (-1.91, 0.58)]
right_face = [(2.04, 3.82), (2.24, 3.82), (1.91, 0.58), (1.72, 0.58)]
prism_xz("Left pylon bronze face", left_face, -0.255, -0.245, BRONZE, 0.018)
prism_xz("Right pylon bronze face", right_face, -0.255, -0.245, BRONZE, 0.018)

# Asymmetrical hooked deck: squared left keypad wing, shallow center, rounded right wing.
deck_outline = [
    (-4.50, -1.58), (-3.54, -1.74), (-2.85, -1.53), (-2.36, -0.92),
    (-1.66, -0.56), (-0.62, -0.45), (0.70, -0.48), (1.72, -0.63),
    (2.72, -0.89), (3.62, -0.91), (4.22, -0.50), (4.46, 0.10),
    (4.30, 0.78), (3.80, 1.24), (2.92, 1.42), (1.92, 1.30),
    (0.84, 1.08), (-0.14, 1.12), (-1.12, 1.30), (-1.88, 1.48),
    (-2.46, 1.18), (-2.92, 0.62), (-3.64, 0.10), (-4.36, -0.56),
]
prism_xy("Asymmetric hooked deck", deck_outline, 3.88, 4.25, GREEN, 0.15)

front_rail = [(-4.34, -1.36, 4.26), (-3.55, -1.58, 4.27),
              (-2.82, -1.39, 4.27), (-2.23, -0.80, 4.28),
              (-1.46, -0.49, 4.28), (-0.40, -0.40, 4.28),
              (0.75, -0.43, 4.28), (1.78, -0.58, 4.28),
              (2.70, -0.80, 4.28), (3.54, -0.82, 4.28),
              (4.12, -0.42, 4.28)]
tube("Segmented front rail", front_rail, 0.16, METAL)
back_rail = [(-4.26, -0.62, 4.27), (-3.68, 0.00, 4.28), (-2.92, 0.62, 4.29),
             (-2.42, 1.14, 4.29), (-1.76, 1.39, 4.29), (-0.72, 1.18, 4.29),
             (0.38, 1.17, 4.29), (1.52, 1.31, 4.29), (2.72, 1.36, 4.29),
             (3.66, 1.13, 4.29), (4.25, 0.67, 4.28)]
tube("Rear perimeter rail", back_rail, 0.12, DARK)

left_panel = [(-4.13, -1.30), (-3.50, -1.44), (-2.96, -1.25),
              (-2.53, -0.76), (-2.60, 0.30), (-3.19, 0.18),
              (-3.80, -0.31), (-4.23, -0.74)]
right_panel = [(1.72, -0.38), (2.70, -0.65), (3.48, -0.65),
               (4.00, -0.29), (4.08, 0.39), (3.60, 0.89),
               (2.78, 1.04), (1.92, 0.88)]
prism_xy("Left hooked control field", left_panel, 4.24, 4.34, GREEN_EDGE, 0.07)
prism_xy("Right rounded control field", right_panel, 4.24, 4.34, GREEN_EDGE, 0.07)

# Sparse markers show scale without committing to invented control layouts.
left_keypad_rows = (
    ((-3.80, -1.08), (-3.51, -1.08), (-3.22, -1.08), (-2.93, -1.08)),
    ((-3.76, -0.80), (-3.47, -0.80), (-3.18, -0.80), (-2.89, -0.80)),
    ((-3.72, -0.52), (-3.43, -0.52), (-3.14, -0.52), (-2.85, -0.52)),
)
for row, positions in enumerate(left_keypad_rows):
    for column, (x, y) in enumerate(positions):
        cube(f"Left keypad marker {row}-{column}", (x, y, 4.42),
             (0.095, 0.07, 0.045), IVORY, rotation=(0, 0, math.radians(-8)),
             bevel_width=0.018)
for index, (x, y) in enumerate(((2.45, -0.30), (2.79, -0.35), (3.13, -0.35),
                                (2.62, 0.02), (2.96, 0.00), (3.30, -0.02))):
    cube(f"Right control marker {index}", (x, y, 4.42), (0.10, 0.07, 0.045),
         IVORY, rotation=(0, 0, math.radians(4)), bevel_width=0.018)

for index, (x, y) in enumerate(((-3.41, -0.25), (-3.02, -0.16))):
    cylinder(f"Left round control base {index}", (x, y, 4.405), 0.125, 0.045, METAL,
             vertices=32)
    cylinder(f"Left round control cap {index}", (x, y, 4.445), 0.095, 0.055, IVORY,
             vertices=32)
for index, (x, y, radius) in enumerate(((3.53, 0.35, 0.13),
                                        (3.22, 0.55, 0.11)), start=3):
    cylinder(f"Round deck control {index}", (x, y, 4.42), radius, 0.09, IVORY,
             vertices=24)
cube("Left dark selector", (-3.78, -0.22, 4.43), (0.09, 0.16, 0.07), DARK,
     rotation=(0, 0, math.radians(-8)), bevel_width=0.035)
left_keypad_bed = [(-3.35, -0.01), (-2.68, 0.01),
                   (-2.35, 0.60), (-2.91, 0.67)]
prism_xy("Left recessed keypad bed", left_keypad_bed, 4.35, 4.40, DARK, 0.05)
left_dark_rows = (
    ((-3.20, 0.09), (-2.98, 0.09), (-2.76, 0.09)),
    ((-3.12, 0.23), (-2.90, 0.23), (-2.68, 0.23)),
    ((-3.04, 0.37), (-2.82, 0.37), (-2.60, 0.37)),
    ((-2.93, 0.51), (-2.71, 0.51), (-2.49, 0.51)),
)
for row, positions in enumerate(left_dark_rows):
    for column, (x, y) in enumerate(positions):
        cube(f"Left dark keypad marker {row}-{column}", (x, y, 4.44),
             (0.06, 0.045, 0.03), METAL, rotation=(0, 0, math.radians(-8)),
             bevel_width=0.012)

# Low integrated monitor; the dial overlaps its upper-left frame.
monitor = bpy.data.objects.new("Integrated monitor assembly", None)
bpy.context.collection.objects.link(monitor)
monitor.location = (-0.18, 0.24, 4.30)
monitor.rotation_euler = (math.radians(-9), 0, math.radians(-1.5))
parts = [
    cube("Monitor rear shell", (0, 0, 0.84), (1.70, 0.19, 1.05), DARK,
         bevel_width=0.16),
    cube("Monitor green frame", (0, -0.22, 0.80), (1.58, 0.09, 0.93), GREEN,
         bevel_width=0.18),
    cube("Wide inset screen", (0.18, -0.325, 0.72), (1.22, 0.025, 0.66), SCREEN,
         bevel_width=0.12),
    cube("Monitor lower bolster", (0.08, -0.22, -0.22), (1.52, 0.16, 0.18), GREEN,
         bevel_width=0.10),
        cube("Monitor lower dark cushion", (0.08, -0.31, -0.42), (1.42, 0.12, 0.13), DARK,
            bevel_width=0.08),
    cube("Monitor top cap", (0.36, -0.02, 1.95), (1.20, 0.17, 0.18), METAL,
         bevel_width=0.07),
    cube("Dial instrument strip", (-1.43, -0.28, 0.65), (0.17, 0.06, 0.64), METAL,
         bevel_width=0.05),
        cube("Screen trim top", (0.16, -0.36, 1.42), (1.28, 0.035, 0.035), BRONZE,
            bevel_width=0.025),
        cube("Screen trim bottom", (0.16, -0.36, 0.02), (1.28, 0.035, 0.035), BRONZE,
            bevel_width=0.025),
        cube("Screen trim left", (-1.10, -0.36, 0.72), (0.035, 0.035, 0.68), BRONZE,
            bevel_width=0.025),
        cube("Screen trim right", (1.42, -0.36, 0.72), (0.035, 0.035, 0.68), BRONZE,
            bevel_width=0.025),
        cube("Outer frame top", (0.16, -0.32, 1.56), (1.42, 0.04, 0.045), METAL,
            bevel_width=0.03),
        cube("Outer frame bottom", (0.16, -0.32, -0.10), (1.42, 0.04, 0.045), METAL,
            bevel_width=0.03),
        cube("Outer frame left", (-1.24, -0.32, 0.73), (0.045, 0.04, 0.79), METAL,
            bevel_width=0.03),
        cube("Outer frame right", (1.56, -0.32, 0.73), (0.045, 0.04, 0.79), METAL,
            bevel_width=0.03),
]
for slot in range(5):
    parts.append(cube(f"Instrument slot {slot}", (-1.43, -0.36, 0.28 + slot * 0.20),
                      (0.09, 0.018, 0.035), DARK, bevel_width=0.012))
dial_star_points = []
for index in range(24):
    angle = math.tau * index / 24
    radius = 0.165 if index % 2 == 0 else 0.115
    dial_star_points.append((-1.35 + math.cos(angle) * radius,
                             1.50 + math.sin(angle) * radius))

parts.extend([
    cylinder("Dial outer", (-1.35, -0.34, 1.50), 0.35, 0.065, METAL,
             rotation=(math.radians(90), 0, 0), vertices=64),
    cylinder("Dial bronze bezel", (-1.35, -0.385, 1.50), 0.305, 0.045, BRONZE,
             rotation=(math.radians(90), 0, 0), vertices=64),
    cylinder("Dial navy ring", (-1.35, -0.42, 1.50), 0.255, 0.035, DIAL,
             rotation=(math.radians(90), 0, 0), vertices=64),
    cylinder("Dial ivory annulus", (-1.35, -0.45, 1.50), 0.205, 0.035, IVORY,
             rotation=(math.radians(90), 0, 0), vertices=64),
    radial_prism_xz("Dial bronze star", (-1.35, 1.50), dial_star_points,
                    -0.49, -0.47, BRONZE, 0.008),
    cylinder("Dial blue center", (-1.35, -0.505, 1.50), 0.095, 0.025, DIAL,
             rotation=(math.radians(90), 0, 0), vertices=48),
    cylinder("Dial bronze hub", (-1.35, -0.525, 1.50), 0.026, 0.018, BRONZE,
             rotation=(math.radians(90), 0, 0), vertices=32),
])
parent_to(parts, monitor)

# Prop-visible cable runs anchor the monitor into both control wings.
for index in range(3):
    offset = index * 0.13
    poly_tube(f"Right monitor cable {index}",
              [(1.42, 0.56 + offset, 5.64 - index * 0.10),
               (1.72, 0.78 + offset, 5.45 - index * 0.10),
               (1.92, 0.86 + offset, 4.84 - index * 0.06),
               (2.45 + index * 0.18, 0.68, 4.43)],
              0.026, DARK)
poly_tube("Left dial cable", [(-1.56, 0.32, 5.73), (-1.78, 0.49, 5.37),
                              (-1.98, 0.58, 4.78), (-2.50, 0.50, 4.43)],
          0.032, BRONZE)
cylinder("Left keypad cable socket", (-2.50, 0.50, 4.42), 0.075, 0.08, BRONZE,
         vertices=24)

studio_floor = material("Studio floor", (0.018, 0.020, 0.021), 0.0, 0.88)
cube("Inspection floor", (0, 0, -0.10), (6.4, 5.2, 0.08), studio_floor,
     bevel_width=0)
bpy.ops.object.light_add(type="AREA", location=(-4.8, -5.2, 8.8))
bpy.context.object.data.energy = 1250
bpy.context.object.data.shape = "DISK"
bpy.context.object.data.size = 5.0
bpy.ops.object.light_add(type="AREA", location=(4.8, 2.4, 6.6))
bpy.context.object.data.energy = 850
bpy.context.object.data.color = (0.38, 0.62, 0.52)
bpy.context.object.data.size = 4.0

bpy.ops.object.camera_add(location=(-8.6, -11.6, 7.6))
camera = bpy.context.object
camera.name = "Reference comparison camera"
bpy.context.scene.camera = camera
target = Vector((0, -0.05, 3.25))
camera.rotation_euler = (target - camera.location).to_track_quat("-Z", "Y").to_euler()
camera.data.lens = 58

scene = bpy.context.scene
scene.render.engine = "BLENDER_EEVEE"
scene.render.resolution_x = 1400
scene.render.resolution_y = 1050
scene.render.resolution_percentage = 100
scene.render.image_settings.file_format = "PNG"
scene.world.color = (0.006, 0.007, 0.009)

bpy.ops.object.select_all(action="DESELECT")
for obj in scene.objects:
    if obj.type in {"MESH", "CURVE", "EMPTY"} and obj.name != "Inspection floor":
        obj.select_set(True)
bpy.context.view_layer.objects.active = next(
    obj for obj in scene.objects if obj.select_get() and obj.type == "MESH"
)

bpy.ops.wm.save_as_mainfile(filepath=str(OUT / "destiny_console_v3.blend"))
try:
    bpy.ops.wm.obj_export(filepath=str(OUT / "destiny_console_v3.obj"),
                          export_selected_objects=True, export_materials=True,
                          forward_axis="NEGATIVE_Z", up_axis="Y")
except Exception as exc:
    print("OBJ export warning:", exc)

scene.render.filepath = str(OUT / "destiny_console_v3_preview.png")
bpy.ops.render.render(write_still=True)
print("Destiny console v3 generated in", OUT)