package emi.survivors.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Lo que el servidor le cuenta cada 4 ticks al jugador que esta en una partida: sirve para activar la camara cenital y para dibujar la interfaz.
 * {@code activo} a false = la partida ha terminado (el cliente devuelve su camara y su interfaz).
 * {@code armas} y {@code pasivas} son listas «id:nivel,id:nivel» (los nombres e iconos los saca el cliente de {@code Catalogo}); {@code jefe} es el nombre del jefe
 * vivo o vacio. {@code msptServidor} y {@code msNuestro} son medidas de rendimiento (ms por tick del servidor, y lo que gasta este mod).
 * {@code caido} = el jugador se ha quedado sin vida y espera a que lo revivan ({@code reviveProg} = 0-100); {@code companeros} = los demas jugadores del equipo,
 * «nombre|vida|vidaMax|estado» separados por «;» (estado 0 en pie, 1 caido, 2 eligiendo carta). {@code derrotados} es el total del equipo.
 * {@code evento} = el titulo de la marea en marcha (o vacio) y {@code eventoSeg} = los segundos que le quedan.
 * {@code pokemon} = el nombre del Pokemon del jugador, {@code zona} = el nombre del mundo/bioma de la partida y {@code estrellas} = las michiestrellitas que lleva ganadas.
 * La lista de {@code armas} es «id:nivel:nombre:icono:ticksDesdeElUltimoUso,…».
 */
public record EstadoPayload(boolean activo, int hp, int hpMax, int segundos, int enemigos, int derrotados, int nivel, int xp, int xpSig, boolean esperando,
                            String jefe, int jefeHp, int jefeMax, String armas, String pasivas, float msptServidor, float msNuestro,
                            boolean caido, int reviveProg, String companeros, String evento, int eventoSeg, String pokemon, String zona, int estrellas) implements CustomPayload {
    public static final CustomPayload.Id<EstadoPayload> ID = new CustomPayload.Id<>(Identifier.of("emi_survivors", "estado"));
    public static final PacketCodec<RegistryByteBuf, EstadoPayload> CODEC = PacketCodec.of(EstadoPayload::escribir, EstadoPayload::leer);

    public static EstadoPayload apagado() { return new EstadoPayload(false, 0, 0, 0, 0, 0, 1, 0, 1, false, "", 0, 0, "", "", 0f, 0f, false, 0, "", "", 0, "", "", 0); }

    private static void escribir(EstadoPayload p, RegistryByteBuf b) {
        b.writeBoolean(p.activo); b.writeInt(p.hp); b.writeInt(p.hpMax); b.writeInt(p.segundos); b.writeInt(p.enemigos); b.writeInt(p.derrotados);
        b.writeInt(p.nivel); b.writeInt(p.xp); b.writeInt(p.xpSig); b.writeBoolean(p.esperando);
        b.writeString(p.jefe); b.writeInt(p.jefeHp); b.writeInt(p.jefeMax); b.writeString(p.armas); b.writeString(p.pasivas);
        b.writeFloat(p.msptServidor); b.writeFloat(p.msNuestro);
        b.writeBoolean(p.caido); b.writeInt(p.reviveProg); b.writeString(p.companeros); b.writeString(p.evento); b.writeInt(p.eventoSeg);
        b.writeString(p.pokemon); b.writeString(p.zona); b.writeInt(p.estrellas);
    }

    private static EstadoPayload leer(RegistryByteBuf b) {
        return new EstadoPayload(b.readBoolean(), b.readInt(), b.readInt(), b.readInt(), b.readInt(), b.readInt(), b.readInt(), b.readInt(), b.readInt(), b.readBoolean(),
                b.readString(), b.readInt(), b.readInt(), b.readString(), b.readString(), b.readFloat(), b.readFloat(),
                b.readBoolean(), b.readInt(), b.readString(), b.readString(), b.readInt(), b.readString(), b.readString(), b.readInt());
    }

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
