package emi.fall;

import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
    private static final class Bomba { Entity e; Vec3d p, v; int edad; Jug lanza; }

    private final List<Punto> puntos = new ArrayList<>();
    private final List<Bomba> bombas = new ArrayList<>();
    private final Map<Long, String> original = new HashMap<>();           // el bloque del suelo de cada casilla (para saber si sigue en pie)
    private int ganador = -2;                                              // equipo ganador (-1 = empate)
    private double radioActual = R;

    @Override public String id() { return "bombas"; }
    @Override public String nombre() { return "Guerra de bombas"; }
    @Override public boolean equipos() { return true; }
    @Override public String regla() { return "Rojos contra azules. Coge una bomba, lánzala con clic derecho y deja sin suelo al rival. El que cae queda eliminado."; }

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
        original.clear();
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
    }

    @Override public void liberar(Ctx c) { }

    private ItemStack objetoBomba() {
        ItemStack st = new ItemStack(Items.TNT);
        st.set(DataComponentTypes.CUSTOM_NAME, Text.literal("BOMBA · clic derecho para lanzarla").formatted(Formatting.RED, Formatting.BOLD));
        return st;
    }

    @Override
    public boolean alUsar(Ctx c, Jug j, ServerPlayerEntity p) {
        if (!j.bomba || j.fuera) return false;
        j.bomba = false;
        p.getInventory().clear();
        Vec3d mira = p.getRotationVector();
        Bomba b = new Bomba();
        b.p = p.getEyePos().add(mira.multiply(0.9));
        b.v = mira.multiply(1.25).add(0, 0.12, 0);
        b.lanza = j;
        b.e = Disp.bloque(c.w, "minecraft:tnt", b.p.x, b.p.y, b.p.z, 0.8f, 0.8f, 0.8f, 0, 0);
        bombas.add(b);
        p.playSoundToPlayer(SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.MASTER, 1f, 0.7f);
        return true;
    }

    private boolean hayBloque(ServerWorld w, double x, double z) {
        int S = Minijuegos.SUELO;
        return !w.getBlockState(new BlockPos((int) Math.floor(x), S, (int) Math.floor(z))).isAir();
    }

    private void explotar(Ctx c, Vec3d en, Jug lanza) {
        ServerWorld w = c.w;
        int S = Minijuegos.SUELO;
        int ex = (int) Math.floor(en.x), ez = (int) Math.floor(en.z), r = (int) Math.ceil(RADIO_EXPLOSION);
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > RADIO_EXPLOSION - 0.3 + c.rnd.nextDouble() * 0.6) continue;
            BlockPos bp = new BlockPos(ex + dx, S, ez + dz);
            if (!w.getBlockState(bp).isAir()) Obra.poner(w, bp.getX(), S, bp.getZ(), "air");
        }
        w.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, en.x, S + 1.2, en.z, 1, 0, 0, 0, 0);
        w.spawnParticles(ParticleTypes.CLOUD, en.x, S + 1.2, en.z, 30, 1.6, 0.4, 1.6, 0.1);
        w.playSound(null, en.x, S + 1, en.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 3.0f, 0.9f);
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
            if (pt.hay) { if (c.tick % 2 == 0) Disp.moverBloque(pt.e, pt.x, S + 2.0 + 0.15 * Math.sin(c.tick / 6.0), pt.z, 0.8f, 0.8f, 0.8f, c.tick / 12.0, 2); }
            else if (c.tick >= pt.listoEn && hayBloque(c.w, pt.x, pt.z)) {
                pt.hay = true; pt.e = Disp.bloque(c.w, "minecraft:tnt", pt.x, S + 2.0, pt.z, 0.8f, 0.8f, 0.8f, 0, 0);
            }
        }
        // las bombas lanzadas
        for (int i = bombas.size() - 1; i >= 0; i--) {
            Bomba b = bombas.get(i);
            b.edad++;
            b.v = b.v.add(0, -0.045, 0);
            Vec3d nuevo = b.p.add(b.v);
            boolean boom = false;
            if (nuevo.y <= S + 1.0 && b.v.y < 0 && hayBloque(c.w, nuevo.x, nuevo.z)) { boom = true; nuevo = new Vec3d(nuevo.x, S + 1.0, nuevo.z); }
            else for (Jug j : c.jugs()) {
                ServerPlayerEntity p = c.pl(j);
                if (p == null || j.fuera || j == b.lanza && b.edad < 8) continue;
                if (Math.abs(p.getX() - nuevo.x) < 0.9 && Math.abs(p.getZ() - nuevo.z) < 0.9 && nuevo.y > p.getY() - 0.3 && nuevo.y < p.getY() + 2.1) { boom = true; break; }
            }
            if (boom) { explotar(c, nuevo, b.lanza); Disp.quitar(b.e); bombas.remove(i); continue; }
            if (nuevo.y < S - 12 || b.edad > 100) { Disp.quitar(b.e); bombas.remove(i); continue; }
            b.p = nuevo;
            Disp.moverBloque(b.e, nuevo.x, nuevo.y, nuevo.z, 0.8f, 0.8f, 0.8f, b.edad * 0.5, 1);
        }
        // recoger bombas
        for (Jug j : c.jugs()) {
            ServerPlayerEntity p = c.pl(j);
            if (p == null || j.fuera || j.bomba) continue;
            for (Punto pt : puntos) {
                if (!pt.hay) continue;
                if (Math.abs(p.getX() - pt.x) < 1.5 && Math.abs(p.getZ() - pt.z) < 1.5 && p.getY() < S + 3) {
                    pt.hay = false; Disp.quitar(pt.e); pt.listoEn = c.tick + 140;
                    j.bomba = true;
                    p.getInventory().setStack(0, objetoBomba());
                    p.getInventory().selectedSlot = 0;
                    p.playSoundToPlayer(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.MASTER, 1f, 0.8f);
                    p.sendMessage(Text.literal("¡Tienes una bomba! Clic derecho para lanzarla.").formatted(Formatting.RED), true);
                    break;
                }
            }
        }
        // el borde se va comiendo cuando pasa mucho tiempo
        if (c.tick >= ENCOGER_DESDE_S * 20 && c.tick % 20 == 0) {
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
                c.eliminar(j);
                j.bomba = false;
                p.getInventory().clear();
                c.avisar(Text.literal("☠ " + j.nombre + " ha caído").formatted(j.equipo == 0 ? Formatting.RED : Formatting.AQUA));
                Msg.title(p, Text.literal("¡ELIMINADO!").formatted(Formatting.RED, Formatting.BOLD), Text.literal("Mira cómo acaban los demás").formatted(Formatting.GRAY), 3, 40, 10);
                p.playSoundToPlayer(SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.MASTER, 0.5f, 1.8f);
            }
            if (!j.fuera) vivos[j.equipo < 0 ? 0 : j.equipo]++;
        }
        c.t.barra(1f - (float) c.tick / (LIMITE_S * 20));
        int conectados0 = 0, conectados1 = 0;
        for (Jug j : c.jugs()) { if (j.equipo == 1) conectados1++; else conectados0++; }
        boolean fin = c.tick >= LIMITE_S * 20 || (vivos[0] == 0 && conectados0 > 0) || (vivos[1] == 0 && conectados1 > 0) || (vivos[0] == 0 && vivos[1] == 0);
        if (fin) ganador = vivos[0] > vivos[1] ? 0 : vivos[1] > vivos[0] ? 1 : -1;
        return fin;
    }

    @Override
    public List<Jug> clasificacion(Ctx c) {
        List<Jug> l = c.jugs();
        l.sort((a, b) -> {
            int pa = (a.equipo == ganador ? 100000 : 0) + (a.fuera ? a.ordenFuera : 50000);
            int pb = (b.equipo == ganador ? 100000 : 0) + (b.fuera ? b.ordenFuera : 50000);
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
        int resta = Math.max(0, LIMITE_S - c.tick / 20);
        return (j.equipo == 0 ? "ROJOS " : "AZULES ") + "· vivos: rojos " + a + " / azules " + b + (j.bomba ? " · ¡BOMBA! clic derecho" : "") + " · " + resta + " s";
    }

    @Override
    public void alIrse(Ctx c, Jug j) { j.bomba = false; }
}
