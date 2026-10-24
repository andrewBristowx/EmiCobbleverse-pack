package emi.cafe.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * El panel de la esquina de arriba a la derecha mientras estas en un cafe: el local, las monedas, lo ganado en la jornada y los pedidos que esperan (con su paciencia).
 * El servidor lo manda cada 5 ticks; si dejan de llegar datos (saliste del cafe) el panel desaparece solo y se devuelven las interfaces que se habian escondido.
 */
@Environment(EnvType.CLIENT)
final class PanelCafe {
    private PanelCafe() {}

    private static JsonObject datos;
    private static long ultimo;
    private static final int MAX_FILAS = 7;

    static void recibir(String json) {
        try { datos = JsonParser.parseString(json).getAsJsonObject(); ultimo = System.currentTimeMillis(); } catch (RuntimeException e) { datos = null; }
    }

    static boolean activo() { return datos != null && System.currentTimeMillis() - ultimo < 2500; }

    static void olvidar() { datos = null; }

    private static String str(JsonObject o, String k, String def) { JsonElement e = o.get(k); return e == null ? def : e.getAsString(); }
    private static int num(JsonObject o, String k) { JsonElement e = o.get(k); return e == null ? 0 : e.getAsInt(); }

    static void dibujar(DrawContext g) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!activo() || mc.currentScreen != null || mc.options.hudHidden || mc.player == null) return;
        JsonObject o = datos;
        TextRenderer f = mc.textRenderer;
        boolean visita = o.has("visita") && o.get("visita").getAsBoolean();
        String estado = str(o, "estado", "cerrado");

        final int W = 164, P = 5, FILA = 10;
        JsonArray ped = o.has("pedidos") ? o.getAsJsonArray("pedidos") : new JsonArray();
        int filas = visita ? 3 : 6;
        int nPed = Math.min(MAX_FILAS, ped.size());
        int alto = P * 2 + filas * FILA + (visita ? 0 : 4 + FILA + Math.max(1, nPed) * 18 + (ped.size() > MAX_FILAS ? FILA : 0));
        int x = g.getScaledWindowWidth() - W - 6, y = 6;

        g.fill(x - 1, y - 1, x + W + 1, y + alto + 1, 0xFFFFD34E);
        g.fill(x, y, x + W, y + alto, 0xE0181426);
        int cy = y + P;
        // titulo y estado
        String titulo = visita ? "Café de " + str(o, "dueno", "?") : str(o, "cafe", "Café Michialien");
        g.drawText(f, f.trimToWidth(titulo, W - 54), x + P, cy, 0xFFFFD34E, true);
        String chip = visita ? "VISITA" : switch (estado) { case "abierto" -> "ABIERTO"; case "obras" -> "OBRAS"; case "cerrando" -> "CERRANDO"; default -> "CERRADO"; };
        int cc = visita ? 0xFF6CC8FF : switch (estado) { case "abierto" -> 0xFF6FE08A; case "obras" -> 0xFFFFB347; case "cerrando" -> 0xFFFFB347; default -> 0xFFB8B8C8; };
        g.drawText(f, chip, x + W - P - f.getWidth(chip), cy, cc, true);
        cy += FILA;
        g.drawText(f, "Local nivel " + num(o, "local") + " · " + str(o, "estilo", ""), x + P, cy, 0xFFFFFFFF, true);
        cy += FILA;
        g.drawText(f, "Reputación: " + str(o, "rep", "") + (visita ? " · ♥ " + num(o, "megusta") : ""), x + P, cy, 0xFFC9B3FF, true);
        cy += FILA;
        if (visita) return;

        g.drawText(f, "Monedas: " + num(o, "monedas"), x + P, cy, 0xFFFFD34E, true);
        cy += FILA;
        boolean abierto = estado.equals("abierto") || estado.equals("cerrando");
        String gan = (abierto ? "Jornada: +" : "Última jornada: +") + num(o, "ganado");
        g.drawText(f, gan, x + P, cy, 0xFF6FE08A, true);
        int equipo = num(o, "equipo");
        if (equipo > 1 && num(o, "ganadoCafe") != num(o, "ganado")) { String t = "(café: " + num(o, "ganadoCafe") + ")"; g.drawText(f, t, x + W - P - f.getWidth(t), cy, 0xFFB8B8C8, true); }
        cy += FILA;
        g.drawText(f, "Serv. " + num(o, "servidos") + " · Enf. " + num(o, "enfadados") + " · Propinas " + num(o, "propinas"), x + P, cy, 0xFFB8B8C8, true);
        cy += FILA;
        int platos = num(o, "platos");
        g.drawText(f, "Platos limpios: " + platos + (o.has("evento") ? " · " + str(o, "evento", "") : ""), x + P, cy, platos == 0 ? 0xFFFF6B5E : 0xFFFFFFFF, true);
        cy += FILA;

        // los pedidos
        cy += 2;
        g.fill(x + P, cy, x + W - P, cy + 1, 0x66FFFFFF);
        cy += 2;
        g.drawText(f, "PEDIDOS PENDIENTES (" + ped.size() + ")", x + P, cy, 0xFFFF9AD5, true);
        cy += FILA;
        if (ped.size() == 0) { g.drawText(f, abierto ? "Ninguno por ahora" : "—", x + P, cy + 4, 0xFFB8B8C8, true); return; }
        for (int i = 0; i < nPed; i++) {
            JsonObject e = ped.get(i).getAsJsonObject();
            try {
                ItemStack st = new ItemStack(Registries.ITEM.get(Identifier.of(str(e, "item", "minecraft:air"))));
                g.drawItem(st, x + P, cy);
            } catch (RuntimeException ignored) { }
            boolean esp = e.has("especial") && e.get("especial").getAsBoolean();
            boolean atiende = e.has("atiende") && e.get("atiende").getAsBoolean();
            String nom = f.trimToWidth(str(e, "plato", "?"), W - 2 * P - 22 - (atiende ? 8 : 0));
            g.drawText(f, nom, x + P + 19, cy, esp ? 0xFFFFD34E : 0xFFFFFFFF, true);
            if (atiende) g.drawText(f, "▶", x + W - P - 7, cy, 0xFF6FE08A, true);
            int pac = Math.max(0, Math.min(100, num(e, "pac")));
            int bx = x + P + 19, bw = W - 2 * P - 19, by = cy + 10;
            g.fill(bx, by, bx + bw, by + 3, 0xFF000000);
            int color = pac > 50 ? 0xFF6FE08A : pac > 25 ? 0xFFFFD34E : 0xFFFF6B5E;
            g.fill(bx, by, bx + bw * pac / 100, by + 3, color);
            cy += 18;
        }
        if (ped.size() > MAX_FILAS) g.drawText(f, "+" + (ped.size() - MAX_FILAS) + " más…", x + P, cy, 0xFFB8B8C8, true);
    }
}
