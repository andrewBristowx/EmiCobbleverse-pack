package emi.fall;

import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Random;

/**
 * «Camino correcto» (puente de cristal del Juego del Calamar): 14 filas de DOS cristales (izquierda y derecha) separadas por 3 bloques de vacio; hay que saltar de una fila
 * a la siguiente. En cada fila solo UNO aguanta (al azar, distinto en cada partida): el otro se rompe nada mas pisarlo y caes (reapareces en la salida). Los cristales rotos
 * se quedan rotos, asi que los que van detras aprenden de los que caen. Gana quien llega antes al otro lado.
 */
final class MiniCamino implements Mini {
    static final int ZC = 300;                  // centro de la arena (z)
    static final int FILAS = 14;
    static final int X0 = 3;                    // la primera fila empieza en este x (la plataforma de salida acaba en x = -1: 3 bloques de vacio)
    static final int PROF = 3;                  // cada cristal mide 3 de fondo (en x) y 3 de ancho (en z)
    static final int PASO = 6;                  // de una fila a la siguiente: 3 de cristal + 3 de vacio
    static final int Z0 = ZC - 4;               // el cristal izquierdo ocupa z = Z0..Z0+2, el derecho Z0+4..Z0+6 (un bloque de vacio entre los dos)
    static final int FIN_X = X0 + (FILAS - 1) * PASO + PROF + 3;     // la plataforma de llegada (3 bloques de vacio despues de la ultima fila)
    static final int LIMITE_S = 150, EXTRA_S = 40;
    static final int RETRASO = 2;               // ticks desde que pisas el cristal falso hasta que se rompe (casi en el acto: se oye y se ve el crujido)

    private final int[] bueno = new int[FILAS];                 // la columna (0 o 1) que aguanta en cada fila
    private final boolean[][] roto = new boolean[FILAS][2];
    private final long[][] romper = new long[FILAS][2];         // tick en que se rompe (0 = no se rompe)
    private int primero = -1;                                   // tick en que llego el primero (-1 = nadie)

    @Override public String id() { return "camino"; }
    @Override public String nombre() { return "Camino correcto"; }
    @Override public String regla() { return "Salta de cristal en cristal: en cada fila solo uno aguanta y el falso se rompe nada más pisarlo. ¡Llega primero al otro lado!"; }
    @Override public String[] pasos() {
        return new String[]{"Cruza el puente de cristal saltando de fila en fila: en cada fila hay dos cristales, solo uno aguanta.",
                "El falso se rompe en cuanto lo pisas y vuelves a la salida. Los rotos se quedan rotos para todos: mira dónde caen los demás.",
                "Gana quien llegue antes al final."};
    }
    @Override public String objetivo() { return "Elige el cristal que aguanta"; }
    @Override public String musica() { return "tension"; }
    @Override public int vivo(Ctx c, Jug j) { return j.fila; }
    @Override public String unidad() { return "fila"; }

    private static int filaX(int fila) { return X0 + fila * PASO; }
    private static int colZ(int col) { return Z0 + col * 4; }

    @Override
    public void construir(ServerWorld w) {
        BlockPos marca = new BlockPos(1, -64, ZC);                       // la marca de la version 2 (puente con separaciones)
        if (w.getBlockState(marca).isOf(Blocks.CRYING_OBSIDIAN)) return;
        long t0 = System.nanoTime();
        int S = Minijuegos.SUELO;
        for (int x = -14; x <= FIN_X + 20; x++) for (int z = ZC - 8; z <= ZC + 8; z++) for (int y = S - 1; y <= S + 10; y++) Obra.poner(w, x, y, z, "air");   // borra la version vieja
        Disp.barrer(w, 40, ZC, 90);                                      // carteles de la version vieja
        for (int x = -12; x <= -1; x++) for (int z = ZC - 5; z <= ZC + 5; z++) { Obra.poner(w, x, S, z, ((x + z) & 1) == 0 ? "light_blue_concrete" : "white_concrete"); Obra.poner(w, x, S - 1, z, "white_concrete"); }
        for (int x = FIN_X; x <= FIN_X + 11; x++) for (int z = ZC - 5; z <= ZC + 5; z++) { Obra.poner(w, x, S, z, ((x + z) & 1) == 0 ? "lime_concrete" : "yellow_concrete"); Obra.poner(w, x, S - 1, z, "white_concrete"); }
        for (int y = 1; y <= 5; y++) { Obra.poner(w, FIN_X + 1, S + y, ZC - 5, "gold_block"); Obra.poner(w, FIN_X + 1, S + y, ZC + 5, "gold_block"); }
        for (int z = ZC - 5; z <= ZC + 5; z++) Obra.poner(w, FIN_X + 1, S + 6, z, ((z & 1) == 0) ? "gold_block" : "yellow_concrete");
        Disp.texto(w, "{\"text\":\"META\",\"color\":\"gold\",\"bold\":true}", FIN_X + 1.5, S + 8.2, ZC + 0.5, 6f, false);
        Disp.texto(w, "{\"text\":\"SALIDA\",\"color\":\"aqua\",\"bold\":true}", -6.5, S + 4.5, ZC + 0.5, 4f, false);
        Disp.texto(w, "{\"text\":\"¡Salta de cristal en cristal!\",\"color\":\"light_purple\",\"bold\":true}", 1.5, S + 4.5, ZC + 0.5, 3f, false);
        Obra.poner(w, 0, -64, ZC, "crying_obsidian");
        Obra.poner(w, 1, -64, ZC, "crying_obsidian");
        EmiFall.LOG.info("Arena «camino correcto» (version 2) construida en {} ms", (System.nanoTime() - t0) / 1_000_000);
    }


    @Override public void ponerDeco(ServerWorld w) { decorar(w); }

    static void decorar(ServerWorld w) {
        if (!Deco.pendiente(w, ZC)) return;
        int S = Minijuegos.SUELO;
        Random r = new Random(4);
        for (int x = -8; x <= FIN_X + 8; x += 12) for (int s = -1; s <= 1; s += 2) Deco.farola(w, x, S + 1, ZC + s * 9, S - 12, s < 0 ? "s" : "n");
        for (int x = 10; x <= FIN_X; x += 30) for (int s = -1; s <= 1; s += 2) Deco.arbolSeco(w, x, S - 12, ZC + s * 14, 14, r);
        Deco.michi(w, -22, S - 3, ZC - 13, 'z', 2);
        Deco.michi(w, FIN_X + 22, S - 3, ZC - 13, 'z', 2);
        Deco.luna(w, FIN_X / 2, S + 32, ZC - 34, 9, 'x');
        Deco.pedestal(w, FIN_X + 6, S, ZC - 9, "gigante_gatitoalien_witch_hat", "w");
        Deco.pedestal(w, FIN_X + 6, S, ZC + 9, "gigante_gatitoalien_devil_horns", "w");
        Deco.pedestal(w, -10, S, ZC + 9, "gigante_gatitoalien_gold_witch_hat", "e");
        Deco.marcar(w, ZC);
    }

    @Override public double[] zona() { return null; }

    /** Quita lo que haya del puente, pone los cristales (todos) y elige el camino de esta partida (al azar en cada fila). */
    private void ponerCristales(ServerWorld w, Random rnd) {
        int S = Minijuegos.SUELO;
        for (int x = 0; x < FIN_X; x++) for (int z = ZC - 6; z <= ZC + 6; z++) Obra.poner(w, x, S, z, "air");
        for (int f = 0; f < FILAS; f++) {
            bueno[f] = rnd.nextInt(2);
            for (int k = 0; k < 2; k++) { roto[f][k] = false; romper[f][k] = 0; rellenar(w, f, k, S); }
        }
        EmiFall.LOG.info("[camino] camino de esta partida (cristal bueno de cada fila, 0 = izquierda): {}", java.util.Arrays.toString(bueno));
    }

    private static void rellenar(ServerWorld w, int fila, int col, int S) {
        String bloque = "light_blue_stained_glass";                  // todos iguales: nada debe delatar cual es el bueno
        for (int dx = 0; dx < PROF; dx++) for (int dz = 0; dz < 3; dz++) Obra.poner(w, filaX(fila) + dx, S, colZ(col) + dz, bloque);
    }

    private static void quitarCristal(ServerWorld w, int fila, int col, int S) {
        for (int dx = 0; dx < PROF; dx++) for (int dz = 0; dz < 3; dz++) Obra.poner(w, filaX(fila) + dx, S, colZ(col) + dz, "air");
        w.spawnParticles(ParticleTypes.CLOUD, filaX(fila) + 1.5, S + 1, colZ(col) + 1.5, 14, 1.0, 0.2, 1.0, 0.05);
        w.spawnParticles(ParticleTypes.CRIT, filaX(fila) + 1.5, S + 1, colZ(col) + 1.5, 20, 1.2, 0.3, 1.2, 0.2);
        w.playSound(null, filaX(fila) + 1.5, S, colZ(col) + 1.5, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 1.6f, 0.9f);
    }

    @Override
    public void empezar(Ctx c) {
        primero = -1;
        c.forzar(-14, ZC - 8, FIN_X + 14, ZC + 8);
        ponerCristales(c.w, c.rnd);
        int i = 0;
        for (Jug j : c.jugs()) {
            int n = i++;
            c.colocar(j, -10 + (n % 3) * 2 + 0.5, Minijuegos.SUELO + 1, ZC - 4 + (n / 3) * 2 + 0.5, -90f);
        }
    }

    @Override public void liberar(Ctx c) { }

    private int limite(Ctx c) { return primero < 0 ? Torneo.tt(LIMITE_S * 20) : Math.min(Torneo.tt(LIMITE_S * 20), primero + Torneo.tt(EXTRA_S * 20)); }

    @Override
    public boolean tick(Ctx c) {
        int S = Minijuegos.SUELO;
        // los cristales que tocaba romper
        for (int f = 0; f < FILAS; f++) for (int k = 0; k < 2; k++) if (romper[f][k] > 0 && c.tick >= romper[f][k]) { romper[f][k] = 0; roto[f][k] = true; quitarCristal(c.w, f, k, S); }
        int llegados = 0, jugando = 0;
        for (Jug j : c.jugs()) {
            ServerPlayerEntity p = c.pl(j);
            if (p == null) continue;
            jugando++;
            if (j.llego) { llegados++; continue; }
            double x = p.getX(), y = p.getY(), z = p.getZ();
            if (y < S - 8) {                                                       // cayo al vacio: otra vez a la salida
                j.caidas++;
                int n = Math.floorMod(j.id.hashCode(), 6);
                c.reaparecer(j, -10 + (n % 3) * 2 + 0.5, S + 1, ZC - 4 + n * 1.5 + 0.5, -90f);
                p.sendMessage(Text.literal("¡Has caído! Vuelves a la salida.").formatted(Formatting.RED), true);
                p.playSoundToPlayer(SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.MASTER, 0.4f, 1.6f);
                continue;
            }
            int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
            if (bx >= FIN_X && y > S + 0.5 && y < S + 3) {                         // la meta
                j.llego = true; j.tLlegada = c.tick; j.fila = FILAS;
                if (primero < 0) { primero = c.tick; c.avisar(Msg.say(Text.literal(j.nombre + " ha llegado primero. Los demás tenéis " + EXTRA_S + " segundos.").formatted(Formatting.GOLD))); }
                int puesto = 1; for (Jug o : c.jugs()) if (o != j && o.llego) puesto++;
                p.sendMessage(Text.literal("¡Has llegado el " + puesto + "º!").formatted(Formatting.GREEN, Formatting.BOLD), true);
                p.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1f, 1.2f);
                continue;
            }
            if (y > S + 2.2 || y < S + 0.5 || !p.isOnGround()) continue;           // solo cuenta estar de pie sobre un cristal
            int rel = bx - X0;
            if (rel < 0) continue;
            int fila = rel / PASO;
            if (fila >= FILAS || rel % PASO >= PROF) continue;
            int col = bz >= Z0 && bz <= Z0 + 2 ? 0 : bz >= Z0 + 4 && bz <= Z0 + 6 ? 1 : -1;
            if (col < 0) continue;
            if (col != bueno[fila]) {                                              // cristal falso: se rompe en cuanto lo pisas
                if (!roto[fila][col] && romper[fila][col] == 0) {
                    romper[fila][col] = c.tick + RETRASO;
                    c.w.spawnParticles(ParticleTypes.CRIT, filaX(fila) + 1.5, S + 1.1, colZ(col) + 1.5, 10, 1.0, 0.1, 1.0, 0.1);
                    c.w.playSound(null, x, S, z, SoundEvents.BLOCK_GLASS_HIT, SoundCategory.BLOCKS, 1.4f, 0.6f);
                }
            } else if (fila + 1 > j.fila) j.fila = fila + 1;
        }
        c.t.barra(1f - (float) c.tick / limite(c));
        return (jugando > 0 && llegados >= jugando) || c.tick >= limite(c) || jugando == 0;
    }

    @Override
    public List<Jug> clasificacion(Ctx c) {
        List<Jug> l = c.jugs();
        l.sort((a, b) -> {
            if (a.llego != b.llego) return a.llego ? -1 : 1;
            if (a.llego) return Long.compare(a.tLlegada, b.tLlegada);
            if (a.fila != b.fila) return Integer.compare(b.fila, a.fila);
            return Integer.compare(a.caidas, b.caidas);
        });
        return l;
    }

    @Override
    public void limpiar(Ctx c) { }

    @Override
    public String hud(Ctx c, Jug j) {
        int resta = Math.max(0, (limite(c) - c.tick) / 20);
        return "Fila " + Math.min(FILAS, j.fila) + "/" + FILAS + " · caídas " + j.caidas + " · " + (resta / 60) + ":" + String.format("%02d", resta % 60) + (j.llego ? " · ¡LLEGASTE!" : "");
    }
}
