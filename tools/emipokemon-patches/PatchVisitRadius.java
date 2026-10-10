import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;

/**
 * El radio libre de /emipokemon visitar pasa de 150 a 300 bloques (pack 1.0.81). Quien visita una estructura queda "atado" a ella: si se aleja mas
 * de 150 bloques de la entrada, Emipokemon lo devuelve a la llegada. Las mazmorras del mundo emi_estructuras:dungeons llegan a medir ~200 bloques
 * (fortalezas, palacios, la Foundry...), asi que con 150 el final de la mazmorra quedaba fuera. Las celdas estan a 500 bloques una de otra, no se pisan.
 * Cambia el cuadrado del radio (22500.0 = 150^2) por 90000.0 (300^2) en `StructureVisitService$VisitSession.withinRadius`.
 * Tambien cambia el aviso de llegada ("radio de 150 bloques" -> 300) de `StructureVisitService`.
 * Uso: PatchVisitRadius <jar_extraido> <salida>
 */
public class PatchVisitRadius {
    static final String SVC = "com/emipokemon/admin/StructureVisitService";
    static final String CLS = "com/emipokemon/admin/StructureVisitService$VisitSession";

    public static void main(String[] a) throws Exception {
        Path in = Paths.get(a[0]), out = Paths.get(a[1]);
        ClassNode cn = new ClassNode();
        new ClassReader(Files.readAllBytes(in.resolve(CLS + ".class"))).accept(cn, 0);
        int changed = 0;
        for (MethodNode m : cn.methods) {
            if (!m.name.equals("withinRadius")) continue;
            for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext())
                if (i instanceof LdcInsnNode l && l.cst instanceof Double d && d == 22500.0) { l.cst = 90000.0; changed++; }
        }
        if (changed != 1) throw new IllegalStateException("expected 1 radius constant, found " + changed);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        Path o = out.resolve(CLS + ".class");
        Files.createDirectories(o.getParent());
        Files.write(o, cw.toByteArray());

        // el aviso de llegada dice "radio de 150 bloques": tambien se cambia
        ClassNode sn = new ClassNode();
        new ClassReader(Files.readAllBytes(in.resolve(SVC + ".class"))).accept(sn, 0);
        int msgs = 0;
        for (MethodNode m : sn.methods)
            for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext())
                if (i instanceof InvokeDynamicInsnNode d && d.bsmArgs.length > 0 && d.bsmArgs[0] instanceof String t && t.contains("radio de 150 bloques")) {
                    d.bsmArgs[0] = t.replace("radio de 150 bloques", "radio de 300 bloques"); msgs++;
                }
        if (msgs != 1) throw new IllegalStateException("expected 1 arrival message, found " + msgs);
        ClassWriter sw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        sn.accept(sw);
        Files.write(out.resolve(SVC + ".class"), sw.toByteArray());
        System.out.println("patched OK");
    }
}
