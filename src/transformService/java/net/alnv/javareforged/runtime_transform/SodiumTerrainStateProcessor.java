package net.alnv.javareforged.runtime_transform;

import java.util.Set;
import net.neoforged.neoforgespi.transformation.ClassProcessor;
import net.neoforged.neoforgespi.transformation.ClassProcessorIds;
import net.neoforged.neoforgespi.transformation.ProcessorName;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;


public final class SodiumTerrainStateProcessor implements ClassProcessor {
    private static final String TARGET_CLASS =
        "net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager";
    private static final String TARGET_OWNER = TARGET_CLASS.replace('.', '/');
    private static final String LEVEL_DESC = "Lnet/minecraft/client/multiplayer/ClientLevel;";
    private static final String CACHE_DESC =
        "Lnet/caffeinemc/mods/sodium/client/world/cloned/ClonedChunkSectionCache;";
    private static final String BUILDER_DESC =
        "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/executor/ChunkBuilder;";
    private static final String SOURCE_ACCESS =
        "net/jr/client/compat/sodium/SodiumTerrainSourceAccess";
    private static final String SOURCE_METHOD = "javaReforged$setTerrainSource";

    @Override
    public ProcessorName name() {
        return new ProcessorName("java_reforged", "sodium_terrain_state");
    }

    @Override
    public Set<ProcessorName> runsAfter() {
        return Set.of(
            ClassProcessorIds.MIXIN,
            new ProcessorName("java_reforged", "runtime_field_redirects")
        );
    }

    @Override
    public boolean handlesClass(SelectionContext context) {
        return OptionalModPresence.sodiumLoaded()
            && !context.empty()
            && TARGET_CLASS.equals(context.type().getClassName());
    }

    @Override
    public ComputeFlags processClass(TransformationContext context) {
        ClassNode classNode = context.node();
        FieldNode level = field(classNode, "level", LEVEL_DESC);
        FieldNode sectionCache = field(classNode, "sectionCache", CACHE_DESC);
        FieldNode builder = field(classNode, "builder", BUILDER_DESC);
        level.access &= ~Opcodes.ACC_FINAL;
        sectionCache.access &= ~Opcodes.ACC_FINAL;
        builder.access &= ~Opcodes.ACC_FINAL;
        if (!classNode.interfaces.contains(SOURCE_ACCESS)) {
            classNode.interfaces.add(SOURCE_ACCESS);
        }

        classNode.methods.removeIf(method ->
            SOURCE_METHOD.equals(method.name)
                && ("(" + LEVEL_DESC + "Ljava/lang/Object;)V").equals(method.desc)
        );
        classNode.methods.add(createSourceMethod());
        context.audit("installed Sodium split terrain-source state boundary");
        return ComputeFlags.SIMPLE_REWRITE;
    }

    private static FieldNode field(ClassNode owner, String name, String descriptor) {
        for (FieldNode field : owner.fields) {
            if (name.equals(field.name) && descriptor.equals(field.desc)) {
                return field;
            }
        }
        throw new IllegalStateException(
            "Unsupported Sodium RenderSectionManager: missing " + name + descriptor
        );
    }

    private static MethodNode createSourceMethod() {
        MethodNode method = new MethodNode(
            Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
            SOURCE_METHOD,
            "(" + LEVEL_DESC + "Ljava/lang/Object;)V",
            null,
            null
        );
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new FieldInsnNode(
            Opcodes.PUTFIELD,
            TARGET_OWNER,
            "level",
            LEVEL_DESC
        ));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        method.instructions.add(new TypeInsnNode(
            Opcodes.CHECKCAST,
            "net/caffeinemc/mods/sodium/client/world/cloned/ClonedChunkSectionCache"
        ));
        method.instructions.add(new FieldInsnNode(
            Opcodes.PUTFIELD,
            TARGET_OWNER,
            "sectionCache",
            CACHE_DESC
        ));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.maxStack = 2;
        method.maxLocals = 3;
        return method;
    }
}
