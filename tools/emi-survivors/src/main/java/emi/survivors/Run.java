package emi.survivors;

import com.cobblemon.mod.common.pokemon.Pokemon;
import emi.survivors.net.EstadoPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Una partida de la fase 0 (un solo jugador): mantiene {@code objetivo} Pokemon enemigos persiguiendo al jugador, dispara su ataque automatico, cuenta el dano
 * por contacto y mide cuanto cuesta todo en ms por tick. Se acaba cuando el jugador llega a 0 de vida (salvo en modo prueba «inmortal») o con /survivor parar.
 */
final class Run {
    static final int RAMPA = 200;               // ticks que tarda en llegar al numero de enemigos pedido (10 s)
    static final int MAX_NUEVOS_POR_TICK = 3;   // crear un Pokemon de Cobblemon cuesta: se reparte
    static final int CD_ATAQUE = 14;            // ticks entre disparos
    static final double ALCANCE = 15.0;         // bloques a los que apunta el ataque

    final ServerWorld w;
    final UUID id;
    final String nombre;
    final Pokemon pk;
    final String tipo;
    final String morphAntes;
    final int objetivo;
    final boolean inmortal;
    final Random rnd = new Random();
    final List<Enemigo> enemigos = new ArrayList<>();
    final List<Proyectil> proyectiles = new ArrayList<>();
    final List<BlockPos> rocas = new ArrayList<>();
    final Medidor med = new Medidor();
    final ParticleEffect particulaAtaque;
    List<Especies.E> pool1, pool2;
    final java.util.Set<String> especiesVistas = new java.util.HashSet<>();
    int fallosCrear;

    int tick, hp = Partida.HP_MAX, iframes, cdAtaque, derrotados;
    float dmgAtaque;
    // medias del servidor cuando ya estan todos los enemigos (para el resumen final)
    double sumaEstable, sumaNuestroEstable; int nEstable;

    Run(ServerWorld w, ServerPlayerEntity p, Pokemon pk, String morphAntes, int objetivo, boolean inmortal) {
        this.w = w; this.id = p.getUuid(); this.nombre = p.getGameProfile().getName(); this.pk = pk; this.morphAntes = morphAntes;
        this.objetivo = objetivo; this.inmortal = inmortal;
        this.tipo = pk.getPrimaryType().getName();
        this.particulaAtaque = Arena.particula(tipo);
        this.dmgAtaque = 14f + pk.getLevel() * 0.12f;
        pool1 = Especies.entre(180, 330);
        pool2 = Especies.entre(331, 405);
        rocas.addAll(Arena.colocarRocas(w, rnd, 18));
    }

    ServerPlayerEntity jugador() { return Partida.server == null ? null : Partida.server.getPlayerManager().getPlayer(id); }

    int segundos() { return tick / 20; }

    void tick() {
        long t0 = System.nanoTime();
        ServerPlayerEntity p = jugador();
        if (p == null || p.isRemoved() || p.getServerWorld() != w) { Partida.terminar(this, "El jugador ya no esta en la arena."); return; }
        tick++;
        Vec3d pp = p.getPos();

        // 1) mantener el numero de enemigos (con una rampa al principio)
        int meta = Math.min(objetivo, (int) ((long) objetivo * tick / RAMPA));
        for (int n = 0; n < MAX_NUEVOS_POR_TICK && enemigos.size() < meta; n++) if (!crearEnemigo(pp)) break;

        // 2) separar a los enemigos entre si (rejilla de 1,6 bloques), moverlos y contar el daño por contacto
        separar();
        if (iframes > 0) iframes--;
        for (Enemigo en : enemigos) {
            en.mover(pp.x, pp.z);
            if (en.cdContacto > 0) en.cdContacto--;
            double dx = en.e.getX() - pp.x, dz = en.e.getZ() - pp.z, r = en.radio + 0.4;
            if (en.cdContacto == 0 && iframes == 0 && dx * dx + dz * dz < r * r && Math.abs(en.e.getY() - pp.y) < 1.8) {
                en.cdContacto = 20; iframes = 8;
                if (!inmortal) hp -= Math.round(en.dmg);
                p.networkHandler.sendPacket(new EntityStatusS2CPacket(p, (byte) 2));
            }
        }

        // 3) ataque automatico
        if (cdAtaque > 0) cdAtaque--;
        else {
            Enemigo obj = masCercano(pp, ALCANCE);
            if (obj != null) {
                Vec3d d = new Vec3d(obj.e.getX() - pp.x, 0, obj.e.getZ() - pp.z);
                if (d.lengthSquared() > 1e-6) {
                    proyectiles.add(new Proyectil(w, pp.add(0, 0.9, 0), d.normalize(), dmgAtaque, particulaAtaque, 18, 2));
                    cdAtaque = CD_ATAQUE;
                }
            }
        }
        for (Iterator<Proyectil> it = proyectiles.iterator(); it.hasNext(); ) if (!it.next().tick(enemigos)) it.remove();

        // 4) retirar a los derrotados
        for (Iterator<Enemigo> it = enemigos.iterator(); it.hasNext(); ) {
            Enemigo en = it.next();
            if (en.hp > 0) continue;
            w.spawnParticles(ParticleTypes.POOF, en.e.getX(), en.e.getY() + 0.5, en.e.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
            en.quitar();
            it.remove();
            derrotados++;
        }

        // 5) interfaz y medidas
        if (p.getHungerManager().getFoodLevel() < 20 && tick % 100 == 0) p.getHungerManager().setFoodLevel(20);
        med.registrar(System.nanoTime() - t0, Partida.server);
        if (tick > RAMPA + 60 && tick % 10 == 0) { sumaEstable += med.msptServidor(); sumaNuestroEstable += med.msNuestro(); nEstable++; }
        if (tick % 2 == 0 && ServerPlayNetworking.canSend(p, EstadoPayload.ID)) ServerPlayNetworking.send(p, new EstadoPayload(true, Math.max(0, hp), Partida.HP_MAX, segundos(), enemigos.size(), derrotados, med.msptServidor(), med.msNuestro()));
        if (tick % 100 == 0) EmiSurvivors.LOG.info("[medida] t={}s enemigos={} derrotados={} hp={} | nuestro={} ms/tick (max {}) | servidor={} ms/tick (media de 100, max {})",
                segundos(), enemigos.size(), derrotados, hp, fmt(med.msNuestro()), fmt(med.msNuestroMax()), fmt(med.msptServidor()), fmt(med.msptMax()));
        if (hp <= 0) Partida.terminar(this, "Te han derrotado.");
    }

    private static final double CELDA = 1.6, SEP_MAX = 0.13;

    /** Cada enemigo se aparta de los que tiene muy cerca (asi la horda forma un corro en vez de apilarse en un punto). */
    private void separar() {
        java.util.HashMap<Long, List<Enemigo>> rejilla = new java.util.HashMap<>();
        for (Enemigo en : enemigos) rejilla.computeIfAbsent(clave((int) Math.floor(en.e.getX() / CELDA), (int) Math.floor(en.e.getZ() / CELDA)), k -> new ArrayList<>(4)).add(en);
        for (Enemigo en : enemigos) {
            int cx = (int) Math.floor(en.e.getX() / CELDA), cz = (int) Math.floor(en.e.getZ() / CELDA);
            double sx = 0, sz = 0;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                List<Enemigo> l = rejilla.get(clave(cx + dx, cz + dz));
                if (l == null) continue;
                for (Enemigo o : l) {
                    if (o == en) continue;
                    double ex = en.e.getX() - o.e.getX(), ez = en.e.getZ() - o.e.getZ(), d2 = ex * ex + ez * ez, min = (en.radio + o.radio) * 0.9;
                    if (d2 >= min * min) continue;
                    if (d2 < 1e-6) { sx += (rnd.nextDouble() - 0.5) * 0.2; sz += (rnd.nextDouble() - 0.5) * 0.2; continue; }
                    double d = Math.sqrt(d2), f = (min - d) / min;
                    sx += ex / d * f; sz += ez / d * f;
                }
            }
            double m = Math.sqrt(sx * sx + sz * sz);
            double k = Math.min(SEP_MAX, m * 0.14) / (m > 1e-9 ? m : 1);
            en.sepx = sx * k; en.sepz = sz * k;
        }
    }

    private static long clave(int cx, int cz) { return ((long) cx << 32) ^ (cz & 0xffffffffL); }

    private Enemigo masCercano(Vec3d pp, double alcance) {
        Enemigo mejor = null; double mejorD = alcance * alcance;
        for (Enemigo en : enemigos) {
            double dx = en.e.getX() - pp.x, dz = en.e.getZ() - pp.z, d = dx * dx + dz * dz;
            if (d < mejorD) { mejorD = d; mejor = en; }
        }
        return mejor;
    }

    private boolean crearEnemigo(Vec3d pp) {
        Especies.E sp; String extra = ""; boolean fuerte;
        Especies.E michi = rnd.nextInt(10) == 0 ? Especies.michi() : null;      // 1 de cada 10 es un Michialien (el toque de Emi)
        if (michi != null) {
            sp = michi; fuerte = true; extra = Especies.FORMAS_MICHI[rnd.nextInt(Especies.FORMAS_MICHI.length)];
        } else {
            fuerte = rnd.nextInt(10) < 3;
            List<Especies.E> pool = fuerte ? pool2 : pool1;
            if (pool.isEmpty()) pool = pool1.isEmpty() ? pool2 : pool1;
            if (pool.isEmpty()) return false;
            sp = pool.get(rnd.nextInt(pool.size()));
        }
        for (int intento = 0; intento < 5; intento++) {
            double ang = rnd.nextDouble() * Math.PI * 2, r = 21 + rnd.nextDouble() * 5;
            double x = pp.x + Math.cos(ang) * r, z = pp.z + Math.sin(ang) * r;
            if (!w.getBlockState(new BlockPos((int) Math.floor(x), (int) Arena.Y0, (int) Math.floor(z))).isAir()) continue;
            Enemigo en = Enemigo.crear(w, sp, extra, new Vec3d(x, Arena.Y0, z), fuerte, rnd);
            if (en == null) { fallosCrear++; return false; }
            enemigos.add(en);
            especiesVistas.add(sp.id());
            return true;
        }
        return false;
    }

    /** Quita todo lo de la partida del mundo (enemigos y rocas). */
    void limpiar() {
        for (Enemigo en : enemigos) en.quitar();
        enemigos.clear();
        proyectiles.clear();
        Arena.quitarRocas(w, rocas);
    }

    boolean hayMedidaEstable() { return nEstable > 0; }

    float msptEstable() { return nEstable == 0 ? 0f : (float) (sumaEstable / nEstable); }
    float nuestroEstable() { return nEstable == 0 ? 0f : (float) (sumaNuestroEstable / nEstable); }

    static String fmt(float v) { return String.format(java.util.Locale.ROOT, "%.2f", v); }
}
