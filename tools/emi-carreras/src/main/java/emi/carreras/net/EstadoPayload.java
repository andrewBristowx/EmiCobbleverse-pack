package emi.carreras.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Estado de la carrera para la interfaz del cliente (tabla de posiciones y mapa). El servidor lo envia unas 4 veces por segundo a quien tenga el mod. */
public record EstadoPayload(boolean activo, String pista, String nombrePista, float mx, float mz, float mancho, float mlargo, int mpx, int mpy,
                            int vueltas, int tiempo, List<Jugador> jugadores) implements CustomPayload {
    public static final CustomPayload.Id<EstadoPayload> ID = new CustomPayload.Id<>(Identifier.of("emi_carreras", "estado"));

    /** Un corredor: posicion en el mundo, vuelta (0 = primera) y puesto actual (1 = el primero). */
    public record Jugador(UUID id, String nombre, float x, float z, int vuelta, int puesto, boolean fin) {}

    public static final PacketCodec<RegistryByteBuf, EstadoPayload> CODEC = PacketCodec.of(EstadoPayload::escribir, EstadoPayload::leer);

    private static void escribir(EstadoPayload p, RegistryByteBuf b) {
        b.writeBoolean(p.activo);
        b.writeString(p.pista == null ? "" : p.pista);
        b.writeString(p.nombrePista == null ? "" : p.nombrePista);
        b.writeFloat(p.mx); b.writeFloat(p.mz); b.writeFloat(p.mancho); b.writeFloat(p.mlargo);
        b.writeVarInt(p.mpx); b.writeVarInt(p.mpy);
        b.writeVarInt(p.vueltas); b.writeVarInt(p.tiempo);
        b.writeVarInt(p.jugadores.size());
        for (Jugador j : p.jugadores) {
            b.writeUuid(j.id); b.writeString(j.nombre); b.writeFloat(j.x); b.writeFloat(j.z);
            b.writeVarInt(j.vuelta); b.writeVarInt(j.puesto); b.writeBoolean(j.fin);
        }
    }

    private static EstadoPayload leer(RegistryByteBuf b) {
        boolean activo = b.readBoolean();
        String pista = b.readString(), nombre = b.readString();
        float mx = b.readFloat(), mz = b.readFloat(), mancho = b.readFloat(), mlargo = b.readFloat();
        int mpx = b.readVarInt(), mpy = b.readVarInt(), vueltas = b.readVarInt(), tiempo = b.readVarInt();
        int n = b.readVarInt();
        List<Jugador> l = new ArrayList<>();
        for (int i = 0; i < n; i++) l.add(new Jugador(b.readUuid(), b.readString(), b.readFloat(), b.readFloat(), b.readVarInt(), b.readVarInt(), b.readBoolean()));
        return new EstadoPayload(activo, pista, nombre, mx, mz, mancho, mlargo, mpx, mpy, vueltas, tiempo, l);
    }

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
