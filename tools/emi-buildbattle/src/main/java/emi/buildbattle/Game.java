package emi.buildbattle;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Estado y reglas del minijuego. Todo corre en el hilo del servidor. */
public final class Game {
    public enum Phase { IDLE, QUEUE, STARTING, BUILDING, VOTING, RESULTS, ENDING }

    public static final String TAG_NPC = "emi_bb_npc";
    public static final String TAG_LABEL = "emi_bb_label";
    public static final String VOTE_KEY = "emi_bb_vote";
    public static final RegistryKey<World> ARENA = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("emi_buildbattle", "arena"));

    private static MinecraftServer server;
    private static Cfg cfg = new Cfg();

    private static Phase phase = Phase.IDLE;
    private static final LinkedHashSet<UUID> queue = new LinkedHashSet<>();
    private static final Map<UUID, String> names = new HashMap<>();
    private static final Map<UUID, Plot> participants = new LinkedHashMap<>();   // quienes juegan ahora (con su parcela)
    private static final List<Plot> plots = new ArrayList<>();                  // todas las parcelas de la partida (aunque su dueno se haya ido)
    private static String theme = "";
    private static List<Plot> order = new ArrayList<>();
    private static int voteIdx = 0;
    private static final Map<UUID, Map<Integer, Integer>> votes = new HashMap<>();
    private static List<Result> ranking = new ArrayList<>();
    private static long nextReminder = 0;
    private static ServerBossBar bar;
    private static long timerEnd = -1;
    private static long timerStart = -1;
    private static int lastWarn = Integer.MAX_VALUE;
    private static final Random RNG = new Random();

    private static final class Task { long due; Runnable r; Task(long d, Runnable r) { due = d; this.r = r; } }
    private static final List<Task> tasks = new ArrayList<>();

    public record Result(Plot plot, double avg, int sum, int count, int rank) {}

    // ---------------------------------------------------------------- acceso

    public static Cfg cfg() { return cfg; }
    public static Phase phase() { return phase; }
    public static MinecraftServer server() { return server; }
    public static ServerWorld arena() { return server == null ? null : server.getWorld(ARENA); }
    public static boolean isParticipant(PlayerEntity p) { return participants.containsKey(p.getUuid()); }
    public static Plot plotOf(PlayerEntity p) { return participants.get(p.getUuid()); }
    public static boolean isBuilding(PlayerEntity p) { return phase == Phase.BUILDING && isParticipant(p); }
    public static String theme() { return theme; }

    public static void init(MinecraftServer s) {
        server = s;
        cfg = Cfg.load();
        Catalog.rebuild(s, cfg);
        if (arena() == null) EmiBuildBattle.LOG.error("La dimension emi_buildbattle:arena no esta cargada: el minijuego no funcionara");
        // si el servidor se cayo con una partida en marcha, hay restos en la arena
        java.nio.file.Path dirty = s.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("emi_buildbattle").resolve("dirty");
        if (java.nio.file.Files.exists(dirty) && arena() != null) {
            EmiBuildBattle.LOG.info("Hay restos de una partida anterior: limpio la arena");
            List<Plot> all = new ArrayList<>();
            for (int i = 0; i < cfg.maxPlayers; i++) all.add(new Plot(i, cfg, new UUID(0, i), "?"));
            PlotOps.clear(arena(), all);
            PlotOps.whenIdle(() -> markDirty(false));
        }
    }

    public static void reload() {
        cfg = Cfg.load();
        Catalog.rebuild(server, cfg);
    }

    public static void shutdown() { server = null; }

    private static void markDirty(boolean d) {
        try {
            java.nio.file.Path f = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("emi_buildbattle").resolve("dirty");
            if (d) { java.nio.file.Files.createDirectories(f.getParent()); java.nio.file.Files.writeString(f, "1"); }
            else java.nio.file.Files.deleteIfExists(f);
        } catch (Exception e) { EmiBuildBattle.LOG.warn("dirty", e); }
    }

    private static List<ServerPlayerEntity> onlineParticipants() {
        List<ServerPlayerEntity> l = new ArrayList<>();
        for (UUID id : participants.keySet()) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
            if (p != null) l.add(p);
        }
        return l;
    }

    private static void later(int ticks, Runnable r) { tasks.add(new Task(server.getTicks() + ticks, r)); }

    // ---------------------------------------------------------------- cola

    public static String open() {
        if (phase != Phase.IDLE) return "Ya hay una partida en marcha (fase: " + phase.name().toLowerCase(Locale.ROOT) + "). Usa /bb parar si quieres cancelarla.";
        if (arena() == null) return "La dimensión de la arena no está cargada (falta emi_buildbattle:arena).";
        if (PlotOps.busy()) return "Todavía estoy limpiando la arena de la partida anterior; espera unos segundos.";
        queue.clear();
        phase = Phase.QUEUE;
        nextReminder = server.getTicks() + cfg.queueReminderSeconds * 20L;
        announceQueue(true);
        return null;
    }

    public static String closeQueue() {
        if (phase != Phase.QUEUE) return "No hay ninguna cola abierta.";
        Msg.broadcast(server, Msg.say("La cola se ha cerrado sin empezar."));
        queue.clear();
        phase = Phase.IDLE;
        return null;
    }

    private static void announceQueue(boolean first) {
        MutableText t = Msg.say(first ? "¡Se abre el minijuego de construcción! " : "¡Cola de construcción abierta! ")
                .append(Text.literal("(" + queue.size() + " apuntados) ").formatted(Formatting.GRAY))
                .append(Msg.button("UNIRSE", "/bb unirse", Formatting.GREEN, "Entrar a la cola del minijuego"));
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (queue.contains(p.getUuid()) || participants.containsKey(p.getUuid())) continue;
            p.sendMessage(t, false);
            if (first) {
                Msg.title(p, Text.literal("¡BuildBattle!").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD),
                        Text.literal("Haz clic en [UNIRSE] en el chat").formatted(Formatting.GRAY), 10, 70, 20);
                Msg.sound(p, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 0.8f, 1.5f);
            }
        }
    }

    public static String join(ServerPlayerEntity p) {
        if (phase != Phase.QUEUE) return phase == Phase.IDLE ? "Ahora mismo no hay ninguna cola abierta." : "La partida ya empezo; no se puede entrar.";
        if (queue.contains(p.getUuid())) return "Ya estás apuntado.";
        if (queue.size() >= cfg.maxPlayers) return "La cola está llena (" + cfg.maxPlayers + " jugadores).";
        queue.add(p.getUuid());
        names.put(p.getUuid(), p.getGameProfile().getName());
        Msg.broadcast(server, Msg.say(Text.literal(p.getGameProfile().getName()).formatted(Formatting.YELLOW)
                .append(Text.literal(" se ha unido a la cola ").formatted(Formatting.WHITE))
                .append(Text.literal("(" + queue.size() + ")").formatted(Formatting.GRAY))));
        Msg.title(p, Text.literal("¡Estas dentro!").formatted(Formatting.GREEN, Formatting.BOLD),
                Text.literal("Espera a que empiece la partida").formatted(Formatting.GRAY), 5, 50, 10);
        Msg.sound(p, SoundEvents.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f);
        return null;
    }

    public static String leave(ServerPlayerEntity p) {
        if (phase == Phase.QUEUE && queue.remove(p.getUuid())) {
            Msg.broadcast(server, Msg.say(Text.literal(p.getGameProfile().getName() + " salio de la cola (" + queue.size() + ")").formatted(Formatting.GRAY)));
            return null;
        }
        if (participants.containsKey(p.getUuid())) {
            participants.remove(p.getUuid());
            Saved.restore(server, p);
            Msg.broadcast(server, Msg.say(Text.literal(p.getGameProfile().getName() + " ha abandonado la partida").formatted(Formatting.GRAY)));
            return null;
        }
        return "No estás en ninguna partida.";
    }

    // ---------------------------------------------------------------- empezar

    public static String start(String themeText) {
        if (phase != Phase.QUEUE) return "Primero abre la cola con /bb abrir.";
        List<ServerPlayerEntity> ps = new ArrayList<>();
        for (UUID id : queue) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
            if (p != null) ps.add(p);
        }
        if (ps.size() < cfg.minPlayers) return "Hacen falta al menos " + cfg.minPlayers + " jugadores (ahora " + ps.size() + ").";
        if (arena() == null) return "La dimensión de la arena no está cargada.";
        phase = Phase.STARTING;
        theme = themeText == null ? "" : themeText.trim();
        plots.clear(); participants.clear(); votes.clear(); ranking.clear(); order.clear();
        int i = 0;
        for (ServerPlayerEntity p : ps) {
            Plot pl = new Plot(i++, cfg, p.getUuid(), p.getGameProfile().getName());
            plots.add(pl);
        }
        queue.clear();
        markDirty(true);
        PlotOps.build(arena(), cfg, plots);
        List<ServerPlayerEntity> finalPs = ps;
        for (int c = 5; c >= 1; c--) {
            final int n = c;
            later((5 - c) * 20, () -> {
                if (phase != Phase.STARTING) return;
                for (ServerPlayerEntity p : finalPs) {
                    Msg.title(p, Text.literal(String.valueOf(n)).formatted(Formatting.GOLD, Formatting.BOLD),
                            Text.literal("Preparando tu parcela...").formatted(Formatting.GRAY), 0, 25, 5);
                    Msg.sound(p, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 1f, 0.8f + 0.1f * (5 - n));
                }
            });
        }
        later(100, () -> PlotOps.whenIdle(() -> enterAll(finalPs)));
        return null;
    }

    private static void enterAll(List<ServerPlayerEntity> ps) {
        if (phase != Phase.STARTING) return;
        int ok = 0;
        for (int i = 0; i < ps.size(); i++) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(ps.get(i).getUuid());
            Plot pl = plots.get(i);
            if (p == null) continue;              // se desconecto mientras se preparaba
            try {
                Saved.save(server, p);
            } catch (RuntimeException e) {
                p.sendMessage(Msg.err("No pude guardar tu inventario, asi que no te meto en la partida (no pierdes nada)."), false);
                continue;
            }
            participants.put(p.getUuid(), pl);
            names.put(p.getUuid(), pl.ownerName);
            enterPlot(p, pl);
            ok++;
        }
        spawnNpcs();
        phase = Phase.BUILDING;
        nextActionBar = 0;
        for (ServerPlayerEntity p : onlineParticipants()) announceTheme(p);
        Msg.broadcast(server, Msg.say(Text.literal(ok + " jugadores han entrado a construir").formatted(Formatting.GRAY)));
    }

    /** Teletransporta a la parcela, vacia el inventario y lo prepara para construir. No guarda nada. */
    private static void enterPlot(ServerPlayerEntity p, Plot pl) {
        p.stopRiding();
        p.closeHandledScreen();
        p.getInventory().clear();
        p.clearStatusEffects();
        p.setHealth(p.getMaxHealth());
        p.getHungerManager().setFoodLevel(20);
        Vec3d s = pl.spawn();
        p.teleport(arena(), s.x, s.y, s.z, 0f, 0f);
        setupBuilder(p);
    }

    /** Supervivencia (los bloques salen del Constructor, no del inventario creativo) pero volando, sin dano y con rotura casi instantanea. */
    private static void setupBuilder(ServerPlayerEntity p) {
        p.changeGameMode(GameMode.SURVIVAL);
        p.getAbilities().allowFlying = true;
        p.getAbilities().flying = true;
        p.sendAbilitiesUpdate();
        p.setInvulnerable(true);
        giveHaste(p);
        giveFastBreak(p);
    }

    private static void giveHaste(ServerPlayerEntity p) {
        StatusEffectInstance cur = p.getStatusEffect(StatusEffects.HASTE);
        if (cur != null && cur.getAmplifier() >= 120 && cur.getDuration() > 20000) return;
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 1_000_000, 127, false, false, false));
    }

    public static final Identifier FAST_BREAK = Identifier.of("emi_buildbattle", "fast_break");

    /** Volando se rompe 5 veces mas despacio; este modificador (temporal, no se guarda) lo compensa de sobra: casi todo sale al instante. */
    public static void giveFastBreak(ServerPlayerEntity p) {
        var inst = p.getAttributeInstance(EntityAttributes.PLAYER_BLOCK_BREAK_SPEED);
        if (inst != null && !inst.hasModifier(FAST_BREAK))
            inst.addTemporaryModifier(new EntityAttributeModifier(FAST_BREAK, 49.0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    public static void removeFastBreak(ServerPlayerEntity p) {
        var inst = p.getAttributeInstance(EntityAttributes.PLAYER_BLOCK_BREAK_SPEED);
        if (inst != null) inst.removeModifier(FAST_BREAK);
    }

    private static void announceTheme(ServerPlayerEntity p) {
        Text t = theme.isEmpty() ? Text.literal("¡A construir!").formatted(Formatting.GOLD, Formatting.BOLD)
                : Text.literal(theme).formatted(Formatting.GOLD, Formatting.BOLD);
        Text sub = theme.isEmpty() ? Text.literal("Habla con el constructor para pedir bloques").formatted(Formatting.GRAY)
                : Text.literal("Tema de la partida").formatted(Formatting.GRAY);
        Msg.title(p, t, sub, 10, 80, 20);
        Msg.sound(p, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 0.4f, 1.6f);
        p.sendMessage(Msg.say(Text.literal("Construye dentro de tu parcela (64x64). Habla con el ")
                .append(Text.literal("Constructor").formatted(Formatting.GOLD))
                .append(Text.literal(" o usa /bb bloques para pedir bloques gratis.")).formatted(Formatting.WHITE)), false);
        if (!theme.isEmpty()) p.sendMessage(Msg.say(Text.literal("Tema: ").formatted(Formatting.WHITE)
                .append(Text.literal(theme).formatted(Formatting.GOLD, Formatting.BOLD))), false);
    }

    public static String setTheme(String t) {
        if (phase != Phase.BUILDING && phase != Phase.QUEUE) return "El tema solo se puede cambiar antes de la votación.";
        theme = t.trim();
        if (phase == Phase.BUILDING) for (ServerPlayerEntity p : onlineParticipants()) announceTheme(p);
        else Msg.broadcast(server, Msg.say("El tema sera: " + theme));
        return null;
    }

    // ---------------------------------------------------------------- NPC y carteles

    private static void spawnNpcs() {
        ServerWorld w = arena();
        for (Plot pl : plots) {
            Vec3d n = pl.npcPos();
            boolean ok = false;
            VillagerEntity v = server.shouldSpawnNpcs() ? EntityType.VILLAGER.create(w) : null;
            if (v != null) {
                v.refreshPositionAndAngles(n.x, n.y, n.z, 90f, 0f);
                v.setHeadYaw(90f); v.setBodyYaw(90f);
                v.setAiDisabled(true);
                v.setInvulnerable(true);
                v.setSilent(true);
                v.setPersistent();
                v.setVillagerData(v.getVillagerData().withProfession(VillagerProfession.MASON));
                v.setCustomName(Text.literal("Constructor").formatted(Formatting.GOLD, Formatting.BOLD));
                v.setCustomNameVisible(true);
                v.addCommandTag(TAG_NPC);
                ok = w.spawnEntity(v);
            }
            if (!ok) {
                // si el servidor tiene spawn-npcs=false los aldeanos no pueden existir: un soporte de armadura hace de constructor
                EmiBuildBattle.LOG.warn("No se pudo crear el aldeano constructor (spawn-npcs=false?); uso un soporte de armadura");
                ArmorStandEntity a = EntityType.ARMOR_STAND.create(w);
                if (a != null) {
                    a.refreshPositionAndAngles(n.x, n.y, n.z, 90f, 0f);
                    a.setShowArms(true);
                    a.setInvulnerable(true);
                    a.setSilent(true);
                    a.equipStack(net.minecraft.entity.EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                    a.equipStack(net.minecraft.entity.EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
                    a.equipStack(net.minecraft.entity.EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
                    a.equipStack(net.minecraft.entity.EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
                    a.equipStack(net.minecraft.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
                    a.setCustomName(Text.literal("Constructor").formatted(Formatting.GOLD, Formatting.BOLD));
                    a.setCustomNameVisible(true);
                    a.addCommandTag(TAG_NPC);
                    w.spawnEntity(a);
                }
            }
            runCmd("summon armor_stand " + n.x + " " + (n.y + 2.15) + " " + n.z
                    + " {Tags:[\"" + TAG_LABEL + "\"],Invisible:1b,Marker:1b,NoGravity:1b,Invulnerable:1b,CustomNameVisible:1b,"
                    + "CustomName:'{\"text\":\"Parcela de " + pl.ownerName.replaceAll("[^A-Za-z0-9_ ]", "")
                    + "\",\"color\":\"yellow\",\"bold\":true}'}");
        }
    }

    private static void runCmd(String cmd) {
        ServerCommandSource src = server.getCommandSource().withWorld(arena()).withSilent();
        server.getCommandManager().executeWithPrefix(src, cmd);
    }

    private static void removeLabels() {
        ServerWorld w = arena();
        if (w == null) return;
        for (Entity e : new ArrayList<>(collect(w))) if (e.getCommandTags().contains(TAG_LABEL) || e.getCommandTags().contains(TAG_NPC)) e.discard();
    }

    private static List<Entity> collect(ServerWorld w) {
        List<Entity> l = new ArrayList<>();
        for (Entity e : w.iterateEntities()) l.add(e);
        return l;
    }

    public static boolean isNpc(Entity e) { return e.getCommandTags().contains(TAG_NPC); }

    // ---------------------------------------------------------------- temporizador (solo avisa)

    public static String timer(int minutes) {
        if (phase != Phase.BUILDING) return "El temporizador solo se usa mientras se construye.";
        stopTimer();
        timerStart = server.getTicks();
        timerEnd = timerStart + minutes * 1200L;
        lastWarn = Integer.MAX_VALUE;
        bar = new ServerBossBar(Text.literal("Tiempo restante").formatted(Formatting.GOLD), BossBar.Color.YELLOW, BossBar.Style.PROGRESS);
        for (ServerPlayerEntity p : onlineParticipants()) bar.addPlayer(p);
        Msg.title(onlineParticipants(), Text.literal(minutes + " minutos").formatted(Formatting.YELLOW, Formatting.BOLD),
                Text.literal("¡Empieza el tiempo!").formatted(Formatting.GRAY), 5, 40, 10);
        return null;
    }

    public static void stopTimer() {
        if (bar != null) { bar.clearPlayers(); bar = null; }
        timerEnd = -1;
    }

    public static String timerStatus() {
        if (timerEnd < 0) return null;
        long left = Math.max(0, timerEnd - server.getTicks()) / 20;
        return String.format("%d:%02d", left / 60, left % 60);
    }

    private static void tickTimer() {
        if (timerEnd < 0 || bar == null) return;
        long now = server.getTicks();
        long total = timerEnd - timerStart;
        long left = Math.max(0, timerEnd - now);
        bar.setPercent(total <= 0 ? 0f : (float) left / total);
        long secs = left / 20;
        bar.setName(Text.literal("Tiempo restante: " + String.format("%d:%02d", secs / 60, secs % 60)).formatted(Formatting.GOLD));
        int[] marks = {300, 120, 60, 30, 10, 5, 4, 3, 2, 1};
        for (int m : marks) {
            if (secs <= m && lastWarn > m) {
                lastWarn = m;
                List<ServerPlayerEntity> ps = onlineParticipants();
                if (m >= 30) {
                    Msg.broadcastTo(ps, Msg.say(Text.literal("Quedan " + (m >= 60 ? (m / 60) + " min" : m + " s")).formatted(Formatting.YELLOW)));
                    Msg.sound(ps, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 0.8f, 1.2f);
                } else {
                    for (ServerPlayerEntity p : ps) Msg.actionBar(p, Text.literal(String.valueOf(m)).formatted(Formatting.RED, Formatting.BOLD));
                    Msg.sound(ps, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 1f, 1.5f);
                }
                break;
            }
        }
        if (left <= 0) {
            List<ServerPlayerEntity> ps = onlineParticipants();
            Msg.title(ps, Text.literal("¡Se acabo el tiempo!").formatted(Formatting.RED, Formatting.BOLD),
                    Text.literal("Termina lo que estes haciendo").formatted(Formatting.GRAY), 5, 60, 20);
            Msg.sound(ps, SoundEvents.ENTITY_WITHER_SPAWN, 0.35f, 1.4f);
            Msg.broadcastTo(ps, Msg.say("¡Se acabo el tiempo! Espera a que el organizador abra la votación."));
            stopTimer();
        }
    }

    // ---------------------------------------------------------------- votacion

    public static String beginVote() {
        if (phase != Phase.BUILDING) return "Solo se puede abrir la votación mientras se construye.";
        if (plots.isEmpty()) return "No hay construcciones.";
        stopTimer();
        phase = Phase.VOTING;
        removeLabels();
        order = new ArrayList<>(plots);
        Collections.shuffle(order, RNG);
        voteIdx = 0;
        votes.clear();
        for (ServerPlayerEntity p : onlineParticipants()) {
            p.closeHandledScreen();
            setupVoter(p);
        }
        Msg.broadcastTo(onlineParticipants(), Msg.say("¡Votación! Haz clic derecho con la lana del 1 al 9 (9 = la mejor). Puedes cambiar tu voto hasta que pasemos a la siguiente."));
        showPlot();
        return null;
    }

    private static void setupVoter(ServerPlayerEntity p) {
        p.getInventory().clear();
        p.changeGameMode(GameMode.ADVENTURE);
        p.getAbilities().allowFlying = true;
        p.getAbilities().flying = true;
        p.getAbilities().invulnerable = true;
        p.sendAbilitiesUpdate();
        p.setInvulnerable(true);
        p.clearStatusEffects();
        removeFastBreak(p);
        giveVoteItems(p);
    }

    private static final Item[] WOOL = {Items.RED_WOOL, Items.ORANGE_WOOL, Items.YELLOW_WOOL, Items.LIME_WOOL, Items.GREEN_WOOL,
            Items.CYAN_WOOL, Items.LIGHT_BLUE_WOOL, Items.BLUE_WOOL, Items.PURPLE_WOOL};
    private static final Formatting[] WOOL_COLOR = {Formatting.RED, Formatting.GOLD, Formatting.YELLOW, Formatting.GREEN, Formatting.DARK_GREEN,
            Formatting.DARK_AQUA, Formatting.AQUA, Formatting.BLUE, Formatting.LIGHT_PURPLE};

    private static ItemStack voteItem(int score) {
        ItemStack s = new ItemStack(WOOL[score - 1], score);
        s.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Puntuar: " + score).styled(st -> st.withColor(WOOL_COLOR[score - 1]).withBold(true).withItalic(false)));
        s.set(DataComponentTypes.LORE, new LoreComponent(List.of(Text.literal("Clic derecho para votar " + score).styled(st -> st.withColor(Formatting.GRAY).withItalic(false)))));
        NbtCompound n = new NbtCompound();
        n.putInt(VOTE_KEY, score);
        s.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(n));
        return s;
    }

    private static void giveVoteItems(ServerPlayerEntity p) {
        for (int i = 0; i < 9; i++) p.getInventory().setStack(i, voteItem(i + 1));
        for (int i = 9; i < p.getInventory().size(); i++) p.getInventory().setStack(i, ItemStack.EMPTY);
        p.getInventory().selectedSlot = 4;
        p.currentScreenHandler.sendContentUpdates();
    }

    public static int voteScore(ItemStack s) {
        if (s.isEmpty()) return 0;
        NbtComponent c = s.get(DataComponentTypes.CUSTOM_DATA);
        if (c == null || !c.contains(VOTE_KEY)) return 0;
        return c.copyNbt().getInt(VOTE_KEY);
    }

    public static Plot currentPlot() { return phase == Phase.VOTING && voteIdx < order.size() ? order.get(voteIdx) : null; }

    private static void showPlot() {
        Plot pl = order.get(voteIdx);
        Vec3d s = pl.spawn();
        ServerWorld w = arena();
        for (ServerPlayerEntity p : onlineParticipants()) {
            p.teleport(w, s.x, s.y + 6, s.z - 1, 0f, 30f);
            p.getAbilities().flying = true;
            p.sendAbilitiesUpdate();
            Msg.title(p, Text.literal("Construcción " + (voteIdx + 1) + " / " + order.size()).formatted(Formatting.GOLD, Formatting.BOLD),
                    pl.owner.equals(p.getUuid()) ? Text.literal("Esta es la tuya").formatted(Formatting.GRAY) : Text.literal("Vota del 1 al 9").formatted(Formatting.GRAY), 5, 40, 10);
            Msg.sound(p, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.0f);
            if (pl.owner.equals(p.getUuid())) Msg.actionBar(p, Text.literal("Esta es tu construcción: no puedes votarla").formatted(Formatting.GRAY));
            else {
                Integer prev = votes.getOrDefault(p.getUuid(), Map.of()).get(pl.index);
                Msg.actionBar(p, Text.literal(prev == null ? "Sin voto todavía" : "Tu voto: " + prev).formatted(Formatting.GRAY));
            }
        }
    }

    public static void onVote(ServerPlayerEntity p, int score) {
        if (phase != Phase.VOTING || !participants.containsKey(p.getUuid())) return;
        Plot pl = order.get(voteIdx);
        if (pl.owner.equals(p.getUuid())) {
            Msg.actionBar(p, Text.literal("No puedes votar tu propia construcción").formatted(Formatting.RED));
            Msg.sound(p, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.8f, 0.6f);
            return;
        }
        votes.computeIfAbsent(p.getUuid(), k -> new HashMap<>()).put(pl.index, score);
        Msg.actionBar(p, Text.literal("Votaste ").formatted(Formatting.WHITE).append(Text.literal(score + " / 9").formatted(WOOL_COLOR[score - 1], Formatting.BOLD)));
        Msg.sound(p, SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 0.8f, 0.5f + score * 0.1f);
        if (allVoted()) {
            for (ServerPlayerEntity op : server.getPlayerManager().getPlayerList()) {
                if (Perm.admin(op.getCommandSource()) && !participants.containsKey(op.getUuid())) {
                    op.sendMessage(Msg.say(Text.literal("Todos han votado esta construcción (" + voteProgress() + "). ").formatted(Formatting.GREEN)
                            .append(Msg.button("SIGUIENTE", "/bb siguiente", Formatting.GOLD, "Pasar a la siguiente construcción"))), false);
                }
            }
        }
    }

    private static boolean allVoted() {
        String pr = voteProgress();
        int i = pr.indexOf('/');
        return i > 0 && pr.substring(0, i).equals(pr.substring(i + 1)) && !pr.startsWith("0/");
    }

    /** Votos recibidos de la construccion actual / los que pueden votar. */
    public static String voteProgress() {
        if (phase != Phase.VOTING) return "";
        Plot pl = order.get(voteIdx);
        int voted = 0, eligible = 0;
        for (UUID id : participants.keySet()) {
            if (id.equals(pl.owner)) continue;
            eligible++;
            if (votes.getOrDefault(id, Map.of()).containsKey(pl.index)) voted++;
        }
        return voted + "/" + eligible;
    }

    public static String next() {
        if (phase != Phase.VOTING) return "No estamos en la votación.";
        if (voteIdx < order.size() - 1) { voteIdx++; showPlot(); return null; }
        reveal();
        return null;
    }

    public static String prev() {
        if (phase != Phase.VOTING) return "No estamos en la votación.";
        if (voteIdx == 0) return "Ya estas en la primera construcción.";
        voteIdx--; showPlot();
        return null;
    }

    // ---------------------------------------------------------------- resultados

    public static List<Result> computeRanking() {
        List<Result> tmp = new ArrayList<>();
        for (Plot pl : plots) {
            int sum = 0, n = 0;
            for (Map.Entry<UUID, Map<Integer, Integer>> e : votes.entrySet()) {
                if (e.getKey().equals(pl.owner)) continue;
                Integer sc = e.getValue().get(pl.index);
                if (sc != null) { sum += sc; n++; }
            }
            tmp.add(new Result(pl, n == 0 ? 0 : (double) sum / n, sum, n, 0));
        }
        tmp.sort(Comparator.<Result>comparingDouble(r -> -r.avg()).thenComparingInt(r -> -r.sum()));
        List<Result> out = new ArrayList<>();
        int rank = 0;
        for (int i = 0; i < tmp.size(); i++) {
            Result r = tmp.get(i);
            if (i == 0 || Math.abs(r.avg() - tmp.get(i - 1).avg()) > 1e-9 || r.sum() != tmp.get(i - 1).sum()) rank = i + 1;
            out.add(new Result(r.plot(), r.avg(), r.sum(), r.count(), rank));
        }
        return out;
    }

    private static String fmt(Result r) {
        return String.format(Locale.ROOT, "%.2f", r.avg()) + " pts";
    }

    private static void reveal() {
        phase = Phase.RESULTS;
        ranking = computeRanking();
        List<ServerPlayerEntity> ps = onlineParticipants();
        for (ServerPlayerEntity p : ps) { p.getInventory().clear(); p.currentScreenHandler.sendContentUpdates(); }
        Msg.title(ps, Text.literal("¡Resultados!").formatted(Formatting.GOLD, Formatting.BOLD),
                Text.literal("Atención...").formatted(Formatting.GRAY), 10, 60, 10);
        Msg.sound(ps, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1f, 0.8f);
        int top = Math.min(cfg.topSize, ranking.size());
        int t = 80;
        for (int i = top - 1; i >= 0; i--) {
            final Result r = ranking.get(i);
            final boolean winner = i == 0;
            later(t, () -> revealOne(r, winner));
            t += cfg.revealSeconds * 20;
        }
        later(t, Game::printFullRanking);
    }

    private static void revealOne(Result r, boolean winner) {
        if (phase != Phase.RESULTS) return;
        List<ServerPlayerEntity> ps = onlineParticipants();
        Formatting col = r.rank() == 1 ? Formatting.GOLD : r.rank() == 2 ? Formatting.GRAY : Formatting.RED;
        String place = r.rank() + ".º lugar";
        Msg.title(ps, Text.literal(r.plot().ownerName).formatted(col, Formatting.BOLD),
                Text.literal(place + "  -  " + fmt(r)).formatted(Formatting.WHITE), 5, 90, 20);
        Msg.broadcastTo(ps, Msg.say(Text.literal(place + ": ").formatted(col, Formatting.BOLD)
                .append(Text.literal(r.plot().ownerName).formatted(Formatting.WHITE, Formatting.BOLD))
                .append(Text.literal("  " + fmt(r) + " (" + r.count() + " votos)").formatted(Formatting.GRAY))));
        // llevar a todos a esa construccion
        Vec3d s = r.plot().spawn();
        for (ServerPlayerEntity p : ps) {
            p.teleport(arena(), s.x, s.y + 6, s.z - 1, 0f, 30f);
            p.getAbilities().flying = true; p.sendAbilitiesUpdate();
        }
        Msg.sound(ps, winner ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.ENTITY_PLAYER_LEVELUP, 1f, winner ? 1f : 0.8f);
        if (winner) {
            for (int i = 0; i < 6; i++) later(i * 12, () -> fireworks(r.plot()));
        }
    }

    private static void fireworks(Plot pl) {
        ServerWorld w = arena();
        if (w == null || phase != Phase.RESULTS) return;
        Vec3d c = new Vec3d(pl.ox + pl.size / 2.0, pl.floorY + 10, pl.oz + pl.size / 2.0);
        for (int i = 0; i < 4; i++) {
            ItemStack fw = new ItemStack(Items.FIREWORK_ROCKET);
            int[][] cols = {{0xFF5555, 0xFFFF55}, {0x55FFFF, 0xFFFFFF}, {0xFF55FF, 0xFFAA00}, {0x55FF55, 0xFFFF55}};
            fw.set(DataComponentTypes.FIREWORKS, new FireworksComponent(1, List.of(new FireworkExplosionComponent(
                    FireworkExplosionComponent.Type.LARGE_BALL, IntList.of(cols[RNG.nextInt(cols.length)]), IntList.of(0xFFFFFF), true, true))));
            double ox = (RNG.nextDouble() - 0.5) * pl.size * 0.8, oz = (RNG.nextDouble() - 0.5) * pl.size * 0.8;
            w.spawnEntity(new FireworkRocketEntity(w, c.x + ox, c.y, c.z + oz, fw));
        }
    }

    public static void printFullRanking() {
        if (ranking.isEmpty()) ranking = computeRanking();
        List<ServerPlayerEntity> ps = onlineParticipants();
        Msg.broadcastTo(ps, Msg.say(Text.literal("Clasificación final").formatted(Formatting.GOLD, Formatting.BOLD)));
        for (Result r : ranking) {
            Msg.broadcastTo(ps, Text.literal(" " + r.rank() + ". ").formatted(Formatting.YELLOW)
                    .append(Text.literal(r.plot().ownerName).formatted(Formatting.WHITE))
                    .append(Text.literal("  " + fmt(r) + "  (total " + r.sum() + ", " + r.count() + " votos)").formatted(Formatting.GRAY)));
        }
    }

    public static List<Result> ranking() { return ranking; }

    // ---------------------------------------------------------------- terminar

    public static String end() {
        if (phase == Phase.IDLE) return "No hay ninguna partida.";
        if (phase == Phase.QUEUE) return closeQueue();
        if (phase == Phase.ENDING) return "Ya se esta cerrando la partida.";
        phase = Phase.ENDING;
        stopTimer();
        tasks.clear();
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (participants.containsKey(p.getUuid()) || Saved.exists(server, p.getUuid())) {
                p.closeHandledScreen();
                Saved.restore(server, p);
                p.sendMessage(Msg.say("¡Gracias por jugar! Tienes de vuelta todas tus cosas."), false);
            }
        }
        participants.clear();
        ServerWorld w = arena();
        if (w != null) {
            for (Entity e : collect(w)) if (!(e instanceof PlayerEntity)) e.discard();
            Schem.exportAll(w, cfg, new ArrayList<>(plots));   // antes de limpiar: la cola de trabajos es FIFO
            PlotOps.clear(w, new ArrayList<>(plots));
        }
        PlotOps.whenIdle(() -> {
            markDirty(false);
            plots.clear(); order.clear(); votes.clear(); ranking.clear(); theme = "";
            phase = Phase.IDLE;
            Msg.broadcast(server, Msg.say("La arena está limpia. ¡Hasta la próxima!"));
        });
        return null;
    }

    // ---------------------------------------------------------------- jugadores que entran/salen

    public static void onJoin(ServerPlayerEntity p) {
        if (participants.containsKey(p.getUuid()) && phase != Phase.IDLE) {
            Plot pl = participants.get(p.getUuid());
            switch (phase) {
                case BUILDING -> { setupBuilder(p); announceTheme(p); }
                case VOTING -> { setupVoter(p); showPlot(); }
                case RESULTS -> { setupVoter(p); p.getInventory().clear(); }
                default -> { }
            }
            return;
        }
        if (Saved.exists(server, p.getUuid())) {
            // se quedo guardado (se cayo el servidor o salio durante la partida): devolverle sus cosas
            server.execute(() -> {
                if (Saved.restore(server, p)) p.sendMessage(Msg.say("Te devuelvo lo que tenias antes del minijuego."), false);
            });
        }
    }

    public static void onDisconnect(ServerPlayerEntity p) {
        if (phase == Phase.QUEUE && queue.remove(p.getUuid())) {
            Msg.broadcast(server, Msg.say(Text.literal(p.getGameProfile().getName() + " salio de la cola (" + queue.size() + ")").formatted(Formatting.GRAY)));
        }
        if (bar != null) bar.removePlayer(p);
    }

    // ---------------------------------------------------------------- vigilancia (cada tick)

    private static long nextActionBar = 0;

    private static long lastErr = 0;

    public static void tick(MinecraftServer s) {
        try {
            tick0(s);
        } catch (Throwable t) {
            // un fallo del minijuego nunca debe tirar el servidor
            long now = System.currentTimeMillis();
            if (now - lastErr > 5000) { lastErr = now; EmiBuildBattle.LOG.error("Error en el tick del minijuego", t); }
        }
    }

    private static void tick0(MinecraftServer s) {
        PlotOps.tick();
        long now = s.getTicks();
        if (!tasks.isEmpty()) {
            List<Task> due = new ArrayList<>();
            for (Iterator<Task> it = tasks.iterator(); it.hasNext(); ) {
                Task t = it.next();
                if (t.due <= now) { it.remove(); due.add(t); }
            }
            for (Task t : due) { try { t.r.run(); } catch (Exception e) { EmiBuildBattle.LOG.error("tarea", e); } }
        }
        if (phase == Phase.QUEUE && now >= nextReminder) {
            nextReminder = now + cfg.queueReminderSeconds * 20L;
            announceQueue(false);
        }
        if (phase != Phase.BUILDING && phase != Phase.VOTING && phase != Phase.RESULTS) return;
        if (now % 2 == 0) clamp();
        if (now % 10 == 0) sanitize();
        if (now % 20 == 0) clean();
        if (phase == Phase.VOTING && now % 20 == 0) {
            for (ServerPlayerEntity op : server.getPlayerManager().getPlayerList()) {
                if (participants.containsKey(op.getUuid()) || !Perm.admin(op.getCommandSource())) continue;
                Msg.actionBar(op, Text.literal("Construcción " + (voteIdx + 1) + "/" + order.size() + "  -  votos " + voteProgress()
                        + "  -  /bb siguiente").formatted(Formatting.AQUA));
            }
        }
        if (phase == Phase.BUILDING) {
            tickTimer();
            if (now % 20 == 0 && !theme.isEmpty()) {
                MutableText bar = Text.literal("Tema: ").formatted(Formatting.WHITE).append(Text.literal(theme).formatted(Formatting.GOLD, Formatting.BOLD));
                String left = timerStatus();
                if (left != null) bar.append(Text.literal("   " + left).formatted(Formatting.YELLOW));
                for (ServerPlayerEntity p : onlineParticipants()) Msg.actionBar(p, bar);
            }
        }
    }

    private static void clamp() {
        for (ServerPlayerEntity p : onlineParticipants()) {
            if (p.getServerWorld().getRegistryKey() != ARENA) {
                // se salio de la dimension (portal, comando...): devolverlo
                Plot pl = participants.get(p.getUuid());
                Vec3d s = pl.spawn();
                p.teleport(arena(), s.x, s.y, s.z, 0f, 0f);
                continue;
            }
            Plot pl;
            int margin;
            if (phase == Phase.BUILDING) { pl = participants.get(p.getUuid()); margin = 0; }
            else { pl = phase == Phase.VOTING ? order.get(voteIdx) : nearestPlot(p); margin = cfg.viewMargin; }
            if (pl == null) continue;
            Vec3d pos = p.getPos();
            if (pl.outside(pos, margin)) {
                Vec3d c = pl.clamp(pos, margin);
                p.teleport(arena(), c.x, c.y, c.z, p.getYaw(), p.getPitch());
                p.setVelocity(Vec3d.ZERO);
                p.velocityModified = true;
            }
        }
    }

    private static Plot nearestPlot(ServerPlayerEntity p) {
        Plot best = null; double bd = Double.MAX_VALUE;
        for (Plot pl : plots) {
            double d = Math.pow(pl.ox + pl.size / 2.0 - p.getX(), 2) + Math.pow(pl.oz + pl.size / 2.0 - p.getZ(), 2);
            if (d < bd) { bd = d; best = pl; }
        }
        return best;
    }

    private static void sanitize() {
        for (ServerPlayerEntity p : onlineParticipants()) {
            Inventory inv = p.getInventory();
            boolean changed = false;
            if (phase == Phase.BUILDING) {
                for (int i = 0; i < inv.size(); i++) {
                    ItemStack st = inv.getStack(i);
                    if (st.isEmpty()) continue;
                    if (!Catalog.isAllowed(st.getItem()) || hasHiddenContents(st)) { inv.setStack(i, ItemStack.EMPTY); changed = true; }
                }
                p.getHungerManager().setFoodLevel(20);
                p.getHungerManager().setSaturationLevel(20f);
                giveHaste(p);
                giveFastBreak(p);
                if (!p.getAbilities().allowFlying) { p.getAbilities().allowFlying = true; p.sendAbilitiesUpdate(); }
            } else {
                // votacion / resultados: solo las 9 lanas en la barra (o nada)
                for (int i = 0; i < inv.size(); i++) {
                    ItemStack want = phase == Phase.VOTING && i < 9 ? voteItem(i + 1) : ItemStack.EMPTY;
                    ItemStack have = inv.getStack(i);
                    boolean same = want.isEmpty() ? have.isEmpty() : (voteScore(have) == i + 1 && have.getCount() == i + 1);
                    if (!same) { inv.setStack(i, want); changed = true; }
                }
                p.getHungerManager().setFoodLevel(20);
                p.setHealth(p.getMaxHealth());
            }
            if (changed) { p.currentScreenHandler.sendContentUpdates(); p.playerScreenHandler.sendContentUpdates(); }
        }
    }

    private static boolean hasHiddenContents(ItemStack st) {
        var c = st.get(DataComponentTypes.CONTAINER);
        if (c != null && c.iterateNonEmpty().iterator().hasNext()) return true;
        if (st.contains(DataComponentTypes.BUNDLE_CONTENTS)) return true;
        NbtComponent be = st.get(DataComponentTypes.BLOCK_ENTITY_DATA);
        return be != null && be.contains("Items");
    }

    /** En la arena no puede haber nada vivo salvo jugadores, NPC y decoracion colocada por los jugadores. */
    private static void clean() {
        ServerWorld w = arena();
        if (w == null) return;
        for (Entity e : collect(w)) {
            if (e instanceof PlayerEntity) continue;
            if (e.getCommandTags().contains(TAG_NPC) || e.getCommandTags().contains(TAG_LABEL)) continue;
            if (e instanceof AbstractDecorationEntity || e instanceof ArmorStandEntity || e instanceof DisplayEntity || e instanceof FireworkRocketEntity
                    || e instanceof net.minecraft.entity.FallingBlockEntity) continue;
            e.discard();
        }
    }

    // ---------------------------------------------------------------- permisos de interaccion

    /** Puede el jugador colocar/romper/usar en esta posicion? */
    public static boolean allowBuildAt(PlayerEntity p, BlockPos pos) {
        if (!participants.containsKey(p.getUuid())) return true;
        if (phase != Phase.BUILDING) return false;
        Plot pl = participants.get(p.getUuid());
        return pl.canBuildAt(pos);
    }

    public static List<Plot> plots() { return plots; }
    public static Map<UUID, Plot> participants() { return participants; }
    public static int queueSize() { return queue.size(); }
    public static List<String> queueNames() {
        List<String> l = new ArrayList<>();
        for (UUID id : queue) l.add(names.getOrDefault(id, id.toString()));
        return l;
    }
    public static int voteIndex() { return voteIdx; }
    public static int voteTotal() { return order.size(); }

    /** Teletransporta a un espectador (admin) a la parcela de un participante. */
    public static String spectate(ServerPlayerEntity admin, String who) {
        if (phase != Phase.BUILDING && phase != Phase.VOTING && phase != Phase.RESULTS) return "No hay partida en marcha.";
        if (participants.containsKey(admin.getUuid())) return "Estás jugando; no puedes espectar.";
        Plot target = null;
        for (Plot pl : plots) if (pl.ownerName.equalsIgnoreCase(who)) target = pl;
        if (target == null) return "No hay ninguna parcela de " + who + ".";
        if (!Saved.exists(server, admin.getUuid())) {
            try { Saved.save(server, admin); } catch (RuntimeException e) { return "No pude guardar tu posicion."; }
        }
        Vec3d s = target.spawn();
        admin.changeGameMode(GameMode.SPECTATOR);
        admin.teleport(arena(), s.x, s.y + 6, s.z - 1, 0f, 30f);
        return null;
    }

    public static String back(ServerPlayerEntity admin) {
        if (participants.containsKey(admin.getUuid())) return "Estás jugando; usa /bb salir.";
        if (!Saved.restore(server, admin)) return "No tienes nada guardado a lo que volver.";
        return null;
    }
}
