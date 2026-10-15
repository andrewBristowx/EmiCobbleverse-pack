package emi.huida.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * El recorrido para el minimapa de la interfaz: puntos (x, z del mundo) del eje del camino de la copia del jugador, uno cada {@code paso} bloques de
 * recorrido empezando en 0, y los datos fijos del nivel (largo, distancia de la meta y puntos que se pueden conseguir en total).
 */
public record MapaPayload(float paso, float largo, float meta, int puntosTotal, float[] pts) implements CustomPayload {
    public static final CustomPayload.Id<MapaPayload> ID = new CustomPayload.Id<>(Identifier.of("emi_huida", "mapa"));
    public static final PacketCodec<RegistryByteBuf, MapaPayload> CODEC = PacketCodec.of(MapaPayload::escribir, MapaPayload::leer);

    private static void escribir(MapaPayload p, RegistryByteBuf b) {
        b.writeFloat(p.paso); b.writeFloat(p.largo); b.writeFloat(p.meta); b.writeInt(p.puntosTotal); b.writeInt(p.pts.length);
        for (float f : p.pts) b.writeFloat(f);
    }
    private static MapaPayload leer(RegistryByteBuf b) {
        float paso = b.readFloat(), largo = b.readFloat(), meta = b.readFloat(); int total = b.readInt(), n = b.readInt();
        float[] d = new float[Math.max(0, Math.min(n, 20_000))];
        for (int k = 0; k < n; k++) { float f = b.readFloat(); if (k < d.length) d[k] = f; }
        return new MapaPayload(paso, largo, meta, total, d);
    }

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
