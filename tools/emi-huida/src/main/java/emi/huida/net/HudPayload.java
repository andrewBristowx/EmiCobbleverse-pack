package emi.huida.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Lo que muestra la interfaz de la huida: donde van el jugador y el Tyrantrum en el recorrido ({@code s}, {@code sCh}), sus puntos de michiestrellas,
 * su puesto entre los jugadores, los puntos del primero, los errores, el tiempo (ticks) y si ya ha terminado ({@code fin}: 0 corriendo, 1 escapo, 2 atrapado).
 */
public record HudPayload(float s, float sCh, int puntos, int puesto, int jugadores, int lider, int errores, int ticks, int fin) implements CustomPayload {
    /** Distancia entre los centros del Tyrantrum y del jugador a la que te alcanza su boca (la boca esta ~6 bloques por delante de su centro, el hocico ~7): la interfaz cuenta la distancia desde ahi. */
    public static final double CAPTURA = 6.0;
    public static final CustomPayload.Id<HudPayload> ID = new CustomPayload.Id<>(Identifier.of("emi_huida", "hud"));
    public static final PacketCodec<RegistryByteBuf, HudPayload> CODEC = PacketCodec.of(HudPayload::escribir, HudPayload::leer);

    private static void escribir(HudPayload p, RegistryByteBuf b) {
        b.writeFloat(p.s); b.writeFloat(p.sCh); b.writeInt(p.puntos); b.writeInt(p.puesto); b.writeInt(p.jugadores); b.writeInt(p.lider);
        b.writeInt(p.errores); b.writeInt(p.ticks); b.writeByte(p.fin);
    }
    private static HudPayload leer(RegistryByteBuf b) {
        return new HudPayload(b.readFloat(), b.readFloat(), b.readInt(), b.readInt(), b.readInt(), b.readInt(), b.readInt(), b.readInt(), b.readByte());
    }

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
