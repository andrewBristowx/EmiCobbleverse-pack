import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;

/**
 * Tienda de movimientos:
 *  1) ShopCatalog.reload()/initialize() llaman a MoveShopExtras.merge(...) para cargar config/emipokemon/shop/extra/*.json (en memoria).
 *  2) ShopNetworking.open(...) filtra el catalogo enviado segun la categoria (NPC de movimientos = solo sus 4 pestañas).
 *  3) ServiceNpcEntity$NpcKind.safeCategory() acepta tm_moves, egg_moves, star_moves y tutor_moves (antes solo 9 categorias fijas).
 * Uso: PatchShop <jar_extraido> <stub_classes> <MoveShopExtras_classes> <salida>
 */
public class PatchShop {
    static final String SC = "com/emipokemon/shop/ShopCatalog";
    static final String CONFIG = SC + "$Config";
    static final String NK = "com/emipokemon/npc/ServiceNpcEntity$NpcKind";
    static final String EXTRAS = "com/emipokemon/shop/MoveShopExtras";

    static ClassNode read(Path p) throws Exception {
        ClassNode cn = new ClassNode();
        new ClassReader(Files.readAllBytes(p)).accept(cn, 0);
        return cn;
    }
    static void write(ClassNode cn, Path p) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        Files.createDirectories(p.getParent());
        Files.write(p, cw.toByteArray());
    }
    static MethodNode method(ClassNode cn, String name) {
        MethodNode found = null;
        for (MethodNode m : cn.methods) if (m.name.equals(name)) { if (found != null) throw new IllegalStateException("dup " + name); found = m; }
        if (found == null) throw new IllegalStateException("missing " + name);
        return found;
    }
    static boolean isLogger(AbstractInsnNode i, String text) {
        return i instanceof LdcInsnNode l && text.equals(l.cst);
    }

    public static void main(String[] a) throws Exception {
        Path jar = Paths.get(a[0]), stub = Paths.get(a[1]), extras = Paths.get(a[2]), out = Paths.get(a[3]);

        // --- ShopCatalog ---
        ClassNode sc = read(jar.resolve(SC + ".class"));
        String callMerge = "(L" + CONFIG + ";Ljava/nio/file/Path;)V";

        MethodNode reload = method(sc, "reload");
        AbstractInsnNode target = null;
        for (AbstractInsnNode i = reload.instructions.getFirst(); i != null; i = i.getNext())
            if (isLogger(i, "Reloaded Poke Mart: {} available products, {} unavailable entries")) { target = i.getPrevious(); break; } // GETSTATIC LOGGER
        if (target == null || target.getOpcode() != Opcodes.GETSTATIC) throw new IllegalStateException("reload anchor not found");
        InsnList add = new InsnList();
        add.add(new VarInsnNode(Opcodes.ALOAD, 2)); // `loaded`
        add.add(new VarInsnNode(Opcodes.ALOAD, 0));
        add.add(new FieldInsnNode(Opcodes.GETFIELD, SC, "file", "Ljava/nio/file/Path;"));
        add.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/nio/file/Path", "getParent", "()Ljava/nio/file/Path;", true));
        add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, EXTRAS, "merge", callMerge, false));
        reload.instructions.insertBefore(target, add);

        MethodNode init = method(sc, "initialize");
        target = null;
        for (AbstractInsnNode i = init.instructions.getFirst(); i != null; i = i.getNext())
            if (isLogger(i, "Created default Poke Mart catalog at {}")) { target = i.getPrevious(); break; }
        if (target == null || target.getOpcode() != Opcodes.GETSTATIC) throw new IllegalStateException("initialize anchor not found");
        add = new InsnList();
        add.add(new VarInsnNode(Opcodes.ALOAD, 0));
        add.add(new FieldInsnNode(Opcodes.GETFIELD, SC, "config", "L" + CONFIG + ";"));
        add.add(new VarInsnNode(Opcodes.ALOAD, 0));
        add.add(new FieldInsnNode(Opcodes.GETFIELD, SC, "file", "Ljava/nio/file/Path;"));
        add.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/nio/file/Path", "getParent", "()Ljava/nio/file/Path;", true));
        add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, EXTRAS, "merge", callMerge, false));
        init.instructions.insertBefore(target, add);
        write(sc, out.resolve(SC + ".class"));

        // --- ShopNetworking.open(player, category, product, message, success): json = MoveShopExtras.filter(json, category) ---
        String SN = "com/emipokemon/shop/network/ShopNetworking";
        ClassNode sn = read(jar.resolve(SN + ".class"));
        int hooked = 0;
        for (MethodNode m : sn.methods) {
            if (!m.name.equals("open") || !m.desc.endsWith("Ljava/lang/String;Ljava/lang/String;Z)V")) continue;
            for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
                if (i instanceof MethodInsnNode mi && mi.name.equals("snapshotJson")) {
                    InsnList f = new InsnList();
                    f.add(new VarInsnNode(Opcodes.ALOAD, 1)); // category
                    f.add(new MethodInsnNode(Opcodes.INVOKESTATIC, EXTRAS, "filter", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", false));
                    m.instructions.insert(mi, f);
                    hooked++;
                    break;
                }
            }
        }
        if (hooked != 1) throw new IllegalStateException("ShopNetworking hook count " + hooked);
        write(sn, out.resolve(SN + ".class"));

        // --- NpcKind.safeCategory ---
        ClassNode nk = read(jar.resolve(NK + ".class"));
        ClassNode stubNk = read(stub.resolve(NK + ".class"));
        MethodNode oldM = method(nk, "safeCategory");
        MethodNode newM = method(stubNk, "safeCategory");
        if (!oldM.desc.equals(newM.desc) || oldM.access != newM.access) throw new IllegalStateException("safeCategory signature differs: " + oldM.access + oldM.desc + " vs " + newM.access + newM.desc);
        nk.methods.set(nk.methods.indexOf(oldM), newM);
        write(nk, out.resolve(NK + ".class"));

        // --- MoveShopExtras (new class) ---
        for (String n : new String[]{"MoveShopExtras", "MoveShopExtras$Extra"}) {
            Files.write(out.resolve("com/emipokemon/shop/" + n + ".class"), Files.readAllBytes(extras.resolve("com/emipokemon/shop/" + n + ".class")));
        }
        System.out.println("patched OK");
    }
}
