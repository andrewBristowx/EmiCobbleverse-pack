package emi.huida.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Un mensaje del servidor al cliente con un texto JSON: los datos del menu de la sala ({@code menu}; vacio = cerrar la pantalla; {@code menu_act} = solo refrescar si ya esta abierto). */
public record TextoPayload(String tipo, String json) implements CustomPayload {
    public static final CustomPayload.Id<TextoPayload> ID = new CustomPayload.Id<>(Identifier.of("emi_huida", "texto"));
    public static final PacketCodec<RegistryByteBuf, TextoPayload> CODEC = PacketCodec.of(
            (p, b) -> { b.writeString(p.tipo); b.writeString(p.json, 1 << 20); },
            b -> new TextoPayload(b.readString(), b.readString(1 << 20)));

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
