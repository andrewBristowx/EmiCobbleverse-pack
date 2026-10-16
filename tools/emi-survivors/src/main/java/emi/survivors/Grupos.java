package emi.survivors;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Los grupos de la sala: hasta 4 jugadores que se juntan para jugar a la vez en la misma arena. Uno es el lider (el que crea el grupo; si se va, pasa al siguiente);
 * cada uno elige su Pokemon y se marca «listo»; el lider empieza cuando estan todos listos. Al acabar la partida el grupo sigue junto en la sala. Logica pura
 * (sin Minecraft: solo uuid y nombres), asi se prueba con un main; las acciones (mensajes, menus, empezar la partida) las hace {@code Acciones}.
 * Los metodos devuelven el motivo si no se puede hacer algo, o null si se hizo.
 */
final class Grupos {
    static final int MAX = 4;

    static final class Miembro {
        final UUID id;
        String nombre;
        boolean listo;
        int hueco;                    // hueco (1..6) del equipo de Cobblemon con el Pokemon que lleva (solo si esta listo)
        String especie = "";          // lo que ven los demas de ese Pokemon
        int nivel;

        Miembro(UUID id, String nombre) { this.id = id; this.nombre = nombre; }
    }

    static final class Grupo {
        UUID lider;
        final List<Miembro> miembros = new ArrayList<>();
        /** Esta jugando una partida ahora mismo (no se puede entrar ni cambiar nada). */
        boolean enPartida;
        final long creado = System.currentTimeMillis();

        Miembro de(UUID id) { for (Miembro m : miembros) if (m.id.equals(id)) return m; return null; }
        boolean completo() { return miembros.size() >= MAX; }
        boolean todosListos() { for (Miembro m : miembros) if (!m.listo) return false; return !miembros.isEmpty(); }
        Miembro lider() { return de(lider); }

        /** Los nombres de los que faltan por estar listos, separados por comas. */
        String faltan() {
            StringBuilder sb = new StringBuilder();
            for (Miembro m : miembros) if (!m.listo) { if (sb.length() > 0) sb.append(", "); sb.append(m.nombre); }
            return sb.toString();
        }
    }

    private final Map<UUID, Grupo> deJugador = new LinkedHashMap<>();
    private final List<Grupo> todos = new ArrayList<>();

    Grupo de(UUID id) { return deJugador.get(id); }

    /** Los grupos a los que se puede unir uno (con sitio y sin estar jugando), el mas antiguo primero. */
    List<Grupo> abiertos() {
        List<Grupo> l = new ArrayList<>();
        for (Grupo g : todos) if (!g.completo() && !g.enPartida) l.add(g);
        return l;
    }

    List<Grupo> todos() { return new ArrayList<>(todos); }

    String crear(UUID id, String nombre) {
        if (deJugador.containsKey(id)) return "Ya estás en un grupo. Sal de él para crear otro.";
        Grupo g = new Grupo();
        g.lider = id;
        g.miembros.add(new Miembro(id, nombre));
        deJugador.put(id, g);
        todos.add(g);
        return null;
    }

    String unir(UUID id, String nombre, UUID lider) {
        if (deJugador.containsKey(id)) return "Ya estás en un grupo. Sal de él para unirte a otro.";
        Grupo g = deJugador.get(lider);
        if (g == null) return "Ese grupo ya no existe.";
        if (g.enPartida) return "Ese grupo está jugando ahora mismo.";
        if (g.completo()) return "Ese grupo está lleno (" + MAX + " jugadores).";
        g.miembros.add(new Miembro(id, nombre));
        deJugador.put(id, g);
        return null;
    }

    /** Un jugador deja su grupo (si era el lider, pasa al que lleva mas tiempo; si no queda nadie, el grupo desaparece). */
    String salir(UUID id) {
        Grupo g = deJugador.remove(id);
        if (g == null) return "No estás en ningún grupo.";
        g.miembros.removeIf(m -> m.id.equals(id));
        if (g.miembros.isEmpty()) todos.remove(g);
        else if (g.lider.equals(id)) g.lider = g.miembros.get(0).id;
        return null;
    }

    String expulsar(UUID lider, UUID objetivo) {
        Grupo g = deJugador.get(lider);
        if (g == null) return "No estás en ningún grupo.";
        if (!g.lider.equals(lider)) return "Solo el líder puede expulsar.";
        if (lider.equals(objetivo)) return "No puedes expulsarte a ti: usa «salir del grupo».";
        if (g.enPartida) return "El grupo está jugando ahora mismo.";
        if (g.de(objetivo) == null) return "Ese jugador no está en tu grupo.";
        return salir(objetivo);
    }

    /** Se marca «listo» con el Pokemon del hueco {@code hueco}. */
    String listo(UUID id, int hueco, String especie, int nivel) {
        Grupo g = deJugador.get(id);
        if (g == null) return "No estás en ningún grupo.";
        if (g.enPartida) return "El grupo está jugando ahora mismo.";
        Miembro m = g.de(id);
        m.listo = true; m.hueco = hueco; m.especie = especie; m.nivel = nivel;
        return null;
    }

    String noListo(UUID id) {
        Grupo g = deJugador.get(id);
        if (g == null) return "No estás en ningún grupo.";
        if (g.enPartida) return "El grupo está jugando ahora mismo.";
        Miembro m = g.de(id);
        m.listo = false; m.especie = ""; m.nivel = 0;
        return null;
    }

    /** El grupo empieza una partida. */
    void alEmpezar(Grupo g) { g.enPartida = true; }

    /** La partida del grupo ha acabado: siguen juntos, pero nadie esta listo. */
    void alTerminar(Grupo g) {
        g.enPartida = false;
        for (Miembro m : g.miembros) { m.listo = false; m.especie = ""; m.nivel = 0; }
    }

    void vaciar() { deJugador.clear(); todos.clear(); }
}
