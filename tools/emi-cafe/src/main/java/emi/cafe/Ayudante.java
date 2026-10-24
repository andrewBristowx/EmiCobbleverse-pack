package emi.cafe;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;

/**
 * Un ayudante contratado: un Pokemon de Cobblemon sin IA que se queda en la cocina con un rotulo encima y hace una tarea cada cierto tiempo ({@link Cafe#tickAyudantes}):
 * el lavaplatos recoge los platos sucios de las mesas y el camarero lleva a la mesa los platos que quedan listos en las estaciones.
 */
final class Ayudante {
    enum Tipo {
        LAVA("squirtle", "Lavaplatos", 7.5, 3.5, 20 * 9, "ayud_lava"),
        MESERO("chansey", "Camarero", 9.5, 3.5, 20 * 5, "ayud_mesero");

        final String especie, nombre, mejora;
        final double x, z;
        final int cada;                  // ticks entre una tarea y la siguiente

        Tipo(String especie, String nombre, double x, double z, int cada, String mejora) { this.especie = especie; this.nombre = nombre; this.x = x; this.z = z; this.cada = cada; this.mejora = mejora; }
    }

    final Tipo tipo;
    final PokemonEntity e;
    final Entity rotulo;
    int espera;

    private Ayudante(Tipo tipo, PokemonEntity e, Entity rotulo) { this.tipo = tipo; this.e = e; this.rotulo = rotulo; this.espera = tipo.cada; }

    static Ayudante crear(ServerWorld w, int ox, Tipo t) {
        try {
            PokemonEntity e = PokemonProperties.Companion.parse(t.especie + " level=20").createEntity(w);
            e.setAiDisabled(true);
            e.setEnablePoseTypeRecalculation(false);
            e.setInvulnerable(true);
            e.setSilent(true);
            e.setNoGravity(true);
            e.noClip = true;
            e.setPersistent();
            e.setCustomNameVisible(false);
            try { e.hideNameRendering(); } catch (Throwable x) { /* version de Cobblemon sin esta funcion */ }
            e.addCommandTag(Disp.TAG);
            e.refreshPositionAndAngles(ox + t.x, Local.Y + 1, t.z, 0f, 0f);
            w.spawnEntity(e);
            e.getPokemon().setScaleModifier((float) Math.max(0.35, Math.min(3.0, 1.0 / Math.max(0.3, e.getHeight()))));
            Entity rotulo = Disp.texto(w, "{\"text\":\"" + t.nombre + "\",\"color\":\"gold\",\"bold\":true}", ox + t.x, Local.Y + 2.4, t.z, 0.9f, false);
            return new Ayudante(t, e, rotulo);
        } catch (Throwable x) {
            EmiCafe.LOG.error("No se pudo crear al ayudante {}", t, x);
            return null;
        }
    }

    void quitar() { Disp.quitar(rotulo); if (!e.isRemoved()) e.discard(); }
}
