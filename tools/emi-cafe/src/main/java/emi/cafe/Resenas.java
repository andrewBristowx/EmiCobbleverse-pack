package emi.cafe;

import java.util.List;
import java.util.Random;

/**
 * Resenas de los clientes (0.33.0): al irse, cada cliente deja de 1 a 5 estrellas segun lo que espero y si le sirvieron su favorito. La valoracion media de las ultimas
 * {@link #MAX} resenas se ve en el menu y, con suficientes resenas, mueve la llegada de clientes: muy buena atrae mas gente, mala la espanta. Sin Minecraft.
 */
final class Resenas {
    private Resenas() {}

    static final int MAX = 40;
    /** Con menos resenas que esto la valoracion todavia no cambia nada. */
    static final int MIN_EFECTO = 10;

    /** Una resena guardada (Gson). */
    static final class Resena {
        String esp = "";
        int est;
        String plato = "";
        String texto = "";
        long dia;                        // dia (epoch) en que se escribio
    }

    /** Las estrellas por servir: pronto = 5, bien = 4, justo = 3; el plato favorito sube una. */
    static int estrellas(double pacienciaRestante, boolean favorito) {
        int e = pacienciaRestante > 0.7 ? 5 : pacienciaRestante > 0.4 ? 4 : 3;
        return favorito ? Math.min(5, e + 1) : e;
    }

    static final int ENFADADO = 1;

    private static final String[][] TEXTOS = {
        {},
        {"Esperé tanto que me fui con hambre.", "Nadie me atendió a tiempo. No vuelvo.", "Ni un plato en la mesa... ¡qué decepción!"},
        {"{p}: llegó frío y tarde.", "Demasiada espera para pedir {p}.", "Podría estar mejor: tardaron mucho."},
        {"{p}: bien, pero esperé bastante.", "Correcto. {p} cumple.", "Buen sitio; si fueran más rápidos, genial."},
        {"Muy rico: {p}. Volveré.", "Buen servicio y {p} sabroso.", "{p}: casi perfecto."},
        {"¡{p}: de otro mundo!", "Servicio rapidísimo y {p} delicioso.", "El mejor café de la calle: pedí {p}.", "¡Volveré con mis amigos!"}
    };

    static String texto(int est, String plato, Random rnd) {
        String[] t = TEXTOS[Math.max(1, Math.min(5, est))];
        return t[rnd.nextInt(t.length)].replace("{p}", plato == null || plato.isEmpty() ? "El plato" : plato);
    }

    /** Anade una resena al principio y recorta la lista a {@link #MAX}. */
    static void anadir(List<Resena> lista, Resena r) {
        lista.add(0, r);
        while (lista.size() > MAX) lista.remove(lista.size() - 1);
    }

    /** La media de estrellas (0 si no hay resenas). */
    static double media(List<Resena> lista) {
        if (lista == null || lista.isEmpty()) return 0;
        int s = 0;
        for (Resena r : lista) s += r.est;
        return s / (double) lista.size();
    }

    /** Cuanto cambia el tiempo entre clientes: <1 = llegan antes. Solo cuenta con {@link #MIN_EFECTO} resenas o mas. */
    static double factorLlegada(List<Resena> lista) {
        if (lista == null || lista.size() < MIN_EFECTO) return 1.0;
        double m = media(lista);
        return m >= 4.5 ? 0.92 : m >= 4.0 ? 0.97 : m < 3.0 ? 1.10 : 1.0;
    }

    static String efectoTexto(List<Resena> lista) {
        if (lista == null || lista.size() < MIN_EFECTO) return "Con " + MIN_EFECTO + " reseñas la valoración empieza a atraer (o espantar) clientes.";
        double f = factorLlegada(lista);
        return f < 0.95 ? "¡Tu fama atrae gente! Los clientes llegan un " + Math.round((1 - f) * 100) + " % antes."
            : f < 1.0 ? "Buena fama: los clientes llegan un " + Math.round((1 - f) * 100) + " % antes."
            : f > 1.0 ? "Mala fama: los clientes tardan un " + Math.round((f - 1) * 100) + " % más en llegar."
            : "Valoración normal: no cambia la llegada de clientes.";
    }
}
