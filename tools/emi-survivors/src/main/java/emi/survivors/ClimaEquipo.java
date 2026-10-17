package emi.survivors;

import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * El clima de UN equipo en marcha: cuando toca lo cambia (con un titulo para avisar), dibuja su ambiente con particulas alrededor de cada jugador (llueve, nieva, hay
 * niebla...) y, con tormenta, lanza rayos que se AVISAN en el suelo (un cuadrado rojo que se rellena durante {@link #AVISO} ticks; el rayo cae justo en el cuadrado y hace
 * dano a lo que haya dentro: a los jugadores les quita vida y a los enemigos los mata si son normales). Las marcas son las mismas que usan los jefes ({@link Marcas}).
 */
final class ClimaEquipo {
    private final Equipo eq;
    Clima actual = Clima.DESPEJADO;
    private int cambios;

    static final int AVISO = 28, LADO = 3, CADA_RAYO = 90;
    private static final double DANO_RAYO = 16, DANO_ENEMIGO = 90;

    private static final class Rayo {
        final double x, z; int t; Entity fondo, relleno;
        Rayo(double x, double z) { this.x = x; this.z = z; }
    }
    private final List<Rayo> rayos = new ArrayList<>();

    ClimaEquipo(Equipo eq) { this.eq = eq; }

    /** El nombre del clima para ponerlo en la interfaz (vacio si esta despejado). */
    String etiqueta() { return actual == Clima.DESPEJADO ? "" : actual.nombre; }

    double multiplicador(String tipo) { return actual.multiplicador(tipo); }

    double velEnemigos() { return actual.velEnemigos; }

    void tick(int tick) {
        double seg = tick / 20.0;
        if (Clima.cambiosHasta(seg) > cambios) { cambios = Clima.cambiosHasta(seg); cambiar(Clima.sortear(eq.rnd, actual)); }
        if (actual == Clima.DESPEJADO && rayos.isEmpty()) return;
        if (tick % 3 == 0) ambiente();
        if (actual == Clima.TORMENTA && tick % CADA_RAYO == 40) nuevoRayo();
        for (Iterator<Rayo> it = rayos.iterator(); it.hasNext(); ) {
            Rayo r = it.next();
            if (++r.t >= AVISO) { caer(r); quitar(r); it.remove(); }
        }
    }

    /** Solo para pruebas (/survivor dev clima): pone ese clima ahora. */
    void forzar(Clima nuevo) { cambiar(nuevo); }

    private void cambiar(Clima nuevo) {
        actual = nuevo;
        String res = nuevo.resumen();
        List<ServerPlayerEntity> ps = new ArrayList<>();
        for (Run m : eq.miembros) { ServerPlayerEntity p = m.jugador(); if (p != null) ps.add(p); }
        Text t = Text.literal(nuevo == Clima.DESPEJADO ? "Se despeja el cielo" : nuevo.aviso).formatted(Formatting.AQUA, Formatting.BOLD);
        Msg.title(ps, t, Text.literal(nuevo == Clima.DESPEJADO ? "Sin efectos" : nuevo.subtitulo()).formatted(Formatting.GRAY), 8, 70, 15);
        eq.avisar(Msg.say("☁ Clima: " + nuevo.nombre + (res.isEmpty() ? "." : ". " + res + ".")));
        EmiSurvivors.LOG.info("[clima] {} a los {} s: {}", eq.nombres(), eq.segundos(), nuevo.nombre);
    }

    /** Las particulas del ambiente alrededor de cada jugador. */
    private void ambiente() {
        for (Run m : eq.miembros) {
            if (m.estado != Run.VIVO && m.estado != Run.CAIDO) continue;
            double x = m.pp.x, y = m.pp.y, z = m.pp.z;
            switch (actual) {
                case LLUVIA, TORMENTA -> {
                    int n = actual == Clima.TORMENTA ? 1 : 0;
                    eq.w.spawnParticles(ParticleTypes.FALLING_WATER, x, y + 4, z, 22 + 10 * n, 10, 1.5, 10, 0.0);
                    eq.w.spawnParticles(ParticleTypes.RAIN, x, y + 0.1, z, 10 + 6 * n, 10, 0.0, 10, 0.0);                   // las gotas al caer al suelo (se ven bien desde arriba)
                }
                case NIEVE -> eq.w.spawnParticles(ParticleTypes.SNOWFLAKE, x, y + 4, z, 22, 10, 2.0, 10, 0.01);
                case NIEBLA -> eq.w.spawnParticles(ParticleTypes.CLOUD, x, y + 0.8, z, 8, 10, 0.5, 10, 0.0);
                case SOL -> eq.w.spawnParticles(ParticleTypes.END_ROD, x, y + 4, z, 2, 9, 3, 9, 0.0);
                default -> { }
            }
        }
    }

    private void nuevoRayo() {
        List<Run> vivos = eq.vivos();
        if (vivos.isEmpty()) return;
        Run m = vivos.get(eq.rnd.nextInt(vivos.size()));
        // cerca del jugador (a veces justo encima, para que haya que moverse), siempre dentro de la arena
        double a = eq.rnd.nextDouble() * Math.PI * 2, d = eq.rnd.nextDouble() < 0.4 ? 0.5 : 3 + eq.rnd.nextDouble() * 9;
        double x = m.pp.x + Math.cos(a) * d, z = m.pp.z + Math.sin(a) * d;
        if (!Arena.dentro(eq, x, z, 3.0)) { x = m.pp.x; z = m.pp.z; }
        Rayo r = new Rayo(x, z);
        r.fondo = Marcas.crear(eq, Marcas.CRISTAL_ROJO, x, Marcas.Y_FONDO, z, 0, LADO, 0.05f, LADO, false);
        r.relleno = Marcas.crear(eq, Marcas.ALFOMBRA_ROJA, x, Marcas.Y_RELLENO, z, 0, 0.2f, 1f, 0.2f, false);
        Marcas.poner(r.relleno, x, Marcas.Y_RELLENO, z, 0, LADO, 1f, LADO, false, AVISO);          // el cuadrado se va rellenando
        rayos.add(r);
    }

    private void caer(Rayo r) {
        double y = Arena.Y0;
        for (double h = 0; h <= 14; h += 0.7) {
            double sw = 0.4 * (h / 14.0) * (eq.rnd.nextDouble() - 0.5);
            eq.w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, r.x + sw, y + h, r.z + sw, 3, 0.1, 0.1, 0.1, 0.0);
        }
        eq.w.spawnParticles(ParticleTypes.EXPLOSION, r.x, y + 0.5, r.z, 1, 0, 0, 0, 0);
        eq.w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, r.x, y + 0.5, r.z, 30, LADO * 0.4, 0.3, LADO * 0.4, 0.3);
        Sonidos.en(eq.w, r.x, y, r.z, Sonidos.TRUENO, SoundCategory.MASTER, 1.0f, 0.9f);
        double dano = DANO_RAYO * Curvas.danoMul(eq.tick / 20.0);
        int jugadores = 0, enemigos = 0;
        for (Run m : eq.miembros) {
            if (m.estado == Run.VIVO && Marcas.dentroDeCuadrado(m.pp.x, m.pp.z, r.x, r.z, LADO, 0.3)) { m.danarJugador((float) dano); jugadores++; }
        }
        for (Enemigo en : eq.enemigos) {
            if (en.hp <= 0 || en.tipo == Enemigo.Tipo.JEFE) continue;
            if (Marcas.dentroDeCuadrado(en.e.getX(), en.e.getZ(), r.x, r.z, LADO, 0.3)) { en.hp -= (float) DANO_ENEMIGO; enemigos++; }
        }
        if (Boolean.getBoolean("emi.survivors.debug")) EmiSurvivors.LOG.info("[clima] rayo en ({}, {}): {} jugador(es) y {} enemigo(s) dentro, {} de daño", Math.round(r.x), Math.round(r.z), jugadores, enemigos, Math.round(dano));
    }

    private void quitar(Rayo r) { Marcas.quitar(eq, r.fondo); Marcas.quitar(eq, r.relleno); }

    /** Al acabar la partida: quita las marcas de los rayos que quedaran. */
    void limpiar() { for (Rayo r : rayos) quitar(r); rayos.clear(); }
}
