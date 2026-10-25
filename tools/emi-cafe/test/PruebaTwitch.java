package emi.cafe;
public class PruebaTwitch {
    static int fallos = 0;
    static void ok(boolean c, String m) { if (!c) { fallos++; System.out.println("FALLA: " + m); } else System.out.println("ok: " + m); }
    public static void main(String[] a) {
        ok(TwitchTexto.compacto("Papas fritas").equals("papasfritas"), "compacto Papas fritas");
        ok(TwitchTexto.compacto("Menú del gran chef").equals("menudelgranchef"), "compacto con tilde");
        ok(TwitchTexto.compacto("papas_fritas").equals("papasfritas"), "compacto con guion bajo");
        ok(TwitchTexto.limpiarMensaje("quiero bien fritas emi").equals("quiero bien fritas emi"), "mensaje normal");
        ok(TwitchTexto.limpiarMensaje("mira https://malo.com/x").isEmpty(), "enlace con https");
        ok(TwitchTexto.limpiarMensaje("entra a www.algo.tv").isEmpty(), "enlace con www");
        ok(TwitchTexto.limpiarMensaje("visita malo.gg ya").isEmpty(), "dominio .gg");
        ok(TwitchTexto.limpiarMensaje("eres un hijo de puta").isEmpty(), "insulto con espacios");
        ok(TwitchTexto.limpiarMensaje("p u t a").isEmpty(), "puta separada");
        ok(TwitchTexto.limpiarMensaje("PUTA madre").isEmpty(), "puta suelta");
        ok(TwitchTexto.limpiarMensaje("m13rd4").isEmpty(), "mierda con numeros");
        ok(TwitchTexto.limpiarMensaje("tengo una computadora nueva").equals("tengo una computadora nueva"), "computadora no es insulto");
        ok(TwitchTexto.limpiarMensaje("el puto amo").isEmpty(), "puto suelto");
        ok(TwitchTexto.limpiarMensaje("Scunthorpe classic assess").equals("Scunthorpe classic assess"), "assess/classic no son insulto");
        ok(TwitchTexto.limpiarMensaje("di \"hola\" \\ amigo").equals("di 'hola' amigo"), "comillas y barra");
        ok(TwitchTexto.limpiarMensaje("a\nb\tc").equals("a b c"), "saltos de linea");
        String largo = "palabra ".repeat(30);
        String r = TwitchTexto.limpiarMensaje(largo);
        ok(r.length() <= 81 && r.endsWith("…"), "largo recortado: " + r.length());
        ok(TwitchTexto.nombreSeguro("María_TV", "maria_tv").equals("María_TV"), "nombre con tilde");
        ok(TwitchTexto.nombreSeguro("<b>x</b>\"", "x").equals("bxb"), "nombre sin signos: " + TwitchTexto.nombreSeguro("<b>x</b>\"", "x"));
        ok(TwitchTexto.nombreSeguro("putaaaa", "x").equals("Espectador"), "nombre sucio");
        ok(TwitchTexto.nombreSeguro("", "").equals("Espectador"), "nombre vacio");
        ok(TwitchTexto.nombreSeguro("A".repeat(40), "x").length() == 20, "nombre largo");
        String l = TwitchTexto.enLineas("gracias emi eres la mejor del mundo mundial siempre");
        ok(l.split("\n").length >= 2 && l.split("\n").length <= 3, "lineas: " + l.replace("\n", "|"));
        for (String ln : l.split("\n")) ok(ln.length() <= 26, "linea corta: " + ln.length());
        System.out.println(fallos == 0 ? "TODO BIEN" : fallos + " FALLOS");
        System.exit(fallos == 0 ? 0 : 1);
    }
}
