package emi.estructuras;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.Structure;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Coloca todas las estructuras en la dimension plana, una por una, repartiendo el trabajo en ticks.
 * Es reanudable: el progreso se guarda en world/emi_estructuras/progreso.json.
 */
public final class Generador {
    public static final int CELDA = 500;          // separacion entre estructuras (el radio libre de /visitar es 150)
    public static final int COLUMNAS = 10;
    public static final int SUELO = 63;           // y del bloque de cesped
    private static final long PRESUPUESTO_NS = 25_000_000L;   // 25 ms por tick
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final class Hecha {
        public String id; public int x, z; public int minX, minY, minZ, maxX, maxY, maxZ;
        public int entradaX, entradaY, entradaZ; public String clave; public boolean ok; public String error;
    }
    public static final class Progreso { public boolean terminado; public Map<String, Hecha> hechas = new LinkedHashMap<>(); }

    /** Cada mundo plano: el de las estructuras Pokemon (Cobbleverse y Legendary Monuments) y el de Dungeons y jefes. */
    public static final class Mundo {
        public final String nombre;
        public final RegistryKey<World> dim;
        final String archivo;
        public Progreso progreso = new Progreso();
        public List<Entrada> todas = List.of();
        public final List<String> fallos = new ArrayList<>();
        Mundo(String nombre, String dimension, String archivo) {
            this.nombre = nombre; this.dim = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("emi_estructuras", dimension)); this.archivo = archivo;
        }
        public ServerWorld mundo() { return server == null ? null : server.getWorld(dim); }
        public boolean esDungeons() { return this == DUNGEONS; }
    }
    public static final Mundo POKEMON = new Mundo("pokemon", "plano", "progreso.json");
    public static final Mundo DUNGEONS = new Mundo("dungeons", "dungeons", "progreso_dungeons.json");
    public static final List<Mundo> MUNDOS = List.of(POKEMON, DUNGEONS);

    public static Mundo porNombre(String n) { for (Mundo m : MUNDOS) if (m.nombre.equalsIgnoreCase(n)) return m; return null; }
    /** El mundo donde esta hecha una estructura (null si no esta en ninguno). */
    public static Mundo mundoDe(String id) { for (Mundo m : MUNDOS) if (m.progreso.hechas.containsKey(id)) return m; return null; }

    private static final class Tarea {
        final Mundo m; final Entrada e; final boolean limpiar; final boolean fin;
        Tarea(Mundo m, Entrada e, boolean limpiar, boolean fin) { this.m = m; this.e = e; this.limpiar = limpiar; this.fin = fin; }
    }

    private static MinecraftServer server;
    private static final Deque<Tarea> cola = new ArrayDeque<>();
    private static Trabajo actual;
    private static boolean enMarcha;
    private static int esperar;

    public static boolean enMarcha() { return enMarcha; }
    public static String actualNombre() { return actual == null ? "-" : actual.m.nombre + ":" + actual.e.id; }
    public static int pendientes() { int n = actual == null ? 0 : 1; for (Tarea t : cola) if (!t.fin) n++; return n; }

    private static Path archivo(Mundo m) { return server.getSavePath(WorldSavePath.ROOT).resolve("emi_estructuras").resolve(m.archivo); }

    public static void iniciar(MinecraftServer s) {
        server = s;
        for (Mundo m : MUNDOS) {
            try {
                Path f = archivo(m);
                if (Files.exists(f)) {
                    Progreso p = GSON.fromJson(Files.readString(f), Progreso.class);
                    if (p != null) m.progreso = p;
                }
            } catch (Exception e) { EmiEstructuras.LOG.warn("No pude leer el progreso de {}", m.nombre, e); }
        }
    }

    public static void parar() {
        enMarcha = false; cola.clear();
        if (actual != null) { actual.liberar(); actual = null; }
    }

    private static void guardar(Mundo m) {
        try {
            Path f = archivo(m);
            Files.createDirectories(f.getParent());
            Files.writeString(f, GSON.toJson(m.progreso));
        } catch (IOException e) { EmiEstructuras.LOG.warn("No pude guardar el progreso de {}", m.nombre, e); }
    }

    /** Tiene sentido arrancar solo: algun mundo cargado y sin terminar. */
    public static boolean hayPendiente() { for (Mundo m : MUNDOS) if (m.mundo() != null && !m.progreso.terminado) return true; return false; }

    /**
     * Empieza (o reanuda) la generacion de los mundos dados. forzar = rehacer TODO: cada estructura ya hecha se limpia (se vuelve al suelo plano
     * dentro de su zona: lo roto y lo puesto desaparece) y se coloca de nuevo. Devuelve un mensaje de error o null.
     */
    public static String arrancar(List<Mundo> ms, boolean forzar) {
        if (enMarcha) return "Ya se esta generando.";
        StringBuilder avisos = new StringBuilder();
        int total = 0;
        for (Mundo m : ms) {
            if (m.mundo() == null) { avisos.append("La dimension ").append(m.dim.getValue()).append(" no esta cargada. "); continue; }
            if (!forzar && m.progreso.terminado) { avisos.append(m.nombre).append(": ya esta generado (usa forzar para rehacerlo). "); continue; }
            m.todas = Catalogo.construir(server, m);
            if (forzar) m.fallos.clear();
            int n = 0;
            for (Entrada e : m.todas) {
                Hecha h = m.progreso.hechas.get(e.id.toString());
                if (!forzar && h != null && h.ok) continue;
                cola.add(new Tarea(m, e, h != null && tieneZona(h), false)); n++;
            }
            if (n == 0) { m.progreso.terminado = true; guardar(m); avisos.append(m.nombre).append(": no queda nada por generar. "); continue; }
            cola.add(new Tarea(m, null, false, true));
            total += n;
            EmiEstructuras.LOG.info("Generando {} estructuras en {}", n, m.dim.getValue());
        }
        if (total == 0) return avisos.length() == 0 ? "No hay nada que generar." : avisos.toString().trim();
        enMarcha = true;
        return null;
    }

    /** Rehace un mundo entero o una estructura: limpia su zona (lo roto y lo puesto desaparece) y la coloca otra vez. Devuelve un error o null. */
    public static String regenerar(Mundo m, String id) {
        if (enMarcha) return "Ya se esta generando; espera a que termine o usa /emiestructuras parar.";
        if (m.mundo() == null) return "La dimension " + m.dim.getValue() + " no esta cargada.";
        m.todas = Catalogo.construir(server, m);
        int n = 0;
        for (Entrada e : m.todas) {
            if (id != null && !e.id.toString().equals(id)) continue;
            Hecha h = m.progreso.hechas.get(e.id.toString());
            if (h == null && id == null) continue;       // al regenerar el mundo solo se rehace lo que ya existe
            cola.add(new Tarea(m, e, h != null && tieneZona(h), false)); n++;
        }
        if (n == 0) return id == null ? "Todavia no hay nada generado en " + m.nombre + "." : "No conozco la estructura " + id + " en " + m.nombre + ".";
        cola.add(new Tarea(m, null, false, true));
        m.fallos.clear();
        enMarcha = true;
        EmiEstructuras.LOG.info("Regenerando {} estructuras en {}", n, m.dim.getValue());
        return null;
    }

    static boolean tieneZona(Hecha h) { return h.maxX != h.minX || h.maxZ != h.minZ; }

    /** Primera celda de la rejilla que no usa ninguna estructura de ese mundo. */
    static int[] celdaLibre(Mundo m) {
        java.util.Set<Integer> usadas = new java.util.HashSet<>();
        for (Hecha h : m.progreso.hechas.values()) usadas.add(Math.floorDiv(h.z, CELDA) * COLUMNAS + Math.floorDiv(h.x, CELDA));
        int i = 0;
        while (usadas.contains(i)) i++;
        return new int[]{(i % COLUMNAS) * CELDA, (i / COLUMNAS) * CELDA};
    }

    public static void tick() {
        if (!enMarcha) return;
        if (esperar > 0) { esperar--; return; }
        long t0 = System.nanoTime();
        try {
            while (System.nanoTime() - t0 < PRESUPUESTO_NS) {
                if (actual == null) {
                    Tarea t = cola.poll();
                    if (t == null) { enMarcha = false; return; }
                    if (t.fin) { terminar(t.m); continue; }
                    actual = new Trabajo(t);
                }
                if (actual.paso(actual.m.mundo(), t0)) { actual = null; esperar = 10; break; }
                if (actual.esperando()) break;
            }
        } catch (Throwable t) {
            EmiEstructuras.LOG.error("Fallo generando {}", actual == null ? "?" : actual.e.id, t);
            if (actual != null) { actual.fallo(t); actual.liberar(); actual = null; }
            esperar = 20;
        }
    }

    private static void terminar(Mundo m) {
        boolean ok = true, intentadas = true;
        for (Entrada e : m.todas) {
            Hecha h = m.progreso.hechas.get(e.id.toString());
            if (h == null || !h.ok) ok = false;
            if (h == null) intentadas = false;
        }
        if (!m.esDungeons() && Catalogo.saltadas > 0) {
            ok = false;
            m.fallos.add(Catalogo.saltadas + " estructuras de las 69 no estan registradas (activa los datapacks Johto/Hoenn/Sinnoh y Terralith ANTES de arrancar el servidor)");
        }
        // en Dungeons una estructura que no se pueda generar no impide darlo por terminado (queda en los fallos y se reintenta con /emiestructuras generar dungeons)
        final boolean todoOk = ok;
        m.progreso.terminado = m.esDungeons() ? intentadas : ok;
        guardar(m);
        String msg = todoOk ? "Listas todas las estructuras de " + m.nombre + " (" + m.todas.size() + ")."
                : "Terminado " + m.nombre + " con fallos: " + m.fallos.size() + ". Mira /emiestructuras estado y el log.";
        EmiEstructuras.LOG.info(msg);
        server.getPlayerManager().getPlayerList().stream().filter(p -> p.hasPermissionLevel(2))
                .forEach(p -> p.sendMessage(Text.literal("[Emi Estructuras] " + msg).formatted(todoOk ? Formatting.GREEN : Formatting.GOLD), false));
    }

    // ------------------------------------------------------------------ un trabajo = una estructura

    private static final class Trabajo {
        final Mundo m;
        final Entrada e;
        final Hecha vieja;                 // lo que habia antes (para limpiar su zona) o null
        final boolean limpiar;
        final int cx, cz;                  // centro de su celda
        int fase = 7;
        StructureStart inicio; StructureTemplate plantilla; BlockBox bb; BlockPos origenPlantilla;
        List<ChunkPos> chunks = new ArrayList<>(); int ci;
        List<ChunkPos> forzados = new ArrayList<>();
        int px, pz;                         // cursor de la preparacion
        BlockBox balsa;
        boolean enterrada, flotante;
        int entX, entY, entZ;               // donde aparece quien visita
        int zc;                             // z del centro
        int esperaAsentar = 0;
        int sx, sz, sy;                     // cursor del barrido de fugas
        int fugasQuitadas;
        int pasoExtra = 0;                  // sub-fase de la preparacion (0 balsa/pozo, 1 rampa, 2 pilar)
        // limpieza previa
        int lx0, lx1, lz0, lz1, lci;        // zona (bloques) y cursor de chunks
        List<ChunkPos> zona = new ArrayList<>();
        int limpiados;

        Trabajo(Tarea t) {
            this.m = t.m; this.e = t.e; this.limpiar = t.limpiar;
            Hecha h = m.progreso.hechas.get(e.id.toString());
            this.vieja = h;
            if (h != null) { this.cx = h.x; this.cz = h.z; }       // la misma celda de siempre
            else { int[] c = celdaLibre(m); this.cx = c[0]; this.cz = c[1]; }
        }

        void fallo(Throwable t) {
            m.fallos.add(e.id + ": " + t);
            Hecha h = vieja != null ? vieja : new Hecha();
            h.id = e.id.toString(); h.x = cx; h.z = cz; h.clave = e.clave; h.ok = false; h.error = String.valueOf(t);
            m.progreso.hechas.put(h.id, h);
            guardar(m);
        }

        void liberar() {
            ServerWorld w = m.mundo();
            if (w != null) for (ChunkPos c : forzados) w.setChunkForced(c.x, c.z, false);
            forzados.clear();
        }

        void asentar() { esperaAsentar = (int) server.getTicks() + 80; sx = bb.getMinX() - 40; sz = bb.getMinZ() - 40; sy = SUELO + 1; }

        /**
         * Estructuras totalmente enterradas cuya sala queda cerrada bajo piedra (p. ej. el gimnasio de Giovanni): el pozo y la rampa
         * llegan solo hasta el techo de la caja, asi que se excava un tunel que sigue la pendiente de la rampa hacia dentro y luego
         * sigue recto hasta la sala. Si la sala ya esta abierta por arriba (santuarios) no hace nada.
         */
        void abrirAcceso(ServerWorld w) {
            BlockPos.Mutable m = new BlockPos.Mutable();
            int tope = enterrada ? bb.getMaxY() : SUELO - 1;     // una estructura "sellada" tiene su salon bajo el suelo aunque la caja salga por arriba
            int techo = Integer.MIN_VALUE;
            for (int y = tope; y >= bb.getMinY() && techo == Integer.MIN_VALUE; y--)
                for (int x = bb.getMinX(); x <= bb.getMaxX() && techo == Integer.MIN_VALUE; x++)
                    for (int z = bb.getMinZ(); z <= bb.getMaxZ(); z++)
                        if (w.getBlockState(m.set(x, y, z)).isAir()) { techo = y; break; }
            if (techo == Integer.MIN_VALUE || (enterrada && tope - techo <= 2)) return;
            // capa con mas espacio libre (en las mazmorras es donde estan las salas grandes; un hueco suelto no vale)
            int ty = techo, mejor = -1;
            for (int y = techo; y >= Math.max(bb.getMinY() + 1, techo - 24); y--) {
                int n = 0;
                for (int x = bb.getMinX(); x <= bb.getMaxX(); x++)
                    for (int z = bb.getMinZ(); z <= bb.getMaxZ(); z++)
                        if (w.getBlockState(m.set(x, y, z)).isAir() && w.getBlockState(m.set(x, y + 1, z)).isAir()) n++;
                if (n > mejor) { mejor = n; ty = y; }
            }
            int tx = Integer.MAX_VALUE, tz = zc;
            for (int x = bb.getMinX(); x <= bb.getMaxX() && tx == Integer.MAX_VALUE; x++)
                for (int z = bb.getMinZ(); z <= bb.getMaxZ(); z++)
                    if (w.getBlockState(m.set(x, ty, z)).isAir() && w.getBlockState(m.set(x, ty + 1, z)).isAir()) {
                        if (tx == Integer.MAX_VALUE || Math.abs(z - zc) < Math.abs(tz - zc)) { tx = x; tz = z; }
                    }
            if (tx == Integer.MAX_VALUE) return;
            int flags = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;
            BlockState aire = Blocks.AIR.getDefaultState(), ladrillo = Blocks.STONE_BRICKS.getDefaultState();
            BlockState escalon = Blocks.STONE_BRICK_STAIRS.getDefaultState().with(net.minecraft.block.StairsBlock.FACING, net.minecraft.util.math.Direction.WEST);
            int x = bb.getMinX() - 7, piso = tope + 1;
            while (x <= tx || (piso > ty && x <= tx + 60)) {   // pendiente: baja 1 bloque por bloque; si la sala queda cerca sigue bajando por dentro
                boolean cae = piso > ty;
                for (int zz = zc - 2; zz <= zc + 2; zz++) {
                    for (int y = piso + 1; y <= piso + 4; y++) w.setBlockState(m.set(x, y, zz), aire, flags);
                    w.setBlockState(m.set(x, piso, zz), cae ? escalon : ladrillo, flags);
                }
                if (cae) piso--;
                x++;
            }
            for (int xx = Math.max(bb.getMinX(), tx - 1); xx <= tx; xx++)   // por si la pendiente no llego a bajar del todo
                for (int zz = zc - 2; zz <= zc + 2; zz++) for (int y = ty; y <= ty + 3; y++) w.setBlockState(m.set(xx, y, zz), aire, flags);
            for (int zz = Math.min(zc, tz); zz <= Math.max(zc, tz); zz++)   // y de lado hasta la z de la sala
                for (int xx = tx - 2; xx <= tx + 2; xx++) for (int y = ty; y <= ty + 3; y++) w.setBlockState(m.set(xx, y, zz), aire, flags);
            for (int zz = Math.min(zc, tz) - 2; zz <= Math.max(zc, tz) + 2; zz++)   // suelo bajo el tramo final (si era hueco de la sala)
                for (int xx = tx - 2; xx <= tx + 2; xx++) if (w.getBlockState(m.set(xx, ty - 1, zz)).isAir()) w.setBlockState(m, ladrillo, flags);
            // dentro de la sala sigue bajando con escalones hasta pisar suelo firme (las salas altas, como los santuarios, tienen el suelo mucho mas abajo)
            int xs = tx + 3, ps = ty, caida = 0;
            while (caida < 4 && w.getBlockState(m.set(xs, ty - 1 - caida, tz)).isAir()) caida++;
            boolean entro = false;
            for (int k = 1; caida >= 4 && k <= 40; k++) {     // si el suelo esta a menos de 4 bloques no hace falta
                int px2 = xs + k - 1, py = ps - 1 - k;
                boolean eraAire = w.getBlockState(m.set(px2, py, tz)).isAir();
                if (!eraAire && (entro || k >= 3)) break;     // salimos de la sala: no seguir picando piedra
                entro |= eraAire;
                for (int zz = tz - 2; zz <= tz + 2; zz++) {
                    for (int y = py + 1; y <= py + 4; y++) w.setBlockState(m.set(px2, y, zz), aire, flags);
                    w.setBlockState(m.set(px2, py, zz), escalon, flags);
                }
                if (eraAire && !w.getBlockState(m.set(px2, py - 1, tz)).isAir()) break;
            }
            EmiEstructuras.LOG.info("{}: acceso excavado hasta la sala en x={} y={} z={} (techo de la sala y={})", e.id, tx, ty, tz, techo);
        }


        // ------------------------------------------------------------ limpieza previa (regenerar)

        static final BlockState BEDROCK = Blocks.BEDROCK.getDefaultState(), PIEDRA = Blocks.STONE.getDefaultState(),
                TIERRA = Blocks.DIRT.getDefaultState(), CESPED = Blocks.GRASS_BLOCK.getDefaultState(), AIRE = Blocks.AIR.getDefaultState();

        /** El bloque que hay en el mundo plano limpio (las capas de la dimension): bedrock, 124 de piedra, 2 de tierra y cesped en y=63. */
        static BlockState base(int y, int fondo) {
            if (y == fondo) return BEDROCK;
            if (y <= SUELO - 3) return PIEDRA;
            if (y <= SUELO - 1) return TIERRA;
            if (y == SUELO) return CESPED;
            return AIRE;
        }

        /**
         * Vuelve al suelo plano toda la zona de la estructura anterior (su caja, los 40 bloques de margen, la rampa o el pilar de la entrada):
         * lo roto y lo puesto desaparece, y tambien las entidades sueltas. Las secciones que ya son exactamente el suelo plano se saltan
         * comparando solo cuantos bloques de cada tipo tienen, asi que es rapido salvo donde hay algo que borrar.
         */
        boolean limpiarZona(ServerWorld w, long t0) {
            if (!limpiar || vieja == null || !tieneZona(vieja)) return true;
            if (zona.isEmpty()) {
                lx0 = Math.min(vieja.minX - 40, vieja.entradaX - 12); lx1 = vieja.maxX + 40;
                lz0 = vieja.minZ - 40; lz1 = vieja.maxZ + 40;
                for (int x = lx0 >> 4; x <= lx1 >> 4; x++) for (int z = lz0 >> 4; z <= lz1 >> 4; z++) zona.add(new ChunkPos(x, z));
                for (ChunkPos c : zona) { w.setChunkForced(c.x, c.z, true); forzados.add(c); }
                // quien este dentro sale al mundo normal (no se le puede dejar dentro de lo que se va a borrar)
                net.minecraft.util.math.Box caja = new net.minecraft.util.math.Box(lx0, w.getBottomY(), lz0, lx1 + 1, w.getTopY(), lz1 + 1);
                for (net.minecraft.server.network.ServerPlayerEntity p : new ArrayList<>(w.getPlayers())) {
                    if (!caja.contains(p.getX(), p.getY(), p.getZ())) continue;
                    // primero se cierra su visita (si no, Emipokemon lo traeria de vuelta aqui al instante) y vuelve a donde estaba
                    try { server.getCommandManager().executeWithPrefix(p.getCommandSource().withLevel(2), "emipokemon visitar volver"); } catch (Throwable t) { EmiEstructuras.LOG.warn("visitar volver", t); }
                    if (p.getServerWorld() == w && caja.contains(p.getX(), p.getY(), p.getZ())) {
                        ServerWorld ow = server.getOverworld();
                        BlockPos sp = ow.getSpawnPos();
                        p.teleport(ow, sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5, 0f, 0f);
                    }
                    p.sendMessage(Text.literal("[Emi Estructuras] Se esta regenerando esta zona; te saque al mundo normal.").formatted(Formatting.GOLD), false);
                }
            }
            BlockPos.Mutable mp = new BlockPos.Mutable();
            int flags = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;
            int fondo = w.getBottomY();
            while (lci < zona.size()) {
                ChunkPos c = zona.get(lci);
                net.minecraft.world.chunk.Chunk ch = w.getChunk(c.x, c.z);
                net.minecraft.world.chunk.ChunkSection[] secs = ch.getSectionArray();
                for (int i = 0; i < secs.length; i++) {
                    int y0 = fondo + i * 16;
                    if (!seccionDistinta(secs[i], y0, fondo)) continue;
                    int xa = Math.max(c.getStartX(), lx0), xb = Math.min(c.getEndX(), lx1), za = Math.max(c.getStartZ(), lz0), zb = Math.min(c.getEndZ(), lz1);
                    for (int y = y0; y < y0 + 16; y++) {
                        BlockState esperado = base(y, fondo);
                        for (int x = xa; x <= xb; x++) for (int z = za; z <= zb; z++) {
                            mp.set(x, y, z);
                            if (ch.getBlockState(mp) != esperado) { w.setBlockState(mp, esperado, flags); limpiados++; }
                        }
                    }
                }
                lci++;
                if (agotado(t0)) return lci >= zona.size() && descartarEntidades(w);
            }
            return descartarEntidades(w);
        }

        boolean entidadesHechas;
        boolean descartarEntidades(ServerWorld w) {
            if (entidadesHechas) return true;
            entidadesHechas = true;
            net.minecraft.util.math.Box caja = new net.minecraft.util.math.Box(lx0, w.getBottomY(), lz0, lx1 + 1, w.getTopY(), lz1 + 1);
            int n = 0;
            for (net.minecraft.entity.Entity en : w.getEntitiesByClass(net.minecraft.entity.Entity.class, caja, en -> !(en instanceof net.minecraft.entity.player.PlayerEntity))) { en.discard(); n++; }
            EmiEstructuras.LOG.info("{}: zona limpiada ({} bloques cambiados, {} entidades borradas)", e.id, limpiados, n);
            return true;
        }

        /** true si la seccion NO es justo el suelo plano (comparando cuantos bloques hay de cada tipo). */
        static boolean seccionDistinta(net.minecraft.world.chunk.ChunkSection sec, int y0, int fondo) {
            Map<BlockState, Integer> hay = new java.util.HashMap<>();
            sec.getBlockStateContainer().count((st, n) -> hay.merge(st, n, Integer::sum));
            Map<BlockState, Integer> debe = new java.util.HashMap<>();
            for (int y = y0; y < y0 + 16; y++) debe.merge(base(y, fondo), 256, Integer::sum);
            return !hay.equals(debe);
        }


        /**
         * true si la estructura tiene una cavidad grande bajo el suelo a la que NO se llega andando desde la entrada (p. ej. la caverna del Void Blossom,
         * enterrada bajo 30 bloques de piedra aunque su caja sobresalga por arriba). Recorre el aire desde la entrada y mira si toca algun hueco profundo.
         */
        boolean sellada(ServerWorld w) {
            if (enterrada || bb.getMinY() >= SUELO - 10) return false;
            int x0 = Math.min(bb.getMinX(), entX) - 2, x1 = bb.getMaxX() + 2, z0 = bb.getMinZ() - 2, z1 = bb.getMaxZ() + 2;
            int y0 = Math.max(w.getBottomY(), bb.getMinY() - 1), y1 = Math.min(w.getTopY() - 1, SUELO + 24);
            int ax = x1 - x0 + 1, az = z1 - z0 + 1, ay = y1 - y0 + 1;
            long total = (long) ax * az * ay;
            if (total > 60_000_000L) return false;
            BlockPos.Mutable m = new BlockPos.Mutable();
            int hondo = 0;
            for (int x = bb.getMinX(); x <= bb.getMaxX(); x++) for (int z = bb.getMinZ(); z <= bb.getMaxZ(); z++)
                for (int y = Math.max(y0, bb.getMinY()); y <= SUELO - 6 && y <= bb.getMaxY(); y++) if (w.getBlockState(m.set(x, y, z)).isAir()) hondo++;
            if (hondo < 1000) return false;
            java.util.BitSet vistos = new java.util.BitSet((int) total);
            int[] pila = new int[1 << 16]; int n = 0;
            int ini = ((entX - x0) * az + (zc - z0)) * ay + (SUELO + 1 - y0);
            pila[n++] = ini; vistos.set(ini);
            int[] d = {1, -1, 0, 0, 0, 0, 0, 0, 1, -1, 0, 0, 0, 0, 0, 0, 1, -1};   // x, y, z
            while (n > 0) {
                int c = pila[--n];
                int y = c % ay, z = (c / ay) % az + z0, x = c / ay / az + x0; y += y0;
                if (x >= bb.getMinX() && x <= bb.getMaxX() && z >= bb.getMinZ() && z <= bb.getMaxZ() && y <= SUELO - 6 && y >= bb.getMinY()) return false;   // llega a un hueco profundo: no esta sellada
                for (int k = 0; k < 6; k++) {
                    int nx = x + (k < 2 ? d[k] : 0), ny = y + (k >= 2 && k < 4 ? d[k - 2] : 0), nz = z + (k >= 4 ? d[k - 4] : 0);
                    if (nx < x0 || nx > x1 || nz < z0 || nz > z1 || ny < y0 || ny > y1) continue;
                    int ci = ((nx - x0) * az + (nz - z0)) * ay + (ny - y0);
                    if (vistos.get(ci)) continue;
                    vistos.set(ci);
                    if (!w.getBlockState(m.set(nx, ny, nz)).isAir()) continue;
                    if (n == pila.length) pila = java.util.Arrays.copyOf(pila, n * 2);
                    pila[n++] = ci;
                }
            }
            EmiEstructuras.LOG.info("{}: sellada bajo el suelo ({} bloques de aire hondo sin acceso); se le abre un tunel", e.id, hondo);
            return true;
        }

        boolean esperando() { return fase == 4 && server.getTicks() < esperaAsentar; }

        boolean agotado(long t0) { return System.nanoTime() - t0 >= PRESUPUESTO_NS; }

        boolean paso(ServerWorld w, long t0) {
            switch (fase) {
                case 7 -> { if (!limpiarZona(w, t0)) return false; fase = 0; }
                case 0 -> inicio(w);
                case 1 -> { if (!preparar(w, t0)) return false; fase = 2; ci = 0; }
                case 2 -> { if (!cargar(w, t0)) return false; fase = 3; ci = 0; }
                case 3 -> { if (!colocar(w, t0)) return false; fase = (enterrada || sellada(w)) ? 6 : 4; if (fase == 4) asentar(); }
                case 6 -> { abrirAcceso(w); fase = 4; asentar(); }
                case 4 -> { if (server.getTicks() < esperaAsentar) return false; if (!fugas(w, t0)) return false; fase = 5; }
                case 5 -> { fin(w); return true; }
                default -> { return true; }
            }
            return false;
        }

        void inicio(ServerWorld w) {
            BlockPos centro = new BlockPos(cx, SUELO + 1, cz);
            if (e.plantilla) {
                Optional<StructureTemplate> t = w.getStructureTemplateManager().getTemplate(e.id);
                if (t.isEmpty()) throw new IllegalStateException("No existe la plantilla " + e.id);
                plantilla = t.get();
                var sz = plantilla.getSize();
                int y = e.oceano ? 43 : SUELO + 1;
                origenPlantilla = new BlockPos(cx - sz.getX() / 2, y, cz - sz.getZ() / 2);
                bb = new BlockBox(origenPlantilla.getX(), y, origenPlantilla.getZ(), origenPlantilla.getX() + sz.getX() - 1, y + sz.getY() - 1, origenPlantilla.getZ() + sz.getZ() - 1);
            } else {
                Registry<Structure> reg = w.getRegistryManager().get(RegistryKeys.STRUCTURE);
                Structure s = reg.get(e.id);
                if (s == null) throw new IllegalStateException("Estructura no registrada: " + e.id);
                ChunkGenerator gen = w.getChunkManager().getChunkGenerator();
                inicio = s.createStructureStart(w.getRegistryManager(), gen, gen.getBiomeSource(), w.getChunkManager().getNoiseConfig(),
                        w.getStructureTemplateManager(), w.getSeed(), new ChunkPos(centro), 0, w, b -> true);
                if (!inicio.hasChildren()) throw new IllegalStateException("La estructura " + e.id + " no genera piezas aqui");
                bb = inicio.getBoundingBox();
            }
            zc = (bb.getMinZ() + bb.getMaxZ()) / 2;
            enterrada = bb.getMaxY() < SUELO - 2;
            flotante = bb.getMinY() > SUELO + 12;
            int margen = e.oceano ? 28 : (enterrada ? 8 : 3);
            if (enterrada) {
                int prof = SUELO - bb.getMaxY();
                entX = bb.getMinX() - 7 - (prof - 1) - 4; entY = SUELO + 1; entZ = zc;
                margen = Math.max(margen, prof + 14);          // para cargar tambien la rampa
            } else if (flotante) {
                entX = bb.getMinX() - 8; entY = SUELO + 1; entZ = zc;   // la y real la da la plataforma
            } else if (e.oceano) {
                entX = bb.getMinX() - 32; entY = SUELO + 1; entZ = zc;
            } else {
                entX = bb.getMinX() - 10; entY = SUELO + 1; entZ = zc;
            }
            if (e.oceano) balsa = new BlockBox(bb.getMinX() - margen, Math.min(bb.getMinY(), SUELO), bb.getMinZ() - margen, bb.getMaxX() + margen, SUELO, bb.getMaxZ() + margen);
            int x0 = (Math.min(bb.getMinX() - margen, entX - 6)) >> 4, x1 = (bb.getMaxX() + margen) >> 4, z0 = (bb.getMinZ() - margen) >> 4, z1 = (bb.getMaxZ() + margen) >> 4;
            for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) { chunks.add(new ChunkPos(x, z)); }
            for (ChunkPos c : chunks) { w.setChunkForced(c.x, c.z, true); forzados.add(c); }
            px = balsa == null ? 0 : balsa.getMinX(); pz = balsa == null ? 0 : balsa.getMinZ();
            fase = 1;
            EmiEstructuras.LOG.info("[{}] {} bb=({},{},{})-({},{},{}) {}", m.nombre + "/" + (cx / CELDA + cz / CELDA * COLUMNAS + 1), e.id, bb.getMinX(), bb.getMinY(), bb.getMinZ(), bb.getMaxX(), bb.getMaxY(), bb.getMaxZ(), e.oceano ? "OCEANO" : enterrada ? "ENTERRADA" : flotante ? "FLOTANTE" : "");
        }

        /** Prepara el terreno: balsa de agua (oceano), pozo y rampa (enterrada) o pilar con plataforma (flotante). */
        boolean preparar(ServerWorld w, long t0) {
            int flags = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;
            BlockPos.Mutable m = new BlockPos.Mutable();
            if (balsa != null) {
                // las celdas estan lejos de cualquier jugador: hay que tener los chunks cargados antes de tocar bloques
                if (pasoExtra == 0) { for (ChunkPos c : chunks) w.getChunk(c.x, c.z); pasoExtra = 1; }
                BlockState agua = Blocks.WATER.getDefaultState(), aire = Blocks.AIR.getDefaultState();
                while (px <= balsa.getMaxX()) {
                    for (int z = balsa.getMinZ(); z <= balsa.getMaxZ(); z++) {
                        for (int y = balsa.getMinY(); y <= SUELO; y++) w.setBlockState(m.set(px, y, z), y <= SUELO - 1 ? agua : aire, flags);
                    }
                    px++;
                    if (agotado(t0)) return false;
                }
                return true;
            }
            if (enterrada) return excavar(w, t0, m, flags);
            if (flotante) return pilar(w, t0, m, flags);
            return true;
        }

        int ex, eprog;     // cursores de excavar
        boolean excavar(ServerWorld w, long t0, BlockPos.Mutable m, int flags) {
            if (pasoExtra == 0) { for (ChunkPos c : chunks) w.getChunk(c.x, c.z); pasoExtra = 1; ex = bb.getMinX() - 6; }
            BlockState aire = Blocks.AIR.getDefaultState();
            int tope = bb.getMaxY();
            if (pasoExtra == 1) {   // pozo sobre la estructura
                while (ex <= bb.getMaxX() + 6) {
                    for (int z = bb.getMinZ() - 6; z <= bb.getMaxZ() + 6; z++) for (int y = tope + 1; y <= SUELO; y++) w.setBlockState(m.set(ex, y, z), aire, flags);
                    ex++;
                    if (agotado(t0)) return false;
                }
                pasoExtra = 2; eprog = 0;
            }
            // rampa hacia el oeste: sube un bloque por cada bloque (escalones de ladrillo), 5 de ancho
            BlockState escalon = Blocks.STONE_BRICK_STAIRS.getDefaultState().with(net.minecraft.block.StairsBlock.FACING, net.minecraft.util.math.Direction.WEST);
            int prof = SUELO - tope;
            while (eprog < prof + 2) {
                int x = bb.getMinX() - 7 - eprog;
                int piso = tope + 1 + eprog;
                for (int z = zc - 2; z <= zc + 2; z++) {
                    for (int y = piso + 1; y <= SUELO + 1; y++) w.setBlockState(m.set(x, y, z), aire, flags);
                    if (piso <= SUELO) w.setBlockState(m.set(x, piso, z), escalon, flags);
                }
                eprog++;
                if (agotado(t0)) return false;
            }
            return true;
        }

        boolean pilar(ServerWorld w, long t0, BlockPos.Mutable m, int flags) {
            for (ChunkPos c : chunks) w.getChunk(c.x, c.z);
            int yPlat = bb.getMinY() + (bb.getMaxY() - bb.getMinY()) / 2;
            BlockState ladrillo = Blocks.STONE_BRICKS.getDefaultState();
            for (int y = SUELO; y < yPlat; y++) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) w.setBlockState(m.set(entX + dx, y, zc + dz), ladrillo, flags);
            for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) w.setBlockState(m.set(entX + dx, yPlat, zc + dz), ladrillo, flags);
            entY = yPlat + 1;
            return true;
        }

        boolean cargar(ServerWorld w, long t0) {
            while (ci < chunks.size()) {
                ChunkPos c = chunks.get(ci++);
                w.getChunk(c.x, c.z);
                if (agotado(t0)) return ci >= chunks.size();
            }
            return true;
        }

        boolean colocar(ServerWorld w, long t0) {
            if (plantilla != null) {
                if (ci > 0) return true;
                StructurePlacementData d = new StructurePlacementData();
                plantilla.place(w, origenPlantilla, origenPlantilla, d, w.getRandom(), Block.NOTIFY_LISTENERS);
                ci = 1;
                return true;
            }
            ChunkGenerator gen = w.getChunkManager().getChunkGenerator();
            int x0 = bb.getMinX() >> 4, x1 = bb.getMaxX() >> 4, z0 = bb.getMinZ() >> 4, z1 = bb.getMaxZ() >> 4;
            int ancho = z1 - z0 + 1, total = (x1 - x0 + 1) * ancho;
            while (ci < total) {
                int cxk = x0 + ci / ancho, czk = z0 + ci % ancho;
                ChunkPos cp = new ChunkPos(cxk, czk);
                inicio.place(w, w.getStructureAccessor(), gen, w.getRandom(),
                        new BlockBox(cp.getStartX(), w.getBottomY(), cp.getStartZ(), cp.getEndX(), w.getTopY(), cp.getEndZ()), cp);
                ci++;
                if (agotado(t0)) return ci >= total;
            }
            return true;
        }

        /** Despues de 5 s con los chunks cargados (los liquidos ya han fluido), borra los liquidos que se hayan escapado por encima del suelo fuera de la estructura. */
        boolean fugas(ServerWorld w, long t0) {
            BlockPos.Mutable m = new BlockPos.Mutable();
            int x1 = bb.getMaxX() + 40, z1 = bb.getMaxZ() + 40;
            int yTope = Math.min(SUELO + 8, w.getTopY() - 1);
            int flags = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;
            while (sx <= x1) {
                for (; sz <= z1; sz++) {
                    boolean dentro = sx >= bb.getMinX() && sx <= bb.getMaxX() && sz >= bb.getMinZ() && sz <= bb.getMaxZ();
                    if (dentro) continue;
                    if (!w.isChunkLoaded(sx >> 4, sz >> 4)) continue;
                    for (int y = SUELO + 1; y <= yTope; y++) {
                        if (!w.getFluidState(m.set(sx, y, sz)).isEmpty()) { w.setBlockState(m, Blocks.AIR.getDefaultState(), flags); fugasQuitadas++; }
                    }
                }
                sz = bb.getMinZ() - 40; sx++;
                if (agotado(t0)) return false;
            }
            if (fugasQuitadas > 0) EmiEstructuras.LOG.warn("{}: quite {} bloques de liquido que se escapaban", e.id, fugasQuitadas);
            return true;
        }

        void fin(ServerWorld w) {
            Hecha h = new Hecha();
            h.id = e.id.toString(); h.x = cx; h.z = cz; h.clave = e.clave; h.ok = true;
            h.minX = bb.getMinX(); h.minY = bb.getMinY(); h.minZ = bb.getMinZ(); h.maxX = bb.getMaxX(); h.maxY = bb.getMaxY(); h.maxZ = bb.getMaxZ();
            h.entradaX = entX; h.entradaZ = entZ; h.entradaY = entY;
            m.progreso.hechas.put(h.id, h);
            guardar(m);
            registrar(m, h);
            liberar();
        }
    }

    /**
     * Vuelve a decirle a Emipokemon donde esta cada estructura ya generada, con la clave que le toca ahora en el menu (las ubicaciones
     * extra de extra-locations.json). Sirve para mundos que se generaron antes de que esas ubicaciones existieran: no coloca nada.
     */
    public static int sincronizar() {
        if (server == null) return 0;
        int n = 0;
        for (Mundo m : MUNDOS) {
            if (m.mundo() == null) continue;
            List<Entrada> catalogo = Catalogo.construir(server, m);
            boolean cambio = false;
            for (Entrada e : catalogo) {
                Hecha h = m.progreso.hechas.get(e.id.toString());
                if (h == null || !h.ok || e.clave == null) continue;
                if (!e.clave.equals(h.clave)) { h.clave = e.clave; cambio = true; }
                registrar(m, h);
                n++;
            }
            if (cambio) guardar(m);
        }
        return n;
    }

    /** Le dice a Emipokemon que esta ubicacion esta aqui, para que /emipokemon visitar mande a la dimension de ese mundo. */
    public static void registrar(Mundo m, Hecha h) {
        if (h.clave == null) return;
        try {
            Class<?> c = Class.forName("com.emipokemon.admin.ImportantLocationService");
            Method me = c.getMethod("recordGeneratedLocation", MinecraftServer.class, String.class, String.class, RegistryKey.class, BlockPos.class);
            Object r = me.invoke(null, server, h.clave, h.id, m.dim, new BlockPos(h.entradaX, h.entradaY, h.entradaZ));
            if (!Boolean.TRUE.equals(r)) EmiEstructuras.LOG.warn("Emipokemon no acepto la ubicacion {} ({})", h.clave, h.id);
        } catch (ClassNotFoundException ex) {
            EmiEstructuras.LOG.warn("Emipokemon no esta instalado: no se registran las ubicaciones de /visitar");
        } catch (Throwable t) {
            EmiEstructuras.LOG.warn("No pude registrar {} en Emipokemon", h.clave, t);
        }
    }
}
