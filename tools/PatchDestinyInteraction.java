import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

/** Release fallback that applies the alpha.28 interaction source to alpha.27. */
public final class PatchDestinyInteraction implements Opcodes {
    private static final String BLOCK =
            "com/dankscripts/sgjdestiny_dhd/block/DestinyDHDConsoleBlock";
    private static final String PROVIDER = BLOCK + "$DestinyMenuProvider";
    private static final String DESTINY_MENU = BLOCK + "$DestinyUniverseDHDMenu";
    private static final String UNIVERSE_BLOCK =
            "net/povstalec/sgjourney/common/blocks/dhd/UniverseDHDBlock";
    private static final String UNIVERSE_ENTITY =
            "net/povstalec/sgjourney/common/block_entities/dhd/UniverseDHDEntity";

    private static ClassWriter writer(ClassReader reader) {
        return new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override protected String getCommonSuperClass(String left, String right) {
                return "java/lang/Object";
            }
        };
    }

    private static byte[] patchBlock(byte[] original) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = writer(reader);
        reader.accept(new ClassVisitor(ASM8, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                if (name.equals("m_6227_")) return null;
                MethodVisitor base = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!name.equals("<init>")) return base;
                return new MethodVisitor(ASM8, base) {
                    boolean parentConstructed;

                    @Override public void visitMethodInsn(int opcode, String owner, String method,
                                                          String desc, boolean isInterface) {
                        if (!parentConstructed) {
                            super.visitMethodInsn(opcode, owner, method, desc, isInterface);
                            if (opcode == INVOKESPECIAL && owner.equals(UNIVERSE_BLOCK)
                                    && method.equals("<init>")) parentConstructed = true;
                        }
                    }

                    @Override public void visitInsn(int opcode) {
                        if (!parentConstructed || opcode == RETURN) super.visitInsn(opcode);
                    }

                    @Override public void visitVarInsn(int opcode, int var) {
                        if (!parentConstructed) super.visitVarInsn(opcode, var);
                    }

                    @Override public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                        if (!parentConstructed) super.visitFieldInsn(opcode, owner, name, desc);
                    }

                    @Override public void visitTypeInsn(int opcode, String type) {
                        if (!parentConstructed) super.visitTypeInsn(opcode, type);
                    }

                    @Override public void visitLdcInsn(Object value) {
                        if (!parentConstructed) super.visitLdcInsn(value);
                    }

                    @Override public void visitLabel(Label label) {
                        if (!parentConstructed) super.visitLabel(label);
                    }

                    @Override public void visitLineNumber(int line, Label start) {
                        if (!parentConstructed) super.visitLineNumber(line, start);
                    }

                    @Override public void visitLocalVariable(String name, String desc, String sig,
                                                             Label start, Label end, int index) {
                        // Old debug ranges no longer describe the shortened constructor.
                    }
                };
            }

            @Override public void visitEnd() {
                addUseMethod(cv);
                super.visitEnd();
            }
        }, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static void addUseMethod(ClassVisitor cv) {
        String desc = "(Lnet/minecraft/world/level/block/state/BlockState;"
                + "Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;"
                + "Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;"
                + "Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;";
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "m_6227_", desc, null, null);
        mv.visitCode();
        Label server = new Label();
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "net/minecraft/world/level/Level", "m_5776_", "()Z", false);
        mv.visitJumpInsn(IFEQ, server);
        mv.visitFieldInsn(GETSTATIC, "net/minecraft/world/InteractionResult", "SUCCESS",
                "Lnet/minecraft/world/InteractionResult;");
        mv.visitInsn(ARETURN);
        mv.visitLabel(server);
        Label notSneaking = new Label();
        mv.visitVarInsn(ALOAD, 4);
        mv.visitMethodInsn(INVOKEVIRTUAL, "net/minecraft/world/entity/player/Player", "m_6144_", "()Z", false);
        mv.visitJumpInsn(IFEQ, notSneaking);
        mv.visitFieldInsn(GETSTATIC, "net/minecraft/world/InteractionResult", "CONSUME",
                "Lnet/minecraft/world/InteractionResult;");
        mv.visitInsn(ARETURN);
        mv.visitLabel(notSneaking);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "net/minecraft/world/level/Level", "m_7702_",
                "(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;", false);
        mv.visitVarInsn(ASTORE, 7);
        Label done = new Label();
        mv.visitVarInsn(ALOAD, 7);
        mv.visitTypeInsn(INSTANCEOF, UNIVERSE_ENTITY);
        mv.visitJumpInsn(IFEQ, done);
        mv.visitVarInsn(ALOAD, 7);
        mv.visitTypeInsn(CHECKCAST, UNIVERSE_ENTITY);
        mv.visitVarInsn(ASTORE, 8);
        mv.visitVarInsn(ALOAD, 4);
        mv.visitTypeInsn(INSTANCEOF, "net/minecraft/server/level/ServerPlayer");
        mv.visitJumpInsn(IFEQ, done);
        mv.visitVarInsn(ALOAD, 4);
        mv.visitTypeInsn(CHECKCAST, "net/minecraft/server/level/ServerPlayer");
        mv.visitVarInsn(ALOAD, 8);
        mv.visitMethodInsn(INVOKEVIRTUAL, UNIVERSE_ENTITY, "generate", "()V", false);
        mv.visitTypeInsn(NEW, PROVIDER);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 8);
        mv.visitMethodInsn(INVOKESPECIAL, PROVIDER, "<init>",
                "(L" + UNIVERSE_ENTITY + ";)V", false);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKESTATIC, "net/minecraftforge/network/NetworkHooks", "openScreen",
                "(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/MenuProvider;"
                        + "Lnet/minecraft/core/BlockPos;)V", false);
        mv.visitLabel(done);
        mv.visitFieldInsn(GETSTATIC, "net/minecraft/world/InteractionResult", "CONSUME",
                "Lnet/minecraft/world/InteractionResult;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static byte[] provider() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(V17, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, PROVIDER, null,
                "java/lang/Object", new String[]{"net/minecraft/world/MenuProvider"});
        cw.visitField(ACC_PRIVATE | ACC_FINAL, "dhd", "L" + UNIVERSE_ENTITY + ";", null, null).visitEnd();
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(L" + UNIVERSE_ENTITY + ";)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, PROVIDER, "dhd", "L" + UNIVERSE_ENTITY + ";");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        mv = cw.visitMethod(ACC_PUBLIC, "m_5446_", "()Lnet/minecraft/network/chat/Component;", null, null);
        mv.visitCode();
        mv.visitLdcInsn("screen.sgjourney.dhd");
        mv.visitMethodInsn(INVOKESTATIC, "net/minecraft/network/chat/Component", "m_237115_",
                "(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;", true);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        mv = cw.visitMethod(ACC_PUBLIC, "m_7208_",
                "(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/entity/player/Player;)"
                        + "Lnet/minecraft/world/inventory/AbstractContainerMenu;", null, null);
        mv.visitCode();
        mv.visitTypeInsn(NEW, DESTINY_MENU);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, PROVIDER, "dhd", "L" + UNIVERSE_ENTITY + ";");
        mv.visitMethodInsn(INVOKESPECIAL, DESTINY_MENU, "<init>",
                "(ILnet/minecraft/world/entity/player/Inventory;L" + UNIVERSE_ENTITY + ";)V", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] destinyMenu() {
        String universeMenu = "net/povstalec/sgjourney/common/menu/UniverseDHDMenu";
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(V17, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, DESTINY_MENU, null, universeMenu, null);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>",
                "(ILnet/minecraft/world/entity/player/Inventory;L" + UNIVERSE_ENTITY + ";)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKESPECIAL, universeMenu, "<init>",
                "(ILnet/minecraft/world/entity/player/Inventory;L" + UNIVERSE_ENTITY + ";)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        mv = cw.visitMethod(ACC_PUBLIC, "m_6875_",
                "(Lnet/minecraft/world/entity/player/Player;)Z", null, null);
        mv.visitCode();
        mv.visitInsn(ICONST_1);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) throw new IllegalArgumentException("INPUT.class OUTPUT_DIRECTORY");
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        Files.write(input, patchBlock(Files.readAllBytes(input)));
        Path provider = output.resolve(PROVIDER + ".class");
        Files.createDirectories(provider.getParent());
        Files.write(provider, provider());
        Files.write(output.resolve(DESTINY_MENU + ".class"), destinyMenu());
    }
}
