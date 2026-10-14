package emi.huida.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** El servidor le dice al cliente si esta en una huida (para poner la camara en tercera persona por detras). Mas adelante llevara la distancia, errores, etc. */
public record EstadoPayload(boolean activo) implements CustomPayload {
    public static final CustomPayload.Id<EstadoPayload> ID = new CustomPayload.Id<>(Identifier.of("emi_huida", "estado"));
    public static final PacketCodec<RegistryByteBuf, EstadoPayload> CODEC = PacketCodec.of(EstadoPayload::escribir, EstadoPayload::leer);

    private static void escribir(EstadoPayload p, RegistryByteBuf b) { b.writeBoolean(p.activo); }
    private static EstadoPayload leer(RegistryByteBuf b) { return new EstadoPayload(b.readBoolean()); }

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
