import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;

public class PatchTreasure {
    static final String TH = "com/emipokemon/events/treasure/TreasureHuntService";
    static final String NB = "com/emipokemon/npc/NpcBattleService";
    static final String PLAYER = "net/minecraft/class_3222";
    static final String NPC = "com/emipokemon/npc/ServiceNpcEntity";
    static final String SIG2 = "(L" + PLAYER + ";L" + NPC + ";)V";
    static final String SIG3 = "(L" + PLAYER + ";L" + NPC + ";Z)V";

    static ClassNode read(Path p) throws Exception {
        ClassNode cn = new ClassNode();
        new ClassReader(Files.readAllBytes(p)).accept(cn, 0);
        return cn;
    }
    static void write(ClassNode cn, Path p) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        Files.createDirectories(p.getParent());
        Files.write(p, cw.toByteArray());
    }
    public static void main(String[] a) throws Exception {
        Path in = Paths.get(a[0]), out = Paths.get(a[1]);

        // TreasureHuntService.emiOnBattleResult(player, npc, won): cleanup, then break the seal if the player won
        ClassNode th = read(in.resolve(TH + ".class"));
        for (MethodNode m : th.methods) if (m.name.equals("onTrainerVictory") && !m.desc.equals(SIG2)) throw new IllegalStateException("unexpected onTrainerVictory " + m.desc);
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "emiOnBattleResult", SIG3, null, null);
        LabelNode end = new LabelNode();
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, TH, "cleanupTrainerPokemon", SIG2, false));
        m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
        m.instructions.add(new JumpInsnNode(Opcodes.IFEQ, end));
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, TH, "onTrainerVictory", SIG2, false));
        m.instructions.add(end);
        m.instructions.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        m.instructions.add(new InsnNode(Opcodes.RETURN));
        th.methods.add(m);
        write(th, out.resolve(TH + ".class"));

        // NpcBattleService: the treasure branch of the battle-end lambda now also reports the result
        ClassNode nb = read(in.resolve(NB + ".class"));
        MethodNode lam = null;
        for (MethodNode x : nb.methods) if (x.name.startsWith("lambda$handleBattleEnded$") && x.desc.endsWith("ActiveBattle;Z)V")) { if (lam != null) throw new IllegalStateException("multiple lambdas"); lam = x; }
        if (lam == null) throw new IllegalStateException("lambda not found");
        int patched = 0;
        for (AbstractInsnNode i = lam.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof MethodInsnNode mi && mi.getOpcode() == Opcodes.INVOKESTATIC && mi.owner.equals(TH) && mi.name.equals("cleanupTrainerPokemon")) {
                lam.instructions.insertBefore(mi, new VarInsnNode(Opcodes.ILOAD, 3)); // `won` (lambda param #4: server, playerId, active, won)
                mi.name = "emiOnBattleResult";
                mi.desc = SIG3;
                patched++;
            }
        }
        if (patched != 1) throw new IllegalStateException("expected 1 cleanup call, got " + patched);
        write(nb, out.resolve(NB + ".class"));
        System.out.println("patched OK");
    }
}
