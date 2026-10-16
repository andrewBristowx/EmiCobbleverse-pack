package emi.survivors;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/** /survivor. De momento solo hay comandos de prueba para el organizador (operador de nivel 2 o el permiso emi_survivors.admin). */
public final class Cmds {
    @FunctionalInterface private interface Tarea { String run() throws CommandSyntaxException; }

    public static void register(CommandDispatcher<ServerCommandSource> d) {
        var root = CommandManager.literal("survivor");
        // /survivor prueba [hueco]: una partida de verdad (oleadas, cartas, jefe) para quien lo escribe, con el Pokemon del hueco 1..6 del equipo
        root.then(admin("prueba").executes(c -> con(c, () -> Partida.iniciar(c.getSource().getPlayerOrThrow(), 1, 0, true), "Preparando la partida..."))
                .then(CommandManager.argument("hueco", IntegerArgumentType.integer(1, 6))
                        .executes(c -> con(c, () -> Partida.iniciar(c.getSource().getPlayerOrThrow(), IntegerArgumentType.getInteger(c, "hueco"), 0, true), "Preparando la partida..."))));
        // /survivor medida <enemigos> [hueco]: el modo de MEDIDA de la fase 0 (un numero fijo de enemigos desde el principio, sin oleadas ni jefe)
        root.then(admin("medida").then(CommandManager.argument("enemigos", IntegerArgumentType.integer(0, 400))
                .executes(c -> con(c, () -> Partida.iniciar(c.getSource().getPlayerOrThrow(), 1, Math.max(1, IntegerArgumentType.getInteger(c, "enemigos")), true), "Preparando la medida..."))
                .then(CommandManager.argument("hueco", IntegerArgumentType.integer(1, 6))
                        .executes(c -> con(c, () -> Partida.iniciar(c.getSource().getPlayerOrThrow(), IntegerArgumentType.getInteger(c, "hueco"),
                                Math.max(1, IntegerArgumentType.getInteger(c, "enemigos")), true), "Preparando la medida...")))));
        // /survivor parar: cancela tu partida; si eres administrador y no estas en ninguna, cancela todas
        root.then(CommandManager.literal("parar").executes(c -> {
            var p = c.getSource().getPlayerOrThrow();
            if (Partida.participa(p)) return con(c, () -> Partida.parar(p));
            if (Perm.admin(c.getSource())) { int n = Partida.pararTodas(); c.getSource().sendFeedback(() -> Msg.say(n == 0 ? "No hay ninguna partida en marcha." : "Canceladas " + n + " partidas."), false); return 1; }
            c.getSource().sendError(Msg.err("No estás en ninguna partida.")); return 0;
        }));
        // modo prueba: el jugador no pierde vida (para medir el rendimiento sin que la partida acabe)
        root.then(admin("inmortal").executes(c -> inmortal(c, !Partida.inmortal))
                .then(CommandManager.literal("on").executes(c -> inmortal(c, true)))
                .then(CommandManager.literal("off").executes(c -> inmortal(c, false))));
        // ayudas para probar (solo en tu propia partida): /survivor dev xp|nivel|tiempo <n>, jefe, elite, curar, arma <id> [nivel], pasiva <id> [nivel]
        var dev = admin("dev");
        for (String k : new String[]{"xp", "nivel", "tiempo"})
            dev.then(CommandManager.literal(k).then(CommandManager.argument("n", IntegerArgumentType.integer(0, 100000))
                    .executes(c -> con(c, () -> Partida.dev(c.getSource().getPlayerOrThrow(), k, "", IntegerArgumentType.getInteger(c, "n"))))));
        for (String k : new String[]{"jefe", "elite", "curar", "sinxp"})
            dev.then(CommandManager.literal(k).executes(c -> con(c, () -> Partida.dev(c.getSource().getPlayerOrThrow(), k, "", 1))));
        for (String k : new String[]{"arma", "solo", "pasiva"})
            dev.then(CommandManager.literal(k).then(CommandManager.argument("id", StringArgumentType.word())
                    .executes(c -> con(c, () -> Partida.dev(c.getSource().getPlayerOrThrow(), k, StringArgumentType.getString(c, "id"), 1)))
                    .then(CommandManager.argument("nivel", IntegerArgumentType.integer(1, 5))
                            .executes(c -> con(c, () -> Partida.dev(c.getSource().getPlayerOrThrow(), k, StringArgumentType.getString(c, "id"), IntegerArgumentType.getInteger(c, "nivel")))))));
        root.then(dev);
        root.then(CommandManager.literal("estado").executes(c -> { c.getSource().sendFeedback(() -> Msg.say(Partida.estado()), false); return 1; }));
        root.executes(c -> { c.getSource().sendFeedback(() -> Msg.say("Emi Survivors (fase 1). Comandos de prueba: /survivor prueba [hueco], /survivor medida <enemigos>, /survivor parar, /survivor inmortal, /survivor estado, /survivor dev ..."), false); return 1; });
        d.register(root);
    }

    private static LiteralArgumentBuilder<ServerCommandSource> admin(String nombre) {
        return CommandManager.literal(nombre).requires(Perm::admin);
    }

    private static int inmortal(CommandContext<ServerCommandSource> c, boolean on) {
        Partida.inmortal = on;
        c.getSource().sendFeedback(() -> Msg.say("Modo prueba inmortal " + (on ? "activado" : "desactivado") + " (vale para la siguiente partida)."), true);
        return 1;
    }

    private static int con(CommandContext<ServerCommandSource> c, Tarea t) { return con(c, t, null); }

    private static int con(CommandContext<ServerCommandSource> c, Tarea t, String ok) {
        try {
            String err = t.run();
            if (err != null) { c.getSource().sendError(Msg.err(err)); return 0; }
            if (ok != null) c.getSource().sendFeedback(() -> Msg.say(ok), false);
            return 1;
        } catch (CommandSyntaxException e) {
            c.getSource().sendError(Text.literal("Este comando solo lo puede usar un jugador."));
            return 0;
        }
    }
}
