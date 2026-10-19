package emi.fall;

import java.util.UUID;

/** Un jugador del torneo: sus puntos y lo que hace falta apuntar de él en la ronda en marcha. */
final class Jug {
    final UUID id;
    String nombre;
    int puntos;                  // total del torneo
    int puntosRonda;             // lo que ha ganado en la ronda que acaba de terminar
    int puestoRonda;             // 1 = el mejor de la ronda que acaba de terminar
    boolean fuera;               // eliminado de la ronda en marcha (mira como espectador)
    int ordenFuera;              // el orden en que lo eliminaron (el primero que cae = 1)
    int equipo = -1;             // 0 = rojo, 1 = azul (solo en las rondas de equipos)
    double sx, sy, sz; float syaw;                       // el sitio donde espera durante la cuenta atras
    // lo que usan los minijuegos (cada uno lo que necesite)
    int fila, caidas, checkpoint;
    long tLlegada = -1;
    boolean llego;
    long inmune;                 // hasta que tick no le afectan los golpes
    boolean bomba;               // lleva una bomba en la mano (minijuego de las bombas)
    long tPad;                   // cuando uso por ultima vez un trampolin
    int ultimoTocado = -1;       // quien le dio el ultimo golpe (por indice), para los minijuegos de empujones
    int estrTorneo, puestoFinal;                          // estrellas ganadas en este torneo y su puesto final
    java.util.List<String> lineasTorneo = new java.util.ArrayList<>();
    int cuenta;                  // contador en vivo del minijuego (caramelos cogidos, segundos a salvo…)
    long hasta;                  // hasta que tick vale algo temporal (cooldown, reaparicion…)
    boolean tiene;               // pilla-pilla: lleva la calabaza
    Jug(UUID id, String nombre) { this.id = id; this.nombre = nombre; }
}
