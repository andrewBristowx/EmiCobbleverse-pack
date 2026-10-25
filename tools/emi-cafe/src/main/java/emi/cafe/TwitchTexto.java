package emi.cafe;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Limpieza de los textos que llegan del chat de Twitch antes de ponerlos en pantalla (sin nada de Minecraft: se prueba aparte). */
final class TwitchTexto {
    private TwitchTexto() {}

    static final int MAX_MENSAJE = 80, MAX_NOMBRE = 20, ANCHO_LINEA = 26;
    /** Carpeta de datos del cafe (de ahi se lee twitch_filtro.txt); null = sin palabras extra. */
    static java.nio.file.Path carpeta;

    /** Sin tildes, en minusculas y solo letras y numeros: «Papas fritas» = «papas_fritas» = «papasfritas». */
    static String compacto(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(java.util.Locale.ROOT);
        return n.replaceAll("[^a-z0-9ñ]", "");
    }

    private static final Pattern ENLACE = Pattern.compile("(?i)(https?:|www\\.|\\b\\S+\\.(com|net|org|tv|gg|io|ly|me|xyz|co|es|ar|mx)\\b)");
    /** Palabras que no deben salir en pantalla, comparadas por trozo (solo las largas y sin otro sentido). */
    private static final String[] FUERTES = {"nigger", "nigga", "faggot", "fagot", "pendej", "cabron", "mierda", "chingad", "culero", "maricon", "hijodeputa", "hijueputa", "hdp", "joder", "carajo",
            "follar", "violar", "violacion", "suicid", "hitler", "nazi", "pedofil", "pornografia", "cuck", "retard", "kys", "kill yourself", "mamaguevo", "gilipoll", "zorra", "porno"};
    /** Palabras que solo cuentan enteras (como trozo darian falsos avisos: «computadora» lleva «puta»). */
    private static final Set<String> SUELTAS = new HashSet<>(List.of("puta", "puto", "putas", "putos", "verga", "pija", "culo", "pene", "sexo", "sex", "fuck", "shit", "bitch", "dick", "cock", "pussy", "rape", "idiota", "imbecil", "estupido", "coño", "cono", "perra"));
    private static long filtroMod;
    private static List<String> extra = List.of();

    /** Palabras extra de la streamer: {@code world/emi_cafe/twitch_filtro.txt}, una por linea (se vuelve a leer si el archivo cambia). */
    private static List<String> extras() {
        try {
            if (carpeta == null) return extra;
            java.nio.file.Path f = carpeta.resolve("twitch_filtro.txt");
            if (!java.nio.file.Files.exists(f)) { extra = List.of(); return extra; }
            long mod = java.nio.file.Files.getLastModifiedTime(f).toMillis();
            if (mod != filtroMod) {
                filtroMod = mod;
                List<String> l = new ArrayList<>();
                for (String ln : java.nio.file.Files.readAllLines(f)) { String w = compacto(ln); if (!w.isEmpty()) l.add(w); }
                extra = l;
            }
        } catch (Throwable t) { /* se queda con lo que habia */ }
        return extra;
    }

    /** ¿Lleva algo que no debe salir? Se mira sin tildes, cambiando 0→o 1→i 3→e 4→a 5→s 7→t @→a y quitando los signos entre letras. */
    static boolean sucio(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(java.util.Locale.ROOT);
        String leet = n.replace('0', 'o').replace('1', 'i').replace('3', 'e').replace('4', 'a').replace('5', 's').replace('7', 't').replace('@', 'a').replace('$', 's');
        String junto = leet.replaceAll("[^a-zñ]", "");
        for (String f : FUERTES) if (junto.contains(f.replace(" ", ""))) return true;
        for (String f : extras()) if (junto.contains(f)) return true;
        List<String> palabras = new ArrayList<>();
        StringBuilder letras = new StringBuilder();                                   // «p u t a»: las letras sueltas seguidas se juntan en una palabra
        for (String tok : leet.split("[^a-zñ]+")) {
            if (tok.length() == 1) { letras.append(tok); continue; }
            if (letras.length() > 0) { palabras.add(letras.toString()); letras.setLength(0); }
            if (!tok.isEmpty()) palabras.add(tok);
        }
        if (letras.length() > 0) palabras.add(letras.toString());
        for (String w : palabras) {
            if (SUELTAS.contains(w) || SUELTAS.contains(w.replaceAll("(.)\\1{2,}", "$1"))) return true;        // «putaaaa» = «puta»
        }
        return false;
    }

    /** El mensaje listo para ponerlo en el globo: sin enlaces, comillas ni cosas raras, con un largo maximo; vacio si no vale. */
    static String limpiarMensaje(String m) {
        if (m == null) return "";
        if (ENLACE.matcher(m).find() || sucio(m)) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < m.length(); i++) {
            char ch = m.charAt(i);
            if (Character.isLetterOrDigit(ch) || " .,;:!?¡¿'()-_~+*&%/#=".indexOf(ch) >= 0) sb.append(ch);
            else if (ch == '"' || ch == '`' || ch == '´') sb.append('\'');
            else if (Character.isWhitespace(ch)) sb.append(' ');
        }
        String s = sb.toString().replaceAll(" {2,}", " ").strip();
        if (s.length() > MAX_MENSAJE) { s = s.substring(0, MAX_MENSAJE); int sp = s.lastIndexOf(' '); if (sp > MAX_MENSAJE / 2) s = s.substring(0, sp); s = s.strip() + "…"; }
        return s;
    }

    /** Un trozo del chat sin nada raro (para decir en el chat del juego lo que pidieron). */
    static String limpiar(String s) {
        String l = limpiarMensaje(s);
        return l.length() > 24 ? l.substring(0, 24) : l;
    }

    /** El nombre que se ve en pantalla: el de Twitch sin signos raros y con un largo maximo; «Espectador» si no vale. */
    static String nombreSeguro(String nombre, String login) {
        String base = nombre == null || nombre.isBlank() ? login : nombre;
        StringBuilder sb = new StringBuilder();
        if (base != null) for (int i = 0; i < base.length() && sb.length() < MAX_NOMBRE; i++) { char ch = base.charAt(i); if (Character.isLetterOrDigit(ch) || ch == '_') sb.append(ch); }
        String s = sb.toString();
        return s.isEmpty() || sucio(s) ? "Espectador" : s;
    }

    /** El mensaje partido en lineas cortas (maximo 3) para el globo. */
    static String enLineas(String m) {
        StringBuilder out = new StringBuilder(), linea = new StringBuilder();
        int lineas = 1;
        for (String w : m.split(" ")) {
            if (linea.length() > 0 && linea.length() + 1 + w.length() > ANCHO_LINEA) { out.append(linea).append('\n'); linea.setLength(0); lineas++; }
            if (w.length() > ANCHO_LINEA) w = w.substring(0, ANCHO_LINEA);
            linea.append(linea.length() > 0 ? " " : "").append(w);
        }
        out.append(linea);
        return out.toString();
    }
}
