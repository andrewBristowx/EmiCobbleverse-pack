import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;

/**
 * Pedidos del Cafe desde el chat de Twitch (pack 1.0.217): en {@code TwitchChatRelayService.lambda$tick$0}, por cada {@code ChatMessage} nuevo del puente,
 * se llama a {@code TwitchCafeHook.alChat(msg)} justo antes de meterlo en la lista que se reenvia a los jugadores.
 * Ademas, en {@code tick}: el rele solo consulta el chat si hay conectado un destinatario (chatViewerUuids u operador); si no, ahora
 * pregunta antes a {@code TwitchCafeHook.escuchar()} y sigue adelante cuando el cafe de la streamer esta abierto.
 * Hay que meter en el jar la clase nueva {@code com/emipokemon/twitch/TwitchCafeHook.class} (ver twitch-cafe/).
 * Uso: PatchTwitchCafe <jar_extraido> <clases_compiladas(TwitchCafeHook)> <salida>
 */
public class PatchTwitchCafe {
    static final String SVC = "com/emipokemon/twitch/TwitchChatRelayService", HELPER = "com/emipokemon/twitch/TwitchCafeHook", MSG = "com/emipokemon/twitch/TwitchBridgeClient$ChatMessage";

    public static void main(String[] a) throws Exception {
        Path in = Paths.get(a[0]), classes = Paths.get(a[1]), out = Paths.get(a[2]);
        ClassNode cn = new ClassNode();
        new ClassReader(Files.readAllBytes(in.resolve(SVC + ".class"))).accept(cn, 0);
        MethodNode m = null;
        for (MethodNode x : cn.methods) if (x.name.equals("lambda$tick$0")) m = x;
        if (m == null) throw new IllegalStateException("lambda$tick$0 no encontrado");
        int puestos = 0;
        MethodNode tick = null;
        for (MethodNode x : cn.methods) if (x.name.equals("tick") && x.desc.startsWith("(Lnet/minecraft/server/MinecraftServer;J")) tick = x;
        if (tick == null) throw new IllegalStateException("tick no encontrado");
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof MethodInsnNode mi && mi.owner.equals(HELPER)) throw new IllegalStateException("ya estaba parcheado");
            // ALOAD lista; ALOAD mensaje; INVOKEINTERFACE List.add; POP
            if (i instanceof MethodInsnNode mi && mi.getOpcode() == Opcodes.INVOKEINTERFACE && mi.owner.equals("java/util/List") && mi.name.equals("add")
                    && i.getPrevious() instanceof VarInsnNode msgVar && msgVar.getOpcode() == Opcodes.ALOAD
                    && msgVar.getPrevious() instanceof VarInsnNode listVar && listVar.getOpcode() == Opcodes.ALOAD) {
                // el mensaje tiene que ser un ChatMessage: la instruccion que lo guardo fue un checkcast a ChatMessage
                boolean esMensaje = false;
                for (AbstractInsnNode p = msgVar.getPrevious(); p != null; p = p.getPrevious())
                    if (p instanceof TypeInsnNode t && t.getOpcode() == Opcodes.CHECKCAST) { esMensaje = t.desc.equals(MSG); break; }
                if (!esMensaje) continue;
                InsnList pre = new InsnList();
                pre.add(new VarInsnNode(Opcodes.ALOAD, msgVar.var));
                pre.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, "alChat", "(L" + MSG + ";)V", false));
                m.instructions.insertBefore(listVar, pre);
                puestos++;
            }
        }
        if (puestos != 1) throw new IllegalStateException("esperaba 1 sitio donde llamar al gancho y hay " + puestos);
        // tick: ... recipients(server); astore; aload; List.isEmpty; IFEQ seguir; RETURN  →  tras el IFEQ: if (TwitchCafeHook.escuchar()) goto seguir
        int escuchas = 0;
        for (AbstractInsnNode i = tick.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof MethodInsnNode mi && mi.owner.equals("java/util/List") && mi.name.equals("isEmpty")
                    && mi.getPrevious() instanceof VarInsnNode
                    && mi.getNext() instanceof JumpInsnNode j && j.getOpcode() == Opcodes.IFEQ) {
                AbstractInsnNode sig = j.getNext();
                while (sig != null && sig.getOpcode() < 0) sig = sig.getNext();         // saltar etiquetas y numeros de linea
                if (sig == null || sig.getOpcode() != Opcodes.RETURN) continue;
                InsnList pre = new InsnList();
                pre.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, "escuchar", "()Z", false));
                pre.add(new JumpInsnNode(Opcodes.IFNE, j.label));
                tick.instructions.insert(j, pre);
                escuchas++;
            }
        }
        if (escuchas != 1) throw new IllegalStateException("esperaba 1 sitio para escuchar() y hay " + escuchas);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        Path o = out.resolve(SVC + ".class");
        Files.createDirectories(o.getParent());
        Files.write(o, cw.toByteArray());
        Path h = classes.resolve(HELPER + ".class");
        if (!Files.exists(h)) throw new IllegalStateException("falta " + h);
        Path oh = out.resolve(HELPER + ".class");
        Files.copy(h, oh, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("patched OK (TwitchChatRelayService: alChat por cada mensaje y escuchar() cuando el cafe esta abierto)");
    }
}
