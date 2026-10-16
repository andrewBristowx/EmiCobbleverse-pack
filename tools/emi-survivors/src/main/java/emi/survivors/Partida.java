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
 * devolver al jugador a su sitio y contar lo que paso). Puede haber varias partidas a la vez, cada una de UN jugador y en su propia arena (a 1024 bloques de las
 * demas); el multijugador de verdad (grupos de hasta 4 en la misma arena) llega en la fase 2.
 */
public final class Partida {
    private Partida() {}

    static final double VELOCIDAD = 0.14;            // atributo de velocidad del jugador (0,1 = andar normal de Minecraft); 0,14 = ~6 bloques/s
    static final double SEPARACION = 1024.0;         // distancia entre las arenas de partidas simultaneas
    static final int MAX_PARTIDAS = Integer.getInteger("emi.survivors.max_partidas", 4);
    private static final Identifier ID_VEL = Identifier.of("emi_survivors", "velocidad");
    private static final Identifier ID_SALTO = Identifier.of("emi_survivors", "sin_salto");
    private static final Identifier ID_ESCALA = Identifier.of("emi_survivors", "escala");
    /** Altura (bloques) a la que se normaliza el tamano del Pokemon del jugador: asi un Unown diminuto y un Snorlax se ven parecidos de grandes. */
    static final double ALTURA_JUGADOR = 1.7;

    static MinecraftServer server;
    static Datos datos;
    static final Map<UUID, Run> runs = new LinkedHashMap<>();
    /** Modo prueba: el jugador no pierde vida (para medir el rendimiento sin que la partida acabe). */
    static boolean inmortal;

    private static final List<UUID> pendientes = new ArrayList<>();      // jugadores a los que hay que devolver sus cosas al entrar
    private static final List<Long> pendientesTick = new ArrayList<>();

    static ServerWorld mundo() {
        return server == null ? null : server.getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of("emi_survivors", "arena")));
    }

    static void init(MinecraftServer s) {
        server = s;
        runs.clear();
        datos = new Datos(s.getSavePath(WorldSavePath.ROOT).resolve("emi_survivors"));
        ServerWorld w = mundo();
        if (w == null) { EmiSurvivors.LOG.warn("No existe la dimension emi_survivors:arena: el minijuego no funcionara."); return; }
        // restos de una partida anterior que no pudo limpiarse (servidor parado a la fuerza): enemigos con nuestra etiqueta
        List<Entity> borrar = new ArrayList<>();
        for (Entity e : w.iterateEntities()) if (e.getCommandTags().contains("emi_survivors")) borrar.add(e);
        for (Entity e : borrar) e.discard();
        if (!borrar.isEmpty()) EmiSurvivors.LOG.info("Se han quitado {} enemigos que habian quedado de una partida anterior.", borrar.size());
        try { ServerWorld sala = Mundo.sala(); if (sala != null) Mundo.construirSala(sala); else EmiSurvivors.LOG.warn("No existe la dimension emi_survivors:entrada."); }
        catch (Throwable t) { EmiSurvivors.LOG.error("No se pudo construir la sala de entrada", t); }
    }

    static void shutdown() {
        for (Run r : new ArrayList<>(runs.values())) terminar(r, "El servidor se apaga.");
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

    static void tick(MinecraftServer s) {
        for (Run r : new ArrayList<>(runs.values())) {
            if (runs.get(r.id) != r) continue;
            try { r.tick(); }
            catch (Throwable t) { EmiSurvivors.LOG.error("Error en la partida de {}; se cancela", r.nombre, t); terminar(r, "Error interno: se cancela la partida."); }
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

    // ---------------------------------------------------------------- comandos

    private static int huecoLibre() {
        for (int slot = 0; slot < MAX_PARTIDAS; slot++) {
            boolean usado = false;
            for (Run r : runs.values()) if (r.slot == slot) usado = true;
            if (!usado) return slot;
        }
        return -1;
    }

    /**
     * Empieza una partida para {@code p} con el Pokemon del hueco {@code hueco} (1..6) de su equipo. {@code fijo > 0} = modo de medida de la fase 0 (ese numero de
     * enemigos a la vez, sin oleadas ni jefe). {@code modoPrueba}: el jugador viene de fuera (comando de administrador): se le guarda y se le devuelve todo al acabar.
     * Devuelve el motivo si no se puede, o null si empezo.
     */
    public static String iniciar(ServerPlayerEntity p, int hueco, int fijo, boolean modoPrueba) {
        if (server == null) return "El minijuego aun no esta listo.";
        if (runs.containsKey(p.getUuid())) return "Ya estás en una partida. Usa /survivor parar para cancelarla.";
        if (runs.size() >= MAX_PARTIDAS) return "Hay " + runs.size() + " partidas en marcha (el máximo es " + MAX_PARTIDAS + "). Espera a que acabe alguna.";
        ServerWorld w = mundo();
        if (w == null) return "No existe la dimension emi_survivors:arena (¿esta instalado el mod en el servidor?).";
        if (modoPrueba && Saved.exists(server, p.getUuid())) return "Tienes cosas guardadas de una partida anterior: sal y vuelve a entrar para recuperarlas.";
        if (!modoPrueba && !Mundo.enSala(p)) return "Tienes que estar en la sala del minijuego (usa /survivor entrar).";
        if (!ServerPlayNetworking.canSend(p, EstadoPayload.ID)) return "Tu juego no tiene el mod Emi Survivors: actualiza el pack y vuelve a entrar.";
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(p);
        Pokemon pk = party.get(hueco - 1);
        StringBuilder eq = new StringBuilder();
        for (int i = 0; i < 6; i++) { Pokemon x = party.get(i); if (x != null) eq.append(i + 1).append("=").append(x.getSpecies().getName()).append(" Nv.").append(x.getLevel()).append("  "); }
        EmiSurvivors.LOG.info("Equipo de {}: {}", p.getGameProfile().getName(), eq);
        if (pk == null) return "No tienes ningún Pokémon en el hueco " + hueco + " del equipo.";
        if (!Morph.disponible()) return "Falta el mod CobbleMorph (el jugador va transformado en su Pokémon).";
        int slot = huecoLibre();
        if (slot < 0) return "No hay arenas libres.";
        if (modoPrueba) { try { Saved.save(server, p, false); } catch (RuntimeException e) { return "No se pudo guardar tu inventario: no entras."; } }
        String antes = modoPrueba ? Morph.actual(p.getUuid()) : "";
        double[] orig = Arena.origen(slot);
        double ox = orig[0], oz = orig[1];
        Run run;
        try {
            if (runs.isEmpty()) Pausas.pausar();
            Arena.preparar(w, slot);
            p.teleport(w, ox + 0.5, Arena.Y0, oz + 0.5, 180f, 0f);
            prepararJugador(p);
            Morph.transformar(server, p, pk);
            fijarEscala(p, pk);
            Map<String, Integer> meta = new java.util.HashMap<>(datos.poke(datos.jugador(p.getUuid(), p.getGameProfile().getName()), pk.getUuid().toString(), pk.getSpecies().getResourceIdentifier().getPath()).mejoras);
            run = new Run(w, p, pk, slot, ox, oz, antes, fijo, inmortal, modoPrueba, meta);
            runs.put(p.getUuid(), run);
        } catch (Throwable t) {
            // si algo falla a medias, el jugador no se queda a medio camino: se le devuelve todo
            EmiSurvivors.LOG.error("No se pudo empezar la partida", t);
            runs.remove(p.getUuid());
            try {
                if (modoPrueba) { Morph.restaurar(server, p, antes); Saved.restore(server, p); }
                else Mundo.volverASala(p, null);
            } catch (Throwable t2) { EmiSurvivors.LOG.error("Ademas, no se pudo devolver al jugador a su sitio", t2); }
            if (runs.isEmpty()) { try { Pausas.reanudar(); } catch (Throwable t2) { /* nada */ } }
            return "No se pudo empezar la partida (mira el registro del servidor); te hemos devuelto a tu sitio.";
        }
        EmiSurvivors.LOG.info("Partida de {}: Pokemon {} (tipo {}, ataque inicial {}), hueco de arena {}, fijo={}, inmortal={}", p.getGameProfile().getName(), pk.getSpecies().getName(), run.tipo, run.inicial.nombre(), slot, fijo, inmortal);
        p.sendMessage(Msg.say("¡Empieza! Muévete con WASD; tu " + pk.getSpecies().getName() + " ataca solo con " + run.inicial.nombre() + ". Para salir: /survivor parar"), false);
        return null;
    }

    /** Cancela la partida de {@code p}. */
    public static String parar(ServerPlayerEntity p) {
        Run r = runs.get(p.getUuid());
        if (r == null) return "No estás en ninguna partida.";
        terminar(r, "Partida cancelada.");
        return null;
    }

    /** Cancela todas las partidas (organizador). Devuelve cuantas habia. */
    public static int pararTodas() {
        int n = runs.size();
        for (Run r : new ArrayList<>(runs.values())) terminar(r, "Partida cancelada por un administrador.");
        return n;
    }

    public static String estado() {
        if (runs.isEmpty()) return "No hay ninguna partida en marcha." + (inmortal ? " (modo prueba inmortal activado)" : "");
        StringBuilder sb = new StringBuilder();
        for (Run r : runs.values()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("Partida de ").append(r.nombre).append(": ").append(r.segundos()).append(" s, nivel ").append(r.nivel).append(", ").append(r.enemigos.size()).append(" enemigos, ")
                    .append(r.derrotados).append(" derrotados, vida ").append(Math.max(0, Math.round(r.hp))).append("/").append(Math.round(r.st.hpMax))
                    .append(" | servidor ").append(Run.fmt(r.med.msptServidor())).append(" ms/tick, nuestro ").append(Run.fmt(r.med.msNuestro())).append(" ms/tick");
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- fin de la partida

    static void terminar(Run r, String motivo) {
        if (r == null || runs.get(r.id) != r) return;
        runs.remove(r.id);
        ServerPlayerEntity p = r.jugador();
        r.limpiar();
        int seg = r.segundos();
        EmiSurvivors.LOG.info("Fin de la partida de {}: {} | {} s, nivel {}, {} derrotados, {} especies distintas, {} fallos al crear{}", r.nombre, motivo, seg, r.nivel, r.derrotados, r.especiesVistas.size(), r.fallosCrear,
                r.fijo > 0 ? " | medias con " + r.fijo + " enemigos: servidor " + Run.fmt(r.msptEstable()) + " ms/tick, nuestro " + Run.fmt(r.nuestroEstable()) + " ms/tick (max servidor " + Run.fmt(r.med.msptMax()) + " ms)" : "");
        // lo que se gana: monedas para las mejoras y, si es su mejor marca, un sitio en el top (las pruebas de administrador y las medidas no cuentan)
        boolean cuenta = r.fijo == 0 && !r.inmortal && !r.modoPrueba;
        int monedas = 0, puesto = 0;
        if (cuenta) {
            try {
                monedas = Curvas.monedas(seg, r.derrotados, r.nivel, r.jefeDerrotado, r.st.monedas);
                Datos.Entrada e = new Datos.Entrada();
                e.especie = r.especieId; e.aspectos = String.join(" ", r.pk.getAspects()); e.nivelPokemon = r.pk.getLevel();
                e.segundos = seg; e.derrotados = r.derrotados; e.nivel = r.nivel; e.jefe = r.jefeDerrotado;
                puesto = datos.registrar(r.id, r.nombre, r.pokeId, e, monedas);
            } catch (RuntimeException ex) { EmiSurvivors.LOG.error("No se pudo guardar el resultado de la partida de {}", r.nombre, ex); }
        }
        if (p != null) {
            try {
                if (ServerPlayNetworking.canSend(p, EstadoPayload.ID)) ServerPlayNetworking.send(p, EstadoPayload.apagado());
                if (ServerPlayNetworking.canSend(p, GemasPayload.ID)) ServerPlayNetworking.send(p, GemasPayload.vacio());
                if (ServerPlayNetworking.canSend(p, TextoPayload.ID)) ServerPlayNetworking.send(p, new TextoPayload("cartas", ""));
            } catch (Throwable t) { /* el cliente no tiene el mod */ }
            if (r.modoPrueba) {
                Morph.restaurar(server, p, r.morphAntes);
                if (!Saved.restore(server, p)) quitarEfectos(p);
            } else {
                quitarEfectos(p);
                Mundo.volverASala(p, new Menu.Resultado(motivo, seg, r.derrotados, r.nivel, monedas, puesto, r.jefeDerrotado, cuenta));
            }
            p.sendMessage(Msg.say(motivo + " Aguantaste " + (seg / 60) + ":" + String.format("%02d", seg % 60) + ", llegaste al nivel " + r.nivel + " y derrotaste a " + r.derrotados + " Pokémon."
                    + (cuenta ? " Ganas " + monedas + " michiestrellitas." + (puesto > 0 ? " Puesto " + puesto + " del top." : "") : "")), false);
            if (r.fijo > 0 && r.hayMedidaEstable())
                p.sendMessage(Text.literal("Rendimiento de esta prueba (con " + r.fijo + " enemigos): el servidor tardó de media " + Run.fmt(r.msptEstable())
                        + " ms por tick (50 es el límite); este mod gastó " + Run.fmt(r.nuestroEstable()) + " ms.").formatted(Formatting.GRAY), false);
        }
        if (runs.isEmpty()) { try { Pausas.reanudar(); } catch (Throwable t) { /* nada */ } }
    }

    // ---------------------------------------------------------------- jugador

    static void alEntrar(ServerPlayerEntity p) {
        if (server == null) return;
        if (Saved.exists(server, p.getUuid()) && !participa(p)) { pendientes.add(p.getUuid()); pendientesTick.add(server.getTicks() + 40L); }
    }

    static void alSalir(ServerPlayerEntity p) {
        Run r = runs.get(p.getUuid());
        if (r != null) terminar(r, "Te desconectaste.");
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

    /** Velocidad al andar: ~6 bloques/s por el multiplicador {@code mult} de las mejoras del jugador. */
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

    /** Para probar: da experiencia, niveles, ataques... a la partida del administrador. Devuelve el motivo si no se puede, o null. */
    public static String dev(ServerPlayerEntity p, String que, String id, int n) {
        Run r = runs.get(p.getUuid());
        if (r == null) return "Primero empieza una partida con /survivor prueba.";
        switch (que) {
            case "xp" -> r.ganarXp(Math.max(1, n));
            case "nivel" -> { r.nivel += Math.max(1, n); r.pendientes += Math.max(1, n); r.xpSig = Curvas.xpPara(r.nivel); r.xp = 0; }
            case "tiempo" -> r.tick = Math.max(0, n) * 20;
            case "jefe" -> { if (r.jefe != null && r.jefe.vivo()) return "Ya hay un jefe vivo."; r.crearJefe(p); }
            case "elite" -> { return r.devElite() ? null : "No se pudo crear el campeón."; }
            case "curar" -> r.hp = r.st.hpMax;
            case "sinxp" -> { r.sinXp = !r.sinXp; return null; }
            case "arma" -> {
                if (Catalogo.arma(id) == null) return "Ataque desconocido: " + id;
                int nv = Math.max(1, Math.min(Catalogo.MAX_NIVEL, n));
                if (r.armaObj.containsKey(id)) { r.armas.put(id, nv); r.armaObj.get(id).nivel = nv; } else r.agregarArma(id, nv);
            }
            case "solo" -> {
                if (Catalogo.arma(id) == null) return "Ataque desconocido: " + id;
                r.armas.clear(); r.armaObj.clear();
                r.agregarArma(id, Math.max(1, Math.min(Catalogo.MAX_NIVEL, n)));
            }
            case "pasiva" -> {
                if (Catalogo.pasiva(id) == null) return "Mejora desconocida: " + id;
                r.pasivas.put(id, Math.max(1, Math.min(Catalogo.MAX_NIVEL, n)));
                r.recalcular();
            }
            default -> { return "Opciones: xp, nivel, tiempo, jefe, elite, curar, sinxp, arma, solo, pasiva."; }
        }
        return null;
    }
}
