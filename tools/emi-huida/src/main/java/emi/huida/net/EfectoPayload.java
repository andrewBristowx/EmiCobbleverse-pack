package emi.huida.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Efectos de dibujo sobre el propio jugador (nivel 3): {@code aplastado} 0..1 = lo plano que se le ve (1 = una tortita); el servidor lo manda cada tick mientras dura y el cliente lo olvida si deja de llegar. */
public record EfectoPayload(float aplastado) implements CustomPayload {
    public static final CustomPayload.Id<EfectoPayload> ID = new CustomPayload.Id<>(Identifier.of("emi_huida", "efecto"));
    public static final PacketCodec<RegistryByteBuf, EfectoPayload> CODEC = PacketCodec.of((p, b) -> b.writeFloat(p.aplastado), b -> new EfectoPayload(b.readFloat()));

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
