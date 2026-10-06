import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;

/**
 * TwitchProfileStore.save() ya no escribe el archivo con el candado del almacen cogido: delega en TwitchStoreSave.save(store).
 * Uso: PatchProfileSave <jar_extraido> <clases_compiladas(TwitchStoreSave)> <salida>
 */
public class PatchProfileSave {
    static final String STORE = "com/emipokemon/twitch/TwitchProfileStore";
    static final String HELPER = "com/emipokemon/twitch/TwitchStoreSave";

    public static void main(String[] a) throws Exception {
        Path in = Paths.get(a[0]), classes = Paths.get(a[1]), out = Paths.get(a[2]);
        ClassNode cn = new ClassNode();
        new ClassReader(Files.readAllBytes(in.resolve(STORE + ".class"))).accept(cn, 0);
        int fields = 0;
        for (FieldNode f : cn.fields)
            if ((f.name.equals("profiles") || f.name.equals("file")) && (f.access & Opcodes.ACC_PRIVATE) != 0) { f.access &= ~Opcodes.ACC_PRIVATE; fields++; }
        if (fields != 2) throw new IllegalStateException("campos profiles/file no encontrados: " + fields);
        MethodNode save = null;
        for (MethodNode m : cn.methods) if (m.name.equals("save") && m.desc.equals("()V")) save = m;
        if (save == null || (save.access & Opcodes.ACC_SYNCHRONIZED) == 0) throw new IllegalStateException("save() synchronized no encontrado");
        save.access &= ~Opcodes.ACC_SYNCHRONIZED;
        save.instructions = new InsnList();
        save.tryCatchBlocks.clear();
        if (save.localVariables != null) save.localVariables.clear();
        save.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        save.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, "save", "(L" + STORE + ";)V", false));
        save.instructions.add(new InsnNode(Opcodes.RETURN));
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        Path o = out.resolve(STORE + ".class");
        Files.createDirectories(o.getParent());
        Files.write(o, cw.toByteArray());
        for (String c : new String[]{HELPER}) {
            Path src = classes.resolve(c + ".class");
            if (!Files.exists(src)) throw new IllegalStateException("falta " + src);
            Files.copy(src, out.resolve(c + ".class"), StandardCopyOption.REPLACE_EXISTING);
        }
        // las clases internas del helper (TypeToken anonimo)
        try (var ds = Files.newDirectoryStream(classes.resolve("com/emipokemon/twitch"), "TwitchStoreSave$*.class")) {
            for (Path p : ds) Files.copy(p, out.resolve("com/emipokemon/twitch").resolve(p.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
        }
        System.out.println("patched OK (save() ya no mantiene el candado durante la escritura)");
    }
}
