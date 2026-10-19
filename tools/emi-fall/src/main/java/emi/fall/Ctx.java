package emi.fall;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Todo lo que un minijuego necesita saber de la ronda en marcha y las ayudas para colocar, eliminar y avisar a los jugadores. */
final class Ctx {
    final Torneo t;
    final MinecraftServer server;
    final ServerWorld w;
    final Random rnd = new Random();
    int tick;                    // ticks desde que empezo el juego (despues de la cuenta atras)
    boolean retener = true;      // durante la cuenta atras los jugadores no se mueven del sitio
    int eliminados;              // cuantos se han eliminado en esta ronda

    private final List<long[]> forzados = new ArrayList<>();

    /**
     * Mantiene cargados los chunks de un rectangulo (en bloques) hasta que acabe la ronda: si no, los chunks lejanos de los jugadores se descargan y las entidades que alli
     * se mueven (barras, frutas, bombas) se quedan como una copia congelada que el mod ya no controla.
     */
    void forzar(int x0, int z0, int x1, int z1) {
        for (int cx = Math.floorDiv(x0, 16); cx <= Math.floorDiv(x1, 16); cx++) for (int cz = Math.floorDiv(z0, 16); cz <= Math.floorDiv(z1, 16); cz++) {
            w.setChunkForced(cx, cz, true);
            w.getChunk(cx, cz);                          // lo carga ya (si hay entidades guardadas de una partida anterior, asi se pueden barrer)
            forzados.add(new long[]{cx, cz});
        }
    }

    void soltarForzados() {
        for (long[] c : forzados) w.setChunkForced((int) c[0], (int) c[1], false);
        forzados.clear();
    }

    Ctx(Torneo t, ServerWorld w) { this.t = t; this.server = t.server; this.w = w; }

    List<Jug> jugs() { return new ArrayList<>(t.jugs.values()); }

    ServerPlayerEntity pl(Jug j) { return server.getPlayerManager().getPlayer(j.id); }

    /** Los que siguen en la ronda y estan conectados. */
    List<Jug> vivos() {
        List<Jug> l = new ArrayList<>();
        for (Jug j : t.jugs.values()) if (!j.fuera && pl(j) != null) l.add(j);
        return l;
    }

    int segundos() { return tick / 20; }

    /** Pone al jugador en (x, y, z) mirando a {@code yaw}, listo para jugar (modo aventura, sin caida, sin velocidad). Ese sitio es donde espera la cuenta atras. */
    void colocar(Jug j, double x, double y, double z, float yaw) {
        ServerPlayerEntity p = pl(j);
        j.sx = x; j.sy = y; j.sz = z; j.syaw = yaw;
        j.fuera = false;
        if (p == null) return;
        p.changeGameMode(GameMode.ADVENTURE);
        p.getAbilities().flying = false; p.sendAbilitiesUpdate();
        p.teleport(w, x, y, z, yaw, 0f);
        p.setVelocity(Vec3d.ZERO);
        p.fallDistance = 0;
        p.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(p));
    }

    /** Reaparece a un jugador (sin cambiar el sitio de espera). */
    void reaparecer(Jug j, double x, double y, double z, float yaw) {
        ServerPlayerEntity p = pl(j);
        if (p == null) return;
        p.teleport(w, x, y, z, yaw, 0f);
        p.setVelocity(Vec3d.ZERO);
        p.fallDistance = 0;
        p.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(p));
    }

    /** Elimina al jugador de la ronda: se queda mirando como espectador. */
    void eliminar(Jug j) {
        if (j.fuera) return;
        j.fuera = true;
        j.ordenFuera = ++eliminados;
        ServerPlayerEntity p = pl(j);
        if (p != null) {
            p.changeGameMode(GameMode.SPECTATOR);
            p.setVelocity(Vec3d.ZERO);
        }
    }

    /** Le da un empujon al jugador: el cliente recibe la nueva velocidad. */
    void empujar(ServerPlayerEntity p, Vec3d v) {
        p.setVelocity(v);
        p.velocityModified = true;
        p.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(p));
    }

    void avisar(Text texto) { for (Jug j : t.jugs.values()) { ServerPlayerEntity p = pl(j); if (p != null) p.sendMessage(texto, false); } }

    void titulo(Text titulo, Text sub, int in, int stay, int out) { for (Jug j : t.jugs.values()) { ServerPlayerEntity p = pl(j); if (p != null) Msg.title(p, titulo, sub, in, stay, out); } }

    void sonido(net.minecraft.sound.SoundEvent s, float vol, float pitch) { for (Jug j : t.jugs.values()) { ServerPlayerEntity p = pl(j); if (p != null) Msg.sound(p, s, vol, pitch); } }

    static double dist2(Entity a, double x, double z) { double dx = a.getX() - x, dz = a.getZ() - z; return dx * dx + dz * dz; }
}
