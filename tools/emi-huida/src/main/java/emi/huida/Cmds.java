package emi.huida;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/** /huida. Los subcomandos del organizador piden ser operador (nivel 2) o el permiso emi_huida.admin. */
public final class Cmds {
    @FunctionalInterface private interface Tarea { String run() throws CommandSyntaxException; }

    public static void register(CommandDispatcher<ServerCommandSource> d) {
        var root = CommandManager.literal("huida");
        // --- jugadores
        root.then(CommandManager.literal("unirse").executes(c -> con(c, () -> Huida.unirse(c.getSource().getPlayerOrThrow()))));
        root.then(CommandManager.literal("salir").executes(c -> con(c, () -> Huida.salir(c.getSource().getPlayerOrThrow()))));
        root.then(CommandManager.literal("listo").executes(c -> con(c, () -> Huida.listo(c.getSource().getPlayerOrThrow()))));
        root.then(CommandManager.literal("estado").executes(c -> { c.getSource().sendFeedback(() -> Msg.say(Huida.estado()), false); return 1; }));
        root.then(CommandManager.literal("ayuda").executes(Cmds::ayuda));
        root.executes(Cmds::ayuda);
        // --- organizador
        root.then(admin("abrir").executes(c -> con(c, Huida::abrir, "Cola abierta: se ha avisado a todos.")));
        root.then(admin("cerrar").executes(c -> con(c, Huida::cerrarCola)));
        // /huida empezar = lleva a todos a su pista y espera a que confirmen «listo»; /huida empezar ya = sin confirmacion
        root.then(admin("empezar").executes(c -> con(c, () -> Huida.empezar(false), "Preparando la huida: se espera a que todos confirmen «listo» (/huida saltar para no esperar)..."))
                .then(CommandManager.literal("ya").executes(c -> con(c, () -> Huida.empezar(true), "Preparando la huida (sin confirmación)..."))));
        root.then(admin("saltar").executes(c -> con(c, Huida::saltar, "Confirmación saltada.")));
        root.then(admin("forzar").executes(c -> con(c, Huida::saltar, "Confirmación saltada.")));
        root.then(admin("prueba").then(CommandManager.argument("escena", com.mojang.brigadier.arguments.StringArgumentType.word())
                .executes(c -> con(c, () -> Huida.prueba(c.getSource().getPlayerOrThrow(), com.mojang.brigadier.arguments.StringArgumentType.getString(c, "escena")), "Preparando la prueba..."))));
        root.then(admin("parar").executes(c -> con(c, () -> Huida.parar(null))));
        d.register(root);
    }

    private static LiteralArgumentBuilder<ServerCommandSource> admin(String name) { return CommandManager.literal(name).requires(Perm::admin); }

    private static int con(CommandContext<ServerCommandSource> c, Tarea f) { return con(c, f, null); }

    /** Ejecuta; si devuelve texto es un error. */
    private static int con(CommandContext<ServerCommandSource> c, Tarea f, String ok) {
        String r;
        try { r = f.run(); } catch (CommandSyntaxException cs) { c.getSource().sendError(Text.literal(cs.getMessage())); return 0; }
        catch (Throwable e) {
            EmiHuida.LOG.error("Error en /huida", e);
            c.getSource().sendError(Text.literal("Error interno: " + e)); return 0;
        }
        if (r != null) { c.getSource().sendError(Text.literal(r)); return 0; }
        if (ok != null) c.getSource().sendFeedback(() -> Msg.say(ok), false);
        return 1;
    }

    private static int ayuda(CommandContext<ServerCommandSource> c) {
        c.getSource().sendFeedback(() -> Msg.say("Huida del Tyrantrum: corre por las ruinas, salta, agáchate (shift) y esquiva antes de que te alcance."), false);
        c.getSource().sendFeedback(() -> Msg.say("Jugadores: /huida unirse · /huida listo (o tecla R) · /huida salir · /huida estado"), false);
        if (Perm.admin(c.getSource())) c.getSource().sendFeedback(() -> Msg.say("Organizador: /huida abrir · /huida empezar [ya] · /huida saltar (salta la confirmación) · /huida parar · /huida cerrar"), false);
        return 1;
    }
}
