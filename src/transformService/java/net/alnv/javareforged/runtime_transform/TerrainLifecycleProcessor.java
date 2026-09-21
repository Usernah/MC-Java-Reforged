package net.alnv.javareforged.runtime_transform;

import java.util.Set;
import net.neoforged.neoforgespi.transformation.ClassProcessor;
import net.neoforged.neoforgespi.transformation.ClassProcessorIds;
import net.neoforged.neoforgespi.transformation.ProcessorName;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public final class TerrainLifecycleProcessor implements ClassProcessor {
    private static final String RENDER_SECTION_OWNER =
        "net/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection";
    private static final String COMPILE_TASK_OWNER = RENDER_SECTION_OWNER + "$CompileTask";
    private static final String RESORT_TASK_OWNER = RENDER_SECTION_OWNER + "$ResortTransparencyTask";
    private static final String SECTION_DISPATCHER_OWNER =
        "net/minecraft/client/renderer/chunk/SectionRenderDispatcher";
    private static final String TERRAIN_SECTION_OWNERS_OWNER =
        "net/jr/client/runtime/terrain/TerrainTaskOwnership";
    private static final String TERRAIN_COORDINATOR_OWNER =
        "net/jr/client/runtime/terrain/TerrainCoordinator";
    private static final Set<String> TARGET_CLASSES = Set.of(
        "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection",
        "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$CompileTask",
        "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$ResortTransparencyTask"
    );

    @Override
    public ProcessorName name() {
        return new ProcessorName("java_reforged", "terrain_lifecycle");
    }

    @Override
    public Set<ProcessorName> runsAfter() {
        return Set.of(ClassProcessorIds.MIXIN);
    }

    @Override
    public boolean handlesClass(SelectionContext context) {
        return !context.empty() && TARGET_CLASSES.contains(context.type().getClassName());
    }

    @Override
    public ComputeFlags processClass(TransformationContext context) {
        ClassNode classNode = context.node();
        int hooks = 0;
        for (MethodNode method : classNode.methods) {
            hooks += rewriteTerrainLifecycle(classNode, method);
        }
        if (hooks == 0) {
            return ComputeFlags.NO_REWRITE;
        }
        context.audit("installed JavaReforged terrain lifecycle hooks", Integer.toString(hooks));
        return ComputeFlags.SIMPLE_REWRITE;
    }

    private static int rewriteTerrainLifecycle(ClassNode classNode, MethodNode method) {
        if (RENDER_SECTION_OWNER.equals(classNode.name)) {
            return rewriteRenderSectionLifecycle(method);
        }
        if (COMPILE_TASK_OWNER.equals(classNode.name) || RESORT_TASK_OWNER.equals(classNode.name)) {
            return rewriteSectionTaskLifecycle(classNode, method);
        }
        return 0;
    }

    private static int rewriteRenderSectionLifecycle(MethodNode method) {
        if (!"setSectionMesh".equals(method.name)
            || !("(Lnet/minecraft/client/renderer/chunk/SectionMesh;)"
                + "Lnet/minecraft/client/renderer/chunk/SectionMesh;").equals(method.desc)) {
            return 0;
        }

        int hooks = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; ) {
            AbstractInsnNode nextInsn = insn.getNext();
            if (!(insn instanceof MethodInsnNode methodInsn)
                || methodInsn.getOpcode() != Opcodes.INVOKEINTERFACE
                || !"java/util/function/Consumer".equals(methodInsn.owner)
                || !"accept".equals(methodInsn.name)
                || !"(Ljava/lang/Object;)V".equals(methodInsn.desc)) {
                insn = nextInsn;
                continue;
            }

            InsnList notifyPlayerGraphs = new InsnList();
            notifyPlayerGraphs.add(new VarInsnNode(Opcodes.ALOAD, 0));
            notifyPlayerGraphs.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                TERRAIN_COORDINATOR_OWNER,
                "onSectionCompiled",
                "(L" + RENDER_SECTION_OWNER + ";)V",
                false
            ));
            method.instructions.insert(methodInsn, notifyPlayerGraphs);
            hooks++;
            insn = nextInsn;
        }
        return hooks;
    }

    private static int rewriteSectionTaskLifecycle(ClassNode classNode, MethodNode method) {
        int hooks = 0;
        if ("<init>".equals(method.name)) {
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn.getOpcode() != Opcodes.RETURN) {
                    continue;
                }
                InsnList hook = new InsnList();
                hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
                hook.add(new FieldInsnNode(
                    Opcodes.GETFIELD,
                    classNode.name,
                    "this$1",
                    "L" + RENDER_SECTION_OWNER + ";"
                ));
                hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
                hook.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    TERRAIN_SECTION_OWNERS_OWNER,
                    "taskCreated",
                    "(L" + RENDER_SECTION_OWNER + ";Ljava/lang/Object;)V",
                    false
                ));
                method.instructions.insertBefore(insn, hook);
                hooks++;
            }
            return hooks;
        }

        if ("doTask".equals(method.name)) {
            hooks += insertTaskExecutingAtHead(method);
            hooks += rewriteTaskCameraReference(method);
            hooks += insertTaskFinishedBeforeReturns(method, Opcodes.ARETURN);
            return hooks;
        }
        if ("cancel".equals(method.name) && "()V".equals(method.desc)) {
            return insertTaskCancelledBeforeReturns(method);
        }
        return 0;
    }

    private static int insertTaskExecutingAtHead(MethodNode method) {
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            TERRAIN_SECTION_OWNERS_OWNER,
            "taskExecuting",
            "(Ljava/lang/Object;)V",
            false
        ));
        method.instructions.insert(hook);
        return 1;
    }

    private static int rewriteTaskCameraReference(MethodNode method) {
        int hooks = 0;
        String cameraReferenceDesc = "Ljava/util/concurrent/atomic/AtomicReference;";
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; ) {
            AbstractInsnNode nextInsn = insn.getNext();
            if (!(insn instanceof FieldInsnNode fieldInsn)
                || fieldInsn.getOpcode() != Opcodes.GETFIELD
                || !SECTION_DISPATCHER_OWNER.equals(fieldInsn.owner)
                || !"cameraPosition".equals(fieldInsn.name)
                || !cameraReferenceDesc.equals(fieldInsn.desc)) {
                insn = nextInsn;
                continue;
            }

            InsnList replacement = new InsnList();
            replacement.add(new InsnNode(Opcodes.POP));
            replacement.add(new VarInsnNode(Opcodes.ALOAD, 0));
            replacement.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                TERRAIN_SECTION_OWNERS_OWNER,
                "cameraReferenceForTask",
                "(Ljava/lang/Object;)" + cameraReferenceDesc,
                false
            ));
            method.instructions.insertBefore(fieldInsn, replacement);
            method.instructions.remove(fieldInsn);
            hooks++;
            insn = nextInsn;
        }
        return hooks;
    }

    private static int insertTaskFinishedBeforeReturns(MethodNode method, int returnOpcode) {
        int hooks = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() != returnOpcode) {
                continue;
            }
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                TERRAIN_SECTION_OWNERS_OWNER,
                "taskFinished",
                "(Ljava/lang/Object;)V",
                false
            ));
            method.instructions.insertBefore(insn, hook);
            hooks++;
        }
        return hooks;
    }

    private static int insertTaskCancelledBeforeReturns(MethodNode method) {
        int hooks = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() != Opcodes.RETURN) {
                continue;
            }
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                TERRAIN_SECTION_OWNERS_OWNER,
                "taskCancelled",
                "(Ljava/lang/Object;)V",
                false
            ));
            method.instructions.insertBefore(insn, hook);
            hooks++;
        }
        return hooks;
    }

}
