package emi.carreras;

import net.minecraft.block.Blocks;
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

import java.util.ArrayList;
import java.util.List;

/**
 * La sala de entrada de las carreras (la dimension {@code emi_carreras:entrada}): un circuito circular con meta de cuadros y los peluches gigantes del Michialien, donde se espera
 * a los demas corredores, se elige pista, se entrena en solitario, se ven los logros y se compra en la tienda. Se entra con {@code /carrera entrar} (se guarda lo que llevabas:
 * inventario, sitio, modo de juego...) y se sale con {@code /carrera salir}. Al acabar una carrera se vuelve a la sala, no al sitio de antes.
 */
public final class Sala {
    private Sala() {}

    static final Identifier ID = Identifier.of("emi_carreras", "entrada");
    /** El bloque que abre el menu al tocarlo (una mesa de encantamientos en el centro de la sala). */
    static final BlockPos TERMINAL = new BlockPos(0, -60, 0);
    static final double X = 0.5, Y = -60.0, Z = 12.5;
    static final int RADIO = 20;
    private static final String ETIQUETA_MENU = "emi_carreras_menu";

    static ServerWorld sala() {
        MinecraftServer s = Carrera.server;
        return s == null ? null : s.getWorld(RegistryKey.of(RegistryKeys.WORLD, ID));
    }

    public static boolean enSala(PlayerEntity p) { return p.getWorld().getRegistryKey().getValue().equals(ID); }

    /** En la sala no se toca nada (ni bloques, ni entidades, ni objetos) salvo el terminal y el objeto del menu. */
    public static boolean bloqueado(PlayerEntity p) { return enSala(p); }

    // ------------------------------------------------------------------------------------------------ objeto del menu

    static ItemStack itemMenu() {
        ItemStack st = new ItemStack(Items.NETHER_STAR);
        st.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Menú de Emi Carreras").styled(s -> s.withColor(Formatting.GOLD).withBold(true).withItalic(false)));
        st.set(DataComponentTypes.LORE, new LoreComponent(List.of(Text.literal("Clic derecho: carreras, entrenamiento, logros y tienda").styled(s -> s.withColor(Formatting.GRAY).withItalic(false)))));
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

    /** Cada segundo: a quien esta en la sala se le devuelve el objeto del menu si lo ha perdido; a quien se ha ido de la sala sin pasar por SALIR, sus cosas. */
    static void tick(MinecraftServer s) {
        if (s.getTicks() % 20 != 7 || Carrera.server == null) return;
        ServerWorld w = sala();
        if (w != null) {
            for (ServerPlayerEntity p : w.getPlayers()) if (!Carrera.participa(p)) darItemMenu(p);
            List<Entity> suelo = new ArrayList<>();          // objetos tirados al suelo de la sala (la estrella del menu...): se quitan, no se acumulan
            for (Entity e : w.iterateEntities()) if (e instanceof ItemEntity) suelo.add(e);
            for (Entity e : suelo) e.discard();
        }
        // alguien con lo suyo guardado en la sala que ya no esta ni en la sala ni en la pista (se fue con un comando de otro mod: /spawn, /home, /tpa...): se le devuelve lo suyo donde este
        for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
            if (enSala(p) || Carrera.participa(p) || p.getWorld().getRegistryKey() == Carrera.MUNDO || !Saved.exists(s, p.getUuid()) || !Saved.enLobby(s, p.getUuid())) continue;
            try {
                Carrera.sacarDeLaCola(p);
                if (Saved.restore(s, p)) p.sendMessage(Msg.say("Has salido de la sala de carreras: te hemos devuelto tus cosas."), false);
            } catch (Throwable t) { EmiCarreras.LOG.error("No se pudo devolver sus cosas a {}", p.getGameProfile().getName(), t); }
        }
    }

    // ------------------------------------------------------------------------------------------------ entrar y salir

    /** {@code /carrera entrar}: guarda lo que llevas y te lleva a la sala. Devuelve el motivo si no se puede, o null. */
    public static String entrar(ServerPlayerEntity p) {
        MinecraftServer s = Carrera.server;
        if (s == null) return "Las carreras aun no estan listas.";
        ServerWorld w = sala();
        if (w == null) return "No existe la dimension emi_carreras:entrada (¿esta instalado el mod en el servidor?).";
        if (Carrera.participa(p)) return "Ya estás en una carrera.";
        if (enSala(p)) { Menu.abrir(p, "jugar"); return null; }                       // ya estaba en la sala: solo se le vuelve a abrir el menu
        if (p.getWorld().getRegistryKey() == Carrera.MUNDO && Saved.exists(s, p.getUuid())) { volverASala(p, null); return null; }   // en la pista sin carrera (se quedo a medias): de vuelta a la sala
        if (!Morph.disponible()) return "Falta el mod CobbleMorph (el jugador va transformado en su Pokémon).";
        boolean yaGuardado = Saved.exists(s, p.getUuid()) && Saved.enLobby(s, p.getUuid());       // ya habia entrado y algo lo saco de la sala: sus cosas siguen guardadas
        if (Saved.exists(s, p.getUuid()) && !yaGuardado) return "Tienes cosas guardadas de una carrera anterior: sal y vuelve a entrar al servidor para recuperarlas.";
        if (!yaGuardado) { try { Saved.save(s, p, true); } catch (RuntimeException e) { return "No se pudo guardar tu inventario: no entras."; } }
        try {
            construir(w);
            Morph.restaurar(s, p, "");                    // en la sala vas como persona
            preparar(p);
            p.teleport(w, X, Y, Z, 180f, 0f);
            darItemMenu(p);
        } catch (Throwable t) {
            EmiCarreras.LOG.error("No se pudo entrar a la sala", t);
            try { Morph.restaurar(s, p, Saved.morph(s, p.getUuid())); Saved.restore(s, p); } catch (Throwable t2) { EmiCarreras.LOG.error("Ademas, no se pudo devolver al jugador a su sitio", t2); }
            return "No se pudo entrar (mira el registro del servidor); te hemos devuelto a tu sitio.";
        }
        Msg.title(p, Text.literal("¡Bienvenido!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal("Las carreras de Emi y los Michialiens").formatted(Formatting.LIGHT_PURPLE), 10, 50, 15);
        EmiCarreras.LOG.info("{} entra a la sala de carreras", p.getGameProfile().getName());
        Menu.abrir(p, "jugar");
        return null;
    }

    /** {@code /carrera salir} fuera de una carrera: te devuelve a tu sitio con tus cosas. */
    public static String salir(ServerPlayerEntity p) {
        MinecraftServer s = Carrera.server;
        if (s == null) return "Las carreras aun no estan listas.";
        Carrera.sacarDeLaCola(p);
        if (!Saved.exists(s, p.getUuid())) return enSala(p) ? "No hay nada que devolverte (pide ayuda a un administrador)." : "No estás en la sala de carreras.";
        if (!Saved.restore(s, p)) return "No se pudieron devolver tus cosas (mira el registro del servidor).";
        Menu.cerrar(p);
        p.sendMessage(Msg.say("Has vuelto a tu sitio. ¡Hasta pronto!"), false);
        return null;
    }

    /** Modo sala: aventura e inmortal, sin inventario ni efectos. */
    private static void preparar(ServerPlayerEntity p) {
        Montura.quitarJugador(p);
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

    /** Al acabar una carrera (o al volver a entrar con la carrera perdida): de vuelta a la sala, sin transformar y con el menu abierto. */
    static void volverASala(ServerPlayerEntity p, String resultadoJson) {
        MinecraftServer s = Carrera.server;
        ServerWorld w = sala();
        if (s == null || w == null) return;
        if (p.hasVehicle()) p.stopRiding();
        Morph.restaurar(s, p, "");
        preparar(p);
        p.teleport(w, X, Y, Z, 180f, 0f);
        darItemMenu(p);
        Menu.abrir(p, "jugar", resultadoJson);
    }

    /** Lo que hace la carrera con un jugador al terminar o abandonar: si venia de la sala, vuelve a la sala; si no, a donde estaba. */
    static void devolver(MinecraftServer s, ServerPlayerEntity p, String resultadoJson) {
        if (Saved.exists(s, p.getUuid()) && Saved.enLobby(s, p.getUuid())) { Montura.quitarJugador(p); volverASala(p, resultadoJson); }
        else Saved.restore(s, p);
    }

    /** Un jugador que vuelve a entrar al servidor: si se quedo en la pista (la carrera se perdio al desconectarse), vuelve a la sala. */
    static void alEntrar(ServerPlayerEntity p) {
        MinecraftServer s = Carrera.server;
        if (s == null || Carrera.participa(p)) return;
        if (!Saved.exists(s, p.getUuid())) return;
        if (Saved.enLobby(s, p.getUuid())) {
            if (!enSala(p)) { Carrera.sacarDeLaCola(p); volverASala(p, null); }
            return;
        }
        if (Saved.restore(s, p)) p.sendMessage(Msg.say("Se te ha devuelto lo que tenías antes de la carrera."), false);
    }

    // ------------------------------------------------------------------------------------------------ construir la sala

    /** Construye la sala la primera vez (queda guardada en el mundo; la marca es un bloque de obsidiana llorona bajo el centro). */
    static boolean construir(ServerWorld w) {
        if (w.getBlockState(new BlockPos(0, -64, 0)).isOf(Blocks.CRYING_OBSIDIAN)) return false;
        long t0 = System.nanoTime();
        int R = RADIO;
        // ---- la plataforma: asfalto con rayas amarillas, aro exterior de cuadros blanco y negro, circulo central blanco
        for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > R) continue;
            String top;
            if (d < 3.4) top = "white_concrete";
            else if (d < 5.8) top = "smooth_quartz";
            else if (d >= R - 3 && d <= R - 0.6) top = ((dx + dz) & 1) == 0 ? "white_concrete" : "black_concrete";                   // los cuadros de la meta
            else if (d >= 11.4 && d < 12.6 && ((int) Math.floor(Math.atan2(dz, dx) * 8 / Math.PI) & 1) == 0) top = "yellow_concrete";   // rayas discontinuas
            else top = "gray_concrete";
            Obra.poner(w, dx, -61, dz, top);
            if (d <= R - 1) Obra.poner(w, dx, -62, dz, "light_gray_concrete");
            if (d <= R - 5) Obra.poner(w, dx, -63, dz, "stone");
            if (d <= R - 9) Obra.poner(w, dx, -64, dz, "stone");
        }
        // ---- el borde: un murete, farolas y un muro invisible para no caerse
        for (int dx = -R - 2; dx <= R + 2; dx++) for (int dz = -R - 2; dz <= R + 2; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d >= R - 0.6 && d <= R + 0.4) Obra.poner(w, dx, -60, dz, ((dx + dz) & 1) == 0 ? "red_concrete" : "white_concrete");
            if (d > R + 0.4 && d <= R + 1.8) for (int y = -60; y <= -53; y++) Obra.poner(w, dx, y, dz, Blocks.BARRIER.getDefaultState());
        }
        for (int k = 0; k < 24; k++) {
            double a = k * Math.PI * 2 / 24;
            int x = (int) Math.round(Math.cos(a) * (R - 0.2)), z = (int) Math.round(Math.sin(a) * (R - 0.2));
            Obra.poner(w, x, -59, z, "pearlescent_froglight");
            Obra.poner(w, x, -58, z, "end_rod");
        }
        // ---- el arco de meta (frente a quien llega): dos columnas y una viga de cuadros
        for (int y = -60; y <= -53; y++) { Obra.poner(w, -7, y, -9, ((y & 1) == 0) ? "white_concrete" : "black_concrete"); Obra.poner(w, 7, y, -9, ((y & 1) == 0) ? "white_concrete" : "black_concrete"); }
        for (int x = -7; x <= 7; x++) for (int y = -54; y <= -53; y++) Obra.poner(w, x, y, -9, (((x + y) & 1) == 0) ? "white_concrete" : "black_concrete");
        for (int x = -6; x <= 6; x++) Obra.poner(w, x, -52, -9, "red_concrete");
        // ---- el terminal del menu: una mesa de encantamientos con cuatro farolas alrededor
        Obra.poner(w, TERMINAL.getX(), TERMINAL.getY(), TERMINAL.getZ(), "enchanting_table");
        for (int[] c : new int[][]{{2, 2}, {-2, 2}, {2, -2}, {-2, -2}}) {
            Obra.poner(w, c[0], -60, c[1], "pearlescent_froglight");
            Obra.poner(w, c[0], -59, c[1], "end_rod");
        }
        // ---- los dos peluches gigantes del fondo, en islotes, mirando a quien llega
        String[] formas = {"michi_gold_crown_28", "michi_base_28"};
        for (int k = 0; k < 2; k++) {
            int sx = k == 0 ? -34 : 34, sz = -46;
            for (int cap = 0; cap < 5; cap++) Obra.disco(w, sx, -61 - Math.min(cap, 3), sz, 24 - cap * 3.5, Obra.b(cap < 2 ? "gray_concrete" : "stone"));
            for (double b = 0; b < Math.PI * 2; b += 0.03) Obra.poner(w, sx + (int) Math.round(Math.cos(b) * 23), -61, sz + (int) Math.round(Math.sin(b) * 23), ((int) Math.round(b * 10) & 1) == 0 ? "white_concrete" : "black_concrete");
            Obra.ponerEstatua(w, formas[k], sx, -60, sz, Obra.girarHacia(sx, sz, X, Z));
        }
        Obra.poner(w, 0, -64, 0, Blocks.CRYING_OBSIDIAN.getDefaultState());
        EmiCarreras.LOG.info("Sala de carreras construida en {} ms", (System.nanoTime() - t0) / 1_000_000);
        return true;
    }
}
