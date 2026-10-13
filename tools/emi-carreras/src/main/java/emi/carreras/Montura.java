package emi.carreras;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonPropertyExtractor;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * La "montura" de un corredor: el jugador va invisible y a pie (misma velocidad para todos, fijada por atributos) y su Pokemon
 * es una copia sin IA que se coloca encima cada tick. Asi sirve cualquier especie (en Cobblemon solo ~115 se pueden montar) y
 * el servidor no necesita nada en el cliente.
 */
public final class Montura {
    private static final Identifier ID_VEL = Identifier.of("emi_carreras", "velocidad_fija");
    private static final Identifier ID_SALTO = Identifier.of("emi_carreras", "sin_salto");
    private static final Identifier ID_ESCALON = Identifier.of("emi_carreras", "escalon");
    private static final Identifier ID_EFECTO = Identifier.of("emi_carreras", "efecto");

    /** Altura (en bloques) a la que se normalizan todos los Pokemon para que ninguno tape la vista ni sea minusculo. */
    public static float alturaBase = 1.3f;

    /** Una especie vale si camina y no vuela (ni es de tipo Volador). */
    public static boolean esTerrestre(Pokemon p) {
        Species sp = p.getSpecies();
        var mv = sp.getBehaviour().getMoving();
        if (!mv.getWalk().getCanWalk() || mv.getFly().getCanFly()) return false;
        for (var t : sp.getTypes()) if ("flying".equals(t.getName())) return false;
        return true;
    }

    /** Equipo del jugador (solo los que valen), con su posicion 1..6 en el equipo. */
    public static List<Candidato> candidatos(ServerPlayerEntity pl) {
        List<Candidato> out = new ArrayList<>();
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(pl);
        for (int i = 0; i < 6; i++) {
            Pokemon p = party.get(i);
            if (p == null) continue;
            out.add(new Candidato(i + 1, p, esTerrestre(p)));
        }
        return out;
    }

    public record Candidato(int slot, Pokemon pokemon, boolean valido) {}

    /** Crea la copia visual del Pokemon (sin IA, sin dano, sin sonido) en la posicion dada. */
    public static PokemonEntity crearVisual(ServerWorld w, Pokemon original, Text nombre, Vec3d pos, float yaw) {
        PokemonProperties props = original.createPokemonProperties(PokemonPropertyExtractor.ALL);
        PokemonEntity e = props.createEntity(w);
        Pokemon copia = e.getPokemon();
        Species sp = copia.getSpecies();
        float h = Math.max(sp.getHitbox().height(), sp.getHitbox().width() * 0.8f) * sp.getBaseScale();
        if (h > 0.01f) copia.setScaleModifier(Math.max(0.3f, Math.min(2.5f, alturaBase / h)));
        copia.setNickname(nombre.copy());
        e.setAiDisabled(true);
        e.setInvulnerable(true);
        e.setNoGravity(true);
        e.setSilent(true);
        e.setPersistent();
        e.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0f);
        w.spawnEntity(e);
        return e;
    }

    /**
     * Coloca el visual encima del jugador. El servidor no conoce la velocidad de un jugador (la calcula su cliente), asi que
     * se usa lo que se movio desde el tick anterior ({@code delta}) y se adelanta {@code ticks} para compensar el retraso de la red.
     */
    public static void seguir(PokemonEntity e, ServerPlayerEntity pl, Vec3d delta, double ticks) {
        double x = pl.getX() + delta.x * ticks, z = pl.getZ() + delta.z * ticks;
        e.refreshPositionAndAngles(x, pl.getY(), z, pl.bodyYaw, 0f);
        e.setHeadYaw(pl.headYaw);
        e.setBodyYaw(pl.bodyYaw);
        e.setVelocity(delta);
    }

    /** Pone al jugador en "modo corredor": invisible, sin salto, velocidad fija igual para todos y escalones de 1 bloque. */
    public static void prepararJugador(ServerPlayerEntity p, double velocidad) {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, StatusEffectInstance.INFINITE, 0, false, false, false));
        p.setInvulnerable(true);
        var vel = p.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (vel != null) {
            vel.removeModifier(ID_VEL);
            // el valor base de un jugador es 0.1: el modificador suma lo que falte para llegar a "velocidad"
            vel.addTemporaryModifier(new EntityAttributeModifier(ID_VEL, velocidad - vel.getBaseValue(), EntityAttributeModifier.Operation.ADD_VALUE));
        }
        var salto = p.getAttributeInstance(EntityAttributes.GENERIC_JUMP_STRENGTH);
        if (salto != null) { salto.removeModifier(ID_SALTO); salto.addTemporaryModifier(new EntityAttributeModifier(ID_SALTO, -1.0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)); }
        var esc = p.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT);
        if (esc != null) { esc.removeModifier(ID_ESCALON); esc.addTemporaryModifier(new EntityAttributeModifier(ID_ESCALON, 0.5, EntityAttributeModifier.Operation.ADD_VALUE)); }
    }

    /** Turbo (positivo) o ralentizacion (negativo) como fraccion de la velocidad; -1 = parado. */
    public static void velocidadEfecto(ServerPlayerEntity p, double factor) {
        var vel = p.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (vel == null) return;
        vel.removeModifier(ID_EFECTO);
        if (factor != 0) vel.addTemporaryModifier(new EntityAttributeModifier(ID_EFECTO, factor, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    public static void quitarJugador(ServerPlayerEntity p) {
        p.removeStatusEffect(StatusEffects.INVISIBILITY);
        p.setInvulnerable(false);
        for (var par : new Object[][]{{EntityAttributes.GENERIC_MOVEMENT_SPEED, ID_VEL}, {EntityAttributes.GENERIC_MOVEMENT_SPEED, ID_EFECTO}, {EntityAttributes.GENERIC_JUMP_STRENGTH, ID_SALTO}, {EntityAttributes.GENERIC_STEP_HEIGHT, ID_ESCALON}}) {
            @SuppressWarnings("unchecked")
            var inst = p.getAttributeInstance((net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.attribute.EntityAttribute>) par[0]);
            if (inst != null) inst.removeModifier((Identifier) par[1]);
        }
    }
}
