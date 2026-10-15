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

    static final double VEL_CALABAZA = 8.0;            // bloques/s con los que rueda cada calabaza hacia el jugador
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
        final List<Cal> cals = new ArrayList<>(); int lanzadas; int[] libres = {}; int paso = 26;      // carril libre de cada oleada (0 = izquierda, 1 = centro, 2 = derecha) y ticks entre una y otra
        Din(Nivel.Mod m, int idx) { mod = m; tipo = m.tipo(); s0 = m.s0(); fin = s0 + m.filas(); this.idx = idx; }
    }

    static final class Cal { Entity e; double s, v, rodado; }

    /** Los obstaculos moviles del nivel cargado, para la copia de un jugador (vacio en el nivel 1). */
    static List<Din> crear() {
        List<Din> l = new ArrayList<>();
        int i = 0, nK = 0;
        for (Nivel.Mod m : Nivel.MODS) if (m.tipo() == 'F' || m.tipo() == 'K') {
            Din d = new Din(m, i++);
            d.omega = (1.7 + 0.3 * (d.idx % 3)) * (d.idx % 2 == 0 ? 1 : -1);          // rad/s (una vuelta cada 3,7 a 2,9 s), en sentidos alternos
            d.fase0 = d.idx * 1.3;
            if (d.tipo == 'K') {                       // el primer tramo de calabazas: 3 oleadas; el ultimo: 4 y mas seguidas (el carril libre cambia siempre al vecino: 2,1 bloques)
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
            boolean cerca = j.s >= d.s0 - (d.tipo == 'K' ? 14 : 34) && j.s <= d.fin + (d.tipo == 'K' ? 3 : 1)
                    && j.sPerseguidor + Nivel.DEF.frente() < (d.tipo == 'K' ? d.fin : d.s0 + 1);       // cuando el Pumpkaboo llega a su zona los destruye (los atraviesa)
            if (!cerca) { if (d.activo) apagar(d); continue; }
            if (!d.activo) activar(w, j, d, now);
            String g = d.tipo == 'F' ? aspas(w, j, p, d, now) : rodantes(w, j, p, d, now, v);
            if (g != null) golpe = g;
        }
        return golpe;
    }

    private static void activar(ServerWorld w, Huida.J j, Din d, long now) {
        d.activo = true; d.t0 = now; d.lanzadas = 0;
        if (d.tipo == 'F') {
            Vec3d hub = Escenas.P(j.hueco, d.s0 + Nivel.F_HUB, 0, 0.0);
            d.barra = EscenasCalabaza.bloque(w, hub, "minecraft:bone_block[axis=x]");
            d.puntaA = EscenasCalabaza.bloque(w, hub, "minecraft:jack_o_lantern[facing=south]");
            d.puntaB = EscenasCalabaza.bloque(w, hub, "minecraft:jack_o_lantern[facing=south]");
        }
    }

    static void apagar(Din d) {
        d.activo = false;
        for (Entity e : new Entity[]{d.barra, d.puntaA, d.puntaB}) if (e != null && !e.isRemoved()) e.discard();
        d.barra = d.puntaA = d.puntaB = null;
        for (Cal c : d.cals) if (c.e != null && !c.e.isRemoved()) c.e.discard();
        d.cals.clear();
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
        if (Math.abs(along) <= r + 0.3 && Math.abs(perp) <= lim) { d.frenado = now + 20; return "¡Te golpeó el hueso giratorio!"; }
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
                c.e = EscenasCalabaza.bloque(w, Escenas.P(h, c.s, c.v, 0), "minecraft:jack_o_lantern[facing=south]");
                d.cals.add(c);
            }
            d.lanzadas++;
            w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, net.minecraft.sound.SoundCategory.NEUTRAL, 0.35f, 1.5f);
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
            c.s -= VEL_CALABAZA / 20.0; c.rodado += VEL_CALABAZA / 20.0;
            if (c.s < j.s - 8 || c.s < d.s0 - 30 || c.e == null || c.e.isRemoved()) { if (c.e != null && !c.e.isRemoved()) c.e.discard(); it.remove(); continue; }
            Vec3d base = Escenas.P(h, c.s, c.v, 0);
            double yawRad = Math.toRadians(Nivel.rumbo(c.s));
            Quaternionf rot = new Quaternionf().rotateY((float) -yawRad).rotateX((float) (-c.rodado / rad));        // rueda hacia el jugador
            EscenasCalabaza.poner(c.e, base.add(0, rad, 0), new Vector3f(0f, -rad, 0f), rot, TAM_CALABAZA, TAM_CALABAZA, TAM_CALABAZA);
            EscenasCalabaza.sync(c.e);
            if (now % 5 == 0) {
                double dist = Math.abs(c.s - j.s);
                if (dist < 22) Sonidos.sfx(p, SoundEvents.ENTITY_IRON_GOLEM_STEP, (float) Math.max(0.15, 0.9 - dist / 30.0), 0.55f);
                w.spawnParticles(ParticleTypes.POOF, base.x, base.y + 0.1, base.z, 1, 0.3, 0.05, 0.3, 0.0);
            }
            // choque: misma zona del camino (0,85 + 0,35 de largo, 0,85 + 0,3 de ancho) y pies por debajo de la parte de arriba de la calabaza
            if (now >= d.frenado && Math.abs(c.s - j.s) < rad + 0.35 && Math.abs(c.v - vJug) < rad + 0.3 && p.getY() < Nivel.Y0 + TAM_CALABAZA - 0.1) {
                d.frenado = now + 20;
                golpe = "¡Te arrolló una calabaza!";
                w.spawnParticles(ParticleTypes.POOF, base.x, base.y + 1, base.z, 14, 0.6, 0.5, 0.6, 0.05);
                w.playSound(null, base.x, base.y, base.z, SoundEvents.BLOCK_WOOD_BREAK, net.minecraft.sound.SoundCategory.NEUTRAL, 1.2f, 0.7f);
                c.e.discard(); it.remove();
            }
        }
        return golpe;
    }
}
