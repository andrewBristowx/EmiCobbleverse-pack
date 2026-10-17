import java.io.File; import java.lang.reflect.*; import java.net.*;
public class Prueba {
    static String estado() throws Exception { Object c = Class.forName("com.emipokemon.Emipokemon").getMethod("configManager").invoke(null); Object cfg = c.getClass().getMethod("get").invoke(c);
        return "items=" + cfg.getClass().getField("itemCleanupEnabled").get(cfg) + " cap=" + cfg.getClass().getField("entityCapEnabled").get(cfg) + " mega=" + ((Object) cfg.getClass().getField("megaBoss").get(cfg)).getClass().getField("enabled").get(cfg.getClass().getField("megaBoss").get(cfg)); }
    public static void main(String[] a) throws Exception {
        URL[] cp = new URL[a.length - 2]; for (int i = 2; i < a.length; i++) cp[i - 2] = new File(a[i]).toURI().toURL();
        ClassLoader base = new URLClassLoader(cp, Prueba.class.getClassLoader());
        ClassLoader ls = new URLClassLoader(new URL[]{new File(a[0]).toURI().toURL()}, base), lc = new URLClassLoader(new URL[]{new File(System.getenv("STUBS")).toURI().toURL(), new File(a[1]).toURI().toURL()}, base);
        Class<?> ps = Class.forName("emi.survivors.Pausas", true, ls), pc = Class.forName("emi.carreras.Pausas", true, lc);
        
        Object cfg = Class.forName("emi.carreras.Cfg", true, lc).getDeclaredConstructor().newInstance();
        Class<?> car = Class.forName("emi.carreras.Carrera", true, lc); Field f = car.getDeclaredField("cfg"); f.setAccessible(true); f.set(null, cfg);
        Method sp = ps.getDeclaredMethod("pausar"), sr = ps.getDeclaredMethod("reanudar"), cp2 = pc.getDeclaredMethod("pausar"), cr = pc.getDeclaredMethod("reanudar");
        for (Method m : new Method[]{sp, sr, cp2, cr}) m.setAccessible(true);
        System.out.println("0 inicio:            " + estado());
        sp.invoke(null); System.out.println("1 survivors pausa:   " + estado());
        cp2.invoke(null); System.out.println("2 + carreras pausa:  " + estado());
        sr.invoke(null); System.out.println("3 survivors acaba:   " + estado() + "   (debe seguir parado)");
        cr.invoke(null); System.out.println("4 carreras acaba:    " + estado() + "   (debe volver a true)");
        cp2.invoke(null); sp.invoke(null); cr.invoke(null); System.out.println("5 al reves, carreras acaba: " + estado() + " (parado)");
        sr.invoke(null); System.out.println("6 survivors acaba:   " + estado());
        sr.invoke(null); cr.invoke(null); System.out.println("7 doble reanudar:    " + estado());
    }
}
