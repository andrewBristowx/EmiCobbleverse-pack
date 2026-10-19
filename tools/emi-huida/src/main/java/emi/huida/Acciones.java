package emi.huida;

import emi.huida.net.AccionPayload;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Lo que el servidor hace cuando el cliente manda una accion desde el menu de la sala (abrir una huida, unirse, empezar, comprar...). Siempre se comprueba todo: el cliente no es de fiar. */
final class Acciones {
    private Acciones() {}

    private static final Map<UUID, Long> ULTIMO_REFRESCO = new HashMap<>();

    /** Cuenta el resultado de una accion (el motivo, si no se pudo) y refresca el menu de quien la pidio. */
    private static void respuesta(ServerPlayerEntity p, String error) {
        if (error != null) p.sendMessage(Msg.err(error), false);
        if (Sala.enSala(p) && !Huida.participa(p)) Menu.actualizar(p);
    }

    static void recibir(ServerPlayerEntity p, AccionPayload a) {
        try {
            if (Huida.srv() == null || Huida.datos == null) return;
            switch (a.accion()) {
                case "refrescar" -> {
                    long ahora = System.currentTimeMillis();
                    Long antes = ULTIMO_REFRESCO.put(p.getUuid(), ahora);
                    if (antes != null && ahora - antes < 400) return;
                    if (Sala.enSala(p) && !Huida.participa(p)) Menu.actualizar(p);
                }
                case "menu" -> { if (Sala.enSala(p) && !Huida.participa(p)) Menu.abrir(p, a.a().isEmpty() ? "jugar" : a.a()); }
                case "crear" -> respuesta(p, Sala.enSala(p) ? Huida.crearDesdeSala(p, a.a(), false) : "Solo desde la sala de la huida.");
                case "solo" -> {
                    String err = Sala.enSala(p) ? Huida.crearDesdeSala(p, a.a(), true) : "Solo desde la sala de la huida.";
                    if (err != null) respuesta(p, err); else Menu.cerrar(p);
                }
                case "unirse" -> respuesta(p, Sala.enSala(p) ? Huida.unirse(p) : "Solo desde la sala de la huida.");
                case "salirCola" -> respuesta(p, Huida.jug.containsKey(p.getUuid()) ? Huida.salir(p) : null);
                case "lanzar" -> {
                    String err = Huida.empezarDesdeSala(p, Perm.admin(p.getCommandSource()));
                    if (err != null) respuesta(p, err); else Menu.cerrar(p);
                }
                case "cancelar" -> respuesta(p, Huida.cancelarDesdeSala(p, Perm.admin(p.getCommandSource())));
                case "tienda_comprar" -> {
                    if (!Sala.enSala(p) || Huida.participa(p)) return;
                    Datos.Jugador j = Huida.datos.jugador(p.getUuid(), p.getGameProfile().getName());
                    String err = Tienda.comprar(j, a.a(), System.currentTimeMillis(), java.time.ZoneId.systemDefault(), art -> TiendaCorreo.enviar(p.getServer(), p.getUuid(), art));
                    if (err != null) p.sendMessage(Msg.err(err), false);
                    else {
                        Huida.datos.guardar(p.getUuid());
                        Tienda.Articulo art = Tienda.articulo(a.a());
                        p.sendMessage(Msg.say("Comprado: " + art.nombre() + " (" + art.precio() + " estrellas). Lo tienes en /correo."), false);
                        Msg.sound(p, net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9f, 1.2f);
                    }
                    Menu.abrir(p, "tienda");
                }
                case "salir" -> { String err = Sala.salir(p); if (err != null) p.sendMessage(Msg.err(err), false); }
                default -> EmiHuida.LOG.debug("Accion desconocida de {}: {}", p.getGameProfile().getName(), a.accion());
            }
        } catch (IllegalArgumentException e) {
            // un numero mal escrito: se ignora
        } catch (Throwable t) {
            EmiHuida.LOG.error("Error al atender la accion {} de {}", a.accion(), p.getGameProfile().getName(), t);
        }
    }
}
