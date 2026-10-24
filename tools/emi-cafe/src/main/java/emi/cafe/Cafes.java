package emi.cafe;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reparte los locales: cada jugador que entra tiene el suyo (su «cafe») en la dimension {@code emi_cafe:cafe}, a 64 bloques del de al lado, y puede invitar a otros
 * jugadores conectados, que cocinan en su cafe como invitados.
 */
final class Cafes {
    private Cafes() {}

    static final RegistryKey<World> DIM = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("emi_cafe", "cafe"));
    static final int MAX_JUGADORES = 4;
    private static final Map<UUID, Cafe> DUENIOS = new HashMap<>();             // dueño -> su cafe
    private static final Map<UUID, Cafe> POR_JUGADOR = new HashMap<>();         // cualquiera que este dentro (dueño o invitado) -> el cafe
    private record Invitacion(UUID duenio, long expira) {}
    private static final Map<UUID, Invitacion> INVITACIONES = new HashMap<>();
    private static long reloj;

    static ServerWorld mundo(MinecraftServer s) { return s.getWorld(DIM); }

    static boolean enCafe(ServerPlayerEntity p) { return p.getServerWorld().getRegistryKey().equals(DIM); }

    /** El cafe donde esta este jugador (null si no esta en ninguno). */
    static Cafe de(UUID id) { return POR_JUGADOR.get(id); }

    static boolean tiene(UUID id) { return POR_JUGADOR.containsKey(id); }

    private static int slotLibre() {
        outer:
        for (int i = 0; ; i++) { for (Cafe c : DUENIOS.values()) if (c.slot == i) continue outer; return i; }
    }

    /** Si el jugador esta en otro minijuego o mundo del servidor, no puede entrar al cafe (su inventario ya esta guardado alli). */
    private static String ocupado(ServerPlayerEntity p) {
        String ns = p.getServerWorld().getRegistryKey().getValue().getNamespace();
        if (ns.startsWith("emi_") && !ns.equals("emi_cafe")) return "está en otro minijuego (o mundo del servidor): tiene que salir primero";
        return null;
    }

    private static void meter(ServerPlayerEntity p, Cafe c) {
        MinecraftServer s = p.getServer();
        if (!Saved.exists(s, p.getUuid())) Saved.save(s, p, true);
        POR_JUGADOR.put(p.getUuid(), c);
        c.jugadores.add(p.getUuid());
        c.manos.put(p.getUuid(), null);
        p.getInventory().clear();
        p.clearStatusEffects();
        p.changeGameMode(GameMode.ADVENTURE);
        p.getAbilities().flying = false; p.sendAbilitiesUpdate();
        p.teleport(c.w, c.ox + Local.SX, Local.Y + 1, Local.SZ, 180f, 0f);
        c.sincronizarMano(p);
    }

    /** Mete al jugador en su propio cafe (guardando antes lo suyo). Devuelve el error, o null. */
    static String entrar(ServerPlayerEntity p) {
        MinecraftServer s = p.getServer();
        ServerWorld w = mundo(s);
        if (w == null) return "No existe la dimensión del café (¿falta el mod emi-cafe en el servidor?).";
        if (POR_JUGADOR.containsKey(p.getUuid())) return "Ya estás en un café: usa /cafe salir para irte.";
        String oc = ocupado(p);
        if (oc != null) return "Estás en otro minijuego (o mundo del servidor): sal primero de él.";
        if (EmiCafe.datos == null) return "El café aún no está listo.";
        Cafe c = new Cafe(s, w, p.getUuid(), slotLibre());
        DUENIOS.put(p.getUuid(), c);
        c.reconstruir();
        meter(p, c);
        Datos.Jugador d = c.dj();
        if (d.inicial == null) {
            p.sendMessage(Msg.say("¡Bienvenido! Elige al Pokémon que será tu cocinero: te enseña tu primer plato."), false);
            Menus.iniciales(c, p);
        } else p.sendMessage(Msg.say("Bienvenido a tu café. Clic derecho en la caja registradora (a la derecha, en la barra) para abrir el café, comprar o invitar a un amigo."), false);
        return null;
    }

    /** Invita a otro jugador conectado a tu café (solo el dueño). */
    static String invitar(Cafe c, ServerPlayerEntity host, ServerPlayerEntity target) {
        if (!c.esDuenio(host)) return "Solo el dueño del café puede invitar.";
        if (c.construyendo) return "Estás construyendo: termina antes de invitar.";
        if (target == host) return "No puedes invitarte a ti mismo.";
        if (POR_JUGADOR.containsKey(target.getUuid())) return target.getGameProfile().getName() + " ya está en un café.";
        if (c.jugadores.size() >= MAX_JUGADORES) return "Tu café está lleno (máximo " + MAX_JUGADORES + " personas).";
        String oc = ocupado(target);
        if (oc != null) return target.getGameProfile().getName() + " " + oc + ".";
        INVITACIONES.put(target.getUuid(), new Invitacion(host.getUuid(), reloj + 20 * 60));
        String hn = host.getGameProfile().getName();
        target.sendMessage(Msg.say(hn + " te invita a su Café Michialien. ").append(Text.literal("[ACEPTAR]").styled(s -> s.withColor(Formatting.GREEN).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cafe aceptar " + hn)))).append(Text.literal(" (vale 1 minuto)").formatted(Formatting.GRAY)), false);
        host.sendMessage(Msg.say("Invitación enviada a " + target.getGameProfile().getName() + "."), false);
        return null;
    }

    /** El invitado acepta: entra al cafe del dueño que le invito. */
    static String aceptar(ServerPlayerEntity guest, String nombreDuenio) {
        Invitacion inv = INVITACIONES.get(guest.getUuid());
        if (inv == null || inv.expira < reloj) return "No tienes ninguna invitación (o caducó).";
        Cafe c = DUENIOS.get(inv.duenio);
        ServerPlayerEntity host = c == null ? null : c.jugador(inv.duenio);
        if (c == null || host == null) { INVITACIONES.remove(guest.getUuid()); return "Ese café ya cerró."; }
        if (nombreDuenio != null && !nombreDuenio.isEmpty() && !host.getGameProfile().getName().equalsIgnoreCase(nombreDuenio)) return "Esa invitación no es de " + nombreDuenio + ".";
        if (POR_JUGADOR.containsKey(guest.getUuid())) return "Ya estás en un café.";
        if (c.jugadores.size() >= MAX_JUGADORES) return "El café está lleno.";
        String oc = ocupado(guest);
        if (oc != null) return "Estás en otro minijuego (o mundo del servidor): sal primero de él.";
        INVITACIONES.remove(guest.getUuid());
        meter(guest, c);
        guest.sendMessage(Msg.say("Estás en el café de " + host.getGameProfile().getName() + ". Ayuda a cocinar y servir: lo que ganes es tuyo. /cafe salir para irte."), false);
        host.sendMessage(Msg.say(guest.getGameProfile().getName() + " se unió a tu café."), false);
        return null;
    }

    /** Saca al jugador del cafe y le devuelve todo. Si es el dueño, se cierra el cafe y salen todos los invitados. */
    static boolean salir(ServerPlayerEntity p) {
        Cafe c = POR_JUGADOR.get(p.getUuid());
        MinecraftServer s = p.getServer();
        if (c != null && c.esDuenio(p)) {
            for (UUID id : new ArrayList<>(c.jugadores)) {
                POR_JUGADOR.remove(id);
                if (id.equals(p.getUuid())) continue;
                ServerPlayerEntity g = c.jugador(id);
                if (g != null) { g.sendMessage(Msg.say("El dueño cerró el café."), false); Menus.cerrar(g); g.closeHandledScreen(); Saved.restore(s, g); }
            }
            DUENIOS.remove(p.getUuid());
            c.cerrar();
        } else if (c != null) {
            POR_JUGADOR.remove(p.getUuid());
            c.jugadores.remove(p.getUuid());
            c.manos.remove(p.getUuid());
        }
        return Saved.restore(s, p);
    }

    /** Un jugador que estaba en un cafe se fue por otro medio (por ejemplo otro minijuego): se le quita del cafe sin tocar lo que guardamos de el. Si el dueño se va, se cierra el cafe. */
    static void sinRestaurar(ServerPlayerEntity p) {
        Cafe c = POR_JUGADOR.remove(p.getUuid());
        if (c == null) return;
        c.jugadores.remove(p.getUuid());
        c.manos.remove(p.getUuid());
        if (c.esDuenio(p)) {
            for (UUID id : new ArrayList<>(c.jugadores)) {
                POR_JUGADOR.remove(id);
                ServerPlayerEntity g = c.jugador(id);
                if (g != null) { g.sendMessage(Msg.say("El dueño se fue: se cerró el café."), false); Menus.cerrar(g); g.closeHandledScreen(); Saved.restore(p.getServer(), g); }
            }
            DUENIOS.remove(p.getUuid());
            c.cerrar();
        }
    }

    /** Se desconecta: si era el dueño se cierra su cafe y salen los invitados; lo suyo se le devuelve al volver a entrar (Retornos). */
    static void alDesconectar(ServerPlayerEntity p) {
        Cafe c = POR_JUGADOR.get(p.getUuid());
        INVITACIONES.remove(p.getUuid());
        if (c == null) return;
        MinecraftServer s = p.getServer();
        if (c.esDuenio(p)) {
            for (UUID id : new ArrayList<>(c.jugadores)) {
                POR_JUGADOR.remove(id);
                if (id.equals(p.getUuid())) continue;
                ServerPlayerEntity g = c.jugador(id);
                if (g != null) { g.sendMessage(Msg.say("El dueño salió: se cerró el café."), false); Menus.cerrar(g); g.closeHandledScreen(); Saved.restore(s, g); }
            }
            DUENIOS.remove(p.getUuid());
            c.cerrar();
        } else {
            POR_JUGADOR.remove(p.getUuid());
            c.jugadores.remove(p.getUuid());
            c.manos.remove(p.getUuid());
        }
    }

    static void tick(MinecraftServer s) {
        reloj++;
        for (Cafe c : DUENIOS.values().toArray(new Cafe[0])) {
            try { c.tick(); } catch (Throwable t) { EmiCafe.LOG.error("Error en un tick del cafe", t); }
        }
        if (reloj % 200 == 0) INVITACIONES.values().removeIf(i -> i.expira < reloj);
    }

    static void cerrarTodos() {
        for (Cafe c : DUENIOS.values()) c.cerrar();
        DUENIOS.clear();
        POR_JUGADOR.clear();
        INVITACIONES.clear();
    }
}
