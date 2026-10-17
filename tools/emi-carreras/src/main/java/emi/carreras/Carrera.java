package emi.carreras;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.ItemStack;
import emi.carreras.net.EstadoPayload;
import emi.carreras.net.PoderPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.WorldSavePath;
import net.minecraft.block.Block;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Estado y reglas de la carrera. Todo ocurre en la dimension emi_carreras:pista. */
public final class Carrera {
    public enum Fase { LIBRE, COLA, PRESENTACION, CUENTA, CARRERA, FIN }

    public static final RegistryKey<World> MUNDO = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("emi_carreras", "pista"));
    static final String TAG = "emi_carrera";

    static MinecraftServer server;
    static Cfg cfg = new Cfg();
    static Pista pista;
    static Fase fase = Fase.LIBRE;
    static final LinkedHashMap<UUID, Corredor> corr = new LinkedHashMap<>();
    static final List<UUID> llegada = new ArrayList<>();
    static final Random rnd = new Random();
    static int tickCuenta, tickInicio, tickFin, limite = -1, tickAviso, vueltasTotal, graciaHasta;
    static int[] podioTick;
    static int presIni, presDur;
    static List<Corredor> presOrden = new ArrayList<>();
    static List<com.cobblemon.mod.common.entity.pokemon.PokemonEntity> presVisuales = new ArrayList<>();

    static final List<Caja> cajas = new ArrayList<>();
    static final List<Bola> bolas = new ArrayList<>();
    static final List<Trampa> trampas = new ArrayList<>();

    static final class Caja { Vec3d pos; int libreEn; Entity ent; UUID uuid; boolean creada; int sinEntidad; boolean estrella; }
    static final class Bola { SnowballEntity ent; Corredor dueno; int nace; Vec3d ultima; double fuerza = 1; }
    static final class Trampa { PokemonEntity ent; Corredor dueno; int nace; String tipo = "voltorb"; double fuerza = 1; }
    /** Voltorb fijo de la pista: explota con quien lo toque y reaparece solo. */
    static final class Fijo { Vec3d pos; PokemonEntity ent; int libreEn; }
    static final List<Fijo> fijos = new ArrayList<>();
    static final Corredor PELIGRO = new Corredor(new UUID(0, 0), "un Voltorb");
    static String musicaActual; static int musicaProx;
    static Datos datos;                                   // estrellas, estadisticas y tiempos de los jugadores (se carga al arrancar)
    static boolean entrenamiento;                         // la carrera en marcha es un entrenamiento en solitario
    static int jugadoresAlEmpezar;
    static UUID anfitrion;                                // quien abrio la carrera desde la sala (el que la empieza); null si la abrio un organizador con /carrera abrir
    static int listosDesde = -1;                          // tick desde el que estan todos listos (cuenta atras para empezar solos)
    static int cambios;                                   // sube con cada cambio de la cola: asi el menu sabe cuando refrescarse
    static final int ESPERA_LISTOS = 160;                 // 8 s desde que estan todos listos hasta que sale la carrera
    static final Map<UUID, String> resultados = new HashMap<>();       // lo que se ensena a cada jugador en el menu al volver a la sala
    static final Map<String, Integer> DURACION_MUSICA = Map.of("circuito", 1024, "volcan", 800, "emi", 1200);
    static final Map<String, Integer> NOMBRES = new HashMap<>();

    // ------------------------------------------------------------------ arranque

    public static void init(MinecraftServer s) {
        server = s;
        cfg = Cfg.load();
        datos = new Datos(s.getSavePath(WorldSavePath.ROOT).resolve("emi_carreras"));
        Montura.alturaBase = cfg.alturaPokemon;
        pista = Pista.cargar(cfg.pista);
        if (pista == null) EmiCarreras.LOG.error("No hay pista valida ({}): el minijuego no se podra usar", cfg.pista);
        else EmiCarreras.LOG.info("Pista '{}' lista: {} puntos de control, {} cajas, {} huecos de salida", pista.id, pista.n(),
                pista.cajas == null ? 0 : pista.cajas.length, pista.parrilla.length);
        fase = Fase.LIBRE;
    }

    public static void shutdown() { try { Pausas.reanudar(); } catch (Throwable t) { } limpiarEntidades(); }

    static ServerWorld mundo() { return server.getWorld(MUNDO); }

    // ------------------------------------------------------------------ interfaz del cliente (posiciones, mapa, poder)

    /** Envia el estado de la carrera a los jugadores que tengan el mod en el cliente (los demas siguen viendo solo el texto de la barra). */
    static void enviarEstado(int now, boolean activo) {
        if (server == null || pista == null) return;
        List<EstadoPayload.Jugador> lista = new ArrayList<>();
        if (activo) {
            Map<Corredor, Integer> pu = puestos();
            for (Corredor c : corr.values()) {
                ServerPlayerEntity p = server.getPlayerManager().getPlayer(c.id);
                if (p == null || c.abandono) continue;
                lista.add(new EstadoPayload.Jugador(c.id, c.nombre, (float) p.getX(), (float) p.getZ(), c.vuelta, pu.getOrDefault(c, corr.size()), c.finTick >= 0, Math.max(0, (c.escudoHasta - now + 19) / 20), c.estrellas));
            }
        }
        var m = pista.mapa;
        EstadoPayload pl = new EstadoPayload(activo, pista.id, pista.nombre, m == null ? 0 : (float) m.x0, m == null ? 0 : (float) m.z0, m == null ? 0 : (float) m.ancho, m == null ? 0 : (float) m.largo,
                m == null ? 0 : m.px, m == null ? 0 : m.py, vueltasTotal, fase == Fase.CARRERA ? now - tickInicio : 0, fase == Fase.PRESENTACION ? 0 : fase == Fase.CUENTA ? 1 : fase == Fase.CARRERA ? 2 : 3, cfg.estrellas && pista.estrellas != null ? cfg.estrellasMejora : 0, lista);
        for (ServerPlayerEntity p : jugadores()) if (ServerPlayNetworking.canSend(p, EstadoPayload.ID)) ServerPlayNetworking.send(p, pl);
    }

    /** Avisa al cliente del poder que lleva el corredor (con la estrella delante si esta mejorado). */
    static void enviarPoder(ServerPlayerEntity p, Corredor c, boolean nuevo) {
        if (p == null || !ServerPlayNetworking.canSend(p, PoderPayload.ID)) return;
        Poder pw = c.poder;
        if (pw == null) { ServerPlayNetworking.send(p, PoderPayload.ninguno()); return; }
        Integer col = pw.color.getColorValue();
        ServerPlayNetworking.send(p, new PoderPayload(pw.name(), (c.mejorado ? "★ " : "") + pw.nombre, (c.mejorado ? "¡Mejorado! " : "") + pw.descripcion, pw.itemId, col == null ? 0xFFFFFF : col, nuevo));
    }

    /** Avisa al cliente del poder que lleva (null = ninguno). */
    static void enviarPoder(ServerPlayerEntity p, Poder pw, boolean nuevo) {
        if (p == null || !ServerPlayNetworking.canSend(p, PoderPayload.ID)) return;
        if (pw == null) { ServerPlayNetworking.send(p, PoderPayload.ninguno()); return; }
        Integer col = pw.color.getColorValue();
        ServerPlayNetworking.send(p, new PoderPayload(pw.name(), pw.nombre, pw.descripcion, pw.itemId, col == null ? 0xFFFFFF : col, nuevo));
    }

    // ------------------------------------------------------------------ sonido

    /** Efecto de sonido para un jugador (se apaga con "sonidos": false en la configuracion). */
    static void sfx(ServerPlayerEntity p, SoundEvent s, float vol, float pitch) { if (cfg.sonidos && p != null) Msg.sound(p, s, vol, pitch); }

    static SoundEvent evento(String id) { return SoundEvent.of(Identifier.of("emi_carreras", id)); }

    /** Musica de fondo en bucle: se vuelve a lanzar cuando acaba (el cliente no sabe repetir un sonido que viene del servidor). */
    static void musica(int now) {
        if (!cfg.musica || pista == null || pista.musica == null) return;
        if (musicaActual != null && now < musicaProx) return;
        musicaActual = pista.musica;
        musicaProx = now + DURACION_MUSICA.getOrDefault(musicaActual, 800) - 2;
        SoundEvent ev = evento("musica." + musicaActual);
        for (ServerPlayerEntity p : jugadores()) p.playSoundToPlayer(ev, SoundCategory.MUSIC, cfg.volumenMusica, 1f);
    }

    static void pararMusica() {
        if (server == null) return;
        if (musicaActual != null) {
            Identifier id = Identifier.of("emi_carreras", "musica." + musicaActual);
            for (ServerPlayerEntity p : jugadores()) p.networkHandler.sendPacket(new StopSoundS2CPacket(id, SoundCategory.MUSIC));
        }
        musicaActual = null;
    }

    /** Sonidos de ambiente de la pista (volcan: burbujas de lava y retumbos lejanos). */
    static void ambiente(int now) {
        if (!cfg.sonidos || pista == null || !"volcan".equals(pista.ambiente) || now % 50 != 0) return;
        for (ServerPlayerEntity p : jugadores()) {
            double f = rnd.nextDouble();
            if (f < 0.45) sfx(p, SoundEvents.BLOCK_LAVA_POP, 0.5f, 0.8f + rnd.nextFloat() * 0.4f);
            else if (f < 0.75) sfx(p, SoundEvents.BLOCK_LAVA_AMBIENT, 0.6f, 0.9f);
            else if (f < 0.85) sfx(p, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 0.25f, 0.5f);
        }
    }
    public static Fase fase() { return fase; }
    public static boolean participa(net.minecraft.entity.player.PlayerEntity p) { return corr.containsKey(p.getUuid()) && fase != Fase.LIBRE && fase != Fase.COLA; }
    static int ahora() { return server.getTicks(); }
    static int totalVueltas() { return cfg.vueltas > 0 ? cfg.vueltas : pista.vueltas; }

    static List<ServerPlayerEntity> jugadores() {
        List<ServerPlayerEntity> l = new ArrayList<>();
        for (UUID id : corr.keySet()) { ServerPlayerEntity p = server.getPlayerManager().getPlayer(id); if (p != null) l.add(p); }
        return l;
    }

    // ------------------------------------------------------------------ cola

    /** La pista de la configuracion (la de siempre si no esta entre las elegibles). */
    static String pistaPorDefecto() { return cfg != null && ids().contains(cfg.pista) ? cfg.pista : (ids().isEmpty() ? "" : ids().get(0)); }

    /** Ids de las pistas que se pueden elegir. */
    public static List<String> ids() {
        if (cfg == null) return List.of();
        List<String> l = new ArrayList<>(cfg.pistas);
        for (String d : Cfg.PISTAS_DE_SERIE) if (!l.contains(d)) l.add(d);       // las pistas nuevas salen aunque la configuracion ya existiera sin ellas
        return l;
    }

    public static String abrir(String id) {
        if (fase != Fase.LIBRE) return "Ya hay una carrera en marcha o la cola ya esta abierta.";
        if (id != null && !id.isBlank() && (pista == null || !id.equals(pista.id))) {
            Pista np = Pista.cargar(id);
            if (np == null) return "No existe la pista '" + id + "'. Pistas: " + String.join(", ", ids());
            pista = np;
        } else if (id == null || id.isBlank()) {
            if (pista == null || !pista.id.equals(cfg.pista)) { Pista np = Pista.cargar(cfg.pista); if (np != null) pista = np; }
        }
        return abrir();
    }

    public static String abrir() {
        if (pista == null) return "No hay pista cargada (revisa el log del servidor).";
        if (mundo() == null) return "No existe la dimension emi_carreras:pista (¿falta el mod o el datapack?).";
        if (fase != Fase.LIBRE) return "Ya hay una carrera en marcha o la cola ya esta abierta.";
        corr.clear();
        fase = Fase.COLA;
        anfitrion = null; entrenamiento = false; listosDesde = -1; cambios++;
        tickAviso = ahora();
        anunciar();
        return null;
    }

    static void anunciar() {
        if (anfitrion != null) {       // abierta desde la sala: se invita a entrar a la sala (los que ya estan dentro la ven en el menu)
            Corredor h = corr.get(anfitrion);
            Text t = Text.empty().append(Msg.PREFIX)
                    .append(Text.literal((h == null ? "Alguien" : h.nombre) + " ha abierto una carrera en «" + pista.nombre + "». ").formatted(Formatting.YELLOW))
                    .append(Msg.button("ENTRAR A LA SALA", "/carrera entrar", Formatting.GREEN, "Ir a la sala de carreras y apuntarte"));
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) if (!Sala.enSala(p) && !corr.containsKey(p.getUuid())) p.sendMessage(t, false);
            return;
        }
        Text t = Text.empty().append(Msg.PREFIX)
                .append(Text.literal("¡Carrera de Pokémon en «" + pista.nombre + "»! ").formatted(Formatting.YELLOW))
                .append(Msg.button("UNIRSE", "/carrera unirse", Formatting.GREEN, "Apuntarte a la carrera"));
        Msg.broadcast(server, t);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList())
            if (!corr.containsKey(p.getUuid())) Msg.title(p, Text.literal("¡CARRERA!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal("Escribe /carrera unirse").formatted(Formatting.YELLOW), 5, 50, 10);
    }

    public static String cerrarCola() {
        if (fase != Fase.COLA) return "No hay cola abierta.";
        if (anfitrion == null) Msg.broadcast(server, Msg.say("Se ha cerrado la cola de la carrera."));
        else Msg.broadcastTo(jugadoresSala(), Msg.say("Se ha cancelado la carrera."));
        corr.clear();
        anfitrion = null; entrenamiento = false; listosDesde = -1; cambios++;
        fase = Fase.LIBRE;
        return null;
    }

    public static String unirse(ServerPlayerEntity p) {
        if (fase != Fase.COLA) return "No hay ninguna carrera abierta ahora mismo.";
        if (corr.containsKey(p.getUuid())) return "Ya estas apuntado.";
        if (corr.size() >= cfg.maxPlayers) return "La carrera esta llena (" + cfg.maxPlayers + " corredores).";
        if (p.getServerWorld().getRegistryKey() == MUNDO) return "No puedes apuntarte desde la pista.";
        Corredor c = new Corredor(p.getUuid(), p.getName().getString());
        corr.put(p.getUuid(), c);
        var cand = Montura.candidatos(p);
        cand.stream().filter(Montura.Candidato::valido).findFirst().ifPresent(x -> { c.slot = x.slot(); c.pokemon = x.pokemon(); });
        cambios++;
        if (Sala.enSala(p)) {          // desde la sala se ve todo en el menu: nada de listas en el chat
            for (ServerPlayerEntity o : jugadores()) if (o != p) o.sendMessage(Msg.say(c.nombre + " se ha apuntado (" + corr.size() + ")."), true);
            return null;
        }
        p.sendMessage(Msg.say("¡Apuntado! Corredores: " + corr.size() + "/" + cfg.maxPlayers), false);
        mostrarEquipo(p, c, cand);
        for (ServerPlayerEntity o : jugadores()) if (o != p) o.sendMessage(Msg.say(c.nombre + " se ha apuntado (" + corr.size() + ")."), false);
        return null;
    }

    // ------------------------------------------------------------------ cola desde la sala

    static List<ServerPlayerEntity> jugadoresSala() {
        List<ServerPlayerEntity> l = new ArrayList<>();
        if (server != null) for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) if (Sala.enSala(p)) l.add(p);
        return l;
    }

    static boolean esAnfitrion(ServerPlayerEntity p) { return anfitrion != null && anfitrion.equals(p.getUuid()); }

    /** Quita a alguien de la cola; si era el anfitrion pasa a serlo el siguiente, y si no queda nadie la carrera se cierra. */
    static void quitarDeLaCola(UUID id) {
        if (corr.remove(id) == null) return;
        cambios++; listosDesde = -1;
        if (anfitrion == null) return;
        if (corr.isEmpty()) { cerrarCola(); return; }
        if (anfitrion.equals(id)) {
            anfitrion = corr.keySet().iterator().next();
            ServerPlayerEntity n = server.getPlayerManager().getPlayer(anfitrion);
            if (n != null) n.sendMessage(Msg.say("Ahora eres el anfitrión de la carrera: tú la empiezas."), false);
        }
    }

    /** Desde la sala: abre una carrera en esa pista (quien la abre es el anfitrion y se apunta solo) o, si {@code entrenar}, un entrenamiento en solitario que sale ya. */
    public static String crearDesdeSala(ServerPlayerEntity p, String id, boolean entrenar) {
        if (!Sala.enSala(p)) return "Solo se abren desde la sala de carreras (/carrera entrar).";
        if (fase != Fase.LIBRE) return fase == Fase.COLA ? "Ya hay una carrera abierta: únete a ella." : "Hay una carrera en marcha: espera a que acabe.";
        if (id == null || id.isBlank() || !ids().contains(id)) return "Esa pista no existe. Pistas: " + String.join(", ", ids());
        Pista np = pista != null && pista.id.equals(id) ? pista : Pista.cargar(id);
        if (np == null) return "No se ha podido cargar la pista '" + id + "'.";
        if (mundo() == null) return "No existe la dimension emi_carreras:pista (¿falta el mod o el datapack?).";
        pista = np;
        corr.clear();
        fase = Fase.COLA;
        anfitrion = p.getUuid(); entrenamiento = entrenar; listosDesde = -1; cambios++;
        tickAviso = ahora();
        String err = unirse(p);
        if (err == null && entrenar) err = empezar();
        if (err != null) { corr.clear(); anfitrion = null; entrenamiento = false; fase = Fase.LIBRE; cambios++; return err; }
        if (!entrenar) anunciar();
        return null;
    }

    /** Marca (o desmarca) que el jugador esta listo; cuando lo estan todos (y son al menos 2) la carrera sale sola a los 8 s. */
    public static String marcarListo(ServerPlayerEntity p, boolean listo) {
        Corredor c = corr.get(p.getUuid());
        if (c == null || fase != Fase.COLA) return "Solo puedes marcarte listo mientras esperas a que empiece la carrera.";
        c.listo = listo; cambios++; listosDesde = -1;
        return null;
    }

    /** El anfitrion (o un organizador) empieza la carrera. */
    public static String empezarDesdeSala(ServerPlayerEntity p, boolean admin) {
        if (fase != Fase.COLA) return "No hay ninguna carrera abierta.";
        if (!esAnfitrion(p) && !admin) return "Solo el anfitrión puede empezar la carrera.";
        String r = empezar();
        if (r == null) cambios++;
        return r;
    }

    /** El anfitrion (o un organizador) cancela la carrera abierta. */
    public static String cancelarDesdeSala(ServerPlayerEntity p, boolean admin) {
        if (fase != Fase.COLA) return "No hay ninguna carrera abierta.";
        if (!esAnfitrion(p) && !admin) return "Solo el anfitrión puede cancelar la carrera.";
        return cerrarCola();
    }

    /** Cada tick de la cola: si hay al menos 2 corredores y estan todos listos, cuenta atras y sale sola. */
    static void colaAuto(int now) {
        if (anfitrion == null || corr.size() < 2) { listosDesde = -1; return; }
        boolean todos = true;
        for (Corredor c : corr.values()) if (!c.listo) { todos = false; break; }
        if (!todos) { listosDesde = -1; return; }
        if (listosDesde < 0) listosDesde = now;
        int resta = ESPERA_LISTOS - (now - listosDesde);
        if (resta > 0) {
            if (resta % 20 == 0) for (ServerPlayerEntity p : jugadores()) Msg.title(p, Text.literal(String.valueOf(resta / 20)).formatted(Formatting.YELLOW, Formatting.BOLD), Text.literal("¡Todos listos! Sale la carrera...").formatted(Formatting.GRAY), 0, 22, 0);
            return;
        }
        listosDesde = -1;
        String r = empezar();
        if (r != null) {
            Msg.broadcastTo(jugadores(), Msg.err(r));
            for (Corredor c : corr.values()) c.listo = false;
            cambios++;
        } else cambios++;
    }

    static void mostrarEquipo(ServerPlayerEntity p, Corredor c, List<Montura.Candidato> cand) {
        p.sendMessage(Msg.say(Text.literal("Elige tu Pokémon de carrera (solo terrestres; todos corren igual de rápido):").formatted(Formatting.WHITE)), false);
        boolean alguno = false;
        for (var x : cand) {
            if (!x.valido()) {
                p.sendMessage(Text.literal("   " + x.slot() + ". ").formatted(Formatting.DARK_GRAY).append(x.pokemon().getDisplayName(false).copy().formatted(Formatting.DARK_GRAY)).append(Text.literal("  (no puede: no es terrestre)").formatted(Formatting.DARK_GRAY)), false);
                continue;
            }
            alguno = true;
            boolean sel = c.slot == x.slot();
            p.sendMessage(Text.literal("   " + x.slot() + ". ").formatted(Formatting.GRAY).append(x.pokemon().getDisplayName(false).copy().formatted(Formatting.WHITE))
                    .append(Text.literal("  ")).append(sel ? Text.literal("[ELEGIDO]").formatted(Formatting.GREEN, Formatting.BOLD)
                            : Msg.button("ELEGIR", "/carrera elegir " + x.slot(), Formatting.AQUA, "Correr con este Pokémon")), false);
        }
        if (!alguno) p.sendMessage(Text.literal("   No tienes ningún Pokémon terrestre en el equipo: te prestaremos uno.").formatted(Formatting.YELLOW), false);
    }

    public static String elegir(ServerPlayerEntity p, int slot) {
        Corredor c = corr.get(p.getUuid());
        if (c == null || (fase != Fase.COLA)) return "Solo puedes elegir mientras esperas a que empiece la carrera.";
        var cand = Montura.candidatos(p);
        var x = cand.stream().filter(k -> k.slot() == slot).findFirst();
        if (x.isEmpty()) return "No tienes ningún Pokémon en el hueco " + slot + ".";
        if (!x.get().valido()) return "Ese Pokémon no es terrestre (vuela o no camina): elige otro.";
        c.slot = slot; c.pokemon = x.get().pokemon(); c.prestado = false; cambios++;
        if (!Sala.enSala(p)) mostrarEquipo(p, c, cand);
        return null;
    }

    public static String salir(ServerPlayerEntity p) {
        Corredor c = corr.get(p.getUuid());
        if (c == null) return "No estas apuntado.";
        if (fase == Fase.COLA) { quitarDeLaCola(p.getUuid()); p.sendMessage(Msg.say("Te has bajado de la carrera."), false); return null; }
        if (fase == Fase.FIN) return "La carrera ya ha terminado.";
        abandonar(c, p, "ha abandonado la carrera");
        return null;
    }

    // ------------------------------------------------------------------ salida

    public static String empezar() {
        if (fase != Fase.COLA) return "No hay cola abierta (usa /carrera abrir).";
        corr.values().removeIf(c -> server.getPlayerManager().getPlayer(c.id) == null);
        if (corr.size() < cfg.minPlayers) return "Faltan corredores: hacen falta al menos " + cfg.minPlayers + " y hay " + corr.size() + ".";
        ServerWorld w = mundo();
        if (w == null) return "No existe la dimension de la pista.";
        if (pista.plantilla != null && !instalada()) {
            String r = instalar();
            if (r != null) return r;
        }
        List<Corredor> orden = new ArrayList<>(corr.values());
        Collections.shuffle(orden, rnd);
        for (int i = 0; i < orden.size(); i++) orden.get(i).hueco = i % pista.parrilla.length;
        limpiarEntidades();
        equipo();
        for (Corredor c : orden) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(c.id);
            try { if (!(Saved.exists(server, c.id) && Saved.enLobby(server, c.id))) Saved.save(server, p); }          // desde la sala ya esta guardado lo de antes de entrar
            catch (RuntimeException e) { return "No se pudo guardar el inventario de " + c.nombre + ": la carrera no empieza (nada ha cambiado)."; }
        }
        vueltasTotal = totalVueltas();
        jugadoresAlEmpezar = orden.size();
        resultados.clear();
        llegada.clear();
        podioTick = null;
        for (Corredor c : orden) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(c.id);
            if (c.pokemon == null) { c.pokemon = prestar(); c.prestado = true; p.sendMessage(Msg.say("Corres con un Pokémon prestado: " + c.pokemon.getSpecies().getName() + "."), false); }
            double[] g = pista.parrilla[c.hueco];
            c.reaparicion = new Vec3d(g[0], g[1], g[2]); c.reaparicionYaw = (float) g[3];
            c.vuelta = 0; c.siguiente = 1; c.finTick = -1; c.tiemposVuelta = new int[vueltasTotal]; c.abandono = false; c.recogidas = c.golpesDados = c.golpesRecibidos = 0;
            c.poder = null; c.mejorado = false; c.estrellas = 0; c.escudoHasta = c.paradoHasta = c.lentoHasta = c.turboHasta = c.ciegoHasta = c.golpeInmuneHasta = 0; c.ultimoModificador = Double.NaN; c.previa = null;
            p.getInventory().clear();
            p.clearStatusEffects();
            p.changeGameMode(cfg.presentacion ? GameMode.SPECTATOR : GameMode.ADVENTURE);   // en la presentacion el jugador hace de camara
            p.getAbilities().flying = false; p.sendAbilitiesUpdate();
            p.setHealth(p.getMaxHealth());
            p.teleport(w, g[0], g[1], g[2], (float) g[3], 0f);
            unir(p.getNameForScoreboard());
            if (!cfg.presentacion) prepararCorredor(p, c);
        }
        crearCajas(w);
        crearVoltorbFijos(w);
        Pausas.pausar();
        graciaHasta = ahora() + 60;
        if (cfg.presentacion) comenzarPresentacion(w, orden);
        if (fase != Fase.PRESENTACION) { fase = Fase.CUENTA; tickCuenta = ahora() + cfg.cuentaAtras * 20; }
        EmiCarreras.LOG.info("Carrera en '{}' con {} corredores y {} vueltas", pista.id, corr.size(), vueltasTotal);
        return null;
    }

    static Pokemon prestar() {
        String sp = cfg.prestados.get(rnd.nextInt(cfg.prestados.size()));
        return PokemonProperties.Companion.parse("species=" + sp + " level=20").create();
    }

    static Scoreboard sb() { return server.getScoreboard(); }

    /** Mete en el equipo de la carrera solo a quien no tenga equipo (otros mods usan equipos para prefijos/colores y no hay que tocarlos). */
    static void unir(String nombre) {
        try { if (sb().getScoreHolderTeam(nombre) == null) sb().addScoreHolderToTeam(nombre, equipo()); }
        catch (Exception e) { EmiCarreras.LOG.debug("equipo: {}", e.toString()); }
    }

    static void soltar(String nombre) {
        try { Team t = sb().getTeam("emi_carrera"); if (t != null && sb().getScoreHolderTeam(nombre) == t) sb().removeScoreHolderFromTeam(nombre, t); }
        catch (Exception e) { EmiCarreras.LOG.debug("equipo: {}", e.toString()); }
    }

    static Team equipo() {
        Team t = sb().getTeam("emi_carrera");
        if (t == null) t = sb().addTeam("emi_carrera");
        t.setCollisionRule(AbstractTeam.CollisionRule.NEVER);
        t.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.NEVER);
        t.setShowFriendlyInvisibles(false);
        return t;
    }

    // ------------------------------------------------------------------ tick

    public static void tick(MinecraftServer s) {
        try { tick2(s); }
        catch (Throwable t) {
            EmiCarreras.LOG.error("Error en la carrera; se cancela para no tumbar el servidor", t);
            try { limpiar(); } catch (Throwable t2) { EmiCarreras.LOG.error("Tampoco se pudo limpiar", t2); fase = Fase.LIBRE; corr.clear(); }
        }
    }

    static void tick2(MinecraftServer s) {
        if (fase == Fase.LIBRE) { if (s.getTicks() % 200 == 0 && mundo() != null) limpiarEntidades(); return; }
        int now = ahora();
        if (fase == Fase.COLA) {
            for (UUID id : new ArrayList<>(corr.keySet())) if (server.getPlayerManager().getPlayer(id) == null) quitarDeLaCola(id);
            if (fase != Fase.COLA) return;
            colaAuto(now);
            if (cfg.queueReminderSeconds > 0 && now - tickAviso >= cfg.queueReminderSeconds * 20) { tickAviso = now; anunciar(); }
            return;
        }
        ServerWorld w = mundo();
        if (w == null) { parar("la dimension de la pista ya no existe"); return; }

        if (fase == Fase.PRESENTACION) presentacion(w, now);
        if (fase == Fase.PRESENTACION || fase == Fase.CUENTA) girarMonedas(w, now);      // las monedas giran desde el principio

        if (fase == Fase.CUENTA) {
            int restan = tickCuenta - now;
            if (restan % 20 == 0 && restan > 0) {
                int seg = restan / 20;
                for (ServerPlayerEntity p : jugadores()) {
                    Msg.title(p, Text.literal(String.valueOf(seg)).formatted(seg <= 3 ? Formatting.RED : Formatting.YELLOW, Formatting.BOLD), Text.literal(pista.nombre).formatted(Formatting.GRAY), 0, 22, 0);
                    Msg.sound(p, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 1f, seg <= 3 ? 1.6f : 1.0f);
                }
            }
            if (restan <= 0) {
                fase = Fase.CARRERA; tickInicio = now; limite = -1;
                for (ServerPlayerEntity p : jugadores()) {
                    Msg.title(p, Text.literal("¡YA!").formatted(Formatting.GREEN, Formatting.BOLD), null, 0, 30, 10);
                    Msg.sound(p, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1f, 1.8f);
                    sfx(p, SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1f);
                    sfx(p, SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.8f, 1.2f);
                }
                musicaActual = null;
            }
        }

        // jugadores desconectados o fuera de la pista
        for (Corredor c : new ArrayList<>(corr.values())) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(c.id);
            if (p == null) { if (!c.abandono) abandonar(c, null, "se ha desconectado"); continue; }
            if (c.abandono) continue;
            if (p.getServerWorld().getRegistryKey() != MUNDO) {
                if (now < graciaHasta) {        // el teletransporte puede tardar un tick en completarse: se repite
                    p.teleport(w, c.reaparicion.x, c.reaparicion.y, c.reaparicion.z, c.reaparicionYaw, 0f);
                    continue;
                }
                EmiCarreras.LOG.info("{} esta fuera de la pista (dimension {}): se le da por retirado", c.nombre, p.getServerWorld().getRegistryKey().getValue());
                abandonar(c, p, "ha salido de la pista"); continue;
            }
            if (fase != Fase.PRESENTACION) mantener(p, c, w, now);
        }

        if (fase == Fase.CARRERA) {
            Map<Corredor, Integer> puestos = puestos();
            for (Corredor c : corr.values()) {
                ServerPlayerEntity p = server.getPlayerManager().getPlayer(c.id);
                if (p == null || c.abandono || c.finTick >= 0) continue;
                avanzar(p, c, w, now, puestos);
            }
            moverProyectiles(w, now);
            comprobarTrampas(w, now);
            actualizarCajas(w, now);
            actualizarFijos(w, now);
            musica(now);
            ambiente(now);
            if (now % 4 == 0) hud(now, puestos);
            // final: todos han acabado o se acabo el tiempo
            boolean todos = true;
            for (Corredor c : corr.values()) if (!c.abandono && c.finTick < 0) { todos = false; break; }
            if (todos || (limite > 0 && now >= limite)) finalizar();
        } else if (fase == Fase.FIN) {
            for (Corredor c : corr.values()) {
                ServerPlayerEntity p = server.getPlayerManager().getPlayer(c.id);
                if (p != null && !c.abandono) Montura.velocidadEfecto(p, 0);
            }
            if (now >= tickFin) limpiar();
        }
        if (fase == Fase.CARRERA || fase == Fase.FIN) enviarAlPodio(now);
        if (now % 5 == 0) enviarEstado(now, true);
        if (now % 40 == 0) expulsarIntrusos(w);
        if (now % 20 == 10) barrerMonedasSueltas(w);
    }

    /**
     * Quita las monedas (cajas) que no son las de esta carrera: las que quedaron guardadas en el mundo de una carrera anterior (si el servidor
     * se paro o los trozos de pista estaban descargados al limpiar) reaparecen al cargarse el trozo y se verian quietas, duplicadas.
     */
    static void barrerMonedasSueltas(ServerWorld w) {
        Set<UUID> buenas = new HashSet<>();
        for (Caja c : cajas) if (c.uuid != null) buenas.add(c.uuid);
        for (Corredor c : corr.values()) if (c.asiento != null) buenas.add(c.asiento.getUuid());
        var mp = pista == null ? null : pista.mapa;       // solo se mira la zona de la pista (y un margen): un cuadro de 60 millones de bloques cada segundo era carisimo
        Box zona = mp == null ? new Box(-30000000, -64, -30000000, 30000000, 320, 30000000)
                : new Box(mp.x0 - 60, -64, mp.z0 - 60, mp.x0 + mp.ancho + 60, 320, mp.z0 + mp.largo + 60);
        int n = 0;
        for (var e : w.getEntitiesByClass(net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity.class, zona, e -> e.getCommandTags().contains(TAG))) {
            if (!buenas.contains(e.getUuid())) { e.discard(); n++; }
        }
        Set<UUID> conocidos = new HashSet<>();
        for (Corredor c : corr.values()) if (c.visual != null) conocidos.add(c.visual.getUuid());
        for (Trampa t : trampas) if (t.ent != null) conocidos.add(t.ent.getUuid());
        for (Fijo f : fijos) if (f.ent != null) conocidos.add(f.ent.getUuid());
        for (var e : presVisuales) conocidos.add(e.getUuid());
        for (var e : w.getEntitiesByClass(PokemonEntity.class, zona, e -> e.getCommandTags().contains(TAG))) {
            if (!conocidos.contains(e.getUuid())) { e.discard(); n++; }          // Pokemon de una carrera anterior que reaparecen al cargar su trozo
        }
        if (n > 0) EmiCarreras.LOG.info("Se quitaron {} monedas sueltas de una carrera anterior", n);
        if (ahora() % 200 == 10) { int ok = 0; for (Caja c : cajas) if (c.uuid != null && w.getEntity(c.uuid) != null) ok++; EmiCarreras.LOG.info("Monedas: {} de {} existen (las demas se crean al cargarse su trozo de pista)", ok, cajas.size()); }
    }

    /** En la pista no debe haber nada vivo salvo los corredores y lo que pone el minijuego (Pokemon salvajes, mobs...). */
    static void expulsarIntrusos(ServerWorld w) {
        Box zona = new Box(pista.origen[0] - 40, pista.origen[1] - 20, pista.origen[2] - 40, pista.origen[0] + 400, pista.origen[1] + 120, pista.origen[2] + 400);
        for (net.minecraft.entity.LivingEntity e : w.getEntitiesByClass(net.minecraft.entity.LivingEntity.class, zona,
                x -> !(x instanceof net.minecraft.entity.player.PlayerEntity) && !x.getCommandTags().contains(TAG)))
            e.discard();
    }

    /** Mantenimiento por jugador: hambre, inventario, visual, efectos de velocidad. */
    static void mantener(ServerPlayerEntity p, Corredor c, ServerWorld w, int now) {
        p.getHungerManager().setFoodLevel(6);          // con 6 o menos no se puede correr: todos van a la misma velocidad
        p.getHungerManager().setSaturationLevel(0f);
        p.setHealth(p.getMaxHealth());
        // visual del Pokemon
        if (Morph.disponible()) {
            // el jugador va transformado en su Pokemon (CobbleMorph): no hace falta copia visual
            if (c.visual != null) { c.visual.discard(); c.visual = null; }
        } else if (c.visual == null || c.visual.isRemoved()) {
            if (w.isChunkLoaded(p.getBlockPos())) {
                c.visual = Montura.crearVisual(w, c.pokemon, p.getName(), p.getPos(), p.getYaw());
                c.visual.addCommandTag(TAG);
                unir(c.visual.getNameForScoreboard());
            }
        } else {
            Vec3d ahora = p.getPos();
            Vec3d delta = c.previa == null ? Vec3d.ZERO : ahora.subtract(c.previa);
            c.previa = ahora;
            Montura.seguir(c.visual, p, delta, cfg.adelanto);
            boolean brilla = now < c.escudoHasta;
            if (c.visual.isGlowing() != brilla) c.visual.setGlowing(brilla);
        }
        boolean protegido = now < c.escudoHasta;
        if (p.isGlowing() != protegido) p.setGlowing(protegido);        // con la proteccion el jugador brilla
        if (now % 2 == 0) {          // efectos visibles para todos: turbo, golpe y escudo
            if (now < c.turboHasta && now >= c.paradoHasta) w.spawnParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.3, p.getZ(), 2, 0.25, 0.1, 0.25, 0.02);
            if (now < c.paradoHasta && fase == Fase.CARRERA) w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 0.8, p.getZ(), 4, 0.4, 0.5, 0.4, 0.3);
            if (now < c.escudoHasta) w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 0.9, p.getZ(), 3, 0.6, 0.6, 0.6, 0.0);
        }
        // inventario: solo el poder en la ranura 5
        if (now % 5 == 0) {
            var inv = p.getInventory();
            boolean cambio = false;
            for (int i = 0; i < inv.size(); i++) {
                ItemStack st = inv.getStack(i);
                if (i == 4) {
                    if (c.poder == null) { if (!st.isEmpty()) { inv.setStack(i, ItemStack.EMPTY); cambio = true; } }
                    else if (st.isEmpty() || !st.getName().getString().contains(c.poder.nombre) || st.getName().getString().startsWith("★") != c.mejorado) { inv.setStack(i, c.poder.item(c.mejorado)); cambio = true; }
                } else if (!st.isEmpty()) { inv.setStack(i, ItemStack.EMPTY); cambio = true; }
            }
            if (cambio) { p.playerScreenHandler.sendContentUpdates(); }
            if (inv.selectedSlot != 4 && c.poder != null) { inv.selectedSlot = 4; p.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(4)); }
            for (ItemEntity ie : w.getEntitiesByClass(ItemEntity.class, p.getBoundingBox().expand(6), e -> true)) ie.discard();
        }
        if (now >= c.ciegoHasta && c.ciegoHasta != 0) c.ciegoHasta = 0;
        if (now % 5 == 0 && c.estrellas >= cfg.estrellasMejora && fase == Fase.CARRERA) comprobarMejora(c, p);
        aplicarVelocidad(p, c, now);
    }

    static void aplicarVelocidad(ServerPlayerEntity p, Corredor c, int now) {
        boolean parado = fase == Fase.CUENTA || fase == Fase.PRESENTACION || now < c.paradoHasta;
        double f = 0;
        if (now < c.turboHasta) f += c.turboFactor;
        if (now < c.lentoHasta) f -= c.lentoFactor;
        double mod = parado ? -1.0 : Math.max(-0.9, f);
        if (Double.compare(mod, c.ultimoModificador) != 0) { Montura.velocidadEfecto(p, mod); c.ultimoModificador = mod; }
    }

    /** Puntos de control, vueltas, caidas, turbos y cajas. */
    static void avanzar(ServerPlayerEntity p, Corredor c, ServerWorld w, int now, Map<Corredor, Integer> puestos) {
        Vec3d pos = p.getPos();
        if (c.vueloTicks > 0) { volar(p, c, w, now); pos = p.getPos(); }
        else {
            if (pos.y < pista.caidaY) { reaparecer(p, c, w, now); return; }
            var sa = pista.saltoEntre(c.posAnt, pos);
            if (sa != null && now >= c.saltoCooldown) iniciarVuelo(p, c, sa, w, now);
        }
        c.posAnt = pos;
        if (pista.enTurbo(pos)) {
            if (now >= c.turboHasta) sfx(p, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.7f, 1.3f);
            c.turboHasta = Math.max(c.turboHasta, now + 25); c.turboFactor = Math.max(c.turboFactor, 0.45);
        }
        double barro = pista.lodoEn(pos);
        if (barro > 0 && c.vueloTicks == 0) {
            if (now >= c.lentoHasta) c.lentoFactor = barro; else c.lentoFactor = Math.max(c.lentoFactor, barro);
            c.lentoHasta = Math.max(c.lentoHasta, now + 8);
            if (now % 8 == 0) { sfx(p, SoundEvents.BLOCK_MUD_STEP, 0.8f, 0.8f); w.spawnParticles(ParticleTypes.SMOKE, p.getX(), p.getY() + 0.1, p.getZ(), 3, 0.3, 0.05, 0.3, 0.01); }
        }
        if (pista.zona(c.siguiente).contains(pos)) pasarPunto(p, c, w, now);
    }

    // ------------------------------------------------------------------ saltos (plataformas de salto)

    /** Lanza al corredor en arco hasta el destino de la plataforma; con turbo activo llega mas lejos (otro destino). */
    static void iniciarVuelo(ServerPlayerEntity p, Corredor c, Pista.Salto sa, ServerWorld w, int now) {
        boolean turbo = now < c.turboHasta && sa.destino_turbo != null;
        double[] d = turbo ? sa.destino_turbo : sa.destino;
        Vec3d pos = p.getPos();
        Vec3d centro = sa.caja.getCenter();
        double lat = 0;
        if (sa.normal != null) lat = Math.max(-3.2, Math.min(3.2, (pos.x - centro.x) * sa.normal[0] + (pos.z - centro.z) * sa.normal[1]));
        double nx = sa.normal == null ? 0 : sa.normal[0], nz = sa.normal == null ? 0 : sa.normal[1];
        c.vueloA = pos;
        c.vueloB = new Vec3d(d[0] + nx * lat, d[1], d[2] + nz * lat);
        c.vueloAltura = sa.altura;
        c.vueloTurbo = turbo;
        c.vueloIni = now;
        c.vueloTicks = Math.max(8, turbo && sa.ticks_turbo > 0 ? sa.ticks_turbo : sa.ticks);
        c.saltoCooldown = now + c.vueloTicks + 30;
        // el jugador va montado en un asiento invisible que el servidor mueve por la parabola: el movimiento de una entidad lo interpola el cliente
        // (sale fluido) y no depende de la red ni de los fps del jugador, a diferencia de teletransportarlo cada tick o de darle velocidad.
        try {
            Entity asiento = EntityType.ITEM_DISPLAY.create(w);
            asiento.refreshPositionAndAngles(pos.x, pos.y, pos.z, p.getYaw(), 0f);
            asiento.addCommandTag(TAG);
            w.spawnEntity(asiento);
            p.startRiding(asiento, true);
            c.asiento = asiento;
        } catch (Throwable t) { EmiCarreras.LOG.warn("no se pudo crear el asiento del salto: {}", t.toString()); c.asiento = null; }
        EmiCarreras.LOG.info("{} salta{} ({} ticks)", c.nombre, turbo ? " con turbo" : "", c.vueloTicks);
        sfx(p, SoundEvents.ENTITY_BREEZE_JUMP, 1f, 1f);
        sfx(p, SoundEvents.BLOCK_SLIME_BLOCK_BREAK, 1f, 0.7f);
        if (turbo) { sfx(p, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 0.9f); Msg.title(p, Text.literal("¡Salto con turbo!").formatted(Formatting.AQUA, Formatting.BOLD), null, 0, 25, 8); }
        w.spawnParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.2, pos.z, 18, 0.4, 0.1, 0.4, 0.08);
    }

    static final boolean DEPURAR = Boolean.getBoolean("emi.carreras.debug");

    static Vec3d arco(Corredor c, double t) {
        return new Vec3d(c.vueloA.x + (c.vueloB.x - c.vueloA.x) * t,
                c.vueloA.y + (c.vueloB.y - c.vueloA.y) * t + 4 * c.vueloAltura * t * (1 - t),
                c.vueloA.z + (c.vueloB.z - c.vueloA.z) * t);
    }

    /** Un tick de vuelo: se mueve el asiento por la parabola (el jugador va montado). */
    static void volar(ServerPlayerEntity p, Corredor c, ServerWorld w, int now) {
        int k = now - c.vueloIni, T = c.vueloTicks;
        p.fallDistance = 0;
        if (c.asiento == null || c.asiento.isRemoved()) { c.vueloTicks = 0; c.asiento = null; return; }
        if (p.getVehicle() != c.asiento) p.startRiding(c.asiento, true);     // si pulsa agacharse para bajar, se le vuelve a montar
        if (k >= T) {
            c.asiento.refreshPositionAndAngles(c.vueloB.x, c.vueloB.y, c.vueloB.z, p.getYaw(), 0f);
            terminarVuelo(p, c, w, now, true);
            return;
        }
        Vec3d e = arco(c, (k + 1) / (double) T);
        c.asiento.refreshPositionAndAngles(e.x, e.y, e.z, p.getYaw(), 0f);
        if (DEPURAR) EmiCarreras.LOG.info("vuelo k={} jugador=({}, {}, {}) asiento=({}, {}, {})", k, String.format("%.1f", p.getX()), String.format("%.1f", p.getY()), String.format("%.1f", p.getZ()), String.format("%.1f", e.x), String.format("%.1f", e.y), String.format("%.1f", e.z));
        if (now % 3 == 0) w.spawnParticles(ParticleTypes.CLOUD, e.x, e.y, e.z, 2, 0.15, 0.1, 0.15, 0.01);
    }

    /** Baja al jugador del asiento y lo deja en el sitio exacto del aterrizaje. */
    static void terminarVuelo(ServerPlayerEntity p, Corredor c, ServerWorld w, int now, boolean normal) {
        c.vueloTicks = 0; c.posAnt = null;
        if (p != null) p.stopRiding();
        if (c.asiento != null) { c.asiento.discard(); c.asiento = null; }
        if (p == null || !normal) return;
        p.networkHandler.requestTeleport(c.vueloB.x, c.vueloB.y, c.vueloB.z, p.getYaw(), p.getPitch());
        Vec3d h = new Vec3d(c.vueloB.x - c.vueloA.x, 0, c.vueloB.z - c.vueloA.z);
        Vec3d d = h.lengthSquared() > 1e-6 ? h.normalize().multiply(0.3) : Vec3d.ZERO;
        p.setVelocity(d.x, -0.1, d.z); p.velocityModified = true;
        if (c.vueloB.y > pista.caidaY + 3) {          // aterrizaje en la pista
            c.turboHasta = Math.max(c.turboHasta, now + 15); c.turboFactor = Math.max(c.turboFactor, 0.3);
            sfx(p, SoundEvents.BLOCK_ANVIL_LAND, 0.4f, 1.6f);
            w.spawnParticles(ParticleTypes.CLOUD, c.vueloB.x, c.vueloB.y + 0.2, c.vueloB.z, 12, 0.4, 0.1, 0.4, 0.06);
        }
    }

    // ------------------------------------------------------------------ Voltorb fijos

    static void crearVoltorbFijos(ServerWorld w) {
        fijos.clear();
        if (pista == null || pista.voltorbs == null) return;
        for (double[] a : pista.voltorbs) { Fijo f = new Fijo(); f.pos = new Vec3d(a[0], a[1], a[2]); fijos.add(f); }
    }

    static PokemonEntity crearVoltorb(ServerWorld w, double x, double y, double z, float yaw) {
        PokemonEntity e = PokemonProperties.Companion.parse("species=voltorb level=10").createEntity(w);
        e.setAiDisabled(true); e.setInvulnerable(true); e.setSilent(true); e.setNoGravity(false); e.setPersistent();
        e.getPokemon().setScaleModifier(1.3f);
        e.refreshPositionAndAngles(x, y, z, yaw, 0f);
        e.addCommandTag(TAG);
        w.spawnEntity(e);
        unir(e.getNameForScoreboard());
        return e;
    }

    /** Los Voltorb fijos solo existen cuando hay un corredor cerca (asi no quedan guardados en trozos descargados). */
    static void actualizarFijos(ServerWorld w, int now) {
        if (now % 5 != 0) return;
        for (Fijo f : fijos) {
            double cerca = 1e9;
            for (ServerPlayerEntity p : jugadores()) cerca = Math.min(cerca, Math.hypot(p.getX() - f.pos.x, p.getZ() - f.pos.z));
            if (f.ent == null || f.ent.isRemoved()) {
                f.ent = null;
                if (now >= f.libreEn && cerca < 70) f.ent = crearVoltorb(w, f.pos.x, f.pos.y, f.pos.z, rnd.nextFloat() * 360);
                continue;
            }
            if (cerca > 110) { f.ent.discard(); f.ent = null; continue; }
            Corredor v = rivalCerca(f.ent.getPos(), null, 1.4);
            if (v != null) {
                w.spawnParticles(ParticleTypes.EXPLOSION, f.pos.x, f.pos.y + 0.7, f.pos.z, 3, 0.3, 0.3, 0.3, 0.0);
                w.playSound(null, f.ent.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 0.8f, 1.2f);
                golpear(v, PELIGRO, "voltorb", w, now);
                f.ent.discard(); f.ent = null; f.libreEn = now + 20 * 12;
            }
        }
    }

    static void pasarPunto(ServerPlayerEntity p, Corredor c, ServerWorld w, int now) {
        int t = now - tickInicio;
        if (c.siguiente == 0) {                       // cruza la meta
            c.tiemposVuelta[c.vuelta] = t - c.ultimaVueltaTick; c.ultimaVueltaTick = t;
            c.vuelta++;
            EmiCarreras.LOG.info("{} completa la vuelta {} ({})", c.nombre, c.vuelta, fmt(c.tiemposVuelta[c.vuelta - 1]));
            if (c.vuelta >= vueltasTotal) { terminar(p, c, t); return; }
            Msg.title(p, Text.literal("Vuelta " + (c.vuelta + 1) + "/" + vueltasTotal).formatted(Formatting.GOLD, Formatting.BOLD), Text.literal(c.vuelta + 1 == vueltasTotal ? "¡Última vuelta!" : "Tu mejor vuelta: " + fmt(mejorVuelta(c))).formatted(Formatting.YELLOW), 0, 30, 10);
            Msg.sound(p, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
            sfx(p, c.vuelta + 1 == vueltasTotal ? SoundEvents.BLOCK_BELL_USE : SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1f, c.vuelta + 1 == vueltasTotal ? 1f : 1.5f);
            c.siguiente = 1;
        } else {
            double[] cp = pista.checkpoints[c.siguiente];
            c.reaparicion = new Vec3d(cp[6], cp[7], cp[8]); c.reaparicionYaw = (float) cp[9];
            EmiCarreras.LOG.info("{} pasa el punto de control {}", c.nombre, c.siguiente);
            sfx(p, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 0.6f, 1.4f);
            c.siguiente = (c.siguiente + 1) % pista.n();
        }
    }

    static int mejorVuelta(Corredor c) { int m = Integer.MAX_VALUE; for (int i = 0; i < c.vuelta; i++) m = Math.min(m, c.tiemposVuelta[i]); return m == Integer.MAX_VALUE ? 0 : m; }

    static void reaparecer(ServerPlayerEntity p, Corredor c, ServerWorld w, int now) {
        EmiCarreras.LOG.info("{} se cae de la pista y reaparece en el punto de control", c.nombre);
        terminarVuelo(p, c, w, now, false);
        if (fase == Fase.CARRERA) perderEstrellas(c, p, "Caída");
        p.teleport(w, c.reaparicion.x, c.reaparicion.y, c.reaparicion.z, c.reaparicionYaw, 0f);
        c.paradoHasta = Math.max(c.paradoHasta, now + cfg.respawnSegundos * 20);
        c.turboHasta = 0;
        c.previa = null;
        Msg.sound(p, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.2f);
        sfx(p, SoundEvents.BLOCK_LAVA_EXTINGUISH, 0.8f, 1f);
    }

    static void terminar(ServerPlayerEntity p, Corredor c, int t) {
        c.finTick = t;
        llegada.add(c.id);
        EmiCarreras.LOG.info("{} termina en el puesto {} ({})", c.nombre, llegada.size(), fmt(t));
        int puesto = llegada.size();
        Msg.broadcastTo(jugadores(), Msg.say(Text.literal(c.nombre).formatted(Formatting.AQUA).append(Text.literal(" ha terminado en el puesto " + puesto + " (" + fmt(t) + ")").formatted(Formatting.WHITE))));
        Msg.title(p, Text.literal("¡META!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal("Puesto " + puesto + " · " + fmt(t)).formatted(Formatting.YELLOW), 0, 80, 20);
        Msg.sound(p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        sfx(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        if (puesto == 1 && limite < 0) {
            limite = ahora() + cfg.segundosTrasPrimero * 20;
            Msg.broadcastTo(jugadores(), Msg.say("Quedan " + cfg.segundosTrasPrimero + " segundos para que termine la carrera."));
        }
        if (podioTick == null) podioTick = new int[0];
        c.poder = null; enviarPoder(p, (Poder) null, false);
        c.paradoHasta = 0;
        podioPendiente.put(c.id, ahora() + 60);
    }

    static final Map<UUID, Integer> podioPendiente = new HashMap<>();

    static void enviarAlPodio(int now) {
        if (pista.podio == null) return;
        for (var it = podioPendiente.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            if (now < e.getValue()) continue;
            it.remove();
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(e.getKey());
            Corredor c = corr.get(e.getKey());
            if (p == null || c == null || c.abandono) continue;
            double[] d = pista.podio;
            p.teleport(mundo(), d[0] + (rnd.nextDouble() - 0.5) * 3, d[1], d[2] + (rnd.nextDouble() - 0.5) * 3, (float) d[3], 0f);
            c.previa = null;
        }
    }

    static void abandonar(Corredor c, ServerPlayerEntity p, String motivo) {
        EmiCarreras.LOG.info("{} {}", c.nombre, motivo);
        c.abandono = true;
        if (c.visual != null) { c.visual.discard(); c.visual = null; }
        Msg.broadcastTo(jugadores(), Msg.say(c.nombre + " " + motivo + "."));
        if (p != null) { soltar(p.getNameForScoreboard()); p.setGlowing(false); Sala.devolver(server, p, null); }
        // si se desconecto, se le devuelve lo suyo al volver a entrar (Saved queda en disco)
    }

    // ------------------------------------------------------------------ clasificacion

    /** Orden actual: mas progreso primero; a igualdad, el mas cerca de su siguiente punto de control. */
    static List<Corredor> ordenados() {
        List<Corredor> l = new ArrayList<>();
        Map<Corredor, Double> pun = new HashMap<>();
        for (Corredor c : corr.values()) {
            if (c.abandono && c.finTick < 0) continue;
            double v = c.progreso(pista);
            if (c.finTick < 0) {
                ServerPlayerEntity p = server.getPlayerManager().getPlayer(c.id);
                if (p != null) v += 900 - Math.min(900, p.getPos().distanceTo(pista.centro(c.siguiente)));
            }
            pun.put(c, v); l.add(c);
        }
        l.sort((a, b) -> Double.compare(pun.get(b), pun.get(a)));
        return l;
    }

    static Map<Corredor, Integer> puestos() {
        Map<Corredor, Integer> m = new HashMap<>();
        int i = 1;
        for (Corredor c : ordenados()) m.put(c, i++);
        return m;
    }

    static void hud(int now, Map<Corredor, Integer> puestos) {
        int n = puestos.size();
        for (Corredor c : corr.values()) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(c.id);
            if (p == null || c.abandono) continue;
            if (ServerPlayNetworking.canSend(p, EstadoPayload.ID)) continue;       // con la interfaz del cliente ya sale todo en pantalla
            Text t;
            if (c.finTick >= 0) t = Text.literal("¡Has terminado! Puesto " + (llegada.indexOf(c.id) + 1) + " · " + fmt(c.finTick)).formatted(Formatting.GOLD);
            else {
                var tx = Text.literal("Vuelta " + Math.min(c.vuelta + 1, vueltasTotal) + "/" + vueltasTotal).formatted(Formatting.GOLD)
                        .append(Text.literal("   Puesto " + puestos.getOrDefault(c, n) + "/" + n).formatted(Formatting.AQUA))
                        .append(Text.literal("   " + fmt(now - tickInicio)).formatted(Formatting.WHITE));
                if (c.poder != null) tx.append(Text.literal("   ").append(c.poder.titulo()));
                if (now < c.escudoHasta) tx.append(Text.literal("  ✔ protegido " + ((c.escudoHasta - now + 19) / 20) + " s").formatted(Formatting.GREEN));
                t = tx;
            }
            Msg.actionBar(p, t);
        }
    }

    static String fmt(int ticks) {
        int ms = ticks * 50, m = ms / 60000, s = (ms / 1000) % 60, d = (ms % 1000) / 100;
        return m + ":" + (s < 10 ? "0" : "") + s + "." + d;
    }

    // ------------------------------------------------------------------ final

    static void finalizar() {
        fase = Fase.FIN;
        tickFin = ahora() + 200;
        pararMusica();
        for (ServerPlayerEntity pj : jugadores()) sfx(pj, SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, 1f, 1f);
        List<Corredor> orden = new ArrayList<>();
        for (UUID id : llegada) { Corredor c = corr.get(id); if (c != null) orden.add(c); }
        for (Corredor c : ordenados()) if (!orden.contains(c)) orden.add(c);
        Text cab = Text.literal("══ Resultados · " + pista.nombre + " ══").formatted(Formatting.GOLD, Formatting.BOLD);
        Msg.broadcast(server, cab);
        int i = 1;
        for (Corredor c : orden) {
            String linea = i + ". " + c.nombre + (c.finTick >= 0 ? "  " + fmt(c.finTick) + (c.vuelta > 0 ? "  (mejor vuelta " + fmt(mejorVuelta(c)) + ")" : "") : "  no ha terminado");
            Msg.broadcast(server, Text.literal(linea).formatted(i == 1 ? Formatting.GOLD : i == 2 ? Formatting.GRAY : i == 3 ? Formatting.RED : Formatting.WHITE));
            if (i <= cfg.recompensas.size() && c.finTick >= 0) {
                String cmd = cfg.recompensas.get(i - 1);
                if (cmd != null && !cmd.isBlank()) try { server.getCommandManager().executeWithPrefix(server.getCommandSource(), cmd.replace("{jugador}", c.nombre)); }
                catch (Exception e) { EmiCarreras.LOG.warn("Fallo el premio '{}'", cmd, e); }
            }
            i++;
        }
        if (!orden.isEmpty()) {
            Corredor g = orden.get(0);
            ServerPlayerEntity pg = server.getPlayerManager().getPlayer(g.id);
            for (ServerPlayerEntity p : jugadores()) Msg.title(p, Text.literal("🏆 " + g.nombre).formatted(Formatting.GOLD, Formatting.BOLD), Text.literal("¡ha ganado la carrera!").formatted(Formatting.YELLOW), 5, 80, 20);
            if (pg != null && pista.podio != null) fuegos(pg);
        }
        try { apuntarResultados(orden); } catch (Throwable t) { EmiCarreras.LOG.error("No se pudieron apuntar los resultados", t); }
        for (ServerPlayerEntity p : jugadores()) p.sendMessage(Msg.say("En unos segundos volverás " + (Saved.exists(server, p.getUuid()) && Saved.enLobby(server, p.getUuid()) ? "a la sala." : "a donde estabas.")), false);
        // quitar proyectiles y trampas
        limpiarEntidades(false);
        // los que no han llegado van al podio tambien
        for (Corredor c : corr.values()) if (c.finTick < 0 && !c.abandono) podioPendiente.put(c.id, ahora() + 20);
    }

    /** Apunta la carrera de cada corredor (estrellas, logros y tiempos) y deja preparado lo que se le ensena en el menu al volver a la sala. */
    static void apuntarResultados(List<Corredor> orden) {
        if (datos == null) return;
        int dur = Math.max(0, ahora() - tickInicio);
        int llegados = 0;
        for (Corredor c : orden) {
            boolean terminada = c.finTick >= 0;
            if (terminada) llegados++;
            if (c.abandono) continue;
            int puesto = terminada ? llegados : 0;                         // los que no terminan van detras de todos los que llegaron
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(c.id);
            Datos.Jugador j = datos.jugador(c.id, c.nombre);
            String especie = c.pokemon == null ? "" : c.pokemon.getSpecies().getResourceIdentifier().getPath();
            boolean primera = terminada && !j.mejores.containsKey(pista.id);
            boolean record = terminada && datos.registrarTiempo(c.id, c.nombre, pista.id, c.finTick, especie, System.currentTimeMillis());
            var res = Logros.aplicar(j, new Logros.Carrera(pista.id, especie, puesto, jugadoresAlEmpezar, entrenamiento, c.recogidas, c.golpesDados, c.golpesRecibidos, c.finTick, dur, "gatitoalien".equals(especie)), record, primera);
            datos.guardar(c.id);
            resultados.put(c.id, Menu.resultado(pista.nombre, entrenamiento, puesto, jugadoresAlEmpezar, terminada ? c.finTick : -1, record, res, j.estrellas));
            if (p != null) {
                if (res.total() > 0) p.sendMessage(Msg.say(Text.literal("Ganas " + res.total() + " estrellas ★ (tienes " + j.estrellas + ").").formatted(Formatting.GOLD)), false);
                for (String l : res.lineas) p.sendMessage(Text.literal("   " + l).formatted(Formatting.YELLOW), false);
            }
        }
    }

    static void fuegos(ServerPlayerEntity p) {
        ServerWorld w = (ServerWorld) p.getWorld();
        for (int k = 0; k < 6; k++) w.spawnParticles(ParticleTypes.FIREWORK, p.getX() + (rnd.nextDouble() - 0.5) * 6, p.getY() + 1 + rnd.nextDouble() * 4, p.getZ() + (rnd.nextDouble() - 0.5) * 6, 12, 0.3, 0.3, 0.3, 0.1);
    }

    public static String parar(String motivo) {
        if (fase == Fase.LIBRE) return "No hay nada en marcha.";
        Msg.broadcast(server, Msg.say("Carrera cancelada" + (motivo == null ? "." : ": " + motivo + ".")));
        limpiar();
        return null;
    }

    // ------------------------------------------------------------------ presentacion (camaras)

    static final int PRES_ESPERA = 100;   // ticks de espera: el cliente tarda en cargar la dimension

    /** Deja al jugador listo para correr: modo aventura, velocidad fija y transformado en su Pokemon. */
    static void prepararCorredor(ServerPlayerEntity p, Corredor c) {
        Montura.prepararJugador(p, cfg.velocidad, !Morph.disponible());
        Morph.transformar(server, p, c.pokemon);
        p.sendMessage(Msg.say("¡A la parrilla! Cuando acabe la cuenta atrás, corre. Pulsa F5 para verte desde fuera."), false);
    }

    /**
     * Presentacion: los jugadores van en modo espectador y se les mueve ellos mismos como si fueran la camara (asi no hace falta nada en el cliente);
     * en cada sitio de la parrilla se pone una copia sin IA del Pokemon de su corredor para poder enfocarla.
     */
    static void comenzarPresentacion(ServerWorld w, List<Corredor> orden) {
        if (orden.isEmpty()) return;
        presOrden = new ArrayList<>(orden);
        presOrden.sort(java.util.Comparator.comparingInt(c -> c.hueco));
        presDur = cfg.presentacionSegundos > 0 ? cfg.presentacionSegundos * 20 : Math.max(24, Math.min(60, 240 / presOrden.size()));
        presIni = ahora();
        presVisuales.clear();
        for (Corredor c : presOrden) {
            try {
                var e = Montura.crearVisual(w, c.pokemon, Text.literal(c.nombre), c.reaparicion, c.reaparicionYaw);
                e.setHeadYaw(c.reaparicionYaw); e.setBodyYaw(c.reaparicionYaw);
                e.addCommandTag(TAG);
                presVisuales.add(e);
            } catch (Throwable t) { EmiCarreras.LOG.warn("presentacion: no se pudo crear el Pokemon de {}: {}", c.nombre, t.toString()); }
        }
        fase = Fase.PRESENTACION;
    }

    /** Un plano por corredor: la camara gira alrededor de su Pokemon y sale su nombre. Al terminar cada uno se transforma en su sitio y empieza la cuenta atras. */
    static void presentacion(ServerWorld w, int now) {
        int t = now - presIni;
        int n = presOrden.size();
        if (n == 0 || t >= PRES_ESPERA + n * presDur) { terminarPresentacion(now); return; }
        if (t < PRES_ESPERA) {
            if (t % 20 == 0) for (ServerPlayerEntity p : jugadores()) Msg.actionBar(p, Text.literal("Preparando la parrilla...").formatted(Formatting.GRAY));
            return;
        }
        int seg = (t - PRES_ESPERA) / presDur;
        int dentro = (t - PRES_ESPERA) % presDur;
        Corredor c = presOrden.get(seg);
        if (dentro == 0) {
            EmiCarreras.LOG.info("Presentacion: plano {} de {} ({})", seg + 1, n, c.nombre);
            for (ServerPlayerEntity p : jugadores())
                Msg.title(p, Text.literal(c.nombre).formatted(Formatting.YELLOW, Formatting.BOLD),
                        Text.literal(c.pokemon.getSpecies().getName()).formatted(Formatting.GRAY), 2, Math.max(4, presDur - 8), 6);
        }
        double f = dentro / (double) presDur;
        double ang = Math.toRadians(c.reaparicionYaw + 50 - 80 * f);      // barrido desde un lado hacia el otro, delante del corredor
        double r = 4.0 - 0.8 * f;
        double objX = c.reaparicion.x, objY = c.reaparicion.y + 0.9, objZ = c.reaparicion.z;
        double cx = objX - Math.sin(ang) * r, cz = objZ + Math.cos(ang) * r, cy = c.reaparicion.y + 1.7 + 0.5 * f;
        double dx = objX - cx, dy = objY - cy, dz = objZ - cz;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        for (ServerPlayerEntity p : jugadores()) p.networkHandler.requestTeleport(cx, cy - p.getStandingEyeHeight(), cz, yaw, pitch);
    }

    static void terminarPresentacion(int now) {
        EmiCarreras.LOG.info("Presentacion terminada");
        quitarPresentacion();
        ServerWorld w = mundo();
        for (Corredor c : corr.values()) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(c.id);
            if (p == null || c.abandono || w == null) continue;
            p.changeGameMode(GameMode.ADVENTURE);
            p.getAbilities().flying = false; p.sendAbilitiesUpdate();
            p.teleport(w, c.reaparicion.x, c.reaparicion.y, c.reaparicion.z, c.reaparicionYaw, 0f);
            prepararCorredor(p, c);
            Msg.title(p, Text.literal("¡Preparados!").formatted(Formatting.GOLD, Formatting.BOLD), null, 0, 20, 5);
        }
        graciaHasta = now + 20;
        fase = Fase.CUENTA;
        tickCuenta = now + cfg.cuentaAtras * 20;
    }

    /** Quita las copias de los Pokemon de la presentacion. */
    static void quitarPresentacion() {
        for (var e : presVisuales) e.discard();
        presVisuales.clear();
        presOrden = new ArrayList<>();
    }

    /** Devuelve a todos lo suyo, quita Pokemon, cajas y proyectiles y deja el minijuego libre. */
    static void limpiar() {
        try { Pausas.reanudar(); } catch (Throwable t) { EmiCarreras.LOG.warn("pausas: {}", t.toString()); }
        try { enviarEstado(ahora(), false); for (ServerPlayerEntity pj : jugadores()) enviarPoder(pj, (Poder) null, false); } catch (Throwable t) { EmiCarreras.LOG.warn("interfaz: {}", t.toString()); }
        try { pararMusica(); } catch (Throwable t) { EmiCarreras.LOG.warn("musica: {}", t.toString()); }
        try { quitarPresentacion(); } catch (Throwable t) { EmiCarreras.LOG.warn("presentacion: {}", t.toString()); }
        for (Corredor c : corr.values()) {
            try {
                if (c.visual != null) { c.visual.discard(); c.visual = null; }
                ServerPlayerEntity p = server.getPlayerManager().getPlayer(c.id);
                if (p != null) {
                    soltar(p.getNameForScoreboard());
                    p.setGlowing(false); if (p.hasVehicle() && c.asiento != null) p.stopRiding();
                    if (!Saved.exists(server, c.id)) EmiCarreras.LOG.warn("{} no tenia nada guardado que devolver", c.nombre);
                    else Sala.devolver(server, p, resultados.get(c.id));
                }
            } catch (Throwable t) { EmiCarreras.LOG.error("No se pudo devolver a {} a su sitio (se le devolvera al volver a entrar)", c.nombre, t); }
        }
        corr.clear(); llegada.clear(); podioPendiente.clear(); resultados.clear(); entrenamiento = false;
        try { limpiarEntidades(); } catch (Throwable t) { EmiCarreras.LOG.error("limpiando entidades", t); }
        try { Team t = sb().getTeam("emi_carrera"); if (t != null) sb().removeTeam(t); } catch (Throwable t) { EmiCarreras.LOG.debug("{}", t.toString()); }
        fase = Fase.LIBRE;
    }

    static void limpiarEntidades() { limpiarEntidades(true); }

    /** Quita todo lo que haya creado el minijuego en la pista (cajas, proyectiles, trampas y Pokemon visuales sueltos). */
    static void limpiarEntidades(boolean todo) {
        ServerWorld w = server == null ? null : mundo();
        for (Bola b : bolas) if (b.ent != null) b.ent.discard();
        bolas.clear();
        for (Trampa t : trampas) if (t.ent != null) t.ent.discard();
        trampas.clear();
        for (Fijo f : fijos) if (f.ent != null) f.ent.discard();
        if (todo) fijos.clear();
        if (!todo || w == null) return;
        for (Caja c : cajas) if (c.ent != null) c.ent.discard();
        cajas.clear();
        Set<UUID> vivos = new HashSet<>();
        for (Corredor c : corr.values()) if (c.visual != null) vivos.add(c.visual.getUuid());
        for (Entity e : w.getEntitiesByClass(Entity.class, new Box(-30000000, -64, -30000000, 30000000, 320, 30000000), e -> e.getCommandTags().contains(TAG)))
            if (!vivos.contains(e.getUuid())) e.discard();
    }

    // ------------------------------------------------------------------ entrada/salida de jugadores

    public static void alEntrar(ServerPlayerEntity p) {
        if (!Saved.exists(server, p.getUuid())) return;
        Corredor c = corr.get(p.getUuid());
        if (c != null && !c.abandono && (fase == Fase.CUENTA || fase == Fase.CARRERA || fase == Fase.FIN)) return;   // sigue en la carrera
        Sala.alEntrar(p);
    }

    /** Quita al jugador de la cola de la carrera (o lo retira si ya habia empezado): al irse de la sala. */
    static void sacarDeLaCola(ServerPlayerEntity p) {
        Corredor c = corr.get(p.getUuid());
        if (c == null) return;
        if (fase == Fase.COLA) quitarDeLaCola(p.getUuid());
        else if (fase != Fase.FIN && !c.abandono) abandonar(c, p, "ha abandonado la carrera");
    }

    public static void alSalir(ServerPlayerEntity p) {
        if (fase == Fase.COLA) quitarDeLaCola(p.getUuid());
    }

    // ------------------------------------------------------------------ pista (instalacion de la plantilla)

    static Path marca() { return server.getSavePath(WorldSavePath.ROOT).resolve("emi_carreras").resolve("instalada_" + pista.id + ".txt"); }
    static boolean instalada() {
        try { return Files.exists(marca()) && Files.readString(marca()).trim().equals(pista.version); } catch (Exception e) { return false; }
    }

    /** Coloca la plantilla de la pista en la dimension (una sola vez; /carrera instalar lo repite). null = bien. */
    public static String instalar() {
        if (pista == null || pista.plantilla == null) return "Esta pista no trae plantilla.";
        ServerWorld w = mundo();
        if (w == null) return "No existe la dimension de la pista.";
        Optional<StructureTemplate> t = server.getStructureTemplateManager().getTemplate(Identifier.of(pista.plantilla));
        if (t.isEmpty()) return "No encuentro la plantilla " + pista.plantilla + ".";
        BlockPos o = new BlockPos(pista.origen[0], pista.origen[1], pista.origen[2]);
        var size = t.get().getSize();
        List<ChunkPos> forzados = new ArrayList<>();
        for (int cx = o.getX() >> 4; cx <= (o.getX() + size.getX()) >> 4; cx++)
            for (int cz = o.getZ() >> 4; cz <= (o.getZ() + size.getZ()) >> 4; cz++) { w.setChunkForced(cx, cz, true); forzados.add(new ChunkPos(cx, cz)); }
        try {
            long t0 = System.currentTimeMillis();
            if (Files.exists(marca())) {      // ya habia una version anterior: se borra para que no queden restos
                BlockPos.Mutable mp = new BlockPos.Mutable();
                for (int x = o.getX(); x <= o.getX() + size.getX(); x++)
                    for (int z = o.getZ(); z <= o.getZ() + size.getZ(); z++)
                        for (int y = o.getY(); y <= o.getY() + size.getY(); y++) {
                            mp.set(x, y, z);
                            if (!w.getBlockState(mp).isAir()) w.setBlockState(mp, net.minecraft.block.Blocks.AIR.getDefaultState(), Block.FORCE_STATE);
                        }
            }
            boolean ok = t.get().place(w, o, o, new StructurePlacementData().setIgnoreEntities(true), w.getRandom(), Block.NOTIFY_LISTENERS);
            EmiCarreras.LOG.info("Pista '{}' colocada ({}) en {} ms", pista.id, ok, System.currentTimeMillis() - t0);
            if (!ok) return "Minecraft no pudo colocar la plantilla.";
            Files.createDirectories(marca().getParent());
            Files.writeString(marca(), pista.version);
        } catch (Exception e) {
            EmiCarreras.LOG.error("No se pudo colocar la pista", e);
            return "Error al colocar la pista: " + e.getMessage();
        } finally {
            for (ChunkPos c : forzados) w.setChunkForced(c.x, c.z, false);
        }
        return null;
    }

    // ------------------------------------------------------------------ cajas y poderes

    static void crearCajas(ServerWorld w) {
        cajas.clear();
        if (!cfg.poderes || pista.cajas == null) return;
        for (double[] a : pista.cajas) {
            Caja c = new Caja(); c.pos = new Vec3d(a[0] + 0.5, a[1], a[2] + 0.5); c.libreEn = 0;
            invocarCaja(w, c);
            cajas.add(c);
        }
        if (cfg.estrellas && pista.estrellas != null) {
            for (double[] a : pista.estrellas) {
                Caja c = new Caja(); c.pos = new Vec3d(a[0] + 0.5, a[1], a[2] + 0.5); c.libreEn = 0; c.estrella = true;
                invocarCaja(w, c);
                cajas.add(c);
            }
        }
    }

    static final int ESTRELLA_MODELO = 7020002;   // custom_model_data de la michiestrellita (assets/minecraft/models/item/nether_star.json)
    static final int MONEDA_MODELO = 7020001;     // custom_model_data de la moneda del Michialien (assets/minecraft/models/item/nautilus_shell.json)

    /**
     * Crea la moneda con un UUID conocido de antemano. No se busca la entidad por zona: una entidad en un trozo cargado "a medias" (lejos del jugador) existe
     * pero no sale en las busquedas por zona, y asi se creaba una moneda nueva cada tick (miles de monedas quietas y el servidor ahogado).
     */
    static void invocarCaja(ServerWorld w, Caja c) {
        c.uuid = UUID.randomUUID();
        int[] u = {(int) (c.uuid.getMostSignificantBits() >> 32), (int) c.uuid.getMostSignificantBits(), (int) (c.uuid.getLeastSignificantBits() >> 32), (int) c.uuid.getLeastSignificantBits()};
        String cmd = String.format(Locale.ROOT, "summon minecraft:item_display %.2f %.2f %.2f {UUID:[I;%d,%d,%d,%d],Tags:[\"%s\"],item_display:\"fixed\","
                + "item:{id:\"%s\",count:1,components:{\"minecraft:custom_model_data\":%d}},"
                + "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,1.1f,0f],scale:[%s]}}",
                c.pos.x, c.pos.y, c.pos.z, u[0], u[1], u[2], u[3], TAG, c.estrella ? "minecraft:nether_star" : "minecraft:nautilus_shell",
                c.estrella ? ESTRELLA_MODELO : MONEDA_MODELO, c.estrella ? "1.1f,1.1f,1.1f" : "0.9f,0.9f,0.9f");
        try { server.getCommandManager().executeWithPrefix(server.getCommandSource().withWorld(w).withSilent(), cmd); }
        catch (Exception e) { EmiCarreras.LOG.warn("No se pudo crear una caja", e); }
        Entity e = w.getEntity(c.uuid);
        c.ent = e;
        c.creada = e != null;
        c.sinEntidad = 0;
    }

    /** Las monedas giran: cada 10 ticks se les cambia la rotacion con interpolacion (media vuelta cada 20 ticks... un cuarto de vuelta por paso). */
    static void girarMonedas(ServerWorld w, int now) {
        if (now % 10 != 0) return;
        double paso = (now / 10) * Math.PI / 2;
        double sn = Math.sin(paso / 2), cs = Math.cos(paso / 2);
        for (Caja c : cajas) {
            if (c.ent == null || c.ent.isRemoved()) continue;
            String cmd = String.format(Locale.ROOT, "data merge entity %s {start_interpolation:0,interpolation_duration:10,transformation:{left_rotation:[0f,%.5ff,0f,%.5ff],right_rotation:[0f,0f,0f,1f],translation:[0f,1.1f,0f],scale:[0.9f,0.9f,0.9f]}}",
                    c.ent.getUuidAsString(), sn, cs);
            try { server.getCommandManager().executeWithPrefix(server.getCommandSource().withWorld(w).withSilent(), cmd); } catch (Exception e) { }
        }
    }

    static void actualizarCajas(ServerWorld w, int now) {
        girarMonedas(w, now);
        for (Caja c : cajas) {
            if (c.ent == null || c.ent.isRemoved()) {
                Entity e = c.uuid == null ? null : w.getEntity(c.uuid);      // puede haber vuelto al cargarse su trozo de pista
                if (e != null && !e.isRemoved()) { c.ent = e; c.creada = true; c.sinEntidad = 0; }
                else if (c.uuid == null) { if (now >= c.libreEn && now % 10 == 0) invocarCaja(w, c); continue; }   // cogida y reaparece / aun no creada
                else if (!c.creada) { if (now % 20 == 0) invocarCaja(w, c); continue; }                            // el trozo no estaba cargado: se reintenta
                else { if (++c.sinEntidad > 200 && w.isChunkLoaded(net.minecraft.util.math.ChunkPos.toLong((int) Math.floor(c.pos.x) >> 4, (int) Math.floor(c.pos.z) >> 4))) { c.uuid = null; c.creada = false; } continue; }
            }
            if (now % 10 == 0) w.spawnParticles(ParticleTypes.END_ROD, c.pos.x, c.pos.y + 1, c.pos.z, 1, 0.4, 0.4, 0.4, 0.01);
            if (c.estrella) {
                for (Corredor r : corr.values()) {
                    ServerPlayerEntity p = server.getPlayerManager().getPlayer(r.id);
                    if (p == null || r.abandono || r.finTick >= 0) continue;
                    if (Math.abs(p.getX() - c.pos.x) < 1.5 && Math.abs(p.getZ() - c.pos.z) < 1.5 && Math.abs(p.getY() - c.pos.y) < 3) {
                        ganarEstrellas(r, p, 1);
                        c.ent.discard(); c.ent = null; c.uuid = null; c.creada = false; c.libreEn = now + cfg.estrellaRespawnSegundos * 20;
                        w.spawnParticles(ParticleTypes.END_ROD, c.pos.x, c.pos.y + 1, c.pos.z, 8, 0.3, 0.3, 0.3, 0.05);
                        break;
                    }
                }
                continue;
            }
            for (Corredor r : corr.values()) {
                ServerPlayerEntity p = server.getPlayerManager().getPlayer(r.id);
                if (p == null || r.abandono || r.finTick >= 0 || r.poder != null) continue;
                if (Math.abs(p.getX() - c.pos.x) < 1.6 && Math.abs(p.getZ() - c.pos.z) < 1.6 && Math.abs(p.getY() - c.pos.y) < 3) {
                    r.poder = sortearPara(r); r.mejorado = false;
                    enviarPoder(p, r, true);
                    Msg.sound(p, SoundEvents.ENTITY_ITEM_PICKUP, 1f, 1.4f);
                    sfx(p, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.5f);
                    p.sendMessage(Text.literal("¡Has conseguido ").formatted(Formatting.WHITE).append(r.poder.titulo()).append(Text.literal("! ").formatted(Formatting.WHITE)).append(Text.literal(r.poder.descripcion).formatted(Formatting.GRAY)), false);
                    c.ent.discard(); c.ent = null; c.uuid = null; c.creada = false; c.libreEn = now + cfg.cajaRespawnSegundos * 20;
                    w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, c.pos.x, c.pos.y + 1, c.pos.z, 12, 0.4, 0.4, 0.4, 0.05);
                    break;
                }
            }
        }
    }

    /** SOLO PRUEBAS (comandos _poder y _golpe, para el organizador): da un poder o recibe un golpe sin necesitar rivales. */
    public static String probar(ServerPlayerEntity p, String que) {
        Corredor c = corr.get(p.getUuid());
        if (c == null || fase != Fase.CARRERA) return "Solo durante la carrera.";
        if (que.startsWith("golpe_")) { golpear(c, PELIGRO, que.substring(6), mundo(), ahora()); return null; }   // solo pruebas: te da un golpe (pokeball, voltorb, trueno)
        try { c.poder = Poder.valueOf(que.toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException e) { return "Poder desconocido: " + Arrays.toString(Poder.values()); }
        c.mejorado = false;
        enviarPoder(p, c, true);
        return null;
    }

    /** Elige el poder de una caja segun el puesto del corredor (quien va detras tiene mas probabilidad de los fuertes). */
    static Poder sortearPara(Corredor r) {
        List<Corredor> orden = ordenados();
        double rr = orden.size() <= 1 ? 0.5 : orden.indexOf(r) / (double) (orden.size() - 1);
        return Poder.sortear(rr, rnd);
    }

    // ------------------------------------------------------------------ michiestrellitas

    /** Suma michiestrellitas y, si llega a la meta, mejora el poder. */
    static void ganarEstrellas(Corredor c, ServerPlayerEntity p, int n) {
        c.estrellas += n;
        c.recogidas += n;
        float tono = 1.0f + Math.min(1.0f, c.estrellas / (float) Math.max(1, cfg.estrellasMejora)) * 0.9f;       // suena mas agudo cuanto mas cerca de la meta
        Msg.sound(p, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, tono);
        sfx(p, SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT, 0.7f, tono);
        comprobarMejora(c, p);
    }

    /**
     * Con la meta de michiestrellitas (10): el poder que llevas pasa a ser MEJORADO (un poco mas fuerte y duradero); si no llevas ninguno, recibes uno ya mejorado.
     * Si ya llevas uno mejorado, las estrellas se guardan (sin pasar de la meta) hasta que cojas otro.
     */
    static void comprobarMejora(Corredor c, ServerPlayerEntity p) {
        int meta = Math.max(1, cfg.estrellasMejora);
        if (c.estrellas < meta || p == null || c.abandono || c.finTick >= 0) return;
        if (c.poder != null && c.mejorado) { c.estrellas = meta; return; }
        if (c.poder == null) {
            Poder nuevo = sortearPara(c);
            for (int i = 0; i < 6 && nuevo == Poder.LLUVIA; i++) nuevo = sortearPara(c);         // la lluvia de estrellas no vale de premio por estrellas
            c.poder = nuevo;
        }
        c.mejorado = true;
        c.estrellas -= meta;
        enviarPoder(p, c, true);
        Msg.sound(p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        sfx(p, SoundEvents.BLOCK_BEACON_POWER_SELECT, 1f, 1.4f);
        Msg.title(p, Text.literal("¡" + meta + " michiestrellitas!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal("Tu poder ahora es ★ " + c.poder.nombre + " (más fuerte)").formatted(Formatting.YELLOW), 0, 35, 8);
        mundo().spawnParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 0.7, 0.5, 0.2);
    }

    /** Un golpe de verdad o una caida te quitan la mitad de las michiestrellitas. */
    static void perderEstrellas(Corredor c, ServerPlayerEntity p, String motivo) {
        int perdidas = c.estrellas / 2;
        if (perdidas <= 0) return;
        c.estrellas -= perdidas;
        p.sendMessage(Msg.say(motivo + ": pierdes " + perdidas + " michiestrellita" + (perdidas == 1 ? "" : "s") + "."), true);
        sfx(p, SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, 0.8f, 0.8f);
        mundo().spawnParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.4, p.getZ(), 12, 0.4, 0.4, 0.4, 0.15);
    }

    /** SOLO PRUEBAS (comando _meta, para el organizador): el corredor cruza la meta ahora mismo. */
    public static String probarMeta(ServerPlayerEntity p) {
        Corredor c = corr.get(p.getUuid());
        if (c == null || fase != Fase.CARRERA) return "Solo durante la carrera (cuando acabe la cuenta atrás).";
        if (c.finTick >= 0) return "Ya has llegado.";
        terminar(p, c, Math.max(1, ahora() - tickInicio));
        return null;
    }

    /** SOLO PRUEBAS (comando _estrellas, para el organizador): da michiestrellitas. */
    public static String probarEstrellas(ServerPlayerEntity p, int n) {
        Corredor c = corr.get(p.getUuid());
        if (c == null || fase != Fase.CARRERA) return "Solo durante la carrera.";
        ganarEstrellas(c, p, n);
        return null;
    }

    /** Clic derecho del corredor: usa su poder. */
    public static void usarPoder(ServerPlayerEntity p) {
        Corredor c = corr.get(p.getUuid());
        if (c == null || fase != Fase.CARRERA || c.poder == null || c.abandono || c.finTick >= 0) return;
        int now = ahora();
        if (now < c.usoCooldown) return;
        c.usoCooldown = now + 6;
        ServerWorld w = mundo();
        Poder pw = c.poder;
        boolean mej = c.mejorado;
        double f = mej ? 1.6 : 1.0;                       // el poder mejorado dura y pega un 60 % mas
        c.poder = null; c.mejorado = false;
        enviarPoder(p, (Poder) null, false);
        EmiCarreras.LOG.info("{} usa {}{}", c.nombre, pw, mej ? " (mejorado)" : "");
        p.getInventory().setStack(4, ItemStack.EMPTY);
        boolean atras = p.isSneaking();
        double yaw = Math.toRadians(p.getYaw());
        Vec3d dir = new Vec3d(-Math.sin(yaw), 0, Math.cos(yaw));
        switch (pw) {
            case BAYA -> { turbo(c, mej ? 0.5 : 0.35, (int) (30 * f), now); Msg.sound(p, SoundEvents.ENTITY_GENERIC_EAT, 1f, 1.2f); }
            case CARAMELO -> { turbo(c, mej ? 0.9 : 0.7, (int) (70 * f), now); Msg.sound(p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1f, 1.5f); w.spawnParticles(ParticleTypes.FIREWORK, p.getX(), p.getY() + 1, p.getZ(), 20, 0.4, 0.5, 0.4, 0.1); }
            case ESCUDO -> {
                c.escudoHasta = now + (int) (160 * f);
                Msg.sound(p, SoundEvents.ITEM_SHIELD_BLOCK, 1f, 1.2f);
                Msg.sound(p, SoundEvents.BLOCK_BEACON_ACTIVATE, 0.8f, 1.6f);
                Msg.title(p, Text.literal("¡Protegido!").formatted(Formatting.GREEN, Formatting.BOLD), Text.literal("Nada te para durante " + (int) (8 * f) + " segundos").formatted(Formatting.GRAY), 0, 25, 8);
                w.spawnParticles(ParticleTypes.ENCHANTED_HIT, p.getX(), p.getY() + 1, p.getZ(), 40, 0.6, 0.7, 0.6, 0.4);
            }
            case POKEBALL -> lanzarBola(w, p, c, atras ? dir.multiply(-1) : dir, atras ? 0 : p.getPitch(), now, f);
            case VOLTORB -> soltarTrampa(w, p, c, dir, now, "voltorb", "voltorb", f);
            case TRUENO -> trueno(w, p, c, now, f);
            case MERMELADA -> soltarTrampa(w, p, c, dir, now, "grimer", "mermelada", f);
            case BOSTEZO -> bostezo(w, p, c, now, f);
            case OVNI -> ovni(w, p, c, now, f, mej);
            case TINTA -> tinta(w, p, c, now, f);
            case ESTRELLA -> {
                turbo(c, 0.5, (int) (100 * f), now);
                c.escudoHasta = Math.max(c.escudoHasta, now + (int) (100 * f));
                Msg.sound(p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1f, 0.8f);
                sfx(p, SoundEvents.BLOCK_BEACON_ACTIVATE, 0.8f, 2f);
                Msg.title(p, Text.literal("¡Superestrella!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal("Turbo y protección").formatted(Formatting.GRAY), 0, 25, 8);
                w.spawnParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 40, 0.6, 0.7, 0.6, 0.3);
            }
            case LLUVIA -> {
                int n = mej ? 8 : 5;
                Msg.sound(p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1f, 1.6f);
                w.spawnParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 2, p.getZ(), 40, 0.8, 1.0, 0.8, 0.05);
                ganarEstrellas(c, p, n);
                p.sendMessage(Msg.say("¡Llueven michiestrellitas: +" + n + "!"), false);
            }
        }
    }

    static void turbo(Corredor c, double factor, int ticks, int now) {
        c.turboHasta = Math.max(c.turboHasta, now + ticks); c.turboFactor = Math.max(c.turboFactor, factor);
    }

    static void lanzarBola(ServerWorld w, ServerPlayerEntity p, Corredor c, Vec3d dir, float pitch, int now, double f) {
        // se lanza hacia donde apunta el jugador (tambien arriba/abajo, limitado para que no se vaya al cielo ni al suelo): hay que apuntar
        double vy = -Math.sin(Math.toRadians(Math.max(-12f, Math.min(12f, pitch)))) * 1.3;
        SnowballEntity s = new SnowballEntity(w, p.getX() + dir.x * 1.2, p.getY() + 0.9, p.getZ() + dir.z * 1.2);
        s.setItem(Poder.POKEBALL.item());
        s.setNoGravity(true);
        s.setOwner(p);
        s.setVelocity(dir.x * 1.3, vy, dir.z * 1.3);
        s.addCommandTag(TAG);
        w.spawnEntity(s);
        Bola b = new Bola(); b.ent = s; b.dueno = c; b.nace = now; b.ultima = s.getPos(); b.fuerza = f;
        bolas.add(b);
        w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 1f, 0.8f);
    }

    static void moverProyectiles(ServerWorld w, int now) {
        for (Iterator<Bola> it = bolas.iterator(); it.hasNext(); ) {
            Bola b = it.next();
            boolean quitada = b.ent.isRemoved();
            Vec3d pos = quitada ? b.ultima.add(b.ent.getVelocity()) : b.ent.getPos();
            Corredor v = rivalCerca(pos, b.dueno, quitada ? 2.4 : 1.4);
            if (v != null) { golpear(v, b.dueno, "pokeball", w, now, b.fuerza); b.ent.discard(); it.remove(); continue; }
            if (quitada || now - b.nace > 90) { b.ent.discard(); it.remove(); continue; }
            b.ultima = pos;
        }
    }

    static Corredor rivalCerca(Vec3d pos, Corredor excepto, double radio) {
        Corredor mejor = null; double md = radio;
        for (Corredor r : corr.values()) {
            if (r == excepto || r.abandono || r.finTick >= 0) continue;
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(r.id);
            if (p == null) continue;
            double d = Math.hypot(p.getX() - pos.x, p.getZ() - pos.z);
            if (d < md && Math.abs(p.getY() + 0.9 - pos.y) < 2.2) { md = d; mejor = r; }
        }
        return mejor;
    }

    /** Trampa detras del corredor: un Voltorb (explota) o un Grimer (pega); el que la toque recibe el golpe de ese tipo. */
    static void soltarTrampa(ServerWorld w, ServerPlayerEntity p, Corredor c, Vec3d dir, int now, String especie, String tipo, double f) {
        PokemonEntity e = PokemonProperties.Companion.parse("species=" + especie + " level=10").createEntity(w);
        e.setAiDisabled(true); e.setInvulnerable(true); e.setSilent(true); e.setNoGravity(false); e.setPersistent();
        e.getPokemon().setScaleModifier(especie.equals("voltorb") ? 1.3f : 1.1f);
        e.refreshPositionAndAngles(p.getX() - dir.x * 2.0, p.getY(), p.getZ() - dir.z * 2.0, p.getYaw(), 0f);
        e.addCommandTag(TAG);
        w.spawnEntity(e);
        unir(e.getNameForScoreboard());
        Trampa t = new Trampa(); t.ent = e; t.dueno = c; t.nace = now; t.tipo = tipo; t.fuerza = f;
        trampas.add(t);
        Msg.sound(p, SoundEvents.ENTITY_ITEM_PICKUP, 1f, 0.6f);
    }

    static void comprobarTrampas(ServerWorld w, int now) {
        for (Iterator<Trampa> it = trampas.iterator(); it.hasNext(); ) {
            Trampa t = it.next();
            if (t.ent.isRemoved() || now - t.nace > 20 * 90) { t.ent.discard(); it.remove(); continue; }
            Corredor v = rivalCerca(t.ent.getPos(), now - t.nace < 20 ? t.dueno : null, 1.3);
            if (v == null && now - t.nace >= 20) {
                ServerPlayerEntity po = server.getPlayerManager().getPlayer(t.dueno.id);
                if (po != null && !t.dueno.abandono && Math.hypot(po.getX() - t.ent.getX(), po.getZ() - t.ent.getZ()) < 1.3 && t.dueno.finTick < 0) v = t.dueno;
            }
            if (v != null) {
                if (t.tipo.equals("voltorb")) {
                    w.spawnParticles(ParticleTypes.EXPLOSION, t.ent.getX(), t.ent.getY() + 0.7, t.ent.getZ(), 3, 0.3, 0.3, 0.3, 0.0);
                    w.playSound(null, t.ent.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 0.8f, 1.2f);
                } else {
                    w.spawnParticles(ParticleTypes.ITEM_SLIME, t.ent.getX(), t.ent.getY() + 0.7, t.ent.getZ(), 25, 0.4, 0.4, 0.4, 0.1);
                    w.playSound(null, t.ent.getBlockPos(), SoundEvents.BLOCK_SLIME_BLOCK_PLACE, SoundCategory.PLAYERS, 1f, 0.7f);
                }
                golpear(v, t.dueno, t.tipo, w, now, t.fuerza);
                t.ent.discard(); it.remove();
            }
        }
    }

    static void trueno(ServerWorld w, ServerPlayerEntity p, Corredor c, int now, double f) {
        List<Corredor> orden = ordenados();
        int yo = orden.indexOf(c), n = 0;
        for (int i = 0; i < yo; i++) {
            Corredor v = orden.get(i);
            ServerPlayerEntity pv = server.getPlayerManager().getPlayer(v.id);
            if (pv == null || v.finTick >= 0) continue;
            LightningEntity rayo = EntityType.LIGHTNING_BOLT.create(w);
            if (rayo != null) { rayo.refreshPositionAfterTeleport(pv.getX(), pv.getY(), pv.getZ()); rayo.setCosmetic(true); w.spawnEntity(rayo); }
            golpear(v, c, "trueno", w, now, f);
            n++;
        }
        p.sendMessage(Msg.say(n == 0 ? "Vas el primero: el Trueno no ha alcanzado a nadie." : "¡El Trueno ha alcanzado a " + n + " rival(es)!"), false);
    }

    /** Los rivales cercanos (cualquier lado) se duermen un momento. */
    static void bostezo(ServerWorld w, ServerPlayerEntity p, Corredor c, int now, double f) {
        double radio = 14 * f;
        int n = 0;
        w.spawnParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1.2, p.getZ(), 30, 1.0, 0.6, 1.0, 0.05);
        w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_CAT_PURREOW, SoundCategory.PLAYERS, 1.2f, 1.4f);
        for (Corredor v : new ArrayList<>(corr.values())) {
            if (v == c || v.abandono || v.finTick >= 0) continue;
            ServerPlayerEntity pv = server.getPlayerManager().getPlayer(v.id);
            if (pv == null || pv.distanceTo(p) > radio) continue;
            golpear(v, c, "bostezo", w, now, f);
            n++;
        }
        p.sendMessage(Msg.say(n == 0 ? "El bostezo no ha dormido a nadie (no hay rivales cerca)." : "¡El bostezo ha dormido a " + n + " rival(es)!"), false);
    }

    /** Un OVNI abduce al que va primero (si eres tu, no pasa nada); mejorado, tambien al segundo. */
    static void ovni(ServerWorld w, ServerPlayerEntity p, Corredor c, int now, double f, boolean mej) {
        List<Corredor> orden = ordenados();
        int n = 0;
        for (int i = 0; i < Math.min(orden.size(), mej ? 2 : 1); i++) {
            Corredor v = orden.get(i);
            if (v == c || v.abandono || v.finTick >= 0) continue;
            ServerPlayerEntity pv = server.getPlayerManager().getPlayer(v.id);
            if (pv == null) continue;
            w.spawnParticles(ParticleTypes.END_ROD, pv.getX(), pv.getY() + 3, pv.getZ(), 40, 0.4, 1.8, 0.4, 0.02);
            w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pv.getX(), pv.getY() + 1, pv.getZ(), 25, 0.5, 1.2, 0.5, 0.3);
            w.playSound(null, pv.getBlockPos(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.2f, 0.6f);
            golpear(v, c, "ovni", w, now, f);
            n++;
        }
        p.sendMessage(Msg.say(n == 0 ? "Vas el primero: el OVNI no ha encontrado a nadie." : "¡El OVNI ha abducido a " + n + " rival(es)!"), false);
    }

    /** Los rivales que van delante no ven nada unos segundos. */
    static void tinta(ServerWorld w, ServerPlayerEntity p, Corredor c, int now, double f) {
        List<Corredor> orden = ordenados();
        int yo = orden.indexOf(c), n = 0;
        for (int i = 0; i < yo; i++) {
            Corredor v = orden.get(i);
            ServerPlayerEntity pv = server.getPlayerManager().getPlayer(v.id);
            if (pv == null || v.finTick >= 0) continue;
            w.spawnParticles(ParticleTypes.SQUID_INK, pv.getX(), pv.getY() + 1.4, pv.getZ(), 30, 0.4, 0.4, 0.4, 0.05);
            golpear(v, c, "tinta", w, now, f);
            n++;
        }
        p.sendMessage(Msg.say(n == 0 ? "Vas el primero: la tinta no ha alcanzado a nadie." : "¡La tinta ha cegado a " + n + " rival(es)!"), false);
    }

    static int d(int base, double f) { return (int) Math.round(base * f); }

    /** Aplica un ataque a v (respeta el escudo y un breve periodo de inmunidad tras un golpe). `f` = fuerza (1 normal, 1,6 si el poder estaba mejorado). */
    static void golpear(Corredor v, Corredor autor, String tipo, ServerWorld w, int now) { golpear(v, autor, tipo, w, now, 1.0); }

    static void golpear(Corredor v, Corredor autor, String tipo, ServerWorld w, int now, double f) {
        ServerPlayerEntity pv = server.getPlayerManager().getPlayer(v.id);
        if (pv == null || now < v.golpeInmuneHasta) return;
        if (now < v.escudoHasta) {          // la proteccion dura todo su tiempo y para todos los ataques
            Msg.sound(pv, SoundEvents.ITEM_SHIELD_BLOCK, 1f, 1f);
            w.spawnParticles(ParticleTypes.ENCHANTED_HIT, pv.getX(), pv.getY() + 1, pv.getZ(), 20, 0.5, 0.6, 0.5, 0.3);
            if (now >= v.golpeInmuneHasta) { pv.sendMessage(Msg.say("¡Tu protección ha parado el ataque de " + autor.nombre + "!"), false); v.golpeInmuneHasta = now + 10; }
            return;
        }
        switch (tipo) {
            case "pokeball" -> { v.paradoHasta = now + d(50, f); v.lentoHasta = now + d(90, f); v.lentoFactor = 0.4; }
            case "voltorb" -> { v.paradoHasta = now + d(25, f); v.lentoHasta = now + d(70, f); v.lentoFactor = 0.5; }
            case "bostezo" -> { v.paradoHasta = now + d(34, f); v.lentoHasta = now + d(80, f); v.lentoFactor = 0.5; }
            case "ovni" -> { v.paradoHasta = now + d(50, f); v.lentoHasta = now + d(110, f); v.lentoFactor = 0.5; }
            case "mermelada" -> { v.lentoHasta = now + d(110, f); v.lentoFactor = 0.65; }                 // pegajoso: no te para, te frena mucho
            case "tinta" -> {
                v.lentoHasta = now + d(70, f); v.lentoFactor = 0.3;
                pv.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.BLINDNESS, d(60, f), 0, false, false, false));
            }
            default -> { v.paradoHasta = now + d(16, f); v.lentoHasta = now + d(80, f); v.lentoFactor = 0.4; }
        }
        v.turboHasta = 0;
        v.golpeInmuneHasta = now + 30;
        EmiCarreras.LOG.info("{} recibe {} de {}", v.nombre, tipo, autor.nombre);
        String que = switch (tipo) {
            case "pokeball" -> "te ha capturado con una Pokéball";
            case "voltorb" -> "te ha hecho explotar un Voltorb";
            case "bostezo" -> "te ha dormido con un bostezo";
            case "ovni" -> "te ha abducido con un OVNI";
            case "mermelada" -> "te ha pegado con un Grimer";
            case "tinta" -> "te ha cegado con tinta";
            default -> "te ha lanzado un Trueno";
        };
        Msg.title(pv, Text.literal("¡Golpe!").formatted(Formatting.RED, Formatting.BOLD), Text.literal(autor.nombre + " " + que).formatted(Formatting.GRAY), 0, 25, 5);
        Msg.sound(pv, SoundEvents.ENTITY_GENERIC_HURT, 1f, 0.8f);
        w.spawnParticles(ParticleTypes.CRIT, pv.getX(), pv.getY() + 1, pv.getZ(), 15, 0.4, 0.5, 0.4, 0.2);
        v.golpesRecibidos++;
        if (autor != PELIGRO) autor.golpesDados++;
        perderEstrellas(v, pv, "Golpe");
        ServerPlayerEntity pa = server.getPlayerManager().getPlayer(autor.id);
        if (pa != null && !tipo.equals("trueno") && !tipo.equals("ovni") && !tipo.equals("bostezo") && !tipo.equals("tinta")) pa.sendMessage(Msg.say("¡Has alcanzado a " + v.nombre + "!"), false);
    }
}
