package emi.carreras.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** El poder que lleva el corredor (para la tarjeta de la interfaz). `id` vacio = ya no lleva ninguno; `nuevo` = acaba de cogerlo (se muestra grande unos segundos). */
public record PoderPayload(String id, String nombre, String descripcion, String item, int color, boolean nuevo) implements CustomPayload {
    public static final CustomPayload.Id<PoderPayload> ID = new CustomPayload.Id<>(Identifier.of("emi_carreras", "poder"));
    public static final PacketCodec<RegistryByteBuf, PoderPayload> CODEC = PacketCodec.of(PoderPayload::escribir, PoderPayload::leer);

    public static PoderPayload ninguno() { return new PoderPayload("", "", "", "", 0xFFFFFF, false); }

    private static void escribir(PoderPayload p, RegistryByteBuf b) {
        b.writeString(p.id); b.writeString(p.nombre); b.writeString(p.descripcion); b.writeString(p.item); b.writeInt(p.color); b.writeBoolean(p.nuevo);
    }

    private static PoderPayload leer(RegistryByteBuf b) {
        return new PoderPayload(b.readString(), b.readString(), b.readString(), b.readString(), b.readInt(), b.readBoolean());
    }

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
