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
        // /huida abrir [nivel]: tyrantrum (por defecto) o calabaza (Halloween, Pumpkaboo)
        root.then(admin("abrir").executes(c -> con(c, Huida::abrir, "Cola abierta: se ha avisado a todos."))
                .then(CommandManager.argument("nivel", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests((c, b) -> net.minecraft.command.CommandSource.suggestMatching(java.util.List.of("tyrantrum", "calabaza"), b))
                        .executes(c -> {
                            Niveles.Def niv = Niveles.porNombre(com.mojang.brigadier.arguments.StringArgumentType.getString(c, "nivel"));
                            if (niv == null) { c.getSource().sendError(Text.literal("Niveles: tyrantrum, calabaza.")); return 0; }
                            return con(c, () -> Huida.abrir(niv), "Cola abierta (" + niv.titulo() + "): se ha avisado a todos.");
                        })));
        root.then(admin("cerrar").executes(c -> con(c, Huida::cerrarCola)));
        // /huida empezar = lleva a todos a su pista y espera a que confirmen «listo»; /huida empezar ya = sin confirmacion
        root.then(admin("empezar").executes(c -> con(c, () -> Huida.empezar(false), "Preparando la huida: se espera a que todos confirmen «listo» (/huida saltar para no esperar)..."))
                .then(CommandManager.literal("ya").executes(c -> con(c, () -> Huida.empezar(true), "Preparando la huida (sin confirmación)..."))));
        root.then(admin("saltar").executes(c -> con(c, Huida::saltar, "Confirmación saltada.")));
        root.then(admin("forzar").executes(c -> con(c, Huida::saltar, "Confirmación saltada.")));
        root.then(admin("prueba").then(CommandManager.argument("escena", com.mojang.brigadier.arguments.StringArgumentType.word())
                .executes(c -> con(c, () -> Huida.prueba(c.getSource().getPlayerOrThrow(), com.mojang.brigadier.arguments.StringArgumentType.getString(c, "escena")), "Preparando la prueba..."))
                .then(CommandManager.argument("nivel", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(c -> {
                            Niveles.Def niv = Niveles.porNombre(com.mojang.brigadier.arguments.StringArgumentType.getString(c, "nivel"));
                            if (niv == null) { c.getSource().sendError(Text.literal("Niveles: tyrantrum, calabaza.")); return 0; }
                            return con(c, () -> Huida.prueba(c.getSource().getPlayerOrThrow(), com.mojang.brigadier.arguments.StringArgumentType.getString(c, "escena"), niv), "Preparando la prueba...");
                        }))));
        // /huida ir <s>: lleva al organizador (que esta corriendo) a esa distancia del recorrido, para probar un obstaculo sin correr todo el nivel
        root.then(admin("ir").then(CommandManager.argument("s", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0, 5000))
                .executes(c -> con(c, () -> Huida.ir(c.getSource().getPlayerOrThrow(), com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(c, "s")), "Teletransportado."))));
        // modo prueba: el Tyrantrum no te atrapa (para probar el final con un equipo que va con lag); sin argumento se alterna
        root.then(admin("modoprueba").executes(c -> modoPrueba(c, !Huida.modoPrueba))
                .then(CommandManager.literal("on").executes(c -> modoPrueba(c, true)))
                .then(CommandManager.literal("off").executes(c -> modoPrueba(c, false))));
        // al terminar, los jugadores esperan en la meta: el organizador cierra con /huida terminar (clasificacion y vuelta a su sitio)
        root.then(admin("terminar").executes(c -> con(c, Huida::cerrar, "Huida cerrada: todos vuelven a su sitio.")));
        root.then(admin("fin").executes(c -> con(c, Huida::cerrar, "Huida cerrada: todos vuelven a su sitio.")));
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

    private static int modoPrueba(CommandContext<ServerCommandSource> c, boolean on) {
        Huida.modoPrueba = on;
        EmiHuida.LOG.info("Huida: modo prueba {} por {}", on ? "ACTIVADO" : "desactivado", c.getSource().getName());
        c.getSource().sendFeedback(() -> Msg.say(on
                ? "MODO PRUEBA ACTIVADO: el Tyrantrum ya no te atrapa (se queda a unos 9 bloques) y las carreras no cuentan en la clasificación. Se apaga con /huida modoprueba off (y al reiniciar el servidor)."
                : "Modo prueba desactivado: el Tyrantrum vuelve a atraparte."), true);
        return 1;
    }

    private static int ayuda(CommandContext<ServerCommandSource> c) {
        c.getSource().sendFeedback(() -> Msg.say("La huida: corre por las ruinas (o por el cementerio de Halloween), salta, agáchate (shift) y esquiva antes de que te alcance el Tyrantrum (o el Pumpkaboo)."), false);
        c.getSource().sendFeedback(() -> Msg.say("Jugadores: /huida unirse · /huida listo (o tecla R) · /huida salir · /huida estado"), false);
        if (Perm.admin(c.getSource())) c.getSource().sendFeedback(() -> Msg.say("Organizador: /huida abrir [tyrantrum|calabaza] · /huida empezar [ya] · /huida saltar (salta la confirmación) · /huida terminar (cierra al acabar todos) · /huida parar · /huida cerrar · /huida modoprueba [on|off] (el Tyrantrum no te atrapa, para probar)"), false);
        return 1;
    }
}
