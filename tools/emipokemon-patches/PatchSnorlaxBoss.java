import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;

/**
 * Snorlax Emi (segunda parte, se aplica despues de PatchSnorlaxEmi):
 *  - SnorlaxRaidService: ya no teletransporta a los participantes (2 sitios) ni les pone la camara al aparecer y morir (2 cutscenes);
 *    resolveCombat pasa por SnorlaxExtras.resolve (ataques nuevos: Salto Aplastante, Lluvia de Rocas, Bostezo).
 *  - NpcCommands.createCustom: al crear el NPC "emi_snorlax_queue" se le pone su skin empaquetada (SnorlaxNpcSkin).
 * Uso: PatchSnorlaxBoss <jar_extraido> <clases_compiladas(SnorlaxExtras, SnorlaxNpcSkin)> <salida>
 */
public class PatchSnorlaxBoss {
    static final String PKG = "com/emipokemon/raid/snorlax/";
    static final String RAID = PKG + "SnorlaxRaidService";
    static final String EXTRAS = PKG + "SnorlaxExtras";
    static final String SKIN = PKG + "SnorlaxNpcSkin";
    static final String NPCCMD = "com/emipokemon/npc/command/NpcCommands";

    static void fail(String m) { throw new IllegalStateException(m); }

    static ClassNode read(Path p) throws Exception {
        ClassNode cn = new ClassNode();
        new ClassReader(Files.readAllBytes(p)).accept(cn, 0);
        return cn;
    }

    static void write(ClassNode cn, Path out, String name) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        Path o = out.resolve(name + ".class");
        Files.createDirectories(o.getParent());
        Files.write(o, cw.toByteArray());
    }

    public static void main(String[] a) throws Exception {
        Path in = Paths.get(a[0]), classes = Paths.get(a[1]), out = Paths.get(a[2]);

        ClassNode raid = read(in.resolve(RAID + ".class"));
        Set<String> open = new HashSet<>(List.of("resolveCombat", "knockback", "triggerAnimation", "broadcastToParticipants", "damageMultiplier", "phaseMeleeDamage", "phaseAreaSlamDamage"));
        Set<String> opened = new HashSet<>();
        for (MethodNode m : raid.methods) {
            if (open.contains(m.name) && (m.access & Opcodes.ACC_PRIVATE) != 0 && (m.access & Opcodes.ACC_STATIC) != 0) {
                m.access &= ~Opcodes.ACC_PRIVATE;
                opened.add(m.name);
            }
        }
        if (!opened.equals(open)) fail("metodos que se esperaban privados y no se encontraron: " + open.stream().filter(x -> !opened.contains(x)).toList());

        int combat = 0, tp = 0, cut = 0;
        for (MethodNode m : raid.methods) {
            for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
                if (!(i instanceof MethodInsnNode mi)) continue;
                if (mi.getOpcode() == Opcodes.INVOKESTATIC && mi.owner.equals(RAID) && mi.name.equals("resolveCombat")) {
                    mi.owner = EXTRAS; mi.name = "resolve"; combat++;
                } else if (mi.getOpcode() == Opcodes.INVOKEVIRTUAL && mi.owner.equals("net/minecraft/class_3222") && mi.name.equals("method_14251")) {
                    mi.setOpcode(Opcodes.INVOKESTATIC); mi.owner = EXTRAS; mi.name = "noTeleport"; mi.itf = false;
                    mi.desc = "(Lnet/minecraft/class_3222;" + mi.desc.substring(1); tp++;
                } else if (mi.getOpcode() == Opcodes.INVOKESTATIC && mi.owner.equals(RAID) && mi.name.equals("startCutscene")) {
                    mi.owner = EXTRAS; mi.name = "skipCutscene"; cut++;
                }
            }
        }
        if (combat != 1 || tp != 2 || cut != 2) fail("SnorlaxRaidService no coincide: resolveCombat=" + combat + " teleports=" + tp + " cutscenes=" + cut);
        write(raid, out, RAID);

        ClassNode npc = read(in.resolve(NPCCMD + ".class"));
        MethodNode m = null;
        for (MethodNode x : npc.methods) if (x.name.equals("createCustom") && x.desc.equals("(Lnet/minecraft/class_2168;Ljava/lang/String;Z)I")) m = x;
        if (m == null) fail("createCustom no encontrado");
        // la variable "id" es el resultado de normalizeId(rawId)
        int idVar = -1;
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext())
            if (i instanceof MethodInsnNode mi && mi.name.equals("normalizeId") && i.getNext() instanceof VarInsnNode v && v.getOpcode() == Opcodes.ASTORE) { idVar = v.var; break; }
        if (idVar < 0) fail("variable id no encontrada");
        AbstractInsnNode ret = null;
        for (AbstractInsnNode i = m.instructions.getLast(); i != null; i = i.getPrevious())
            if (i.getOpcode() == Opcodes.IRETURN && i.getPrevious().getOpcode() == Opcodes.ICONST_1) { ret = i.getPrevious(); break; }
        if (ret == null) fail("return 1 final no encontrado");
        InsnList add = new InsnList();
        add.add(new FieldInsnNode(Opcodes.GETSTATIC, NPCCMD, "assets", "Lcom/emipokemon/visual/VisualAssetService;"));
        add.add(new VarInsnNode(Opcodes.ALOAD, 0));
        add.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/class_2168", "method_9211", "()Lnet/minecraft/server/MinecraftServer;", false));
        add.add(new VarInsnNode(Opcodes.ALOAD, idVar));
        add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, SKIN, "apply", "(Lcom/emipokemon/visual/VisualAssetService;Lnet/minecraft/server/MinecraftServer;Ljava/lang/String;)V", false));
        m.instructions.insertBefore(ret, add);
        write(npc, out, NPCCMD);

        for (String c : new String[]{EXTRAS, SKIN}) {
            Path src = classes.resolve(c + ".class");
            if (!Files.exists(src)) fail("falta " + src);
            Files.createDirectories(out.resolve(c).getParent());
            Files.copy(src, out.resolve(c + ".class"), StandardCopyOption.REPLACE_EXISTING);
        }
        System.out.println("patched OK: resolveCombat=" + combat + " teleports=" + tp + " cutscenes=" + cut + " (variable id=" + idVar + ")");
    }
}
