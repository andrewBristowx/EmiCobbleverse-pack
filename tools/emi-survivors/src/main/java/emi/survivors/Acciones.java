package emi.survivors;

import emi.survivors.net.AccionPayload;
import net.minecraft.server.network.ServerPlayerEntity;

/** Lo que el servidor hace cuando el cliente manda una accion desde una pantalla (carta elegida, jugar, comprar...). Siempre se comprueba todo: el cliente no es de fiar. */
final class Acciones {
    private Acciones() {}

    static void recibir(ServerPlayerEntity p, AccionPayload a) {
        try {
            switch (a.accion()) {
                case "carta" -> {
                    Run r = Partida.de(p.getUuid());
                    if (r != null) r.elegirCarta(Integer.parseInt(a.a().trim()));
                }
                default -> EmiSurvivors.LOG.debug("Accion desconocida de {}: {}", p.getGameProfile().getName(), a.accion());
            }
        } catch (NumberFormatException e) {
            // un numero mal escrito: se ignora
        } catch (Throwable t) {
            EmiSurvivors.LOG.error("Error al atender la accion {} de {}", a.accion(), p.getGameProfile().getName(), t);
        }
    }
}
