package emi.cafe;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Un cliente: un Pokemon real de Cobblemon sin IA (invulnerable y callado) que el mod mueve cada tick por una ruta fija: entra por la puerta, va a su mesa, pide un plato
 * (un globo con el dibujo del plato y una barra de paciencia encima), come cuando se lo sirven, paga y se va. Si se le acaba la paciencia se va enfadado y sin pagar.
 */
final class Cliente {
    enum Fase { ENTRANDO, ESPERANDO, COMIENDO, SALIENDO }

    static final String[] ESPECIES = {"rattata", "pidgey", "eevee", "pikachu", "slowpoke", "meowth", "psyduck", "jigglypuff", "snubbull", "growlithe"};
    /** Especies cuyo modelo se ve mucho mas grande que su caja de colision (medido en el juego): se encogen para no pasar por encima de la mesa. */
    static final java.util.Map<String, Float> AJUSTE = java.util.Map.of("rattata", 0.58f, "slowpoke", 0.65f);
    static String forzar;                           // solo pruebas: /cafe dev especie <id> hace que todos sean de esa especie
    static final double VELOCIDAD = 0.14;           // bloques por tick al andar
    static final double ALTURA = 1.15;              // todos se ven del mismo tamaño aproximado

    final PokemonEntity e;
    final Mesa mesa;
    final String especie;
    Fase fase = Fase.ENTRANDO;
    Item pedido;
    int paciencia, pacienciaMax;
    int comiendo;
    boolean enfadado;
    boolean sirvio;                          // se le sirvio el plato (deja plato sucio al irse)
    private final List<double[]> ruta;
    private int paso;
    private double alto = ALTURA;
    private Entity burbuja, icono, barra;
    private PoseType pose = PoseType.STAND;

    private Cliente(PokemonEntity e, Mesa mesa, String especie, List<double[]> ruta) { this.e = e; this.mesa = mesa; this.especie = especie; this.ruta = ruta; }

    /** Crea el cliente en la calle, delante de la puerta, y lo manda a su mesa; null si Cobblemon no pudo crearlo. */
    static Cliente crear(ServerWorld w, int ox, Mesa mesa, Random rnd) {
        String especie = forzar != null ? forzar : ESPECIES[rnd.nextInt(ESPECIES.length)];
        try {
            PokemonEntity e = PokemonProperties.Companion.parse(especie + " level=" + (5 + rnd.nextInt(20))).createEntity(w);
            e.setAiDisabled(true);
            e.setEnablePoseTypeRecalculation(false);
            e.setInvulnerable(true);
            e.setSilent(true);
            e.setNoGravity(true);
            e.noClip = true;                      // la ruta es fija: ni las mesas ni los otros clientes lo frenan (un Slowpoke no cabria entre las mesas)
            e.setPersistent();
            e.setCustomNameVisible(false);
            try { e.hideNameRendering(); } catch (Throwable t) { /* version de Cobblemon sin esta funcion */ }
            e.addCommandTag(Disp.TAG);
            double x0 = ox + Local.PUERTA_X, z0 = Local.CALLE_Z;
            e.refreshPositionAndAngles(x0, Local.Y + 1, z0, 180f, 0f);
            w.spawnEntity(e);
            List<double[]> ruta = new ArrayList<>();
            ruta.add(new double[]{x0, z0});
            ruta.add(new double[]{x0, Local.PASILLO_Z});
            ruta.add(new double[]{mesa.sx, Local.PASILLO_Z});
            ruta.add(new double[]{mesa.sx, mesa.sz});
            Cliente c = new Cliente(e, mesa, especie, ruta);
            float escala = (float) Math.max(0.35, Math.min(3.0, ALTURA / Math.max(0.3, e.getHeight()))) * AJUSTE.getOrDefault(especie, 1f);
            e.getPokemon().setScaleModifier(escala);
            c.alto = ALTURA;
            return c;
        } catch (Throwable t) {
            EmiCafe.LOG.error("No se pudo crear el cliente {}", especie, t);
            return null;
        }
    }

    boolean fuera() { return e.isRemoved(); }

    /** Un tick. Devuelve true cuando el cliente ya se ha ido y se puede borrar. */
    boolean tick(Cafe cafe, Random rnd) {
        ServerWorld w = (ServerWorld) e.getWorld();
        switch (fase) {
            case ENTRANDO -> {
                if (andar(ruta.get(paso)) && ++paso >= ruta.size()) {
                    fase = Fase.ESPERANDO;
                    mirarAlNorte();
                    pedido = cafe.elegirPedido(rnd);
                    pacienciaMax = paciencia = cafe.pacienciaTicks();
                    ponerPose(PoseType.STAND);
                    crearGlobo(w);
                    cafe.sonido(e.getX(), e.getY(), e.getZ(), net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.0f, 1.4f);
                }
            }
            case ESPERANDO -> {
                if (--paciencia <= 0) { enfadado = true; irse(w); cafe.alEnfadarse(this); }
                else if (paciencia % 10 == 0) actualizarGlobo(w);
            }
            case COMIENDO -> {
                if (--comiendo <= 0) irse(w);
                else if (comiendo % 12 == 0) w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, e.getX(), e.getY() + alto + 0.1, e.getZ(), 2, 0.25, 0.1, 0.25, 0.0);
            }
            case SALIENDO -> {
                if (andar(ruta.get(paso))) { if (--paso < 0) { quitar(); return true; } }
            }
        }
        return false;
    }

    /** Se le ha servido el plato correcto: empieza a comer. */
    void comer() {
        fase = Fase.COMIENDO;
        sirvio = true;
        comiendo = 70;
        quitarGlobo();
        ponerPose(PoseType.STAND);
    }

    /** Se acaba el turno: se va sin pagar y sin enfadarse. */
    void despedir() { if (fase == Fase.ESPERANDO) irse((ServerWorld) e.getWorld()); }

    private void irse(ServerWorld w) {
        quitarGlobo();
        fase = Fase.SALIENDO;
        paso = ruta.size() - 2;                  // vuelve por donde vino (sin repetir el punto de la mesa)
        if (enfadado) w.spawnParticles(ParticleTypes.ANGRY_VILLAGER, e.getX(), e.getY() + alto, e.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
    }

    private void mirarAlNorte() { e.setYaw(180f); e.setHeadYaw(180f); e.setBodyYaw(180f); }

    /** Un paso hacia (x, z) de la ruta; true si ya llego. */
    private boolean andar(double[] p) {
        double dx = p[0] - e.getX(), dz = p[1] - e.getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d < 0.12) { ponerPose(PoseType.STAND); return true; }
        double v = Math.min(VELOCIDAD, d);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        e.setYaw(yaw); e.setHeadYaw(yaw); e.setBodyYaw(yaw);
        e.setVelocity(Vec3d.ZERO);
        e.setPosition(e.getX() + dx / d * v, Local.Y + 1, e.getZ() + dz / d * v);
        ponerPose(PoseType.WALK);
        return false;
    }

    /** La pose del modelo (con la IA apagada Cobblemon las dejaria siempre «de pie»): andando, el modelo hace su animacion de caminar. */
    private void ponerPose(PoseType nueva) {
        if (nueva == pose) return;
        pose = nueva;
        try { e.getDataTracker().set(PokemonEntity.getPOSE_TYPE(), nueva); } catch (Throwable t) { /* se queda como estaba */ }
    }

    // ------------------------------------------------------------ el globo del pedido

    private double ybase() { return e.getY() + alto + 0.55; }

    private void crearGlobo(ServerWorld w) {
        burbuja = Disp.objeto(w, CafeItems.id(CafeItems.BURBUJA), e.getX(), ybase(), e.getZ(), 0.95f, true);
        icono = Disp.objeto(w, CafeItems.id(pedido), e.getX(), ybase() + 0.08, e.getZ(), 0.5f, true, 0.04f);
        barra = Disp.texto(w, barraJson(), e.getX(), ybase() + 0.62, e.getZ(), 1.0f, false);
    }

    private void actualizarGlobo(ServerWorld w) { Disp.cambiarTexto(barra, barraJson(), e.getX(), ybase() + 0.62, e.getZ(), 1.0f, false); }

    private String barraJson() {
        double f = pacienciaMax == 0 ? 0 : (double) paciencia / pacienciaMax;
        int n = (int) Math.ceil(f * 6);
        String color = f > 0.5 ? "green" : f > 0.25 ? "yellow" : "red";
        return "{\"text\":\"" + "▮".repeat(Math.max(0, n)) + "\",\"color\":\"" + color + "\"}";
    }

    private void quitarGlobo() { Disp.quitar(burbuja); Disp.quitar(icono); Disp.quitar(barra); burbuja = icono = barra = null; }

    void quitar() { quitarGlobo(); if (!e.isRemoved()) e.discard(); }

    /** La paciencia que le queda, de 0 a 1 (para la propina). */
    double pacienciaRestante() { return pacienciaMax == 0 ? 0 : (double) paciencia / pacienciaMax; }
}
