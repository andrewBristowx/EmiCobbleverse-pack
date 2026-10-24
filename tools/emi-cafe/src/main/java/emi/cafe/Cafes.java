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
        if (c.dj().libre) p.teleport(c.w, c.ox + Local.PUERTA_X, Local.Y + 1, Local.CALLE_Z, 180f, 0f);        // parcela libre: la acera de delante siempre esta libre
        else p.teleport(c.w, c.ox + Local.SX, Local.Y + 1, Local.SZ, 180f, 0f);
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
        Cafe c = new Cafe(s, w, p.getUuid(), Ciudad.slotDe(p.getUuid(), p.getGameProfile().getName()));
        DUENIOS.put(p.getUuid(), c);
        c.migrarAyudantes();
        c.reconstruir();
        meter(p, c);
        Datos.Jugador d = c.dj();
        if (d.inicial == null) {
            p.sendMessage(Msg.say("¡Bienvenido! Elige al Pokémon que será tu cocinero: te enseña tu primer plato."), false);
            Menus.iniciales(c, p);
        } else {
            p.sendMessage(Msg.say("Bienvenido a tu café. Clic derecho en la caja registradora (a la derecha, en la barra) para abrir el café, comprar o invitar a un amigo. En la pared del fondo de la cocina está el LIBRO DE RECETAS (clic derecho). Fuera hay una ciudad: puedes pasear hasta los cafés de los demás; para cocinar con ellos te tienen que invitar."), false);
            c.iniciarTutorial(false);
        }
        return null;
    }

    /** ¿Se puede invitar a este jugador ahora? Si esta libre (fuera de los minijuegos) o si es el dueño de su cafe con el cafe cerrado y a solas (al aceptar, deja el suyo). */
    static boolean invitable(ServerPlayerEntity q) {
        Cafe c = POR_JUGADOR.get(q.getUuid());
        if (c != null) return c.esDuenio(q) && enCafe(q) && c.estado == Cafe.Estado.PREPARANDO && !c.construyendo && c.trabajadores() <= 1 && c.visitantes.isEmpty();
        return ocupado(q) == null;
    }

    /** Va andando (se teletransporta, pero sin cambiar nada de lo tuyo) a la acera de delante del cafe de un vecino de la ciudad. Hay que estar dentro de la ciudad. */
    static String irA(ServerPlayerEntity p, String nombre) {
        if (!enCafe(p)) return "Solo se puede ir al café de un vecino estando ya en la ciudad: usa /cafe entrar (o /cafe visitar " + nombre + ").";
        for (Ciudad.Vecino v : Ciudad.todos()) if (v.nombre.equalsIgnoreCase(nombre)) {
            p.teleport(mundo(p.getServer()), Ciudad.origen(v.slot) + Local.PUERTA_X, Local.Y + 1, Local.CALLE_Z, 180f, 0f);
            p.sendMessage(Msg.say("Estás frente al café de " + v.nombre + ". Puedes entrar a mirar; para cocinar con él, tiene que invitarte."), false);
            return null;
        }
        return "No hay ningún café de " + nombre + " en la ciudad.";
    }

    /** Clic en la caja registradora de un vecino: una linea con lo suyo y, si esta dentro de su café, el boton de «me gusta». */
    static void infoVecino(ServerPlayerEntity p, int slot) {
        String nom = Ciudad.nombreDe(slot);
        UUID uid = Ciudad.duenioDe(slot);
        if (nom == null || uid == null || EmiCafe.datos == null) return;
        Datos.Jugador d = EmiCafe.datos.jugador(uid, null);
        Cafe c = DUENIOS.get(uid);
        net.minecraft.text.MutableText m = Msg.say((d.nombreCafe == null || d.nombreCafe.isEmpty() ? "Café" : "«" + d.nombreCafe + "»") + " de " + nom + " · ♥ " + d.megusta + " · estilo " + Estilo.NOMBRES[Estilo.nivelDe(Estilo.puntos(d.decor, d.estilo, d.pintura))]
                + " · reputación " + Reputacion.nombre(Reputacion.nivel(d.rep)) + (c == null ? " · cerrado: no está" : c.estado == Cafe.Estado.ABIERTO ? " · ABIERTO" : "") + " ");
        if (c != null && !c.esDuenio(p)) m.append(Text.literal("[ME GUSTA]").styled(s -> s.withColor(Formatting.LIGHT_PURPLE).withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cafe megusta " + nom))));
        p.sendMessage(m, false);
    }

    static final int MAX_VISITANTES = 8;

    /** Los cafes abiertos ahora mismo (para la pestaña CAFÉS). */
    static java.util.Collection<Cafe> abiertos() { return DUENIOS.values(); }

    static Cafe deDuenio(String nombre) {
        for (Cafe c : DUENIOS.values()) { ServerPlayerEntity d = c.jugador(c.duenio); if (d != null && d.getGameProfile().getName().equalsIgnoreCase(nombre)) return c; }
        return null;
    }

    /** Va de visita al cafe de otro jugador (que tiene que estar dentro de él y permitir visitas). Solo mira: no cocina ni sirve. Devuelve el error, o null. */
    static String visitar(ServerPlayerEntity p, String nombre) {
        MinecraftServer s = p.getServer();
        if (mundo(s) == null) return "No existe la dimensión del café.";
        if (EmiCafe.datos == null) return "El café aún no está listo.";
        if (POR_JUGADOR.containsKey(p.getUuid())) return "Ya estás en un café: sal primero (/cafe salir).";
        String oc = ocupado(p);
        if (oc != null) return "Estás en otro minijuego (o mundo del servidor): sal primero de él.";
        Cafe c = deDuenio(nombre);
        if (c == null) return nombre + " no tiene su café abierto ahora mismo (solo se visita a quien está dentro de él).";
        ServerPlayerEntity du = c.jugador(c.duenio);
        if (c.duenio.equals(p.getUuid())) return "Ese es tu propio café.";
        if (!c.dj().visitas) return du.getGameProfile().getName() + " no acepta visitas ahora.";
        if (c.visitantes.size() >= MAX_VISITANTES) return "Ya hay demasiadas visitas en ese café.";
        meter(p, c);
        c.visitantes.add(p.getUuid());
        p.teleport(c.w, c.ox + Local.PUERTA_X, Local.Y + 1, Local.CALLE_Z, 180f, 0f);
        p.sendMessage(Msg.say("Estás de visita en el café de " + du.getGameProfile().getName() + ", en la acera de delante de su puerta. Entra a mirar cómo lo decoró y, si te gusta, toca su caja registradora y dale «me gusta». Puedes pasear por toda la ciudad. /cafe salir para irte."), false);
        du.sendMessage(Msg.say(p.getGameProfile().getName() + " está de visita en tu café."), false);
        return null;
    }

    /** Un visitante da «me gusta» al cafe (una vez por persona): el dueño gana estrellas y reputacion. */
    static String megusta(Cafe c, ServerPlayerEntity p) {
        if (c.esDuenio(p) || !enCafe(p)) return "Solo se da «me gusta» al café de otro vecino, estando en la ciudad.";
        Datos.Jugador d = c.dj();
        String id = p.getUuid().toString();
        if (d.likesDe.contains(id)) return "Ya le diste «me gusta» a este café.";
        d.likesDe.add(id); d.megusta++;
        Progreso.dar(c, d, c.duenio, 2);
        EmiCafe.datos.guardar(c.duenio);
        c.ganarRep(3);
        Progreso.dar(null, c.dj(p), p.getUuid(), 1);
        EmiCafe.datos.guardar(p.getUuid());
        ServerPlayerEntity du = c.jugador(c.duenio);
        if (du != null) { du.sendMessage(Msg.say("¡" + p.getGameProfile().getName() + " le dio «me gusta» a tu café! (♥ " + d.megusta + ") +2 estrellas."), false); Sonidos.jugador(du, "megusta", 1f, 1f); }
        p.sendMessage(Msg.say("Le diste «me gusta» al café. +1 estrella."), false);
        Sonidos.jugador(p, "megusta", 1f, 1f);
        return null;
    }

    /** Invita a otro jugador conectado a tu café (solo el dueño). */
    static String invitar(Cafe c, ServerPlayerEntity host, ServerPlayerEntity target) {
        if (!c.esDuenio(host)) return "Solo el dueño del café puede invitar.";
        if (c.construyendo) return "Estás construyendo: termina antes de invitar.";
        if (target == host) return "No puedes invitarte a ti mismo.";
        if (c.trabajadores() >= MAX_JUGADORES) return "Tu café está lleno (máximo " + MAX_JUGADORES + " personas).";
        if (!invitable(target)) return target.getGameProfile().getName() + " no puede venir ahora (está en un café abierto, con invitados o en otro minijuego).";
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
        if (c.trabajadores() >= MAX_JUGADORES) return "El café está lleno.";
        if (!invitable(guest)) return "Ahora no puedes aceptar: cierra tu café (y despide a tus invitados) o sal del otro minijuego.";
        INVITACIONES.remove(guest.getUuid());
        Cafe propio = POR_JUGADOR.get(guest.getUuid());
        if (propio != null) {                                                       // estaba paseando por la ciudad con su cafe cerrado: lo deja y se va a cocinar al del amigo
            POR_JUGADOR.remove(guest.getUuid());
            DUENIOS.remove(guest.getUuid());
            propio.cerrar();
            Menus.cerrar(guest);
        }
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
            c.quitarJugador(p.getUuid());
        }
        return Saved.restore(s, p);
    }

    /** Un jugador que estaba en un cafe se fue por otro medio (por ejemplo otro minijuego): se le quita del cafe sin tocar lo que guardamos de el. Si el dueño se va, se cierra el cafe. */
    static void sinRestaurar(ServerPlayerEntity p) {
        Cafe c = POR_JUGADOR.remove(p.getUuid());
        if (c == null) return;
        c.quitarJugador(p.getUuid());
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
            c.quitarJugador(p.getUuid());
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
