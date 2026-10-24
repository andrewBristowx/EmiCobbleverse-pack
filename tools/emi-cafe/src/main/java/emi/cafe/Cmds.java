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
                    if (!Cafes.tiene(p.getUuid()) && !Cafes.enCafe(p) && !Saved.exists(p.getServer(), p.getUuid())) return "No estás en un café.";
                    if (!Cafes.salir(p)) return "No encuentro lo que guardé de ti.";
                    return null;
                })))
                .then(CommandManager.literal("menu").executes(c -> con(c, p -> {
                    Cafe cf = Cafes.de(p.getUuid());
                    if (cf == null) return "Entra primero con /cafe entrar.";
                    Menus.principal(cf, p);
                    return null;
                })))
                .then(CommandManager.literal("invitar").then(CommandManager.argument("jugador", net.minecraft.command.argument.EntityArgumentType.player()).executes(c -> con(c, p -> {
                    Cafe cf = Cafes.de(p.getUuid());
                    if (cf == null) return "Entra primero a tu café con /cafe entrar.";
                    return Cafes.invitar(cf, p, net.minecraft.command.argument.EntityArgumentType.getPlayer(c, "jugador"));
                }))))
                .then(CommandManager.literal("aceptar").executes(c -> con(c, p -> Cafes.aceptar(p, null)))
                        .then(CommandManager.argument("duenio", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c -> con(c, p -> Cafes.aceptar(p, com.mojang.brigadier.arguments.StringArgumentType.getString(c, "duenio"))))))
                .then(CommandManager.literal("visitar").then(CommandManager.argument("jugador", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c -> con(c, p -> Cafes.visitar(p, com.mojang.brigadier.arguments.StringArgumentType.getString(c, "jugador"))))))
                .then(CommandManager.literal("visitas").then(CommandManager.argument("si", com.mojang.brigadier.arguments.BoolArgumentType.bool()).executes(c -> con(c, p -> {
                    Datos.Jugador j = EmiCafe.datos.jugador(p.getUuid(), p.getGameProfile().getName());
                    j.visitas = com.mojang.brigadier.arguments.BoolArgumentType.getBool(c, "si");
                    EmiCafe.datos.guardar(p.getUuid());
                    p.sendMessage(Msg.say(j.visitas ? "Tu café acepta visitas." : "Tu café ya no acepta visitas."), false);
                    return null;
                }))))
                .then(CommandManager.literal("info").executes(c -> con(c, p -> {
                    Cafe cf = Cafes.de(p.getUuid());
                    if (cf == null) return "No estás en tu café: entra con /cafe entrar.";
                    Datos.Jugador j = EmiCafe.datos.jugador(p.getUuid(), p.getGameProfile().getName());
                    p.sendMessage(Msg.say("Monedas: " + j.monedas + " · clientes servidos: " + j.clientes + " · turnos: " + j.turnos + " · mejor turno: " + j.mejorTurno + " · estrellas: " + j.estrellas), false);
                    return null;
                })))
                .then(CommandManager.literal("dev").requires(Perm::admin)
                        .then(CommandManager.literal("cliente").executes(c -> con(c, p -> {
                            Cafe cf = Cafes.de(p.getUuid());
                            if (cf == null) return "Entra primero con /cafe entrar.";
                            return cf.forzarCliente() ? null : "No hay mesa libre.";
                        })))
                        .then(CommandManager.literal("visitante").then(CommandManager.argument("id", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c -> con(c, p -> {
                            Cafe cf = Cafes.de(p.getUuid());
                            if (cf == null) return "Entra primero con /cafe entrar.";
                            Especial e = Especial.de(com.mojang.brigadier.arguments.StringArgumentType.getString(c, "id"));
                            if (e == null) { StringBuilder b = new StringBuilder(); for (Especial x : Especial.values()) b.append(x.id).append(' '); return "Uno de: " + b; }
                            for (Mesa m : cf.mesas) if (m.libre()) return cf.forzarEspecial(m, e) ? null : "No se pudo crear.";
                            return "No hay mesa libre.";
                        }))))
                        .then(CommandManager.literal("especie").then(CommandManager.argument("id", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c -> con(c, p -> {
                            String id = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "id");
                            Cliente.forzar = id.equals("azar") ? null : id;
                            return null;
                        }))))
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
                        .then(CommandManager.literal("evento").then(CommandManager.argument("id", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c -> con(c, p -> {
                            Evento e = Evento.de(com.mojang.brigadier.arguments.StringArgumentType.getString(c, "id"));
                            if (e == null) return "ninguno, hora_punta, plato_del_dia, dia_michialien o critico";
                            Cafe cf = Cafes.de(p.getUuid());
                            if (cf == null) return "Entra primero con /cafe entrar.";
                            if (cf.estado == Cafe.Estado.ABIERTO) cf.aplicarEvento(e); else { Cafe.eventoForzado = e; p.sendMessage(Msg.say("El evento saldrá la próxima vez que abras."), false); }
                            return null;
                        }))))
                        .then(CommandManager.literal("rep").then(CommandManager.argument("n", IntegerArgumentType.integer(-100000, 100000)).executes(c -> con(c, p -> {
                            Cafe cf = Cafes.de(p.getUuid());
                            if (cf == null) return "Entra primero con /cafe entrar.";
                            cf.ganarRep(IntegerArgumentType.getInteger(c, "n"));
                            p.sendMessage(Msg.say("Reputación: " + cf.dj().rep + " (nivel " + cf.nivelRep() + "); estilo: " + cf.puntosEstilo() + " puntos (nivel " + cf.nivelEstilo() + ")"), false);
                            return null;
                        }))))
                        .then(CommandManager.literal("critico").executes(c -> con(c, p -> {
                            Cafe cf = Cafes.de(p.getUuid());
                            if (cf == null) return "Entra primero con /cafe entrar.";
                            return cf.forzarCritico() ? null : "No hay mesa libre.";
                        })))
                        .then(CommandManager.literal("estrellas").then(CommandManager.argument("n", IntegerArgumentType.integer(-100000, 100000)).executes(c -> con(c, p -> {
                            Datos.Jugador j = EmiCafe.datos.jugador(p.getUuid(), p.getGameProfile().getName());
                            j.estrellas = Math.max(0, j.estrellas + IntegerArgumentType.getInteger(c, "n"));
                            EmiCafe.datos.guardar(p.getUuid());
                            p.sendMessage(Msg.say("Ahora tienes " + j.estrellas + " estrellas."), false);
                            return null;
                        }))))
                        .then(CommandManager.literal("turno").executes(c -> con(c, p -> {
                            Cafe cf = Cafes.de(p.getUuid());
                            if (cf == null) return "Entra primero con /cafe entrar.";
                            if (cf.estado == Cafe.Estado.ABIERTO) cf.cerrarTurno();
                            else return cf.abrirTurno();
                            return null;
                        })))
                        .then(CommandManager.literal("inicial").then(CommandManager.argument("id", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c -> con(c, p -> {
                            Recetas.Inicial ini = Recetas.Inicial.de(com.mojang.brigadier.arguments.StringArgumentType.getString(c, "id"));
                            if (ini == null) return "charmander, squirtle o bulbasaur";
                            Datos.Jugador j = EmiCafe.datos.jugador(p.getUuid(), p.getGameProfile().getName());
                            j.inicial = ini.id; j.platos.clear(); j.platos.add(ini.plato.id);
                            EmiCafe.datos.guardar(p.getUuid());
                            Cafe cf = Cafes.de(p.getUuid());
                            if (cf != null) cf.reconstruir();
                            return null;
                        }))))
                        .then(CommandManager.literal("platos").executes(c -> con(c, p -> {            // aprende todos los platos (para probar la cocina entera)
                            Datos.Jugador j = EmiCafe.datos.jugador(p.getUuid(), p.getGameProfile().getName());
                            for (Recetas.Plato pl : Recetas.Plato.values()) if (!j.platos.contains(pl.id)) j.platos.add(pl.id);
                            EmiCafe.datos.guardar(p.getUuid());
                            Cafe cf = Cafes.de(p.getUuid());
                            if (cf != null) cf.reconstruir();
                            return null;
                        })))
                        .then(CommandManager.literal("mejora").then(CommandManager.argument("id", com.mojang.brigadier.arguments.StringArgumentType.word()).then(CommandManager.argument("nivel", IntegerArgumentType.integer(0, 9)).executes(c -> con(c, p -> {
                            Recetas.Mejora m = Recetas.Mejora.de(com.mojang.brigadier.arguments.StringArgumentType.getString(c, "id"));
                            if (m == null) return "mesas, velocidad, paciencia, vajilla, ampliar, publicidad, ayud_lava, ayud_mesero";
                            Datos.Jugador j = EmiCafe.datos.jugador(p.getUuid(), p.getGameProfile().getName());
                            j.mejoras.put(m.id, Math.min(m.max(), IntegerArgumentType.getInteger(c, "nivel")));
                            EmiCafe.datos.guardar(p.getUuid());
                            Cafe cf = Cafes.de(p.getUuid());
                            if (cf != null) cf.reconstruir();
                            return null;
                        })))))
                        .then(CommandManager.literal("reiniciar").executes(c -> con(c, p -> {
                            Datos.Jugador j = EmiCafe.datos.jugador(p.getUuid(), p.getGameProfile().getName());
                            j.inicial = null; j.platos.clear(); j.mejoras.clear(); j.monedas = 0;
                            EmiCafe.datos.guardar(p.getUuid());
                            Cafe cf = Cafes.de(p.getUuid());
                            if (cf != null) cf.reconstruir();
                            return null;
                        })))
                        .then(CommandManager.literal("local").executes(c -> con(c, p -> {
                            Cafe cf = Cafes.de(p.getUuid());
                            if (cf == null) return "Entra primero con /cafe entrar.";
                            p.sendMessage(Msg.say("Local nº " + cf.slot + ", origen x=" + cf.ox + ", clientes=" + cf.clientes.size()), false);
                            return null;
                        }))))
        );
    }

    private interface Accion { String hacer(ServerPlayerEntity p) throws Exception; }

    private static int con(com.mojang.brigadier.context.CommandContext<ServerCommandSource> c, Accion a) {
        ServerPlayerEntity p = c.getSource().getPlayer();
        if (p == null) { c.getSource().sendError(Text.literal("Solo para jugadores.")); return 0; }
        String err;
        try { err = a.hacer(p); } catch (com.mojang.brigadier.exceptions.CommandSyntaxException x) { err = "No encuentro a ese jugador (tiene que estar conectado)."; } catch (Throwable t) { EmiCafe.LOG.error("Error en un comando", t); err = "Error interno (mira el registro del servidor)."; }
        if (err != null) { p.sendMessage(Msg.err(err), false); return 0; }
        return 1;
    }
}
