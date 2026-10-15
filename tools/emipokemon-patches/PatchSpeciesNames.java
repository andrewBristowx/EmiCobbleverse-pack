import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.regex.*;

/**
 * Lideres que no se podian retar por «Equipo invalido: Pokemon no valido en el equipo: mr_mime level=N» (pack 1.0.141).
 * El catalogo de Emipokemon escribe {@code mr_mime} (Sabrina de Kanto, Lucian de Sinnoh y Valerie de Kalos) y {@code great_tusk} (el lider personalizado «Somita»),
 * pero en Cobblemon las especies son {@code mrmime} y {@code greattusk}: {@code PokemonSpecies.getByName("mr_mime")} da null y {@code validateTeam} rechaza el equipo.
 * (1) {@code ChallengeCatalog} y {@code CustomRegionTrainerCatalog}: las cadenas de equipo que EMPIEZAN por uno de esos nombres se corrigen (RENAMES).
 * (2) {@code NpcBattleService.validateTeam}: tras limpiar cada linea del equipo se llama a {@code SpeciesFix.fix(linea)} (clase nueva, ver species-names/),
 *     que corrige por si sola cualquier especie con guion bajo/guion/punto/apostrofe que exista sin ellos (equipos de NPC editados a mano, por ejemplo).
 * Uso: PatchSpeciesNames <jar_extraido> <salida> [solo-validar]   (escribe los .class parcheados en <salida>/com/emipokemon/...;
 * «solo-validar» omite el punto (1) y sirve para probar solo la red de seguridad). Meter tambien SpeciesFix.class en el jar.
 */
public class PatchSpeciesNames {
    static final String NBS = "com/emipokemon/npc/NpcBattleService", FIX = "com/emipokemon/npc/SpeciesFix";
    static final String[] CATALOGOS = {"com/emipokemon/challenge/ChallengeCatalog", "com/emipokemon/npc/CustomRegionTrainerCatalog"};
    static final String[][] RENAMES = {{"mr_mime", "mrmime"}, {"great_tusk", "greattusk"}};

    public static void main(String[] a) throws Exception {
        Path in = Paths.get(a[0]), out = Paths.get(a[1]);
        boolean soloValidar = a.length > 2 && a[2].equals("solo-validar");

        // ---- (2) NpcBattleService.validateTeam: spec = SpeciesFix.fix(spec) justo despues de «raw == null ? "" : raw.strip()»
        ClassNode cn = read(in, NBS);
        MethodNode vt = null;
        for (MethodNode m : cn.methods) if (m.name.equals("validateTeam") && m.desc.equals("(Ljava/util/List;)Ljava/util/List;")) vt = m;
        if (vt == null) throw new IllegalStateException("NpcBattleService.validateTeam no encontrado");
        int hits = 0;
        for (AbstractInsnNode i = vt.instructions.getFirst(); i != null; i = i.getNext()) {
            if (!(i instanceof MethodInsnNode mi) || !mi.owner.equals("java/lang/String") || !mi.name.equals("strip")) continue;
            AbstractInsnNode st = nextReal(i);
            if (!(st instanceof VarInsnNode v) || v.getOpcode() != Opcodes.ASTORE) throw new IllegalStateException("tras strip() no hay ASTORE: el codigo no es el esperado");
            InsnList add = new InsnList();
            add.add(new VarInsnNode(Opcodes.ALOAD, v.var));
            add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, FIX, "fix", "(Ljava/lang/String;)Ljava/lang/String;", false));
            add.add(new VarInsnNode(Opcodes.ASTORE, v.var));
            vt.instructions.insert(st, add);
            hits++;
            break;
        }
        if (hits != 1) throw new IllegalStateException("esperaba 1 strip() en validateTeam, hay " + hits);
        write(cn, out, NBS);
        System.out.println("validateTeam: SpeciesFix.fix insertado");
        if (soloValidar) return;

        // ---- (1) nombres de especie en los catalogos
        Pattern cabeza = Pattern.compile("^(" + RENAMES[0][0] + "|" + RENAMES[1][0] + ")(?=\\s|$)");
        for (String cat : CATALOGOS) {
            ClassNode c = read(in, cat);
            int n = 0;
            for (MethodNode m : c.methods)
                for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
                    if (!(i instanceof LdcInsnNode l) || !(l.cst instanceof String s)) continue;
                    Matcher mt = cabeza.matcher(s);
                    if (!mt.find()) continue;
                    String nuevo = mt.group(1).equals(RENAMES[0][0]) ? RENAMES[0][1] : RENAMES[1][1];
                    l.cst = nuevo + s.substring(mt.end());
                    n++;
                }
            if (n == 0) throw new IllegalStateException("no encontre ningun nombre que corregir en " + cat);
            write(c, out, cat);
            System.out.println(cat + ": " + n + " cadena(s) de equipo corregida(s)");
        }
    }

    static AbstractInsnNode nextReal(AbstractInsnNode i) {
        AbstractInsnNode n = i.getNext();
        while (n != null && (n instanceof LabelNode || n instanceof LineNumberNode || n instanceof FrameNode)) n = n.getNext();
        return n;
    }

    static ClassNode read(Path in, String name) throws Exception {
        ClassNode cn = new ClassNode();
        new ClassReader(Files.readAllBytes(in.resolve(name + ".class"))).accept(cn, 0);
        return cn;
    }

    static void write(ClassNode cn, Path out, String name) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        Path p = out.resolve(name + ".class");
        Files.createDirectories(p.getParent());
        Files.write(p, cw.toByteArray());
    }
}
