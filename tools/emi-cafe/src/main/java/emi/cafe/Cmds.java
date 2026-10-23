package emi.cafe;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** Los comandos del cafe: {@code /cafe entrar|salir|info}, y {@code /cafe dev ...} para el organizador. */
final class Cmds {
    private Cmds() {}

    static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(CommandManager.literal("cafe")
                .then(CommandManager.literal("entrar").executes(c -> con(c, Cafes::entrar)))
                .then(CommandManager.literal("salir").executes(c -> con(c, p -> {
                    if (!Cafes.tiene(p.getUuid()) && !Cafes.enCafe(p) && !Saved.exists(p.getServer(), p.getUuid())) return "No estás en tu café.";
                    if (!Cafes.salir(p)) return "No encuentro lo que guardé de ti.";
                    return null;
                })))
                .then(CommandManager.literal("info").executes(c -> con(c, p -> {
                    Cafe cf = Cafes.de(p.getUuid());
                    if (cf == null) return "No estás en tu café: entra con /cafe entrar.";
                    Datos.Jugador j = EmiCafe.datos.jugador(p.getUuid(), p.getGameProfile().getName());
                    p.sendMessage(Msg.say("Monedas: " + j.monedas + " · clientes servidos: " + j.clientes + " · ganado esta visita: " + cf.ganado), false);
                    return null;
                })))
                .then(CommandManager.literal("dev").requires(Perm::admin)
                        .then(CommandManager.literal("cliente").executes(c -> con(c, p -> {
                            Cafe cf = Cafes.de(p.getUuid());
                            if (cf == null) return "Entra primero con /cafe entrar.";
                            return cf.forzarCliente() ? null : "No hay mesa libre.";
                        })))
                        .then(CommandManager.literal("monedas").then(CommandManager.argument("n", IntegerArgumentType.integer(-100000, 100000)).executes(c -> con(c, p -> {
                            Datos.Jugador j = EmiCafe.datos.jugador(p.getUuid(), p.getGameProfile().getName());
                            j.monedas = Math.max(0, j.monedas + IntegerArgumentType.getInteger(c, "n"));
                            EmiCafe.datos.guardar(p.getUuid());
                            p.sendMessage(Msg.say("Ahora tienes " + j.monedas + " monedas."), false);
                            return null;
                        }))))
                        // simula el clic derecho en un bloque (para probar sin ratón: el banco de pruebas solo sabe hacer clic izquierdo)
                        .then(CommandManager.literal("usar").then(CommandManager.argument("pos", BlockPosArgumentType.blockPos()).executes(c -> con(c, p -> {
                            Cafe cf = Cafes.de(p.getUuid());
                            if (cf == null) return "Entra primero con /cafe entrar.";
                            BlockPos pos = BlockPosArgumentType.getBlockPos(c, "pos");
                            cf.usarBloque(p, pos);
                            return null;
                        }))))
                        .then(CommandManager.literal("local").executes(c -> con(c, p -> {
                            Cafe cf = Cafes.de(p.getUuid());
                            if (cf == null) return "Entra primero con /cafe entrar.";
                            p.sendMessage(Msg.say("Local nº " + cf.slot + ", origen x=" + cf.ox + ", clientes=" + cf.clientes.size()), false);
                            return null;
                        }))))
        );
    }

    private interface Accion { String hacer(ServerPlayerEntity p); }

    private static int con(com.mojang.brigadier.context.CommandContext<ServerCommandSource> c, Accion a) {
        ServerPlayerEntity p = c.getSource().getPlayer();
        if (p == null) { c.getSource().sendError(Text.literal("Solo para jugadores.")); return 0; }
        String err;
        try { err = a.hacer(p); } catch (Throwable t) { EmiCafe.LOG.error("Error en un comando", t); err = "Error interno (mira el registro del servidor)."; }
        if (err != null) { p.sendMessage(Msg.err(err), false); return 0; }
        return 1;
    }
}
