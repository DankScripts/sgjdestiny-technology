import com.dankscripts.sgjdestiny_dhd.client.DestinyMeshBuffers;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryUtil;

/** Runs against the reobfuscated mod and installed Minecraft, with actual native allocation. */
public class MeshBufferReuseTest {
    public static void main(String[] args) throws Exception {
        RenderSystem.initRenderThread();
        BufferBuilder first = DestinyMeshBuffers.staging();
        var field = java.util.Arrays.stream(BufferBuilder.class.getDeclaredFields())
                .filter(f -> f.getType() == ByteBuffer.class).findFirst().orElseThrow();
        field.setAccessible(true);
        ByteBuffer data = (ByteBuffer) field.get(first);
        long address = MemoryUtil.memAddress(data);
        int capacity = data.capacity();
        for (int i = 0; i < 100_000; i++) {
            BufferBuilder next = DestinyMeshBuffers.staging();
            if (next != first || MemoryUtil.memAddress((ByteBuffer) field.get(next)) != address
                    || ((ByteBuffer) field.get(next)).capacity() != capacity)
                throw new AssertionError("Native staging allocation changed during reuse");
        }
        int[] closed = {0};
        DestinyMeshBuffers.register(MeshBufferReuseTest.class, () -> closed[0]++);
        DestinyMeshBuffers.register(MeshBufferReuseTest.class, () -> closed[0] += 10);
        if (closed[0] != 1) throw new AssertionError("Replaced renderer was not cleaned up");
        DestinyMeshBuffers.clearCaches();
        DestinyMeshBuffers.clearCaches();
        if (closed[0] != 21) throw new AssertionError("Reload/logout cleanup not retained");
        System.out.println("PASS: 100,000 staging requests reuse one native allocation (" + capacity
                + " bytes); renderer replacement and repeat cleanup callbacks verified.");
    }
}
