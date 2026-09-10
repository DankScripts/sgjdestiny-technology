#!/usr/bin/env python3
"""Remap locally compiled Mojang-named classes to Forge 1.20.1 SRG names."""

from pathlib import Path
import struct
import sys


FIELDS = {
    ("hasPhysics", "Z"): "f_107219_",
    ("CUSTOM", "Lnet/minecraft/client/particle/ParticleRenderType;"): "f_107433_",
    ("x", "D"): "f_82479_",
    ("y", "D"): "f_82480_",
    ("z", "D"): "f_82481_",
    ("BLOCK", "Lnet/minecraft/core/DefaultedRegistry;"): "f_256975_",
    ("validBlocks", "Ljava/util/Set;"): "f_58915_",
}

METHODS = {
    ("tick", "()V"): "m_5989_",
    ("remove", "()V"): "m_107274_",
    ("render", "(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"): "m_5744_",
    ("getRenderType", "()Lnet/minecraft/client/particle/ParticleRenderType;"): "m_7556_",
    ("createParticle", "(Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDD)Lnet/minecraft/client/particle/Particle;"): "m_6966_",
    ("getPosition", "()Lnet/minecraft/world/phys/Vec3;"): "m_90583_",
    ("getInstance", "()Lnet/minecraft/client/Minecraft;"): "m_91087_",
    ("renderBuffers", "()Lnet/minecraft/client/renderer/RenderBuffers;"): "m_91269_",
    ("bufferSource", "()Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"): "m_110104_",
    ("lightning", "()Lnet/minecraft/client/renderer/RenderType;"): "m_110502_",
    ("getBuffer", "(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"): "m_6299_",
    ("endBatch", "(Lnet/minecraft/client/renderer/RenderType;)V"): "m_109912_",
    ("vertex", "(DDD)Lcom/mojang/blaze3d/vertex/VertexConsumer;"): "m_5483_",
    ("color", "(IIII)Lcom/mojang/blaze3d/vertex/VertexConsumer;"): "m_6122_",
    ("endVertex", "()V"): "m_5752_",
    ("getInstance", "()Lcom/mojang/blaze3d/vertex/Tesselator;"): "m_85913_",
    ("getBuilder", "()Lcom/mojang/blaze3d/vertex/BufferBuilder;"): "m_85915_",
    ("begin", "(Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;Lcom/mojang/blaze3d/vertex/VertexFormat;)V"): "m_166779_",
    ("end", "()Lcom/mojang/blaze3d/vertex/BufferBuilder$RenderedBuffer;"): "m_231175_",
    ("drawWithShader", "(Lcom/mojang/blaze3d/vertex/BufferBuilder$RenderedBuffer;)V"): "m_231202_",
    ("getPositionColorShader", "()Lnet/minecraft/client/renderer/ShaderInstance;"): "m_172811_",
    ("getX", "()I"): "m_123341_",
    ("getY", "()I"): "m_123342_",
    ("getZ", "()I"): "m_123343_",
    ("getStepX", "()I"): "m_122429_",
    ("getStepZ", "()I"): "m_122431_",
    ("offset", "(III)Lnet/minecraft/core/BlockPos;"): "m_7918_",
    ("immutable", "()Lnet/minecraft/core/BlockPos;"): "m_7949_",
    ("betweenClosed", "(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Ljava/lang/Iterable;"): "m_121940_",
    ("distSqr", "(Lnet/minecraft/core/Vec3i;)D"): "m_123331_",
    ("containsKey", "(Lnet/minecraft/resources/ResourceLocation;)Z"): "m_7804_",
    ("get", "(Lnet/minecraft/resources/ResourceLocation;)Ljava/lang/Object;"): "m_7745_",
    ("location", "()Lnet/minecraft/resources/ResourceLocation;"): "m_135782_",
    ("getGameTime", "()J"): "m_46467_",
    ("dimension", "()Lnet/minecraft/resources/ResourceKey;"): "m_46472_",
    ("hasChunkAt", "(Lnet/minecraft/core/BlockPos;)Z"): "m_46805_",
    ("getChunkSource", "()Lnet/minecraft/server/level/ServerChunkCache;"): "m_7726_",
    ("getLightEngine", "()Lnet/minecraft/server/level/ThreadedLevelLightEngine;"): "m_7827_",
    ("checkBlock", "(Lnet/minecraft/core/BlockPos;)V"): "m_7174_",
    ("getBlockState", "(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"): "m_8055_",
    ("setBlock", "(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"): "m_7731_",
    ("sendParticles", "(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"): "m_8767_",
    ("defaultBlockState", "()Lnet/minecraft/world/level/block/state/BlockState;"): "m_49966_",
    ("is", "(Lnet/minecraft/world/level/block/Block;)Z"): "m_60713_",
    ("getProperties", "()Ljava/util/Collection;"): "m_61147_",
    ("hasProperty", "(Lnet/minecraft/world/level/block/state/properties/Property;)Z"): "m_61138_",
    ("getValue", "(Lnet/minecraft/world/level/block/state/properties/Property;)Ljava/lang/Comparable;"): "m_61143_",
    ("setValue", "(Lnet/minecraft/world/level/block/state/properties/Property;Ljava/lang/Comparable;)Ljava/lang/Object;"): "m_61124_",
    ("accept", "(Lnet/minecraft/world/level/ItemLike;)V"): "m_246326_",
}

FIELDS.update({
    ("POSITION_COLOR", "Lcom/mojang/blaze3d/vertex/VertexFormat;"): "f_85815_",
})


def u2(data, offset):
    return struct.unpack_from(">H", data, offset)[0]


def remap(path: Path):
    data = path.read_bytes()
    count = u2(data, 8)
    entries = [None] * count
    offset = 10
    index = 1
    while index < count:
        tag = data[offset]
        offset += 1
        if tag == 1:
            length = u2(data, offset)
            offset += 2
            entries[index] = [tag, data[offset:offset + length]]
            offset += length
        elif tag in (3, 4): entries[index] = [tag, data[offset:offset + 4]]; offset += 4
        elif tag in (5, 6): entries[index] = [tag, data[offset:offset + 8]]; offset += 8; index += 1
        elif tag in (7, 8, 16, 19, 20): entries[index] = [tag, data[offset:offset + 2]]; offset += 2
        elif tag in (9, 10, 11, 12, 17, 18): entries[index] = [tag, data[offset:offset + 4]]; offset += 4
        elif tag == 15: entries[index] = [tag, data[offset:offset + 3]]; offset += 3
        else: raise ValueError(f"unsupported constant-pool tag {tag}: {path}")
        index += 1

    tail = bytearray(data[offset:])
    cache = {entry[1].decode(): i for i, entry in enumerate(entries) if entry and entry[0] == 1}

    def add_utf8(value):
        if value in cache: return cache[value]
        entries.append([1, value.encode()])
        cache[value] = len(entries) - 1
        return cache[value]

    for entry in entries[1:count]:
        if not entry or entry[0] != 12: continue
        name_i, desc_i = struct.unpack(">HH", entry[1])
        name = entries[name_i][1].decode()
        desc = entries[desc_i][1].decode()
        replacement = METHODS.get((name, desc)) or FIELDS.get((name, desc))
        if replacement: entry[1] = struct.pack(">HH", add_utf8(replacement), desc_i)

    pos = 6
    interfaces = u2(tail, pos)
    pos += 2 + interfaces * 2

    def patch_members(position, mapping):
        members = u2(tail, position)
        position += 2
        for _ in range(members):
            name_at, desc_at = position + 2, position + 4
            name_i, desc_i = u2(tail, name_at), u2(tail, desc_at)
            replacement = mapping.get((entries[name_i][1].decode(), entries[desc_i][1].decode()))
            if replacement: struct.pack_into(">H", tail, name_at, add_utf8(replacement))
            attributes = u2(tail, position + 6)
            position += 8
            for _ in range(attributes):
                length = struct.unpack_from(">I", tail, position + 2)[0]
                position += 6 + length
        return position

    pos = patch_members(pos, FIELDS)
    patch_members(pos, METHODS)

    output = bytearray(data[:8]) + struct.pack(">H", len(entries))
    for entry in entries[1:]:
        if entry is None: continue
        output.append(entry[0])
        output += (struct.pack(">H", len(entry[1])) + entry[1]) if entry[0] == 1 else entry[1]
    path.write_bytes(output + tail)


if __name__ == "__main__":
    for argument in sys.argv[1:]: remap(Path(argument))
