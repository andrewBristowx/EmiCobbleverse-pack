package emi.survivors;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/** /survivor. En la fase 0 solo hay comandos de prueba para el organizador (operador de nivel 2 o el permiso emi_survivors.admin). */
public final class Cmds {
    @FunctionalInterface private interface Tarea { String run() throws CommandSyntaxException; }

    public static void register(CommandDispatcher<ServerCommandSource> d) {
        var root = CommandManager.literal("survivor");
        // /survivor prueba [enemigos] [hueco]: partida de prueba para quien lo escribe (enemigos: 0..400, por defecto 64; hueco del equipo 1..6, por defecto 1)
        root.then(admin("prueba").executes(c -> con(c, () -> Partida.prueba(c.getSource().getPlayerOrThrow(), 64, 1), "Preparando la partida de prueba...")).then(
                CommandManager.argument("enemigos", IntegerArgumentType.integer(0, 400))
                        .executes(c -> con(c, () -> Partida.prueba(c.getSource().getPlayerOrThrow(), IntegerArgumentType.getInteger(c, "enemigos"), 1), "Preparando la partida de prueba..."))
                        .then(CommandManager.argument("hueco", IntegerArgumentType.integer(1, 6))
                                .executes(c -> con(c, () -> Partida.prueba(c.getSource().getPlayerOrThrow(), IntegerArgumentType.getInteger(c, "enemigos"),
                                        IntegerArgumentType.getInteger(c, "hueco")), "Preparando la partida de prueba...")))));
        root.then(CommandManager.literal("parar").executes(c -> con(c, () -> Partida.parar(Perm.admin(c.getSource()) ? null : c.getSource().getPlayerOrThrow()))));
        // modo prueba: el jugador no pierde vida (para medir el rendimiento sin que la partida acabe)
        root.then(admin("inmortal").executes(c -> inmortal(c, !Partida.inmortal))
                .then(CommandManager.literal("on").executes(c -> inmortal(c, true)))
                .then(CommandManager.literal("off").executes(c -> inmortal(c, false))));
        root.then(CommandManager.literal("estado").executes(c -> { c.getSource().sendFeedback(() -> Msg.say(Partida.estado()), false); return 1; }));
        root.executes(c -> { c.getSource().sendFeedback(() -> Msg.say("Emi Survivors (fase 0, prueba de viabilidad). Comandos: /survivor prueba [enemigos] [hueco], /survivor parar, /survivor inmortal, /survivor estado"), false); return 1; });
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
