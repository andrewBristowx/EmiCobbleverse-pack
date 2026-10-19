package emi.fall;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Los comandos del torneo: {@code /fall entrar|unirse|empezar|salir|estado|parar}. */
final class Cmds {
    private Cmds() {}

    static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(CommandManager.literal("fall")
                .then(CommandManager.literal("entrar").executes(c -> con(c, p -> Sala.entrar(p))))
                .then(CommandManager.literal("unirse").executes(c -> con(c, p -> Torneo.unirse(p))))
                .then(CommandManager.literal("empezar")
                        .executes(c -> con(c, p -> Torneo.empezar(p, Perm.admin(c.getSource()), "")))
                        .then(CommandManager.argument("juegos", StringArgumentType.greedyString()).executes(c -> con(c, p -> Torneo.empezar(p, Perm.admin(c.getSource()), StringArgumentType.getString(c, "juegos"))))))
                .then(CommandManager.literal("salir").executes(c -> con(c, p -> {
                    if (Torneo.apuntado(p.getUuid())) Torneo.dejar(p);
                    if (!Sala.enSala(p) && !Torneo.participa(p) && !net.minecraft.world.World.OVERWORLD.equals(p.getServerWorld().getRegistryKey()) && !Saved.exists(p.getServer(), p.getUuid())) return "No estas en el torneo.";
                    if (!Sala.salir(p.getServer(), p)) return Sala.enSala(p) ? "No encuentro lo que guardé de ti." : "No estas en la sala del torneo.";
                    return null;
                })))
                .then(CommandManager.literal("estado").executes(c -> estado(c.getSource())))
                .then(CommandManager.literal("parar").requires(Perm::admin).executes(c -> { Torneo.parar(); c.getSource().sendFeedback(() -> Msg.say("Torneo cancelado."), false); return 1; }))
        );
    }

    private interface Accion { String hacer(ServerPlayerEntity p); }

    private static int con(com.mojang.brigadier.context.CommandContext<ServerCommandSource> c, Accion a) {
        ServerPlayerEntity p = c.getSource().getPlayer();
        if (p == null) { c.getSource().sendError(Text.literal("Solo para jugadores.")); return 0; }
        String err;
        try { err = a.hacer(p); } catch (Throwable t) { EmiFall.LOG.error("Error en un comando", t); err = "Error interno (mira el registro del servidor)."; }
        if (err != null) { p.sendMessage(Msg.err(err), false); return 0; }
        return 1;
    }

    private static int estado(ServerCommandSource src) {
        Torneo q = Torneo.actual;
        if (q == null) { src.sendFeedback(() -> Msg.say("No hay torneo. Entra con /fall entrar y apúntate con /fall unirse."), false); return 1; }
        StringBuilder sb = new StringBuilder("Torneo (" + q.fase + "), " + q.jugs.size() + " jugadores:");
        for (Jug j : q.jugs.values()) sb.append(' ').append(j.nombre).append('=').append(j.puntos);
        String s = sb.toString();
        src.sendFeedback(() -> Msg.say(Text.literal(s).formatted(Formatting.WHITE)), false);
        return 1;
    }
}
