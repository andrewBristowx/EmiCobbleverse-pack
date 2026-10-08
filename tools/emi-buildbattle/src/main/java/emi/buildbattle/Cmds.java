package emi.buildbattle;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Function;

/** /buildbattle (alias /bb). Los subcomandos del organizador piden permiso de operador (nivel 2). */
public final class Cmds {
    private static final int ADMIN = 2;

    public static void register(CommandDispatcher<ServerCommandSource> d) {
        var root = CommandManager.literal("buildbattle");
        // --- jugadores
        root.then(CommandManager.literal("unirse").executes(c -> player(c, Game::join)));
        root.then(CommandManager.literal("salir").executes(c -> player(c, Game::leave)));
        root.then(CommandManager.literal("bloques").executes(c -> {
            ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
            if (!Game.isBuilding(p)) return fail(c, "Solo puedes pedir bloques mientras construyes.");
            Menu.open(p);
            return 1;
        }));
        root.then(CommandManager.literal("buscar").then(CommandManager.argument("texto", StringArgumentType.greedyString()).executes(c -> {
            ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
            if (!Game.isBuilding(p)) return fail(c, "Solo puedes pedir bloques mientras construyes.");
            Menu.openSearch(p, StringArgumentType.getString(c, "texto"));
            return 1;
        })));
        root.then(CommandManager.literal("estado").executes(Cmds::status));
        root.then(CommandManager.literal("ayuda").executes(Cmds::help));
        root.executes(Cmds::help);

        // --- organizador
        root.then(admin("abrir").executes(c -> simple(c, Game::open, "Cola abierta: se ha avisado a todos los jugadores.")));
        root.then(admin("cerrar").executes(c -> simple(c, Game::closeQueue, null)));
        root.then(admin("empezar").executes(c -> simple(c, () -> Game.start(""), "Preparando las parcelas..."))
                .then(CommandManager.argument("tema", StringArgumentType.greedyString())
                        .executes(c -> simple(c, () -> Game.start(StringArgumentType.getString(c, "tema")), "Preparando las parcelas..."))));
        root.then(admin("tema").then(CommandManager.argument("texto", StringArgumentType.greedyString())
                .executes(c -> simple(c, () -> Game.setTheme(StringArgumentType.getString(c, "texto")), "Tema cambiado."))));
        root.then(admin("temporizador")
                .then(CommandManager.argument("minutos", IntegerArgumentType.integer(1, 240))
                        .executes(c -> simple(c, () -> Game.timer(IntegerArgumentType.getInteger(c, "minutos")), "Temporizador en marcha (solo avisa, no cambia de fase).")))
                .then(CommandManager.literal("cancelar").executes(c -> simple(c, () -> { Game.stopTimer(); return null; }, "Temporizador cancelado."))));
        root.then(admin("votar").executes(c -> simple(c, Game::beginVote, "Votación abierta.")));
        root.then(admin("siguiente").executes(c -> simple(c, Game::next, null)));
        root.then(admin("anterior").executes(c -> simple(c, Game::prev, null)));
        root.then(admin("top").executes(c -> {
            if (Game.ranking().isEmpty() && Game.phase() != Game.Phase.VOTING && Game.phase() != Game.Phase.RESULTS) return fail(c, "Todavía no hay resultados.");
            Game.printFullRanking();
            return 1;
        }));
        root.then(admin("terminar").executes(c -> simple(c, Game::end, "Cerrando la partida: devuelvo los inventarios y limpio la arena.")));
        root.then(admin("parar").executes(c -> simple(c, Game::end, "Partida cancelada: devuelvo los inventarios y limpio la arena.")));
        root.then(admin("expulsar").then(CommandManager.argument("jugador", net.minecraft.command.argument.EntityArgumentType.player()).executes(c -> {
            ServerPlayerEntity t = net.minecraft.command.argument.EntityArgumentType.getPlayer(c, "jugador");
            String err = Game.leave(t);
            if (err != null) return fail(c, err);
            c.getSource().sendFeedback(() -> Msg.say(t.getGameProfile().getName() + " ha salido de la partida."), true);
            return 1;
        })));
        root.then(admin("espectar").then(CommandManager.argument("jugador", StringArgumentType.word()).executes(c -> {
            String err = Game.spectate(c.getSource().getPlayerOrThrow(), StringArgumentType.getString(c, "jugador"));
            if (err != null) return fail(c, err);
            c.getSource().sendFeedback(() -> Msg.say("Modo espectador. Usa /bb volver para regresar."), false);
            return 1;
        })));
        root.then(admin("volver").executes(c -> {
            String err = Game.back(c.getSource().getPlayerOrThrow());
            if (err != null) return fail(c, err);
            return 1;
        }));
        root.then(admin("recargar").executes(c -> {
            Game.reload();
            c.getSource().sendFeedback(() -> Msg.say("Configuración recargada (" + Catalog.all().size() + " bloques en el catalogo)."), false);
            return 1;
        }));

        if (System.getProperty("emi.bb.test") != null) {   // solo para pruebas automaticas del desarrollador
            root.then(CommandManager.literal("_voto").then(CommandManager.argument("jugador", net.minecraft.command.argument.EntityArgumentType.player())
                    .then(CommandManager.argument("n", IntegerArgumentType.integer(1, 9)).executes(c -> {
                        Game.onVote(net.minecraft.command.argument.EntityArgumentType.getPlayer(c, "jugador"), IntegerArgumentType.getInteger(c, "n"));
                        return 1;
                    }))));
        }
        d.register(root);
        d.register(CommandManager.literal("bb").redirect(d.getRoot().getChild("buildbattle")));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> admin(String name) {
        return CommandManager.literal(name).requires(Perm::admin);
    }

    private interface Action { String run(); }

    private static int simple(CommandContext<ServerCommandSource> c, Action a, String ok) {
        String err = a.run();
        if (err != null) return fail(c, err);
        if (ok != null) c.getSource().sendFeedback(() -> Msg.say(ok), false);
        return 1;
    }

    private static int player(CommandContext<ServerCommandSource> c, Function<ServerPlayerEntity, String> f) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
        String err = f.apply(p);
        if (err != null) return fail(c, err);
        return 1;
    }

    private static int fail(CommandContext<ServerCommandSource> c, String msg) {
        c.getSource().sendError(Msg.err(msg));
        return 0;
    }

    private static int help(CommandContext<ServerCommandSource> c) {
        ServerCommandSource s = c.getSource();
        s.sendFeedback(() -> Msg.say(Text.literal("Comandos").formatted(Formatting.GOLD)), false);
        String[][] players = {{"/bb unirse", "entrar a la cola"}, {"/bb salir", "salir de la cola o de la partida"},
                {"/bb bloques", "abrir el menu de bloques gratis"}, {"/bb buscar <texto>", "buscar un bloque por nombre"}, {"/bb estado", "ver como va la partida"}};
        for (String[] l : players) s.sendFeedback(() -> Text.literal(" " + l[0] + " ").formatted(Formatting.YELLOW).append(Text.literal(l[1]).formatted(Formatting.GRAY)), false);
        if (Perm.admin(s)) {
            String[][] adm = {{"/bb abrir", "abrir la cola y avisar a todos"}, {"/bb cerrar", "cerrar la cola sin empezar"},
                    {"/bb empezar [tema]", "llevar a los apuntados a su parcela"}, {"/bb tema <texto>", "cambiar el tema"},
                    {"/bb temporizador <min>|cancelar", "cuenta atras (solo avisa)"}, {"/bb votar", "abrir la votación"},
                    {"/bb siguiente / anterior", "pasar de construcción; en la última revela el top"}, {"/bb top", "repetir la clasificación"},
                    {"/bb terminar", "devolver todo a los jugadores y limpiar la arena"}, {"/bb parar", "cancelar la partida"},
                    {"/bb expulsar <jugador>", "sacar a alguien de la partida"}, {"/bb espectar <jugador>", "mirar su parcela (/bb volver)"},
                    {"/bb recargar", "recargar config/emi_buildbattle.json"}};
            s.sendFeedback(() -> Text.literal("Organizador:").formatted(Formatting.GOLD), false);
            for (String[] l : adm) s.sendFeedback(() -> Text.literal(" " + l[0] + " ").formatted(Formatting.AQUA).append(Text.literal(l[1]).formatted(Formatting.GRAY)), false);
        }
        return 1;
    }

    private static int status(CommandContext<ServerCommandSource> c) {
        ServerCommandSource s = c.getSource();
        Game.Phase ph = Game.phase();
        String name = switch (ph) {
            case IDLE -> "sin partida"; case QUEUE -> "cola abierta"; case STARTING -> "preparando"; case BUILDING -> "construyendo";
            case VOTING -> "votación"; case RESULTS -> "resultados"; case ENDING -> "cerrando";
        };
        s.sendFeedback(() -> Msg.say(Text.literal("Fase: ").formatted(Formatting.WHITE).append(Text.literal(name).formatted(Formatting.GOLD))), false);
        if (ph == Game.Phase.QUEUE) {
            s.sendFeedback(() -> Text.literal(" Apuntados (" + Game.queueSize() + "): " + String.join(", ", Game.queueNames())).formatted(Formatting.GRAY), false);
        } else if (ph != Game.Phase.IDLE) {
            s.sendFeedback(() -> Text.literal(" Jugando: " + Game.participants().size() + " (parcelas: " + Game.plots().size() + ")").formatted(Formatting.GRAY), false);
            if (!Game.theme().isEmpty()) s.sendFeedback(() -> Text.literal(" Tema: " + Game.theme()).formatted(Formatting.GRAY), false);
            String t = Game.timerStatus();
            if (t != null) s.sendFeedback(() -> Text.literal(" Tiempo: " + t).formatted(Formatting.GRAY), false);
            if (ph == Game.Phase.VOTING && Perm.admin(s)) {
                s.sendFeedback(() -> Text.literal(" Construcción " + (Game.voteIndex() + 1) + "/" + Game.voteTotal() + ", votos: " + Game.voteProgress()).formatted(Formatting.GRAY), false);
            }
        }
        return 1;
    }
}
