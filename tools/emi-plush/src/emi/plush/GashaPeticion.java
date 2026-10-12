package emi.plush;

import io.netty.buffer.ByteBuf;
import net.minecraft.class_2960;
import net.minecraft.class_8710;
import net.minecraft.class_9135;
import net.minecraft.class_9139;

/** Cliente → servidor: lo que pide la pantalla del Gasha (JSON en una cadena: {"op":"estado"|"tirar","x":..,"y":..,"z":..,"n":1|10}). */
public record GashaPeticion(String json) implements class_8710 {
    public static final class_8710.class_9154<GashaPeticion> ID = new class_8710.class_9154<>(class_2960.method_60655(EmiPlush.ID, "gasha_peticion"));
    public static final class_9139<ByteBuf, GashaPeticion> CODEC = class_9135.field_48554.method_56432(GashaPeticion::new, GashaPeticion::json);

    @Override
    public class_8710.class_9154<? extends class_8710> method_56479() {
        return ID;
    }
}
