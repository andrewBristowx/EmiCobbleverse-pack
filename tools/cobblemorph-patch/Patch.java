import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;

/**
 * Parchea CobbleMorph 1.0.5 (licencia CC0):
 *  - PokemonSuggestionProvider: sugiere el id (eevee_gatito) en vez del nombre con espacio.
 *  - TransformCommand y PokemonTransformLayer (cliente): PokemonSpecies.getByName -> SpeciesLookup.find.
 * Uso: Patch <jar_extraido> <salida>
 */
public class Patch {
    static final String LOOKUP = "cobblemorph/SpeciesLookup";
    static final String PS = "com/cobblemon/mod/common/api/pokemon/PokemonSpecies";
    static final String SPECIES = "com/cobblemon/mod/common/pokemon/Species";

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

    static int redirectGetByName(ClassNode cn) {
        int n = 0;
        for (MethodNode m : cn.methods) {
            for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
                if (i instanceof MethodInsnNode mi && mi.getOpcode() == Opcodes.INVOKESTATIC && mi.owner.equals(PS) && mi.name.equals("getByName")) {
                    mi.owner = LOOKUP;
                    mi.name = "find";
                    mi.itf = false;
                    n++;
                }
            }
        }
        return n;
    }

    public static void main(String[] a) throws Exception {
        Path in = Paths.get(a[0]), out = Paths.get(a[1]);

        ClassNode sp = read(in.resolve("cobblemorph/command/PokemonSuggestionProvider.class"));
        MethodNode lambda = null;
        for (MethodNode m : sp.methods) if (m.name.startsWith("lambda$suggest$")) { if (lambda != null) throw new IllegalStateException("varias lambdas"); lambda = m; }
        if (lambda == null || !lambda.desc.equals("(L" + SPECIES + ";)Ljava/lang/String;")) throw new IllegalStateException("lambda de sugerencias distinta de la esperada");
        InsnList body = new InsnList();
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new MethodInsnNode(Opcodes.INVOKESTATIC, LOOKUP, "suggestName", "(L" + SPECIES + ";)Ljava/lang/String;", false));
        body.add(new InsnNode(Opcodes.ARETURN));
        lambda.instructions = body;
        lambda.tryCatchBlocks.clear();
        if (lambda.localVariables != null) lambda.localVariables.clear();
        write(sp, out.resolve("cobblemorph/command/PokemonSuggestionProvider.class"));

        ClassNode tc = read(in.resolve("cobblemorph/command/TransformCommand.class"));
        if (redirectGetByName(tc) != 1) throw new IllegalStateException("TransformCommand: getByName no encontrado exactamente una vez");
        write(tc, out.resolve("cobblemorph/command/TransformCommand.class"));

        ClassNode layer = read(in.resolve("cobblemorph/client/PokemonTransformLayer.class"));
        if (redirectGetByName(layer) != 1) throw new IllegalStateException("PokemonTransformLayer: getByName no encontrado exactamente una vez");
        write(layer, out.resolve("cobblemorph/client/PokemonTransformLayer.class"));
        System.out.println("patched OK");
    }
}
