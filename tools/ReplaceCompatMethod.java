import java.nio.file.Files;
import java.nio.file.Path;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;

/** Build helper: transplant one method without recompiling the proven compatibility class. */
public final class ReplaceCompatMethod {
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException("base.class donor.class methodName output.class");
        }
        ClassNode base = read(Path.of(args[0]));
        ClassNode donor = read(Path.of(args[1]));
        String methodName = args[2];
        MethodNode replacement = donor.methods.stream()
                .filter(method -> method.name.equals(methodName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Donor method not found: " + methodName));
        base.methods.removeIf(method -> method.name.equals(methodName) && method.desc.equals(replacement.desc));
        base.methods.add(replacement);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        base.accept(writer);
        Files.write(Path.of(args[3]), writer.toByteArray());
    }

    private static ClassNode read(Path path) throws Exception {
        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(path)).accept(node, 0);
        return node;
    }
}
