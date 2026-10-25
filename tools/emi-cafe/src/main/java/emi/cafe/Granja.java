package emi.cafe;

import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * La granja del cafe (0.24.0): huertos y corrales al fondo de la parcela que dan ingredientes de calidad. Aqui estan solo las reglas y las cuentas (sin mundo):
 * los cultivos y animales, el tiempo de crecimiento, lo que se cosecha, la despensa con calidad y el extra de dinero al servir un plato hecho con ingredientes de la granja.
 * Lo que se dibuja en el mundo y el michigranjero estan en {@link Fincas}.
 */
final class Granja {
    private Granja() {}

    enum Tipo { HUERTO, CORRAL }

    /**
     * Lo que se puede criar. {@code ingrediente} = el objeto del cafe que da (el de la caja de siempre: el mismo plato, pero con calidad); {@code ciclo} = segundos que tarda en estar listo
     * a velocidad normal; {@code bloques} = el cultivo que se ve crecer (el primero que exista en el juego: asi funciona con o sin Farmer's Delight); {@code especie} = el Pokemon del corral.
     */
    enum Cultivo {
        PAPAS("papas", "Huerto de papas", Tipo.HUERTO, CafeItems.PAPA, 420, 900, "Minecraft", "Las papas de siempre.", new String[]{"minecraft:potatoes"}, null),
        TRIGO("trigo", "Campo de trigo", Tipo.HUERTO, CafeItems.HARINA, 420, 900, "Minecraft", "Trigo molido: la harina del café.", new String[]{"minecraft:wheat"}, null),
        COL("col", "Huerto de col", Tipo.HUERTO, CafeItems.LECHUGA, 480, 1000, "Farmer's Delight", "Col de Farmer's Delight: hace de lechuga.", new String[]{"farmersdelight:cabbages", "minecraft:beetroots"}, null),
        TOMATES("tomates", "Huerto de tomates", Tipo.HUERTO, CafeItems.TOMATE, 540, 1200, "Farmer's Delight", "Tomates de Farmer's Delight.", new String[]{"farmersdelight:tomatoes", "minecraft:sweet_berry_bush"}, null),
        CEBOLLAS("cebollas", "Huerto de cebollas", Tipo.HUERTO, CafeItems.CEBOLLA, 480, 1000, "Farmer's Delight", "Cebollas de Farmer's Delight: platos nuevos.", new String[]{"farmersdelight:onions", "minecraft:carrots"}, null),
        CALABAZAS("calabazas", "Huerto de calabazas", Tipo.HUERTO, CafeItems.CALABAZA, 540, 1100, "Minecraft", "Calabazas para sopas y gratinados.", new String[]{"minecraft:pumpkin_stem"}, null),
        BAYAS("bayas", "Huerto de bayas", Tipo.HUERTO, CafeItems.BAYA, 480, 1000, "Cobblemon", "Bayas de Cobblemon: las bayas del café.", new String[]{"cobblemon:oran_berry", "minecraft:sweet_berry_bush"}, null),
        VACAS("vacas", "Corral de Miltank", Tipo.CORRAL, CafeItems.LECHE, 420, 1400, "Cobblemon", "Miltank da la leche del café.", new String[0], "miltank"),
        GALLINAS("gallinas", "Corral de Chansey", Tipo.CORRAL, CafeItems.HUEVO, 420, 1400, "Cobblemon", "Chansey pone los huevos.", new String[0], "chansey"),
        TAUROS("tauros", "Corral de Tauros", Tipo.CORRAL, CafeItems.CARNE, 600, 1800, "Cobblemon", "Tauros da la carne.", new String[0], "tauros"),
        ABEJAS("abejas", "Colmena de Combee", Tipo.CORRAL, CafeItems.MIEL, 600, 1600, "Cobblemon", "Combee hace la miel.", new String[0], "combee");

        final String id, nombre, fuente, texto, especie;
        final Tipo tipo;
        final Item ingrediente;
        final int ciclo, precio;
        final String[] bloques;

        Cultivo(String id, String nombre, Tipo tipo, Item ingrediente, int ciclo, int precio, String fuente, String texto, String[] bloques, String especie) {
            this.id = id; this.nombre = nombre; this.tipo = tipo; this.ingrediente = ingrediente; this.ciclo = ciclo; this.precio = precio; this.fuente = fuente; this.texto = texto;
            this.bloques = bloques; this.especie = especie;
        }

        static Cultivo de(String id) { for (Cultivo c : values()) if (c.id.equals(id)) return c; return null; }
    }

    // ------------------------------------------------------------ niveles y calidad

    static final int NIVEL_MAX = 5;
    static final int NIVEL_LOCAL = 5;                              // el local tiene que ser de nivel 5 para tener granja
    static final int MAX_FINCAS = 6;
    static final int CAPACIDAD = 120;                              // lo que cabe en la despensa de cada ingrediente (suma de todas las calidades)
    static final String[] CALIDAD = {"", "Fresco", "Selecto", "Superior", "Excelente", "Gourmet"};
    private static final int[] BONUS = {0, 6, 12, 20, 30, 45};     // % extra sobre la parte del plato que es ese ingrediente (calidad 1 a 5)

    /** Cuantos huertos y corrales caben segun el nivel del local: 2 en el nivel 5 y uno mas por cada nivel hasta 6 (nivel 9). */
    static int fincasMax(int nivelLocal) { return Math.max(0, Math.min(MAX_FINCAS, nivelLocal - 3)); }

    static int bonusPct(int calidad) { return BONUS[Math.max(0, Math.min(NIVEL_MAX, calidad))]; }

    /** Cuanto cuesta subir el huerto o corral del nivel {@code nivel} al siguiente (0 si ya esta al maximo). */
    static int costeMejora(Cultivo c, int nivel) {
        if (nivel >= NIVEL_MAX) return 0;
        double[] f = {0.8, 1.6, 3.0, 5.0};
        return (int) Math.round(c.precio * f[nivel - 1] / 10.0) * 10;
    }

    /** Animales de un corral: 2 + nivel. */
    static int animales(int nivel) { return 2 + nivel; }

    /** Unidades que da cada cosecha. */
    static int rendimiento(Cultivo c, int nivel) { return c.tipo == Tipo.HUERTO ? 16 + 4 * nivel : 3 * animales(nivel); }

    /** Cuanto falta para la cosecha, de 0 a 1 (1 = listo). */
    static double progreso(Datos.Finca f, long ahora) {
        Cultivo c = Cultivo.de(f.tipo);
        if (c == null) return 0;
        return Math.max(0, Math.min(1.0, (ahora - f.desde) / (c.ciclo * 1000.0)));
    }

    static long restanteSeg(Datos.Finca f, long ahora) {
        Cultivo c = Cultivo.de(f.tipo);
        if (c == null) return 0;
        return Math.max(0, (long) Math.ceil((f.desde + c.ciclo * 1000L - ahora) / 1000.0));
    }

    // ------------------------------------------------------------ la despensa (ingrediente con calidad)

    private static String clave(Item i, int calidad) { return CafeItems.id(i) + "@" + calidad; }

    static int cantidad(Datos.Jugador d, Item ing, int calidad) { return d.despensa.getOrDefault(clave(ing, calidad), 0); }

    /** Todo lo que hay de ese ingrediente, de cualquier calidad. */
    static int cantidad(Datos.Jugador d, Item ing) { int n = 0; for (int q = 1; q <= NIVEL_MAX; q++) n += cantidad(d, ing, q); return n; }

    /** Guarda lo cosechado; lo que no cabe se pierde. Devuelve lo que entro. */
    static int guardar(Datos.Jugador d, Item ing, int calidad, int n) {
        int q = Math.max(1, Math.min(NIVEL_MAX, calidad));
        int cabe = Math.max(0, CAPACIDAD - cantidad(d, ing));
        int entra = Math.min(n, cabe);
        if (entra > 0) d.despensa.merge(clave(ing, q), entra, Integer::sum);
        return entra;
    }

    /** La mejor calidad que hay de ese ingrediente (0 = nada). */
    static int mejorCalidad(Datos.Jugador d, Item ing) {
        for (int q = NIVEL_MAX; q >= 1; q--) if (cantidad(d, ing, q) > 0) return q;
        return 0;
    }

    /** Gasta una unidad de la mejor calidad que haya y devuelve cual era (0 = no habia). */
    static int gastar(Datos.Jugador d, Item ing) {
        int q = mejorCalidad(d, ing);
        if (q == 0) return 0;
        String k = clave(ing, q);
        int n = d.despensa.getOrDefault(k, 0) - 1;
        if (n <= 0) d.despensa.remove(k); else d.despensa.put(k, n);
        return q;
    }

    // ------------------------------------------------------------ el extra al servir

    /** Los ingredientes de un plato (los de sus cajas, sin repetir y en orden). */
    static List<Item> ingredientes(Recetas.Plato pl) {
        List<Item> l = new ArrayList<>();
        for (Estacion e : pl.estaciones) if (e.tipo == Estacion.Tipo.CAJA && e.ingrediente != null && !l.contains(e.ingrediente)) l.add(e.ingrediente);
        return l;
    }

    /** El resultado de gastar ingredientes de la granja al servir un plato: el extra (en fraccion del precio), cuantos ingredientes se usaron y la mejor calidad usada. */
    record Uso(double extra, int usados, int mejor, int total) {}

    /**
     * Al servir un plato se gasta una unidad de cada ingrediente que haya en la despensa (siempre la mejor calidad: el chef y quien sirve a mano cogen lo mejor) y el plato paga mas:
     * cada ingrediente de calidad q suma {@code BONUS[q] %} repartido entre todos los ingredientes del plato (un ingrediente sin granja suma 0).
     */
    static Uso usar(Datos.Jugador d, Recetas.Plato pl) {
        if (pl == null || d.despensa.isEmpty()) return new Uso(0, 0, 0, pl == null ? 0 : ingredientes(pl).size());
        List<Item> ings = ingredientes(pl);
        if (ings.isEmpty()) return new Uso(0, 0, 0, 0);
        double suma = 0; int usados = 0, mejor = 0;
        for (Item i : ings) {
            int q = gastar(d, i);
            if (q > 0) { suma += BONUS[q] / 100.0; usados++; mejor = Math.max(mejor, q); }
        }
        return new Uso(suma / ings.size(), usados, mejor, ings.size());
    }

    /** Lo mismo sin gastar nada (para mostrar en el menu cuanto mas paga un plato con lo que hay). */
    static double extraPosible(Datos.Jugador d, Recetas.Plato pl) {
        List<Item> ings = ingredientes(pl);
        if (ings.isEmpty()) return 0;
        double suma = 0;
        for (Item i : ings) suma += BONUS[mejorCalidad(d, i)] / 100.0;
        return suma / ings.size();
    }

    // ------------------------------------------------------------ donde esta cada finca (relativo al origen de la parcela)

    /** Cada finca ocupa 9 x 9 bloques al fondo de la parcela: x de {@code X0 + 10 * slot} y z de -11 a -3. */
    static final int X0 = -3, ANCHO = 9, Z0 = -11, Z1 = -3;
    static int x0(int slot) { return X0 + 10 * slot; }

    /** ¿Cae (rx, rz) dentro de una finca ya comprada? */
    static boolean enHuella(Datos.Jugador d, int rx, int rz) {
        if (rz < Z0 || rz > Z1 || d == null) return false;
        for (Datos.Finca f : d.granja) if (rx >= x0(f.slot) && rx < x0(f.slot) + ANCHO) return true;
        return false;
    }

    /** El primer sitio libre de la granja (-1 = lleno). */
    static int sitioLibre(Datos.Jugador d, int max) {
        for (int s = 0; s < Math.min(MAX_FINCAS, Math.max(0, max)); s++) {
            boolean ocupado = false;
            for (Datos.Finca f : d.granja) if (f.slot == s) ocupado = true;
            if (!ocupado) return s;
        }
        return -1;
    }

    static Datos.Finca finca(Datos.Jugador d, int slot) { for (Datos.Finca f : d.granja) if (f.slot == slot) return f; return null; }
}
