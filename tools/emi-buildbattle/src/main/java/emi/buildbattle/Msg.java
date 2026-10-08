package emi.buildbattle;

import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Collection;

/** Mensajes con el estilo del minijuego. */
public final class Msg {
    public static final Text PREFIX = Text.literal("[BuildBattle] ").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD);

    public static MutableText say(String s) { return Text.empty().append(PREFIX).append(Text.literal(s).formatted(Formatting.WHITE)); }
    public static MutableText say(Text t) { return Text.empty().append(PREFIX).append(t); }
    public static MutableText err(String s) { return Text.empty().append(PREFIX).append(Text.literal(s).formatted(Formatting.RED)); }

    /** Boton clicable en el chat. */
    public static MutableText button(String label, String command, Formatting color, String hover) {
        return Text.literal("[" + label + "]").styled(s -> s.withColor(color).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hover))));
    }

    public static void title(ServerPlayerEntity p, Text title, Text subtitle, int fadeIn, int stay, int fadeOut) {
        p.networkHandler.sendPacket(new TitleFadeS2CPacket(fadeIn, stay, fadeOut));
        p.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle == null ? Text.empty() : subtitle));
        p.networkHandler.sendPacket(new TitleS2CPacket(title));
    }

    public static void title(Collection<ServerPlayerEntity> ps, Text title, Text subtitle, int fadeIn, int stay, int fadeOut) {
        for (ServerPlayerEntity p : ps) title(p, title, subtitle, fadeIn, stay, fadeOut);
    }

    public static void actionBar(ServerPlayerEntity p, Text t) { p.sendMessage(t, true); }

    public static void sound(ServerPlayerEntity p, SoundEvent s, float vol, float pitch) {
        p.playSoundToPlayer(s, SoundCategory.MASTER, vol, pitch);
    }

    public static void sound(Collection<ServerPlayerEntity> ps, SoundEvent s, float vol, float pitch) {
        for (ServerPlayerEntity p : ps) sound(p, s, vol, pitch);
    }

    public static void broadcastTo(Collection<ServerPlayerEntity> ps, Text t) { for (ServerPlayerEntity p : ps) p.sendMessage(t, false); }

    public static void broadcast(MinecraftServer server, Text t) { server.getPlayerManager().broadcast(t, false); }
}
