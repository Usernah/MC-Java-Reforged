package net.alnv.javareforged.runtime_transform;

import java.util.Set;
import net.neoforged.neoforgespi.transformation.ClassProcessor;
import net.neoforged.neoforgespi.transformation.ClassProcessorIds;
import net.neoforged.neoforgespi.transformation.ProcessorName;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public final class BetterCombatCompatProcessor implements ClassProcessor {
    private static final String MINECRAFT_OWNER = "net/minecraft/client/Minecraft";
    private static final String ATTACK_INTERACTOR_DESCRIPTOR = "()Lnet/bettercombat/client/AttackInteractor;";
    private static final String INTERACTORS_OWNER = "net/jr/client/compat/bettercombat/BetterCombatInteractors";

    @Override
    public ProcessorName name() {
        return new ProcessorName("java_reforged", "better_combat_compat");
    }

    @Override
    public Set<ProcessorName> runsAfter() {
        return Set.of(ClassProcessorIds.MIXIN);
    }

    @Override
    public boolean handlesClass(SelectionContext context) {
        return !context.empty() && "net.minecraft.client.Minecraft".equals(context.type().getClassName());
    }

    @Override
    public ComputeFlags processClass(TransformationContext context) {
        ClassNode classNode = context.node();
        int redirects = 0;
        for (MethodNode method : classNode.methods) {
            redirects += redirectInteractorCalls(method);
        }
        if (redirects == 0) {
            return ComputeFlags.NO_REWRITE;
        }
        context.audit("redirected Better Combat interactors", Integer.toString(redirects));
        return ComputeFlags.SIMPLE_REWRITE;
    }

    private static int redirectInteractorCalls(MethodNode method) {
        int redirects = 0;
        for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; ) {
            AbstractInsnNode next = instruction.getNext();
            if (instruction instanceof MethodInsnNode call
                && MINECRAFT_OWNER.equals(call.owner)
                && ATTACK_INTERACTOR_DESCRIPTOR.equals(call.desc)
                && call.name.endsWith("interactor")) {
                method.instructions.set(
                    call,
                    new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        INTERACTORS_OWNER,
                        "current",
                        "(Lnet/minecraft/client/Minecraft;)Lnet/bettercombat/client/AttackInteractor;",
                        false
                    )
                );
                redirects++;
            }
            instruction = next;
        }
        return redirects;
    }
}
