import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public final class PatchAe2TerminalLight {
    private static final String TARGET = "appeng/parts/reporting/AbstractPartReporting.class";
    private static final String OWNER = "appeng/parts/reporting/AbstractPartReporting";

    private PatchAe2TerminalLight() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: PatchAe2TerminalLight <input.jar> <output.jar>");
        }

        Path input = Paths.get(args[0]);
        Path output = Paths.get(args[1]);
        Files.createDirectories(output.getParent());

        boolean patched = false;
        try (JarFile jar = new JarFile(input.toFile());
             JarOutputStream out = new JarOutputStream(Files.newOutputStream(output))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                JarEntry copy = new JarEntry(entry.getName());
                copy.setTime(entry.getTime());
                out.putNextEntry(copy);

                try (InputStream in = jar.getInputStream(entry)) {
                    byte[] bytes = readAll(in);
                    if (TARGET.equals(entry.getName())) {
                        bytes = patchClass(bytes);
                        patched = true;
                    }
                    out.write(bytes);
                }
                out.closeEntry();
            }
        }

        if (!patched) {
            throw new IllegalStateException("Did not find " + TARGET + " in " + input);
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static byte[] patchClass(byte[] original) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if ("getLightLevel".equals(name) && "()I".equals(descriptor)) {
                    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                    writeGetLightLevel(mv);
                    return null;
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        }, 0);
        return writer.toByteArray();
    }

    private static void writeGetLightLevel(MethodVisitor mv) {
        Label off = new Label();
        Label done = new Label();

        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, OWNER, "isPowered", "()Z", false);
        mv.visitJumpInsn(Opcodes.IFEQ, off);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, OWNER, "isLightSource", "()Z", false);
        mv.visitJumpInsn(Opcodes.IFEQ, off);
        mv.visitIntInsn(Opcodes.BIPUSH, 15);
        mv.visitJumpInsn(Opcodes.GOTO, done);
        mv.visitLabel(off);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitLabel(done);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, OWNER, "blockLight", "(I)I", false);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
    }
}
