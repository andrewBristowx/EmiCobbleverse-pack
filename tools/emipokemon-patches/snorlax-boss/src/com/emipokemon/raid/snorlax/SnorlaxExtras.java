package com.emipokemon.raid.snorlax;

import com.emipokemon.config.EmipokemonConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.server.MinecraftServer;

/**
 * Extras del jefe Snorlax Emi (parche sobre SnorlaxRaidService):
 *  - resolve(...) sustituye a resolveCombat(...) en cada tick de combate: primero deja actuar a los ataques nuevos y, si no hay ninguno
 *    en marcha, llama al combate original (cuerpo a cuerpo, golpe de area, onda expansiva, aplastamiento).
 *  - Ataques nuevos para quien lo ataca de lejos (el jefe es grande y lento): Salto Aplastante, Lluvia de Rocas y Bostezo.
 *  - noTeleport / skipCutscene: ya no teletransporta a los participantes ni les pone la camara al aparecer y morir.
 * Todo va en try/catch: si algo falla, se vuelve al combate original.
 */
public final class SnorlaxExtras {
    private static final int IDLE = 0, LEAP = 1, ROCKS = 2, YAWN = 3;
    private static final Random RNG = new Random();
    private static final boolean DEBUG = Boolean.getBoolean("emi.snorlax.debug");
    private static int debugTicks;
    private static int debugCalls;

    private static UUID bossId;
    private static int mode = IDLE;
    private static int ticks;
    private static int gap;
    private static final Map<UUID, Integer> kite = new HashMap<>();
    private static String leapName;
    private static final List<double[]> spots = new ArrayList<>();

    private SnorlaxExtras() {
    }

    /** Sustituye a SnorlaxRaidService.resolveCombat (misma firma). */
    public static void resolve(MinecraftServer server, class_3218 world, class_1297 entity, class_1309 living, SnorlaxRaidService.ActiveRaid raid, EmipokemonConfig.SnorlaxBossSettings config) {
        boolean handled = false;
        try {
            handled = tick(server, world, entity, raid, config);
        } catch (Throwable error) {
            System.err.println("[snorlax_emi] error en los ataques extra: " + error);
            reset();
            gap = 200;
        }
        if (!handled) {
            SnorlaxRaidService.resolveCombat(server, world, entity, living, raid, config);
        }
    }

    /** Sustituye a ServerPlayerEntity.teleport(world, x, y, z, yaw, pitch): ya no se reune a los jugadores en la arena. */
    public static void noTeleport(class_3222 player, class_3218 world, double x, double y, double z, float yaw, float pitch) {
    }

    /** Sustituye a SnorlaxRaidService.startCutscene (misma firma): sin camara; se ejecuta directamente lo que iba al terminar. */
    public static void skipCutscene(MinecraftServer server, SnorlaxRaidService.ActiveRaid raid, class_3218 world, net.minecraft.class_243 startPos, net.minecraft.class_243 endPos, float startYaw, float endYaw, float startPitch, float endPitch, int totalTicks, Runnable onComplete) {
        if (onComplete != null) {
            onComplete.run();
        }
    }

    private static void reset() {
        mode = IDLE;
        ticks = 0;
        kite.clear();
        spots.clear();
        leapName = null;
    }

    private static void run(MinecraftServer server, String command) {
        server.method_3734().method_44252(server.method_3739().method_9217(), command);
    }

    private static String uuid(class_1297 e) {
        return e.method_5667().toString();
    }

    private static String num(double v) {
        return String.format(java.util.Locale.ROOT, "%.2f", v);
    }

    private static boolean tick(MinecraftServer server, class_3218 world, class_1297 entity, SnorlaxRaidService.ActiveRaid raid, EmipokemonConfig.SnorlaxBossSettings config) {
        if (!entity.method_5667().equals(bossId)) {
            reset();
            bossId = entity.method_5667();
            gap = 120;
            System.out.println("[snorlax_emi] ataques extra activos (salto, rocas, bostezo)");
        }
        if (DEBUG && (++debugCalls % 100) == 1) {
            System.out.println("[snorlax_emi] debug: llamadas=" + debugCalls + " participantes=" + raid.participants.size() + " crush=" + raid.crushChargeTicksRemaining + " onda=" + raid.shockwaveActive + " fase=" + raid.phase);
        }
        if (raid.crushChargeTicksRemaining >= 0 || raid.shockwaveActive) {
            return false;
        }
        List<class_3222> alive = new ArrayList<>();
        int dbgNull = 0, dbgDead = 0, dbgWorld = 0;
        for (UUID id : raid.participants) {
            class_3222 p = server.method_3760().method_14602(id);
            if (p == null) {
                dbgNull++;
            } else if (!p.method_5805()) {
                dbgDead++;
            } else if (p.method_37908() != world) {
                dbgWorld++;
            } else {
                alive.add(p);
            }
        }
        if (alive.isEmpty()) {
            if (DEBUG && debugCalls % 100 == 1) {
                System.out.println("[snorlax_emi] debug: ningun participante vivo en este mundo (offline=" + dbgNull + " muertos=" + dbgDead + " otro mundo=" + dbgWorld + ")");
            }
            reset();
            return false;
        }
        // quien se queda lejos (arqueros): se cuentan los ticks seguidos a mas de 12 bloques
        UUID kiter = null;
        class_3222 farthest = null;
        double farthestD2 = 0;
        for (class_3222 p : alive) {
            double d2 = entity.method_5858(p);
            if (d2 > farthestD2) {
                farthestD2 = d2;
                farthest = p;
            }
            if (d2 > 144.0) {
                int n = kite.merge(p.method_5667(), 1, Integer::sum);
                if (n >= 80 && (kiter == null || n > kite.get(kiter))) {
                    kiter = p.method_5667();
                }
            } else {
                kite.remove(p.method_5667());
            }
        }
        if (DEBUG && (++debugTicks % 100) == 0) {
            System.out.println("[snorlax_emi] debug: vivos=" + alive.size() + " modo=" + mode + " espera=" + gap + " kite=" + kite.values() + " fase=" + raid.phase);
        }
        if (mode != IDLE) {
            ticks--;
            switch (mode) {
                case LEAP -> tickLeap(server, world, entity, raid, config, alive);
                case ROCKS -> tickRocks(server, world, entity, raid, config, alive);
                case YAWN -> tickYawn(server, world, entity, raid, config, alive);
                default -> reset();
            }
            return true;
        }
        if (gap > 0) {
            gap--;
            if (kiter == null || gap > 60) {
                return false;
            }
        }
        // elegir ataque
        class_3222 target = null;
        if (kiter != null) {
            for (class_3222 p : alive) {
                if (p.method_5667().equals(kiter)) {
                    target = p;
                }
            }
        }
        if (target != null) {
            startLeap(server, world, entity, raid, target);
            return true;
        }
        List<Integer> options = new ArrayList<>();
        if (farthest != null && farthestD2 > 64.0) {
            options.add(LEAP);
            options.add(LEAP);
            options.add(LEAP);
        }
        if (raid.phase >= 2) {
            options.add(ROCKS);
            options.add(ROCKS);
            boolean anyClose = false;
            for (class_3222 p : alive) {
                if (entity.method_5858(p) < 144.0) {
                    anyClose = true;
                }
            }
            if (anyClose) {
                options.add(YAWN);
                options.add(YAWN);
            }
        } else if (farthest != null && farthestD2 > 196.0) {
            options.add(ROCKS);
        }
        if (options.isEmpty()) {
            gap = 40;
            return false;
        }
        int pick = options.get(RNG.nextInt(options.size()));
        switch (pick) {
            case LEAP -> startLeap(server, world, entity, raid, farthest);
            case ROCKS -> startRocks(server, world, entity, raid, alive, config);
            default -> startYawn(server, world, entity, raid);
        }
        return true;
    }

    private static void endAttack(int phase) {
        mode = IDLE;
        spots.clear();
        leapName = null;
        gap = phase >= 3 ? 80 + RNG.nextInt(60) : phase == 2 ? 120 + RNG.nextInt(80) : 160 + RNG.nextInt(100);
    }

    private static String dim(class_3218 world) {
        return world.method_27983().method_29177().toString();
    }

    private static void ring(MinecraftServer server, class_3218 world, double x, double y, double z, String color, double radius, int count) {
        run(server, "execute in " + dim(world) + " positioned " + num(x) + " " + num(y) + " " + num(z) + " run particle minecraft:dust{color:" + color + ",scale:2} ~ ~0.15 ~ " + num(radius) + " 0.05 " + num(radius) + " 0 " + count + " force");
    }

    private static void damage(MinecraftServer server, class_1297 boss, class_3222 player, double amount) {
        run(server, "damage " + uuid(player) + " " + num(amount) + " minecraft:mob_attack by " + uuid(boss));
    }

    // ---------- Salto Aplastante: salta sobre el que esta lejos ----------
    private static void startLeap(MinecraftServer server, class_3218 world, class_1297 entity, SnorlaxRaidService.ActiveRaid raid, class_3222 target) {
        mode = LEAP;
        ticks = 30;
        spots.clear();
        spots.add(new double[]{target.method_23317(), target.method_23318(), target.method_23321()});
        leapName = target.method_7334().getName();
        kite.remove(target.method_5667());
        SnorlaxRaidService.broadcastToParticipants(server, raid, "§c¡Snorlax Emi salta hacia §f" + leapName + "§c! §7¡Muévete de ahí!");
        SnorlaxRaidService.triggerAnimation(entity, "physical");
        System.out.println("[snorlax_emi] salto aplastante -> " + leapName);
    }

    private static void tickLeap(MinecraftServer server, class_3218 world, class_1297 entity, SnorlaxRaidService.ActiveRaid raid, EmipokemonConfig.SnorlaxBossSettings config, List<class_3222> alive) {
        double[] s = spots.get(0);
        if (ticks > 0) {
            if (ticks % 4 == 0) {
                ring(server, world, s[0], s[1], s[2], "[1.0,0.1,0.1]", 5.0, 14);
            }
            return;
        }
        run(server, "execute in " + dim(world) + " run tp " + uuid(entity) + " " + num(s[0]) + " " + num(s[1]) + " " + num(s[2]));
        double damage = SnorlaxRaidService.phaseAreaSlamDamage(raid.phase, config) * 1.3 * SnorlaxRaidService.damageMultiplier(raid.participants.size(), config);
        for (class_3222 p : alive) {
            double dx = p.method_23317() - s[0], dz = p.method_23321() - s[2], dy = p.method_23318() - s[1];
            if (dx * dx + dz * dz <= 5.5 * 5.5 && Math.abs(dy) < 4.0) {
                damage(server, entity, p, damage);
                SnorlaxRaidService.knockback(entity, p, 1.4);
            }
        }
        run(server, "execute in " + dim(world) + " positioned " + num(s[0]) + " " + num(s[1]) + " " + num(s[2]) + " run particle minecraft:explosion_emitter ~ ~0.5 ~ 0 0 0 0 1 force");
        run(server, "execute in " + dim(world) + " positioned " + num(s[0]) + " " + num(s[1]) + " " + num(s[2]) + " run particle minecraft:poof ~ ~0.3 ~ 3 0.2 3 0.1 60 force");
        run(server, "execute in " + dim(world) + " positioned " + num(s[0]) + " " + num(s[1]) + " " + num(s[2]) + " run playsound emipokemon:snorlax_emi_voz hostile @a ~ ~ ~ 3 1");
        endAttack(raid.phase);
    }

    // ---------- Lluvia de Rocas: marcas en el suelo bajo los jugadores ----------
    private static void startRocks(MinecraftServer server, class_3218 world, class_1297 entity, SnorlaxRaidService.ActiveRaid raid, List<class_3222> alive, EmipokemonConfig.SnorlaxBossSettings config) {
        mode = ROCKS;
        ticks = 40;
        spots.clear();
        List<class_3222> pool = new ArrayList<>(alive);
        Collections.shuffle(pool, RNG);
        int count = Math.min(pool.size(), raid.phase >= 3 ? 4 : raid.phase == 2 ? 3 : 1);
        for (int i = 0; i < count; i++) {
            class_3222 p = pool.get(i);
            spots.add(new double[]{p.method_23317(), p.method_23318(), p.method_23321()});
        }
        SnorlaxRaidService.broadcastToParticipants(server, raid, "§6¡Snorlax Emi arranca rocas del suelo! §7¡Sal de las marcas naranjas!");
        SnorlaxRaidService.triggerAnimation(entity, "physical");
        System.out.println("[snorlax_emi] lluvia de rocas (" + count + " marcas)");
    }

    private static void tickRocks(MinecraftServer server, class_3218 world, class_1297 entity, SnorlaxRaidService.ActiveRaid raid, EmipokemonConfig.SnorlaxBossSettings config, List<class_3222> alive) {
        if (ticks > 0) {
            if (ticks % 4 == 0) {
                for (double[] s : spots) {
                    ring(server, world, s[0], s[1], s[2], "[1.0,0.55,0.0]", 3.5, 12);
                }
            }
            return;
        }
        double damage = SnorlaxRaidService.phaseMeleeDamage(raid.phase, config) * 0.9 * SnorlaxRaidService.damageMultiplier(raid.participants.size(), config);
        for (double[] s : spots) {
            for (class_3222 p : alive) {
                double dx = p.method_23317() - s[0], dz = p.method_23321() - s[2], dy = p.method_23318() - s[1];
                if (dx * dx + dz * dz <= 3.5 * 3.5 && Math.abs(dy) < 4.0) {
                    damage(server, entity, p, damage);
                    SnorlaxRaidService.knockback(entity, p, 0.8);
                }
            }
            run(server, "execute in " + dim(world) + " positioned " + num(s[0]) + " " + num(s[1]) + " " + num(s[2]) + " run particle minecraft:block{block_state:\"minecraft:stone\"} ~ ~1 ~ 1.2 1 1.2 0.1 50 force");
            run(server, "execute in " + dim(world) + " positioned " + num(s[0]) + " " + num(s[1]) + " " + num(s[2]) + " run playsound minecraft:block.stone.break hostile @a ~ ~ ~ 3 0.6");
        }
        endAttack(raid.phase);
    }

    // ---------- Bostezo: ralentiza y debilita a los cercanos ----------
    private static void startYawn(MinecraftServer server, class_3218 world, class_1297 entity, SnorlaxRaidService.ActiveRaid raid) {
        mode = YAWN;
        ticks = 36;
        SnorlaxRaidService.broadcastToParticipants(server, raid, "§5¡Snorlax Emi bosteza! §7¡Aléjate o te quedarás sin fuerzas!");
        System.out.println("[snorlax_emi] bostezo");
    }

    private static void tickYawn(MinecraftServer server, class_3218 world, class_1297 entity, SnorlaxRaidService.ActiveRaid raid, EmipokemonConfig.SnorlaxBossSettings config, List<class_3222> alive) {
        if (ticks > 0) {
            if (ticks % 6 == 0) {
                run(server, "execute as " + uuid(entity) + " at @s run particle minecraft:cloud ~ ~1 ~ 4 0.8 4 0.02 25 force");
            }
            return;
        }
        for (class_3222 p : alive) {
            if (entity.method_5858(p) <= 12.0 * 12.0) {
                run(server, "effect give " + uuid(p) + " minecraft:slowness 6 2 true");
                run(server, "effect give " + uuid(p) + " minecraft:weakness 8 1 true");
                if (raid.phase >= 3) {
                    run(server, "effect give " + uuid(p) + " minecraft:nausea 4 0 true");
                }
            }
        }
        run(server, "execute as " + uuid(entity) + " at @s run particle minecraft:dust{color:[0.6,0.2,0.8],scale:2} ~ ~0.5 ~ 6 0.5 6 0 80 force");
        run(server, "execute as " + uuid(entity) + " at @s run playsound emipokemon:snorlax_emi_voz hostile @a ~ ~ ~ 3 0.7");
        endAttack(raid.phase);
    }
}
