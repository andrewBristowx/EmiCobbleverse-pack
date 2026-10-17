package emi.survivors;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import emi.survivors.net.EstadoPayload;
import emi.survivors.net.GemasPayload;
import emi.survivors.net.TextoPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controla las partidas: empezarlas (guardar al jugador, llevarlo a su arena, transformarlo en su Pokemon), llamarlas cada tick, y acabarlas (limpiar la arena,
 * devolver a los jugadores a la sala y contar lo que paso). Cada partida es un {@link Equipo}: una arena (a 1024 bloques de las demas) con de 1 a 4 jugadores, cada
 * uno con su {@link Run}. Puede haber varias partidas a la vez ({@link #MAX_PARTIDAS}).
 */
public final class Partida {
    private Partida() {}

    static final double VELOCIDAD = 0.14;            // atributo de velocidad del jugador (0,1 = andar normal de Minecraft); 0,14 = ~6 bloques/s
    static final double SEPARACION = 1024.0;         // distancia entre las arenas de partidas simultaneas
    static final int MAX_PARTIDAS = Integer.getInteger("emi.survivors.max_partidas", 4);
    /** Enemigos como mucho en todo el servidor a la vez (se reparten entre las partidas en marcha). */
    static final int MAX_ENEMIGOS_TOTAL = Integer.getInteger("emi.survivors.max_enemigos", 360);
    private static final Identifier ID_VEL = Identifier.of("emi_survivors", "velocidad");
    private static final Identifier ID_SALTO = Identifier.of("emi_survivors", "sin_salto");
    private static final Identifier ID_ESCALA = Identifier.of("emi_survivors", "escala");
    /** Altura (bloques) a la que se normaliza el tamano del Pokemon del jugador: asi un Unown diminuto y un Snorlax se ven parecidos de grandes. */
    static final double ALTURA_JUGADOR = 1.7;

    static MinecraftServer server;
    static Datos datos;
    /** Los grupos de la sala (quien se junta con quien para jugar a la vez). */
    static final Grupos grupos = new Grupos();
    /** Las partidas en marcha. */
    static final List<Equipo> equipos = new ArrayList<>();
    /** Cada jugador que esta en una partida, con su {@link Run} (que dice en cual). */
    static final Map<UUID, Run> runs = new LinkedHashMap<>();
    /** Modo prueba: los administradores no pierden vida (para medir el rendimiento sin que la partida acabe) y su partida no cuenta. */
    static boolean inmortal;

    private static final List<UUID> pendientes = new ArrayList<>();      // jugadores a los que hay que devolver sus cosas al entrar
    private static final List<Long> pendientesTick = new ArrayList<>();

    /** Un jugador que va a empezar una partida y el hueco (1..6) de su equipo de Cobblemon con el Pokemon que llevara. */
    record Candidato(ServerPlayerEntity p, int hueco) {}

    /** Para probar cambios de diseño sin reiniciar el servidor (/survivor dev reconstruir): borra la arena de ese hueco y la vuelve a construir. Devuelve el motivo si no se puede, o null. */
    static String reconstruir(Biomas.Bioma bioma, int slot) {
        ServerWorld w = mundo(bioma);
        if (w == null) return "No existe la dimension emi_survivors:" + bioma.dimension() + ".";
        for (Equipo e : equipos) if (e.w == w && e.slot == slot) return "Hay una partida en esa arena: paradla antes (/survivor parar).";
        long t0 = System.nanoTime();
        Arena.reconstruir(w, slot, bioma);
        EmiSurvivors.LOG.info("Arena {} ({}) reconstruida en {} ms (dev)", slot, bioma.id(), (System.nanoTime() - t0) / 1_000_000);
        return null;
    }

    /** El mundo de las arenas del bioma (la pradera por defecto). */
    static ServerWorld mundo(Biomas.Bioma b) { return Mundo.arena(b); }

    static void init(MinecraftServer s) {
        server = s;
        runs.clear();
        equipos.clear();
        grupos.vaciar();
        datos = new Datos(s.getSavePath(WorldSavePath.ROOT).resolve("emi_survivors"));
        if (mundo(Biomas.PRADERA) == null) { EmiSurvivors.LOG.warn("No existe la dimension emi_survivors:arena: el minijuego no funcionara."); return; }
        // restos de una partida anterior que no pudo limpiarse (servidor parado a la fuerza): enemigos con nuestra etiqueta (en la arena de cada bioma)
        for (Biomas.Bioma b : Biomas.TODOS) {
            ServerWorld w = mundo(b);
            if (w == null) { EmiSurvivors.LOG.warn("No existe la dimension emi_survivors:{}: ese bioma no estara disponible.", b.dimension()); continue; }
            List<Entity> borrar = new ArrayList<>();
            for (Entity e : w.iterateEntities()) if (e.getCommandTags().contains("emi_survivors")) borrar.add(e);
            for (Entity e : borrar) e.discard();
            if (!borrar.isEmpty()) EmiSurvivors.LOG.info("Se han quitado {} enemigos que habian quedado de una partida anterior ({}).", borrar.size(), b.id());
        }
        Colores.limpiarTodos(s);                                  // los equipos de colores del marcador (contorno de los enemigos con personalidad) que pudieran haber quedado
        try { ServerWorld sala = Mundo.sala(); if (sala != null) Mundo.construirSala(sala); else EmiSurvivors.LOG.warn("No existe la dimension emi_survivors:entrada."); }
        catch (Throwable t) { EmiSurvivors.LOG.error("No se pudo construir la sala de entrada", t); }
    }

    static void shutdown() {
        for (Equipo eq : new ArrayList<>(equipos)) terminarEquipo(eq, "El servidor se apaga.");
        try { Pausas.reanudar(); } catch (Throwable t) { /* nada */ }
    }

    public static boolean participa(PlayerEntity p) { return runs.containsKey(p.getUuid()); }

    static Run de(UUID id) { return runs.get(id); }

    /** Cuantos enemigos se permiten segun lo cargado que va el servidor: si los ticks tardan mucho, se crean menos (proteccion del servidor). */
    static double factorRendimiento() {
        if (server == null) return 1.0;
        float ms = server.getAverageTickTime();
        return ms > 46 ? 0.3 : ms > 42 ? 0.5 : ms > 38 ? 0.75 : 1.0;
    }

    /** Cuantos enemigos puede tener como mucho cada partida: el total del servidor repartido entre las que hay (nunca menos de 40). */
    static int topePorEquipo() { return Math.max(40, MAX_ENEMIGOS_TOTAL / Math.max(1, equipos.size())); }

    static void tick(MinecraftServer s) {
        for (Equipo eq : new ArrayList<>(equipos)) {
            if (!equipos.contains(eq)) continue;
            try { eq.tick(); }
            catch (Throwable t) { EmiSurvivors.LOG.error("Error en la partida de {}; se cancela", eq.nombres(), t); terminarEquipo(eq, "Error interno: se cancela la partida."); }
        }
        // devolver sus cosas a quien volvio a entrar despues de que el servidor se parara en plena partida
        for (int i = pendientes.size() - 1; i >= 0; i--) {
            if (s.getTicks() < pendientesTick.get(i)) continue;
            UUID id = pendientes.remove(i); pendientesTick.remove(i);
            ServerPlayerEntity p = s.getPlayerManager().getPlayer(id);
            if (p == null || participa(p) || !Saved.exists(s, id)) continue;
            if (Saved.enLobby(s, id)) { Mundo.alEntrar(p); continue; }           // sigue en el mundo del minijuego: si se quedo en una arena, a la sala
            Morph.restaurar(s, p, "");
            Saved.restore(s, p);
            p.sendMessage(Msg.say("Te hemos devuelto tus cosas de la partida."), false);
        }
    }

    // ---------------------------------------------------------------- empezar

    private static int huecoLibre() {
        for (int slot = 0; slot < MAX_PARTIDAS; slot++) {
            boolean usado = false;
            for (Equipo eq : equipos) if (eq.slot == slot) usado = true;
            if (!usado) return slot;
        }
        return -1;
    }

    /**
     * Empieza una partida EN SOLITARIO para {@code p} con el Pokemon del hueco {@code hueco} (1..6) de su equipo. {@code fijo > 0} = modo de medida de la fase 0 (ese numero
     * de enemigos a la vez, sin oleadas ni jefe). {@code modoPrueba}: el jugador viene de fuera (comando de administrador): se le guarda y se le devuelve todo al acabar.
     * Devuelve el motivo si no se puede, o null si empezo.
     */
    public static String iniciar(ServerPlayerEntity p, int hueco, int fijo, boolean modoPrueba) { return iniciar(p, hueco, fijo, modoPrueba, Biomas.PRADERA); }

    /** Igual, en la arena del bioma {@code bioma}. */
    public static String iniciar(ServerPlayerEntity p, int hueco, int fijo, boolean modoPrueba, Biomas.Bioma bioma) {
        return iniciarEquipo(List.of(new Candidato(p, hueco)), fijo, modoPrueba, null, bioma);
    }

    /** Empieza una partida para uno o varios jugadores (un grupo de la sala) en la misma arena. Devuelve el motivo si no se puede, o null si empezo. */
    static String iniciarEquipo(List<Candidato> cs, int fijo, boolean modoPrueba) { return iniciarEquipo(cs, fijo, modoPrueba, null, Biomas.PRADERA); }

    /** Igual, para el grupo {@code grupo} de la sala (null = jugadores sueltos) en la arena de {@code bioma}: al acabar la partida el grupo sigue junto. */
    static String iniciarEquipo(List<Candidato> cs, int fijo, boolean modoPrueba, Grupos.Grupo grupo, Biomas.Bioma bioma) {
        if (server == null) return "El minijuego aun no esta listo.";
        if (cs.isEmpty()) return "No hay jugadores.";
        if (equipos.size() >= MAX_PARTIDAS) return "Hay " + equipos.size() + " partidas en marcha (el máximo es " + MAX_PARTIDAS + "). Espera a que acabe alguna.";
        if (bioma == null) bioma = Biomas.PRADERA;
        ServerWorld w = mundo(bioma);
        if (w == null) return "No existe la dimension emi_survivors:" + bioma.dimension() + " (¿esta instalado el mod en el servidor?).";
        if (!Morph.disponible()) return "Falta el mod CobbleMorph (el jugador va transformado en su Pokémon).";
        List<Pokemon> pks = new ArrayList<>();
        for (Candidato c : cs) {
            ServerPlayerEntity p = c.p();
            String n = p.getGameProfile().getName();
            if (runs.containsKey(p.getUuid())) return n + " ya está en una partida." + (cs.size() == 1 ? " Usa /survivor parar para cancelarla." : "");
            if (modoPrueba && Saved.exists(server, p.getUuid())) return "Tienes cosas guardadas de una partida anterior: sal y vuelve a entrar para recuperarlas.";
            if (!modoPrueba && !Mundo.enSala(p)) return n + " no está en la sala del minijuego (usa /survivor entrar).";
            if (!ServerPlayNetworking.canSend(p, EstadoPayload.ID)) return n + " no tiene el mod Emi Survivors: hay que actualizar el pack y volver a entrar.";
            PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(p);
            Pokemon pk = party.get(c.hueco() - 1);
            StringBuilder eq = new StringBuilder();
            for (int i = 0; i < 6; i++) { Pokemon x = party.get(i); if (x != null) eq.append(i + 1).append("=").append(x.getSpecies().getName()).append(" Nv.").append(x.getLevel()).append("  "); }
            EmiSurvivors.LOG.info("Equipo de {}: {}", n, eq);
            if (pk == null) return (cs.size() == 1 ? "No tienes" : n + " no tiene") + " ningún Pokémon en el hueco " + c.hueco() + " del equipo.";
            pks.add(pk);
        }
        int slot = huecoLibre();
        if (slot < 0) return "No hay arenas libres.";
        if (modoPrueba) for (Candidato c : cs) { try { Saved.save(server, c.p(), false); } catch (RuntimeException e) { return "No se pudo guardar tu inventario: no entras."; } }
        double[] orig = Arena.origen(slot);
        double ox = orig[0], oz = orig[1];
        Equipo eq = new Equipo(w, slot, ox, oz, fijo, modoPrueba, bioma);
        eq.grupo = grupo;
        List<Candidato> tocados = new ArrayList<>();
        String[] antes = new String[cs.size()];
        try {
            if (equipos.isEmpty()) Pausas.pausar();
            Arena.preparar(w, slot, bioma);
            Arena.limpiarRestos(w, slot);
            int n = cs.size();
            for (int i = 0; i < n; i++) {
                Candidato c = cs.get(i);
                ServerPlayerEntity p = c.p();
                Pokemon pk = pks.get(i);
                antes[i] = modoPrueba ? Morph.actual(p.getUuid()) : "";
                tocados.add(c);
                double ang = i * Math.PI * 2 / n, dist = n == 1 ? 0 : 2.5;
                p.teleport(w, ox + 0.5 + Math.cos(ang) * dist, Arena.Y0, oz + 0.5 + Math.sin(ang) * dist, 180f, 0f);
                prepararJugador(p);
                Morph.transformar(server, p, pk);
                fijarEscala(p, pk);
                Map<String, Integer> meta = new java.util.HashMap<>(datos.poke(datos.jugador(p.getUuid(), p.getGameProfile().getName()), pk.getUuid().toString(), pk.getSpecies().getResourceIdentifier().getPath()).mejoras);
                Run run = new Run(eq, p, pk, antes[i], inmortal && Perm.admin(p.getCommandSource()), meta);   // el modo inmortal es solo de administradores
                eq.miembros.add(run);
                eq.jugadoresAlEmpezar = eq.miembros.size();
                runs.put(p.getUuid(), run);
            }
            equipos.add(eq);
            if (grupo != null) grupos.alEmpezar(grupo);
        } catch (Throwable t) {
            // si algo falla a medias, nadie se queda a medio camino: se le devuelve todo a todos
            EmiSurvivors.LOG.error("No se pudo empezar la partida", t);
            for (int i = 0; i < tocados.size(); i++) {
                ServerPlayerEntity p = tocados.get(i).p();
                runs.remove(p.getUuid());
                try {
                    if (modoPrueba) { Morph.restaurar(server, p, antes[i]); Saved.restore(server, p); }
                    else Mundo.volverASala(p, null);
                } catch (Throwable t2) { EmiSurvivors.LOG.error("Ademas, no se pudo devolver al jugador a su sitio", t2); }
            }
            eq.limpiar();
            if (equipos.isEmpty()) { try { Pausas.reanudar(); } catch (Throwable t2) { /* nada */ } }
            return "No se pudo empezar la partida (mira el registro del servidor); te hemos devuelto a tu sitio.";
        }
        for (Run run : eq.miembros) {
            EmiSurvivors.LOG.info("Partida de {}: Pokemon {} (tipo {}, ataque inicial {}), hueco de arena {}, equipo de {}, fijo={}, inmortal={}", run.nombre, run.pk.getSpecies().getName(), run.tipo, run.inicial.nombre(), slot,
                    eq.miembros.size(), fijo, run.inmortal);
            ServerPlayerEntity p = run.jugador();
            Sonidos.sfx(p, Sonidos.EMPIEZA, 1.0f, 1.0f);
            Musica.silencio(p, 1500);                                     // se oye la fanfarria de «¡empieza!» y despues entra la musica del bioma
            if (p != null) p.sendMessage(Msg.say("¡Empieza! Muévete con WASD; tu " + run.pk.getSpecies().getName() + " ataca solo con " + run.inicial.nombre() + ". Para salir: /survivor parar"
                    + (eq.miembros.size() > 1 ? ". Si un compañero cae, quédate a su lado unos segundos para revivirlo." : "")), false);
        }
        return null;
    }

    /** Saca de su partida a {@code p} (la cancela si estaba solo; si hay mas jugadores, los demas siguen). */
    public static String parar(ServerPlayerEntity p) {
        Run r = runs.get(p.getUuid());
        if (r == null) return "No estás en ninguna partida.";
        quitar(r, "Partida cancelada.");
        return null;
    }

    /** Cancela todas las partidas (organizador). Devuelve cuantas habia. */
    public static int pararTodas() {
        int n = equipos.size();
        for (Equipo eq : new ArrayList<>(equipos)) terminarEquipo(eq, "Partida cancelada por un administrador.");
        return n;
    }

    public static String estado() {
        if (equipos.isEmpty()) return "No hay ninguna partida en marcha." + (inmortal ? " (modo prueba inmortal activado)" : "");
        StringBuilder sb = new StringBuilder();
        for (Equipo eq : equipos) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("Partida de ").append(eq.nombres()).append(" (arena ").append(eq.slot).append(", ").append(eq.bioma.id()).append("): ").append(eq.segundos()).append(" s, ").append(eq.enemigos.size()).append(" enemigos, ")
                    .append(eq.derrotados).append(" derrotados");
            for (Run r : eq.miembros)
                sb.append(" | ").append(r.nombre).append(" nivel ").append(r.nivel).append(", vida ").append(Math.max(0, Math.round(r.hp))).append("/").append(Math.round(r.st.hpMax)).append(r.estado == Run.CAIDO ? " (caído)" : r.esperandoCarta ? " (eligiendo carta)" : "");
            if (eq.jefe != null && eq.jefe.vivo()) sb.append(" | jefe ").append(eq.jefe.nombre).append(" ").append(Math.round(eq.jefe.en.hp)).append("/").append(Math.round(eq.jefe.en.hpMax)).append(eq.jefe.furia ? " (furia)" : "");
            if (eq.marea != null) sb.append(" | marea ").append(eq.marea.titulo()).append(" (").append(eq.marea.segundosRestantes()).append(" s)");
            sb.append(" | jefes derrotados ").append(eq.jefesDerrotados);
            sb.append(" | servidor ").append(Run.fmt(eq.med.msptServidor())).append(" ms/tick, nuestro ").append(Run.fmt(eq.med.msNuestro())).append(" ms/tick");
            if (!eq.objetos.isEmpty()) {
                sb.append(" | objetos:");
                for (Objeto o : eq.objetos) sb.append(' ').append(Objeto.nombre(o.tipo)).append('@').append(Math.round(o.x - eq.miembros.get(0).pp.x)).append(',').append(Math.round(o.z - eq.miembros.get(0).pp.z));
            }
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- fin de la partida

    /** Un jugador deja la partida (se acaba para el: cancelar, salir, desconectarse...). Si era el ultimo, se acaba la partida entera. */
    static void quitar(Run r, String motivo) {
        if (r == null) return;
        Equipo eq = r.eq;
        if (runs.get(r.id) != r || !eq.miembros.contains(r)) return;
        if (eq.miembros.size() == 1) { terminarEquipo(eq, motivo); return; }
        eq.miembros.remove(r);
        runs.remove(r.id);
        r.proyectiles.clear();
        for (Gema g : eq.gemas) if (g.dueno == r) { g.dueno = null; g.atraidaT = 0; }
        boolean cuenta = cuentaPartida(eq);
        cerrarJugador(r, eq, motivo, eq.segundos(), cuenta, 0);
        if (eq.grupo != null) GrupoCtl.salir(r.id, r.nombre, false);                 // quien deja la partida a medias deja tambien el grupo
        eq.avisar(Msg.say(r.nombre + " ha dejado la partida."));
        if (eq.vivos().isEmpty()) terminarEquipo(eq, "No queda nadie en pie.");
    }

    /** Ya no se usa: se mantiene el nombre antiguo (un jugador deja la partida). */
    static void terminar(Run r, String motivo) { quitar(r, motivo); }

    /** ¿Cuenta esta partida para el top y las monedas? No cuentan las pruebas de administrador, las medidas ni las de modo inmortal. */
    private static boolean cuentaPartida(Equipo eq) {
        if (eq.fijo > 0 || eq.modoPrueba) return false;
        for (Run m : eq.miembros) if (m.inmortal) return false;
        return true;
    }

    /** Acaba la partida de un equipo: apunta el top, da las monedas y devuelve a cada jugador a la sala (o a su sitio, si era una prueba de administrador). */
    static void terminarEquipo(Equipo eq, String motivo) {
        if (eq == null || eq.terminado) return;
        eq.terminado = true;
        equipos.remove(eq);
        List<Run> ms = new ArrayList<>(eq.miembros);
        int seg = eq.segundos();
        boolean cuenta = cuentaPartida(eq);
        eq.limpiar();
        EmiSurvivors.LOG.info("Fin de la partida de {} ({}): {} | {} s, {} derrotados, {} especies distintas ({} % de los normales eran de tipos del bioma), {} fallos al crear{}", eq.nombres(), eq.bioma.id(), motivo, seg, eq.derrotados, eq.especiesVistas.size(),
                eq.normalesTotal == 0 ? 0 : Math.round(100.0 * eq.normalesDelBioma / eq.normalesTotal), eq.fallosCrear,
                eq.fijo > 0 ? " | medias con " + eq.fijo + " enemigos: servidor " + Run.fmt(eq.msptEstable()) + " ms/tick, nuestro " + Run.fmt(eq.nuestroEstable()) + " ms/tick (max servidor " + Run.fmt(eq.med.msptMax()) + " ms)" : "");
        // el top es del equipo: lo que ha aguantado entre todos
        int puesto = 0;
        if (cuenta && !ms.isEmpty()) {
            try { puesto = datos.registrarEquipo(entradaDelEquipo(eq, ms, seg)); }
            catch (RuntimeException ex) { EmiSurvivors.LOG.error("No se pudo guardar el resultado de la partida de {}", eq.nombres(), ex); }
        }
        for (Run r : ms) { runs.remove(r.id); cerrarJugador(r, eq, motivo, seg, cuenta, puesto); }
        eq.miembros.clear();
        if (eq.grupo != null) grupos.alTerminar(eq.grupo);                        // el grupo sigue junto en la sala, pero nadie esta listo
        if (equipos.isEmpty()) { try { Pausas.reanudar(); } catch (Throwable t) { /* nada */ } }
    }

    private static Datos.Entrada entradaDelEquipo(Equipo eq, List<Run> ms, int seg) {
        Datos.Entrada e = new Datos.Entrada();
        e.segundos = seg; e.derrotados = eq.derrotados; e.jefe = eq.jefesDerrotados > 0; e.jefes = eq.jefesDerrotados;
        List<String> ids = new ArrayList<>(), nombres = new ArrayList<>();
        int nivelMax = 1;
        for (Run r : ms) {
            Datos.MiembroTop m = new Datos.MiembroTop();
            m.uuid = r.id.toString(); m.nombre = r.nombre; m.especie = r.especieId; m.aspectos = String.join(" ", r.pk.getAspects()); m.nivelPokemon = r.pk.getLevel();
            m.derrotados = r.derrotados; m.nivel = r.nivel;
            e.miembros.add(m);
            ids.add(m.uuid); nombres.add(m.nombre);
            nivelMax = Math.max(nivelMax, r.nivel);
        }
        e.nivel = nivelMax;
        e.modo = ms.size() == 1 ? Datos.SOLO : Datos.GRUPO;
        java.util.Collections.sort(ids);
        e.uuid = String.join("+", ids);
        e.nombre = String.join(" + ", nombres);
        e.especie = e.miembros.get(0).especie; e.aspectos = e.miembros.get(0).aspectos; e.nivelPokemon = e.miembros.get(0).nivelPokemon;
        return e;
    }

    /** Lo que se le hace a un jugador al acabar su partida: monedas, devolverlo a la sala (o a su sitio) y contarle como le fue. */
    private static void cerrarJugador(Run r, Equipo eq, String motivo, int seg, boolean cuenta, int puesto) {
        ServerPlayerEntity p = r.jugador();
        int monedas = 0, retosCoronas = 0;
        List<String> retosLineas = new ArrayList<>();
        if (cuenta) {
            try {
                monedas = Curvas.monedas(seg, r.derrotados, r.nivel, eq.jefesDerrotados, r.shinies, r.st.monedas);
                datos.registrarJugador(r.id, r.nombre, r.pokeId, r.especieId, seg, monedas);
                int evos = 0;
                for (int nivelArma : r.armas.values()) if (nivelArma >= Catalogo.NIVEL_EVO) evos++;
                Retos.Novedades nv = datos.retos(r.id, r.nombre, new Retos.Partida(seg, r.derrotados, r.nivel, eq.jefesDerrotados, eq.bioma.id(), eq.jugadoresAlEmpezar, evos, r.especieId, r.shinies), System.currentTimeMillis(), java.time.ZoneId.systemDefault());
                retosLineas = nv.lineas; retosCoronas = nv.coronas;
            } catch (RuntimeException ex) { EmiSurvivors.LOG.error("No se pudo guardar el resultado de la partida de {}", r.nombre, ex); }
        }
        EmiSurvivors.LOG.info("  {}: nivel {}, {} derrotados, {} monedas{}", r.nombre, r.nivel, r.derrotados, monedas, r.estado == Run.CAIDO ? " (acabó caído)" : "");
        if (p == null) return;
        try {
            if (ServerPlayNetworking.canSend(p, EstadoPayload.ID)) ServerPlayNetworking.send(p, EstadoPayload.apagado());
            if (ServerPlayNetworking.canSend(p, GemasPayload.ID)) ServerPlayNetworking.send(p, GemasPayload.vacio());
            if (ServerPlayNetworking.canSend(p, TextoPayload.ID)) ServerPlayNetworking.send(p, new TextoPayload("cartas", ""));
        } catch (Throwable t) { /* el cliente no tiene el mod */ }
        StringBuilder otros = new StringBuilder();
        for (Run o : eq.miembros) if (o != r) { if (otros.length() > 0) otros.append(", "); otros.append(o.nombre); }
        if (eq.modoPrueba) {
            Morph.restaurar(server, p, r.morphAntes);
            if (!Saved.restore(server, p)) quitarEfectos(p);
        } else {
            quitarEfectos(p);
            Mundo.volverASala(p, new Menu.Resultado(motivo, seg, r.derrotados, r.nivel, monedas, puesto, eq.jefesDerrotados, cuenta, otros.toString(), retosLineas, retosCoronas));
        }
        for (String l : retosLineas) p.sendMessage(Msg.say(l), false);
        if (cuenta && r.shinies > 0) p.sendMessage(Msg.say("✦ Shinies derrotados: " + r.shinies + " (+" + Curvas.MONEDAS_POR_SHINY * r.shinies + " michiestrellitas, antes de la suerte)."), false);
        p.sendMessage(Msg.say(motivo + " Aguantasteis " + (seg / 60) + ":" + String.format("%02d", seg % 60) + ", llegaste al nivel " + r.nivel + " y derrotaste a " + r.derrotados + " Pokémon."
                + (cuenta ? " Ganas " + monedas + " michiestrellitas." + (puesto > 0 ? " Puesto " + puesto + " del top." : "") : "")), false);
        if (eq.fijo > 0 && eq.hayMedidaEstable())
            p.sendMessage(Text.literal("Rendimiento de esta prueba (con " + eq.fijo + " enemigos): el servidor tardó de media " + Run.fmt(eq.msptEstable())
                    + " ms por tick (50 es el límite); este mod gastó " + Run.fmt(eq.nuestroEstable()) + " ms.").formatted(Formatting.GRAY), false);
    }

    // ---------------------------------------------------------------- jugador

    static void alEntrar(ServerPlayerEntity p) {
        if (server == null) return;
        if (Saved.exists(server, p.getUuid()) && !participa(p)) { pendientes.add(p.getUuid()); pendientesTick.add(server.getTicks() + 40L); }
    }

    static void alSalir(ServerPlayerEntity p) {
        Run r = runs.get(p.getUuid());
        if (r != null) quitar(r, "Te desconectaste.");
        GrupoCtl.salir(p.getUuid(), p.getGameProfile().getName(), false);                  // quien se desconecta deja el grupo
    }

    /** Modo partida: aventura, inmortal para Minecraft (la vida es la del minijuego), sin inventario y sin salto. La velocidad la pone la partida ({@link #fijarVelocidad}). */
    private static void prepararJugador(ServerPlayerEntity p) {
        p.clearStatusEffects();
        p.getInventory().clear();
        p.changeGameMode(GameMode.ADVENTURE);
        p.setInvulnerable(true);
        p.setGlowing(true);                  // contorno brillante: se ve a traves de la horda
        p.getAbilities().flying = false;
        p.sendAbilitiesUpdate();
        p.getHungerManager().setFoodLevel(20); p.getHungerManager().setSaturationLevel(5f);
        atributo(p, EntityAttributes.GENERIC_JUMP_STRENGTH, ID_SALTO, -1.0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, true);
    }

    /** Velocidad al andar: ~6 bloques/s por el multiplicador {@code mult} de las mejoras del jugador (0 = inmovil). */
    static void fijarVelocidad(ServerPlayerEntity p, double mult) {
        atributo(p, EntityAttributes.GENERIC_MOVEMENT_SPEED, ID_VEL, VELOCIDAD * mult - 0.1, EntityAttributeModifier.Operation.ADD_VALUE, true);
    }

    /** Tamano del jugador (el modelo del Pokemon transformado lo respeta): hace que mida ~1,7 bloques sea cual sea el Pokemon. */
    static void fijarEscala(ServerPlayerEntity p, Pokemon pk) {
        double h = 1.0;
        try {
            var sp = pk.getSpecies();
            h = Math.max(sp.getHitbox().height(), sp.getHitbox().width() * 0.8f) * sp.getBaseScale() * pk.getScaleModifier();
        } catch (Throwable t) { /* se queda en 1 */ }
        double escala = Math.max(0.7, Math.min(3.0, ALTURA_JUGADOR / Math.max(0.1, h)));
        EntityAttributeInstance inst = p.getAttributeInstance(EntityAttributes.GENERIC_SCALE);
        if (inst == null) return;
        inst.removeModifier(ID_ESCALA);
        inst.addTemporaryModifier(new EntityAttributeModifier(ID_ESCALA, escala / inst.getBaseValue() - 1.0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    static void quitarEfectos(ServerPlayerEntity p) {
        EntityAttributeInstance esc = p.getAttributeInstance(EntityAttributes.GENERIC_SCALE);
        if (esc != null) esc.removeModifier(ID_ESCALA);
        p.setInvulnerable(false);
        p.setGlowing(false);
        atributo(p, EntityAttributes.GENERIC_MOVEMENT_SPEED, ID_VEL, 0, EntityAttributeModifier.Operation.ADD_VALUE, false);
        atributo(p, EntityAttributes.GENERIC_JUMP_STRENGTH, ID_SALTO, 0, EntityAttributeModifier.Operation.ADD_VALUE, false);
    }

    private static void atributo(ServerPlayerEntity p, RegistryEntry<EntityAttribute> a, Identifier id, double v, EntityAttributeModifier.Operation op, boolean poner) {
        EntityAttributeInstance inst = p.getAttributeInstance(a);
        if (inst == null) return;
        inst.removeModifier(id);
        if (poner) inst.addTemporaryModifier(new EntityAttributeModifier(id, v, op));
    }

    // ---------------------------------------------------------------- ayudas de prueba (administrador)

    /** Para probar: da michiestrellitas al administrador (las que se ganan jugando se apuntan al acabar una partida). */
    public static String devMonedas(ServerPlayerEntity p, int n) {
        if (datos == null) return "El minijuego aun no esta listo.";
        Datos.Jugador j = datos.jugador(p.getUuid(), p.getGameProfile().getName());
        j.monedas = Math.max(0, j.monedas + n);
        datos.guardar(p.getUuid());
        return null;
    }

    public static String devCoronas(ServerPlayerEntity p, int n) {
        if (datos == null) return "El minijuego aun no esta listo.";
        Datos.Jugador j = datos.jugador(p.getUuid(), p.getGameProfile().getName());
        j.coronas = Math.max(0, j.coronas + n);
        datos.guardar(p.getUuid());
        return null;
    }

    /** Para probar: da experiencia, niveles, ataques... a la partida del administrador. Devuelve el motivo si no se puede, o null. */
    public static String dev(ServerPlayerEntity p, String que, String id, int n) {
        Run r = runs.get(p.getUuid());
        if (r == null) return "Primero empieza una partida con /survivor prueba.";
        switch (que) {
            case "xp" -> r.ganarXp(Math.max(1, n));
            case "nivel" -> { r.nivel += Math.max(1, n); r.pendientes += Math.max(1, n); r.xpSig = Curvas.xpPara(r.nivel); r.xp = 0; }
            case "tiempo" -> r.eq.tick = Math.max(0, n) * 20;
            case "jefe" -> { if (r.eq.jefe != null && r.eq.jefe.vivo()) return "Ya hay un jefe vivo."; if (!r.eq.crearJefe(n > 0 ? n : r.eq.jefesSalidos + 1)) return "No se pudo crear el jefe (mira el registro)."; }
            case "jefehp" -> {                    // deja al jefe vivo con ese porcentaje de vida (0 = lo mata): para probar la furia y los premios
                if (r.eq.jefe == null || !r.eq.jefe.vivo()) return "No hay ningun jefe vivo.";
                r.eq.jefe.en.hp = r.eq.jefe.en.hpMax * Math.max(0, Math.min(100, n)) / 100f;
            }
            case "marea" -> {                     // empieza ya una marea de ese tipo (michis, cerco, tipo, estampida)
                Mareas.Tipo tp = Mareas.porNombre(id);
                if (tp == null) return "Mareas: michis, cerco, tipo, estampida.";
                r.eq.iniciarMarea(r.eq.mareasSalidas + 1, tp);
            }
            case "pers" -> {                      // crea un enemigo con esa personalidad cerca
                Pers pp = Pers.porNombre(id);
                if (pp == null) return "Personalidades: veloz, bruto, embiste, explosivo.";
                if (!r.eq.devPers(pp, r)) return "No se pudo crear el enemigo.";
            }
            case "clima" -> {
                Clima cl = null;
                for (Clima x : Clima.values()) if (x.name().equalsIgnoreCase(id)) cl = x;
                if (cl == null) return "Climas: despejado, sol, lluvia, tormenta, nieve, niebla.";
                r.eq.clima.forzar(cl);
            }
            case "shiny" -> { return r.eq.devShiny(r) ? null : "No se pudo crear el shiny."; }
            case "elite" -> { return r.eq.devElite() ? null : "No se pudo crear el campeón."; }
            case "curar" -> r.hp = r.st.hpMax;
            case "dano" -> r.hp -= Math.max(1, n);
            case "sinxp" -> { r.sinXp = !r.sinXp; return null; }
            case "arma" -> {
                if (Catalogo.arma(id) == null) return "Ataque desconocido: " + id;
                int nv = Math.max(1, Math.min(Catalogo.NIVEL_EVO, n));
                if (r.armaObj.containsKey(id)) { r.armas.put(id, nv); r.armaObj.get(id).nivel = nv; } else r.agregarArma(id, nv);
            }
            case "solo" -> {
                if (Catalogo.arma(id) == null) return "Ataque desconocido: " + id;
                r.armas.clear(); r.armaObj.clear();
                r.agregarArma(id, Math.max(1, Math.min(Catalogo.NIVEL_EVO, n)));
            }
            case "dps" -> {                       // mide el dano que hacen los ataques desde la ultima vez (y vuelve a empezar): sale en el registro del servidor
                double seg = (r.tick - r.dpsTick0) / 20.0;
                if (r.dpsTick0 >= 0 && seg > 0.5) EmiSurvivors.LOG.info("[dps] ataques={} dano={} en {} s = {} por segundo, derrotados={}, enemigos={}", r.armas, Math.round(r.danoHecho), Run.fmt((float) seg), Run.fmt((float) (r.danoHecho / seg)), r.derrotados - r.dpsDerrotados0, r.enemigos.size());
                r.danoHecho = 0; r.dpsTick0 = r.tick; r.dpsDerrotados0 = r.derrotados;
            }
            case "usar" -> {                      // como si hubieras pisado ese objeto, sin tener que ir a por el
                int t = Objeto.porNombre(id);
                if (t < 0) return "Objetos: baya, iman, bomba, caramelo.";
                r.recoger(t);
            }
            case "objeto" -> {
                int t = Objeto.porNombre(id);
                if (t < 0) return "Objetos: baya, iman, bomba, caramelo.";
                r.eq.soltarObjeto(t, r.pp.x + 2.5 + 2.0 * r.eq.objetos.size(), r.pp.z - 1.5);       // cada uno un poco mas lejos del anterior, para verlos todos
            }
            case "pasiva" -> {
                if (Catalogo.pasiva(id) == null) return "Mejora desconocida: " + id;
                r.pasivas.put(id, Math.max(1, Math.min(Catalogo.MAX_NIVEL, n)));
                r.recalcular();
            }
            default -> { return "Opciones: xp, nivel, tiempo, jefe [n], jefehp <%>, marea <tipo>, pers <nombre>, elite, curar, dano, sinxp, dps, arma, solo, pasiva, objeto, usar."; }
        }
        return null;
    }
}
