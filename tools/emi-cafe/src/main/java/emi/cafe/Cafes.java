package emi.cafe;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Reparte los locales: cada jugador que entra tiene el suyo en la dimension {@code emi_cafe:cafe}, a 64 bloques del de al lado. */
final class Cafes {
    private Cafes() {}

    static final RegistryKey<World> DIM = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("emi_cafe", "cafe"));
    private static final Map<UUID, Cafe> CAFES = new HashMap<>();

    static ServerWorld mundo(MinecraftServer s) { return s.getWorld(DIM); }

    static boolean enCafe(ServerPlayerEntity p) { return p.getServerWorld().getRegistryKey().equals(DIM); }

    static Cafe de(UUID id) { return CAFES.get(id); }

    static boolean tiene(UUID id) { return CAFES.containsKey(id); }

    private static int slotLibre() {
        outer:
        for (int i = 0; ; i++) { for (Cafe c : CAFES.values()) if (c.slot == i) continue outer; return i; }
    }

    /** Mete al jugador en su cafe (guardando antes lo suyo). Devuelve el error, o null. */
    static String entrar(ServerPlayerEntity p) {
        MinecraftServer s = p.getServer();
        ServerWorld w = mundo(s);
        if (w == null) return "No existe la dimensión del café (¿falta el mod emi-cafe en el servidor?).";
        if (CAFES.containsKey(p.getUuid())) return "Ya estás en tu café: usa /cafe salir para irte.";
        String ns = p.getServerWorld().getRegistryKey().getValue().getNamespace();
        if (ns.startsWith("emi_") && !ns.equals("emi_cafe") && !ns.equals("emi_recursos") && !ns.equals("emi_estructuras")) return "Estás en otro minijuego: sal primero de él.";
        if (!Saved.exists(s, p.getUuid())) Saved.save(s, p, true);
        int slot = slotLibre();
        Cafe c = new Cafe(s, w, p.getUuid(), slot);
        Local.construir(w, c.ox);
        CAFES.put(p.getUuid(), c);
        p.getInventory().clear();
        p.clearStatusEffects();
        p.changeGameMode(GameMode.ADVENTURE);
        p.getAbilities().flying = false; p.sendAbilitiesUpdate();
        p.teleport(w, c.ox + Local.SX, Local.Y + 1, Local.SZ, 180f, 0f);
        c.sincronizarMano(p);
        p.sendMessage(Msg.say("Bienvenido a tu café. Clic derecho en la caja de papas para coger una; llévala a la tabla y luego a la freidora, y sirve las papas fritas al cliente."), false);
        return null;
    }

    /** Saca al jugador del cafe y le devuelve todo. */
    static boolean salir(ServerPlayerEntity p) {
        Cafe c = CAFES.remove(p.getUuid());
        if (c != null) c.cerrar();
        return Saved.restore(p.getServer(), p);
    }

    /** Se desconecta: se cierra su cafe; lo suyo se le devuelve al volver a entrar (Retornos). */
    static void alDesconectar(ServerPlayerEntity p) {
        Cafe c = CAFES.remove(p.getUuid());
        if (c != null) c.cerrar();
    }

    static void tick(MinecraftServer s) {
        for (Cafe c : CAFES.values().toArray(new Cafe[0])) {
            try { c.tick(); } catch (Throwable t) { EmiCafe.LOG.error("Error en un tick del cafe", t); }
        }
    }

    static void cerrarTodos() {
        for (Cafe c : CAFES.values()) c.cerrar();
        CAFES.clear();
    }
}
