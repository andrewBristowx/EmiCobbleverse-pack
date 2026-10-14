package emi.huida;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonBehaviourFlag;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.net.messages.client.animation.PlayPosableAnimationPacket;
import emi.huida.net.CamaraPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Set;

/**
 * Las escenas de la huida: la entrada (el michi demonio llega volando, se burla, levanta la reja y suelta al Tyrantrum), el final (el michi angel
 * le da de comer al Tyrantrum para pararlo) y el mordisco (si te alcanza, te come). Cada escena es una linea de tiempo en ticks; los michis son
 * Pokemon de verdad (GatitoAlien con accesorio) movidos por el servidor, y la camara la manda el servidor ({@link CamaraPayload}).
 */
final class Escenas {
    private Escenas() {}

    // ---------------------------------------------------------------- utilidades

    /** Punto del mundo a distancia {@code s} del camino, {@code v} a la derecha del eje (en el sentido de la marcha) y {@code dy} sobre los pies del jugador. */
    static Vec3d P(int hueco, double s, double v, double dy) {
        double[] e = Nivel.ejePos(hueco, s);
        double yaw = Math.toRadians(e[2]);
        return new Vec3d(e[0] - v * Math.cos(yaw), Nivel.Y0 + dy, e[1] - v * Math.sin(yaw));
    }

    static float yawHacia(Vec3d de, Vec3d a) { return (float) Math.toDegrees(Math.atan2(-(a.x - de.x), a.z - de.z)); }

    static double suave(double t) { t = Math.max(0, Math.min(1, t)); return t * t * (3 - 2 * t); }
    static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    static Vec3d lerp(Vec3d a, Vec3d b, double t) { return new Vec3d(lerp(a.x, b.x, t), lerp(a.y, b.y, t), lerp(a.z, b.z, t)); }
    /** angulo (grados) interpolado por el camino corto */
    static float lerpAng(float a, float b, double t) { float d = ((b - a + 540f) % 360f) - 180f; return (float) (a + d * t); }

    static Vec3d bezier(Vec3d a, Vec3d b, Vec3d c, Vec3d d, double t) {
        double u = 1 - t;
        return a.multiply(u * u * u).add(b.multiply(3 * u * u * t)).add(c.multiply(3 * u * t * t)).add(d.multiply(t * t * t));
    }

    /** Camara de escena: desde {@code pos} mirando a {@code mira}. */
    static void camara(ServerPlayerEntity p, Vec3d pos, Vec3d mira, float fov, float temblor, boolean corte) {
        if (p == null || demoJ != null) return;
        double dx = mira.x - pos.x, dy = mira.y - pos.y, dz = mira.z - pos.z, h = Math.sqrt(dx * dx + dz * dz);
        ServerPlayNetworking.send(p, new CamaraPayload(true, corte, (float) pos.x, (float) pos.y, (float) pos.z,
                (float) Math.toDegrees(Math.atan2(-dx, dz)), (float) -Math.toDegrees(Math.atan2(dy, h)), fov, temblor));
    }

    static void camaraOff(ServerPlayerEntity p) { if (p != null && demoJ == null) ServerPlayNetworking.send(p, CamaraPayload.apagada()); }

    static void sonido(ServerWorld w, Vec3d pos, SoundEvent ev, float vol, float pitch) { w.playSound(null, pos.x, pos.y, pos.z, ev, SoundCategory.NEUTRAL, vol, pitch); }
    static void sonido(ServerWorld w, Vec3d pos, String id, float vol, float pitch) { sonido(w, pos, Sonidos.ev(id), vol, pitch); }

    static void particulas(ServerWorld w, ParticleEffect tipo, Vec3d pos, int n, double dx, double dy, double dz, double vel) {
        w.spawnParticles(tipo, pos.x, pos.y, pos.z, n, dx, dy, dz, vel);
    }

    /** Anima a un Pokemon con una de las animaciones con nombre de su poser (cry, physical, special, status...). */
    static void anim(PokemonEntity e, ServerPlayerEntity p, String nombre) {
        if (e == null || p == null) return;
        try { new PlayPosableAnimationPacket(e.getId(), Set.of(nombre), List.of()).sendToPlayer(p); } catch (Throwable t) { EmiHuida.LOG.warn("animacion {}: {}", nombre, t.toString()); }
    }

    /** Crea un GatitoAlien con el accesorio indicado (devil_horns / halo), sin IA ni gravedad, volando y grande. null si no se puede (p. ej. emi-plush no esta instalado). */
    static PokemonEntity michi(ServerWorld w, String accesorio, Vec3d pos, float yaw, float escala) {
        try {
            PokemonProperties props = PokemonProperties.Companion.parse("gatitoalien level=30 accessory=" + accesorio);
            PokemonEntity e = props.createEntity(w);
            e.getPokemon().setScaleModifier(escala);
            e.setAiDisabled(true);
            e.setInvulnerable(true);
            e.setNoGravity(true);
            e.setSilent(true);
            e.setPersistent();
            e.setCustomNameVisible(false);
            e.noClip = true;                      // sin empujones: si no, al flotar junto al Tyrantrum (cajas que se solapan) lo apartaba de su sitio hacia arriba
            e.setBehaviourFlag(PokemonBehaviourFlag.FLYING, true);
            e.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0f);
            e.setHeadYaw(yaw); e.setBodyYaw(yaw);
            w.spawnEntity(e);
            return e;
        } catch (Throwable t) {
            EmiHuida.LOG.error("No se pudo crear al michi ({})", accesorio, t);
            return null;
        }
    }

    /** Coloca al michi (lo teletransporta): {@code moviendo} = pose de volar (alas batiendo) o de flotar quieto. */
    static void mover(PokemonEntity e, Vec3d pos, float yaw, boolean moviendo) {
        if (e == null || e.isRemoved()) return;
        e.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0f);
        e.setHeadYaw(yaw); e.setBodyYaw(yaw);
        e.setVelocity(Vec3d.ZERO);
        ((Corredor) (Object) e).emi_huida$setPaso(moviendo ? 0.3f : 0f);
        // posicion exacta a los clientes en cada tick (el seguimiento normal manda saltitos relativos y el michi se veia desplazado)
        if (e.getWorld() instanceof ServerWorld sw) sw.getChunkManager().sendToNearbyPlayers(e, new net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket(e));
    }

    /** Solo para pruebas (/huida prueba michis): una fila de michis delante del jugador (demonio y angel volando / flotando / de pie y uno normal) para revisar el modelo y las animaciones. */
    static String banco(ServerPlayerEntity p) {
        ServerWorld w = p.getServerWorld();
        double r = Math.toRadians(p.getYaw());
        Vec3d f = new Vec3d(-Math.sin(r), 0, Math.cos(r)), d = new Vec3d(-Math.cos(r), 0, -Math.sin(r));
        String[][] lista = {{"devil_horns", "fly"}, {"devil_horns", "hover"}, {"devil_horns", "stand"}, {"halo", "fly"}, {"halo", "hover"}, {"halo", "stand"}, {"crown", "stand"}};
        int n = 0;
        for (int i = 0; i < lista.length; i++) {
            Vec3d pos = p.getPos().add(f.multiply(7)).add(d.multiply((i - 3) * 4.5)).add(0, lista[i][1].equals("stand") ? 0 : 2, 0);
            PokemonEntity e = michi(w, lista[i][0], pos, p.getYaw() + 180f, 1.4f);
            if (e == null) continue;
            e.addCommandTag("emi_huida_banco");
            e.setBehaviourFlag(PokemonBehaviourFlag.FLYING, !lista[i][1].equals("stand"));
            ((Corredor) (Object) e).emi_huida$setPaso(lista[i][1].equals("fly") ? 0.3f : 0f);
            n++;
        }
        return n == 0 ? "No se pudo crear ningun michi." : null;
    }

    static void quitarBanco(ServerWorld w) {
        for (Entity e : new java.util.ArrayList<>(w.iterateEntities().iterator().hasNext() ? java.util.stream.StreamSupport.stream(w.iterateEntities().spliterator(), false).toList() : List.<Entity>of()))
            if (e.getCommandTags().contains("emi_huida_banco")) e.discard();
    }

    // ---- demostracion libre de la escena del angel (solo pruebas): sin camara de escena, el jugador (espectador) la mira desde donde quiera y se puede congelar en un tick
    private static Huida.J demoJ; private static ServerPlayerEntity demoP; private static int demoT, demoCongelar;

    static String demoAngel(ServerPlayerEntity p, int congelarEn) {
        ServerWorld w = p.getServerWorld();
        demoFin();
        double r = Math.toRadians(p.getYaw());
        PokemonEntity ty = Huida.nuevoTyrantrum(w, p.getX() - Math.sin(r) * 22, p.getY(), p.getZ() + Math.cos(r) * 22, p.getYaw() + 180f);       // mirando hacia el jugador
        if (ty == null) return "No se pudo crear al Tyrantrum.";
        ty.addCommandTag("emi_huida_banco");
        demoJ = new Huida.J(p); demoJ.perseguidor = ty; demoJ.nivel = null; demoP = p; demoT = 0; demoCongelar = congelarEn;
        return null;
    }

    static void demoFin() {
        if (demoJ != null) { quitar(demoJ.michi); if (demoJ.comida != null) demoJ.comida.discard(); if (demoJ.perseguidor != null) demoJ.perseguidor.discard(); }
        demoJ = null; demoP = null;
    }

    static void tickDemo() {
        if (demoJ == null || demoP == null || demoP.isDisconnected()) { if (demoJ != null) demoFin(); return; }
        ServerWorld w = demoP.getServerWorld();
        int t = demoCongelar > 0 ? Math.min(demoT, demoCongelar) : demoT;
        try { finalAngel(demoJ, demoP, w, t); } catch (Throwable e) { EmiHuida.LOG.error("demo angel", e); demoFin(); return; }
        if (demoT < (demoCongelar > 0 ? demoCongelar : 400)) demoT++;
        else if (demoCongelar <= 0) demoFin();
    }

    static void quitar(PokemonEntity e) { if (e != null && !e.isRemoved()) e.discard(); }

    // ---------------------------------------------------------------- escena 1: la entrada

    /** Duracion de la presentacion (ticks). Los 3 ultimos segundos son la cuenta atras. */
    static final int INTRO = 200;
    private static final int T_VUELO = 58, T_BURLA = 60, T_GIRO = 90, T_GOLPE = 108, T_REJA = 114, T_RUGIDO = 140, T_HUIDA = 128, T_FIN_HUIDA = 178;
    private static final int PASO_REJA = 3;      // ticks entre una fila de barrotes y la siguiente

    /** Posicion del michi demonio en vuelo (de lejos hasta la reja). */
    private static final double G = Nivel.S_ENTRADA;

    /** Punto a {@code off} bloques de la reja (positivo = hacia el jugador). */
    private static Vec3d g(int h, double off, double v, double dy) { return P(h, G + off, v, dy); }

    private static Vec3d vueloEntrada(int h, double u) {
        return bezier(g(h, 27, 20, 30), g(h, 13, 14, 9), g(h, 2, -7, 17), g(h, 2.5, 0, 17.5), u);
    }

    private static Vec3d vueloSalida(int h, double u) {
        return bezier(g(h, 2.5, 0, 17.5), g(h, 0, 6, 23), g(h, 17, 16, 30), g(h, 37, 22, 46), u);
    }

    static void intro(Huida.J j, ServerPlayerEntity p, ServerWorld w, int t) {
        int h = j.hueco;
        Vec3d reja = g(h, 0, 0, 5.5);
        Vec3d hover = g(h, 2.5, 0, 17.5);
        if (t < 5 && j.michi == null) j.michi = michi(w, "devil_horns", vueloEntrada(h, 0), 200f, 3.2f);
        if (t == 0) Msg.actionBar(p, Text.literal("Algo se acerca volando...").formatted(Formatting.GRAY, Formatting.ITALIC));
        PokemonEntity m = j.michi;

        // ---- el michi: llega volando, se burla, golpea la reja y huye
        Vec3d mp = hover;
        float yaw = 0f;
        boolean vuela = true;
        if (t < T_VUELO) {
            double u = t / (double) T_VUELO, e = 1 - (1 - u) * (1 - u);
            mp = vueloEntrada(h, e);
            Vec3d sig = vueloEntrada(h, 1 - (1 - Math.min(1, (t + 1) / (double) T_VUELO)) * (1 - Math.min(1, (t + 1) / (double) T_VUELO)));
            yaw = yawHacia(mp, sig);
            if (t > T_VUELO - 14) yaw = lerpAng(yaw, 0f, suave((t - (T_VUELO - 14)) / 14.0));      // al llegar se pone de cara a la camara
            if (t % 6 == 0) sonido(w, mp, SoundEvents.ENTITY_PHANTOM_FLAP, 0.9f, 1.7f);
            if (t == 6) sonido(w, mp, SoundEvents.ENTITY_BAT_TAKEOFF, 1.2f, 1.3f);
        } else if (t < T_HUIDA) {
            vuela = t < T_BURLA + 4 || t >= T_GIRO - 4;
            mp = hover.add(0, 0.35 * Math.sin(t * 0.22), 0.12 * Math.sin(t * 0.13));
            yaw = 0f;
            if (t >= T_GIRO && t < T_GIRO + 14) yaw = (float) (360 * suave((t - T_GIRO) / 14.0));        // pirueta de burla
            else if (t >= T_GOLPE - 6) yaw = lerpAng(0f, 180f, suave((t - (T_GOLPE - 6)) / 6.0));         // se gira hacia la reja
            if (t % 8 == 0) sonido(w, mp, SoundEvents.ENTITY_PHANTOM_FLAP, 0.7f, 1.8f);
        } else {
            double u = Math.min(1.0, (t - T_HUIDA) / (double) (T_FIN_HUIDA - T_HUIDA));
            double e = u * u;
            mp = vueloSalida(h, e);
            Vec3d sig = vueloSalida(h, Math.min(1.0, ((t + 1 - T_HUIDA) / (double) (T_FIN_HUIDA - T_HUIDA)) * ((t + 1 - T_HUIDA) / (double) (T_FIN_HUIDA - T_HUIDA))));
            yaw = u < 0.15 ? lerpAng(180f, yawHacia(mp, sig), suave(u / 0.15)) : yawHacia(mp, sig);
            if (t % 5 == 0) sonido(w, mp, SoundEvents.ENTITY_PHANTOM_FLAP, 0.8f, 1.6f);
        }
        if (m != null) {
            if (t >= T_FIN_HUIDA + 2) { quitar(m); j.michi = null; m = null; }
            else mover(m, mp, yaw, vuela);
        }
        if (Huida.DEBUG && t % 10 == 0) EmiHuida.LOG.info("escena intro t={} michi={} pos={} removido={} pose={} vuela={}", t, m != null, mp, m != null && m.isRemoved(), m != null ? m.getCurrentPoseType() : null, vuela);

        // ---- sonidos y animaciones de la burla
        if (t == T_BURLA) { anim(m, p, "cry"); sonido(w, mp, SoundEvents.ENTITY_CAT_AMBIENT, 1.6f, 1.15f); Msg.actionBar(p, Text.literal("El Michi Demonio se burla de ti...").formatted(Formatting.RED, Formatting.ITALIC)); }
        if (t == T_BURLA + 14) sonido(w, mp, SoundEvents.ENTITY_CAT_PURREOW, 1.5f, 1.45f);
        if (t == 76 || t == 80 || t == 84) sonido(w, mp, SoundEvents.ENTITY_CAT_AMBIENT, 1.5f, t == 76 ? 1.75f : t == 80 ? 1.55f : 1.9f);
        if (t == 76) anim(m, p, "status");
        if (t == 86) sonido(w, mp, "sfx.burla", 1.5f, 1f);
        if (t == T_GIRO) { anim(m, p, "special"); sonido(w, mp, SoundEvents.ENTITY_CAT_BEG_FOR_FOOD, 1.4f, 1.6f); }
        if (t == T_GOLPE) { anim(m, p, "physical"); sonido(w, mp, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.4f); Msg.actionBar(p, Text.literal("¡Ha levantado la reja!").formatted(Formatting.GOLD, Formatting.BOLD)); }
        if (t >= T_GOLPE + 2 && t < T_REJA + 4) {            // chispas del michi a la reja
            double u = (t - T_GOLPE - 2) / 8.0;
            particulas(w, ParticleTypes.END_ROD, lerp(mp, reja.add(0, 1.5, 0), u), 4, 0.15, 0.15, 0.15, 0.02);
            particulas(w, ParticleTypes.ENCHANT, reja.add(0, 3, 0), 6, 1.5, 3, 0.3, 0.3);
        }

        // ---- la reja sube (se quitan los barrotes por filas desde abajo)
        int fila = t < T_REJA ? -1 : (t - T_REJA) / PASO_REJA;
        if (t >= T_REJA && (t - T_REJA) % PASO_REJA == 0 && fila < Nivel.ENTRADA_ALTO + 1) {
            for (Nivel.Bloque b : j.nivel.entrada()) if ((int) b.s() == fila) w.breakBlock(b.pos(), false);
            if (fila == 0) sonido(w, reja, SoundEvents.BLOCK_IRON_DOOR_OPEN, 1.6f, 0.5f);
            sonido(w, reja, SoundEvents.BLOCK_CHAIN_PLACE, 1.4f, 0.7f + 0.03f * fila);
            if (fila % 2 == 0) sonido(w, reja, SoundEvents.BLOCK_PISTON_EXTEND, 0.8f, 0.5f);
            particulas(w, ParticleTypes.POOF, g(h, 0, 0, 0.3), 12, 3.5, 0.2, 0.3, 0.02);
            if (fila == Nivel.ENTRADA_ALTO) { sonido(w, reja, SoundEvents.BLOCK_ANVIL_LAND, 1.4f, 0.55f); particulas(w, ParticleTypes.CAMPFIRE_COSY_SMOKE, g(h, 0, 0, 1), 6, 3.5, 0.3, 0.3, 0.02); }
        }

        // ---- el Tyrantrum ruge
        if (t == T_RUGIDO) {
            Huida.rugir(j.perseguidor);
            if (j.perseguidor != null) sonido(w, j.perseguidor.getPos(), SoundEvents.ENTITY_RAVAGER_ROAR, 2.2f, 0.45f);
        }
        if (t == T_HUIDA + 14 && m != null) { anim(m, p, "cry"); sonido(w, mp, SoundEvents.ENTITY_CAT_AMBIENT, 1.4f, 1.9f); }
        if (t == T_HUIDA + 20 && m != null) sonido(w, mp, SoundEvents.ENTITY_CAT_AMBIENT, 1.3f, 1.7f);

        // ---- la camara
        float temblor = 0f;
        if (t >= T_REJA && t < T_RUGIDO) temblor = 0.02f;
        if (t >= T_RUGIDO && t < T_RUGIDO + 24) temblor = (float) (0.22 * (1 - (t - T_RUGIDO) / 24.0));
        if (t < 58) {
            double u = suave(t / 58.0);
            Vec3d cam = lerp(g(h, 17, -12, 13), g(h, 14, -11, 11), u);
            Vec3d mira = lerp(g(h, 2, 0, 9), mp, suave((t - 14) / 30.0));
            camara(p, cam, mira, 1.1f, 0f, t == 0);
        } else if (t < 112) {
            double u = suave((t - 58) / 54.0);
            Vec3d cam = lerp(g(h, 9.5, 2.5, 12.5), g(h, 7.5, 1.2, 14.5), u);
            camara(p, cam, mp.add(0, 0.3, 0), 0.85f, 0f, t == 58);
        } else if (t < 150) {
            double u = suave((t - 112) / 38.0);
            Vec3d cam = lerp(g(h, 10, 1.5, 2.3), g(h, 6.5, 0.5, 2.7), u);          // desde dentro del pasillo, abajo: la reja sube y detras se ve al Tyrantrum
            float fov = t < T_RUGIDO ? 1.1f : 0.95f;
            camara(p, cam, g(h, -5, 0, 5.5), fov, temblor, t == 112);
        } else if (t == 150) {
            camaraOff(p);          // el cliente vuelve a la camara de la huida con una transicion suave
        }
    }

    // ---------------------------------------------------------------- el Tyrantrum (referencias para el final y el mordisco)

    /** Donde esta la boca del Tyrantrum respecto a su centro (bloques por delante, altura sobre el suelo), medido en el juego con una regla de bloques: de pie, la boca esta ~6 por delante
     *  y a ~5,6 de altura (el hocico llega a ~7); con el rugido sube a ~7,2; y en el mordisco (animacion «physical») echa la cabeza hacia delante y abajo: la boca queda a ~9 por delante y ~2 de altura. */
    static final double BOCA_ADELANTE = 6.0, BOCA_ALTO = 5.6;
    static final double BOCA_ABIERTA_ADELANTE = 6.2, BOCA_ABIERTA_ALTO = 7.2;
    static final double BOCA_MORDISCO_ADELANTE = 9.0, BOCA_MORDISCO_ALTO = 2.0;

    /** Punto a {@code adelante} bloques por delante del Tyrantrum, {@code derecha} a su derecha y {@code alto} sobre sus pies. */
    static Vec3d T(PokemonEntity ty, double adelante, double derecha, double alto) {
        double r = Math.toRadians(ty.getYaw());
        return ty.getPos().add(-Math.sin(r) * adelante - Math.cos(r) * derecha, alto, Math.cos(r) * adelante - Math.sin(r) * derecha);
    }

    /** Un objeto flotando (pastel, etc.), grande y sin gravedad. */
    static Entity objeto(ServerWorld w, Vec3d pos, String item, float escala) {
        try {
            Entity e = EntityType.ITEM_DISPLAY.create(w);
            NbtCompound n = StringNbtReader.parse("{item:{id:\"" + item + "\",count:1},item_display:\"fixed\",billboard:\"center\",transformation:{translation:[0f,0f,0f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],scale:["
                    + escala + "f," + escala + "f," + escala + "f]}}");
            e.readNbt(n);
            e.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0f, 0f);
            w.spawnEntity(e);
            return e;
        } catch (Throwable t) {
            EmiHuida.LOG.error("No se pudo crear el objeto {}", item, t);
            return null;
        }
    }

    // ---------------------------------------------------------------- escena 2: el michi angel (al llegar a la meta)

    static final int FINAL_ANGEL = 175;

    /** Donde se queda flotando el angel: a la izquierda de la cabeza del Tyrantrum, de perfil para quien mira de frente, con la boca justo a su lado. */
    static final double ANGEL_ADELANTE = 5.9, ANGEL_DERECHA = -3.8, ANGEL_ALTO = 6.6;

    private static Vec3d vueloAngel(PokemonEntity ty, double u, boolean sale) {
        Vec3d meta = T(ty, ANGEL_ADELANTE, ANGEL_DERECHA, ANGEL_ALTO);
        if (!sale) return bezier(T(ty, 22, -12, 32), T(ty, 18, -8, 16), T(ty, 11, -5, 11), meta, u);
        return bezier(meta, T(ty, 9, -5, 12), T(ty, 16, -10, 20), T(ty, 28, -16, 42), u);
    }

    /** Donde el angel sostiene la comida: delante de el, hacia la boca. */
    private static Vec3d mano(Vec3d ap, Vec3d boca) {
        double dx = boca.x - ap.x, dz = boca.z - ap.z, l = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
        return ap.add(dx / l * 1.7, 0.9, dz / l * 1.7);
    }

    static void finalAngel(Huida.J j, ServerPlayerEntity p, ServerWorld w, int t) {
        PokemonEntity ty = j.perseguidor;
        if (ty == null || ty.isRemoved()) { camaraOff(p); return; }
        int T0 = 8, T_LLEGA = T0 + 40, T_COMIDA = 56, T_ROAR = 62, T_LANZA = 72, T_COME = 82, T_TRAGA = 124, T_SALE = 138;
        PokemonEntity a = j.michi;
        if (Huida.DEBUG && t % 10 == 0) EmiHuida.LOG.info("escena angel t={} tyr={} angel={} jugador={}", t, ty.getPos(), j.michi == null ? null : j.michi.getPos(), p.getPos());
        if (t >= T0 && t < T0 + 5 && a == null) j.michi = a = michi(w, "halo", vueloAngel(ty, 0, false), 0f, 3.0f);
        Vec3d boca = T(ty, BOCA_ABIERTA_ADELANTE, 0, BOCA_ABIERTA_ALTO);

        // ---- el angel: baja, ofrece la comida, la lanza, saluda y se va
        if (a != null) {
            Vec3d ap; float yaw; boolean vuela = true;
            if (t < T_LLEGA) {
                double u = Math.max(0, (t - T0) / (double) (T_LLEGA - T0)), e = 1 - (1 - u) * (1 - u);
                ap = vueloAngel(ty, e, false);
                Vec3d sig = vueloAngel(ty, Math.min(1, 1 - (1 - Math.min(1, (t + 1 - T0) / (double) (T_LLEGA - T0))) * (1 - Math.min(1, (t + 1 - T0) / (double) (T_LLEGA - T0)))), false);
                yaw = yawHacia(ap, sig);
                if (u > 0.7) yaw = lerpAng(yaw, yawHacia(ap, boca), suave((u - 0.7) / 0.3));
                if (t % 5 == 0) sonido(w, ap, SoundEvents.ENTITY_PHANTOM_FLAP, 0.9f, 1.9f);
            } else if (t < T_SALE) {
                ap = vueloAngel(ty, 1, false).add(0, 0.3 * Math.sin(t * 0.2), 0);
                yaw = yawHacia(ap, boca);
                vuela = false;
                if (t % 8 == 0) sonido(w, ap, SoundEvents.ENTITY_PHANTOM_FLAP, 0.6f, 1.9f);
            } else {
                double u = Math.min(1, (t - T_SALE) / 36.0), e = u * u;
                ap = vueloAngel(ty, e, true);
                Vec3d sig = vueloAngel(ty, Math.min(1, ((t + 1 - T_SALE) / 36.0) * ((t + 1 - T_SALE) / 36.0)), true);
                yaw = yawHacia(ap, sig);
            }
            if (t >= T_SALE + 38) { quitar(a); j.michi = null; a = null; }
            else mover(a, ap, yaw, vuela);
            if (t == T0) { sonido(w, ap, "sfx.angel", 1.6f, 1f); }
            if (t >= T0 && t < T_LLEGA && t % 2 == 0) particulas(w, ParticleTypes.END_ROD, ap, 2, 0.4, 0.4, 0.4, 0.02);
            if (t == T_LLEGA) { anim(a, p, "status"); sonido(w, ap, SoundEvents.ENTITY_CAT_PURREOW, 1.5f, 1.3f); Msg.actionBar(p, Text.literal("¡El Michi Ángel ha venido a ayudarte!").formatted(Formatting.AQUA, Formatting.BOLD)); }
            if (t == T_LANZA) { anim(a, p, "physical"); sonido(w, ap, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.5f); }
            if (t == T_TRAGA + 4) { anim(a, p, "special"); sonido(w, ap, SoundEvents.ENTITY_CAT_AMBIENT, 1.5f, 1.4f); }
            if (t == T_SALE) { anim(a, p, "cry"); }
        }

        // ---- la comida
        if (t == T_COMIDA && a != null) {
            j.comida = objeto(w, mano(a.getPos(), boca), "minecraft:cake", 2.2f);
            sonido(w, a.getPos(), SoundEvents.ENTITY_ITEM_PICKUP, 1.5f, 0.6f);
        }
        if (j.comida != null && !j.comida.isRemoved()) {
            Vec3d ap = a != null ? a.getPos() : boca;
            if (t < T_LANZA) {
                Vec3d c = mano(ap, boca).add(0, 0.15 * Math.sin(t * 0.3), 0);
                j.comida.refreshPositionAndAngles(c.x, c.y, c.z, (t * 12) % 360, 0f);
            } else if (t < T_COME) {
                double u = (t - T_LANZA) / (double) (T_COME - T_LANZA);
                Vec3d ini = mano(ap, boca);
                Vec3d c = lerp(ini, boca, u).add(0, 1.3 * Math.sin(Math.PI * u), 0);
                j.comida.refreshPositionAndAngles(c.x, c.y, c.z, (t * 25) % 360, 0f);
                if (t % 2 == 0) particulas(w, ParticleTypes.END_ROD, c, 1, 0.1, 0.1, 0.1, 0.01);
            } else {
                j.comida.discard(); j.comida = null;
                sonido(w, boca, SoundEvents.ENTITY_GENERIC_EAT, 1.8f, 0.55f);
                particulas(w, new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.CAKE)), boca, 18, 0.5, 0.4, 0.5, 0.12);
                particulas(w, ParticleTypes.POOF, boca, 6, 0.4, 0.3, 0.4, 0.03);
            }
        }

        // ---- el Tyrantrum: ruge con la boca abierta, se traga el pastel, mastica y se calma
        if (t == 0) { ((Corredor) (Object) ty).emi_huida$setPaso(0f); ty.setSprinting(false); }
        if (t == T_ROAR) { Huida.rugir(ty); sonido(w, boca, SoundEvents.ENTITY_RAVAGER_ROAR, 1.6f, 0.55f); }
        if (t >= T_COME + 4 && t < T_TRAGA && (t - T_COME) % 12 == 4) {
            anim(ty, p, "recoil");
            sonido(w, boca, SoundEvents.ENTITY_GENERIC_EAT, 1.7f, 0.5f + 0.05f * ((t - T_COME) / 12));
            particulas(w, new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.CAKE)), T(ty, BOCA_ADELANTE, 0, BOCA_ALTO), 6, 0.4, 0.2, 0.4, 0.08);
        }
        if (t == T_TRAGA) {
            sonido(w, boca, "sfx.glup", 1.8f, 0.6f);
            sonido(w, boca, SoundEvents.ENTITY_PLAYER_BURP, 2.0f, 0.4f);
            particulas(w, ParticleTypes.HEART, T(ty, 4, 0, 8.5), 14, 2.5, 1.0, 1.5, 0.05);
            Huida.titulosEscape(j, p);
        }
        if (t > T_TRAGA && t < T_SALE + 10 && t % 6 == 0) particulas(w, ParticleTypes.HEART, T(ty, 4, 0, 9), 2, 2.2, 0.6, 1.2, 0.03);

        // ---- la camara (siempre por el pasillo o por encima de los muros: fuera de el hay muros y arboles)
        float temblor = (t >= T_ROAR && t < T_ROAR + 20) ? (float) (0.1 * (1 - (t - T_ROAR) / 20.0)) : 0f;
        if (t < 56) {              // desde arriba: se ve al Tyrantrum parado, al jugador y bajar al angel
            double u = suave((t - T0) / 48.0);
            Vec3d cam = lerp(T(ty, 15.0, -10.5, 16.0), T(ty, 14.0, -9.5, 14.5), u);
            camara(p, cam, lerp(T(ty, 6.5, 0, 4.5), a != null ? a.getPos().add(0, 1.5, 0) : T(ty, 8, 0, 7), 0.5), 1.05f, 0f, t == T0);
        } else if (t < 124) {      // cara a cara con el Tyrantrum: el angel le da el pastel
            double u = suave((t - 56) / 68.0);
            Vec3d cam = lerp(T(ty, 17.5, 1.0, 7.4), T(ty, 15.5, 0.6, 7.2), u);
            camara(p, cam, lerp(T(ty, ANGEL_ADELANTE, ANGEL_DERECHA / 2, ANGEL_ALTO + 0.3), boca, 0.3), 0.95f, temblor, t == 56);
        } else if (t < 160) {      // otra vez desde arriba, con los corazones
            double u = suave((t - 124) / 36.0);
            Vec3d cam = lerp(T(ty, 15.0, -10.0, 16.0), T(ty, 13.5, -9.0, 14.5), u);
            camara(p, cam, T(ty, 5.0, 0, 6.5), 1.05f, 0f, t == 124);
        } else if (t == 160) {
            camaraOff(p);
        }
    }

    // ---------------------------------------------------------------- escena 3: el Tyrantrum te come

    static final int FINAL_COMIDO = 175;

    static void comido(Huida.J j, ServerPlayerEntity p, ServerWorld w, int t) {
        PokemonEntity ty = j.perseguidor;
        if (ty == null || ty.isRemoved()) { camaraOff(p); return; }
        if (Huida.DEBUG && t % 10 == 0) EmiHuida.LOG.info("escena comido t={} tyr={} yaw={} jugador={}", t, ty.getPos(), ty.getYaw(), p.getPos());
        int T_MUERDE = 28;                  // la mandibula de la animacion «physical» se cierra a 1,33 s = 27 ticks
        if (t == 0) j.escBase = ty.getPos();
        if (t <= 22 && j.escBase != null) {           // carrerilla: mientras echa la cabeza atras (la animacion «physical» empieza asi) el Tyrantrum retrocede 3 bloques para que su boca llegue donde esta el jugador al morder
            double r = Math.toRadians(ty.getYaw()), d = 3.0 * suave(t / 22.0);
            ty.refreshPositionAndAngles(j.escBase.x + Math.sin(r) * d, j.escBase.y, j.escBase.z - Math.cos(r) * d, ty.getYaw(), 0f);
        }
        Vec3d bocaAbajo = T(ty, BOCA_MORDISCO_ADELANTE, 0, 0.6);
        if (t == 0) {
            ((Corredor) (Object) ty).emi_huida$setPaso(0f); ty.setSprinting(false);
            anim(ty, p, "physical");
            sonido(w, ty.getPos(), SoundEvents.ENTITY_RAVAGER_ROAR, 2.0f, 0.5f);
        }
        if (t == T_MUERDE - 6) sonido(w, p.getPos(), SoundEvents.ENTITY_WARDEN_ATTACK_IMPACT, 1.5f, 0.6f);
        if (t == T_MUERDE) {
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 20 * 60, 0, false, false, false));
            p.teleport(w, bocaAbajo.x, bocaAbajo.y, bocaAbajo.z, p.getYaw(), 0f);
            sonido(w, bocaAbajo, SoundEvents.ENTITY_GENERIC_EAT, 2.0f, 0.5f);
            sonido(w, bocaAbajo, SoundEvents.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.6f, 0.5f);
            particulas(w, ParticleTypes.CRIT, bocaAbajo.add(0, 1, 0), 30, 0.8, 0.8, 0.8, 0.4);
            particulas(w, ParticleTypes.POOF, bocaAbajo, 10, 0.8, 0.4, 0.8, 0.05);
            Msg.actionBar(p, Text.literal("¡ÑAM!").formatted(Formatting.RED, Formatting.BOLD));
            sonido(w, bocaAbajo, "sfx.derrota_comido", 1.5f, 1f);
        }
        if (t > T_MUERDE && t < T_MUERDE + 8) { p.setVelocity(0, 0, 0); p.velocityModified = true; }
        // masticar (la animacion «recoil» abre y cierra la mandibula)
        int c = t - (T_MUERDE + 24);
        if (c >= 0 && c < 72 && c % 18 == 0) {
            anim(ty, p, "recoil");
            sonido(w, T(ty, BOCA_ADELANTE, 0, BOCA_ALTO), SoundEvents.ENTITY_GENERIC_EAT, 1.8f, 0.45f + 0.04f * (c / 18));
            particulas(w, ParticleTypes.CRIT, T(ty, BOCA_ADELANTE, 0, BOCA_ALTO), 4, 0.4, 0.2, 0.4, 0.2);
        }
        if (t == T_MUERDE + 76) Msg.title(p, Text.literal("¡TE COMIÓ!").formatted(Formatting.RED, Formatting.BOLD), Text.literal("★ " + j.puntos + " · Errores: " + j.errores).formatted(Formatting.GRAY), 0, 50, 20);
        if (t == T_MUERDE + 100) {
            sonido(w, T(ty, 8, 0, 4), "sfx.glup", 1.8f, 0.6f);
            sonido(w, T(ty, 8, 0, 4), SoundEvents.ENTITY_PLAYER_BURP, 2.0f, 0.4f);
            particulas(w, ParticleTypes.CLOUD, T(ty, BOCA_ADELANTE, 0, BOCA_ALTO), 12, 0.4, 0.3, 0.4, 0.05);
            Msg.actionBar(p, Text.literal("*¡Burp!*").formatted(Formatting.GREEN, Formatting.ITALIC));
        }

        // ---- la camara: de frente (el mordisco viene hacia ella), luego desde arriba y por ultimo un primer plano de la cara
        float temblor = (t >= T_MUERDE && t < T_MUERDE + 10) ? 0.18f : 0f;
        if (t < 16) return;                                 // los primeros ticks se ve la camara normal (el Tyrantrum se frena y echa la cabeza atras)
        if (t < T_MUERDE + 12) {
            double u = suave((t - 16) / 24.0);
            Vec3d cam = lerp(T(ty, 18.0, 0.8, 4.2), T(ty, 16.5, 0.4, 3.6), u);
            camara(p, cam, lerp(T(ty, 8.0, 0, 5.0), T(ty, 9.0, 0, 2.8), u), 0.95f, temblor, t == 16);
        } else if (t < T_MUERDE + 62) {
            double u = suave((t - T_MUERDE - 12) / 50.0);
            Vec3d cam = lerp(T(ty, 12.5, -7.0, 13.0), T(ty, 11.0, -6.0, 11.5), u);
            camara(p, cam, T(ty, 7.0, 0, 3.8), 0.9f, 0f, t == T_MUERDE + 12);
        } else if (t < T_MUERDE + 108) {
            double u = suave((t - T_MUERDE - 62) / 46.0);
            Vec3d cam = lerp(T(ty, 17.0, 0.8, 7.2), T(ty, 14.5, 0.5, 6.6), u);
            camara(p, cam, T(ty, 6.0, 0, 5.4), 0.85f, 0f, t == T_MUERDE + 62);
        } else if (t == T_MUERDE + 108) {
            camaraOff(p);
        }
    }
}
