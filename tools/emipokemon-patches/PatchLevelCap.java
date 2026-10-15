import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;

/**
 * Lideres de las regiones propias (Teselia, Kalos, Galar, Paldea, Alola) sin el limite de nivel de RCT (pack 1.0.138).
 * (1) {@code NpcBattleService.start}: la comprobacion «profile != null && !levelCap(player).allowed()» se salta si {@code LevelCapBypass.skip(profile)}.
 * (2) {@code DynamicTrainerLevelService.scale(player, profile)}: para esas regiones devuelve {@code LevelCapBypass.scale(...)} (escalado sin tope).
 * Hace falta ademas meter en el jar la clase nueva {@code com/emipokemon/challenge/LevelCapBypass.class} (ver level-cap/).
 * Uso: PatchLevelCap <jar_extraido> <salida>   (escribe los dos .class parcheados en <salida>/com/emipokemon/...)
 */
public class PatchLevelCap {
    static final String NBS = "com/emipokemon/npc/NpcBattleService", DTL = "com/emipokemon/challenge/DynamicTrainerLevelService";
    static final String BYPASS = "com/emipokemon/challenge/LevelCapBypass", PROFILE = "com/emipokemon/challenge/ChallengeProfile";
    static final String SCALING = "com/emipokemon/challenge/DynamicTrainerLevelService$ScalingResult";

    public static void main(String[] a) throws Exception {
        Path in = Paths.get(a[0]), out = Paths.get(a[1]);

        // ---- (1) NpcBattleService.start
        ClassNode cn = read(in, NBS);
        MethodNode start = null;
        for (MethodNode m : cn.methods) if (m.name.equals("start") && m.desc.equals("(Lnet/minecraft/class_3222;L" + "com/emipokemon/npc/ServiceNpcEntity;)Z")) start = m;
        if (start == null) throw new IllegalStateException("NpcBattleService.start no encontrado");
        int hits = 0;
        for (AbstractInsnNode i = start.instructions.getFirst(); i != null; i = i.getNext()) {
            if (!(i instanceof MethodInsnNode mi) || !mi.name.equals("levelCap") || !mi.owner.equals("com/emipokemon/challenge/RctCobbleverseBridge")) continue;
            // esperado: ... ALOAD 2 ; IFNULL L ; ALOAD 0 ; INVOKESTATIC levelCap
            AbstractInsnNode load0 = prevReal(i), jump = prevReal(load0);
            if (!(load0 instanceof VarInsnNode v0 && v0.var == 0 && v0.getOpcode() == Opcodes.ALOAD) || !(jump instanceof JumpInsnNode j && j.getOpcode() == Opcodes.IFNULL))
                throw new IllegalStateException("el codigo alrededor de levelCap no es el esperado");
            AbstractInsnNode load2 = prevReal(jump);
            if (!(load2 instanceof VarInsnNode v2 && v2.var == 2 && v2.getOpcode() == Opcodes.ALOAD)) throw new IllegalStateException("profile no esta en la variable 2");
            InsnList add = new InsnList();
            add.add(new VarInsnNode(Opcodes.ALOAD, 2));
            add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BYPASS, "skip", "(L" + PROFILE + ";)Z", false));
            add.add(new JumpInsnNode(Opcodes.IFNE, j.label));
            start.instructions.insert(jump, add);
            hits++;
            break;
        }
        if (hits != 1) throw new IllegalStateException("esperaba 1 llamada a levelCap en start, hay " + hits);
        write(cn, out, NBS);

        // ---- (2) DynamicTrainerLevelService.scale
        cn = read(in, DTL);
        MethodNode scale = null;
        for (MethodNode m : cn.methods) if (m.name.equals("scale") && m.desc.equals("(Lnet/minecraft/class_3222;L" + PROFILE + ";)L" + SCALING + ";")) scale = m;
        if (scale == null) throw new IllegalStateException("DynamicTrainerLevelService.scale no encontrado");
        LabelNode seguir = new LabelNode();
        InsnList pre = new InsnList();
        pre.add(new VarInsnNode(Opcodes.ALOAD, 1));
        pre.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BYPASS, "skip", "(L" + PROFILE + ";)Z", false));
        pre.add(new JumpInsnNode(Opcodes.IFEQ, seguir));
        pre.add(new VarInsnNode(Opcodes.ALOAD, 0));
        pre.add(new VarInsnNode(Opcodes.ALOAD, 1));
        pre.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BYPASS, "scale", "(Lnet/minecraft/class_3222;L" + PROFILE + ";)L" + SCALING + ";", false));
        pre.add(new InsnNode(Opcodes.ARETURN));
        pre.add(seguir);
        pre.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        scale.instructions.insert(pre);
        write(cn, out, DTL);
        System.out.println("patched OK");
    }

    static AbstractInsnNode prevReal(AbstractInsnNode n) {
        AbstractInsnNode p = n.getPrevious();
        while (p != null && (p instanceof LabelNode || p instanceof LineNumberNode || p instanceof FrameNode)) p = p.getPrevious();
        return p;
    }

    static ClassNode read(Path in, String name) throws Exception {
        ClassNode cn = new ClassNode();
        new ClassReader(Files.readAllBytes(in.resolve(name + ".class"))).accept(cn, 0);
        return cn;
    }

    static void write(ClassNode cn, Path out, String name) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        Path o = out.resolve(name + ".class");
        Files.createDirectories(o.getParent());
        Files.write(o, cw.toByteArray());
    }
}
