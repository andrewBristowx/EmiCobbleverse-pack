package emi.huida;

/** Ajustes del perseguidor (prototipo). Se pueden cambiar sin recompilar con -Demi.huida.vel_ini=... y -Demi.huida.vel_max=...; mas adelante iran a un archivo de configuracion. */
final class Cfg {
    private Cfg() {}
    static double velIni() { return Double.parseDouble(System.getProperty("emi.huida.vel_ini", String.valueOf(Huida.VEL_PERSEGUIDOR_INI))); }
    static double velMax() { return Double.parseDouble(System.getProperty("emi.huida.vel_max", String.valueOf(Huida.VEL_PERSEGUIDOR_MAX))); }
}
