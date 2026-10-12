package emi.estructuras;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Coloca los lideres NPC de Emipokemon dentro de los gimnasios y ligas de emi_gimnasios. `emi_estructuras/npcs.json` dice, por plantilla, donde
 * va cada NPC (posicion relativa a la esquina de la plantilla, giro y el comando de Emipokemon que lo crea, p. ej. `emipokemon desafios teselia rayo`).
 */
public final class NpcsGimnasio {
    private static JsonObject tabla;

    private NpcsGimnasio() {}

    private static JsonObject tabla() {
        if (tabla == null) {
            try (var in = NpcsGimnasio.class.getResourceAsStream("/emi_estructuras/npcs.json")) {
                tabla = in == null ? new JsonObject() : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (Exception e) {
                EmiEstructuras.LOG.warn("No pude leer npcs.json", e);
                tabla = new JsonObject();
            }
        }
        return tabla;
    }

    /** Crea los NPC de la plantilla `id` colocada con su esquina en `origen`. Devuelve cuantos comandos se ejecutaron. */
    public static int crear(MinecraftServer server, ServerWorld mundo, String id, BlockPos origen) {
        if (!tabla().has(id)) return 0;
        JsonArray lista = tabla().getAsJsonArray(id);
        int n = 0;
        for (var el : lista) {
            JsonObject o = el.getAsJsonObject();
            Vec3d pos = new Vec3d(origen.getX() + o.get("x").getAsDouble(), origen.getY() + o.get("y").getAsDouble(), origen.getZ() + o.get("z").getAsDouble());
            float giro = o.has("yaw") ? o.get("yaw").getAsFloat() : 0f;
            String cmd = o.get("cmd").getAsString();
            String idNpc = o.has("id") ? o.get("id").getAsString() : null;
            try {
                var fuente = server.getCommandSource().withWorld(mundo).withPosition(pos).withRotation(new Vec2f(0f, giro)).withLevel(4).withSilent();
                var despachador = server.getCommandManager().getDispatcher();
                int res = 0;
                try { res = despachador.execute(cmd, fuente); } catch (CommandSyntaxException e) { EmiEstructuras.LOG.warn("Comando de NPC no valido ({}): {}", cmd, e.getMessage()); }
                if (res <= 0 && idNpc != null) {
                    // ya existia ese NPC (p. ej. el que Emipokemon deja en el pueblo): se mueve al gimnasio si esta cargado
                    try { res = despachador.execute("emipokemon npc mover " + idNpc, fuente); } catch (CommandSyntaxException e) { EmiEstructuras.LOG.warn("No pude mover {}: {}", idNpc, e.getMessage()); }
                    EmiEstructuras.LOG.info("NPC {} ya existia: {}", idNpc, res > 0 ? "movido al gimnasio" : "no esta cargado; quedo donde estaba (usa /emipokemon npc mover " + idNpc + " dentro del gimnasio)");
                }
                // el comando de Emipokemon deja al NPC mirando al sur (la fuente no es una entidad): se gira hacia la entrada
                despachador.execute(String.format(Locale.ROOT, "execute positioned %.2f %.2f %.2f as @e[type=!minecraft:player,type=!minecraft:item,distance=..1.2,limit=1,sort=nearest] run tp @s ~ ~ ~ %.1f 0", pos.x, pos.y, pos.z, giro), fuente);
                n++;
                EmiEstructuras.LOG.info("NPC de {}: /{} en {}", id, cmd, pos);
            } catch (Throwable t) {
                EmiEstructuras.LOG.warn("No pude crear el NPC de {} ({})", id, cmd, t);
            }
        }
        return n;
    }
}
