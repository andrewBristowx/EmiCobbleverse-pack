package emi.huida.client;

import emi.huida.net.CamaraPayload;

/**
 * Camara de las escenas (entrada con el michi demonio, michi angel, mordisco): el servidor manda la posicion y la mirada cada tick; aqui se suaviza
 * (los saltos de plano, marcados con {@code corte}, son directos), se le anade la sacudida y, al acabar la escena, se hace una transicion de ~0,7 s
 * hasta la camara normal de la huida.
 */
public final class CineCamara {
    private CineCamara() {}

    private static volatile CamaraPayload ultimo;
    private static volatile long recibido;
    private static volatile boolean cortePendiente;
    private static final double[] cur = new double[5];
    private static float curFov = 1.15f;
    private static boolean iniciada;
    private static double fade;               // 1 = camara de escena, 0 = camara normal (baja durante la transicion de salida)
    private static long antes;
    private static final boolean DEBUG = Boolean.getBoolean("emi.huida.debug");
    private static int nDbg;

    static void recibir(CamaraPayload p) {
        ultimo = p; recibido = System.currentTimeMillis();
        if (p.activo() && p.corte()) cortePendiente = true;
    }

    /** (el servidor manda la camara cada tick y avisa al terminar; el margen de 1,5 s es solo por si se pierde ese aviso, y evita que un tiron del cliente deje ver un instante la camara normal) */
    public static boolean activa() { CamaraPayload p = ultimo; return p != null && p.activo() && System.currentTimeMillis() - recibido < 1500; }
    /** Hay escena o se esta volviendo de ella. */
    public static boolean enUso() { return activa() || fade > 0; }

    public static float fov(float normal) { return enUso() ? curFov : normal; }

    static void reiniciar() { ultimo = null; iniciada = false; fade = 0; cortePendiente = false; }

    /** @param normal pose normal de la huida {x, y, z, yaw, pitch}; devuelve la que hay que usar. */
    public static double[] mezclar(double[] normal) {
        long ahora = System.nanoTime();
        double dt = antes == 0 ? 0.016 : Math.min(0.1, (ahora - antes) / 1e9);
        antes = ahora;
        CamaraPayload p = ultimo;
        if (DEBUG && activa() && (nDbg++ % 4) == 0) {
            var mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc.world != null) for (var e : mc.world.getEntities())
                if (e instanceof com.cobblemon.mod.common.entity.pokemon.PokemonEntity pe && pe.getPokemon().getSpecies().getName().equals("GatitoAlien"))
                    emi.huida.EmiHuida.LOG.info("cine michi id={} pos=({}, {}, {}) pose={} escala={} invisible={} camara=({}, {}, {}) dist={} caja={} aspectos={}", pe.getId(), (int) pe.getX(), (int) pe.getY(), (int) pe.getZ(), pe.getCurrentPoseType(), pe.getPokemon().getScaleModifier(), pe.isInvisible(), (int) cur[0], (int) cur[1], (int) cur[2], (int) Math.sqrt(pe.squaredDistanceTo(cur[0], cur[1], cur[2])), pe.getBoundingBox().getAverageSideLength(), pe.getPokemon().getAspects());
        }
        if (activa()) {
            double[] meta = {p.x(), p.y(), p.z(), p.yaw(), p.pitch()};
            if (!iniciada || cortePendiente) {
                System.arraycopy(meta, 0, cur, 0, 5); curFov = p.fov();
                iniciada = true; cortePendiente = false;
            } else {
                double k = 1 - Math.exp(-dt * 16);
                for (int i = 0; i < 3; i++) cur[i] += (meta[i] - cur[i]) * k;
                for (int i = 3; i < 5; i++) cur[i] += ang(meta[i] - cur[i]) * k;
                curFov += (p.fov() - curFov) * (float) (1 - Math.exp(-dt * 6));
            }
            fade = 1;
            double[] r = cur.clone();
            if (p.temblor() > 0.001f) {
                double t = ahora / 1e9, a = p.temblor();
                r[0] += a * (Math.sin(t * 47.0) + 0.6 * Math.sin(t * 91.0 + 1.3));
                r[1] += a * (Math.sin(t * 53.0 + 0.7) + 0.6 * Math.sin(t * 83.0));
                r[2] += a * (Math.sin(t * 41.0 + 2.1) + 0.6 * Math.sin(t * 97.0 + 0.4));
                r[3] += a * 3.0 * Math.sin(t * 61.0); r[4] += a * 3.0 * Math.sin(t * 57.0 + 1.0);
            }
            return r;
        }
        if (fade > 0 && normal != null && iniciada) {
            fade = Math.max(0, fade - dt / 0.7);
            double t = fade * fade * (3 - 2 * fade);
            double[] r = new double[5];
            for (int i = 0; i < 3; i++) r[i] = normal[i] + (cur[i] - normal[i]) * t;
            for (int i = 3; i < 5; i++) r[i] = normal[i] + ang(cur[i] - normal[i]) * t;
            curFov += (1.15f - curFov) * (float) (1 - Math.exp(-dt * 6));
            if (fade <= 0) { iniciada = false; curFov = 1.15f; }
            return r;
        }
        if (fade > 0) fade = 0;
        iniciada = false;
        return normal;
    }

    private static double ang(double d) { return ((d + 540.0) % 360.0) - 180.0; }
}
