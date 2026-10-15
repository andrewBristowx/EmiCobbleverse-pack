import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;

/**
 * Reparto de los premios de Twitch vencidos (pack 1.0.139): {@code TwitchSupportRewardService.deliverOnePending()} empieza llamando a
 * {@code CommunityRewards.tick(this)}. Hace falta meter en el jar la clase nueva {@code com/emipokemon/twitch/CommunityRewards.class} (ver community-rewards/).
 * Uso: PatchCommunityRewards <jar_extraido> <salida>   (escribe TwitchSupportRewardService.class parcheada en <salida>/com/emipokemon/twitch/)
 */
public class PatchCommunityRewards {
    static final String SVC = "com/emipokemon/twitch/TwitchSupportRewardService", HELPER = "com/emipokemon/twitch/CommunityRewards";

    public static void main(String[] a) throws Exception {
        Path in = Paths.get(a[0]), out = Paths.get(a[1]);
        ClassNode cn = new ClassNode();
        new ClassReader(Files.readAllBytes(in.resolve(SVC + ".class"))).accept(cn, 0);
        MethodNode m = null;
        for (MethodNode x : cn.methods) if (x.name.equals("deliverOnePending") && x.desc.equals("()V")) m = x;
        if (m == null) throw new IllegalStateException("deliverOnePending no encontrado");
        boolean yaParcheado = false;
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) if (i instanceof MethodInsnNode mi && mi.owner.equals(HELPER)) yaParcheado = true;
        if (yaParcheado) throw new IllegalStateException("ya estaba parcheado");
        InsnList pre = new InsnList();
        pre.add(new VarInsnNode(Opcodes.ALOAD, 0));
        pre.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, "tick", "(Ljava/lang/Object;)V", false));
        m.instructions.insert(pre);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        Path o = out.resolve(SVC + ".class");
        Files.createDirectories(o.getParent());
        Files.write(o, cw.toByteArray());
        System.out.println("patched OK");
    }
}
