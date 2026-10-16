package emi.survivors;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lo que se hace con los grupos de la sala (los datos y las reglas estan en {@link Grupos}): comprobar que quien pide algo esta en la sala, avisar por el chat a los
 * del grupo, refrescar sus menus y, cuando el lider empieza, llevar a todos a la misma arena ({@link Partida#iniciarEquipo}).
 * Cada metodo devuelve el motivo si no se puede, o null si se hizo.
 */
final class GrupoCtl {
    private GrupoCtl() {}

    private static MinecraftServer server() { return Partida.server; }

    private static ServerPlayerEntity jugador(UUID id) { return server() == null ? null : server().getPlayerManager().getPlayer(id); }

    private static String enSala(ServerPlayerEntity p) {
        if (!Mundo.enSala(p)) return "Los grupos se manejan desde la sala del minijuego (/survivor entrar).";
        if (Partida.participa(p)) return "Estás en una partida.";
        return null;
    }

    // ------------------------------------------------------------------------------------------------ avisos

    /** Manda un mensaje por el chat a todos los del grupo (menos a {@code salvo}, si se da). */
    static void avisar(Grupos.Grupo g, String texto, UUID salvo) {
        for (Grupos.Miembro m : new ArrayList<>(g.miembros)) {
            if (m.id.equals(salvo)) continue;
            ServerPlayerEntity p = jugador(m.id);
            if (p != null) p.sendMessage(Msg.say(texto), false);
        }
    }

    /** Vuelve a mandar los datos del menu a los del grupo que lo tengan abierto (los demas no se enteran: la pantalla solo se actualiza si esta abierta). */
    static void refrescar(Grupos.Grupo g) {
        if (g == null) return;
        for (Grupos.Miembro m : new ArrayList<>(g.miembros)) {
            ServerPlayerEntity p = jugador(m.id);
            if (p != null && Mundo.enSala(p) && !Partida.participa(p)) Menu.actualizar(p);
        }
    }

    // ------------------------------------------------------------------------------------------------ acciones

    static String crear(ServerPlayerEntity p) {
        String e = enSala(p);
        if (e != null) return e;
        e = Partida.grupos.crear(p.getUuid(), p.getGameProfile().getName());
        if (e != null) return e;
        p.sendMessage(Msg.say("Has creado un grupo. Tus amigos pueden unirse desde la pestaña GRUPO del menú (o con /survivor grupo unirse " + p.getGameProfile().getName() + ")."), false);
        refrescar(Partida.grupos.de(p.getUuid()));
        return null;
    }

    static String unir(ServerPlayerEntity p, UUID lider) {
        String e = enSala(p);
        if (e != null) return e;
        e = Partida.grupos.unir(p.getUuid(), p.getGameProfile().getName(), lider);
        if (e != null) return e;
        Grupos.Grupo g = Partida.grupos.de(p.getUuid());
        avisar(g, p.getGameProfile().getName() + " se ha unido al grupo (" + g.miembros.size() + "/" + Grupos.MAX + ").", null);
        refrescar(g);
        return null;
    }

    /** Deja el grupo (si estaba en uno). Se llama tambien al salir de la sala o al desconectarse, sin avisar al propio jugador. */
    static String salir(ServerPlayerEntity p) { return salir(p.getUuid(), p.getGameProfile().getName(), true); }

    static String salir(UUID id, String nombre, boolean avisarAlJugador) {
        Grupos.Grupo g = Partida.grupos.de(id);
        if (g == null) return "No estás en ningún grupo.";
        boolean eraLider = g.lider.equals(id);
        String e = Partida.grupos.salir(id);
        if (e != null) return e;
        ServerPlayerEntity p = jugador(id);
        if (avisarAlJugador && p != null) p.sendMessage(Msg.say("Has salido del grupo."), false);
        if (!g.miembros.isEmpty()) {
            avisar(g, nombre + " ha salido del grupo." + (eraLider ? " Ahora el líder es " + g.lider().nombre + "." : ""), null);
            refrescar(g);
        }
        if (avisarAlJugador && p != null && Mundo.enSala(p) && !Partida.participa(p)) Menu.actualizar(p);
        return null;
    }

    static String expulsar(ServerPlayerEntity lider, UUID objetivo) {
        Grupos.Grupo g = Partida.grupos.de(lider.getUuid());
        Grupos.Miembro m = g == null ? null : g.de(objetivo);
        String nombre = m == null ? "" : m.nombre;
        String e = Partida.grupos.expulsar(lider.getUuid(), objetivo);
        if (e != null) return e;
        ServerPlayerEntity p = jugador(objetivo);
        if (p != null) { p.sendMessage(Msg.say("El líder te ha expulsado del grupo."), false); if (Mundo.enSala(p) && !Partida.participa(p)) Menu.actualizar(p); }
        avisar(g, nombre + " ha sido expulsado del grupo.", null);
        refrescar(g);
        return null;
    }

    /** Se marca «listo» con el Pokemon del hueco {@code hueco} (1..6) de su equipo. */
    static String listo(ServerPlayerEntity p, int hueco) {
        String e = enSala(p);
        if (e != null) return e;
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(p);
        Pokemon pk = hueco >= 1 && hueco <= 6 ? party.get(hueco - 1) : null;
        if (pk == null) return "No tienes ningún Pokémon en ese hueco del equipo.";
        e = Partida.grupos.listo(p.getUuid(), hueco, pk.getSpecies().getName(), pk.getLevel());
        if (e != null) return e;
        Grupos.Grupo g = Partida.grupos.de(p.getUuid());
        avisar(g, p.getGameProfile().getName() + " está listo con " + pk.getSpecies().getName() + " (" + listos(g) + "/" + g.miembros.size() + ").", p.getUuid());
        refrescar(g);
        return null;
    }

    static String noListo(ServerPlayerEntity p) {
        String e = Partida.grupos.noListo(p.getUuid());
        if (e != null) return e;
        refrescar(Partida.grupos.de(p.getUuid()));
        return null;
    }

    private static int listos(Grupos.Grupo g) { int n = 0; for (Grupos.Miembro m : g.miembros) if (m.listo) n++; return n; }

    /** El lider elige la arena (el bioma) del grupo: se guarda en el grupo y se ve en el menu de todos. */
    static String elegirBioma(ServerPlayerEntity p, Biomas.Bioma bioma) {
        String e = enSala(p);
        if (e != null) return e;
        Grupos.Grupo g = Partida.grupos.de(p.getUuid());
        if (g == null) return "No estás en ningún grupo.";
        if (!g.lider.equals(p.getUuid())) return "Solo el líder elige la arena.";
        if (g.enPartida) return "El grupo ya está jugando.";
        if (bioma == null) return "Esa arena no existe.";
        if (Mundo.arena(bioma) == null) return "Esa arena no está disponible en este servidor.";
        g.bioma = bioma.id();
        refrescar(g);
        return null;
    }

    /** El lider empieza la partida del grupo con el Pokemon del hueco {@code hueco}, en la arena que haya elegido el grupo; hacen falta todos listos. */
    static String empezar(ServerPlayerEntity lider, int hueco) { return empezar(lider, hueco, null); }

    /** Igual, en la arena del bioma {@code bioma} (null = la que tenga elegida el grupo; si se da, el grupo se queda con ella). */
    static String empezar(ServerPlayerEntity lider, int hueco, Biomas.Bioma bioma) {
        String e = enSala(lider);
        if (e != null) return e;
        Grupos.Grupo g = Partida.grupos.de(lider.getUuid());
        if (g == null) return "No estás en ningún grupo.";
        if (!g.lider.equals(lider.getUuid())) return "Solo el líder puede empezar la partida.";
        if (g.enPartida) return "El grupo ya está jugando.";
        e = listo(lider, hueco);
        if (e != null) return e;
        if (!g.todosListos()) return "Faltan por estar listos: " + g.faltan() + ".";
        List<Partida.Candidato> cs = new ArrayList<>();
        for (Grupos.Miembro m : g.miembros) {
            ServerPlayerEntity p = jugador(m.id);
            if (p == null || !Mundo.enSala(p) || Partida.participa(p)) return m.nombre + " no está en la sala del minijuego.";
            cs.add(new Partida.Candidato(p, m.hueco));
        }
        if (bioma != null) g.bioma = bioma.id();
        e = Partida.iniciarEquipo(cs, 0, false, g, Biomas.deIdOPradera(g.bioma));
        if (e != null) { refrescar(g); return e; }
        return null;
    }
}
