package emi.survivors.client;

import emi.survivors.Catalogo;
import emi.survivors.net.EstadoPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * La interfaz de la partida, con el estilo de los juegos de supervivencia en pixel-art: arriba a la izquierda el nombre del Pokemon, su nivel y su barra de vida
 * con los numeros dentro; arriba en el centro el tiempo y una linea con la zona, los derrotados y las michiestrellitas; abajo en el centro las ranuras de los
 * ataques (icono, numero, puntos de nivel; se encienden al usarse) y, al lado, los objetos; y la barra del jefe, la marea en marcha y los companeros.
 * Se dibuja en un lienzo de «unidades de interfaz» cuyo tamano en pixeles es entero (asi los dibujos de 24x24 salen nitidos con cualquier escala de Minecraft).
 */
@Environment(EnvType.CLIENT)
public final class SurvivorsHud {
    private SurvivorsHud() {}

    private static final int CONTORNO = 0xFF0C0F1A, NAVY = 0xFF121828, NAVY_VACIO = 0x70182030, BORDE = 0xFF3A4260, BORDE_VACIO = 0x60506080;
    private static final int ROSA = 0xFFFF5AA5, ORO = 0xFFFFD34E, GRIS = 0xFF8A93B0, ACERO = 0xFF9DB4D8, VIDA = 0xFFFF5A6E, XP = 0xFF5BC8F5, JEFE = 0xFFC03AB8;
    private static final int TILE = 34, TILE_P = 28, SEP = 4, SEP_P = 3, NIVEL_ENCENDIDO = 7, COL_FUSION = 13;      // ranuras de ataque 34x34, de objeto 28x34

    // lo que llevabas la ultima vez (para resaltar un ataque que acabas de conseguir o de subir de nivel)
    private static final Map<String, Integer> antes = new HashMap<>();
    private static String resaltado = "";
    private static long resaltadoHasta;
    private static int ultimoSegundo;

    private record Ranura(String id, int nivel, String nombre, String icono, int hace, String cabeza) {}

    // ------------------------------------------------------------------------------------------------ dibujos de ayuda

    /** Un rectangulo con las esquinas redondeadas (radio r). */
    static void redondo(DrawContext g, int x, int y, int w, int h, int r, int color) {
        for (int i = 0; i < h; i++) {
            int d = Math.min(i, h - 1 - i);
            int inset = d >= r ? 0 : (int) Math.round(r - Math.sqrt(Math.max(0, r * r - (r - d - 0.5) * (r - d - 0.5))));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
        }
    }

    /** Una barra con forma de pildora: {@code lleno} = pixeles ya rellenos (de 0 a w). */
    static void pildora(DrawContext g, int x, int y, int w, int h, int lleno, int color) {
        int r = h / 2;
        for (int i = 0; i < h; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
            int x0 = x + inset, x1 = Math.min(x + w - inset, x + lleno);
            if (x1 > x0) g.fill(x0, y + i, x1, y + i + 1, color);
        }
    }

    /** Una barra de pildora con contorno y fondo oscuro, rellena una fraccion {@code f}. */
    static void barra(DrawContext g, int x, int y, int w, int h, float f, int color) {
        pildora(g, x - 1, y - 1, w + 2, h + 2, w + 2, CONTORNO);
        pildora(g, x, y, w, h, w, 0xFF2A2236);
        pildora(g, x, y, w, h, Math.round(w * Math.max(0f, Math.min(1f, f))), color);
    }

    private static final int[][] DIG = {
            {7, 5, 5, 5, 7}, {2, 6, 2, 2, 7}, {7, 1, 7, 4, 7}, {7, 1, 7, 1, 7}, {5, 5, 7, 1, 1}, {7, 4, 7, 1, 7}, {7, 4, 7, 5, 7}, {7, 1, 2, 2, 2}, {7, 5, 7, 5, 7}, {7, 5, 7, 1, 7}};

    /** Un numerito de 3x5 pixeles (con sombra). */
    static void digito(DrawContext g, int d, int x, int y, int color) {
        if (d < 0 || d > 9) return;
        for (int pass = 0; pass < 2; pass++) {
            int ox = pass == 0 ? 1 : 0, oy = pass == 0 ? 1 : 0, c = pass == 0 ? 0xFF000000 : color;
            for (int f = 0; f < 5; f++) for (int k = 0; k < 3; k++) if ((DIG[d][f] >> (2 - k) & 1) != 0) g.fill(x + k + ox, y + f + oy, x + k + ox + 1, y + f + oy + 1, c);
        }
    }

    private static void tiempo(DrawContext g, TextRenderer tr, int segundos, int cx, int y, float escala, int color) {
        String t = (segundos / 60) + ":" + String.format(Locale.ROOT, "%02d", segundos % 60);
        var m = g.getMatrices();
        m.push(); m.translate(cx, y, 0); m.scale(escala, escala, 1f);
        g.drawCenteredTextWithShadow(tr, t, 0, 0, color);
        m.pop();
    }

    private static Ranura[] ranuras(String armas) {
        if (armas.isEmpty()) return new Ranura[0];
        String[] l = armas.split(",");
        Ranura[] r = new Ranura[l.length];
        int n = 0;
        for (String s : l) {
            String[] p = s.split(":", 6);
            if (p.length < 5) continue;
            try { r[n++] = new Ranura(p[0], Integer.parseInt(p[1]), p[2], p[3], Integer.parseInt(p[4]), p.length > 5 ? p[5] : ""); } catch (NumberFormatException e) { /* se salta */ }
        }
        return n == r.length ? r : java.util.Arrays.copyOf(r, n);
    }

    // ------------------------------------------------------------------------------------------------ la interfaz

    public static void render(DrawContext g, RenderTickCounter tick) {
        EstadoPayload e = EmiSurvivorsClient.estado();
        if (e == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        int fbW = mc.getWindow().getFramebufferWidth(), fbH = mc.getWindow().getFramebufferHeight();
        int n = Math.max(1, Math.round(fbH / 540f));                      // pixeles de pantalla por unidad de interfaz (entero: los dibujos salen nitidos)
        int W = fbW / n, H = fbH / n;
        var m = g.getMatrices();
        m.push();
        m.scale(n / (float) mc.getWindow().getScaleFactor(), n / (float) mc.getWindow().getScaleFactor(), 1f);
        try {
            dibujar(g, tr, e, W, H, mc);
        } finally {
            m.pop();
        }
    }

    private static void dibujar(DrawContext g, TextRenderer tr, EstadoPayload e, int W, int H, MinecraftClient mc) {
        var m = g.getMatrices();
        long ahora = System.currentTimeMillis();

        // ---- arriba a la izquierda: nombre, nivel y vida
        g.drawTextWithShadow(tr, e.pokemon().isEmpty() ? "Tu Pokémon" : e.pokemon(), 6, 5, 0xFFFFFFFF);
        int bx = 6 + Math.max(tr.getWidth("Nv. 88"), tr.getWidth("Nv. " + e.nivel())) + 6, by = 17, bw = 116, bh = 10;
        g.drawTextWithShadow(tr, "Nv. " + e.nivel(), 6, by + 1, ORO);
        float f = e.hpMax() <= 0 ? 0f : e.hp() / (float) e.hpMax();
        int colVida = f < 0.25f && (ahora / 250) % 2 == 0 ? 0xFFFF2E48 : VIDA;
        barra(g, bx, by, bw, bh, f, colVida);
        g.drawCenteredTextWithShadow(tr, e.hp() + " / " + e.hpMax(), bx + bw / 2, by + 1, 0xFFFFFFFF);
        barra(g, bx, by + bh + 3, bw, 3, e.xpSig() <= 0 ? 0f : e.xp() / (float) e.xpSig(), XP);

        // ---- arriba en el centro: el tiempo y la linea de datos
        tiempo(g, tr, e.segundos(), W / 2, 4, 2f, 0xFFFFFFFF);
        String info = (e.zona().isEmpty() ? "" : e.zona().toUpperCase(Locale.ROOT) + " · ") + e.derrotados() + " DERROTADOS · " + e.estrellas() + " MICHIESTRELLITAS";
        g.drawCenteredTextWithShadow(tr, info, W / 2, 23, ACERO);

        // ---- el jefe y la marea
        int ey = 36;
        if (!e.jefe().isEmpty() && e.jefeMax() > 0) {
            int jw = 220;
            g.drawCenteredTextWithShadow(tr, e.jefe(), W / 2, ey, 0xFFFFB6F0);
            barra(g, (W - jw) / 2, ey + 11, jw, 8, e.jefeHp() / (float) e.jefeMax(), JEFE);
            ey += 24;
        }
        if (!e.evento().isEmpty()) {
            String txt = e.evento() + (e.eventoSeg() > 0 ? "  " + (e.eventoSeg() / 60) + ":" + String.format(Locale.ROOT, "%02d", e.eventoSeg() % 60) : "");
            g.drawCenteredTextWithShadow(tr, txt, W / 2, ey, ORO);
        }

        // ---- los companeros (arriba a la derecha)
        if (!e.companeros().isEmpty()) {
            int cx = W - 116, cy = 6;
            for (String c : e.companeros().split(";")) {
                String[] q = c.split("\\|");
                if (q.length < 4) continue;
                int hp, max, est;
                try { hp = Integer.parseInt(q[1]); max = Integer.parseInt(q[2]); est = Integer.parseInt(q[3]); } catch (NumberFormatException ex) { continue; }
                String nom = q[0].length() > 14 ? q[0].substring(0, 13) + "…" : q[0];
                g.drawTextWithShadow(tr, nom, cx, cy, est == 1 ? 0xFFFF8A80 : 0xFFFFFFFF);
                if (est == 1) g.drawTextWithShadow(tr, "CAÍDO", cx + 70, cy, 0xFFFF5555);
                else if (est == 2) g.drawTextWithShadow(tr, "elige…", cx + 70, cy, 0xFFB8B8C8);
                float fc = est == 1 || max <= 0 ? 0f : hp / (float) max;
                barra(g, cx, cy + 11, 100, 5, fc, VIDA);
                cy += 24;
            }
        }

        // ---- abajo en el centro: los ataques y los objetos
        Ranura[] atq = ranuras(e.armas());
        if (e.segundos() + 2 < ultimoSegundo) { antes.clear(); resaltado = ""; }             // otra partida
        ultimoSegundo = e.segundos();
        for (Ranura r : atq) {
            int ant = antes.getOrDefault(r.id(), 0);
            if (r.nivel() > ant) { resaltado = r.id(); resaltadoHasta = ahora + 3500; }               // un ataque nuevo o que sube de nivel: marco dorado y su nombre un rato
            antes.put(r.id(), r.nivel());
        }
        // una ranura por cada ataque que no esta fusionado dentro de otro; los fusionados salen en mini en la columna de su ranura
        java.util.List<Ranura> cabezas = new java.util.ArrayList<>();
        for (Ranura r : atq) if (r.cabeza().isEmpty()) cabezas.add(r);
        // objetos: una ranura por cada uno que no esta fusionado dentro de otro; los fusionados salen en mini a la derecha de su ranura (id:nivel:cabeza)
        String[] ps = e.pasivas().isEmpty() ? new String[0] : e.pasivas().split(",");
        java.util.List<String[]> cabP = new java.util.ArrayList<>();
        java.util.List<String[]> unidosP = new java.util.ArrayList<>();
        for (String sp : ps) {
            String[] q = sp.split(":", 3);
            if (q.length < 2) continue;
            if (q.length > 2 && !q[2].isEmpty()) unidosP.add(q); else cabP.add(q);
        }
        int[] anchosP = new int[Catalogo.MAX_PASIVAS];
        int anchoP = (Catalogo.MAX_PASIVAS - 1) * SEP_P;
        for (int i = 0; i < Catalogo.MAX_PASIVAS; i++) {
            anchosP[i] = TILE_P;
            if (i < cabP.size()) for (String[] u : unidosP) if (u[2].equals(cabP.get(i)[0])) { anchosP[i] = TILE_P + COL_FUSION; break; }
            anchoP += anchosP[i];
        }
        int[] anchos = new int[Catalogo.MAX_ARMAS];
        int anchoA = (Catalogo.MAX_ARMAS - 1) * SEP, hueco = 12;
        for (int i = 0; i < Catalogo.MAX_ARMAS; i++) {
            anchos[i] = TILE;
            if (i < cabezas.size()) for (Ranura r : atq) if (r.cabeza().equals(cabezas.get(i).id())) { anchos[i] = TILE + COL_FUSION; break; }
            anchoA += anchos[i];
        }
        int x0 = (W - (anchoA + hueco + anchoP)) / 2, yA = H - 6 - TILE;
        int xa = x0;
        for (int i = 0; i < Catalogo.MAX_ARMAS; i++) {
            Ranura r = i < cabezas.size() ? cabezas.get(i) : null;
            java.util.List<Ranura> unidos = new java.util.ArrayList<>();
            if (r != null) for (Ranura q : atq) if (q.cabeza().equals(r.id())) unidos.add(q);
            boolean luz = r != null && ahora < resaltadoHasta && (r.id().equals(resaltado) || unidos.stream().anyMatch(q -> q.id().equals(resaltado)));
            boolean enc = r != null && r.hace() <= NIVEL_ENCENDIDO;
            for (Ranura q : unidos) enc |= q.hace() <= NIVEL_ENCENDIDO;
            ranuraAtaque(g, xa, yA, i + 1, r, enc, luz);
            for (int k = 0; k < unidos.size() && k < 3; k++) {                       // los ataques fusionados: iconos pequenos en la columna de la derecha
                Iconos.dibujar(g, unidos.get(k).icono(), xa + TILE + 1, yA + 1 + k * 11, 10);
            }
            if (luz) g.drawCenteredTextWithShadow(tr, r.nombre(), xa + TILE / 2, yA - 11, r.nivel() >= Catalogo.NIVEL_EVO ? ORO : 0xFFFFFFFF);
            xa += anchos[i] + SEP;
        }
        int px = x0 + anchoA + hueco, yP = yA;
        int xp = px;
        for (int i = 0; i < Catalogo.MAX_PASIVAS; i++) {
            String id = "";
            int nivel = 0;
            if (i < cabP.size()) { id = cabP.get(i)[0]; try { nivel = Integer.parseInt(cabP.get(i)[1]); } catch (NumberFormatException ex) { nivel = 1; } }
            ranuraObjeto(g, xp, yP, id, nivel);
            int k = 0;
            for (String[] u : unidosP) if (u[2].equals(id) && k < 3) { Iconos.dibujar(g, "p_" + u[0], xp + TILE_P + 1, yP + 1 + k * 11, 10); k++; }        // los fusionados: iconos pequenos en la columna de la derecha
            xp += anchosP[i] + SEP_P;
        }

        // ---- si estas caido: aviso grande y lo que falta para que te revivan
        if (e.caido()) {
            g.fill(0, 0, W, H, 0x40A00000);
            m.push(); m.translate(W / 2f, H / 2f - 34, 0); m.scale(2.2f, 2.2f, 1f);
            g.drawCenteredTextWithShadow(tr, "¡ESTÁS CAÍDO!", 0, 0, 0xFFFF5555);
            m.pop();
            g.drawCenteredTextWithShadow(tr, e.companeros().isEmpty() ? "Esperando..." : "Un compañero puede revivirte si se queda a tu lado unos segundos", W / 2, H / 2 - 8, 0xFFFFFFFF);
            int rw = 160, rx = (W - rw) / 2, ry = H / 2 + 8;
            barra(g, rx, ry, rw, 8, e.reviveProg() / 100f, 0xFF6FE08A);
            if (e.reviveProg() > 0) g.drawCenteredTextWithShadow(tr, "Reviviendo… " + e.reviveProg() + "%", W / 2, ry + 12, 0xFF9BF0B0);
        }
        if (Boolean.getBoolean("emi.survivors.debug"))
            g.drawTextWithShadow(tr, String.format(Locale.ROOT, "Enemigos %d · gemas %d · servidor %.1f ms/tick · mod %.2f ms · FPS %d", e.enemigos(), GemasCliente.cuantas(), e.msptServidor(), e.msNuestro(), mc.getCurrentFps()),
                    6, H - 64, 0xFFB8B8C8);
    }

    /** Una ranura de ataque: marco (rosa si acaba de usarse, dorado si lo acabas de conseguir o ya evoluciono), icono, numero y puntos de nivel. */
    private static void ranuraAtaque(DrawContext g, int x, int y, int numero, Ranura r, boolean fogonazo, boolean luz) {
        boolean evo = r != null && r.nivel() >= Catalogo.NIVEL_EVO;
        int borde = r == null ? BORDE_VACIO : fogonazo ? ROSA : luz || evo ? ORO : BORDE;
        redondo(g, x, y, TILE, TILE, 5, borde);
        redondo(g, x + 2, y + 2, TILE - 4, TILE - 4, 3, r == null ? NAVY_VACIO : NAVY);
        if (r != null) {
            Iconos.dibujar(g, r.icono(), x + 5, y + 3, Iconos.CELDA);
            int llenos = Math.min(Catalogo.MAX_NIVEL, r.nivel());
            for (int k = 0; k < Catalogo.MAX_NIVEL; k++) g.fill(x + 5 + k * 5, y + 28, x + 9 + k * 5, y + 30, k < llenos ? (evo ? ORO : fogonazo ? ROSA : 0xFFFFC94A) : 0xFF3A4262);
        }
        digito(g, numero, x + 4, y + 4, r == null ? 0xFF5A6280 : fogonazo ? ROSA : 0xFFB8C0DC);
    }

    /** Una ranura de objeto: marco, icono y puntos de nivel (como las de ataque, pero mas estrecha y sin numero). */
    private static void ranuraObjeto(DrawContext g, int x, int y, String id, int nivel) {
        boolean vacia = id.isEmpty();
        redondo(g, x, y, TILE_P, TILE, 5, vacia ? BORDE_VACIO : BORDE);
        redondo(g, x + 2, y + 2, TILE_P - 4, TILE - 4, 3, vacia ? NAVY_VACIO : NAVY);
        if (vacia) return;
        Iconos.dibujar(g, "p_" + id, x + 2, y + 3, Iconos.CELDA);
        int llenos = Math.min(Catalogo.MAX_NIVEL, nivel);
        for (int k = 0; k < Catalogo.MAX_NIVEL; k++) g.fill(x + 2 + k * 5, y + 28, x + 6 + k * 5, y + 30, k < llenos ? (nivel >= Catalogo.MAX_NIVEL ? ORO : 0xFFFFC94A) : 0xFF3A4262);
    }
}
