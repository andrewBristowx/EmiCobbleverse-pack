import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;

/**
 * Eventos del mundo de Emipokemon sin tirones y con sitio fijo (pack 1.0.169). Ver event-sites/src/com/emipokemon/raid/EventSites.java.
 *  - MegaBossService.searchAround y LegendaryTrainerService.searchAround (la busqueda de lugar de los 5 eventos: Mega, Gigantamax, Tera, Shiny y entrenador legendario):
 *    al principio llaman a EventSites.prepare (sitio marcado, nadie conectado, centro en un jugador) y, dentro del bucle, se saltan los puntos cuyo trozo de mapa no esta cargado
 *    (antes pedian la altura de puntos a miles de bloques y eso generaba el mapa en el hilo principal: tirones de 2-5 s).
 *  - TeraBossService.initialize llama a EventSites.init() (carga el archivo y registra /emipokemon sitio ...).
 *  - TeraBossService.spawn: el id de especie del catalogo pasa por EventSites.fixSpecies (arregla «emipokemonfloragatoemi»).
 * Uso: PatchEventSites <jar_extraido> <salida>   (EventSites.class se compila aparte y se copia a la salida: com/emipokemon/raid/EventSites*.class)
 */
public class PatchEventSites {
    static final String ES = "com/emipokemon/raid/EventSites";
    static final String W = "net/minecraft/class_3218", POS = "net/minecraft/class_2338", HM = "net/minecraft/class_2902$class_2903";

    public static void main(String[] a) throws Exception {
        Path in = Paths.get(a[0]), out = Paths.get(a[1]);
        patchSearch(in, out, "com/emipokemon/raid/MegaBossService");
        patchSearch(in, out, "com/emipokemon/npc/LegendaryTrainerService");
        patchTera(in, out);
        System.out.println("patched OK");
    }

    static ClassNode leer(Path in, String c) throws Exception {
        ClassNode cn = new ClassNode();
        new ClassReader(Files.readAllBytes(in.resolve(c + ".class"))).accept(cn, 0);
        return cn;
    }

    static void escribir(ClassNode cn, Path out, String c) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override protected String getCommonSuperClass(String x, String y) { return "java/lang/Object"; }
        };
        cn.accept(cw);
        Path o = out.resolve(c + ".class");
        Files.createDirectories(o.getParent());
        Files.write(o, cw.toByteArray());
    }

    static void patchSearch(Path in, Path out, String cls) throws Exception {
        ClassNode cn = leer(in, cls);
        MethodNode m = null;
        for (MethodNode x : cn.methods) if (x.name.equals("searchAround") && x.desc.equals("(L" + W + ";DDDI)L" + POS + ";")) m = x;
        if (m == null) throw new IllegalStateException(cls + ": no searchAround");

        // 1) el filtro de trozos cargados dentro del bucle: justo antes de «aload_0; getstatic Heightmap; iload x; iload z; invokevirtual getTopY»
        FieldInsnNode gs = null;
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext())
            if (i instanceof FieldInsnNode f && f.getOpcode() == Opcodes.GETSTATIC && f.owner.equals(HM) && f.name.equals("field_13197")) { gs = f; break; }
        if (gs == null) throw new IllegalStateException(cls + ": no heightmap");
        AbstractInsnNode ax = gs.getNext(), az = ax.getNext();
        if (!(ax instanceof VarInsnNode vx && vx.getOpcode() == Opcodes.ILOAD) || !(az instanceof VarInsnNode vz && vz.getOpcode() == Opcodes.ILOAD)) throw new IllegalStateException(cls + ": x/z");
        AbstractInsnNode w0 = gs.getPrevious();
        if (!(w0 instanceof VarInsnNode vw && vw.getOpcode() == Opcodes.ALOAD && vw.var == 0)) throw new IllegalStateException(cls + ": aload_0");
        IincInsnNode inc = null; JumpInsnNode back = null;
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof IincInsnNode ii) inc = ii;
            if (i instanceof JumpInsnNode j && j.getOpcode() == Opcodes.GOTO) back = j;
        }
        if (inc == null || back == null) throw new IllegalStateException(cls + ": bucle");
        LabelNode ok = new LabelNode();
        InsnList f = new InsnList();
        f.add(new VarInsnNode(Opcodes.ALOAD, 0));
        f.add(new VarInsnNode(Opcodes.ILOAD, ((VarInsnNode) ax).var));
        f.add(new VarInsnNode(Opcodes.ILOAD, ((VarInsnNode) az).var));
        f.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ES, "cargado", "(L" + W + ";II)Z", false));
        f.add(new JumpInsnNode(Opcodes.IFNE, ok));
        f.add(new IincInsnNode(inc.var, 1));
        f.add(new JumpInsnNode(Opcodes.GOTO, back.label));
        f.add(ok);
        m.instructions.insertBefore(w0, f);

        // 2) el principio del metodo
        LabelNode noSitio = new LabelNode(), seguir = new LabelNode();
        InsnList p = new InsnList();
        p.add(new VarInsnNode(Opcodes.ALOAD, 0));
        p.add(new VarInsnNode(Opcodes.DLOAD, 1));
        p.add(new VarInsnNode(Opcodes.DLOAD, 3));
        p.add(new VarInsnNode(Opcodes.DLOAD, 5));
        p.add(new VarInsnNode(Opcodes.ILOAD, 7));
        p.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ES, "prepare", "(L" + W + ";DDDI)L" + POS + ";", false));
        p.add(new InsnNode(Opcodes.DUP));
        p.add(new JumpInsnNode(Opcodes.IFNULL, noSitio));
        p.add(new InsnNode(Opcodes.ARETURN));
        p.add(noSitio);
        p.add(new InsnNode(Opcodes.POP));
        p.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ES, "abortar", "()Z", false));
        p.add(new JumpInsnNode(Opcodes.IFEQ, seguir));
        p.add(new InsnNode(Opcodes.ACONST_NULL));
        p.add(new InsnNode(Opcodes.ARETURN));
        p.add(seguir);
        p.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ES, "ncx", "()D", false));
        p.add(new VarInsnNode(Opcodes.DSTORE, 1));
        p.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ES, "ncz", "()D", false));
        p.add(new VarInsnNode(Opcodes.DSTORE, 3));
        p.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ES, "nr", "()D", false));
        p.add(new VarInsnNode(Opcodes.DSTORE, 5));
        m.instructions.insert(p);
        escribir(cn, out, cls);
    }

    static void patchTera(Path in, Path out) throws Exception {
        String cls = "com/emipokemon/raid/TeraBossService";
        ClassNode cn = leer(in, cls);
        int init = 0, fix = 0;
        for (MethodNode m : cn.methods) {
            if (m.name.equals("initialize") && m.desc.startsWith("(Lcom/emipokemon/config/ConfigManager;)")) {
                m.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC, ES, "init", "()V", false));
                init++;
            }
            if (m.name.equals("spawn") && m.desc.startsWith("(Lnet/minecraft/server/MinecraftServer;")) {
                for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext())
                    if (i instanceof MethodInsnNode mi && mi.name.equals("speciesId") && mi.owner.equals("com/emipokemon/gacha/catalog/PokemonCatalogEntry")) {
                        AbstractInsnNode n = mi.getNext();
                        if (n instanceof InvokeDynamicInsnNode) { m.instructions.insert(mi, new MethodInsnNode(Opcodes.INVOKESTATIC, ES, "fixSpecies", "(Ljava/lang/String;)Ljava/lang/String;", false)); fix++; }
                    }
            }
        }
        if (init != 1 || fix != 1) throw new IllegalStateException("Tera: init=" + init + " fix=" + fix);
        escribir(cn, out, cls);
    }
}
