package emi.fall;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * «Busca el símbolo»: una pantalla gigante en el centro enseña una Pokeball al azar; hay ocho plataformas de colores, cada una con su Pokeball encima. Cuando acaba el
 * tiempo todo el suelo desaparece menos la plataforma de ese símbolo: quien no esta encima cae y queda eliminado. Cada ronda hay menos tiempo y la plataforma es mas
 * pequeña, y mientras tanto caen bloques que lanzan a los jugadores hacia otras plataformas. Gana el ultimo que queda.
 */
final class MiniPlataformas implements Mini {
    static final int ZC = 900;
    static final double R_SUELO = 21.5;           // radio del suelo (disco)
    static final double R_ANILLO = 14.0;          // a que distancia del centro estan las plataformas
    static final int MAX_RONDAS = 14;

    static final String[] ITEMS = {"cobblemon:poke_ball", "cobblemon:great_ball", "cobblemon:ultra_ball", "cobblemon:master_ball", "cobblemon:premier_ball", "cobblemon:luxury_ball", "cobblemon:timer_ball", "cobblemon:quick_ball"};
    static final String[] NOMBRES = {"Poké Ball", "Super Ball", "Ultra Ball", "Master Ball", "Honor Ball", "Lujo Ball", "Turno Ball", "Veloz Ball"};
    static final String[] COLORES = {"red_concrete", "blue_concrete", "yellow_concrete", "magenta_concrete", "white_concrete", "black_concrete", "orange_concrete", "lime_concrete"};

    private enum Paso { MOSTRAR, CAER, PAUSA }

    private static final class Golpe { double x, z; int impacto; Entity e; }

    private final List<Entity> adornos = new ArrayList<>();
    private final List<Golpe> golpes = new ArrayList<>();
    private final List<long[]> suelo = new ArrayList<>();         // [x, z, indiceDeBloque]
    private final List<String> bloqueDe = new ArrayList<>();
    private Entity pantalla, cuenta;
    private int ronda, objetivo = -1, previo = -1;
    private Paso paso = Paso.PAUSA;
    private int tPaso, tDuracion;
    private int proxGolpe;

    @Override public String id() { return "plataformas"; }
    @Override public String nombre() { return "Busca el símbolo"; }
    @Override public String regla() { return "La pantalla enseña una Poké Ball: corre a la plataforma que la tiene. ¡El resto del suelo desaparece! Cuidado con lo que cae."; }

    private static double cx(int k) { return Math.cos(k * Math.PI / 4) * R_ANILLO; }
    private static double cz(int k) { return Math.sin(k * Math.PI / 4) * R_ANILLO; }

    /** El bloque que va en la casilla (dx, dz) relativa al centro. */
    private static String bloqueDeCasilla(int dx, int dz) {
        for (int k = 0; k < 8; k++) if (Math.abs(dx - Math.round(cx(k))) <= 3 && Math.abs(dz - Math.round(cz(k))) <= 3) return COLORES[k];
        return ((dx + dz) & 1) == 0 ? "light_gray_concrete" : "gray_concrete";
    }

    @Override
    public void construir(ServerWorld w) {
        BlockPos marca = new BlockPos(0, -64, ZC);
        if (w.getBlockState(marca).isOf(Blocks.CRYING_OBSIDIAN)) return;
        Obra.poner(w, 0, -64, ZC, "crying_obsidian");
    }

    /** Pone (o repone) todo el suelo. */
    private void ponerSuelo(ServerWorld w) {
        int S = Minijuegos.SUELO;
        suelo.clear(); bloqueDe.clear();
        int n = (int) Math.ceil(R_SUELO);
        for (int dx = -n; dx <= n; dx++) for (int dz = -n; dz <= n; dz++) {
            if (Math.sqrt(dx * dx + dz * dz) > R_SUELO) continue;
            String b = bloqueDeCasilla(dx, dz);
            suelo.add(new long[]{dx, dz, bloqueDe.size()}); bloqueDe.add(b);
            Obra.poner(w, dx, S, ZC + dz, b);
        }
    }

    /** Quita todo el suelo menos la plataforma {@code k} (de lado 2*mitad+1 centrada en el centro de esa plataforma). */
    private void quitarSueloMenos(ServerWorld w, int k, int mitad) {
        int S = Minijuegos.SUELO;
        long ox = Math.round(cx(k)), oz = Math.round(cz(k));
        for (long[] c : suelo) {
            if (Math.abs(c[0] - ox) <= mitad && Math.abs(c[1] - oz) <= mitad) continue;
            Obra.poner(w, (int) c[0], S, ZC + (int) c[1], "air");
        }
    }

    private void reponerSuelo(ServerWorld w) {
        int S = Minijuegos.SUELO;
        for (long[] c : suelo) Obra.poner(w, (int) c[0], S, ZC + (int) c[1], bloqueDe.get((int) c[2]));
    }

    private void quitarEntidades() {
        for (Entity e : adornos) Disp.quitar(e);
        adornos.clear();
        for (Golpe g : golpes) Disp.quitar(g.e);
        golpes.clear();
        Disp.quitar(pantalla); Disp.quitar(cuenta); pantalla = null; cuenta = null;
    }

    @Override
    public void empezar(Ctx c) {
        quitarEntidades();
        ronda = 0; objetivo = -1; previo = -1; paso = Paso.PAUSA; tPaso = 0; tDuracion = 60; proxGolpe = 0;
        ponerSuelo(c.w);
        int S = Minijuegos.SUELO;
        for (int k = 0; k < 8; k++) {
            Entity e = Disp.objeto(c.w, ITEMS[k], cx(k) + 0.5, S + 3.0, ZC + cz(k) + 0.5, 2.2f, true);
            if (e != null) adornos.add(e);
        }
        pantalla = Disp.objeto(c.w, ITEMS[0], 0.5, S + 14, ZC + 0.5, 7f, true);
        cuenta = Disp.texto(c.w, "{\"text\":\"\",\"color\":\"yellow\",\"bold\":true}", 0.5, S + 9, ZC + 0.5, 5f, false);
        // un pilar en el centro que sujeta la pantalla
        for (int y = 1; y <= 6; y++) Obra.poner(c.w, 0, S + y, ZC, y == 6 ? "sea_lantern" : "quartz_pillar");
        int i = 0, n = c.jugs().size();
        for (Jug j : c.jugs()) { double a = 2 * Math.PI * (i++) / Math.max(1, n); c.colocar(j, Math.cos(a) * 3.5 + 0.5, S + 1, ZC + Math.sin(a) * 3.5 + 0.5, (float) Math.toDegrees(a) + 90f); }
    }

    @Override public void liberar(Ctx c) { nuevaRonda(c); }

    private int tiempoMostrar() { return (int) Math.max(60, Math.round(7.0 * 20 - ronda * 11)); }     // 7 s al principio, 3 s al final
    private int mitadPlataforma() { return ronda < 4 ? 3 : ronda < 8 ? 2 : 1; }                        // 7x7, 5x5 y 3x3

    private void nuevaRonda(Ctx c) {
        ronda++;
        int k;
        do { k = c.rnd.nextInt(8); } while (k == previo);
        previo = objetivo = k;
        paso = Paso.MOSTRAR; tPaso = 0; tDuracion = tiempoMostrar();
        Disp.cambiarObjeto(pantalla, ITEMS[k], 0.5, Minijuegos.SUELO + 14, ZC + 0.5, 7f, true);
        c.sonido(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1f, 0.8f + k * 0.08f);
        proxGolpe = 10;
    }

    private void golpear(Ctx c) {
        // un bloque que cae sobre un sitio marcado con particulas: empuja a quien este cerca
        Golpe g = new Golpe();
        double a = c.rnd.nextDouble() * Math.PI * 2, d = Math.sqrt(c.rnd.nextDouble()) * (R_SUELO - 3);
        List<Jug> vivos = c.vivos();
        if (!vivos.isEmpty() && c.rnd.nextDouble() < 0.6) {                      // casi siempre sobre alguien (con un poco de despiste)
            ServerPlayerEntity p = c.pl(vivos.get(c.rnd.nextInt(vivos.size())));
            g.x = p.getX() + c.rnd.nextGaussian() * 1.5; g.z = p.getZ() + c.rnd.nextGaussian() * 1.5;
        } else { g.x = Math.cos(a) * d + 0.5; g.z = ZC + Math.sin(a) * d + 0.5; }
        g.impacto = 26;
        g.e = Disp.bloque(c.w, "minecraft:anvil", g.x, Minijuegos.SUELO + 16, g.z, 1.4f, 1.4f, 1.4f, 0, 0);
        golpes.add(g);
    }

    @Override
    public boolean tick(Ctx c) {
        int S = Minijuegos.SUELO;
        tPaso++;
        List<Jug> vivos = c.vivos();
        switch (paso) {
            case MOSTRAR -> {
                if (tPaso % 5 == 0) {
                    int resta = (int) Math.ceil((tDuracion - tPaso) / 20.0);
                    Disp.cambiarTexto(cuenta, "{\"text\":\"" + resta + "\",\"color\":\"" + (resta <= 1 ? "red" : "yellow") + "\",\"bold\":true}", 0.5, S + 9, ZC + 0.5, 5f, false);
                    if (tPaso % 20 == 0 && tDuracion - tPaso <= 60) c.sonido(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 0.8f, 1.4f);
                }
                if (--proxGolpe <= 0) { golpear(c); proxGolpe = Math.max(8, 30 - ronda * 2); if (ronda > 6 && c.rnd.nextBoolean()) golpear(c); }
                if (tPaso >= tDuracion) {
                    quitarSueloMenos(c.w, objetivo, mitadPlataforma());
                    c.w.playSound(null, 0.5, S + 1, ZC + 0.5, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 3f, 0.6f);
                    Disp.cambiarTexto(cuenta, "{\"text\":\"\",\"color\":\"yellow\"}", 0.5, S + 9, ZC + 0.5, 5f, false);
                    paso = Paso.CAER; tPaso = 0; tDuracion = 50;
                }
            }
            case CAER -> { if (tPaso >= tDuracion) { paso = Paso.PAUSA; tPaso = 0; tDuracion = 36; reponerSuelo(c.w); } }
            case PAUSA -> {
                if (tPaso >= tDuracion) {
                    int quedan = c.vivos().size();
                    if (ronda >= MAX_RONDAS || (c.jugs().size() > 1 && quedan <= 1) || quedan == 0) return true;
                    nuevaRonda(c);
                }
            }
        }
        // los bloques que caen
        for (int i = golpes.size() - 1; i >= 0; i--) {
            Golpe g = golpes.get(i);
            g.impacto--;
            if (g.impacto > 0) {
                double f = 1.0 - g.impacto / 26.0;
                Disp.moverBloque(g.e, g.x, S + 16 - f * 15, g.z, 1.4f, 1.4f, 1.4f, 0, 1);
                if (g.impacto % 4 == 0) {
                    for (int k = 0; k < 10; k++) { double a = k * Math.PI / 5; c.w.spawnParticles(ParticleTypes.SMOKE, g.x + Math.cos(a) * 2.6, S + 1.15, g.z + Math.sin(a) * 2.6, 1, 0, 0, 0, 0); }
                    c.w.spawnParticles(ParticleTypes.CRIT, g.x, S + 1.2, g.z, 4, 1.0, 0.0, 1.0, 0.0);
                }
                continue;
            }
            Disp.quitar(g.e);
            golpes.remove(i);
            c.w.spawnParticles(ParticleTypes.EXPLOSION, g.x, S + 1.2, g.z, 3, 0.6, 0.1, 0.6, 0);
            c.w.playSound(null, g.x, S + 1, g.z, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 1.5f, 0.8f);
            for (Jug j : c.jugs()) {
                ServerPlayerEntity p = c.pl(j);
                if (p == null || j.fuera) continue;
                double dx = p.getX() - g.x, dz = p.getZ() - g.z, d = Math.sqrt(dx * dx + dz * dz);
                if (d > 3.0 || p.getY() > S + 2.5) continue;
                double n = Math.max(0.3, d), f = 1.0 - d / 3.0;
                c.empujar(p, new Vec3d(dx / n * (0.7 + 0.9 * f), 0.4 + 0.3 * f, dz / n * (0.7 + 0.9 * f)));
            }
        }
        // eliminar a los que caen
        for (Jug j : c.jugs()) {
            ServerPlayerEntity p = c.pl(j);
            if (p == null || j.fuera) continue;
            if (p.getY() < S - 4) {
                c.eliminar(j);
                j.fila = ronda;
                c.avisar(Text.literal("☠ " + j.nombre + " ha caído").formatted(Formatting.RED));
                Msg.title(p, Text.literal("¡ELIMINADO!").formatted(Formatting.RED, Formatting.BOLD), Text.literal("Has llegado a la ronda " + ronda).formatted(Formatting.GRAY), 3, 40, 10);
                p.playSoundToPlayer(SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.MASTER, 0.5f, 1.8f);
                // los espectadores miran desde arriba
                p.teleport(c.w, p.getX(), S + 10, p.getZ(), p.getYaw(), 0f);
            }
        }
        c.t.barra(1f - (float) ronda / MAX_RONDAS);
        return false;
    }

    @Override
    public List<Jug> clasificacion(Ctx c) {
        List<Jug> l = c.jugs();
        l.sort((a, b) -> {
            if (a.fuera != b.fuera) return a.fuera ? 1 : -1;
            if (a.fila != b.fila) return Integer.compare(b.fila, a.fila);
            return Integer.compare(b.ordenFuera, a.ordenFuera);
        });
        return l;
    }

    @Override public void limpiar(Ctx c) { quitarEntidades(); }

    @Override
    public String hud(Ctx c, Jug j) {
        return "Ronda " + ronda + "/" + MAX_RONDAS + " · quedan " + c.vivos().size() + (j.fuera ? " · eliminado" : "");
    }
}
