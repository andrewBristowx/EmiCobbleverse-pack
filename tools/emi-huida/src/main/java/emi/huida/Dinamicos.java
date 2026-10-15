package emi.huida;

import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Los obstaculos que SE MUEVEN del nivel 2 (el dueño pidio obstaculos distintos y mas dificiles que los del nivel 1):
 * <ul>
 *   <li><b>F, aspas giratorias</b>: un pilar con una barra de hueso gigante (con una calabaza encendida en cada punta) que gira a su alrededor, a ras de suelo
 *       (el pilar esta en el centro y la barra barre todo el ancho del camino); hay que saltarla cuando llega (como la barra de fuego de un Mario). Su fase depende
 *       solo del tiempo que lleva el jugador corriendo, asi que el ritmo es igual en cada partida y se puede aprender.</li>
 *   <li><b>K, calabazas rodantes</b>: mientras cruzas su zona, bajan hacia ti oleadas de calabazas gigantes por tres carriles (siempre hay un carril libre): cambia de carril o salta por
 *       encima (miden 1,7: hay que saltar en el momento justo). Si te tocan te arrollan (un error).</li>
 * </ul>
 * Son entidades {@code block_display} (las mismas del nivel 2, {@link EscenasCalabaza#bloque}) que el servidor mueve cada tick; el choque se comprueba a mano
 * (distancias), como el de los obstaculos de bloques. Solo existen mientras el jugador esta cerca de su zona y se quitan al pasar, al ser alcanzado y al terminar.
 */
final class Dinamicos {
    private Dinamicos() {}

    static final double VEL_CALABAZA = 8.0;            // bloques/s con los que rueda cada calabaza hacia el jugador (nivel 3: VEL_CAJA)
    static final double VEL_CAJA = 9.0;
    static double velK() { return Nivel.cosmos() ? VEL_CAJA : VEL_CALABAZA; }
    /** Los bloques de cada nivel: huesos y calabazas encendidas (nivel 2) o barras de cuarzo, luces marinas y cajas de regalo de colores (nivel 3). */
    static String bloqueBarra() { return Nivel.cosmos() ? "minecraft:quartz_pillar[axis=x]" : "minecraft:bone_block[axis=x]"; }
    static String bloquePunta() { return Nivel.cosmos() ? "minecraft:sea_lantern" : "minecraft:jack_o_lantern[facing=south]"; }
    private static final String[] CAJAS = {"minecraft:pink_glazed_terracotta", "minecraft:light_blue_glazed_terracotta", "minecraft:magenta_glazed_terracotta", "minecraft:purple_glazed_terracotta"};
    static String bloqueK(int i) { return Nivel.cosmos() ? CAJAS[i % CAJAS.length] : "minecraft:jack_o_lantern[facing=south]"; }
    static final int PASO_METEORITO_1 = 16, PASO_METEORITO_2 = 11;       // ticks entre un meteorito y otro en cada zona de meteoritos del nivel 3
    static final int AVISO_METEORITO = 24;                               // ticks que se ve el aviso en el suelo antes de que caiga
    static final float TAM_CALABAZA = 1.7f;            // arista de la calabaza rodante (hay que saltar ~1,7 o cambiar de carril)
    static final double[] CARRILES = {-2.1, 0.0, 2.1};
    static final float BARRA_ALTO = 0.7f;               // la barra gira a ras de suelo y mide 0,7 de alto: se salta (no se puede pasar por debajo)
    
    /** Un obstaculo movil de la copia de un jugador. */
    static final class Din {
        final Nivel.Mod mod; final char tipo; final double s0, fin; final int idx;
        boolean activo; long t0; long frenado;
        // aspas
        Entity barra, puntaA, puntaB; double fase0, omega; int semi;
        // calabazas
        final List<Cal> cals = new ArrayList<>(); int lanzadas; int[] libres = {}; int paso = 26;
        // meteoritos
        final List<Met> mets = new ArrayList<>(); long proxMet; int pasoMet = 16;      // carril libre de cada oleada (0 = izquierda, 1 = centro, 2 = derecha) y ticks entre una y otra
        Din(Nivel.Mod m, int idx) { mod = m; tipo = m.tipo(); s0 = m.s0(); fin = s0 + m.filas(); this.idx = idx; }
    }

    static final class Cal { Entity e; double s, v, rodado; }
    /** Un meteorito: apunta a un sitio del suelo ({@code s}, {@code v}), cae en el tick {@code impacto}. */
    static final class Met { Entity e; double s, v; long impacto; Vec3d suelo; }

    /** Los obstaculos moviles del nivel cargado, para la copia de un jugador (vacio en el nivel 1). */
    static List<Din> crear() {
        List<Din> l = new ArrayList<>();
        int i = 0, nK = 0, nF = 0, nM = 0;
        for (Nivel.Mod m : Nivel.MODS) if (m.tipo() == 'F' || m.tipo() == 'K' || m.tipo() == 'M') {
            Din d = new Din(m, i++);
            d.omega = (1.7 + 0.3 * (d.idx % 3)) * (d.idx % 2 == 0 ? 1 : -1);          // rad/s (una vuelta cada 3,7 a 2,9 s), en sentidos alternos
            if (Nivel.cosmos() && d.tipo == 'F') { double[] om = {1.9, 2.4, -2.9}; d.omega = om[Math.min(nF, om.length - 1)]; nF++; }       // nivel 3: tres aspas cada vez mas rapidas
            d.fase0 = d.idx * 1.3;
            if (d.tipo == 'M') { d.pasoMet = nM++ == 0 ? PASO_METEORITO_1 : PASO_METEORITO_2; }
            if (d.tipo == 'K' && Nivel.cosmos()) {      // nivel 3: mas oleadas y mas rapidas
                if (nK++ == 0) { d.libres = new int[]{1, 0, 1, 2}; d.paso = 24; } else { d.libres = new int[]{1, 2, 1, 0, 1}; d.paso = 20; }
            } else if (d.tipo == 'K') {                       // el primer tramo de calabazas: 3 oleadas; el ultimo: 4 y mas seguidas (el carril libre cambia siempre al vecino: 2,1 bloques)
                if (nK++ == 0) { d.libres = new int[]{1, 0, 1}; d.paso = 26; } else { d.libres = new int[]{1, 2, 1, 0}; d.paso = 22; }
            }
            l.add(d);
        }
        return l;
    }

    /** Mueve los obstaculos moviles que estan cerca del jugador y devuelve el texto del error si alguno lo ha tocado (null si no). */
    static String tick(ServerWorld w, Huida.J j, ServerPlayerEntity p, long now, double v) {
        String golpe = null;
        for (Din d : j.dins) {
            boolean cerca = j.s >= d.s0 - (d.tipo == 'K' ? 14 : d.tipo == 'M' ? 10 : 34) && j.s <= d.fin + (d.tipo == 'K' ? 3 : 1)
                    && j.sPerseguidor + Nivel.DEF.frente() < (d.tipo == 'K' || d.tipo == 'M' ? d.fin : d.s0 + 1);       // cuando el perseguidor llega a su zona los destruye (los atraviesa)
            if (!cerca) { if (d.activo) apagar(d); continue; }
            if (!d.activo) activar(w, j, d, now);
            String g = d.tipo == 'F' ? aspas(w, j, p, d, now) : d.tipo == 'M' ? meteoritos(w, j, p, d, now, v) : rodantes(w, j, p, d, now, v);
            if (g != null) golpe = g;
        }
        return golpe;
    }

    private static void activar(ServerWorld w, Huida.J j, Din d, long now) {
        d.activo = true; d.t0 = now; d.lanzadas = 0;
        if (d.tipo == 'F') {
            Vec3d hub = Escenas.P(j.hueco, d.s0 + Nivel.F_HUB, 0, 0.0);
            d.barra = EscenasCalabaza.bloque(w, hub, bloqueBarra());
            d.puntaA = EscenasCalabaza.bloque(w, hub, bloquePunta());
            d.puntaB = EscenasCalabaza.bloque(w, hub, bloquePunta());
        }
        d.proxMet = now + 6;
    }

    static void apagar(Din d) {
        d.activo = false;
        for (Entity e : new Entity[]{d.barra, d.puntaA, d.puntaB}) if (e != null && !e.isRemoved()) e.discard();
        d.barra = d.puntaA = d.puntaB = null;
        for (Cal c : d.cals) if (c.e != null && !c.e.isRemoved()) c.e.discard();
        d.cals.clear();
        for (Met m : d.mets) if (m.e != null && !m.e.isRemoved()) m.e.discard();
        d.mets.clear();
    }

    static void quitar(Huida.J j) { for (Din d : j.dins) apagar(d); }

    // ---------------------------------------------------------------- aspas giratorias

    private static String aspas(ServerWorld w, Huida.J j, ServerPlayerEntity p, Din d, long now) {
        Vec3d hub = Escenas.P(j.hueco, d.s0 + Nivel.F_HUB, 0, 0.0);
        double ang = d.fase0 + d.omega * (now - j.inicioTick) / 20.0;
        Quaternionf rot = new Quaternionf().rotateY((float) ang);
        double r = Nivel.F_RADIO;
        EscenasCalabaza.poner(d.barra, hub, new Vector3f(), rot, (float) (2 * r), BARRA_ALTO, 0.8f);
        EscenasCalabaza.poner(d.puntaA, hub, new Vector3f((float) (r - 0.45), 0f, 0f), rot, 1.1f, 1.1f, 1.1f);
        EscenasCalabaza.poner(d.puntaB, hub, new Vector3f((float) -(r - 0.45), 0f, 0f), rot, 1.1f, 1.1f, 1.1f);
        EscenasCalabaza.sync(d.barra); EscenasCalabaza.sync(d.puntaA); EscenasCalabaza.sync(d.puntaB);
        // un silbido cada vez que la barra pasa por la direccion del camino
        int semi = (int) Math.floor(ang / Math.PI);
        if (semi != d.semi) { d.semi = semi; w.playSound(null, hub.x, hub.y, hub.z, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, net.minecraft.sound.SoundCategory.NEUTRAL, 0.5f, 0.55f); }
        if (now % 6 == 0) {
            double a = ang;
            w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, hub.x + Math.cos(a) * r, hub.y + 0.9, hub.z - Math.sin(a) * r, 1, 0.05, 0.05, 0.05, 0.0);
            w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, hub.x - Math.cos(a) * r, hub.y + 0.9, hub.z + Math.sin(a) * r, 1, 0.05, 0.05, 0.05, 0.0);
        }
        // choque: el jugador (a menos de 0,6 de la barra, o de 0,8 en las puntas) y con los pies por debajo de lo alto de la barra (0,7 sobre el suelo)
        double rx = p.getX() - hub.x, rz = p.getZ() - hub.z;
        double along = rx * Math.cos(ang) - rz * Math.sin(ang), perp = rx * Math.sin(ang) + rz * Math.cos(ang);
        if (Huida.DEBUG && now % 2 == 0 && Math.abs(along) < r + 2.5 && Math.abs(perp) < 6) EmiHuida.LOG.info("dbgF along={} perp={} y={}", String.format("%.2f", along), String.format("%.2f", perp), String.format("%.2f", p.getY() - Nivel.Y0));
        if (now < d.frenado || p.getY() > Nivel.Y0 + BARRA_ALTO + 0.05) return null;
        double lim = Math.abs(along) > r - 1.2 ? 0.8 : 0.6;
        if (Math.abs(along) <= r + 0.3 && Math.abs(perp) <= lim) { d.frenado = now + 20; return Nivel.cosmos() ? "¡Te golpeó la barra de estrellas!" : "¡Te golpeó el hueso giratorio!"; }
        return null;
    }

    // ---------------------------------------------------------------- calabazas rodantes

    private static String rodantes(ServerWorld w, Huida.J j, ServerPlayerEntity p, Din d, long now, double vJug) {
        int h = j.hueco;
        // nuevas oleadas: dos calabazas en dos carriles (el tercero queda libre), a ~30 bloques por delante del jugador
        if (d.lanzadas < d.libres.length && now >= d.t0 + (long) d.paso * d.lanzadas && j.s < d.fin - 3) {
            int libre = d.libres[d.lanzadas];
            for (int k = 0; k < 3; k++) {
                if (k == libre) continue;
                Cal c = new Cal();
                c.s = j.s + 30; c.v = CARRILES[k];
                c.e = EscenasCalabaza.bloque(w, Escenas.P(h, c.s, c.v, 0), bloqueK(d.lanzadas * 3 + k));
                d.cals.add(c);
            }
            d.lanzadas++;
            if (Nivel.cosmos()) w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, net.minecraft.sound.SoundCategory.NEUTRAL, 1.2f, 0.6f);
            else w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, net.minecraft.sound.SoundCategory.NEUTRAL, 0.35f, 1.5f);
        }
        if (Huida.DEBUG && now % 2 == 0) {      // para el piloto automatico: distancia a la oleada mas cercana por delante y su carril libre
            double minDs = 1e9; double suma = 0; int n = 0;
            for (Cal c : d.cals) { double ds = c.s - j.s; if (ds > -1.6 && ds < minDs) minDs = ds; }
            if (minDs < 1e8) { for (Cal c : d.cals) if (Math.abs(c.s - j.s - minDs) < 0.6) { suma += c.v; n++; } }
            if (n > 0) EmiHuida.LOG.info("dbgK ds={} libre={}", String.format("%.1f", minDs), String.format("%.1f", -suma));
        }
        String golpe = null;
        float rad = TAM_CALABAZA / 2f;
        for (java.util.Iterator<Cal> it = d.cals.iterator(); it.hasNext(); ) {
            Cal c = it.next();
            c.s -= velK() / 20.0; c.rodado += velK() / 20.0;
            if (c.s < j.s - 8 || c.s < d.s0 - 30 || c.e == null || c.e.isRemoved()) { if (c.e != null && !c.e.isRemoved()) c.e.discard(); it.remove(); continue; }
            Vec3d base = Escenas.P(h, c.s, c.v, 0);
            double yawRad = Math.toRadians(Nivel.rumbo(c.s));
            Quaternionf rot = new Quaternionf().rotateY((float) -yawRad).rotateX((float) (-c.rodado / rad));        // rueda hacia el jugador
            EscenasCalabaza.poner(c.e, base.add(0, rad, 0), new Vector3f(0f, -rad, 0f), rot, TAM_CALABAZA, TAM_CALABAZA, TAM_CALABAZA);
            EscenasCalabaza.sync(c.e);
            if (now % 5 == 0) {
                double dist = Math.abs(c.s - j.s);
                if (dist < 22) Sonidos.sfx(p, Nivel.cosmos() ? SoundEvents.BLOCK_AMETHYST_BLOCK_STEP : SoundEvents.ENTITY_IRON_GOLEM_STEP, (float) Math.max(0.15, 0.9 - dist / 30.0), Nivel.cosmos() ? 0.7f : 0.55f);
                w.spawnParticles(ParticleTypes.POOF, base.x, base.y + 0.1, base.z, 1, 0.3, 0.05, 0.3, 0.0);
            }
            // choque: misma zona del camino (0,85 + 0,35 de largo, 0,85 + 0,3 de ancho) y pies por debajo de la parte de arriba de la calabaza
            if (now >= d.frenado && Math.abs(c.s - j.s) < rad + 0.35 && Math.abs(c.v - vJug) < rad + 0.3 && p.getY() < Nivel.Y0 + TAM_CALABAZA - 0.1) {
                d.frenado = now + 20;
                golpe = Nivel.cosmos() ? "¡Te arrolló una caja de regalo!" : "¡Te arrolló una calabaza!";
                w.spawnParticles(ParticleTypes.POOF, base.x, base.y + 1, base.z, 14, 0.6, 0.5, 0.6, 0.05);
                w.playSound(null, base.x, base.y, base.z, SoundEvents.BLOCK_WOOD_BREAK, net.minecraft.sound.SoundCategory.NEUTRAL, 1.2f, 0.7f);
                c.e.discard(); it.remove();
            }
        }
        return golpe;
    }

    // ---------------------------------------------------------------- lluvia de meteoritos (solo nivel 3)

    private static final java.util.Random RND = new java.util.Random();
    private static final net.minecraft.particle.DustParticleEffect AVISO = new net.minecraft.particle.DustParticleEffect(new Vector3f(1f, 0.35f, 0.75f), 1.4f);

    /**
     * Cada pocos ticks aparece en el suelo, por delante del jugador, un anillo rosa que se cierra (el aviso: ~1,2 s) y despues cae un meteorito de roca
     * ardiente justo ahi. Si estas dentro del anillo cuando cae («¡Te cayo un meteorito!») es un error; saltando en ese momento tambien te libras.
     * Apunta a un sitio alrededor de donde estaras (por delante y a los lados), no siempre justo encima.
     */
    private static String meteoritos(ServerWorld w, Huida.J j, ServerPlayerEntity p, Din d, long now, double vJug) {
        int h = j.hueco;
        if (now >= d.proxMet && d.mets.size() < 5 && j.s >= d.s0 - 4 && j.s < d.fin - 7) {
            d.proxMet = now + d.pasoMet;
            double sT = Math.max(d.s0 + 1.5, Math.min(d.fin - 1.5, j.s + 5.8 * AVISO_METEORITO / 20.0 + (RND.nextDouble() * 2.4 - 1.2)));
            double lim = Math.max(1.0, Nivel.mediaAncho(sT) - 1.3);
            Met m = new Met();
            m.s = sT; m.v = Math.max(-lim, Math.min(lim, vJug + (RND.nextDouble() * 4.8 - 2.4)));
            m.impacto = now + AVISO_METEORITO;
            m.suelo = Escenas.P(h, m.s, m.v, 0);
            d.mets.add(m);
            Sonidos.sfx(p, SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 0.5f, 1.1f);
        }
        String golpe = null;
        for (java.util.Iterator<Met> it = d.mets.iterator(); it.hasNext(); ) {
            Met m = it.next();
            long falta = m.impacto - now;
            if (falta > 0) {
                if (now % 2 == 0) {                    // el aviso: un anillo rosa en el suelo que se va cerrando
                    double r = 0.9 + 1.0 * falta / (double) AVISO_METEORITO;
                    for (int k = 0; k < 12; k++) {
                        double a = now * 0.25 + k * Math.PI / 6;
                        w.spawnParticles(AVISO, m.suelo.x + Math.cos(a) * r, m.suelo.y + 0.12, m.suelo.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
                    }
                    w.spawnParticles(ParticleTypes.END_ROD, m.suelo.x, m.suelo.y + 0.2 + (AVISO_METEORITO - falta) * 0.05, m.suelo.z, 1, 0.15, 0.05, 0.15, 0.0);
                }
                if (falta % 6 == 0) Sonidos.sfx(p, SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 0.4f, 1.0f + 0.7f * (1f - falta / (float) AVISO_METEORITO));
                if (falta <= 9) {                      // y en los ultimos 9 ticks cae del cielo
                    if (m.e == null) m.e = EscenasCalabaza.bloque(w, m.suelo.add(0, 18, 0), "minecraft:magma_block");
                    double u = 1 - falta / 9.0, y = 18 * (1 - u * u);
                    Quaternionf rot = new Quaternionf().rotateY((float) (now * 0.4)).rotateX((float) (now * 0.3));
                    EscenasCalabaza.poner(m.e, m.suelo.add(0, y, 0), new Vector3f(0f, -0.85f, 0f), rot, 1.7f, 1.7f, 1.7f);
                    EscenasCalabaza.sync(m.e);
                    w.spawnParticles(ParticleTypes.FLAME, m.suelo.x, m.suelo.y + y + 1.2, m.suelo.z, 3, 0.3, 0.3, 0.3, 0.02);
                    w.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, m.suelo.x, m.suelo.y + y + 2.0, m.suelo.z, 1, 0.2, 0.2, 0.2, 0.0);
                    if (falta == 9) w.playSound(null, m.suelo.x, m.suelo.y + 6, m.suelo.z, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, net.minecraft.sound.SoundCategory.NEUTRAL, 1.2f, 0.5f);
                }
                if (Huida.DEBUG && now % 2 == 0 && falta <= 20) EmiHuida.LOG.info("dbgM falta={} ds={} dv={}", falta, String.format("%.1f", m.s - j.s), String.format("%.1f", m.v - vJug));
                continue;
            }
            // impacto
            if (m.e != null && !m.e.isRemoved()) m.e.discard();
            w.spawnParticles(ParticleTypes.EXPLOSION, m.suelo.x, m.suelo.y + 0.5, m.suelo.z, 1, 0, 0, 0, 0);
            w.spawnParticles(ParticleTypes.FLAME, m.suelo.x, m.suelo.y + 0.4, m.suelo.z, 14, 0.7, 0.2, 0.7, 0.08);
            w.spawnParticles(ParticleTypes.POOF, m.suelo.x, m.suelo.y + 0.3, m.suelo.z, 16, 0.8, 0.1, 0.8, 0.06);
            w.spawnParticles(ParticleTypes.END_ROD, m.suelo.x, m.suelo.y + 0.6, m.suelo.z, 12, 0.7, 0.5, 0.7, 0.12);
            w.playSound(null, m.suelo.x, m.suelo.y, m.suelo.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), net.minecraft.sound.SoundCategory.NEUTRAL, 0.9f, 1.5f);
            w.playSound(null, m.suelo.x, m.suelo.y, m.suelo.z, SoundEvents.BLOCK_ANVIL_LAND, net.minecraft.sound.SoundCategory.NEUTRAL, 0.5f, 0.6f);
            if (now >= d.frenado && Math.hypot(p.getX() - m.suelo.x, p.getZ() - m.suelo.z) < 1.9 && p.getY() < Nivel.Y0 + 1.3) { d.frenado = now + 20; golpe = "¡Te cayó un meteorito!"; }
            it.remove();
        }
        return golpe;
    }
}
