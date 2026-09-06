"""Patch the tested alpha.26 class to inherit SGJourney's Universe DHD logic.

This is a release-environment fallback for when ForgeGradle is unavailable. The
normal source build already expresses the same change directly in Java.
"""

from pathlib import Path
import struct
import sys


OLD_SUPER = "net/minecraft/world/level/block/HorizontalDirectionalBlock"
NEW_SUPER = "net/povstalec/sgjourney/common/blocks/dhd/UniverseDHDBlock"
REMOVE_NAME = "m_7926_"
REMOVE_DESC = "(Lnet/minecraft/world/level/block/state/StateDefinition$Builder;)V"


def u2(data, offset):
    return struct.unpack_from(">H", data, offset)[0], offset + 2


def u4(data, offset):
    return struct.unpack_from(">I", data, offset)[0], offset + 4


def skip_member(data, offset):
    start = offset
    offset += 6
    count, offset = u2(data, offset)
    for _ in range(count):
        offset += 2
        length, offset = u4(data, offset)
        offset += length
    return data[start:offset], offset


def patch(source, destination):
    data = source.read_bytes()
    if data[:4] != b"\xca\xfe\xba\xbe":
        raise ValueError("Not a Java class file")

    constant_count = struct.unpack_from(">H", data, 8)[0]
    offset = 10
    constants = [None] * constant_count
    rebuilt = bytearray(data[:10])
    index = 1
    replaced = False

    while index < constant_count:
        start = offset
        tag = data[offset]
        offset += 1
        if tag == 1:
            length, offset = u2(data, offset)
            raw = data[offset:offset + length]
            offset += length
            text = raw.decode("utf-8")
            constants[index] = text
            if text == OLD_SUPER:
                raw = NEW_SUPER.encode("utf-8")
                replaced = True
            rebuilt.extend(bytes((tag,)) + struct.pack(">H", len(raw)) + raw)
        else:
            sizes = {3: 4, 4: 4, 5: 8, 6: 8, 7: 2, 8: 2, 9: 4,
                     10: 4, 11: 4, 12: 4, 15: 3, 16: 2, 17: 4,
                     18: 4, 19: 2, 20: 2}
            size = sizes[tag]
            offset += size
            rebuilt.extend(data[start:offset])
            if tag in (5, 6):
                index += 1
        index += 1

    if not replaced:
        raise ValueError("Expected superclass was not found")

    # Copy class header, interfaces, and fields.
    header_start = offset
    offset += 6
    interfaces_count, offset = u2(data, offset)
    offset += interfaces_count * 2
    fields_count, offset = u2(data, offset)
    for _ in range(fields_count):
        _, offset = skip_member(data, offset)
    rebuilt.extend(data[header_start:offset])

    method_count, offset = u2(data, offset)
    kept = []
    removed = 0
    for _ in range(method_count):
        member, next_offset = skip_member(data, offset)
        name_index = struct.unpack_from(">H", data, offset + 2)[0]
        desc_index = struct.unpack_from(">H", data, offset + 4)[0]
        if constants[name_index] == REMOVE_NAME and constants[desc_index] == REMOVE_DESC:
            removed += 1
        else:
            kept.append(member)
        offset = next_offset

    if removed != 1:
        raise ValueError(f"Expected to remove one state-definition override, removed {removed}")

    rebuilt.extend(struct.pack(">H", len(kept)))
    for member in kept:
        rebuilt.extend(member)
    rebuilt.extend(data[offset:])
    destination.write_bytes(rebuilt)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("usage: patch_destiny_universe_dhd_class.py INPUT.class OUTPUT.class")
    patch(Path(sys.argv[1]), Path(sys.argv[2]))
