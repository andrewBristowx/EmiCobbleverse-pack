package emi.huida;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.world.World;

/**
 * Emi (la skin del dueno) para la escena final del nivel 3: una entidad con forma de jugador que no hace nada por si sola (sin IA, inmortal, no se
 * empuja) y a la que la escena le pone un gesto con {@link #ESTADO}: 0 quieta, 1 saluda, 2 brazos arriba, 3 «¡ay!» (manos en las mejillas).
 * Su textura y su modelo estan en la parte de cliente ({@code client.EmiRenderer}). Hay que tenerla tambien en los clientes (ya va en el mod).
 */
public class EmiEntity extends PathAwareEntity {
    public static final TrackedData<Integer> ESTADO = DataTracker.registerData(EmiEntity.class, TrackedDataHandlerRegistry.INTEGER);
    /** El tipo de entidad (se registra al iniciar el mod). */
    public static EntityType<EmiEntity> TIPO;

    public EmiEntity(EntityType<? extends EmiEntity> tipo, World mundo) {
        super(tipo, mundo);
        setAiDisabled(true);
        setPersistent();
        setInvulnerable(true);
    }

    @Override protected void initDataTracker(DataTracker.Builder b) { super.initDataTracker(b); b.add(ESTADO, 0); }

    public int estado() { return getDataTracker().get(ESTADO); }
    public void ponerEstado(int e) { getDataTracker().set(ESTADO, e); }

    @Override public boolean damage(DamageSource fuente, float cantidad) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canImmediatelyDespawn(double distancia) { return false; }
}
