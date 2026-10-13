package emi.carreras;

import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Datos de una pista: dnde esta, la parrilla de salida, los puntos de control, las cajas de poderes y los turbos.
 * Los genera el script de la pista (tools/emi-carreras/pista/) y se leen de data/emi_carreras/pistas/&lt;id&gt;.json
 * (o de config/emi_carreras/pistas/&lt;id&gt;.json si existe, para ajustarlos sin recompilar).
 */
public final class Pista {
    public String id;
    public String nombre = "Pista";
    public String plantilla;             // id de la plantilla de estructura (data/emi_carreras/structure/<x>.nbt); null = ya construida
    public int[] origen = {0, 64, 0};    // esquina donde se coloca la plantilla
    public String version = "1";         // cambia cuando cambia la plantilla: asi se vuelve a colocar sola
    public int vueltas = 3;
    public double[][] parrilla;          // x, y, z, yaw de cada hueco de salida (el 1.º es la pole)
    public double[] podio;               // x, y, z, yaw donde se ve el final
    /** Puntos de control en orden. [0] es la META. Cada uno: x1,y1,z1, x2,y2,z2 (zona), rx,ry,rz, ryaw (donde reaparece quien se cae). */
    public double[][] checkpoints;
    public double[][] cajas;             // x, y, z de cada caja de poderes
    public double[][] estrellas;         // x, y, z de cada michiestrellita (se cogen y reaparecen)
    public double[][] turbos;            // x1,y1,z1, x2,y2,z2 de cada franja de turbo
    public double caidaY = 40;           // por debajo de esta Y se considera caido de la pista
    /** Plataformas de salto: al pisarlas el servidor lanza al corredor en arco hasta `destino` (o `destino_turbo` si lleva turbo activo). */
    public Salto[] saltos;
    public double[][] lodo;              // x1,y1,z1, x2,y2,z2, factor de frenada (0.45 = pierde el 45 % de la velocidad)
    public double[][] voltorbs;          // x, y, z de cada Voltorb fijo (explota al tocarlo y reaparece)
    public String musica;                // id de la musica de fondo (emi_carreras:musica.<id>); null = ninguna
    public String ambiente;              // "volcan": sonidos de ambiente
    /** Mapa de la pista para la interfaz: imagen assets/emi_carreras/textures/gui/mapa_<id>.png (px x py) que cubre desde (x0,z0) del mundo, ancho x largo bloques. */
    public Mapa mapa;

    public static final class Mapa { public double x0, z0, ancho, largo; public int px, py; }

    public static final class Salto {
        public double[] zona;            // caja que activa el salto (x1,y1,z1,x2,y2,z2)
        public double[] destino;         // x,y,z de aterrizaje
        public double[] destino_turbo;   // aterrizaje si lleva turbo (null = el mismo)
        public double[] normal;          // nx,nz: eje lateral de la pista en el aterrizaje
        public double altura = 4;        // altura maxima del arco sobre la recta
        public int ticks = 30;           // duracion del vuelo
        public int ticks_turbo = 0;      // duracion del vuelo con turbo (0 = ticks)
        public transient Box caja;
    }

    private transient Box[] zonas, zonasTurbo, zonasLodo;

    public static Pista cargar(String id) {
        Gson g = new Gson();
        try {
            Path over = FabricLoader.getInstance().getConfigDir().resolve("emi_carreras").resolve("pistas").resolve(id + ".json");
            Pista p = null;
            if (Files.exists(over)) p = g.fromJson(Files.readString(over), Pista.class);
            else try (InputStream in = Pista.class.getResourceAsStream("/data/emi_carreras/pistas/" + id + ".json")) {
                if (in != null) p = g.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), Pista.class);
            }
            if (p == null) return null;
            p.id = id;
            if (p.parrilla == null || p.parrilla.length == 0 || p.checkpoints == null || p.checkpoints.length < 2) {
                EmiCarreras.LOG.error("La pista {} esta incompleta (parrilla/checkpoints)", id);
                return null;
            }
            p.zonas = new Box[p.checkpoints.length];
            for (int i = 0; i < p.zonas.length; i++) p.zonas[i] = caja(p.checkpoints[i]);
            p.zonasTurbo = new Box[p.turbos == null ? 0 : p.turbos.length];
            for (int i = 0; i < p.zonasTurbo.length; i++) p.zonasTurbo[i] = caja(p.turbos[i]);
            if (p.saltos != null) for (Salto sa : p.saltos) sa.caja = caja(sa.zona);
            p.zonasLodo = new Box[p.lodo == null ? 0 : p.lodo.length];
            for (int i = 0; i < p.zonasLodo.length; i++) p.zonasLodo[i] = caja(p.lodo[i]);
            return p;
        } catch (Exception e) {
            EmiCarreras.LOG.error("No se pudo leer la pista {}", id, e);
            return null;
        }
    }

    private static Box caja(double[] a) {
        return new Box(Math.min(a[0], a[3]), Math.min(a[1], a[4]), Math.min(a[2], a[5]),
                Math.max(a[0], a[3]) + 1, Math.max(a[1], a[4]) + 1, Math.max(a[2], a[5]) + 1);
    }

    public int n() { return checkpoints.length; }
    public Box zona(int i) { return zonas[i]; }
    public Vec3d centro(int i) { return zonas[i].getCenter(); }
    public boolean enTurbo(Vec3d p) { for (Box b : zonasTurbo) if (b.contains(p)) return true; return false; }
    /** factor de frenada del lodo bajo el jugador (0 = no esta en lodo) */
    public double lodoEn(Vec3d p) { for (int i = 0; i < zonasLodo.length; i++) if (zonasLodo[i].contains(p)) return lodo[i][6]; return 0; }
    public Salto saltoEn(Vec3d p) { if (saltos != null) for (Salto s : saltos) if (s.caja.contains(p)) return s; return null; }
    /** Como saltoEn, pero mirando todo el trayecto entre dos posiciones (la zona del borde mide un bloque y un corredor con turbo avanza casi uno por tick). */
    public Salto saltoEntre(Vec3d a, Vec3d b) {
        if (a == null) return saltoEn(b);
        for (int i = 0; i <= 4; i++) { Salto s = saltoEn(a.lerp(b, i / 4.0)); if (s != null) return s; }
        return null;
    }
}
