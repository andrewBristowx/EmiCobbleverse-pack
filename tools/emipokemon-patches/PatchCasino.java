import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;

public class PatchCasino {
    static final String TS = "com/emipokemon/casino/CasinoTableService";
    static final String NET = "com/emipokemon/casino/CasinoNetworking";
    static final String SESSION = TS + "$Session";
    static final String ENTITY = "com/emipokemon/casino/CasinoMachineBlockEntity";
    static final String PLAYER = "net/minecraft/class_3222";

    static ClassNode read(Path p) throws Exception {
        ClassNode cn = new ClassNode();
        new ClassReader(Files.readAllBytes(p)).accept(cn, 0);
        return cn;
    }
    static MethodNode method(ClassNode cn, String name) {
        MethodNode found = null;
        for (MethodNode m : cn.methods) if (m.name.equals(name)) { if (found != null) throw new IllegalStateException("dup " + name); found = m; }
        if (found == null) throw new IllegalStateException("missing " + name);
        return found;
    }
    static void write(ClassNode cn, Path p) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        Files.createDirectories(p.getParent());
        Files.write(p, cw.toByteArray());
    }

    public static void main(String[] a) throws Exception {
        Path in = Paths.get(a[0]), stub = Paths.get(a[1]), out = Paths.get(a[2]);
        ClassNode ts = read(in.resolve(TS + ".class"));
        ClassNode stubTs = read(stub.resolve(TS + ".class"));

        // 1) new helper methods copied from the javac-compiled stub (frames included)
        for (String n : new String[]{"emiClaimView", "emiReleaseView"}) {
            MethodNode m = method(stubTs, n);
            ts.methods.add(m);
        }

        // 2) open()/action(): after `Session s = this.session(player, machine)` drop the player from every other table's viewers
        for (String n : new String[]{"open", "action"}) {
            MethodNode m = method(ts, n);
            boolean done = false;
            for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
                if (i instanceof MethodInsnNode mi && mi.owner.equals(TS) && mi.name.equals("session")) {
                    AbstractInsnNode st = mi.getNext();
                    if (!(st instanceof VarInsnNode vs) || vs.getOpcode() != Opcodes.ASTORE) throw new IllegalStateException("no astore after session() in " + n);
                    InsnList add = new InsnList();
                    add.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    add.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    add.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, PLAYER, "method_5667", "()Ljava/util/UUID;", false));
                    add.add(new VarInsnNode(Opcodes.ALOAD, vs.var));
                    add.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, TS, "emiClaimView", "(Ljava/util/UUID;L" + SESSION + ";)V", false));
                    m.instructions.insert(st, add);
                    done = true;
                    break;
                }
            }
            if (!done) throw new IllegalStateException("session() call not found in " + n);
        }

        // 3) broadcast(): only push the table screen to people who are currently viewing it (not to every participant)
        {
            MethodNode m = method(ts, "broadcast");
            boolean done = false;
            for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
                if (i instanceof MethodInsnNode mi && mi.owner.equals("java/util/LinkedHashSet") && mi.name.equals("addAll")) {
                    AbstractInsnNode pop = mi.getNext();
                    if (pop.getOpcode() != Opcodes.POP) throw new IllegalStateException("expected POP after addAll");
                    // preceding: aload set, aload session, getfield participants, invokevirtual keySet
                    AbstractInsnNode keySet = mi.getPrevious(), getfield = keySet.getPrevious(), aloadS = getfield.getPrevious(), aloadSet = aloadS.getPrevious();
                    if (!(keySet instanceof MethodInsnNode k && k.name.equals("keySet")) || !(getfield instanceof FieldInsnNode f && f.name.equals("participants")))
                        throw new IllegalStateException("unexpected broadcast shape");
                    for (AbstractInsnNode r : new AbstractInsnNode[]{aloadSet, aloadS, getfield, keySet, mi, pop}) m.instructions.remove(r);
                    done = true;
                    break;
                }
            }
            if (!done) throw new IllegalStateException("addAll not found in broadcast");
        }
        write(ts, out.resolve(TS + ".class"));

        // 4) CasinoNetworking: opening/using a non-table machine (slot, claw, flip...) also leaves any table the player was watching
        ClassNode net = read(in.resolve(NET + ".class"));
        {
            MethodNode m = method(net, "open");
            InsnList add = new InsnList();
            add.add(new FieldInsnNode(Opcodes.GETSTATIC, NET, "TABLES", "L" + TS + ";"));
            add.add(new VarInsnNode(Opcodes.ALOAD, 0));
            add.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, PLAYER, "method_5667", "()Ljava/util/UUID;", false));
            add.add(new VarInsnNode(Opcodes.ALOAD, 1));
            add.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, TS, "emiReleaseView", "(Ljava/util/UUID;L" + ENTITY + ";)V", false));
            m.instructions.insert(add);
        }
        {
            MethodNode m = method(net, "action");
            boolean done = false;
            for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
                if (i instanceof TypeInsnNode t && t.getOpcode() == Opcodes.CHECKCAST && t.desc.equals(ENTITY) && t.getNext() instanceof VarInsnNode vs && vs.getOpcode() == Opcodes.ASTORE) {
                    InsnList add = new InsnList();
                    add.add(new FieldInsnNode(Opcodes.GETSTATIC, NET, "TABLES", "L" + TS + ";"));
                    add.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    add.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, PLAYER, "method_5667", "()Ljava/util/UUID;", false));
                    add.add(new VarInsnNode(Opcodes.ALOAD, vs.var));
                    add.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, TS, "emiReleaseView", "(Ljava/util/UUID;L" + ENTITY + ";)V", false));
                    m.instructions.insert(vs, add);
                    done = true;
                    break;
                }
            }
            if (!done) throw new IllegalStateException("machine cast not found in action");
        }
        write(net, out.resolve(NET + ".class"));
        System.out.println("patched OK");
    }
}
