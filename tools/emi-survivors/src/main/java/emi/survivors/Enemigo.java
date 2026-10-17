package emi.survivors;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * Un Pokemon enemigo: un {@link PokemonEntity} real de Cobblemon sin IA, invulnerable y silencioso, que el mod mueve cada tick hacia el jugador con la fisica
 * normal (se pone su velocidad y Minecraft resuelve las colisiones, la gravedad y la animacion de andar). Su vida es nuestra, no la de Minecraft.
 */
final class Enemigo {
    /** DEBIL y FUERTE = los normales (tramo bajo y alto de estadisticas), MICHI = un Michialien (el toque de Emi), ELITE = uno grande con mucha vida, JEFE = el jefe. */
    enum Tipo { DEBIL, FUERTE, MICHI, ELITE, JEFE }

    final PokemonEntity e;
    final Especies.E especie;
    final Tipo tipo;
    int xp;                     // experiencia que suelta al caer
    float hp, hpMax;
    float dmg;
    double vel;                 // bloques por tick
    double radio;               // radio de contacto (bloques)
    int cdContacto;
    Run ultimo;                 // el jugador que le dio el ultimo golpe (a el se le cuenta como derrotado)
    double kbx, kbz;            // empuje por golpes (bloques/tick, decae)
    double sepx, sepz;          // separacion de los vecinos (la calcula la partida cada tick)
    int lentoT, aturdidoT;      // ticks que le quedan ralentizado / aturdido (ataques de hielo, lodo, trueno, terremoto...)
    double lentoF = 1.0;        // multiplicador de su velocidad mientras esta ralentizado
    Pers pers;                  // su personalidad (null = un enemigo normal); ver Pers
    String colorEquipo;         // el equipo de color del marcador que le da el contorno (ver Colores)
    int embFase, embT = 40;     // personalidad «embiste»: 0 anda, 1 avisa (quieto), 2 carga; ticks que le quedan en cada fase
    double edx, edz;            // direccion de su embestida
    int rumboT;                 // estampida: si es > 0 sigue la direccion (rdx, rdz) y no persigue; al llegar a 0 desaparece sin soltar nada
    double rdx, rdz;
    boolean huye, dorado;        // el Michialien dorado de un evento: huye del jugador mas cercano, no hace dano y suelta un tesoro grande
    boolean tesoro;             // un tesoro pequeño: enemigo-sombra invisible que no se mueve ni hace daño y que se rompe con los ataques (ver Tesoros)
    boolean legendario;         // el legendario que sale de una grieta (ver Grietas)
    boolean shiny;              // un variocolor: mas vida y experiencia, suelta un objeto seguro y cuenta como shiny derrotado
    boolean sinBotin;           // desaparece sin soltar gemas ni contar como derrotado

    private Enemigo(PokemonEntity e, Especies.E especie, Tipo tipo, int xp, float hp, float dmg, double velBloquesPorSegundo, double radio) {
        this.e = e; this.especie = especie; this.tipo = tipo; this.xp = xp; this.hp = this.hpMax = hp; this.dmg = dmg; this.vel = velBloquesPorSegundo / 20.0; this.radio = radio;
    }

    boolean grande() { return tipo == Tipo.ELITE || tipo == Tipo.JEFE; }

    /** Lo ralentiza ({@code f} = fraccion de su velocidad, 0,5 = va a la mitad) durante {@code ticks}. Los campeones se ralentizan menos y el jefe casi nada. */
    void ralentizar(int ticks, double f) {
        if (tipo == Tipo.JEFE) f = Math.max(f, 0.8);
        else if (tipo == Tipo.ELITE) f = Math.max(f, 0.55);
        if (lentoT <= 0 || f <= lentoF) lentoF = f;
        lentoT = Math.max(lentoT, ticks);
    }

    /** Lo deja quieto {@code ticks} (los campeones, un tercio; el jefe es inmune). */
    void aturdir(int ticks) {
        if (tipo == Tipo.JEFE) return;
        aturdidoT = Math.max(aturdidoT, tipo == Tipo.ELITE ? ticks / 3 : ticks);
    }

    /**
     * Crea y coloca un enemigo; null si Cobblemon no pudo crearlo. {@code hpMul}, {@code dmgMul} y {@code velMul} son los multiplicadores del momento de la partida
     * (ver {@link Curvas}); {@code hpJefe} solo lo usa el jefe.
     */
    static Enemigo crear(ServerWorld w, Especies.E sp, String extra, Vec3d pos, Tipo tipo, double hpMul, double dmgMul, double velMul, float hpJefe, java.util.Random rnd) {
        return crear(w, sp, extra, pos, tipo, hpMul, dmgMul, velMul, hpJefe, rnd, 0f, 0f, 0.0);
    }

    /** Igual; para un jefe se pueden dar su altura, su dano de contacto y su velocidad (bloques/s); con 0 se usan los de siempre (4,6 de alto, 25 de dano, 2,2 b/s). */
    static Enemigo crear(ServerWorld w, Especies.E sp, String extra, Vec3d pos, Tipo tipo, double hpMul, double dmgMul, double velMul, float hpJefe, java.util.Random rnd,
                         float altoJefe, float dmgJefe, double velJefe) {
        try {
            int nivel = switch (tipo) { case DEBIL -> 8; case ELITE -> 35; case JEFE -> 60; default -> 20; };
            PokemonEntity e = PokemonProperties.Companion.parse(sp.id() + " level=" + nivel + (extra == null || extra.isEmpty() ? "" : " " + extra)).createEntity(w);
            float alto = switch (tipo) { case ELITE -> 2.4f + rnd.nextFloat() * 0.5f; case JEFE -> altoJefe > 0 ? altoJefe : 4.6f; default -> 1.0f + rnd.nextFloat() * 0.7f; };   // los normales se ven parecidos
            float escala = Math.max(0.3f, Math.min(8.0f, alto / sp.alto()));
            e.getPokemon().setScaleModifier(escala);
            e.setAiDisabled(true);
            e.setEnablePoseTypeRecalculation(false);            // la pose la pone este mod (andando o parado): con la IA apagada Cobblemon los dejaba siempre «de pie», sin animar el paso
            e.setInvulnerable(true);
            e.setSilent(true);
            e.setPersistent();
            e.setCustomNameVisible(false);
            e.addCommandTag("emi_survivors");
            double yaw = rnd.nextDouble() * 360.0;
            e.refreshPositionAndAngles(pos.x, pos.y, pos.z, (float) yaw, 0f);
            w.spawnEntity(e);
            float hp, dmg; double vel; int xp;
            switch (tipo) {
                case FUERTE -> { hp = 42f; dmg = 8f; vel = 2.8 + rnd.nextDouble() * 0.9; xp = Curvas.xpEnemigo(true, false); }
                case MICHI -> { hp = 36f; dmg = 6f; vel = 3.0 + rnd.nextDouble() * 0.8; xp = Curvas.xpEnemigo(true, true); }
                case ELITE -> { hp = 240f; dmg = 12f; vel = 2.6; xp = 10; }
                case JEFE -> { hp = hpJefe; dmg = dmgJefe > 0 ? dmgJefe : 25f; vel = velJefe > 0 ? velJefe : 2.2; xp = 60; }
                default -> { hp = 24f; dmg = 5f; vel = 2.3 + rnd.nextDouble() * 0.9; xp = Curvas.xpEnemigo(false, false); }
            }
            if (tipo != Tipo.JEFE) hp *= (float) hpMul;
            dmg *= (float) dmgMul;
            vel *= velMul;
            return new Enemigo(e, sp, tipo, xp, hp, dmg, vel, 0.35 + alto * (tipo == Tipo.JEFE ? 0.33 : 0.25));
        } catch (Throwable t) {
            EmiSurvivors.LOG.error("No se pudo crear el enemigo {}", sp.id(), t);
            return null;
        }
    }

    /**
     * Un tick de movimiento hacia (ox, oz): velocidad horizontal propia mas el empuje. Se mueve con {@code Entity.move} (que resuelve las colisiones con bloques y
     * suma la distancia caminada que Cobblemon usa para la animacion de andar): con la IA apagada, la fisica normal ({@code travel}) no mueve a un Pokemon de Cobblemon.
     * La componente vertical (-0,2) lo pega al suelo.
     */
    void mover(double ox, double oz) { mover(ox, oz, 1.0); }

    /** Igual, con la velocidad multiplicada por {@code factor} (la carga del jefe, o 0 para dejarlo quieto mirando al jugador). */
    void mover(double ox, double oz, double factor) {
        double dx = ox - e.getX(), dz = oz - e.getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        double vx = 0, vz = 0;
        if (aturdidoT > 0) { aturdidoT--; factor = 0; }
        else if (lentoT > 0) { lentoT--; factor *= lentoF; }
        if (d > radio + 0.2 && factor != 0) { vx = dx / d * vel * factor; vz = dz / d * vel * factor; }
        vx += kbx + sepx; vz += kbz + sepz;
        kbx *= 0.78; kbz *= 0.78;
        if (Math.abs(kbx) < 0.003) kbx = 0;
        if (Math.abs(kbz) < 0.003) kbz = 0;
        if (d > 0.05) {
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            e.setYaw(yaw); e.setHeadYaw(yaw); e.setBodyYaw(yaw);
        }
        e.setVelocity(Vec3d.ZERO);
        e.move(MovementType.SELF, new Vec3d(vx, -0.2, vz));
        ponerPose(vx * vx + vz * vz > 1e-6 ? PoseType.WALK : PoseType.STAND);
        e.setSprinting(factor != 0 && d > radio + 0.4);
    }

    private PoseType pose = PoseType.STAND;

    /** La pose del modelo (se manda al cliente solo cuando cambia): andando, el modelo hace su animacion de caminar. */
    private void ponerPose(PoseType nueva) {
        if (nueva == pose) return;
        pose = nueva;
        try { e.getDataTracker().set(PokemonEntity.getPOSE_TYPE(), nueva); } catch (Throwable t) { /* se queda como estaba */ }
    }

    /** Un tick de un enemigo con la personalidad «embiste»: anda hacia el jugador, de vez en cuando se queda quieto un momento (con particulas de enfado) y se lanza en linea recta. */
    void embestir(Run obj, ServerWorld w, java.util.Random rnd) {
        double dx = obj.pp.x - e.getX(), dz = obj.pp.z - e.getZ(), d = Math.sqrt(dx * dx + dz * dz);
        switch (embFase) {
            case 0 -> {
                mover(obj.pp.x, obj.pp.z);
                if (--embT <= 0) {
                    if (d > 3.0 && d < 14.0) { embFase = 1; embT = 14; }
                    else embT = 15;
                }
            }
            case 1 -> {
                mover(obj.pp.x, obj.pp.z, 0);
                if (embT % 3 == 0) w.spawnParticles(ParticleTypes.ANGRY_VILLAGER, e.getX(), e.getY() + radio * 2.5, e.getZ(), 1, 0.2, 0.2, 0.2, 0.0);
                if (--embT <= 0) { embFase = 2; embT = 12; double l = Math.max(d, 1e-6); edx = dx / l; edz = dz / l; }
            }
            default -> {
                mover(e.getX() + edx * 10, e.getZ() + edz * 10, 3.4);
                if (--embT <= 0) { embFase = 0; embT = 70 + rnd.nextInt(50); }
            }
        }
    }

    /** Un tick de un enemigo que HUYE de {@code de} (el jugador mas cercano): corre en sentido contrario, y si se acerca al muro tira hacia el centro de la arena. */
    void huir(double px, double pz, double cx, double cz, double radioArena) {
        double dx = e.getX() - px, dz = e.getZ() - pz, d = Math.max(1e-6, Math.sqrt(dx * dx + dz * dz));
        double tx = e.getX() + dx / d * 10, tz = e.getZ() + dz / d * 10;
        double ex = e.getX() - cx, ez = e.getZ() - cz, de = Math.sqrt(ex * ex + ez * ez);
        if (de > radioArena - 8) { tx = tx * 0.4 + cx * 0.6 + (-ez / Math.max(1e-6, de)) * 6; tz = tz * 0.4 + cz * 0.6 + (ex / Math.max(1e-6, de)) * 6; }     // cerca del muro: gira hacia dentro
        mover(tx, tz);
    }

    /** Un tick de un enemigo de la estampida: corre por su carril (el triple de deprisa) y, al acabar su tiempo, se va sin soltar nada. */
    void correr() {
        mover(e.getX() + rdx * 10, e.getZ() + rdz * 10, 3.0);
        if (--rumboT <= 0) { sinBotin = true; hp = 0; }
    }

    void quitar() {
        Colores.quitar(this);
        if (e != null && !e.isRemoved()) e.discard();
    }
}
