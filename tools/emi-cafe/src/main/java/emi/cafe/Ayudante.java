package emi.cafe;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;

/**
 * Un Michialien contratado: un GatitoAlien de Cobblemon sin IA que se queda en la cocina con un rotulo con su nombre y hace una tarea cada cierto tiempo ({@link Cafe#tickAyudantes}):
 * el lavaplatos recoge los platos sucios de las mesas, el mesero lleva a la mesa los platos que quedan listos en las estaciones y el chef cocina el plato de un cliente que espera.
 */
final class Ayudante {
    final Personal.Rol rol;
    final Datos.Empleado emp;
    final PokemonEntity e;
    final Entity rotulo;
    final double x, z;
    int espera;
    // el chef: lo que esta cocinando ahora
    Mesa mesaChef;
    Recetas.Plato platoChef;
    int restante;

    private Ayudante(Personal.Rol rol, Datos.Empleado emp, PokemonEntity e, Entity rotulo, double x, double z) {
        this.rol = rol; this.emp = emp; this.e = e; this.rotulo = rotulo; this.x = x; this.z = z; this.espera = rol.cada;
    }

    private static String json(String nombre, String rol, String extra) {
        return "{\"text\":\"" + nombre + "\",\"color\":\"gold\",\"bold\":true,\"extra\":[{\"text\":\" · " + rol + (extra.isEmpty() ? "" : " · " + extra) + "\",\"color\":\"gray\",\"bold\":false}]}";
    }

    static Ayudante crear(ServerWorld w, int ox, Datos.Empleado emp, int idx, int ancho) {
        Personal.Rol rol = Personal.Rol.de(emp.rol);
        if (rol == null) return null;
        String aspecto = Personal.ASPECTOS[Math.floorMod(emp.aspecto, Personal.ASPECTOS.length)][0];
        double x = Math.min(ancho - 1.5, 3.0 + 3.0 * idx), z = 3.5;
        try {
            PokemonEntity e = PokemonProperties.Companion.parse("gatitoalien " + aspecto + " level=20").createEntity(w);
            e.setAiDisabled(true);
            e.setEnablePoseTypeRecalculation(false);
            e.setInvulnerable(true);
            e.setSilent(true);
            e.setNoGravity(true);
            e.noClip = true;
            e.setPersistent();
            e.setCustomNameVisible(false);
            try { e.hideNameRendering(); } catch (Throwable t) { /* version de Cobblemon sin esta funcion */ }
            e.addCommandTag(Disp.TAG);
            e.refreshPositionAndAngles(ox + x, Local.Y + 1, z, 0f, 0f);
            w.spawnEntity(e);
            e.getPokemon().setScaleModifier((float) Math.max(0.35, Math.min(3.0, 1.0 / Math.max(0.3, e.getHeight()))));
            Entity rotulo = Disp.texto(w, json(emp.nombre, rol.nombre, ""), ox + x, Local.Y + 2.4, z, 0.9f, false);
            return new Ayudante(rol, emp, e, rotulo, x, z);
        } catch (Throwable t) {
            EmiCafe.LOG.error("No se pudo crear al empleado {}", emp.nombre, t);
            return null;
        }
    }

    /** Cambia lo que dice el rotulo (el chef pone «cocina: pizza» mientras trabaja). */
    void rotular(String extra) {
        if (rotulo == null || rotulo.isRemoved()) return;
        Disp.cambiarTexto(rotulo, json(emp.nombre, rol.nombre, extra), e.getX(), Local.Y + 2.4, e.getZ(), 0.9f, false);
    }

    void quitar() { Disp.quitar(rotulo); if (!e.isRemoved()) e.discard(); }
}
