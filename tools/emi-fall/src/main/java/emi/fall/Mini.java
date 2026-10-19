package emi.fall;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.Map;

/** Un minijuego del torneo. Cada uno construye su arena (una sola vez) en la dimension {@code emi_fall:arena} y lleva su propia partida. */
interface Mini {
    String id();
    String nombre();
    /** Dos frases que se leen antes de empezar: que hay que hacer. */
    String regla();
    /** La explicacion breve que se lee antes de empezar: 2-3 lineas cortas (que hay que hacer, como se puntua, que hay que evitar). */
    default String[] pasos() { return new String[]{regla()}; }
    /** Una frase muy corta para el marcador de la derecha mientras se juega. */
    default String objetivo() { return ""; }
    /** La cancion de fondo ({@code espera}, {@code carrera}, {@code tension} o {@code fiesta}). */
    default String musica() { return "carrera"; }
    /** Lo que lleva el jugador en esta ronda para el marcador (caramelos, segundos…); -1 = este minijuego no tiene cuenta en vivo. */
    default int vivo(Ctx c, Jug j) { return -1; }
    /** Lo que cuenta {@link #vivo} («caramelos», «segundos»…). */
    default String unidad() { return ""; }
    /** ¿Se juega por equipos (rojo y azul)? */
    default boolean equipos() { return false; }

    /** Construye la arena si todavia no existe (idempotente). */
    void construir(ServerWorld w);
    /** Pone la decoracion de Halloween y del Michialien de la arena (una vez; ver {@link Deco}). Se llama despues de {@link #construir}. */
    default void ponerDeco(ServerWorld w) {}
    /** Prepara la ronda: coloca a los jugadores en su sitio de salida (con {@link Ctx#colocar}) y deja todo listo. Los jugadores esperan quietos hasta {@link #liberar}. */
    void empezar(Ctx c);
    /** Se acabo la cuenta atras: ¡ya! */
    void liberar(Ctx c);
    /** Un tick de juego. Devuelve true cuando la ronda ha terminado. */
    boolean tick(Ctx c);
    /** Los jugadores de mejor a peor (los empatados, en cualquier orden). */
    List<Jug> clasificacion(Ctx c);
    /** Puntos extra por encima de los del puesto (por ejemplo el equipo ganador). */
    default Map<Jug, Integer> extra(Ctx c) { return Map.of(); }
    /** Quita todo lo temporal (entidades, bloques cambiados) al acabar la ronda. */
    void limpiar(Ctx c);
    /** Lo que ve cada jugador en la barra de arriba durante el juego (vacio = nada). */
    default String hud(Ctx c, Jug j) { return ""; }
    /** El jugador ha pulsado el boton derecho con un objeto en la mano. true = ya lo has atendido (se anula el uso normal). */
    default boolean alUsar(Ctx c, Jug j, ServerPlayerEntity p) { return false; }
    /** Donde estan sus entidades moviles ({centro x, centro z, radio}) para barrer las copias congeladas; null = no tiene. */
    default double[] zona() { return null; }
    /** Alguien se ha ido a media ronda. */
    default void alIrse(Ctx c, Jug j) {}
}
