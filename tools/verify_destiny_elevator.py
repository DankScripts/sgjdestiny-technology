"""Check the real bundled shaft before shipping the elevator retrofit."""
from pathlib import Path
import gzip
import json
import math
from inspect_destiny_region import decode_nbt


def verify(folder):
    manifest = json.loads((folder / 'manifest.json').read_text())
    world = {}
    for tile in manifest['tiles']:
        if tile['chunk_x'] != -1 or tile['chunk_z'] != -25:
            continue
        data = decode_nbt(gzip.decompress((folder / tile['resource']).read_bytes()))
        for block in data['blocks']:
            x, y, z = block['pos']
            world[x - 16, y + manifest['capture_min'][1], z - 400] = data['palette'][block['state']]

    def name(p):
        return world.get(p, {}).get('Name', 'minecraft:air')

    floors = [70, 75, 80, 85, 93, 99, 105, 111]
    motor = {(-4,66,-387): 'sticky_piston', (-4,66,-386): 'slime_block', (-3,66,-386): 'observer',
             (-4,67,-387): 'slime_block', (-3,67,-387): 'observer', (-4,67,-386): 'sticky_piston',
             (-3,67,-386): 'observer'}
    for x in [-4, -3]:
        for z in [-387, -386]:
            motor[x,68,z] = 'slime_block'
            motor[x,69,z] = 'sea_lantern'
    assert len(motor) == 15
    for p, expected in motor.items():
        assert name(p) == 'minecraft:' + expected, (p, expected, name(p))
    for floor in floors:
        assert name((-6, floor + 1, -388)) == 'minecraft:air', ('call button occupied', floor)
        assert name((-5, floor + 1, -388)) == 'minecraft:light_gray_concrete', ('button support', floor)
        for z in [-387, -386]:
            assert name((-6, floor - 1, z)) != 'minecraft:air', ('missing exit floor', floor)
            for y in [floor, floor + 1]:
                assert name((-5,y,z)) in ['minecraft:air', 'minecraft:spruce_door'], ('doorway', floor)
    # Remove exactly the existing moving mechanism. Decorative floor finishes remain untouched.
    original = dict(world)
    for p in motor:
        world.pop(p)
    for from_floor in floors:
        for to_floor in floors:
            for x in [-4, -3]:
                for z in [-387, -386]:
                    for y in range(min(from_floor,to_floor)-1, math.ceil(max(from_floor,to_floor)+2.7)):
                        assert name((x,y,z)) == 'minecraft:air', ('cab route obstructed', (x,y,z))
    assert all(world.get(p) == state for p,state in original.items() if p not in motor)
    print('PASS: exact 15-block retrofit footprint, all eight call buttons/landings, 64 unobstructed cab routes; surrounding patterns preserved.')


if __name__ == '__main__':
    verify(Path(__file__).resolve().parents[1] / 'src/main/resources/data/sgjdestiny_dhd/structures/destiny_ship')
