package emi.huida.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Camara de escena (cinematica): mientras llega con {@code activo} la camara del jugador va a esta posicion y mira a este rumbo (el cliente la suaviza).
 * {@code corte} = plano nuevo (el cliente salta de golpe en vez de deslizarse), {@code fov} = multiplicador del campo de vision y {@code temblor} =
 * amplitud (en bloques) de la sacudida. Con {@code activo} a false vuelve a la camara de la huida con una transicion suave.
 */
public record CamaraPayload(boolean activo, boolean corte, float x, float y, float z, float yaw, float pitch, float fov, float temblor) implements CustomPayload {
    public static final CustomPayload.Id<CamaraPayload> ID = new CustomPayload.Id<>(Identifier.of("emi_huida", "camara"));
    public static final PacketCodec<RegistryByteBuf, CamaraPayload> CODEC = PacketCodec.of(CamaraPayload::escribir, CamaraPayload::leer);

    public static CamaraPayload apagada() { return new CamaraPayload(false, false, 0, 0, 0, 0, 0, 1.15f, 0); }

    private static void escribir(CamaraPayload p, RegistryByteBuf b) {
        b.writeBoolean(p.activo); b.writeBoolean(p.corte);
        b.writeFloat(p.x); b.writeFloat(p.y); b.writeFloat(p.z); b.writeFloat(p.yaw); b.writeFloat(p.pitch); b.writeFloat(p.fov); b.writeFloat(p.temblor);
    }
    private static CamaraPayload leer(RegistryByteBuf b) {
        return new CamaraPayload(b.readBoolean(), b.readBoolean(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat());
    }

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
