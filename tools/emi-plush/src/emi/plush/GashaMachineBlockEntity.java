package emi.plush;

import net.minecraft.class_1657;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_2487;
import net.minecraft.class_2586;
import net.minecraft.class_2596;
import net.minecraft.class_2602;
import net.minecraft.class_2622;
import net.minecraft.class_2680;
import net.minecraft.class_7225;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Entidad de la máquina del Gasha: fase 0 reposo, 1 girando (la bola rueda ~1.5 s), 2 premio (se abre la compuerta). Los premios los reparte
 * {@link GashaServidor} cuando alguien tira desde la pantalla; aquí solo está la animación de la máquina del mundo, sincronizada al cliente.
 */
public class GashaMachineBlockEntity extends class_2586 implements GeoBlockEntity {
    static final int GIRO = 32, PREMIO = 44;
    private static final RawAnimation REPOSO = RawAnimation.begin().thenLoop("animation.gacha_machine.idle");
    private static final RawAnimation GIRANDO = RawAnimation.begin().thenPlay("animation.gacha_machine.activate").thenLoop("animation.gacha_machine.roll");
    private static final RawAnimation SALE = RawAnimation.begin().thenPlay("animation.gacha_machine.result_epic").thenLoop("animation.gacha_machine.idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int fase;
    private int ticks;

    public GashaMachineBlockEntity(class_2338 pos, class_2680 state) {
        super(EmiPlush.MAQUINA_GASHA_BE, pos, state);
    }

    /** Clic derecho (solo servidor): el jugador abre la pantalla del Gasha. */
    public void usar(class_1657 player) {
        if (player instanceof net.minecraft.class_3222 sp) GashaServidor.abrir(sp, method_11016());
    }

    /** Efecto en la máquina del mundo cuando alguien tira desde la pantalla: gira un momento y abre la compuerta. */
    public void efecto() {
        if (fase == 0) cambiar(1, GIRO);
    }

    private void cambiar(int nueva, int t) {
        fase = nueva;
        ticks = t;
        method_5431();
        field_11863.method_8413(method_11016(), method_11010(), method_11010(), 3);
    }

    public static void tick(class_1937 world, class_2338 pos, class_2680 state, GashaMachineBlockEntity e) {
        if (world.method_8608() || e.fase == 0) return;
        if (--e.ticks > 0) return;
        if (e.fase == 1) e.cambiar(2, PREMIO);
        else e.cambiar(0, 0);
    }

    @Override
    protected void method_11007(class_2487 nbt, class_7225.class_7874 reg) {
        super.method_11007(nbt, reg);
        nbt.method_10569("Fase", fase);
        nbt.method_10569("Ticks", ticks);
    }

    @Override
    protected void method_11014(class_2487 nbt, class_7225.class_7874 reg) {
        super.method_11014(nbt, reg);
        fase = nbt.method_10550("Fase");
        ticks = nbt.method_10550("Ticks");
    }

    @Override
    public class_2487 method_16887(class_7225.class_7874 reg) {
        class_2487 nbt = new class_2487();
        method_11007(nbt, reg);
        return nbt;
    }

    @Override
    public class_2596<class_2602> method_38235() {
        return class_2622.method_38585(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "maquina", 0, estado -> estado.setAndContinue(fase == 1 ? GIRANDO : fase == 2 ? SALE : REPOSO)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
