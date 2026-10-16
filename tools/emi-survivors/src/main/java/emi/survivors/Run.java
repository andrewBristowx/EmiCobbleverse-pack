package emi.survivors;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import emi.survivors.net.EstadoPayload;
import emi.survivors.net.GemasPayload;
import emi.survivors.net.TextoPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Una partida de un jugador: el jugador, transformado en su Pokemon, se mueve por la arena mientras los enemigos (Pokemon de Cobblemon sin IA, movidos por codigo)
 * le persiguen; sus ataques se usan solos; los enemigos caidos sueltan gemas de experiencia; al subir de nivel la partida se PAUSA y elige 1 de 3 cartas (ataque
 * nuevo, mejora de un ataque o mejora pasiva); cada minuto sale un campeon y a los 3:00 el jefe. Se acaba cuando la vida llega a 0 (o con /survivor parar).
 * Lo que dura la partida (sin contar las pausas de las cartas) es lo que cuenta para el top.
 * Con {@code fijo > 0} es el modo de MEDIDA de la fase 0: ese numero fijo de enemigos desde el principio, sin oleadas ni jefe (para medir el rendimiento).
 */
final class Run {
    static final int RAMPA = 200;               // modo medida: ticks que tarda en llegar al numero de enemigos pedido (10 s)
    static final int MAX_NUEVOS_POR_TICK = 3;   // crear un Pokemon de Cobblemon cuesta: se reparte
    static final int T_CARTA = Integer.getInteger("emi.survivors.carta_ticks", 20 * 30);         // 30 s para elegir carta; si no, se elige una al azar (la propiedad solo se usa en pruebas)
    static final int MAX_GEMAS = 300;

    final ServerWorld w;
    final UUID id;
    final String nombre;
    final Pokemon pk;
    final String pokeId, especieId;
    final String tipo;
    final Catalogo.Inicial inicial;
    final int slot;
    final double ox, oz;                         // centro de la arena de esta partida
    final boolean inmortal, modoPrueba;
    final int fijo;
    final String morphAntes;
    final Random rnd = new Random();
    final Map<String, Integer> meta;             // mejoras permanentes de este Pokemon
    Stats st = new Stats();
    final Map<String, Integer> armas = new LinkedHashMap<>(), pasivas = new LinkedHashMap<>();
    final Map<String, Armas.Arma> armaObj = new LinkedHashMap<>();
    final List<Enemigo> enemigos = new ArrayList<>();
    final List<Proyectil> proyectiles = new ArrayList<>();
    final List<Gema> gemas = new ArrayList<>();
    final Medidor med = new Medidor();
    final List<Especies.E> pool1, pool2;
    final Set<String> especiesVistas = new HashSet<>();
    int fallosCrear;

    int tick, iframes, derrotados, nivel = 1, xp, xpSig = Curvas.xpPara(1), pendientes;
    double hp, presupuesto;
    boolean esperandoCarta, jefeSalio, jefeDerrotado;
    boolean sinXp;                                  // solo para pruebas (/survivor dev sinxp): no se gana experiencia
    int cartaCuenta;
    List<Cartas.Carta> oferta;
    Jefe jefe;
    Vec3d pp = Vec3d.ZERO;
    private boolean gemasEnviadas;
    private int sonidoGema;
    // medias del servidor cuando ya estan todos los enemigos (para el resumen final)
    double sumaEstable, sumaNuestroEstable; int nEstable;

    Run(ServerWorld w, ServerPlayerEntity p, Pokemon pk, int slot, double ox, double oz, String morphAntes, int fijo, boolean inmortal, boolean modoPrueba, Map<String, Integer> meta) {
        this.w = w; this.id = p.getUuid(); this.nombre = p.getGameProfile().getName(); this.pk = pk; this.slot = slot; this.ox = ox; this.oz = oz;
        this.morphAntes = morphAntes; this.fijo = fijo; this.inmortal = inmortal; this.modoPrueba = modoPrueba; this.meta = new HashMap<>(meta);
        this.pokeId = pk.getUuid().toString();
        this.especieId = pk.getSpecies().getResourceIdentifier().getPath();
        this.tipo = pk.getPrimaryType().getName().toLowerCase(java.util.Locale.ROOT);
        this.inicial = Catalogo.inicial(tipo);
        recalcular();
        hp = st.hpMax;
        agregarArma(inicial.arma(), 1);
        pool1 = Especies.entre(180, 330);
        pool2 = Especies.entre(331, 405);
    }

    ServerPlayerEntity jugador() { return Partida.server == null ? null : Partida.server.getPlayerManager().getPlayer(id); }

    int segundos() { return tick / 20; }

    Vec3d mira() {
        ServerPlayerEntity p = jugador();
        return p == null ? new Vec3d(0, 0, 1) : Vec3d.fromPolar(0f, p.getYaw());
    }

    /** Las particulas de un ataque: el ataque con el que empieza el Pokemon usa las de su tipo; los demas, las suyas. */
    ParticleEffect particula(String armaId, ParticleEffect nativa) { return armaId.equals(inicial.arma()) ? Arena.particula(tipo) : nativa; }

    String nombreArma(String id) { return id.equals(inicial.arma()) ? inicial.nombre() : Catalogo.arma(id).nombre(); }

    // ------------------------------------------------------------------------------------------------ estadisticas y mejoras

    /** Vuelve a calcular las estadisticas (base + mejoras permanentes + mejoras pasivas) y las aplica al jugador. */
    void recalcular() {
        double antes = st.hpMax;
        Stats n = new Stats();
        Meta.aplicar(n, meta);
        Catalogo.aplicarPasivas(n, pasivas);
        st = n;
        if (hp > 0 && n.hpMax > antes) hp += n.hpMax - antes;       // al subir la vida maxima, tambien sube la actual
        if (hp > n.hpMax) hp = n.hpMax;
        ServerPlayerEntity p = jugador();
        if (p != null) Partida.fijarVelocidad(p, n.vel);
    }

    void agregarArma(String idArma, int nivelArma) {
        armas.put(idArma, nivelArma);
        armaObj.put(idArma, Armas.crear(idArma, this, nivelArma));
    }

    // ------------------------------------------------------------------------------------------------ un tick

    void tick() {
        long t0 = System.nanoTime();
        ServerPlayerEntity p = jugador();
        if (p == null || p.isRemoved() || p.getServerWorld() != w) { Partida.terminar(this, "El jugador ya no esta en la arena."); return; }

        // pausa: eligiendo carta (el tiempo no corre, los enemigos estan quietos)
        if (esperandoCarta) {
            if (--cartaCuenta <= 0) elegirCarta(rnd.nextInt(oferta.size()));
            else if (cartaCuenta % 4 == 0) enviarEstado(p);
            return;
        }

        tick++;
        pp = p.getPos();
        if (!Arena.dentro(this, pp.x, pp.z, -2.0)) { p.teleport(w, ox + 0.5, Arena.Y0, oz + 0.5, p.getYaw(), 0f); pp = p.getPos(); }      // por si acaso se sale del muro
        double t = tick / 20.0;

        // 1) crear enemigos
        if (fijo > 0) {
            int meta = Math.min(fijo, (int) ((long) fijo * tick / RAMPA));
            for (int n = 0; n < MAX_NUEVOS_POR_TICK && enemigos.size() < meta; n++) if (!crearEnemigo(Enemigo.Tipo.DEBIL)) break;
        } else {
            int cap = (int) Math.round(Curvas.tope(t) * Partida.factorRendimiento());
            presupuesto = Math.min(3.0, presupuesto + Curvas.ritmo(t) / 20.0);
            for (int n = 0; n < MAX_NUEVOS_POR_TICK && presupuesto >= 1.0 && enemigos.size() < cap; n++) {
                if (!crearEnemigo(Enemigo.Tipo.DEBIL)) break;
                presupuesto -= 1.0;
            }
            if (tick % (20 * Curvas.CADA_ELITE) == 0) { if (crearEnemigo(Enemigo.Tipo.ELITE)) Msg.actionBar(p, Text.literal("¡Cuidado, se acerca un campeón!").formatted(Formatting.GOLD, Formatting.BOLD)); }
            if (!jefeSalio && t >= Curvas.T_JEFE) crearJefe(p);
        }

        // 2) separar a los enemigos entre si (rejilla de 1,6 bloques), moverlos y contar el dano por contacto
        separar();
        if (iframes > 0) iframes--;
        for (Enemigo en : enemigos) if (jefe == null || en != jefe.en) en.mover(pp.x, pp.z);
        if (jefe != null && jefe.vivo()) jefe.tick(pp);
        for (Enemigo en : enemigos) {
            if (en.cdContacto > 0) en.cdContacto--;
            double dx = en.e.getX() - pp.x, dz = en.e.getZ() - pp.z, r = en.radio + 0.4;
            if (en.cdContacto == 0 && iframes == 0 && dx * dx + dz * dz < r * r && Math.abs(en.e.getY() - pp.y) < (en.grande() ? 3.0 : 1.8)) {
                en.cdContacto = 20;
                danarJugador(en.dmg);
            }
        }

        // 3) los ataques del jugador
        for (Armas.Arma a : armaObj.values()) a.tick(pp);
        for (Iterator<Proyectil> it = proyectiles.iterator(); it.hasNext(); ) if (!it.next().tick(enemigos)) it.remove();

        // 4) retirar a los derrotados (sueltan gemas)
        for (Iterator<Enemigo> it = enemigos.iterator(); it.hasNext(); ) {
            Enemigo en = it.next();
            if (en.hp > 0) continue;
            muerte(en);
            it.remove();
        }

        // 5) gemas, regeneracion y subir de nivel
        tickGemas(p);
        if (st.regen > 0 && tick % 20 == 0 && hp < st.hpMax) hp = Math.min(st.hpMax, hp + st.regen);
        if (p.getHungerManager().getFoodLevel() < 20 && tick % 100 == 0) p.getHungerManager().setFoodLevel(20);

        // 6) interfaz y medidas
        med.registrar(System.nanoTime() - t0, Partida.server);
        if (tick > RAMPA + 60 && tick % 10 == 0) { sumaEstable += med.msptServidor(); sumaNuestroEstable += med.msNuestro(); nEstable++; }
        if (tick % 4 == 0) enviarEstado(p);
        if (tick % 2 == 0) enviarGemas(p);
        if (tick % 100 == 0) EmiSurvivors.LOG.info("[medida] {} t={}s nivel={} enemigos={} gemas={} derrotados={} hp={} | nuestro={} ms/tick (max {}) | servidor={} ms/tick (media de 100, max {})",
                nombre, segundos(), nivel, enemigos.size(), gemas.size(), derrotados, Math.round(hp), fmt(med.msNuestro()), fmt(med.msNuestroMax()), fmt(med.msptServidor()), fmt(med.msptMax()));
        if (hp <= 0) { Partida.terminar(this, "Te han derrotado."); return; }
        if (pendientes > 0) ofrecerCartas();
    }

    // ------------------------------------------------------------------------------------------------ dano

    /** El jugador recibe dano (sin pasar de la armadura). Con el modo prueba «inmortal» no pierde vida. */
    void danarJugador(float dmg) {
        iframes = 8;
        ServerPlayerEntity p = jugador();
        if (!inmortal) hp -= Math.max(1, Math.round(dmg * (1 - st.armadura)));
        if (p != null) p.networkHandler.sendPacket(new EntityStatusS2CPacket(p, (byte) 2));
    }

    /** Un ataque del jugador golpea a un enemigo: dano base por la mejora de dano, empuje (menor en elites y jefe) y destello rojo. */
    void golpear(Enemigo en, double dmgBase, double kx, double kz) {
        if (en.hp <= 0) return;
        en.hp -= (float) (dmgBase * st.dano);
        double kb = en.tipo == Enemigo.Tipo.JEFE ? 0.05 : en.tipo == Enemigo.Tipo.ELITE ? 0.35 : 1.0;
        en.kbx += kx * kb; en.kbz += kz * kb;
        w.sendEntityStatus(en.e, (byte) 2);
        w.spawnParticles(ParticleTypes.CRIT, en.e.getX(), en.e.getY() + 0.7, en.e.getZ(), 3, 0.2, 0.2, 0.2, 0.1);
    }

    Enemigo masCercano(Vec3d desde, double alcance, Collection<Enemigo> excluir) {
        Enemigo mejor = null; double mejorD = alcance * alcance;
        for (Enemigo en : enemigos) {
            if (en.hp <= 0 || (excluir != null && excluir.contains(en))) continue;
            double dx = en.e.getX() - desde.x, dz = en.e.getZ() - desde.z, d = dx * dx + dz * dz;
            if (d < mejorD) { mejorD = d; mejor = en; }
        }
        return mejor;
    }

    // ------------------------------------------------------------------------------------------------ muertes, gemas y experiencia

    private void muerte(Enemigo en) {
        double x = en.e.getX(), y = en.e.getY(), z = en.e.getZ();
        w.spawnParticles(ParticleTypes.POOF, x, y + 0.5, z, en.grande() ? 25 : 6, 0.3, 0.3, 0.3, 0.02);
        en.quitar();
        derrotados++;
        switch (en.tipo) {
            case JEFE -> {
                jefeDerrotado = true; jefe = null;
                for (int k = 0; k < 6; k++) { double a = k * Math.PI / 3; gemas.add(new Gema(x + Math.cos(a) * 2.5, z + Math.sin(a) * 2.5, 25)); }
                nivel += 2; pendientes += 2;            // el tesoro del jefe: dos subidas de nivel
                hp = Math.min(st.hpMax, hp + st.hpMax * 0.5);
                ServerPlayerEntity p = jugador();
                if (p != null) {
                    Msg.title(p, Text.literal("¡JEFE DERROTADO!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal("Tesoro: 2 niveles y la mitad de tu vida").formatted(Formatting.YELLOW), 5, 50, 15);
                    p.playSoundToPlayer(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 0.8f, 1.0f);
                }
            }
            case ELITE -> { for (int k = 0; k < 3; k++) { double a = k * Math.PI * 2 / 3; gemas.add(new Gema(x + Math.cos(a) * 1.2, z + Math.sin(a) * 1.2, 5)); } }
            default -> gemas.add(new Gema(x, z, en.xp));
        }
    }

    private void tickGemas(ServerPlayerEntity p) {
        double rad2 = st.iman * st.iman;
        for (Iterator<Gema> it = gemas.iterator(); it.hasNext(); ) {
            Gema g = it.next();
            g.edad++;
            double dx = pp.x - g.x, dz = pp.z - g.z, d2 = dx * dx + dz * dz;
            if (!g.atraida && d2 < rad2) g.atraida = true;
            if (!g.atraida) continue;
            g.atraidaT++;
            double d = Math.sqrt(d2), vel = Math.min(0.9, 0.25 + 0.04 * g.atraidaT);
            if (d < 0.9) {
                it.remove();
                ganarXp(g.valor);
                if (tick - sonidoGema >= 2) { sonidoGema = tick; p.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 0.25f, 0.8f + rnd.nextFloat() * 0.6f); }
            } else { g.x += dx / d * Math.min(vel, d); g.z += dz / d * Math.min(vel, d); }
        }
        if (gemas.size() > MAX_GEMAS) fusionarGemas();
    }

    /** Si hay demasiadas gemas, se juntan las mas lejanas en pocas grandes (asi no se pierde experiencia ni se llena el cliente). */
    private void fusionarGemas() {
        gemas.sort((a, b) -> Double.compare(Math.hypot(b.x - pp.x, b.z - pp.z), Math.hypot(a.x - pp.x, a.z - pp.z)));
        int juntar = gemas.size() - MAX_GEMAS + 20, suma = 0;
        double cx = 0, cz = 0;
        for (int k = 0; k < juntar; k++) { Gema g = gemas.remove(0); suma += g.valor; cx += g.x; cz += g.z; }
        gemas.add(new Gema(cx / juntar, cz / juntar, suma));
    }

    void ganarXp(int v) {
        if (sinXp) return;
        xp += Math.max(1, (int) Math.round(v * st.xp));
        while (xp >= xpSig) { xp -= xpSig; nivel++; xpSig = Curvas.xpPara(nivel); pendientes++; }
    }

    // ------------------------------------------------------------------------------------------------ cartas

    private void ofrecerCartas() {
        oferta = Cartas.sortear(rnd, armas, pasivas);
        esperandoCarta = true; cartaCuenta = T_CARTA;
        ServerPlayerEntity p = jugador();
        if (p == null) return;
        p.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.6f, 1.0f);
        JsonObject o = new JsonObject();
        o.addProperty("nivel", nivel - pendientes + 1);
        o.addProperty("seg", T_CARTA / 20);
        JsonArray a = new JsonArray();
        for (Cartas.Carta c : oferta) a.add(cartaJson(c));
        o.add("cartas", a);
        if (ServerPlayNetworking.canSend(p, TextoPayload.ID)) ServerPlayNetworking.send(p, new TextoPayload("cartas", o.toString()));
        enviarEstado(p);
    }

    private JsonObject cartaJson(Cartas.Carta c) {
        JsonObject o = new JsonObject();
        o.addProperty("tipo", c.tipo());
        o.addProperty("nueva", c.nueva());
        o.addProperty("nivel", c.nivel());
        switch (c.tipo()) {
            case "arma" -> {
                Catalogo.Arma d = Catalogo.arma(c.id());
                o.addProperty("titulo", nombreArma(c.id())); o.addProperty("texto", d.textos()[c.nivel() - 1]); o.addProperty("icono", d.icono());
            }
            case "pasiva" -> {
                Catalogo.Pasiva d = Catalogo.pasiva(c.id());
                o.addProperty("titulo", d.nombre()); o.addProperty("texto", d.texto()); o.addProperty("icono", d.icono());
            }
            case "curar" -> { o.addProperty("titulo", "Pastel Michi"); o.addProperty("texto", "Recuperas el 40% de tu vida."); o.addProperty("icono", "minecraft:cake"); }
            default -> { o.addProperty("titulo", "Estrella Fugaz"); o.addProperty("texto", "Ganas experiencia al instante."); o.addProperty("icono", "minecraft:nether_star"); }
        }
        return o;
    }

    /** El jugador (o el tiempo) elige la carta {@code idx}; la partida sigue. */
    void elegirCarta(int idx) {
        if (!esperandoCarta || oferta == null || idx < 0 || idx >= oferta.size()) return;
        Cartas.Carta c = oferta.get(idx);
        switch (c.tipo()) {
            case "arma" -> {
                if (c.nueva()) agregarArma(c.id(), 1);
                else { armas.put(c.id(), c.nivel()); Armas.Arma a = armaObj.get(c.id()); if (a != null) a.nivel = c.nivel(); }
            }
            case "pasiva" -> { pasivas.put(c.id(), c.nivel()); recalcular(); }
            case "curar" -> hp = Math.min(st.hpMax, hp + st.hpMax * 0.4);
            default -> ganarXp(Math.max(1, (int) (xpSig * 0.6)));
        }
        pendientes = Math.max(0, pendientes - 1);
        esperandoCarta = false; oferta = null;
        ServerPlayerEntity p = jugador();
        if (p != null && ServerPlayNetworking.canSend(p, TextoPayload.ID)) ServerPlayNetworking.send(p, new TextoPayload("cartas", ""));
        if (p != null) enviarEstado(p);
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

    /** Un sitio donde puede aparecer un enemigo: en un anillo de 21-26 bloques del jugador, dentro de la arena y con suelo. null si no se encuentra. */
    private Vec3d sitioDeAparicion(double distMin, double distMax) {
        for (int intento = 0; intento < 8; intento++) {
            double ang = rnd.nextDouble() * Math.PI * 2, r = distMin + rnd.nextDouble() * (distMax - distMin);
            double x = pp.x + Math.cos(ang) * r, z = pp.z + Math.sin(ang) * r;
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
        Enemigo en = Enemigo.crear(w, sp, extra, s, tipoE, Curvas.vidaMul(tm), Curvas.danoMul(tm), Curvas.velMul(tm), 0f, rnd);
        if (en == null) { fallosCrear++; return false; }
        enemigos.add(en);
        especiesVistas.add(sp.id());
        return true;
    }

    /** Solo para pruebas (/survivor dev elite): crea un campeon ya. */
    boolean devElite() { return crearEnemigo(Enemigo.Tipo.ELITE); }

    /** El jefe: Snorlax Emi si esta Emipokemon (si no, un Snorlax normal), enorme, a unos 20 bloques del jugador. */
    void crearJefe(ServerPlayerEntity p) {
        jefeSalio = true;
        Especies.E sp = Especies.porId("emipokemon:snorlax_emi");
        String nom = "Snorlax Emi";
        if (sp == null) { sp = Especies.porId("snorlax"); nom = "Snorlax"; }
        if (sp == null) { EmiSurvivors.LOG.warn("No hay especie para el jefe"); return; }
        Vec3d s = sitioDeAparicion(17, 22);
        if (s == null) { jefeSalio = false; return; }                 // se vuelve a intentar el siguiente tick
        double t = tick / 20.0;
        Enemigo en = Enemigo.crear(w, sp, "", s, Enemigo.Tipo.JEFE, 1, Curvas.danoMul(t), 1, Curvas.vidaJefe(t), rnd);
        if (en == null) { fallosCrear++; return; }
        enemigos.add(en);
        jefe = new Jefe(this, en, nom);
        Msg.title(p, Text.literal("¡" + nom.toUpperCase() + "!").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), Text.literal("Esquiva los círculos y las líneas rojas").formatted(Formatting.YELLOW), 5, 50, 15);
    }

    // ------------------------------------------------------------------------------------------------ paquetes al cliente

    private String listaArmas() {
        StringBuilder sb = new StringBuilder();
        for (var e : armas.entrySet()) { if (sb.length() > 0) sb.append(','); sb.append(e.getKey()).append(':').append(e.getValue()).append(':').append(nombreArma(e.getKey())); }
        return sb.toString();
    }

    private String listaPasivas() {
        StringBuilder sb = new StringBuilder();
        for (var e : pasivas.entrySet()) { if (sb.length() > 0) sb.append(','); sb.append(e.getKey()).append(':').append(e.getValue()); }
        return sb.toString();
    }

    private void enviarEstado(ServerPlayerEntity p) {
        if (!ServerPlayNetworking.canSend(p, EstadoPayload.ID)) return;
        boolean hayJefe = jefe != null && jefe.vivo();
        ServerPlayNetworking.send(p, new EstadoPayload(true, (int) Math.max(0, Math.ceil(hp)), (int) Math.round(st.hpMax), segundos(), enemigos.size(), derrotados, nivel, xp, xpSig, esperandoCarta,
                hayJefe ? jefe.nombre : "", hayJefe ? (int) Math.max(0, Math.ceil(jefe.en.hp)) : 0, hayJefe ? (int) Math.round(jefe.en.hpMax) : 0, listaArmas(), listaPasivas(), med.msptServidor(), med.msNuestro()));
    }

    private void enviarGemas(ServerPlayerEntity p) {
        if (!ServerPlayNetworking.canSend(p, GemasPayload.ID)) return;
        if (gemas.isEmpty()) {
            if (gemasEnviadas) { ServerPlayNetworking.send(p, GemasPayload.vacio()); gemasEnviadas = false; }
            return;
        }
        int n = 0;
        for (Gema g : gemas) if (Math.abs(g.x - pp.x) < 60 && Math.abs(g.z - pp.z) < 60) n++;
        short[] dx = new short[n], dz = new short[n]; byte[] tp = new byte[n];
        int i = 0;
        for (Gema g : gemas) {
            if (Math.abs(g.x - pp.x) >= 60 || Math.abs(g.z - pp.z) >= 60) continue;
            dx[i] = (short) Math.round((g.x - pp.x) * 16); dz[i] = (short) Math.round((g.z - pp.z) * 16); tp[i] = (byte) g.tipo(); i++;
        }
        ServerPlayNetworking.send(p, new GemasPayload(pp.x, pp.z, dx, dz, tp));
        gemasEnviadas = true;
    }

    // ------------------------------------------------------------------------------------------------ fin

    /** Quita todo lo de la partida del mundo (enemigos y rocas). */
    void limpiar() {
        for (Enemigo en : enemigos) en.quitar();
        enemigos.clear();
        proyectiles.clear();
        gemas.clear();
    }

    boolean hayMedidaEstable() { return nEstable > 0; }

    float msptEstable() { return nEstable == 0 ? 0f : (float) (sumaEstable / nEstable); }
    float nuestroEstable() { return nEstable == 0 ? 0f : (float) (sumaNuestroEstable / nEstable); }

    static String fmt(float v) { return String.format(java.util.Locale.ROOT, "%.2f", v); }
}
