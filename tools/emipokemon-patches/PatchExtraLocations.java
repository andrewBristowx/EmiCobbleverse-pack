import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;

/**
 * Menu de ubicaciones con huecos para las estructuras que Emipokemon no traia (pack 1.0.74).
 * Al final del inicializador estatico de RegionalStructureAuditService llama a ExtraLocations.apply(EXPECTED), que añade
 * las ubicaciones de config/emipokemon/extra-locations.json. Uso: PatchExtraLocations <jar_extraido> <salida>
 * Ademas sube de 24 a 99 el maximo del numero en /emipokemon visitar|marcar|... <region> <numero> (EmipokemonCommands).
 * (ExtraLocations.class se compila aparte con javac --release 21 -cp <jar_extraido>:gson:fabric-loader y se copia al jar).
 */
public class PatchExtraLocations {
    static final String C = "com/emipokemon/admin/RegionalStructureAuditService";
    static final String CMD = "com/emipokemon/command/EmipokemonCommands";

    /** salta etiquetas y numeros de linea */
    static AbstractInsnNode real(AbstractInsnNode n) { while (n != null && n.getOpcode() < 0) n = n.getPrevious(); return n; }

    public static void main(String[] a) throws Exception {
        Path in = Paths.get(a[0]), out = Paths.get(a[1]);
        ClassNode cn = new ClassNode();
        new ClassReader(Files.readAllBytes(in.resolve(C + ".class"))).accept(cn, 0);
        MethodNode m = null;
        for (MethodNode x : cn.methods) if (x.name.equals("<clinit>")) m = x;
        if (m == null) throw new IllegalStateException("<clinit> not found");
        boolean already = false, hasExpected = false;
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof MethodInsnNode mi && mi.owner.equals("com/emipokemon/admin/ExtraLocations")) already = true;
            if (i instanceof FieldInsnNode f && f.name.equals("EXPECTED") && f.getOpcode() == Opcodes.GETSTATIC) hasExpected = true;
        }
        if (already) throw new IllegalStateException("already patched");
        if (!hasExpected) throw new IllegalStateException("EXPECTED not used in <clinit>: unexpected class");
        AbstractInsnNode ret = null;
        for (AbstractInsnNode i = m.instructions.getLast(); i != null; i = i.getPrevious()) if (i.getOpcode() == Opcodes.RETURN) { ret = i; break; }
        InsnList add = new InsnList();
        add.add(new FieldInsnNode(Opcodes.GETSTATIC, C, "EXPECTED", "Ljava/util/Map;"));
        add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/emipokemon/admin/ExtraLocations", "apply", "(Ljava/util/Map;)V", false));
        m.instructions.insertBefore(ret, add);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        Path o = out.resolve(C + ".class");
        Files.createDirectories(o.getParent());
        Files.write(o, cw.toByteArray());

        // /emipokemon visitar|marcar|admin structures mapmark <region> <numero>: el numero estaba limitado a 1..24 (Sinnoh tenia 24);
        // ahora 1..99 para poder llegar a las ubicaciones extra (Sinnoh pasa de 24)
        ClassNode cmd = new ClassNode();
        new ClassReader(Files.readAllBytes(in.resolve(CMD + ".class"))).accept(cmd, 0);
        int changed = 0;
        for (MethodNode x : cmd.methods)
            for (AbstractInsnNode i = x.instructions.getFirst(); i != null; i = i.getNext())
                if (i instanceof MethodInsnNode mi && mi.owner.equals("com/mojang/brigadier/arguments/IntegerArgumentType") && mi.name.equals("integer") && mi.desc.equals("(II)Lcom/mojang/brigadier/arguments/IntegerArgumentType;")
                        && real(i.getPrevious()) instanceof IntInsnNode b && b.getOpcode() == Opcodes.BIPUSH && b.operand == 24
                        && real(b.getPrevious()).getOpcode() == Opcodes.ICONST_1) { b.operand = 99; changed++; }
        if (changed != 3) throw new IllegalStateException("expected 3 integer(1,24) arguments, found " + changed);
        ClassWriter cw2 = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cmd.accept(cw2);
        Path o2 = out.resolve(CMD + ".class");
        Files.createDirectories(o2.getParent());
        Files.write(o2, cw2.toByteArray());
        System.out.println("patched OK");
    }
}
