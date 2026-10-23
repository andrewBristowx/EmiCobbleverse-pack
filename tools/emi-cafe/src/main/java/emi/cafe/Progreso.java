package emi.cafe;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.ToLongFunction;

/**
 * Estrellas, misiones (3 del dia y 2 de la semana, las mismas para todos) y logros con escalones. Las estrellas son la moneda de la tienda del servidor
 * (tickets y objetos por el correo + el peluche del Michialien chef). Todo lo premia al momento, sin reclamar. Las cifras son una primera estimacion del asistente.
 */
final class Progreso {
    private Progreso() {}

    // ------------------------------------------------------------ estrellas

    /** Suma estrellas al jugador y las apunta en el resumen del turno (cafe puede ser null). */
    static void dar(Cafe cafe, Datos.Jugador j, java.util.UUID id, int n) {
        if (n <= 0) return;
        j.estrellas += n; j.estrellasTotal += n;
        if (cafe != null) cafe.estrellasTurno.merge(id, n, Integer::sum);
    }

    // ------------------------------------------------------------ misiones

    /** {@code evento}: servidos, ganado, propinas, turnos, sin_enfado, turno_max (progreso = el maximo) o lavados. */
    record Mision(String id, String texto, String evento, int objetivo, int premio) {}

    static final List<Mision> DIA = List.of(
            new Mision("d_servir", "Sirve 8 clientes", "servidos", 8, 10),
            new Mision("d_turno", "Gana 300 monedas en una sola jornada", "turno_max", 300, 12),
            new Mision("d_turnos", "Cierra el café 2 veces habiendo servido", "turnos", 2, 8),
            new Mision("d_sinenfado", "Cierra una jornada sin que se enfade nadie", "sin_enfado", 1, 12),
            new Mision("d_propina", "Consigue 40 monedas de propina", "propinas", 40, 10),
            new Mision("d_lavar", "Lava 6 platos", "lavados", 6, 8));
    static final List<Mision> SEMANA = List.of(
            new Mision("s_servir", "Sirve 40 clientes", "servidos", 40, 40),
            new Mision("s_turnos", "Cierra el café 8 veces habiendo servido", "turnos", 8, 35),
            new Mision("s_ganado", "Gana 800 monedas", "ganado", 800, 40),
            new Mision("s_sinenfado", "Cierra 3 jornadas sin que se enfade nadie", "sin_enfado", 3, 40));

    static String claveSemana(LocalDate d) { return d.get(IsoFields.WEEK_BASED_YEAR) + "-W" + d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR); }

    /** Al cambiar el dia o la semana se borra lo de las misiones de ese periodo. */
    static void refrescar(Datos.Jugador j) {
        LocalDate hoy = LocalDate.now(ZoneId.systemDefault());
        String d = hoy.toString(), s = claveSemana(hoy);
        if (!d.equals(j.misDia)) { j.misDia = d; borrar(j, "d_"); }
        if (!s.equals(j.misSemana)) { j.misSemana = s; borrar(j, "s_"); }
    }

    private static void borrar(Datos.Jugador j, String prefijo) {
        j.misProg.keySet().removeIf(k -> k.startsWith(prefijo));
        j.misHechas.removeIf(k -> k.startsWith(prefijo));
    }

    private static List<Mision> elegir(List<Mision> pool, int n, String semilla) {
        List<Mision> l = new ArrayList<>(pool);
        java.util.Collections.shuffle(l, new Random(semilla.hashCode()));
        return l.subList(0, Math.min(n, l.size()));
    }

    static List<Mision> deHoy(Datos.Jugador j) { refrescar(j); return elegir(DIA, 3, "dia" + j.misDia); }
    static List<Mision> deSemana(Datos.Jugador j) { refrescar(j); return elegir(SEMANA, 2, "sem" + j.misSemana); }

    /** Ocurrio algo: suma {@code n} a las misiones activas que lo cuenten (o guarda el maximo) y paga las que se completan. */
    static void evento(Cafe cafe, ServerPlayerEntity p, Datos.Jugador j, String ev, int n) {
        List<Mision> activas = new ArrayList<>(deHoy(j)); activas.addAll(deSemana(j));
        for (Mision m : activas) {
            if (!m.evento().equals(ev) || j.misHechas.contains(m.id())) continue;
            int ya = j.misProg.getOrDefault(m.id(), 0);
            int nuevo = ev.equals("turno_max") ? Math.max(ya, n) : ya + n;
            j.misProg.put(m.id(), Math.min(nuevo, m.objetivo()));
            if (nuevo >= m.objetivo()) {
                j.misHechas.add(m.id());
                dar(cafe, j, p.getUuid(), m.premio());
                p.sendMessage(Msg.say("¡Misión cumplida: " + m.texto() + "! +" + m.premio() + " estrellas."), false);
                Msg.sound(p, net.minecraft.sound.SoundEvents.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
            }
        }
    }

    // ------------------------------------------------------------ logros

    /** Una familia de logros: {@code metas[k]} es lo que hay que alcanzar para el escalon k y {@code premios[k]} las estrellas que da. */
    record Logro(String id, String nombre, String descripcion, int[] metas, int[] premios, ToLongFunction<Datos.Jugador> valor) {}

    static final List<Logro> LOGROS = List.of(
            new Logro("clientes", "Camarero", "clientes servidos", new int[]{10, 50, 200, 1000}, new int[]{5, 15, 40, 100}, j -> j.clientes),
            new Logro("turnos", "Jornada completa", "jornadas cerradas", new int[]{3, 10, 30, 100}, new int[]{5, 15, 40, 100}, j -> j.turnos),
            new Logro("ganancias", "Pequeño empresario", "monedas ganadas en total", new int[]{200, 1000, 5000, 20000}, new int[]{5, 15, 40, 100}, j -> j.monedasTotal),
            new Logro("mejor", "Jornada de récord", "monedas en una sola jornada", new int[]{300, 800, 2000, 5000}, new int[]{5, 15, 40, 100}, j -> j.mejorTurno),
            new Logro("propinas", "Encantador", "monedas de propina", new int[]{50, 300, 1500}, new int[]{5, 20, 60}, j -> j.propinasTotal),
            new Logro("sinenfados", "Todos contentos", "jornadas sin que se enfade nadie", new int[]{1, 5, 20}, new int[]{10, 30, 80}, j -> j.sinEnfados),
            new Logro("platos", "Chef variado", "platos aprendidos", new int[]{2, 4, 7, 11}, new int[]{10, 20, 40, 80}, j -> j.platos.size()),
            new Logro("mejoras", "Local de lujo", "niveles de mejora comprados", new int[]{1, 4, 8, 12}, new int[]{5, 15, 30, 50}, j -> j.mejoras.values().stream().mapToInt(Integer::intValue).sum()));

    /** Cobra los escalones de logros que se acaban de alcanzar. */
    static void revisarLogros(Cafe cafe, ServerPlayerEntity p, Datos.Jugador j) {
        for (Logro l : LOGROS) {
            int cobrado = j.logros.getOrDefault(l.id(), 0);
            long v = l.valor().applyAsLong(j);
            while (cobrado < l.metas().length && v >= l.metas()[cobrado]) {
                int premio = l.premios()[cobrado];
                cobrado++;
                dar(cafe, j, p.getUuid(), premio);
                p.sendMessage(Msg.say("¡Logro «" + l.nombre() + "» " + cobrado + "/" + l.metas().length + "! (" + l.metas()[cobrado - 1] + " " + l.descripcion() + ") +" + premio + " estrellas."), false);
                Msg.sound(p, net.minecraft.sound.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }
            j.logros.put(l.id(), cobrado);
        }
    }
}
