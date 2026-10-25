package emi.cafe;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import com.cobblemon.mod.common.entity.PoseType;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * La granja de un cafe en el mundo (0.24.0): los huertos y corrales detras del muro del fondo de la parcela (9 x 9 bloques cada uno, ver {@link Granja}), el cultivo que se ve crecer, los animales y el
 * michigranjero de cada uno, que cosecha cuando esta listo y lo deja en la despensa. Solo vive mientras alguien esta en el cafe; el tiempo de crecimiento cuenta con el reloj de pared
 * (crece aunque el dueño no este: al volver estara listo y los michigranjeros se ponen a cosechar).
 */
final class Fincas {
    private final Cafe c;
    private final Random rnd = new Random();
    private final Map<Integer, Vivo> vivos = new HashMap<>();

    /** Una casilla de cultivo; {@code dir} = hacia donde mira (el cacao se pega a un tronco; null = nada). */
    private record Celda(BlockPos pos, Direction dir) {}

    /** Lo que hay en el mundo de una finca. */
    private static final class Vivo {
        Datos.Finca f;
        Granja.Cultivo cult;
        Ayudante granjero;
        final List<PokemonEntity> animales = new ArrayList<>();
        final List<Celda> cultivos = new ArrayList<>();
        Block bloque;                                   // el cultivo que se ve (null en un corral)
        Entity rotulo;
        boolean cosechando;
        int edad = -1;                                  // la etapa que se esta mostrando
    }

    Fincas(Cafe c) { this.c = c; }

    private Datos.Jugador d() { return c.dj(); }

    // ------------------------------------------------------------ construir y quitar

    /** Pone en el mundo todas las fincas del dueño (terreno, cultivo, animales y michigranjeros). */
    void construir() {
        quitar();
        if (d().granja.isEmpty()) return;
        terreno();
        for (Datos.Finca f : d().granja) { puerta(f); construirFinca(f); }
    }

    /**
     * El terreno de detras del muro del fondo de la parcela (0.26.0): suelo de hierba de lado a lado, el camino de justo detras del muro, un muro propio al fondo y a los lados,
     * y una puerta de 3 bloques en el muro de la parcela (y en el seto) delante de cada finca. El dueño no puede construir aqui: solo es de la granja.
     */
    private void terreno() {
        for (int x = c.ox + Ciudad.X0; x <= c.ox + Ciudad.X1; x++) for (int z = Granja.Z_MURO; z <= Granja.Z_CAMINO; z++) {
            boolean lado = x == c.ox + Ciudad.X0 || x == c.ox + Ciudad.X1;
            poner(x, -1, z, "dirt");
            poner(x, 0, z, z == Granja.Z_CAMINO ? "coarse_dirt" : "grass_block");
            for (int y = 1; y <= 9; y++) poner(x, y, z, "air");
            if (z == Granja.Z_MURO || lado) { poner(x, 1, z, "stone_bricks"); poner(x, 2, z, "stone_brick_slab[type=top]"); }
        }
    }

    /** La puerta de 3 bloques que une el jardin con la finca: abre el muro de la parcela y el seto, y pone un farolillo a cada lado. */
    private void puerta(Datos.Finca f) {
        int ax = ax(f);
        for (int dx = 3; dx <= 5; dx++) {
            poner(ax + dx, 1, Local.Z_ATRAS, "air"); poner(ax + dx, 2, Local.Z_ATRAS, "air");
            poner(ax + dx, 1, Local.Z_ATRAS + 1, "air");
            poner(ax + dx, 0, Local.Z_ATRAS, "coarse_dirt");
        }
        poner(ax + 2, 3, Local.Z_ATRAS, "lantern[hanging=false]");
        poner(ax + 6, 3, Local.Z_ATRAS, "lantern[hanging=false]");
    }

    /** Quita lo que se mueve (animales, michigranjeros y rotulos); el terreno se queda. */
    void quitar() {
        for (Vivo v : vivos.values()) quitarVivo(v);
        vivos.clear();
    }

    private void quitarVivo(Vivo v) {
        if (v.granjero != null) v.granjero.quitar();
        for (PokemonEntity e : v.animales) if (!e.isRemoved()) e.discard();
        Disp.quitar(v.rotulo);
    }

    private int ax(Datos.Finca f) { return c.ox + Granja.x0(f.slot); }

    private void poner(int x, int dy, int z, String b) { Obra.poner(c.w, x, Local.Y + dy, z, b); }

    private static Block bloque(String id) {
        Identifier i = Identifier.of(id);
        if (!Registries.BLOCK.containsId(i)) return null;
        Block b = Registries.BLOCK.get(i);
        return b == net.minecraft.block.Blocks.AIR ? null : b;
    }

    private void construirFinca(Datos.Finca f) {
        Granja.Cultivo cult = Granja.Cultivo.de(f.tipo);
        if (cult == null) return;
        Vivo v = new Vivo();
        v.f = f; v.cult = cult;
        int ax = ax(f);
        // el suelo y el vallado (un hueco en la valla del lado de la puerta, en el centro)
        for (int dx = 0; dx < Granja.ANCHO; dx++) for (int z = Granja.Z0; z <= Granja.Z1; z++) {
            boolean borde = dx == 0 || dx == Granja.ANCHO - 1 || z == Granja.Z0 || z == Granja.Z1;
            poner(ax + dx, -1, z, "dirt");
            poner(ax + dx, 0, z, borde ? "grass_block" : cult.tipo == Granja.Tipo.HUERTO ? "dirt" : "grass_block");
            for (int y = 1; y <= 3; y++) poner(ax + dx, y, z, "air");
            if (borde && !(z == Granja.Z1 && dx == 4)) poner(ax + dx, 1, z, "oak_fence");
        }
        poner(ax, 2, Granja.Z0, "lantern[hanging=false]"); poner(ax + Granja.ANCHO - 1, 2, Granja.Z0, "lantern[hanging=false]");
        poner(ax + 1, 2, Granja.Z1, "lantern[hanging=false]"); poner(ax + Granja.ANCHO - 2, 2, Granja.Z1, "lantern[hanging=false]");
        for (String id : cult.bloques) { v.bloque = bloque(id); if (v.bloque != null) break; }
        boolean arrozal = v.bloque != null && Registries.BLOCK.getId(v.bloque).getPath().equals("rice");
        if (cult == Granja.Cultivo.CACAO) {                                                      // troncos de jungla con cacao pegado por los cuatro lados, a dos alturas
            for (int dx = 1; dx <= 7; dx++) for (int z = Granja.Z0 + 1; z < Granja.Z1; z++) poner(ax + dx, 0, z, "grass_block");
            for (int px : new int[]{2, 6}) for (int pz : new int[]{Granja.Z0 + 2, Granja.Z0 + 5}) {
                poner(ax + px, 1, pz, "jungle_log"); poner(ax + px, 2, pz, "jungle_log"); poner(ax + px, 3, pz, "jungle_leaves[persistent=true]");
                for (int dy = 1; dy <= 2; dy++) for (Direction dir : Direction.Type.HORIZONTAL)
                    v.cultivos.add(new Celda(new BlockPos(ax + px - dir.getOffsetX(), Local.Y + dy, pz - dir.getOffsetZ()), dir));      // el tronco queda en la direccion «dir» desde el cacao
            }
        } else if (cult.tipo == Granja.Tipo.HUERTO && arrozal) {                                  // arrozal: todo agua y el arroz plantado en el agua
            for (int dx = 1; dx <= 7; dx++) for (int z = Granja.Z0 + 1; z < Granja.Z1; z++) { poner(ax + dx, 0, z, "water"); v.cultivos.add(new Celda(new BlockPos(ax + dx, Local.Y, z), null)); }
        } else if (cult.tipo == Granja.Tipo.HUERTO) {
            for (int dx = 1; dx <= 7; dx++) for (int z = Granja.Z0 + 1; z < Granja.Z1; z++) {
                if (dx == 4) { poner(ax + dx, 0, z, "water"); continue; }
                poner(ax + dx, 0, z, "farmland[moisture=7]");
                v.cultivos.add(new Celda(new BlockPos(ax + dx, Local.Y + 1, z), null));
            }
        } else if (cult == Granja.Cultivo.PESCADO) {                                              // estanque: todo agua, con los Magikarp dentro
            for (int dx = 1; dx <= 7; dx++) for (int z = Granja.Z0 + 1; z < Granja.Z1; z++) poner(ax + dx, 0, z, "water");
            poner(ax + 1, 1, Granja.Z0 + 1, "lily_pad"); poner(ax + 6, 1, Granja.Z0 + 3, "lily_pad"); poner(ax + 3, 1, Granja.Z1 - 2, "lily_pad");
        } else {
            poner(ax + 1, 1, Granja.Z0 + 1, "hay_block"); poner(ax + 7, 1, Granja.Z0 + 1, "hay_block"); poner(ax + 1, 2, Granja.Z0 + 1, "hay_block");
            poner(ax + 4, 1, Granja.Z0 + 1, "water_cauldron[level=3]");
            for (int dx = 1; dx <= 7; dx += 2) poner(ax + dx, 1, Granja.Z1 - 1, "short_grass");
        }
        // quita los carteles de texto que hubiera de antes (por si el servidor se paro sin avisar)
        for (Entity e : c.w.getEntitiesByType(EntityType.TEXT_DISPLAY, new Box(ax, Local.Y + 1, Granja.Z0, ax + Granja.ANCHO, Local.Y + 6, Granja.Z1 + 1), x -> x.getCommandTags().contains(Disp.TAG))) e.discard();
        vivos.put(f.slot, v);
        actualizarCultivos(v, Granja.progreso(f, System.currentTimeMillis()), true);
        if (cult.tipo == Granja.Tipo.CORRAL) animales(v);
        crearGranjero(v);
        etiqueta(v, Granja.progreso(f, System.currentTimeMillis()), System.currentTimeMillis());
    }

    private void animales(Vivo v) {
        int ax = ax(v.f), n = Granja.animales(v.f.nivel);
        List<double[]> puestos = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double x = 0, z = 0;
            for (int intento = 0; intento < 20; intento++) {                  // un sitio dentro del corral que no este pegado a otro animal
                x = ax + 1.5 + rnd.nextDouble() * 6.0; z = Granja.Z0 + 2.5 + rnd.nextDouble() * 5.0;
                boolean libre = true;
                for (double[] q : puestos) if (Math.hypot(q[0] - x, q[1] - z) < 1.4) libre = false;
                if (libre) break;
            }
            puestos.add(new double[]{x, z});
            try {
                PokemonEntity e = PokemonProperties.Companion.parse(v.cult.especie + " level=" + (8 + rnd.nextInt(10))).createEntity(c.w);
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
                boolean agua = v.cult == Granja.Cultivo.PESCADO;
                e.refreshPositionAndAngles(x, agua ? Local.Y + 0.35 : Local.Y + 1, z, rnd.nextFloat() * 360f, 0f);
                c.w.spawnEntity(e);
                e.getPokemon().setScaleModifier((float) Math.max(0.35, Math.min(3.0, 0.9 / Math.max(0.3, e.getHeight()))));
                if (agua) try { e.getDataTracker().set(PokemonEntity.getPOSE_TYPE(), PoseType.SWIM); } catch (Throwable t) { /* se queda como estaba */ }
                v.animales.add(e);
            } catch (Throwable t) {
                EmiCafe.LOG.error("No se pudo crear el animal {}", v.cult.especie, t);
            }
        }
    }

    private void crearGranjero(Vivo v) {
        Datos.Empleado emp = v.f.granjero;
        emp.rol = "granjero"; emp.nivel = v.f.nivel;
        double x = ax(v.f) + 4.5, z = Granja.Z1 + 1.5;
        Ayudante a = Ayudante.crear(c.w, emp, x, Local.Y + 1, z);
        if (a != null) { a.alSitio(); v.granjero = a; }
    }

    // ------------------------------------------------------------ crecimiento

    /** Pone el cultivo en la etapa que toca segun el avance (0 a 1). */
    private void actualizarCultivos(Vivo v, double prog, boolean forzar) {
        if (v.bloque == null || v.cosechando) return;
        IntProperty age = v.bloque.getStateManager().getProperty("age") instanceof IntProperty ip ? ip : null;
        int max = age == null ? 1 : Collections.max(age.getValues());
        int etapa = prog >= 1.0 ? max : (int) Math.floor(prog * max);
        if (!forzar && etapa == v.edad) return;
        v.edad = etapa;
        for (Celda ce : v.cultivos) ponerCultivo(v, ce, age, etapa);
    }

    private void ponerCultivo(Vivo v, Celda ce, IntProperty age, int etapa) {
        BlockState s = v.bloque.getDefaultState();
        if (age != null) s = s.with(age, Math.max(0, Math.min(Collections.max(age.getValues()), etapa)));
        if (ce.dir() != null && s.contains(net.minecraft.state.property.Properties.HORIZONTAL_FACING)) s = s.with(net.minecraft.state.property.Properties.HORIZONTAL_FACING, ce.dir());
        BlockPos p = ce.pos();
        if (c.w.getBlockState(p) != s) c.w.setBlockState(p, s, Obra.BANDERAS);
    }

    private void etiqueta(Vivo v, double prog, long ahora) {
        if (v.rotulo == null || v.rotulo.isRemoved()) v.rotulo = null;
        String linea2 = v.cosechando ? "Cosechando…" : prog >= 1.0 ? "¡Lista para cosechar!" : Math.round(prog * 100) + " % · " + Granja.CALIDAD[v.f.nivel];
        String json = "{\"text\":\"" + v.cult.nombre + "\",\"color\":\"gold\",\"bold\":true,\"extra\":[{\"text\":\"\\nNv " + v.f.nivel + " · " + linea2 + "\",\"color\":\"" + (prog >= 1.0 ? "green" : "gray") + "\",\"bold\":false}]}";
        double x = ax(v.f) + 4.5, y = Local.Y + 3.3, z = Granja.Z1 - 0.2;
        if (v.rotulo == null) v.rotulo = Disp.texto(c.w, json, x, y, z, 0.9f, false);
        else Disp.cambiarTexto(v.rotulo, json, x, y, z, 0.9f, false);
    }

    // ------------------------------------------------------------ el michigranjero

    private List<Ayudante> granjeros() {
        List<Ayudante> l = new ArrayList<>();
        for (Vivo v : vivos.values()) if (v.granjero != null && !v.granjero.e.isRemoved()) l.add(v.granjero);
        return l;
    }

    /** Cada tick: los michigranjeros andan; cada segundo se mira el crecimiento y, si esta listo y el michigranjero esta libre, sale a cosechar. */
    void tick(int t) {
        if (vivos.isEmpty()) return;
        List<Ayudante> gs = granjeros();
        for (Ayudante a : gs) a.mover(gs, t);
        if (t % 20 != 0) return;
        long ahora = System.currentTimeMillis();
        for (Vivo v : new ArrayList<>(vivos.values())) {
            double prog = Granja.progreso(v.f, ahora);
            if (!v.cosechando) {
                actualizarCultivos(v, prog, false);
                Ayudante a = v.granjero;
                if (prog >= 1.0 && a != null && !a.e.isRemoved() && !a.ocupado() && a.enCasa()) empezarCosecha(v);
            }
            if (t % 100 == 0) etiqueta(v, prog, ahora);
        }
    }

    private void empezarCosecha(Vivo v) {
        Ayudante a = v.granjero;
        v.cosechando = true;
        double ax = ax(v.f), z1 = Granja.Z1;
        double hx = a.hx, hz = a.hz;
        Runnable fin = () -> entregar(v);
        a.paso(ax + 4.5, z1 + 0.5, 4, null, null, ax + 4.5, z1 - 5);                                  // entra por el hueco de la valla
        if (v.cult.tipo == Granja.Tipo.HUERTO) {
            double zc = Granja.Z0 + 5.5, za = Granja.Z0 + 2.0;                                               // las dos filas de cultivo que recorre
            double[][] paradas = {{2.5, zc}, {2.5, za}, {6.5, za}, {6.5, zc}};
            for (int k = 0; k < paradas.length; k++) {
                final int q = k;
                a.paso(ax + paradas[k][0], paradas[k][1], a.t(36), () -> cosechando(a), () -> recoger(v, q), ax + paradas[k][0] + (k < 2 ? 1 : -1), paradas[k][1]);
            }
        } else {
            a.paso(ax + 4.5, Granja.Z0 + 4.0, a.t(80), () -> ordenando(a, v), null, ax + 4.5, Granja.Z0 + 2.0);
        }
        a.paso(ax + 4.5, z1 + 0.5, 4, null, () -> a.llevar(c.w, CafeItems.id(v.cult.ingrediente)), hx, hz + 5);
        a.paso(hx, hz, a.t(14), null, fin, hx, hz + 5);
    }

    private void cosechando(Ayudante a) {
        c.sonido(a.e.getX(), a.e.getY(), a.e.getZ(), SoundEvents.BLOCK_GRASS_BREAK, 0.6f, 1.2f);
        c.w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, a.e.getX(), a.e.getY() + 0.8, a.e.getZ(), 4, 0.4, 0.3, 0.4, 0.0);
    }

    private void ordenando(Ayudante a, Vivo v) {
        c.sonido(a.e.getX(), a.e.getY(), a.e.getZ(), v.cult == Granja.Cultivo.ABEJAS ? SoundEvents.BLOCK_BEEHIVE_WORK : SoundEvents.ENTITY_COW_MILK, 0.5f, 1.2f);
        c.w.spawnParticles(ParticleTypes.HEART, a.e.getX(), a.e.getY() + 1.4, a.e.getZ(), 2, 0.5, 0.3, 0.5, 0.0);
    }

    /** El michigranjero acaba una parada del huerto: deja a cero los cultivos de ese cuarto del huerto. */
    private void recoger(Vivo v, int cuarto) {
        IntProperty age = v.bloque == null ? null : v.bloque.getStateManager().getProperty("age") instanceof IntProperty ip ? ip : null;
        int ax = ax(v.f);
        for (Celda ce : v.cultivos) {
            boolean izq = ce.pos().getX() - ax < 4, atras = ce.pos().getZ() < Granja.Z0 + 4;
            boolean en = switch (cuarto) { case 0 -> izq && !atras; case 1 -> izq && atras; case 2 -> !izq && atras; default -> !izq && !atras; };
            if (en && v.bloque != null) ponerCultivo(v, ce, age, 0);
        }
    }

    /** Llega con la cosecha: la guarda en la despensa y empieza otro ciclo. */
    private void entregar(Vivo v) {
        Ayudante a = v.granjero;
        Datos.Jugador d = d();
        int unidades = Granja.rendimiento(v.cult, v.f.nivel);
        int entra = Granja.guardar(d, v.cult.ingrediente, v.f.nivel, unidades);
        v.f.desde = System.currentTimeMillis();
        v.f.cosechas++;
        d.granjaCosechado += entra;
        v.cosechando = false;
        v.edad = -1;
        if (a != null) a.llevar(c.w, null);
        actualizarCultivos(v, 0, true);
        etiqueta(v, 0, v.f.desde);
        EmiCafe.datos.guardar(c.duenio);
        ServerPlayerEntity dueno = c.jugador(c.duenio);
        if (a != null) {
            c.w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, a.e.getX(), a.e.getY() + 1.0, a.e.getZ(), 8, 0.4, 0.4, 0.4, 0.0);
            c.sonido(a.e.getX(), a.e.getY(), a.e.getZ(), SoundEvents.ENTITY_ITEM_PICKUP, 0.7f, 1.0f);
        }
        if (dueno != null) {
            String que = entra + " × " + CafeItems.nombre(v.cult.ingrediente) + " (" + Granja.CALIDAD[v.f.nivel] + ")";
            dueno.sendMessage(Text.literal("🌾 " + v.f.granjero.nombre + " dejó en la despensa: " + que + (entra < unidades ? " — la despensa está llena, " + (unidades - entra) + " se perdieron" : "")).formatted(Formatting.GREEN), false);
        }
        if (EmiCafe.LOG.isDebugEnabled() || Boolean.getBoolean("emi.cafe.debug")) EmiCafe.LOG.info("[granja] {} cosechó {} de {} (calidad {})", v.f.granjero.nombre, entra, v.cult.id, v.f.nivel);
    }

    // ------------------------------------------------------------ acciones del dueño

    private String comprobarDuenio(ServerPlayerEntity p) {
        if (!c.esDuenio(p)) return "Solo el dueño maneja la granja.";
        if (c.construyendo) return "Sal del modo construcción para manejar la granja.";
        return null;
    }

    /** Compra un huerto o corral nuevo (viene con su michigranjero). Devuelve el error, o null. */
    String comprar(ServerPlayerEntity p, String id) {
        String err = comprobarDuenio(p);
        if (err != null) return err;
        Granja.Cultivo cult = Granja.Cultivo.de(id);
        if (cult == null) return "Eso no se puede criar.";
        Datos.Jugador d = d();
        if (c.nivelLocal() < Granja.NIVEL_LOCAL) return "La granja necesita un local de nivel " + Granja.NIVEL_LOCAL + ".";
        int max = Granja.fincasMax(c.nivelLocal());
        if (d.granja.size() >= max) return "Tu local de nivel " + c.nivelLocal() + " admite " + max + " huertos y corrales" + (max < Granja.MAX_FINCAS ? " (uno más por cada nivel de local)." : ".");
        for (Datos.Finca f : d.granja) if (f.tipo.equals(cult.id)) return "Ya tienes ese " + (cult.tipo == Granja.Tipo.HUERTO ? "huerto" : "corral") + ".";
        if (d.monedas < cult.precio) return "Te faltan monedas (" + cult.precio + ").";
        int slot = Granja.sitioLibre(d, max);
        if (slot < 0) return "No queda sitio en la granja.";
        d.monedas -= cult.precio;
        crear(p, d, cult, slot);
        return null;
    }

    /** Solo pruebas: pone una finca gratis y sin limites en el primer sitio libre (hasta 6). */
    String darGratis(ServerPlayerEntity p, String id) {
        Granja.Cultivo cult = Granja.Cultivo.de(id);
        if (cult == null) return "Eso no se puede criar.";
        int slot = Granja.sitioLibre(d(), Granja.MAX_FINCAS);
        if (slot < 0) return "No queda sitio.";
        crear(p, d(), cult, slot);
        return null;
    }

    private void crear(ServerPlayerEntity p, Datos.Jugador d, Granja.Cultivo cult, int slot) {
        Datos.Finca f = new Datos.Finca();
        f.tipo = cult.id; f.slot = slot; f.nivel = 1; f.desde = System.currentTimeMillis();
        f.granjero.rol = "granjero"; f.granjero.nombre = Personal.nombreNuevo(todosLosNombres(d)); f.granjero.aspecto = 6; f.granjero.nivel = 1;
        d.granja.add(f);
        EmiCafe.datos.guardar(c.duenio);
        if (vivos.isEmpty()) terreno();                                                         // la primera finca crea el terreno de detras del muro (no se rehace con las demas)
        puerta(f);
        construirFinca(f);
        p.sendMessage(Msg.say("¡Compraste «" + cult.nombre + "»! Viene con su michigranjero " + f.granjero.nombre + ", que cosecha solo y lo deja en la despensa. Ponle nombre en la pestaña GRANJA (botón NOMBRE) o con /cafe granja nombre " + (slot + 1) + " <nombre>."), false);
        Sonidos.jugador(p, "subir", 1f, 1.1f);
    }

    private static List<Datos.Empleado> todosLosNombres(Datos.Jugador d) {
        List<Datos.Empleado> l = new ArrayList<>(d.personal);
        for (Datos.Finca f : d.granja) l.add(f.granjero);
        return l;
    }

    /** Sube de nivel un huerto o corral (mejor calidad y, en los corrales, un animal mas). */
    String mejorar(ServerPlayerEntity p, int slot) {
        String err = comprobarDuenio(p);
        if (err != null) return err;
        Datos.Jugador d = d();
        Datos.Finca f = Granja.finca(d, slot);
        if (f == null) return "Ese huerto no existe.";
        Granja.Cultivo cult = Granja.Cultivo.de(f.tipo);
        if (f.nivel >= Granja.NIVEL_MAX) return "Ya está al nivel máximo.";
        int coste = Granja.costeMejora(cult, f.nivel);
        if (d.monedas < coste) return "Te faltan monedas (" + coste + ").";
        d.monedas -= coste;
        f.nivel++;
        f.granjero.nivel = f.nivel;
        EmiCafe.datos.guardar(c.duenio);
        Vivo v = vivos.remove(slot);
        if (v != null) quitarVivo(v);
        construirFinca(f);
        p.sendMessage(Msg.say(cult.nombre + " sube al nivel " + f.nivel + " (" + Granja.CALIDAD[f.nivel] + "): sus ingredientes pagan un " + Granja.bonusPct(f.nivel) + " % más en el plato que los lleva"
                + (cult.tipo == Granja.Tipo.CORRAL ? " y ahora hay " + Granja.animales(f.nivel) + " animales" : "") + "."), false);
        Sonidos.jugador(p, "subir", 1f, 1.1f);
        return null;
    }

    String renombrar(ServerPlayerEntity p, int slot, String nombre) {
        if (!c.esDuenio(p)) return "Solo el dueño puede poner nombres.";
        Datos.Finca f = Granja.finca(d(), slot);
        if (f == null) return "Ese huerto no existe.";
        String n = Personal.limpiar(nombre, 16);
        if (n.isEmpty()) return "Escribe un nombre.";
        f.granjero.nombre = n;
        EmiCafe.datos.guardar(c.duenio);
        reponerGranjero(f);
        p.sendMessage(Msg.say("Ahora se llama " + n + "."), false);
        return null;
    }

    String cambiarAspecto(ServerPlayerEntity p, int slot) {
        if (!c.esDuenio(p)) return "Solo el dueño puede cambiar aspectos.";
        Datos.Finca f = Granja.finca(d(), slot);
        if (f == null) return "Ese huerto no existe.";
        f.granjero.aspecto = (f.granjero.aspecto + 1) % Personal.ASPECTOS.length;
        EmiCafe.datos.guardar(c.duenio);
        reponerGranjero(f);
        return null;
    }

    private void reponerGranjero(Datos.Finca f) {
        Vivo v = vivos.get(f.slot);
        if (v == null) return;
        boolean cosechando = v.cosechando;
        if (v.granjero != null) v.granjero.quitar();
        v.granjero = null;
        v.cosechando = false;
        crearGranjero(v);
        if (cosechando) v.edad = -1;                                                  // la cosecha se interrumpe: los cultivos se vuelven a poner segun el avance
    }

    /** Solo pruebas: deja una finca lista para cosechar ya (null = todas). */
    void listaYa(Integer slot) {
        for (Datos.Finca f : d().granja) if (slot == null || f.slot == slot) f.desde = 0;
    }

    /** Solo pruebas: cuantas fincas hay en el mundo y cuantas cosechando. */
    String estado() {
        int cos = 0;
        for (Vivo v : vivos.values()) if (v.cosechando) cos++;
        return vivos.size() + " fincas, " + cos + " cosechando";
    }
}
