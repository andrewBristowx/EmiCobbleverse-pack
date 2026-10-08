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
    public static final RegistryKey<World> PLANO = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("emi_estructuras", "plano"));
    public static final int CELDA = 500;          // separacion entre estructuras (el radio libre de /visitar es 150)
    public static final int COLUMNAS = 10;
    public static final int SUELO = 63;           // y del bloque de cesped
    private static final long PRESUPUESTO_NS = 25_000_000L;   // 25 ms por tick
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final class Hecha {
        public String id; public int x, z; public int minX, minY, minZ, maxX, maxY, maxZ;
        public int entradaX, entradaY, entradaZ; public String clave; public boolean ok;
    }
    public static final class Progreso { public boolean terminado; public Map<String, Hecha> hechas = new LinkedHashMap<>(); }

    private static MinecraftServer server;
    private static Progreso progreso = new Progreso();
    private static List<Entrada> todas = List.of();
    private static final Deque<Entrada> pendientes = new ArrayDeque<>();
    private static Trabajo actual;
    private static boolean enMarcha;
    private static int esperar;
    private static final List<String> fallos = new ArrayList<>();

    public static boolean enMarcha() { return enMarcha; }
    public static Progreso progreso() { return progreso; }
    public static List<Entrada> todas() { return todas; }
    public static String actualNombre() { return actual == null ? "-" : actual.e.id.toString(); }
    public static int pendientes() { return pendientes.size() + (actual == null ? 0 : 1); }
    public static List<String> fallos() { return fallos; }
    public static ServerWorld mundo() { return server == null ? null : server.getWorld(PLANO); }

    private static Path archivo() { return server.getSavePath(WorldSavePath.ROOT).resolve("emi_estructuras").resolve("progreso.json"); }

    public static void iniciar(MinecraftServer s) {
        server = s;
        try {
            Path f = archivo();
            if (Files.exists(f)) {
                Progreso p = GSON.fromJson(Files.readString(f), Progreso.class);
                if (p != null) progreso = p;
            }
        } catch (Exception e) { EmiEstructuras.LOG.warn("No pude leer el progreso", e); }
    }

    public static void parar() {
        enMarcha = false; pendientes.clear();
        if (actual != null) { actual.liberar(); actual = null; }
    }

    private static void guardar() {
        try {
            Path f = archivo();
            Files.createDirectories(f.getParent());
            Files.writeString(f, GSON.toJson(progreso));
        } catch (IOException e) { EmiEstructuras.LOG.warn("No pude guardar el progreso", e); }
    }

    /** Empieza (o reanuda) la generacion. forzar = rehacerlo todo. Devuelve un mensaje de error o null. */
    public static String arrancar(boolean forzar) {
        if (enMarcha) return "Ya se esta generando.";
        if (mundo() == null) return "La dimension emi_estructuras:plano no esta cargada.";
        if (forzar) { progreso = new Progreso(); fallos.clear(); }
        else if (progreso.terminado) return "Ya esta generado (usa /emiestructuras generar forzar para rehacerlo).";
        todas = Catalogo.construir(server);
        pendientes.clear();
        for (int i = 0; i < todas.size(); i++) {
            Entrada e = todas.get(i);
            Hecha h = progreso.hechas.get(e.id.toString());
            if (h != null && h.ok) continue;
            pendientes.add(e);
        }
        if (pendientes.isEmpty()) { progreso.terminado = true; guardar(); return "No queda nada por generar."; }
        enMarcha = true;
        EmiEstructuras.LOG.info("Generando {} estructuras en emi_estructuras:plano", pendientes.size());
        return null;
    }

    public static int indice(Entrada e) {
        for (int i = 0; i < todas.size(); i++) if (todas.get(i) == e || todas.get(i).id.equals(e.id)) return i;
        return 0;
    }

    public static void tick() {
        if (!enMarcha) return;
        if (esperar > 0) { esperar--; return; }
        long t0 = System.nanoTime();
        try {
            while (System.nanoTime() - t0 < PRESUPUESTO_NS) {
                if (actual == null) {
                    Entrada e = pendientes.poll();
                    if (e == null) { terminar(); return; }
                    actual = new Trabajo(e, indice(e));
                }
                if (actual.paso(mundo(), t0)) { actual = null; esperar = 10; break; }
                if (actual.esperando()) break;
            }
        } catch (Throwable t) {
            EmiEstructuras.LOG.error("Fallo generando {}", actual == null ? "?" : actual.e.id, t);
            if (actual != null) { fallos.add(actual.e.id + ": " + t); actual.liberar(); actual = null; }
            esperar = 20;
        }
    }

    private static void terminar() {
        enMarcha = false;
        boolean ok = true;
        for (Entrada e : todas) { Hecha h = progreso.hechas.get(e.id.toString()); if (h == null || !h.ok) ok = false; }
        if (Catalogo.saltadas > 0) {
            ok = false;
            fallos.add(Catalogo.saltadas + " estructuras de las 69 no estan registradas (activa los datapacks Johto/Hoenn/Sinnoh y Terralith ANTES de arrancar el servidor)");
        }
        final boolean todoOk = ok;
        progreso.terminado = todoOk;
        guardar();
        String msg = todoOk ? "Generadas todas las estructuras en emi_estructuras:plano (" + todas.size() + ")."
                : "Generacion terminada con fallos: " + fallos.size() + ". Mira el log y repite /emiestructuras generar.";
        EmiEstructuras.LOG.info(msg);
        server.getPlayerManager().getPlayerList().stream().filter(p -> p.hasPermissionLevel(2))
                .forEach(p -> p.sendMessage(Text.literal("[Emi Estructuras] " + msg).formatted(todoOk ? Formatting.GREEN : Formatting.GOLD), false));
    }

    // ------------------------------------------------------------------ un trabajo = una estructura

    private static final class Trabajo {
        final Entrada e;
        final int cx, cz;                  // centro de su celda
        int fase = 0;
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

        Trabajo(Entrada e, int indice) {
            this.e = e;
            this.cx = (indice % COLUMNAS) * CELDA;
            this.cz = (indice / COLUMNAS) * CELDA;
        }

        void liberar() {
            ServerWorld w = mundo();
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
            int tope = bb.getMaxY();
            int techo = Integer.MIN_VALUE;
            for (int y = tope; y >= bb.getMinY() && techo == Integer.MIN_VALUE; y--)
                for (int x = bb.getMinX(); x <= bb.getMaxX() && techo == Integer.MIN_VALUE; x++)
                    for (int z = bb.getMinZ(); z <= bb.getMaxZ(); z++)
                        if (w.getBlockState(m.set(x, y, z)).isAir()) { techo = y; break; }
            if (techo == Integer.MIN_VALUE || tope - techo <= 2) return;
            int ty = Math.max(bb.getMinY() + 1, techo - 4), tx = Integer.MAX_VALUE, tz = zc;
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
            while (x <= tx) {                        // tramo en pendiente (baja un bloque por cada bloque hacia dentro)
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
            if (piso > ty) {                         // la pendiente no alcanzo la sala: pozo vertical (caida)
                for (int zz = zc - 2; zz <= zc + 2; zz++) for (int y = ty; y <= piso + 3; y++) w.setBlockState(m.set(tx, y, zz), aire, flags);
            }
            EmiEstructuras.LOG.info("{}: acceso excavado hasta la sala en x={} y={} (techo de la sala y={})", e.id, tx, ty, techo);
        }

        boolean esperando() { return fase == 4 && server.getTicks() < esperaAsentar; }

        boolean agotado(long t0) { return System.nanoTime() - t0 >= PRESUPUESTO_NS; }

        boolean paso(ServerWorld w, long t0) {
            switch (fase) {
                case 0 -> inicio(w);
                case 1 -> { if (!preparar(w, t0)) return false; fase = 2; ci = 0; }
                case 2 -> { if (!cargar(w, t0)) return false; fase = 3; ci = 0; }
                case 3 -> { if (!colocar(w, t0)) return false; fase = enterrada ? 6 : 4; if (!enterrada) asentar(); }
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
            flotante = bb.getMinY() > SUELO + 40;
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
            EmiEstructuras.LOG.info("[{}] {} bb=({},{},{})-({},{},{}) {}", indice(e) + 1, e.id, bb.getMinX(), bb.getMinY(), bb.getMinZ(), bb.getMaxX(), bb.getMaxY(), bb.getMaxZ(), e.oceano ? "OCEANO" : enterrada ? "ENTERRADA" : flotante ? "FLOTANTE" : "");
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
            progreso.hechas.put(h.id, h);
            guardar();
            registrar(h);
            liberar();
        }
    }

    /** Le dice a Emipokemon que esta ubicacion esta aqui, para que /emipokemon visitar mande a la dimension plana. */
    public static void registrar(Hecha h) {
        if (h.clave == null) return;
        try {
            Class<?> c = Class.forName("com.emipokemon.admin.ImportantLocationService");
            Method m = c.getMethod("recordGeneratedLocation", MinecraftServer.class, String.class, String.class, RegistryKey.class, BlockPos.class);
            Object r = m.invoke(null, server, h.clave, h.id, PLANO, new BlockPos(h.entradaX, h.entradaY, h.entradaZ));
            if (!Boolean.TRUE.equals(r)) EmiEstructuras.LOG.warn("Emipokemon no acepto la ubicacion {} ({})", h.clave, h.id);
        } catch (ClassNotFoundException e) {
            EmiEstructuras.LOG.warn("Emipokemon no esta instalado: no se registran las ubicaciones de /visitar");
        } catch (Throwable t) {
            EmiEstructuras.LOG.warn("No pude registrar {} en Emipokemon", h.clave, t);
        }
    }
}
