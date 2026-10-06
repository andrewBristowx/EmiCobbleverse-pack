package com.emipokemon.raid.snorlax;

import net.minecraft.class_1657;
import net.minecraft.class_1937;
import net.minecraft.class_2960;
import net.minecraft.class_3414;
import net.minecraft.class_3419;
import net.minecraft.class_6880;

/**
 * Voz del jefe Snorlax Emi: sustituye los sonidos de vanilla (rugidos) por emipokemon:snorlax_emi_voz
 * (assets/emipokemon/sounds/snorlax_emi_voz.ogg). Las llamadas originales a ServerWorld.playSound se redirigen aqui;
 * se ignora el sonido que traian, el tono se deja en 1.0 y el volumen minimo es 2 (se oye por toda la arena).
 */
public final class SnorlaxVoice {
    private static final class_3414 VOZ = class_3414.method_47908(class_2960.method_60655("emipokemon", "snorlax_emi_voz"));

    private SnorlaxVoice() {
    }

    /** Sustituye a ServerWorld.playSound(Player, x, y, z, SoundEvent, SoundSource, vol, pitch). */
    public static void play(class_1937 world, class_1657 except, double x, double y, double z, class_3414 ignored, class_3419 category, float volume, float pitch) {
        world.method_43128(except, x, y, z, VOZ, category, Math.max(volume, 2.0f), 1.0f);
    }

    /** Sustituye a ServerWorld.playSound(Player, x, y, z, Holder<SoundEvent>, SoundSource, vol, pitch). */
    public static void playHolder(class_1937 world, class_1657 except, double x, double y, double z, class_6880 ignored, class_3419 category, float volume, float pitch) {
        world.method_43128(except, x, y, z, VOZ, category, Math.max(volume, 2.0f), 1.0f);
    }
}
