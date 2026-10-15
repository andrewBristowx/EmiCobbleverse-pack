package emi.huida.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** El servidor le dice al cliente si esta en una huida y lo que necesita la camara: la entidad del Tyrantrum, el rumbo del trazado (yaw suavizado en las esquinas), los errores y si se esta esperando a que todos confirmen «listo» (entonces la tecla de listo manda el comando). */
public record EstadoPayload(boolean activo, int perseguidor, float rumbo, int errores, boolean espera) implements CustomPayload {
    public static final CustomPayload.Id<EstadoPayload> ID = new CustomPayload.Id<>(Identifier.of("emi_huida", "estado"));
    public static final PacketCodec<RegistryByteBuf, EstadoPayload> CODEC = PacketCodec.of(EstadoPayload::escribir, EstadoPayload::leer);

    private static void escribir(EstadoPayload p, RegistryByteBuf b) { b.writeBoolean(p.activo); b.writeInt(p.perseguidor); b.writeFloat(p.rumbo); b.writeInt(p.errores); b.writeBoolean(p.espera); }
    private static EstadoPayload leer(RegistryByteBuf b) { return new EstadoPayload(b.readBoolean(), b.readInt(), b.readFloat(), b.readInt(), b.readBoolean()); }

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
