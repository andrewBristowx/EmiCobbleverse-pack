package emi.survivors;

/**
 * Las estadisticas del jugador en una partida: salen de la base, de las mejoras permanentes de su Pokemon (menu «Mejoras») y de las mejoras pasivas
 * que va eligiendo al subir de nivel. Son datos puros (sin Minecraft).
 */
final class Stats {
    double hpMax = 100;         // vida maxima
    double regen = 0;           // vida por segundo
    double vel = 1.0;           // multiplicador de la velocidad al andar
    double dano = 1.0;          // multiplicador del dano de todos los ataques
    double recarga = 1.0;       // multiplicador del tiempo entre usos (menor = mas rapido)
    double area = 1.0;          // multiplicador del tamano de los ataques
    double iman = 2.5;          // radio (bloques) al que se atraen las gemas
    double xp = 1.0;            // multiplicador de la experiencia ganada
    double armadura = 0;        // fraccion del dano de contacto que se evita (0..0,6)
    double monedas = 1.0;       // multiplicador de las monedas al acabar
}
