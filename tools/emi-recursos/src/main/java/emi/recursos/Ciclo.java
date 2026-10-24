package emi.recursos;

import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/** El reloj de los mundos de recursos: crea el ciclo, avisa antes del reinicio, saca a la gente y lo vuelve a crear con otra semilla. */
final class Ciclo {
    /** Cuanto dura un ciclo; {@code -Demi.recursos.dias=0.01} lo acorta para probar. */
    static final long DURACION_MS = (long) (Double.parseDouble(System.getProperty("emi.recursos.dias", "3")) * 86_400_000d);
    /** Avisos antes del reinicio (en milisegundos): 1 h, 30 min, 10 min, 5 min, 1 min, 30 s, 10 s y la cuenta de los ultimos segundos. */
    private static final long[] AVISOS = {3_600_000, 1_800_000, 600_000, 300_000, 60_000, 30_000, 10_000, 5_000, 4_000, 3_000, 2_000, 1_000};
    private static final Set<Long> avisados = new HashSet<>();
    private static int tick;

    // ------------------------------------------------------------ arranque y parada

    static void alArrancar(MinecraftServer s) {
        try {
            Estado.cargar(s);
            Estado.Ciclo c = Estado.ciclo();
            if (c == null || System.currentTimeMillis() >= c.reinicioMs) {
                c = nuevo(c == null ? 0 : c.epoca);
                Estado.ciclo(c);
                EmiRecursos.LOG.info("Mundos de recursos: ciclo nuevo {} (semilla {})", c.epoca, c.semilla);
            }
            Mundos.limpiarCarpetas(s, c.epoca);
            Mundos.abrir(s, c);
            avisados.clear();
            EmiRecursos.LOG.info("Mundos de recursos abiertos: ciclo {}, se reinician en {}", c.epoca, formato(c.reinicioMs - System.currentTimeMillis()));
        } catch (Throwable t) {
            // un fallo aqui no debe tumbar el servidor: simplemente no hay mundos de recursos hasta el proximo arranque
            EmiRecursos.LOG.error("No se pudieron abrir los mundos de recursos; /recursos no estara disponible", t);
            Mundos.cerrar();
        }
    }

    static void alParar(MinecraftServer s) { Mundos.guardarDragon(); Estado.guardar(); }

    private static Estado.Ciclo nuevo(int epocaAnterior) {
        Estado.Ciclo c = new Estado.Ciclo();
        c.epoca = epocaAnterior + 1;
        c.semilla = new Random().nextLong();
        c.inicioMs = System.currentTimeMillis();
        c.reinicioMs = c.inicioMs + DURACION_MS;
        return c;
    }

    // ------------------------------------------------------------ el reloj

    static void tick(MinecraftServer s) {
        if (++tick % 20 != 0) return;
        if (tick % 1200 == 0 && Mundos.abiertos()) Mundos.guardarDragon();
        Estado.Ciclo c = Estado.ciclo();
        if (c == null || !Mundos.abiertos()) return;
        long falta = c.reinicioMs - System.currentTimeMillis();
        if (falta <= 0) { reiniciar(s); return; }
        long umbral = -1;
        for (long a : AVISOS) if (falta <= a) umbral = a;      // AVISOS va de mayor a menor: el ultimo que cumple es el mas pequeño que aun no se ha pasado
        if (umbral > 0 && avisados.add(umbral)) {
            for (long b : AVISOS) if (b > umbral) avisados.add(b);
            avisar(s, falta);
        }
    }

    private static void avisar(MinecraftServer s, long falta) {
        for (ServerPlayerEntity p : dentro(s)) {
            if (falta <= 10_000) {
                p.networkHandler.sendPacket(new TitleFadeS2CPacket(0, 25, 5));
                p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("Vuelves al mundo normal").formatted(Formatting.YELLOW)));
                p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(String.valueOf(Math.max(1, (falta + 500) / 1000))).formatted(Formatting.RED, Formatting.BOLD)));
            } else {
                p.sendMessage(Text.literal("[Recursos] ").formatted(Formatting.GOLD, Formatting.BOLD)
                        .append(Text.literal("El mundo de recursos se reinicia en " + formato(falta) + ". Vuelve con /recursos salir: lo que lleves encima lo conservas, lo que dejes aqui se pierde.").formatted(Formatting.YELLOW)), false);
            }
        }
    }

    /** Los jugadores que ahora mismo estan en cualquiera de los tres mundos de recursos. */
    static ArrayList<ServerPlayerEntity> dentro(MinecraftServer s) {
        ArrayList<ServerPlayerEntity> l = new ArrayList<>();
        for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) if (Mundos.esRecursos(p.getServerWorld())) l.add(p);
        return l;
    }

    /** El reinicio: saca a todos al mundo normal, borra los tres mundos y abre otros nuevos con otra semilla. */
    static void reiniciar(MinecraftServer s) {
        for (ServerPlayerEntity p : dentro(s)) sacar(p, "El mundo de recursos se ha reiniciado. Has vuelto al mundo normal.");
        Estado.Ciclo viejo = Estado.ciclo();
        Mundos.cerrar();
        Estado.Ciclo c = nuevo(viejo == null ? 0 : viejo.epoca);
        Estado.ciclo(c);
        try { Mundos.abrir(s, c); } catch (Throwable t) { EmiRecursos.LOG.error("No se pudieron abrir los mundos de recursos nuevos", t); Mundos.cerrar(); return; }
        avisados.clear();
        EmiRecursos.LOG.info("Mundos de recursos: reiniciados, ciclo {} (semilla {})", c.epoca, c.semilla);
        s.getPlayerManager().broadcast(Text.literal("[Recursos] ").formatted(Formatting.GOLD, Formatting.BOLD)
                .append(Text.literal("¡Hay un mundo de recursos nuevo! Entra con /recursos entrar. Se reinicia en " + formato(c.reinicioMs - System.currentTimeMillis()) + ".").formatted(Formatting.GREEN)), false);
    }

    // ------------------------------------------------------------ jugadores

    /** Devuelve al jugador al sitio por donde entro (o al spawn del mundo normal si no se sabe o ya no existe). */
    static void sacar(ServerPlayerEntity p, String mensaje) {
        Estado.Jug j = Estado.jug(p.getUuid());
        ServerWorld destino = null;
        if (j != null && j.dim != null) {
            ServerWorld w = p.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(j.dim)));
            if (w != null && !Mundos.esRecursos(w)) destino = w;
        }
        if (destino != null) p.teleport(destino, j.x, j.y, j.z, j.yaw, j.pitch);
        else Mundos.alSpawnNormal(p);
        if (j != null) { j.dentro = false; Estado.guardar(); }
        if (mensaje != null) p.sendMessage(Text.literal("[Recursos] ").formatted(Formatting.GOLD, Formatting.BOLD).append(Text.literal(mensaje).formatted(Formatting.YELLOW)), false);
    }

    /** Al conectarse: quien se desconecto dentro de los mundos de recursos aparece en el spawn del mundo normal. */
    static void alEntrar(ServerPlayerEntity p) {
        Estado.Jug j = Estado.jug(p.getUuid());
        boolean enRecursos = Mundos.esRecursos(p.getServerWorld());
        if (!enRecursos && (j == null || !j.dentro)) return;
        Mundos.alSpawnNormal(p);
        if (j != null) { j.dentro = false; Estado.guardar(); }
        p.sendMessage(Text.literal("[Recursos] ").formatted(Formatting.GOLD, Formatting.BOLD)
                .append(Text.literal("Te desconectaste dentro del mundo de recursos: has aparecido en el spawn del mundo normal.").formatted(Formatting.YELLOW)), false);
    }

    /** Apunta si el jugador esta dentro o fuera de los mundos de recursos cada vez que cambia de mundo (entrar, portales, morir...). */
    static void alCambiarDeMundo(ServerPlayerEntity p, ServerWorld origen, ServerWorld destino) {
        boolean ahora = Mundos.esRecursos(destino);
        Estado.Jug j = ahora ? Estado.jugNuevo(p.getUuid()) : Estado.jug(p.getUuid());
        if (j == null || j.dentro == ahora) return;
        j.dentro = ahora;
        Estado.guardar();
    }

    // ------------------------------------------------------------ textos

    static String formato(long ms) {
        long m = Math.max(0, ms) / 60_000, h = m / 60, d = h / 24;
        if (d >= 1) return d + (d == 1 ? " dia " : " dias ") + (h % 24) + " h";
        if (h >= 1) return h + " h " + (m % 60) + " min";
        if (m >= 1) return m + " min";
        return Math.max(1, ms / 1000) + " s";
    }
}
