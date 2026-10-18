package emi.huida;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.List;

/**
 * La sala de entrada de la huida (la dimension {@code emi_huida:entrada}): unas ruinas en la selva con la puerta dorada de la meta y, en un pedestal, el peluche del dinosaurio
 * que se compra en la tienda. Desde ella se abre una huida, se espera a los demas, se corre solo, se ven los logros y las misiones y se compra. Se entra con {@code /huida entrar}
 * (se guarda lo que llevabas: inventario, sitio, modo de juego...) y se sale con {@code /huida salir}. Al acabar una huida se vuelve a la sala, no al sitio de antes.
 */
public final class Sala {
    private Sala() {}

    static final Identifier ID = Identifier.of("emi_huida", "entrada");
    /** El bloque que abre el menu al tocarlo (una mesa de encantamientos en el centro de la sala). */
    static final BlockPos TERMINAL = new BlockPos(0, -60, 0);
    static final double X = 0.5, Y = -60.0, Z = 12.5;
    static final int RADIO = 20;
    private static final String ETIQUETA_MENU = "emi_huida_menu";

    static ServerWorld sala() {
        MinecraftServer s = Huida.srv();
        return s == null ? null : s.getWorld(RegistryKey.of(RegistryKeys.WORLD, ID));
    }

    public static boolean enSala(PlayerEntity p) { return p.getWorld().getRegistryKey().getValue().equals(ID); }

    /** En la sala no se toca nada (ni bloques, ni entidades, ni objetos) salvo el terminal y el objeto del menu. */
    public static boolean bloqueado(PlayerEntity p) { return enSala(p); }

    // ------------------------------------------------------------------------------------------------ objeto del menu

    static ItemStack itemMenu() {
        ItemStack st = new ItemStack(Items.NETHER_STAR);
        st.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Menú de Emi Huida").styled(s -> s.withColor(Formatting.GOLD).withBold(true).withItalic(false)));
        st.set(DataComponentTypes.LORE, new LoreComponent(List.of(Text.literal("Clic derecho: huida, logros, misiones y tienda").styled(s -> s.withColor(Formatting.GRAY).withItalic(false)))));
        NbtCompound tag = new NbtCompound();
        tag.putBoolean(ETIQUETA_MENU, true);
        st.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
        return st;
    }

    public static boolean esItemMenu(ItemStack st) {
        if (st == null || st.isEmpty()) return false;
        NbtComponent c = st.get(DataComponentTypes.CUSTOM_DATA);
        return c != null && c.contains(ETIQUETA_MENU);
    }

    private static void darItemMenu(ServerPlayerEntity p) {
        for (int i = 0; i < p.getInventory().size(); i++) if (esItemMenu(p.getInventory().getStack(i))) return;
        p.getInventory().setStack(4, itemMenu());
        p.getInventory().selectedSlot = 4;
        p.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(4));          // el cliente no se entera solo de que cambia la ranura elegida
    }

    private static int ultimaFirma = Integer.MIN_VALUE;

    /** Cada medio segundo: si ha cambiado la huida (alguien se apunta, empieza, termina...), se refresca el menu de todos los que estan en la sala. */
    private static void empujar(MinecraftServer s) {
        if (s.getTicks() % 10 != 3) return;
        int f = Huida.firma();
        if (f == ultimaFirma) return;
        ultimaFirma = f;
        for (ServerPlayerEntity p : jugadoresSala()) if (!Huida.participa(p)) Menu.actualizar(p);
    }

    static List<ServerPlayerEntity> jugadoresSala() {
        ServerWorld w = sala();
        return w == null ? List.of() : new ArrayList<>(w.getPlayers());
    }

    /** Cada tick del servidor: refresca los menus, y cada segundo da el objeto del menu, limpia el suelo, para los eventos de Emipokemon y devuelve las cosas a quien se fue de la sala sin pasar por SALIR. */
    static void tick(MinecraftServer s) {
        if (Huida.srv() == null) return;
        empujar(s);
        if (s.getTicks() % 20 == 3) Pausas.actualizar(s);                 // con alguien en la sala o en la huida, Emipokemon no limpia ni lanza bosses
        if (s.getTicks() % 20 != 7) return;
        ServerWorld w = sala();
        if (w != null) {
            for (ServerPlayerEntity p : w.getPlayers()) if (!Huida.participa(p)) darItemMenu(p);
            List<Entity> suelo = new ArrayList<>();          // objetos tirados al suelo de la sala (la estrella del menu...): se quitan, no se acumulan
            for (Entity e : w.iterateEntities()) if (e instanceof ItemEntity) suelo.add(e);
            for (Entity e : suelo) e.discard();
        }
        // alguien con lo suyo guardado en la sala que ya no esta ni en la sala ni en la huida (se fue con un comando de otro mod: /spawn, /home, /tpa...): se le devuelve lo suyo donde este
        for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
            if (enSala(p) || Huida.participa(p) || Huida.esMundoHuida(p.getServerWorld().getRegistryKey()) || !Saved.exists(s, p.getUuid()) || !Saved.enLobby(s, p.getUuid())) continue;
            try {
                Huida.sacarDeLaCola(p);
                if (Saved.restore(s, p)) p.sendMessage(Msg.say("Has salido de la sala de la huida: te hemos devuelto tus cosas."), false);
            } catch (Throwable t) { EmiHuida.LOG.error("No se pudo devolver sus cosas a {}", p.getGameProfile().getName(), t); }
        }
    }

    // ------------------------------------------------------------------------------------------------ entrar y salir

    /** {@code /huida entrar}: guarda lo que llevas y te lleva a la sala. Devuelve el motivo si no se puede, o null. */
    public static String entrar(ServerPlayerEntity p) {
        MinecraftServer s = Huida.srv();
        if (s == null) return "La huida aun no esta lista.";
        ServerWorld w = sala();
        if (w == null) return "No existe la dimension emi_huida:entrada (¿esta instalado el mod en el servidor?).";
        if (Huida.participa(p)) return "Ya estás en una huida.";
        String ns = p.getWorld().getRegistryKey().getValue().getNamespace();
        if (ns.equals("emi_survivors") || ns.equals("emi_carreras")) return "Estás en otro minijuego: sal primero (/survivor salir o /carrera salir).";
        if (enSala(p)) { Menu.abrir(p, "jugar"); return null; }                       // ya estaba en la sala: solo se le vuelve a abrir el menu
        if (Huida.esMundoHuida(p.getServerWorld().getRegistryKey()) && Saved.exists(s, p.getUuid()) && Saved.enLobby(s, p.getUuid())) { volverASala(p, null); return null; }   // en la pista sin huida (se quedo a medias): de vuelta a la sala
        if (Morph.transformado(p.getUuid())) return "Destransfórmate antes (/untransform) para jugar la huida.";
        if (p.hasVehicle()) return "Bájate de lo que montas.";
        boolean yaGuardado = Saved.exists(s, p.getUuid()) && Saved.enLobby(s, p.getUuid());       // ya habia entrado y algo lo saco de la sala: sus cosas siguen guardadas
        if (Saved.exists(s, p.getUuid()) && !yaGuardado) return "Tienes cosas guardadas de una huida anterior: sal y vuelve a entrar al servidor para recuperarlas.";
        if (!yaGuardado) { try { Saved.save(s, p, true); } catch (RuntimeException e) { return "No se pudo guardar tu inventario: no entras."; } }
        try {
            construir(w);
            preparar(p);
            p.teleport(w, X, Y, Z, 180f, 0f);
            darItemMenu(p);
        } catch (Throwable t) {
            EmiHuida.LOG.error("No se pudo entrar a la sala", t);
            try { Saved.restore(s, p); } catch (Throwable t2) { EmiHuida.LOG.error("Ademas, no se pudo devolver al jugador a su sitio", t2); }
            return "No se pudo entrar (mira el registro del servidor); te hemos devuelto a tu sitio.";
        }
        Msg.title(p, Text.literal("¡Bienvenido!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal("La huida: corre, salta y no te dejes atrapar").formatted(Formatting.LIGHT_PURPLE), 10, 50, 15);
        EmiHuida.LOG.info("{} entra a la sala de la huida", p.getGameProfile().getName());
        Menu.abrir(p, "jugar");
        return null;
    }

    /** {@code /huida salir} fuera de una huida: te devuelve a tu sitio con tus cosas. */
    public static String salir(ServerPlayerEntity p) {
        MinecraftServer s = Huida.srv();
        if (s == null) return "La huida aun no esta lista.";
        Huida.sacarDeLaCola(p);
        if (!Saved.exists(s, p.getUuid())) return enSala(p) ? "No hay nada que devolverte (pide ayuda a un administrador)." : "No estás en la sala de la huida.";
        if (!Saved.restore(s, p)) return "No se pudieron devolver tus cosas (mira el registro del servidor).";
        Menu.cerrar(p);
        p.sendMessage(Msg.say("Has vuelto a tu sitio. ¡Hasta pronto!"), false);
        return null;
    }

    /** Modo sala: aventura e inmortal, sin inventario ni efectos. */
    private static void preparar(ServerPlayerEntity p) {
        Huida.quitarEfectos(p);
        p.setGlowing(false);
        p.clearStatusEffects();
        p.getInventory().clear();
        p.changeGameMode(GameMode.ADVENTURE);
        p.setInvulnerable(true);
        p.getAbilities().flying = false;
        p.sendAbilitiesUpdate();
        p.getHungerManager().setFoodLevel(20); p.getHungerManager().setSaturationLevel(5f);
        p.setHealth(p.getMaxHealth());
    }

    /** Al acabar una huida (o al volver a entrar con la huida perdida): de vuelta a la sala, con el menu abierto (y con lo que has ganado si lo hay). */
    static void volverASala(ServerPlayerEntity p, String resultadoJson) {
        MinecraftServer s = Huida.srv();
        ServerWorld w = sala();
        if (s == null || w == null) return;
        if (p.hasVehicle()) p.stopRiding();
        preparar(p);
        p.teleport(w, X, Y, Z, 180f, 0f);
        darItemMenu(p);
        Menu.abrir(p, "jugar", resultadoJson);
    }

    /** Un jugador que vuelve a entrar al servidor: si se quedo en el nivel (la huida se perdio al desconectarse), vuelve a la sala; si guardo cosas sin ser de la sala, se las devolvemos. */
    static void alEntrar(ServerPlayerEntity p) {
        MinecraftServer s = Huida.srv();
        if (s == null || Huida.participa(p)) return;
        if (!Saved.exists(s, p.getUuid())) return;
        if (Saved.enLobby(s, p.getUuid())) {
            if (!enSala(p)) { Huida.sacarDeLaCola(p); volverASala(p, null); }
            return;
        }
        if (Saved.restore(s, p)) p.sendMessage(Msg.say("Te hemos devuelto tus cosas de la huida."), false);
    }

    // ------------------------------------------------------------------------------------------------ construir la sala

    /** El bloque de otro mod con su estado (p. ej. {@code emi_plush:gatitoalien_dino} mirando al sur); null si el bloque no existe en este servidor. */
    private static BlockState estado(String id, String propiedad, String valor) {
        Block b = Registries.BLOCK.get(Identifier.of(id));
        if (b == null || b == Blocks.AIR) return null;
        BlockState st = b.getDefaultState();
        for (Property<?> pr : st.getProperties()) if (pr.getName().equals(propiedad)) return con(st, pr, valor);
        return st;
    }

    private static <T extends Comparable<T>> BlockState con(BlockState s, Property<T> p, String valor) { return p.parse(valor).map(x -> s.with(p, x)).orElse(s); }

    private static String ladrillo(int dx, int dz) {
        int h = Math.floorMod(dx * 73856093 ^ dz * 19349663, 7);
        return h < 3 ? "mossy_stone_bricks" : h < 5 ? "stone_bricks" : h == 5 ? "cracked_stone_bricks" : "mossy_cobblestone";
    }

    /** Construye la sala la primera vez (queda guardada en el mundo; la marca es un bloque de obsidiana llorona bajo el centro). */
    static boolean construir(ServerWorld w) {
        if (w.getBlockState(new BlockPos(0, -64, 0)).isOf(Blocks.CRYING_OBSIDIAN)) return false;
        long t0 = System.nanoTime();
        int R = RADIO;
        // ---- la plataforma: ruinas de piedra con musgo, un aro de hierba de selva por fuera y un circulo de piedra tallada en el centro
        for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > R) continue;
            String top;
            if (d < 3.6) top = "chiseled_stone_bricks";
            else if (d < 5.8) top = "smooth_stone";
            else if (d >= R - 3.5) top = ((dx * 31 + dz * 17) & 3) == 0 ? "moss_block" : "grass_block";
            else if (d >= 11.4 && d < 12.6 && ((int) Math.floor(Math.atan2(dz, dx) * 8 / Math.PI) & 1) == 0) top = "gold_block";     // anillo dorado discontinuo
            else top = ladrillo(dx, dz);
            Obra.poner(w, dx, -61, dz, top);
            if (d <= R - 1) Obra.poner(w, dx, -62, dz, "dirt");
            if (d <= R - 5) Obra.poner(w, dx, -63, dz, "stone");
            if (d <= R - 9) Obra.poner(w, dx, -64, dz, "stone");
            // hierbas, helechos y flores sobre el aro verde
            if (d >= R - 3.5 && d < R - 1.2 && ((dx * 7 + dz * 13) & 7) == 0) Obra.poner(w, dx, -60, dz, (dx + dz) % 3 == 0 ? "fern" : "short_grass");
        }
        // ---- el borde: un murete de piedra con musgo con lamparas, y un muro invisible para no caerse
        for (int dx = -R - 2; dx <= R + 2; dx++) for (int dz = -R - 2; dz <= R + 2; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d >= R - 0.6 && d <= R + 0.4) Obra.poner(w, dx, -60, dz, "mossy_stone_brick_wall");
            if (d > R + 0.4 && d <= R + 1.8) for (int y = -60; y <= -53; y++) Obra.poner(w, dx, y, dz, Blocks.BARRIER.getDefaultState());
        }
        for (int k = 0; k < 24; k++) {
            double a = k * Math.PI * 2 / 24;
            int x = (int) Math.round(Math.cos(a) * (R - 0.2)), z = (int) Math.round(Math.sin(a) * (R - 0.2));
            Obra.poner(w, x, -59, z, "lantern");
        }
        // ---- la puerta dorada de la meta (frente a quien llega): dos columnas de ruinas y una viga dorada con una fila de lamparas
        for (int y = -60; y <= -53; y++) for (int sx : new int[]{-7, 7}) {
            Obra.poner(w, sx, y, -9, y == -60 || y == -53 ? "chiseled_stone_bricks" : (y & 1) == 0 ? "mossy_stone_bricks" : "stone_bricks");
            Obra.poner(w, sx + (sx < 0 ? -1 : 1), y, -9, (y & 1) == 0 ? "stone_bricks" : "mossy_stone_bricks");
        }
        for (int x = -8; x <= 8; x++) for (int y = -52; y <= -51; y++) Obra.poner(w, x, y, -9, y == -52 ? "gold_block" : "chiseled_stone_bricks");
        for (int x = -7; x <= 7; x += 2) Obra.poner(w, x, -50, -9, "lantern");
        for (int x = -6; x <= 6; x++) if ((x & 1) == 0) Obra.poner(w, x, -53, -9, Blocks.JUNGLE_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true));           // hojas colgando de la viga
        // ---- el terminal del menu: una mesa de encantamientos con cuatro farolas alrededor
        Obra.poner(w, TERMINAL.getX(), TERMINAL.getY(), TERMINAL.getZ(), "enchanting_table");
        for (int[] c : new int[][]{{2, 2}, {-2, 2}, {2, -2}, {-2, -2}}) {
            Obra.poner(w, c[0], -60, c[1], "jungle_fence");
            Obra.poner(w, c[0], -59, c[1], "jungle_fence");
            Obra.poner(w, c[0], -58, c[1], "lantern");
        }
        // ---- el peluche del dinosaurio (el premio de la tienda) en un pedestal dorado, mirando a quien llega
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) Obra.poner(w, dx, -60, -7 + dz, "gold_block");
        Obra.poner(w, 0, -60, -7, "gold_block");
        BlockState dino = estado("emi_plush:gatitoalien_dino", "facing", "south");
        if (dino != null) Obra.poner(w, 0, -59, -7, dino); else EmiHuida.LOG.warn("No hay peluche del dinosaurio (¿emi-plush 1.18.0?): el pedestal de la sala queda vacio.");
        for (int sx : new int[]{-3, 3}) { Obra.poner(w, sx, -60, -7, "jungle_fence"); Obra.poner(w, sx, -59, -7, "jungle_fence"); Obra.poner(w, sx, -58, -7, "lantern"); }
        // ---- cuatro arboles de selva por los rincones
        int[][] arboles = {{-15, -13}, {15, -13}, {-16, 6}, {16, 6}};
        for (int[] a : arboles) arbol(w, a[0], a[1], 8 + Math.floorMod(a[0] * 3 + a[1], 3));
        Obra.poner(w, 0, -64, 0, Blocks.CRYING_OBSIDIAN.getDefaultState());
        EmiHuida.LOG.info("Sala de la huida construida en {} ms", (System.nanoTime() - t0) / 1_000_000);
        return true;
    }

    private static void arbol(ServerWorld w, int cx, int cz, int alto) {
        BlockState hoja = Blocks.JUNGLE_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
        for (int y = 0; y < alto; y++) Obra.poner(w, cx, -60 + y, cz, "jungle_log");
        int cy = -60 + alto - 2;
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) for (int dy = -1; dy <= 3; dy++) {
            double d = Math.sqrt(dx * dx + dz * dz + (dy - 0.8) * (dy - 0.8) * 1.6);
            if (d <= 3.1 && !(dx == 0 && dz == 0 && dy <= 0)) Obra.poner(w, cx + dx, cy + dy, cz + dz, hoja);
        }
        Obra.poner(w, cx, cy + 3, cz, hoja);
    }
}
