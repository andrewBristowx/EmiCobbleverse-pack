package emi.cafe;

/**
 * El personal del cafe: Michialien que el dueño contrata (mesero, lavaplatos y chef), con el nombre que quiera. Cuantos caben depende del tamaño del local. Sin Minecraft: solo tablas.
 */
final class Personal {
    private Personal() {}

    /** Lo que sabe hacer cada uno. {@code cada} = ticks entre una tarea y la siguiente. */
    enum Rol {
        MESERO("mesero", "Mesero", "Lleva a la mesa los platos que quedan listos en las estaciones.", 800, 3, 4, 20 * 5, "emi_cafe:campana", 1, 8),
        LAVAPLATOS("lavaplatos", "Lavaplatos", "Lava solo los platos sucios que quedan en las mesas.", 400, 2, 2, 20 * 9, "emi_cafe:pila_platos", 2, 6),
        CHEF("chef", "Chef", "Cocina por ti el plato de un cliente que espera (tarda un poco, más cuantas más estaciones usa).", 1500, 3, 5, 20, "emi_cafe:cocinero_charmander", 0, 8),
        /** Puestos de 0.32.0; desde 0.34.0 tienen tarea propia: el anfitrion calma a quien se impacienta y el cajero cobra la cuenta en la mesa. */
        ANFITRION("anfitrion", "Anfitrión", "Atiende a los clientes: aguantan más esperando (+5 %, más con el nivel) y va a calmar al que se impacienta (+25 % de paciencia, más con el nivel).", 900, 4, 3, 20, "emi_cafe:cartel", 3, 2),
        CAJERO("cajero", "Cajero", "Va a la mesa a cobrar la cuenta cuando el cliente termina de comer: propina extra (+4 % del pago, más con el nivel).", 700, 5, 4, 20, "emi_cafe:caja_registradora", 7, 2),
        /** El michigranjero de un huerto o corral (0.24.0): viene con la finca, no se contrata (max 0) y no cuenta en el total. */
        GRANJERO("granjero", "Michigranjero", "Cuida un huerto o un corral y lleva la cosecha a la despensa.", 0, 5, 0, 20, "emi_cafe:icono_granjero", 6, 0);

        final String id, nombre, descripcion, icono;
        final int precio, reqLocal, reqRep, cada, aspectoDefecto, max;

        Rol(String id, String nombre, String descripcion, int precio, int reqLocal, int reqRep, int cada, String icono, int aspectoDefecto, int max) {
            this.id = id; this.nombre = nombre; this.descripcion = descripcion; this.precio = precio; this.reqLocal = reqLocal; this.reqRep = reqRep; this.cada = cada; this.icono = icono; this.aspectoDefecto = aspectoDefecto; this.max = max;
        }

        static Rol de(String id) { for (Rol r : values()) if (r.id.equals(id)) return r; return null; }
    }

    // ------------------------------------------------------------ efectos de los puestos nuevos, uniformes y felicidad (0.32.0)

    /** Cuanta paciencia extra dan los anfitriones (fraccion: 0,06 = +6 %): cada uno da 5 % + 1 % por nivel. */
    static double bonoPaciencia(java.util.List<Datos.Empleado> l) {
        double b = 0;
        for (Datos.Empleado e : l) if (Rol.ANFITRION.id.equals(e.rol)) b += (4 + nivel(e)) / 100.0;
        return b;
    }

    /** Cuanto paga de mas la cuenta que cobra cada cajero en la mesa (fraccion del pago): 3 % + 1 % por nivel. Solo vale si llega a la mesa (desde 0.34.0). */
    static double bonoPropina(java.util.List<Datos.Empleado> l) {
        double b = 0;
        for (Datos.Empleado e : l) if (Rol.CAJERO.id.equals(e.rol)) b += (3 + nivel(e)) / 100.0;
        return b;
    }

    /** Uniforme: +2 % de pago por cada puesto con 2 o mas empleados que lleven todos el mismo aspecto, y +3 % mas si todo el personal (4 o mas) va igual. */
    static int uniformePct(java.util.List<Datos.Empleado> l) {
        int pct = 0;
        for (Rol r : Rol.values()) {
            if (r == Rol.GRANJERO) continue;
            int n = 0, asp = -1; boolean igual = true;
            for (Datos.Empleado e : l) if (r.id.equals(e.rol)) { n++; if (asp < 0) asp = e.aspecto; else if (asp != e.aspecto) igual = false; }
            if (n >= 2 && igual) pct += 2;
        }
        int n = 0, asp = -1; boolean igual = true;
        for (Datos.Empleado e : l) { n++; if (asp < 0) asp = e.aspecto; else if (asp != e.aspecto) igual = false; }
        if (n >= 4 && igual) pct += 3;
        return pct;
    }

    /** La felicidad de un empleado (0 a 100; 70 al contratarlo) y como se llama cada tramo. */
    static final String[] ANIMOS = {"Agotado", "Normal", "Contento", "Radiante"};
    static int animoNivel(int felicidad) { return felicidad < 30 ? 0 : felicidad < 60 ? 1 : felicidad < 85 ? 2 : 3; }
    /** Cuanto cambia lo rapido que trabaja segun su humor: 0,90 / 1,00 / 1,05 / 1,12. */
    static double animo(int felicidad) { return new double[]{0.90, 1.0, 1.05, 1.12}[animoNivel(felicidad)]; }
    /** Lo que cuesta darle un premio (sube 25 puntos de felicidad). */
    static int costePremio(Datos.Empleado e) { return 40 * nivel(e); }

    /** Cuantos empleados caben en total (la suma de lo que admite cada puesto); todos los contratados trabajan. */
    static int maxTotal() { int n = 0; for (Rol r : Rol.values()) n += r.max; return n; }

    static final int NIVEL_MAX = 10;
    private static final double[] COSTE_SUBIDA = {0.5, 1.0, 1.6, 2.4, 3.4, 4.6, 6.0, 7.6, 9.4};      // por el precio de contratar al rol: de nivel 1 a 2, de 2 a 3...

    /** El nivel del empleado (1 a 10). */
    static int nivel(Datos.Empleado e) { return Math.max(1, Math.min(NIVEL_MAX, e.nivel)); }

    /** Monedas que cuesta subirlo al nivel siguiente (0 si ya esta al maximo). */
    static int costeSubida(Rol rol, int nivel) { return nivel >= NIVEL_MAX ? 0 : (int) Math.round(rol.precio * COSTE_SUBIDA[nivel - 1]); }

    /** Cuanto mas rapido anda (+12 % por nivel) y trabaja (+15 % por nivel): multiplicadores de 1,0 a 1,48 y de 1,0 a 1,6. */
    static double velocidad(int nivel) { return 1 + 0.12 * (nivel - 1); }
    static double trabajo(int nivel) { return 1 + 0.15 * (nivel - 1); }

    /** Los aspectos del Michialien: {propiedades de Cobblemon de emi-plush, nombre}. */
    static final String[][] ASPECTOS = {
        {"accessory=chef", "Gorro de chef"}, {"accessory=suit", "Traje y corbata"}, {"accessory=bow", "Lazo"}, {"accessory=scarf", "Bufanda"},
        {"accessory=sunglasses", "Gafas de sol"}, {"accessory=headphones", "Auriculares"}, {"accessory=beach_hat", "Sombrero de playa"}, {"accessory=flower_crown", "Corona de flores"},
        {"accessory=santa_hat", "Gorro navideño"}, {"accessory=witch_hat", "Sombrero de bruja"}, {"accessory=pirate", "Pirata"}, {"accessory=crown", "Corona"},
    };

    static final String[] NOMBRES = {"Michi", "Luna", "Nube", "Canela", "Fresa", "Moka", "Pepita", "Tofu", "Coco", "Mochi", "Brisa", "Chispa"};

    /** Para ordenar la lista por puesto: chefs, meseros, lavaplatos, anfitriones, cajeros y michigranjeros. */
    static int ordenRol(String id) {
        Rol[] o = {Rol.CHEF, Rol.MESERO, Rol.LAVAPLATOS, Rol.ANFITRION, Rol.CAJERO, Rol.GRANJERO};
        for (int i = 0; i < o.length; i++) if (o[i].id.equals(id)) return i;
        return o.length;
    }

    /** Cuantos hay ya de ese puesto. */
    static int cuantos(java.util.List<Datos.Empleado> l, Rol rol) { int n = 0; for (Datos.Empleado e : l) if (rol.id.equals(e.rol)) n++; return n; }

    /** El nivel de local que hace falta para contratar al siguiente de ese puesto, teniendo ya {@code tienes}: el primero pide el del puesto y cada uno mas, un nivel mas. */
    static int localNecesario(Rol rol, int tienes) { return rol.reqLocal + tienes; }

    /** Por que no se puede contratar a otro de ese puesto (vacio = se puede, salvo monedas y reputacion). */
    static String bloqueo(Rol rol, int tienes, int nivelLocal) {
        if (tienes >= rol.max) return "MÁXIMO (" + rol.max + ")";
        if (nivelLocal < localNecesario(rol, tienes)) return "Local nivel " + localNecesario(rol, tienes);
        return "";
    }

    /** Un nombre por defecto que no se repita con los que ya hay. */
    static String nombreNuevo(java.util.List<Datos.Empleado> actuales) {
        for (String n : NOMBRES) { boolean uso = false; for (Datos.Empleado e : actuales) if (e.nombre.equals(n)) uso = true; if (!uso) return n; }
        return "Michi " + (actuales.size() + 1);
    }

    /** Quita lo que no se puede mostrar (formatos, comillas, barras) y recorta. Devuelve "" si no queda nada. */
    static String limpiar(String s, int max) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) if (c >= 32 && c != '§' && c != '"' && c != '\\' && c != 127) b.append(c);
        String r = b.toString().trim().replaceAll("\\s+", " ");
        return r.length() > max ? r.substring(0, max).trim() : r;
    }
}
