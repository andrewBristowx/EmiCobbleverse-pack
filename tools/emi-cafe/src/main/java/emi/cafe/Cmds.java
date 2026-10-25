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
                .then(CommandManager.literal("nombre").then(CommandManager.argument("texto", com.mojang.brigadier.arguments.StringArgumentType.greedyString()).executes(c -> con(c, p -> {
                    Cafe cf = Cafes.de(p.getUuid());
                    if (cf == null || !cf.esDuenio(p)) return "Entra primero a tu café con /cafe entrar.";
                    return cf.ponerNombreCafe(p, com.mojang.brigadier.arguments.StringArgumentType.getString(c, "texto"));
                }))))
                .then(CommandManager.literal("deshacer").executes(c -> con(c, p -> {
                    Cafe cf = Cafes.de(p.getUuid());
                    if (cf == null || !cf.esDuenio(p)) return "Entra primero a tu café con /cafe entrar.";
                    String err = cf.obras.deshacer(p);
                    if (err == null) p.sendMessage(Msg.say("Deshecho."), false);
                    return err;
                })))
                .then(CommandManager.literal("baul").executes(c -> con(c, p -> buscarBaul(p, ""))).then(CommandManager.argument("texto", com.mojang.brigadier.arguments.StringArgumentType.greedyString()).executes(c -> con(c, p -> buscarBaul(p, com.mojang.brigadier.arguments.StringArgumentType.getString(c, "texto"))))))
                .then(CommandManager.literal("plano")
                        .then(CommandManager.literal("guardar").then(CommandManager.argument("nombre", com.mojang.brigadier.arguments.StringArgumentType.greedyString()).executes(c -> con(c, p -> {
                            Cafe cf = Cafes.de(p.getUuid());
                            if (cf == null || !cf.esDuenio(p)) return "Entra primero a tu café con /cafe entrar.";
                            String err = cf.obras.guardarPlano(p, com.mojang.brigadier.arguments.StringArgumentType.getString(c, "nombre"));
                            if (err == null) { p.sendMessage(Msg.say("Plano guardado."), false); Menus.abrir(cf, p, "planos", false); }
                            return err;
                        }))))
                        .then(CommandManager.literal("restaurar").then(CommandManager.argument("nombre", com.mojang.brigadier.arguments.StringArgumentType.greedyString()).executes(c -> con(c, p -> {
                            Cafe cf = Cafes.de(p.getUuid());
                            if (cf == null || !cf.esDuenio(p)) return "Entra primero a tu café con /cafe entrar.";
                            String err = cf.obras.restaurarPlano(p, com.mojang.brigadier.arguments.StringArgumentType.getString(c, "nombre"));
                            if (err == null) p.sendMessage(Msg.say("Plano restaurado."), false);
                            return err;
                        }))))
                        .then(CommandManager.literal("borrar").then(CommandManager.argument("nombre", com.mojang.brigadier.arguments.StringArgumentType.greedyString()).executes(c -> con(c, p -> {
                            Cafe cf = Cafes.de(p.getUuid());
                            if (cf == null || !cf.esDuenio(p)) return "Entra primero a tu café con /cafe entrar.";
                            return cf.obras.borrarPlano(p, com.mojang.brigadier.arguments.StringArgumentType.getString(c, "nombre"));
                        })))))
                .then(CommandManager.literal("buscar").executes(c -> con(c, p -> buscar(p, ""))).then(CommandManager.argument("texto", com.mojang.brigadier.arguments.StringArgumentType.greedyString()).executes(c -> con(c, p -> buscar(p, com.mojang.brigadier.arguments.StringArgumentType.getString(c, "texto"))))))
                .then(CommandManager.literal("personal").then(CommandManager.literal("nombre").then(CommandManager.argument("numero", IntegerArgumentType.integer(1, 20)).then(CommandManager.argument("texto", com.mojang.brigadier.arguments.StringArgumentType.greedyString()).executes(c -> con(c, p -> {
                    Cafe cf = Cafes.de(p.getUuid());
                    if (cf == null || !cf.esDuenio(p)) return "Entra primero a tu café con /cafe entrar.";
                    return cf.renombrar(p, IntegerArgumentType.getInteger(c, "numero"), com.mojang.brigadier.arguments.StringArgumentType.getString(c, "texto"));
                }))))))
                .then(CommandManager.literal("granja").then(CommandManager.literal("nombre").then(CommandManager.argument("numero", IntegerArgumentType.integer(1, 6)).then(CommandManager.argument("texto", com.mojang.brigadier.arguments.StringArgumentType.greedyString()).executes(c -> con(c, p -> {
                    Cafe cf = Cafes.de(p.getUuid());
                    if (cf == null || !cf.esDuenio(p)) return "Entra primero a tu café con /cafe entrar.";
                    return cf.granja.renombrar(p, IntegerArgumentType.getInteger(c, "numero") - 1, com.mojang.brigadier.arguments.StringArgumentType.getString(c, "texto"));
                }))))))
                .then(CommandManager.literal("ir").then(CommandManager.argument("jugador", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c -> con(c, p -> Cafes.irA(p, com.mojang.brigadier.arguments.StringArgumentType.getString(c, "jugador"))))))
                .then(CommandManager.literal("megusta").then(CommandManager.argument("jugador", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c -> con(c, p -> {
                    Cafe otro = Cafes.deDuenio(com.mojang.brigadier.arguments.StringArgumentType.getString(c, "jugador"));
                    if (otro == null) return "Solo se da «me gusta» a un café cuyo dueño está dentro de él.";
                    return Cafes.megusta(otro, p);
                }))))
                .then(CommandManager.literal("libro").executes(c -> con(c, p -> {
                    Cafe cf = Cafes.de(p.getUuid());
                    if (cf == null) return "Entra primero con /cafe entrar.";
                    Menus.libro(cf, p);
                    return null;
                })))
                .then(CommandManager.literal("visitas").then(CommandManager.argument("si", com.mojang.brigadier.arguments.BoolArgumentType.bool()).executes(c -> con(c, p -> {
                    Datos.Jugador j = EmiCafe.datos.jugador(p.getUuid(), p.getGameProfile().getName());
                    j.visitas = com.mojang.brigadier.arguments.BoolArgumentType.getBool(c, "si");
                    EmiCafe.datos.guardar(p.getUuid());
                    p.sendMessage(Msg.say(j.visitas ? "Tu café acepta visitas." : "Tu café ya no acepta visitas."), false);
                    return null;
                }))))
                .then(CommandManager.literal("tutorial").executes(c -> con(c, p -> {
                    Cafe cf = Cafes.de(p.getUuid());
                    if (cf == null || !cf.esDuenio(p)) return "Entra primero a tu café con /cafe entrar.";
                    if (EmiCafe.datos.jugador(p.getUuid(), p.getGameProfile().getName()).inicial == null) return "Primero elige a tu Pokémon cocinero.";
                    cf.iniciarTutorial(true);
                    return null;
                })).then(CommandManager.literal("saltar").executes(c -> con(c, p -> {
                    Cafe cf = Cafes.de(p.getUuid());
                    if (cf == null || !cf.esDuenio(p)) return "Entra primero a tu café con /cafe entrar.";
                    cf.quitarTutorial(true);
                    p.sendMessage(Msg.say("Guía quitada. Con /cafe tutorial la repites."), false);
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
                        .then(CommandManager.literal("vecino").then(CommandManager.argument("nombre", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c -> con(c, p -> {
                            String nombre = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "nombre");
                            java.util.UUID id = java.util.UUID.nameUUIDFromBytes(("vecino:" + nombre).getBytes());
                            net.minecraft.server.world.ServerWorld mundo = Cafes.mundo(p.getServer());
                            if (mundo == null) return "No existe la dimensión del café.";
                            EmiCafe.datos.jugador(id, nombre);
                            Cafe cf = new Cafe(p.getServer(), mundo, id, Ciudad.slotDe(id, nombre));
                            cf.reconstruir(); cf.cerrar();
                            p.sendMessage(Msg.say("Vecino de prueba «" + nombre + "» en la parcela " + cf.slot + " (x=" + cf.ox + "). Ir: /cafe ir " + nombre), false);
                            return null;
                        }))))
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
                        .then(CommandManager.literal("granja")
                                .then(CommandManager.literal("comprar").then(CommandManager.argument("tipo", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c -> con(c, p -> {
                                    Cafe cf = Cafes.de(p.getUuid());
                                    if (cf == null) return "Entra primero con /cafe entrar.";
                                    return cf.granja.comprar(p, com.mojang.brigadier.arguments.StringArgumentType.getString(c, "tipo"));
                                }))))
                                .then(CommandManager.literal("dar").then(CommandManager.argument("tipo", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c -> con(c, p -> {
                                    Cafe cf = Cafes.de(p.getUuid());
                                    if (cf == null) return "Entra primero con /cafe entrar.";
                                    return cf.granja.darGratis(p, com.mojang.brigadier.arguments.StringArgumentType.getString(c, "tipo"));
                                }))))
                                .then(CommandManager.literal("mejorar").then(CommandManager.argument("slot", IntegerArgumentType.integer(0, 5)).executes(c -> con(c, p -> {
                                    Cafe cf = Cafes.de(p.getUuid());
                                    if (cf == null) return "Entra primero con /cafe entrar.";
                                    return cf.granja.mejorar(p, IntegerArgumentType.getInteger(c, "slot"));
                                }))))
                                .then(CommandManager.literal("listo").executes(c -> con(c, p -> {
                                    Cafe cf = Cafes.de(p.getUuid());
                                    if (cf == null) return "Entra primero con /cafe entrar.";
                                    cf.granja.listaYa(null);
                                    return null;
                                })))
                                .then(CommandManager.literal("probar").then(CommandManager.argument("plato", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c -> con(c, p -> {
                                    Cafe cf = Cafes.de(p.getUuid());
                                    if (cf == null) return "Entra primero con /cafe entrar.";
                                    Recetas.Plato pl = Recetas.Plato.de(com.mojang.brigadier.arguments.StringArgumentType.getString(c, "plato"));
                                    if (pl == null) return "Un id de plato.";
                                    Granja.Uso u = Granja.usar(cf.dj(), pl);
                                    p.sendMessage(Msg.say(pl.nombre() + ": extra " + Math.round(u.extra() * 1000) / 10.0 + " % (" + u.usados() + " de " + u.total() + " ingredientes de la granja, mejor calidad " + u.mejor() + ")"), false);
                                    return null;
                                }))))
                                .then(CommandManager.literal("estado").executes(c -> con(c, p -> {
                                    Cafe cf = Cafes.de(p.getUuid());
                                    if (cf == null) return "Entra primero con /cafe entrar.";
                                    Datos.Jugador dd = cf.dj();
                                    StringBuilder b = new StringBuilder(cf.granja.estado()).append(" · despensa:");
                                    for (java.util.Map.Entry<String, Integer> en : dd.despensa.entrySet()) b.append(' ').append(en.getKey().replace("emi_cafe:", "")).append('=').append(en.getValue());
                                    b.append(" · extra ganado ").append(dd.granjaBonus);
                                    p.sendMessage(Msg.say(b.toString()), false);
                                    return null;
                                }))))
                        .then(CommandManager.literal("pedido").then(CommandManager.argument("id", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c -> con(c, p -> {
                            String id = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "id");
                            Recetas.Plato pl = Recetas.Plato.de(id);
                            if (!id.equals("azar") && pl == null) return "Un id de plato (limonada, papas_fritas...) o «azar».";
                            Cafe.pedidoForzado = pl == null ? null : pl.item;
                            return null;
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
                        .then(CommandManager.literal("construir").executes(c -> con(c, p -> {                // solo pruebas: entra o sale del modo construccion
                            Cafe cf = Cafes.de(p.getUuid());
                            if (cf == null) return "Entra primero con /cafe entrar.";
                            if (cf.construyendo) { cf.salirConstruccion(p); return null; }
                            return cf.entrarConstruccion(p);
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
                            if (m == null) return "mesas, velocidad, paciencia, vajilla, ampliar, publicidad, propinas";
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
                            j.libre = true; j.kitDado = false; j.almacen.clear(); if (j.baul != null) j.baul.clear(); j.decor.clear(); j.quitados.clear(); j.estPos.clear(); j.mesasPos.clear(); j.mesasCap = 0; j.tutorialHecho = false;      // desde 0.18.0: parcela vacia otra vez
                            EmiCafe.datos.guardar(p.getUuid());
                            Cafe cf = Cafes.de(p.getUuid());
                            if (cf != null) cf.reconstruir();
                            return null;
                        })))
                        .then(CommandManager.literal("almacen").then(CommandManager.argument("item", com.mojang.brigadier.arguments.StringArgumentType.string()).then(CommandManager.argument("n", IntegerArgumentType.integer(-1000, 1000)).executes(c -> con(c, p -> {
                            Cafe cf = Cafes.de(p.getUuid());
                            if (cf == null) return "Entra primero con /cafe entrar.";
                            cf.obras.sumarStock(com.mojang.brigadier.arguments.StringArgumentType.getString(c, "item"), IntegerArgumentType.getInteger(c, "n"));
                            EmiCafe.datos.guardar(p.getUuid());
                            if (cf.construyendo) cf.obras.llenarInventario(p);
                            return null;
                        })))))
                        .then(CommandManager.literal("catalogo").executes(c -> {                                // solo pruebas: escribe en el registro cuantos articulos tiene cada fuente y grupo de la tienda DECORAR
                            StringBuilder sb = new StringBuilder("Catálogo de DECORAR:");
                            for (Decor.Fuente f : Decor.fuentes()) {
                                int n = 0; StringBuilder gs = new StringBuilder();
                                for (Decor.Grupo g : f.grupos()) { n += g.arts().size(); gs.append(" [").append(g.nombre()).append(' ').append(g.arts().size()).append(']'); }
                                sb.append("\n  ").append(f.nombre()).append(" = ").append(n).append(':').append(gs);
                            }
                            EmiCafe.LOG.info(sb.toString());
                            c.getSource().sendFeedback(() -> Text.literal("Catálogo escrito en el registro."), false);
                            return 1;
                        }))
                        .then(CommandManager.literal("libre").then(CommandManager.argument("v", com.mojang.brigadier.arguments.BoolArgumentType.bool()).executes(c -> con(c, p -> {      // solo pruebas: pasa tu cafe a parcela libre o al de antes
                            Datos.Jugador j = EmiCafe.datos.jugador(p.getUuid(), p.getGameProfile().getName());
                            j.libre = com.mojang.brigadier.arguments.BoolArgumentType.getBool(c, "v");
                            EmiCafe.datos.guardar(p.getUuid());
                            Cafe cf = Cafes.de(p.getUuid());
                            if (cf != null) cf.reconstruir();
                            return null;
                        }))))
                        .then(CommandManager.literal("poner").then(CommandManager.argument("x", IntegerArgumentType.integer(-10, 70)).then(CommandManager.argument("y", IntegerArgumentType.integer(0, 9)).then(CommandManager.argument("z", IntegerArgumentType.integer(-12, 16)).then(CommandManager.argument("bloque", com.mojang.brigadier.arguments.StringArgumentType.string()).executes(c -> ponerDev(c, null)).then(CommandManager.argument("dir", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c -> ponerDev(c, com.mojang.brigadier.arguments.StringArgumentType.getString(c, "dir")))))))))
                        .then(CommandManager.literal("local").executes(c -> con(c, p -> {
                            Cafe cf = Cafes.de(p.getUuid());
                            if (cf == null) return "Entra primero con /cafe entrar.";
                            p.sendMessage(Msg.say("Local nº " + cf.slot + ", origen x=" + cf.ox + ", clientes=" + cf.clientes.size()), false);
                            return null;
                        }))))
        );
    }

    private interface Accion { String hacer(ServerPlayerEntity p) throws Exception; }

    /** Solo pruebas: /cafe dev poner x y z bloque [norte|sur|este|oeste] (coordenadas del local; y = 1 es sobre el suelo). */
    private static int ponerDev(com.mojang.brigadier.context.CommandContext<net.minecraft.server.command.ServerCommandSource> c, String dir) {
        return con(c, p -> {
            Cafe cf = Cafes.de(p.getUuid());
            if (cf == null) return "Entra primero con /cafe entrar.";
            net.minecraft.block.Block b = net.minecraft.registry.Registries.BLOCK.get(net.minecraft.util.Identifier.of(com.mojang.brigadier.arguments.StringArgumentType.getString(c, "bloque")));
            net.minecraft.block.BlockState st = b.getDefaultState();
            if (dir != null && st.contains(net.minecraft.block.HorizontalFacingBlock.FACING))
                st = st.with(net.minecraft.block.HorizontalFacingBlock.FACING, switch (dir) { case "norte" -> net.minecraft.util.math.Direction.NORTH; case "sur" -> net.minecraft.util.math.Direction.SOUTH; case "oeste" -> net.minecraft.util.math.Direction.WEST; default -> net.minecraft.util.math.Direction.EAST; });
            return cf.obras.ponerDev(IntegerArgumentType.getInteger(c, "x"), IntegerArgumentType.getInteger(c, "y"), IntegerArgumentType.getInteger(c, "z"), st);
        });
    }

    /** {@code /cafe buscar <texto>}: busca en la tienda DECORAR (por el nombre en ingles del objeto, por ejemplo «oak chair»); sin texto borra la busqueda. */
    private static String buscar(ServerPlayerEntity p, String texto) {
        Cafe cf = Cafes.de(p.getUuid());
        if (cf == null || !cf.esDuenio(p)) return "Entra primero a tu café con /cafe entrar.";
        Menus.vista(p).buscar(texto);
        Menus.abrir(cf, p, "decorar", false);
        return null;
    }

    /** {@code /cafe baul <texto>}: filtra el baul por el nombre del objeto; sin texto quita el filtro. */
    private static String buscarBaul(ServerPlayerEntity p, String texto) {
        Cafe cf = Cafes.de(p.getUuid());
        if (cf == null || !cf.esDuenio(p)) return "Entra primero a tu café con /cafe entrar.";
        Menus.vista(p).baulBuscar = texto == null ? "" : texto.trim();
        Menus.abrir(cf, p, "baul", false);
        return null;
    }

    private static int con(com.mojang.brigadier.context.CommandContext<ServerCommandSource> c, Accion a) {
        ServerPlayerEntity p = c.getSource().getPlayer();
        if (p == null) { c.getSource().sendError(Text.literal("Solo para jugadores.")); return 0; }
        String err;
        try { err = a.hacer(p); } catch (com.mojang.brigadier.exceptions.CommandSyntaxException x) { err = "No encuentro a ese jugador (tiene que estar conectado)."; } catch (Throwable t) { EmiCafe.LOG.error("Error en un comando", t); err = "Error interno (mira el registro del servidor)."; }
        if (err != null) { p.sendMessage(Msg.err(err), false); return 0; }
        return 1;
    }
}
