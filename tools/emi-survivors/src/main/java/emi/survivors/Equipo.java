package emi.survivors;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Una partida en marcha: UNA arena con su horda, sus gemas, su jefe y su reloj, y de 1 a 4 jugadores dentro (cada uno es un {@link Run} con su Pokemon, su vida,
 * sus ataques y su experiencia). Es el «equipo» del top: lo que cuenta es lo que aguanta todo el equipo.
 *
 * Reglas del juego en equipo:
 * - Los enemigos persiguen al jugador en pie mas cercano. Quien esta eligiendo carta queda en una burbuja (nadie le toca, no ataca, no se mueve) y los demas siguen;
 *   si todos los que estan en pie estan eligiendo carta, el reloj se para (como en solitario).
 * - Quien se queda sin vida cae (no sale de la partida): un companero que se quede a su lado unos 3 segundos lo revive con la mitad de la vida. La partida acaba
 *   cuando caen todos.
 * - Las gemas son de quien las atrae primero; cada derrotado se cuenta a quien dio el ultimo golpe; los premios del jefe son para todos.
 * - Con mas jugadores salen mas enemigos y algo mas fuertes ({@link Curvas#escalaCantidad}, {@link Curvas#escalaVida}).
 * Con un solo jugador es exactamente la partida en solitario de la fase 1.
 */
final class Equipo {
    static final int RAMPA = 200;               // modo medida: ticks que tarda en llegar al numero de enemigos pedido (10 s)
    static final int MAX_NUEVOS_POR_TICK = 3;   // crear un Pokemon de Cobblemon cuesta: se reparte
    static final int MAX_GEMAS = 300;
    static final int REVIVIR_TICKS = 60;        // 3 s a su lado para revivir a un companero caido
    static final double REVIVIR_DIST = 2.6;

    final ServerWorld w;
    final int slot;
    final double ox, oz;                         // centro de la arena de esta partida
    final int fijo;                              // > 0 = modo de MEDIDA de la fase 0 (ese numero de enemigos desde el principio, sin oleadas ni jefe)
    Grupos.Grupo grupo;                          // el grupo de la sala que juega esta partida (null si son jugadores sueltos)
    final boolean modoPrueba;                    // partida de prueba de un administrador (sin pasar por la sala: se le guarda y devuelve todo)
    final Random rnd = new Random();
    final List<Run> miembros = new ArrayList<>();
    final List<Enemigo> enemigos = new ArrayList<>();
    final List<Gema> gemas = new ArrayList<>();
    final Medidor med = new Medidor();
    final List<Especies.E> pool1, pool2;
    final Set<String> especiesVistas = new HashSet<>();
    int tick, derrotados, fallosCrear, ticksParado;
    double presupuesto;
    boolean jefeSalio, jefeDerrotado;
    boolean terminado;                           // ya se acabo (para no acabarla dos veces)
    Jefe jefe;
    // medias del servidor cuando ya estan todos los enemigos (para el resumen final)
    double sumaEstable, sumaNuestroEstable; int nEstable;

    Equipo(ServerWorld w, int slot, double ox, double oz, int fijo, boolean modoPrueba) {
        this.w = w; this.slot = slot; this.ox = ox; this.oz = oz; this.fijo = fijo; this.modoPrueba = modoPrueba;
        pool1 = Especies.entre(180, 330);
        pool2 = Especies.entre(331, 405);
    }

    int segundos() { return tick / 20; }

    /** Los jugadores que siguen en pie (aunque ahora esten eligiendo carta). */
    List<Run> vivos() {
        List<Run> l = new ArrayList<>(miembros.size());
        for (Run m : miembros) if (m.estado == Run.VIVO) l.add(m);
        return l;
    }

    /** Los jugadores a los que los enemigos pueden perseguir y golpear: en pie y sin estar eligiendo carta. */
    List<Run> objetivos() {
        List<Run> l = new ArrayList<>(miembros.size());
        for (Run m : miembros) if (m.estado == Run.VIVO && !m.esperandoCarta) l.add(m);
        return l;
    }

    static Run masCercano(List<Run> l, double x, double z) {
        Run mejor = null; double mejorD = Double.MAX_VALUE;
        for (Run m : l) {
            double dx = m.pp.x - x, dz = m.pp.z - z, d = dx * dx + dz * dz;
            if (d < mejorD) { mejorD = d; mejor = m; }
        }
        return mejor;
    }

    void avisar(Text t) { for (Run m : miembros) { ServerPlayerEntity p = m.jugador(); if (p != null) p.sendMessage(t, false); } }

    // ------------------------------------------------------------------------------------------------ un tick

    void tick() {
        long t0 = System.nanoTime();

        // jugadores que ya no estan en la arena (desconectados, llevados a otro sitio...): se les saca de la partida
        for (Run m : new ArrayList<>(miembros)) {
            ServerPlayerEntity p = m.jugador();
            if (p == null || p.isRemoved() || p.getServerWorld() != w) Partida.quitar(m, "El jugador ya no esta en la arena.");
        }
        if (miembros.isEmpty() || !Partida.equipos.contains(this)) return;

        // la cuenta atras de las cartas corre aunque el reloj este parado
        boolean activo = false;
        for (Run m : miembros) {
            if (m.esperandoCarta && --m.cartaCuenta <= 0) m.elegirCarta(rnd.nextInt(m.oferta.size()));
            if (m.estado == Run.VIVO && !m.esperandoCarta) activo = true;
        }
        if (!activo) {                              // todos los que estan en pie estan eligiendo carta: el tiempo no corre (pero todos siguen recibiendo el estado, o su cliente creeria que ya acabo)
            if (++ticksParado % 4 == 0) for (Run m : miembros) m.enviarEstado();
            return;
        }

        tick++;
        for (Run m : miembros) {
            ServerPlayerEntity p = m.jugador();
            m.tick = tick;
            m.pp = p.getPos();
            if (!Arena.dentro(this, m.pp.x, m.pp.z, -2.0)) { p.teleport(w, ox + 0.5, Arena.Y0, oz + 0.5, p.getYaw(), 0f); m.pp = p.getPos(); }      // por si acaso se sale del muro
        }
        double t = tick / 20.0;
        int n = miembros.size();
        List<Run> objetivos = objetivos();

        // 1) crear enemigos
        if (fijo > 0) {
            int meta = Math.min(fijo, (int) ((long) fijo * tick / RAMPA));
            for (int k = 0; k < MAX_NUEVOS_POR_TICK && enemigos.size() < meta; k++) if (!crearEnemigo(Enemigo.Tipo.DEBIL)) break;
        } else {
            int cap = (int) Math.round(Math.min(Curvas.tope(t, n), Partida.topePorEquipo()) * Partida.factorRendimiento());
            presupuesto = Math.min(3.0, presupuesto + Curvas.ritmo(t, n) / 20.0);
            for (int k = 0; k < MAX_NUEVOS_POR_TICK && presupuesto >= 1.0 && enemigos.size() < cap; k++) {
                if (!crearEnemigo(Enemigo.Tipo.DEBIL)) break;
                presupuesto -= 1.0;
            }
            if (tick % (20 * Curvas.CADA_ELITE) == 0) { if (crearEnemigo(Enemigo.Tipo.ELITE)) for (Run m : miembros) { ServerPlayerEntity p = m.jugador(); if (p != null) Msg.actionBar(p, Text.literal("¡Cuidado, se acerca un campeón!").formatted(Formatting.GOLD, Formatting.BOLD)); } }
            if (!jefeSalio && t >= Curvas.T_JEFE) crearJefe();
        }

        // 2) separar a los enemigos entre si (rejilla de 1,6 bloques), moverlos hacia el jugador en pie mas cercano y contar el dano por contacto
        separar();
        for (Run m : miembros) if (m.iframes > 0) m.iframes--;
        for (Enemigo en : enemigos) {
            if (jefe != null && en == jefe.en) continue;
            Run obj = masCercano(objetivos, en.e.getX(), en.e.getZ());
            if (obj == null) en.mover(en.e.getX(), en.e.getZ(), 0); else en.mover(obj.pp.x, obj.pp.z);
        }
        if (jefe != null && jefe.vivo()) jefe.tick(objetivos);
        for (Enemigo en : enemigos) {
            if (en.cdContacto > 0) en.cdContacto--;
            if (en.cdContacto > 0) continue;
            for (Run m : objetivos) {
                if (m.iframes > 0) continue;
                double dx = en.e.getX() - m.pp.x, dz = en.e.getZ() - m.pp.z, r = en.radio + 0.4;
                if (dx * dx + dz * dz < r * r && Math.abs(en.e.getY() - m.pp.y) < (en.grande() ? 3.0 : 1.8)) {
                    en.cdContacto = 20;
                    m.danarJugador(en.dmg);
                    break;
                }
            }
        }

        // 3) los ataques de cada jugador
        for (Run m : miembros) if (m.estado == Run.VIVO && !m.esperandoCarta) m.tickAtaques();

        // 4) retirar a los derrotados (sueltan gemas)
        for (Iterator<Enemigo> it = enemigos.iterator(); it.hasNext(); ) {
            Enemigo en = it.next();
            if (en.hp > 0) continue;
            muerte(en);
            it.remove();
        }

        // 5) gemas, regeneracion y revivir
        for (Run m : miembros) if (m.estado == Run.VIVO && !m.esperandoCarta) m.tickGemasYRegen();
        if (gemas.size() > MAX_GEMAS) fusionarGemas();
        revivir();

        // 6) interfaz y medidas
        med.registrar(System.nanoTime() - t0, Partida.server);
        if (tick > RAMPA + 60 && tick % 10 == 0) { sumaEstable += med.msptServidor(); sumaNuestroEstable += med.msNuestro(); nEstable++; }
        if (tick % 4 == 0) for (Run m : miembros) m.enviarEstado();
        if (tick % 2 == 0) for (Run m : miembros) m.enviarGemas();
        if (tick % 100 == 0) EmiSurvivors.LOG.info("[medida] {} t={}s nivel={} enemigos={} gemas={} derrotados={} hp={} | nuestro={} ms/tick (max {}) | servidor={} ms/tick (media de 100, max {})",
                nombres(), segundos(), miembros.get(0).nivel, enemigos.size(), gemas.size(), derrotados, resumenVida(), Run.fmt(med.msNuestro()), Run.fmt(med.msNuestroMax()), Run.fmt(med.msptServidor()), Run.fmt(med.msptMax()));

        // 7) quien se queda sin vida cae; si caen todos, se acabo
        for (Run m : miembros) if (m.estado == Run.VIVO && m.hp <= 0) m.caer();
        if (vivos().isEmpty()) { Partida.terminarEquipo(this, miembros.size() == 1 ? "Te han derrotado." : "Han caído todos los jugadores."); return; }
        for (Run m : miembros) if (m.estado == Run.VIVO && m.pendientes > 0 && !m.esperandoCarta) m.ofrecerCartas();
    }

    String nombres() {
        StringBuilder sb = new StringBuilder();
        for (Run m : miembros) { if (sb.length() > 0) sb.append('+'); sb.append(m.nombre); }
        return sb.toString();
    }

    private String resumenVida() {
        StringBuilder sb = new StringBuilder();
        for (Run m : miembros) { if (sb.length() > 0) sb.append('/'); sb.append(Math.round(m.hp)); }
        return sb.toString();
    }

    // ------------------------------------------------------------------------------------------------ revivir

    private void revivir() {
        for (Run c : miembros) {
            if (c.estado != Run.CAIDO) continue;
            boolean cerca = false;
            for (Run m : miembros) {
                if (m == c || m.estado != Run.VIVO || m.esperandoCarta) continue;
                double dx = m.pp.x - c.pp.x, dz = m.pp.z - c.pp.z;
                if (dx * dx + dz * dz <= REVIVIR_DIST * REVIVIR_DIST) { cerca = true; break; }
            }
            if (cerca) c.reviveT++; else c.reviveT = Math.max(0, c.reviveT - 2);
            if (tick % 3 == 0) {
                w.spawnParticles(ParticleTypes.SOUL, c.pp.x, c.pp.y + 1.2, c.pp.z, 1, 0.25, 0.4, 0.25, 0.01);
                int pts = 14, hechos = (int) (pts * Math.min(1.0, c.reviveT / (double) REVIVIR_TICKS));
                for (int k = 0; k < pts; k++) {
                    double a = k * Math.PI * 2 / pts;
                    w.spawnParticles(k < hechos ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.SMOKE, c.pp.x + Math.cos(a) * REVIVIR_DIST * 0.6, c.pp.y + 0.1, c.pp.z + Math.sin(a) * REVIVIR_DIST * 0.6, 1, 0, 0, 0, 0);
                }
            }
            if (c.reviveT >= REVIVIR_TICKS) c.revivir();
        }
    }

    // ------------------------------------------------------------------------------------------------ enemigos

    private static final double CELDA = 1.6, SEP_MAX = 0.13;

    /** Cada enemigo se aparta de los que tiene muy cerca (asi la horda forma un corro en vez de apilarse en un punto). */
    private void separar() {
        HashMap<Long, List<Enemigo>> rejilla = new HashMap<>();
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

    /** Un sitio donde puede aparecer un enemigo: en un anillo de 21-26 bloques de un jugador en pie al azar, dentro de la arena y con suelo. null si no se encuentra. */
    private Vec3d sitioDeAparicion(double distMin, double distMax) {
        List<Run> base = vivos();
        if (base.isEmpty()) base = miembros;
        if (base.isEmpty()) return null;
        for (int intento = 0; intento < 8; intento++) {
            Vec3d c = base.get(rnd.nextInt(base.size())).pp;
            double ang = rnd.nextDouble() * Math.PI * 2, r = distMin + rnd.nextDouble() * (distMax - distMin);
            double x = c.x + Math.cos(ang) * r, z = c.z + Math.sin(ang) * r;
            if (!Arena.dentro(this, x, z, 2.0)) continue;
            BlockPos pie = new BlockPos((int) Math.floor(x), (int) Arena.Y0, (int) Math.floor(z));
            if (!w.getBlockState(pie).getCollisionShape(w, pie).isEmpty() || w.getBlockState(pie.down()).isAir()) continue;     // sin bloque macizo en los pies y con suelo debajo
            return new Vec3d(x, Arena.Y0, z);
        }
        return null;
    }

    private boolean crearEnemigo(Enemigo.Tipo pedido) {
        double t = tick / 20.0;
        Especies.E sp; String extra = ""; Enemigo.Tipo tipoE = pedido;
        if (pedido == Enemigo.Tipo.ELITE) {
            List<Especies.E> pool = pool2.isEmpty() ? pool1 : pool2;
            if (pool.isEmpty()) return false;
            sp = pool.get(rnd.nextInt(pool.size()));
        } else if (rnd.nextDouble() < Curvas.P_MICHI && Especies.michi() != null) {             // 1 de cada 10 es un Michialien (el toque de Emi)
            sp = Especies.michi(); tipoE = Enemigo.Tipo.MICHI; extra = Especies.FORMAS_MICHI[rnd.nextInt(Especies.FORMAS_MICHI.length)];
        } else {
            boolean fuerte = rnd.nextDouble() < (fijo > 0 ? 0.3 : Curvas.pFuerte(t));
            List<Especies.E> pool = fuerte ? pool2 : pool1;
            if (pool.isEmpty()) pool = pool1.isEmpty() ? pool2 : pool1;
            if (pool.isEmpty()) return false;
            sp = pool.get(rnd.nextInt(pool.size()));
            tipoE = fuerte ? Enemigo.Tipo.FUERTE : Enemigo.Tipo.DEBIL;
        }
        Vec3d s = sitioDeAparicion(21, 26);
        if (s == null) return false;
        double tm = fijo > 0 ? 0 : t;
        Enemigo en = Enemigo.crear(w, sp, extra, s, tipoE, Curvas.vidaMul(tm) * Curvas.escalaVida(miembros.size()), Curvas.danoMul(tm), Curvas.velMul(tm), 0f, rnd);
        if (en == null) { fallosCrear++; return false; }
        enemigos.add(en);
        especiesVistas.add(sp.id());
        return true;
    }

    /** Solo para pruebas (/survivor dev elite): crea un campeon ya. */
    boolean devElite() { return crearEnemigo(Enemigo.Tipo.ELITE); }

    /** El jefe: Snorlax Emi si esta Emipokemon (si no, un Snorlax normal), enorme, a unos 20 bloques de un jugador. */
    void crearJefe() {
        jefeSalio = true;
        Especies.E sp = Especies.porId("emipokemon:snorlax_emi");
        String nom = "Snorlax Emi";
        if (sp == null) { sp = Especies.porId("snorlax"); nom = "Snorlax"; }
        if (sp == null) { EmiSurvivors.LOG.warn("No hay especie para el jefe"); return; }
        Vec3d s = sitioDeAparicion(17, 22);
        if (s == null) { jefeSalio = false; return; }                 // se vuelve a intentar el siguiente tick
        double t = tick / 20.0;
        Enemigo en = Enemigo.crear(w, sp, "", s, Enemigo.Tipo.JEFE, 1, Curvas.danoMul(t), 1, Curvas.vidaJefe(t) * (float) Curvas.escalaJefe(miembros.size()), rnd);
        if (en == null) { fallosCrear++; return; }
        enemigos.add(en);
        jefe = new Jefe(this, en, nom);
        for (Run m : miembros) {
            ServerPlayerEntity p = m.jugador();
            if (p != null) Msg.title(p, Text.literal("¡" + nom.toUpperCase() + "!").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), Text.literal("Esquiva los círculos y las líneas rojas").formatted(Formatting.YELLOW), 5, 50, 15);
        }
    }

    // ------------------------------------------------------------------------------------------------ muertes y gemas

    private void muerte(Enemigo en) {
        double x = en.e.getX(), y = en.e.getY(), z = en.e.getZ();
        w.spawnParticles(ParticleTypes.POOF, x, y + 0.5, z, en.grande() ? 25 : 6, 0.3, 0.3, 0.3, 0.02);
        en.quitar();
        derrotados++;
        if (en.ultimo != null && miembros.contains(en.ultimo)) en.ultimo.derrotados++;
        switch (en.tipo) {
            case JEFE -> {
                jefeDerrotado = true; jefe = null;
                for (int k = 0; k < 6; k++) { double a = k * Math.PI / 3; gemas.add(new Gema(x + Math.cos(a) * 2.5, z + Math.sin(a) * 2.5, 25)); }
                for (Run m : miembros) m.premioJefe();
            }
            case ELITE -> { for (int k = 0; k < 3; k++) { double a = k * Math.PI * 2 / 3; gemas.add(new Gema(x + Math.cos(a) * 1.2, z + Math.sin(a) * 1.2, 5)); } }
            default -> gemas.add(new Gema(x, z, en.xp));
        }
    }

    /** Si hay demasiadas gemas, se juntan las mas lejanas en pocas grandes (asi no se pierde experiencia ni se llena el cliente). */
    private void fusionarGemas() {
        List<Run> base = vivos().isEmpty() ? miembros : vivos();
        gemas.sort((a, b) -> Double.compare(distMinimaA(base, b), distMinimaA(base, a)));
        int juntar = gemas.size() - MAX_GEMAS + 20, suma = 0;
        double cx = 0, cz = 0;
        for (int k = 0; k < juntar; k++) { Gema g = gemas.remove(0); suma += g.valor; cx += g.x; cz += g.z; }
        gemas.add(new Gema(cx / juntar, cz / juntar, suma));
    }

    private static double distMinimaA(List<Run> l, Gema g) {
        double mejor = Double.MAX_VALUE;
        for (Run m : l) mejor = Math.min(mejor, Math.hypot(g.x - m.pp.x, g.z - m.pp.z));
        return mejor;
    }

    // ------------------------------------------------------------------------------------------------ fin

    /** Quita todo lo de la partida del mundo (enemigos, disparos y gemas). */
    void limpiar() {
        for (Enemigo en : enemigos) en.quitar();
        enemigos.clear();
        gemas.clear();
        for (Run m : miembros) m.proyectiles.clear();
    }

    boolean hayMedidaEstable() { return nEstable > 0; }

    float msptEstable() { return nEstable == 0 ? 0f : (float) (sumaEstable / nEstable); }
    float nuestroEstable() { return nEstable == 0 ? 0f : (float) (sumaNuestroEstable / nEstable); }

    /** Lo que los demas jugadores ven del jugador {@code de}: «nombre|vida|vidaMax|estado» separados por «;» (estado 0 en pie, 1 caido, 2 eligiendo carta). */
    String companerosDe(Run de) {
        StringBuilder sb = new StringBuilder();
        for (Run m : miembros) {
            if (m == de) continue;
            if (sb.length() > 0) sb.append(';');
            sb.append(m.nombre.replace('|', ' ').replace(';', ' ')).append('|').append((int) Math.max(0, Math.ceil(m.hp))).append('|').append((int) Math.round(m.st.hpMax)).append('|')
                    .append(m.estado == Run.CAIDO ? 1 : m.esperandoCarta ? 2 : 0);
        }
        return sb.toString();
    }
}
