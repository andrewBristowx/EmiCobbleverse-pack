package emi.estructuras;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmiEstructuras implements ModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("emi_estructuras");
    private static int arranque = -1;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((d, access, env) -> registrar(d));
        ServerLifecycleEvents.SERVER_STARTED.register(s -> {
            Generador.iniciar(s);
            arranque = 600;     // 30 s despues de arrancar, para no competir con el resto del arranque
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> Generador.parar());
        ServerTickEvents.END_SERVER_TICK.register(s -> {
            if (arranque > 0 && --arranque == 0) {
                int n = Generador.sincronizar();
                if (n > 0) LOG.info("Registradas en Emipokemon {} ubicaciones ya generadas", n);
                if (!Generador.progreso().terminado && Generador.mundo() != null) {
                    String err = Generador.arrancar(false);
                    if (err != null) LOG.info("Generacion automatica: {}", err);
                }
            }
            Generador.tick();
        });
    }

    private static Text msg(String s, Formatting f) { return Text.literal("[Emi Estructuras] ").formatted(Formatting.LIGHT_PURPLE).append(Text.literal(s).formatted(f)); }

    private static void registrar(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> d) {
        var raiz = CommandManager.literal("emiestructuras").requires(s -> s.hasPermissionLevel(2));
        raiz.then(CommandManager.literal("generar")
                .executes(c -> {
                    String err = Generador.arrancar(false);
                    c.getSource().sendFeedback(() -> msg(err == null ? "Generando las estructuras (tarda varios minutos; mira /emiestructuras estado)." : err, err == null ? Formatting.GREEN : Formatting.RED), true);
                    return err == null ? 1 : 0;
                })
                .then(CommandManager.literal("forzar").executes(c -> {
                    String err = Generador.arrancar(true);
                    c.getSource().sendFeedback(() -> msg(err == null ? "Regenerando TODAS las estructuras desde cero." : err, err == null ? Formatting.GOLD : Formatting.RED), true);
                    return err == null ? 1 : 0;
                })));
        raiz.then(CommandManager.literal("parar").executes(c -> {
            Generador.parar();
            c.getSource().sendFeedback(() -> msg("Generacion parada. El progreso queda guardado.", Formatting.YELLOW), true);
            return 1;
        }));
        raiz.then(CommandManager.literal("registrar").executes(c -> {
            int n = Generador.sincronizar();
            c.getSource().sendFeedback(() -> msg("Registradas en Emipokemon " + n + " ubicaciones (sin generar nada).", Formatting.GREEN), true);
            return 1;
        }));
        raiz.then(CommandManager.literal("estado").executes(c -> {
            var p = Generador.progreso();
            long ok = p.hechas.values().stream().filter(h -> h.ok).count();
            c.getSource().sendFeedback(() -> msg(ok + " estructuras hechas" + (p.terminado ? " (terminado)" : "") + (Generador.enMarcha()
                    ? "; en marcha: " + Generador.actualNombre() + ", quedan " + Generador.pendientes() : "") + ", fallos: " + Generador.fallos().size(), Formatting.WHITE), false);
            for (String f : Generador.fallos()) c.getSource().sendFeedback(() -> Text.literal(" - " + f).formatted(Formatting.RED), false);
            return 1;
        }));
        raiz.then(CommandManager.literal("ir").then(CommandManager.argument("id", StringArgumentType.greedyString())
                .suggests((ctx, b) -> { Generador.progreso().hechas.keySet().forEach(b::suggest); return b.buildFuture(); })
                .executes(c -> {
                    ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
                    String id = StringArgumentType.getString(c, "id").trim();
                    var h = Generador.progreso().hechas.get(id);
                    if (h == null || Generador.mundo() == null) { c.getSource().sendError(msg("No conozco esa estructura (todavia).", Formatting.RED)); return 0; }
                    p.teleport(Generador.mundo(), h.entradaX + 0.5, h.entradaY, h.entradaZ + 0.5, -90f, 10f);
                    return 1;
                })));
        raiz.then(CommandManager.literal("lista").executes(c -> {
            var todas = Generador.todas().isEmpty() ? Catalogo.construir(c.getSource().getServer()) : Generador.todas();
            c.getSource().sendFeedback(() -> msg(todas.size() + " estructuras en total (las 69 de /emipokemon visitar primero).", Formatting.WHITE), false);
            for (var e : todas) c.getSource().sendFeedback(() -> Text.literal(" " + (e.clave == null ? "·" : e.clave) + "  " + e.id + (e.plantilla ? " (plantilla)" : "")).formatted(Formatting.GRAY), false);
            return 1;
        }));
        if (System.getProperty("emi.est.test") != null) {   // diagnostico del desarrollador: dibuja una estructura ya colocada
            raiz.then(CommandManager.literal("_foto").then(CommandManager.argument("id", StringArgumentType.greedyString()).executes(c -> {
                String id = StringArgumentType.getString(c, "id").trim();
                var h = Generador.progreso().hechas.get(id);
                if (h == null) { c.getSource().sendError(msg("No esta hecha: " + id, Formatting.RED)); return 0; }
                try {
                    String r = Fotos.hacer(Generador.mundo(), h, 20);
                    c.getSource().sendFeedback(() -> msg("foto " + r, Formatting.GREEN), false);
                } catch (Exception e) { LOG.error("foto", e); c.getSource().sendError(msg("fallo: " + e, Formatting.RED)); }
                return 1;
            })));
        }
        if (System.getProperty("emi.est.test") != null) raiz.then(CommandManager.literal("_planta").then(CommandManager.argument("a", StringArgumentType.greedyString()).executes(c -> {
            try {
                String[] a = StringArgumentType.getString(c, "a").trim().split(" ");
                int x0 = Integer.parseInt(a[0]), z0 = Integer.parseInt(a[1]), x1 = Integer.parseInt(a[2]), z1 = Integer.parseInt(a[3]), y = Integer.parseInt(a[4]);
                var w = Generador.mundo();
                for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) w.getChunk(cx, cz);
                var img = new java.awt.image.BufferedImage(x1 - x0 + 1, z1 - z0 + 1, java.awt.image.BufferedImage.TYPE_INT_RGB);
                var m = new net.minecraft.util.math.BlockPos.Mutable();
                for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) img.setRGB(x - x0, z - z0, w.getBlockState(m.set(x, y, z)).isAir() ? 0xFFFFFF : 0x303030);
                var big = new java.awt.image.BufferedImage(img.getWidth() * 4, img.getHeight() * 4, java.awt.image.BufferedImage.TYPE_INT_RGB);
                for (int i = 0; i < big.getWidth(); i++) for (int j = 0; j < big.getHeight(); j++) big.setRGB(i, j, img.getRGB(i / 4, j / 4));
                javax.imageio.ImageIO.write(big, "png", new java.io.File(w.getServer().getSavePath(net.minecraft.util.WorldSavePath.ROOT).toFile(), "emi_estructuras/fotos/planta_" + y + ".png"));
            } catch (Exception e) { LOG.error("planta", e); }
            return 1;
        })));
        d.register(raiz);
    }
}
