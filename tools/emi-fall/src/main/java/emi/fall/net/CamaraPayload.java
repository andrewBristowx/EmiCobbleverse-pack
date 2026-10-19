package emi.fall.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** El servidor le pide al cliente que juegue en tercera persona (como en Fall Guys) mientras dura el torneo, y que vuelva a como estaba al acabar. */
public record CamaraPayload(boolean tercera) implements CustomPayload {
    public static final CustomPayload.Id<CamaraPayload> ID = new CustomPayload.Id<>(Identifier.of("emi_fall", "camara"));
    public static final PacketCodec<RegistryByteBuf, CamaraPayload> CODEC = PacketCodec.of((p, buf) -> buf.writeBoolean(p.tercera), buf -> new CamaraPayload(buf.readBoolean()));

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
