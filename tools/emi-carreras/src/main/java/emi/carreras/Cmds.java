package emi.carreras;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

/** /carrera. Los subcomandos del organizador piden ser operador (nivel 2) o el permiso emi_carreras.admin. */
public final class Cmds {
    @FunctionalInterface private interface Tarea { String run() throws CommandSyntaxException; }

    public static void register(CommandDispatcher<ServerCommandSource> d) {
        var root = CommandManager.literal("carrera");
        // --- jugadores
        root.then(CommandManager.literal("unirse").executes(c -> con(c, () -> Carrera.unirse(c.getSource().getPlayerOrThrow()))));
        root.then(CommandManager.literal("salir").executes(c -> con(c, () -> Carrera.salir(c.getSource().getPlayerOrThrow()))));
        root.then(CommandManager.literal("elegir").then(CommandManager.argument("hueco", IntegerArgumentType.integer(1, 6))
                .executes(c -> con(c, () -> Carrera.elegir(c.getSource().getPlayerOrThrow(), IntegerArgumentType.getInteger(c, "hueco"))))));
        root.then(CommandManager.literal("estado").executes(Cmds::estado));
        root.then(CommandManager.literal("ayuda").executes(Cmds::ayuda));
        root.executes(Cmds::ayuda);
        // --- organizador
        root.then(admin("abrir").executes(c -> con(c, Carrera::abrir, "Cola abierta: se ha avisado a todos.")));
        root.then(admin("cerrar").executes(c -> con(c, Carrera::cerrarCola)));
        root.then(admin("empezar").executes(c -> con(c, Carrera::empezar, "Preparando la parrilla...")));
        root.then(admin("parar").executes(c -> con(c, () -> Carrera.parar(null))));
        root.then(admin("instalar").executes(c -> con(c, Carrera::instalar, "Pista colocada.")));
        root.then(admin("recargar").executes(c -> con(c, () -> { if (Carrera.fase() != Carrera.Fase.LIBRE) return "Espera a que no haya carrera."; Carrera.init(c.getSource().getServer()); return null; }, "Config y pista recargadas.")));
        root.then(admin("expulsar").then(CommandManager.argument("jugador", EntityArgumentType.player()).executes(c -> con(c, () -> {
            ServerPlayerEntity p = EntityArgumentType.getPlayer(c, "jugador");
            var co = Carrera.corr.get(p.getUuid());
            if (co == null) return "Ese jugador no esta en la carrera.";
            if (Carrera.fase() == Carrera.Fase.COLA) Carrera.corr.remove(p.getUuid()); else Carrera.abandonar(co, p, "ha sido expulsado");
            return null;
        }, "Hecho."))));
        root.then(admin("_poder").then(CommandManager.argument("nombre", com.mojang.brigadier.arguments.StringArgumentType.word())
                .executes(c -> con(c, () -> Carrera.probar(c.getSource().getPlayerOrThrow(), com.mojang.brigadier.arguments.StringArgumentType.getString(c, "nombre")), "Poder dado."))));
        d.register(root);
    }

    private static LiteralArgumentBuilder<ServerCommandSource> admin(String name) {
        return CommandManager.literal(name).requires(Perm::admin);
    }

    private static int con(CommandContext<ServerCommandSource> c, Tarea f) { return con(c, f, null); }

    /** Ejecuta; si devuelve texto es un error. */
    private static int con(CommandContext<ServerCommandSource> c, Tarea f, String ok) {
        String r;
        try { r = f.run(); } catch (CommandSyntaxException cs) { c.getSource().sendError(Text.literal(cs.getMessage())); return 0; }
        catch (RuntimeException e) {
            EmiCarreras.LOG.error("Error en /carrera", e);
            c.getSource().sendError(Text.literal("Error interno: " + e)); return 0;
        }
        if (r != null) { c.getSource().sendError(Text.literal(r)); return 0; }
        if (ok != null) c.getSource().sendFeedback(() -> Msg.say(ok), false);
        return 1;
    }

    private static int estado(CommandContext<ServerCommandSource> c) {
        String t = switch (Carrera.fase()) {
            case LIBRE -> "No hay ninguna carrera.";
            case COLA -> "Cola abierta: " + Carrera.corr.size() + " corredor(es) apuntado(s).";
            case CUENTA -> "Cuenta atras en marcha.";
            case CARRERA -> "Carrera en marcha.";
            case FIN -> "La carrera ha terminado.";
        };
        c.getSource().sendFeedback(() -> Msg.say(t), false);
        return 1;
    }

    private static int ayuda(CommandContext<ServerCommandSource> c) {
        String[] l = {"/carrera unirse · /carrera salir · /carrera elegir <1-6> · /carrera estado",
                "Corres con uno de tus Pokémon terrestres (todos igual de rápidos). Pasa por todos los puntos de control.",
                "Recoge los huevos misteriosos y usa el poder con clic derecho (agáchate para tirarlo hacia atrás).",
                "Organizador: /carrera abrir · empezar · parar · cerrar · expulsar <jugador> · instalar · recargar"};
        for (String s : l) c.getSource().sendFeedback(() -> Text.literal(s).formatted(Formatting.YELLOW), false);
        return 1;
    }
}
