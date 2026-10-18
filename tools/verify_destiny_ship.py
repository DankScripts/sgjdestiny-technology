"""Check the captured structure against its manifest and gate exclusions."""
from __future__ import annotations

import collections
import gzip
import json
import sys
from pathlib import Path

from inspect_destiny_region import decode_nbt


def verify(folder: Path):
    manifest = json.loads((folder / "manifest.json").read_text(encoding="utf-8"))
    seen = set()
    counts = collections.Counter()
    entities = collections.Counter()
    door_parts = collections.defaultdict(set)
    door_entities = set()
    oxygen_refill_positions = set()
    suit_chest_items = None
    water_amounts = []
    gate = {tuple(item["pos"]) for item in manifest["preserved_gate_positions"]}
    assert len(gate) == 26
    assert len(manifest["tiles"]) == manifest["tile_count"]
    for tile in manifest["tiles"]:
        raw = gzip.decompress((folder / tile["resource"]).read_bytes())
        assert raw.count(b"\x09\x00\x04size\x03\x00\x00\x00\x03") == 1, tile["resource"]
        assert raw.count(b"\x09\x00\x03pos\x03\x00\x00\x00\x03") == tile["blocks"], tile["resource"]
        data = decode_nbt(raw)
        assert data["size"] == [16, manifest["capture_max"][1] - manifest["capture_min"][1] + 1, 16]
        assert len(data["blocks"]) == tile["blocks"]
        for block in data["blocks"]:
            local = block["pos"]
            pos = (tile["chunk_x"] * 16 + local[0], manifest["capture_min"][1] + local[1], tile["chunk_z"] * 16 + local[2])
            assert pos not in seen, f"duplicate cell {pos}"
            assert pos not in gate, f"preserved gate overwritten at {pos}"
            seen.add(pos)
            state = data["palette"][block["state"]]
            counts[state["Name"]] += 1
            if state["Name"] == "sgjdestiny_dhd:destiny_oxygen_storage_tank":
                oxygen_refill_positions.add(pos)
            if state["Name"] == "sgjdestiny_dhd:destiny_ship_airlock":
                props = state["Properties"]
                column, row = int(props["column"]), int(props["row"])
                facing = props["facing"]
                offsets = {"north": (-1, 0), "east": (0, -1),
                           "south": (1, 0), "west": (0, 1)}
                dx, dz = offsets[facing]
                anchor = (pos[0] + dx * column, pos[1] - row,
                          pos[2] + dz * column)
                door_parts[anchor].add((column, row))
            if "nbt" in block:
                entities[block["nbt"]["id"]] += 1
                if pos == (8, 85, -284) and block["nbt"]["id"] == "minecraft:chest":
                    suit_chest_items = collections.Counter(
                        item["id"] for item in block["nbt"].get("Items", [])
                        for _ in range(item.get("Count", 0)))
                if block["nbt"]["id"] == "sgjdestiny_dhd:destiny_water_holding_tank":
                    water_amounts.append(block["nbt"]["WaterTank"]["Amount"])
                if block["nbt"]["id"] == "sgjdestiny_dhd:destiny_ship_airlock":
                    door_entities.add(pos)
                if block["nbt"]["id"] == "sgjdestiny_dhd:kino_dispenser":
                    assert pos == tuple(manifest["kino_dispenser"])
                    assert block["nbt"]["StoredKinos"] == 2
            if pos == (-4, 75, -316):
                assert state["Name"] == "sgjdestiny_dhd:destiny_dhd"
    assert sum(counts.values()) == (manifest["counts"]["ship_blocks"]
                                    + manifest["counts"]["original_room_air_cells"])
    assert manifest["counts"]["source_non_air_blocks"] == (
        manifest["counts"]["ship_blocks"]
        - manifest["counts"].get("repaired_airlock_parts", 0)
        + manifest["counts"]["excluded_placed_shuttle_blocks"]
        + manifest["counts"]["preserved_gate_blocks"])
    assert counts["minecraft:snow"] == 0
    assert counts["minecraft:snow_block"] == 0
    assert counts["minecraft:spawner"] == 0
    assert entities["minecraft:mob_spawner"] == 0
    assert counts["sgjdestiny_dhd:destiny_shuttle_collision"] == 0
    assert counts["sgjdestiny_dhd:destiny_shuttle_airlock"] == 0
    assert counts["sgjdestiny_dhd:destiny_pilot_seat"] == 0
    assert oxygen_refill_positions == {(4, 86, -291), (5, 86, -291), (6, 86, -291)}
    assert suit_chest_items == collections.Counter({
        f"sgjdestiny_dhd:destiny_environmental_{piece}": 2
        for piece in ("helmet", "chestplate", "leggings", "boots")
    } | {"sgjdestiny_dhd:destiny_oxygen_tank": 2})
    assert sorted(water_amounts) == [0, 0, 52000]
    assert not any(name.startswith("ad_astra:") for name in counts)
    assert manifest["counts"].get("converted_legacy_airlock_blocks", 0) in (0, 18)
    assert manifest["counts"].get("removed_placeholder_lamps", 0) in (0, 7)
    assert len(door_parts) == manifest["counts"]["complete_airlock_doors"]
    assert all(parts == {(column, row) for column in range(3) for row in range(3)}
               for parts in door_parts.values())
    assert door_entities == set(door_parts)
    assert entities["sgjdestiny_dhd:kino_dispenser"] == 1
    assert entities["sgjourney:universe_dhd"] == 1
    assert dict(entities) == manifest["block_entity_ids"]
    assert dict(counts) == manifest["block_ids"]
    for block, minimum in {
        "sgjdestiny_dhd:destiny_oxygen_generator": 6,
        "sgjdestiny_dhd:destiny_water_holding_tank": 81,
        "sgjdestiny_dhd:destiny_control_hub": 1,
        "sgjdestiny_dhd:destiny_control_hub_part": 40,
        "sgjdestiny_dhd:destiny_corridor": 25,
        # Helper counts can change when builders alter corridor entrances.
        # Exact per-cell fidelity is checked against the source snapshot below.
        "sgjdestiny_dhd:destiny_corridor_collision": 25,
        "sgjdestiny_dhd:destiny_door_controller": 66,
        "sgjdestiny_dhd:always_lit_weathered_naquadah_copper_lamp": 308,
    }.items():
        assert counts[block] >= minimum, f"missing captured ship block: {block}"
    assert manifest["counts"]["excluded_placed_shuttle_blocks"] > 0
    print(f"Verified {len(manifest['tiles'])} tiles, {sum(counts.values())} placements, "
          f"{sum(entities.values())} block entities, and all 26 preserved Stargate cells.")


if __name__ == "__main__":
    verify(Path(sys.argv[1]))
    if len(sys.argv) > 2:
        from verify_ship_source_exact import verify as verify_source
        verify_source(Path(sys.argv[2]), Path(sys.argv[1]), Path(sys.argv[3]))
