package emi.survivors;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import emi.survivors.net.TextoPayload;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * «Ms. President», la presidenta de Michialien S.A., una michi con traje y maletin que patrocina los minijuegos del mundo. Es un GatitoAlien de Cobblemon con el aspecto
 * {@code suit} (el traje de emi-plush 1.15.0), sin IA, invulnerable y que mira a quien se acerca. Un administrador la pone donde esta con {@code /survivor npc colocar}; el
 * sitio se guarda en {@code emi_survivors/ms_president.json} y, si la michi no esta (se paro el servidor a la fuerza, un limpiador de entidades...), el servidor la vuelve a
 * poner cuando el trozo de mapa esta cargado. Al hacerle clic derecho se abre una pantalla de conversacion (cliente: {@code MsPresidentPantalla}) que termina ofreciendo
 * unirse al juego (llevar al jugador a la sala de Emi Survivors).
 */
final class MsPresident {
    private MsPresident() {}

    static final String TAG = "emi_ms_president", NOMBRE = "Ms. President";
    /** Distancia (bloques) a la que mira a quien pasa cerca. */
    private static final double MIRA = 8.0;

    private static Path archivo;
    private static String dim = "";
    private static double x, y, z;
    private static float yaw;
    private static boolean hay;
    private static int t;

    private static boolean registrado;

    /** Al arrancar el servidor: lee el sitio guardado (y registra, una sola vez, el clic derecho sobre ella). */
    static void alIniciar(MinecraftServer s) {
        archivo = s.getSavePath(WorldSavePath.ROOT).resolve("emi_survivors").resolve("ms_president.json");
        hay = false;
        try {
            if (Files.exists(archivo)) {
                JsonObject o = JsonParser.parseString(Files.readString(archivo)).getAsJsonObject();
                dim = o.get("dim").getAsString(); x = o.get("x").getAsDouble(); y = o.get("y").getAsDouble(); z = o.get("z").getAsDouble(); yaw = o.get("yaw").getAsFloat();
                hay = true;
            }
        } catch (Exception e) {
            EmiSurvivors.LOG.warn("No se pudo leer ms_president.json", e);
        }
        if (registrado) return;
        registrado = true;
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!entity.getCommandTags().contains(TAG)) return ActionResult.PASS;
            if (!world.isClient && hand == Hand.MAIN_HAND && player instanceof ServerPlayerEntity sp) hablar(sp);
            return ActionResult.SUCCESS;                                  // nada de las interacciones normales de un Pokemon
        });
    }

    private static void guardar() {
        try {
            Files.createDirectories(archivo.getParent());
            JsonObject o = new JsonObject();
            o.addProperty("dim", dim); o.addProperty("x", x); o.addProperty("y", y); o.addProperty("z", z); o.addProperty("yaw", yaw);
            Files.writeString(archivo, o.toString());
        } catch (Exception e) {
            EmiSurvivors.LOG.warn("No se pudo guardar ms_president.json", e);
        }
    }

    private static ServerWorld mundo(MinecraftServer s) {
        return s.getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(dim)));
    }

    private static List<PokemonEntity> buscar(ServerWorld w, double cx, double cz, double radio) {
        return w.getEntitiesByClass(PokemonEntity.class, new Box(cx - radio, w.getBottomY(), cz - radio, cx + radio, w.getTopY(), cz + radio), e -> e.getCommandTags().contains(TAG));
    }

    // ------------------------------------------------------------------------------------------------ colocar y mantener

    /** {@code /survivor npc colocar}: la pone donde esta el administrador, mirando hacia el. Devuelve el motivo si no se pudo. */
    static String colocar(ServerPlayerEntity p) {
        ServerWorld w = p.getServerWorld();
        quitarTodas(w.getServer());
        dim = w.getRegistryKey().getValue().toString();
        x = Math.floor(p.getX()) + 0.5; y = p.getY(); z = Math.floor(p.getZ()) + 0.5; yaw = p.getYaw() + 180f;
        hay = true;
        guardar();
        return crear(w) ? null : "No se pudo crear a Ms. President (mira el registro del servidor).";
    }

    /** {@code /survivor npc quitar}: la quita y olvida el sitio. */
    static String quitar(MinecraftServer s) {
        if (!hay) return "No hay ninguna Ms. President colocada.";
        quitarTodas(s);
        hay = false;
        try { Files.deleteIfExists(archivo); } catch (Exception e) { /* nada */ }
        return null;
    }

    private static void quitarTodas(MinecraftServer s) {
        if (!hay) return;
        ServerWorld w = mundo(s);
        if (w == null) return;
        for (PokemonEntity e : buscar(w, x, z, 6)) e.discard();
    }

    private static boolean crear(ServerWorld w) {
        try {
            PokemonEntity e = PokemonProperties.Companion.parse("gatitoalien level=50 accessory=suit uncatchable=yes").createEntity(w);
            e.getPokemon().setScaleModifier(1.5f);
            e.setAiDisabled(true);
            e.setInvulnerable(true);
            e.setPersistent();
            e.setSilent(true);
            e.getDataTracker().set(PokemonEntity.getUNBATTLEABLE(), true);
            e.setCustomName(Text.literal(NOMBRE).formatted(Formatting.GOLD, Formatting.BOLD));
            e.setCustomNameVisible(true);
            e.addCommandTag(TAG);
            e.refreshPositionAndAngles(x, y, z, yaw, 0f);
            e.setHeadYaw(yaw); e.setBodyYaw(yaw);
            return w.spawnEntity(e);
        } catch (Throwable t) {
            EmiSurvivors.LOG.error("No se pudo crear a Ms. President", t);
            return false;
        }
    }

    /** Cada medio segundo mira a quien se acerca; cada 5 s comprueba que siga ahi (si el trozo de mapa esta cargado). */
    static void tick(MinecraftServer s) {
        if (!hay || ++t % 10 != 0) return;
        ServerWorld w = mundo(s);
        if (w == null) return;
        BlockPos bp = BlockPos.ofFloored(x, y, z);
        if (!w.isChunkLoaded(bp.getX() >> 4, bp.getZ() >> 4)) return;
        List<PokemonEntity> l = buscar(w, x, z, 6);
        if (t % 100 == 0 || l.isEmpty()) {
            if (l.isEmpty() && t % 100 != 0) return;                       // si falta, se espera al siguiente repaso (el trozo puede estar cargandose)
            if (l.isEmpty()) { crear(w); return; }
            for (int i = 1; i < l.size(); i++) l.get(i).discard();          // sobran (no deberia pasar)
        }
        if (l.isEmpty()) return;
        PokemonEntity e = l.get(0);
        ServerPlayerEntity cerca = null; double mejor = MIRA * MIRA;
        for (ServerPlayerEntity p : w.getPlayers()) { double d = p.squaredDistanceTo(e); if (d < mejor && !p.isSpectator()) { mejor = d; cerca = p; } }
        float objetivo = cerca == null ? yaw : (float) Math.toDegrees(Math.atan2(-(cerca.getX() - e.getX()), cerca.getZ() - e.getZ()));
        e.setYaw(objetivo); e.setHeadYaw(objetivo); e.setBodyYaw(objetivo);
    }

    // ------------------------------------------------------------------------------------------------ conversacion

    /** Abre la pantalla de conversacion (o dice por chat que falta el mod en el cliente). */
    static void hablar(ServerPlayerEntity p) {
        if (!ServerPlayNetworking.canSend(p, TextoPayload.ID)) {
            p.sendMessage(Msg.say("Ms. President: «¡Habrá muchos minijuegos y yo los patrocino! Para ver mi mensaje hace falta el mod Emi Survivors: actualiza el pack.»"), false);
            return;
        }
        ServerPlayNetworking.send(p, new TextoPayload("ms", "{}"));
        Sonidos.en(p.getServerWorld(), p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_CAT_AMBIENT, SoundCategory.NEUTRAL, 0.9f, 1.4f);
    }

    /** El jugador eligio un juego en la conversacion: «survivors» lo lleva a la sala de Emi Survivors (o abre el menu si ya esta en ella); «carreras» ejecuta {@code /carrera entrar}. */
    static void unirse(ServerPlayerEntity p, String juego) {
        if ("carreras".equals(juego)) { irACarreras(p); return; }
        if ("emi_carreras".equals(p.getWorld().getRegistryKey().getValue().getNamespace())) { p.sendMessage(Msg.err("Estás en las carreras: sal primero (/carrera salir)."), false); return; }
        if (Partida.participa(p)) { p.sendMessage(Msg.err("Ya estás en una partida."), false); return; }
        if (Mundo.enSala(p)) { Menu.enviar(p, "jugar", null); return; }
        String err = Mundo.entrar(p);
        if (err != null) p.sendMessage(Msg.err(err), false);
        else p.sendMessage(Msg.say("Ms. President: «¡Bienvenido a Emi Survivors! Tu aventura empieza aquí.»"), false);
    }

    /** Las carreras son otro mod: se le pide con su propio comando (si no esta instalado en el servidor, se avisa). */
    private static void irACarreras(ServerPlayerEntity p) {
        MinecraftServer s = p.getServer();
        if (s == null || s.getCommandManager().getDispatcher().getRoot().getChild("carrera") == null) {
            p.sendMessage(Msg.err("Las carreras aún no están disponibles en este servidor."), false);
            return;
        }
        if (Mundo.dentro(p) || Partida.participa(p)) { p.sendMessage(Msg.err("Estás en Emi Survivors: sal primero (/survivor salir)."), false); return; }
        s.getCommandManager().executeWithPrefix(p.getCommandSource(), "carrera entrar");
    }
}
