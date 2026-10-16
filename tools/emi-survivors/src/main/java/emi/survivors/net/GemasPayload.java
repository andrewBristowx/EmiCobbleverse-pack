package emi.survivors.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Las gemas de experiencia (michiestrellitas) que hay en el suelo de la partida. Se mandan respecto al jugador ({@code dx}, {@code dz} en 1/16 de bloque) para que
 * quepan en 2 bytes cada una; {@code tipo}: 0 pequena, 1 mediana, 2 grande. El cliente las dibuja (no son entidades).
 */
public record GemasPayload(double px, double pz, short[] dx, short[] dz, byte[] tipo) implements CustomPayload {
    public static final CustomPayload.Id<GemasPayload> ID = new CustomPayload.Id<>(Identifier.of("emi_survivors", "gemas"));
    public static final PacketCodec<RegistryByteBuf, GemasPayload> CODEC = PacketCodec.of(GemasPayload::escribir, GemasPayload::leer);

    public static GemasPayload vacio() { return new GemasPayload(0, 0, new short[0], new short[0], new byte[0]); }

    private static void escribir(GemasPayload p, RegistryByteBuf b) {
        b.writeDouble(p.px); b.writeDouble(p.pz);
        b.writeVarInt(p.dx.length);
        for (int i = 0; i < p.dx.length; i++) { b.writeShort(p.dx[i]); b.writeShort(p.dz[i]); b.writeByte(p.tipo[i]); }
    }

    private static GemasPayload leer(RegistryByteBuf b) {
        double px = b.readDouble(), pz = b.readDouble();
        int n = Math.min(b.readVarInt(), 2000);
        short[] dx = new short[n], dz = new short[n]; byte[] t = new byte[n];
        for (int i = 0; i < n; i++) { dx[i] = b.readShort(); dz[i] = b.readShort(); t[i] = b.readByte(); }
        return new GemasPayload(px, pz, dx, dz, t);
    }

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
