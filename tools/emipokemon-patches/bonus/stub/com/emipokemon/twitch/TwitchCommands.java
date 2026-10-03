package com.emipokemon.twitch;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.class_2168;

final class TwitchCommands {
    private static int feedback(class_2168 source, String message) { return 0; }

    static int emiActivateBonus(TwitchService service, CommandContext<class_2168> context) throws CommandSyntaxException {
        class_2168 source = (class_2168) context.getSource();
        String label = service.activateRandomRelicBonus(source.method_9211());
        return feedback(source, label == null ? "No se pudo activar el bono ahora mismo." : "Bono de servidor activado: ".concat(label));
    }
}
