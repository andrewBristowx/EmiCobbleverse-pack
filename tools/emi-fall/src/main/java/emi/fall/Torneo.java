package emi.fall;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Un torneo: la cola de la sala, las rondas (un minijuego cada una), los puntos y la entrega del premio simbolico (el titulo de ganador; los premios de verdad los
 * da el dueño). Solo hay uno a la vez. Fases: COLA (la sala espera a los jugadores) → por cada ronda INTRO (se lee la regla) → CUENTA (3, 2, 1) → JUEGO →
 * RESULTADO → ... → FINAL (el ganador) y todos vuelven a la sala.
 */
final class Torneo {
    enum Fase { COLA, INTRO, CUENTA, JUEGO, RESULTADO, FINAL }

    static Torneo actual;

    static final int T_INTRO = 140, T_CUENTA = 70, T_RESULTADO = 160, T_FINAL = 260;
    static final String[] ORDEN = {"carrera", "camino", "bombas", "plataformas"};

    final MinecraftServer server;
    final Map<UUID, Jug> jugs = new LinkedHashMap<>();
    UUID anfitrion;
    Fase fase = Fase.COLA;
    final List<Mini> rondas = new ArrayList<>();
    int idx = -1;
    Mini mini;
    Ctx ctx;
    int t;
    private final ServerBossBar barra = new ServerBossBar(Text.literal("Emi Fall"), BossBar.Color.PINK, BossBar.Style.PROGRESS);
    private int aviso;

    Torneo(MinecraftServer s) { this.server = s; }

    // ------------------------------------------------------------------------------------------------ consultas

    /** ¿Este jugador esta jugando un torneo ya empezado? */
    static boolean participa(ServerPlayerEntity p) { Torneo q = actual; return q != null && q.fase != Fase.COLA && q.jugs.containsKey(p.getUuid()); }

    /** ¿Esta apuntado (en la cola o jugando)? */
    static boolean apuntado(UUID id) { Torneo q = actual; return q != null && q.jugs.containsKey(id); }

    static void quitarEfectos(ServerPlayerEntity p) {
        p.clearStatusEffects();
        p.setGlowing(false);
        p.setInvisible(false);
        p.getAbilities().flying = false;
    }

    // ------------------------------------------------------------------------------------------------ la cola

    /** Apunta al jugador al torneo de la cola (lo crea si no habia). null = bien; si no, el motivo. */
    static String unirse(ServerPlayerEntity p) {
        MinecraftServer s = p.getServer();
        Torneo q = actual;
        if (q != null && q.fase != Fase.COLA) return q.jugs.containsKey(p.getUuid()) ? "Ya estas en el torneo." : "El torneo ya ha empezado: espera al siguiente.";
        if (!Sala.enSala(p)) { String e = Sala.entrar(p); if (e != null) return e; }
        if (q == null) { q = actual = new Torneo(s); q.anfitrion = p.getUuid(); }
        if (q.jugs.containsKey(p.getUuid())) return "Ya estas apuntado.";
        if (q.jugs.size() >= 16) return "El torneo esta lleno (16 jugadores).";
        q.jugs.put(p.getUuid(), new Jug(p.getUuid(), p.getGameProfile().getName()));
        q.avisarTodos(Msg.say(Text.literal(p.getGameProfile().getName() + " se ha apuntado al torneo (" + q.jugs.size() + " jugadores)" + (q.anfitrion.equals(p.getUuid()) ? " y es el anfitrión. Cuando estéis todos: " : ". ")).formatted(Formatting.WHITE))
                .append(q.anfitrion.equals(p.getUuid()) ? Msg.button("EMPEZAR", "/fall empezar", Formatting.GREEN, "Empieza el torneo con los que hay") : Text.empty()));
        return null;
    }

    /** Se quita de la cola o del torneo (sin sacarlo de la sala si solo estaba en la cola). */
    static void dejar(ServerPlayerEntity p) {
        Torneo q = actual;
        if (q == null || !q.jugs.containsKey(p.getUuid())) return;
        Jug j = q.jugs.remove(p.getUuid());
        if (q.ctx != null && q.mini != null) q.mini.alIrse(q.ctx, j);
        q.barra.removePlayer(p);
        if (q.fase != Fase.COLA) {
            Morph.quitar(q.server, p);
            quitarEfectos(p);
            ServerWorld w = Sala.mundo(q.server);
            if (w != null) { p.changeGameMode(GameMode.ADVENTURE); p.teleport(w, Sala.SX, Sala.SY, Sala.SZ, 180f, 0f); }
        }
        q.avisarTodos(Msg.say(j.nombre + " ha dejado el torneo."));
        if (q.jugs.isEmpty()) q.cerrar();
        else if (q.anfitrion.equals(j.id)) q.anfitrion = q.jugs.keySet().iterator().next();
    }

    /** Un jugador se ha desconectado. */
    static void alDesconectar(ServerPlayerEntity p) {
        Torneo q = actual;
        if (q == null || !q.jugs.containsKey(p.getUuid())) return;
        Jug j = q.jugs.remove(p.getUuid());
        if (q.ctx != null && q.mini != null) q.mini.alIrse(q.ctx, j);
        q.barra.removePlayer(p);
        if (q.jugs.isEmpty()) q.cerrar();
        else if (q.anfitrion.equals(j.id)) q.anfitrion = q.jugs.keySet().iterator().next();
    }

    /** Empieza el torneo. {@code juegos} = los ids separados por comas (vacio = los cuatro, en su orden). */
    static String empezar(ServerPlayerEntity quien, boolean admin, String juegos) {
        Torneo q = actual;
        if (q == null) return "No hay ningun torneo: apuntate primero con /fall unirse.";
        if (q.fase != Fase.COLA) return "El torneo ya esta en marcha.";
        if (!admin && !quien.getUuid().equals(q.anfitrion)) return "Solo el anfitrion (o un administrador) puede empezarlo.";
        if (q.jugs.size() < 2 && !admin && !Boolean.getBoolean("emi.fall.prueba")) return "Hacen falta al menos 2 jugadores.";
        q.rondas.clear();
        String[] ids = juegos == null || juegos.isBlank() ? ORDEN : juegos.trim().split("[ ,]+");
        for (String id : ids) { Mini m = Minijuegos.porId(id); if (m == null) return "Minijuego desconocido: " + id + " (hay: " + String.join(", ", Minijuegos.ids()) + ")."; q.rondas.add(m); }
        ServerWorld w = Minijuegos.arena(q.server);
        if (w == null) return "No existe la dimension de las arenas (emi_fall:arena).";
        for (Mini m : q.rondas) m.construir(w);
        for (Jug j : q.jugs.values()) { j.puntos = 0; j.puntosRonda = 0; ServerPlayerEntity p = q.server.getPlayerManager().getPlayer(j.id); if (p != null) q.barra.addPlayer(p); }
        q.idx = -1;
        q.avisarTodos(Msg.say(Text.literal("¡Empieza el torneo con " + q.jugs.size() + " jugadores y " + q.rondas.size() + " rondas!").formatted(Formatting.GOLD, Formatting.BOLD)));
        q.siguiente();
        return null;
    }

    /** Cancela el torneo y devuelve a todos a la sala. */
    static void parar() {
        Torneo q = actual;
        if (q == null) return;
        q.avisarTodos(Msg.say("El torneo se ha cancelado."));
        q.cerrar();
    }

    /** El jugador pulsa el boton derecho con un objeto: se lo pasa al minijuego (las bombas). */
    static void alUsar(ServerPlayerEntity p) {
        Torneo q = actual;
        if (q == null || q.fase != Fase.JUEGO || q.mini == null || q.ctx == null) return;
        Jug j = q.jugs.get(p.getUuid());
        if (j != null && !j.fuera) q.mini.alUsar(q.ctx, j, p);
    }

    // ------------------------------------------------------------------------------------------------ las rondas

    private void siguiente() {
        if (mini != null && ctx != null) { try { mini.limpiar(ctx); } catch (Throwable e) { EmiFall.LOG.error("Error al limpiar {}", mini.id(), e); } }
        idx++;
        if (idx >= rondas.size()) { final_(); return; }
        mini = rondas.get(idx);
        ServerWorld w = Minijuegos.arena(server);
        ctx = new Ctx(this, w);
        for (Jug j : jugs.values()) { j.fuera = false; j.ordenFuera = 0; j.equipo = -1; j.fila = 0; j.caidas = 0; j.checkpoint = 0; j.tLlegada = -1; j.llego = false; j.inmune = 0; j.ultimoTocado = -1; }
        if (mini.equipos()) {                                    // equipos al azar y parejos
            List<Jug> l = new ArrayList<>(jugs.values());
            java.util.Collections.shuffle(l, ctx.rnd);
            for (int i = 0; i < l.size(); i++) l.get(i).equipo = i % 2;
        }
        fase = Fase.INTRO; t = 0;
        barra.setName(Text.literal("Ronda " + (idx + 1) + "/" + rondas.size() + " · " + mini.nombre()).formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
        barra.setPercent(1f);
        try { mini.empezar(ctx); } catch (Throwable e) { EmiFall.LOG.error("Error al preparar {}", mini.id(), e); avisarTodos(Msg.err("Error al preparar " + mini.nombre() + ": se salta.")); siguiente(); return; }
        for (Jug j : jugs.values()) {
            ServerPlayerEntity p = pl(j);
            if (p == null) continue;
            p.getInventory().clear();
            quitarEfectos(p);
            Morph.michi(server, p, mini.equipos() ? (j.equipo == 0 ? "devil_horns-accessory" : "halo-accessory") : "");
            p.setGlowing(mini.equipos());
        }
        if (mini.equipos()) colorear();
        ctx.titulo(Text.literal(mini.nombre().toUpperCase()).formatted(Formatting.GOLD, Formatting.BOLD), Text.literal(mini.regla()).formatted(Formatting.YELLOW), 8, 100, 10);
        avisarTodos(Msg.say(Text.literal("Ronda " + (idx + 1) + ": " + mini.nombre() + ". ").formatted(Formatting.GOLD).append(Text.literal(mini.regla()).formatted(Formatting.WHITE))));
        ctx.sonido(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.8f, 1.0f);
    }

    /** Pone a cada jugador en el color de su equipo (el contorno que brilla y el nombre). */
    private void colorear() {
        net.minecraft.scoreboard.Scoreboard sb = server.getScoreboard();
        String[] nombres = {"emi_fall_rojo", "emi_fall_azul"};
        Formatting[] cols = {Formatting.RED, Formatting.BLUE};
        for (int i = 0; i < 2; i++) {
            net.minecraft.scoreboard.Team tm = sb.getTeam(nombres[i]);
            if (tm == null) tm = sb.addTeam(nombres[i]);
            tm.setColor(cols[i]);
        }
        for (Jug j : jugs.values()) { net.minecraft.scoreboard.Team tm = sb.getTeam(nombres[j.equipo]); if (tm != null) sb.addScoreHolderToTeam(j.nombre, tm); }
    }

    private void quitarColores() {
        net.minecraft.scoreboard.Scoreboard sb = server.getScoreboard();
        for (Jug j : jugs.values()) { net.minecraft.scoreboard.Team tm = sb.getScoreHolderTeam(j.nombre); if (tm != null && tm.getName().startsWith("emi_fall_")) sb.removeScoreHolderFromTeam(j.nombre, tm); }
    }

    ServerPlayerEntity pl(Jug j) { return server.getPlayerManager().getPlayer(j.id); }

    void avisarTodos(Text tx) { for (Jug j : jugs.values()) { ServerPlayerEntity p = pl(j); if (p != null) p.sendMessage(tx, false); } }

    /** Los puntos de un puesto (0 = el primero) entre {@code n} jugadores: n - puesto, mas 3, 2 y 1 de bonus a los tres primeros. */
    static int puntosPuesto(int puesto, int n) { return Math.max(1, n - puesto) + (puesto == 0 ? 3 : puesto == 1 ? 2 : puesto == 2 ? 1 : 0); }

    private void resultado() {
        List<Jug> orden = new ArrayList<>(mini.clasificacion(ctx));
        for (Jug j : jugs.values()) if (!orden.contains(j)) orden.add(j);
        Map<Jug, Integer> extra = mini.extra(ctx);
        int n = orden.size();
        for (int i = 0; i < n; i++) {
            Jug j = orden.get(i);
            j.puestoRonda = i + 1;
            j.puntosRonda = puntosPuesto(i, n) + extra.getOrDefault(j, 0);
            j.puntos += j.puntosRonda;
        }
        fase = Fase.RESULTADO; t = 0;
        // todos vuelven a ser aventureros (los eliminados dejan de ser espectadores) para mirar los resultados
        Text cab = Text.literal("Resultados de " + mini.nombre()).formatted(Formatting.GOLD, Formatting.BOLD);
        avisarTodos(Msg.say(cab));
        for (int i = 0; i < Math.min(5, n); i++) avisarTodos(Text.literal("  " + (i + 1) + ". " + orden.get(i).nombre + " (+" + orden.get(i).puntosRonda + ")").formatted(i == 0 ? Formatting.GOLD : Formatting.WHITE));
        for (Jug j : jugs.values()) {
            ServerPlayerEntity p = pl(j);
            if (p == null) continue;
            Msg.title(p, Text.literal("Puesto " + j.puestoRonda + "º").formatted(j.puestoRonda == 1 ? Formatting.GOLD : Formatting.AQUA, Formatting.BOLD), Text.literal("+" + j.puntosRonda + " puntos · total " + j.puntos).formatted(Formatting.YELLOW), 5, 60, 10);
            Msg.sound(p, j.puestoRonda <= 3 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9f, 1.0f);
        }
        enviarClasificacion();
        EmiFall.LOG.info("[torneo] ronda {} ({}) terminada: {}", idx + 1, mini.id(), resumen(orden));
    }

    private String resumen(List<Jug> orden) { StringBuilder sb = new StringBuilder(); for (Jug j : orden) sb.append(j.nombre).append('=').append(j.puntosRonda).append('(').append(j.puntos).append(") "); return sb.toString(); }

    private List<Jug> clasificacionTotal() {
        List<Jug> l = new ArrayList<>(jugs.values());
        l.sort((a, b) -> a.puntos != b.puntos ? Integer.compare(b.puntos, a.puntos) : Integer.compare(b.puntosRonda, a.puntosRonda));
        return l;
    }

    private void enviarClasificacion() {
        List<Jug> l = clasificacionTotal();
        avisarTodos(Text.literal("Clasificación del torneo:").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
        for (int i = 0; i < l.size(); i++) avisarTodos(Text.literal("  " + (i + 1) + ". " + l.get(i).nombre + " · " + l.get(i).puntos + " puntos").formatted(i == 0 ? Formatting.GOLD : Formatting.GRAY));
    }

    private void final_() {
        fase = Fase.FINAL; t = 0;
        mini = null;
        List<Jug> l = clasificacionTotal();
        Jug g = l.isEmpty() ? null : l.get(0);
        barra.setName(Text.literal("¡Fin del torneo!").formatted(Formatting.GOLD, Formatting.BOLD));
        barra.setPercent(1f);
        if (g != null) {
            avisarTodos(Msg.say(Text.literal("★ ¡" + g.nombre + " ha ganado el torneo con " + g.puntos + " puntos! ★").formatted(Formatting.GOLD, Formatting.BOLD)));
            for (Jug j : jugs.values()) {
                ServerPlayerEntity p = pl(j);
                if (p == null) continue;
                Msg.title(p, Text.literal(g.nombre).formatted(Formatting.GOLD, Formatting.BOLD), Text.literal("¡ES EL CAMPEÓN!").formatted(Formatting.YELLOW), 10, 120, 20);
                Msg.sound(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                p.changeGameMode(GameMode.ADVENTURE);
            }
            EmiFall.LOG.info("[torneo] GANADOR: {} con {} puntos; clasificacion: {}", g.nombre, g.puntos, resumen(l));
        }
        enviarClasificacion();
        // los jugadores esperan en la sala de la ultima ronda (ya no hay nada que hacer): se les lleva a la sala del torneo
        ServerWorld w = Sala.mundo(server);
        if (w != null) for (Jug j : jugs.values()) {
            ServerPlayerEntity p = pl(j);
            if (p == null) continue;
            Morph.quitar(server, p);
            quitarEfectos(p);
            p.changeGameMode(GameMode.ADVENTURE);
            p.teleport(w, Sala.SX, Sala.SY, Sala.SZ + 0.0, 180f, 0f);
        }
    }

    /** Cierra el torneo: todos a la sala (o a donde estaban si ya no estan en ella). */
    void cerrar() {
        barra.clearPlayers();
        if (mini != null && ctx != null) { try { mini.limpiar(ctx); } catch (Throwable e) { EmiFall.LOG.error("Error al limpiar {}", mini.id(), e); } }
        quitarColores();
        ServerWorld w = Sala.mundo(server);
        for (Jug j : jugs.values()) {
            ServerPlayerEntity p = pl(j);
            if (p == null) continue;
            Morph.quitar(server, p);
            quitarEfectos(p);
            p.getInventory().clear();
            p.changeGameMode(GameMode.ADVENTURE);
            if (w != null && !Sala.enSala(p)) p.teleport(w, Sala.SX, Sala.SY, Sala.SZ, 180f, 0f);
        }
        if (actual == this) actual = null;
    }

    // ------------------------------------------------------------------------------------------------ cada tick

    static void tick(MinecraftServer s) {
        Torneo q = actual;
        if (q == null) return;
        try { q.tick(); } catch (Throwable e) { EmiFall.LOG.error("Error en el torneo", e); }
    }

    private void tick() {
        t++;
        switch (fase) {
            case COLA -> { if (++aviso % 600 == 0 && jugs.size() > 0) avisarTodos(Msg.say("Hay " + jugs.size() + " apuntados. El anfitrión puede empezar con /fall empezar.")); }
            case INTRO -> {
                retener();
                if (t >= T_INTRO) { fase = Fase.CUENTA; t = 0; }
            }
            case CUENTA -> {
                retener();
                if (t % 20 == 1 && t < 61) {
                    int n = 3 - t / 20;
                    ctx.titulo(Text.literal(String.valueOf(n)).formatted(n == 1 ? Formatting.GREEN : n == 2 ? Formatting.YELLOW : Formatting.RED, Formatting.BOLD), Text.empty(), 0, 18, 2);
                    ctx.sonido(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 1f, 1.0f);
                }
                if (t >= T_CUENTA) {
                    ctx.retener = false; fase = Fase.JUEGO; t = 0;
                    ctx.titulo(Text.literal("¡YA!").formatted(Formatting.GREEN, Formatting.BOLD), Text.empty(), 0, 16, 6);
                    ctx.sonido(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1f, 1.6f);
                    mini.liberar(ctx);
                }
            }
            case JUEGO -> {
                ctx.tick++;
                if (ctx.tick % 20 == 0) for (Jug j : jugs.values()) { ServerPlayerEntity p = pl(j); if (p != null) p.getHungerManager().setFoodLevel(20); }
                if (ctx.tick % 10 == 0) for (Jug j : jugs.values()) {
                    ServerPlayerEntity p = pl(j);
                    if (p == null) continue;
                    String h = mini.hud(ctx, j);
                    if (!h.isEmpty()) Msg.actionBar(p, Text.literal(h).formatted(Formatting.WHITE, Formatting.BOLD));
                }
                if (mini.tick(ctx)) { resultado(); }
            }
            case RESULTADO -> { if (t >= T_RESULTADO) siguiente(); }
            case FINAL -> { if (t >= T_FINAL) cerrar(); }
        }
    }

    /** Durante la cuenta atras nadie se mueve (solo puede girar la cabeza). */
    private void retener() {
        for (Jug j : jugs.values()) {
            ServerPlayerEntity p = pl(j);
            if (p == null || p.getServerWorld() != ctx.w) continue;
            double dx = p.getX() - j.sx, dz = p.getZ() - j.sz;
            if (dx * dx + dz * dz > 0.09 || p.getY() < j.sy - 3) { p.teleport(ctx.w, j.sx, p.getY() < j.sy - 3 ? j.sy : Math.max(p.getY(), j.sy), j.sz, p.getYaw(), p.getPitch()); p.setVelocity(net.minecraft.util.math.Vec3d.ZERO); }
        }
    }

    /** Para el barra de progreso del tiempo (lo llama el minijuego). */
    void barra(float fraccion) { barra.setPercent(Math.max(0f, Math.min(1f, fraccion))); }
}
