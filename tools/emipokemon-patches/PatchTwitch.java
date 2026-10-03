import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;

/** /twitch eventos ... : los avisos del mod lo anuncian asi, pero solo existia como /emi twitch eventos ... */
public class PatchTwitch {
    static final String TC = "com/emipokemon/twitch/TwitchCommands";
    static final String SVC = "com/emipokemon/twitch/TwitchService";
    static final String LAB = "com/mojang/brigadier/builder/LiteralArgumentBuilder";
    static final String AB = "com/mojang/brigadier/builder/ArgumentBuilder";

    public static void main(String[] a) throws Exception {
        Path in = Paths.get(a[0]), out = Paths.get(a[1]);
        ClassNode cn = new ClassNode();
        new ClassReader(Files.readAllBytes(in.resolve(TC + ".class"))).accept(cn, 0);
        MethodNode m = null;
        for (MethodNode x : cn.methods) if (x.name.equals("registerCommands")) m = x;
        if (m == null) throw new IllegalStateException("registerCommands not found");

        // the `hasPermission(2)` predicate used by `/emi twitch ... requires(...)` (last Predicate lambda in the method)
        InvokeDynamicInsnNode perm = null;
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext())
            if (i instanceof InvokeDynamicInsnNode d && d.desc.equals("()Ljava/util/function/Predicate;")) perm = d;
        if (perm == null) throw new IllegalStateException("permission predicate not found");
        // sanity: that lambda must be hasPermission(2)
        Handle impl = (Handle) perm.bsmArgs[1];
        MethodNode lam = null;
        for (MethodNode x : cn.methods) if (x.name.equals(impl.getName()) && x.desc.equals(impl.getDesc())) lam = x;
        boolean ok = false;
        for (AbstractInsnNode i = lam.instructions.getFirst(); i != null; i = i.getNext())
            if (i instanceof MethodInsnNode mi && mi.name.equals("method_9259") && i.getPrevious().getOpcode() == Opcodes.ICONST_2) ok = true;
        if (!ok) throw new IllegalStateException("predicate is not hasPermission(2)");

        AbstractInsnNode ret = null;
        for (AbstractInsnNode i = m.instructions.getLast(); i != null; i = i.getPrevious()) if (i.getOpcode() == Opcodes.RETURN) { ret = i; break; }
        if (ret == null) throw new IllegalStateException("return not found");

        // dispatcher.register(literal("twitch").then(supportEvents(service).requires(hasPermission(2))));
        InsnList add = new InsnList();
        add.add(new VarInsnNode(Opcodes.ALOAD, 0));
        add.add(new LdcInsnNode("twitch"));
        add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/minecraft/class_2170", "method_9247", "(Ljava/lang/String;)L" + LAB + ";", false));
        add.add(new VarInsnNode(Opcodes.ALOAD, 1));
        add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, TC, "supportEvents", "(L" + SVC + ";)L" + LAB + ";", false));
        add.add(new InvokeDynamicInsnNode(perm.name, perm.desc, perm.bsm, perm.bsmArgs));
        add.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, LAB, "requires", "(Ljava/util/function/Predicate;)L" + AB + ";", false));
        add.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, LAB, "then", "(L" + AB + ";)L" + AB + ";", false));
        add.add(new TypeInsnNode(Opcodes.CHECKCAST, LAB));
        add.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "com/mojang/brigadier/CommandDispatcher", "register", "(L" + LAB + ";)Lcom/mojang/brigadier/tree/LiteralCommandNode;", false));
        add.add(new InsnNode(Opcodes.POP));
        m.instructions.insertBefore(ret, add);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        Path o = out.resolve(TC + ".class");
        Files.createDirectories(o.getParent());
        Files.write(o, cw.toByteArray());
        System.out.println("patched OK");
    }
}
