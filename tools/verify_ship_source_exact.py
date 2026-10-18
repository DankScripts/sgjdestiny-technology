"""Audit a ship export against a closed-world region snapshot, cell for cell.

Unlike manifest-only checks, this catches a misplaced pattern even when total
material counts agree. No floor/ceiling substitution or door repair is accepted.
"""
from __future__ import annotations

import gzip
import hashlib
import json
import sys
from collections import Counter, defaultdict
from pathlib import Path

from inspect_destiny_region import chunks, decode_nbt

AIR = {'minecraft:air', 'minecraft:cave_air', 'minecraft:void_air'}
EXCLUDED = {
    'sgjourney:universe_ring', 'sgjourney:universe_stargate',
    'sgjourney:universe_stargate_chevron', 'sgjourney:basic_interface',
    'sgjdestiny_dhd:destiny_shuttle_collision',
    'sgjdestiny_dhd:destiny_shuttle_airlock', 'sgjdestiny_dhd:destiny_pilot_seat',
}


def canonical(state):
    return state['Name'], tuple(sorted(state.get('Properties', {}).items()))


def verify(source: Path, folder: Path, report_path: Path):
    manifest = json.loads((folder / 'manifest.json').read_text(encoding='utf-8'))
    lo, hi = manifest['capture_min'], manifest['capture_max']
    inside = lambda p: all(a <= b <= c for a, b, c in zip(lo, p, hi))
    expected, source_nbt = {}, {}
    excluded = Counter()
    outside = Counter()
    for cx, cz, chunk in chunks(source):
        for section in chunk.get('sections', []):
            states = section.get('block_states', {})
            palette = states.get('palette', [])
            if not palette or all(p['Name'] in AIR for p in palette):
                continue
            packed = states.get('data', [])
            width = max(4, (len(palette)-1).bit_length())
            # Decode each packed long into its padded slots independently of
            # the exporter, then enumerate the section in Y/Z/X order.
            slots = []
            for word in packed:
                word &= (1 << 64)-1
                for _ in range(64 // width):
                    slots.append(word & ((1 << width)-1))
                    word >>= width
            if not packed:
                slots = [0]*4096
            assert len(slots) >= 4096
            for i, index in enumerate(slots[:4096]):
                y, rem = divmod(i, 256)
                z, x = divmod(rem, 16)
                pos = (16*cx+x, 16*section['Y']+y, 16*cz+z)
                state = palette[index]
                name = state['Name']
                if name in AIR:
                    continue
                if not inside(pos):
                    outside[name] += 1
                elif name in EXCLUDED:
                    excluded[name] += 1
                else:
                    assert pos not in expected
                    expected[pos] = canonical(state)
        for entity in chunk.get('block_entities', []):
            pos = tuple(entity[k] for k in ('x', 'y', 'z'))
            if pos in expected:
                source_nbt[pos] = entity

    actual, actual_nbt, all_positions = {}, {}, set()
    for tile in manifest['tiles']:
        data = decode_nbt(gzip.decompress((folder/tile['resource']).read_bytes()))
        assert not data['entities'], 'Unexpected captured entity (including shuttle)'
        for block in data['blocks']:
            x, y, z = block['pos']
            pos = (tile['chunk_x']*16+x, lo[1]+y, tile['chunk_z']*16+z)
            assert pos not in all_positions, f'Duplicate placement at {pos}'
            all_positions.add(pos)
            state = data['palette'][block['state']]
            if state['Name'] not in AIR:
                actual[pos] = canonical(state)
            if 'nbt' in block:
                actual_nbt[pos] = block['nbt']
    missing = expected.keys()-actual.keys()
    extra = actual.keys()-expected.keys()
    changed = [p for p in expected.keys() & actual.keys() if expected[p] != actual[p]]
    assert not missing and not extra and not changed, {
        'missing': sorted(missing)[:10], 'extra': sorted(extra)[:10],
        'changed': [(p, expected[p], actual[p]) for p in changed[:10]]}
    assert actual_nbt == source_nbt, 'Block entity contents differ from saved source'
    gate_positions = {tuple(b['pos']) for b in manifest['preserved_gate_positions']}
    assert not gate_positions & all_positions
    assert len(gate_positions) == 26
    hashes = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(source.glob('r.*.*.mca'))}
    assert hashes == manifest['source_region_sha256']
    layers = defaultdict(list)
    for pos, state in sorted(expected.items()):
        layers[pos[1]].append((pos, state))
    report = {
        'source_snapshot': str(source.resolve()), 'matched_non_air_blocks': len(actual),
        'matched_block_entities': len(actual_nbt), 'state_mismatches': 0,
        'excluded_blocks': dict(excluded), 'outside_capture_bounds': dict(outside),
        'block_counts': dict(sorted(Counter(s[0] for s in actual.values()).items())),
        'verified_layers': {str(y): {'blocks': len(values), 'position_state_sha256':
            hashlib.sha256(json.dumps(values, sort_keys=True).encode()).hexdigest()}
            for y, values in sorted(layers.items())},
        'water_tanks': [entity for entity in actual_nbt.values()
                        if entity['id'] == 'sgjdestiny_dhd:destiny_water_holding_tank'],
    }
    report_path.write_text(json.dumps(report, indent=2)+'\n', encoding='utf-8')
    print(f'EXACT MATCH: {len(actual)} blocks, all states/directions, {len(layers)} height layers, '
          f'{len(actual_nbt)} complete block-entity records. Stargate excluded.')
    print('Blocks outside capture bounds:', dict(outside))


if __name__ == '__main__':
    verify(*(Path(arg) for arg in sys.argv[1:]))
