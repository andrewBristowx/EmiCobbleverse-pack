package emi.survivors;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import emi.survivors.net.EstadoPayload;
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
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Controla la partida de la fase 0: empezarla (guardar al jugador, llevarlo a la arena, transformarlo en su Pokemon), llamarla cada tick, y acabarla
 * (limpiar la arena, devolver al jugador a su sitio y contar lo que paso). Solo hay una partida a la vez y un jugador por partida; el multijugador
 * (grupos de hasta 4) llega en la fase 2.
 */
public final class Partida {
    private Partida() {}

    static final int HP_MAX = 100;
    static final double VELOCIDAD = 0.14;            // atributo de velocidad del jugador (0,1 = andar normal de Minecraft); 0,14 = ~6 bloques/s
    private static final Identifier ID_VEL = Identifier.of("emi_survivors", "velocidad");
    private static final Identifier ID_SALTO = Identifier.of("emi_survivors", "sin_salto");

    static MinecraftServer server;
    static Run run;
    /** Modo prueba: el jugador no pierde vida (para medir el rendimiento sin que la partida acabe). */
    static boolean inmortal;

    private static final List<UUID> pendientes = new ArrayList<>();      // jugadores a los que hay que devolver sus cosas al entrar
    private static final List<Long> pendientesTick = new ArrayList<>();

    static ServerWorld mundo() {
        return server == null ? null : server.getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of("emi_survivors", "arena")));
    }

    static void init(MinecraftServer s) {
        server = s;
        run = null;
        ServerWorld w = mundo();
        if (w == null) { EmiSurvivors.LOG.warn("No existe la dimension emi_survivors:arena: el minijuego no funcionara."); return; }
        // restos de una partida anterior que no pudo limpiarse (servidor parado a la fuerza): enemigos con nuestra etiqueta
        List<Entity> borrar = new ArrayList<>();
        for (Entity e : w.iterateEntities()) if (e.getCommandTags().contains("emi_survivors")) borrar.add(e);
        for (Entity e : borrar) e.discard();
        if (!borrar.isEmpty()) EmiSurvivors.LOG.info("Se han quitado {} enemigos que habian quedado de una partida anterior.", borrar.size());
    }

    static void shutdown() {
        if (run != null) terminar(run, "El servidor se apaga.");
        try { Pausas.reanudar(); } catch (Throwable t) { /* nada */ }
    }

    public static boolean participa(PlayerEntity p) { return run != null && run.id.equals(p.getUuid()); }

    static void tick(MinecraftServer s) {
        if (run != null) {
            try { run.tick(); }
            catch (Throwable t) { EmiSurvivors.LOG.error("Error en la partida; se cancela", t); if (run != null) terminar(run, "Error interno: se cancela la partida."); }
        }
        // devolver sus cosas a quien volvio a entrar despues de que el servidor se parara en plena partida
        for (int i = pendientes.size() - 1; i >= 0; i--) {
            if (s.getTicks() < pendientesTick.get(i)) continue;
            UUID id = pendientes.remove(i); pendientesTick.remove(i);
            ServerPlayerEntity p = s.getPlayerManager().getPlayer(id);
            if (p != null && !participa(p) && Saved.exists(s, id)) {
                Morph.restaurar(s, p, "");
                Saved.restore(s, p);
                p.sendMessage(Msg.say("Te hemos devuelto tus cosas de la partida."), false);
            }
        }
    }

    // ---------------------------------------------------------------- comandos

    /** Empieza una partida de prueba para {@code p}: {@code enemigos} Pokemon a la vez, con el Pokemon del hueco {@code slot} (1..6) del equipo. */
    public static String prueba(ServerPlayerEntity p, int enemigos, int slot) {
        if (server == null) return "El minijuego aun no esta listo.";
        if (run != null) return "Ya hay una partida en marcha (" + run.nombre + "). Usa /survivor parar para cancelarla.";
        ServerWorld w = mundo();
        if (w == null) return "No existe la dimension emi_survivors:arena (¿esta instalado el mod en el servidor?).";
        if (Saved.exists(server, p.getUuid())) return "Tienes cosas guardadas de una partida anterior: sal y vuelve a entrar para recuperarlas.";
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(p);
        Pokemon pk = party.get(slot - 1);
        StringBuilder eq = new StringBuilder();
        for (int i = 0; i < 6; i++) { Pokemon x = party.get(i); if (x != null) eq.append(i + 1).append("=").append(x.getSpecies().getName()).append(" Nv.").append(x.getLevel()).append("  "); }
        EmiSurvivors.LOG.info("Equipo de {}: {}", p.getGameProfile().getName(), eq);
        if (pk == null) return "No tienes ningun Pokémon en el hueco " + slot + " del equipo.";
        if (!Morph.disponible()) return "Falta el mod CobbleMorph (el jugador va transformado en su Pokémon).";
        try { Saved.save(server, p); } catch (RuntimeException e) { return "No se pudo guardar tu inventario: no entras."; }
        String antes = Morph.actual(p.getUuid());
        try {
            Pausas.pausar();
            p.teleport(w, 0.5, Arena.Y0, 0.5, 180f, 0f);
            prepararJugador(p);
            Morph.transformar(server, p, pk);
            run = new Run(w, p, pk, antes, enemigos, inmortal);
        } catch (Throwable t) {
            // si algo falla a medias, el jugador no se queda a medio camino: se le devuelve todo
            EmiSurvivors.LOG.error("No se pudo empezar la partida de prueba", t);
            run = null;
            try { Morph.restaurar(server, p, antes); Saved.restore(server, p); } catch (Throwable t2) { EmiSurvivors.LOG.error("Ademas, no se pudo devolver al jugador a su sitio", t2); }
            try { Pausas.reanudar(); } catch (Throwable t2) { /* nada */ }
            return "No se pudo empezar la partida (mira el registro del servidor); te hemos devuelto a tu sitio.";
        }
        EmiSurvivors.LOG.info("Partida de prueba de {}: {} enemigos, Pokemon {} (tipo {}), inmortal={}", p.getGameProfile().getName(), enemigos, pk.getSpecies().getName(), run.tipo, inmortal);
        p.sendMessage(Msg.say("¡Empieza! Muévete con WASD; tu " + pk.getSpecies().getName() + " ataca solo. Para salir: /survivor parar"), false);
        return null;
    }

    /** Cancela la partida en marcha si es de {@code p} (o de cualquiera si {@code p} es null: el organizador). */
    public static String parar(ServerPlayerEntity p) {
        if (run == null) return "No hay ninguna partida en marcha.";
        if (p != null && !run.id.equals(p.getUuid())) return "La partida en marcha es de " + run.nombre + ".";
        terminar(run, "Partida cancelada.");
        return null;
    }

    public static String estado() {
        if (run == null) return "No hay ninguna partida en marcha." + (inmortal ? " (modo prueba inmortal activado)" : "");
        return "Partida de " + run.nombre + ": " + run.segundos() + " s, " + run.enemigos.size() + " enemigos, " + run.derrotados + " derrotados, vida " + Math.max(0, run.hp) + "/" + HP_MAX
                + " | servidor " + Run.fmt(run.med.msptServidor()) + " ms/tick, nuestro " + Run.fmt(run.med.msNuestro()) + " ms/tick";
    }

    // ---------------------------------------------------------------- fin de la partida

    static void terminar(Run r, String motivo) {
        if (r == null || run != r) return;
        run = null;
        ServerPlayerEntity p = r.jugador();
        r.limpiar();
        int seg = r.segundos();
        EmiSurvivors.LOG.info("Fin de la partida de {}: {} | {} s, {} derrotados, {} especies distintas, {} fallos al crear | medias con {} enemigos: servidor {} ms/tick, nuestro {} ms/tick (max servidor {} ms)",
                r.nombre, motivo, seg, r.derrotados, r.especiesVistas.size(), r.fallosCrear, r.objetivo, Run.fmt(r.msptEstable()), Run.fmt(r.nuestroEstable()), Run.fmt(r.med.msptMax()));
        if (p != null) {
            try { if (ServerPlayNetworking.canSend(p, EstadoPayload.ID)) ServerPlayNetworking.send(p, EstadoPayload.apagado()); } catch (Throwable t) { /* el cliente no tiene el mod */ }
            Morph.restaurar(server, p, r.morphAntes);
            if (!Saved.restore(server, p)) quitarEfectos(p);
            p.sendMessage(Msg.say(motivo + " Aguantaste " + (seg / 60) + ":" + String.format("%02d", seg % 60) + " y derrotaste a " + r.derrotados + " Pokémon."), false);
            if (r.hayMedidaEstable())
                p.sendMessage(Text.literal("Rendimiento de esta prueba (con " + r.objetivo + " enemigos): el servidor tardó de media " + Run.fmt(r.msptEstable())
                        + " ms por tick (50 es el límite); este mod gastó " + Run.fmt(r.nuestroEstable()) + " ms.").formatted(Formatting.GRAY), false);
        }
        try { Pausas.reanudar(); } catch (Throwable t) { /* nada */ }
    }

    // ---------------------------------------------------------------- jugador

    static void alEntrar(ServerPlayerEntity p) {
        if (server == null) return;
        if (Saved.exists(server, p.getUuid()) && !participa(p)) { pendientes.add(p.getUuid()); pendientesTick.add(server.getTicks() + 40L); }
    }

    static void alSalir(ServerPlayerEntity p) {
        if (run != null && run.id.equals(p.getUuid())) terminar(run, "Te desconectaste.");
    }

    /** Modo partida: aventura, inmortal para Minecraft (la vida es la del minijuego), sin inventario, velocidad fija y sin salto. */
    private static void prepararJugador(ServerPlayerEntity p) {
        p.clearStatusEffects();
        p.getInventory().clear();
        p.changeGameMode(GameMode.ADVENTURE);
        p.setInvulnerable(true);
        p.setGlowing(true);                  // contorno brillante: se ve a traves de la horda
        p.getAbilities().flying = false;
        p.sendAbilitiesUpdate();
        p.getHungerManager().setFoodLevel(20); p.getHungerManager().setSaturationLevel(5f);
        atributo(p, EntityAttributes.GENERIC_MOVEMENT_SPEED, ID_VEL, VELOCIDAD - 0.1, EntityAttributeModifier.Operation.ADD_VALUE, true);
        atributo(p, EntityAttributes.GENERIC_JUMP_STRENGTH, ID_SALTO, -1.0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, true);
    }

    static void quitarEfectos(ServerPlayerEntity p) {
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
}
