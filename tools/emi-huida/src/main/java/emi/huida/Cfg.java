package emi.huida;

/** Ajustes. Se pueden cambiar sin recompilar con -D en la linea de Java; mas adelante iran a un archivo de configuracion. */
final class Cfg {
    private Cfg() {}
    /** velocidad del Tyrantrum (bloques/s) cuando va a la distancia que toca; -Demi.huida.vel_base=3 lo hace mas lento (para pruebas) */
    /** volumen de la musica de fondo (0-1); -Demi.huida.vol_musica=0.3 */
    static float volumenMusica() { return Float.parseFloat(System.getProperty("emi.huida.vol_musica", "0.8")); }
    /** durante la huida se paran la limpieza de objetos y los bosses/eventos de Emipokemon (solo en memoria); -Demi.huida.pausar_eventos=false para no hacerlo */
    static boolean pausarEventos() { return !"false".equals(System.getProperty("emi.huida.pausar_eventos")); }
    static double velBase() { return Double.parseDouble(System.getProperty("emi.huida.vel_base", String.valueOf(Huida.VEL_BASE))); }
}
