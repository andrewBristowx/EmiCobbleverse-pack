import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;

/**
 * Armaduras Emi de nivel (Hierro/Oro/Esmeralda): crash del cliente al dibujarlas (mesa de herreria al mejorar armadura con Fragmento Emi).
 * EmipokemonClient.onInitializeClient solo registraba el renderizador GeckoLib de las 4 piezas base; el proveedor de las
 * demas queda en null y GeckoLib hace NullPointerException al dibujarlas.
 *  - EmipokemonClient.registerEmiArmorRenderer pasa de privado a de paquete.
 *  - onInitializeClient llama a EmiTierArmorRender.register() justo despues del registro de EMI_BOOTS.
 * Uso: PatchArmorRender <jar_extraido> <clases_compiladas(EmiTierArmorRender)> <salida>
 */
public class PatchArmorRender {
    static final String PKG = "com/emipokemon/client/";
    static void fail(String m) { throw new IllegalStateException(m); }

    public static void main(String[] a) throws Exception {
        Path in = Paths.get(a[0]), classes = Paths.get(a[1]), out = Paths.get(a[2]);
        ClassNode cn = new ClassNode();
        new ClassReader(Files.readAllBytes(in.resolve(PKG + "EmipokemonClient.class"))).accept(cn, 0);
        boolean priv = false;
        for (MethodNode m : cn.methods)
            if (m.name.equals("registerEmiArmorRenderer") && (m.access & Opcodes.ACC_PRIVATE) != 0) { m.access &= ~Opcodes.ACC_PRIVATE; priv = true; }
        if (!priv) fail("registerEmiArmorRenderer (privado) no encontrado");
        MethodNode init = null;
        for (MethodNode m : cn.methods) if (m.name.equals("onInitializeClient") && m.desc.equals("()V")) init = m;
        if (init == null) fail("onInitializeClient no encontrado");
        AbstractInsnNode boots = null;
        for (AbstractInsnNode i = init.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof MethodInsnNode mi && mi.owner.equals(PKG + "EmiTierArmorRender")) fail("ya parcheado");
            if (i instanceof FieldInsnNode fi && fi.getOpcode() == Opcodes.GETSTATIC && fi.name.equals("EMI_BOOTS")) {
                AbstractInsnNode n = i.getNext();
                if (n instanceof MethodInsnNode mi && mi.name.equals("registerEmiArmorRenderer")) boots = n;
            }
        }
        if (boots == null) fail("registro de EMI_BOOTS no encontrado");
        init.instructions.insert(boots, new MethodInsnNode(Opcodes.INVOKESTATIC, PKG + "EmiTierArmorRender", "register", "()V", false));
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        Files.createDirectories(out.resolve(PKG));
        Files.write(out.resolve(PKG + "EmipokemonClient.class"), cw.toByteArray());
        Path src = classes.resolve(PKG + "EmiTierArmorRender.class");
        if (!Files.exists(src)) fail("falta " + src);
        Files.copy(src, out.resolve(PKG + "EmiTierArmorRender.class"), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("patched OK");
    }
}
