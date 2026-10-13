package emi.plush;

import io.netty.buffer.ByteBuf;
import net.minecraft.class_2960;
import net.minecraft.class_8710;
import net.minecraft.class_9135;
import net.minecraft.class_9139;

/** Servidor → cliente: abrir la pantalla, estado (tickets/garantía) o resultado de las tiradas (JSON en una cadena). */
public record GashaRespuesta(String json) implements class_8710 {
    public static final class_8710.class_9154<GashaRespuesta> ID = new class_8710.class_9154<>(class_2960.method_60655(EmiPlush.ID, "gasha_respuesta"));
    public static final class_9139<ByteBuf, GashaRespuesta> CODEC = class_9135.field_48554.method_56432(GashaRespuesta::new, GashaRespuesta::json);

    @Override
    public class_8710.class_9154<? extends class_8710> method_56479() {
        return ID;
    }
}
