package emi.survivors;

import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Los tesoros pequeños y los cofres grandes de UN equipo en marcha (ver {@link Tesoros}). */
final class TesorosEquipo {
    private final Equipo eq;
    /** Cada tesoro pequeño: el enemigo-sombra invisible que reciben los golpes y el barril que se ve. */
    private final Map<Enemigo, Entity> pequenos = new HashMap<>();
    private boolean inicial;
    private int cofresAbiertos;

    // el cofre grande en marcha
    private boolean cofreActivo;
    private double cx, cz;
    private int carga, restante, tc;
    private final List<Entity> visualCofre = new ArrayList<>();
    private Entity fondoCofre, rellenoCofre;

    TesorosEquipo(Equipo eq) { this.eq = eq; }

    int cantidad() { return pequenos.size(); }

    /** El texto de la interfaz del cofre grande (vacio si no hay). */
    String texto(Run r) {
        if (!cofreActivo) return "";
        return "COFRE GRANDE " + Grietas.direccion(cx - r.pp.x, cz - r.pp.z) + " · " + Math.min(100, carga * 100 / Tesoros.CARGA_COFRE) + " %";
    }

    int segundosRestantes() { return cofreActivo ? Math.max(0, restante / 20) : 0; }

    void tick(int tick) {
        double seg = tick / 20.0;
        if (!inicial) { inicial = true; for (int k = 0; k < Tesoros.PEQUENOS; k++) nuevoPequeno(); }
        else if (tick % Tesoros.REPONER_TICKS == 0 && pequenos.size() < Tesoros.PEQUENOS) nuevoPequeno();
        if (cofreActivo) cofre();
        else if (!eq.mundoCerrado() && Tesoros.cofresQueTocan(seg) > cofresAbiertos) abrirCofre();
    }

    // ------------------------------------------------------------ tesoros pequeños

    private void nuevoPequeno() {
        Especies.E sp = Especies.porId("pichu");
        if (sp == null) sp = Especies.porId("rattata");
        if (sp == null) return;
        for (int intento = 0; intento < 12; intento++) {
            double a = eq.rnd.nextDouble() * Math.PI * 2, d = 6 + eq.rnd.nextDouble() * (Arena.RADIO - 12);
            double x = eq.ox + Math.cos(a) * d, z = eq.oz + Math.sin(a) * d;
            boolean cerca = false;
            for (Run m : eq.miembros) if (Math.hypot(m.pp.x - x, m.pp.z - z) < 9) cerca = true;
            if (cerca) continue;
            Enemigo en = Enemigo.crear(eq.w, sp, "", new Vec3d(x, Arena.Y0, z), Enemigo.Tipo.DEBIL, 1, 0, 0, 0f, eq.rnd);
            if (en == null) return;
            en.tesoro = true; en.vel = 0; en.dmg = 0; en.xp = 0;
            en.hp = en.hpMax = (float) Tesoros.vida(eq.tick / 20.0);
            en.e.setInvisible(true);
            try { en.e.getPokemon().setScaleModifier(0.06f); } catch (Throwable t) { /* se queda del tamaño normal */ }            // Cobblemon no respeta «invisible»: se hace diminuto y queda dentro del barril
            en.radio = 0.6;
            Entity barril = Marcas.crear(eq, "minecraft:barrel", x, Arena.Y0, z, 0, 0.9f, 0.9f, 0.9f, false);
            eq.enemigos.add(en);
            pequenos.put(en, barril);
            return;
        }
    }

    /** Un tesoro pequeño se ha roto (lo llama el equipo al retirar a los derrotados): experiencia y, a veces, un objeto. */
    void alRomper(Enemigo en, double x, double z) {
        Entity barril = pequenos.remove(en);
        if (barril != null) Marcas.quitar(eq, barril);
        for (int k = 0; k < Tesoros.GEMAS; k++) { double a = k * Math.PI * 2 / Tesoros.GEMAS + eq.rnd.nextDouble(); eq.gemas.add(new Gema(x + Math.cos(a) * 0.9, z + Math.sin(a) * 0.9, Tesoros.XP_GEMA)); }
        if (eq.rnd.nextDouble() < Tesoros.P_OBJETO) eq.soltarObjeto(Objeto.sortear(eq.rnd, false), x, z);
        eq.w.spawnParticles(ParticleTypes.CRIT, x, Arena.Y0 + 0.6, z, 14, 0.4, 0.4, 0.4, 0.2);
        eq.w.playSound(null, x, Arena.Y0, z, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 0.7f, 1.2f);
    }

    // ------------------------------------------------------------ cofre grande

    private void abrirCofre() {
        Vec3d s = eq.sitioDeAparicion(16, 26);
        if (s == null) return;
        cofresAbiertos = Math.max(cofresAbiertos + 1, Tesoros.cofresQueTocan(eq.tick / 20.0));
        cx = s.x; cz = s.z; carga = 0; tc = 0; restante = Tesoros.DURACION_COFRE_SEG * 20; cofreActivo = true;
        // el cofre: un cuerpo de barril alargado con la tapa dorada, sobre un cuadrado dorado en el suelo
        visualCofre.add(Marcas.crear(eq, "minecraft:barrel", cx, Arena.Y0, cz, 0, 2.4f, 1.5f, 1.6f, false));
        visualCofre.add(Marcas.crear(eq, "minecraft:gold_block", cx, Arena.Y0 + 1.5, cz, 0, 2.5f, 0.35f, 1.7f, false));
        visualCofre.add(Marcas.crear(eq, "minecraft:iron_block", cx, Arena.Y0 + 0.9, cz - 0.82, 0, 0.5f, 0.5f, 0.15f, false));
        fondoCofre = Marcas.crear(eq, "minecraft:yellow_stained_glass", cx, Marcas.Y_FONDO, cz, 0, (float) Tesoros.LADO_COFRE, 0.05f, (float) Tesoros.LADO_COFRE, false);
        rellenoCofre = Marcas.crear(eq, "minecraft:yellow_carpet", cx, Marcas.Y_RELLENO, cz, 0, 0.2f, 1f, 0.2f, false);
        List<ServerPlayerEntity> ps = new ArrayList<>();
        for (Run m : eq.miembros) { ServerPlayerEntity p = m.jugador(); if (p != null) ps.add(p); }
        Msg.title(ps, Text.literal("¡UN COFRE GRANDE!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal("Quédate junto a él para abrirlo").formatted(Formatting.YELLOW), 6, 60, 12);
        eq.avisar(Msg.say("✦ Ha aparecido un cofre grande. Quédate dentro del cuadrado dorado hasta que se llene la barra: sube un nivel y suelta un objeto. Desaparece en " + Tesoros.DURACION_COFRE_SEG + " s."));
        EmiSurvivors.LOG.info("[cofre] {} a los {} s: cofre grande en ({}, {})", eq.nombres(), eq.segundos(), Math.round(cx), Math.round(cz));
    }

    private void cofre() {
        tc++; restante--;
        List<Run> dentro = new ArrayList<>();
        for (Run m : eq.vivos()) if (Marcas.dentroDeCuadrado(m.pp.x, m.pp.z, cx, cz, Tesoros.LADO_COFRE, 0.3)) dentro.add(m);
        carga += dentro.size();
        if (tc % 5 == 0) {
            float f = Math.min(1f, carga / (float) Tesoros.CARGA_COFRE);
            Marcas.poner(rellenoCofre, cx, Marcas.Y_RELLENO, cz, 0, (float) Tesoros.LADO_COFRE * f, 1f, (float) Tesoros.LADO_COFRE * f, false, 5);
            eq.w.spawnParticles(ParticleTypes.END_ROD, cx, Arena.Y0 + 2.5, cz, 3, 0.8, 0.5, 0.8, 0.01);
        }
        if (carga >= Tesoros.CARGA_COFRE) { abierto(dentro); return; }
        if (restante <= 0) { quitarCofre(); eq.avisar(Msg.say("El cofre grande ha desaparecido. Saldrá otro más adelante.")); }
    }

    private void abierto(List<Run> dentro) {
        eq.w.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, cx, Arena.Y0 + 1.5, cz, 50, 1.0, 1.0, 1.0, 0.3);
        eq.w.playSound(null, cx, Arena.Y0, cz, SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 1.0f, 1.0f);
        eq.soltarObjeto(Objeto.sortear(eq.rnd, true), cx, cz + 2.2);
        for (Run m : dentro) {
            m.nivel++; m.pendientes++;                       // un nivel: sale la pantalla de elegir carta
            ServerPlayerEntity p = m.jugador();
            if (p != null) { Msg.title(p, Text.literal("¡COFRE ABIERTO!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal("+1 nivel y un objeto en el suelo").formatted(Formatting.YELLOW), 5, 50, 12); Sonidos.sfx(p, Sonidos.NIVEL, 0.9f, 1.1f); }
        }
        EmiSurvivors.LOG.info("[cofre] {} abre el cofre a los {} s ({} jugador(es) dentro)", eq.nombres(), eq.segundos(), dentro.size());
        quitarCofre();
    }

    private void quitarCofre() {
        for (Entity e : visualCofre) Marcas.quitar(eq, e);
        visualCofre.clear();
        Marcas.quitar(eq, fondoCofre); Marcas.quitar(eq, rellenoCofre);
        fondoCofre = null; rellenoCofre = null;
        cofreActivo = false;
    }

    /** Solo para pruebas (/survivor dev cofre): saca un cofre grande ya. */
    boolean forzarCofre() { if (cofreActivo) return false; abrirCofre(); return cofreActivo; }

    /** Al acabar la partida o cambiar de mundo: quita todo (los barriles se quitan solos con las marcas y los enemigos-sombra con la lista de enemigos). */
    void limpiar() { pequenos.clear(); quitarCofre(); inicial = false; }
}
