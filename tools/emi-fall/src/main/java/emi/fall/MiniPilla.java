package emi.fall;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * «Calabaza caliente»: uno de cada cuatro jugadores lleva una calabaza encendida sobre la cabeza (y corre un poco mas). Se pasa tocando a otro jugador. Cuando se acaba la cuenta atras
 * de la calabaza, EXPLOTA en la cabeza de quien la lleve: queda eliminado (sin eliminaciones: cuenta una explosion y vuelve a jugar). Gana el ultimo en pie (sin eliminaciones: quien menos
 * explosiones se come y mas tiempo pasa sin la calabaza).
 */
final class MiniPilla implements Mini {
    static final int ZC = 2100;
    static final double R = 15.5;
    static final int LIMITE_S = 150, LIMITE_SIN_S = 100;
    static final double TOQUE = 1.7;

    private final Map<Jug, Entity> calabazas = new HashMap<>();
    private long proxBoom;
    private int booms;
    private int limite;
    private int intervaloSin = 14;

    @Override public String id() { return "pilla"; }
    @Override public String nombre() { return "Calabaza caliente"; }
    @Override public String regla() { return "Quien lleva la calabaza la pasa tocando a otro jugador. ¡Cuando acaba la cuenta atrás, explota en la cabeza de quien la tenga!"; }
    @Override public String[] pasos() {
        return new String[]{"Algunos jugadores llevan una calabaza encendida y corren más rápido: tócate con otro para pasársela.",
                "Cuando acaba la cuenta atrás, la calabaza EXPLOTA en la cabeza de quien la lleve.",
                "Huye de quien la tiene. Gana el último en pie (o quien menos explosiones se coma)."};
    }
    @Override public String objetivo() { return "Pasa la calabaza antes de que explote"; }
    @Override public String musica() { return "fiesta"; }
    @Override public int vivo(Ctx c, Jug j) { return j.cuenta; }
    @Override public String unidad() { return "segundos sin calabaza"; }
    @Override public double[] zona() { return new double[]{0, ZC, 28}; }

    @Override
    public void construir(ServerWorld w) {
        BlockPos marca = new BlockPos(0, -64, ZC);
        if (w.getBlockState(marca).isOf(Blocks.CRYING_OBSIDIAN)) return;
        int S = Minijuegos.SUELO;
        int n = (int) Math.ceil(R) + 1;
        for (int dx = -n; dx <= n; dx++) for (int dz = -n; dz <= n; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > R) continue;
            String b = d > R - 1.2 ? "blackstone" : (Math.abs(d - 7.5) < 0.7 || d < 2.2) ? "purple_concrete" : (((dx + dz) & 1) == 0 ? "orange_concrete" : "red_concrete");
            Obra.poner(w, dx, S, ZC + dz, b);
            Obra.poner(w, dx, S - 1, ZC + dz, "black_concrete");
        }
        // pilares y lapidas para esconderse
        int[][] pilares = {{6, 0}, {-6, 0}, {0, 6}, {0, -6}, {10, 5}, {-10, -5}, {-5, 10}, {5, -10}};
        for (int[] p : pilares) for (int y = 1; y <= 3; y++) Obra.poner(w, p[0], S + y, ZC + p[1], y == 3 ? "jack_o_lantern" : "polished_blackstone_bricks");
        Disp.texto(w, "{\"text\":\"CALABAZA CALIENTE\",\"color\":\"gold\",\"bold\":true}", 0.5, S + 9, ZC + 0.5, 6f, false);
        Obra.poner(w, 0, -64, ZC, "crying_obsidian");
    }

    @Override public void ponerDeco(ServerWorld w) { decorar(w); }

    static void decorar(ServerWorld w) {
        if (!Deco.pendiente(w, ZC)) return;
        int S = Minijuegos.SUELO;
        Random r = new Random(21);
        Deco.anilloBarrera(w, 0, ZC, R + 0.4, S + 1, S + 5);
        Deco.anilloFarolas(w, 0, ZC, R + 2, 14, S + 1, S - 6);
        for (int i = 0; i < 6; i++) { double a = 2 * Math.PI * i / 6 + 0.25; Deco.arbolSeco(w, (int) Math.round(Math.cos(a) * (R + 6)), S - 5, ZC + (int) Math.round(Math.sin(a) * (R + 6)), 9, r); }
        for (int i = 0; i < 16; i++) { double a = r.nextDouble() * 2 * Math.PI; Deco.telarañas(w, (int) Math.round(Math.cos(a) * (R - 0.5)), S + 1, ZC + (int) Math.round(Math.sin(a) * (R - 0.5)), 2, r); }
        for (int i = 0; i < 8; i++) { double a = 2 * Math.PI * i / 8 + 0.2; Deco.lapida(w, (int) Math.round(Math.cos(a) * (R + 3.5)), S, ZC + (int) Math.round(Math.sin(a) * (R + 3.5)), i); }
        Deco.michi(w, -13, S - 2, ZC + 36, 'x', 2);
        Deco.luna(w, 0, S + 32, ZC - 30, 9, 'x');
        Deco.pedestal(w, 0, S, ZC, "gigante_gatitoalien_gold_witch_hat", "s");
        Deco.marcar(w, ZC);
    }

    @Override
    public void empezar(Ctx c) {
        c.forzar(-22, ZC - 22, 22, ZC + 22);
        Disp.barrerMoviles(c.w, 0, ZC, 28);
        quitarEntidades();
        limite = Torneo.tt((c.sinElim() ? LIMITE_SIN_S : LIMITE_S) * 20);
        booms = 0;
        int n = c.jugs().size(), i = 0;
        for (Jug j : c.jugs()) {
            j.cuenta = 0; j.caidas = 0; j.tiene = false; j.hasta = 0;
            double a = 2 * Math.PI * (i++) / Math.max(1, n) + 0.4;
            c.colocar(j, Math.cos(a) * 11 + 0.5, Minijuegos.SUELO + 1, ZC + Math.sin(a) * 11 + 0.5, (float) Math.toDegrees(a) + 90f);
        }
    }

    private void quitarEntidades() {
        for (Entity e : calabazas.values()) Disp.quitar(e);
        calabazas.clear();
    }

    private int cuantasCalabazas(Ctx c) { return Math.max(1, (int) Math.ceil(c.vivos().size() / 4.0)); }

    private int llevan(Ctx c) { int k = 0; for (Jug j : c.jugs()) if (j.tiene && !j.fuera) k++; return k; }

    private void dar(Ctx c, Jug j) {
        j.tiene = true;
        ServerPlayerEntity p = c.pl(j);
        if (p == null) return;
        Sonido.sfx(p, "maullido", 1f, 0.8f);
        Msg.title(p, Text.literal("¡LA LLEVAS!").formatted(Formatting.RED, Formatting.BOLD), Text.literal("Toca a otro jugador para pasársela").formatted(Formatting.GOLD), 0, 30, 8);
        c.w.spawnParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1.5, p.getZ(), 16, 0.3, 0.4, 0.3, 0.05);
    }

    /** Reparte las calabazas que falten entre jugadores que no la lleven (los recien soltados, no). */
    private void completar(Ctx c, long tick) {
        List<Jug> libres = new ArrayList<>();
        for (Jug j : c.vivos()) if (!j.tiene && j.hasta <= tick) libres.add(j);
        if (libres.isEmpty()) for (Jug j : c.vivos()) if (!j.tiene) libres.add(j);
        java.util.Collections.shuffle(libres, c.rnd);
        while (llevan(c) < cuantasCalabazas(c) && !libres.isEmpty()) dar(c, libres.remove(0));
    }

    @Override
    public void liberar(Ctx c) {
        booms = 0;
        proxBoom = c.tick + Torneo.tt((c.sinElim() ? intervaloSin : 20) * 20);
        completar(c, c.tick);
    }

    private long intervalo(Ctx c) {
        return Torneo.tt((c.sinElim() ? intervaloSin : (int) Math.max(8, 16 - 1.5 * booms)) * 20);
    }

    @Override
    public boolean tick(Ctx c) {
        int S = Minijuegos.SUELO;
        // la calabaza sobre la cabeza de quien la lleva
        for (Jug j : c.jugs()) {
            ServerPlayerEntity p = c.pl(j);
            Entity e = calabazas.get(j);
            if (p == null || j.fuera || !j.tiene) {
                if (e != null) { Disp.quitar(e); calabazas.remove(j); }
                if (p != null && p.isGlowing() && !j.tiene) p.setGlowing(false);
                continue;
            }
            if (e == null || e.isRemoved()) { e = Disp.bloque(c.w, "minecraft:jack_o_lantern", p.getX(), p.getY() + 1.6, p.getZ(), 0.9f, 0.9f, 0.9f, 0, 0); calabazas.put(j, e); }
            else Disp.moverBloque(e, p.getX(), p.getY() + 1.55 + 0.08 * Math.sin(c.tick * 0.3), p.getZ(), 0.9f, 0.9f, 0.9f, c.tick * 0.12, 0);
            if (c.tick % 20 == 0) p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 50, 0, true, false));
            if (!p.isGlowing()) p.setGlowing(true);
            if (c.tick % 4 == 0) c.w.spawnParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 2.1, p.getZ(), 1, 0.15, 0.05, 0.15, 0.01);
        }
        // pasar la calabaza al tocar
        for (Jug h : c.jugs()) {
            ServerPlayerEntity ph = c.pl(h);
            if (ph == null || h.fuera || !h.tiene) continue;
            for (Jug o : c.jugs()) {
                if (o == h || o.fuera || o.tiene || o.hasta > c.tick) continue;
                ServerPlayerEntity po = c.pl(o);
                if (po == null) continue;
                double dx = ph.getX() - po.getX(), dz = ph.getZ() - po.getZ();
                if (dx * dx + dz * dz > TOQUE * TOQUE || Math.abs(ph.getY() - po.getY()) > 2) continue;
                h.tiene = false; h.hasta = c.tick + 40;               // quien la suelta no puede recibirla de vuelta en 2 s
                ph.removeStatusEffect(StatusEffects.SPEED); ph.setGlowing(false);
                dar(c, o);
                o.hasta = c.tick + 10;
                Sonido.sfx(ph, "pop", 0.8f, 1.3f);
                break;
            }
        }
        // cuenta atras y explosion
        long resta = proxBoom - c.tick;
        if (resta > 0 && resta <= 100 && resta % 20 == 0) for (Jug j : c.jugs()) if (j.tiene && !j.fuera) Sonido.sfx(c.pl(j), "tic", 1f, 0.9f + (100 - resta) / 200f);
        if (c.tick >= proxBoom) {
            for (Jug j : c.jugs()) {
                if (!j.tiene || j.fuera) continue;
                ServerPlayerEntity p = c.pl(j);
                j.tiene = false;
                Entity e = calabazas.remove(j);
                Disp.quitar(e);
                if (p == null) continue;
                p.removeStatusEffect(StatusEffects.SPEED); p.setGlowing(false);
                c.w.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
                c.w.spawnParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 40, 0.6, 0.6, 0.6, 0.15);
                c.w.playSound(null, p.getX(), p.getY(), p.getZ(), net.minecraft.sound.SoundEvents.ENTITY_GENERIC_EXPLODE.value(), net.minecraft.sound.SoundCategory.PLAYERS, 1.4f, 1f);
                Sonido.sfx(p, "boo", 0.8f, 1f);
                if (c.sinElim()) {
                    j.caidas++; j.cuenta = 0; j.hasta = c.tick + 60;
                    c.avisar(Text.literal("¡Le explotó la calabaza a " + j.nombre + "! (" + j.caidas + ")").formatted(Formatting.GOLD));
                } else {
                    j.cuenta = c.segundos();
                    c.eliminar(j);
                    c.avisar(Text.literal("☠ ¡La calabaza explotó en la cabeza de " + j.nombre + "!").formatted(Formatting.RED));
                    Msg.title(p, Text.literal("¡BOOM!").formatted(Formatting.RED, Formatting.BOLD), Text.literal("Eliminado").formatted(Formatting.GRAY), 3, 40, 10);
                    p.teleport(c.w, 0.5, S + 9, ZC + 0.5, p.getYaw(), 0f);
                }
            }
            booms++;
            completar(c, c.tick);
            proxBoom = c.tick + intervalo(c);
        }
        if (c.tick % 20 == 0) for (Jug j : c.jugs()) if (!j.fuera && !j.tiene && c.pl(j) != null) j.cuenta++;
        // por si alguien se sale
        for (Jug j : c.jugs()) { ServerPlayerEntity p = c.pl(j); if (p != null && !j.fuera && p.getY() < S - 3) c.reaparecer(j, 0.5, S + 1, ZC + 0.5, p.getYaw()); }
        c.t.barra(1f - (float) c.tick / limite);
        if (c.tick >= limite) return true;
        if (!c.sinElim()) { int vivos = c.vivos().size(); if (vivos == 0 || (c.jugs().size() > 1 && vivos <= 1)) return true; }
        return false;
    }

    @Override
    public List<Jug> clasificacion(Ctx c) {
        List<Jug> l = c.jugs();
        l.sort((a, b) -> {
            if (c.sinElim()) return a.caidas != b.caidas ? Integer.compare(a.caidas, b.caidas) : Integer.compare(b.cuenta, a.cuenta);
            if (a.fuera != b.fuera) return a.fuera ? 1 : -1;
            if (!a.fuera) return Integer.compare(b.cuenta, a.cuenta);
            return Integer.compare(b.ordenFuera, a.ordenFuera);
        });
        return l;
    }

    @Override
    public void limpiar(Ctx c) {
        quitarEntidades();
        for (Jug j : c.jugs()) { j.tiene = false; ServerPlayerEntity p = c.pl(j); if (p != null) { p.removeStatusEffect(StatusEffects.SPEED); p.setGlowing(false); } }
    }

    @Override public void alIrse(Ctx c, Jug j) { j.tiene = false; Entity e = calabazas.remove(j); Disp.quitar(e); }

    @Override
    public String hud(Ctx c, Jug j) {
        long resta = Math.max(0, (proxBoom - c.tick + 19) / 20);
        if (j.fuera) return "Eliminado · aguantaste " + j.cuenta + " s";
        if (j.tiene) return "¡LA LLEVAS! Toca a otro · BOOM en " + resta + " s";
        return "Huye de la calabaza · BOOM en " + resta + " s" + (c.sinElim() ? " · explosiones " + j.caidas : "");
    }
}
