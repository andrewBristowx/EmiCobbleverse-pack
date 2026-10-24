package emi.cafe;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Un Michialien contratado: un GatitoAlien de Cobblemon sin IA con un rotulo con su nombre que anda por el local haciendo su tarea ({@link Cafe#tickAyudantes}):
 * el lavaplatos recoge los platos sucios de las mesas y los lava en el fregadero, el mesero lleva a la mesa los platos que quedan listos en las estaciones y el chef
 * va de estacion en estacion cocinando el plato de un cliente que espera. Cuando no tiene nada que hacer vuelve a su sitio.
 * Como los clientes, el mod lo mueve cada tick (sin IA): una cola de {@link Paso}s con los sitios a los que ir, lo que hacer al llegar y cuanto esperar alli.
 */
final class Ayudante {
    static final double VELOCIDAD = 0.13;           // bloques por tick al andar (los clientes, 0.14)
    private static final double SEPARACION = 0.8;   // a menos de esto de otro empleado, espera o esquiva

    /** Un paso de la tarea: ir a (x, z), esperar {@code espera} ticks mirando a (mx, mz) (si es finito), con {@code mientras} cada 10 ticks, y al final {@code alLlegar}. */
    record Paso(double x, double z, double y, int espera, Runnable mientras, Runnable alLlegar, double mx, double mz) {}

    final Personal.Rol rol;
    final Datos.Empleado emp;
    final PokemonEntity e;
    final Entity rotulo;
    final double hx, hz;                            // su sitio de descanso (coordenadas del mundo)
    double hy = Local.Y + 1;                        // y la altura de sus pies alli
    Rutas rutas;                                    // para no salirse de las casillas pisables al esquivar
    private int tick;
    int espera;                                     // ticks que faltan para buscar una tarea nueva (cuenta en su sitio)

    // la tarea de ahora
    final ArrayDeque<Paso> pasos = new ArrayDeque<>();
    BooleanSupplier vigente;                        // false = la tarea ya no tiene sentido (el cliente se fue...): se cancela
    Entity llevado;                                 // el plato que lleva en las manos (dibujo)
    Mesa mesaChef, mesaServ, mesaLava;              // la mesa a la que sirve el chef / el mesero / de la que recoge el lavaplatos
    Recetas.Plato platoChef;
    // la ventanilla del chef: donde deja el plato terminado para que lo lleve un mesero
    BlockPos pase;
    Item paseItem;
    Mesa paseMesa;
    Entity paseVisual;
    Ayudante paseReserva;                           // el mesero que va a recogerlo
    Cafe.Est estReserva;                            // el plato listo que el mesero va a recoger (que otro mesero no lo coja)
    private boolean llegado, moviendo;
    private final int prio;                         // al cruzarse dos que andan, cede el de numero mas alto (el primero contratado pasa)
    private int esperaPaso, bloqueo, libre;
    private PoseType pose = PoseType.STAND;
    private int vigilancia;

    private Ayudante(Personal.Rol rol, Datos.Empleado emp, PokemonEntity e, Entity rotulo, double hx, double hz) {
        this.rol = rol; this.emp = emp; this.e = e; this.rotulo = rotulo; this.hx = hx; this.hz = hz; this.prio = (int) (emp.nombre.hashCode() & 0xffff); this.espera = 40;
    }

    private static String json(String nombre, String rol, String extra) {
        return "{\"text\":\"" + nombre + "\",\"color\":\"gold\",\"bold\":true,\"extra\":[{\"text\":\" · " + rol + (extra.isEmpty() ? "" : " · " + extra) + "\",\"color\":\"gray\",\"bold\":false}]}";
    }

    private static String rolNivel(Personal.Rol rol, Datos.Empleado emp) { return rol.nombre + " Nv " + Personal.nivel(emp); }

    /** Ticks de espera en un sitio, acortados segun el nivel (con un minimo de 2). */
    int t(int ticks) { return Math.max(2, (int) Math.round(ticks / Personal.trabajo(Personal.nivel(emp)))); }

    /** Crea al empleado en su sitio de descanso (x, y, z del mundo; y = la altura de los pies). */
    static Ayudante crear(ServerWorld w, Datos.Empleado emp, double x, double y, double z) {
        Personal.Rol rol = Personal.Rol.de(emp.rol);
        if (rol == null) return null;
        String aspecto = Personal.ASPECTOS[Math.floorMod(emp.aspecto, Personal.ASPECTOS.length)][0];
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
            e.refreshPositionAndAngles(x, y, z, 0f, 0f);
            w.spawnEntity(e);
            e.getPokemon().setScaleModifier((float) Math.max(0.35, Math.min(3.0, 1.0 / Math.max(0.3, e.getHeight()))));
            Entity rotulo = Disp.texto(w, json(emp.nombre, rolNivel(rol, emp), ""), x, y + 1.4, z, 0.9f, false);
            Ayudante a = new Ayudante(rol, emp, e, rotulo, x, z);
            a.hy = y;
            return a;
        } catch (Throwable t) {
            EmiCafe.LOG.error("No se pudo crear al empleado {}", emp.nombre, t);
            return null;
        }
    }

    /** Cambia lo que dice el rotulo (el chef pone «cocina: pizza» mientras trabaja). */
    void rotular(String extra) {
        if (rotulo == null || rotulo.isRemoved()) return;
        Disp.cambiarTexto(rotulo, json(emp.nombre, rolNivel(rol, emp), extra), e.getX(), e.getY() + 1.4, e.getZ(), 0.9f, false);
    }

    void quitar() { Disp.quitar(rotulo); Disp.quitar(llevado); llevado = null; Disp.quitar(paseVisual); paseVisual = null; if (!e.isRemoved()) e.discard(); }

    /** Deja un plato terminado en su ventanilla (se ve flotando sobre la barra). */
    void ponerEnPase(ServerWorld w, Item plato, Mesa para) {
        Disp.quitar(paseVisual);
        paseItem = plato; paseMesa = para; paseReserva = null;
        if (pase != null) paseVisual = Disp.emplatado(w, CafeItems.id(plato), pase.getX() + 0.5, pase.getY() + 1.3, pase.getZ() + 0.5, 0.6f, true);
    }

    /** Quita el plato de la ventanilla (lo recogio un mesero o ya no hace falta). */
    void vaciarPase() { Disp.quitar(paseVisual); paseVisual = null; paseItem = null; paseMesa = null; }

    // ------------------------------------------------------------ la tarea

    boolean ocupado() { return !pasos.isEmpty(); }

    boolean enCasa() { return Math.hypot(e.getX() - hx, e.getZ() - hz) < 0.15; }

    /** Añade un paso al final de la tarea. */
    void paso(double x, double z, int espera, Runnable mientras, Runnable alLlegar, double mx, double mz) { pasos.add(new Paso(x, z, Double.NaN, espera, mientras, alLlegar, mx, mz)); }

    /** Añade un camino sin parar en cada punto; el ultimo mira hacia (mx, mz) cuando llega. */
    void ruta(List<double[]> r, double mx, double mz) {
        for (int i = 0; i < r.size(); i++) {
            boolean ult = i == r.size() - 1;
            pasos.add(new Paso(r.get(i)[0], r.get(i)[1], r.get(i).length > 2 ? r.get(i)[2] : Double.NaN, 0, null, null, ult ? mx : Double.NaN, ult ? mz : Double.NaN));
        }
    }

    /** Pone (o cambia) el plato que lleva en las manos; null = lo deja. */
    void llevar(ServerWorld w, String itemId) {
        Disp.quitar(llevado);
        llevado = itemId == null ? null : itemId.equals(CafeItems.id(CafeItems.PILA_PLATOS)) ? Disp.objeto(w, itemId, e.getX(), e.getY() + 0.55, e.getZ(), 0.5f, true) : Disp.emplatado(w, itemId, e.getX(), e.getY() + 0.55, e.getZ(), 0.55f, true);
        colocarLlevado();
    }

    /** Borra la tarea (y lo que lleve en las manos) sin moverlo. */
    void limpiar() {
        pasos.clear(); vigente = null; llegado = false; esperaPaso = 0; bloqueo = 0;
        Disp.quitar(llevado); llevado = null;
        mesaChef = mesaServ = mesaLava = null; platoChef = null;
        if (estReserva != null) { estReserva.reserva = null; estReserva = null; }
    }

    /** Lo deja en su sitio de golpe (al cerrar el cafe o al reconstruirse el local). */
    void alSitio() {
        limpiar();
        vaciarPase(); paseReserva = null;
        e.setPosition(hx, hy, hz);
        e.setYaw(0f); e.setHeadYaw(0f); e.setBodyYaw(0f);
        ponerPose(PoseType.STAND);
        colocarRotulo();
    }

    // ------------------------------------------------------------ el movimiento

    /** Un tick de movimiento. {@code todos} = los demas empleados (para no chocar). */
    void mover(List<Ayudante> todos, int ahora) {
        tick = ahora;
        moviendo = false;
        if (vigente != null && ++vigilancia % 5 == 0 && !vigente.getAsBoolean()) { cancelarAviso = true; return; }
        Paso p = pasos.peek();
        if (p == null) { ponerPose(PoseType.STAND); return; }
        if (!llegado) {
            double dx = p.x() - e.getX(), dz = p.z() - e.getZ();
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < 0.1) { llegado = true; esperaPaso = p.espera(); bloqueo = 0; }
            else { andar(dx, dz, d, Double.isFinite(p.y()) ? p.y() : rutas != null ? rutas.alturaCerca(p.x(), p.z(), e.getY(), tick) : e.getY(), todos); return; }
        }
        ponerPose(PoseType.STAND);
        if (Double.isFinite(p.mx())) mirar(p.mx(), p.mz());
        if (esperaPaso > 0) {
            esperaPaso--;
            if (p.mientras() != null && esperaPaso % 10 == 0) p.mientras().run();
            return;
        }
        pasos.poll();
        llegado = false;
        if (p.alLlegar() != null) p.alLlegar().run();
    }

    /** true = la tarea ya no vale (la lee {@link Cafe} para cancelarla y mandarlo a casa). */
    boolean cancelarAviso;

    private void andar(double dx, double dz, double d, double ty, List<Ayudante> todos) {
        double v = Math.min(VELOCIDAD * Personal.velocidad(Personal.nivel(emp)), d);
        double ux = dx / d, uz = dz / d;
        if (libre > 0) libre--;
        else if (choca(e.getX() + ux * v, e.getZ() + uz * v, todos)) {
            // esquiva girando hacia un lado; si no puede, espera (y si lleva mucho esperando, pasa de largo)
            boolean paso = false;
            for (double ang : new double[]{0.9, -0.9}) {
                double c = Math.cos(ang), s = Math.sin(ang);
                double vx = ux * c - uz * s, vz = ux * s + uz * c;
                double qx = e.getX() + vx * v, qz = e.getZ() + vz * v;
                if (!choca(qx, qz, todos) && (rutas == null || rutas.libre(qx, qz, e.getY(), tick))) { ux = vx; uz = vz; paso = true; break; }
            }
            if (!paso) {
                ponerPose(PoseType.STAND);
                if (++bloqueo > 40) { libre = 25; bloqueo = 0; }
                return;
            }
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-ux, uz));
        e.setYaw(yaw); e.setHeadYaw(yaw); e.setBodyYaw(yaw);
        e.setVelocity(Vec3d.ZERO);
        e.setPosition(e.getX() + ux * v, e.getY() + (ty - e.getY()) * Math.min(1.0, v / d), e.getZ() + uz * v);       // sube y baja escaleras poco a poco
        moviendo = true;
        ponerPose(PoseType.WALK);
        colocarRotulo();
        colocarLlevado();
    }

    /** ¿Chocaria con otro empleado que tiene delante en (nx, nz)? (solo cuenta si se acercan: asi puede alejarse de uno que ya esta pegado) */
    private boolean choca(double nx, double nz, List<Ayudante> todos) {
        for (Ayudante b : todos) {
            if (b == this || b.e.isRemoved()) continue;
            if (b.moviendo && b.prio > prio) continue;                        // el otro anda y cede el paso: sigo recto
            double ahora = Math.hypot(e.getX() - b.e.getX(), e.getZ() - b.e.getZ());
            double luego = Math.hypot(nx - b.e.getX(), nz - b.e.getZ());
            if (luego < SEPARACION && luego < ahora - 1e-6) return true;
        }
        return false;
    }

    private void mirar(double tx, double tz) {
        double dx = tx - e.getX(), dz = tz - e.getZ();
        if (Math.abs(dx) < 1e-3 && Math.abs(dz) < 1e-3) return;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        e.setYaw(yaw); e.setHeadYaw(yaw); e.setBodyYaw(yaw);
        colocarLlevado();
    }

    private void colocarRotulo() {
        if (rotulo != null && !rotulo.isRemoved()) rotulo.setPosition(e.getX(), e.getY() + 1.4, e.getZ());
    }

    /** El plato va delante del Michialien, a la altura del pecho. */
    private void colocarLlevado() {
        if (llevado == null || llevado.isRemoved()) return;
        double rad = Math.toRadians(e.getYaw());
        Disp.mover(llevado, e.getX() - Math.sin(rad) * 0.5, e.getY() + 0.55, e.getZ() + Math.cos(rad) * 0.5);
    }

    private void ponerPose(PoseType nueva) {
        if (nueva == pose) return;
        pose = nueva;
        try { e.getDataTracker().set(PokemonEntity.getPOSE_TYPE(), nueva); } catch (Throwable t) { /* se queda como estaba */ }
    }
}
