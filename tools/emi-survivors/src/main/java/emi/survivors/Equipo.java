package emi.survivors;

import net.minecraft.entity.Entity;
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
    static final int MAX_GEMAS = 300, MAX_OBJETOS = 30;
    static final int REVIVIR_TICKS = 60;        // 3 s a su lado para revivir a un companero caido
    static final double REVIVIR_DIST = 2.6;

    ServerWorld w;                               // la dimension del mundo en el que esta ahora el equipo (cambia al superar la Sentencia)
    Biomas.Bioma bioma;                          // el bioma (la arena) del mundo actual
    final int slot;
    final double ox, oz;                         // centro de la arena de esta partida
    double cx, cz, radio = Arena.RADIO;          // el sitio por el que se mueve ahora el equipo: la arena, o el escenario del duelo con un legendario (ver entrarDuelo)
    boolean duelo;                               // el equipo esta en el escenario del duelo con un legendario: no salen enemigos ni jefes ni cambian los horarios
    int tickDuelo0;
    final int fijo;                              // > 0 = modo de MEDIDA de la fase 0 (ese numero de enemigos desde el principio, sin oleadas ni jefe)
    Grupos.Grupo grupo;                          // el grupo de la sala que juega esta partida (null si son jugadores sueltos)
    final boolean modoPrueba;                    // partida de prueba de un administrador (sin pasar por la sala: se le guarda y devuelve todo)
    final Random rnd = new Random();
    final List<Run> miembros = new ArrayList<>();
    final List<Enemigo> enemigos = new ArrayList<>();
    final List<Gema> gemas = new ArrayList<>();
    final List<Objeto> objetos = new ArrayList<>();
    final List<Entity> marcas = new ArrayList<>();          // las marcas de aviso del suelo que hay ahora (ataques de jefes, mareas): ver Marcas
    final Medidor med = new Medidor();
    List<Especies.E> pool1, pool2, sesgo1, sesgo2;       // las especies que pueden salir, y de ellas las de los tipos favoritos del bioma
    final Set<String> especiesVistas = new HashSet<>();
    int tick, derrotados, fallosCrear, ticksParado;
    double presupuesto;
    Marea marea;                                    // la marea en marcha (null si no hay)
    int mareasSalidas;
    boolean avisoPers;                              // ya se explico que significan los colores de los enemigos con personalidad
    private final List<Bomba> bombas = new ArrayList<>();      // las bombas que dejan los enemigos explosivos al caer
    private final List<DisparoEnemigo> disparos = new ArrayList<>();      // las bolas que lanzan los enemigos a distancia (ver Ataque)
    boolean avisoAtaque;                            // ya se explico que algunos enemigos atacan de lejos o con area
    int jugadoresAlEmpezar = 1;                  // cuantos jugadores empezaron la partida (para las misiones de grupo; aunque alguien se vaya)
    int jefesSalidos, jefesDerrotados;              // cuantos jefes han salido y cuantos se han derrotado (uno cada 3 minutos; ver Jefes)
    private final List<double[]> refuerzos = new ArrayList<>();      // sitios (x, z) donde faltan por aparecer los refuerzos que ha invocado un jefe
    private Jefes.Def refuerzosDe;
    boolean terminado;                           // ya se acabo (para no acabarla dos veces)
    Jefe jefe;
    // medias del servidor cuando ya estan todos los enemigos (para el resumen final)
    double sumaEstable, sumaNuestroEstable; int nEstable;

    Equipo(ServerWorld w, int slot, double ox, double oz, int fijo, boolean modoPrueba, Biomas.Bioma bioma) {
        this.w = w; this.slot = slot; this.ox = ox; this.oz = oz; this.cx = ox; this.cz = oz; this.fijo = fijo; this.modoPrueba = modoPrueba; this.bioma = bioma;
        mundo = Math.max(0, Mundos.indice(bioma.id()));
        jefesSalidos = Jefes.POR_MUNDO * mundo;
        prepararEspecies();
    }

    /** Las especies que pueden salir en el mundo actual. */
    private void prepararEspecies() {
        pool1 = Especies.entre(180, 330);
        pool2 = Especies.entre(331, 405);
        sesgo1 = favoritas(pool1);
        sesgo2 = favoritas(pool2);
    }

    // ------------------------------------------------------------------------------------------------ mundos en secuencia (pradera -> bosque -> cosmos -> Michi Gigante)
    int mundo;                                   // 0 = pradera, 1 = bosque, 2 = cosmos (ver Mundos)
    int tickMundo0;                              // el tick del equipo en que se entro al mundo actual (los horarios de jefes, mareas, grietas... cuentan desde ahi)
    int mareasBase;                              // marcas ya salidas al entrar al mundo actual
    boolean faseFinal;                           // ya se paso la Sentencia del ultimo mundo: toca el Michi Gigante
    boolean victoria;                            // el Michi Gigante acaba de caer (se atiende en el mismo tick: ver alVencerGigante)
    boolean ganado;                              // alguna vez se venció a un Michi Gigante: la partida cuenta como GANADA (premio, marca «✔» en el top, logro) aunque siga en el Mas alla
    int vueltasHechas;                           // cuantos Michi Gigante se han vencido (cada uno abre una vuelta mas dificil: «el Mas alla»)
    private boolean acabarGanando;               // no se pudo seguir (falta una dimension): la partida acaba con victoria

    /** En que vuelta del recorrido va el equipo (0 = la primera; desde la 1 estan en «el Mas alla»). */
    int vuelta() { return Curvas.vuelta(mundo); }

    /** Segundos desde que el equipo entro al mundo actual. */
    double tm() { return (tick - tickMundo0) / 20.0; }

    /** De esas especies, las que tienen alguno de los tipos favoritos del bioma. */
    private List<Especies.E> favoritas(List<Especies.E> l) {
        List<Especies.E> r = new ArrayList<>();
        for (Especies.E e : l) if (bioma.tipos().contains(e.tipo1()) || bioma.tipos().contains(e.tipo2())) r.add(e);
        return r;
    }

    /** El grupo de especies del que sale un enemigo normal: con la probabilidad del bioma, el de sus tipos favoritos (fantasmas en el bosque, psiquicos en el cosmos...); si no, el general. */
    private List<Especies.E> pool(boolean fuerte) {
        List<Especies.E> favoritas = fuerte ? sesgo2 : sesgo1;
        List<Especies.E> l = !favoritas.isEmpty() && rnd.nextDouble() < bioma.sesgo() ? favoritas : fuerte ? pool2 : pool1;
        return l.isEmpty() ? (pool1.isEmpty() ? pool2 : pool1) : l;
    }

    /** Para el resumen del log: de los enemigos normales que han salido, cuantos eran de un tipo favorito del bioma (cuenta cada vez que se elige una especie). */
    int normalesTotal, normalesDelBioma;

    private Especies.E elegirNormal(List<Especies.E> pool) {
        Especies.E sp = pool.get(rnd.nextInt(pool.size()));
        normalesTotal++;
        if (bioma.tipos().contains(sp.tipo1()) || bioma.tipos().contains(sp.tipo2())) normalesDelBioma++;
        return sp;
    }

    /** El nombre del mundo (bioma) de esta partida, para la interfaz. */
    /** El clima de la partida (cambia cada 2:30; ver {@link Clima}). */
    ClimaEquipo clima = new ClimaEquipo(this);
    /** Las grietas con legendarios (ver {@link Grietas}). */
    GrietaEquipo grieta = new GrietaEquipo(this);
    /** La prueba final (ver {@link Sentencia}). */
    SentenciaEquipo sentencia = new SentenciaEquipo(this);
    /** Los eventos cortos de la partida (ver {@link Eventos}). */
    EventoEquipo evento = new EventoEquipo(this);
    /** Tesoros pequeños y cofres grandes del mapa (ver {@link Tesoros}). */
    TesorosEquipo tesoros = new TesorosEquipo(this);

    /** ¿Ya no salen cosas nuevas (la Sentencia ha empezado)? */
    boolean mundoCerrado() { return Sentencia.toca(tm()); }

    /** Lo que se escribe en la interfaz como zona: el bioma y, si no esta despejado, el clima. */
    String zona() {
        if (duelo) return "DUELO LEGENDARIO";
        String c = clima.etiqueta(), n = vuelta() > 0 ? bioma.nombre() + " · Más allá " + (vuelta() + 1) : bioma.nombre();
        return c.isEmpty() ? n : n + " · " + c;
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
            if (!Arena.dentro(this, m.pp.x, m.pp.z, -2.0)) { p.teleport(w, cx + 0.5, Arena.Y0, cz + 0.5, p.getYaw(), 0f); m.pp = p.getPos(); }      // por si acaso se sale del muro
        }
        double t = tick / 20.0;
        int n = miembros.size();
        List<Run> objetivos = objetivos();

        // 1) crear enemigos
        if (fijo > 0) {
            int meta = Math.min(fijo, (int) ((long) fijo * tick / RAMPA));
            for (int k = 0; k < MAX_NUEVOS_POR_TICK && enemigos.size() < meta; k++) if (!crearEnemigo(Enemigo.Tipo.DEBIL)) break;
        } else if (!duelo) {
            double fm = marea != null && marea.enemigosPropios() ? 1.25 : 1.0;
            int cap = tesoros.cantidad() + (int) Math.round(Math.min(Curvas.TOPE_EQUIPO, Math.min(Curvas.tope(t, n) * Curvas.mundoTope(mundo), Partida.topePorEquipo()) * fm) * Partida.factorRendimiento());
            presupuesto = Math.min(3.0, presupuesto + Curvas.ritmo(t, n) * Curvas.mundoRitmo(mundo) * (marea == null ? 1.0 : marea.factorRitmo()) * (sentencia.activa() ? Sentencia.RITMO : 1.0) / 20.0);
            for (int k = 0; k < MAX_NUEVOS_POR_TICK && presupuesto >= 1.0 && enemigos.size() < cap; k++) {
                if (!crearEnemigo(Enemigo.Tipo.DEBIL)) break;
                presupuesto -= 1.0;
            }
            if (tick % (20 * Curvas.eliteCada(mundo)) == 0) { if (crearEnemigo(Enemigo.Tipo.ELITE)) for (Run m : miembros) { ServerPlayerEntity p = m.jugador(); if (p != null) Msg.actionBar(p, Text.literal("¡Cuidado, se acerca un campeón!").formatted(Formatting.GOLD, Formatting.BOLD)); } }
            boolean final_ = Sentencia.toca(tm()) || duelo;                                       // desde la Sentencia ya no salen jefes ni mareas nuevos
            if (!final_ && (jefe == null || !jefe.vivo()) && jefesSalidos < Jefes.POR_MUNDO * mundo + Jefes.tocanEnMundo(tm(), mundo)) crearJefe(jefesSalidos + 1);        // uno vivo a la vez: si el anterior sigue, el siguiente espera
            if (!final_ && marea == null && (jefe == null || !jefe.vivo()) && mareasSalidas < mareasBase + Mareas.tocan(tm())) {
                mareasSalidas = Math.max(mareasSalidas, mareasBase + Mareas.tocan(tm()) - 1);          // si una marea llega tarde (habia un jefe vivo), las que se perdieron no se acumulan
                iniciarMarea(++mareasSalidas, null);
            }
        }
        if (marea != null && marea.tick(objetivos)) { marea.limpiar(); marea = null; }

        crearRefuerzos();
        if (fijo == 0) { int tw = tick - tickMundo0; grieta.tick(tw); if (!duelo) { clima.tick(tw); sentencia.tick(tw); evento.tick(tw); tesoros.tick(tw); } }

        // 2) separar a los enemigos entre si (rejilla de 1,6 bloques), moverlos hacia el jugador en pie mas cercano y contar el dano por contacto
        separar();
        for (Run m : miembros) if (m.iframes > 0) m.iframes--;
        for (Enemigo en : enemigos) {
            if (jefe != null && en == jefe.en) continue;
            if (grieta.controla(en)) continue;                                            // el legendario de una grieta se mueve con su propio «jefe» (ver GrietaEquipo)
            if (en.tesoro) continue;                                                       // un tesoro pequeño: no se mueve
            if (en.shiny && tick % 5 == 0) w.spawnParticles(ParticleTypes.END_ROD, en.e.getX(), en.e.getY() + en.radio * 2.2, en.e.getZ(), 2, 0.35, 0.45, 0.35, 0.02);          // el brillo de los shinies
            if (en.rumboT > 0) { en.correr(); continue; }                                  // estampida: sigue su carril
            if (en.huye) { Run cerca = masCercano(objetivos, en.e.getX(), en.e.getZ()); if (cerca != null) en.huir(cerca.pp.x, cerca.pp.z, ox, oz, Arena.RADIO); continue; }       // el Michialien dorado: huye
            Run obj = masCercano(objetivos, en.e.getX(), en.e.getZ());
            if (obj == null) en.mover(en.e.getX(), en.e.getZ(), 0);
            else if (en.pers == Pers.EMBISTE) en.embestir(obj, w, rnd);
            else if (en.ataque != null) en.atacar(this, obj, clima.velEnemigos());
            else en.mover(obj.pp.x, obj.pp.z, clima.velEnemigos());
        }
        if (jefe != null && jefe.vivo()) jefe.tick(objetivos);
        tickBombas(objetivos);
        for (Iterator<DisparoEnemigo> it = disparos.iterator(); it.hasNext(); ) if (!it.next().tick(w, objetivos)) it.remove();
        for (Enemigo en : enemigos) {
            if (en.tesoro) continue;
            if (en.cdContacto > 0) en.cdContacto--;
            if (en.cdContacto > 0) continue;
            for (Run m : objetivos) {
                if (m.iframes > 0) continue;
                double dx = en.e.getX() - m.pp.x, dz = en.e.getZ() - m.pp.z, r = en.radio + 0.4;
                if (dx * dx + dz * dz < r * r && Math.abs(en.e.getY() - m.pp.y) < (en.grande() ? 3.0 : 1.8)) {
                    en.cdContacto = 20;
                    m.danarJugador(en.dmg, en);
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
        recogerObjetos();
        revivir();

        // 6) interfaz y medidas
        med.registrar(System.nanoTime() - t0, Partida.server);
        if (tick > RAMPA + 60 && tick % 10 == 0) { sumaEstable += med.msptServidor(); sumaNuestroEstable += med.msNuestro(); nEstable++; }
        if (tick % 4 == 0) for (Run m : miembros) m.enviarEstado();
        if (tick % 2 == 0) for (Run m : miembros) m.enviarGemas();
        if (tick % 100 == 0 && Boolean.getBoolean("emi.survivors.debug")) {
            java.util.Map<String, Integer> poses = new java.util.TreeMap<>();
            for (Enemigo en : enemigos) { try { poses.merge(en.e.getCurrentPoseType().name(), 1, Integer::sum); } catch (Throwable ex) { poses.merge("?", 1, Integer::sum); } }
            EmiSurvivors.LOG.info("[poses] {}", poses);
        }
        if (tick % 100 == 0) EmiSurvivors.LOG.info("[medida] {} t={}s nivel={} enemigos={} gemas={} derrotados={} hp={} | nuestro={} ms/tick (max {}) | servidor={} ms/tick (media de 100, max {})",
                nombres(), segundos(), miembros.get(0).nivel, enemigos.size(), gemas.size(), derrotados, resumenVida(), Run.fmt(med.msNuestro()), Run.fmt(med.msNuestroMax()), Run.fmt(med.msptServidor()), Run.fmt(med.msptMax()));

        // 7) quien se queda sin vida cae; si caen todos, se acabo
        for (Run m : miembros) if (m.estado == Run.VIVO && m.hp <= 0) m.caer();
        if (vivos().isEmpty()) {
            for (Run m : miembros) { ServerPlayerEntity p = m.jugador(); Musica.sonarDespues(p, Sonidos.DERROTA, 700, 1.0f); Musica.silencio(p, 5500); }       // se oye la melodia triste; luego suena la de la sala
            Partida.terminarEquipo(this, miembros.size() == 1 ? "Te han derrotado." : "Han caído todos los jugadores.");
            return;
        }
        if (acabarGanando) { Partida.terminarEquipo(this, "¡Habéis derrotado al Michi Gigante!"); return; }
        if (victoria) { victoria = false; alVencerGigante(); }
        if (sentencia.superada() && !faseFinal) { if (Mundos.ultimoDeVuelta(mundo)) empezarFinal(); else pasarDeMundo(false); }
        if (faseFinal && !gigantePuesto) gigantePuesto = crearJefe(Jefes.GIGANTE_N);
        for (Run m : miembros) {
            if (m.estado != Run.VIVO || m.esperandoCarta) continue;
            if (m.retrasoCartas > 0) { m.retrasoCartas--; continue; }               // tras viajar de mundo se ve el titulo antes de que salgan las cartas
            if (m.pendientes > 0) m.ofrecerCartas();
            else if (m.fusiones > 0) m.ofrecerFusion();
        }
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
    Vec3d sitioDeAparicion(double distMin, double distMax) {
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
        Especies.E sp = null; String extra = ""; Enemigo.Tipo tipoE = pedido;
        boolean normal = pedido != Enemigo.Tipo.ELITE;
        if (!normal) {
            List<Especies.E> pool = pool2.isEmpty() ? pool1 : pool2;
            if (pool.isEmpty()) return false;
            sp = pool.get(rnd.nextInt(pool.size()));
        } else if (marea != null && marea.tipo == Mareas.Tipo.MICHIS && Especies.michi() != null) {          // marea de Michialiens: todos lo son
            sp = Especies.michi(); tipoE = Enemigo.Tipo.MICHI; extra = Especies.FORMAS_MICHI[rnd.nextInt(Especies.FORMAS_MICHI.length)];
        } else if (marea != null && marea.tipo == Mareas.Tipo.TIPO && marea.pk != null) {                    // marea de un tipo: todos son de ese tipo
            List<Especies.E> l = Especies.deTipo(marea.pk.id(), 180, 405);
            if (!l.isEmpty()) { sp = l.get(rnd.nextInt(l.size())); tipoE = sp.bst() > 330 ? Enemigo.Tipo.FUERTE : Enemigo.Tipo.DEBIL; }
        }
        if (sp == null) {
            if (rnd.nextDouble() < Curvas.P_MICHI && Especies.michi() != null) {             // 1 de cada 10 es un Michialien (el toque de Emi)
                sp = Especies.michi(); tipoE = Enemigo.Tipo.MICHI; extra = Especies.FORMAS_MICHI[rnd.nextInt(Especies.FORMAS_MICHI.length)];
            } else {
                boolean fuerte = rnd.nextDouble() < (fijo > 0 ? 0.3 : Math.min(0.9, Curvas.pFuerte(t) + Curvas.mundoFuerte(mundo)));
                List<Especies.E> pool = pool(fuerte);
                if (pool.isEmpty()) return false;
                sp = elegirNormal(pool);
                tipoE = fuerte ? Enemigo.Tipo.FUERTE : Enemigo.Tipo.DEBIL;
            }
        }
        Vec3d s = sitioDeAparicion(21, 26);
        if (s == null) return false;
        double tm = fijo > 0 ? 0 : t;
        double hpMul = Curvas.vidaMul(tm) * Curvas.escalaVida(miembros.size()) * (fijo > 0 ? 1 : Curvas.mundoVida(mundo)), velMul = Curvas.velMul(tm) * (fijo > 0 ? 1 : Curvas.mundoVel(mundo));
        if (marea != null && marea.tipo == Mareas.Tipo.MICHIS && normal) { hpMul *= Mareas.VIDA_MICHIS; velMul *= Mareas.VELOCIDAD_MICHIS; }
        boolean shiny = normal && fijo == 0 && tipoE != Enemigo.Tipo.MICHI && rnd.nextDouble() < Curvas.P_SHINY;
        Enemigo en = Enemigo.crear(w, sp, shiny ? (extra + " shiny=yes").trim() : extra, s, tipoE, hpMul, Curvas.danoMul(tm) * (fijo > 0 ? 1 : Curvas.mundoDano(mundo)), velMul, 0f, rnd);
        if (en == null) { fallosCrear++; return false; }
        enemigos.add(en);
        especiesVistas.add(sp.id());
        if (shiny) marcarShiny(en);
        if (normal && !shiny && fijo == 0 && (marea == null || marea.tipo != Mareas.Tipo.MICHIS) && rnd.nextDouble() < Pers.probabilidad(t) * Curvas.mundoPers(mundo)) aplicarPers(en, Pers.sortear(rnd));
        if (en.pers == null && !shiny && fijo == 0 && tm >= 0 && (normal ? rnd.nextDouble() < Ataque.probabilidad(t) * Curvas.mundoPers(mundo) && (marea == null || marea.tipo != Mareas.Tipo.MICHIS) : true)) darAtaque(en);
        return true;
    }

    /** Le da a un enemigo el ataque que le toca por su tipo (ver {@link Ataque}). Hay como mucho 24 enemigos con ataque a la vez (cuesta). */
    void darAtaque(Enemigo en) {
        int n = 0;
        for (Enemigo o : enemigos) if (o.ataque != null) n++;
        if (n >= 24) return;
        en.ataque = Ataque.paraTipo(en.especie.tipo1(), en.especie.tipo2());
        en.atT = 30 + rnd.nextInt(60);                          // para que no disparen todos a la vez nada mas aparecer
        avisarAtaque();
    }

    private boolean avisoShiny;

    /** Convierte a un enemigo en un shiny (el aspecto variocolor ya lo trae de {@link Enemigo#crear}): mas vida, 5 veces mas experiencia, aviso y brillo. */
    void marcarShiny(Enemigo en) {
        en.shiny = true;
        en.hp *= 2f; en.hpMax = en.hp;
        en.xp *= 5;
        Colores.marcar(en, "yellow");                       // contorno amarillo para verlo entre la horda
        try { if (!en.e.getPokemon().getShiny()) { EmiSurvivors.LOG.warn("[shiny] la propiedad shiny=yes no bastó para {}; se pone a mano", en.especie.id()); en.e.getPokemon().setShiny(true); } }
        catch (Throwable ex) { /* se queda con el aspecto que tenga */ }
        for (Run m : miembros) Sonidos.sfx(m.jugador(), Sonidos.BRILLO, 0.7f, 1.4f);
        avisar(Msg.say(avisoShiny ? "✦ ¡Un shiny!" : "✦ ¡Ha aparecido un Pokémon shiny (variocolor, con contorno amarillo)! Derrótalo: da mucha experiencia, un objeto seguro y michiestrellitas extra."));
        avisoShiny = true;
    }

    /** Solo para pruebas (/survivor dev shiny): un shiny a unos 8 bloques del jugador. */
    boolean devShiny(Run r) {
        double a = rnd.nextDouble() * Math.PI * 2;
        double x = r.pp.x + Math.cos(a) * 8, z = r.pp.z + Math.sin(a) * 8;
        if (!Arena.dentro(this, x, z, 3.0)) { x = r.pp.x - Math.cos(a) * 8; z = r.pp.z - Math.sin(a) * 8; }
        double t = tick / 20.0;
        List<Especies.E> pool = pool(false);
        if (pool.isEmpty()) return false;
        Especies.E sp = elegirNormal(pool);
        Enemigo en = Enemigo.crear(w, sp, "shiny=yes", new Vec3d(x, Arena.Y0, z), Enemigo.Tipo.DEBIL, Curvas.vidaMul(t) * Curvas.escalaVida(miembros.size()) * Curvas.mundoVida(mundo), Curvas.danoMul(t) * Curvas.mundoDano(mundo), Curvas.velMul(t) * Curvas.mundoVel(mundo), 0f, rnd);
        if (en == null) return false;
        enemigos.add(en);
        marcarShiny(en);
        return true;
    }

    /** Le da una personalidad a un enemigo (ver {@link Pers}): cambia sus numeros, lo agranda si hace falta y le pone el contorno de su color. */
    void aplicarPers(Enemigo en, Pers p) {
        en.pers = p;
        en.hp *= (float) p.vida; en.hpMax = en.hp; en.dmg *= (float) p.dano; en.vel *= p.vel;
        if (p == Pers.BRUTO) en.xp *= 2;
        if (p.tam != 1.0) {
            try { en.e.getPokemon().setScaleModifier((float) (en.e.getPokemon().getScaleModifier() * p.tam)); en.radio *= p.tam; } catch (Throwable ex) { /* se queda del tamano normal */ }
        }
        Colores.marcar(en, p.color);
        if (!avisoPers) { avisoPers = true; avisar(Msg.say(Pers.LEYENDA)); }
    }

    /**
     * Cuantos enemigos mas puede traer ahora un suceso (el cerco, la estampida): los sucesos se saltan el tope normal de enemigos, asi que tienen su propio margen
     * (30 por encima del tope del equipo) y se reducen si el servidor va justo ({@link Partida#factorRendimiento}).
     */
    int espacioEventos() {
        int tope = Math.min(Curvas.TOPE_EQUIPO, Partida.topePorEquipo() + 40) + 30;
        return (int) Math.max(0, (tope - enemigos.size()) * Partida.factorRendimiento());
    }

    /** Un enemigo normal en un sitio dado (los usan las mareas: el cerco). Devuelve null si no se pudo. */
    Enemigo crearNormalEn(double x, double z) {
        double t = tick / 20.0;
        boolean fuerte = rnd.nextDouble() < Curvas.pFuerte(t);
        List<Especies.E> pool = pool(fuerte);
        if (pool.isEmpty()) return null;
        Especies.E sp = elegirNormal(pool);
        Enemigo en = Enemigo.crear(w, sp, "", new Vec3d(x, Arena.Y0, z), fuerte ? Enemigo.Tipo.FUERTE : Enemigo.Tipo.DEBIL, Curvas.vidaMul(t) * Curvas.escalaVida(miembros.size()) * Curvas.mundoVida(mundo), Curvas.danoMul(t) * Curvas.mundoDano(mundo), Curvas.velMul(t) * Curvas.mundoVel(mundo), 0f, rnd);
        if (en == null) { fallosCrear++; return null; }
        enemigos.add(en);
        especiesVistas.add(sp.id());
        return en;
    }

    /** Solo para pruebas (/survivor dev pers): crea un enemigo con esa personalidad a unos 8 bloques del jugador. */
    boolean devPers(Pers p, Run r) {
        double a = rnd.nextDouble() * Math.PI * 2;
        double x = r.pp.x + Math.cos(a) * 8, z = r.pp.z + Math.sin(a) * 8;
        if (!Arena.dentro(this, x, z, 3.0)) { x = r.pp.x - Math.cos(a) * 8; z = r.pp.z - Math.sin(a) * 8; }
        Enemigo en = crearNormalEn(x, z);
        if (en == null) return false;
        aplicarPers(en, p);
        return true;
    }

    /** Solo para pruebas (/survivor dev ataque <nombre>): un enemigo con ese ataque a unos 8 bloques del jugador. */
    boolean devAtaque(Ataque a, Run r) {
        double ang = rnd.nextDouble() * Math.PI * 2;
        double x = r.pp.x + Math.cos(ang) * 8, z = r.pp.z + Math.sin(ang) * 8;
        if (!Arena.dentro(this, x, z, 3.0)) { x = r.pp.x - Math.cos(ang) * 8; z = r.pp.z - Math.sin(ang) * 8; }
        Enemigo en = crearNormalEn(x, z);
        if (en == null) return false;
        en.ataque = a; en.atT = 20;
        avisarAtaque();
        return true;
    }

    /** Un enemigo de la estampida: sale de (x, z) y corre en la direccion (ux, uz) por un carril de {@code largo} bloques, sin perseguir a nadie, y se va al llegar al final. */
    void crearCorredor(double x, double z, double ux, double uz, double largo) {
        double t = tick / 20.0;
        boolean fuerte = rnd.nextDouble() < Curvas.pFuerte(t);
        List<Especies.E> pool = pool(fuerte);
        if (pool.isEmpty()) return;
        Especies.E sp = elegirNormal(pool);
        Enemigo en = Enemigo.crear(w, sp, "", new Vec3d(x, Arena.Y0, z), fuerte ? Enemigo.Tipo.FUERTE : Enemigo.Tipo.DEBIL, Curvas.vidaMul(t) * Curvas.escalaVida(miembros.size()) * Curvas.mundoVida(mundo), Curvas.danoMul(t) * 1.3 * Curvas.mundoDano(mundo), Curvas.velMul(t) * Curvas.mundoVel(mundo), 0f, rnd);
        if (en == null) { fallosCrear++; return; }
        en.rdx = ux; en.rdz = uz;
        en.rumboT = (int) (largo / (en.vel * 3.0)) + 15;
        float yaw = (float) Math.toDegrees(Math.atan2(-ux, uz));
        en.e.setYaw(yaw); en.e.setHeadYaw(yaw); en.e.setBodyYaw(yaw);
        enemigos.add(en);
        especiesVistas.add(sp.id());
    }

    /** Empieza la marea numero {@code k} (si {@code forzado} es null, la que le toca por orden; si no, ese tipo: solo para pruebas). */
    void iniciarMarea(int k, Mareas.Tipo forzado) {
        Mareas.Tipo tp = forzado != null ? forzado : Mareas.tipo(k);
        Mareas.TipoPk pk = tp == Mareas.Tipo.TIPO ? Mareas.tipoAlAzar(rnd) : null;
        if (marea != null) marea.limpiar();
        marea = new Marea(this, tp, k, pk);
    }

    // ------------------------------------------------------------------------------------------------ bombas de los enemigos explosivos

    /** La bomba que deja un enemigo explosivo al caer: un cuadrado rojo en el suelo que se rellena durante 1,2 s y explota. */
    private final class Bomba {
        static final double LADO = 6.0;
        static final int AVISO = 24;
        final double x, z;
        final double lado;                  // lado del cuadrado (6 las de los explosivos y las de los eventos; menos las de los enemigos con mortero)
        final float dano;                   // -1 = el de las bombas normales (Pers.danoBomba)
        int t;
        net.minecraft.entity.Entity fondo, relleno;

        Bomba(double x, double z) { this(x, z, LADO, -1f); }

        Bomba(double x, double z, double lado, float dano) {
            this.x = x; this.z = z; this.lado = lado; this.dano = dano;
            fondo = Marcas.crear(Equipo.this, Marcas.CRISTAL_ROJO, x, Marcas.Y_FONDO, z, 0, (float) lado, 0.05f, (float) lado, false);
            relleno = Marcas.crear(Equipo.this, Marcas.ALFOMBRA_ROJA, x, Marcas.Y_RELLENO, z, 0, 0.2f, 1f, 0.2f, false);
        }
    }

    /** El mortero de un enemigo a distancia: un cuadrado rojo pequeno donde estabas que explota en 1,2 s. Con 16 bombas a la vez no se pone mas. */
    void ponerBombaPequena(double x, double z, double lado, float dano) { if (bombas.size() < 16) bombas.add(new Bomba(x, z, lado, dano)); }

    /** Una bola de un enemigo a distancia (hasta 60 a la vez). */
    void ponerDisparo(DisparoEnemigo d) { if (disparos.size() < 60) disparos.add(d); }

    /** La primera vez que sale un enemigo con ataque se explica que se avisan. */
    void avisarAtaque() { if (!avisoAtaque) { avisoAtaque = true; avisar(Msg.say(Ataque.LEYENDA)); } }

    /** Deja una bomba avisada en el suelo (la misma de los enemigos explosivos); con 12 a la vez no se pone mas. */
    void ponerBomba(double x, double z) { if (bombas.size() < 12) bombas.add(new Bomba(x, z)); }

    private void tickBombas(List<Run> objetivos) {
        for (Iterator<Bomba> it = bombas.iterator(); it.hasNext(); ) {
            Bomba b = it.next();
            b.t++;
            if (b.t == 2) Marcas.poner(b.relleno, b.x, Marcas.Y_RELLENO, b.z, 0, (float) b.lado, 1f, (float) b.lado, false, Bomba.AVISO - 2);
            if (b.t < Bomba.AVISO) continue;
            Marcas.quitar(this, b.fondo); Marcas.quitar(this, b.relleno);
            w.spawnParticles(ParticleTypes.EXPLOSION, b.x, Arena.Y0 + 0.8, b.z, 2, 1.0, 0.3, 1.0, 0.0);
            w.spawnParticles(ParticleTypes.FLAME, b.x, Arena.Y0 + 0.6, b.z, 24, b.lado * 0.25, 0.4, b.lado * 0.25, 0.08);
            for (Run m : miembros) { double dd = Math.hypot(m.pp.x - b.x, m.pp.z - b.z); if (dd < 30) Sonidos.sfx(m.jugador(), Sonidos.EXPLOSION_SUAVE, 0.9f * (float) (1.0 - dd / 30.0), 1.1f); }
            for (Run m : objetivos) if (Marcas.dentroDeCuadrado(m.pp.x, m.pp.z, b.x, b.z, b.lado, 0.3)) m.danarJugador(b.dano >= 0 ? b.dano : Pers.danoBomba(tick / 20.0));
            it.remove();
        }
    }

    /** Solo para pruebas (/survivor dev elite): crea un campeon ya. */
    boolean devElite() { return crearEnemigo(Enemigo.Tipo.ELITE); }

    boolean gigantePuesto;

    // ------------------------------------------------------------------------------------------------ duelo con un legendario

    /**
     * El equipo viaja al escenario del duelo (a 300 bloques de la arena, en el mismo mundo): lo que haya en la arena (enemigos, marcas, tesoros, marea, evento) se quita y el
     * reloj del mundo se queda parado; las gemas y los objetos que hubiera se quedan donde estan. Lo vuelve a sacar {@link #salirDuelo}.
     */
    void entrarDuelo() {
        for (Enemigo en : enemigos) en.quitar();
        enemigos.clear(); refuerzos.clear(); bombas.clear(); disparos.clear();
        if (marea != null) { marea.limpiar(); marea = null; }
        evento.limpiar(); tesoros.limpiar();
        Marcas.quitarTodas(this);
        for (Run m : miembros) m.proyectiles.clear();
        Arena.prepararDuelo(w, slot);
        double[] o = Arena.origenDuelo(slot);
        cx = o[0]; cz = o[1]; radio = Arena.RADIO_DUELO;
        duelo = true; tickDuelo0 = tick;
        int n = miembros.size(), k = 0;
        for (Run m : miembros) {
            m.posAntes = m.pp;
            ServerPlayerEntity p = m.jugador();
            double ang = Math.PI + k++ * Math.PI * 2 / n, dist = n == 1 ? 12 : 11;
            if (p != null) { p.teleport(w, cx + 0.5 + Math.cos(ang) * dist, Arena.Y0, cz + 0.5 + Math.sin(ang) * dist, -90f, 0f); m.pp = p.getPos(); }
            m.iframes = Math.max(m.iframes, 40);
        }
    }

    /** El duelo ha terminado: todos vuelven a la arena, al sitio donde estaban, y el reloj del mundo sigue donde se quedo. */
    void salirDuelo() {
        if (!duelo) return;
        for (Enemigo en : enemigos) en.quitar();
        enemigos.clear(); bombas.clear(); disparos.clear();
        Marcas.quitarTodas(this);
        gemas.removeIf(g -> g.x > ox + 150); objetos.removeIf(o -> o.x > ox + 150);
        tesoros.limpiar();                                   // los barriles se vuelven a poner
        cx = ox; cz = oz; radio = Arena.RADIO;
        duelo = false;
        tickMundo0 += tick - tickDuelo0;
        for (Run m : miembros) {
            ServerPlayerEntity p = m.jugador();
            Vec3d d = m.posAntes != null && Arena.dentro(this, m.posAntes.x, m.posAntes.z, 2.0) ? m.posAntes : new Vec3d(ox + 0.5, Arena.Y0, oz + 0.5);
            if (p != null) { p.teleport(w, d.x, Arena.Y0, d.z, p.getYaw(), 0f); m.pp = p.getPos(); }
            m.iframes = Math.max(m.iframes, 60);
        }
    }

    /** Pasa al siguiente mundo: lo del mundo viejo se limpia, el equipo viaja a la arena del nuevo y los horarios (jefes, mareas, climas...) vuelven a empezar. */
    private void pasarDeMundo(boolean masAlla) {
        Biomas.Bioma nuevo = Mundos.siguiente(mundo);
        ServerWorld w2 = Partida.mundo(nuevo);
        if (w2 == null) { EmiSurvivors.LOG.warn("[mundos] no existe la dimension de {}: se acaba la partida con victoria", nuevo.id()); ganado = true; acabarGanando = true; return; }
        EmiSurvivors.LOG.info("[mundos] {} pasa de {} a {} a los {} s", nombres(), bioma.id(), nuevo.id(), segundos());
        limpiar();
        jefe = null;
        if (duelo) { duelo = false; cx = ox; cz = oz; radio = Arena.RADIO; }          // por si se viaja en pleno duelo (solo con comandos de prueba): el nuevo mundo no tiene escenario de duelo
        Arena.preparar(w2, slot, nuevo);
        Arena.limpiarRestos(w2, slot);
        ServerWorld viejo = w;
        w = w2; bioma = nuevo; mundo++; tickMundo0 = tick; mareasBase = mareasSalidas; jefesSalidos = Jefes.POR_MUNDO * mundo;
        prepararEspecies();
        clima = new ClimaEquipo(this); grieta = new GrietaEquipo(this); sentencia = new SentenciaEquipo(this); evento = new EventoEquipo(this); tesoros = new TesorosEquipo(this);
        int n = miembros.size(), k = 0;
        for (Run m : miembros) {
            ServerPlayerEntity p = m.jugador();
            m.w = w2;
            if (m.estado == Run.CAIDO) m.revivir();
            m.hp = m.st.hpMax;
            m.nivel += 2; m.pendientes += 2; m.retrasoCartas = 100;      // premio de superar el mundo (las cartas salen a los 5 s)
            m.iframes = Math.max(m.iframes, 60);
            double ang = k++ * Math.PI * 2 / n, dist = n == 1 ? 0 : 2.5;
            if (p != null) {
                p.teleport(w2, ox + 0.5 + Math.cos(ang) * dist, Arena.Y0, oz + 0.5 + Math.sin(ang) * dist, 180f, 0f);
                m.pp = p.getPos();
                if (masAlla) Msg.title(p, Text.literal(vueltasHechas == 1 ? "¡VICTORIA!" : "¡OTRO MICHI GIGANTE VENCIDO!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal("Pero esto no ha terminado: EL MÁS ALLÁ · vuelta " + (vuelta() + 1)).formatted(Formatting.LIGHT_PURPLE), 5, 90, 20);
                else Msg.title(p, Text.literal("¡MUNDO SUPERADO!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal(nuevo.nombre() + (vuelta() > 0 ? " · Más allá " + (vuelta() + 1) : "") + " · +2 niveles y vida completa").formatted(Formatting.YELLOW), 5, 70, 15);
                Sonidos.sfx(p, Sonidos.JEFE_DERROTADO, 1.0f, 1.0f);
                Musica.silencio(p, 2500);
            }
        }
        if (!masAlla) avisar(Msg.say("★ ¡Habéis superado la Sentencia! Viajáis a: " + nuevo.nombre() + (vuelta() > 0 ? " (Más allá, vuelta " + (vuelta() + 1) + ")" : "") + "."));
        if (Curvas.DIFICULTAD > 0) {
            String dif = "⚠ Aquí los enemigos son más duros: +" + Math.round((Curvas.mundoVida(mundo) - 1) * 100) + " % de vida, +" + Math.round((Curvas.mundoDano(mundo) - 1) * 100) + " % de daño, +"
                    + Math.round((Curvas.mundoVel(mundo) - 1) * 100) + " % de velocidad, más enemigos a la vez y campeones cada " + Curvas.eliteCada(mundo) + " s.";
            avisar(Msg.say(dif));
            EmiSurvivors.LOG.info("[mundos] dificultad del mundo {}: {}", mundo, dif);
        }
    }

    /**
     * El Michi Gigante ha caido. La primera vez es la VICTORIA (la partida queda como ganada: premio, marca en el top, logro), pero ya no se acaba: el equipo sigue en
     * «el Mas alla», otra vuelta por la pradera, el bosque y el cosmos con todo mas duro, jefes nuevos mas fuertes y otro Michi Gigante al final de cada vuelta. La
     * partida acaba cuando caen todos o el equipo se va (el tiempo que aguanten sigue contando para el top).
     */
    private void alVencerGigante() {
        boolean primera = !ganado;
        ganado = true; vueltasHechas++;
        faseFinal = false; gigantePuesto = false;
        EmiSurvivors.LOG.info("[mundos] {} vence al Michi Gigante a los {} s (vuelta {} del Mas alla)", nombres(), segundos(), vueltasHechas);
        avisar(Msg.say(primera
                ? "★★ ¡VICTORIA! Habéis derrotado al Michi Gigante. Pero la grieta del cosmos no se ha cerrado: seguís hacia EL MÁS ALLÁ, donde todo es más duro (jefes nuevos, otro Michi Gigante al final de cada vuelta y " + Curvas.MONEDAS_POR_VUELTA + " michiestrellitas más por cada uno). Podéis salir cuando queráis con /survivor salir."
                : "★ ¡Otro Michi Gigante vencido! Seguís hacia la vuelta " + (vueltasHechas + 1) + " del Más allá."));
        pasarDeMundo(true);
    }

    /** Solo para pruebas (/survivor dev mundo): salta lo que falte del mundo actual (pasa al siguiente, o llama al Michi Gigante si es el cosmos). */
    void devSaltarMundo() {
        if (faseFinal) return;
        if (Mundos.ultimoDeVuelta(mundo)) empezarFinal(); else pasarDeMundo(false);
    }

    /** Se paso la Sentencia del ultimo mundo de la vuelta (el cosmos): sale el Michi Gigante. */
    private void empezarFinal() {
        faseFinal = true;
        EmiSurvivors.LOG.info("[mundos] {} empieza el combate final a los {} s", nombres(), segundos());
        avisar(Msg.say("★ ¡Habéis superado la última Sentencia! Pero algo enorme se acerca..."));
    }

    boolean jefeDerrotado() { return jefesDerrotados > 0; }

    /**
     * El jefe numero {@code n} (ver {@link Jefes}): su especie de Emipokemon si esta instalado (si no, la de repuesto de Cobblemon), enorme, a unos 20 bloques de un jugador.
     * Devuelve false si no ha podido (no hay sitio: se vuelve a intentar el siguiente tick; o no hay especie: se salta ese jefe).
     */
    boolean crearJefe(int n) {
        Jefes.Def d = Jefes.def(n);
        Especies.E sp = Especies.porId(d.especie());
        if (sp == null) sp = Especies.porId(d.alternativa());
        if (sp == null) { EmiSurvivors.LOG.warn("No hay especie para el jefe {} ({} ni {}): se salta", d.nombre(), d.especie(), d.alternativa()); jefesSalidos = n; return false; }
        Vec3d s = sitioDeAparicion(17, 22);
        if (s == null) return false;
        d = Jefes.deLaVuelta(d, n, vuelta());
        float hp = (float) (Jefes.vida(n, vuelta()) * Curvas.escalaJefe(miembros.size()));
        float dmgJefe = Jefes.dano(n, vuelta());
        Enemigo en = Enemigo.crear(w, sp, d.extra(), s, Enemigo.Tipo.JEFE, 1, 1.0, 1, hp, rnd, d.alto(), dmgJefe, d.vel());
        if (en == null) { fallosCrear++; return false; }
        jefesSalidos = n;
        enemigos.add(en);
        jefe = new Jefe(this, en, d, n, dmgJefe);
        Formatting color = Formatting.byName(d.color());
        if (color == null) color = Formatting.LIGHT_PURPLE;
        for (Run m : miembros) {
            ServerPlayerEntity p = m.jugador();
            if (p == null) continue;
            Msg.title(p, Text.literal("¡" + d.nombre().toUpperCase() + "!").formatted(color, Formatting.BOLD), Text.literal(d.aviso()).formatted(Formatting.YELLOW), 5, 60, 15);
            Sonidos.sfx(p, Sonidos.JEFE_APARECE, 0.85f, 1.0f);
        }
        EmiSurvivors.LOG.info("[jefe] {}: sale el jefe {} ({}) a los {} s con {} de vida", nombres(), n, d.nombre(), segundos(), Math.round(hp));
        return true;
    }

    /** Un jefe invoca {@code n} refuerzos alrededor de (bx, bz): se crean poco a poco en los siguientes ticks (crear un Pokemon de Cobblemon cuesta). */
    void invocar(Jefes.Def d, int n, double bx, double bz, double radio) {
        refuerzosDe = d;
        for (int k = 0; k < n; k++) {
            double ang = k * Math.PI * 2 / n + rnd.nextDouble() * 0.4, r = radio + 1.5 + rnd.nextDouble() * 3.0;
            double x = bx + Math.cos(ang) * r, z = bz + Math.sin(ang) * r;
            if (!Arena.dentro(this, x, z, 2.0)) { x = bx + (ox - bx) * 0.3; z = bz + (oz - bz) * 0.3; }
            refuerzos.add(new double[]{x, z});
        }
    }

    private void crearRefuerzos() {
        for (int k = 0; k < MAX_NUEVOS_POR_TICK && !refuerzos.isEmpty(); k++) {
            double[] p = refuerzos.remove(refuerzos.size() - 1);
            Jefes.Def d = refuerzosDe;
            Especies.E sp = null; String extra = ""; Enemigo.Tipo tipo = Enemigo.Tipo.DEBIL;
            if (d != null) for (String id : d.secuaz().split("\\|")) {
                if (id.isEmpty()) continue;
                if (id.equals("gatitoalien")) {
                    sp = Especies.michi();
                    if (sp != null) { tipo = Enemigo.Tipo.MICHI; extra = Especies.FORMAS_MICHI[rnd.nextInt(Especies.FORMAS_MICHI.length)]; }
                } else sp = Especies.porId(id);
                if (sp != null) break;
            }
            if (sp == null) {                                                                  // sin especie propia: un enemigo normal cualquiera
                List<Especies.E> pool = pool1.isEmpty() ? pool2 : pool1;
                if (pool.isEmpty()) continue;
                sp = pool.get(rnd.nextInt(pool.size()));
            }
            double t = tick / 20.0;
            Enemigo en = Enemigo.crear(w, sp, extra, new Vec3d(p[0], Arena.Y0, p[1]), tipo, Curvas.vidaMul(t) * Curvas.escalaVida(miembros.size()) * Curvas.mundoVida(mundo) * 0.6, Curvas.danoMul(t) * Curvas.mundoDano(mundo), Curvas.velMul(t) * Curvas.mundoVel(mundo) * 1.15, 0f, rnd);
            if (en == null) { fallosCrear++; continue; }
            enemigos.add(en);
            especiesVistas.add(sp.id());
        }
    }

    // ------------------------------------------------------------------------------------------------ muertes y gemas

    private void muerte(Enemigo en) {
        double x = en.e.getX(), y = en.e.getY(), z = en.e.getZ();
        if (en.sinBotin) { en.quitar(); return; }                    // un corredor de la estampida que llego al final: se va sin mas
        if (en.tesoro) { en.quitar(); tesoros.alRomper(en, x, z); return; }       // un tesoro pequeño roto: no cuenta como derrotado
        w.spawnParticles(ParticleTypes.POOF, x, y + 0.5, z, en.grande() ? 25 : 6, 0.3, 0.3, 0.3, 0.02);
        en.quitar();
        derrotados++;
        if (en.pers == Pers.EXPLOSIVO && bombas.size() < 12) bombas.add(new Bomba(x, z));
        if (en.legendario) grieta.alDerrotar(en, x, z);
        if (en.dorado) { evento.alDerrotarDorado(en, x, z); return; }
        if (en.ultimo != null && miembros.contains(en.ultimo)) { en.ultimo.derrotados++; en.ultimo.alDerrotar(en); }
        switch (en.tipo) {
            case JEFE -> {
                Jefe j = jefe;
                if (j != null) j.limpiar();
                int n = j != null ? j.numero : jefesDerrotados + 1;
                jefesDerrotados++; jefe = null;
                if (n == Jefes.GIGANTE_N) victoria = true;                  // lo atiende el tick del equipo (alVencerGigante)
                for (int k = 0; k < 6; k++) { double a = k * Math.PI / 3; gemas.add(new Gema(x + Math.cos(a) * 2.5, z + Math.sin(a) * 2.5, 25 + 10 * (n - 1))); }
                for (Run m : miembros) m.premioJefe(n, j == null ? "El jefe" : j.nombre, Jefes.esElUltimo(n));
                soltarObjeto(Objeto.BAYA, x + 1.8, z); soltarObjeto(Objeto.BAYA, x - 1.8, z); soltarObjeto(Objeto.IMAN, x, z + 1.8);
                if (n >= 3) soltarObjeto(Objeto.CARAMELO, x, z - 1.8);
                EmiSurvivors.LOG.info("[jefe] {}: jefe {} derrotado a los {} s", nombres(), n, segundos());
            }
            case ELITE -> {
                for (int k = 0; k < 3; k++) { double a = k * Math.PI * 2 / 3; gemas.add(new Gema(x + Math.cos(a) * 1.2, z + Math.sin(a) * 1.2, 5)); }
                if (fijo == 0) soltarObjeto(Objeto.sortear(rnd, true), x, z);
            }
            default -> {
                gemas.add(new Gema(x, z, en.xp));
                if (en.shiny) {
                    w.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y + 0.8, z, 30, 0.5, 0.6, 0.5, 0.25);
                    for (Run m : miembros) Sonidos.sfx(m.jugador(), Sonidos.BRILLO, 0.9f, 1.0f);
                    if (fijo == 0) soltarObjeto(Objeto.sortear(rnd, true), x, z);
                    if (en.ultimo != null) { ServerPlayerEntity pj = en.ultimo.jugador(); if (pj != null) pj.sendMessage(Msg.say("✦ ¡Shiny derrotado! +" + Curvas.MONEDAS_POR_SHINY + " michiestrellitas al acabar la partida y un objeto en el suelo."), false); }
                }
                if (fijo == 0 && rnd.nextDouble() < Curvas.P_OBJETO) soltarObjeto(Objeto.sortear(rnd, false), x, z);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ objetos del suelo

    /** Deja un objeto en el suelo (si hay demasiados, desaparece el mas viejo). Dentro de la arena. */
    void soltarObjeto(int tipo, double x, double z) {
        double dx = x - cx, dz = z - cz, lim = radio - 2.5, d = Math.sqrt(dx * dx + dz * dz);
        if (d > lim) { x = cx + dx / d * lim; z = cz + dz / d * lim; }
        while (objetos.size() >= MAX_OBJETOS) objetos.remove(0);
        objetos.add(new Objeto(tipo, x, z));
    }

    /** El primer jugador en pie que pisa un objeto se lo queda; si nadie lo coge, desaparece a los 90 s. */
    private void recogerObjetos() {
        for (Iterator<Objeto> it = objetos.iterator(); it.hasNext(); ) {
            Objeto o = it.next();
            if (--o.vida <= 0) { it.remove(); continue; }
            Run quien = null;
            for (Run m : miembros) {
                if (m.estado != Run.VIVO || m.esperandoCarta) continue;
                double dx = m.pp.x - o.x, dz = m.pp.z - o.z;
                if (dx * dx + dz * dz < 1.3 * 1.3) { quien = m; break; }
            }
            if (quien == null) continue;
            it.remove();
            quien.recoger(o.tipo);
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
        objetos.clear();
        refuerzos.clear();
        bombas.clear();
        disparos.clear();
        if (marea != null) { marea.limpiar(); marea = null; }
        clima.limpiar();
        grieta.limpiar();
        evento.limpiar();
        tesoros.limpiar();
        Marcas.quitarTodas(this);
        for (Run m : miembros) m.proyectiles.clear();
        try { int n = Arena.barrer(w, slot); if (n > 0) EmiSurvivors.LOG.info("Arena {} limpia: {} entidades sueltas quitadas al acabar.", slot, n); }   // nada de esta partida (enemigos, objetos, experiencia) queda para la siguiente
        catch (Throwable t) { EmiSurvivors.LOG.warn("No se pudo barrer la arena {}: {}", slot, t.toString()); }
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
