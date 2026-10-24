package emi.cafe;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import emi.cafe.net.TextoPayload;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * El cafe de un jugador (su «dueño»): el local, los clientes, las estaciones, la jornada (se abre y se cierra desde la caja registradora, sin reloj) y quienes estan dentro (el dueño y los amigos que invite).
 * Lo que cada uno lleva en las manos es el objeto de verdad en la mano (hueco 1 de la barra, que no se puede cambiar), no en el inventario.
 * El dinero ganado va a quien sirve; las mejoras y los platos que se conocen son los del dueño.
 */
final class Cafe {
    enum Estado { PREPARANDO, ABIERTO, CERRANDO }

    /** Lo que hay en una estacion: lo que se esta haciendo, el avance y el plato terminado que espera a que lo recojan. */
    static final class Est {
        Item resultado;                       // lo que saldra (null = no esta trabajando)
        int ticks, total;
        Item listo;                           // terminado y esperando
        Ayudante reserva;                     // el mesero que va a recogerlo
        int listoTicks;                       // desde cuando espera (la freidora quema)
        final List<Item> dentro = new ArrayList<>();      // en la mesa de emplatar: lo que se ha puesto
        int cola;                             // en el fregadero: platos sucios esperando
        final List<UUID> quien = new ArrayList<>();       // y quien puso cada uno (para las misiones)
        Entity icono, barra;
    }

    static final int QUEMA_AVISO = 20 * 6, QUEMA = 20 * 10;

    final UUID duenio;
    final MinecraftServer server;
    final ServerWorld w;
    final int ox, slot;
    final Random rnd = new Random();
    final Set<UUID> jugadores = new LinkedHashSet<>();
    final Map<UUID, Item> manos = new HashMap<>();
    final List<Mesa> mesas = new ArrayList<>();
    final List<Cliente> clientes = new ArrayList<>();
    final Map<BlockPos, Est> ests = new HashMap<>();
    Estado estado = Estado.PREPARANDO;
    final Set<Estacion> estEnMano = EnumSet.noneOf(Estacion.class);   // estaciones que el dueño ha recogido en el modo construccion y aun no ha vuelto a poner
    boolean construyendo;                                 // modo construccion: el dueño decora a solas (el cafe esta cerrado)
    Tutorial tut;                                         // la guia de inicio (solo mientras no se ha terminado)
    final List<Ayudante> ayudantes = new ArrayList<>();
    final Rutas rutas;                                    // caminos por la parcela esquivando lo construido
    final Obras obras = new Obras(this);                  // construir (parcela libre, almacen, modo construccion)
    final Set<UUID> visitantes = new LinkedHashSet<>();   // gente que solo esta de visita (tambien estan en `jugadores`, pero no cocinan ni sirven)
    Evento evento = Evento.NINGUNO;                       // el suceso de la jornada de hoy
    static Item pedidoForzado;                            // solo pruebas: /cafe dev pedido <plato> hace que todos pidan eso
    static Evento eventoForzado;                          // solo pruebas: /cafe dev evento <nombre> lo pone para la proxima vez que se abra
    Recetas.Plato platoDelDia;
    boolean criticoPendiente;
    int criticoEn;
    int t;
    int cierre;
    int proximoCliente;
    int platos;                                           // platos limpios en la pila
    // lo de este turno (para el resumen)
    int servidos, enfadados, propinas, ganado;
    final Map<UUID, Integer> ganadoPor = new HashMap<>();
    final Map<UUID, Integer> servidosPor = new HashMap<>();
    final Map<UUID, Integer> estrellasTurno = new HashMap<>();

    Cafe(MinecraftServer server, ServerWorld w, UUID duenio, int slot) {
        this.server = server; this.w = w; this.duenio = duenio; this.slot = slot; this.ox = slot * Local.SEPARACION;
        this.rutas = new Rutas(w, this.ox);
        jugadores.add(duenio);
    }

    Datos.Jugador dj() { ServerPlayerEntity p = jugador(duenio); return EmiCafe.datos.jugador(duenio, p == null ? null : p.getGameProfile().getName()); }
    Datos.Jugador dj(ServerPlayerEntity p) { return EmiCafe.datos.jugador(p.getUuid(), p.getGameProfile().getName()); }
    /** Quita a alguien del cafe (dueño aparte): ya no trabaja ni visita. */
    void quitarJugador(UUID id) { jugadores.remove(id); visitantes.remove(id); manos.remove(id); muestras.remove(id); }
    ServerPlayerEntity jugador(UUID id) { return server.getPlayerManager().getPlayer(id); }
    List<ServerPlayerEntity> presentes() { List<ServerPlayerEntity> l = new ArrayList<>(); for (UUID id : jugadores) { ServerPlayerEntity p = jugador(id); if (p != null) l.add(p); } return l; }
    boolean esDuenio(ServerPlayerEntity p) { return p.getUuid().equals(duenio); }

    // ------------------------------------------------------------ lo que dicen las mejoras

    int nivel(Recetas.Mejora m) { return dj().nivel(m.id); }
    int numMesas() { return Math.min(Local.MESAS.length, 2 + nivel(Recetas.Mejora.MESAS) + 2 * nivel(Recetas.Mejora.AMPLIAR)); }
    double velocidad() { return 1.0 - 0.2 * nivel(Recetas.Mejora.VELOCIDAD); }
    int pacienciaTicks() { return (int) (20 * 90 * (1.0 + 0.25 * nivel(Recetas.Mejora.PACIENCIA)) * (evento == Evento.HORA_PUNTA ? 0.85 : 1.0)); }
    int platosMax() { return 4 + 2 * nivel(Recetas.Mejora.VAJILLA); }
    /** Ticks entre un cliente y el siguiente (menos con mejor publicidad y con mas gente cocinando). */
    int intervalo() {
        double base = 20 * (16 - 3 * nivel(Recetas.Mejora.PUBLICIDAD)) / (1.0 + 0.5 * (trabajadores() - 1));
        base *= Estilo.multLlegada(nivelEstilo()) * (evento == Evento.HORA_PUNTA ? 0.55 : 1.0);
        return (int) base + rnd.nextInt(60);
    }

    // ------------------------------------------------------------ nivel del local (1 a 5)

    /** El nivel del local: 1 + los niveles de «Ampliar el local» (tamaño, mesas, estaciones, platos y mejoras que se pueden tener dependen de él). */
    int nivelLocal() { return 1 + nivel(Recetas.Mejora.AMPLIAR); }

    /** Cuantas estaciones de trabajo cuentan para el limite (sin la basura ni el fregadero, que vienen con el cafe). */
    static int cuentaEstaciones(Set<Estacion> s) { int n = 0; for (Estacion e : s) if (e.x >= 0 && !e.siempre) n++; return n; }

    int estacionesUsadas() { return cuentaEstaciones(activas()); }

    /** Por que este plato no se puede comprar ahora (null = se puede): el nivel del local o el limite de estaciones. */
    String bloqueoPlato(Recetas.Plato pl) {
        int nl = nivelLocal();
        if (pl.nivelLocal() > nl) return "Hace falta el local de nivel " + pl.nivelLocal() + " (ahora tienes el " + nl + ")";
        Set<Estacion> despues = activas();
        despues.addAll(pl.estaciones);
        int max = Local.maxEstaciones(nl), n = cuentaEstaciones(despues);
        if (n > max) return "Tu local de nivel " + nl + " solo admite " + max + " estaciones (con este plato tendrías " + n + "): amplíalo";
        return null;
    }

    /** El estado de una estacion (null si aun no se ha usado). */
    Est estDe(Estacion e) { BlockPos p = posDe(e); return p == null ? null : ests.get(p); }

    /** Donde esta ahora una estacion (null si no esta en el local). */
    BlockPos posDe(Estacion e) {
        int[] q = posEstaciones().get(e);
        return q == null ? null : Local.pos(ox, q[0], q.length > 2 ? q[2] : 1, q[1]);
    }

    /** Los que cocinan y sirven (sin contar a los que solo visitan). */
    int trabajadores() { return jugadores.size() - visitantes.size(); }

    // ------------------------------------------------------------ estilo, reputacion y gustos

    int puntosEstilo() { Datos.Jugador d = dj(); return Estilo.puntos(d.decor, d.estilo, d.pintura); }
    int nivelEstilo() { return Estilo.nivelDe(puntosEstilo()); }
    int nivelRep() { return Reputacion.nivel(dj().rep); }

    /** Suma (o resta) reputacion al cafe; al subir de nivel avisa a todos y da estrellas al dueño. */
    void ganarRep(int n) {
        Datos.Jugador d = dj();
        int antes = Reputacion.nivel(d.rep);
        d.rep = Math.max(0, d.rep + n);
        int ahora = Reputacion.nivel(d.rep);
        if (ahora <= antes) return;
        int estrellas = 5 * ahora;
        Progreso.dar(this, d, duenio, estrellas);
        String extra = ahora == 2 ? " Ya puedes contratar al lavaplatos." : ahora == Reputacion.NIVEL_CRITICO ? " Ahora el crítico gastronómico puede visitarte." : ahora == 4 ? " Ya puedes contratar al camarero." : "";
        aTodos(p -> {
            Msg.title(p, Text.literal("¡Tu café sube!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal(Reputacion.nombre(ahora) + " (nivel " + ahora + ")").formatted(Formatting.YELLOW), 5, 60, 10);
            p.sendMessage(Msg.say("¡Reputación del café: " + Reputacion.nombre(ahora) + " (nivel " + ahora + ")! Todo se paga un poco más y el dueño gana " + estrellas + " estrellas." + extra), false);
            Sonidos.jugador(p, "subir", 1f, 1f);
        });
        EmiCafe.datos.guardar(duenio);
    }

    /** Los platos que se pueden cocinar ahora: los que el dueño sabe y, en una parcela libre, cuyas estaciones estan todas puestas. */
    List<String> idsServibles() {
        List<String> ids = dj().platos;
        if (!dj().libre) return ids;
        List<String> r = new ArrayList<>();
        for (String id : ids) { Recetas.Plato pl = Recetas.Plato.de(id); if (pl != null && tieneEstaciones(pl.estaciones)) r.add(id); }
        return r;
    }

    /** El plato favorito de esa especie (entre los que el dueño sabe hacer); null si no sabe ninguno. */
    Item favoritoDe(String especie) {
        String id = Gustos.favorito(especie, idsServibles());
        Recetas.Plato pl = id == null ? null : Recetas.Plato.de(id);
        return pl == null ? null : pl.item;
    }

    /** El plato mas caro que sabe hacer el dueño (lo que pide el critico). */
    Item pedidoCritico() {
        Recetas.Plato mejor = null;
        for (String id : idsServibles()) { Recetas.Plato pl = Recetas.Plato.de(id); if (pl != null && (mejor == null || pl.precio > mejor.precio)) mejor = pl; }
        return mejor == null ? CafeItems.PAPAS_FRITAS : mejor.item;
    }

    Set<Estacion> activas() {
        Set<Estacion> s = EnumSet.noneOf(Estacion.class);
        for (String id : dj().platos) { Recetas.Plato pl = Recetas.Plato.de(id); if (pl != null) s.addAll(pl.estaciones); }
        return s;
    }

    /** Construye (o reconstruye) el local segun lo que ha desbloqueado el dueño; borra lo que hubiera dentro. */
    void reconstruir() {
        for (Cliente c : clientes) c.quitar();
        clientes.clear();
        mesas.clear();
        for (Est e : ests.values()) { Disp.quitar(e.icono); Disp.quitar(e.barra); }
        ests.clear();
        quitarAyudantes();
        Datos.Jugador d = dj();
        obras.cambio();
        obras.kit();
        obras.sincronizar();
        boolean libre = d.libre;
        List<int[]> pos = mesasPos();
        Local.construir(w, ox, libre ? new java.util.EnumMap<>(Estacion.class) : posEstaciones(), libre ? new ArrayList<>() : pos, nivel(Recetas.Mejora.AMPLIAR), d.estilo, d.pintura, d.nombreCafe, d.nombre, Ciudad.hay(slot + 1), libre);
        for (int[] m : pos) mesas.add(new Mesa(Local.pos(ox, m[0], m.length > 2 ? m[2] : 1, m[1])));
        aplicarDecor();
        if (libre) { obras.actualizarRotulo(); obras.letrero(estado == Estado.ABIERTO); }
        crearPersonal();
    }

    /** Empieza la guia de inicio si el dueño aun no la ha hecho (y ya tiene cocinero). */
    void iniciarTutorial(boolean forzar) {
        Datos.Jugador d = dj();
        if (tut != null || d.inicial == null) return;
        if (!forzar) {
            if (d.clientes > 0 || d.turnos > 0) d.tutorialHecho = true;                 // los que ya juegan no la necesitan
            if (d.tutorialHecho) return;
        } else d.tutorialHecho = false;
        Recetas.Inicial ini = Recetas.Inicial.de(d.inicial);
        if (ini == null) return;
        tut = new Tutorial(this, ini);
        ServerPlayerEntity p = jugador(duenio);
        if (p != null && d.libre && !construyendo && estado == Estado.PREPARANDO && !obras.colocado(CafeBlocks.REGISTRADORA)) obras.entrar(p);      // parcela vacia: se empieza construyendo
        if (p != null) {
            p.sendMessage(Msg.say("GUÍA DE INICIO: arriba hay una barra que te dice qué hacer en cada momento, y verás unas chispas sobre el sitio al que ir. Se mueve con WASD y todo se hace con CLIC DERECHO. Para quitarla: /cafe tutorial saltar."), false);
            Msg.title(p, Text.literal("Guía de inicio").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), Text.literal("Sigue la barra de arriba").formatted(Formatting.YELLOW), 5, 60, 10);
        }
    }

    void quitarTutorial(boolean marcarHecha) {
        if (tut != null) { tut.quitar(); tut = null; }
        if (marcarHecha) { dj().tutorialHecho = true; EmiCafe.datos.guardar(duenio); }
    }

    void quitarAyudantes() { for (Ayudante a : ayudantes) a.quitar(); ayudantes.clear(); }

    // ------------------------------------------------------------ el personal (Michialien contratados)

    /** Pasa las mejoras de ayudante de las versiones viejas (un Squirtle lavaplatos, un Chansey camarero) a empleados Michialien. */
    void migrarAyudantes() {
        Datos.Jugador d = dj();
        if (d.personal == null) d.personal = new ArrayList<>();
        boolean cambio = false;
        for (String[] m : new String[][]{{"ayud_lava", "lavaplatos"}, {"ayud_mesero", "mesero"}}) {
            if (d.nivel(m[0]) > 0) {
                Datos.Empleado e = new Datos.Empleado();
                e.rol = m[1]; e.nombre = Personal.nombreNuevo(d.personal); e.aspecto = Personal.Rol.de(m[1]).aspectoDefecto;
                d.personal.add(e);
                d.mejoras.remove(m[0]);
                cambio = true;
            }
        }
        if (cambio) EmiCafe.datos.guardar(duenio);
    }

    private void crearPersonal() {
        Datos.Jugador d = dj();
        if (d.personal == null) d.personal = new ArrayList<>();
        rutas.sucia();
        int ancho = Local.ancho(nivel(Recetas.Mejora.AMPLIAR));
        List<int[]> usadas = new ArrayList<>();
        for (int i = 0; i < d.personal.size() && i < 8; i++) {                     // cada uno descansa en una casilla libre de la cocina, separados
            int[] pu = d.personal.get(i).puesto;                                   // el sitio que le puso el dueño con su peluche, o uno libre de la cocina
            int[] c = pu != null ? rutas.casillaCerca(ox + pu[0] + 0.5, pu[2] + 0.5, Local.Y + pu[1], usadas, 0.0, t)
                    : rutas.casillaCerca(ox + Math.min(ancho - 2, 3 + 2.5 * i), 3.5, Local.Y + 1, usadas, 2.0, t);
            if (c == null) continue;
            usadas.add(c);
            Ayudante a = Ayudante.crear(w, d.personal.get(i), c[0] + 0.5, rutas.alturaCerca(c[0] + 0.5, c[1] + 0.5, pu != null ? Local.Y + pu[1] : Local.Y + 1, t), c[1] + 0.5);
            if (a != null) { a.rutas = rutas; ayudantes.add(a); }
        }
        asignarVentanillas();
    }

    /** Cada chef tiene su ventanilla: un trozo de la barra (la encimera de la fila de la barra, o la mas cercana) donde deja el plato para que lo recoja un mesero. */
    private void asignarVentanillas() {
        List<BlockPos> libres = new ArrayList<>();
        boolean libre = dj().libre;                                                // en una parcela libre las encimeras pueden estar en cualquier sitio y a cualquier altura
        int xa = libre ? ox + Ciudad.X0 + 1 : ox + 1, xb = libre ? ox + Ciudad.X1 : ox + Local.ancho(nivel(Recetas.Mejora.AMPLIAR));
        int za = libre ? Local.Z_ATRAS + 2 : 1, zb = libre ? 15 : 12, ya = 1, yb = libre ? 8 : 1;
        for (int x = xa; x < xb; x++) for (int z = za; z <= zb; z++) for (int y = ya; y <= yb; y++) {
            BlockPos b = new BlockPos(x, Local.Y + y, z);
            if (w.getBlockState(b).getBlock() == CafeBlocks.ENCIMERA && !rutas.adyacentes(b, t).isEmpty()) libres.add(b);
        }
        final double cx = libre ? ox + 8.0 : ox + 8.0, cz = libre ? 8.0 : 6.5;
        libres.sort((p, q) -> Double.compare(Math.hypot(p.getX() + 0.5 - cx, p.getZ() + 0.5 - cz), Math.hypot(q.getX() + 0.5 - cx, q.getZ() + 0.5 - cz)));
        List<BlockPos> dadas = new ArrayList<>();
        for (Ayudante a : ayudantes) {
            if (a.rol != Personal.Rol.CHEF) continue;
            for (BlockPos b : libres) {
                boolean cerca = false;
                for (BlockPos d : dadas) if (Math.abs(d.getX() - b.getX()) + Math.abs(d.getZ() - b.getZ()) < 2) cerca = true;      // sin dos ventanillas pegadas
                if (!cerca) { a.pase = b; dadas.add(b); break; }
            }
        }
    }

    /** Vuelve a poner a los empleados (despues de contratar, despedir, renombrar o cambiar un aspecto). */
    void recargarPersonal() { quitarAyudantes(); crearPersonal(); }

    /** El dueño contrata a un Michialien. Devuelve el error, o null. */
    String contratar(ServerPlayerEntity p, Personal.Rol rol) {
        if (!esDuenio(p)) return "Solo el dueño contrata.";
        if (estado != Estado.PREPARANDO || construyendo) return "Contrata con el café cerrado y sin construir.";
        Datos.Jugador d = dj();
        if (d.personal == null) d.personal = new ArrayList<>();
        int tienes = Personal.cuantos(d.personal, rol);
        if (tienes >= rol.max) return "Ya tienes el máximo de " + rol.nombre.toLowerCase() + "s (" + rol.max + ").";
        if (nivelLocal() < Personal.localNecesario(rol, tienes)) return "Para contratar al " + (tienes + 1) + "º " + rol.nombre.toLowerCase() + " hace falta un local de nivel " + Personal.localNecesario(rol, tienes) + ".";
        if (nivelRep() < rol.reqRep) return "Para contratar a un " + rol.nombre.toLowerCase() + " hace falta reputación nivel " + rol.reqRep + ".";
        if (d.monedas < rol.precio) return "Te faltan monedas (" + rol.precio + ").";
        d.monedas -= rol.precio;
        Datos.Empleado e = new Datos.Empleado();
        e.rol = rol.id; e.nombre = Personal.nombreNuevo(d.personal); e.aspecto = rol.aspectoDefecto;
        d.personal.add(e);
        EmiCafe.datos.guardar(duenio);
        recargarPersonal();
        p.sendMessage(Msg.say("¡Contrataste a " + e.nombre + " como " + rol.nombre.toLowerCase() + "! Para cambiarle el nombre: pestaña PERSONAL → NOMBRE (o /cafe personal nombre " + d.personal.size() + " <nombre>)."), false);
        Sonidos.jugador(p, "subir", 1f, 1.1f);
        return null;
    }

    String despedir(ServerPlayerEntity p, int idx) {
        if (!esDuenio(p)) return "Solo el dueño despide.";
        if (estado != Estado.PREPARANDO || construyendo) return "Despide con el café cerrado y sin construir.";
        Datos.Jugador d = dj();
        if (idx < 0 || idx >= d.personal.size()) return "Ese empleado no existe.";
        Datos.Empleado e = d.personal.remove(idx);
        EmiCafe.datos.guardar(duenio);
        recargarPersonal();
        p.sendMessage(Msg.say("Despediste a " + e.nombre + "."), false);
        return null;
    }

    /** Cambia el nombre de un empleado (1 = el primero). */
    String renombrar(ServerPlayerEntity p, int num, String nombre) {
        if (!esDuenio(p)) return "Solo el dueño puede poner nombres.";
        Datos.Jugador d = dj();
        if (num < 1 || num > d.personal.size()) return "No tienes un empleado número " + num + ".";
        String n = Personal.limpiar(nombre, 16);
        if (n.isEmpty()) return "Escribe un nombre.";
        d.personal.get(num - 1).nombre = n;
        EmiCafe.datos.guardar(duenio);
        recargarPersonal();
        p.sendMessage(Msg.say("Ahora se llama " + n + "."), false);
        return null;
    }

    /** Sube de nivel a un empleado (necesita que el local tenga ese nivel, y cuesta monedas). Devuelve el error, o null. */
    String mejorarEmpleado(ServerPlayerEntity p, int idx) {
        if (!esDuenio(p)) return "Solo el dueño mejora a su personal.";
        if (estado != Estado.PREPARANDO || construyendo) return "Mejora al personal con el café cerrado y sin construir.";
        Datos.Jugador d = dj();
        if (idx < 0 || idx >= d.personal.size()) return "Ese empleado no existe.";
        Datos.Empleado e = d.personal.get(idx);
        Personal.Rol rol = Personal.Rol.de(e.rol);
        if (rol == null) return "Ese empleado no existe.";
        int n = Personal.nivel(e);
        if (n >= Personal.NIVEL_MAX) return e.nombre + " ya está al nivel máximo.";
        if (n + 1 > nivelLocal()) return "Para subir a " + e.nombre + " al nivel " + (n + 1) + " hace falta un local de nivel " + (n + 1) + ".";
        int coste = Personal.costeSubida(rol, n);
        if (d.monedas < coste) return "Te faltan monedas (" + coste + ").";
        d.monedas -= coste;
        e.nivel = n + 1;
        EmiCafe.datos.guardar(duenio);
        recargarPersonal();
        p.sendMessage(Msg.say(e.nombre + " sube al nivel " + (n + 1) + ": anda un " + Math.round((Personal.velocidad(n + 1) - 1) * 100) + " % más rápido y trabaja un " + Math.round((Personal.trabajo(n + 1) - 1) * 100) + " % más rápido."), false);
        Sonidos.jugador(p, "subir", 1f, 1.1f);
        return null;
    }

    String cambiarAspecto(ServerPlayerEntity p, int idx) {
        if (!esDuenio(p)) return "Solo el dueño puede cambiar aspectos.";
        Datos.Jugador d = dj();
        if (idx < 0 || idx >= d.personal.size()) return "Ese empleado no existe.";
        Datos.Empleado e = d.personal.get(idx);
        e.aspecto = (e.aspecto + 1) % Personal.ASPECTOS.length;
        EmiCafe.datos.guardar(duenio);
        recargarPersonal();
        return null;
    }

    /** Le pone nombre al cafe (el rotulo del tejado). Devuelve el error, o null. */
    String ponerNombreCafe(ServerPlayerEntity p, String nombre) {
        if (!esDuenio(p)) return "Solo el dueño puede ponerle nombre al café.";
        String n = Personal.limpiar(nombre, 24);
        Datos.Jugador d = dj();
        d.nombreCafe = n;
        EmiCafe.datos.guardar(duenio);
        if (d.libre) obras.actualizarRotulo(); else Local.rotulo(w, ox, n, d.nombre);
        p.sendMessage(Msg.say(n.isEmpty() ? "Tu café vuelve a llamarse «Café Michialien»." : "Tu café ahora se llama «" + n + "»."), false);
        return null;
    }

    /** ¿Estan en el cafe todas las estaciones que usa este plato? */
    private boolean tieneEstaciones(java.util.Collection<Estacion> l) {
        for (Estacion e : l) if (posDe(e) == null) return false;
        return true;
    }

    // ------------------------------------------------------------ las tareas de los empleados (andan por el local)

    private static final boolean DEBUG = Boolean.getBoolean("emi.cafe.debug");

    /** Solo pruebas ({@code -Demi.cafe.debug=true}): escribe en el log lo que hace cada empleado. */
    private void dbg(Ayudante a, String que) { if (DEBUG) EmiCafe.LOG.info("[personal] {} ({}): {}", a.emp.nombre, a.rol.id, que); }

    /**
     * Añade a la tarea el camino hasta quedar pegado a ese bloque (una mesa, una estacion, la ventanilla...), mirandolo, esquivando lo construido. {@code excluir} = casillas que
     * se prefiere no usar (donde se sienta el cliente). Devuelve el punto final {x, z, y}, o null si no hay camino (entonces no añade nada).
     */
    private double[] irJunto(Ayudante a, BlockPos bloque, double[] desde, List<int[]> excluir) {
        List<int[]> metas = rutas.adyacentes(bloque, t);
        List<int[]> ocupadas = new ArrayList<>();                                  // las casillas donde ya esta parado otro empleado (para no ponerse encima)
        if (excluir != null) ocupadas.addAll(excluir);
        for (Ayudante b : ayudantes) if (b != a && !b.e.isRemoved()) ocupadas.add(new int[]{(int) Math.floor(b.e.getX()), (int) Math.floor(b.e.getZ())});
        excluir = ocupadas;
        if (!excluir.isEmpty()) {
            List<int[]> sin = new ArrayList<>();
            for (int[] m : metas) { boolean fuera = false; for (int[] x : excluir) if (x[0] == m[0] && x[1] == m[1]) fuera = true; if (!fuera) sin.add(m); }
            if (!sin.isEmpty()) metas = sin;
        }
        List<double[]> cam = rutas.camino(desde[0], desde[1], desde[2], metas, t);
        if (cam == null || cam.isEmpty()) return null;
        a.ruta(cam, bloque.getX() + 0.5, bloque.getZ() + 0.5);
        return cam.get(cam.size() - 1);
    }

    private static double[] pos(Ayudante a) { return new double[]{a.e.getX(), a.e.getZ(), a.e.getY()}; }

    /** La casilla donde se sienta el cliente de esa mesa (para no ponerse encima). */
    private static List<int[]> asiento(Mesa m) { return List.of(new int[]{(int) Math.floor(m.sx), (int) Math.floor(m.sz)}); }

    /** Manda a un empleado a camino de su sitio (borrando lo que tuviera que hacer). */
    private void mandarACasa(Ayudante a) {
        a.limpiar();
        a.rotular("");
        List<double[]> cam = rutas.camino(a.e.getX(), a.e.getZ(), a.e.getY(), List.of(new int[]{(int) Math.floor(a.hx), (int) Math.floor(a.hz)}), t);
        if (cam == null) cam = List.of(new double[]{a.hx, a.hz, a.hy});
        a.ruta(cam, a.hx, a.hz + 10);
    }

    /** Los clientes que esperan su plato, el que llego antes primero. */
    private List<Mesa> esperando() {
        List<Mesa> l = new ArrayList<>();
        for (Mesa m : mesas) if (m.cliente != null && m.cliente.fase == Cliente.Fase.ESPERANDO && m.cliente.pedido != null) l.add(m);
        l.sort((x, y) -> Long.compare(x.cliente.llegada, y.cliente.llegada));
        return l;
    }

    /** ¿Ya va otro empleado a servir esa mesa? ({@code contarChefs}: el chef cuenta, el mesero no: si el dueño dejo un plato listo, el mesero lo lleva aunque el chef ya estuviera cocinando eso.) */
    private boolean mesaOcupada(Mesa m, Ayudante yo, boolean contarChefs) {
        for (Ayudante b : ayudantes) if (b != yo && (b.mesaServ == m || (contarChefs && (b.mesaChef == m || b.paseMesa == m)))) return true;
        return false;
    }

    private boolean hayMeseros() { for (Ayudante a : ayudantes) if (a.rol == Personal.Rol.MESERO && !a.e.isRemoved()) return true; return false; }

    /** Las estaciones por las que pasa el chef para ese pedido, en orden (null si no sabe hacerlo): la receta del plato, o, para un visitante especial, cajas → procesadoras → mesa de emplatar. */
    private List<Estacion> recetaDe(Cliente c) {
        if (c.critico) return null;
        if (c.esp != null) {
            List<Estacion> l = new ArrayList<>(c.esp.requiere);
            l.sort((x, y) -> Integer.compare(orden(x), orden(y)));
            return l;
        }
        Recetas.Plato pl = Recetas.Plato.de(c.pedido);
        return pl == null ? null : pl.estaciones;
    }

    private static int orden(Estacion e) { return switch (e.tipo) { case CAJA -> 0; case PROCESO -> 1; case EMPLATAR -> 2; default -> 3; }; }

    /** Pone en marcha lo que mueve a los empleados: cada tick andan y, los que estan en su sitio sin nada que hacer, buscan una tarea (solo con el cafe abierto). */
    private void tickAyudantes() {
        if (ayudantes.isEmpty()) return;
        ServerPlayerEntity duenioP = estado == Estado.ABIERTO ? jugador(duenio) : null;
        if (duenioP == null) {                                    // cerrado (o el dueño no esta): todos a su sitio
            for (Ayudante a : ayudantes) if (!a.e.isRemoved() && (a.ocupado() || a.llevado != null || a.paseItem != null || !a.enCasa())) { dbg(a, "cafe cerrado: vuelve a su sitio"); a.alSitio(); a.rotular(""); }
            return;
        }
        if (DEBUG) for (int i = 0; i < ayudantes.size(); i++) for (int j = i + 1; j < ayudantes.size(); j++) {      // pruebas: ¿se pisan dos empleados?
            Ayudante p = ayudantes.get(i), q = ayudantes.get(j);
            double d = Math.hypot(p.e.getX() - q.e.getX(), p.e.getZ() - q.e.getZ());
            if (d < 0.5) EmiCafe.LOG.info("[personal] CHOQUE {} y {} a {} bloques ({}, {})", p.emp.nombre, q.emp.nombre, String.format("%.2f", d), String.format("%.1f", p.e.getX()), String.format("%.1f", p.e.getZ()));
        }
        if (DEBUG) for (Ayudante a : ayudantes) if (!a.e.isRemoved() && !rutas.pisable((int) Math.floor(a.e.getX()), (int) Math.floor(a.e.getZ()), a.e.getY(), t)) EmiCafe.LOG.info("[personal] ATRAVIESA {} esta en una casilla ocupada ({}, {})", a.emp.nombre, String.format("%.1f", a.e.getX()), String.format("%.1f", a.e.getZ()));
        for (Ayudante a : ayudantes) {                            // el plato que un chef dejo en su ventanilla y ya no hace falta se tira
            if (a.paseItem != null && a.paseReserva == null && !esperaPlato(a.paseMesa, a.paseItem)) { dbg(a, "tira el plato de la ventanilla (el cliente se fue)"); a.vaciarPase(); }
        }
        for (Ayudante a : ayudantes) {
            if (a.e.isRemoved()) continue;
            a.mover(ayudantes, t);
            if (a.cancelarAviso) { a.cancelarAviso = false; dbg(a, "tarea cancelada (el cliente se fue o se llevaron el plato)"); mandarACasa(a); continue; }
            if (a.ocupado()) continue;
            if (!a.enCasa()) { mandarACasa(a); continue; }
            if (--a.espera > 0) continue;
            switch (a.rol) {
                case LAVAPLATOS -> tareaLavaplatos(a);
                case MESERO -> tareaMesero(a);
                case CHEF -> tareaChef(a);
            }
            a.espera = a.ocupado() ? 0 : a.t(20);                  // sin tarea: vuelve a mirar dentro de 1 s
        }
    }

    private void tareaLavaplatos(Ayudante a) {
        List<Mesa> sucias = new ArrayList<>();
        for (Mesa m : mesas) if (m.sucio != null) sucias.add(m);
        sucias.sort((x, y) -> Long.compare(x.sucioDesde, y.sucioDesde));                // la que lleva mas tiempo sucia, primero
        for (Mesa m : sucias) {
            boolean otro = false;
            for (Ayudante b : ayudantes) if (b != a && b.mesaLava == m) otro = true;
            if (otro) continue;
            double[] aMesa = irJunto(a, m.bloque, pos(a), null);
            if (aMesa == null) { a.pasos.clear(); continue; }
            a.mesaLava = m;
            dbg(a, "va a recoger el plato sucio de la mesa " + m.bloque.toShortString());
            BlockPos fr = posDe(Estacion.FREGADERO);
            a.paso(aMesa[0], aMesa[1], a.t(14), null, () -> {
                if (m.sucio == null) { mandarACasa(a); return; }
                Disp.quitar(m.sucio); m.sucio = null;
                dbg(a, "recogio el plato sucio, va al fregadero");
                a.llevar(w, CafeItems.id(CafeItems.PILA_PLATOS));
                Runnable lavado = () -> { a.llevar(w, null); platos++; a.mesaLava = null; dbg(a, "plato lavado (limpios: " + platos + ")"); };
                double[] aFr = fr == null ? null : irJunto(a, fr, pos(a), null);
                if (aFr == null) { a.paso(a.e.getX(), a.e.getZ(), a.t(40), () -> lavando(a), lavado, Double.NaN, Double.NaN); return; }   // sin fregadero (lo lleva el dueño en la mano): lava alli mismo
                a.paso(aFr[0], aFr[1], a.t(50), () -> lavando(a), lavado, fr.getX() + 0.5, fr.getZ() + 0.5);
            }, m.bloque.getX() + 0.5, m.bloque.getZ() + 0.5);
            a.vigente = () -> m.sucio != null || a.llevado != null;
            return;
        }
    }

    private void lavando(Ayudante a) {
        Sonidos.mundo(w, a.e.getX(), a.e.getY(), a.e.getZ(), "lavar", 0.6f, 1.0f);
        w.spawnParticles(ParticleTypes.SPLASH, a.e.getX(), a.e.getY() + 0.8, a.e.getZ(), 6, 0.3, 0.2, 0.3, 0.0);
    }

    /** El mesero lleva el plato que ya esta listo (en una estacion que dejo el dueño o en la ventanilla de un chef) al cliente que lleva mas tiempo esperandolo. */
    private void tareaMesero(Ayudante a) {
        if (platos <= 0) return;
        for (Mesa m : esperando()) {
            Cliente c = m.cliente;
            if (mesaOcupada(m, a, false)) continue;
            Item plato = c.pedido;
            BlockPos origen = null;
            Est est = null;
            Estacion es = null;
            Ayudante chef = null;
            for (Map.Entry<BlockPos, Est> en : ests.entrySet()) {
                Est x = en.getValue();
                if (x.listo != plato || x.reserva != null) continue;
                es = Estacion.de(w.getBlockState(en.getKey()).getBlock());
                if (es == null) continue;
                est = x; origen = en.getKey();
                break;
            }
            if (origen == null) for (Ayudante ch : ayudantes) {
                if (ch.paseItem == plato && ch.paseMesa == m && ch.paseReserva == null && ch.pase != null) { chef = ch; origen = ch.pase; break; }
            }
            if (origen == null) continue;
            double[] aOrigen = irJunto(a, origen, pos(a), null);
            if (aOrigen == null) { a.pasos.clear(); continue; }
            a.mesaServ = m;
            final Est estF = est;
            final Estacion esF = es;
            final Ayudante chefF = chef;
            final BlockPos origenF = origen;
            if (est != null) { a.estReserva = est; est.reserva = a; dbg(a, "va por " + es.nombre + " para servir en la mesa " + m.bloque.toShortString()); }
            else { chef.paseReserva = a; dbg(a, "va a la ventanilla de " + chef.emp.nombre + " para servir en la mesa " + m.bloque.toShortString()); }
            a.paso(aOrigen[0], aOrigen[1], a.t(10), null, () -> {
                if (estF != null) {
                    if (estF.listo != plato) { mandarACasa(a); return; }               // alguien (el dueño) se lo llevo
                    estF.listo = null; estF.listoTicks = 0;
                    dibujar(origenF, esF, estF);
                    estF.reserva = null; a.estReserva = null;
                } else {
                    if (chefF.paseItem != plato) { mandarACasa(a); return; }
                    chefF.vaciarPase();
                    chefF.paseReserva = null;
                }
                dbg(a, "tiene el plato, va a la mesa");
                a.llevar(w, CafeItems.id(plato));
                a.vigente = () -> esperaPlato(m, plato);
                double[] aMesa = irJunto(a, m.bloque, pos(a), asiento(m));
                if (aMesa == null) { dbg(a, "no encuentra camino a la mesa"); mandarACasa(a); return; }
                a.paso(aMesa[0], aMesa[1], a.t(10), null, () -> servir(a, m, plato), m.bloque.getX() + 0.5, m.bloque.getZ() + 0.5);
            }, origen.getX() + 0.5, origen.getZ() + 0.5);
            a.vigente = () -> (estF != null ? (estF.listo == plato) : (chefF.paseItem == plato)) || a.llevado != null ? esperaPlato(m, plato) : false;
            return;
        }
    }

    /** ¿Sigue esperando el cliente de esa mesa ese plato? */
    private static boolean esperaPlato(Mesa m, Item plato) {
        Cliente c = m != null ? m.cliente : null;
        return c != null && c.fase == Cliente.Fase.ESPERANDO && c.pedido == plato;
    }

    /** El empleado llega a la mesa con el plato: si el cliente sigue esperando eso, se lo da (y el dueño cobra). */
    private void servir(Ayudante a, Mesa m, Item plato) {
        a.llevar(w, null);
        a.vigente = null;
        ServerPlayerEntity d = jugador(duenio);
        if (d != null && platos > 0 && esperaPlato(m, plato)) {
            w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, a.e.getX(), a.e.getY() + 0.9, a.e.getZ(), 8, 0.3, 0.3, 0.3, 0.0);
            cobrar(d, m, plato, true);
            dbg(a, "sirvio y cobro");
        } else dbg(a, "llego a la mesa pero ya no hacia falta");
        a.mesaServ = null; a.mesaChef = null; a.platoChef = null;
        a.rotular("");
    }

    /** El chef coge al cliente que lleva mas tiempo esperando y sabe cocinar, recorre sus estaciones y, con meseros, deja el plato en su ventanilla; sin meseros lo lleva el mismo. */
    private void tareaChef(Ayudante a) {
        if (platos <= 0 || a.paseItem != null) return;
        for (Mesa m : esperando()) {
            Cliente c = m.cliente;
            List<Estacion> receta = recetaDe(c);
            if (receta == null || receta.isEmpty() || !tieneEstaciones(receta) || mesaOcupada(m, a, true)) continue;
            Item plato = c.pedido;
            String nombrePlato = CafeItems.nombre(plato);
            double[] f = pos(a);
            boolean ok = true;
            for (int k = 0; k < receta.size(); k++) {
                Estacion es = receta.get(k);
                boolean ultima = k == receta.size() - 1;
                BlockPos b = posDe(es);
                double[] fin = b == null ? null : irJunto(a, b, f, null);
                if (fin == null) { ok = false; break; }
                int espera = es.tipo == Estacion.Tipo.CAJA ? 24 : es.tipo == Estacion.Tipo.EMPLATAR ? 44 : Math.max(44, Math.min(100, es.ticks / 2));
                boolean proceso = es.tipo != Estacion.Tipo.CAJA;
                a.paso(fin[0], fin[1], a.t(espera), () -> cocinando(a, proceso), ultima ? () -> a.llevar(w, CafeItems.id(plato)) : null, b.getX() + 0.5, b.getZ() + 0.5);   // al acabar la ultima, el plato queda en sus manos
                f = fin;
            }
            boolean aLaVentanilla = ok && a.pase != null && hayMeseros();
            if (ok) {
                BlockPos destino = aLaVentanilla ? a.pase : m.bloque;
                double[] fin = irJunto(a, destino, f, aLaVentanilla ? null : asiento(m));
                if (fin == null) ok = false;
                else {
                    // al llegar a la ultima estacion el chef coge el plato (se ve en sus manos) y va a dejarlo
                    a.paso(fin[0], fin[1], a.t(10), null, () -> {
                        if (aLaVentanilla) {
                            a.llevar(w, null);
                            if (!esperaPlato(m, plato)) { dbg(a, "el cliente se fue: tira el plato"); a.mesaChef = null; a.platoChef = null; a.vigente = null; a.rotular(""); return; }
                            a.ponerEnPase(w, plato, m);
                            a.mesaChef = null; a.platoChef = null; a.vigente = null; a.rotular("");
                            dbg(a, "dejo " + nombrePlato + " en su ventanilla para la mesa " + m.bloque.toShortString());
                        } else servir(a, m, plato);
                    }, destino.getX() + 0.5, destino.getZ() + 0.5);
                }
            }
            if (!ok) { a.pasos.clear(); continue; }
            a.mesaChef = m; a.platoChef = c.esp != null ? null : Recetas.Plato.de(plato);
            a.rotular("cocina: " + nombrePlato);
            dbg(a, "cocina " + nombrePlato + (c.esp != null ? " (visitante especial)" : "") + " para la mesa " + m.bloque.toShortString());
            a.vigente = () -> esperaPlato(m, plato) && !mesaOcupada(m, a, false);          // si un mesero ya lleva a esa mesa el plato del dueño, el chef lo deja
            return;
        }
    }

    private void cocinando(Ayudante a, boolean proceso) {
        if (DEBUG && t % 20 < 10) dbg(a, "trabajando en x=" + String.format("%.1f", a.e.getX()));
        w.spawnParticles(proceso ? ParticleTypes.CLOUD : ParticleTypes.HAPPY_VILLAGER, a.e.getX(), a.e.getY() + 1.1, a.e.getZ(), 3, 0.25, 0.1, 0.25, 0.01);
        if (proceso) Sonidos.mundo(w, a.e.getX(), a.e.getY(), a.e.getZ(), "chisporroteo", 0.3f, 1.4f);
    }

    // ------------------------------------------------------------ el libro de recetas: «muestrame como»

    private record Muestra(List<BlockPos> sitios, List<String> nombres, long inicio, long hasta) {}
    private final Map<UUID, Muestra> muestras = new HashMap<>();

    /** El libro de la pared: enseña un plato. Los sitios de las estaciones que hacen falta se iluminan uno detras de otro con chispas (y la barra de abajo dice el paso). */
    void mostrarReceta(ServerPlayerEntity p, Recetas.Plato pl) {
        if (pl == null) return;
        List<BlockPos> sitios = new ArrayList<>();
        List<String> nombres = new ArrayList<>();
        List<String> faltan = new ArrayList<>();
        for (Estacion e : pl.estaciones) {
            BlockPos b = posDe(e);
            if (b == null) faltan.add(e.nombre); else { sitios.add(b); nombres.add(e.nombre); }
        }
        p.sendMessage(Msg.say(pl.nombre() + " (paga " + pl.precio + " monedas): " + pl.receta + "."), false);
        if (!faltan.isEmpty()) p.sendMessage(Msg.err("Aún no tienes en tu café: " + String.join(", ", faltan) + "."), false);
        if (!sitios.isEmpty()) {
            muestras.put(p.getUuid(), new Muestra(sitios, nombres, t, t + 20L * 6 * sitios.size()));
            p.sendMessage(Msg.say("Mira las chispas: te enseñan, una por una, las estaciones que usa."), false);
        }
    }

    private void tickMuestras(List<ServerPlayerEntity> ps) {
        if (muestras.isEmpty()) return;
        for (java.util.Iterator<Map.Entry<UUID, Muestra>> it = muestras.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Muestra> en = it.next();
            Muestra m = en.getValue();
            ServerPlayerEntity p = jugador(en.getKey());
            if (p == null || t > m.hasta() || !jugadores.contains(en.getKey())) { it.remove(); continue; }
            int i = (int) (((t - m.inicio()) / 50) % m.sitios().size());
            BlockPos b = m.sitios().get(i);
            for (double dy = 1.2; dy <= 2.6; dy += 0.35) w.spawnParticles(p, net.minecraft.particle.ParticleTypes.HAPPY_VILLAGER, true, b.getX() + 0.5, b.getY() + dy, b.getZ() + 0.5, 1, 0.1, 0.0, 0.1, 0.0);
            if (t % 10 == 5) msg(p, "Paso " + (i + 1) + "/" + m.sitios().size() + ": " + m.nombres().get(i));
        }
    }

    // ------------------------------------------------------------ modo construccion

    /** ¿Puede ir una estacion en esta casilla ({x, z} del local)? En cualquier sitio donde se pueda construir de tu parcela (con suelo debajo); la puerta y su camino se quedan libres. */
    boolean casillaEstacion(int x, int z) {
        BlockPos p = Local.pos(ox, x, 1, z);
        return construible(p) && !w.getBlockState(p.down()).isAir();
    }

    /** Donde va cada estacion del cafe ahora: la que eligio el dueño, o su sitio de siempre, o la primera casilla libre si ese sitio ya esta ocupado. No incluye las que lleva en la mano. */
    Map<Estacion, int[]> posEstaciones() {
        Datos.Jugador d = dj();
        if (d.libre) return obras.estaciones();                      // parcela libre: las que el dueño ha puesto (x, z, y del local)
        if (d.estPos == null) d.estPos = new java.util.HashMap<>();
        Set<Estacion> act = activas();
        List<Estacion> quedan = new ArrayList<>();
        for (Estacion e : Estacion.values()) if (e.x >= 0 && (e.siempre || act.contains(e)) && !estEnMano.contains(e)) quedan.add(e);
        Map<Estacion, int[]> r = new java.util.EnumMap<>(Estacion.class);
        Set<Integer> ocupadas = new java.util.HashSet<>();
        for (java.util.Iterator<Estacion> it = quedan.iterator(); it.hasNext(); ) {         // 1.º las que eligio el dueño
            Estacion e = it.next();
            int[] q = d.estPos.get(e.name());
            if (q != null && q.length == 2 && casillaEstacion(q[0], q[1]) && ocupadas.add(q[0] * 100 + q[1])) { r.put(e, q); it.remove(); }
        }
        for (java.util.Iterator<Estacion> it = quedan.iterator(); it.hasNext(); ) {         // 2.º las que siguen en su sitio de siempre
            Estacion e = it.next();
            if (casillaEstacion(e.x, e.z) && ocupadas.add(e.x * 100 + e.z)) { r.put(e, new int[]{e.x, e.z}); it.remove(); }
        }
        for (Estacion e : quedan) {                                                        // 3.º el resto, donde quepa
            search:
            for (int z : new int[]{1, 5, 2, 3, 4}) for (int x = 1; x < Local.ancho(nivel(Recetas.Mejora.AMPLIAR)); x++)
                if (casillaEstacion(x, z) && ocupadas.add(x * 100 + z)) { r.put(e, new int[]{x, z}); break search; }
        }
        return r;
    }

    /** Donde estan las mesas ({x, z} del local): las que el dueño ha movido o, si faltan, los sitios de siempre. */
    List<int[]> mesasPos() {
        Datos.Jugador d = dj();
        if (d.libre) return obras.mesas();                           // parcela libre: las que el dueño ha puesto (x, z, y del local)
        if (d.mesasPos == null) d.mesasPos = new ArrayList<>();
        int n = numMesas();
        if (d.mesasCap < n) {                                  // se ganaron mesas con una mejora: las nuevas salen en sus sitios de siempre
            int faltan = n - d.mesasCap;
            for (int[] def : Local.MESAS) {
                if (faltan <= 0) break;
                boolean usado = false;
                for (int[] q : d.mesasPos) if (q[0] == def[0] && q[1] == def[1]) usado = true;
                if (!usado) { d.mesasPos.add(def.clone()); faltan--; }
            }
            d.mesasCap = n;
        }
        return d.mesasPos;
    }

    /** ¿Esta este bloque dentro del edificio de este cafe (paredes incluidas)? */
    boolean contiene(BlockPos pos) {
        if (dj().libre) return enParcela(pos);
        int rx = pos.getX() - ox;
        return rx >= 0 && rx <= Local.ancho(nivel(Recetas.Mejora.AMPLIAR)) && pos.getZ() >= 0 && pos.getZ() <= 13;
    }

    /** ¿Esta este bloque en la parcela de este cafe (edificio, jardin, acera, calle y parque)? */
    boolean enParcela(BlockPos pos) {
        int rx = pos.getX() - ox;
        return rx >= Ciudad.X0 && rx <= Ciudad.X1 && pos.getZ() >= Local.Z_ATRAS && pos.getZ() <= Local.Z_FRENTE;
    }

    boolean construible(BlockPos pos) { return obras.construible(pos); }

    /** Vuelve a poner lo que el dueño construyo (despues de reconstruir la parcela). */
    void aplicarDecor() { obras.aplicarDecor(); }

    /** Entra en el modo construccion (solo el dueño, con el cafe cerrado y sin invitados). Devuelve el error, o null. */
    String entrarConstruccion(ServerPlayerEntity p) { return obras.entrar(p); }

    void salirConstruccion(ServerPlayerEntity p) { obras.salir(p); }

    /** Clic derecho del dueño en modo construccion. */
    ActionResult clicConstruccion(ServerPlayerEntity p, net.minecraft.util.hit.BlockHitResult hit, Hand hand) { return obras.clic(p, hit, hand); }

    /** Clic izquierdo del dueño en modo construccion. */
    ActionResult ataqueConstruccion(ServerPlayerEntity p, BlockPos pos) { return obras.ataque(p, pos); }

    // ------------------------------------------------------------ mensajes a todos

    void aTodos(java.util.function.Consumer<ServerPlayerEntity> f) { for (ServerPlayerEntity p : presentes()) f.accept(p); }

    void sonido(double x, double y, double z, SoundEvent s, float vol, float pitch) { w.playSound(null, x, y, z, s, SoundCategory.BLOCKS, vol, pitch); }

    // ------------------------------------------------------------ cosas que piden los clientes

    /** Lo que pide un cliente de esa especie: a menudo su plato favorito, en el dia del plato casi siempre ese, y si no uno cualquiera de los que se saben. */
    Item elegirPedido(Random r, String especie) {
        if (pedidoForzado != null) return pedidoForzado;
        List<String> ids = idsServibles();
        if (ids.isEmpty()) return CafeItems.PAPAS_FRITAS;
        if (platoDelDia != null && r.nextInt(100) < 30) return platoDelDia.item;
        Item fav = favoritoDe(especie);
        if (fav != null && r.nextInt(100) < 45) return fav;
        Recetas.Plato pl = Recetas.Plato.de(ids.get(r.nextInt(ids.size())));
        return pl == null ? CafeItems.PAPAS_FRITAS : pl.item;
    }

    void alEnfadarse(Cliente c) {
        enfadados++;
        ganarRep(c.critico ? -10 : -3);
        aTodos(p -> Msg.actionBar(p, Text.literal("Un cliente se fue enfadado...").formatted(Formatting.RED)));
        sonido(c.e.getX(), c.e.getY(), c.e.getZ(), SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }

    // ------------------------------------------------------------ el turno

    /** Abre el cafe (solo el dueño, y solo si ya eligio cocinero). No hay reloj: se cierra desde la caja registradora cuando se quiera. */
    String abrirTurno() {
        if (construyendo) return "Termina de construir antes de abrir el café.";
        if (estado != Estado.PREPARANDO) return "El café ya está abierto.";
        if (dj().inicial == null) return "Primero elige a tu Pokémon cocinero.";
        if (dj().libre) {
            if (mesasPos().isEmpty()) return "Pon al menos una MESA para los clientes (modo construcción).";
            if (idsServibles().isEmpty()) {
                StringBuilder f = new StringBuilder();
                Recetas.Plato pl = dj().platos.isEmpty() ? null : Recetas.Plato.de(dj().platos.get(0));
                if (pl != null) for (Estacion es : pl.estaciones) if (posDe(es) == null) f.append(f.length() > 0 ? ", " : "").append(es.nombre);
                return "Te faltan estaciones puestas para cocinar" + (pl == null ? "" : " " + pl.nombre()) + (f.length() > 0 ? ": " + f : "") + " (modo construcción).";
            }
        }
        estado = Estado.ABIERTO;
        proximoCliente = 40;
        platos = platosMax();
        servidos = enfadados = propinas = ganado = 0;
        ganadoPor.clear(); servidosPor.clear(); estrellasTurno.clear();
        Evento ev = eventoForzado != null ? eventoForzado : elegirEvento();
        eventoForzado = null;
        aTodos(p -> {
            Msg.title(p, Text.literal("¡ABRIMOS!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal("Cierra desde la caja registradora cuando quieras").formatted(Formatting.YELLOW), 5, 50, 10);
            Sonidos.jugador(p, "abrir", 1.0f, 1.0f);
        });
        obras.letrero(true);
        aplicarEvento(ev);
        return null;
    }

    /** El suceso de hoy al azar (con condiciones: el critico pide reputacion y 3 platos, el dia del plato 2 platos, el dia Michialien algun visitante posible). */
    private Evento elegirEvento() {
        java.time.LocalDate hoy = java.time.LocalDate.now();
        boolean fechasHalloween = (hoy.getMonthValue() == 10 && hoy.getDayOfMonth() >= 24) || (hoy.getMonthValue() == 11 && hoy.getDayOfMonth() <= 1);
        if (fechasHalloween && rnd.nextInt(100) < 55) return Evento.HALLOWEEN;
        int r = rnd.nextInt(100);
        Evento e = r < 25 ? Evento.NINGUNO : r < 45 ? Evento.HORA_PUNTA : r < 65 ? Evento.PLATO_DEL_DIA : r < 85 ? Evento.DIA_MICHIALIEN : Evento.CRITICO;
        Datos.Jugador d = dj();
        if (e == Evento.CRITICO && (Reputacion.nivel(d.rep) < Reputacion.NIVEL_CRITICO || d.platos.size() < 3)) e = Evento.HORA_PUNTA;
        if (e == Evento.PLATO_DEL_DIA && d.platos.size() < 2) e = Evento.HORA_PUNTA;
        if (e == Evento.DIA_MICHIALIEN && elegibles().isEmpty()) e = Evento.HORA_PUNTA;
        return e;
    }

    /** Pone el suceso del dia (y lo anuncia a todos). */
    void aplicarEvento(Evento e) {
        evento = e;
        platoDelDia = null; criticoPendiente = false;
        if (e == Evento.NINGUNO) return;
        if (e == Evento.PLATO_DEL_DIA) {
            List<Recetas.Plato> sabe = new ArrayList<>();
            for (String id : idsServibles()) { Recetas.Plato pl = Recetas.Plato.de(id); if (pl != null) sabe.add(pl); }
            if (sabe.isEmpty()) { evento = Evento.NINGUNO; return; }
            platoDelDia = sabe.get(rnd.nextInt(sabe.size()));
        }
        if (e == Evento.CRITICO) { criticoPendiente = true; criticoEn = 20 * 25; }
        String texto = e.nombre + (platoDelDia != null ? ": " + platoDelDia.nombre() + " (se paga el doble)" : "") + ". " + e.descripcion;
        aTodos(p -> { p.sendMessage(Msg.say("Hoy: " + texto), false); Sonidos.jugador(p, "evento", 1f, 1f); });
    }

    /** Cierra el cafe: no entran mas clientes y los que esperan se van sin pagar. */
    void cerrarTurno() {
        if (estado != Estado.ABIERTO) return;
        estado = Estado.CERRANDO;
        cierre = 0;
        for (Cliente c : clientes) c.despedir();
        obras.letrero(false);
        aTodos(p -> {
            Msg.title(p, Text.literal("¡CERRAMOS!").formatted(Formatting.RED, Formatting.BOLD), Text.literal("Hasta mañana").formatted(Formatting.YELLOW), 5, 40, 10);
            Sonidos.jugador(p, "cerrar", 1.0f, 1.0f);
        });
    }

    private void terminarTurno() {
        for (ServerPlayerEntity p : presentes()) {
            Datos.Jugador d = dj(p);
            int gan = ganadoPor.getOrDefault(p.getUuid(), 0), serv = servidosPor.getOrDefault(p.getUuid(), 0);
            if (serv >= 3) {                                                          // cerrar sin haber servido casi nada no cuenta como jornada
                d.turnos++;
                Progreso.dar(this, d, p.getUuid(), 5);                                    // por terminar el turno sirviendo
                Progreso.evento(this, p, d, "turnos", 1);
                if (enfadados == 0) { d.sinEnfados++; Progreso.dar(this, d, p.getUuid(), 5); Progreso.evento(this, p, d, "sin_enfado", 1); }
                if (gan > d.mejorTurno) { if (d.mejorTurno > 0) Progreso.dar(this, d, p.getUuid(), 5); d.mejorTurno = gan; }      // el primer turno no cuenta como record
                Progreso.evento(this, p, d, "turno_max", gan);
            }
            Progreso.revisarLogros(this, p, d);
            EmiCafe.datos.guardar(p.getUuid());
        }
        estado = Estado.PREPARANDO;
        evento = Evento.NINGUNO; platoDelDia = null; criticoPendiente = false;
        for (UUID id : jugadores) manos.put(id, null);
        reconstruir();
        aTodos(p -> { sincronizarMano(p); Menus.resumen(this, p); });
    }

    // ------------------------------------------------------------ cada tick

    void tick() {
        List<ServerPlayerEntity> ps = presentes();
        if (ps.isEmpty()) return;
        t++;
        for (ServerPlayerEntity p : ps) {
            if (p.getServerWorld() != w) { Cafes.sinRestaurar(p); continue; }          // se fue por otro medio (otro minijuego): ya no es de este cafe; Retornos le devuelve lo suyo si vuelve aqui
            if (p.getY() < Local.Y - 8) p.teleport(w, ox + Local.SX, Local.Y + 1, Local.SZ, 180f, 0f);
        }
        ps = presentes();
        if (ps.isEmpty()) return;
        if (estado == Estado.ABIERTO) {
            if (--proximoCliente <= 0) { proximoCliente = intervalo(); nuevoCliente(); }
            if (criticoPendiente && --criticoEn <= 0) { if (forzarCritico()) criticoPendiente = false; else criticoEn = 40; }
        }
        tickAyudantes();
        obras.tick(t);
        tickMuestras(ps);
        if (tut != null && t % 10 == 0) { tut.tick(); if (tut.terminada()) tut = null; }
        for (int i = clientes.size() - 1; i >= 0; i--) {
            Cliente c = clientes.get(i);
            boolean fin = c.fuera() || c.tick(this, rnd);
            if (c.fase == Cliente.Fase.SALIENDO && c.mesa.cliente == c) {
                c.mesa.cliente = null; Disp.quitar(c.mesa.plato); c.mesa.plato = null;
                if (!c.enfadado && c.sirvio) ponerSucio(c.mesa);
            }
            if (fin) { c.quitar(); if (c.mesa.cliente == c) c.mesa.cliente = null; clientes.remove(i); }
        }
        if (estado == Estado.CERRANDO) {
            cierre++;
            if (clientes.isEmpty() || cierre > 20 * 12) terminarTurno();
        }
        for (Map.Entry<BlockPos, Est> en : ests.entrySet()) tickEstacion(en.getKey(), en.getValue());
        if (t % 2 == 0) for (ServerPlayerEntity p : ps) sincronizarMano(p);
        if (t % 10 == 0) {
            for (ServerPlayerEntity p : ps) {
                p.getHungerManager().setFoodLevel(20);
                p.setHealth(p.getMaxHealth());
                Msg.actionBar(p, hud(p));
            }
        }
        if (t % 5 == 0) for (ServerPlayerEntity p : ps) enviarPanel(p);
        if (t % 10 == 0) {
            for (Entity e : w.getEntitiesByType(EntityType.ITEM, new Box(ox - 4, Local.Y - 2, -4, ox + Local.ancho(2) + 6, Local.Y + 8, 24), x -> true)) e.discard();
        }
    }

    private Text hud(ServerPlayerEntity p) {
        if (visitantes.contains(p.getUuid())) {
            ServerPlayerEntity du = jugador(duenio);
            Datos.Jugador d = dj();
            return Text.literal("Visitando el café de " + (du == null ? "?" : du.getGameProfile().getName()) + " · ♥ " + d.megusta + " · Estilo: " + Estilo.NOMBRES[nivelEstilo()] + " · Reputación: " + Reputacion.nombre(Reputacion.nivel(d.rep)) + " · caja registradora = menú").formatted(Formatting.AQUA);
        }
        Item c = manos.get(p.getUuid());
        Text mano = c == null ? Text.literal("manos vacías").formatted(Formatting.GRAY) : Text.literal(CafeItems.nombre(c)).formatted(Formatting.YELLOW);
        Text r = Text.literal("");
        if (estado == Estado.ABIERTO) r = Text.literal("● ABIERTO" + (evento != Evento.NINGUNO ? " · " + evento.nombre : "") + "   ").formatted(Formatting.GREEN, Formatting.BOLD);
        else if (construyendo) r = Text.literal("🔨 CONSTRUYENDO — clic derecho = poner · clic izquierdo = quitar · caja registradora = menú   ").formatted(Formatting.GOLD, Formatting.BOLD);
        else if (estado == Estado.PREPARANDO) r = Text.literal("Café cerrado — usa la caja registradora   ").formatted(Formatting.AQUA);
        return Text.empty().append(r).append(Text.literal("Llevas: ").formatted(Formatting.GOLD)).append(mano);       // las monedas, los platos limpios y los pedidos van en el panel del cliente
    }

    /** Lo que dibuja el panel de la esquina (cliente): local, monedas, ganancias de la jornada y los pedidos que esperan. Sin panel viejo = el cliente lo olvida a los 2 s. */
    private void enviarPanel(ServerPlayerEntity p) {
        com.google.gson.JsonObject o = new com.google.gson.JsonObject();
        Datos.Jugador d = dj();
        boolean visita = visitantes.contains(p.getUuid());
        o.addProperty("visita", visita);
        o.addProperty("cafe", d.nombreCafe == null || d.nombreCafe.isEmpty() ? "Café Michialien" : d.nombreCafe);
        o.addProperty("dueno", d.nombre);
        o.addProperty("estado", estado == Estado.ABIERTO ? "abierto" : construyendo ? "obras" : estado == Estado.CERRANDO ? "cerrando" : "cerrado");
        o.addProperty("local", nivelLocal());
        o.addProperty("rep", Reputacion.nombre(nivelRep()));
        o.addProperty("estilo", Estilo.NOMBRES[nivelEstilo()]);
        o.addProperty("megusta", d.megusta);
        if (!visita) {
            o.addProperty("monedas", dj(p).monedas);
            o.addProperty("ganado", ganadoPor.getOrDefault(p.getUuid(), 0));
            o.addProperty("ganadoCafe", ganado);
            o.addProperty("servidos", servidos);
            o.addProperty("enfadados", enfadados);
            o.addProperty("propinas", propinas);
            o.addProperty("platos", platos);
            o.addProperty("equipo", trabajadores());
            if (!evento.equals(Evento.NINGUNO)) o.addProperty("evento", evento.nombre);
            com.google.gson.JsonArray ped = new com.google.gson.JsonArray();
            for (Mesa m : mesas) {
                Cliente c = m.cliente;
                if (c == null || c.fase != Cliente.Fase.ESPERANDO || c.pedido == null) continue;
                com.google.gson.JsonObject e = new com.google.gson.JsonObject();
                e.addProperty("item", CafeItems.id(c.pedido));
                e.addProperty("plato", CafeItems.nombre(c.pedido));
                e.addProperty("pac", (int) Math.round(c.pacienciaRestante() * 100));
                e.addProperty("quien", c.esp != null ? c.esp.michi : c.critico ? "Crítico" : c.especie);
                e.addProperty("especial", c.esp != null || c.critico);
                boolean atiende = false;
                for (Ayudante a : ayudantes) if (a.mesaChef == m || a.mesaServ == m) atiende = true;
                e.addProperty("atiende", atiende);
                ped.add(e);
            }
            o.add("pedidos", ped);
        }
        ServerPlayNetworking.send(p, new TextoPayload("panel", o.toString()));
    }

    /** Probabilidad (de 100) de que el siguiente cliente sea un visitante especial, si hay alguno disponible y no hay otro dentro. */
    static final int PROB_ESPECIAL = 12;

    /** Los visitantes especiales que pueden venir: los que tienen todas las estaciones que necesitan sus ingredientes. */
    List<Especial> elegibles() {
        List<Especial> l = new ArrayList<>();
        Set<Estacion> act = activas();
        if (dj().libre) { act = EnumSet.noneOf(Estacion.class); act.addAll(posEstaciones().keySet()); }
        for (Especial e : Especial.values()) if (act.containsAll(e.requiere)) l.add(e);
        return l;
    }

    private boolean hayEspecial() { for (Cliente c : clientes) if (c.esp != null) return true; return false; }

    private void nuevoCliente() {
        List<Mesa> libres = new ArrayList<>();
        for (Mesa m : mesas) if (m.libre()) libres.add(m);
        if (libres.isEmpty()) return;
        Mesa m = libres.get(rnd.nextInt(libres.size()));
        int prob = evento == Evento.DIA_MICHIALIEN ? 40 : evento == Evento.HALLOWEEN ? 45 : PROB_ESPECIAL + Estilo.probEspecialExtra(nivelEstilo());
        if (!hayEspecial() && rnd.nextInt(100) < prob) {
            List<Especial> el = elegibles();
            if (evento == Evento.HALLOWEEN) {                                    // en la Noche de brujas vienen los disfrazados de miedo (si el dueño ya puede atenderlos)
                List<Especial> miedo = new ArrayList<>();
                for (Especial x : el) if (x == Especial.BRUJITA || x == Especial.CALABAZA || x == Especial.DIABLILLO) miedo.add(x);
                if (!miedo.isEmpty()) el = miedo;
            }
            if (!el.isEmpty() && forzarEspecial(m, el.get(rnd.nextInt(el.size())))) return;
        }
        forzarCliente(m);
    }

    /** Crea ya un visitante especial en esa mesa (tambien lo usa el comando de pruebas). */
    boolean forzarEspecial(Mesa m, Especial e) {
        Cliente c = Cliente.crear(w, ox, m, rnd, e, false, nivelLocal(), rutas, t, dj().libre);
        if (c == null) return false;
        m.cliente = c;
        clientes.add(c);
        return true;
    }

    /** El visitante especial llega a su mesa: el café aprende su plato y se lo explica a todos. */
    void alLlegarEspecial(Cliente c) {
        Especial e = c.esp;
        Datos.Jugador d = dj();
        boolean nuevo = !d.especiales.contains(e.id);
        if (nuevo) { d.especiales.add(e.id); ServerPlayerEntity duenioP = jugador(duenio); if (duenioP != null) EmiCafe.datos.guardar(duenio); }
        aTodos(p -> {
            Msg.title(p, Text.literal("¡" + e.michi + "!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal("Quiere: " + e.nombrePlato()).formatted(Formatting.YELLOW), 5, 60, 10);
            p.sendMessage(Msg.say((nuevo ? "¡Visita especial! " : "Otra vez por aquí: ") + e.michi + " " + (nuevo ? "te enseña" : "pide") + " «" + e.nombrePlato() + "»: " + e.receta() + ". Paga " + e.precio + " monedas, y se queda más tiempo."), false);
            Msg.sound(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
        });
        w.spawnParticles(ParticleTypes.END_ROD, c.e.getX(), c.e.getY() + 1.0, c.e.getZ(), 14, 0.4, 0.5, 0.4, 0.02);
    }

    /** Crea ya al critico gastronomico en una mesa libre (false si no hay ninguna). */
    boolean forzarCritico() {
        for (Mesa m : mesas) if (m.libre()) {
            Cliente c = Cliente.crear(w, ox, m, rnd, null, true, nivelLocal(), rutas, t, dj().libre);
            if (c == null) return false;
            m.cliente = c;
            clientes.add(c);
            return true;
        }
        return false;
    }

    /** El critico llega a su mesa. */
    void alLlegarCritico(Cliente c) {
        aTodos(p -> {
            Msg.title(p, Text.literal("¡Crítico gastronómico!").formatted(Formatting.AQUA, Formatting.BOLD), Text.literal("Quiere: " + CafeItems.nombre(c.pedido) + " — paga el triple").formatted(Formatting.YELLOW), 5, 60, 10);
            p.sendMessage(Msg.say("¡Ha llegado un crítico gastronómico! Pide «" + CafeItems.nombre(c.pedido) + "», paga el triple y, si lo dejas contento, tu reputación sube mucho. Si se va enfadado, baja."), false);
            Msg.sound(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0.8f);
        });
        w.spawnParticles(ParticleTypes.END_ROD, c.e.getX(), c.e.getY() + 1.0, c.e.getZ(), 14, 0.4, 0.5, 0.4, 0.02);
    }

    /** Crea un cliente ya (tambien lo usa el comando de pruebas). false si no hay mesa libre. */
    boolean forzarCliente(Mesa m) {
        Cliente c = Cliente.crear(w, ox, m, rnd, null, false, nivelLocal(), rutas, t, dj().libre);
        if (c == null) return false;
        m.cliente = c;
        clientes.add(c);
        return true;
    }

    boolean forzarCliente() {
        for (Mesa m : mesas) if (m.libre()) return forzarCliente(m);
        return false;
    }

    private void ponerSucio(Mesa m) {
        Disp.quitar(m.sucio);
        m.sucioDesde = t;
        m.sucio = Disp.objeto(w, CafeItems.id(CafeItems.PLATO_SUCIO), m.bloque.getX() + 0.5, m.bloque.getY() + 1.3, m.bloque.getZ() + 0.5, 0.5f, true);
    }

    // ------------------------------------------------------------ las manos

    /** El objeto de la mano del jugador tiene que ser lo que dice {@code manos}; el resto del inventario, vacio, y el hueco 1 elegido. */
    void sincronizarMano(ServerPlayerEntity p) {
        if (construyendo) return;                              // construyendo, el inventario es de verdad (los bloques que ha comprado)
        var inv = p.getInventory();
        boolean cambio = false;
        Item c = manos.get(p.getUuid());
        ItemStack quiero = c == null ? ItemStack.EMPTY : new ItemStack(c);
        for (int i = 0; i < inv.size(); i++) {
            ItemStack st = inv.getStack(i);
            if (i == 0) { if (!ItemStack.areItemsEqual(st, quiero) || st.getCount() != quiero.getCount()) { inv.setStack(0, quiero); cambio = true; } }
            else if (!st.isEmpty()) { inv.setStack(i, ItemStack.EMPTY); cambio = true; }
        }
        if (inv.selectedSlot != 0) { inv.selectedSlot = 0; p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket(0)); }
        if (cambio && p.currentScreenHandler == p.playerScreenHandler) { p.playerScreenHandler.sendContentUpdates(); }
    }

    void msg(ServerPlayerEntity p, String s) { Msg.actionBar(p, Text.literal(s).formatted(Formatting.YELLOW)); }

    // ------------------------------------------------------------ usar bloques y clientes

    /** El jugador hace clic derecho en un bloque del local. */
    void usarBloque(ServerPlayerEntity p, BlockPos pos) {
        net.minecraft.block.Block bl = w.getBlockState(pos).getBlock();
        if (bl == CafeBlocks.LIBRO_PARED) { Menus.libro(this, p); return; }
        Estacion e = Estacion.de(bl);
        if (e == null) return;
        if (!contiene(pos)) {                                                        // un local de otro vecino: se puede mirar y pasear, pero no se cocina sin invitacion
            int slotAjeno = Ciudad.slotEn(pos.getX());
            if (e.tipo == Estacion.Tipo.REGISTRADORA) { Cafes.infoVecino(p, slotAjeno); return; }
            String nom = Ciudad.nombreDe(slotAjeno);
            msg(p, nom == null ? "Aquí no se puede cocinar" : "Es el café de " + nom + ": solo puedes cocinar ahí si te invita");
            return;
        }
        if (visitantes.contains(p.getUuid())) { if (e.tipo == Estacion.Tipo.REGISTRADORA) Menus.abrir(this, p, "cafes", false); else msg(p, "Estás de visita: mira, no toques"); return; }
        Item carga = manos.get(p.getUuid());
        double x = pos.getX() + 0.5, y = pos.getY() + 1, z = pos.getZ() + 0.5;
        switch (e.tipo) {
            case CAJA -> {
                if (carga != null) { msg(p, "Ya llevas algo en las manos"); return; }
                manos.put(p.getUuid(), e.ingrediente);
                sonido(x, y, z, SoundEvents.ENTITY_ITEM_PICKUP, 0.6f, 1.2f);
            }
            case BASURA -> {
                if (carga == null) { msg(p, "No llevas nada que tirar"); return; }
                if (carga == CafeItems.PLATO_SUCIO) { msg(p, "Los platos sucios se lavan en el fregadero"); return; }
                manos.put(p.getUuid(), null);
                sonido(x, y, z, SoundEvents.BLOCK_COMPOSTER_FILL, 0.8f, 1.0f);
            }
            case PROCESO -> proceso(p, e, pos, carga);
            case EMPLATAR -> emplatar(p, e, pos, carga);
            case FREGADERO -> fregadero(p, e, pos, carga);
            case MESA -> {
                Mesa m = null;
                for (Mesa k : mesas) if (k.bloque.equals(pos)) m = k;
                if (m == null) return;
                if (m.sucio != null) {
                    if (carga != null) { msg(p, "Tienes las manos ocupadas"); return; }
                    Disp.quitar(m.sucio); m.sucio = null;
                    manos.put(p.getUuid(), CafeItems.PLATO_SUCIO);
                    sonido(x, y, z, SoundEvents.ENTITY_ITEM_PICKUP, 0.6f, 0.9f);
                } else servir(p, m, carga);
            }
            case REGISTRADORA -> Menus.principal(this, p);
        }
        sincronizarMano(p);
    }

    /** Clic derecho a un Pokemon: si es un cliente, es como tocar su mesa. */
    boolean usarEntidad(ServerPlayerEntity p, Entity en) {
        if (visitantes.contains(p.getUuid())) return false;
        for (Cliente c : clientes) if (c.e == en) { servir(p, c.mesa, manos.get(p.getUuid())); sincronizarMano(p); return true; }
        return false;
    }

    private Est est(BlockPos pos) { return ests.computeIfAbsent(pos, k -> new Est()); }

    private void proceso(ServerPlayerEntity p, Estacion e, BlockPos pos, Item carga) {
        Est est = est(pos);
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        if (est.listo != null) {
            if (carga != null) { msg(p, "Tienes las manos ocupadas"); return; }
            manos.put(p.getUuid(), est.listo); est.listo = null; est.listoTicks = 0;
            sonido(x, y, z, SoundEvents.ENTITY_ITEM_PICKUP, 0.6f, 1.0f);
        } else if (est.resultado != null) {
            msg(p, e.nombre + " está trabajando");
        } else if (carga == null) {
            msg(p, "Trae algo para: " + e.nombre.toLowerCase());
        } else {
            Item sale = Recetas.procesa(e, carga);
            if (sale == null) { msg(p, "Eso no va aquí: " + e.nombre.toLowerCase()); sonido(x, y, z, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.7f, 0.8f); return; }
            est.resultado = sale; est.ticks = 0; est.total = Math.max(10, (int) Math.round(e.ticks * velocidad()));
            manos.put(p.getUuid(), null);
            if (e == Estacion.FREIDORA || e == Estacion.PLANCHA || e == Estacion.HORNO || e == Estacion.OLLA) Sonidos.mundo(w, x, y, z, "chisporroteo", 0.8f, 1.0f);
            else sonido(x, y, z, sonidoDe(e), 0.8f, 1.0f);
        }
        dibujar(pos, e, est);
    }

    private static SoundEvent sonidoDe(Estacion e) {
        return switch (e) {
            case FREIDORA, CAFETERA -> SoundEvents.BLOCK_BREWING_STAND_BREW;
            case EXPRIMIDOR, OLLA -> SoundEvents.ITEM_BUCKET_FILL;
            case PLANCHA -> SoundEvents.BLOCK_FIRE_EXTINGUISH;
            case HORNO -> SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE;
            case BATIDORA -> SoundEvents.BLOCK_GRINDSTONE_USE;
            default -> SoundEvents.BLOCK_WOOD_HIT;
        };
    }

    private void emplatar(ServerPlayerEntity p, Estacion e, BlockPos pos, Item carga) {
        Est est = est(pos);
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        if (est.listo != null) {
            if (carga != null) { msg(p, "Tienes las manos ocupadas"); return; }
            manos.put(p.getUuid(), est.listo); est.listo = null;
            sonido(x, y, z, SoundEvents.ENTITY_ITEM_PICKUP, 0.6f, 1.0f);
        } else if (est.resultado != null) {
            msg(p, e.nombre + " está trabajando");
        } else if (carga == null) {
            if (est.dentro.isEmpty()) { msg(p, "Aquí se juntan los ingredientes de un plato"); return; }
            manos.put(p.getUuid(), est.dentro.remove(est.dentro.size() - 1));           // se puede recoger lo que se puso por error
            sonido(x, y, z, SoundEvents.ENTITY_ITEM_PICKUP, 0.6f, 1.0f);
        } else {
            if (!Recetas.sirveParaEmplatar(carga) || est.dentro.contains(carga) || est.dentro.size() >= 2) { msg(p, "Eso no va aquí: " + e.nombre.toLowerCase()); sonido(x, y, z, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.7f, 0.8f); return; }
            est.dentro.add(carga);
            manos.put(p.getUuid(), null);
            sonido(x, y, z, SoundEvents.BLOCK_WOOD_PLACE, 0.8f, 1.2f);
            if (est.dentro.size() == 2) {
                Item sale = Recetas.combina(est.dentro.get(0), est.dentro.get(1));
                if (sale == null) { msg(p, "Esos ingredientes no combinan"); manos.put(p.getUuid(), est.dentro.remove(1)); }
                else { est.resultado = sale; est.ticks = 0; est.total = Math.max(10, (int) Math.round(e.ticks * velocidad())); est.dentro.clear(); }
            }
        }
        dibujar(pos, e, est);
    }

    private void fregadero(ServerPlayerEntity p, Estacion e, BlockPos pos, Item carga) {
        Est est = est(pos);
        if (carga == CafeItems.PLATO_SUCIO) {
            if (est.cola >= 4) { msg(p, "El fregadero está lleno de platos"); return; }
            est.cola++; est.quien.add(p.getUuid());
            manos.put(p.getUuid(), null);
            sonido(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, SoundEvents.BLOCK_WOOD_PLACE, 0.8f, 0.8f);
            if (est.resultado == null) { est.resultado = CafeItems.PLATO_LIMPIO; est.ticks = 0; est.total = Math.max(10, (int) Math.round(e.ticks * velocidad())); }
        } else msg(p, carga == null ? "Aquí se lavan los platos sucios" : "Aquí solo se lavan platos sucios");
        dibujar(pos, e, est);
    }

    private void tickEstacion(BlockPos pos, Est est) {
        Estacion e = Estacion.de(w.getBlockState(pos).getBlock());
        if (e == null) return;
        double x = pos.getX() + 0.5, y = pos.getY() + 1, z = pos.getZ() + 0.5;
        if (est.resultado != null) {
            est.ticks++;
            if (est.ticks >= est.total) {
                if (e.tipo == Estacion.Tipo.FREGADERO) {
                    est.cola--;
                    platos++;
                    if (!est.quien.isEmpty()) { ServerPlayerEntity lav = jugador(est.quien.remove(0)); if (lav != null) { Datos.Jugador dl = dj(lav); dl.lavados++; Progreso.evento(this, lav, dl, "lavados", 1); } }
                    Sonidos.mundo(w, x, y, z, "lavar", 0.7f, 1.0f);
                    w.spawnParticles(ParticleTypes.SPLASH, x, y + 0.3, z, 6, 0.2, 0.1, 0.2, 0.0);
                    if (est.cola > 0) { est.ticks = 0; } else est.resultado = null;
                } else {
                    est.listo = est.resultado; est.resultado = null; est.listoTicks = 0;
                    Sonidos.mundo(w, x, y, z, "listo", 1.0f, 1.0f);
                    w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 0.2, z, 4, 0.2, 0.1, 0.2, 0.0);
                }
                dibujar(pos, e, est);
            } else if (est.ticks % 5 == 0) {
                dibujar(pos, e, est);
                if ((e == Estacion.FREIDORA || e == Estacion.PLANCHA || e == Estacion.HORNO || e == Estacion.OLLA) && est.ticks % 15 == 0) w.spawnParticles(ParticleTypes.SMOKE, x, y + 0.1, z, 2, 0.15, 0.05, 0.15, 0.0);
                if (e == Estacion.TABLA && est.ticks % 10 == 0) sonido(x, y, z, SoundEvents.BLOCK_WOOD_HIT, 0.5f, 1.4f);
                if (e == Estacion.FREGADERO && est.ticks % 12 == 0) sonido(x, y, z, SoundEvents.BLOCK_WATER_AMBIENT, 0.4f, 1.3f);
            }
        } else if (est.listo != null && e.quema()) {
            est.listoTicks++;
            if (est.listoTicks == QUEMA_AVISO) { sonido(x, y, z, SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 0.6f); w.spawnParticles(ParticleTypes.SMOKE, x, y + 0.2, z, 8, 0.2, 0.1, 0.2, 0.01); dibujar(pos, e, est); }
            else if (est.listoTicks > QUEMA_AVISO && est.listoTicks % 8 == 0) w.spawnParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.3, z, 2, 0.15, 0.1, 0.15, 0.01);
            if (est.listoTicks >= QUEMA) {
                est.listo = e.quemado();
                Sonidos.mundo(w, x, y, z, "quemado", 1.0f, 1.0f);
                w.spawnParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.3, z, 12, 0.25, 0.15, 0.25, 0.02);
                aTodos(pl -> Msg.actionBar(pl, Text.literal("¡Se quemó algo en: " + e.nombre.toLowerCase() + "! Tíralo a la basura").formatted(Formatting.RED)));
                dibujar(pos, e, est);
            }
        }
    }

    /** Lo que se ve sobre la estacion: el objeto (el que se esta haciendo o el terminado) y una barra de avance. */
    private void dibujar(BlockPos pos, Estacion e, Est est) {
        double x = pos.getX() + 0.5, z = pos.getZ() + 0.5, y = pos.getY() + e.alturaVisual();
        Item mostrar = est.listo != null ? est.listo : est.resultado != null && e.tipo != Estacion.Tipo.FREGADERO ? est.resultado : e.tipo == Estacion.Tipo.FREGADERO && est.cola > 0 ? CafeItems.PLATO_SUCIO : !est.dentro.isEmpty() ? est.dentro.get(0) : null;
        if (mostrar == null) { Disp.quitar(est.icono); Disp.quitar(est.barra); est.icono = est.barra = null; return; }
        if (est.icono == null || est.icono.isRemoved()) est.icono = Disp.objeto(w, CafeItems.id(mostrar), x, y + 0.2, z, 0.55f, true);
        else Disp.cambiarObjeto(est.icono, CafeItems.id(mostrar), x, y + 0.2, z, 0.55f, true);
        String barra;
        if (est.listo == CafeItems.PAPAS_QUEMADAS || est.listo == CafeItems.COMIDA_QUEMADA) barra = "{\"text\":\"¡Quemado!\",\"color\":\"red\",\"bold\":true}";
        else if (est.listo != null) barra = est.listoTicks >= QUEMA_AVISO ? "{\"text\":\"¡Se quema!\",\"color\":\"gold\",\"bold\":true}" : "{\"text\":\"¡Listo!\",\"color\":\"green\",\"bold\":true}";
        else if (est.resultado != null) {
            int n = Math.round(8f * est.ticks / Math.max(1, est.total));
            String extra = e.tipo == Estacion.Tipo.FREGADERO ? " x" + est.cola : "";
            barra = "{\"text\":\"" + "▮".repeat(n) + "▯".repeat(8 - n) + extra + "\",\"color\":\"gold\"}";
        } else barra = "{\"text\":\"" + est.dentro.size() + "/2\",\"color\":\"aqua\"}";
        if (est.barra == null || est.barra.isRemoved()) est.barra = Disp.texto(w, barra, x, y + 0.75, z, 0.9f, false);
        else Disp.cambiarTexto(est.barra, barra, x, y + 0.75, z, 0.9f, false);
    }

    private void servir(ServerPlayerEntity p, Mesa m, Item carga) {
        Cliente c = m.cliente;
        if (c == null || c.fase != Cliente.Fase.ESPERANDO) { msg(p, "Aquí no hay nadie esperando"); return; }
        if (carga == null) { msg(p, "Lleva un plato en las manos"); return; }
        if (carga != c.pedido) {
            msg(p, "Eso no es lo que pidió");
            sonido(c.e.getX(), c.e.getY(), c.e.getZ(), SoundEvents.ENTITY_VILLAGER_NO, 0.8f, 1.2f);
            return;
        }
        if (platos <= 0) { msg(p, "No quedan platos limpios: lava los sucios en el fregadero"); sonido(c.e.getX(), c.e.getY(), c.e.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.8f, 0.7f); return; }
        cobrar(p, m, carga, false);
    }

    /** Cobra el plato servido a esa mesa (a {@code p}; {@code auto} = lo llevo el camarero ayudante, asi que no se toca lo que lleva en las manos). */
    private void cobrar(ServerPlayerEntity p, Mesa m, Item carga, boolean auto) {
        Cliente c = m.cliente;
        Datos.Jugador dd = dj();
        Recetas.Plato pl = Recetas.Plato.de(carga);
        int base = c.esp != null ? c.esp.precio : pl == null ? 10 : pl.precio;
        if (c.critico) base *= 3;
        else if (c.esp == null && platoDelDia != null && pl == platoDelDia) base *= 2;
        double pac = c.pacienciaRestante();
        int propina = (int) Math.round(base * 0.6 * pac * Estilo.multPropina(nivelEstilo()) * (1.0 + 0.10 * nivel(Recetas.Mejora.PROPINAS)));
        boolean fav = c.favorito;
        if (fav) propina += (int) Math.round(base * 0.4);
        int pago = (int) Math.round((base + propina) * Reputacion.multPago(Reputacion.nivel(dd.rep)) * (evento == Evento.HALLOWEEN ? 1.2 : 1.0));
        platos--;
        if (!auto) manos.put(p.getUuid(), null);
        c.comer();
        if (m.plato != null) Disp.quitar(m.plato);
        m.plato = Disp.emplatado(w, CafeItems.id(carga), m.bloque.getX() + 0.5, m.bloque.getY() + 1.3, m.bloque.getZ() + 0.5, 0.6f, true);       // la comida llega emplatada
        Datos.Jugador d = dj(p);
        d.monedas += pago; d.clientes++; d.monedasTotal += pago; d.propinasTotal += propina;
        servidos++; propinas += propina; ganado += pago;
        ganadoPor.merge(p.getUuid(), pago, Integer::sum);
        servidosPor.merge(p.getUuid(), 1, Integer::sum);
        Progreso.dar(this, d, p.getUuid(), c.esp != null ? 12 : c.critico ? 15 : 2);
        if (c.esp != null) { d.especialesServidos++; p.sendMessage(Msg.say("¡" + c.esp.michi + " quedó encantado con «" + c.esp.nombrePlato() + "»!"), false); w.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, c.e.getX(), c.e.getY() + 1.0, c.e.getZ(), 20, 0.4, 0.5, 0.4, 0.2); }
        if (c.critico) { p.sendMessage(Msg.say("¡El crítico quedó encantado! +15 estrellas y mucha reputación."), false); w.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, c.e.getX(), c.e.getY() + 1.0, c.e.getZ(), 20, 0.4, 0.5, 0.4, 0.2); }
        if (fav) { w.spawnParticles(ParticleTypes.HEART, c.e.getX(), c.e.getY() + 1.6, c.e.getZ(), 8, 0.4, 0.3, 0.4, 0.0); }
        Progreso.evento(this, p, d, "servidos", 1);
        Progreso.evento(this, p, d, "ganado", pago);
        if (propina > 0) Progreso.evento(this, p, d, "propinas", propina);
        Progreso.revisarLogros(this, p, d);
        EmiCafe.datos.guardar(p.getUuid());
        ganarRep(c.critico ? 8 : c.esp != null ? 5 : pac > 0.5 ? 2 : 1);
        Sonidos.mundo(w, c.e.getX(), c.e.getY(), c.e.getZ(), "cobrar", 1.0f, 1.0f);
        w.spawnParticles(ParticleTypes.HEART, c.e.getX(), c.e.getY() + 1.4, c.e.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
        Msg.actionBar(p, Text.literal("+" + pago + " monedas" + (propina > 0 ? " (propina " + propina + ")" : "") + (fav ? " ♥ ¡su favorito!" : "") + (auto ? " (camarero)" : "")).formatted(Formatting.GOLD, Formatting.BOLD));
    }

    // ------------------------------------------------------------ cerrar

    /** Quita clientes, objetos y bloques de dibujo. */
    void cerrar() {
        obras.quitarMarcas();
        if (construyendo) { construyendo = false; estEnMano.clear(); if (EmiCafe.datos != null) EmiCafe.datos.guardar(duenio); }
        quitarAyudantes();
        if (tut != null) { tut.quitar(); tut = null; }
        for (Cliente c : clientes) c.quitar();
        clientes.clear();
        for (Mesa m : mesas) { Disp.quitar(m.plato); Disp.quitar(m.sucio); m.plato = null; m.sucio = null; m.cliente = null; }
        for (Est e : ests.values()) { Disp.quitar(e.icono); Disp.quitar(e.barra); }
        ests.clear();
        Disp.barrerParcelaMoviles(w, ox);
    }
}
