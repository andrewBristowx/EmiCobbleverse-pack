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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * «Camino correcto»: un puente de baldosas de cristal, en cada fila tres baldosas y solo UNA aguanta (el camino cambia en cada partida). Si pisas una falsa se rompe y
 * caes: reapareces al principio. Las baldosas rotas se quedan rotas, asi que los que van detras aprenden de los que caen. Gana quien llega antes al otro lado.
 */
final class MiniCamino implements Mini {
    static final int ZC = 300;                  // centro de la arena (z)
    static final int FILAS = 14, ANCHO = 3;     // 14 filas de baldosas de 3x3, 3 baldosas por fila
    static final int LIMITE_S = 120, EXTRA_S = 40;
    static final int Z0 = ZC - 4;               // la primera columna de baldosas empieza en este z (9 de ancho: 296..304)

    private final int[] bueno = new int[FILAS];                 // la columna que aguanta en cada fila
    private final boolean[][] roto = new boolean[FILAS][3];
    private final long[][] romper = new long[FILAS][3];         // tick en que se rompe (0 = no se rompe)
    private int primero = -1;                                   // tick en que llego el primero (-1 = nadie)

    @Override public String id() { return "camino"; }
    @Override public String nombre() { return "Camino correcto"; }
    @Override public String regla() { return "En cada fila solo una baldosa aguanta. Si pisas una falsa caes y vuelves a empezar. ¡Llega primero al otro lado!"; }

    private static int baldosaX(int fila) { return fila * ANCHO; }

    @Override
    public void construir(ServerWorld w) {
        BlockPos marca = new BlockPos(0, -64, ZC);
        if (w.getBlockState(marca).isOf(Blocks.CRYING_OBSIDIAN)) return;
        long t0 = System.nanoTime();
        int S = Minijuegos.SUELO;
        // la plataforma de salida (11 de ancho) y la de llegada
        for (int x = -12; x <= -1; x++) for (int z = ZC - 5; z <= ZC + 5; z++) { Obra.poner(w, x, S, z, ((x + z) & 1) == 0 ? "light_blue_concrete" : "white_concrete"); Obra.poner(w, x, S - 1, z, "white_concrete"); }
        int fx = FILAS * ANCHO;
        for (int x = fx; x <= fx + 11; x++) for (int z = ZC - 5; z <= ZC + 5; z++) { Obra.poner(w, x, S, z, ((x + z) & 1) == 0 ? "lime_concrete" : "yellow_concrete"); Obra.poner(w, x, S - 1, z, "white_concrete"); }
        // arco de meta
        for (int y = 1; y <= 5; y++) { Obra.poner(w, fx + 1, S + y, ZC - 5, "gold_block"); Obra.poner(w, fx + 1, S + y, ZC + 5, "gold_block"); }
        for (int z = ZC - 5; z <= ZC + 5; z++) Obra.poner(w, fx + 1, S + 6, z, ((z & 1) == 0) ? "gold_block" : "yellow_concrete");
        Disp.texto(w, "{\"text\":\"META\",\"color\":\"gold\",\"bold\":true}", fx + 1.5, S + 8.2, ZC + 0.5, 6f, false);
        Disp.texto(w, "{\"text\":\"SALIDA\",\"color\":\"aqua\",\"bold\":true}", -6.5, S + 4.5, ZC + 0.5, 4f, false);
        Obra.poner(w, 0, -64, ZC, "crying_obsidian");
        EmiFall.LOG.info("Arena «camino correcto» construida en {} ms", (System.nanoTime() - t0) / 1_000_000);
    }

    /** Pone las baldosas (todas) y elige el camino de esta partida. */
    private void ponerBaldosas(ServerWorld w, Random rnd) {
        int S = Minijuegos.SUELO;
        int c = rnd.nextInt(3);
        for (int f = 0; f < FILAS; f++) {
            if (f > 0) c = Math.max(0, Math.min(2, c + rnd.nextInt(3) - 1));
            bueno[f] = c;
            for (int k = 0; k < 3; k++) { roto[f][k] = false; romper[f][k] = 0; }
            for (int k = 0; k < 3; k++) rellenar(w, f, k, S);
        }
    }

    private static void rellenar(ServerWorld w, int fila, int col, int S) {
        String bloque = ((fila + col) & 1) == 0 ? "light_blue_stained_glass" : "cyan_stained_glass";
        for (int dx = 0; dx < 3; dx++) for (int dz = 0; dz < 3; dz++) Obra.poner(w, baldosaX(fila) + dx, S, Z0 + col * 3 + dz, bloque);
    }

    private static void quitarBaldosa(ServerWorld w, int fila, int col, int S) {
        for (int dx = 0; dx < 3; dx++) for (int dz = 0; dz < 3; dz++) Obra.poner(w, baldosaX(fila) + dx, S, Z0 + col * 3 + dz, "air");
        w.spawnParticles(ParticleTypes.CLOUD, baldosaX(fila) + 1.5, S + 1, Z0 + col * 3 + 1.5, 14, 1.0, 0.2, 1.0, 0.05);
        w.playSound(null, baldosaX(fila) + 1.5, S, Z0 + col * 3 + 1.5, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 1.4f, 0.9f);
    }

    @Override
    public void empezar(Ctx c) {
        primero = -1;
        ponerBaldosas(c.w, c.rnd);
        int i = 0;
        for (Jug j : c.jugs()) {
            int n = i++;
            c.colocar(j, -10 + (n % 3) * 2 + 0.5, Minijuegos.SUELO + 1, ZC - 4 + (n / 3) * 2 + 0.5, -90f);
        }
    }

    @Override public void liberar(Ctx c) { }

    private int limite(Ctx c) { return primero < 0 ? LIMITE_S * 20 : Math.min(LIMITE_S * 20, primero + EXTRA_S * 20); }

    @Override
    public boolean tick(Ctx c) {
        int S = Minijuegos.SUELO;
        // las baldosas que tocaba romper
        for (int f = 0; f < FILAS; f++) for (int k = 0; k < 3; k++) if (romper[f][k] > 0 && c.tick >= romper[f][k]) { romper[f][k] = 0; roto[f][k] = true; quitarBaldosa(c.w, f, k, S); }
        int llegados = 0, jugando = 0;
        for (Jug j : c.jugs()) {
            ServerPlayerEntity p = c.pl(j);
            if (p == null) continue;
            jugando++;
            if (j.llego) { llegados++; continue; }
            double x = p.getX(), y = p.getY(), z = p.getZ();
            if (y < S - 8) {                                                       // cayo al vacio: otra vez al principio
                j.caidas++;
                int n = Math.floorMod(j.id.hashCode(), 6);
                c.reaparecer(j, -10 + (n % 3) * 2 + 0.5, S + 1, ZC - 4 + n * 1.5 / 1 + 0.5, -90f);
                p.sendMessage(Text.literal("¡Has caído! Vuelves a la salida.").formatted(Formatting.RED), true);
                c.sonido(SoundEvents.ENTITY_PLAYER_HURT, 0f, 1f);
                p.playSoundToPlayer(SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.MASTER, 0.4f, 1.6f);
                continue;
            }
            int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
            if (y > S + 2.2 || y < S + 0.5) continue;                              // en el aire: no cuenta el suelo de debajo
            if (bx >= FILAS * ANCHO) {                                             // la meta
                j.llego = true; j.tLlegada = c.tick; j.fila = FILAS;
                if (primero < 0) { primero = c.tick; c.avisar(Msg.say(Text.literal(j.nombre + " ha llegado primero. Los demás tenéis " + EXTRA_S + " segundos.").formatted(Formatting.GOLD))); }
                int puesto = 1; for (Jug o : c.jugs()) if (o != j && o.llego) puesto++;
                p.sendMessage(Text.literal("¡Has llegado el " + puesto + "º!").formatted(Formatting.GREEN, Formatting.BOLD), true);
                p.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1f, 1.2f);
                continue;
            }
            if (bx < 0 || bz < Z0 || bz >= Z0 + 9) continue;
            int fila = bx / ANCHO, col = (bz - Z0) / 3;
            if (fila >= FILAS) continue;
            if (col != bueno[fila] && !roto[fila][col] && romper[fila][col] == 0) {        // baldosa falsa: se va a romper
                romper[fila][col] = c.tick + 7;
                c.w.spawnParticles(ParticleTypes.CRIT, baldosaX(fila) + 1.5, S + 1.1, Z0 + col * 3 + 1.5, 8, 1.0, 0.1, 1.0, 0.1);
                c.w.playSound(null, x, S, z, SoundEvents.BLOCK_GLASS_HIT, SoundCategory.BLOCKS, 1.2f, 0.7f);
            } else if (col == bueno[fila] && fila + 1 > j.fila) j.fila = fila + 1;
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
