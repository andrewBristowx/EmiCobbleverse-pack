import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;

/**
 * Emipokemon deja de dar brujulas (pack 1.0.70). `StructureVisitService.giveCompass` (la usan `/emipokemon marcar` y
 * `locateForQuest`, o sea las misiones de localizar estructura) pasa a no hacer nada, y los mensajes que decian
 * "Te di una brujula..." / "Brujula actualizada..." ahora solo informan de la ubicacion y remiten a `/emipokemon visitar`.
 * Uso: PatchNoCompass <jar_extraido> <salida>
 */
public class PatchNoCompass {
    static final String SVC = "com/emipokemon/admin/StructureVisitService";

    public static void main(String[] a) throws Exception {
        Path in = Paths.get(a[0]), out = Paths.get(a[1]);
        ClassNode cn = new ClassNode();
        new ClassReader(Files.readAllBytes(in.resolve(SVC + ".class"))).accept(cn, 0);

        MethodNode give = null;
        for (MethodNode m : cn.methods) if (m.name.equals("giveCompass")) give = m;
        if (give == null) throw new IllegalStateException("giveCompass not found");
        // sanity: must build a compass item (class_1802.field_8251)
        boolean compass = false;
        for (AbstractInsnNode i = give.instructions.getFirst(); i != null; i = i.getNext())
            if (i instanceof FieldInsnNode f && f.name.equals("field_8251")) compass = true;
        if (!compass) throw new IllegalStateException("giveCompass does not use a compass item");
        give.instructions.insert(new InsnNode(Opcodes.RETURN));

        int changed = 0;
        for (MethodNode m : cn.methods)
            for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
                if (!(i instanceof InvokeDynamicInsnNode d) || d.bsmArgs.length == 0 || !(d.bsmArgs[0] instanceof String s)) continue;
                String n = s;
                if (s.equals("§dTe di una brújula que apunta a §f\u0001§d."))
                    n = "§dUbicación de §f\u0001§d registrada. Usa §e/emipokemon visitar§d para ir.";
                else if (s.startsWith("§dBrújula actualizada: apunta a "))
                    n = "§dUbicación de " + s.substring("§dBrújula actualizada: apunta a ".length());
                if (!n.equals(s)) { d.bsmArgs[0] = n; changed++; }
            }
        if (changed != 3) throw new IllegalStateException("expected 3 messages, patched " + changed);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        Path o = out.resolve(SVC + ".class");
        Files.createDirectories(o.getParent());
        Files.write(o, cw.toByteArray());
        System.out.println("patched OK");
    }
}
