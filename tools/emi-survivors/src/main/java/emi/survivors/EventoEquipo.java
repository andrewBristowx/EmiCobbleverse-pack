package emi.survivors;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Los eventos de UN equipo en marcha (ver {@link Eventos}). Solo hay uno a la vez. */
final class EventoEquipo {
    private final Equipo eq;
    private Eventos actual, anterior;
    private int numero, restante;
    private Enemigo dorado;
    private final long semilla;

    EventoEquipo(Equipo eq) { this.eq = eq; this.semilla = eq.rnd.nextLong(); }

    /** El multiplicador de experiencia ahora (2 en la hora dorada). */
    double xpMul() { return actual == Eventos.HORA_DORADA ? 2.0 : 1.0; }

    /** Texto para la interfaz (vacio si no hay evento con tiempo). */
    String texto() {
        if (actual == null || restante <= 0) return "";
        return switch (actual) {
            case HORA_DORADA -> "HORA DORADA · experiencia x2";
            case LLUVIA_ESTRELLAS -> "LLUVIA DE ESTRELLAS";
            case MICHI_DORADO -> "¡MICHIALIEN DORADO! ¡Atrápalo!";
            default -> "";
        };
    }

    int segundosRestantes() { return actual == null ? 0 : Math.max(0, restante / 20); }

    void tick(int tick) {
        double seg = tick / 20.0;
        if (actual == null) {
            if (!Sentencia.toca(seg) && seg >= Eventos.segundo(numero, semilla)) iniciar(Eventos.sortear(eq.rnd, anterior));
            return;
        }
        if (--restante <= 0) { terminar(); return; }
        switch (actual) {
            case LLUVIA_ESTRELLAS -> {
                if (tick % 8 == 0) for (Run m : eq.vivos()) {
                    for (int k = 0; k < 2; k++) {
                        double a = eq.rnd.nextDouble() * Math.PI * 2, d = 2 + eq.rnd.nextDouble() * 12;
                        double x = m.pp.x + Math.cos(a) * d, z = m.pp.z + Math.sin(a) * d;
                        if (!Arena.dentro(eq, x, z, 2.5)) continue;
                        eq.gemas.add(new Gema(x, z, 4));
                        eq.w.spawnParticles(ParticleTypes.END_ROD, x, Arena.Y0 + 6, z, 6, 0.1, 3.0, 0.1, 0.0);
                    }
                }
            }
            case MICHI_DORADO -> {
                if (dorado == null || dorado.hp <= 0 || dorado.e.isRemoved()) { if (dorado != null && dorado.hp > 0) escapar(); else { actual = null; } return; }
                if (tick % 4 == 0) eq.w.spawnParticles(ParticleTypes.GLOW, dorado.e.getX(), dorado.e.getY() + 1.2, dorado.e.getZ(), 4, 0.5, 0.6, 0.5, 0.0);
            }
            default -> { }
        }
    }

    private void iniciar(Eventos e) {
        actual = e; anterior = e; numero++;
        restante = Math.max(1, e.duracionSeg * 20);
        List<ServerPlayerEntity> ps = new ArrayList<>();
        for (Run m : eq.miembros) { ServerPlayerEntity p = m.jugador(); if (p != null) { ps.add(p); Sonidos.sfx(p, Sonidos.BRILLO, 0.9f, 1.1f); } }
        Msg.title(ps, Text.literal(e.titulo).formatted(Formatting.GOLD, Formatting.BOLD), Text.literal(e.subtitulo).formatted(Formatting.YELLOW), 6, 50, 12);
        eq.avisar(Msg.say("✦ " + e.titulo + " " + e.subtitulo + "."));
        EmiSurvivors.LOG.info("[evento] {} a los {} s: {}", eq.nombres(), eq.segundos(), e);
        switch (e) {
            case REGALO_EMI -> {
                for (Run m : eq.vivos()) {
                    m.hp = m.st.hpMax;
                    eq.soltarObjeto(Objeto.sortear(eq.rnd, true), m.pp.x + 2.0, m.pp.z);
                    eq.w.spawnParticles(ParticleTypes.HEART, m.pp.x, m.pp.y + 1.5, m.pp.z, 12, 0.6, 0.6, 0.6, 0.0);
                }
                actual = null;                      // no dura: es instantaneo
            }
            case MICHI_DORADO -> {
                if (!soltarDorado()) { eq.avisar(Msg.say("El Michialien dorado no ha podido venir.")); actual = null; }
            }
            default -> { }
        }
    }

    private boolean soltarDorado() {
        Especies.E sp = Especies.michi();
        Vec3d s = eq.sitioDeAparicion(14, 20);
        if (sp == null || s == null) return false;
        double tm = eq.tick / 20.0;
        Enemigo en = Enemigo.crear(eq.w, sp, "gold=true", s, Enemigo.Tipo.MICHI, Curvas.vidaMul(tm) * 2.5, 0, 1.0, 0f, eq.rnd);
        if (en == null) return false;
        en.huye = true; en.dorado = true; en.dmg = 0; en.xp = 0; en.vel = 4.6 / 20.0;
        Colores.marcar(en, "gold");
        eq.enemigos.add(en);
        dorado = en;
        return true;
    }

    private void escapar() {
        if (dorado != null) { dorado.sinBotin = true; dorado.hp = 0; }
        eq.avisar(Msg.say("El Michialien dorado se ha escapado..."));
        actual = null; dorado = null;
    }

    /** Lo llama el equipo cuando cae el Michialien dorado: tesoro grande. */
    void alDerrotarDorado(Enemigo en, double x, double z) {
        for (int k = 0; k < 10; k++) { double a = k * Math.PI / 5; eq.gemas.add(new Gema(x + Math.cos(a) * 2.2, z + Math.sin(a) * 2.2, 14)); }
        eq.soltarObjeto(Objeto.CARAMELO, x + 1.8, z); eq.soltarObjeto(Objeto.IMAN, x - 1.8, z);
        eq.w.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, x, Arena.Y0 + 1, z, 40, 0.6, 0.8, 0.6, 0.3);
        for (Run m : eq.miembros) {
            ServerPlayerEntity p = m.jugador();
            if (p != null) { Msg.title(p, Text.literal("¡MICHIALIEN ATRAPADO!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal("Estrellas y objetos por el suelo").formatted(Formatting.YELLOW), 5, 50, 12); Sonidos.sfx(p, Sonidos.NIVEL, 0.9f, 1.2f); }
        }
        if (en.ultimo != null) en.ultimo.doradosAtrapados++;
        EmiSurvivors.LOG.info("[evento] {}: michialien dorado atrapado a los {} s", eq.nombres(), eq.segundos());
        actual = null; dorado = null;
    }

    private void terminar() {
        if (actual == Eventos.MICHI_DORADO && dorado != null && dorado.hp > 0) { escapar(); return; }
        actual = null;
    }

    /** Solo para pruebas (/survivor dev evento): lanza ese evento ya. */
    boolean forzar(Eventos e) {
        if (actual == Eventos.MICHI_DORADO && e == Eventos.MICHI_DORADO && dorado != null && dorado.hp > 0) { dorado.hp = 0; dorado.ultimo = eq.miembros.isEmpty() ? null : eq.miembros.get(0); return true; }      // segunda vez: lo atrapa, para probar el tesoro
        if (actual != null) return false;
        iniciar(e);
        return true;
    }

    void limpiar() { actual = null; dorado = null; }
}
