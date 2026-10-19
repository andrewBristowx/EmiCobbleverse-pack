package emi.fall;

import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

/**
 * La sala de espera del torneo (dimension {@code emi_fall:entrada}): una plataforma de colores flotando en el vacio. Se entra con {@code /fall entrar}, que
 * guarda todo lo del jugador (inventario, sitio, modo...) y se lo devuelve al salir. Desde ella los jugadores se apuntan al torneo.
 */
final class Sala {
    private Sala() {}

    static final RegistryKey<World> DIM = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("emi_fall", "entrada"));
    static final int Y = 63;                         // el suelo; los pies estan en Y + 1
    static final double SX = 0.5, SY = Y + 1, SZ = 9.5;
    static final int R = 15;
    static final int VERSION = 1;

    static ServerWorld mundo(MinecraftServer s) { return s.getWorld(DIM); }

    static boolean enSala(ServerPlayerEntity p) { return p.getServerWorld().getRegistryKey().equals(DIM); }

    /** Construye la sala la primera vez (o si cambia VERSION). */
    static void construir(ServerWorld w) {
        BlockPos marca = new BlockPos(0, -64, 0);
        String quiero = "minecraft:crying_obsidian";
        if (w.getBlockState(marca).isOf(Blocks.CRYING_OBSIDIAN) && w.getBlockState(marca.up()).isOf(Blocks.WHITE_CONCRETE)) { if (versionActual(w) == VERSION) return; }
        long t0 = System.nanoTime();
        Disp.barrer(w, 0, 0, 40);
        String[] col = {"pink_concrete", "light_blue_concrete", "yellow_concrete", "lime_concrete", "orange_concrete", "magenta_concrete"};
        for (int dx = -R - 2; dx <= R + 2; dx++) for (int dz = -R - 2; dz <= R + 2; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > R + 1) continue;
            for (int y = Y - 3; y <= Y + 6; y++) Obra.poner(w, dx, y, dz, "air");
            if (d > R - 0.5) { Obra.poner(w, dx, Y, dz, "white_concrete"); Obra.poner(w, dx, Y + 1, dz, "quartz_slab"); continue; }       // el borde bajito
            int c = Math.floorMod((int) Math.floor(d / 2.5) + (dx + dz) / 3, col.length);
            Obra.poner(w, dx, Y, dz, ((dx + dz) & 1) == 0 ? col[c] : "white_concrete");
            Obra.poner(w, dx, Y - 1, dz, "white_concrete");
        }
        // cuatro farolas con luz marina en el borde y un arco de entrada de colores
        for (int k = 0; k < 4; k++) {
            int px = (int) Math.round(Math.cos(Math.PI / 4 + k * Math.PI / 2) * (R - 2)), pz = (int) Math.round(Math.sin(Math.PI / 4 + k * Math.PI / 2) * (R - 2));
            for (int y = 1; y <= 4; y++) Obra.poner(w, px, Y + y, pz, "quartz_pillar");
            Obra.poner(w, px, Y + 5, pz, "sea_lantern");
        }
        // el podio del centro
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) Obra.poner(w, dx, Y + 1, dz, "quartz_block");
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) Obra.poner(w, dx, Y + 2, dz, "gold_block");
        Disp.texto(w, "{\"text\":\"EMI FALL\",\"color\":\"light_purple\",\"bold\":true}", 0.5, Y + 6.5, 0.5, 6f, false);
        Disp.texto(w, "{\"text\":\"El torneo de minijuegos\",\"color\":\"yellow\"}", 0.5, Y + 4.8, 0.5, 2.2f, false);
        Disp.texto(w, "{\"text\":\"Escribe /fall unirse para apuntarte\",\"color\":\"white\"}", SX, Y + 3.2, SZ - 3, 1.6f, false);
        Obra.poner(w, 0, -64, 0, "crying_obsidian");
        Obra.poner(w, 0, -63, 0, "white_concrete");
        Obra.poner(w, 1, -64, 0, "bedrock");           // la version se apunta en un bloque: 1 = bedrock, 2 = obsidiana...
        EmiFall.LOG.info("Sala del torneo construida en {} ms", (System.nanoTime() - t0) / 1_000_000);
    }

    private static int versionActual(ServerWorld w) { return w.getBlockState(new BlockPos(1, -64, 0)).isOf(Blocks.BEDROCK) ? 1 : 0; }

    /** Mete al jugador en la sala (guardando antes lo suyo si es la primera vez). */
    static String entrar(ServerPlayerEntity p) {
        MinecraftServer s = p.getServer();
        ServerWorld w = mundo(s);
        if (w == null) return "No existe la dimension de la sala (¿falta el mod emi-fall en el servidor?).";
        if (Torneo.participa(p)) return "Ya estas en un torneo en marcha: usa /fall salir para irte.";
        construir(w);
        if (!Saved.exists(s, p.getUuid())) Saved.save(s, p, true);
        p.getInventory().clear();
        p.clearStatusEffects();
        p.changeGameMode(GameMode.ADVENTURE);
        p.getAbilities().flying = false; p.sendAbilitiesUpdate();
        p.teleport(w, SX, SY, SZ, 180f, 0f);
        p.sendMessage(Msg.say(Text.literal("Bienvenido a Emi Fall. Escribe ").append(Text.literal("/fall unirse").formatted(Formatting.GOLD)).append(" para apuntarte al torneo; ")
                .append(Text.literal("/fall salir").formatted(Formatting.GOLD)).append(" para irte.")), false);
        return null;
    }

    /** Saca al jugador de la sala y le devuelve todo. */
    static boolean salir(MinecraftServer s, ServerPlayerEntity p) {
        Morph.quitar(s, p);
        return Saved.restore(s, p);
    }

    /** ¿Esta en la sala y por tanto no puede tocar nada? */
    static boolean bloqueado(ServerPlayerEntity p) { return enSala(p); }

    /** Cada tick: nadie se cae de la sala. */
    static void tick(MinecraftServer s) {
        ServerWorld w = mundo(s);
        if (w == null) return;
        for (ServerPlayerEntity p : w.getPlayers()) {
            if (p.getY() < Y - 8) p.teleport(w, SX, SY, SZ, 180f, 0f);
            if (s.getTicks() % 40 == 0) p.getHungerManager().setFoodLevel(20);
        }
    }
}
