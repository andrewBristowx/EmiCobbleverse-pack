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
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Un JUGADOR dentro de una partida: su Pokemon, su vida, sus ataques (que se usan solos), su experiencia y sus cartas. El jugador, transformado en su Pokemon, se
 * mueve por la arena mientras los enemigos (Pokemon de Cobblemon sin IA, movidos por codigo por el {@link Equipo}) le persiguen; los enemigos caidos sueltan gemas;
 * al subir de nivel elige 1 de 3 cartas (ataque nuevo, mejora de un ataque o mejora pasiva). Lo comun a todos los jugadores de la partida (la arena, los enemigos,
 * las gemas, el jefe y el reloj) esta en {@link #eq}. Con un solo jugador es la partida en solitario.
 */
final class Run {
    /** Estados del jugador: en pie o caido (sin vida: un companero puede revivirlo). */
    static final int VIVO = 0, CAIDO = 1;
    static final int T_CARTA = Integer.getInteger("emi.survivors.carta_ticks", 20 * 30);         // 30 s para elegir carta; si no, se elige una al azar (la propiedad solo se usa en pruebas)

    final Equipo eq;
    final ServerWorld w;
    final UUID id;
    final String nombre;
    final Pokemon pk;
    final String pokeId, especieId;
    final String tipo;
    final Catalogo.Inicial inicial;
    final boolean inmortal;
    final String morphAntes;
    final Random rnd;
    final Map<String, Integer> meta;             // mejoras permanentes de este Pokemon
    Stats st = new Stats();
    final Map<String, Integer> armas = new LinkedHashMap<>(), pasivas = new LinkedHashMap<>();
    final Map<String, Armas.Arma> armaObj = new LinkedHashMap<>();
    final List<Enemigo> enemigos;                // los enemigos de la partida (la misma lista que la del equipo)
    final List<Proyectil> proyectiles = new ArrayList<>();

    int tick;                                    // el reloj del equipo (lo copia el equipo en cada tick; lo usan los ataques)
    int iframes, derrotados, nivel = 1, xp, xpSig = Curvas.xpPara(1), pendientes;
    double hp;
    int estado = VIVO, reviveT;
    boolean esperandoCarta;
    int focusCd, bayaCd;                         // ticks hasta que se recargan la Banda Focus y la Baya Zidra (0 = listas)
    double danoHecho;                            // dano total de los ataques (solo para medir el equilibrio: /survivor dev dps)
    int dpsTick0 = -1, dpsDerrotados0;
    boolean sinXp;                               // solo para pruebas (/survivor dev sinxp): no se gana experiencia
    int cartaCuenta;
    List<Cartas.Carta> oferta;
    Vec3d pp = Vec3d.ZERO;
    private boolean gemasEnviadas;
    private int sonidoGema;

    Run(Equipo eq, ServerPlayerEntity p, Pokemon pk, String morphAntes, boolean inmortal, Map<String, Integer> meta) {
        this.eq = eq; this.w = eq.w; this.rnd = eq.rnd; this.enemigos = eq.enemigos;
        this.id = p.getUuid(); this.nombre = p.getGameProfile().getName(); this.pk = pk;
        this.morphAntes = morphAntes; this.inmortal = inmortal; this.meta = new HashMap<>(meta);
        this.pokeId = pk.getUuid().toString();
        this.especieId = pk.getSpecies().getResourceIdentifier().getPath();
        this.tipo = pk.getPrimaryType().getName().toLowerCase(java.util.Locale.ROOT);
        this.inicial = Catalogo.inicial(tipo);
        this.pp = p.getPos();
        recalcular();
        hp = st.hpMax;
        agregarArma(inicial.arma(), 1);
    }

    ServerPlayerEntity jugador() { return Partida.server == null ? null : Partida.server.getPlayerManager().getPlayer(id); }

    int segundos() { return eq.segundos(); }

    Vec3d mira() {
        ServerPlayerEntity p = jugador();
        return p == null ? new Vec3d(0, 0, 1) : Vec3d.fromPolar(0f, p.getYaw());
    }

    /** Las particulas de un ataque: el ataque con el que empieza el Pokemon usa las de su tipo; los demas, las suyas. */
    ParticleEffect particula(String armaId, ParticleEffect nativa) { return armaId.equals(inicial.arma()) ? Arena.particula(tipo) : nativa; }

    /** El nombre del ataque en su nivel: el del movimiento del tipo si es con el que empezo el Pokemon, o el de su evolucion si ya evoluciono. */
    String nombreArma(String id, int nivelArma) {
        Catalogo.Arma a = Catalogo.arma(id);
        if (nivelArma >= Catalogo.NIVEL_EVO && a.evo() != null) return a.evo().nombre();
        return id.equals(inicial.arma()) ? inicial.nombre() : a.nombre();
    }

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
        if (p != null && estado == VIVO) Partida.fijarVelocidad(p, n.vel);
    }

    void agregarArma(String idArma, int nivelArma) {
        armas.put(idArma, nivelArma);
        armaObj.put(idArma, Armas.crear(idArma, this, nivelArma));
    }

    // ------------------------------------------------------------------------------------------------ lo que hace cada tick (lo llama el equipo)

    /** Los ataques del jugador y sus disparos. */
    void tickAtaques() {
        for (Armas.Arma a : armaObj.values()) a.tick(pp);
        for (Iterator<Proyectil> it = proyectiles.iterator(); it.hasNext(); ) if (!it.next().tick(enemigos)) it.remove();
    }

    /** Recoger gemas, regenerar vida y mantener la comida llena. */
    void tickGemasYRegen() {
        ServerPlayerEntity p = jugador();
        double rad2 = st.iman * st.iman;
        for (Iterator<Gema> it = eq.gemas.iterator(); it.hasNext(); ) {
            Gema g = it.next();
            if (g.dueno != null && g.dueno != this) {
                Run o = g.dueno;
                if (o.estado == VIVO && !o.esperandoCarta && eq.miembros.contains(o)) continue;       // ya va hacia otro jugador
                g.dueno = null; g.atraidaT = 0;                                                         // su dueño ya no puede cogerla: queda libre
            }
            double dx = pp.x - g.x, dz = pp.z - g.z, d2 = dx * dx + dz * dz;
            if (g.dueno == null && d2 < rad2) g.dueno = this;
            if (g.dueno != this) continue;
            g.atraidaT++;
            double d = Math.sqrt(d2), vel = Math.min(0.9, 0.25 + 0.04 * g.atraidaT);
            if (d < 0.9) {
                it.remove();
                ganarXp(g.valor);
                if (p != null && tick - sonidoGema >= 2) { sonidoGema = tick; p.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 0.25f, 0.8f + rnd.nextFloat() * 0.6f); }
            } else { g.x += dx / d * Math.min(vel, d); g.z += dz / d * Math.min(vel, d); }
        }
        if (focusCd > 0) focusCd--;
        if (bayaCd > 0) bayaCd--;
        if (st.regen > 0 && tick % 20 == 0 && hp < st.hpMax) hp = Math.min(st.hpMax, hp + st.regen);
        if (p != null && p.getHungerManager().getFoodLevel() < 20 && tick % 100 == 0) p.getHungerManager().setFoodLevel(20);
    }

    /** Suelta las gemas que este jugador habia empezado a atraer (para que otro pueda cogerlas si el no puede). */
    private void soltarGemas() {
        for (Gema g : eq.gemas) if (g.dueno == this) { g.dueno = null; g.atraidaT = 0; }
    }

    // ------------------------------------------------------------------------------------------------ dano, caer y revivir

    /** El jugador recibe dano de algo que no es un enemigo normal (el jefe, por ejemplo). */
    void danarJugador(float dmg) { danarJugador(dmg, null); }

    /**
     * El jugador recibe dano (sin pasar de la armadura). Con el modo prueba «inmortal» no pierde vida. {@code causa} es el enemigo que le toca (null si no es un contacto):
     * con el Casco Dentado ese enemigo sufre dano y sale despedido. La Banda Focus lo deja con 1 de vida si el golpe iba a ser mortal, y la Baya Zidra lo cura al bajar del 40 %.
     */
    void danarJugador(float dmg, Enemigo causa) {
        iframes = 8;
        ServerPlayerEntity p = jugador();
        if (!inmortal) {
            hp -= Math.max(1, Math.round(dmg * (1 - st.armadura)));
            if (hp <= 0 && st.focus > 0 && focusCd <= 0) {
                hp = 1; iframes = 30; focusCd = Catalogo.focusSegundos(st.focus) * 20;
                if (Boolean.getBoolean("emi.survivors.debug")) EmiSurvivors.LOG.info("[objeto] {}: la Banda Focus lo deja con 1 de vida (se recarga en {} s)", nombre, Catalogo.focusSegundos(st.focus));
                w.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, pp.x, pp.y + 1.0, pp.z, 20, 0.4, 0.6, 0.4, 0.2);
                if (p != null) { p.playSoundToPlayer(SoundEvents.ITEM_TOTEM_USE, SoundCategory.MASTER, 0.4f, 1.6f); Msg.actionBar(p, Text.literal("¡Banda Focus! Aguantas con 1 de vida").formatted(Formatting.GOLD, Formatting.BOLD)); }
            } else if (hp > 0 && st.baya > 0 && bayaCd <= 0 && hp < st.hpMax * 0.4) {
                curar(st.hpMax * 0.25); bayaCd = Catalogo.bayaSegundos(st.baya) * 20;
                if (Boolean.getBoolean("emi.survivors.debug")) EmiSurvivors.LOG.info("[objeto] {}: la Baya Zidra lo cura (vida {}; se recarga en {} s)", nombre, Math.round(hp), Catalogo.bayaSegundos(st.baya));
                w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pp.x, pp.y + 1.0, pp.z, 14, 0.4, 0.5, 0.4, 0.0);
                if (p != null) { p.playSoundToPlayer(SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.MASTER, 0.6f, 1.2f); Msg.actionBar(p, Text.literal("¡Baya Zidra! +25% de vida").formatted(Formatting.GREEN, Formatting.BOLD)); }
            }
        }
        if (causa != null && st.casco > 0 && causa.hp > 0) {
            causa.hp -= (float) st.casco;
            causa.ultimo = this;
            double dx = causa.e.getX() - pp.x, dz = causa.e.getZ() - pp.z, d = Math.max(0.01, Math.sqrt(dx * dx + dz * dz)), kb = causa.tipo == Enemigo.Tipo.JEFE ? 0.05 : causa.tipo == Enemigo.Tipo.ELITE ? 0.35 : 1.0;
            causa.kbx += dx / d * 0.9 * kb; causa.kbz += dz / d * 0.9 * kb;
            w.spawnParticles(ParticleTypes.ENCHANTED_HIT, causa.e.getX(), causa.e.getY() + 0.7, causa.e.getZ(), 6, 0.2, 0.2, 0.2, 0.1);
        }
        if (p != null) p.networkHandler.sendPacket(new EntityStatusS2CPacket(p, (byte) 2));
    }

    /** Empuja a un enemigo en la direccion (dx, dz) con esa fuerza (menos a campeones y jefe): para atraer (hacia un punto) o apartar. */
    void impulso(Enemigo en, double dx, double dz, double fuerza) {
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d < 1e-6) return;
        double kb = en.tipo == Enemigo.Tipo.JEFE ? 0.05 : en.tipo == Enemigo.Tipo.ELITE ? 0.35 : 1.0;
        en.kbx += dx / d * fuerza * kb; en.kbz += dz / d * fuerza * kb;
    }

    /** Este jugador acaba de pisar un objeto del suelo: lo que haga (ver {@link Objeto}). */
    void recoger(int tipo) {
        ServerPlayerEntity p = jugador();
        switch (tipo) {
            case Objeto.BAYA -> {
                curar(st.hpMax * 0.30);
                for (Run m : eq.miembros) if (m != this && m.estado == VIVO && Math.hypot(m.pp.x - pp.x, m.pp.z - pp.z) < 6) m.curar(m.st.hpMax * 0.15);
                w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pp.x, pp.y + 1.2, pp.z, 16, 0.5, 0.6, 0.5, 0.0);
                if (p != null) p.playSoundToPlayer(SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.MASTER, 0.7f, 1.2f);
            }
            case Objeto.IMAN -> {
                for (Gema g : eq.gemas) { g.dueno = this; g.atraidaT = Math.max(g.atraidaT, 26); }
                for (int k = 0; k < 36; k++) { double a = k * Math.PI / 18; w.spawnParticles(ParticleTypes.ENCHANT, pp.x + Math.cos(a) * 3, pp.y + 1.0, pp.z + Math.sin(a) * 3, 1, 0.1, 0.3, 0.1, 0.5); }
                if (p != null) p.playSoundToPlayer(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.MASTER, 1.0f, 0.8f);
            }
            case Objeto.BOMBA -> {
                w.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, pp.x, pp.y + 0.5, pp.z, 1, 0, 0, 0, 0);
                for (Enemigo en : enemigos) {
                    if (en.hp <= 0) continue;
                    double dx = en.e.getX() - pp.x, dz = en.e.getZ() - pp.z, d = Math.sqrt(dx * dx + dz * dz);
                    if (d > 22) continue;
                    en.hp -= en.tipo == Enemigo.Tipo.JEFE ? 40f : 150f;
                    en.ultimo = this;
                    impulso(en, dx, dz, 0.9);
                    w.sendEntityStatus(en.e, (byte) 2);
                    w.spawnParticles(ParticleTypes.EXPLOSION, en.e.getX(), en.e.getY() + 0.6, en.e.getZ(), 1, 0, 0, 0, 0);
                }
                if (p != null) p.playSoundToPlayer(SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.MASTER, 0.8f, 0.9f);
            }
            case Objeto.CARAMELO -> {
                nivel++; pendientes++; xpSig = Curvas.xpPara(nivel); xp = Math.min(xp, xpSig - 1);
                w.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, pp.x, pp.y + 1.2, pp.z, 24, 0.4, 0.6, 0.4, 0.2);
                if (p != null) p.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.7f, 1.3f);
            }
            default -> { return; }
        }
        if (p != null) Msg.actionBar(p, Text.literal(Objeto.texto(tipo)).formatted(Formatting.GOLD, Formatting.BOLD));
    }

    /** Recupera vida (sin pasar del maximo). */
    void curar(double v) { if (estado == VIVO && v > 0) hp = Math.min(st.hpMax, hp + v); }

    /** Este jugador acaba de derrotar a un enemigo (lo llama el equipo): la Campana Concha le cura. */
    void alDerrotar(Enemigo en) {
        if (st.concha > 0) curar(st.concha);
    }

    /** Cuantas particulas «de adorno» se dibujan: con muchos ataques a la vez la pantalla se llena, asi que se reducen (1 = todas; con 6 ataques, la mitad). */
    double densidad() { return armas.size() >= 5 ? 0.5 : armas.size() >= 3 ? 0.75 : 1.0; }

    /** Un ataque del jugador golpea a un enemigo: dano base por la mejora de dano, empuje (menor en elites y jefe) y destello rojo. */
    void golpear(Enemigo en, double dmgBase, double kx, double kz) { golpear(en, dmgBase, kx, kz, false); }

    /** Igual; con {@code critico} el golpe es critico seguro. Con el Periscopio hay probabilidad de critico (el doble de dano) y con la Cinta Experto mas dano a campeones y jefes. */
    void golpear(Enemigo en, double dmgBase, double kx, double kz, boolean critico) {
        if (en.hp <= 0) return;
        double d = dmgBase * st.dano;
        if (st.experto > 0 && en.grande()) d *= 1 + st.experto;
        boolean crit = critico || (st.critico > 0 && rnd.nextDouble() < st.critico);
        if (crit) d *= 2;
        en.hp -= (float) d;
        danoHecho += d;
        en.ultimo = this;
        double kb = en.tipo == Enemigo.Tipo.JEFE ? 0.05 : en.tipo == Enemigo.Tipo.ELITE ? 0.35 : 1.0;
        en.kbx += kx * kb; en.kbz += kz * kb;
        w.sendEntityStatus(en.e, (byte) 2);
        w.spawnParticles(crit ? ParticleTypes.ENCHANTED_HIT : ParticleTypes.CRIT, en.e.getX(), en.e.getY() + 0.7, en.e.getZ(), crit ? 8 : 3, 0.2, 0.2, 0.2, 0.1);
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

    /** Se queda sin vida: cae al suelo (inmovil, sin atacar y sin que los enemigos le hagan caso) hasta que un companero lo reviva. */
    void caer() {
        estado = CAIDO; hp = 0; reviveT = 0; iframes = 0;
        esperandoCarta = false; oferta = null;
        soltarGemas();
        ServerPlayerEntity p = jugador();
        if (p != null) {
            Partida.fijarVelocidad(p, 0);
            p.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_DEATH, SoundCategory.MASTER, 0.6f, 1.0f);
            Msg.title(p, Text.literal("¡HAS CAÍDO!").formatted(Formatting.RED, Formatting.BOLD), Text.literal("Un compañero puede revivirte si se queda a tu lado").formatted(Formatting.GRAY), 5, 60, 15);
            if (ServerPlayNetworking.canSend(p, TextoPayload.ID)) ServerPlayNetworking.send(p, new TextoPayload("cartas", ""));
        }
        eq.avisar(Msg.say(nombre + " ha caído. ¡Quedaos a su lado para revivirlo!"));
    }

    /** Un companero lo ha revivido (o ha caido el jefe): vuelve a la partida con la mitad de la vida y un momento sin que le toquen. */
    void revivir() {
        estado = VIVO; reviveT = 0;
        hp = Math.max(1, st.hpMax * 0.5);
        iframes = 60;
        ServerPlayerEntity p = jugador();
        if (p != null) {
            Partida.fijarVelocidad(p, st.vel);
            p.playSoundToPlayer(SoundEvents.ITEM_TOTEM_USE, SoundCategory.MASTER, 0.5f, 1.2f);
            Msg.title(p, Text.literal("¡REVIVIDO!").formatted(Formatting.GREEN, Formatting.BOLD), null, 3, 25, 10);
        }
        w.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, pp.x, pp.y + 1.0, pp.z, 25, 0.4, 0.6, 0.4, 0.2);
        eq.avisar(Msg.say(nombre + " ha vuelto a la partida."));
    }

    /** El premio del jefe: dos niveles y la mitad de la vida (y si estaba caido, se levanta). */
    void premioJefe() {
        nivel += 2; pendientes += 2;            // el tesoro del jefe: dos subidas de nivel
        ServerPlayerEntity p = jugador();
        if (estado == CAIDO) revivir();
        else hp = Math.min(st.hpMax, hp + st.hpMax * 0.5);
        if (p != null) {
            Msg.title(p, Text.literal("¡JEFE DERROTADO!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal("Tesoro: 2 niveles y la mitad de tu vida").formatted(Formatting.YELLOW), 5, 50, 15);
            p.playSoundToPlayer(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 0.8f, 1.0f);
        }
    }

    // ------------------------------------------------------------------------------------------------ experiencia

    void ganarXp(int v) {
        if (sinXp) return;
        xp += Math.max(1, (int) Math.round(v * st.xp));
        while (xp >= xpSig) { xp -= xpSig; nivel++; xpSig = Curvas.xpPara(nivel); pendientes++; }
    }

    // ------------------------------------------------------------------------------------------------ cartas

    void ofrecerCartas() {
        oferta = Cartas.sortear(rnd, armas, pasivas);
        esperandoCarta = true; cartaCuenta = T_CARTA;
        soltarGemas();
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
        enviarEstado();
    }

    private JsonObject cartaJson(Cartas.Carta c) {
        JsonObject o = new JsonObject();
        o.addProperty("tipo", c.tipo());
        o.addProperty("nueva", c.nueva());
        o.addProperty("nivel", c.nivel());
        o.addProperty("evo", c.evo());
        switch (c.tipo()) {
            case "arma" -> {
                Catalogo.Arma d = Catalogo.arma(c.id());
                if (c.evo()) {
                    o.addProperty("titulo", d.evo().nombre());
                    o.addProperty("texto", d.evo().texto() + " Sustituye a «" + nombreArma(c.id(), Catalogo.MAX_NIVEL) + "».");
                    o.addProperty("icono", d.evo().icono());
                } else {
                    String texto = d.textos()[c.nivel() - 1];
                    if (c.nivel() == Catalogo.MAX_NIVEL && d.evo() != null) texto += " Evoluciona con: " + Catalogo.pasiva(d.evo().requiere()).nombre() + ".";
                    o.addProperty("titulo", nombreArma(c.id(), c.nivel())); o.addProperty("texto", texto); o.addProperty("icono", d.icono());
                }
            }
            case "pasiva" -> {
                Catalogo.Pasiva d = Catalogo.pasiva(c.id());
                String texto = d.texto();
                List<Catalogo.Arma> evolucionan = Catalogo.evolucionanCon(c.id(), armas);
                if (!evolucionan.isEmpty()) {
                    StringBuilder sb = new StringBuilder(texto).append(" Con él evoluciona: ");
                    for (int k = 0; k < evolucionan.size(); k++) sb.append(k > 0 ? ", " : "").append(nombreArma(evolucionan.get(k).id(), armas.getOrDefault(evolucionan.get(k).id(), 1)));
                    texto = sb.append(".").toString();
                }
                o.addProperty("titulo", d.nombre()); o.addProperty("texto", texto); o.addProperty("icono", d.icono());
            }
            case "curar" -> { o.addProperty("titulo", "Pastel Michi"); o.addProperty("texto", "Recuperas el 40% de tu vida."); o.addProperty("icono", "minecraft:cake"); }
            default -> { o.addProperty("titulo", "Estrella Fugaz"); o.addProperty("texto", "Ganas experiencia al instante."); o.addProperty("icono", "minecraft:nether_star"); }
        }
        return o;
    }

    /** El jugador (o el tiempo) elige la carta {@code idx}; la partida sigue (con un segundo sin que le toquen, por si la horda estaba encima). */
    void elegirCarta(int idx) {
        if (!esperandoCarta || oferta == null || idx < 0 || idx >= oferta.size()) return;
        Cartas.Carta c = oferta.get(idx);
        switch (c.tipo()) {
            case "arma" -> {
                if (c.nueva()) agregarArma(c.id(), 1);
                else {
                    String antes = nombreArma(c.id(), armas.getOrDefault(c.id(), 1));
                    armas.put(c.id(), c.nivel()); Armas.Arma a = armaObj.get(c.id()); if (a != null) a.nivel = c.nivel();
                    if (c.evo()) alEvolucionar(antes, c.id());
                }
            }
            case "pasiva" -> { pasivas.put(c.id(), c.nivel()); recalcular(); }
            case "curar" -> hp = Math.min(st.hpMax, hp + st.hpMax * 0.4);
            default -> ganarXp(Math.max(1, (int) (xpSig * 0.6)));
        }
        pendientes = Math.max(0, pendientes - 1);
        esperandoCarta = false; oferta = null;
        iframes = Math.max(iframes, 20);
        ServerPlayerEntity p = jugador();
        if (p != null && ServerPlayNetworking.canSend(p, TextoPayload.ID)) ServerPlayNetworking.send(p, new TextoPayload("cartas", ""));
        if (p != null) enviarEstado();
    }

    /** Un ataque acaba de evolucionar: titulo, sonido, particulas y aviso a los companeros. */
    private void alEvolucionar(String antes, String idArma) {
        String despues = nombreArma(idArma, Catalogo.NIVEL_EVO);
        ServerPlayerEntity p = jugador();
        if (p != null) {
            Msg.title(p, Text.literal("¡EVOLUCIÓN!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal(antes + " → " + despues).formatted(Formatting.YELLOW), 5, 50, 15);
            p.playSoundToPlayer(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 0.8f, 1.2f);
        }
        w.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, pp.x, pp.y + 1.0, pp.z, 40, 0.5, 0.8, 0.5, 0.25);
        if (eq.miembros.size() > 1) eq.avisar(Msg.say(nombre + " ha evolucionado «" + antes + "» en «" + despues + "»."));
    }

    // ------------------------------------------------------------------------------------------------ paquetes al cliente

    private String listaArmas() {
        StringBuilder sb = new StringBuilder();
        for (var e : armas.entrySet()) { if (sb.length() > 0) sb.append(','); sb.append(e.getKey()).append(':').append(e.getValue()).append(':').append(nombreArma(e.getKey(), e.getValue())); }
        return sb.toString();
    }

    private String listaPasivas() {
        StringBuilder sb = new StringBuilder();
        for (var e : pasivas.entrySet()) { if (sb.length() > 0) sb.append(','); sb.append(e.getKey()).append(':').append(e.getValue()); }
        return sb.toString();
    }

    void enviarEstado() {
        ServerPlayerEntity p = jugador();
        if (p == null || !ServerPlayNetworking.canSend(p, EstadoPayload.ID)) return;
        Jefe j = eq.jefe;
        boolean hayJefe = j != null && j.vivo();
        ServerPlayNetworking.send(p, new EstadoPayload(true, (int) Math.max(0, Math.ceil(hp)), (int) Math.round(st.hpMax), segundos(), enemigos.size(), eq.derrotados, nivel, xp, xpSig, esperandoCarta,
                hayJefe ? j.nombre : "", hayJefe ? (int) Math.max(0, Math.ceil(j.en.hp)) : 0, hayJefe ? (int) Math.round(j.en.hpMax) : 0, listaArmas(), listaPasivas(), eq.med.msptServidor(), eq.med.msNuestro(),
                estado == CAIDO, estado == CAIDO ? Math.min(100, reviveT * 100 / Equipo.REVIVIR_TICKS) : 0, eq.companerosDe(this)));
    }

    void enviarGemas() {
        ServerPlayerEntity p = jugador();
        if (p == null || !ServerPlayNetworking.canSend(p, GemasPayload.ID)) return;
        List<Gema> gemas = eq.gemas;
        List<Objeto> objetos = eq.objetos;
        if (gemas.isEmpty() && objetos.isEmpty()) {
            if (gemasEnviadas) { ServerPlayNetworking.send(p, GemasPayload.vacio()); gemasEnviadas = false; }
            return;
        }
        int n = 0;
        for (Gema g : gemas) if (Math.abs(g.x - pp.x) < 60 && Math.abs(g.z - pp.z) < 60) n++;
        for (Objeto o : objetos) if (Math.abs(o.x - pp.x) < 60 && Math.abs(o.z - pp.z) < 60) n++;
        short[] dx = new short[n], dz = new short[n]; byte[] tp = new byte[n];
        int i = 0;
        for (Gema g : gemas) {
            if (Math.abs(g.x - pp.x) >= 60 || Math.abs(g.z - pp.z) >= 60) continue;
            dx[i] = (short) Math.round((g.x - pp.x) * 16); dz[i] = (short) Math.round((g.z - pp.z) * 16); tp[i] = (byte) g.tipo(); i++;
        }
        for (Objeto o : objetos) {
            if (Math.abs(o.x - pp.x) >= 60 || Math.abs(o.z - pp.z) >= 60) continue;
            dx[i] = (short) Math.round((o.x - pp.x) * 16); dz[i] = (short) Math.round((o.z - pp.z) * 16); tp[i] = (byte) o.tipo; i++;
        }
        ServerPlayNetworking.send(p, new GemasPayload(pp.x, pp.z, dx, dz, tp));
        gemasEnviadas = true;
    }

    static String fmt(float v) { return String.format(java.util.Locale.ROOT, "%.2f", v); }
}
