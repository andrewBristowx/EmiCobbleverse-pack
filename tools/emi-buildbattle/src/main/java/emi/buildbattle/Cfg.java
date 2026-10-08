package emi.buildbattle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Ajustes del minijuego (config/emi_buildbattle.json). Se crea con estos valores la primera vez. */
public final class Cfg {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public int plotSize = 64;            // lado de la parcela de construccion
    public int plotHeight = 64;          // altura util sobre el suelo
    public int plotSpacing = 96;         // separacion entre parcelas (de origen a origen)
    public int plotsPerRow = 6;
    public int floorY = 64;              // y del cesped de la parcela
    public int wallHeight = 4;           // altura del muro visible
    public String floorBlock = "minecraft:grass_block";
    public String wallBlock = "minecraft:stone_bricks";
    public String wallTopBlock = "minecraft:chiseled_stone_bricks";
    public int minPlayers = 2;
    public int maxPlayers = 24;
    public int queueReminderSeconds = 60;   // aviso periodico para apuntarse mientras la cola esta abierta
    public int viewMargin = 24;             // durante la votacion pueden volar hasta tantos bloques fuera de la parcela
    public int topSize = 3;                 // cuantos puestos se revelan con ceremonia
    public int revealSeconds = 6;           // pausa entre puesto y puesto
    public boolean blockQuestItems = true;   // no ofrecer objetos que sean tarea de una mision de FTB Quests (para que no se completen gratis)
    public int stackSize = 64;              // bloques que da el NPC por clic (como mucho el maximo del objeto)

    /** Un bloque no se ofrece si su id contiene alguno de estos trozos. */
    public List<String> blockedContains = new ArrayList<>(List.of(
            "command_block", "structure_block", "structure_void", "jigsaw", "spawner", "tnt", "respawn_anchor",
            "end_portal", "end_gateway", "test_block", "test_instance", "debug", "reinforced_deepslate", "bundle"));
    /** Ids exactos prohibidos (ej. "cobblemon:pc"). */
    public List<String> blockedItems = new ArrayList<>();
    /** Espacios de nombres enteros prohibidos (ej. "somemod"). */
    public List<String> blockedNamespaces = new ArrayList<>();
    /** Objetos que no son bloques pero tambien se permiten (decoracion). */
    public List<String> extraItems = new ArrayList<>(List.of(
            "minecraft:item_frame", "minecraft:glow_item_frame", "minecraft:painting", "minecraft:armor_stand",
            "minecraft:water_bucket", "minecraft:bucket", "minecraft:bone_meal"));

    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("emi_buildbattle.json"); }

    public static Cfg load() {
        Path p = path();
        try {
            if (Files.exists(p)) {
                Cfg c = GSON.fromJson(Files.readString(p), Cfg.class);
                if (c != null) { c.sanitize(); return c; }
            }
        } catch (Exception e) {
            EmiBuildBattle.LOG.error("No se pudo leer {}; uso los valores por defecto", p, e);
        }
        Cfg c = new Cfg();
        try {
            Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(c));
        } catch (IOException e) {
            EmiBuildBattle.LOG.warn("No se pudo escribir {}", p, e);
        }
        return c;
    }

    private void sanitize() {
        plotSize = Math.max(8, Math.min(plotSize, 128));
        plotHeight = Math.max(8, Math.min(plotHeight, 200));
        plotSpacing = Math.max(plotSize + 24, plotSpacing);
        plotsPerRow = Math.max(1, plotsPerRow);
        wallHeight = Math.max(1, wallHeight);
        minPlayers = Math.max(1, minPlayers);
        maxPlayers = Math.max(1, maxPlayers);
        topSize = Math.max(1, topSize);
        stackSize = Math.max(1, Math.min(stackSize, 64));
        if (blockedContains == null) blockedContains = new ArrayList<>();
        if (blockedItems == null) blockedItems = new ArrayList<>();
        if (blockedNamespaces == null) blockedNamespaces = new ArrayList<>();
        if (extraItems == null) extraItems = new ArrayList<>();
    }
}
