"""Generate the approval model for the Destiny Kino dispenser.

The OBJ is deliberately assembled from separate mechanical parts so it can be
reviewed from every angle in Blockbench before it is converted into the final
Minecraft block model.
"""

from __future__ import annotations

import math
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "blockbench"
OBJ = OUT / "kino_dispenser_preview_v37.obj"
MTL = OUT / "kino_dispenser_preview_v37.mtl"
KINO_OBJ = ROOT / "src/main/resources/assets/sgjdestiny_dhd/models/item/kino.obj"

vertices: list[tuple[float, float, float]] = []
faces: list[tuple[str, list[int]]] = []


def vertex(x: float, y: float, z: float) -> int:
    vertices.append((x, y, z))
    return len(vertices)


def face(material: str, points: list[tuple[float, float, float]]) -> None:
    faces.append((material, [vertex(*point) for point in points]))


def box(name: str, material: str, x1: float, y1: float, z1: float,
        x2: float, y2: float, z2: float) -> None:
    # All faces are emitted independently to keep the preview easy to edit.
    face(material, [(x1, y1, z1), (x2, y1, z1), (x2, y2, z1), (x1, y2, z1)])
    face(material, [(x2, y1, z2), (x1, y1, z2), (x1, y2, z2), (x2, y2, z2)])
    face(material, [(x1, y1, z2), (x1, y1, z1), (x1, y2, z1), (x1, y2, z2)])
    face(material, [(x2, y1, z1), (x2, y1, z2), (x2, y2, z2), (x2, y2, z1)])
    face(material, [(x1, y2, z1), (x2, y2, z1), (x2, y2, z2), (x1, y2, z2)])
    face(material, [(x1, y1, z2), (x2, y1, z2), (x2, y1, z1), (x1, y1, z1)])


def side_cladding(name: str, side: str, x1: float, y1: float, z1: float,
                  x2: float, y2: float, z2: float) -> None:
    """Enclosed side panel with black faces wherever it borders the niche."""
    split = -0.50 if side == "left" else 0.50
    if side == "left":
        face("aged_panel", [(x1, y1, z1), (split, y1, z1),
                            (split, y2, z1), (x1, y2, z1)])
        face("cavity", [(split, y1, z1), (x2, y1, z1),
                        (x2, y2, z1), (split, y2, z1)])
        inner_x, outer_x = x2, x1
    else:
        face("cavity", [(x1, y1, z1), (split, y1, z1),
                        (split, y2, z1), (x1, y2, z1)])
        face("aged_panel", [(split, y1, z1), (x2, y1, z1),
                            (x2, y2, z1), (split, y2, z1)])
        inner_x, outer_x = x1, x2
    face("aged_panel", [(x2, y1, z2), (x1, y1, z2),
                        (x1, y2, z2), (x2, y2, z2)])
    face("cavity", [(inner_x, y1, z1), (inner_x, y1, z2),
                    (inner_x, y2, z2), (inner_x, y2, z1)])
    face("aged_panel", [(outer_x, y1, z2), (outer_x, y1, z1),
                        (outer_x, y2, z1), (outer_x, y2, z2)])
    face("aged_panel", [(x1, y2, z1), (x2, y2, z1),
                        (x2, y2, z2), (x1, y2, z2)])
    face("aged_panel", [(x1, y1, z2), (x2, y1, z2),
                        (x2, y1, z1), (x1, y1, z1)])


def cylinder(material: str, a: tuple[float, float, float], b: tuple[float, float, float],
             radius: float, segments: int = 16) -> None:
    ax, ay, az = a
    bx, by, bz = b
    dx, dy, dz = bx - ax, by - ay, bz - az
    length = math.sqrt(dx * dx + dy * dy + dz * dz)
    ux, uy, uz = dx / length, dy / length, dz / length
    helper = (0.0, 1.0, 0.0) if abs(uy) < 0.9 else (1.0, 0.0, 0.0)
    hx, hy, hz = helper
    vx, vy, vz = (uy * hz - uz * hy, uz * hx - ux * hz, ux * hy - uy * hx)
    vlen = math.sqrt(vx * vx + vy * vy + vz * vz)
    vx, vy, vz = vx / vlen, vy / vlen, vz / vlen
    wx, wy, wz = (uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx)
    ring_a = []
    ring_b = []
    for i in range(segments):
        angle = math.tau * i / segments
        ox = radius * (math.cos(angle) * vx + math.sin(angle) * wx)
        oy = radius * (math.cos(angle) * vy + math.sin(angle) * wy)
        oz = radius * (math.cos(angle) * vz + math.sin(angle) * wz)
        ring_a.append((ax + ox, ay + oy, az + oz))
        ring_b.append((bx + ox, by + oy, bz + oz))
    for i in range(segments):
        j = (i + 1) % segments
        face(material, [ring_a[i], ring_a[j], ring_b[j], ring_b[i]])
    # Export caps as triangle fans. Forge's OBJ loader can leave holes when it
    # triangulates large cap n-gons, which was especially visible in the wide
    # circular pedestal base.
    for i in range(segments):
        j = (i + 1) % segments
        face(material, [a, ring_a[j], ring_a[i]])
        face(material, [b, ring_b[i], ring_b[j]])


def sphere(material: str, center: tuple[float, float, float], radius: float,
           rings: int = 8, segments: int = 16) -> None:
    cx, cy, cz = center
    grid: list[list[tuple[float, float, float]]] = []
    for r in range(rings + 1):
        phi = math.pi * r / rings
        row = []
        for s in range(segments):
            theta = math.tau * s / segments
            row.append((cx + radius * math.sin(phi) * math.cos(theta),
                        cy + radius * math.cos(phi),
                        cz + radius * math.sin(phi) * math.sin(theta)))
        grid.append(row)
    for r in range(rings):
        for s in range(segments):
            n = (s + 1) % segments
            face(material, [grid[r][s], grid[r][n], grid[r + 1][n], grid[r + 1][s]])


def torus_arc(material: str, center: tuple[float, float, float], major: float, minor: float,
              start: float, end: float, arc_segments: int = 8, tube_segments: int = 12) -> None:
    cx, cy, cz = center
    rings = []
    for a in range(arc_segments + 1):
        angle = start + (end - start) * a / arc_segments
        ring = []
        for t in range(tube_segments):
            tube = math.tau * t / tube_segments
            radial = major + minor * math.cos(tube)
            ring.append((cx + radial * math.cos(angle),
                         cy + radial * math.sin(angle),
                         cz + minor * math.sin(tube)))
        rings.append(ring)
    for a in range(arc_segments):
        for t in range(tube_segments):
            n = (t + 1) % tube_segments
            face(material, [rings[a][t], rings[a][n], rings[a + 1][n], rings[a + 1][t]])


def torus_arc_xz(material: str, center: tuple[float, float, float], major: float, minor: float,
                 start: float, end: float, arc_segments: int = 8, tube_segments: int = 12) -> None:
    """Quarter-pipe elbow in the horizontal plane, used to enter the housing face."""
    cx, cy, cz = center
    rings = []
    for a in range(arc_segments + 1):
        angle = start + (end - start) * a / arc_segments
        ring = []
        for t in range(tube_segments):
            tube = math.tau * t / tube_segments
            radial = major + minor * math.cos(tube)
            ring.append((cx + radial * math.cos(angle),
                         cy + minor * math.sin(tube),
                         cz + radial * math.sin(angle)))
        rings.append(ring)
    for a in range(arc_segments):
        for t in range(tube_segments):
            n = (t + 1) % tube_segments
            face(material, [rings[a][t], rings[a][n], rings[a + 1][n], rings[a + 1][t]])


def extruded_polygon(material: str, points: list[tuple[float, float]], z_front: float, z_back: float) -> None:
    front = [(x, y, z_front) for x, y in points]
    back = [(x, y, z_back) for x, y in points]
    face(material, front)
    face(material, list(reversed(back)))
    for i in range(len(points)):
        j = (i + 1) % len(points)
        face(material, [front[i], front[j], back[j], back[i]])


def extruded_ring(material: str, outer: list[tuple[float, float]],
                  inner: list[tuple[float, float]], z_front: float, z_back: float,
                  inner_material: str | None = None) -> None:
    """Build a continuous bezel with a true opening through both faces."""
    if len(outer) != len(inner):
        raise ValueError("Ring contours must contain matching vertices")
    outer_front = [(x, y, z_front) for x, y in outer]
    outer_back = [(x, y, z_back) for x, y in outer]
    inner_front = [(x, y, z_front) for x, y in inner]
    inner_back = [(x, y, z_back) for x, y in inner]
    count = len(outer)
    for i in range(count):
        j = (i + 1) % count
        face(material, [outer_front[i], outer_front[j], inner_front[j], inner_front[i]])
        face(material, [outer_back[j], outer_back[i], inner_back[i], inner_back[j]])
        face(material, [outer_front[i], outer_back[i], outer_back[j], outer_front[j]])
        face(inner_material or material,
             [inner_front[j], inner_back[j], inner_back[i], inner_front[i]])


def actual_kino(center: tuple[float, float, float], scale: float, yaw_degrees: float,
                hide_camera: bool = False) -> None:
    """Insert the shipped Kino OBJ rather than approximating it with a sphere."""
    source_vertices: list[tuple[float, float, float]] = []
    source_faces: list[tuple[str, list[int]]] = []
    material = "kino_shell"
    material_map = {
        "shell": "kino_shell",
        "seam": "kino_seam",
        "camera": "kino_camera",
        "lens": "kino_lens",
        "brass": "kino_brass",
    }
    for line in KINO_OBJ.read_text(encoding="utf-8").splitlines():
        if line.startswith("v "):
            _, x, y, z = line.split()
            source_vertices.append((float(x), float(y), float(z)))
        elif line.startswith("usemtl "):
            material = material_map.get(line.split(maxsplit=1)[1], "kino_shell")
        elif line.startswith("f "):
            indices = [int(part.split("/")[0]) - 1 for part in line.split()[1:]]
            source_faces.append((material, indices))

    cx, cy, cz = center
    angle = math.radians(yaw_degrees)
    sin_yaw, cos_yaw = math.sin(angle), math.cos(angle)
    transformed = []
    for x, y, z in source_vertices:
        # The item OBJ occupies 0..1, so rotate about its actual center.
        x = (x - 0.5) * scale
        y = (y - 0.5) * scale
        z = (z - 0.5) * scale
        transformed.append((cx + x * cos_yaw - z * sin_yaw,
                            cy + y,
                            cz + x * sin_yaw + z * cos_yaw))
    for face_material, indices in source_faces:
        if hide_camera and face_material in {"kino_camera", "kino_lens"}:
            continue
        face(face_material, [transformed[index] for index in indices])


def build() -> None:
    OUT.mkdir(parents=True, exist_ok=True)

    # Floor foot and the tall narrow pedestal shown in the production drawing.
    cylinder("dark_iron", (-0.0, 0.00, 0.02), (0.0, 0.10, 0.02), 0.48, 20)
    cylinder("rim_metal", (0.0, 0.10, 0.02), (0.0, 0.15, 0.02), 0.38, 20)
    box("lower_spine", "dark_iron", -0.25, 0.14, -0.16, 0.25, 1.08, 0.30)
    box("lower_front", "blackened_metal", -0.20, 0.24, -0.25, 0.20, 0.92, -0.15)
    box("service_plate", "rusted_metal", -0.15, 0.35, -0.275, 0.15, 0.68, -0.25)
    cylinder("bolt", (-0.11, 0.40, -0.30), (-0.11, 0.40, -0.25), 0.035, 10)
    cylinder("bolt", (0.11, 0.40, -0.30), (0.11, 0.40, -0.25), 0.035, 10)

    # Rear vertical tower and vented cap.
    box("rear_tower", "blackened_metal", -0.44, 0.92, -0.02, 0.44, 2.78, 0.42)
    # Dark bridge sections extend the rear structure forward until it meets
    # the front housing while preserving the open Kino compartment.
    # These bridge pieces sit behind the niche back panel. Use the same black
    # finish as the cavity so their forward edges cannot show through as a
    # lighter rectangular frame around the stored Kinos.
    box("rear_bridge_upper", "cavity", -0.44, 1.94, -0.12, 0.44, 2.72, -0.02)
    box("rear_bridge_lower", "cavity", -0.44, 0.92, -0.12, 0.44, 1.34, -0.02)
    box("rear_bridge_left", "cavity", -0.44, 1.34, -0.12, -0.34, 1.94, -0.02)
    box("rear_bridge_right", "cavity", 0.34, 1.34, -0.12, 0.44, 1.94, -0.02)
    box("tower_cap", "rim_metal", -0.50, 2.74, -0.08, 0.50, 2.87, 0.44)
    box("cap_dark", "dark_iron", -0.39, 2.87, 0.00, 0.39, 2.97, 0.38)
    for x in (-0.22, -0.11, 0.0, 0.11, 0.22):
        box("top_vent", "cavity", x - 0.035, 2.97, 0.07, x + 0.035, 3.00, 0.30)

    # The wide center tube is the Kino loading path shown entering the top of
    # the screen-used dispenser. It is separate from the thin service pipe.
    cylinder("pipe", (0.0, 2.88, 0.18), (0.0, 5.00, 0.18), 0.235, 20)
    cylinder("pipe_band", (0.0, 2.88, 0.18), (0.0, 3.10, 0.18), 0.275, 20)
    cylinder("pipe_band", (0.0, 4.84, 0.18), (0.0, 5.00, 0.18), 0.275, 20)

    # Upper housing and one continuous shaped bezel around the Kino niche.
    box("upper_housing", "rim_metal", -0.66, 2.04, -0.35, 0.66, 2.72, -0.12)

    # Side cladding bridges the front face assembly to the rear tower so the
    # dispenser reads as one enclosed, bolted housing from either side.
    side_cladding("left_side_cladding", "left", -0.66, 1.18, -0.37,
                  -0.44, 2.04, 0.42)
    side_cladding("right_side_cladding", "right", 0.44, 1.18, -0.37,
                  0.66, 2.04, 0.42)
    for x1, x2 in [(-0.71, -0.65), (0.65, 0.71)]:
        upper_tab_y = 2.45 if x1 < 0 else 2.57
        for y, z in [(upper_tab_y, -0.25), (1.72, -0.25), (1.30, -0.20)]:
            cylinder("bolt", (x1, y, z), (x2, y, z), 0.042, 10)
    # The deep compartment keeps its lower floor. Only the thin front bezel
    # opening is raised to form the retaining edge visible on the prop.
    bay = [(-0.40, 1.94), (0.40, 1.94), (0.48, 1.84), (0.46, 1.50),
           (0.34, 1.34), (-0.32, 1.34), (-0.46, 1.47), (-0.49, 1.72)]
    bezel_opening = [(-0.40, 1.94), (0.40, 1.94), (0.48, 1.84), (0.46, 1.53),
                     (0.34, 1.45), (-0.32, 1.45), (-0.46, 1.50), (-0.49, 1.72)]
    bezel_outer = [(-0.55, 2.04), (0.55, 2.04), (0.63, 1.91), (0.60, 1.40),
                   (0.43, 1.20), (-0.41, 1.20), (-0.60, 1.37), (-0.64, 1.76)]
    bezel_back = -0.36
    extruded_ring("aged_panel", bezel_outer, bezel_opening, -0.43, bezel_back,
                  inner_material="cavity")

    # The niche begins at the rear edge of the bezel, eliminating intersecting
    # side panels while preserving a dark, visibly recessed compartment.
    niche_front = bezel_back
    # Keep the dark rear panel just ahead of the brown rear tower so none of
    # that structural backing shows through the compartment.
    niche_back = -0.04
    face("cavity", [(x, y, niche_back) for x, y in reversed(bay)])
    for i in range(len(bay)):
        j = (i + 1) % len(bay)
        # The bottom is a level floor, not a ramp rising into the Kino bodies.
        if i == 4:
            continue
        front_x1, front_y1 = bezel_opening[i]
        front_x2, front_y2 = bezel_opening[j]
        back_x1, back_y1 = bay[i]
        back_x2, back_y2 = bay[j]
        face("cavity", [(front_x1, front_y1, niche_front),
                         (front_x2, front_y2, niche_front),
                         (back_x2, back_y2, niche_back),
                         (back_x1, back_y1, niche_back)])

    # A short vertical drop sits immediately behind the raised thin bezel;
    # the compartment floor then remains flat at its original lower height.
    face("cavity", [(-0.32, 1.45, bezel_back), (0.34, 1.45, bezel_back),
                     (0.34, 1.34, bezel_back), (-0.32, 1.34, bezel_back)])
    face("cavity", [(-0.32, 1.34, bezel_back), (0.34, 1.34, bezel_back),
                     (0.34, 1.34, niche_back), (-0.32, 1.34, niche_back)])

    # Seal the triangular seams where the low floor meets the angled side
    # walls. Emit both directions so the closures remain visible from inside
    # the niche and from an exterior side view.
    left_seal = [(-0.32, 1.45, bezel_back),
                 (-0.32, 1.34, bezel_back),
                 (-0.32, 1.34, niche_back)]
    right_seal = [(0.34, 1.45, bezel_back),
                  (0.34, 1.34, niche_back),
                  (0.34, 1.34, bezel_back)]
    for seal in (left_seal, right_seal):
        face("cavity", seal)
        face("cavity", list(reversed(seal)))

    # Two copies of the mod's real Kino model sit naturally in the open bay.
    # Their cameras are angled away from straight ahead like the screen-used prop.
    actual_kino((-0.22, 1.56, -0.18), 0.46, -34.0, hide_camera=True)
    actual_kino((0.22, 1.56, -0.18), 0.46, 31.0, hide_camera=True)

    # Heavy horizontal pipe across the face with the characteristic elbows.
    cylinder("pipe", (-0.27, 2.12, -0.61), (0.62, 2.12, -0.61), 0.115, 16)
    cylinder("pipe_band", (-0.25, 2.12, -0.61), (-0.15, 2.12, -0.61), 0.145, 16)
    cylinder("pipe_band", (0.53, 2.12, -0.61), (0.64, 2.12, -0.61), 0.145, 16)
    # This elbow remains level and turns backward directly into the front face.
    torus_arc_xz("pipe", (-0.27, 2.12, -0.49), 0.12, 0.115,
                 -math.pi / 2, -math.pi, 8, 16)
    cylinder("pipe", (-0.39, 2.12, -0.49), (-0.39, 2.12, -0.34), 0.115, 16)
    cylinder("pipe_band", (-0.39, 2.12, -0.49), (-0.39, 2.12, -0.36), 0.145, 16)
    torus_arc("pipe", (0.64, 2.24, -0.61), 0.12, 0.115, -math.pi / 2, 0, 8, 16)
    cylinder("pipe", (0.76, 2.24, -0.61), (0.76, 5.00, -0.61), 0.115, 16)
    cylinder("pipe_band", (0.76, 2.24, -0.61), (0.76, 2.36, -0.61), 0.145, 16)
    cylinder("pipe_band", (0.76, 4.84, -0.61), (0.76, 5.00, -0.61), 0.145, 16)

    # Upper control pod, cable, screen, two round switches and four status keys.
    # The control pod overlaps the front housing slightly so it is visibly
    # mounted to the panel rather than floating ahead of it.
    box("control_body", "rim_metal", -0.10, 2.31, -0.61, 0.40, 2.61, -0.32)
    box("control_face", "aged_panel", -0.06, 2.36, -0.635, 0.36, 2.56, -0.605)
    box("display", "display", 0.11, 2.46, -0.66, 0.32, 2.53, -0.635)
    cylinder("control_button", (-0.01, 2.50, -0.67), (-0.01, 2.50, -0.63), 0.035, 12)
    cylinder("control_button", (0.06, 2.50, -0.67), (0.06, 2.50, -0.63), 0.035, 12)
    for i in range(4):
        box("status_key", "status_light", -0.02 + i * 0.085, 2.39, -0.67,
            0.04 + i * 0.085, 2.44, -0.635)
    # Keep the original control-pod connection. The opposite end wraps around
    # the housing corner and terminates at the existing coin-shaped side tab.
    # Carry the front run beyond the white housing before wrapping around it.
    cylinder("cable", (-0.70, 2.45, -0.52), (-0.10, 2.45, -0.52), 0.025, 10)
    torus_arc_xz("cable", (-0.70, 2.45, -0.41), 0.11, 0.025,
                 -math.pi / 2, -math.pi, 8, 10)
    cylinder("cable", (-0.81, 2.45, -0.41), (-0.81, 2.45, -0.35), 0.025, 10)
    # The second elbow turns inward, aiming squarely into the outward-facing
    # center of the round side tab rather than clipping into its side.
    torus_arc_xz("cable", (-0.71, 2.45, -0.35), 0.10, 0.025,
                 math.pi, math.pi / 2, 8, 10)
    cylinder("cable", (-0.71, 2.45, -0.25), (-0.69, 2.45, -0.25), 0.025, 10)

    # Four prominent service fasteners around the main silver bezel.
    for x, y in [(-0.52, 2.60), (0.52, 2.60), (-0.45, 1.26), (0.45, 1.26)]:
        cylinder("bolt", (x, y, -0.43), (x, y, -0.35), 0.055, 10)
        cylinder("dark_iron", (x, y, -0.455), (x, y, -0.425), 0.022, 10)

    with OBJ.open("w", encoding="utf-8", newline="\n") as stream:
        stream.write("# Kino dispenser approval model\n")
        stream.write(f"mtllib {MTL.name}\n")
        # OBJ viewers face the model from negative Z, so mirror X to preserve
        # the screen-used dispenser's right-hand ceiling riser and controls.
        for x, y, z in vertices:
            stream.write(f"v {-x:.6f} {y:.6f} {z:.6f}\n")
        current = None
        for material, indices in faces:
            if material != current:
                stream.write(f"usemtl {material}\n")
                current = material
            stream.write("f " + " ".join(str(index) for index in reversed(indices)) + "\n")

    MTL.write_text("""# Kino dispenser preview materials
newmtl dark_iron
Kd 0.070 0.060 0.050
Ks 0.30 0.26 0.20
Ns 28

newmtl blackened_metal
Kd 0.115 0.090 0.070
Ks 0.28 0.23 0.17
Ns 22

newmtl rim_metal
Kd 0.42 0.39 0.31
Ks 0.48 0.43 0.34
Ns 44

newmtl aged_panel
Kd 0.30 0.285 0.235
Ks 0.29 0.26 0.20
Ns 20

newmtl rusted_metal
Kd 0.235 0.105 0.045
Ks 0.16 0.095 0.055
Ns 10

newmtl cavity
Kd 0.018 0.018 0.016
Ks 0.02 0.02 0.02
Ns 4

newmtl pipe
Kd 0.145 0.105 0.075
Ks 0.38 0.30 0.22
Ns 34

newmtl pipe_band
Kd 0.32 0.285 0.22
Ks 0.46 0.40 0.30
Ns 40

newmtl bolt
Kd 0.24 0.225 0.19
Ks 0.52 0.47 0.38
Ns 48

newmtl display
Kd 0.015 0.035 0.033
Ke 0.010 0.035 0.030

newmtl control_button
Kd 0.215 0.205 0.175
Ks 0.48 0.43 0.35
Ns 44

newmtl status_light
Kd 0.48 0.455 0.31
Ke 0.07 0.065 0.035

newmtl cable
Kd 0.055 0.050 0.045

newmtl kino_shell
Kd 0.18 0.175 0.155
Ks 0.42 0.39 0.32
Ns 40

newmtl kino_seam
Kd 0.39 0.325 0.19
Ks 0.45 0.37 0.24
Ns 36

newmtl kino_lens
Kd 0.040 0.13 0.14
Ks 0.50 0.60 0.58
Ns 60

newmtl kino_camera
Kd 0.075 0.080 0.075
Ks 0.28 0.30 0.28
Ns 32

newmtl kino_brass
Kd 0.34 0.255 0.12
Ks 0.44 0.34 0.19
Ns 38
""", encoding="utf-8", newline="\n")
    print(OBJ)


if __name__ == "__main__":
    build()
