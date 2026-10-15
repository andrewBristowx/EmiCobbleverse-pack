package emi.huida;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import emi.huida.net.EfectoPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.Iterator;
import java.util.List;

/**
 * Las escenas del nivel 3 («la huida de los michialiens», el final): la entrada (la horda, encerrada tras un portón de cristal rosa, se despierta, maulla,
 * se agolpa y rompe el cristal), cuando te alcanzan (te pasan por encima corriendo y te quedas aplastado como una tortita) y el final (llegan todos juntos
 * a la meta y te rodean; aparece Emi, te dice que te has pasado todos sus gatitos, ellos reaccionan y acaban todos encima de ella).
 * Los michis son Pokemon de verdad (GatitoAlien de distintos aspectos, {@link Horda}), Emi es {@link EmiEntity}, los dialogos son globos de texto.
 */
final class EscenasCosmos {
    private EscenasCosmos() {}

    // ---- los textos (se pueden cambiar aqui)
    static final String EMI_1 = "¡Wao, amor!", EMI_2 = "¡Te pasaste todos mis gatitos!", EMI_AY = "¡Ay ora!";
    static final String MICHI_AMOR = "¿Amor?", MICHI_EMI = "¡Emi!", MICHI_FAVORITO = "¡Es el favorito, ¿verdad?!";

    /** Un globo de texto que sigue a una entidad (un michi o Emi) hasta el tick {@code hasta} de la escena. */
    static final class Burbuja { Entity e; Entity sigue; double dy; int hasta; }

    // ---------------------------------------------------------------- utilidades

    private static Vec3d P(Huida.J j, double s, double v, double dy) { return Escenas.P(j.hueco, s, v, dy); }
    private static void camara(ServerPlayerEntity p, Vec3d pos, Vec3d mira, float fov, float temblor, boolean corte) { Escenas.camara(p, pos, mira, fov, temblor, corte); }
    private static void sonido(ServerWorld w, Vec3d pos, SoundEvent ev, float vol, float pitch) { Escenas.sonido(w, pos, ev, vol, pitch); }
    private static void sonido(ServerWorld w, Vec3d pos, String id, float vol, float pitch) { Escenas.sonido(w, pos, id, vol, pitch); }
    private static double suave(double t) { return Escenas.suave(t); }
    private static double lerp(double a, double b, double t) { return Escenas.lerp(a, b, t); }
    private static Vec3d lerp(Vec3d a, Vec3d b, double t) { return Escenas.lerp(a, b, t); }
    private static final java.util.Random RND = new java.util.Random();

    /** Un maullido de un michi (de los de Minecraft con otro tono), para los coros. */
    private static void maullar(ServerWorld w, Vec3d pos, float vol) {
        SoundEvent[] m = {SoundEvents.ENTITY_CAT_AMBIENT, SoundEvents.ENTITY_CAT_PURREOW, SoundEvents.ENTITY_CAT_STRAY_AMBIENT, SoundEvents.ENTITY_CAT_BEG_FOR_FOOD};
        sonido(w, pos, m[RND.nextInt(m.length)], vol, 0.95f + RND.nextFloat() * 0.8f);
    }

    private static Entity michiE(Horda hd, int i) { return hd.michis.get(Math.floorMod(i, hd.michis.size())).e; }

    /** Pone un globo de texto sobre {@code quien} ({@code dy} sobre sus pies) durante {@code dura} ticks. */
    static void decir(Huida.J j, ServerWorld w, Entity quien, double dy, String texto, String color, float escala, int t, int dura) {
        if (quien == null || quien.isRemoved()) return;
        Entity e = Escenas.globo(w, quien.getPos().add(0, dy, 0), "{\"text\":\"" + texto + "\",\"color\":\"" + color + "\",\"bold\":true}", escala);
        if (e == null) return;
        Burbuja b = new Burbuja(); b.e = e; b.sigue = quien; b.dy = dy; b.hasta = t + dura;
        j.burbujas.add(b);
    }

    private static void actualizarBurbujas(Huida.J j, int t) {
        for (Iterator<Burbuja> it = j.burbujas.iterator(); it.hasNext(); ) {
            Burbuja b = it.next();
            if (t >= b.hasta || b.e == null || b.e.isRemoved()) { if (b.e != null && !b.e.isRemoved()) b.e.discard(); it.remove(); continue; }
            if (b.sigue != null && !b.sigue.isRemoved()) { Vec3d g = b.sigue.getPos().add(0, b.dy, 0); b.e.refreshPositionAndAngles(g.x, g.y, g.z, 0f, 0f); }
        }
    }

    private static double alturaMichi(Horda.Michi m) { return m.escala * 1.0 + 0.8 + m.encima; }

    /** Cuanto se ve de plano al jugador (0..1): se le manda a su cliente cada tick. */
    private static void aplastar(ServerPlayerEntity p, float a) { if (p != null) ServerPlayNetworking.send(p, new EfectoPayload(a)); }

    /** Los michis tienen la boca tan alta que la altura del globo se calcula con su escala: bastan unos 2,6 bloques sobre sus pies por cada 2,5 de escala. */
    static EmiEntity emi(ServerWorld w, Vec3d pos, float yaw) {
        try {
            EmiEntity e = new EmiEntity(EmiEntity.TIPO, w);
            e.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0f);
            e.setHeadYaw(yaw); e.setBodyYaw(yaw);
            e.setNoGravity(true);
            w.spawnEntity(e);
            return e;
        } catch (Throwable t) {
            EmiHuida.LOG.error("No se pudo crear a Emi", t);
            return null;
        }
    }

    // ---------------------------------------------------------------- banco de pruebas (/huida prueba banco3)

    /** Solo para pruebas: la horda entera en formacion, Emi y un michi suelto delante del jugador, para verlos y medirlos. */
    static String banco(ServerPlayerEntity p) {
        ServerWorld w = p.getServerWorld();
        double r = Math.toRadians(p.getYaw());
        Vec3d f = new Vec3d(-Math.sin(r), 0, Math.cos(r)), d = new Vec3d(-Math.cos(r), 0, -Math.sin(r));
        int n = 0;
        for (int i = 0; i < Horda.FORMACION.length; i++) {
            double[] q = Horda.FORMACION[i];
            Vec3d pos = p.getPos().add(f.multiply(10 + q[0])).add(d.multiply(q[1])).add(0, q[3], 0);
            PokemonEntity e = Horda.michiDeTierra(w, Horda.FORMAS[i % Horda.FORMAS.length], pos, p.getYaw() + 180f, (float) q[2]);
            if (e == null) continue;
            e.addCommandTag("emi_huida_banco");
            n++;
        }
        EmiEntity emi = emi(w, p.getPos().add(f.multiply(5)).add(d.multiply(6)), p.getYaw() + 180f);
        if (emi != null) { emi.addCommandTag("emi_huida_banco"); n++; }
        return n == 0 ? "No se pudo crear nada." : null;
    }

    // ---------------------------------------------------------------- escena 1: la entrada
    // 0: plano general; 50: los michis notan al jugador («!») y empiezan a maullar y a dar brincos; 96: el lider grita «¡MIAU!»; 150: se agolpan contra el cristal;
    // 186: el cristal se rompe y la horda sale; 210: «¡CORRE!».

    private static final int I_ALERTA = 50, I_MIAU = 96, I_PRESIONA = 150, I_ROMPE = 186;

    static void intro(Huida.J j, ServerPlayerEntity p, ServerWorld w, int t) {
        Horda hd = j.horda;
        if (hd == null || hd.michis.isEmpty()) { Escenas.camaraOff(p); return; }
        double s0 = Nivel.sInicioPerseguidor();
        double sF = s0;
        if (t >= I_PRESIONA && t < I_ROMPE) sF = s0 + 2.8 * suave((t - I_PRESIONA) / 36.0) + 0.1 * Math.sin(t * 1.9);
        else if (t >= I_ROMPE) sF = s0 + 2.8 + (t - I_ROMPE) * 0.27;
        double salto = t < I_ALERTA ? 0 : t < I_PRESIONA ? 0.9 * suave((t - I_ALERTA) / 60.0) : 1.0;
        boolean corre = t >= I_ROMPE + 3;
        hd.mover(j, sF, t, corre, 8.5, corre ? 0 : salto, 0f);
        j.sPerseguidor = sF;
        actualizarBurbujas(j, t);
        Horda.Michi lider = hd.lider();
        Vec3d lp = lider.e == null ? P(j, s0, 0, 0) : lider.e.getPos();

        if (t == 0) Msg.actionBar(p, Text.literal("¿Qué son esos ruidos...?").formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC));
        if (t == I_ALERTA) {
            int[] quien = {0, 4, 9};
            for (int q : quien) decir(j, w, michiE(hd, q), alturaMichi(hd.michis.get(q)), "!", "red", 2.8f, t, 36);
            sonido(w, lp, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.2f, 1.6f);
        }
        if (t == I_ALERTA + 7) for (int q : new int[]{2, 7, 13}) decir(j, w, michiE(hd, q), alturaMichi(hd.michis.get(q)), "!", "red", 2.8f, t, 30);
        if (t == I_MIAU) {
            decir(j, w, lider.e, alturaMichi(lider), "¡MIAU!", "gold", 3.0f, t, 46);
            for (int q : new int[]{0, 3, 6, 10}) Escenas.anim((PokemonEntity) michiE(hd, q), p, "cry");
            sonido(w, lp, "sfx.horda", 1.4f, 1.0f);
        }
        if (t == 132) decir(j, w, michiE(hd, 5), alturaMichi(hd.michis.get(5)), "¡MIAU MIAU!", "light_purple", 2.6f, t, 40);
        if (t >= 46 && t < I_ROMPE) {                             // el coro de maullidos va a mas
            int cada = t < 100 ? 8 : t < 150 ? 5 : 3;
            if (t % cada == 0) maullar(w, michiE(hd, RND.nextInt(hd.michis.size())).getPos(), 1.2f);
            if (t % 14 == 0 && t > 100) Escenas.particulas(w, ParticleTypes.NOTE, lp.add(RND.nextDouble() * 6 - 3, 3 + RND.nextDouble() * 2, -RND.nextDouble() * 6), 1, 0, 0, 0, 1.0);
        }
        if (t == I_ROMPE) {                                         // el portón de cristal estalla
            for (Nivel.Bloque b : j.nivel.entrada()) w.breakBlock(b.pos(), false);
            Vec3d g = P(j, Nivel.S_ENTRADA, 0, 3);
            sonido(w, g, SoundEvents.BLOCK_GLASS_BREAK, 2.0f, 0.7f);
            sonido(w, g, SoundEvents.BLOCK_GLASS_BREAK, 2.0f, 1.1f);
            sonido(w, g, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 1.0f, 1.3f);
            sonido(w, g, "sfx.horda", 1.8f, 0.9f);
            Escenas.particulas(w, ParticleTypes.EXPLOSION, g, 2, 2.5, 1.5, 0.3, 0);
            Escenas.particulas(w, ParticleTypes.END_ROD, g, 40, 2.8, 2.5, 0.6, 0.25);
            Escenas.particulas(w, ParticleTypes.HEART, g, 10, 2.6, 2.0, 0.6, 0.1);
        }
        if (t > I_ROMPE && t % 4 == 0 && t < I_ROMPE + 22) Escenas.particulas(w, ParticleTypes.CLOUD, lp.add(0, 0.3, 0), 5, 2.4, 0.2, 2.0, 0.02);

        // ---- la camara
        float temblor = t >= I_PRESIONA && t < I_ROMPE ? 0.015f + 0.05f * (t - I_PRESIONA) / (float) (I_ROMPE - I_PRESIONA) : t >= I_ROMPE && t < I_ROMPE + 16 ? 0.22f * (1f - (t - I_ROMPE) / 16f) : 0f;
        double sG = Nivel.S_ENTRADA;
        if (t < 40) {                      // general, desde arriba y desde el lado del jugador: el jugador, el portón y, al fondo, la horda
            double u = suave(t / 40.0);
            camara(p, lerp(P(j, sG + 30, 4.5, 8.0), P(j, sG + 26, 3.0, 6.0), u), lerp(P(j, sG - 3, 0, 2.5), P(j, sG - 3, 0, 2.7), u), 1.0f, 0f, t == 0);
        } else if (t < 100) {              // cara a cara con el lider, a traves del cristal
            double u = suave((t - 40) / 60.0);
            camara(p, P(j, sG + 6.5, lerp(-1.6, 1.4, u), lerp(2.0, 2.6, u)), lp.add(0, 2.6, 0), (float) lerp(0.9, 0.8, u), 0f, t == 40);
        } else if (t < 168) {              // desde arriba, a lo lejos: toda la horda dando brincos
            double u = suave((t - 100) / 68.0);
            camara(p, P(j, sG + 12 - 4 * u, 1.2 - 2.4 * u, 10.0 - 1.5 * u), P(j, sG - 6, 0, 2.0), 1.0f, temblor, t == 100);   // por encima de los muros
        } else {                           // detras del jugador, mirando al portón con la horda empujando; al romperse sale hacia la camara
            double u = suave((t - 168) / 42.0);
            camara(p, P(j, sG + 27, 0, lerp(4.2, 3.6, u)), P(j, sG - 2 + 3 * u, 0, 2.8), (float) lerp(1.0, 0.93, u), temblor, t == 168);
        }
    }

    // ---------------------------------------------------------------- escena 2: la horda te pasa por encima
    // 0: la horda no se para; 8-45: va pasando fila a fila por encima del jugador, que se va quedando plano; ~60: se paran delante, se dan la vuelta y miran;
    // +34: «¡TE APLASTARON!»; 190: fin.

    private static final double V_ESTAMPIDA = 0.36;       // bloques por tick (7,2 por segundo) a los que pasa la horda por encima del jugador
    private static final double DIST_PARADA = 14.5;      // hasta donde llega la primera fila por delante del jugador

    static void aplastado(Huida.J j, ServerPlayerEntity p, ServerWorld w, int t) {
        Horda hd = j.horda;
        if (hd == null || hd.michis.isEmpty()) { Escenas.camaraOff(p); aplastar(p, 1f); return; }
        double sPl = j.s;
        if (t == 0 || j.escBase == null) {
            j.escBase = new Vec3d(j.sPerseguidor, 0, 0);               // x = donde iba la primera fila, y = tick en el que se para (0 = aun no)
            sonido(w, p.getPos(), "sfx.horda", 1.8f, 1.0f);
            Msg.actionBar(p, Text.literal("¡Te van a pasar por encima!").formatted(Formatting.RED, Formatting.BOLD));
        }
        double s0 = j.escBase.x;
        double sStop = sPl + DIST_PARADA;
        double libre = s0 + V_ESTAMPIDA * t, ini = sStop - 5.0;
        double sF = libre < ini ? libre : ini + 5.0 * (1 - Math.pow(1 - Math.min(1.0, (libre - ini) / 5.0), 2));
        boolean corre = sStop - sF > 0.12;
        if (!corre && j.escBase.y == 0) j.escBase = new Vec3d(s0, Math.max(1, t), 0);
        int tPar = j.escBase.y == 0 ? 9999 : (int) j.escBase.y;
        float giro = !corre ? (float) (180 * suave((t - tPar - 10) / 16.0)) : 0f;
        j.sPerseguidor = sF;

        // ---- los michis: corren por encima del jugador (un brinco al pisarlo) y despues se paran, se dan la vuelta y lo miran
        int cruzados = 0;
        for (int i = 0; i < hd.michis.size(); i++) {
            Horda.Michi m = hd.michis.get(i);
            double sM = sF - m.ds, tCruce = (sPl - s0 + m.ds) / V_ESTAMPIDA;
            if (t >= tCruce) cruzados++;
            if (t == (int) Math.ceil(tCruce) && m.e != null) {                       // ¡plaf!
                Vec3d pie = p.getPos();
                sonido(w, pie, SoundEvents.ENTITY_SLIME_SQUISH, 1.6f, 0.55f + RND.nextFloat() * 0.5f);
                sonido(w, pie, SoundEvents.ENTITY_SLIME_SQUISH_SMALL, 1.2f, 0.8f + RND.nextFloat() * 0.4f);
                if (i % 3 == 0) maullar(w, m.e.getPos(), 0.9f);
                Escenas.particulas(w, ParticleTypes.POOF, pie.add(0, 0.3, 0), 8, 0.5, 0.1, 0.5, 0.05);
                Escenas.particulas(w, ParticleTypes.CRIT, pie.add(0, 0.5, 0), 6, 0.5, 0.2, 0.5, 0.3);
            }
            double cerca = Math.abs(sM - sPl);
            double brinco = cerca < 1.3 ? 0.55 * Math.cos(cerca / 1.3 * Math.PI / 2) : 0;       // el brinco que da al pisarlo
            Vec3d pos = Horda.posicion(j.hueco, sF, m, corre ? t : 0).add(0, brinco, 0);
            Horda.poner(m, pos, (float) Nivel.rumbo(Math.max(0, sM)) + giro, corre, 7.2);
        }
        float a = (float) suave(Math.min(1.0, cruzados / 7.0));
        aplastar(p, Math.max(a, j.escBase.y > 0 ? 1f : 0f));
        actualizarBurbujas(j, t);

        // ---- despues: estrellitas mareadas sobre la tortita, «¿Miau?» y el titulo
        if (!corre) {
            int dt = t - tPar;
            if (dt % 3 == 0) {
                double ang = t * 0.35;
                Vec3d pie = p.getPos();
                Escenas.particulas(w, ParticleTypes.CRIT, pie.add(Math.cos(ang) * 0.9, 0.55, Math.sin(ang) * 0.9), 1, 0.02, 0.02, 0.02, 0);
                Escenas.particulas(w, ParticleTypes.CRIT, pie.add(Math.cos(ang + 3.14) * 0.9, 0.55, Math.sin(ang + 3.14) * 0.9), 1, 0.02, 0.02, 0.02, 0);
            }
            if (dt == 14) { decir(j, w, michiE(hd, 4), alturaMichi(hd.michis.get(4)), "¿Miau?", "light_purple", 2.4f, t, 60); maullar(w, michiE(hd, 4).getPos(), 1.2f); }
            if (dt == 28) { decir(j, w, michiE(hd, 10), alturaMichi(hd.michis.get(10)), "¿Miau?", "white", 2.4f, t, 55); maullar(w, michiE(hd, 10).getPos(), 1.2f); }
            if (dt == 40) { decir(j, w, michiE(hd, 1), alturaMichi(hd.michis.get(1)), "¡Ups!", "gold", 2.4f, t, 50); maullar(w, michiE(hd, 1).getPos(), 1.3f); }
            if (dt == 34) {
                Msg.title(p, Text.literal("¡TE APLASTARON!").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD),
                        Text.literal(j.devorado ? "★ " + j.puntos + " · ¡Demasiados errores! Ahora eres una tortita" : "★ " + j.puntos + " · Errores: " + j.errores + " · Ahora eres una tortita").formatted(Formatting.GRAY), 0, 60, 20);
                sonido(w, p.getPos(), "sfx.aplastado", 1.6f, 1f);
                Sonidos.sfx(p, Sonidos.DERROTA, 0.8f, 1.1f);
            }
        }

        // ---- la camara
        float temblor = t < 46 ? 0.04f : 0f;
        Vec3d pl = p.getPos();
        // el camino es estrecho y los muros altos: las camaras van por dentro del camino o por encima de los muros (si no, quedan dentro de los bloques)
        if (t < 30) {                      // desde arriba y desde delante: la horda llega de detras, cruza la imagen y pasa por encima del jugador
            camara(p, P(j, sPl + 5.0, -1.6, 7.0), P(j, sPl - 0.5, 0, 0.6), 1.0f, temblor, t == 0);
        } else if (t < tPar + 12) {        // un poco mas cerca, tambien desde arriba: los ultimos pasan y el jugador queda plano
            double u = suave((t - 30) / Math.max(10.0, tPar - 18));
            camara(p, P(j, sPl + lerp(4.4, 3.4, u), -1.4, lerp(6.6, 5.4, u)), pl.add(0, 0.3, 0), 0.95f, temblor, t == 30);
        } else {                           // detras del jugador mirando hacia delante: la tortita en primer plano y, al fondo, la horda que lo mira
            double u = suave((t - tPar - 12) / 120.0);
            camara(p, P(j, sPl - lerp(4.4, 3.2, u), 0.6, lerp(2.6, 1.9, u)), lerp(P(j, sPl + 9, 0, 2.4), pl.add(0, 0.2, 0), 0.5 + 0.2 * u), 0.95f, 0f, t == tPar + 12);
        }
        if (t == 186) { Escenas.camaraOff(p); }
    }

    // ---------------------------------------------------------------- escena 3: Emi y los gatitos
    // 0-44: la horda llega corriendo y rodea al jugador (todos a la vez); 84: aparece Emi; 96: «¡Wao, amor!»; 144: «¡Te pasaste todos mis gatitos!»;
    // 196-280: los gatitos hablan («¿Amor?», «¡Emi!», «¡Es el favorito, ¿verdad?!»); 296-340: se tiran todos encima de Emi y hacen una pila; 342: «¡Ay ora!»;
    // 410: «¡ESCAPASTE!»; 560: fin de la camara; 600: fin.

    private static final int F_RODEA = 44, F_EMI = 84, F_MICHIS = 196, F_SALTAN = 330, F_AY = 376, F_TITULO = 444, F_CAM_FIN = 612;
    /** Lo que dicen los gatitos: {tick desde F_MICHIS, michi, texto, color, duracion}. */
    private static final Object[][] HABLAN = {
            {0, 3, MICHI_AMOR, "light_purple", 48}, {8, 13, MICHI_AMOR, "light_purple", 48}, {16, 15, MICHI_AMOR, "light_purple", 48},
            {28, 1, MICHI_EMI, "white", 44}, {34, 4, MICHI_EMI, "white", 44}, {40, 14, MICHI_EMI, "white", 44}, {46, 16, MICHI_EMI, "white", 44},
            {60, 2, MICHI_FAVORITO, "gold", 62}, {72, 17, MICHI_FAVORITO, "gold", 62}};

    static void finalEmi(Huida.J j, ServerPlayerEntity p, ServerWorld w, int t) {
        Horda hd = j.horda;
        if (hd == null || hd.michis.isEmpty()) { Escenas.camaraOff(p); return; }
        int n = hd.michis.size();
        // el escenario: un trozo de recta libre ANTES de la puerta dorada (despues de ella el camino mide 8 bloques y acaba en un muro, no cabe ni la camara);
        // en el primer tick (con la camara en corte) se lleva ahi al jugador y la horda llega corriendo desde mas atras
        double sPl = Nivel.S_META - 10.0;
        double sE = sPl + 4.8;
        Vec3d pEmi = P(j, sE, 0, 0), pJug = P(j, sPl, 0, 0);
        float yawEmi = (float) Nivel.rumbo(sE) + 180f;
        if (t == 0) {
            double[] ej = Nivel.ejePos(j.hueco, sPl);
            p.teleport(w, ej[0], Nivel.Y0, ej[1], (float) ej[2], 8f);
            p.setVelocity(0, 0, 0); p.velocityModified = true;
            j.escBase = new Vec3d(sPl - 9.0, 0, 0);
            Msg.actionBar(p, Text.literal("¡Llegaste a la meta... pero la horda llega contigo!").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
            sonido(w, pJug, "sfx.horda", 1.4f, 1.1f);
        }
        double s0 = j.escBase != null ? j.escBase.x : sPl - 9.0;
        actualizarBurbujas(j, t);

        // ---- los michis: llegan todos a la vez y forman un corro alrededor del jugador (con un hueco delante, para Emi); luego se tiran sobre Emi
        for (int i = 0; i < n; i++) {
            Horda.Michi m = hd.michis.get(i);
            if (m.e == null || m.e.isRemoved()) continue;
            double th = 0.85 + (2 * Math.PI - 1.7) * i / (double) n;
            Vec3d slot = P(j, sPl + 0.6 + 4.2 * Math.cos(th), 2.1 * Math.sin(th), m.encima > 0 ? 0 : 0);
            Vec3d ini = Horda.posicion(j.hueco, s0, m, 0);
            float yawSlot = Escenas.yawHacia(slot, pJug);
            Vec3d pos; float yaw; boolean corre = false;
            if (t < F_RODEA) {
                double u = suave(t / (double) F_RODEA);
                pos = lerp(ini, slot, u).add(0, 0.12 * Math.abs(Math.sin(t * 0.55 + m.fase)), 0);
                yaw = Escenas.yawHacia(ini, slot); corre = true;
            } else if (t < F_SALTAN + 2 * i) {
                double salto = t < F_MICHIS + 60 ? 0.35 * Math.abs(Math.sin((t - F_RODEA) * 0.3 + m.fase)) * (t < F_EMI ? 1.6 : 1.0) : 0.2 * Math.abs(Math.sin((t - F_RODEA) * 0.3 + m.fase));
                pos = slot.add(0, salto, 0);
                yaw = t < F_EMI + 8 ? yawSlot : Escenas.lerpAng(yawSlot, Escenas.yawHacia(slot, pEmi), suave((t - F_EMI - 8) / 14.0));
            } else {                                                                           // el salto hacia la pila sobre Emi
                double u = Math.min(1.0, (t - (F_SALTAN + 2 * i)) / 26.0);
                int capa = i < 6 ? 0 : i < 11 ? 1 : i < 15 ? 2 : 3;
                int k = capa == 0 ? i : capa == 1 ? i - 6 : capa == 2 ? i - 11 : i - 15, cuantos = capa == 0 ? 6 : capa == 1 ? 5 : capa == 2 ? 4 : 3;
                // al subirse a Emi los michis se hacen pequeñitos (mas tiernos, y asi Emi, que es mas grande, no queda tapada del todo)
                double r = new double[]{1.5, 1.2, 1.3, 0.3}[capa], alto = new double[]{0, 1.5, 3.0, 3.5}[capa];
                double mitad = capa < 2 ? 1.95 : 1.45;                         // van por los lados y por detras de Emi; su cara queda libre (la camara esta detras de la horda, mirando hacia delante)
                double ang = -mitad + 2 * mitad * (k + 0.5) / cuantos;
                Vec3d fin = P(j, sE + r * Math.cos(ang), r * Math.sin(ang), alto);
                if (u < 1.0 || t == F_SALTAN + 2 * i + 26) { try { m.e.getPokemon().setScaleModifier((float) (m.escala * (1.0 - 0.45 * suave(u)))); } catch (Throwable ex) { /* sin Cobblemon no hay michis */ } }
                pos = lerp(slot, fin, suave(u)).add(0, 3.6 * Math.sin(Math.PI * u), 0);
                yaw = Escenas.yawHacia(pos, pEmi.add(0, 0, 0));
                if (u >= 1.0) pos = fin.add(0, 0.12 * Math.sin(t * 0.5 + m.fase) * (t < F_AY + 40 ? 1 : 0.3), 0);
                if (t == F_SALTAN + 2 * i + 26) { sonido(w, fin, SoundEvents.ENTITY_SLIME_SQUISH_SMALL, 1.0f, 0.8f + RND.nextFloat() * 0.5f); Escenas.particulas(w, ParticleTypes.HEART, fin.add(0, 1.5, 0), 2, 0.6, 0.4, 0.6, 0.02); }
                if (t == F_SALTAN + 2 * i) maullar(w, slot, 1.0f);
            }
            Horda.poner(m, pos, yaw, corre, 8.5);
        }
        j.sPerseguidor = s0;
        if (t == F_RODEA) {
            for (int i = 0; i < n; i += 2) Escenas.anim((PokemonEntity) michiE(hd, i), p, "cry");
            sonido(w, pJug, SoundEvents.ENTITY_CAT_PURREOW, 1.4f, 1.3f);
            Escenas.particulas(w, ParticleTypes.CLOUD, pJug.add(0, 0.2, 0), 25, 3.4, 0.1, 1.8, 0.04);
        }
        if (t >= F_RODEA && t < F_EMI && t % 7 == 0) maullar(w, michiE(hd, RND.nextInt(n)).getPos(), 0.9f);

        // ---- Emi aparece delante del jugador, entre los michis
        EmiEntity emi = j.emi instanceof EmiEntity e ? e : null;
        if (t == F_EMI) {
            emi = emi(w, pEmi, yawEmi);
            j.emi = emi;
            if (emi != null) emi.ponerEstado(1);
            sonido(w, pEmi, "sfx.aparece", 1.8f, 1f);
            sonido(w, pEmi, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.4f);
            sonido(w, pEmi, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.6f, 1.2f);
            sonido(w, pEmi, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.6f, 1.7f);
            Escenas.particulas(w, ParticleTypes.TOTEM_OF_UNDYING, pEmi.add(0, 1.3, 0), 40, 0.6, 1.2, 0.6, 0.35);
            Escenas.particulas(w, ParticleTypes.END_ROD, pEmi.add(0, 1.3, 0), 50, 0.8, 1.4, 0.8, 0.12);
            Escenas.particulas(w, ParticleTypes.HEART, pEmi.add(0, 2.6, 0), 12, 0.8, 0.5, 0.8, 0.05);
        }
        double alturaEmi = t >= F_AY ? 6.0 : 4.4;
        if (t == F_EMI + 12 && emi != null) { decir(j, w, emi, alturaEmi, EMI_1, "light_purple", 3.2f, t, 62); p.sendMessage(Text.literal("Emi: ").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD).append(Text.literal(EMI_1 + " " + EMI_2).formatted(Formatting.WHITE)), false); }
        if (t == F_EMI + 60 && emi != null) { decir(j, w, emi, alturaEmi, EMI_2, "light_purple", 3.0f, t, 92); sonido(w, pEmi, SoundEvents.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 1.2f, 1.5f); }
        if (emi != null && !emi.isRemoved()) {
            if (t == F_SALTAN + 4) emi.ponerEstado(2);
            if (t == F_AY) emi.ponerEstado(3);
            if (t > F_AY && t < F_AY + 30 && t % 5 == 0) Escenas.particulas(w, ParticleTypes.HEART, pEmi.add(0, 6.0, 0), 2, 1.2, 0.5, 1.2, 0.03);
        }
        if (t == F_AY && emi != null) {
            decir(j, w, emi, 6.0, EMI_AY, "light_purple", 3.4f, t, 70);
            sonido(w, pEmi, SoundEvents.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, 1.6f, 1.2f);
            sonido(w, pEmi, "sfx.aplastado", 1.0f, 1.5f);
        }

        // ---- los gatitos hablan
        for (Object[] h : HABLAN) if (t == F_MICHIS + (int) h[0]) {
            int q = (int) h[1];
            decir(j, w, michiE(hd, q), alturaMichi(hd.michis.get(q)), (String) h[2], (String) h[3], 2.6f, t, (int) h[4]);
            maullar(w, michiE(hd, q).getPos(), 1.3f);
        }
        if (t >= F_MICHIS && t < F_SALTAN && t % 9 == 0) Escenas.particulas(w, ParticleTypes.HEART, michiE(hd, RND.nextInt(n)).getPos().add(0, 3.2, 0), 1, 0.4, 0.2, 0.4, 0.02);
        if (t == F_SALTAN - 2) { sonido(w, pJug, SoundEvents.ENTITY_CAT_PURREOW, 1.6f, 1.8f); sonido(w, pEmi, "sfx.horda", 1.3f, 1.4f); }

        // ---- el titulo y los corazones del final
        if (t == F_TITULO) {
            Huida.titulosEscape(j, p, 70);
            Escenas.particulas(w, ParticleTypes.HEART, pEmi.add(0, 6.0, 0), 30, 2.6, 1.6, 1.6, 0.06);
            Escenas.particulas(w, ParticleTypes.TOTEM_OF_UNDYING, pEmi.add(0, 5.5, 0), 30, 2.0, 1.2, 1.2, 0.3);
        }
        if (t > F_TITULO && t % 8 == 0 && t < F_CAM_FIN) Escenas.particulas(w, ParticleTypes.HEART, pEmi.add(0, 6.0, 0), 2, 2.2, 1.0, 1.4, 0.03);

        // ---- la camara: siempre por detras del corro y desde arriba (el camino es estrecho, los muros altos y despues de la meta hay un muro que cierra el camino);
        // asi se ven las caras de los michis de delante, la de Emi y la pila
        if (t < F_RODEA) {                  // la horda llega corriendo (pasa por debajo de la camara) y rodea al jugador
            double u = suave(t / (double) F_RODEA);
            camara(p, P(j, sPl - lerp(13.0, 12.0, u), 0.3, lerp(8.8, 8.4, u)), lerp(P(j, sPl - 3, 0, 2.0), P(j, sPl + 1.0, 0, 2.0), u), 0.9f, 0.03f, t == 0);
        } else if (t < F_EMI) {             // el corro dando brincos alrededor del jugador
            double u = suave((t - F_RODEA) / (double) (F_EMI - F_RODEA));
            camara(p, P(j, sPl - lerp(12.0, 11.0, u), 0.3, lerp(8.4, 8.0, u)), P(j, sPl + 1.4, 0, 2.2), 0.85f, 0f, t == F_RODEA);
        } else if (t < F_MICHIS) {          // Emi aparece y habla: la camara se acerca a ella
            double u = suave((t - F_EMI) / (double) (F_MICHIS - F_EMI));
            Vec3d e = emi != null && !emi.isRemoved() ? emi.getPos() : pEmi;
            camara(p, P(j, sPl - lerp(11.0, 10.0, u), 0.3, lerp(8.0, 7.0, u)), e.add(0, lerp(2.0, 3.0, u), 0), (float) lerp(0.75, 0.5, u), 0f, t == F_EMI);
        } else if (t < F_SALTAN) {          // los gatitos hablan: la camara mira al que habla
            int quien = 3;
            for (Object[] h : HABLAN) if (t >= F_MICHIS + (int) h[0]) quien = (int) h[1];
            Entity q = michiE(hd, quien);
            Vec3d mira = q == null ? pJug : q.getPos().add(0, 2.8, 0);
            double u = suave((t - F_MICHIS) / (double) (F_SALTAN - F_MICHIS));
            camara(p, P(j, sPl - lerp(10.0, 11.5, u), 0.3, lerp(7.2, 8.0, u)), mira, 0.55f, 0f, t == F_MICHIS);
        } else if (t < F_AY + 6) {          // se tiran todos sobre Emi
            double u = suave((t - F_SALTAN) / (double) (F_AY - F_SALTAN));
            camara(p, P(j, sE - lerp(13.5, 12.5, u), 0.2, lerp(7.4, 6.6, u)), pEmi.add(0, lerp(2.4, 3.6, u), 0), (float) lerp(0.7, 0.6, u), t > F_SALTAN + 40 ? 0.04f : 0f, t == F_SALTAN);
        } else if (t < F_TITULO + 30) {     // la pila con «¡Ay ora!» (primer plano) y, al titulo, un paso atras
            double u = suave((t - F_AY - 6) / (double) (F_TITULO + 24 - F_AY - 6));
            camara(p, P(j, sE - lerp(7.5, 10.0, u), 0.0, lerp(4.4, 6.2, u)), pEmi.add(0, lerp(3.9, 4.0, u), 0), (float) lerp(0.7, 0.85, u), 0f, t == F_AY + 6);
        } else if (t < F_CAM_FIN) {
            double u = suave((t - F_TITULO - 30) / (double) (F_CAM_FIN - F_TITULO - 30));
            camara(p, P(j, sE - lerp(9.0, 7.0, u), lerp(0.0, -0.6, u), lerp(6.5, 5.0, u)), pEmi.add(0, 3.8, 0), 0.85f, 0f, t == F_TITULO + 30);
        } else if (t == F_CAM_FIN) {
            Escenas.camaraOff(p);
        }
    }
}
