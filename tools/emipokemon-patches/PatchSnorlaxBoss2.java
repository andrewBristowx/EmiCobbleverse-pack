import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;

/**
 * Snorlax Emi, parte 3 (se aplica despues de PatchSnorlaxBoss): SnorlaxExtras.init() (comando /emipokemon snorlax traer y musica de fondo).
 *  - SnorlaxRaidService.active pasa de privado a de paquete (lo lee SnorlaxExtras).
 *  - SnorlaxBossCommands.register() llama a SnorlaxExtras.init() al final.
 * Uso: PatchSnorlaxBoss2 <jar_extraido> <clases_compiladas(SnorlaxExtras)> <salida>
 */
public class PatchSnorlaxBoss2 {
    static final String PKG = "com/emipokemon/raid/snorlax/";

    static void fail(String m) { throw new IllegalStateException(m); }

    public static void main(String[] a) throws Exception {
        Path in = Paths.get(a[0]), classes = Paths.get(a[1]), out = Paths.get(a[2]);
        ClassNode raid = new ClassNode();
        new ClassReader(Files.readAllBytes(in.resolve(PKG + "SnorlaxRaidService.class"))).accept(raid, 0);
        boolean ok = false;
        for (FieldNode f : raid.fields)
            if (f.name.equals("active") && (f.access & Opcodes.ACC_PRIVATE) != 0 && (f.access & Opcodes.ACC_STATIC) != 0) { f.access &= ~Opcodes.ACC_PRIVATE; ok = true; }
        if (!ok) fail("campo active (privado) no encontrado");
        write(raid, out, PKG + "SnorlaxRaidService");

        ClassNode cmds = new ClassNode();
        new ClassReader(Files.readAllBytes(in.resolve(PKG + "SnorlaxBossCommands.class"))).accept(cmds, 0);
        MethodNode reg = null;
        for (MethodNode m : cmds.methods) if (m.name.equals("register") && m.desc.equals("()V")) reg = m;
        if (reg == null) fail("register() no encontrado");
        for (AbstractInsnNode i = reg.instructions.getFirst(); i != null; i = i.getNext())
            if (i instanceof MethodInsnNode mi && mi.owner.equals(PKG + "SnorlaxExtras")) fail("ya parcheado");
        AbstractInsnNode ret = null;
        for (AbstractInsnNode i = reg.instructions.getLast(); i != null; i = i.getPrevious()) if (i.getOpcode() == Opcodes.RETURN) { ret = i; break; }
        if (ret == null) fail("return no encontrado");
        reg.instructions.insertBefore(ret, new MethodInsnNode(Opcodes.INVOKESTATIC, PKG + "SnorlaxExtras", "init", "()V", false));
        write(cmds, out, PKG + "SnorlaxBossCommands");

        Path src = classes.resolve(PKG + "SnorlaxExtras.class");
        if (!Files.exists(src)) fail("falta " + src);
        Files.createDirectories(out.resolve(PKG));
        Files.copy(src, out.resolve(PKG + "SnorlaxExtras.class"), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("patched OK");
    }

    static void write(ClassNode cn, Path out, String name) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        Path o = out.resolve(name + ".class");
        Files.createDirectories(o.getParent());
        Files.write(o, cw.toByteArray());
    }
}
