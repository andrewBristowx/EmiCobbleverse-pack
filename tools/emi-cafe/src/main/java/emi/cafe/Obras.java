package emi.cafe;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Todo lo de construir en la parcela de un cafe (desde 0.18.0, aparte de {@link Cafe}). Los cafes nuevos son de «parcela libre»: empiezan vacios y cada bloque, estacion, mesa, caja registradora,
 * cartel y libro que el dueño pone se guarda en {@code Datos.Jugador.decor} (clave «x,y,z» relativa al local, valor = estado del bloque); el cafe se reconstruye siempre repitiendo eso. Lo que
 * se tiene para construir es el {@code almacen} (objeto -> cantidad): se llena comprando en la tienda (y con el kit del principio), poner un bloque gasta uno y quitarlo lo devuelve.
 * Se construye en modo supervivencia (nada de creativo). Los cafes de antes (parcela no libre) siguen con su local ya hecho y su forma de mover estaciones y mesas ({@code estEnMano}).
 */
final class Obras {
    private final Cafe c;

    Obras(Cafe c) { this.c = c; }

    private Datos.Jugador d() { return c.dj(); }

    // ------------------------------------------------------------ lo que hay puesto (parcela libre): se lee de los datos y se guarda hasta el siguiente cambio

    private int ver, verCache = -1;
    private final Map<Estacion, int[]> cEst = new EnumMap<>(Estacion.class);
    private final List<int[]> cMesas = new ArrayList<>();
    private final Set<String> cNombres = new HashSet<>();
    private int[] cCartel;
    private final List<int[]> cPuertas = new ArrayList<>();
    private static final Pattern NOMBRE = Pattern.compile("Name:\"([^\"]+)\"");
    private static Map<String, Estacion> porBloque;

    private static Map<String, Estacion> porBloque() {
        if (porBloque == null) {
            Map<String, Estacion> m = new HashMap<>();
            for (Estacion e : Estacion.values()) m.put(Registries.BLOCK.getId(e.bloque).toString(), e);
            porBloque = m;
        }
        return porBloque;
    }

    /** Algo cambio en lo construido: hay que volver a leer los datos y los caminos. */
    void cambio() { ver++; c.rutas.sucia(); }

    private void escanear() {
        if (verCache == ver) return;
        verCache = ver;
        cEst.clear(); cMesas.clear(); cNombres.clear(); cPuertas.clear(); cCartel = null;
        Datos.Jugador d = d();
        if (!d.libre || d.decor == null) return;
        Map<String, Estacion> pb = porBloque();
        for (Map.Entry<String, String> en : d.decor.entrySet()) {
            Matcher m = NOMBRE.matcher(en.getValue());
            if (!m.find()) continue;
            String nombre = m.group(1);
            if (!nombre.startsWith("emi_cafe:")) continue;
            cNombres.add(nombre);
            if (nombre.startsWith("emi_cafe:puerta_cristal_")) cPuertas.add(new int[]{Integer.parseInt(en.getKey().split(",")[0]), Integer.parseInt(en.getKey().split(",")[2]), Integer.parseInt(en.getKey().split(",")[1])});
            String[] k = en.getKey().split(",");
            int rx = Integer.parseInt(k[0]), ry = Integer.parseInt(k[1]), rz = Integer.parseInt(k[2]);
            Estacion e = pb.get(nombre);
            if (e != null) {
                if (e.tipo == Estacion.Tipo.MESA) cMesas.add(new int[]{rx, rz, ry});
                else if (e.x >= 0 && !cEst.containsKey(e)) cEst.put(e, new int[]{rx, rz, ry});
            } else if (nombre.equals("emi_cafe:cartel") && en.getValue().contains("der:\"false\"")) cCartel = new int[]{rx, rz, ry};
        }
    }

    /** Donde esta cada estacion puesta (x, z, y del local). */
    Map<Estacion, int[]> estaciones() { escanear(); return cEst; }

    /** Donde esta cada mesa puesta (x, z, y del local). */
    List<int[]> mesas() { escanear(); return cMesas; }

    /** ¿Hay puesto un bloque de este tipo del cafe (la caja registradora, el cartel, el libro...)? */
    boolean colocado(Block b) { escanear(); return cNombres.contains(Registries.BLOCK.getId(b).toString()); }

    /** La caja registradora puesta (null si aun no la ha puesto). */
    BlockPos posRegistradora() {
        Map<String, String> dec = d().decor;
        if (dec == null) return null;
        String nom = Registries.BLOCK.getId(CafeBlocks.REGISTRADORA).toString();
        for (Map.Entry<String, String> en : dec.entrySet()) if (en.getValue().contains("\"" + nom + "\"")) {
            String[] k = en.getKey().split(",");
            return Local.pos(c.ox, Integer.parseInt(k[0]), Integer.parseInt(k[1]), Integer.parseInt(k[2]));
        }
        return null;
    }

    // ------------------------------------------------------------ el almacen (lo que se tiene para construir)

    int stock(String item) { Integer n = d().almacen.get(item); return n == null ? 0 : n; }

    void sumarStock(String item, int delta) {
        Map<String, Integer> a = d().almacen;
        int n = Math.max(0, stock(item) + delta);
        if (n == 0) a.remove(item); else a.put(item, n);
        datosSucios = true;                                                 // se guarda en disco en 2 s
    }

    static String idItem(Block b) { return Registries.ITEM.getId(b.asItem()).toString(); }

    /** Pone en el inventario todo lo que hay en el almacen (primero lo del cafe: caja registradora, mesas y estaciones). */
    void llenarInventario(ServerPlayerEntity p) {
        p.getInventory().clear();
        List<String> claves = new ArrayList<>(d().almacen.keySet());
        claves.sort((a, b) -> Integer.compare(prioridad(a), prioridad(b)));
        boolean lleno = false;
        for (String k : claves) {
            Item it = Registries.ITEM.get(Identifier.of(k));
            if (it == null || it == net.minecraft.item.Items.AIR) continue;
            int n = stock(k);
            while (n > 0 && !lleno) {
                int q = Math.min(n, it.getMaxCount());
                if (!p.getInventory().insertStack(new ItemStack(it, q))) { lleno = true; break; }
                n -= q;
            }
        }
        List<Datos.Empleado> pers = d().personal;                                  // cada empleado es un peluche: con el en la mano se elige donde descansa
        if (pers != null) for (int i = 0; i < pers.size() && !lleno; i++) {
            ItemStack pl = peluche(pers.get(i), i);
            if (pl != null && !p.getInventory().insertStack(pl)) lleno = true;
        }
        if (lleno) p.sendMessage(Msg.err("Tu inventario de obra está lleno: lo que no cabe se queda guardado hasta que pongas algo."), false);
        p.playerScreenHandler.sendContentUpdates();
    }

    /** El peluche de un empleado (el de su aspecto, con su nombre y su numero guardado dentro); null si no existe ese peluche. */
    private ItemStack peluche(Datos.Empleado e, int idx) {
        String[] asp = Personal.ASPECTOS[Math.floorMod(e.aspecto, Personal.ASPECTOS.length)];
        Item it = Registries.ITEM.get(Identifier.of("emi_plush", "gatitoalien_" + asp[0].substring(asp[0].indexOf('=') + 1)));
        if (it == null || it == net.minecraft.item.Items.AIR) return null;
        ItemStack st = new ItemStack(it);
        Personal.Rol rol = Personal.Rol.de(e.rol);
        st.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, net.minecraft.text.Text.literal(e.nombre + (rol == null ? "" : " · " + rol.nombre)).formatted(net.minecraft.util.Formatting.GOLD));
        net.minecraft.nbt.NbtCompound n = new net.minecraft.nbt.NbtCompound();
        n.putInt("emi_empleado", idx);
        st.set(net.minecraft.component.DataComponentTypes.CUSTOM_DATA, net.minecraft.component.type.NbtComponent.of(n));
        return st;
    }

    /** El numero de empleado de un peluche de empleado (null si lo que lleva en la mano no lo es). */
    private Integer empleadoDe(ItemStack st) {
        var nc = st.get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
        if (nc == null) return null;
        net.minecraft.nbt.NbtCompound n = nc.copyNbt();
        if (!n.contains("emi_empleado")) return null;
        int i = n.getInt("emi_empleado");
        List<Datos.Empleado> pers = d().personal;
        return pers != null && i >= 0 && i < pers.size() ? i : null;
    }

    private final List<Entity> marcasPuesto = new ArrayList<>();

    /** Mientras se construye, un rotulo con el nombre en el sitio donde descansa cada empleado al que se le ha elegido uno. */
    private void ponerMarcas() {
        quitarMarcas();
        List<Datos.Empleado> pers = d().personal;
        if (pers == null) return;
        for (Datos.Empleado e : pers) if (e.puesto != null)
            marcasPuesto.add(Disp.texto(c.w, "{\"text\":\"" + e.nombre.replace("\\", "").replace("\"", "") + "\",\"color\":\"gold\",\"bold\":true}", c.ox + e.puesto[0] + 0.5, Local.Y + e.puesto[1] + 0.9, e.puesto[2] + 0.5, 0.9f, false));
    }

    void quitarMarcas() { for (Entity m : marcasPuesto) Disp.quitar(m); marcasPuesto.clear(); }

    /** Clic con el peluche de un empleado: ahi descansara (agachado: vuelve a un sitio automatico de la cocina). */
    private ActionResult colocarEmpleado(ServerPlayerEntity p, BlockHitResult hit, int idx) {
        Datos.Empleado e = d().personal.get(idx);
        if (p.isSneaking()) {
            e.puesto = null;
            c.msg(p, e.nombre + " vuelve a descansar donde haya sitio en la cocina");
        } else {
            BlockPos t = hit.getBlockPos().offset(hit.getSide());
            if (!c.enParcela(t)) { c.msg(p, "Ese terreno no es tuyo"); return ActionResult.FAIL; }
            e.puesto = new int[]{t.getX() - c.ox, t.getY() - Local.Y, t.getZ()};
            c.msg(p, e.nombre + " descansará aquí (" + (e.rol.isEmpty() ? "" : Personal.Rol.de(e.rol) == null ? "" : Personal.Rol.de(e.rol).nombre.toLowerCase()) + ")");
            c.w.spawnParticles(net.minecraft.particle.ParticleTypes.HAPPY_VILLAGER, t.getX() + 0.5, t.getY() + 0.3, t.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0.0);
        }
        c.sonido(p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ITEM_PICKUP, 0.6f, 1.4f);
        ponerMarcas();
        datosSucios = true;
        return ActionResult.FAIL;
    }

    /** Si en el almacen hay mas de lo que cabe en el inventario (o falta el peluche de un empleado), rellena los huecos que hayan quedado libres. */
    void completar(ServerPlayerEntity p) {
        if (!c.construyendo) return;
        var inv = p.getInventory();
        Map<String, Integer> tengo = new HashMap<>();
        Set<Integer> pelu = new HashSet<>();
        for (ItemStack s : inv.main) {
            if (s.isEmpty()) continue;
            Integer e = empleadoDe(s);
            if (e != null) { pelu.add(e); continue; }
            tengo.merge(Registries.ITEM.getId(s.getItem()).toString(), s.getCount(), Integer::sum);
        }
        for (Map.Entry<String, Integer> en : new ArrayList<>(d().almacen.entrySet())) {
            Item it = Registries.ITEM.get(Identifier.of(en.getKey()));
            if (it == null || it == net.minecraft.item.Items.AIR) continue;
            int falta = en.getValue() - tengo.getOrDefault(en.getKey(), 0);
            while (falta > 0) {
                int q = Math.min(falta, it.getMaxCount());
                if (!inv.insertStack(new ItemStack(it, q))) { p.playerScreenHandler.sendContentUpdates(); return; }
                falta -= q;
            }
        }
        List<Datos.Empleado> pers = d().personal;
        if (pers != null) for (int i = 0; i < pers.size(); i++) if (!pelu.contains(i)) {
            ItemStack pl = peluche(pers.get(i), i);
            if (pl != null && !inv.insertStack(pl)) break;
        }
        p.playerScreenHandler.sendContentUpdates();
    }

    private static int prioridad(String k) {
        if (k.equals(idItem(CafeBlocks.REGISTRADORA))) return 0;
        if (k.equals(idItem(CafeBlocks.MESA))) return 1;
        return k.startsWith("emi_cafe:") ? 2 : 3;
    }

    /** Devuelve un objeto al almacen (y al inventario si se esta construyendo). */
    void devolverItem(ServerPlayerEntity p, String item) {
        sumarStock(item, 1);
        Item it = Registries.ITEM.get(Identifier.of(item));
        if (it != null && it != net.minecraft.item.Items.AIR && c.construyendo) p.getInventory().insertStack(new ItemStack(it));
        c.sonido(p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ITEM_PICKUP, 0.5f, 1.1f);
        p.playerScreenHandler.sendContentUpdates();
    }

    // ------------------------------------------------------------ kit del principio y lo que siempre se tiene

    private static final Object[][] KIT = {
            {"emi_cafe:mesa", 2}, {"emi_cafe:encimera", 8}, {"minecraft:oak_planks", 64}, {"minecraft:stone_bricks", 32}, {"minecraft:glass_pane", 16},
            {"minecraft:oak_stairs", 8}, {"minecraft:oak_slab", 8}, {"minecraft:white_carpet", 16}};
    static final int MONEDAS_KIT = 200;

    private void asegurar(Block b) {
        if (!colocado(b) && stock(idItem(b)) == 0) sumarStock(idItem(b), 1);
    }

    /** Se da una vez, al elegir cocinero, a las parcelas libres: mesas, encimeras y unos bloques para empezar, y unas monedas. */
    void kit() {
        Datos.Jugador d = d();
        if (!d.libre || d.kitDado || d.inicial == null) return;
        d.kitDado = true;
        for (Object[] k : KIT) sumarStock((String) k[0], (Integer) k[1]);
        d.monedas += MONEDAS_KIT;
        ServerPlayerEntity p = c.jugador(c.duenio);
        if (p != null) p.sendMessage(Msg.say("Tu parcela está vacía: es tuya para construir. Te damos un kit (caja registradora, 2 mesas, estaciones de tu plato, cartel, libro, encimeras y unos bloques) y " + MONEDAS_KIT + " monedas. Lo demás se compra en la caja registradora → DECORAR."), false);
        EmiCafe.datos.guardar(c.duenio);
    }

    /** Se tienen siempre (puestas o guardadas): la caja registradora, el cartel, el libro, la basura, el fregadero y las estaciones de los platos que se saben. */
    void sincronizar() {
        Datos.Jugador d = d();
        if (!d.libre) return;
        escanear();
        asegurar(CafeBlocks.REGISTRADORA); asegurar(CafeBlocks.CARTEL); asegurar(CafeBlocks.LIBRO_PARED);
        Set<Estacion> act = c.activas();
        for (Estacion e : Estacion.values()) if (e.x >= 0 && (e.siempre || act.contains(e))) asegurar(e.bloque);
    }

    // ------------------------------------------------------------ que se puede construir y donde

    /**
     * ¿Es una casilla donde se puede construir? Casi toda la parcela propia: el jardin de atras, el local, los lados y la acera de delante, hasta 9 bloques de alto. No la calle ni el parque
     * (son de todos), ni el seto y el muro de la parcela, ni el camino por donde entran los clientes. En un cafe de antes tambien se guardan la puerta y su letrero.
     */
    boolean construible(BlockPos pos) {
        int rx = pos.getX() - c.ox, ry = pos.getY() - Local.Y, rz = pos.getZ();
        if (ry < 0 || ry > 9) return false;
        if (rx < Ciudad.X0 + 1 || rx > Ciudad.X1 - 1 || rz < Local.Z_ATRAS + 2 || rz > 15) return false;
        if (rz >= 14 && rx >= 5 && rx <= 10) return false;                                                    // el camino de la puerta se queda libre
        if (!d().libre) {
            if (rz == 13 && rx >= 7 && rx <= 8 && ry >= 1 && ry <= 4) return false;                          // y la puerta
            if (rz == 14 && rx >= 7 && rx <= 8 && ry >= 5) return false;                                      // y el letrero que hay encima
        }
        return true;
    }

    private static final Set<String> PROHIBIDOS = Set.of("bedrock", "barrier", "light", "spawner", "trial_spawner", "vault", "tnt", "end_portal", "end_portal_frame", "end_gateway", "nether_portal", "reinforced_deepslate",
            "jigsaw", "structure_void", "hopper", "dropper", "dispenser", "crafter", "observer", "target", "sculk_sensor", "calibrated_sculk_sensor", "sculk_shrieker", "lava", "water", "fire", "soul_fire", "cobweb_dummy");

    /** ¿Se puede colocar este bloque en la parcela? Todo salvo lo que es peligroso o hace cosas raras: bloques de comandos y estructuras, TNT, pistones, redstone, embudos, generadores de monstruos, cofres y hornos, arena y grava, fuego y liquidos. */
    static boolean permitido(Block b) {
        Identifier id = Registries.BLOCK.getId(b);
        String p = id.getPath();
        if (id.getNamespace().equals("emi_cafe")) return b == CafeBlocks.ENCIMERA || b == CafeBlocks.MESA || b == CafeBlocks.CARTEL || b == CafeBlocks.LIBRO_PARED || b instanceof PuertaCristalBlock || Estacion.de(b) != null;
        if (PROHIBIDOS.contains(p) || p.contains("command_block") || p.contains("structure") || p.contains("redstone") || p.contains("piston") || p.contains("repeater") || p.contains("comparator") || p.contains("test_")) return false;
        if (b instanceof net.minecraft.block.FallingBlock || b instanceof net.minecraft.block.FluidBlock || b instanceof net.minecraft.block.AbstractFireBlock || b instanceof net.minecraft.block.TntBlock) return false;
        if (b instanceof net.minecraft.block.BlockWithEntity) {
            return b instanceof net.minecraft.block.AbstractSignBlock || b instanceof net.minecraft.block.AbstractBannerBlock || b instanceof net.minecraft.block.BedBlock
                    || b instanceof net.minecraft.block.AbstractSkullBlock || b instanceof net.minecraft.block.CampfireBlock || b instanceof net.minecraft.block.BellBlock || b instanceof net.minecraft.block.EnchantingTableBlock;
        }
        return true;
    }

    /** En un cafe de antes, los bloques del cafe que no se quitan con el clic normal (la caja registradora, el libro, el letrero). */
    private static boolean protegido(Block b) {
        return Registries.BLOCK.getId(b).getNamespace().equals("emi_cafe") && b != CafeBlocks.ENCIMERA && b != CafeBlocks.MESA && !(b instanceof PuertaCristalBlock) && Estacion.de(b) == null;
    }

    String clave(BlockPos pos) { return Decor.clave(pos.getX() - c.ox, pos.getY() - Local.Y, pos.getZ()); }

    private boolean datosSucios;                                           // hay cambios sin guardar en disco (se guardan cada 2 s y al salir del modo construccion)

    void guardarPronto() { datosSucios = true; cambio(); }

    /** Cada tick: guarda lo que haya cambiado cada 2 segundos. */
    void tick(int t) { if (datosSucios && t % 40 == 0) { datosSucios = false; EmiCafe.datos.guardar(c.duenio); } }

    /**
     * Vuelve a poner lo que el dueño construyo (despues de reconstruir la parcela): en un cafe de antes, primero quita lo que habia quitado del edificio. Lo que ya no cabe (fuera de lo que
     * se puede construir, encima de una estacion) se olvida.
     */
    void aplicarDecor() {
        Datos.Jugador d = d();
        if (d.quitados == null) d.quitados = new LinkedHashSet<>();
        for (java.util.Iterator<String> it = d.quitados.iterator(); it.hasNext(); ) {
            String[] k = it.next().split(",");
            BlockPos pos = new BlockPos(c.ox + Integer.parseInt(k[0]), Local.Y + Integer.parseInt(k[1]), Integer.parseInt(k[2]));
            if (!construible(pos)) { it.remove(); continue; }
            Block ahi = c.w.getBlockState(pos).getBlock();
            if (!d.libre && (protegido(ahi) || Estacion.de(ahi) != null || ahi == CafeBlocks.MESA)) continue;
            c.w.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
        }
        if (d.decor == null || d.decor.isEmpty()) { cambio(); return; }
        java.util.Iterator<Map.Entry<String, String>> it = d.decor.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> en = it.next();
            String[] k = en.getKey().split(",");
            BlockState st = Decor.leer(en.getValue());
            BlockPos pos = new BlockPos(c.ox + Integer.parseInt(k[0]), Local.Y + Integer.parseInt(k[1]), Integer.parseInt(k[2]));
            if (st == null || !construible(pos)) { it.remove(); continue; }
            Block ahi = c.w.getBlockState(pos).getBlock();
            if (!d.libre && (protegido(ahi) || Estacion.de(ahi) != null || ahi == CafeBlocks.MESA)) { it.remove(); continue; }
            c.w.setBlockState(pos, st, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
        }
        cambio();
    }

    // ------------------------------------------------------------ las puertas de cristal

    private final java.util.Map<Long, Integer> puertaSinGente = new HashMap<>();       // ticks (de 4 en 4) que lleva cada puerta abierta sin nadie cerca

    /**
     * Abre las puertas de cristal cuando se acerca alguien (con una campanilla de tienda) y las cierra cuando ya no queda nadie cerca. Abren: siempre el dueño y los jugadores del cafe;
     * con el cafe abierto, tambien los clientes y los empleados. Con el cafe cerrado nadie mas entra. El estado abierto/cerrado solo vive en el mundo (en lo guardado siempre esta cerrada).
     */
    void tickPuertas(int t) {
        if (t % 4 != 0) return;
        escanear();
        if (cPuertas.isEmpty()) return;
        List<double[]> gente = new ArrayList<>();
        for (ServerPlayerEntity p : c.presentes()) gente.add(new double[]{p.getX(), p.getY(), p.getZ()});
        if (c.estado == Cafe.Estado.ABIERTO) {
            for (Cliente cl : c.clientes) if (!cl.e.isRemoved()) gente.add(new double[]{cl.e.getX(), cl.e.getY(), cl.e.getZ()});
            for (Ayudante a : c.ayudantes) if (!a.e.isRemoved()) gente.add(new double[]{a.e.getX(), a.e.getY(), a.e.getZ()});
        }
        boolean sono = false;
        for (int[] d : cPuertas) {
            BlockPos pos = Local.pos(c.ox, d[0], d[2], d[1]);
            BlockState st = c.w.getBlockState(pos);
            if (!(st.getBlock() instanceof PuertaCristalBlock)) continue;
            boolean hay = false;
            for (double[] q : gente) if (Math.abs(q[0] - pos.getX() - 0.5) <= 2.2 && Math.abs(q[2] - pos.getZ() - 0.5) <= 2.2 && q[1] > pos.getY() - 2.0 && q[1] < pos.getY() + 2.5) { hay = true; break; }
            long k = pos.asLong();
            boolean abierta = st.get(PuertaCristalBlock.ABIERTA);
            if (hay) {
                puertaSinGente.put(k, 0);
                if (!abierta) {
                    c.w.setBlockState(pos, st.with(PuertaCristalBlock.ABIERTA, true), Block.NOTIFY_ALL);
                    if (!sono) { Sonidos.mundo(c.w, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, "campanilla", 1.0f, 1.0f); sono = true; }
                }
            } else if (abierta) {
                int n = puertaSinGente.merge(k, 1, Integer::sum);
                if (n >= 5) {                                                                       // ~1 s sin nadie cerca
                    c.w.setBlockState(pos, st.with(PuertaCristalBlock.ABIERTA, false), Block.NOTIFY_ALL);
                    puertaSinGente.remove(k);
                }
            }
        }
    }

    // ------------------------------------------------------------ el cartel con el nombre y el letrero abierto/cerrado

    private Entity rotuloA, rotuloB;

    /** Pone (o quita) el nombre del cafe sobre el cartel que el dueño colocó (parcela libre). */
    void actualizarRotulo() {
        Disp.quitar(rotuloA); Disp.quitar(rotuloB); rotuloA = rotuloB = null;
        Datos.Jugador d = d();
        if (!d.libre) return;
        escanear();
        if (cCartel == null) return;
        BlockPos b = Local.pos(c.ox, cCartel[0], cCartel[2], cCartel[1]);
        String nombre = d.nombreCafe == null || d.nombreCafe.isEmpty() ? "CAFÉ MICHIALIEN" : d.nombreCafe;
        float escala = nombre.length() > 16 ? 1.0f : nombre.length() > 12 ? 1.25f : 1.5f;
        rotuloA = Disp.texto(c.w, "{\"text\":\"" + nombre.replace("\\", "").replace("\"", "") + "\",\"color\":\"light_purple\",\"bold\":true}", b.getX() + 1.0, b.getY() + 1.35, b.getZ() + 0.2, escala, false);
        if (d.nombre != null && !d.nombre.isEmpty()) rotuloB = Disp.texto(c.w, "{\"text\":\"de " + d.nombre.replace("\\", "").replace("\"", "") + "\",\"color\":\"yellow\"}", b.getX() + 1.0, b.getY() + 1.1, b.getZ() + 0.2, 0.8f, false);
    }

    /** El letrero (verde ABIERTO / rojo CERRADO): el del cartel que colocó el dueño, o el de la puerta de un cafe de antes. */
    void letrero(boolean abierto) {
        if (!d().libre) { Local.letrero(c.w, c.ox, abierto); return; }
        escanear();
        if (cCartel == null) return;
        BlockPos b = Local.pos(c.ox, cCartel[0], cCartel[2], cCartel[1]);
        for (int k = 0; k < 2; k++) {
            BlockPos q = b.east(k);
            BlockState st = c.w.getBlockState(q);
            if (st.getBlock() == CafeBlocks.CARTEL) c.w.setBlockState(q, st.with(PlacaBlock.ABIERTO, abierto), Block.NOTIFY_ALL);
        }
    }

    // ------------------------------------------------------------ el modo construccion

    /** Entra en el modo construccion (solo el dueño, con el cafe cerrado y sin invitados). Devuelve el error, o null. */
    String entrar(ServerPlayerEntity p) {
        if (!c.esDuenio(p)) return "Solo el dueño puede construir.";
        if (c.estado != Cafe.Estado.PREPARANDO) return "Cierra el café antes de construir.";
        if (c.trabajadores() > 1) return "Despide primero a tus invitados: solo se construye a solas.";
        c.construyendo = true;
        c.estEnMano.clear();
        c.manos.put(p.getUuid(), null);
        c.quitarAyudantes();                                                  // los empleados no estorban mientras se construye (vuelven al terminar)
        p.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);
        p.getAbilities().allowFlying = false; p.getAbilities().flying = false; p.sendAbilitiesUpdate();
        llenarInventario(p);
        ponerMarcas();
        c.rutas.sucia();
        p.sendMessage(Msg.say("MODO CONSTRUCCIÓN: construyes con lo que tienes (tu inventario, E) y lo que compras en la caja registradora → DECORAR. Clic derecho = poner un bloque; clic izquierdo = quitarlo (te lo devuelve). "
                + "Estaciones, mesas, caja registradora, cartel y libro también se ponen y se quitan así. Agachado + clic en el suelo = cambiar el suelo. El cartel se pega a la cara sur de una pared. "
                + "Tus empleados son peluches de tu inventario: clic derecho con uno en el sitio donde quieres que descanse (agachado = vuelve al sitio automático); su nombre se cambia en la pestaña PERSONAL. "
                + "Se guarda solo. Clic derecho en la caja registradora = menú (TERMINAR DE CONSTRUIR)."), false);
        return null;
    }

    void salir(ServerPlayerEntity p) {
        if (!c.construyendo) return;
        c.construyendo = false;
        quitarMarcas();
        p.getInventory().clear();
        p.changeGameMode(net.minecraft.world.GameMode.ADVENTURE);
        datosSucios = false;
        c.rutas.sucia();
        if (d().libre) refrescar();
        else if (!c.estEnMano.isEmpty()) { c.estEnMano.clear(); c.reconstruir(); c.msg(p, "Las estaciones que no pusiste vuelven a donde estaban"); }
        else c.recargarPersonal();
        EmiCafe.datos.guardar(c.duenio);
        c.sincronizarMano(p);
        p.sendMessage(Msg.say("Terminaste de construir. Se guardó todo."), false);
    }

    /** Pone al dia las mesas, los empleados y los textos despues de construir (sin reconstruir la parcela entera). */
    void refrescar() {
        cambio();
        c.mesas.clear();
        for (int[] m : mesas()) c.mesas.add(new Mesa(Local.pos(c.ox, m[0], m[2], m[1])));
        for (Cafe.Est e : c.ests.values()) { Disp.quitar(e.icono); Disp.quitar(e.barra); }
        c.ests.clear();
        c.recargarPersonal();
        actualizarRotulo();
        letrero(false);
    }

    private ItemStack mano(ServerPlayerEntity p) { return p.getStackInHand(Hand.MAIN_HAND); }

    /** Clic derecho del dueño en modo construccion. */
    ActionResult clic(ServerPlayerEntity p, BlockHitResult hit, Hand hand) {
        if (hand != Hand.MAIN_HAND) return ActionResult.FAIL;
        BlockPos clic = hit.getBlockPos();
        if (!c.enParcela(clic)) { c.msg(p, "Ese terreno no es tuyo"); return ActionResult.FAIL; }
        Block bq = c.w.getBlockState(clic).getBlock();
        Estacion eq = Estacion.de(bq);
        if (eq != null && eq.tipo == Estacion.Tipo.REGISTRADORA) { Menus.principal(c, p); return ActionResult.FAIL; }
        if (bq == CafeBlocks.LIBRO_PARED) { Menus.libro(c, p); return ActionResult.FAIL; }
        ItemStack st = mano(p);
        Integer emp = empleadoDe(st);
        if (emp != null) return colocarEmpleado(p, hit, emp);
        if (!(st.getItem() instanceof BlockItem bi)) return ActionResult.FAIL;
        Block bloque = bi.getBlock();
        Datos.Jugador d = d();
        String itemId = Registries.ITEM.getId(bi).toString();
        Estacion est = Estacion.de(bloque);
        boolean esMesa = bloque == CafeBlocks.MESA;
        boolean antigua = !d.libre && ((est != null && est.x >= 0) || esMesa);          // los cafes de antes mueven sus estaciones y mesas recogiendolas (sin almacen)
        if (antigua && est != null) return colocarEstacionAntigua(p, hit, st, est);
        if (!antigua && stock(itemId) <= 0) {
            c.msg(p, "No te queda de eso: compra más en la caja registradora → DECORAR");
            p.playerScreenHandler.syncState();
            return ActionResult.FAIL;
        }
        if (!permitido(bloque)) { c.msg(p, "Ese bloque no se puede poner en el café (es peligroso o raro: comandos, TNT, pistones, redstone, cofres, arena...)"); return ActionResult.FAIL; }
        if (d.libre && p.isSneaking() && clic.getY() - Local.Y == 0) return cambiarSuelo(p, clic, st, bloque, itemId);
        net.minecraft.item.ItemUsageContext uc = new net.minecraft.item.ItemUsageContext(p, hand, hit);
        net.minecraft.item.ItemPlacementContext pc = new net.minecraft.item.ItemPlacementContext(uc);
        BlockPos pos = pc.getBlockPos();
        if (!construible(pos)) { c.msg(p, "Ahí no se puede construir (solo en tu terreno hasta 9 bloques de alto; no el camino de los clientes, ni la calle, el parque o el seto)"); return ActionResult.FAIL; }
        if (esMesa && antigua && c.mesasPos().size() >= c.numMesas()) { c.msg(p, "No tienes más mesas: mejora el café para tener más"); return ActionResult.FAIL; }
        boolean cartel = bloque == CafeBlocks.CARTEL;
        BlockPos pos2 = pos.east();
        if (cartel && (!construible(pos2) || !c.w.getBlockState(pos2).isReplaceable())) { c.msg(p, "El cartel mide 2 bloques de ancho: necesita sitio libre a la derecha"); return ActionResult.FAIL; }
        if (d.libre && est != null && est.x >= 0 && !est.siempre) {
            int puestas = 0;
            for (Estacion e : estaciones().keySet()) if (!e.siempre) puestas++;
            if (puestas >= Local.maxEstaciones(c.nivelLocal())) { c.msg(p, "Tu local de nivel " + c.nivelLocal() + " solo admite " + Local.maxEstaciones(c.nivelLocal()) + " estaciones: amplíalo (mejora «Ampliar el local»)"); return ActionResult.FAIL; }
        }
        if (d.quitados == null) d.quitados = new LinkedHashSet<>();
        if (!esMesa && d.decor.size() + d.quitados.size() >= Decor.MAX_BLOQUES) { c.msg(p, "Has cambiado demasiados bloques (" + Decor.MAX_BLOQUES + ")"); return ActionResult.FAIL; }
        if (!pc.canPlace()) return ActionResult.FAIL;
        ActionResult r = st.useOnBlock(uc);
        if (r.isAccepted()) {
            BlockState nuevo = c.w.getBlockState(pos);
            if (!antigua) sumarStock(itemId, -1);
            if (esMesa && antigua) {
                List<int[]> mp = c.mesasPos();
                if (mp.size() < c.numMesas()) { mp.add(new int[]{pos.getX() - c.ox, pos.getZ()}); c.mesas.add(new Mesa(pos)); }
            } else {
                String k = clave(pos);
                d.quitados.remove(k);
                d.decor.put(k, Decor.guardar(nuevo));
                if (cartel) {
                    BlockState der = CafeBlocks.CARTEL.getDefaultState().with(PlacaBlock.DER, true);
                    c.w.setBlockState(pos2, der, Block.NOTIFY_ALL);
                    d.decor.put(clave(pos2), Decor.guardar(der));
                }
                // una cama, una puerta o una planta alta ocupan dos bloques: se guarda tambien el de al lado
                for (BlockPos q : new BlockPos[]{pos.up(), pos.down(), pos.north(), pos.south(), pos.east(), pos.west()}) {
                    BlockState sq = c.w.getBlockState(q);
                    if (sq.getBlock() == nuevo.getBlock() && !q.equals(pos) && construible(q) && !d.decor.containsKey(clave(q))
                            && (sq.getBlock() instanceof net.minecraft.block.BedBlock || sq.getBlock() instanceof net.minecraft.block.DoorBlock || sq.getBlock() instanceof net.minecraft.block.TallPlantBlock)) {
                        d.quitados.remove(clave(q));
                        d.decor.put(clave(q), Decor.guardar(sq));
                    }
                }
            }
            p.getInventory().markDirty();
            p.playerScreenHandler.sendContentUpdates();
            guardarPronto();
            completar(p);
            if (cartel) { actualizarRotulo(); letrero(false); }
        }
        return ActionResult.FAIL;
    }

    /** Agachado + clic en el suelo (parcela libre): cambia ese bloque del suelo por el que llevas (con bloques enteros). */
    private ActionResult cambiarSuelo(ServerPlayerEntity p, BlockPos pos, ItemStack st, Block bloque, String itemId) {
        if (!bloque.getDefaultState().isFullCube(net.minecraft.world.EmptyBlockView.INSTANCE, BlockPos.ORIGIN) || bloque instanceof net.minecraft.block.BlockWithEntity || Registries.BLOCK.getId(bloque).getNamespace().equals("emi_cafe")) {
            c.msg(p, "El suelo se cambia con bloques enteros (madera, piedra, hormigón...)");
            return ActionResult.FAIL;
        }
        if (!construible(pos)) { c.msg(p, "Ese suelo no se puede cambiar (el camino de los clientes, la calle y el parque son fijos)"); return ActionResult.FAIL; }
        Datos.Jugador d = d();
        String k = clave(pos);
        String previo = d.decor.get(k);
        if (previo != null) {
            BlockState ps = Decor.leer(previo);
            if (ps != null && ps.getBlock() == bloque) return ActionResult.FAIL;                    // ya es ese
            if (ps != null && ps.getBlock().asItem() != net.minecraft.item.Items.AIR) devolverItem(p, Registries.ITEM.getId(ps.getBlock().asItem()).toString());
        }
        BlockState nuevo = bloque.getDefaultState();
        d.decor.put(k, Decor.guardar(nuevo));
        c.w.setBlockState(pos, nuevo, Block.NOTIFY_ALL);
        sumarStock(itemId, -1);
        st.decrement(1);
        c.sonido(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_STONE_PLACE, 0.8f, 1.0f);
        p.getInventory().markDirty();
        p.playerScreenHandler.sendContentUpdates();
        guardarPronto();
        return ActionResult.FAIL;
    }

    /** Un cafe de antes: pone una estacion que el dueño llevaba en la mano en una casilla libre (sobre una encimera la sustituye). */
    private ActionResult colocarEstacionAntigua(ServerPlayerEntity p, BlockHitResult hit, ItemStack st, Estacion e) {
        BlockPos clic = hit.getBlockPos();
        BlockPos t = c.w.getBlockState(clic).getBlock() == CafeBlocks.ENCIMERA ? clic : clic.offset(hit.getSide());
        int rx = t.getX() - c.ox, ry = t.getY() - Local.Y, rz = t.getZ();
        if (!c.estEnMano.contains(e)) { c.msg(p, e.nombre + " ya está colocada: recógela primero (clic izquierdo) para moverla"); return ActionResult.FAIL; }
        if (ry != 1 || !c.casillaEstacion(rx, rz)) { c.msg(p, "Ahí no cabe una estación: ponla en el suelo (o sobre una encimera) de tu terreno, sin tapar la puerta"); return ActionResult.FAIL; }
        BlockState ahi = c.w.getBlockState(t);
        if (!ahi.isAir() && ahi.getBlock() != CafeBlocks.ENCIMERA) { c.msg(p, "Esa casilla está ocupada"); return ActionResult.FAIL; }
        c.w.setBlockState(t, e.bloque.getDefaultState(), Block.NOTIFY_ALL);
        st.decrement(1);
        c.estEnMano.remove(e);
        d().estPos.put(e.name(), new int[]{rx, rz});
        c.sonido(t.getX() + 0.5, t.getY() + 0.5, t.getZ() + 0.5, SoundEvents.BLOCK_WOOD_PLACE, 0.8f, 1.0f);
        p.getInventory().markDirty();
        p.playerScreenHandler.sendContentUpdates();
        guardarPronto();
        return ActionResult.FAIL;
    }

    /** Clic izquierdo del dueño en modo construccion: quita lo suyo (y se lo devuelve al almacen) o un bloque del terreno que se pueda construir. */
    ActionResult ataque(ServerPlayerEntity p, BlockPos pos) {
        BlockState st = c.w.getBlockState(pos);
        if (st.isAir() || !c.enParcela(pos)) return ActionResult.FAIL;
        Datos.Jugador d = d();
        if (d.quitados == null) d.quitados = new LinkedHashSet<>();
        Block b = st.getBlock();
        int ry = pos.getY() - Local.Y, rz = pos.getZ();
        String k = clave(pos);
        if (!d.libre) {                                                                // un cafe de antes: las estaciones y las mesas se recogen para moverlas
            if (b == CafeBlocks.MESA) {
                int rx = pos.getX() - c.ox;
                List<int[]> mp = c.mesasPos();
                for (int i = 0; i < mp.size(); i++) if (mp.get(i)[0] == rx && mp.get(i)[1] == rz) {
                    mp.remove(i);
                    c.mesas.removeIf(m -> m.bloque.equals(pos));
                    c.w.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                    devolverAMano(p, b);
                    guardarPronto();
                    return ActionResult.FAIL;
                }
            }
            Estacion es = Estacion.de(b);
            if (es != null && es.x >= 0 && ry == 1) {
                if (!c.estEnMano.isEmpty() && c.estEnMano.contains(es)) return ActionResult.FAIL;
                c.estEnMano.add(es);
                Cafe.Est viejo = c.ests.remove(pos);
                if (viejo != null) { Disp.quitar(viejo.icono); Disp.quitar(viejo.barra); }
                c.w.setBlockState(pos, (rz == 1 || rz == 5 ? CafeBlocks.ENCIMERA : net.minecraft.block.Blocks.AIR).getDefaultState(), Block.NOTIFY_ALL);
                devolverAMano(p, b);
                c.msg(p, es.nombre + " en tu mano: clic derecho en otro sitio para ponerla");
                guardarPronto();
                return ActionResult.FAIL;
            }
            if (protegido(b) || es != null) { c.msg(p, "Eso es parte del café: no se puede quitar"); return ActionResult.FAIL; }
        }
        if (!construible(pos)) { c.msg(p, "Ahí no se puede quitar (el camino de los clientes, la calle, el parque y el seto son fijos)"); return ActionResult.FAIL; }
        boolean mio = d.decor.containsKey(k);
        if (ry == 0) {                                                                 // el suelo: solo se quita lo que el dueño puso (vuelve el suelo de siempre)
            if (!mio) { c.msg(p, "El suelo no se puede quitar: cámbialo por otro (agachado + clic con un bloque)"); return ActionResult.FAIL; }
            d.decor.remove(k);
            Obra.poner(c.w, pos.getX(), pos.getY(), pos.getZ(), Local.sueloExterior(pos.getX() - c.ox, rz));
        } else {
            if (mio) d.decor.remove(k); else d.quitados.add(k);                       // era del edificio de siempre o del terreno: se apunta como quitado para que siga asi al reconstruir
            c.w.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            if (b == CafeBlocks.CARTEL) {                                              // el cartel son dos mitades: se va la otra tambien
                BlockPos otra = st.get(PlacaBlock.DER) ? pos.west() : pos.east();
                if (c.w.getBlockState(otra).getBlock() == CafeBlocks.CARTEL) { c.w.setBlockState(otra, net.minecraft.block.Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL); d.decor.remove(clave(otra)); }
            }
            for (BlockPos q : new BlockPos[]{pos.up(), pos.down(), pos.north(), pos.south(), pos.east(), pos.west()}) {          // la otra mitad de una puerta o una cama que se ha ido sola
                if (c.w.getBlockState(q).isAir() && d.decor.remove(clave(q)) != null) { /* ya no existe */ }
            }
        }
        if (mio && b.asItem() != net.minecraft.item.Items.AIR) devolverItem(p, Registries.ITEM.getId(b.asItem()).toString());
        guardarPronto();
        completar(p);
        if (b == CafeBlocks.CARTEL) actualizarRotulo();
        return ActionResult.FAIL;
    }

    /** Un cafe de antes: lo que se recoge para moverlo va al inventario (no al almacen). */
    private void devolverAMano(ServerPlayerEntity p, Block b) {
        ItemStack back = new ItemStack(b.asItem());
        if (!back.isEmpty()) p.getInventory().insertStack(back);
        c.sonido(p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ITEM_PICKUP, 0.5f, 1.1f);
        p.playerScreenHandler.sendContentUpdates();
    }

    /** Solo pruebas ({@code /cafe dev poner}): pone un bloque como si lo hubiera puesto el dueño (se guarda en la parcela). */
    String ponerDev(int rx, int ry, int rz, BlockState st) {
        BlockPos pos = new BlockPos(c.ox + rx, Local.Y + ry, rz);
        if (!construible(pos)) return "Ahí no se puede construir.";
        c.w.setBlockState(pos, st, Block.NOTIFY_ALL);
        d().decor.put(clave(pos), Decor.guardar(st));
        guardarPronto();
        return null;
    }

    // ------------------------------------------------------------ comprar para construir

    /** Compra un articulo de la tienda de bloques: va al almacen (y al inventario si se esta construyendo). Devuelve el error, o null. */
    String comprar(ServerPlayerEntity p, Decor.Articulo art) {
        if (art == null || !c.esDuenio(p)) return "Solo el dueño compra.";
        if (c.estado != Cafe.Estado.PREPARANDO) return "Solo se compra con el café cerrado.";
        Datos.Jugador d = c.dj(p);
        if (art.item().equals(idItem(CafeBlocks.MESA))) {
            int tiene = (d.libre ? mesas().size() : c.mesasPos().size()) + stock(art.item());
            if (tiene >= c.numMesas()) return "No caben más mesas en tu café (" + c.numMesas() + "): mejora «Mesas» o amplía el local.";
        }
        if (d.monedas < art.precio()) return "Te faltan " + (art.precio() - d.monedas) + " monedas.";
        Item it = art.objeto();
        if (it == null || it == net.minecraft.item.Items.AIR) return "Ese artículo ya no existe.";
        if (c.construyendo) {
            ItemStack st = new ItemStack(it, art.cantidad());
            if (!p.getInventory().insertStack(st) && !st.isEmpty()) return "Tu inventario está lleno.";
        }
        d.monedas -= art.precio();
        sumarStock(art.item(), art.cantidad());
        EmiCafe.datos.guardar(p.getUuid());
        return null;
    }
}
