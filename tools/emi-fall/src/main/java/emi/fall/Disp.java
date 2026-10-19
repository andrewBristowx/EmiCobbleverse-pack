package emi.fall;

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

    static final String TAG = "emi_fall";

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
        Quaternionf rot = new Quaternionf().rotateY((float) giroY);
        Vector3f tr = new Vector3f(centrado ? -sx / 2f : ax, centrado ? -sy / 2f : ay, centrado ? -sz / 2f : az).rotate(rot);
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
            return e;
        } catch (Throwable t) { EmiFall.LOG.error("No se pudo crear el bloque {}", bloque, t); return null; }
    }

    static void moverBloque(Entity e, double x, double y, double z, float sx, float sy, float sz, double giro, int interp) {
        if (e == null || e.isRemoved()) return;
        String bloque = "minecraft:stone";
        for (String tag : e.getCommandTags()) if (tag.startsWith(TAG + "_bloque:")) bloque = tag.substring(TAG.length() + 8);
        e.readNbt(nbtBloque(bloque, x, y, z, sx, sy, sz, giro, interp));
    }

    private static NbtCompound nbtBloque(String bloque, double x, double y, double z, float sx, float sy, float sz, double giro, int interp) {
        NbtCompound n = new NbtCompound();
        n.put("block_state", estado(bloque));
        NbtCompound br = new NbtCompound(); br.putInt("block", 15); br.putInt("sky", 15);
        n.put("brightness", br);
        n.put("Pos", lista(x, y, z));
        n.put("transformation", transf(0, 0, 0, sx, sy, sz, giro, true));
        if (interp > 0) { n.putInt("start_interpolation", 0); n.putInt("interpolation_duration", interp); }
        return n;
    }

    /** Un objeto (por ejemplo una Pokeball de Cobblemon) dibujado de pie y de tamaño {@code escala}, siempre de cara al jugador si {@code mirar}. */
    static Entity objeto(ServerWorld w, String item, double x, double y, double z, float escala, boolean mirar) {
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
            n.put("transformation", transf(0, 0, 0, escala, escala, escala, 0, false));
            e.readNbt(n);
            e.refreshPositionAndAngles(x, y, z, 0f, 0f);
            w.spawnEntity(e);
            return e;
        } catch (Throwable t) { EmiFall.LOG.error("No se pudo crear el objeto {}", item, t); return null; }
    }

    /** Cambia el objeto de un display de objeto (el simbolo de la pantalla). */
    static void cambiarObjeto(Entity e, String item, double x, double y, double z, float escala, boolean mirar) {
        if (e == null || e.isRemoved()) return;
        NbtCompound n = new NbtCompound();
        NbtCompound it = new NbtCompound(); it.putString("id", item); it.putInt("count", 1);
        n.put("item", it);
        n.putString("item_display", "fixed");
        n.putString("billboard", mirar ? "vertical" : "fixed");
        NbtCompound br = new NbtCompound(); br.putInt("block", 15); br.putInt("sky", 15);
        n.put("brightness", br);
        n.put("Pos", lista(x, y, z));
        n.put("transformation", transf(0, 0, 0, escala, escala, escala, 0, false));
        e.readNbt(n);
    }

    /** Un cartel de texto en el aire (el texto en JSON de Minecraft: {@code {"text":"hola","color":"gold"}}). */
    static Entity texto(ServerWorld w, String json, double x, double y, double z, float escala, boolean fondo) {
        try {
            Entity e = EntityType.TEXT_DISPLAY.create(w);
            e.addCommandTag(TAG);
            e.readNbt(nbtTexto(json, x, y, z, escala, fondo));
            e.refreshPositionAndAngles(x, y, z, 0f, 0f);
            w.spawnEntity(e);
            return e;
        } catch (Throwable t) { EmiFall.LOG.error("No se pudo crear el texto", t); return null; }
    }

    static void cambiarTexto(Entity e, String json, double x, double y, double z, float escala, boolean fondo) {
        if (e == null || e.isRemoved()) return;
        e.readNbt(nbtTexto(json, x, y, z, escala, fondo));
    }

    private static NbtCompound nbtTexto(String json, double x, double y, double z, float escala, boolean fondo) {
        NbtCompound n = new NbtCompound();
        n.putString("text", json);
        n.putString("billboard", "center");
        n.putInt("line_width", 400);
        n.putByte("see_through", (byte) 0);
        if (!fondo) n.putInt("background", 0);
        NbtCompound br = new NbtCompound(); br.putInt("block", 15); br.putInt("sky", 15);
        n.put("brightness", br);
        n.put("Pos", lista(x, y, z));
        n.put("transformation", transf(0, 0, 0, escala, escala, escala, 0, false));
        return n;
    }

    static void quitar(Entity e) { if (e != null && !e.isRemoved()) e.discard(); }

    /** Borra todos los displays de este mod que haya en un cuadrado de la dimension (por si un servidor se paro a medias). */
    static int barrer(ServerWorld w, double cx, double cz, double radio) {
        net.minecraft.util.math.Box caja = new net.minecraft.util.math.Box(cx - radio, w.getBottomY(), cz - radio, cx + radio, w.getTopY(), cz + radio);
        java.util.List<Entity> l = w.getEntitiesByClass(Entity.class, caja, e -> e.getCommandTags().contains(TAG));
        for (Entity e : l) e.discard();
        return l.size();
    }
}
