package emi.fall;

import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * «Guerra de bombas»: dos equipos (rojo y azul) en una plataforma redonda. Se cogen bombas de los puntos del suelo y se lanzan al rival con el boton derecho: al caer
 * explotan, rompen el suelo y lanzan por el aire a quien esta cerca. Quien cae al vacio queda eliminado. Gana el equipo que se queda con jugadores; despues de 90 s el
 * borde de la plataforma se va comiendo hacia dentro.
 */
final class MiniBombas implements Mini {
    static final int ZC = 600;
    static final double R = 14.5;
    static final int LIMITE_S = 150, ENCOGER_DESDE_S = 70;
    static final double RADIO_EXPLOSION = 3.3, RADIO_EMPUJE = 6.5;

    /** Los sitios donde aparecen las bombas (x, z relativos al centro). */
    private static final double[][] PUNTOS = {{0, -9}, {0, 0}, {0, 9}, {-8, -4}, {-8, 4}, {8, -4}, {8, 4}};

    private static final class Punto { double x, z; Entity e; long listoEn; boolean hay; }
    private static final class Bomba { Entity e; Vec3d p, v; int edad, aterrizo = -1; Jug lanza; }
    /** La bomba que vuela desde el suelo hasta encima de la cabeza del jugador que la coge. */
    private static final class Vuelo { Entity e; Vec3d desde; Jug j; int t; }
    /** Un trozo de suelo que salta por los aires al explotar (solo se ve). */
    private static final class Escombro { Entity e; Vec3d p, v; int edad; }

    /** El numero de modelo del objeto de la bomba: el resource pack del mod lo dibuja como un TNT en la barra pero no en la mano. */
    static final int MODELO_BOMBA = 7771;

    private final List<Punto> puntos = new ArrayList<>();
    private final List<Bomba> bombas = new ArrayList<>();
    private final List<Vuelo> vuelos = new ArrayList<>();
    private final List<Escombro> escombros = new ArrayList<>();
    private final Map<Jug, Entity> portadas = new HashMap<>();             // la bomba que cada jugador lleva encima de la cabeza
    private final Map<Long, String> original = new HashMap<>();           // el bloque del suelo de cada casilla (para saber si sigue en pie)
    private int ganador = -2;                                              // equipo ganador (-1 = empate)
    private double radioActual = R;
    private final int[] caidasEq = new int[2];                            // modo sin eliminaciones: cuantas veces ha caido cada equipo
    private int limiteS = LIMITE_S;


    @Override public void ponerDeco(ServerWorld w) { decorar(w); }

    static void decorar(ServerWorld w) {
        if (!Deco.pendiente(w, ZC)) return;
        int S = Minijuegos.SUELO;
        Random r = new Random(6);
        Deco.anilloFarolas(w, 0, ZC, 19, 16, S + 1, S - 12);
        for (int i = 0; i < 6; i++) { double a = 2 * Math.PI * i / 6 + 0.3; Deco.arbolSeco(w, (int) Math.round(Math.cos(a) * 25), S - 14, ZC + (int) Math.round(Math.sin(a) * 25), 16, r); }
        Deco.michi(w, -13, S - 4, ZC - 36, 'x', 2);
        Deco.luna(w, 26, S + 30, ZC + 24, 9, 'z');
        Deco.pedestal(w, 0, S + 8, ZC - 24, "gigante_gatitoalien_devil_horns", "s");
        Deco.pedestal(w, 0, S + 8, ZC + 24, "gigante_gatitoalien_halo", "n");
        Deco.marcar(w, ZC);
    }

    @Override public double[] zona() { return new double[]{0, ZC, 40}; }
    @Override public String id() { return "bombas"; }
    @Override public String nombre() { return "Guerra de bombas"; }
    @Override public boolean equipos() { return true; }
    @Override public String regla() { return "Rojos contra azules. Pasa por una bomba para cogerla (la llevas encima), lánzala con clic derecho y deja sin suelo al rival. El que cae queda eliminado."; }
    @Override public String[] pasos() {
        return new String[]{"Pasa por encima de un TNT del suelo para cogerlo: lo llevas sobre la cabeza.",
                "Clic derecho para lanzarlo: al caer explota, rompe el suelo y manda por los aires a quien esté cerca.",
                "Gana el equipo (rojo o azul) que tira más rivales al vacío. ¡No te caigas tú!"};
    }
    @Override public String objetivo() { return "Tira a los rivales al vacío"; }
    @Override public String musica() { return "tension"; }
    @Override public int vivo(Ctx c, Jug j) { return j.cuenta; }
    @Override public String unidad() { return "derribos"; }

    private static long clave(int x, int z) { return ((long) x << 32) ^ (z & 0xffffffffL); }

    @Override
    public void construir(ServerWorld w) {
        BlockPos marca = new BlockPos(0, -64, ZC);
        if (w.getBlockState(marca).isOf(Blocks.CRYING_OBSIDIAN)) return;
        // el suelo se pone en empezar() (cambia con las explosiones); aqui solo lo que no cambia
        int S = Minijuegos.SUELO;
        Disp.texto(w, "{\"text\":\"GUERRA DE BOMBAS\",\"color\":\"red\",\"bold\":true}", 0.5, S + 9, ZC + 0.5, 6f, false);
        Obra.poner(w, 0, -64, ZC, "crying_obsidian");
    }

    private void ponerSuelo(ServerWorld w) {
        int S = Minijuegos.SUELO;
        int n = (int) Math.ceil(R) + 1;
        for (int dx = -n; dx <= n; dx++) for (int dz = -n; dz <= n; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > R) { Obra.poner(w, dx, S, ZC + dz, "air"); continue; }
            String b = Math.abs(dx) <= 1 ? "white_concrete" : dx < 0 ? (((dx + dz) & 1) == 0 ? "red_concrete" : "orange_concrete") : (((dx + dz) & 1) == 0 ? "blue_concrete" : "light_blue_concrete");
            Obra.poner(w, dx, S, ZC + dz, b);
            original.put(clave(dx, dz), b);
        }
    }

    @Override
    public void empezar(Ctx c) {
        ganador = -2; radioActual = R;
        caidasEq[0] = caidasEq[1] = 0;
        limiteS = c.sinElim() ? 100 : LIMITE_S;
        for (Jug j : c.jugs()) { j.caidas = 0; j.cuenta = 0; }
        original.clear();
        c.forzar(-18, ZC - 18, 18, ZC + 18);
        Disp.barrerMoviles(c.w, 0, ZC, 40);
        quitarEntidades();
        ponerSuelo(c.w);
        int S = Minijuegos.SUELO;
        for (double[] p : PUNTOS) {
            Punto pt = new Punto(); pt.x = p[0] + 0.5; pt.z = ZC + p[1] + 0.5; pt.hay = true;
            pt.e = Disp.bloque(c.w, "minecraft:tnt", pt.x, S + 2.0, pt.z, 0.8f, 0.8f, 0.8f, 0, 0);
            puntos.add(pt);
        }
        int[] cuenta = new int[2];
        for (Jug j : c.jugs()) {
            int e = j.equipo < 0 ? 0 : j.equipo, n = cuenta[e]++;
            double x = e == 0 ? -11 + (n % 2) * 2 : 11 - (n % 2) * 2, z = ZC - 6 + (n / 2) * 2.5;
            c.colocar(j, x + 0.5, S + 1, z + 0.5, e == 0 ? -90f : 90f);
            j.bomba = false;
        }
    }

    private void quitarEntidades() {
        for (Punto p : puntos) Disp.quitar(p.e);
        puntos.clear();
        for (Bomba b : bombas) Disp.quitar(b.e);
        bombas.clear();
        for (Vuelo v : vuelos) Disp.quitar(v.e);
        vuelos.clear();
        for (Escombro e : escombros) Disp.quitar(e.e);
        escombros.clear();
        for (Entity r : portadas.values()) Disp.quitar(r);
        portadas.clear();
    }

    private void quitarPortada(Jug j) {
        Entity r = portadas.remove(j);
        Disp.quitar(r);
    }

    private boolean enVuelo(Jug j) { for (Vuelo v : vuelos) if (v.j == j) return true; return false; }

    /** Termina de coger la bomba: queda montada encima de la cabeza (la ven todos) y el jugador tiene en la mano un objeto invisible para poder hacer clic derecho. */
    private void darBomba(Ctx c, Jug j, ServerPlayerEntity p) {
        j.bomba = true;
        quitarPortada(j);
        Entity r = Disp.bloque(c.w, "minecraft:tnt", p.getX(), p.getY() + 1.9, p.getZ(), 0.9f, 0.9f, 0.9f, 0, 0);
        if (r != null) portadas.put(j, r);                                  // te sigue cada tick (se mueve en tick())
        for (int i = 0; i < 9; i++) p.getInventory().setStack(i, objetoBomba());          // en toda la barra: asi el clic derecho funciona sea cual sea la ranura elegida
        p.playSoundToPlayer(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.MASTER, 1f, 0.8f);
        c.w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.PLAYERS, 0.6f, 1.4f);
        p.sendMessage(Text.literal("¡Tienes una bomba encima! Clic derecho para lanzarla.").formatted(Formatting.RED), true);
    }

    @Override public void liberar(Ctx c) { }

    private ItemStack objetoBomba() {
        ItemStack st = new ItemStack(Items.GUNPOWDER);                                      // se ve como TNT en la barra y en la mano no se ve (resource pack del mod)
        st.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(MODELO_BOMBA));
        st.set(DataComponentTypes.CUSTOM_NAME, Text.literal("BOMBA · clic derecho para lanzarla").formatted(Formatting.RED, Formatting.BOLD));
        return st;
    }

    @Override
    public boolean alUsar(Ctx c, Jug j, ServerPlayerEntity p) {
        if (!j.bomba || j.fuera) return false;
        j.bomba = false;
        quitarPortada(j);
        p.getInventory().clear();
        Vec3d mira = p.getRotationVector();
        Bomba b = new Bomba();
        b.p = p.getEyePos().add(mira.multiply(0.9));
        b.v = mira.multiply(1.4).add(0, 0.08, 0);
        b.lanza = j;
        b.e = Disp.bloque(c.w, "minecraft:tnt", b.p.x, b.p.y, b.p.z, 0.8f, 0.8f, 0.8f, 0, 0);
        bombas.add(b);
        p.swingHand(Hand.MAIN_HAND, true);                                                     // se ve el brazo lanzando
        c.w.playSound(null, b.p.x, b.p.y, b.p.z, SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 1f, 0.7f);
        c.w.playSound(null, b.p.x, b.p.y, b.p.z, SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.PLAYERS, 1f, 1f);
        EmiFall.LOG.info("[bombas] {} lanza una bomba hacia ({}, {})", j.nombre, Math.round(mira.x * 100) / 100.0, Math.round(mira.z * 100) / 100.0);
        return true;
    }

    private boolean hayBloque(ServerWorld w, double x, double z) {
        int S = Minijuegos.SUELO;
        return !w.getBlockState(new BlockPos((int) Math.floor(x), S, (int) Math.floor(z))).isAir();
    }

    private void explotar(Ctx c, Vec3d en, Jug lanza) {
        ServerWorld w = c.w;
        int S = Minijuegos.SUELO;
        int quitados = 0, ex = (int) Math.floor(en.x), ez = (int) Math.floor(en.z), r = (int) Math.ceil(RADIO_EXPLOSION);
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > RADIO_EXPLOSION - 0.3 + c.rnd.nextDouble() * 0.6) continue;
            BlockPos bp = new BlockPos(ex + dx, S, ez + dz);
            if (!w.getBlockState(bp).isAir()) {
                if (escombros.size() < 40 && c.rnd.nextInt(3) == 0) {                          // un trozo de suelo que salta
                    Escombro es = new Escombro();
                    es.p = new Vec3d(bp.getX() + 0.5, S + 0.8, bp.getZ() + 0.5);
                    double d2 = Math.max(0.5, d);
                    es.v = new Vec3d(dx / d2 * (0.2 + c.rnd.nextDouble() * 0.35), 0.45 + c.rnd.nextDouble() * 0.4, dz / d2 * (0.2 + c.rnd.nextDouble() * 0.35));
                    es.e = Disp.bloque(w, "minecraft:" + Registries.BLOCK.getId(w.getBlockState(bp).getBlock()).getPath(), es.p.x, es.p.y, es.p.z, 0.55f, 0.55f, 0.55f, 0, 0);
                    escombros.add(es);
                }
                Obra.poner(w, bp.getX(), S, bp.getZ(), "air"); quitados++;
            }
        }
        w.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, en.x, S + 1.2, en.z, 1, 0, 0, 0, 0);
        w.spawnParticles(ParticleTypes.CLOUD, en.x, S + 1.2, en.z, 30, 1.6, 0.4, 1.6, 0.1);
        w.spawnParticles(ParticleTypes.EXPLOSION, en.x, S + 1.3, en.z, 14, 1.8, 0.5, 1.8, 0.0);
        w.spawnParticles(ParticleTypes.FLAME, en.x, S + 1.3, en.z, 40, 1.2, 0.5, 1.2, 0.12);
        w.spawnParticles(ParticleTypes.LARGE_SMOKE, en.x, S + 1.5, en.z, 24, 1.4, 0.7, 1.4, 0.05);
        w.playSound(null, en.x, S + 1, en.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 3.0f, 0.9f);
        EmiFall.LOG.info("[bombas] explosion en ({}, {}): {} bloques menos", Math.round(en.x * 10) / 10.0, Math.round(en.z * 10) / 10.0, quitados);
        for (Jug j : c.jugs()) {
            ServerPlayerEntity p = c.pl(j);
            if (p == null || j.fuera) continue;
            double dx = p.getX() - en.x, dz = p.getZ() - en.z, d = Math.sqrt(dx * dx + dz * dz);
            if (d > RADIO_EMPUJE || Math.abs(p.getY() - S - 1) > 4) continue;
            double f = 1.0 - d / RADIO_EMPUJE, n = Math.max(0.3, d);
            c.empujar(p, new Vec3d(dx / n * (0.55 + 1.35 * f), 0.35 + 0.55 * f, dz / n * (0.55 + 1.35 * f)));
            j.ultimoTocado = lanza == null ? -1 : c.jugs().indexOf(lanza);
        }
    }

    @Override
    public boolean tick(Ctx c) {
        int S = Minijuegos.SUELO;
        // las bombas en el suelo giran y vuelven a salir
        for (Punto pt : puntos) {
            if (pt.hay) { Disp.moverBloque(pt.e, pt.x, S + 2.0 + 0.15 * Math.sin(c.tick / 6.0), pt.z, 0.8f, 0.8f, 0.8f, c.tick / 12.0, 2); }
            else if (c.tick >= pt.listoEn && hayBloque(c.w, pt.x, pt.z)) {
                pt.hay = true; pt.e = Disp.bloque(c.w, "minecraft:tnt", pt.x, S + 2.0, pt.z, 0.8f, 0.8f, 0.8f, 0, 0);
            }
        }
        // las bombas lanzadas: vuelan echando humo; al caer al suelo parpadean 7 ticks y explotan (si tocan a alguien explotan en el acto)
        for (int i = bombas.size() - 1; i >= 0; i--) {
            Bomba b = bombas.get(i);
            b.edad++;
            Vec3d nuevo = b.p;
            boolean boom = false;
            if (b.aterrizo < 0) {
                b.v = b.v.add(0, -0.03, 0);
                nuevo = b.p.add(b.v);
                if (nuevo.y <= S + 1.0 && b.v.y < 0 && hayBloque(c.w, nuevo.x, nuevo.z)) {
                    b.aterrizo = b.edad; b.v = Vec3d.ZERO; nuevo = new Vec3d(nuevo.x, S + 1.0, nuevo.z);
                    c.w.playSound(null, nuevo.x, nuevo.y, nuevo.z, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 1.2f, 0.6f);
                }
            }
            if (b.aterrizo >= 0 && b.edad - b.aterrizo >= 7) boom = true;
            if (!boom) for (Jug j : c.jugs()) {
                ServerPlayerEntity p = c.pl(j);
                if (p == null || j.fuera || j == b.lanza && b.edad < 8) continue;
                if (Math.abs(p.getX() - nuevo.x) < 1.1 && Math.abs(p.getZ() - nuevo.z) < 1.1 && nuevo.y > p.getY() - 0.5 && nuevo.y < p.getY() + 2.3) { boom = true; break; }
            }
            if (boom) { explotar(c, new Vec3d(nuevo.x, Math.min(nuevo.y, S + 1.0), nuevo.z), b.lanza); Disp.quitar(b.e); bombas.remove(i); continue; }
            if (nuevo.y < S - 12 || b.edad > 100) { Disp.quitar(b.e); bombas.remove(i); continue; }
            b.p = nuevo;
            boolean aterrizada = b.aterrizo >= 0;
            boolean blanco = aterrizada ? ((b.edad - b.aterrizo) / 2) % 2 == 1 : (b.edad / 5) % 2 == 1 && b.edad > 8;
            float tam = aterrizada ? 0.8f + 0.05f * (b.edad - b.aterrizo) : 0.8f;
            double cy = aterrizada ? S + 1.0 + tam / 2 : nuevo.y;
            Disp.cambiarBloque(b.e, blanco ? "minecraft:white_concrete" : "minecraft:tnt", nuevo.x, cy, nuevo.z, tam, tam, tam, aterrizada ? 0 : b.edad * 0.5, 0);
            if (!aterrizada) c.w.spawnParticles(ParticleTypes.SMOKE, nuevo.x, nuevo.y + 0.5, nuevo.z, 2, 0.06, 0.06, 0.06, 0.0);
            if (b.edad % 2 == 0) c.w.spawnParticles(ParticleTypes.FLAME, nuevo.x, cy + tam / 2 + 0.1, nuevo.z, 1, 0.03, 0.03, 0.03, 0.0);
        }
        // los trozos de suelo que saltan
        for (int i = escombros.size() - 1; i >= 0; i--) {
            Escombro e = escombros.get(i);
            e.edad++;
            e.v = e.v.add(0, -0.06, 0);
            e.p = e.p.add(e.v);
            if (e.edad > 24 || e.p.y < S - 6) { Disp.quitar(e.e); escombros.remove(i); continue; }
            Disp.moverBloque(e.e, e.p.x, e.p.y, e.p.z, 0.55f, 0.55f, 0.55f, e.edad * 0.4, 0);
        }
        // la bomba que llevas encima de la cabeza: te sigue
        for (java.util.Iterator<Map.Entry<Jug, Entity>> it = portadas.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Jug, Entity> en = it.next();
            ServerPlayerEntity p = c.pl(en.getKey());
            if (p == null || en.getKey().fuera || en.getValue() == null || en.getValue().isRemoved()) { Disp.quitar(en.getValue()); it.remove(); continue; }
            Disp.moverBloque(en.getValue(), p.getX(), p.getY() + 1.9 + 0.08 * Math.sin(c.tick / 5.0), p.getZ(), 0.9f, 0.9f, 0.9f, c.tick * 0.1, 0);
        }
        // las bombas que vuelan desde el suelo hasta encima de la cabeza de quien las coge
        for (int i = vuelos.size() - 1; i >= 0; i--) {
            Vuelo v = vuelos.get(i);
            v.t++;
            ServerPlayerEntity p = c.pl(v.j);
            if (p == null || v.j.fuera) { Disp.quitar(v.e); vuelos.remove(i); continue; }
            double k = Math.min(1.0, v.t / 8.0), suave = k * k * (3 - 2 * k);
            Vec3d meta = new Vec3d(p.getX(), p.getY() + 2.3, p.getZ());
            Vec3d pos = v.desde.lerp(meta, suave).add(0, Math.sin(k * Math.PI) * 0.9, 0);
            Disp.moverBloque(v.e, pos.x, pos.y, pos.z, 0.8f, 0.8f, 0.8f, v.t * 0.7, 0);
            if (v.t >= 8) { Disp.quitar(v.e); vuelos.remove(i); darBomba(c, v.j, p); }
        }
        // recoger bombas
        for (Jug j : c.jugs()) {
            ServerPlayerEntity p = c.pl(j);
            if (p == null || j.fuera || j.bomba || enVuelo(j)) continue;
            for (Punto pt : puntos) {
                if (!pt.hay) continue;
                if (Math.abs(p.getX() - pt.x) < 1.5 && Math.abs(p.getZ() - pt.z) < 1.5 && p.getY() < S + 3) {
                    pt.hay = false; pt.listoEn = c.tick + 140;
                    Vuelo v = new Vuelo(); v.e = pt.e; v.desde = new Vec3d(pt.x, S + 2.0, pt.z); v.j = j; pt.e = null;     // la bomba del suelo sube hasta tu cabeza
                    vuelos.add(v);
                    EmiFall.LOG.info("[bombas] {} coge una bomba", j.nombre);
                    c.w.spawnParticles(ParticleTypes.POOF, pt.x, S + 1.6, pt.z, 8, 0.3, 0.3, 0.3, 0.03);
                    break;
                }
            }
        }
        // el borde se va comiendo cuando pasa mucho tiempo
        if (c.tick >= Torneo.tt(ENCOGER_DESDE_S * 20) && c.tick % 20 == 0) {
            radioActual = Math.max(3.5, radioActual - 0.55);
            int n = (int) Math.ceil(R) + 1;
            for (int dx = -n; dx <= n; dx++) for (int dz = -n; dz <= n; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > radioActual && d <= radioActual + 1.6 && hayBloque(c.w, dx + 0.5, ZC + dz + 0.5)) Obra.poner(c.w, dx, S, ZC + dz, "air");
            }
        }
        // los que caen quedan eliminados
        int[] vivos = new int[2];
        for (Jug j : c.jugs()) {
            ServerPlayerEntity p = c.pl(j);
            if (p == null) continue;
            if (!j.fuera && p.getY() < S - 3) {
                j.bomba = false;
                quitarPortada(j);
                p.getInventory().clear();
                Sonido.sfx(p, "caida", 0.9f, 1.0f);
                if (c.sinElim()) {                                          // sin eliminaciones: cuenta como derribo del otro equipo y vuelves a tu lado
                    j.caidas++;
                    caidasEq[j.equipo < 0 ? 0 : j.equipo]++;
                    for (Jug o : c.jugs()) if ((o.equipo < 0 ? 0 : o.equipo) != (j.equipo < 0 ? 0 : j.equipo)) o.cuenta++;
                    c.recuperar(j, j.sx, j.sy + 0.5, j.sz, j.syaw);
                    c.avisar(Text.literal("↓ " + j.nombre + " ha caído (vuelve al juego)").formatted(j.equipo == 0 ? Formatting.RED : Formatting.AQUA));
                    Msg.title(p, Text.literal("¡CAÍSTE!").formatted(Formatting.RED, Formatting.BOLD), Text.literal("Vuelves a tu lado: sigue jugando").formatted(Formatting.GRAY), 3, 30, 10);
                } else {
                    c.eliminar(j);
                    for (Jug o : c.jugs()) if ((o.equipo < 0 ? 0 : o.equipo) != (j.equipo < 0 ? 0 : j.equipo)) o.cuenta++;
                    c.avisar(Text.literal("☠ " + j.nombre + " ha caído").formatted(j.equipo == 0 ? Formatting.RED : Formatting.AQUA));
                    Msg.title(p, Text.literal("¡ELIMINADO!").formatted(Formatting.RED, Formatting.BOLD), Text.literal("Mira cómo acaban los demás").formatted(Formatting.GRAY), 3, 40, 10);
                }
            }
            if (!j.fuera) vivos[j.equipo < 0 ? 0 : j.equipo]++;
        }
        c.t.barra(1f - (float) c.tick / Torneo.tt(limiteS * 20));
        int conectados0 = 0, conectados1 = 0;
        for (Jug j : c.jugs()) { if (j.equipo == 1) conectados1++; else conectados0++; }
        boolean fin = c.tick >= Torneo.tt(limiteS * 20) || (!c.sinElim() && ((vivos[0] == 0 && conectados0 > 0) || (vivos[1] == 0 && conectados1 > 0) || (vivos[0] == 0 && vivos[1] == 0)));
        if (fin) {
            if (c.sinElim()) ganador = caidasEq[0] < caidasEq[1] ? 0 : caidasEq[1] < caidasEq[0] ? 1 : -1;      // gana el que menos ha caido
            else ganador = vivos[0] > vivos[1] ? 0 : vivos[1] > vivos[0] ? 1 : -1;
        }
        return fin;
    }

    @Override
    public List<Jug> clasificacion(Ctx c) {
        List<Jug> l = c.jugs();
        l.sort((a, b) -> {
            int pa = (a.equipo == ganador ? 100000 : 0) + (a.fuera ? a.ordenFuera : 50000) - a.caidas * 100;
            int pb = (b.equipo == ganador ? 100000 : 0) + (b.fuera ? b.ordenFuera : 50000) - b.caidas * 100;
            return Integer.compare(pb, pa);
        });
        return l;
    }

    @Override
    public Map<Jug, Integer> extra(Ctx c) {
        Map<Jug, Integer> m = new HashMap<>();
        if (ganador >= 0) for (Jug j : c.jugs()) if (j.equipo == ganador) m.put(j, 4);
        return m;
    }

    @Override
    public void limpiar(Ctx c) {
        quitarEntidades();
        for (Jug j : c.jugs()) { j.bomba = false; ServerPlayerEntity p = c.pl(j); if (p != null) p.getInventory().clear(); }
    }

    @Override
    public String hud(Ctx c, Jug j) {
        int a = 0, b = 0;
        for (Jug o : c.jugs()) if (!o.fuera) { if (o.equipo == 1) b++; else a++; }
        int resta = Math.max(0, Torneo.tt(limiteS * 20) / 20 - c.tick / 20);
        return (j.equipo == 0 ? "ROJOS " : "AZULES ") + (c.sinElim() ? "· caídas: rojos " + caidasEq[0] + " / azules " + caidasEq[1] : "· vivos: rojos " + a + " / azules " + b) + (j.bomba ? " · ¡BOMBA! clic derecho" : "") + " · " + resta + " s";
    }

    @Override
    public void alIrse(Ctx c, Jug j) { j.bomba = false; quitarPortada(j); }
}
