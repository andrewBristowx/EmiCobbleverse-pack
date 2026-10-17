package emi.survivors;

import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * La grieta de UN equipo en marcha (ver {@link Grietas}). Fases: NINGUNA (esperando a la siguiente), ABIERTA (el cuadrado morado en el suelo, que se va rellenando
 * mientras haya jugadores en pie dentro) y LEGENDARIO (ya ha salido el Pokemon legendario; la grieta no vuelve a abrirse hasta que cae).
 */
final class GrietaEquipo {
    private enum Fase { NINGUNA, ABIERTA, ENTRANDO, LEGENDARIO, VOLVER }

    private final Equipo eq;
    private Fase fase = Fase.NINGUNA;
    private int abiertas;                       // cuantas grietas se han abierto ya (para la siguiente: Grietas.tocan)
    private double x, z;
    private int carga, restante, t;
    private Entity fondo, relleno;
    Enemigo legendario;
    private String nombreLeg = "";
    private int cdBomba, retorno;
    private Especies.E especiePendiente;          // el legendario que saldra cuando el equipo ya este en el escenario del duelo

    GrietaEquipo(Equipo eq) { this.eq = eq; }

    /** El texto de la interfaz para un jugador (vacio si no hay grieta): hacia donde esta, cuanto lleva y cuanto queda. */
    String texto(Run r) {
        if (fase == Fase.VOLVER) return "DUELO GANADO · volvéis a la arena";
        if (fase == Fase.ABIERTA) {
            int pct = Math.min(100, carga * 100 / Grietas.CARGA);
            return "GRIETA " + Grietas.direccion(x - r.pp.x, z - r.pp.z) + " · " + pct + " %";
        }
        return "";
    }

    int segundosRestantes() { return fase == Fase.ABIERTA ? Math.max(0, restante / 20) : fase == Fase.VOLVER ? Math.max(0, retorno / 20) : 0; }

    /** Los datos de la barra de vida del legendario para la interfaz (null si no hay). */
    Enemigo legendarioVivo() { return fase == Fase.LEGENDARIO && legendario != null && legendario.hp > 0 ? legendario : null; }

    String nombreLegendario() { return nombreLeg; }

    void tick(int tick) {
        double seg = tick / 20.0;
        switch (fase) {
            case NINGUNA -> {
                if (!Sentencia.toca(seg) && Grietas.tocan(seg) > abiertas && (eq.jefe == null || !eq.jefe.vivo()) && Jefes.tocan(seg + 45) <= eq.jefesSalidos && eq.marea == null) abrir();
            }
            case ABIERTA -> abierta();
            case ENTRANDO -> { if (--retorno <= 0) aparecer(); }
            case VOLVER -> { if (--retorno <= 0) { eq.salirDuelo(); fase = Fase.NINGUNA; } }
            case LEGENDARIO -> {
                if (legendario == null || (legendario.hp > 0 && legendario.e.isRemoved())) { fase = Fase.VOLVER; legendario = null; retorno = 20; return; }          // desaparecio sin caer: se vuelve a la arena
                if (legendario.hp <= 0) return;                         // acaba de caer: el equipo lo retira este mismo tick y llama a alDerrotar
                if (--cdBomba <= 0) {                                   // cada 3,5 s marca una explosion bajo cada jugador en pie (la misma bomba de las personalidades) y otra al azar
                    cdBomba = 70;
                    for (Run m : eq.vivos()) eq.ponerBomba(m.pp.x, m.pp.z);
                    double a = eq.rnd.nextDouble() * Math.PI * 2, d = eq.rnd.nextDouble() * (eq.radio - 4);
                    eq.ponerBomba(eq.cx + Math.cos(a) * d, eq.cz + Math.sin(a) * d);
                }
                if (tick % 6 == 0) eq.w.spawnParticles(ParticleTypes.PORTAL, legendario.e.getX(), legendario.e.getY() + legendario.radio * 2, legendario.e.getZ(), 10, 0.6, 1.0, 0.6, 0.3);
            }
        }
    }

    private void abrir() {
        Vec3d s = eq.sitioDeAparicion(14, 22);
        if (s == null) return;
        abiertas++;
        x = s.x; z = s.z; carga = 0; t = 0; restante = Grietas.DURACION_SEG * 20;
        fase = Fase.ABIERTA;
        fondo = Marcas.crear(eq, "minecraft:purple_stained_glass", x, Marcas.Y_FONDO, z, 0, (float) Grietas.LADO, 0.05f, (float) Grietas.LADO, false);
        relleno = Marcas.crear(eq, "minecraft:magenta_carpet", x, Marcas.Y_RELLENO, z, 0, 0.2f, 1f, 0.2f, false);
        List<ServerPlayerEntity> ps = new ArrayList<>();
        for (Run m : eq.miembros) { ServerPlayerEntity p = m.jugador(); if (p != null) { ps.add(p); Sonidos.sfx(p, Sonidos.TEMBLOR, 0.9f, 0.8f); } }
        Msg.title(ps, Text.literal("¡UNA GRIETA!").formatted(Formatting.DARK_PURPLE, Formatting.BOLD), Text.literal("Quédate en el cuadrado morado").formatted(Formatting.LIGHT_PURPLE), 8, 70, 15);
        eq.avisar(Msg.say("✦ Se ha abierto una grieta (el cuadrado morado del suelo). Quedaos dentro para abrirla del todo: sale un Pokémon legendario con un gran tesoro. Se cierra en " + Grietas.DURACION_SEG + " s."));
        EmiSurvivors.LOG.info("[grieta] {} a los {} s: grieta {} en ({}, {})", eq.nombres(), eq.segundos(), abiertas, Math.round(x), Math.round(z));
    }

    private void abierta() {
        t++; restante--;
        int dentro = 0;
        for (Run m : eq.vivos()) if (Marcas.dentroDeCuadrado(m.pp.x, m.pp.z, x, z, Grietas.LADO, 0.3)) dentro++;
        carga += dentro;
        if (t % 5 == 0) {
            float f = Math.min(1f, carga / (float) Grietas.CARGA);
            Marcas.poner(relleno, x, Marcas.Y_RELLENO, z, 0, (float) Grietas.LADO * f, 1f, (float) Grietas.LADO * f, false, 5);
            // un anillo y una columna de particulas para verla de lejos
            for (int k = 0; k < 12; k++) {
                double a = k * Math.PI / 6 + t * 0.05;
                eq.w.spawnParticles(ParticleTypes.PORTAL, x + Math.cos(a) * 3.2, Arena.Y0 + 0.4, z + Math.sin(a) * 3.2, 2, 0.1, 0.4, 0.1, 0.4);
            }
            eq.w.spawnParticles(ParticleTypes.END_ROD, x, Arena.Y0 + 3, z, 3, 0.4, 3.0, 0.4, 0.01);
        }
        if (carga >= Grietas.CARGA) { emerger(); return; }
        if (restante <= 0) {
            quitarMarcas();
            fase = Fase.NINGUNA;
            eq.avisar(Msg.say("La grieta se ha cerrado. Habrá otra más adelante."));
        }
    }

    private void emerger() {
        quitarMarcas();
        String bioma = eq.bioma.id();
        List<String> ids = new ArrayList<>(Grietas.legendarios(bioma));
        java.util.Collections.shuffle(ids, eq.rnd);
        Especies.E sp = null;
        for (String id : ids) { sp = Especies.porId(id); if (sp != null) break; }
        double tm = eq.tick / 20.0;
        if (sp == null) {
            EmiSurvivors.LOG.warn("[grieta] no hay legendario disponible para {} (probados {})", bioma, ids);
            eq.avisar(Msg.say("La grieta se ha cerrado sin nada dentro."));
            fase = Fase.NINGUNA;
            return;
        }
        eq.entrarDuelo();                                    // el equipo viaja al escenario del duelo y el legendario sale alli, solo contra ellos
        especiePendiente = sp; retorno = 40; fase = Fase.ENTRANDO;                    // un segundo y medio despues (para que cargue el escenario) sale el legendario
        for (Run m : eq.miembros) { ServerPlayerEntity p = m.jugador(); if (p != null) { Msg.title(p, Text.literal("¡EL DUELO!").formatted(Formatting.DARK_PURPLE, Formatting.BOLD), Text.literal("La grieta os ha llevado a su mundo").formatted(Formatting.LIGHT_PURPLE), 5, 40, 10); Sonidos.sfx(p, Sonidos.TEMBLOR, 0.9f, 0.8f); } }
    }

    private void aparecer() {
        Especies.E sp = especiePendiente; especiePendiente = null;
        double tm = eq.tick / 20.0;
        x = eq.cx + 14; z = eq.cz;
        Enemigo en = Enemigo.crear(eq.w, sp, "", new Vec3d(x, Arena.Y0, z), Enemigo.Tipo.ELITE, 1, Curvas.danoMul(tm), 1.0, 0f, eq.rnd);
        if (en == null) {
            EmiSurvivors.LOG.warn("[grieta] no se pudo crear el legendario {}", sp.id());
            eq.avisar(Msg.say("La grieta se ha cerrado sin nada dentro."));
            eq.salirDuelo();
            fase = Fase.NINGUNA;
            return;
        }
        en.legendario = true;
        Colores.marcar(en, "light_purple");                 // contorno morado para encontrarlo entre la horda
        en.hp = en.hpMax = (float) Grietas.vida(tm, eq.miembros.size());
        en.dmg = (float) (Grietas.DANO_CONTACTO * Curvas.danoMul(tm));
        en.vel = 2.3 / 20.0;
        en.xp = 30;
        try { en.e.getPokemon().setScaleModifier(en.e.getPokemon().getScaleModifier() * 1.5f); en.radio *= 1.5; } catch (Throwable ex) { /* se queda del tamaño normal */ }
        eq.enemigos.add(en);
        legendario = en;
        nombreLeg = nombreBonito(sp.id());
        fase = Fase.LEGENDARIO;
        cdBomba = 60;
        eq.w.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, x, Arena.Y0 + 1, z, 1, 0, 0, 0, 0);
        eq.w.spawnParticles(ParticleTypes.PORTAL, x, Arena.Y0 + 1.5, z, 80, 1.5, 1.5, 1.5, 0.8);
        Sonidos.en(eq.w, x, Arena.Y0, z, Sonidos.TRUENO, SoundCategory.MASTER, 1.0f, 0.7f);
        List<ServerPlayerEntity> ps = new ArrayList<>();
        for (Run m : eq.miembros) { ServerPlayerEntity p = m.jugador(); if (p != null) ps.add(p); }
        Msg.title(ps, Text.literal("¡" + nombreLeg.toUpperCase() + "!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal("Un legendario ha salido de la grieta").formatted(Formatting.YELLOW), 5, 60, 15);
        EmiSurvivors.LOG.info("[grieta] {}: sale {} con {} de vida", eq.nombres(), sp.id(), Math.round(en.hpMax));
    }

    /** Lo llama el equipo cuando cae el legendario: mucha experiencia, un objeto, una fusion de ataques y, a los 7 s, la vuelta a la arena. */
    void alDerrotar(Enemigo en, double ex, double ez) {
        for (int k = 0; k < 8; k++) { double a = k * Math.PI / 4; eq.gemas.add(new Gema(ex + Math.cos(a) * 2.5, ez + Math.sin(a) * 2.5, 18)); }
        eq.soltarObjeto(Objeto.sortear(eq.rnd, true), ex + 1.8, ez);
        eq.soltarObjeto(Objeto.CARAMELO, ex - 1.8, ez);
        for (Run m : eq.miembros) {
            m.nivel += 3; m.pendientes += 3;                // mucha experiencia: tres niveles para todos
            m.darFusion();                                  // y la posibilidad de fusionar dos ataques
            ServerPlayerEntity p = m.jugador();
            if (p != null) {
                Msg.title(p, Text.literal("¡LEGENDARIO DERROTADO!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal(nombreLeg + " ha caído · +3 niveles, un objeto y una fusión").formatted(Formatting.YELLOW), 5, 60, 15);
                Sonidos.sfx(p, Sonidos.JEFE_DERROTADO, 0.8f, 1.2f);
            }
        }
        if (en.ultimo != null) en.ultimo.legendarios++;
        EmiSurvivors.LOG.info("[grieta] {}: {} derrotado a los {} s", eq.nombres(), nombreLeg, eq.segundos());
        fase = Fase.VOLVER; legendario = null; retorno = 160;
    }

    private void quitarMarcas() { Marcas.quitar(eq, fondo); Marcas.quitar(eq, relleno); fondo = null; relleno = null; }

    /** Al acabar la partida. */
    void limpiar() { quitarMarcas(); fase = Fase.NINGUNA; legendario = null; }

    /** Solo para pruebas (/survivor dev grieta): abre una grieta ya, aunque no toque. */
    boolean forzar() {
        if (fase == Fase.LEGENDARIO && legendario != null) { legendario.hp = 0; legendario.ultimo = eq.miembros.isEmpty() ? null : eq.miembros.get(0); return true; }      // segunda vez: lo derrota para probar el tesoro
        if (fase == Fase.ABIERTA) { carga = Grietas.CARGA; return true; }          // segunda vez con la grieta abierta: se abre del todo (y empieza el duelo)
        if (fase != Fase.NINGUNA) return false;
        int antes = abiertas;
        abrir();
        return fase == Fase.ABIERTA && abiertas > antes;
    }

    private static String nombreBonito(String id) { return id.isEmpty() ? id : Character.toUpperCase(id.charAt(0)) + id.substring(1); }
}
