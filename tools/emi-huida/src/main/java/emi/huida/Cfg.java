package emi.huida;

/** Ajustes. Se pueden cambiar sin recompilar con -D en la linea de Java; mas adelante iran a un archivo de configuracion. */
final class Cfg {
    private Cfg() {}
    /** velocidad del Tyrantrum (bloques/s) cuando va a la distancia que toca; -Demi.huida.vel_base=3 lo hace mas lento (para pruebas) */
    /** volumen de la musica de fondo (0-1); -Demi.huida.vol_musica=0.3 */
    static float volumenMusica() { return Float.parseFloat(System.getProperty("emi.huida.vol_musica", "0.8")); }
    /** durante la huida se paran la limpieza de objetos y los bosses/eventos de Emipokemon (solo en memoria); -Demi.huida.pausar_eventos=false para no hacerlo */
    static boolean pausarEventos() { return !"false".equals(System.getProperty("emi.huida.pausar_eventos")); }
    /** escenas con camara (entrada con el michi demonio, michi angel, mordisco); -Demi.huida.escenas=false para quitarlas (la huida empieza en 5 s sin presentacion) */
    static boolean escenas() { return !"false".equals(System.getProperty("emi.huida.escenas")); }
    /** solo para pruebas en equipos lentos: -Demi.huida.escena_lenta=3 hace que las escenas vayan 3 veces mas despacio (el cliente de pruebas, con grafica por software, no llega a 20 ticks/s) */
    static int escenaLenta() { return Math.max(1, Integer.getInteger("emi.huida.escena_lenta", 1)); }
    /** Errores que te llevan a ser comido: con 3, los dos primeros se perdonan y al TERCERO el Tyrantrum se lanza y te come (que el Tyrantrum te alcance cuenta como un error). -1 = sin limite (te come al alcanzarte, como antes); -Demi.huida.max_errores=5 para cambiarlo. */
    static int maxErrores() { return Integer.getInteger("emi.huida.max_errores", 3); }
    static double velBase() { return Double.parseDouble(System.getProperty("emi.huida.vel_base", String.valueOf(Nivel.DEF.vel()))); }

    // ---- solo para pruebas: -Demi.huida.dbgcam=<archivo .properties> permite cambiar sin reiniciar el servidor los numeros de las camaras de las escenas del nivel 2 (se relee cada segundo)
    private static final java.util.Properties DBG = new java.util.Properties();
    private static long dbgLeido;
    static double dbg(String clave, double def) {
        String f = System.getProperty("emi.huida.dbgcam");
        if (f == null) return def;
        long t = System.currentTimeMillis();
        if (t - dbgLeido > 1000) {
            dbgLeido = t;
            DBG.clear();
            try (var in = java.nio.file.Files.newInputStream(java.nio.file.Path.of(f))) { DBG.load(in); } catch (Exception ignored) {}
        }
        String v = DBG.getProperty(clave);
        try { return v == null ? def : Double.parseDouble(v.trim()); } catch (NumberFormatException e) { return def; }
    }
}
