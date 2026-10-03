package com.emipokemon.twitch;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.class_2168;
import net.minecraft.class_2561;
import net.minecraft.server.MinecraftServer;

final class TwitchCommands {
    private static int feedback(class_2168 source, String message) { return 0; }

    /** /emi twitch bonus activar: activa el bono aleatorio de servidor (20 min) con un anuncio propio. Devuelve 1 si se activo, 0 si no. */
    static int emiActivateBonus(TwitchService service, CommandContext<class_2168> context) throws CommandSyntaxException {
        class_2168 source = (class_2168) context.getSource();
        MinecraftServer server = source.method_9211();
        String label = service.emiActivateBonusQuiet(server);
        if (label == null) {
            feedback(source, "No se pudo activar el bono ahora mismo.");
            return 0;
        }
        server.method_3760().method_43514(class_2561.method_43470("§6§l✨ ¡FESTÍN DEL DIRECTO! §f".concat(label).concat(" §7(20 min, para todo el servidor)")), false);
        return 1;
    }
}
