package emi.cafe;

import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Mensajes con el estilo del cafe. */
public final class Msg {
    public static final Text PREFIX = Text.literal("[Café] ").formatted(Formatting.GOLD, Formatting.BOLD);

    public static MutableText say(String s) { return Text.empty().append(PREFIX).append(Text.literal(s).formatted(Formatting.WHITE)); }
    public static MutableText err(String s) { return Text.empty().append(PREFIX).append(Text.literal(s).formatted(Formatting.RED)); }

    public static void title(ServerPlayerEntity p, Text title, Text subtitle, int fadeIn, int stay, int fadeOut) {
        p.networkHandler.sendPacket(new TitleFadeS2CPacket(fadeIn, stay, fadeOut));
        p.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle == null ? Text.empty() : subtitle));
        p.networkHandler.sendPacket(new TitleS2CPacket(title));
    }

    public static void actionBar(ServerPlayerEntity p, Text t) { p.sendMessage(t, true); }

    public static void sound(ServerPlayerEntity p, SoundEvent s, float vol, float pitch) { p.playSoundToPlayer(s, SoundCategory.MASTER, vol, pitch); }
}
