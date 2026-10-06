package com.emipokemon.twitch;
/** Solo para compilar: en el jar real estos campos eran privados y el parche los hace de paquete. */
final class TwitchProfileStore {
    final java.util.Map<java.util.UUID, TwitchProfile> profiles = new java.util.HashMap<>();
    final java.nio.file.Path file = null;
}
