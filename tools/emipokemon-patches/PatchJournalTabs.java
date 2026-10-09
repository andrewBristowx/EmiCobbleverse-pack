import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;

/**
 * Pestañas de region del Diario (Ubicaciones) con mas de 4 regiones (pack 1.0.74, junto a PatchExtraLocations).
 * QuestJournalScreen.initLocationButtons repartia el ancho entre 4 pestañas fijas (disponible / 4); con Galar, Paldea y Alola
 * las pestañas se salian del panel y tapaban el boton Actualizar. Ahora reparte entre max(4, numero de regiones), el minimo de ancho baja
 * de 70 a 34 y, con mas de 4 regiones, la pestaña lleva solo el nombre (TabLabel.make). TabLabel.class se compila aparte y se copia al jar.
 * Es una clase de cliente: los jugadores necesitan el jar parcheado. Uso: PatchJournalTabs <jar_extraido> <salida>
 */
public class PatchJournalTabs {
    static final String C = "com/emipokemon/client/progress/QuestJournalScreen";

    public static void main(String[] a) throws Exception {
        Path in = Paths.get(a[0]), out = Paths.get(a[1]);
        ClassNode cn = new ClassNode();
        new ClassReader(Files.readAllBytes(in.resolve(C + ".class"))).accept(cn, 0);
        MethodNode m = null;
        for (MethodNode x : cn.methods) if (x.name.equals("initLocationButtons")) m = x;
        if (m == null) throw new IllegalStateException("initLocationButtons not found");
        // iload 5 ; iconst_4 ; idiv   (el primer idiv del metodo)
        AbstractInsnNode idiv = null;
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) if (i.getOpcode() == Opcodes.IDIV) { idiv = i; break; }
        if (idiv == null || idiv.getPrevious().getOpcode() != Opcodes.ICONST_4) throw new IllegalStateException("unexpected bytecode before idiv");
        AbstractInsnNode four = idiv.getPrevious();
        InsnList add = new InsnList();
        add.add(new VarInsnNode(Opcodes.ALOAD, 0));
        add.add(new FieldInsnNode(Opcodes.GETFIELD, C, "snapshot", "Lcom/emipokemon/progress/JournalSnapshot;"));
        add.add(new FieldInsnNode(Opcodes.GETFIELD, "com/emipokemon/progress/JournalSnapshot", "locationRegions", "Ljava/util/List;"));
        add.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "size", "()I", true));
        add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Math", "max", "(II)I", false));
        m.instructions.insert(four, add);   // iconst_4 ; aload0 ; ... ; Math.max -> max(4, size)
        // ancho = (disponible + 4*hueco) / n - hueco  (con n = 4 es el original: disponible / 4); asi caben n pestañas con sus huecos y el boton Actualizar
        InsnList pre = new InsnList();
        pre.add(new VarInsnNode(Opcodes.ILOAD, 3));
        pre.add(new InsnNode(Opcodes.ICONST_4));
        pre.add(new InsnNode(Opcodes.IMUL));
        pre.add(new InsnNode(Opcodes.IADD));
        m.instructions.insertBefore(four, pre);
        InsnList post = new InsnList();
        post.add(new VarInsnNode(Opcodes.ILOAD, 3));
        post.add(new InsnNode(Opcodes.ISUB));
        m.instructions.insert(idiv, post);
        // el minimo de ancho de pestaña (70) impedia que cupieran mas de 4: pasa a 34 (con 4 regiones sigue mandando disponible / 4)
        boolean min = false, label = false;
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            if (!min && i instanceof IntInsnNode ii && ii.getOpcode() == Opcodes.BIPUSH && ii.operand == 70) { ii.operand = 34; min = true; }
            if (!label && i instanceof InvokeDynamicInsnNode d && d.name.equals("makeConcatWithConstants") && d.desc.equals("(Ljava/lang/String;II)Ljava/lang/String;")) {
                InsnList rep = new InsnList();   // (nombre, localizadas, total) -> TabLabel.make(nombre, localizadas, total, numero de regiones)
                rep.add(new VarInsnNode(Opcodes.ALOAD, 0));
                rep.add(new FieldInsnNode(Opcodes.GETFIELD, C, "snapshot", "Lcom/emipokemon/progress/JournalSnapshot;"));
                rep.add(new FieldInsnNode(Opcodes.GETFIELD, "com/emipokemon/progress/JournalSnapshot", "locationRegions", "Ljava/util/List;"));
                rep.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "size", "()I", true));
                rep.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/emipokemon/client/progress/TabLabel", "make", "(Ljava/lang/String;III)Ljava/lang/String;", false));
                m.instructions.insertBefore(d, rep);
                m.instructions.remove(d);
                label = true;
                break;
            }
        }
        if (!min || !label) throw new IllegalStateException("min=" + min + " label=" + label);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        Path o = out.resolve(C + ".class");
        Files.createDirectories(o.getParent());
        Files.write(o, cw.toByteArray());
        System.out.println("patched OK");
    }
}
