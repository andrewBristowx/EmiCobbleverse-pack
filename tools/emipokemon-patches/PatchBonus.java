import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;

/**
 * Nuevo comando /emi twitch bonus activar (OP nivel 2): activa un bono aleatorio de servidor de 20 min, igual que la Caja Misteriosa del Directo.
 * Lo usa la funcion del datapack EmiCocina al comer el Festin del Directo.
 * Uso: PatchBonus <jar_extraido> <stub_classes> <salida>
 */
public class PatchBonus {
    static final String TC = "com/emipokemon/twitch/TwitchCommands";
    static final String SVC = "com/emipokemon/twitch/TwitchService";
    static final String LAB = "com/mojang/brigadier/builder/LiteralArgumentBuilder";
    static final String AB = "com/mojang/brigadier/builder/ArgumentBuilder";
    static final String CTX = "com/mojang/brigadier/context/CommandContext";

    static ClassNode read(Path p) throws Exception {
        ClassNode cn = new ClassNode();
        new ClassReader(Files.readAllBytes(p)).accept(cn, 0);
        return cn;
    }

    public static void main(String[] a) throws Exception {
        Path jar = Paths.get(a[0]), stub = Paths.get(a[1]), out = Paths.get(a[2]);
        ClassNode cn = read(jar.resolve(TC + ".class"));
        ClassNode stubCn = read(stub.resolve(TC + ".class"));

        MethodNode newM = null;
        for (MethodNode m : stubCn.methods) if (m.name.equals("emiActivateBonus")) newM = m;
        if (newM == null) throw new IllegalStateException("stub method missing");
        for (MethodNode m : cn.methods) if (m.name.equals("emiActivateBonus")) throw new IllegalStateException("already patched");
        cn.methods.add(newM);

        MethodNode reg = null;
        for (MethodNode m : cn.methods) if (m.name.equals("registerCommands")) reg = m;
        if (reg == null) throw new IllegalStateException("registerCommands not found");

        // templates: the hasPermission(2) predicate and a `run:(TwitchService)Command` lambda
        InvokeDynamicInsnNode perm = null, cmd = null;
        for (AbstractInsnNode i = reg.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof InvokeDynamicInsnNode d) {
                if (d.desc.equals("()Ljava/util/function/Predicate;")) perm = d;
                if (d.desc.equals("(L" + SVC + ";)Lcom/mojang/brigadier/Command;") && cmd == null) cmd = d;
            }
        }
        if (perm == null || cmd == null) throw new IllegalStateException("indy templates not found");
        Handle permImpl = (Handle) perm.bsmArgs[1];
        MethodNode lam = null;
        for (MethodNode x : cn.methods) if (x.name.equals(permImpl.getName()) && x.desc.equals(permImpl.getDesc())) lam = x;
        boolean ok = false;
        for (AbstractInsnNode i = lam.instructions.getFirst(); i != null; i = i.getNext())
            if (i instanceof MethodInsnNode mi && mi.name.equals("method_9259") && i.getPrevious().getOpcode() == Opcodes.ICONST_2) ok = true;
        if (!ok) throw new IllegalStateException("predicate is not hasPermission(2)");

        Type samType = (Type) cmd.bsmArgs[0];
        Handle mine = new Handle(Opcodes.H_INVOKESTATIC, TC, "emiActivateBonus", "(L" + SVC + ";L" + CTX + ";)I", false);
        InvokeDynamicInsnNode myCmd = new InvokeDynamicInsnNode(cmd.name, cmd.desc, cmd.bsm, new Object[]{samType, mine, cmd.bsmArgs[2]});

        AbstractInsnNode ret = null;
        for (AbstractInsnNode i = reg.instructions.getLast(); i != null; i = i.getPrevious()) if (i.getOpcode() == Opcodes.RETURN) { ret = i; break; }

        // dispatcher.register(literal("emi").then(literal("twitch").then(literal("bonus").then(literal("activar").requires(perm).executes(cmd)))))
        InsnList add = new InsnList();
        add.add(new VarInsnNode(Opcodes.ALOAD, 0));
        for (String name : new String[]{"emi", "twitch", "bonus", "activar"}) {
            add.add(new LdcInsnNode(name));
            add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/minecraft/class_2170", "method_9247", "(Ljava/lang/String;)L" + LAB + ";", false));
        }
        add.add(new InvokeDynamicInsnNode(perm.name, perm.desc, perm.bsm, perm.bsmArgs));
        add.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, LAB, "requires", "(Ljava/util/function/Predicate;)L" + AB + ";", false));
        add.add(new TypeInsnNode(Opcodes.CHECKCAST, LAB));
        add.add(new VarInsnNode(Opcodes.ALOAD, 1));
        add.add(myCmd);
        add.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, LAB, "executes", "(Lcom/mojang/brigadier/Command;)L" + AB + ";", false));
        for (int k = 0; k < 3; k++) { // activar -> bonus -> twitch -> emi
            add.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, LAB, "then", "(L" + AB + ";)L" + AB + ";", false));
            add.add(new TypeInsnNode(Opcodes.CHECKCAST, LAB));
        }
        add.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "com/mojang/brigadier/CommandDispatcher", "register", "(L" + LAB + ";)Lcom/mojang/brigadier/tree/LiteralCommandNode;", false));
        add.add(new InsnNode(Opcodes.POP));
        reg.instructions.insertBefore(ret, add);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        Path o = out.resolve(TC + ".class");
        Files.createDirectories(o.getParent());
        Files.write(o, cw.toByteArray());
        System.out.println("patched OK");
    }
}
