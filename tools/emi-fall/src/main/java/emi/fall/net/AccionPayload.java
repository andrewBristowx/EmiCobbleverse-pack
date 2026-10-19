package emi.fall.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Lo que el jugador le pide al servidor desde el menu de la sala: abrir un torneo, unirse, empezar, comprar... (el servidor lo comprueba todo; nunca se fia del cliente). */
public record AccionPayload(String accion, String a, String b) implements CustomPayload {
    public static final CustomPayload.Id<AccionPayload> ID = new CustomPayload.Id<>(Identifier.of("emi_fall", "accion"));
    public static final PacketCodec<RegistryByteBuf, AccionPayload> CODEC = PacketCodec.of(
            (p, buf) -> { buf.writeString(p.accion); buf.writeString(p.a); buf.writeString(p.b); },
            buf -> new AccionPayload(buf.readString(64), buf.readString(256), buf.readString(256)));

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
