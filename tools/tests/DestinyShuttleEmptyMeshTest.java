package com.dankscripts.sgjdestiny_dhd.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.concurrent.atomic.AtomicInteger;

public final class DestinyShuttleEmptyMeshTest {
    public static void main(String[] args) {
        try (var storage = new ByteBufferBuilder(1024)) {
            var empty = new BufferBuilder(storage, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            DestinyMeshBuffers.uploadIfPresent(empty, mesh -> {
                throw new AssertionError("An empty filtered layer must not allocate or upload a GPU buffer");
            });
            storage.clear();
            var quad = new BufferBuilder(storage, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int vertex = 0; vertex < 4; vertex++) {
                quad.addVertex(vertex & 1, (vertex >> 1) & 1, 0).setColor(255, 255, 255, 255);
            }
            var uploads = new AtomicInteger();
            DestinyMeshBuffers.uploadIfPresent(quad, mesh -> {
                try {
                    if (mesh.drawState().vertexCount() != 4) throw new AssertionError("Visible quad was lost");
                    uploads.incrementAndGet();
                } finally {
                    mesh.close();
                }
            });
            if (uploads.get() != 1) throw new AssertionError("A visible layer must upload exactly once");
        }
        System.out.println("PASS: empty mesh layers skip GPU upload; visible geometry is retained");
    }
}
