package emi.huida.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Las michiestrellas de la copia del jugador. El cliente las dibuja (giran y suben y bajan) y el servidor decide cuales se cogen.
 * {@code accion}: 0 = lista nueva ({@code datos} = x, y, z, puntos de cada una), 1 = se ha cogido la numero {@code idx}, 2 = borrar todas.
 */
public record EstrellasPayload(int accion, int idx, float[] datos) implements CustomPayload {
    public static final CustomPayload.Id<EstrellasPayload> ID = new CustomPayload.Id<>(Identifier.of("emi_huida", "estrellas"));
    public static final PacketCodec<RegistryByteBuf, EstrellasPayload> CODEC = PacketCodec.of(EstrellasPayload::escribir, EstrellasPayload::leer);

    public static EstrellasPayload lista(float[] datos) { return new EstrellasPayload(0, 0, datos); }
    public static EstrellasPayload cogida(int idx) { return new EstrellasPayload(1, idx, new float[0]); }
    public static EstrellasPayload borrar() { return new EstrellasPayload(2, 0, new float[0]); }

    private static void escribir(EstrellasPayload p, RegistryByteBuf b) {
        b.writeByte(p.accion); b.writeInt(p.idx); b.writeInt(p.datos.length);
        for (float f : p.datos) b.writeFloat(f);
    }
    private static EstrellasPayload leer(RegistryByteBuf b) {
        int a = b.readByte(), i = b.readInt(), n = b.readInt();
        float[] d = new float[Math.max(0, Math.min(n, 100_000))];
        for (int k = 0; k < n; k++) { float f = b.readFloat(); if (k < d.length) d[k] = f; }
        return new EstrellasPayload(a, i, d);
    }

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
