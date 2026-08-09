package net.alnv.javareforged.split_transform;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RuntimeFieldRedirectLaunchPlugin implements ILaunchPluginService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeFieldRedirectLaunchPlugin.class);
    private static final boolean LOG_REWRITES = Boolean.getBoolean("split.runtimeRedirectLog");
    private static final String CLIENT_ACCESS_OWNER = "net/alnv/javareforged/ClientRuntime/runtime/Client";
    private static final String MINECRAFT_OWNER = "net/minecraft/client/Minecraft";
    private static final String GAME_RENDERER_OWNER = "net/minecraft/client/renderer/GameRenderer";
    private static final String LEVEL_RENDERER_OWNER = "net/minecraft/client/renderer/LevelRenderer";
    private static final String[] EXCLUDED_CLASS_PREFIXES = {
        "java.",
        "javax.",
        "jdk.",
        "sun.",
        "com.sun.",
        "cpw.mods.modlauncher.",
        "org.objectweb.asm.",
        "org.spongepowered.asm.",
        "net.alnv.javareforged.",
        "net.alnv.mixin."
    };
    private static final Map<String, Set<String>> RAW_STATE_BOUND_RENDER_METHODS = Map.of(
        LEVEL_RENDERER_OWNER,
        Set.of(
            "renderLevel",
            "renderSky",
            "renderSnowAndRain",
            "setupRender",
            "shouldShowEntityOutlines",
            "tickRain"
        ),
        GAME_RENDERER_OWNER,
        Set.of(
            "renderLevel",
            "renderItemInHand",
            "shouldRenderBlockOutline"
        )
    );
    private static final AtomicBoolean FIRST_REWRITE_LOGGED = new AtomicBoolean();
    private static final AtomicInteger REWRITTEN_CLASS_COUNT = new AtomicInteger();
    private static final AtomicInteger REWRITTEN_FIELD_READ_COUNT = new AtomicInteger();
    private static final AtomicInteger REWRITTEN_FIELD_WRITE_COUNT = new AtomicInteger();

    private static final Map<FieldKey, FieldRedirect> GETFIELD_REDIRECTS = Map.ofEntries(
        Map.entry(
            new FieldKey(MINECRAFT_OWNER, "cameraEntity", "Lnet/minecraft/world/entity/Entity;"),
            new FieldRedirect(CLIENT_ACCESS_OWNER, "cameraEntity")
        ),
        Map.entry(
            new FieldKey(MINECRAFT_OWNER, "crosshairPickEntity", "Lnet/minecraft/world/entity/Entity;"),
            new FieldRedirect(CLIENT_ACCESS_OWNER, "crosshairPickEntity")
        ),
        Map.entry(
            new FieldKey(MINECRAFT_OWNER, "gameMode", "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;"),
            new FieldRedirect(CLIENT_ACCESS_OWNER, "gameMode")
        ),
        Map.entry(
            new FieldKey(MINECRAFT_OWNER, "hitResult", "Lnet/minecraft/world/phys/HitResult;"),
            new FieldRedirect(CLIENT_ACCESS_OWNER, "hitResult")
        ),
        Map.entry(
            new FieldKey(MINECRAFT_OWNER, "level", "Lnet/minecraft/client/multiplayer/ClientLevel;"),
            new FieldRedirect(CLIENT_ACCESS_OWNER, "level")
        ),
        Map.entry(new FieldKey(MINECRAFT_OWNER, "noRender", "Z"), new FieldRedirect(CLIENT_ACCESS_OWNER, "noRender")),
        Map.entry(
            new FieldKey(MINECRAFT_OWNER, "player", "Lnet/minecraft/client/player/LocalPlayer;"),
            new FieldRedirect(CLIENT_ACCESS_OWNER, "player")
        ),
        Map.entry(
            new FieldKey(MINECRAFT_OWNER, "screen", "Lnet/minecraft/client/gui/screens/Screen;"),
            new FieldRedirect(CLIENT_ACCESS_OWNER, "screen")
        )
    );

    private static final Map<FieldKey, FieldRedirect> PUTFIELD_REDIRECTS = Map.ofEntries(
        Map.entry(
            new FieldKey(MINECRAFT_OWNER, "crosshairPickEntity", "Lnet/minecraft/world/entity/Entity;"),
            new FieldRedirect(CLIENT_ACCESS_OWNER, "setCrosshairPickEntity")
        ),
        Map.entry(
            new FieldKey(MINECRAFT_OWNER, "hitResult", "Lnet/minecraft/world/phys/HitResult;"),
            new FieldRedirect(CLIENT_ACCESS_OWNER, "setHitResult")
        )
    );

    @Override
    public String name() {
        return "javareforged_runtime_field_redirects";
    }

    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
        return this.handlesClass(classType, isEmpty, "classloading");
    }

    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty, String reason) {
        if (isEmpty || !shouldTransformClass(classType.getClassName())) {
            return EnumSet.noneOf(Phase.class);
        }
        return EnumSet.of(Phase.AFTER);
    }

    @Override
    public int processClassWithFlags(Phase phase, ClassNode classNode, Type classType, String reason) {
        if (phase != Phase.AFTER || !shouldTransformClass(classType.getClassName())) {
            return ComputeFlags.NO_REWRITE;
        }

        int redirectedReads = 0;
        int redirectedWrites = 0;
        for (MethodNode method : classNode.methods) {
            FieldRewriteCount count = rewriteRuntimeFieldAccesses(classNode, method);
            redirectedReads += count.reads();
            redirectedWrites += count.writes();
        }

        if (redirectedReads == 0 && redirectedWrites == 0) {
            return ComputeFlags.NO_REWRITE;
        }

        logRewrite(classType.getClassName(), redirectedReads, redirectedWrites, reason);
        return ComputeFlags.SIMPLE_REWRITE;
    }

    private static FieldRewriteCount rewriteRuntimeFieldAccesses(ClassNode classNode, MethodNode method) {
        if (shouldSkipMethod(classNode, method)) {
            return new FieldRewriteCount(0, 0);
        }

        int redirectedReads = 0;
        int redirectedWrites = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; ) {
            AbstractInsnNode nextInsn = insn.getNext();
            if (!(insn instanceof FieldInsnNode fieldInsn)) {
                insn = nextInsn;
                continue;
            }
            FieldKey key = new FieldKey(fieldInsn.owner, fieldInsn.name, fieldInsn.desc);
            FieldRedirect redirect;
            if (fieldInsn.getOpcode() == Opcodes.GETFIELD) {
                redirect = GETFIELD_REDIRECTS.get(key);
                if (redirect == null) {
                    insn = nextInsn;
                    continue;
                }
                method.instructions.set(
                    fieldInsn,
                    new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        redirect.accessOwner(),
                        redirect.accessorName(),
                        "(L" + fieldInsn.owner + ";)" + fieldInsn.desc,
                        false
                    )
                );
                redirectedReads++;
                insn = nextInsn;
                continue;
            }

            if (fieldInsn.getOpcode() != Opcodes.PUTFIELD) {
                insn = nextInsn;
                continue;
            }
            redirect = PUTFIELD_REDIRECTS.get(key);
            if (redirect == null) {
                insn = nextInsn;
                continue;
            }
            method.instructions.set(
                fieldInsn,
                new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    redirect.accessOwner(),
                    redirect.accessorName(),
                    "(L" + fieldInsn.owner + ";" + fieldInsn.desc + ")V",
                    false
                )
            );
            redirectedWrites++;
            insn = nextInsn;
        }
        return new FieldRewriteCount(redirectedReads, redirectedWrites);
    }

    private static boolean shouldSkipMethod(ClassNode classNode, MethodNode method) {
        return method.name.startsWith("splitTest$")
            || method.name.startsWith("handler$")
            || method.name.startsWith("redirect$")
            || isRawStateBoundRenderMethod(classNode, method)
            || (MINECRAFT_OWNER.equals(classNode.name) && "fillReport".equals(method.name))
            || "<init>".equals(method.name)
            || "<clinit>".equals(method.name);
    }

    private static boolean isRawStateBoundRenderMethod(ClassNode classNode, MethodNode method) {
        Set<String> methods = RAW_STATE_BOUND_RENDER_METHODS.get(classNode.name);
        return methods != null && methods.contains(method.name);
    }

    private static boolean shouldTransformClass(String className) {
        for (String excludedPrefix : EXCLUDED_CLASS_PREFIXES) {
            if (className.startsWith(excludedPrefix)) {
                return false;
            }
        }
        return true;
    }

    private static void logRewrite(String className, int redirectedReads, int redirectedWrites, String reason) {
        int classCount = REWRITTEN_CLASS_COUNT.incrementAndGet();
        int fieldReadCount = REWRITTEN_FIELD_READ_COUNT.addAndGet(redirectedReads);
        int fieldWriteCount = REWRITTEN_FIELD_WRITE_COUNT.addAndGet(redirectedWrites);
        if (!LOG_REWRITES) {
            return;
        }
        if (FIRST_REWRITE_LOGGED.compareAndSet(false, true)) {
            LOGGER.info(
                "JavaReforged runtime field redirect rewrote first class: {} ({} reads, {} writes, reason={}, totals: {} classes / {} reads / {} writes)",
                className,
                redirectedReads,
                redirectedWrites,
                reason,
                classCount,
                fieldReadCount,
                fieldWriteCount
            );
            return;
        }
        LOGGER.debug(
            "JavaReforged runtime field redirect rewrote class: {} ({} reads, {} writes, reason={}, totals: {} classes / {} reads / {} writes)",
            className,
            redirectedReads,
            redirectedWrites,
            reason,
            classCount,
            fieldReadCount,
            fieldWriteCount
        );
    }

    private record FieldKey(String owner, String name, String desc) {
    }

    private record FieldRedirect(String accessOwner, String accessorName) {
    }

    private record FieldRewriteCount(int reads, int writes) {
    }
}
