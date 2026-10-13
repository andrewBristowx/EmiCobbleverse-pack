package emi.carreras;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

/** Estado de un participante. */
public final class Corredor {
    public final UUID id;
    public String nombre;
    public int slot = 0;                 // hueco elegido del equipo (1..6); 0 = aun no ha elegido
    public Pokemon pokemon;              // el Pokemon elegido (o prestado)
    public boolean prestado;
    public PokemonEntity visual;
    public int hueco;                    // posicion en la parrilla

    // progreso
    public int vuelta = 0;               // vueltas completadas
    public int siguiente = 1;            // siguiente punto de control a pasar (0 = meta)
    public int finTick = -1;             // tick de carrera en el que cruzo la meta final
    public int[] tiemposVuelta;
    public int ultimaVueltaTick = 0;
    public Vec3d reaparicion;
    public float reaparicionYaw;
    public boolean abandono;

    // poderes y efectos (ticks absolutos del servidor)
    public Poder poder;
    public int escudoHasta, paradoHasta, lentoHasta, turboHasta, ciegoHasta, golpeInmuneHasta, usoCooldown;
    public double lentoFactor, turboFactor;
    public double ultimoModificador = Double.NaN;
    public Vec3d previa;
    // vuelo de una plataforma de salto
    public int vueloIni, vueloTicks, saltoCooldown;
    public Vec3d vueloA, vueloB;
    public double vueloAltura;
    public boolean vueloTurbo;
    public net.minecraft.entity.Entity asiento;      // asiento invisible del salto

    public Corredor(UUID id, String nombre) { this.id = id; this.nombre = nombre; }

    /** Medida de progreso para ordenar la clasificacion (cuanto mas, mejor). */
    public double progreso(Pista pista) {
        if (finTick >= 0) return 1e9 - finTick;
        int n = pista.n();
        int idx = vuelta * n + siguiente;
        return idx * 1000.0;
    }
}
