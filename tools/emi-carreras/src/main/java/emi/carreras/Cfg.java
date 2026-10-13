package emi.carreras;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Ajustes del minijuego (config/emi_carreras.json). Se crea con estos valores la primera vez. */
public final class Cfg {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public String pista = "circuito_pokeball";   // id de la pista (data/emi_carreras/pistas/<id>.json)
    public int vueltas = 0;                      // 0 = las que traiga la pista
    public int minPlayers = 1;                   // se puede jugar con menos gente (incluso solo, para probar)
    public int maxPlayers = 12;
    public double velocidad = 0.23;              // atributo de velocidad igual para todos (un jugador normal anda a 0.1; 0.23 = unos 10 bloques/s)
    public double adelanto = 3.0;                // ticks que se adelanta el Pokemon visual para compensar el retraso de la red
    public float alturaPokemon = 1.3f;           // todos los Pokemon se escalan a esta altura (bloques)
    public int cuentaAtras = 5;                  // segundos
    public int segundosTrasPrimero = 45;         // tras llegar el primero, los demas tienen este tiempo
    public int respawnSegundos = 2;              // parada al caerse de la pista
    public int queueReminderSeconds = 60;        // aviso periodico mientras la cola esta abierta
    public boolean poderes = true;
    public int cajaRespawnSegundos = 6;
    /** Pokemon que se prestan al que no tenga ninguno terrestre en el equipo (se elige uno al azar). */
    public List<String> prestados = new ArrayList<>(List.of("growlithe", "ponyta", "eevee", "machop", "mareep", "sandshrew", "vulpix", "riolu", "rattata", "pikachu"));
    /** Comandos que se ejecutan al terminar para cada puesto (1.º, 2.º...); {jugador} se sustituye por el nombre. Vacio = sin premio. */
    public List<String> recompensas = new ArrayList<>(List.of("", "", ""));

    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("emi_carreras.json"); }

    public static Cfg load() {
        Path p = path();
        try {
            if (Files.exists(p)) {
                Cfg c = GSON.fromJson(Files.readString(p), Cfg.class);
                if (c != null) { c.sanitize(); return c; }
            }
        } catch (Exception e) {
            EmiCarreras.LOG.error("No se pudo leer {}; uso los valores por defecto", p, e);
        }
        Cfg c = new Cfg();
        try {
            Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(c));
        } catch (IOException e) {
            EmiCarreras.LOG.warn("No se pudo escribir {}", p, e);
        }
        return c;
    }

    private void sanitize() {
        minPlayers = Math.max(1, minPlayers);
        maxPlayers = Math.max(minPlayers, Math.min(maxPlayers, 24));
        velocidad = Math.max(0.05, Math.min(velocidad, 0.6));
        cuentaAtras = Math.max(1, cuentaAtras);
        segundosTrasPrimero = Math.max(5, segundosTrasPrimero);
        alturaPokemon = Math.max(0.5f, alturaPokemon);
        if (prestados == null || prestados.isEmpty()) prestados = new ArrayList<>(List.of("eevee"));
        if (recompensas == null) recompensas = new ArrayList<>();
    }
}
