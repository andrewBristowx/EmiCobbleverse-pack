package emi.fall;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Entrega lo comprado en la tienda al correo del jugador: llama por reflexion a {@code com.emipokemon.mail.MailboxService.sendGift(servidor, uuid, pila, remitente, mensaje)}
 * del Emipokemon del pack (siempre al correo, aunque el jugador este conectado). Por reflexion para que este mod arranque igual sin Emipokemon (entonces la tienda dice que
 * no puede entregar y no cobra nada). {@code -Demi.fall.correo_prueba=true} da por entregado todo sin Emipokemon (solo para pruebas).
 */
final class TiendaCorreo {
    private TiendaCorreo() {}

    static boolean enviar(MinecraftServer server, UUID id, Tienda.Articulo a) {
        try {
            var item = Registries.ITEM.getOrEmpty(Identifier.of(a.item())).orElse(null);
            if (item == null) { EmiFall.LOG.warn("Tienda: el objeto {} no existe en este servidor.", a.item()); return false; }
            if (Boolean.getBoolean("emi.fall.correo_prueba")) { EmiFall.LOG.info("[tienda] (correo de prueba) {} -> {}", id, a.item()); return true; }
            Class<?> c = Class.forName("com.emipokemon.mail.MailboxService");
            Method m = c.getMethod("sendGift", MinecraftServer.class, UUID.class, ItemStack.class, String.class, String.class);
            m.invoke(null, server, id, new ItemStack(item, 1), "Tienda de Emi Fall", "Comprado con estrellas");
            return true;
        } catch (Throwable t) {
            EmiFall.LOG.warn("Tienda: no se pudo mandar {} al correo: {}", a.item(), t.toString());
            return false;
        }
    }
}
