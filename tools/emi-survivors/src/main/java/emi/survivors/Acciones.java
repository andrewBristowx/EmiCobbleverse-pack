package emi.survivors;

import emi.survivors.net.AccionPayload;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/** Lo que el servidor hace cuando el cliente manda una accion desde una pantalla (carta elegida, jugar, comprar...). Siempre se comprueba todo: el cliente no es de fiar. */
final class Acciones {
    private Acciones() {}

    private static final java.util.Map<UUID, Long> ULTIMO_REFRESCO = new java.util.HashMap<>();

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

    /** Cuenta el resultado de una accion del grupo (el motivo, si no se pudo) y refresca el menu de quien la pidio. */
    private static void respuesta(ServerPlayerEntity p, String error) {
        if (error != null) p.sendMessage(Msg.err(error), false);
        if (Mundo.enSala(p) && !Partida.participa(p)) Menu.actualizar(p);
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
                    if (Partida.grupos.de(p.getUuid()) != null) { p.sendMessage(Msg.err("Estás en un grupo: usa «¡LISTO!» o «¡EMPEZAR!», o sal del grupo para jugar solo."), false); Menu.enviar(p, "jugar", null); return; }
                    String err = Partida.iniciar(p, Integer.parseInt(a.a().trim()), 0, false, Biomas.deIdOPradera(a.b()));
                    if (err != null) { p.sendMessage(Msg.err(err), false); Menu.enviar(p, "jugar", null); }
                    else Menu.cerrar(p);
                }
                case "grupo_crear" -> respuesta(p, GrupoCtl.crear(p));
                case "grupo_unirse" -> respuesta(p, GrupoCtl.unir(p, UUID.fromString(a.a().trim())));
                case "grupo_salir" -> respuesta(p, GrupoCtl.salir(p));
                case "grupo_expulsar" -> respuesta(p, GrupoCtl.expulsar(p, UUID.fromString(a.a().trim())));
                case "grupo_listo" -> { int hueco = Integer.parseInt(a.a().trim()); respuesta(p, hueco <= 0 ? GrupoCtl.noListo(p) : GrupoCtl.listo(p, hueco)); }
                case "grupo_empezar" -> {
                    String err = GrupoCtl.empezar(p, Integer.parseInt(a.a().trim()));          // en la arena que haya elegido el lider (se guarda en el grupo)
                    if (err != null) respuesta(p, err);
                }
                case "bioma" -> respuesta(p, GrupoCtl.elegirBioma(p, Biomas.porId(a.a())));
                case "refrescar" -> {
                    long ahora = System.currentTimeMillis();
                    Long antes = ULTIMO_REFRESCO.put(p.getUuid(), ahora);
                    if (antes != null && ahora - antes < 400) return;                      // el cliente lo pide cada segundo y medio: mas deprisa no hace falta
                    if (Mundo.enSala(p) && !Partida.participa(p)) Menu.actualizar(p);
                }
                case "comprar" -> {
                    if (!Mundo.enSala(p) || Partida.participa(p)) return;
                    String err = comprar(p, a.a(), a.b());
                    if (err != null) p.sendMessage(Msg.err(err), false);
                    Menu.enviar(p, "mejoras", null);
                }
                case "menu" -> { if (Mundo.enSala(p) && !Partida.participa(p)) Menu.enviar(p, a.a().isEmpty() ? "jugar" : a.a(), null); }
                case "salir" -> { String err = Mundo.salir(p); if (err != null) p.sendMessage(Msg.err(err), false); }
                default -> EmiSurvivors.LOG.debug("Accion desconocida de {}: {}", p.getGameProfile().getName(), a.accion());
            }
        } catch (IllegalArgumentException e) {
            // un numero o un uuid mal escrito: se ignora
        } catch (Throwable t) {
            EmiSurvivors.LOG.error("Error al atender la accion {} de {}", a.accion(), p.getGameProfile().getName(), t);
        }
    }
}
