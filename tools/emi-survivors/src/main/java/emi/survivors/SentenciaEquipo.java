package emi.survivors;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** La Sentencia de UN equipo en marcha (ver {@link Sentencia}). */
final class SentenciaEquipo {
    private final Equipo eq;
    private boolean iniciada, superada;
    private int t, cd;

    SentenciaEquipo(Equipo eq) { this.eq = eq; }

    boolean activa() { return iniciada && !superada; }
    boolean superada() { return superada; }

    /** Segundos que quedan de Sentencia (0 si no esta en marcha). */
    int segundosRestantes() { return activa() ? Math.max(0, Sentencia.DURACION_SEG - t / 20) : 0; }

    String texto() { return activa() ? "LA SENTENCIA · sobrevive" : ""; }

    void tick(int tick) {
        if (superada) return;
        if (!iniciada) {
            if (Sentencia.toca(tick / 20.0)) empezar();
            return;
        }
        t++;
        double dentro = t / 20.0;
        if (--cd <= 0) {
            cd = Sentencia.ticksEntreTandas(dentro);
            for (Run m : eq.vivos()) eq.ponerBomba(m.pp.x, m.pp.z);                       // una bajo cada jugador en pie: hay que moverse
            List<Run> vivos = eq.vivos();
            for (int k = 0; k < Sentencia.extrasPorTanda(dentro) && !vivos.isEmpty(); k++) {
                Run m = vivos.get(eq.rnd.nextInt(vivos.size()));
                double a = eq.rnd.nextDouble() * Math.PI * 2, d = 3 + eq.rnd.nextDouble() * 10;
                double x = m.pp.x + Math.cos(a) * d, z = m.pp.z + Math.sin(a) * d;
                if (Arena.dentro(eq, x, z, 3.0)) eq.ponerBomba(x, z);
            }
        }
        if (t >= Sentencia.DURACION_SEG * 20) superar();
    }

    /** Empieza la cuenta atras. */
    void empezar() {
        if (iniciada) return;
        iniciada = true; t = 0; cd = 20;
        List<ServerPlayerEntity> ps = new ArrayList<>();
        for (Run m : eq.miembros) { ServerPlayerEntity p = m.jugador(); if (p != null) { ps.add(p); Sonidos.sfx(p, Sonidos.JEFE_APARECE, 0.9f, 0.7f); } }
        Msg.title(ps, Text.literal("¡LA SENTENCIA!").formatted(Formatting.DARK_RED, Formatting.BOLD), Text.literal("Sobrevive " + Sentencia.DURACION_SEG + " segundos").formatted(Formatting.RED), 8, 80, 15);
        eq.avisar(Msg.say("☠ ¡LA SENTENCIA! Lloverán explosiones cada vez más seguidas y vendrán más enemigos. Sobrevivid " + Sentencia.DURACION_SEG + " segundos para ganar la partida."));
        EmiSurvivors.LOG.info("[sentencia] {} empieza a los {} s", eq.nombres(), eq.segundos());
    }

    private void superar() {
        superada = true;
        for (Run m : eq.miembros) {
            ServerPlayerEntity p = m.jugador();
            if (p != null) { Msg.title(p, Text.literal("¡SENTENCIA SUPERADA!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal("¡Habéis ganado!").formatted(Formatting.YELLOW), 5, 80, 20); Sonidos.sfx(p, Sonidos.JEFE_DERROTADO, 1.0f, 1.0f); }
        }
        EmiSurvivors.LOG.info("[sentencia] {} la supera a los {} s", eq.nombres(), eq.segundos());
        // la partida la cierra el equipo al final de su tick (no a mitad de el)
    }
}
