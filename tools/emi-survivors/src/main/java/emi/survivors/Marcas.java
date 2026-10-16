package emi.survivors;

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
 * Marcas de aviso en el suelo: entidades {@code block_display} planas (un bloque de cristal o de alfombra estirado) que se ven desde la camara cenital MUCHO mejor
 * que las particulas, y las ve cualquier cliente aunque no tenga el mod. Se usan para avisar de los ataques de los jefes y de las mareas: un cristal rojo marca la
 * zona peligrosa, una alfombra roja la va rellenando mientras corre el tiempo del aviso (el cliente anima el crecimiento solo) y un cristal verde marca el sitio seguro.
 * Todas llevan la etiqueta {@code emi_survivors}: si el servidor se para en pleno ataque, {@link Partida#init} las borra al volver a arrancar.
 * Lo que se dibuja y lo que hace dano lo calcula el servidor aparte: aqui solo se pone la forma, que SIEMPRE es un rectangulo (el dano se comprueba con el mismo rectangulo).
 */
final class Marcas {
    private Marcas() {}

    static final String CRISTAL_ROJO = "minecraft:red_stained_glass", ALFOMBRA_ROJA = "minecraft:red_carpet";
    static final String CRISTAL_VERDE = "minecraft:lime_stained_glass", ALFOMBRA_VERDE = "minecraft:lime_carpet";
    static final String CRISTAL_NARANJA = "minecraft:orange_stained_glass", ALFOMBRA_NARANJA = "minecraft:orange_carpet";
    /** Altura sobre el suelo de la arena (los pies de los jugadores estan en {@link Arena#Y0}). */
    static final double Y_FONDO = Arena.Y0 + 0.03, Y_RELLENO = Arena.Y0 + 0.05;

    /**
     * Crea una marca. {@code (x, z)} = centro del rectangulo (o, con {@code extremo}, el centro de su lado de ATRAS: asi un rectangulo largo sale desde un punto, como un
     * rayo o una carga); {@code giro} = angulo en radianes alrededor de la vertical con el que el eje «largo» (Z) apunta hacia {@code (sin giro, cos giro)};
     * {@code ancho} es la medida en el eje X, {@code alto} el grosor y {@code largo} la medida en el eje Z.
     */
    static Entity crear(Equipo eq, String bloque, double x, double y, double z, double giro, float ancho, float alto, float largo, boolean extremo) {
        try {
            Entity e = EntityType.BLOCK_DISPLAY.create(eq.w);
            e.addCommandTag("emi_survivors");
            e.addCommandTag("emi_survivors_bloque:" + bloque);            // BlockDisplay.readNbt pone «aire» si el NBT no lleva el bloque: se repite en cada cambio
            NbtCompound n = nbt(bloque, x, y, z, giro, ancho, alto, largo, extremo, 0);
            e.readNbt(n);
            e.refreshPositionAndAngles(x, y, z, 0f, 0f);
            eq.w.spawnEntity(e);
            eq.marcas.add(e);
            return e;
        } catch (Throwable t) {
            EmiSurvivors.LOG.error("No se pudo crear la marca {}", bloque, t);
            return null;
        }
    }

    /** Cambia la forma y el sitio de una marca. Con {@code interp} > 0 el cliente anima el cambio durante esos ticks (para que crezca o gire suave). */
    static void poner(Entity e, double x, double y, double z, double giro, float ancho, float alto, float largo, boolean extremo, int interp) {
        if (e == null || e.isRemoved()) return;
        String bloque = "minecraft:red_stained_glass";
        for (String tag : e.getCommandTags()) if (tag.startsWith("emi_survivors_bloque:")) bloque = tag.substring("emi_survivors_bloque:".length());
        e.readNbt(nbt(bloque, x, y, z, giro, ancho, alto, largo, extremo, interp));
    }

    /** Quita una marca del mundo y de la lista del equipo. */
    static void quitar(Equipo eq, Entity e) {
        if (e == null) return;
        eq.marcas.remove(e);
        if (!e.isRemoved()) e.discard();
    }

    static void quitarTodas(Equipo eq) {
        for (Entity e : eq.marcas) if (!e.isRemoved()) e.discard();
        eq.marcas.clear();
    }

    private static NbtCompound nbt(String bloque, double x, double y, double z, double giro, float ancho, float alto, float largo, boolean extremo, int interp) {
        NbtCompound n = new NbtCompound();
        NbtCompound bs = new NbtCompound();
        bs.putString("Name", bloque);
        n.put("block_state", bs);
        NbtCompound br = new NbtCompound(); br.putInt("block", 15); br.putInt("sky", 15);
        n.put("brightness", br);                                                       // a pleno brillo: se ve igual de dia y de noche
        n.put("Pos", lista(x, y, z));
        Quaternionf rot = new Quaternionf().rotateY((float) giro);
        Vector3f tr = new Vector3f(-ancho / 2f, 0f, extremo ? 0f : -largo / 2f).rotate(rot);   // el bloque ocupa [0,1]^3 antes de escalar: se centra en X (y en Z salvo en un extremo)
        NbtCompound t = new NbtCompound();
        t.put("translation", listaF(tr.x, tr.y, tr.z));
        t.put("left_rotation", listaF(rot.x, rot.y, rot.z, rot.w));
        t.put("right_rotation", listaF(0f, 0f, 0f, 1f));
        t.put("scale", listaF(ancho, alto, largo));
        n.put("transformation", t);
        if (interp > 0) { n.putInt("start_interpolation", 0); n.putInt("interpolation_duration", interp); }
        return n;
    }

    private static NbtList lista(double... v) { NbtList l = new NbtList(); for (double d : v) l.add(NbtDouble.of(d)); return l; }
    private static NbtList listaF(float... v) { NbtList l = new NbtList(); for (float d : v) l.add(NbtFloat.of(d)); return l; }

    // ------------------------------------------------------------------------------------------------ geometria de los rectangulos (para comprobar el dano)

    /**
     * ¿Esta el punto (px, pz) dentro del rectangulo que sale de (x, z) en la direccion {@code giro} (el mismo de {@link #crear} con {@code extremo}), de {@code largo}
     * hacia delante y {@code ancho} de lado a lado? {@code margen} agranda un poco el rectangulo (el radio del jugador).
     */
    static boolean dentroDeRayo(double px, double pz, double x, double z, double giro, double ancho, double largo, double margen) {
        double dx = px - x, dz = pz - z, fx = Math.sin(giro), fz = Math.cos(giro);
        double u = dx * fx + dz * fz, v = dx * fz - dz * fx;                  // u = hacia delante, v = hacia un lado
        return u >= -margen && u <= largo + margen && Math.abs(v) <= ancho / 2 + margen;
    }

    /** ¿Esta el punto dentro del cuadrado de lado {@code lado} centrado en (cx, cz) y sin girar? */
    static boolean dentroDeCuadrado(double px, double pz, double cx, double cz, double lado, double margen) {
        return Math.abs(px - cx) <= lado / 2 + margen && Math.abs(pz - cz) <= lado / 2 + margen;
    }
}
