package emi.survivors;

/**
 * Una gema de experiencia (una michiestrellita) en el suelo: la deja un enemigo al caer. No es una entidad de Minecraft (costaria demasiado con cientos a la vez):
 * el servidor lleva la lista y el cliente las dibuja. Cuando el jugador se acerca, la gema se siente atraida y vuela hacia el.
 */
final class Gema {
    double x, z;
    final int valor;
    Run dueno;                  // el jugador al que ya se siente atraida (si hay varios jugadores, la gema es del primero que la atrae)
    int atraidaT;               // ticks desde que se siente atraida (cuanto mas, mas rapido vuela)

    Gema(double x, double z, int valor) { this.x = x; this.z = z; this.valor = valor; }

    /** 0 = pequena (azul), 1 = mediana (verde), 2 = grande (rosa dorado): lo que decide como la dibuja el cliente. */
    int tipo() { return valor >= 10 ? 2 : valor >= 3 ? 1 : 0; }
}
