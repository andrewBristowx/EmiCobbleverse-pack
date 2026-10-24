package emi.survivors;

import emi.survivors.net.TextoPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;

import java.util.List;

/**
 * El mundo del minijuego: la sala de entrada (la dimension {@code emi_survivors:entrada}, una plataforma de cristal rosa y lila con los peluches gigantes del Michialien) y las
 * arenas ({@code emi_survivors:arena}). Se entra con {@code /survivor entrar}: se guarda lo que llevabas (inventario, sitio, modo de juego...), se te lleva a la sala y se abre el
 * menu (jugar, mejoras, grupo, top). Con {@code /survivor salir} (o el boton «Salir» del menu) vuelves a tu sitio con tus cosas. Al acabar una partida vuelves a la sala.
 */
public final class Mundo {
    private Mundo() {}

    static final Identifier ID_SALA = Identifier.of("emi_survivors", "entrada");
    /** El bloque que abre el menu al tocarlo (una mesa de encantamientos en el centro de la sala). */
    static final BlockPos TERMINAL = new BlockPos(0, -60, 0);
    static final double SALA_X = 0.5, SALA_Y = -60.0, SALA_Z = 10.5;
    static final int RADIO_SALA = 18;
    private static final String ETIQUETA_MENU = "emi_survivors_menu";

    static ServerWorld sala() {
        MinecraftServer s = Partida.server;
        return s == null ? null : s.getWorld(RegistryKey.of(RegistryKeys.WORLD, ID_SALA));
    }

    static boolean enSala(PlayerEntity p) { return p.getWorld().getRegistryKey().getValue().equals(ID_SALA); }

    /** ¿Esta en la arena de algun bioma (la pradera, el bosque o el cosmos)? */
    static boolean enArena(PlayerEntity p) {
        Identifier id = p.getWorld().getRegistryKey().getValue();
        return "emi_survivors".equals(id.getNamespace()) && Biomas.esArena(id.getPath());
    }

    /** El mundo (dimension) de las arenas de ese bioma, o null si el servidor no lo tiene. */
    static ServerWorld arena(Biomas.Bioma b) {
        MinecraftServer s = Partida.server;
        return s == null ? null : s.getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of("emi_survivors", b.dimension())));
    }

    /** ¿Esta en el mundo del minijuego (la sala o una arena)? */
    public static boolean dentro(PlayerEntity p) { return enSala(p) || enArena(p); }

    /** En la sala no se toca nada (ni bloques, ni entidades, ni objetos) salvo el terminal y el objeto del menu. */
    public static boolean bloqueado(PlayerEntity p) { return Partida.participa(p) || enSala(p); }

    // ------------------------------------------------------------------------------------------------ objeto del menu

    static ItemStack itemMenu() {
        ItemStack st = new ItemStack(Items.NETHER_STAR);
        st.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Menú de Emi Survivors").styled(s -> s.withColor(Formatting.GOLD).withBold(true).withItalic(false)));
        st.set(DataComponentTypes.LORE, new LoreComponent(List.of(Text.literal("Clic derecho: jugar, mejoras y top").styled(s -> s.withColor(Formatting.GRAY).withItalic(false)))));
        NbtCompound tag = new NbtCompound();
        tag.putBoolean(ETIQUETA_MENU, true);
        st.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
        return st;
    }

    static boolean esItemMenu(ItemStack st) {
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

    /** Cada segundo: a quien esta en la sala se le devuelve el objeto del menu si lo ha perdido; a quien se ha ido de la sala sin pasar por SALIR, sus cosas. */
    static void tick(MinecraftServer s) {
        if (s.getTicks() % 20 != 7) return;
        ServerWorld w = sala();
        if (w != null) {
            for (ServerPlayerEntity p : w.getPlayers()) if (!Partida.participa(p)) darItemMenu(p);
            List<net.minecraft.entity.Entity> suelo = new java.util.ArrayList<>();          // objetos tirados al suelo de la sala (la estrella del menu...): se quitan, no se acumulan
            for (net.minecraft.entity.Entity e : w.iterateEntities()) if (e instanceof net.minecraft.entity.ItemEntity) suelo.add(e);
            for (net.minecraft.entity.Entity e : suelo) e.discard();
        }
        // alguien con lo suyo guardado que ya no esta ni en la sala ni en una arena (se fue con un comando de otro mod: /spawn, /home, /tpa...): se le devuelve lo suyo donde este
        for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
            if (dentro(p) || Partida.participa(p) || !Saved.exists(s, p.getUuid()) || !Saved.enLobby(s, p.getUuid())) continue;
            try {
                GrupoCtl.salir(p.getUuid(), p.getGameProfile().getName(), false);
                Morph.restaurar(s, p, Saved.morph(s, p.getUuid()));
                if (Saved.restore(s, p, false)) { Menu.cerrar(p); p.sendMessage(Msg.say("Has salido del mundo del minijuego: te hemos devuelto tus cosas."), false); }
            } catch (Throwable t) { EmiSurvivors.LOG.error("No se pudo devolver sus cosas a {}", p.getGameProfile().getName(), t); }
        }
    }

    // ------------------------------------------------------------------------------------------------ entrar y salir

    /** {@code /survivor entrar}: guarda lo que llevas y te lleva a la sala. Devuelve el motivo si no se puede, o null. */
    public static String entrar(ServerPlayerEntity p) {
        MinecraftServer s = Partida.server;
        if (s == null || Partida.datos == null) return "El minijuego aun no esta listo.";
        ServerWorld w = sala();
        if (w == null) return "No existe la dimension emi_survivors:entrada (¿esta instalado el mod en el servidor?).";
        if (!ServerPlayNetworking.canSend(p, TextoPayload.ID)) return "Tu juego no tiene el mod Emi Survivors (hace falta para ver el menú): actualiza el pack y vuelve a entrar.";
        if (Partida.participa(p)) return "Ya estás en una partida.";
        if ("emi_carreras".equals(p.getWorld().getRegistryKey().getValue().getNamespace())) return "Estás en las carreras: sal primero (/carrera salir).";
        if ("emi_huida".equals(p.getWorld().getRegistryKey().getValue().getNamespace())) return "Estás en la huida: sal primero (/huida salir).";
        if ("emi_cafe".equals(p.getWorld().getRegistryKey().getValue().getNamespace())) return "Estás en el café: sal primero (/cafe salir).";
        if (enSala(p)) { Menu.enviar(p, "jugar", null); return null; }                 // ya estaba en la sala: solo se le vuelve a abrir el menu
        if (enArena(p) && Saved.exists(s, p.getUuid())) { volverASala(p, null); return null; }   // en una arena sin partida (se quedo a medias): de vuelta a la sala
        if (!Morph.disponible()) return "Falta el mod CobbleMorph (el jugador va transformado en su Pokémon).";
        boolean yaGuardado = Saved.exists(s, p.getUuid()) && Saved.enLobby(s, p.getUuid());       // ya habia entrado y algo lo saco de la sala: sus cosas siguen guardadas
        if (Saved.exists(s, p.getUuid()) && !yaGuardado) return "Tienes cosas guardadas de una partida anterior: sal y vuelve a entrar al servidor para recuperarlas.";
        if (!yaGuardado) { try { Saved.save(s, p, true); } catch (RuntimeException e) { return "No se pudo guardar tu inventario: no entras."; } }
        try {
            construirSala(w);
            Morph.restaurar(s, p, "");                    // en la sala vas como persona
            preparar(p);
            p.teleport(w, SALA_X, SALA_Y, SALA_Z, 180f, 0f);
            darItemMenu(p);
        } catch (Throwable t) {
            EmiSurvivors.LOG.error("No se pudo entrar a la sala", t);
            try { Morph.restaurar(s, p, Saved.morph(s, p.getUuid())); Saved.restore(s, p); } catch (Throwable t2) { EmiSurvivors.LOG.error("Ademas, no se pudo devolver al jugador a su sitio", t2); }
            return "No se pudo entrar (mira el registro del servidor); te hemos devuelto a tu sitio.";
        }
        Msg.title(p, Text.literal("¡Bienvenido!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal("El mundo de Emi y los Michialiens").formatted(Formatting.LIGHT_PURPLE), 10, 50, 15);
        EmiSurvivors.LOG.info("{} entra al mundo del minijuego", p.getGameProfile().getName());
        Menu.enviar(p, "jugar", null);
        return null;
    }

    /** {@code /survivor salir}: te devuelve a tu sitio con tus cosas (si estabas en una partida, se acaba). */
    public static String salir(ServerPlayerEntity p) {
        MinecraftServer s = Partida.server;
        if (s == null) return "El minijuego aun no esta listo.";
        Run r = Partida.de(p.getUuid());
        if (r != null && !r.eq.modoPrueba) Partida.quitar(r, "Has salido del minijuego.");
        GrupoCtl.salir(p.getUuid(), p.getGameProfile().getName(), false);                  // al salir del mundo del minijuego se deja el grupo
        if (!Saved.exists(s, p.getUuid())) return dentro(p) ? "No hay nada que devolverte (pide ayuda a un administrador)." : "No estás en el mundo del minijuego.";
        String morph = Saved.morph(s, p.getUuid());
        Morph.restaurar(s, p, morph);
        if (!Saved.restore(s, p)) return "No se pudieron devolver tus cosas (mira el registro del servidor).";
        Menu.cerrar(p);
        p.sendMessage(Msg.say("Has vuelto a tu sitio. ¡Hasta pronto!"), false);
        return null;
    }

    /** Modo sala: aventura e inmortal, sin inventario ni efectos. */
    private static void preparar(ServerPlayerEntity p) {
        Partida.quitarEfectos(p);
        p.clearStatusEffects();
        p.getInventory().clear();
        p.changeGameMode(GameMode.ADVENTURE);
        p.setInvulnerable(true);
        p.getAbilities().flying = false;
        p.sendAbilitiesUpdate();
        p.getHungerManager().setFoodLevel(20); p.getHungerManager().setSaturationLevel(5f);
        p.setHealth(p.getMaxHealth());
    }

    /** Al acabar una partida (o al volver a entrar con la partida perdida): de vuelta a la sala, sin transformar y con el menu abierto (con el resultado si lo hay). */
    static void volverASala(ServerPlayerEntity p, Menu.Resultado res) {
        MinecraftServer s = Partida.server;
        ServerWorld w = sala();
        if (s == null || w == null) return;
        Morph.restaurar(s, p, "");
        preparar(p);
        p.teleport(w, SALA_X, SALA_Y, SALA_Z, 180f, 0f);
        darItemMenu(p);
        Menu.enviar(p, "jugar", res);
    }

    /** Un jugador que vuelve a entrar al servidor: si se quedo en una arena (la partida se perdio al desconectarse), vuelve a la sala. */
    static void alEntrar(ServerPlayerEntity p) {
        MinecraftServer s = Partida.server;
        if (s == null || Partida.participa(p)) return;
        if (enArena(p)) { volverASala(p, null); return; }
        if (!enSala(p) && Saved.exists(s, p.getUuid())) {
            // tiene sus cosas guardadas del minijuego pero esta fuera de su mundo (p. ej. un administrador lo llevo a otro sitio): se le devuelve todo
            Morph.restaurar(s, p, Saved.morph(s, p.getUuid()));
            if (Saved.restore(s, p)) p.sendMessage(Msg.say("Te hemos devuelto tus cosas del minijuego."), false);
        }
    }

    // ------------------------------------------------------------------------------------------------ construir la sala

    /** Construye la sala la primera vez (queda guardada en el mundo; la marca es un bloque de obsidiana llorona bajo el centro). */
    static boolean construirSala(ServerWorld w) {
        if (w.getBlockState(new BlockPos(0, -64, 0)).isOf(Blocks.CRYING_OBSIDIAN)) return false;
        long t0 = System.nanoTime();
        int R = RADIO_SALA;
        // ---- la plataforma: damero rosa y lila, circulo central blanco y aro dorado; debajo, cristal y calcita en forma de cono
        for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > R) continue;
            String top = d < 3.2 ? "white_concrete" : d < 5.6 ? "smooth_quartz" : d >= 15.4 && d < 16.6 ? "yellow_concrete" : ((dx + dz) & 1) == 0 ? "pink_concrete" : "purple_concrete";
            Obra.poner(w, dx, -61, dz, top);
            if (d <= R - 1) Obra.poner(w, dx, -62, dz, "amethyst_block");
            if (d <= R - 5) Obra.poner(w, dx, -63, dz, "calcite");
            if (d <= R - 9) Obra.poner(w, dx, -64, dz, "calcite");
        }
        // ---- el borde: un murete de amatista, farolas de luz perlada y un muro invisible para no caerse
        for (int dx = -R - 2; dx <= R + 2; dx++) for (int dz = -R - 2; dz <= R + 2; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d >= R - 0.6 && d <= R + 0.4) Obra.poner(w, dx, -60, dz, "amethyst_block");
            if (d > R + 0.4 && d <= R + 1.8) for (int y = -60; y <= -53; y++) Obra.poner(w, dx, y, dz, Blocks.BARRIER.getDefaultState());
        }
        for (int k = 0; k < 24; k++) {
            double a = k * Math.PI * 2 / 24;
            int x = (int) Math.round(Math.cos(a) * (R - 0.2)), z = (int) Math.round(Math.sin(a) * (R - 0.2));
            Obra.poner(w, x, -59, z, "pearlescent_froglight");
            Obra.poner(w, x, -58, z, "end_rod");
        }
        // ---- el terminal del menu: una mesa de encantamientos con cuatro farolas alrededor
        Obra.poner(w, TERMINAL.getX(), TERMINAL.getY(), TERMINAL.getZ(), "enchanting_table");
        for (int[] c : new int[][]{{2, 2}, {-2, 2}, {2, -2}, {-2, -2}}) {
            Obra.poner(w, c[0], -60, c[1], "pearlescent_froglight");
            Obra.poner(w, c[0], -59, c[1], "end_rod");
        }
        // ---- los dos peluches gigantes del fondo, en islotes de cristal, mirando a quien llega
        String[] formas = {"michi_gold_crown_28", "michi_base_28"};
        for (int k = 0; k < 2; k++) {
            int sx = k == 0 ? -34 : 34, sz = -46;
            for (int cap = 0; cap < 5; cap++) Obra.disco(w, sx, -61 - Math.min(cap, 3), sz, 24 - cap * 3.5, Obra.b(cap < 2 ? "amethyst_block" : "calcite"));
            for (double b = 0; b < Math.PI * 2; b += 0.03) Obra.poner(w, sx + (int) Math.round(Math.cos(b) * 23), -61, sz + (int) Math.round(Math.sin(b) * 23), "pink_concrete");
            Obra.ponerEstatua(w, formas[k], sx, -60, sz, Obra.girarHacia(sx, sz, SALA_X, SALA_Z));
        }
        Obra.poner(w, 0, -64, 0, Blocks.CRYING_OBSIDIAN.getDefaultState());
        EmiSurvivors.LOG.info("Sala de entrada construida en {} ms", (System.nanoTime() - t0) / 1_000_000);
        return true;
    }
}
