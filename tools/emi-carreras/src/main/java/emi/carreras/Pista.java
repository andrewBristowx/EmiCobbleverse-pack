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
    public double[][] turbos;            // x1,y1,z1, x2,y2,z2 de cada franja de turbo
    public double caidaY = 40;           // por debajo de esta Y se considera caido de la pista

    private transient Box[] zonas, zonasTurbo;

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
}
