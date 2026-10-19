package emi.huida;

import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mientras dura la huida se paran, SOLO EN MEMORIA, la limpieza de objetos («clearlag») y los eventos con aviso general de Emipokemon
 * (bosses del mundo Mega/Gigantamax/Tera, evento shiny, entrenador legendario), para que no salgan avisos ni apariciones en plena huida.
 * Todo por reflexion sobre la configuracion de Emipokemon: si el mod no esta, no hace nada. Al terminar la huida se devuelve cada valor y se
 * adelantan los temporizadores del tiempo parado (el siguiente boss no sale de golpe al acabar la huida).
 * No se escribe el archivo de configuracion de Emipokemon; solo si un organizador cambia algo de su configuracion con un comando durante
 * la huida, ese guardado se llevaria los valores parados (y Emipokemon los guardaria apagados hasta que alguien los vuelva a encender).
 */
final class Pausas {
    private Pausas() {}

    private static final String[] BANDERAS = {"itemCleanupEnabled", "arrowCleanupEnabled", "entityCapEnabled"};   // limpieza de objetos, de flechas y tope de entidades por mundo
    private static final String[] EVENTOS = {"megaBoss", "gigantamaxBoss", "teraBoss", "shinyEvent", "legendaryTrainer"};
    private static final String[][] SERVICIOS = {
            {"com.emipokemon.raid.MegaBossService"}, {"com.emipokemon.raid.GigantamaxBossService"}, {"com.emipokemon.raid.TeraBossService"},
            {"com.emipokemon.raid.ShinyEncounterService"}, {"com.emipokemon.npc.LegendaryTrainerService"}};

    private static final Map<String, Boolean> guardado = compartido("guardado");
    private static final Map<String, Long> ticksAntes = compartido("ticks");
    private static boolean pausado;                         // este mod ya esta contado en el contador compartido

    /** Se llama cada segundo: con alguien en la huida (la sala o un nivel) la limpieza y los bosses de Emipokemon estan parados; cuando no queda nadie, se reanudan. */
    static void actualizar(MinecraftServer s) {
        boolean alguien = false;
        for (var p : s.getPlayerManager().getPlayerList())
            if ("emi_huida".equals(p.getServerWorld().getRegistryKey().getValue().getNamespace())) { alguien = true; break; }
        if (alguien) pausar(); else reanudar();
    }

    static void pausar() {
        java.util.concurrent.atomic.AtomicInteger n = contador();
        synchronized (n) { pausarCon(n); }
    }

    private static void pausarCon(java.util.concurrent.atomic.AtomicInteger n) {
        if (pausado || !Cfg.pausarEventos()) return;
        try {
            Object cfg = config();
            if (cfg == null) return;
            if (n.get() > 0) { n.incrementAndGet(); pausado = true; return; }          // otro minijuego ya lo tiene parado: solo se cuenta
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
            n.set(1);
            pausado = true;
            EmiHuida.LOG.info("Huida en marcha: limpieza de objetos y eventos de Emipokemon en pausa ({})", guardado.keySet());
        } catch (Throwable t) { EmiHuida.LOG.debug("pausar eventos: {}", t.toString()); }
    }

    static void reanudar() {
        java.util.concurrent.atomic.AtomicInteger n = contador();
        synchronized (n) {
            if (!pausado) return;
            pausado = false;
            if (n.decrementAndGet() > 0) return;                                          // aun queda otro minijuego que lo necesita
            reanudarYa();
        }
    }

    private static void reanudarYa() {
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
            EmiHuida.LOG.info("Fin de la huida: limpieza de objetos y eventos de Emipokemon reanudados");
        } catch (Throwable t) { EmiHuida.LOG.warn("reanudar eventos: {}", t.toString()); }
        guardado.clear(); ticksAntes.clear();
    }

    // Varios mods nuestros (carreras, supervivientes, huida...) paran lo mismo: lo guardado y el contador se comparten por las propiedades del sistema (las clases de cada mod son distintas),
    // asi lo de Emipokemon solo se enciende de nuevo cuando ya no queda ningun minijuego que lo necesite.
    private static final String CLAVE = "emi.pausas.estado";

    @SuppressWarnings("unchecked")
    private static Map<String, Object> estado() {
        synchronized (System.getProperties()) {
            Object o = System.getProperties().get(CLAVE);
            if (!(o instanceof Map)) { o = new java.util.concurrent.ConcurrentHashMap<String, Object>(); System.getProperties().put(CLAVE, o); }
            return (Map<String, Object>) o;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Map<String, T> compartido(String nombre) {
        return (Map<String, T>) estado().computeIfAbsent(nombre, k -> java.util.Collections.synchronizedMap(new LinkedHashMap<String, Object>()));
    }

    private static java.util.concurrent.atomic.AtomicInteger contador() {
        return (java.util.concurrent.atomic.AtomicInteger) estado().computeIfAbsent("n", k -> new java.util.concurrent.atomic.AtomicInteger());
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
        } catch (Throwable t) { EmiHuida.LOG.debug("desplazar {}: {}", clase, t.toString()); }
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
