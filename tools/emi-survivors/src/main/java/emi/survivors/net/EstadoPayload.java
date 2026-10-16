package emi.survivors.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Lo que el servidor le cuenta cada 2 ticks al jugador que esta en una partida: sirve para activar la camara cenital y para dibujar la interfaz.
 * {@code activo} a false = la partida ha terminado (el cliente devuelve su camara y su interfaz).
 * {@code msptServidor} y {@code msNuestro} son medidas de rendimiento de la prueba (ms por tick del servidor, y lo que gasta este mod).
 */
public record EstadoPayload(boolean activo, int hp, int hpMax, int segundos, int enemigos, int derrotados, float msptServidor, float msNuestro) implements CustomPayload {
    public static final CustomPayload.Id<EstadoPayload> ID = new CustomPayload.Id<>(Identifier.of("emi_survivors", "estado"));
    public static final PacketCodec<RegistryByteBuf, EstadoPayload> CODEC = PacketCodec.of(EstadoPayload::escribir, EstadoPayload::leer);

    public static EstadoPayload apagado() { return new EstadoPayload(false, 0, 0, 0, 0, 0, 0f, 0f); }

    private static void escribir(EstadoPayload p, RegistryByteBuf b) {
        b.writeBoolean(p.activo); b.writeInt(p.hp); b.writeInt(p.hpMax); b.writeInt(p.segundos); b.writeInt(p.enemigos); b.writeInt(p.derrotados);
        b.writeFloat(p.msptServidor); b.writeFloat(p.msNuestro);
    }

    private static EstadoPayload leer(RegistryByteBuf b) {
        return new EstadoPayload(b.readBoolean(), b.readInt(), b.readInt(), b.readInt(), b.readInt(), b.readInt(), b.readFloat(), b.readFloat());
    }

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
