import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;

/**
 * Jefe Snorlax -> Snorlax Emi (emipokemon:snorlax_emi), con su voz propia y el aviso "te voy a aplastar".
 *  - SnorlaxRaidService.spawnSnorlax: la especie de las propiedades pasa de "snorlax" a "emipokemon:snorlax_emi".
 *  - Sonidos: aparicion, aviso del aplastamiento, impacto del aplastamiento y cambio de fase usan SnorlaxVoice (emipokemon:snorlax_emi_voz).
 *  - Textos visibles: "Snorlax" -> "Snorlax Emi" (jefe, cola, avisos) y el jefe dice "te voy a aplastar".
 * Uso: PatchSnorlaxEmi <jar_extraido> <clases_SnorlaxVoice> <salida>
 */
public class PatchSnorlaxEmi {
    static final String PKG = "com/emipokemon/raid/snorlax/";
    static final String VOICE = PKG + "SnorlaxVoice";
    static final String RAID = PKG + "SnorlaxRaidService";

    static boolean isLog(String s) {
        return s.startsWith("Snorlax raid") || s.startsWith("Could not") || s.contains("raid:") || s.contains("raid boss") && !s.contains("§");
    }

    static String rename(String s) {
        if (!s.contains("Snorlax") || isLog(s) || s.contains("Snorlax Emi")) return s;
        return s.replace("Snorlax", "Snorlax Emi");
    }

    static void fail(String m) { throw new IllegalStateException(m); }

    public static void main(String[] a) throws Exception {
        Path in = Paths.get(a[0]), voice = Paths.get(a[1]), out = Paths.get(a[2]);
        for (String cls : new String[]{"SnorlaxRaidService", "SnorlaxQueueService", "SnorlaxBossCommands"}) {
            ClassNode cn = new ClassNode();
            new ClassReader(Files.readAllBytes(in.resolve(PKG + cls + ".class"))).accept(cn, 0);
            int renamed = 0, species = 0, spawnMsg = 0, crushMsg = 0, sounds = 0;
            for (MethodNode m : cn.methods) {
                int soundsHere = 0;
                for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
                    if (i instanceof LdcInsnNode l && l.cst instanceof String s) {
                        if (cls.equals("SnorlaxRaidService") && m.name.equals("spawnSnorlax") && s.contains("¡Snorlax ha aparecido!")) {
                            l.cst = "§6⚔ ¡Snorlax Emi ha aparecido! §f«¡Los voy a aplastar a todos!» §6Es una pelea a golpes, no una batalla Pokémon.";
                            spawnMsg++;
                            continue;
                        }
                        String r = rename(s);
                        if (!r.equals(s)) { l.cst = r; renamed++; }
                    } else if (i instanceof InvokeDynamicInsnNode d && d.bsmArgs.length > 0 && d.bsmArgs[0] instanceof String s) {
                        if (cls.equals("SnorlaxRaidService") && m.name.equals("spawnSnorlax") && s.startsWith("snorlax level=")) {
                            d.bsmArgs[0] = "emipokemon:" + "snorlax_emi" + s.substring("snorlax".length());
                            species++;
                            continue;
                        }
                        if (cls.equals("SnorlaxRaidService") && m.name.equals("startCrush") && s.contains("va a aplastar a")) {
                            if (s.chars().filter(c -> c == 1).count() != 2) fail("startCrush: se esperaban 2 argumentos");
                            d.bsmArgs[0] = "§4☠ Snorlax Emi: §f«¡Te voy a aplastar, \u0001!» §4(tiene más vida que nadie) §c¡\u0001 segundos para curarse o alejarse!";
                            crushMsg++;
                            continue;
                        }
                        String r = rename(s);
                        if (!r.equals(s)) { d.bsmArgs[0] = r; renamed++; }
                    } else if (cls.equals("SnorlaxRaidService") && i instanceof MethodInsnNode mi && mi.getOpcode() == Opcodes.INVOKEVIRTUAL
                            && mi.owner.equals("net/minecraft/class_3218")
                            && (m.name.equals("spawnSnorlax") || m.name.equals("startCrush") || m.name.equals("transitionPhase") || m.name.equals("resolveCrushCharge"))) {
                        if (mi.name.equals("method_43128") && !m.name.equals("resolveCrushCharge")) {
                            mi.setOpcode(Opcodes.INVOKESTATIC);
                            mi.owner = VOICE; mi.name = "play"; mi.itf = false;
                            mi.desc = "(Lnet/minecraft/class_1937;" + mi.desc.substring(1);
                            soundsHere++;
                        } else if (mi.name.equals("method_60511") && m.name.equals("resolveCrushCharge")) {
                            mi.setOpcode(Opcodes.INVOKESTATIC);
                            mi.owner = VOICE; mi.name = "playHolder"; mi.itf = false;
                            mi.desc = "(Lnet/minecraft/class_1937;" + mi.desc.substring(1);
                            soundsHere++;
                        }
                    }
                }
                if (cls.equals("SnorlaxRaidService") && (m.name.equals("spawnSnorlax") || m.name.equals("startCrush") || m.name.equals("transitionPhase") || m.name.equals("resolveCrushCharge")) && soundsHere != 1)
                    fail(cls + "." + m.name + ": se esperaba 1 sonido y hay " + soundsHere);
                sounds += soundsHere;
            }
            if (cls.equals("SnorlaxRaidService") && (species != 1 || spawnMsg != 1 || crushMsg != 1 || sounds != 4))
                fail("SnorlaxRaidService no coincide: species=" + species + " spawnMsg=" + spawnMsg + " crushMsg=" + crushMsg + " sonidos=" + sounds);
            if (renamed == 0 && !cls.equals("SnorlaxBossCommands")) fail(cls + ": sin textos que renombrar");
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            cn.accept(cw);
            Path o = out.resolve(PKG + cls + ".class");
            Files.createDirectories(o.getParent());
            Files.write(o, cw.toByteArray());
            System.out.println(cls + ": textos=" + renamed + " especie=" + species + " sonidos=" + sounds);
        }
        Path v = voice.resolve(VOICE + ".class");
        if (!Files.exists(v)) fail("falta " + v);
        Files.createDirectories(out.resolve(PKG));
        Files.copy(v, out.resolve(VOICE + ".class"), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("patched OK");
    }
}
