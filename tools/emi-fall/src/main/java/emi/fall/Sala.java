package emi.fall;

import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
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
    static final int VERSION = 4;
    /** El terminal (una mesa de encantamientos en el podio): tocarla abre el menu. */
    static final BlockPos TERMINAL = new BlockPos(0, Y + 2, 2);
    static final String ETIQUETA_MENU = "emi_fall_menu";

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
        Disp.texto(w, "{\"text\":\"El torneo de minijuegos\",\"color\":\"yellow\"}", 0.5, Y + 5.0, 0.5, 2.2f, false);
        Disp.texto(w, "{\"text\":\"Escribe /fall unirse para apuntarte\",\"color\":\"white\"}", SX, Y + 3.0, SZ - 3, 0.8f, false);
        Obra.poner(w, 0, -64, 0, "crying_obsidian");
        Obra.poner(w, 0, -63, 0, "white_concrete");
        // Halloween y Michialien: farolas de calabaza en el borde, un peluche gigante en el podio, la cara del Michialien y una luna creciente al fondo
        for (int k = 0; k < 14; k++) {
            double a = 2 * Math.PI * k / 14;
            int px = (int) Math.round(Math.cos(a) * (R - 0.5)), pz = (int) Math.round(Math.sin(a) * (R - 0.5));
            Deco.calabaza(w, px, Y + 2, pz, "s");
            Deco.p(w, px, Y + 3, pz, "cobweb");
        }
        java.util.Random rr = new java.util.Random(2);
        for (int k = 0; k < 5; k++) { double a = Math.PI * (0.15 + 0.7 * k / 4.0); Deco.arbolSeco(w, (int) Math.round(Math.cos(a) * (R + 6)), Y - 10, -(int) Math.round(Math.sin(a) * (R + 6)), 14, rr); }
        Deco.peluche(w, 0, Y + 3, 0, "gigante_gatitoalien_witch_hat", "s");
        Deco.michi(w, -13, Y - 4, -(R + 14), 'x', 2);
        Deco.luna(w, 22, Y + 26, -(R + 10), 8, 'z');
        Deco.luna(w, -24, Y + 20, R + 10, 6, 'z');
        Obra.poner(w, TERMINAL.getX(), TERMINAL.getY(), TERMINAL.getZ(), "enchanting_table");
        Disp.texto(w, "{\"text\":\"MENÚ\",\"color\":\"gold\",\"bold\":true}", TERMINAL.getX() + 0.5, TERMINAL.getY() + 1.6, TERMINAL.getZ() + 0.5, 1.4f, false);
        Obra.poner(w, 1, -64, 0, "gold_block");           // la version se apunta en un bloque: 1 = bedrock, 2 = obsidiana, 3 = obsidiana llorona, 4 = oro
        EmiFall.LOG.info("Sala del torneo construida en {} ms", (System.nanoTime() - t0) / 1_000_000);
    }

    private static int versionActual(ServerWorld w) { BlockPos m = new BlockPos(1, -64, 0); return w.getBlockState(m).isOf(Blocks.GOLD_BLOCK) ? 4 : w.getBlockState(m).isOf(Blocks.CRYING_OBSIDIAN) ? 3 : w.getBlockState(m).isOf(Blocks.OBSIDIAN) ? 2 : w.getBlockState(m).isOf(Blocks.BEDROCK) ? 1 : 0; }

    static ItemStack itemMenu() {
        ItemStack st = new ItemStack(Items.NETHER_STAR);
        st.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Menú de Emi Fall").styled(x -> x.withColor(Formatting.GOLD).withBold(true).withItalic(false)));
        st.set(DataComponentTypes.LORE, new LoreComponent(java.util.List.of(Text.literal("Clic derecho: torneo, misiones y tienda").styled(x -> x.withColor(Formatting.GRAY).withItalic(false)))));
        NbtCompound tag = new NbtCompound();
        tag.putBoolean(ETIQUETA_MENU, true);
        st.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
        return st;
    }

    static boolean esItemMenu(ItemStack st) {
        if (st == null || st.isEmpty()) return false;
        NbtComponent c = st.get(DataComponentTypes.CUSTOM_DATA);
        return c != null && c.copyNbt().getBoolean(ETIQUETA_MENU);
    }

    static void darItemMenu(ServerPlayerEntity p) { p.getInventory().setStack(4, itemMenu()); }

    /** Mete al jugador en la sala (guardando antes lo suyo si es la primera vez). */
    static String entrar(ServerPlayerEntity p) {
        MinecraftServer s = p.getServer();
        ServerWorld w = mundo(s);
        if (w == null) return "No existe la dimension de la sala (¿falta el mod emi-fall en el servidor?).";
        if (Torneo.participa(p)) return "Ya estas en un torneo en marcha: usa /fall salir para irte.";
        String ns = p.getServerWorld().getRegistryKey().getValue().getNamespace();
        if (ns.equals("emi_survivors") || ns.equals("emi_carreras") || ns.equals("emi_huida")) return "Estás en otro minijuego: sal primero (/survivor salir, /carrera salir o /huida salir).";
        if (enSala(p)) { Menu.abrir(p, "jugar"); return null; }
        construir(w);
        if (!Saved.exists(s, p.getUuid())) Saved.save(s, p, true);
        p.getInventory().clear();
        p.clearStatusEffects();
        p.changeGameMode(GameMode.ADVENTURE);
        p.getAbilities().flying = false; p.sendAbilitiesUpdate();
        p.teleport(w, SX, SY, SZ, 180f, 0f);
        darItemMenu(p);
        Menu.abrir(p, "jugar");
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
