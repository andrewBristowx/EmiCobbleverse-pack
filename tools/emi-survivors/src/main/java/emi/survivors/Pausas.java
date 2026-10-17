package emi.survivors;

import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mientras haya alguien en el minijuego (sala o partida) se paran, SOLO EN MEMORIA, la limpieza de objetos («clearlag») y los eventos con aviso general de Emipokemon
 * (bosses del mundo Mega/Gigantamax/Tera, evento shiny, entrenador legendario), para que no salgan avisos ni apariciones en plena partida.
 * Todo por reflexion sobre la configuracion de Emipokemon: si el mod no esta, no hace nada. Al terminar la partida se devuelve cada valor y se
 * adelantan los temporizadores del tiempo parado (el siguiente boss no sale de golpe al acabar la huida).
 * No se escribe el archivo de configuracion de Emipokemon; solo si un organizador cambia algo de su configuracion con un comando durante
 * la huida, ese guardado se llevaria los valores parados (y Emipokemon los guardaria apagados hasta que alguien los vuelva a encender).
 */
final class Pausas {
    private Pausas() {}

    private static final String[] BANDERAS = {"itemCleanupEnabled", "arrowCleanupEnabled", "entityCapEnabled"};   // limpieza de objetos, de flechas y tope de entidades por mundo (este borra las que sobran)
    private static final String[] EVENTOS = {"megaBoss", "gigantamaxBoss", "teraBoss", "shinyEvent", "legendaryTrainer"};
    private static final String[][] SERVICIOS = {
            {"com.emipokemon.raid.MegaBossService"}, {"com.emipokemon.raid.GigantamaxBossService"}, {"com.emipokemon.raid.TeraBossService"},
            {"com.emipokemon.raid.ShinyEncounterService"}, {"com.emipokemon.npc.LegendaryTrainerService"}};

    private static final Map<String, Boolean> guardado = new LinkedHashMap<>();
    private static final Map<String, Long> ticksAntes = new LinkedHashMap<>();
    private static boolean pausado;
    private static boolean sinEmipokemon;                   // si el mod no esta instalado no se vuelve a buscar cada segundo

    /**
     * Se llama cada segundo: mientras haya alguien en el minijuego (la sala o cualquier arena) la limpieza y los bosses de Emipokemon estan parados;
     * cuando no queda nadie, se reanudan.
     */
    static void actualizar(MinecraftServer s) {
        boolean alguien = false;
        for (var p : s.getPlayerManager().getPlayerList())
            if ("emi_survivors".equals(p.getServerWorld().getRegistryKey().getValue().getNamespace())) { alguien = true; break; }
        if (alguien) pausar(); else reanudar();
    }

    static synchronized void pausar() {
        if (pausado || sinEmipokemon) return;
        try {
            Object cfg = config();
            if (cfg == null) return;
            guardado.clear(); ticksAntes.clear();
            for (String f : BANDERAS) poner(cfg, cfg, f, false, f);
            for (String e : EVENTOS) {
                Object sub = campo(cfg, e);
                if (sub != null) poner(cfg, sub, "enabled", false, e + ".enabled");
            }
            for (String[] s : SERVICIOS) {
                Long t = ticks(s[0]);
                if (t != null) ticksAntes.put(s[0], t);
            }
            pausado = true;
            EmiSurvivors.LOG.info("Hay jugadores en el minijuego: limpieza de objetos y eventos de Emipokemon en pausa ({})", guardado.keySet());
        } catch (ClassNotFoundException t) { sinEmipokemon = true;
        } catch (Throwable t) { EmiSurvivors.LOG.debug("pausar eventos: {}", t.toString()); }
    }

    static synchronized void reanudar() {
        if (!pausado) return;
        pausado = false;
        try {
            Object cfg = config();
            if (cfg != null) {
                for (var en : guardado.entrySet()) {
                    String k = en.getKey();
                    if (k.contains(".")) { Object sub = campo(cfg, k.substring(0, k.indexOf('.'))); if (sub != null) fijar(sub, "enabled", en.getValue()); }
                    else fijar(cfg, k, en.getValue());
                }
            }
            for (String[] s : SERVICIOS) {
                Long antes = ticksAntes.get(s[0]), ahora = ticks(s[0]);
                if (antes != null && ahora != null && ahora > antes) desplazar(s[0], ahora - antes);
            }
            EmiSurvivors.LOG.info("Ya no queda nadie en el minijuego: limpieza de objetos y eventos de Emipokemon reanudados");
        } catch (Throwable t) { EmiSurvivors.LOG.warn("reanudar eventos: {}", t.toString()); }
        guardado.clear(); ticksAntes.clear();
    }

    // ------------------------------------------------------------------ reflexion

    private static Object config() throws Exception {
        Class<?> em = Class.forName("com.emipokemon.Emipokemon");
        Object cm = em.getMethod("configManager").invoke(null);
        return cm == null ? null : cm.getClass().getMethod("get").invoke(cm);
    }

    private static Object campo(Object o, String nombre) {
        try { Field f = o.getClass().getField(nombre); return f.get(o); } catch (Throwable t) { return null; }
    }

    private static void poner(Object cfg, Object en, String nombre, boolean valor, String clave) {
        try {
            Field f = en.getClass().getField(nombre);
            if (f.getType() != boolean.class) return;
            boolean antes = f.getBoolean(en);
            if (!antes) return;                       // ya estaba apagado: no se toca (ni se enciende luego)
            guardado.put(clave, antes);
            f.setBoolean(en, valor);
        } catch (Throwable t) { /* ese ajuste no existe en esta version */ }
    }

    private static void fijar(Object en, String nombre, boolean valor) {
        try { en.getClass().getField(nombre).setBoolean(en, valor); } catch (Throwable t) { }
    }

    /** `serverTicks` (estatico) del servicio, si lo tiene: sirve para saber cuanto tiempo ha estado parado. */
    private static Long ticks(String clase) {
        try {
            Field f = Class.forName(clase).getDeclaredField("serverTicks");
            f.setAccessible(true);
            return f.getLong(null);
        } catch (Throwable t) { return null; }
    }

    /** Adelanta el siguiente evento y el boss activo (si hay) el tiempo que ha estado en pausa. */
    private static void desplazar(String clase, long dt) {
        try {
            Class<?> c = Class.forName(clase);
            sumar(null, c, "nextSpawnTick", dt, true);
            Field fa = null;
            try { fa = c.getDeclaredField("active"); fa.setAccessible(true); } catch (Throwable t) { }
            Object activo = fa == null ? null : fa.get(null);
            if (activo != null) for (String n : new String[]{"expireTick", "lastReminderTick"}) sumar(activo, activo.getClass(), n, dt, false);
        } catch (Throwable t) { EmiSurvivors.LOG.debug("desplazar {}: {}", clase, t.toString()); }
    }

    private static void sumar(Object o, Class<?> c, String nombre, long dt, boolean soloSiPositivo) {
        try {
            Field f = c.getDeclaredField(nombre);
            f.setAccessible(true);
            if (f.getType() != long.class) return;
            long v = f.getLong(o);
            if (soloSiPositivo && v < 0) return;      // -1 = aun sin programar
            f.setLong(o, v + dt);
        } catch (Throwable t) { /* campo inexistente en esta version */ }
    }
}
