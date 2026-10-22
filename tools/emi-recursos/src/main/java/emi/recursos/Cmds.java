package emi.recursos;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import static net.minecraft.server.command.CommandManager.literal;

/** /recursos [info|entrar|salir] y, para administradores, /recursos reiniciar|semilla. */
final class Cmds {
    static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(literal("recursos")
                .executes(Cmds::info)
                .then(literal("info").executes(Cmds::info))
                .then(literal("entrar").executes(Cmds::entrar))
                .then(literal("salir").executes(Cmds::salir))
                .then(literal("reiniciar").requires(s -> s.hasPermissionLevel(2)).executes(Cmds::reiniciar))
                .then(literal("semilla").requires(s -> s.hasPermissionLevel(2)).executes(Cmds::semilla)));
    }

    private static void decir(ServerCommandSource s, String t, Formatting f) {
        s.sendFeedback(() -> Text.literal("[Recursos] ").formatted(Formatting.GOLD, Formatting.BOLD).append(Text.literal(t).formatted(f)), false);
    }

    private static int info(CommandContext<ServerCommandSource> ctx) {
        Estado.Ciclo c = Estado.ciclo();
        ServerCommandSource s = ctx.getSource();
        if (c == null || !Mundos.abiertos()) { decir(s, "Los mundos de recursos no estan disponibles ahora mismo.", Formatting.RED); return 0; }
        decir(s, "Mundo de recursos: un overworld con su nether y su end, nuevos cada " + Ciclo.formato(Ciclo.DURACION_MS) + ". Se reinicia en " + Ciclo.formato(c.reinicioMs - System.currentTimeMillis()) + ".", Formatting.YELLOW);
        decir(s, "Hay " + Ciclo.dentro(s.getServer()).size() + " jugador(es) dentro. /recursos entrar para ir, /recursos salir para volver.", Formatting.GRAY);
        return 1;
    }

    private static int entrar(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource s = ctx.getSource();
        ServerPlayerEntity p = s.getPlayer();
        if (p == null) { decir(s, "Solo lo puede usar un jugador.", Formatting.RED); return 0; }
        if (!Mundos.abiertos()) { decir(s, "Los mundos de recursos no estan disponibles ahora mismo.", Formatting.RED); return 0; }
        ServerWorld actual = p.getServerWorld();
        if (Mundos.esRecursos(actual)) { decir(s, "Ya estas en el mundo de recursos.", Formatting.RED); return 0; }
        if (!"minecraft".equals(actual.getRegistryKey().getValue().getNamespace())) { decir(s, "Estas en un minijuego: sal primero.", Formatting.RED); return 0; }
        Estado.Jug j = Estado.jugNuevo(p.getUuid());
        j.dim = actual.getRegistryKey().getValue().toString();
        j.x = p.getX(); j.y = p.getY(); j.z = p.getZ(); j.yaw = p.getYaw(); j.pitch = p.getPitch();
        Estado.guardar();
        ServerWorld w = Mundos.overworld();
        BlockPos sp = Mundos.spawn();
        p.teleport(w, sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5, 0f, 0f);
        long falta = Estado.ciclo().reinicioMs - System.currentTimeMillis();
        decir(s, "Estas en el mundo de recursos. Se reinicia en " + Ciclo.formato(falta) + " y te avisare antes. /recursos salir para volver cuando quieras.", Formatting.GREEN);
        return 1;
    }

    private static int salir(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource s = ctx.getSource();
        ServerPlayerEntity p = s.getPlayer();
        if (p == null) { decir(s, "Solo lo puede usar un jugador.", Formatting.RED); return 0; }
        if (!Mundos.esRecursos(p.getServerWorld())) { decir(s, "No estas en el mundo de recursos.", Formatting.RED); return 0; }
        Ciclo.sacar(p, "Has vuelto al mundo normal.");
        return 1;
    }

    private static int reiniciar(CommandContext<ServerCommandSource> ctx) {
        decir(ctx.getSource(), "Reiniciando los mundos de recursos ahora...", Formatting.YELLOW);
        Ciclo.reiniciar(ctx.getSource().getServer());
        return 1;
    }

    private static int semilla(CommandContext<ServerCommandSource> ctx) {
        Estado.Ciclo c = Estado.ciclo();
        if (c == null) return 0;
        decir(ctx.getSource(), "Ciclo " + c.epoca + ", semilla " + c.semilla, Formatting.YELLOW);
        return 1;
    }
}
