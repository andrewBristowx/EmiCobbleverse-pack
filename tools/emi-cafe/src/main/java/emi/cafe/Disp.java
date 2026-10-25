package emi.cafe;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Entidades «display» de Minecraft (bloques, objetos y textos que el servidor coloca y mueve; los clientes sin ningun mod las ven): obstaculos que se mueven,
 * el simbolo de la pantalla, los carteles. Cada actualizacion repite el NBT completo (un {@code block_display} sin su bloque se vuelve de aire).
 */
final class Disp {
    private Disp() {}

    static final String TAG = "emi_cafe";

    /** Los displays que el mod controla ahora mismo: cualquier otro display del mod (de partidas anteriores o de una version vieja) es una copia congelada y se barre. */
    private static final java.util.Set<java.util.UUID> VIVOS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static NbtList lista(double... v) { NbtList l = new NbtList(); for (double d : v) l.add(NbtDouble.of(d)); return l; }
    private static NbtList listaF(float... v) { NbtList l = new NbtList(); for (float d : v) l.add(NbtFloat.of(d)); return l; }

    private static NbtCompound estado(String spec) {
        NbtCompound bs = new NbtCompound();
        int i = spec.indexOf('[');
        bs.putString("Name", i < 0 ? spec : spec.substring(0, i));
        if (i >= 0) {
            NbtCompound props = new NbtCompound();
            for (String kv : spec.substring(i + 1, spec.length() - 1).split(",")) { String[] a = kv.split("="); if (a.length == 2) props.putString(a[0], a[1]); }
            bs.put("Properties", props);
        }
        return bs;
    }

    private static NbtCompound transf(float ax, float ay, float az, float sx, float sy, float sz, double giroY, boolean centrado) {
        return transf(ax, ay, az, sx, sy, sz, giroY, centrado, 0f, 0f);
    }

    /** {@code dy}: desplazamiento vertical extra (en el espacio del mundo); {@code rollZ}: giro en el plano del dibujo (para una fruta que rueda). */
    private static NbtCompound transf(float ax, float ay, float az, float sx, float sy, float sz, double giroY, boolean centrado, float dy, float rollZ) {
        Quaternionf rot = new Quaternionf().rotateY((float) giroY).rotateZ(rollZ);
        Vector3f tr = new Vector3f(centrado ? -sx / 2f : ax, (centrado ? -sy / 2f : ay) + dy, centrado ? -sz / 2f : az).rotate(rot);
        NbtCompound t = new NbtCompound();
        t.put("translation", listaF(tr.x, tr.y, tr.z));
        t.put("left_rotation", listaF(rot.x, rot.y, rot.z, rot.w));
        t.put("right_rotation", listaF(0f, 0f, 0f, 1f));
        t.put("scale", listaF(sx, sy, sz));
        return t;
    }

    /** Un bloque suelto de tamaño (sx, sy, sz) centrado en (x, y, z) y girado {@code giro} radianes alrededor de la vertical. {@code interp} > 0 = el cliente anima el cambio. */
    static Entity bloque(ServerWorld w, String bloque, double x, double y, double z, float sx, float sy, float sz, double giro, int interp) {
        try {
            Entity e = EntityType.BLOCK_DISPLAY.create(w);
            e.addCommandTag(TAG);
            e.addCommandTag(TAG + "_bloque:" + bloque);
            e.readNbt(nbtBloque(bloque, x, y, z, sx, sy, sz, giro, interp));
            e.refreshPositionAndAngles(x, y, z, 0f, 0f);
            w.spawnEntity(e);
            VIVOS.add(e.getUuid());
            return e;
        } catch (Throwable t) { EmiCafe.LOG.error("No se pudo crear el bloque {}", bloque, t); return null; }
    }

    static void moverBloque(Entity e, double x, double y, double z, float sx, float sy, float sz, double giro, int interp) {
        if (e == null || e.isRemoved()) return;
        String bloque = "minecraft:stone";
        for (String tag : e.getCommandTags()) if (tag.startsWith(TAG + "_bloque:")) bloque = tag.substring(TAG.length() + 8);
        e.readNbt(nbtBloque(bloque, x, y, z, sx, sy, sz, giro, interp));
    }

    /** Como {@link #moverBloque} pero cambiando tambien de bloque (la bomba que parpadea entre TNT y blanco). */
    static void cambiarBloque(Entity e, String bloque, double x, double y, double z, float sx, float sy, float sz, double giro, int interp) {
        if (e == null || e.isRemoved()) return;
        for (String tag : new java.util.ArrayList<>(e.getCommandTags())) if (tag.startsWith(TAG + "_bloque:")) e.removeCommandTag(tag);
        e.addCommandTag(TAG + "_bloque:" + bloque);
        e.readNbt(nbtBloque(bloque, x, y, z, sx, sy, sz, giro, interp));
    }

    private static NbtCompound nbtBloque(String bloque, double x, double y, double z, float sx, float sy, float sz, double giro, int interp) {
        NbtCompound n = new NbtCompound();
        n.put("block_state", estado(bloque));
        NbtCompound br = new NbtCompound(); br.putInt("block", 15); br.putInt("sky", 15);
        n.put("brightness", br);
        n.put("Pos", lista(x, y, z));
        n.put("transformation", transf(0, 0, 0, sx, sy, sz, giro, true));
        // sin interpolacion del cliente (el parametro interp se ignora): cada cambio se ve tal cual y no depende de como anime cada cliente las entidades
        return n;
    }

    /** Un objeto (por ejemplo una Pokeball de Cobblemon) dibujado de pie y de tamaño {@code escala}, siempre de cara al jugador si {@code mirar}. */
    static Entity objeto(ServerWorld w, String item, double x, double y, double z, float escala, boolean mirar) { return objeto(w, item, x, y, z, escala, mirar, 0f); }

    /** {@code dz}: lo adelanta hacia quien lo mira (con {@code mirar}), para poner un dibujo delante de otro. */
    static Entity objeto(ServerWorld w, String item, double x, double y, double z, float escala, boolean mirar, float dz) {
        try {
            Entity e = EntityType.ITEM_DISPLAY.create(w);
            e.addCommandTag(TAG);
            NbtCompound n = new NbtCompound();
            NbtCompound it = new NbtCompound(); it.putString("id", item); it.putInt("count", 1);
            n.put("item", it);
            n.putString("item_display", "fixed");
            n.putString("billboard", mirar ? "vertical" : "fixed");
            NbtCompound br = new NbtCompound(); br.putInt("block", 15); br.putInt("sky", 15);
            n.put("brightness", br);
            n.put("Pos", lista(x, y, z));
            n.put("transformation", transf(0, 0, dz, escala, escala, escala, 0, false));
            e.readNbt(n);
            e.refreshPositionAndAngles(x, y, z, 0f, 0f);
            w.spawnEntity(e);
            VIVOS.add(e.getUuid());
            return e;
        } catch (Throwable t) { EmiCafe.LOG.error("No se pudo crear el objeto {}", item, t); return null; }
    }

    /** Un objeto grande que siempre mira al jugador y gira en su plano ({@code roll} radianes): la fruta que rueda. */
    static Entity fruta(ServerWorld w, String item, double x, double y, double z, float escala, double roll) {
        try {
            Entity e = EntityType.ITEM_DISPLAY.create(w);
            e.addCommandTag(TAG);
            e.addCommandTag(TAG + "_item:" + item);
            e.readNbt(nbtFruta(item, x, y, z, escala, roll));
            e.refreshPositionAndAngles(x, y, z, 0f, 0f);
            w.spawnEntity(e);
            VIVOS.add(e.getUuid());
            return e;
        } catch (Throwable t) { EmiCafe.LOG.error("No se pudo crear la fruta {}", item, t); return null; }
    }

    static void moverFruta(Entity e, double x, double y, double z, float escala, double roll) {
        if (e == null || e.isRemoved()) return;
        String item = "minecraft:apple";
        for (String tag : e.getCommandTags()) if (tag.startsWith(TAG + "_item:")) item = tag.substring(TAG.length() + 6);
        e.readNbt(nbtFruta(item, x, y, z, escala, roll));
    }

    private static NbtCompound nbtFruta(String item, double x, double y, double z, float escala, double roll) {
        NbtCompound n = new NbtCompound();
        NbtCompound it = new NbtCompound(); it.putString("id", item); it.putInt("count", 1);
        n.put("item", it);
        n.putString("item_display", "fixed");
        n.putString("billboard", "vertical");
        NbtCompound br = new NbtCompound(); br.putInt("block", 15); br.putInt("sky", 15);
        n.put("brightness", br);
        n.put("Pos", lista(x, y, z));
        n.put("transformation", transf(0, 0, 0, escala, escala, escala, 0, false, 0f, (float) roll));
        return n;
    }

    /** Cambia el objeto de un display de objeto (el simbolo de la pantalla). */
    static void cambiarObjeto(Entity e, String item, double x, double y, double z, float escala, boolean mirar) { cambiarObjeto(e, item, x, y, z, escala, mirar, 0f); }

    static void cambiarObjeto(Entity e, String item, double x, double y, double z, float escala, boolean mirar, float dz) {
        if (e == null || e.isRemoved()) return;
        NbtCompound n = new NbtCompound();
        NbtCompound it = new NbtCompound(); it.putString("id", item); it.putInt("count", 1);
        n.put("item", it);
        n.putString("item_display", "fixed");
        n.putString("billboard", mirar ? "vertical" : "fixed");
        NbtCompound br = new NbtCompound(); br.putInt("block", 15); br.putInt("sky", 15);
        n.put("brightness", br);
        n.put("Pos", lista(x, y, z));
        n.put("transformation", transf(0, 0, dz, escala, escala, escala, 0, false));
        e.readNbt(n);
    }

    /** Un cartel de texto en el aire: {@code texto} y su {@code color} (nombre de color de Minecraft, por ejemplo «gold»); en negrita si {@code negrita}. */
    static Entity texto(ServerWorld w, String json, double x, double y, double z, float escala, boolean fondo) {
        try {
            Entity e = EntityType.TEXT_DISPLAY.create(w);
            e.addCommandTag(TAG);
            e.readNbt(nbtTexto(json, x, y, z, escala, fondo));
            e.refreshPositionAndAngles(x, y, z, 0f, 0f);
            w.spawnEntity(e);
            return e;
        } catch (Throwable t) { EmiCafe.LOG.error("No se pudo crear el texto", t); return null; }
    }

    static void cambiarTexto(Entity e, String json, double x, double y, double z, float escala, boolean fondo) {
        if (e == null || e.isRemoved()) return;
        e.readNbt(nbtTexto(json, x, y, z, escala, fondo));
    }

    /** El texto en el formato antiguo «{"text":"hola","color":"gold","bold":true}» (solo estas tres claves) pasado al NBT que lee el juego. */
    private static NbtCompound componente(String json) {
        NbtCompound c = new NbtCompound();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"(text|color)\":\"([^\"]*)\"").matcher(json);
        while (m.find()) c.putString(m.group(1), m.group(2));
        if (json.contains("\"bold\":true")) c.putBoolean("bold", true);
        return c;
    }

    private static NbtCompound nbtTexto(String json, double x, double y, double z, float escala, boolean fondo) {
        NbtCompound n = new NbtCompound();
        n.putString("text", json);                                    // en 1.21.1 el texto del display es una cadena JSON (la forma compuesta de NBT es de versiones posteriores)
        n.putString("billboard", "center");
        n.putString("alignment", "center");                           // sin esta clave 1.21.1 escribe «Display entityNot a string» en el registro en cada cambio
        n.putInt("line_width", 400);
        n.putByte("see_through", (byte) 0);
        if (!fondo) n.putInt("background", 0);
        NbtCompound br = new NbtCompound(); br.putInt("block", 15); br.putInt("sky", 15);
        n.put("brightness", br);
        n.put("Pos", lista(x, y, z));
        n.put("transformation", transf(0, 0, 0, escala, escala, escala, 0, false));
        return n;
    }

    private record Plato(Entity plato, double dy) {}
    /** El plato blanco que hay debajo de una comida emplatada (comida -> plato): quitarla o moverla toca tambien el plato. */
    private static final java.util.Map<Entity, Plato> PLATOS = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /**
     * Una comida servida «emplatada» (desde 0.18.0): el dibujo de la comida, mas pequeño y un poco más alto, delante de un plato blanco. Devuelve el dibujo de la comida; se usa igual que
     * {@link #objeto} ({@link #quitar} y {@link #mover} se llevan tambien el plato).
     */
    static Entity emplatado(ServerWorld w, String item, double x, double y, double z, float escala, boolean mirar) {
        double dy = -0.1 * escala;
        Entity plato = objeto(w, CafeItems.id(CafeItems.PLATO_LIMPIO), x, y + dy, z, escala * 1.2f, mirar, 0f);
        Entity comida = objeto(w, item, x, y + 0.08 * escala, z, escala * 0.7f, mirar, 0.03f);
        if (comida != null && plato != null) PLATOS.put(comida, new Plato(plato, dy - 0.08 * escala));
        return comida;
    }

    /** Mueve un dibujo (y el plato de debajo si es una comida emplatada). */
    static void mover(Entity e, double x, double y, double z) {
        if (e == null || e.isRemoved()) return;
        e.setPosition(x, y, z);
        Plato p = PLATOS.get(e);
        if (p != null && !p.plato().isRemoved()) p.plato().setPosition(x, y + p.dy(), z);
    }

    static void quitar(Entity e) {
        if (e == null) return;
        Plato p = PLATOS.remove(e);
        if (p != null) quitar(p.plato());
        VIVOS.remove(e.getUuid());
        if (!e.isRemoved()) e.discard();
    }

    /** Borra los displays de bloque y de objeto del mod que ya no controla nadie (copias congeladas); deja los carteles de texto. Devuelve cuantos borro. */
    static int barrerHuerfanos(ServerWorld w, double cx, double cz, double radio) {
        net.minecraft.util.math.Box caja = new net.minecraft.util.math.Box(cx - radio, w.getBottomY(), cz - radio, cx + radio, w.getTopY(), cz + radio);
        java.util.List<Entity> l = w.getEntitiesByClass(Entity.class, caja, e -> e.getCommandTags().contains(TAG) && e.getType() != EntityType.TEXT_DISPLAY && !VIVOS.contains(e.getUuid()));
        for (Entity e : l) e.discard();
        return l.size();
    }

    /** Borra los displays de bloques y de objetos (lo que se mueve: barras, frutas, bombas, yunques...) de este mod en un cuadrado; deja los carteles de texto. */
    static int barrerMoviles(ServerWorld w, double cx, double cz, double radio) {
        net.minecraft.util.math.Box caja = new net.minecraft.util.math.Box(cx - radio, w.getBottomY(), cz - radio, cx + radio, w.getTopY(), cz + radio);
        java.util.List<Entity> l = w.getEntitiesByClass(Entity.class, caja, e -> e.getCommandTags().contains(TAG) && !(e.getType() == EntityType.TEXT_DISPLAY));
        for (Entity e : l) e.discard();
        return l.size();
    }

    /** Borra los displays y los Pokemon de este mod de una parcela entera (la de origen {@code ox}), sin tocar a los vecinos. */
    static int barrerParcela(ServerWorld w, int ox) {
        net.minecraft.util.math.Box caja = new net.minecraft.util.math.Box(ox + Ciudad.X0, w.getBottomY(), Granja.Z_MURO, ox + Ciudad.X1 + 1, w.getTopY(), Local.Z_FRENTE + 1);
        java.util.List<Entity> l = w.getEntitiesByClass(Entity.class, caja, e -> e.getCommandTags().contains(TAG));
        for (Entity e : l) e.discard();
        return l.size();
    }

    /** Como {@link #barrerParcela} pero deja los carteles de texto (el nombre sobre la puerta se queda cuando el dueño se va). */
    static int barrerParcelaMoviles(ServerWorld w, int ox) {
        net.minecraft.util.math.Box caja = new net.minecraft.util.math.Box(ox + Ciudad.X0, w.getBottomY(), Granja.Z_MURO, ox + Ciudad.X1 + 1, w.getTopY(), Local.Z_FRENTE + 1);
        java.util.List<Entity> l = w.getEntitiesByClass(Entity.class, caja, e -> e.getCommandTags().contains(TAG) && e.getType() != EntityType.TEXT_DISPLAY);
        for (Entity e : l) e.discard();
        return l.size();
    }

    /** Borra todos los displays de este mod que haya en un cuadrado de la dimension (por si un servidor se paro a medias). */
    static int barrer(ServerWorld w, double cx, double cz, double radio) {
        net.minecraft.util.math.Box caja = new net.minecraft.util.math.Box(cx - radio, w.getBottomY(), cz - radio, cx + radio, w.getTopY(), cz + radio);
        java.util.List<Entity> l = w.getEntitiesByClass(Entity.class, caja, e -> e.getCommandTags().contains(TAG));
        for (Entity e : l) e.discard();
        return l.size();
    }
}
