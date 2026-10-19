package emi.fall;

import emi.fall.net.AccionPayload;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Lo que el servidor hace cuando el cliente manda una accion desde el menu de la sala (abrir un torneo, unirse, empezar, comprar...). Siempre se comprueba todo: el cliente no es de fiar. */
final class Acciones {
    private Acciones() {}

    private static final Map<UUID, Long> ULTIMO_REFRESCO = new HashMap<>();

    private static boolean libre(ServerPlayerEntity p) { return Sala.enSala(p) && !Torneo.participa(p); }

    /** Cuenta el resultado de una accion (el motivo, si no se pudo) y refresca el menu de quien la pidio. */
    private static void respuesta(ServerPlayerEntity p, String error) {
        if (error != null) p.sendMessage(Msg.err(error), false);
        if (libre(p)) Menu.actualizar(p);
    }

    static void recibir(ServerPlayerEntity p, AccionPayload a) {
        try {
            if (EmiFall.datos == null) return;
            switch (a.accion()) {
                case "refrescar" -> {
                    long ahora = System.currentTimeMillis();
                    Long antes = ULTIMO_REFRESCO.put(p.getUuid(), ahora);
                    if (antes != null && ahora - antes < 400) return;
                    if (libre(p)) Menu.actualizar(p);
                }
                case "menu" -> { if (libre(p)) Menu.abrir(p, a.a().isEmpty() ? "jugar" : a.a()); }
                case "crear" -> respuesta(p, libre(p) ? Torneo.abrirDesdeMenu(p, a.a(), a.b().equals("sin")) : "Solo desde la sala de Emi Fall.");
                case "config" -> respuesta(p, libre(p) ? Torneo.configurar(p, a.a(), a.b().equals("sin")) : "Solo desde la sala de Emi Fall.");
                case "solo" -> {
                    String err = libre(p) ? Torneo.soloDesdeMenu(p, a.a(), a.b().equals("sin")) : "Solo desde la sala de Emi Fall.";
                    if (err != null) respuesta(p, err); else Menu.cerrar(p);
                }
                case "unirse" -> {
                    String err = libre(p) ? Torneo.unirse(p) : "Solo desde la sala de Emi Fall.";
                    respuesta(p, err);
                }
                case "salirCola" -> {
                    if (Torneo.apuntado(p.getUuid()) && !Torneo.participa(p)) Torneo.dejar(p);
                    respuesta(p, null);
                }
                case "lanzar" -> {
                    String err = Torneo.empezarDesdeMenu(p, Perm.admin(p.getCommandSource()));
                    if (err != null) respuesta(p, err); else Menu.cerrar(p);
                }
                case "cancelar" -> respuesta(p, Torneo.cancelarDesdeMenu(p, Perm.admin(p.getCommandSource())));
                case "tienda_comprar" -> {
                    if (!libre(p)) return;
                    Datos.Jugador j = EmiFall.datos.jugador(p.getUuid(), p.getGameProfile().getName());
                    String err = Tienda.comprar(j, a.a(), System.currentTimeMillis(), java.time.ZoneId.systemDefault(), art -> TiendaCorreo.enviar(p.getServer(), p.getUuid(), art));
                    if (err != null) p.sendMessage(Msg.err(err), false);
                    else {
                        EmiFall.datos.guardar(p.getUuid());
                        Tienda.Articulo art = Tienda.articulo(a.a());
                        p.sendMessage(Msg.say("Comprado: " + art.nombre() + " (" + art.precio() + " estrellas). Lo tienes en /correo."), false);
                        Sonido.sfx(p, "puntos", 0.9f, 1.2f);
                    }
                    Menu.abrir(p, "tienda");
                }
                case "salir" -> {
                    if (Torneo.apuntado(p.getUuid()) && !Torneo.participa(p)) Torneo.dejar(p);
                    if (libre(p) && !Sala.salir(p.getServer(), p)) p.sendMessage(Msg.err("No encuentro lo que guardé de ti."), false);
                    else Menu.cerrar(p);
                }
                default -> EmiFall.LOG.debug("Accion desconocida de {}: {}", p.getGameProfile().getName(), a.accion());
            }
        } catch (Throwable t) {
            EmiFall.LOG.error("Error al atender la accion {} de {}", a.accion(), p.getGameProfile().getName(), t);
        }
    }
}
