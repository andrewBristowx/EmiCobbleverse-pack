package emi.survivors.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Un mensaje del servidor al cliente con un texto JSON (para lo que no va cada tick): las cartas al subir de nivel ({@code cartas}; vacio = cerrar la pantalla)
 * y los datos del menu ({@code menu}). Asi se pueden cambiar sin tocar el formato de los paquetes.
 */
public record TextoPayload(String tipo, String json) implements CustomPayload {
    public static final CustomPayload.Id<TextoPayload> ID = new CustomPayload.Id<>(Identifier.of("emi_survivors", "texto"));
    public static final PacketCodec<RegistryByteBuf, TextoPayload> CODEC = PacketCodec.of(
            (p, b) -> { b.writeString(p.tipo); b.writeString(p.json, 1 << 20); },
            b -> new TextoPayload(b.readString(), b.readString(1 << 20)));

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
