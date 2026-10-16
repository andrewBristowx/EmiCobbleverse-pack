package emi.survivors;

import emi.survivors.net.AccionPayload;
import net.minecraft.server.network.ServerPlayerEntity;

/** Lo que el servidor hace cuando el cliente manda una accion desde una pantalla (carta elegida, jugar, comprar...). Siempre se comprueba todo: el cliente no es de fiar. */
final class Acciones {
    private Acciones() {}

    /** Sube una mejora permanente del Pokemon con ese UUID (que tiene que estar en el equipo del jugador). Devuelve el motivo si no se puede. */
    private static String comprar(ServerPlayerEntity p, String pokeId, String mejoraId) {
        if (Partida.datos == null) return "El minijuego aun no esta listo.";
        var party = com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(p);
        for (int i = 0; i < 6; i++) {
            var pk = party.get(i);
            if (pk != null && pk.getUuid().toString().equals(pokeId))
                return Partida.datos.comprar(p.getUuid(), p.getGameProfile().getName(), pokeId, pk.getSpecies().getResourceIdentifier().getPath(), mejoraId);
        }
        return "Ese Pokémon ya no está en tu equipo.";
    }

    static void recibir(ServerPlayerEntity p, AccionPayload a) {
        try {
            switch (a.accion()) {
                case "carta" -> {
                    Run r = Partida.de(p.getUuid());
                    if (r != null) r.elegirCarta(Integer.parseInt(a.a().trim()));
                }
                case "jugar" -> {
                    if (Partida.participa(p)) { Menu.cerrar(p); return; }                  // doble clic: la partida ya esta en marcha
                    String err = Partida.iniciar(p, Integer.parseInt(a.a().trim()), 0, false);
                    if (err != null) { p.sendMessage(Msg.err(err), false); Menu.enviar(p, "jugar", null); }
                    else Menu.cerrar(p);
                }
                case "comprar" -> {
                    if (!Mundo.enSala(p) || Partida.participa(p)) return;
                    String err = comprar(p, a.a(), a.b());
                    if (err != null) p.sendMessage(Msg.err(err), false);
                    Menu.enviar(p, "mejoras", null);
                }
                case "menu" -> { if (Mundo.enSala(p) && !Partida.participa(p)) Menu.enviar(p, a.a().isEmpty() ? "jugar" : a.a(), null); }
                case "salir" -> { String err = Mundo.salir(p); if (err != null) p.sendMessage(Msg.err(err), false); }
                case "grupo" -> p.sendMessage(Msg.say("Los grupos de hasta 4 jugadores llegan en la próxima fase. ¡Muy pronto!"), false);
                default -> EmiSurvivors.LOG.debug("Accion desconocida de {}: {}", p.getGameProfile().getName(), a.accion());
            }
        } catch (NumberFormatException e) {
            // un numero mal escrito: se ignora
        } catch (Throwable t) {
            EmiSurvivors.LOG.error("Error al atender la accion {} de {}", a.accion(), p.getGameProfile().getName(), t);
        }
    }
}
