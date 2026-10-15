package emi.huida;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonBehaviourFlag;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.entity.PoseType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Las escenas del nivel de la calabaza (Halloween): la entrada (una calabaza gigante tiembla como si hubiera algo dentro, de ella sale el michi
 * brujita, grita «¡LA CALABAZA!» y la transforma en Pumpkaboo), el final (Snorlax Emi se come al Pumpkaboo) y el tragon (si te alcanza, te traga).
 * Mismo esquema que {@link Escenas}: una linea de tiempo en ticks, Pokemon de verdad movidos por el servidor y la camara mandada por el servidor.
 */
final class EscenasCalabaza {
    private EscenasCalabaza() {}

    // ---------------------------------------------------------------- bloques gigantes (la calabaza)

    /** Un block_display con el bloque dado (p. ej. «minecraft:pumpkin»). Despues se coloca y se escala con {@link #poner}. */
    static Entity bloque(ServerWorld w, Vec3d pos, String bloque) {
        try {
            Entity e = EntityType.BLOCK_DISPLAY.create(w);
            e.addCommandTag("emi_huida_bloque:" + bloque);          // BlockDisplay.readNbt pone «aire» si el NBT no lleva el bloque: hay que repetirlo en cada actualizacion
            NbtCompound n = new NbtCompound();
            n.put("block_state", estadoNbt(bloque));
            NbtCompound br = new NbtCompound(); br.putInt("block", 13); br.putInt("sky", 8);
            n.put("brightness", br);                                 // brilla en la oscuridad (si no, la calabaza no se ve de noche)
            n.put("Pos", lista(pos.x, pos.y, pos.z));
            e.readNbt(n);
            e.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0f, 0f);
            w.spawnEntity(e);
            return e;
        } catch (Throwable t) {
            EmiHuida.LOG.error("No se pudo crear el bloque {}", bloque, t);
            return null;
        }
    }

    /** «minecraft:jack_o_lantern[facing=south]» -> el NBT del estado del bloque. */
    private static NbtCompound estadoNbt(String spec) {
        NbtCompound bs = new NbtCompound();
        int i = spec.indexOf('[');
        bs.putString("Name", i < 0 ? spec : spec.substring(0, i));
        if (i >= 0) {
            NbtCompound props = new NbtCompound();
            for (String kv : spec.substring(i + 1, spec.length() - 1).split(",")) { String[] a = kv.split("="); if (a.length == 2) props.putString(a[0], a[1]); }
            bs.put("Properties", props);
        }
        return bs;
    }

    private static NbtList lista(double... v) { NbtList l = new NbtList(); for (double d : v) l.add(NbtDouble.of(d)); return l; }
    private static NbtList listaF(float... v) { NbtList l = new NbtList(); for (float d : v) l.add(NbtFloat.of(d)); return l; }

    /**
     * Coloca un bloque gigante: {@code pivote} = punto del mundo alrededor del que se gira todo (la base de la calabaza), {@code desde} = donde esta
     * la base-centro de ESTE bloque respecto al pivote (antes de girar), tamano (sx, sy, sz) y giro {@code rot}.
     */
    static void poner(Entity e, Vec3d pivote, Vector3f desde, Quaternionf rot, float sx, float sy, float sz) {
        if (e == null || e.isRemoved()) return;
        Vector3f tr = new Vector3f(desde).sub(sx / 2f, 0f, sz / 2f).rotate(rot);          // el bloque ocupa [0,1]^3 antes de escalar: se centra en x/z y se apoya en y
        NbtCompound n = new NbtCompound();
        n.put("Pos", lista(pivote.x, pivote.y, pivote.z));
        for (String tag : e.getCommandTags()) if (tag.startsWith("emi_huida_bloque:")) n.put("block_state", estadoNbt(tag.substring(17)));
        NbtCompound br = new NbtCompound(); br.putInt("block", 13); br.putInt("sky", 8);
        n.put("brightness", br);
        NbtCompound t = new NbtCompound();
        t.put("translation", listaF(tr.x, tr.y, tr.z));
        t.put("left_rotation", listaF(rot.x, rot.y, rot.z, rot.w));
        t.put("right_rotation", listaF(0f, 0f, 0f, 1f));
        t.put("scale", listaF(sx, sy, sz));
        n.put("transformation", t);
        e.readNbt(n);
    }

    // ---------------------------------------------------------------- Pokemon de las escenas

    /** Snorlax Emi gigante (~11 de alto), sin IA, ni gravedad, ni sonido propio. */
    static PokemonEntity snorlax(ServerWorld w, Vec3d pos, float yaw, float alto) {
        try {
            PokemonEntity e = PokemonProperties.Companion.parse("emipokemon:snorlax_emi level=60").createEntity(w);
            var sp = e.getPokemon().getSpecies();
            float h = Math.max(sp.getHitbox().height(), sp.getHitbox().width() * 0.8f) * sp.getBaseScale();
            e.getPokemon().setScaleModifier(Math.max(0.3f, Math.min(16f, alto / h)));
            e.setAiDisabled(true); e.setInvulnerable(true); e.setNoGravity(true); e.setSilent(true); e.setPersistent(); e.noClip = true;
            e.setCustomNameVisible(false);
            e.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0f);
            e.setHeadYaw(yaw); e.setBodyYaw(yaw);
            w.spawnEntity(e);
            return e;
        } catch (Throwable t) {
            EmiHuida.LOG.error("No se pudo crear a Snorlax Emi", t);
            return null;
        }
    }

    // ---------------------------------------------------------------- banco de pruebas (/huida prueba banco2)

    /** Solo para pruebas: Pumpkaboo, Snorlax Emi, la brujita (volando / flotando / de pie) y la calabaza gigante en fila delante del jugador, para verlos y medirlos. */
    static String banco(ServerPlayerEntity p) {
        ServerWorld w = p.getServerWorld();
        double r = Math.toRadians(p.getYaw());
        Vec3d f = new Vec3d(-Math.sin(r), 0, Math.cos(r)), d = new Vec3d(-Math.cos(r), 0, -Math.sin(r));
        float yaw = p.getYaw() + 180f;
        int n = 0;
        Vec3d base = p.getPos().add(f.multiply(24));
        PokemonEntity pk = Huida.nuevoPerseguidorDe(w, Niveles.CALABAZA, base.add(d.multiply(-30)), yaw);
        if (pk != null) { pk.addCommandTag("emi_huida_banco"); pk.setHeadYaw(yaw); pk.setBodyYaw(yaw); ((Corredor) (Object) pk).emi_huida$setPaso(0.3f); n++; }
        PokemonEntity sn = snorlax(w, base.add(d.multiply(-14)), yaw, 11f);
        if (sn != null) { sn.addCommandTag("emi_huida_banco"); n++; }
        String[] modos = {"fly", "hover", "stand"};
        for (int i = 0; i < 3; i++) {
            PokemonEntity m = Escenas.michi(w, "witch_hat", base.add(d.multiply(-2 + i * 4.5)).add(0, modos[i].equals("stand") ? 0 : 2, 0), yaw, 3.2f);
            if (m == null) continue;
            m.addCommandTag("emi_huida_banco");
            m.setBehaviourFlag(PokemonBehaviourFlag.FLYING, !modos[i].equals("stand"));
            ((Corredor) (Object) m).emi_huida$setPaso(modos[i].equals("fly") ? 0.3f : 0f);
            n++;
        }
        Vec3d c = base.add(d.multiply(14));
        Entity cuerpo = bloque(w, c, "minecraft:pumpkin"), tapa = bloque(w, c, "minecraft:pumpkin");
        if (cuerpo != null) { cuerpo.addCommandTag("emi_huida_banco"); poner(cuerpo, c, new Vector3f(0, 0, 0), new Quaternionf(), 5f, 3.6f, 5f); n++; }
        if (tapa != null) { tapa.addCommandTag("emi_huida_banco"); poner(tapa, c, new Vector3f(0, 3.6f, 0), new Quaternionf(), 5f, 1.2f, 5f); n++; }
        return n == 0 ? "No se pudo crear nada." : null;
    }

    // ---------------------------------------------------------------- utilidades

    /** Punto a {@code s} del camino, {@code v} a la derecha del eje y {@code dy} sobre el suelo, en la copia del jugador. */
    private static Vec3d P(Huida.J j, double s, double v, double dy) { return Escenas.P(j.hueco, s, v, dy); }

    /** Direccion (horizontal, unitaria) del camino a distancia s. */
    private static Vec3d adelante(double s) { double y = Math.toRadians(Nivel.rumbo(s)); return new Vec3d(-Math.sin(y), 0, Math.cos(y)); }

    /** Manda a los clientes cercanos la posicion exacta de una entidad (el seguimiento normal manda saltitos y se veia desplazada). */
    static void sync(Entity e) {
        if (e != null && !e.isRemoved() && e.getWorld() instanceof ServerWorld sw) sw.getChunkManager().sendToNearbyPlayers(e, new net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket(e));
    }

    private static void escala(PokemonEntity e, float f) { if (e != null && !e.isRemoved()) e.getPokemon().setScaleModifier(f); }

    /** Muelle amortiguado: 1 en t=0, oscila y se apaga (para rebotes). */
    private static double muelle(double dt, double tau, double w) { return Math.exp(-dt / tau) * Math.cos(dt * w); }

    private static void sonido(ServerWorld w, Vec3d pos, net.minecraft.sound.SoundEvent ev, float vol, float pitch) { Escenas.sonido(w, pos, ev, vol, pitch); }
    private static void sonido(ServerWorld w, Vec3d pos, String id, float vol, float pitch) { Escenas.sonido(w, pos, id, vol, pitch); }

    // ---------------------------------------------------------------- escena 1: la entrada
    // 0-100: la calabaza gigante tiembla y da saltitos como si hubiera algo dentro; 100: la tapa sale volando y del cuerpo sale el michi brujita;
    // 108: «¡LA CALABAZA!»; 114-148: hechizo; 148: la calabaza se transforma en Pumpkaboo, que ruge; la brujita se va riendo; 184: «¡CORRE!» y sube la musica.

    private static final int T_TIEMBLA = 14, T_NACE = 92, T_POP = 100, T_GRITO = 108, T_HECHIZO = 114, T_TRANS = 148, T_SE_VA = 150, T_CAM_FIN = 174;
    private static final float PW = 5f, PH_CUERPO = 3.6f, PH_TAPA = 1.2f;       // calabaza gigante: 5 de ancho, 3,6 de cuerpo y 1,2 de tapa

    /** Fuerza del temblor de la calabaza (0..1): crece desde que empieza a moverse hasta que revienta; durante el hechizo vuelve a temblar fuerte. */
    private static float fuerza(int t) {
        if (t < T_TIEMBLA) return 0f;
        if (t < T_POP) return Math.min(1f, (t - T_TIEMBLA) / 72f);
        if (t >= T_HECHIZO && t < T_TRANS) return 0.6f + 0.8f * (t - T_HECHIZO) / (float) (T_TRANS - T_HECHIZO);
        return 0f;
    }

    /** Saltito de la calabaza (bloques) en el tick t: cuatro saltos cada vez mas altos antes de reventar. */
    private static double salto(int t) {
        int[] ini = {46, 62, 76, 88};
        for (int k = 0; k < ini.length; k++) {
            int d = t - ini[k];
            if (d >= 0 && d < 9) return (0.35 + 0.28 * k) * Math.sin(Math.PI * d / 9.0);
        }
        return 0;
    }

    private static Vec3d vueloBrujita(Huida.J j, double u) {
        Vec3d dentro = P(j, Nivel.S_PERSEGUIDOR, 0, 2.4), cima = P(j, Nivel.S_PERSEGUIDOR + 1.0, 0, 8.6);
        return Escenas.bezier(dentro, dentro.add(0, 4.0, 0), cima.add(0, -0.5, 0), cima, u);
    }

    /** Donde flota la brujita mientras habla y hechiza. */
    private static Vec3d hoverBrujita(Huida.J j) { return P(j, Nivel.S_PERSEGUIDOR + 3.0, 0, 8.2); }

    static void intro(Huida.J j, ServerPlayerEntity p, ServerWorld w, int t) {
        int h = j.hueco;
        Vec3d base = P(j, Nivel.S_PERSEGUIDOR, 0, 0);
        float yawF = (float) Nivel.ejePos(h, Nivel.S_PERSEGUIDOR)[2];            // mirando hacia el jugador
        Vec3d centro = base.add(0, 2.4, 0);
        if (t < T_TRANS && j.extras.isEmpty()) {                                  // la calabaza (cuerpo y tapa)
            Entity cuerpo = bloque(w, base, "minecraft:pumpkin"), tapa = bloque(w, base, "minecraft:pumpkin");
            j.extras.add(cuerpo); j.extras.add(tapa);
            poner(cuerpo, base, new Vector3f(), new Quaternionf(), PW, PH_CUERPO, PW);
            poner(tapa, base, new Vector3f(0, PH_CUERPO, 0), new Quaternionf(), PW, PH_TAPA, PW);
        }
        Entity cuerpo = j.extras.size() > 0 ? j.extras.get(0) : null, tapa = j.extras.size() > 1 ? j.extras.get(1) : null;
        if (t == 0) Msg.actionBar(p, Text.literal("Una calabaza gigante... ¿se está moviendo?").formatted(Formatting.GOLD, Formatting.ITALIC));

        // ---- la calabaza: tiembla, da saltitos y revienta por arriba
        if (t < T_TRANS && cuerpo != null) {
            float f = fuerza(t);
            double hop = t < T_POP ? salto(t) : 0;
            Quaternionf rot = new Quaternionf().rotateZ((float) (f * 0.15 * Math.sin(t * 1.7))).rotateX((float) (f * 0.10 * Math.sin(t * 2.3 + 1)));
            float sy = 1f;
            if (t < T_POP) { int aterriza = -1; for (int ini : new int[]{46, 62, 76, 88}) if (t >= ini + 9 && t < ini + 13) aterriza = t - ini - 9; if (aterriza >= 0) sy = 1f - 0.07f * (1f - aterriza / 4f); }
            else if (t < T_POP + 24) sy = (float) (1 - 0.45 * muelle(t - T_POP, 4.0, 0.9));
            else if (t >= T_HECHIZO) sy = 1f + (float) (0.07 * Math.sin(t * 1.3) * f);
            Vec3d pivote = base.add(0, hop, 0);
            poner(cuerpo, pivote, new Vector3f(), rot, PW * (t >= T_HECHIZO ? 1f + 0.03f * f : 1f), PH_CUERPO * sy, PW * (t >= T_HECHIZO ? 1f + 0.03f * f : 1f));
            sync(cuerpo);
            if (tapa != null && !tapa.isRemoved()) {
                if (t < T_POP) {
                    float lift = (float) (f * 0.12 * (0.5 + 0.5 * Math.sin(t * 3.1)));
                    poner(tapa, pivote, new Vector3f(0, PH_CUERPO * sy + lift, 0), rot, PW, PH_TAPA, PW);
                } else {                      // la tapa sale volando hacia arriba y un lado, dando vueltas, y se desvanece al caer
                    int dt = t - T_POP;
                    if (dt > 30) { tapa.discard(); j.extras.remove(tapa); }
                    else {
                        Vec3d sp = new Vec3d(0.10 * dt, PH_CUERPO + 0.85 * dt - 0.026 * dt * dt, -0.14 * dt);        // sube, se va hacia un lado y se aleja de la camara (hacia el fondo del camino)
                        poner(tapa, base, new Vector3f((float) sp.x, (float) sp.y, (float) sp.z).rotate(new Quaternionf().rotateY((float) Math.toRadians(-yawF))),
                                new Quaternionf().rotateY((float) Math.toRadians(-yawF)).rotateZ(dt * 0.33f).rotateX(dt * 0.21f), PW, PH_TAPA, PW);
                        sync(tapa);
                        if (dt == 30) Escenas.particulas(w, ParticleTypes.POOF, base.add(sp.x, sp.y, 0), 14, 0.8, 0.5, 0.8, 0.04);
                    }
                }
            }
            // sonidos de que hay alguien dentro: golpes de madera, maullidos apagados y un poco de humo por la rendija
            if (t >= T_TIEMBLA && t < T_POP) {
                for (int ini : new int[]{46, 62, 76, 88}) if (t == ini + 9) {
                    sonido(w, centro, SoundEvents.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.5f, 0.55f + 0.05f * (ini / 30));
                    sonido(w, centro, SoundEvents.BLOCK_WOOD_BREAK, 1.2f, 0.5f);
                    Escenas.particulas(w, ParticleTypes.POOF, base.add(0, 0.2, 0), 8, 2.2, 0.1, 2.2, 0.02);
                }
                if (t == 28 || t == 38) sonido(w, centro, SoundEvents.BLOCK_WOOD_HIT, 1.0f, 0.6f);
                if (t == 52 || t == 69 || t == 84) sonido(w, centro, SoundEvents.ENTITY_CAT_AMBIENT, 1.2f, 0.55f);
                if (t == 56 || t == 82) sonido(w, centro, SoundEvents.ENTITY_CAT_PURREOW, 1.1f, 0.5f);
                if (t % 5 == 0 && t > 30) Escenas.particulas(w, ParticleTypes.SMOKE, base.add(0, PH_CUERPO + 0.1, 0), 2, 1.6, 0.05, 1.6, 0.01);
                if (t % 7 == 0 && t > 50) Escenas.particulas(w, ParticleTypes.SOUL_FIRE_FLAME, base.add(0, PH_CUERPO, 0), 1, 1.4, 0.05, 1.4, 0.02);
            }
        }

        // ---- la brujita: nace dentro (pequena, escondida), sale disparada al reventar la tapa, grita, hechiza y se va riendo
        PokemonEntity m = j.michi;
        if (t >= T_NACE && t < T_SE_VA + 40 && m == null && !(t > T_TRANS)) {
            j.michi = m = Escenas.michi(w, "witch_hat", centro, yawF + 180f, 0.5f);          // de cara a la camara (al jugador)
        }
        if (m != null) {
            Vec3d mp; boolean vuela = true; float yaw = yawF;
            if (t < T_POP) { mp = centro; escala(m, 0.5f); }
            else if (t < T_POP + 14) {
                double u = (t - T_POP) / 14.0, e = 1 - (1 - u) * (1 - u);
                mp = vueloBrujita(j, e);
                escala(m, 0.5f + 2.7f * (float) Escenas.suave(u * 1.4));
                yaw = yawF + (float) (360 * e);                                 // sale girando
            } else if (t < T_SE_VA) {
                mp = hoverBrujita(j).add(0.25 * Math.sin(t * 0.17), 0.3 * Math.sin(t * 0.23), 0);
                vuela = t < T_GRITO || t >= T_HECHIZO;
                yaw = yawF;
                if (t >= T_POP + 14) escala(m, 3.2f);
            } else {                            // se va volando hacia arriba y hacia el jugador (por encima del arco)
                double u = Math.min(1.0, (t - T_SE_VA) / 30.0), e = u * u;
                Vec3d a = hoverBrujita(j), b = P(j, Nivel.S_PERSEGUIDOR + 14, 0, 16), c = P(j, Nivel.S_PERSEGUIDOR + 34, 5, 34);
                mp = Escenas.bezier(a, a.add(0, 3, 0), b, c, e);
                yaw = yawF;
                if (t >= T_SE_VA + 34) { Escenas.quitar(m); j.michi = null; m = null; }
            }
            if (m != null) {
                Escenas.mover(m, mp, yaw, vuela);
                if (t % 6 == 0) sonido(w, mp, SoundEvents.ENTITY_PHANTOM_FLAP, 0.7f, 1.7f);
                if (t == T_POP) {
                    sonido(w, centro, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 0.8f, 1.4f);
                    sonido(w, centro, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.4f, 0.8f);
                    sonido(w, centro, SoundEvents.ENTITY_WITCH_CELEBRATE, 1.6f, 1.2f);
                    Escenas.particulas(w, ParticleTypes.EXPLOSION, centro.add(0, 1.5, 0), 1, 0.8, 0.4, 0.8, 0.0);
                    Escenas.particulas(w, new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.PUMPKIN_SEEDS)), centro.add(0, 2, 0), 40, 1.2, 0.8, 1.2, 0.25);
                    Escenas.particulas(w, new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.PUMPKIN_PIE)), centro.add(0, 2, 0), 16, 1.2, 0.8, 1.2, 0.2);
                    Escenas.particulas(w, ParticleTypes.CLOUD, centro.add(0, 2.2, 0), 30, 1.6, 0.4, 1.6, 0.08);
                }
                if (t >= T_POP && t < T_POP + 14 && t % 2 == 0) Escenas.particulas(w, ParticleTypes.WITCH, mp, 5, 0.6, 0.6, 0.6, 0.05);
                if (t == T_GRITO) {            // «¡LA CALABAZA!»
                    Escenas.anim(m, p, "cry");
                    sonido(w, mp, SoundEvents.ENTITY_CAT_AMBIENT, 1.8f, 1.2f);
                    sonido(w, mp, SoundEvents.ENTITY_WITCH_CELEBRATE, 1.8f, 1.0f);
                    j.globo = Escenas.globo(w, mp.add(0, 4.6, 0), "{\"text\":\"¡LA CALABAZA!\",\"color\":\"gold\",\"bold\":true}", 3.6f);
                    p.sendMessage(Text.literal("Michi Brujita: ").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD).append(Text.literal("¡LA CALABAZA!").formatted(Formatting.GOLD, Formatting.BOLD)), false);
                    Msg.actionBar(p, Text.literal("¡LA CALABAZA!").formatted(Formatting.GOLD, Formatting.BOLD));
                }
                if (t == T_GRITO + 8 || t == T_GRITO + 16) sonido(w, mp, SoundEvents.ENTITY_CAT_AMBIENT, 1.6f, t == T_GRITO + 8 ? 1.5f : 1.35f);
                if (t == T_HECHIZO) {
                    Escenas.anim(m, p, "special");
                    sonido(w, mp, "sfx.hechizo", 1.6f, 1f);
                    sonido(w, mp, SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, 1.4f, 1.1f);
                }
                if (t == T_HECHIZO + 16) Escenas.anim(m, p, "physical");
                if (t > T_HECHIZO && t < T_TRANS) {         // rayos mágicos de la brujita a la calabaza y remolino alrededor de ella
                    double u = (t - T_HECHIZO) / (double) (T_TRANS - T_HECHIZO);
                    for (int k = 0; k < 3; k++) {
                        double q = ((t * 0.21) + k / 3.0) % 1.0;
                        Vec3d pt = Escenas.lerp(mp.add(0, -0.8, 0), centro, q);
                        Escenas.particulas(w, k == 0 ? ParticleTypes.WITCH : k == 1 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.ENCHANT, pt, 3, 0.15, 0.15, 0.15, 0.02);
                    }
                    double ang = t * 0.5;
                    Escenas.particulas(w, ParticleTypes.FLAME, centro.add(Math.cos(ang) * 3.2, 1.0 + Math.sin(t * 0.3), Math.sin(ang) * 3.2), 2, 0.1, 0.1, 0.1, 0.01);
                    Escenas.particulas(w, ParticleTypes.WITCH, centro.add(Math.cos(ang + 3.14) * 3.2, 0.4 + Math.sin(t * 0.3), Math.sin(ang + 3.14) * 3.2), 2, 0.1, 0.1, 0.1, 0.01);
                    if (t % 10 == 0) sonido(w, centro, SoundEvents.BLOCK_BEACON_AMBIENT, 1.0f, 0.5f + 0.9f * (float) u);
                    if (t % 6 == 0) sonido(w, centro, SoundEvents.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.9f, 0.5f);
                }
                if (t == T_TRANS + 2) { Escenas.anim(m, p, "cry"); sonido(w, mp, SoundEvents.ENTITY_WITCH_CELEBRATE, 1.8f, 1.1f); sonido(w, mp, SoundEvents.ENTITY_CAT_AMBIENT, 1.6f, 1.6f); }
                if (t == T_TRANS + 16) sonido(w, mp, SoundEvents.ENTITY_WITCH_CELEBRATE, 1.4f, 0.9f);
            }
        }
        if (j.globo != null) {
            if (t >= T_GRITO + 40 || j.globo.isRemoved() || m == null) { if (!j.globo.isRemoved()) j.globo.discard(); j.globo = null; }
            else {
                double k = 1 + 0.04 * Math.sin(t * 1.4);
                Vec3d g = m.getPos().add(0, 4.6, 0);
                j.globo.refreshPositionAndAngles(g.x, g.y, g.z, 0f, 0f);
            }
        }

        // ---- la transformacion: destello, la calabaza desaparece y aparece el Pumpkaboo, que ruge
        if (t == T_TRANS) {
            Escenas.particulas(w, ParticleTypes.FLASH, centro, 1, 0, 0, 0, 0);
            Escenas.particulas(w, ParticleTypes.EXPLOSION_EMITTER, centro, 1, 0.6, 0.6, 0.6, 0);
            Escenas.particulas(w, ParticleTypes.POOF, centro, 26, 2.2, 1.8, 2.2, 0.08);
            Escenas.particulas(w, ParticleTypes.WITCH, centro, 40, 2.6, 2.2, 2.6, 0.2);
            Escenas.particulas(w, ParticleTypes.FLAME, centro, 30, 2.4, 2.0, 2.4, 0.12);
            Escenas.particulas(w, new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.PUMPKIN_PIE)), centro, 24, 2.0, 1.5, 2.0, 0.2);
            sonido(w, centro, "sfx.transformacion", 2.0f, 1f);
            sonido(w, centro, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 1.2f, 0.7f);
            Huida.quitarExtras(j);
            double[] e = Nivel.perseguidorPos(h, Nivel.S_PERSEGUIDOR);
            PokemonEntity pk = Huida.nuevoPerseguidor(w, e[0], e[1], e[2], (float) e[3]);
            j.perseguidor = pk;
            if (pk != null) escala(pk, 0.3f);
        }
        if (t > T_TRANS && j.perseguidor != null && !j.perseguidor.isRemoved()) {
            PokemonEntity pk = j.perseguidor;
            double[] e = Nivel.perseguidorPos(h, Nivel.S_PERSEGUIDOR);
            int dt = t - T_TRANS;
            float full = pk.getPokemon().getSpecies() != null ? 1f : 1f;
            if (dt <= 12) {                                    // crece desde pequeno con un rebote
                double u = dt / 12.0;
                float k = (float) (0.3 + 0.7 * u + 0.18 * Math.sin(Math.PI * u) );
                escala(pk, Math.max(0.3f, k) * baseEscala(pk));
            }
            Escenas.mover(pk, new Vec3d(e[0], e[1] + 0.35 * Math.sin(t * 0.25), e[2]), (float) e[3], true);
            if (dt == 3) { Huida.rugir(pk); Msg.actionBar(p, Text.literal("¡Ha cobrado vida!").formatted(Formatting.GOLD, Formatting.BOLD)); }
            if (dt == 4) sonido(w, pk.getPos().add(0, 3, 0), SoundEvents.ENTITY_GHAST_SCREAM, 1.2f, 0.6f);
            if (dt < 16 && dt % 2 == 0) Escenas.particulas(w, ParticleTypes.POOF, pk.getPos().add(0, 3, 0), 6, 1.8, 1.8, 1.8, 0.05);
            if (dt > 8 && dt % 6 == 0) Escenas.particulas(w, ParticleTypes.SOUL_FIRE_FLAME, pk.getPos().add(0, 3.2, 0), 2, 2.0, 1.2, 2.0, 0.02);
        }

        // ---- la camara
        float temblor = 0f;
        if (t >= T_POP && t < T_POP + 10) temblor = (float) (0.2 * (1 - (t - T_POP) / 10.0));
        if (t >= T_HECHIZO && t < T_TRANS) temblor = 0.03f + 0.07f * (t - T_HECHIZO) / (float) (T_TRANS - T_HECHIZO);
        if (t >= T_TRANS && t < T_TRANS + 20) temblor = (float) (0.26 * (1 - (t - T_TRANS) / 20.0));
        if (t < 46) {                              // plano general desde fuera del arco: la calabaza quieta al fondo y empieza a moverse
            double u = Escenas.suave(t / 46.0);
            camara(p, Escenas.lerp(P(j, 41, 1.6, 6.5), P(j, 33.5, 0.8, 4.6), u), Escenas.lerp(P(j, 14, 0, 2.6), P(j, 14, 0, 2.2), u), 1.0f, 0.01f * fuerza(t), t == 0);
        } else if (t < 82) {                       // desde la boca del arco, de lado: la calabaza entera con su tapa, temblando
            double u = Escenas.suave((t - 46) / 36.0);
            camara(p, Escenas.lerp(P(j, 29.0, 2.4, 2.8), P(j, 27.0, 1.8, 2.3), u), P(j, 14, 0, 2.4 + (float) salto(t)), 0.95f, 0.02f * fuerza(t), t == 46);
        } else if (t < T_POP) {                    // mas cerca y abajo: los saltos y los golpes antes de reventar
            double u = Escenas.suave((t - 82) / (double) (T_POP - 82));
            camara(p, Escenas.lerp(P(j, 22.5, 2.2, 1.8), P(j, 21.0, 1.8, 1.5), u), P(j, 14, 0, 3.2 + (float) salto(t)), 1.0f, 0.03f * fuerza(t), t == 82);
        } else if (t < T_GRITO + 6) {              // al reventar: desde abajo, la brujita sale disparada hacia arriba
            double u = Escenas.suave((t - T_POP) / 14.0);
            camara(p, P(j, 24.5, -1.8, 1.4), Escenas.lerp(P(j, 14, 0, 4.5), m != null ? m.getPos().add(0, 1.5, 0) : hoverBrujita(j).add(0, 1.5, 0), u), 0.95f, temblor, t == T_POP);
        } else if (t < T_TRANS) {                  // primer plano de la brujita gritando y hechizando
            double u = Escenas.suave((t - T_GRITO - 6) / (double) (T_TRANS - T_GRITO - 6));
            Vec3d mira = m != null ? m.getPos().add(0, 1.7, 0) : hoverBrujita(j).add(0, 1.7, 0);
            camara(p, Escenas.lerp(P(j, 29.0, 0.6, 5.4), P(j, 27.0, 0.4, 5.8), u), Escenas.lerp(mira.add(0, 0.3, 0), Escenas.lerp(mira, P(j, 14, 0, 4.0), 0.4), u), 0.95f, temblor, t == T_GRITO + 6);
        } else if (t < T_CAM_FIN) {                // la revelacion: el Pumpkaboo ruge
            double u = Escenas.suave((t - T_TRANS) / (double) (T_CAM_FIN - T_TRANS));
            camara(p, Escenas.lerp(P(j, 23.0, 0.3, 1.8), P(j, 27.0, 0.6, 3.0), u), P(j, 14, 0, 4.0), 0.95f, temblor, t == T_TRANS);
        } else if (t == T_CAM_FIN) {
            Escenas.camaraOff(p);
        }
    }

    private static float baseEscala(PokemonEntity e) {
        var sp = e.getPokemon().getSpecies();
        float hh = Math.max(sp.getHitbox().height(), sp.getHitbox().width() * 0.8f) * sp.getBaseScale();
        return hh > 0.01f ? Math.max(0.3f, Math.min(16f, Niveles.CALABAZA.alto() / hh)) : 1f;
    }

    private static void camara(ServerPlayerEntity pl, Vec3d pos, Vec3d mira, float fov, float temblor, boolean corte) { Escenas.camara(pl, pos, mira, fov, temblor, corte); }

    /** Punto de camara a {@code s} del camino, {@code v} de lado y {@code y} de alto; {@code clave} permite cambiar los tres numeros desde -Demi.huida.dbgcam (pruebas). */
    private static Vec3d cp(Huida.J j, String clave, double s, double v, double y) {
        return P(j, Cfg.dbg(clave + ".s", s), Cfg.dbg(clave + ".v", v), Cfg.dbg(clave + ".y", y));
    }

    // ---------------------------------------------------------------- escena 2: Snorlax Emi se come al Pumpkaboo (al llegar a la meta)
    // 0: el Pumpkaboo se para detras del jugador; 16: Snorlax Emi echa a andar por el camino desde detras (el suelo tiembla); 44: el Pumpkaboo lo nota y se asusta;
    // 106: Snorlax abre la boca y se lo traga (lo aspira); 140: glup; 150: «¡ESCAPASTE!»; 170: eructa y da las gracias; 190: se echa a dormir; 250: fin.

    /** Alto de Snorlax Emi en la escena y donde esta su boca respecto a su centro (bloques por delante / altura sobre sus pies): se mide en el juego con /huida prueba banco2. */
    static final float SN_ALTO = 11f;
    static final double SN_BOCA_ADELANTE = 1.7, SN_BOCA_ALTO = 8.6;       // medidos con una regla de bloques y con el modelo (cara a 8/16 y boca a 43/16 del centro, x3,28)
    private static final int S_NACE = 16, S_ASUSTA = 44, S_PARA = 104, S_SORBE = 106, S_TRAGA = 140, S_TITULO = 150, S_ERUCTA = 166, S_GRACIAS = 176, S_DUERME = 196, S_CAM_FIN = 238;
    /** Altura del centro del Pumpkaboo sobre sus pies (su cuerpo anaranjado; con el tallo y las hojas mide ~7,3 en total, medido en el juego: la formula del hitbox se queda corta). */
    static final double PK_CENTRO = 2.9;
    private static final double SN_DIST_PARADO = 9.0;        // distancia entre los centros de Snorlax y el Pumpkaboo cuando Snorlax se para

    /** Punto a {@code adelante} / {@code alto} del centro de una entidad que mira por el camino (para la boca de Snorlax). */
    private static Vec3d delante(Entity e, double adelante, double alto) {
        double r = Math.toRadians(e.getYaw());
        return e.getPos().add(-Math.sin(r) * adelante, alto, Math.cos(r) * adelante);
    }

    static void finalSnorlax(Huida.J j, ServerPlayerEntity p, ServerWorld w, int t) {
        PokemonEntity pk = j.perseguidor;
        int h = j.hueco;
        if (t == 0) {
            if (pk == null || pk.isRemoved()) { Escenas.camaraOff(p); return; }
            j.escBase = pk.getPos();
            ((Corredor) (Object) pk).emi_huida$setPaso(0.3f); pk.setSprinting(false);
        }
        double sPl = j.s, sPk0 = j.sPerseguidor;
        float yawF = (float) Nivel.ejePos(h, sPk0)[2];
        PokemonEntity sn = j.michi;

        // ---- Snorlax Emi: llega andando desde detras
        double sStop = sPk0 - SN_DIST_PARADO;
        if (t >= S_NACE && sn == null && t < S_DUERME + 60) {
            double[] e = Nivel.ejePos(h, sPk0 - 36);
            j.michi = sn = snorlax(w, new Vec3d(e[0], Nivel.Y0, e[1]), yawF, SN_ALTO);
        }
        if (sn != null && !sn.isRemoved()) {
            double u = Escenas.suave((t - S_NACE) / (double) (S_PARA - S_NACE));
            double sS = t < S_PARA ? Escenas.lerp(sPk0 - 36, sStop, Math.min(1.0, Math.max(0.0, (t - S_NACE) / (double) (S_PARA - S_NACE)))) : sStop;
            double[] e = Nivel.ejePos(h, sS);
            boolean anda = t >= S_NACE && t < S_PARA;
            sn.refreshPositionAndAngles(e[0], Nivel.Y0, e[1], yawF, 0f);
            sn.setHeadYaw(yawF); sn.setBodyYaw(yawF);
            ((Corredor) (Object) sn).emi_huida$setPaso(anda ? 0.6f * 6.5f / 20f : 0f);
            sn.setSprinting(false);
            sync(sn);
            if (anda && (t - S_NACE) % 12 == 6) {               // pisadas que hacen temblar el suelo
                Vec3d pie = sn.getPos();
                sonido(w, pie, SoundEvents.ENTITY_IRON_GOLEM_STEP, 2.2f, 0.35f);
                sonido(w, pie, SoundEvents.BLOCK_ANVIL_LAND, 0.7f, 0.35f);
                Escenas.particulas(w, ParticleTypes.POOF, pie.add(0, 0.2, 0), 8, 2.6, 0.1, 1.2, 0.03);
                Escenas.particulas(w, ParticleTypes.CAMPFIRE_COSY_SMOKE, pie.add(0, 0.3, 0), 2, 2.4, 0.1, 1.0, 0.01);
            }
            if (t == S_NACE + 2) Msg.actionBar(p, Text.literal("El suelo tiembla...").formatted(Formatting.GRAY, Formatting.ITALIC));
            if (t == S_PARA - 2) { Msg.actionBar(p, Text.literal("¡Snorlax Emi!").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD)); }
            if (t == S_SORBE) { Huida.rugir(sn); Escenas.anim(sn, p, "cry"); }
        }

        // ---- el Pumpkaboo: se queda parado, nota algo detras, se asusta y lo aspiran
        if (pk != null && !pk.isRemoved() && t < S_TRAGA) {
            Vec3d b = j.escBase != null ? j.escBase : pk.getPos();
            Vec3d pos = b; float yaw = yawF;
            double sP = sPk0;
            if (t >= S_ASUSTA && t < S_SORBE) {
                // se da la vuelta y se echa hacia el jugador (todo lo que puede) temblando de miedo
                double u = Escenas.suave((t - S_ASUSTA) / 14.0);
                yaw = Escenas.lerpAng(yawF, yawF + 180f, u);
                double ds = Math.min(2.5, Math.max(0.0, sPl - 3.0 - sPk0)) * Escenas.suave((t - S_ASUSTA - 20) / 40.0);
                sP = sPk0 + ds;
                double[] e = Nivel.perseguidorPos(h, sP);
                pos = new Vec3d(e[0], e[1], e[2]).add(0.18 * Math.sin(t * 2.6), 0.12 * Math.sin(t * 3.1), 0.18 * Math.cos(t * 2.9));
            } else if (t < S_ASUSTA) {
                double[] e = Nivel.perseguidorPos(h, sPk0);
                pos = new Vec3d(e[0], e[1], e[2]);
            }
            float esc = 1f;
            if (t >= S_SORBE) {                         // lo aspiran hacia la boca: gira, se encoge y se acelera
                double u = Math.min(1.0, (t - S_SORBE) / (double) (S_TRAGA - S_SORBE)), e = u * u;
                double sP1 = sPk0 + Math.min(2.5, Math.max(0.0, sPl - 3.0 - sPk0));
                double[] c = Nivel.perseguidorPos(h, sP1);
                Vec3d ini = new Vec3d(c[0], c[1], c[2]);
                esc = (float) (1.0 - 0.88 * e);
                Vec3d boca = sn != null ? delante(sn, SN_BOCA_ADELANTE + 0.5, SN_BOCA_ALTO) : ini;
                double yBase = Escenas.lerp(ini.y, boca.y - PK_CENTRO * esc, e);       // el Pumpkaboo se mide desde sus pies: su centro queda PK_CENTRO (por la escala) mas arriba
                Vec3d xz = Escenas.lerp(ini, boca, e);
                pos = new Vec3d(xz.x + Math.cos(t * 0.9) * 1.2 * (1 - e), yBase, xz.z + Math.sin(t * 0.9) * 1.2 * (1 - e));
                yaw = yawF + 180f + (t - S_SORBE) * 28f;
                if (t % 2 == 0 && sn != null) {
                    Vec3d bo = delante(sn, SN_BOCA_ADELANTE, SN_BOCA_ALTO);
                    Escenas.particulas(w, ParticleTypes.CLOUD, Escenas.lerp(bo, pos.add(0, 3, 0), 0.35 + 0.5 * Math.random()), 1, 0.2, 0.2, 0.2, 0.0);
                    Escenas.particulas(w, ParticleTypes.SOUL_FIRE_FLAME, Escenas.lerp(bo, pos.add(0, 3, 0), 0.2 + 0.6 * Math.random()), 1, 0.3, 0.3, 0.3, 0.01);
                }
            }
            escala(pk, esc * baseEscala(pk));
            Escenas.mover(pk, pos, yaw, true);
            if (t == S_ASUSTA) { Huida.rugir(pk); sonido(w, pos.add(0, 3, 0), SoundEvents.ENTITY_GHAST_SCREAM, 1.0f, 1.5f); }
            if (t >= S_ASUSTA && t < S_SORBE && t % 6 == 0) Escenas.particulas(w, ParticleTypes.FALLING_WATER, pos.add(0, 6.0, 0), 3, 1.5, 0.2, 1.5, 0.0);
        }
        if (t == S_TRAGA) {                              // glup: el Pumpkaboo desaparece en la boca de Snorlax
            Vec3d bo = sn != null ? delante(sn, SN_BOCA_ADELANTE, SN_BOCA_ALTO) : new Vec3d(0, 0, 0);
            if (pk != null && !pk.isRemoved()) pk.discard();
            j.perseguidor = null;
            if (sn != null) {
                sonido(w, bo, "sfx.glup", 2.0f, 0.55f);
                sonido(w, bo, SoundEvents.ENTITY_GENERIC_EAT, 2.0f, 0.5f);
                sonido(w, bo, SoundEvents.ENTITY_PLAYER_BURP, 1.2f, 0.3f);
                Escenas.particulas(w, new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.PUMPKIN_PIE)), bo, 22, 0.6, 0.5, 0.6, 0.14);
                Escenas.particulas(w, new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.PUMPKIN_SEEDS)), bo, 18, 0.6, 0.5, 0.6, 0.12);
                Escenas.particulas(w, ParticleTypes.POOF, bo, 8, 0.5, 0.4, 0.5, 0.03);
                Escenas.anim(sn, p, "recoil");
            }
        }
        if (sn != null && !sn.isRemoved()) {
            if (t == S_TRAGA + 18 || t == S_TRAGA + 34) { Escenas.anim(sn, p, "recoil"); sonido(w, sn.getPos().add(0, 6, 0), SoundEvents.ENTITY_GENERIC_EAT, 1.8f, 0.5f + (t == S_TRAGA + 34 ? 0.08f : 0f)); }
            if (t == S_TITULO) { Huida.titulosEscape(j, p, 40); Escenas.particulas(w, ParticleTypes.HEART, delante(sn, 2, 8.5), 16, 2.6, 1.0, 1.6, 0.05); }
            if (t == S_ERUCTA) {
                Vec3d bo = delante(sn, SN_BOCA_ADELANTE, SN_BOCA_ALTO);
                Huida.rugir(sn);
                sonido(w, bo, SoundEvents.ENTITY_PLAYER_BURP, 2.0f, 0.4f);
                Escenas.particulas(w, ParticleTypes.CLOUD, bo, 14, 0.5, 0.4, 0.5, 0.08);
                Escenas.particulas(w, new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.PUMPKIN_SEEDS)), bo, 12, 0.4, 0.3, 0.4, 0.2);
                Msg.actionBar(p, Text.literal("*¡Burp!*").formatted(Formatting.GREEN, Formatting.ITALIC));
            }
            if (t == S_GRACIAS) {
                j.globo = Escenas.globo(w, sn.getPos().add(0, SN_ALTO + 2.6, 0), "{\"text\":\"¡Gracias por la cena!\",\"color\":\"gold\",\"bold\":true}", 4.0f);
                p.sendMessage(Text.literal("Snorlax Emi: ").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD).append(Text.literal("¡Gracias por la cena, " + j.nombre + "!").formatted(Formatting.GOLD)), false);
            }
            if (t == S_DUERME) {                          // se echa a dormir
                sn.setEnablePoseTypeRecalculation(false);
                sn.getDataTracker().set(PokemonEntity.Companion.getPOSE_TYPE(), PoseType.SLEEP);
                ((Corredor) (Object) sn).emi_huida$setPaso(0f);
                if (j.globo != null && !j.globo.isRemoved()) j.globo.discard();
                j.globo = Escenas.globo(w, sn.getPos().add(0, SN_ALTO + 1.6, 0), "{\"text\":\"Zzz...\",\"color\":\"aqua\",\"bold\":true}", 4.5f);
            }
            if (t > S_DUERME && t % 30 == 0) sonido(w, sn.getPos().add(0, 3, 0), SoundEvents.ENTITY_FOX_SLEEP, 1.4f, 0.4f);
        }
        if (j.globo != null && sn != null && !sn.isRemoved() && t >= S_GRACIAS) {
            if (t < S_DUERME && t >= S_DUERME - 12) { }          // (el globo de agradecimiento se cambia por el de «Zzz» al dormirse)
            if (t > S_GRACIAS + 3) { Vec3d g = sn.getPos().add(0, SN_ALTO + (t >= S_DUERME ? 1.6 : 2.6), 0); j.globo.refreshPositionAndAngles(g.x, g.y, g.z, 0f, 0f); }
        }

        // ---- la camara: siempre desde el lado de la meta, delante del jugador, mirando hacia atras por el pasillo (Snorlax mide ~11: hay que verlo desde lejos)
        float temblor = 0f;
        if (t >= S_NACE && t < S_PARA && (t - S_NACE) % 12 < 5) temblor = 0.04f + 0.05f * (t - S_NACE) / (float) (S_PARA - S_NACE);
        if (t >= S_SORBE && t < S_TRAGA) temblor = 0.05f;
        if (t >= S_TRAGA && t < S_TRAGA + 8) temblor = 0.1f;
        double sCam = Math.min(sPl + 3.5, Nivel.S_TOTAL - 1.0);
        Vec3d mira1 = pk != null && !pk.isRemoved() ? pk.getPos().add(0, PK_CENTRO, 0) : P(j, sPk0, 0, PK_CENTRO);
        Vec3d snC = sn != null && !sn.isRemoved() ? sn.getPos().add(0, 5.5, 0) : P(j, sStop, 0, 5.5);
        if (t < S_ASUSTA) {                  // alto: el Pumpkaboo parado detras del jugador y, al fondo, el pasillo
            double u = Escenas.suave(t / (double) S_ASUSTA);
            camara(p, Escenas.lerp(cp(j, "f1a", sCam, 1.0, 13.0), cp(j, "f1b", sCam, 1.0, 10.0), u), Escenas.lerp(P(j, sPk0 - 8, 0, 3.0), Escenas.lerp(mira1, snC, 0.4), u), 1.0f, temblor, t == 0);
        } else if (t < S_SORBE) {            // alto y en diagonal: el Pumpkaboo se asusta y Snorlax llega por detras (en fila no se vería: el Pumpkaboo lo taparía)
            double u = Escenas.suave((t - S_ASUSTA) / (double) (S_SORBE - S_ASUSTA));
            camara(p, Escenas.lerp(cp(j, "f2a", sCam + 0.5, -9.0, 13.0), cp(j, "f2b", sPk0 - 3.0, -10.0, 13.5), u), Escenas.lerp(Escenas.lerp(mira1, snC, 0.45), snC, u * 0.5), 1.0f, temblor, t == S_ASUSTA);
        } else if (t < S_TRAGA + 4) {        // de lado y alto: Snorlax abre la boca y aspira al Pumpkaboo
            double u = Escenas.suave((t - S_SORBE) / (double) (S_TRAGA - S_SORBE + 4));
            Vec3d boca = sn != null ? delante(sn, SN_BOCA_ADELANTE, SN_BOCA_ALTO) : mira1;
            camara(p, Escenas.lerp(cp(j, "f3a", sPk0 - 4.5, -11.0, 13.0), cp(j, "f3b", sPk0 - 4.5, -9.5, 11.5), u), Escenas.lerp(mira1, boca, 0.55), (float) Cfg.dbg("f3.fov", 1.05), temblor, t == S_SORBE);
        } else if (t < S_CAM_FIN) {          // Snorlax mastica, eructa, da las gracias y se duerme (con el jugador en primer plano)
            double u = Escenas.suave((t - S_TRAGA - 4) / (double) (S_CAM_FIN - S_TRAGA - 4));
            Vec3d cara = sn != null ? delante(sn, SN_BOCA_ADELANTE - 0.6, SN_BOCA_ALTO - 0.4) : P(j, sStop, 0, 6);
            camara(p, Escenas.lerp(cp(j, "f4a", sCam, 0.8, 8.5), cp(j, "f4b", sCam, 0.8, 10.0), u), Escenas.lerp(cara, snC, u * 0.7), 1.0f, temblor, t == S_TRAGA + 4);
        } else if (t == S_CAM_FIN) {
            Escenas.camaraOff(p);
        }
    }

    // ---------------------------------------------------------------- escena 3: el Pumpkaboo te convierte en calabaza
    // 0: ruge y se eleva; 4-34: lanza un hechizo sobre el jugador (rayos y remolino); 34: ¡PUF! el jugador se convierte en una calabaza tallada (el jugador queda invisible);
    // 34-64: la camara se acerca a la calabaza (que bota y se revuelve, con maullidos apagados); 64: «¡TE ATRAPÓ!»; 104: el Pumpkaboo se rie; 120: fin de la camara; 130: fin.

    private static final int G_CONVIERTE = 34, G_TITULO = 64, G_RIE = 96, G_CAM_FIN = 120;

    static void tragado(Huida.J j, ServerPlayerEntity p, ServerWorld w, int t) {
        PokemonEntity pk = j.perseguidor;
        if (pk == null || pk.isRemoved()) { Escenas.camaraOff(p); return; }
        int h = j.hueco;
        if (t == 0) {
            j.escBase = pk.getPos();
            ((Corredor) (Object) pk).emi_huida$setPaso(0.3f); pk.setSprinting(false);
            Huida.rugir(pk);
            sonido(w, pk.getPos().add(0, 3, 0), SoundEvents.ENTITY_GHAST_SCREAM, 1.6f, 0.55f);
            sonido(w, pk.getPos().add(0, 3, 0), SoundEvents.ENTITY_WITCH_CELEBRATE, 1.4f, 0.7f);
            sonido(w, p.getPos(), "sfx.hechizo", 1.6f, 1f);
            Msg.actionBar(p, Text.literal("¡El Pumpkaboo te está lanzando un hechizo!").formatted(Formatting.GOLD, Formatting.BOLD));
        }
        Vec3d b = j.escBase != null ? j.escBase : pk.getPos();
        float yaw = (float) Nivel.rumbo(j.sPerseguidor);
        Vec3d fw = adelante(j.s);
        float base = baseEscala(pk);
        Vec3d pos = b.add(0, 0.6 * Escenas.suave(t / 24.0) + 0.3 * Math.sin(t * 0.3), 0);       // se eleva un poco y se mece
        escala(pk, base * (1f + 0.12f * (float) Escenas.suave(t / 20.0)));
        Escenas.mover(pk, pos, yaw, true);
        Vec3d centroPk = pos.add(0, PK_CENTRO, 0);
        Vec3d jug = p.getPos();                                                                   // los pies del jugador (esta parado)
        Vec3d pecho = jug.add(0, 0.9, 0);
        double ang = t * 0.5;
        if (t >= 4 && t < G_CONVIERTE) {          // rayos de la cara del Pumpkaboo al jugador y un remolino que se cierra alrededor de el
            Vec3d ojos = centroPk.add(fw.multiply(1.6)).add(0, 0.6, 0);
            for (int k = 0; k < 3; k++) {
                double q = ((t * 0.21) + k / 3.0) % 1.0;
                Escenas.particulas(w, k == 0 ? ParticleTypes.WITCH : k == 1 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.ENCHANT, Escenas.lerp(ojos, pecho, q), 3, 0.15, 0.15, 0.15, 0.02);
            }
            double r = 2.2 - 1.6 * (t - 4) / (double) (G_CONVIERTE - 4);
            Escenas.particulas(w, ParticleTypes.FLAME, pecho.add(Math.cos(ang) * r, 0.9 + Math.sin(t * 0.3) * 0.6, Math.sin(ang) * r), 2, 0.05, 0.05, 0.05, 0.01);
            Escenas.particulas(w, ParticleTypes.WITCH, pecho.add(Math.cos(ang + 3.14) * r, -0.2 + Math.sin(t * 0.3) * 0.6, Math.sin(ang + 3.14) * r), 2, 0.05, 0.05, 0.05, 0.01);
            if (t % 8 == 0) sonido(w, pecho, SoundEvents.BLOCK_BEACON_AMBIENT, 1.0f, 0.5f + 0.9f * (t - 4) / (float) (G_CONVIERTE - 4));
        }

        // ---- ¡PUF! el jugador se convierte en una calabaza tallada y encendida que mira a la camara
        float heading = (float) Math.toRadians(Nivel.rumbo(j.s));
        if (t == G_CONVIERTE) {
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 20 * 60, 0, false, false, false));
            Entity cal = bloque(w, jug, "minecraft:jack_o_lantern[facing=south]");
            j.extras.add(cal);
            Escenas.particulas(w, ParticleTypes.FLASH, pecho, 1, 0, 0, 0, 0);
            Escenas.particulas(w, ParticleTypes.POOF, pecho, 30, 0.7, 0.9, 0.7, 0.06);
            Escenas.particulas(w, ParticleTypes.WITCH, pecho, 40, 0.8, 1.0, 0.8, 0.25);
            Escenas.particulas(w, ParticleTypes.FLAME, pecho, 20, 0.6, 0.8, 0.6, 0.1);
            Escenas.particulas(w, new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.PUMPKIN_SEEDS)), pecho, 24, 0.5, 0.6, 0.5, 0.15);
            sonido(w, pecho, "sfx.transformacion", 1.8f, 1.2f);
            sonido(w, pecho, "sfx.derrota_calabaza", 1.2f, 1f);
            sonido(w, pecho, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 0.8f, 1.5f);
            p.sendMessage(Text.literal("¡El Pumpkaboo te ha convertido en una calabaza!").formatted(Formatting.GOLD), false);
        }
        Entity cal = j.extras.isEmpty() ? null : j.extras.get(0);
        if (t >= G_CONVIERTE && cal != null && !cal.isRemoved()) {
            int dt = t - G_CONVIERTE;
            float tam = dt < 10 ? (float) (0.3 + 1.4 * Escenas.suave(dt / 10.0) + 0.4 * Math.sin(Math.PI * dt / 10.0)) : 1.7f;      // crece de golpe con rebote
            double dec = Math.exp(-Math.max(0, dt - 10) / 40.0);
            double bota = 0;
            for (int ini : new int[]{12, 30, 46, 62}) { int d = dt - ini; if (d >= 0 && d < 8) bota = Math.max(bota, 0.45 * dec * Math.sin(Math.PI * d / 8.0)); }       // intenta moverse a saltitos
            Quaternionf rot = new Quaternionf().rotateY(-heading).rotateZ((float) (0.16 * dec * Math.sin(dt * 0.9))).rotateX((float) (0.10 * dec * Math.sin(dt * 1.3 + 1)));
            poner(cal, jug.add(0, bota, 0), new Vector3f(), rot, tam, tam, tam);
            sync(cal);
            if (dt == 12 || dt == 30 || dt == 46) sonido(w, pecho, SoundEvents.ENTITY_CAT_AMBIENT, 1.2f, 0.55f);          // maullidos apagados desde dentro
            if (dt % 6 == 0) Escenas.particulas(w, ParticleTypes.SOUL_FIRE_FLAME, jug.add(0, 1.9, 0), 1, 0.3, 0.1, 0.3, 0.01);
        }

        // ---- «¡TE ATRAPÓ!» con la camara encima de la calabaza
        if (t == G_TITULO) {
            Msg.title(p, Text.literal("¡TE ATRAPÓ!").formatted(Formatting.GOLD, Formatting.BOLD),
                    Text.literal(j.devorado ? "★ " + j.puntos + " · ¡Demasiados errores! Ahora eres una calabaza" : "★ " + j.puntos + " · Errores: " + j.errores + " · Ahora eres una calabaza").formatted(Formatting.GRAY), 0, 55, 20);
            sonido(w, pecho, SoundEvents.ENTITY_WITCH_CELEBRATE, 1.4f, 0.8f);
        }
        if (t == G_RIE) { Huida.rugir(pk); sonido(w, centroPk, SoundEvents.ENTITY_WITCH_CELEBRATE, 1.6f, 0.9f); }

        // ---- la camara: de lejos y en diagonal mientras hechiza, y luego se acerca a la calabaza hasta el primer plano
        float temblor = (t >= G_CONVIERTE && t < G_CONVIERTE + 10) ? 0.14f : 0f;
        double sPl = j.s, sCam = Math.min(sPl + 6.5, Nivel.S_TOTAL - 1.0);
        if (t < 12) return;                                       // al principio se ve la camara normal (el Pumpkaboo se frena y se eleva)
        if (t < G_CONVIERTE) {
            double u = Escenas.suave((t - 12) / (double) (G_CONVIERTE - 12));
            camara(p, Escenas.lerp(cp(j, "g1a", sPl + 6.5, -6.0, 8.5), cp(j, "g1b", sPl + 5.5, -5.0, 7.0), u), Escenas.lerp(centroPk, pecho, 0.5 + 0.3 * u), 1.0f, 0.02f, t == 12);
        } else if (t < G_TITULO) {
            double u = Escenas.suave((t - G_CONVIERTE) / (double) (G_TITULO - G_CONVIERTE));
            camara(p, Escenas.lerp(cp(j, "g2a", Math.min(sPl + 9.0, Nivel.S_TOTAL - 1.0), 0.8, 3.6), cp(j, "g2b", Math.min(sPl + 4.0, Nivel.S_TOTAL - 1.0), 0.3, 2.2), u), pecho.add(0, 0.3 * u, 0), (float) Cfg.dbg("g2.fov", 1.0), temblor, t == G_CONVIERTE);
        } else if (t < G_CAM_FIN) {
            double u = Escenas.suave((t - G_TITULO) / (double) (G_CAM_FIN - G_TITULO));
            camara(p, Escenas.lerp(cp(j, "g3a", Math.min(sPl + 4.0, Nivel.S_TOTAL - 1.0), 0.3, 2.2), cp(j, "g3b", Math.min(sPl + 3.4, Nivel.S_TOTAL - 1.0), 0.1, 1.9), u), pecho.add(0, 0.4, 0), (float) Cfg.dbg("g3.fov", 0.95), 0f, t == G_TITULO);
        } else if (t == G_CAM_FIN) {
            Escenas.camaraOff(p);
        }
    }
}
