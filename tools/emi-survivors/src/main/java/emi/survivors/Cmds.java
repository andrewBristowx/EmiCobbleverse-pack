package emi.survivors;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/** /survivor. De momento solo hay comandos de prueba para el organizador (operador de nivel 2 o el permiso emi_survivors.admin). */
public final class Cmds {
    @FunctionalInterface private interface Tarea { String run() throws CommandSyntaxException; }

    public static void register(CommandDispatcher<ServerCommandSource> d) {
        var root = CommandManager.literal("survivor");
        // para todos los jugadores: entrar al mundo del minijuego, salir, abrir el menu y ver el top
        root.then(CommandManager.literal("entrar").executes(c -> con(c, () -> Mundo.entrar(c.getSource().getPlayerOrThrow()))));
        root.then(CommandManager.literal("salir").executes(c -> con(c, () -> Mundo.salir(c.getSource().getPlayerOrThrow()))));
        root.then(CommandManager.literal("menu").executes(c -> con(c, () -> {
            var p = c.getSource().getPlayerOrThrow();
            if (!Mundo.enSala(p)) return "El menú solo se abre en la sala del minijuego (/survivor entrar).";
            if (Partida.participa(p)) return "Estás en una partida.";
            Menu.enviar(p, "jugar", null); return null;
        })));
        root.then(CommandManager.literal("top").executes(c -> { top(c, 10); return 1; })
                .then(CommandManager.argument("cuantos", IntegerArgumentType.integer(1, 100)).executes(c -> { top(c, IntegerArgumentType.getInteger(c, "cuantos")); return 1; }))
                .then(admin("reiniciar").executes(c -> {
                    String copia = Partida.datos == null ? null : Partida.datos.reiniciarTop();
                    c.getSource().sendFeedback(() -> Msg.say(copia == null ? "El top ya estaba vacío." : "Top reiniciado. Copia del anterior guardada en emi_survivors/" + copia), true);
                    return 1;
                })));
        // grupos (lo mismo que la pestaña GRUPO del menu, por si alguien prefiere el chat): crear, unirse <jugador>, salir, expulsar <jugador>, listo [hueco], noListo, empezar [hueco]
        var gr = CommandManager.literal("grupo");
        gr.then(CommandManager.literal("crear").executes(c -> con(c, () -> GrupoCtl.crear(c.getSource().getPlayerOrThrow()))));
        gr.then(CommandManager.literal("unirse").then(CommandManager.argument("lider", EntityArgumentType.player())
                .executes(c -> con(c, () -> GrupoCtl.unir(c.getSource().getPlayerOrThrow(), EntityArgumentType.getPlayer(c, "lider").getUuid())))));
        gr.then(CommandManager.literal("salir").executes(c -> con(c, () -> GrupoCtl.salir(c.getSource().getPlayerOrThrow()), "Has salido del grupo.")));
        gr.then(CommandManager.literal("expulsar").then(CommandManager.argument("jugador", EntityArgumentType.player())
                .executes(c -> con(c, () -> GrupoCtl.expulsar(c.getSource().getPlayerOrThrow(), EntityArgumentType.getPlayer(c, "jugador").getUuid())))));
        gr.then(CommandManager.literal("listo").executes(c -> con(c, () -> GrupoCtl.listo(c.getSource().getPlayerOrThrow(), 1)))
                .then(CommandManager.argument("hueco", IntegerArgumentType.integer(1, 6)).executes(c -> con(c, () -> GrupoCtl.listo(c.getSource().getPlayerOrThrow(), IntegerArgumentType.getInteger(c, "hueco"))))));
        gr.then(CommandManager.literal("noListo").executes(c -> con(c, () -> GrupoCtl.noListo(c.getSource().getPlayerOrThrow()))));
        gr.then(CommandManager.literal("empezar").executes(c -> con(c, () -> GrupoCtl.empezar(c.getSource().getPlayerOrThrow(), 1)))
                .then(CommandManager.argument("hueco", IntegerArgumentType.integer(1, 6)).executes(c -> con(c, () -> GrupoCtl.empezar(c.getSource().getPlayerOrThrow(), IntegerArgumentType.getInteger(c, "hueco"))))));
        gr.then(CommandManager.literal("info").executes(c -> { c.getSource().sendFeedback(() -> Msg.say(infoGrupos()), false); return 1; }));
        root.then(gr);
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
            var p = c.getSource().getPlayer();
            if (p != null && Partida.participa(p)) return con(c, () -> Partida.parar(p));
            if (Perm.admin(c.getSource())) { int n = Partida.pararTodas(); c.getSource().sendFeedback(() -> Msg.say(n == 0 ? "No hay ninguna partida en marcha." : "Canceladas " + n + " partidas."), false); return 1; }
            c.getSource().sendError(Msg.err("No estás en ninguna partida.")); return 0;
        }));
        // modo prueba: el jugador no pierde vida (para medir el rendimiento sin que la partida acabe)
        root.then(admin("inmortal").executes(c -> inmortal(c, !Partida.inmortal))
                .then(CommandManager.literal("on").executes(c -> inmortal(c, true)))
                .then(CommandManager.literal("off").executes(c -> inmortal(c, false))));
        // ayudas para probar (solo en tu propia partida): /survivor dev xp|nivel|tiempo <n>, jefe, elite, curar, arma <id> [nivel], pasiva <id> [nivel]
        var dev = admin("dev");
        for (String k : new String[]{"xp", "nivel", "tiempo", "dano"})
            dev.then(CommandManager.literal(k).then(CommandManager.argument("n", IntegerArgumentType.integer(0, 100000))
                    .executes(c -> con(c, () -> Partida.dev(c.getSource().getPlayerOrThrow(), k, "", IntegerArgumentType.getInteger(c, "n"))))));
        for (String k : new String[]{"jefe", "elite", "curar", "sinxp", "dps"})
            dev.then(CommandManager.literal(k).executes(c -> con(c, () -> Partida.dev(c.getSource().getPlayerOrThrow(), k, "", 1))));
        for (String k : new String[]{"arma", "solo", "pasiva", "objeto", "usar"})
            dev.then(CommandManager.literal(k).then(CommandManager.argument("id", StringArgumentType.word())
                    .executes(c -> con(c, () -> Partida.dev(c.getSource().getPlayerOrThrow(), k, StringArgumentType.getString(c, "id"), 1)))
                    .then(CommandManager.argument("nivel", IntegerArgumentType.integer(1, Catalogo.NIVEL_EVO))
                            .executes(c -> con(c, () -> Partida.dev(c.getSource().getPlayerOrThrow(), k, StringArgumentType.getString(c, "id"), IntegerArgumentType.getInteger(c, "nivel")))))));
        dev.then(CommandManager.literal("monedas").then(CommandManager.argument("n", IntegerArgumentType.integer(-100000, 100000))
                .executes(c -> con(c, () -> Partida.devMonedas(c.getSource().getPlayerOrThrow(), IntegerArgumentType.getInteger(c, "n"))))));
        root.then(dev);
        root.then(CommandManager.literal("estado").executes(c -> { c.getSource().sendFeedback(() -> Msg.say(Partida.estado()), false); return 1; }));
        root.executes(c -> { c.getSource().sendFeedback(() -> Msg.say("Emi Survivors: /survivor entrar (al mundo del minijuego), /survivor salir, /survivor menu, /survivor top, /survivor grupo crear|unirse|salir|listo|empezar. Administradores: /survivor prueba, medida, parar, inmortal, estado, dev ..."), false); return 1; });
        d.register(root);
    }

    private static void top(CommandContext<ServerCommandSource> c, int n) {
        if (Partida.datos == null) { c.getSource().sendError(Msg.err("El minijuego aun no esta listo.")); return; }
        mostrarTop(c, "Top en solitario (el tiempo aguantado, la mejor marca de cada jugador):", Partida.datos.top(n));
        mostrarTop(c, "Top de grupos (lo que ha aguantado todo el equipo):", Partida.datos.topGrupos(n));
    }

    private static void mostrarTop(CommandContext<ServerCommandSource> c, String titulo, java.util.List<Datos.Entrada> lista) {
        if (lista.isEmpty()) { c.getSource().sendFeedback(() -> Msg.say(titulo + " todavía no hay marcas."), false); return; }
        c.getSource().sendFeedback(() -> Msg.say(titulo), false);
        int pos = 1;
        for (Datos.Entrada e : lista) {
            int seg = e.segundos, i = pos++;
            String linea = i + ". " + e.nombre + " — " + (seg / 60) + ":" + String.format("%02d", seg % 60) + " (" + (Datos.GRUPO.equals(e.modo) ? e.miembros.size() + " jugadores" : e.especie + ", nivel " + e.nivel) + ", " + e.derrotados + " derrotados" + (e.jefe ? ", mató al jefe" : "") + ")";
            c.getSource().sendFeedback(() -> Text.literal(linea), false);
        }
    }

    private static String infoGrupos() {
        var todos = Partida.grupos.todos();
        if (todos.isEmpty()) return "No hay grupos.";
        StringBuilder sb = new StringBuilder();
        for (Grupos.Grupo g : todos) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append("Grupo de ").append(g.lider().nombre).append(g.enPartida ? " (jugando)" : "").append(": ");
            for (int i = 0; i < g.miembros.size(); i++) { Grupos.Miembro m = g.miembros.get(i); if (i > 0) sb.append(", "); sb.append(m.nombre).append(m.listo ? " ✓" : ""); }
        }
        return sb.toString();
    }

    private static LiteralArgumentBuilder<ServerCommandSource> admin(String nombre) {
        return CommandManager.literal(nombre).requires(Perm::admin);
    }

    private static int inmortal(CommandContext<ServerCommandSource> c, boolean on) {
        Partida.inmortal = on;
        c.getSource().sendFeedback(() -> Msg.say("Modo prueba inmortal " + (on ? "activado" : "desactivado") + " (solo para las partidas de administradores: no pierden vida y no cuentan para el top; los demás jugadores no se ven afectados)."), true);
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
