"""Export the Destiny ship as sparse, chunk-aligned Minecraft structures.

Read a copy of the source region files, never the live world. The capture is
limited to the measured ship bounds; entities and Stargate block entities stay
out of the package.
"""
from __future__ import annotations

import gzip
import hashlib
import json
import struct
import sys
from collections import Counter
from pathlib import Path

from inspect_destiny_region import chunks

MIN_Y = 47
MAX_Y = 117
MIN_X = -165
MAX_X = 149
MIN_Z = -424
MAX_Z = -40
ORIGINAL_ROOM_MIN = (-16, 69, -310)
ORIGINAL_ROOM_MAX = (0, 84, -278)
GATE_BLOCKS = {
    "sgjourney:universe_ring",
    "sgjourney:universe_stargate",
    "sgjourney:universe_stargate_chevron",
    "sgjourney:basic_interface",
}
# The source world currently has test shuttles parked near the ship. Their
# collision/seat/door blocks are live vehicle state, not part of the static
# Destiny dimension that should be installed in another world.
PLACED_SHUTTLE_BLOCKS = {
    "sgjdestiny_dhd:destiny_shuttle_collision",
    "sgjdestiny_dhd:destiny_shuttle_airlock",
    "sgjdestiny_dhd:destiny_pilot_seat",
}
# Two remaining Ad Astra doorways in the source ship have the same 3x3
# footprint as our own airlock. Capture them as native ship doors instead.
LEGACY_AIRLOCK_CENTERS = ((-20, 80, -288), (4, 80, -289))
LEGACY_AIRLOCK_PARTS = {
    (center_x - 1 + column, center_y + row, center_z): (column, row)
    for center_x, center_y, center_z in LEGACY_AIRLOCK_CENTERS
    for row in range(3)
    for column in range(3)
}
KEEP_BLOCK_ENTITY_IDS = {
    "sgjdestiny_dhd:kino_dispenser",
    "sgjdestiny_dhd:destiny_ship_airlock",
    "sgjdestiny_dhd:destiny_corridor",
    "sgjdestiny_dhd:destiny_oxygen_generator",
    "sgjdestiny_dhd:destiny_water_holding_tank",
    "sgjdestiny_dhd:destiny_control_hub",
    "sgjourney:universe_dhd",
    "minecraft:chest",
    "minecraft:sign",
    "minecraft:banner",
    "minecraft:brewing_stand",
    "minecraft:beacon",
    "minecraft:comparator",
}


def s(value: str) -> bytes:
    data = value.encode("utf-8")
    return struct.pack(">H", len(data)) + data


def tag(kind: int, name: str, payload: bytes) -> bytes:
    return bytes([kind]) + s(name) + payload


def compound(entries: list[bytes]) -> bytes:
    return b"".join(entries) + b"\0"


def int_array(values: list[int]) -> bytes:
    return struct.pack(">i", len(values)) + b"".join(struct.pack(">i", value) for value in values)


def int_list(values: list[int]) -> bytes:
    # StructureTemplate.load reads size and block pos as TAG_List<TAG_Int>,
    # not TAG_Int_Array. The latter silently becomes an empty list in Minecraft.
    return list_tag(3, [struct.pack(">i", value) for value in values])


def list_tag(kind: int, values: list[bytes]) -> bytes:
    return bytes([kind]) + struct.pack(">i", len(values)) + b"".join(values)


def state_payload(name: str, properties: dict[str, str]) -> bytes:
    entries = [tag(8, "Name", s(name))]
    if properties:
        entries.append(tag(10, "Properties", compound([
            tag(8, key, s(value)) for key, value in sorted(properties.items())
        ])))
    return compound(entries)


def export(source: Path, destination: Path):
    destination.mkdir(parents=True, exist_ok=True)
    tiles = {}
    totals = Counter()
    block_entity_ids = Counter()
    namespaces = Counter()
    block_ids = Counter()
    room_occupied = set()
    gate_positions = []
    dispenser_positions = []
    captured_occupied = set()
    airlock_parts = {}
    for cx, cz, chunk, raw_entities in chunks(source, with_raw=True):
        blocks = []
        palette = []
        palette_index = {}
        entities_by_pos = {
            (entity["x"], entity["y"], entity["z"]): entity
            for entity in chunk.get("block_entities", [])
        }
        for section in chunk.get("sections", []):
            block_states = section.get("block_states", {})
            section_palette = block_states.get("palette", [])
            if not section_palette:
                continue
            if len(section_palette) == 1 and section_palette[0]["Name"] in (
                "minecraft:air", "minecraft:cave_air", "minecraft:void_air"
            ):
                continue
            sy = section["Y"]
            data = block_states.get("data", [])
            bits = max(4, (len(section_palette) - 1).bit_length())
            per_long = 64 // bits
            mask = (1 << bits) - 1
            for index in range(4096):
                state_index = (((data[index // per_long] & 0xffffffffffffffff)
                                >> ((index % per_long) * bits)) & mask) if data else 0
                state = section_palette[state_index]
                name = state["Name"]
                x = cx * 16 + (index & 15)
                y = sy * 16 + ((index >> 8) & 15)
                z = cz * 16 + ((index >> 4) & 15)
                if not (MIN_X <= x <= MAX_X and MIN_Z <= z <= MAX_Z):
                    continue
                if name in ("minecraft:air", "minecraft:cave_air", "minecraft:void_air"):
                    continue
                if not MIN_Y <= y <= MAX_Y:
                    raise ValueError(f"block outside measured ship height: {name} at {x},{y},{z}")
                totals["source_non_air_blocks"] += 1
                if name in PLACED_SHUTTLE_BLOCKS:
                    totals["excluded_placed_shuttle_blocks"] += 1
                    continue
                if name in GATE_BLOCKS:
                    totals["preserved_gate_blocks"] += 1
                    gate_positions.append({"name": name, "pos": [x, y, z]})
                    room_occupied.add((x, y, z))
                    continue
                if (ORIGINAL_ROOM_MIN[0] <= x <= ORIGINAL_ROOM_MAX[0]
                        and ORIGINAL_ROOM_MIN[1] <= y <= ORIGINAL_ROOM_MAX[1]
                        and ORIGINAL_ROOM_MIN[2] <= z <= ORIGINAL_ROOM_MAX[2]):
                    room_occupied.add((x, y, z))
                converted_airlock = False
                if name == "ad_astra:airlock":
                    part = LEGACY_AIRLOCK_PARTS.get((x, y, z))
                    if part is None:
                        raise ValueError(f"unexpected legacy airlock at {x},{y},{z}")
                    column, row = part
                    open_state = state.get("Properties", {}).get("open", "false")
                    name = "sgjdestiny_dhd:destiny_ship_airlock"
                    properties = {
                        "facing": "north", "column": str(column), "row": str(row),
                        "open": open_state, "powered": "false",
                    }
                    converted_airlock = True
                    totals["converted_legacy_airlock_blocks"] += 1
                else:
                    properties = state.get("Properties", {})
                if name == "ad_astra:small_light_blue_industrial_lamp":
                    name = "minecraft:air"
                    totals["removed_placeholder_lamps"] += 1
                converted_spawner = name == "minecraft:spawner"
                if converted_spawner:
                    # The source contains decorative armor-stand spawners. Do
                    # not ship live spawner block entities into another world.
                    name = "minecraft:air"
                    totals["replaced_spawners_with_air"] += 1
                # Snow accumulated during the source-world capture is weather,
                # not part of the ship. Explicit air also removes it on reset.
                if name in ("minecraft:snow", "minecraft:snow_block"):
                    name = "minecraft:air"
                if name.startswith("ad_astra:"):
                    raise ValueError(f"unconverted Ad Astra block {name} at {x},{y},{z}")
                if name == "minecraft:air":
                    properties = {}
                else:
                    captured_occupied.add((x, y, z))
                if name == "sgjdestiny_dhd:destiny_ship_airlock":
                    airlock_parts[(x, y, z)] = dict(properties)
                key = (name, tuple(sorted(properties.items())))
                if key not in palette_index:
                    palette_index[key] = len(palette)
                    palette.append(state_payload(name, properties))
                block_entity = entities_by_pos.get((x, y, z))
                raw_entity = None
                if block_entity:
                    entity_id = block_entity["id"]
                    if converted_spawner and entity_id == "minecraft:mob_spawner":
                        pass
                    elif converted_airlock and entity_id == "ad_astra:sliding_door":
                        pass
                    elif entity_id not in KEEP_BLOCK_ENTITY_IDS:
                        raise ValueError(f"unreviewed block entity {entity_id} at {x},{y},{z}")
                    else:
                        raw_entity = raw_entities[(x, y, z)]
                        if entity_id == "sgjdestiny_dhd:kino_dispenser":
                            if block_entity.get("StoredKinos") != 2:
                                raise ValueError("source Kino Dispenser does not contain exactly two Kinos")
                            dispenser_positions.append([x, y, z])
                            totals["dispenser_with_two_kinos"] += 1
                        block_entity_ids[entity_id] += 1
                if converted_airlock and (column, row) == (0, 0):
                    raw_entity = compound([
                        tag(8, "id", s("sgjdestiny_dhd:destiny_ship_airlock")),
                        tag(3, "x", struct.pack(">i", x)),
                        tag(3, "y", struct.pack(">i", y)),
                        tag(3, "z", struct.pack(">i", z)),
                    ])
                    block_entity_ids["sgjdestiny_dhd:destiny_ship_airlock"] += 1
                pos = int_list([index & 15, y - MIN_Y, (index >> 4) & 15])
                entries = [tag(9, "pos", pos), tag(3, "state", struct.pack(">i", palette_index[key]))]
                if raw_entity:
                    entries.append(tag(10, "nbt", raw_entity))
                blocks.append(compound(entries))
                totals["ship_blocks"] += 1
                namespaces[name.split(":", 1)[0]] += 1
                block_ids[name] += 1
        if blocks:
            tiles[(cx, cz)] = [palette, blocks]

    # Some source-world airlocks have only their anchor left after block edits.
    # Restore their missing sections in the package only; never replace a new
    # non-air block or change the live world. The anchor supplies facing/state.
    offsets = {"north": (1, 0), "east": (0, 1), "south": (-1, 0), "west": (0, -1)}
    anchors = [(pos, props) for pos, props in airlock_parts.items()
               if props.get("column") == "0" and props.get("row") == "0"]
    for (ax, ay, az), anchor_props in anchors:
        dx, dz = offsets[anchor_props["facing"]]
        for row in range(3):
            for column in range(3):
                x, y, z = ax + dx * column, ay + row, az + dz * column
                target = (x, y, z)
                props = dict(anchor_props, column=str(column), row=str(row))
                if target in airlock_parts:
                    existing = airlock_parts[target]
                    if existing.get("column") != str(column) or existing.get("row") != str(row) \
                            or existing.get("facing") != anchor_props["facing"]:
                        raise ValueError(f"conflicting airlock part at {target}")
                    continue
                if target in captured_occupied or any(item["pos"] == [x, y, z]
                                                      for item in gate_positions):
                    raise ValueError(f"cannot repair airlock over another block at {target}")
                cx, cz = x >> 4, z >> 4
                palette, blocks = tiles.setdefault((cx, cz), [[], []])
                state = state_payload("sgjdestiny_dhd:destiny_ship_airlock", props)
                if state not in palette:
                    palette.append(state)
                blocks.append(compound([
                    tag(9, "pos", int_list([x & 15, y - MIN_Y, z & 15])),
                    tag(3, "state", struct.pack(">i", palette.index(state))),
                ]))
                airlock_parts[target] = props
                captured_occupied.add(target)
                room_occupied.add(target)
                totals["ship_blocks"] += 1
                totals["repaired_airlock_parts"] += 1
                namespaces["sgjdestiny_dhd"] += 1
                block_ids["sgjdestiny_dhd:destiny_ship_airlock"] += 1
    totals["complete_airlock_doors"] = len(anchors)

    # SGJourney's original gate room is 17 x 16 x 33 at this fixed anchor.
    # Clear only its cells absent from the server copy so old room walls cannot
    # remain inside the replacement; never touch the 26 live gate components.
    for x in range(ORIGINAL_ROOM_MIN[0], ORIGINAL_ROOM_MAX[0] + 1):
        for y in range(ORIGINAL_ROOM_MIN[1], ORIGINAL_ROOM_MAX[1] + 1):
            for z in range(ORIGINAL_ROOM_MIN[2], ORIGINAL_ROOM_MAX[2] + 1):
                if (x, y, z) in room_occupied:
                    continue
                cx, cz = x >> 4, z >> 4
                palette, blocks = tiles.setdefault((cx, cz), [[], []])
                air_state = state_payload("minecraft:air", {})
                if air_state not in palette:
                    palette.append(air_state)
                entries = [
                    tag(9, "pos", int_list([x & 15, y - MIN_Y, z & 15])),
                    tag(3, "state", struct.pack(">i", palette.index(air_state))),
                ]
                blocks.append(compound(entries))
                totals["original_room_air_cells"] += 1
                block_ids["minecraft:air"] += 1

    if totals["dispenser_with_two_kinos"] != 1:
        raise ValueError("expected exactly one stocked Kino Dispenser")
    if len(dispenser_positions) != 1:
        raise ValueError("expected exactly one Kino Dispenser position")
    if totals["converted_legacy_airlock_blocks"] not in (0, len(LEGACY_AIRLOCK_PARTS)):
        raise ValueError("source has an incomplete legacy airlock conversion")
    if totals["removed_placeholder_lamps"] not in (0, 7):
        raise ValueError("source placeholder lamp count changed unexpectedly")
    if block_entity_ids["sgjourney:universe_dhd"] != 1:
        raise ValueError("expected exactly one Destiny dialing console")
    expected_gate_blocks = 26
    if totals["preserved_gate_blocks"] < expected_gate_blocks:
        raise ValueError("the original Stargate assembly is incomplete")
    if totals["source_non_air_blocks"] != (totals["ship_blocks"]
            - totals["repaired_airlock_parts"] + totals["excluded_placed_shuttle_blocks"]
            + totals["preserved_gate_blocks"]):
        raise ValueError("captured source blocks do not reconcile with ship exclusions")

    metadata = {
        "source_dimension": "sgjourney:destiny",
        "capture_min": [MIN_X, MIN_Y, MIN_Z],
        "capture_max": [MAX_X, MAX_Y, MAX_Z],
        "gate_controller": [-8, 74, -299],
        "dialing_console": [-4, 75, -316],
        "kino_dispenser": dispenser_positions[0],
        "kino_dispenser_stored_kits": 2,
        "original_room_min": ORIGINAL_ROOM_MIN,
        "original_room_max": ORIGINAL_ROOM_MAX,
        "preserved_gate_positions": sorted(gate_positions, key=lambda item: item["pos"]),
        "tile_count": len(tiles),
        "counts": dict(totals),
        "block_entity_ids": dict(block_entity_ids),
        "namespaces": dict(namespaces),
        "block_ids": dict(sorted(block_ids.items())),
        "source_region_sha256": {
            path.name: hashlib.sha256(path.read_bytes()).hexdigest()
            for path in sorted(source.glob("r.*.*.mca"))
        },
        "tiles": [],
    }
    for (cx, cz), (palette, blocks) in sorted(tiles.items()):
        root = compound([
            tag(3, "DataVersion", struct.pack(">i", 3465)),
            tag(9, "size", int_list([16, MAX_Y - MIN_Y + 1, 16])),
            tag(9, "palette", list_tag(10, palette)),
            tag(9, "blocks", list_tag(10, blocks)),
            tag(9, "entities", list_tag(10, [])),
        ])
        path = destination / f"tile_{cx}_{cz}.nbt"
        with path.open("wb") as stream:
            with gzip.GzipFile(fileobj=stream, mode="wb", mtime=0) as packed:
                packed.write(b"\x0a\x00\x00" + root)
        metadata["tiles"].append({"chunk_x": cx, "chunk_z": cz, "resource": path.name,
                                  "blocks": len(blocks)})
    (destination / "manifest.json").write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({key: metadata[key] for key in (
        "tile_count", "counts", "block_entity_ids", "namespaces")}, indent=2))


if __name__ == "__main__":
    export(Path(sys.argv[1]), Path(sys.argv[2]))
