package emi.plush;

import net.minecraft.class_1657;
import net.minecraft.class_1268;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_2487;
import net.minecraft.class_2561;
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

import java.util.UUID;

/**
 * Entidad de la máquina del Gasha: fase 0 reposo, 1 girando (la bola rueda ~1.5 s), 2 premio (se abre la compuerta). El premio sale al
 * terminar de girar: ejecuta `emi_plush:gasha/tirar` como el jugador que insertó el ticket. La fase se sincroniza al cliente para las animaciones.
 */
public class GashaMachineBlockEntity extends class_2586 implements GeoBlockEntity {
    static final int GIRO = 32, PREMIO = 44;
    private static final RawAnimation REPOSO = RawAnimation.begin().thenLoop("animation.gacha_machine.idle");
    private static final RawAnimation GIRANDO = RawAnimation.begin().thenPlay("animation.gacha_machine.activate").thenLoop("animation.gacha_machine.roll");
    private static final RawAnimation SALE = RawAnimation.begin().thenPlay("animation.gacha_machine.result_epic").thenLoop("animation.gacha_machine.idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int fase;
    private int ticks;
    private UUID jugador;

    public GashaMachineBlockEntity(class_2338 pos, class_2680 state) {
        super(EmiPlush.MAQUINA_GASHA_BE, pos, state);
    }

    /** Clic derecho (solo servidor): gasta un ticket de la mano y empieza a girar. */
    public void usar(class_1657 player) {
        if (fase != 0) return;
        var mano = player.method_5998(class_1268.field_5808);
        if (!mano.method_31574(EmiPlush.TICKET_GASHA)) {
            player.method_7353(class_2561.method_43471("block.emi_plush.maquina_gasha_michi.sin_ticket"), true);
            return;
        }
        if (!player.method_7337()) mano.method_7934(1);
        jugador = player.method_5667();
        cambiar(1, GIRO);
        var p = method_11016();
        player.method_5682().method_3734().method_44252(player.method_5671().method_9206(4),
                String.format("playsound minecraft:block.note_block.bell block @a %d %d %d 1 1.2", p.method_10263(), p.method_10264(), p.method_10260()));
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
        if (e.fase == 1) {
            e.cambiar(2, PREMIO);
            var servidor = world.method_8503();
            var jug = e.jugador == null ? null : servidor.method_3760().method_14602(e.jugador);
            if (jug != null) {
                var fuente = jug.method_5671().method_9206(4);
                var comandos = servidor.method_3734();
                comandos.method_44252(fuente, String.format("playsound minecraft:block.note_block.chime block @a %d %d %d 1 1.4", pos.method_10263(), pos.method_10264(), pos.method_10260()));
                comandos.method_44252(fuente, String.format("particle minecraft:happy_villager %.1f %.1f %.1f 0.4 0.6 0.4 0 14", pos.method_10263() + 0.5, pos.method_10264() + 1.4, pos.method_10260() + 0.5));
                comandos.method_44252(fuente, "function emi_plush:gasha/tirar");
            } else {
                net.minecraft.class_1264.method_5449(world, pos.method_10263() + 0.5, pos.method_10264() + 1.2, pos.method_10260() + 0.5, new net.minecraft.class_1799(EmiPlush.TICKET_GASHA));   // se desconectó: devolvemos el ticket
            }
        } else {
            e.cambiar(0, 0);
            e.jugador = null;
        }
    }

    @Override
    protected void method_11007(class_2487 nbt, class_7225.class_7874 reg) {
        super.method_11007(nbt, reg);
        nbt.method_10569("Fase", fase);
        nbt.method_10569("Ticks", ticks);
        if (jugador != null) nbt.method_25927("Jugador", jugador);
    }

    @Override
    protected void method_11014(class_2487 nbt, class_7225.class_7874 reg) {
        super.method_11014(nbt, reg);
        fase = nbt.method_10550("Fase");
        ticks = nbt.method_10550("Ticks");
        jugador = nbt.method_25928("Jugador") ? nbt.method_25926("Jugador") : null;
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
