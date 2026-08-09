package net.alnv.javareforged.split_transform;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.neoforged.neoforgespi.transformation.ClassProcessor;
import net.neoforged.neoforgespi.transformation.ClassProcessorIds;
import net.neoforged.neoforgespi.transformation.ProcessorName;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RuntimeFieldRedirectLaunchPlugin implements ClassProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeFieldRedirectLaunchPlugin.class);
    private static final boolean LOG_REWRITES = Boolean.getBoolean("split.runtimeRedirectLog");
    private static final boolean BROAD_COMPATIBILITY_SCAN = Boolean.getBoolean("split.runtimeRedirectBroad");
    private static final String CLIENT_ACCESS_OWNER = "net/jr/ClientRuntime/runtime/Client";
    private static final String MINECRAFT_OWNER = "net/minecraft/client/Minecraft";
    private static final String GUI_OWNER = "net/minecraft/client/gui/Gui";
    private static final String GAME_RENDERER_OWNER = "net/minecraft/client/renderer/GameRenderer";
    private static final String LEVEL_RENDERER_OWNER = "net/minecraft/client/renderer/LevelRenderer";
    private static final String PARTICLE_ENGINE_OWNER = "net/minecraft/client/particle/ParticleEngine";
    private static final String PARTICLE_ENGINE_FIELDS_OWNER =
        "net/jr/ClientRuntime/runtime/ParticleEngineFields";
    private static final String RENDER_SECTION_OWNER =
        "net/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection";
    private static final String COMPILE_TASK_OWNER = RENDER_SECTION_OWNER + "$CompileTask";
    private static final String RESORT_TASK_OWNER = RENDER_SECTION_OWNER + "$ResortTransparencyTask";
    private static final String SECTION_DISPATCHER_OWNER =
        "net/minecraft/client/renderer/chunk/SectionRenderDispatcher";
    private static final String TERRAIN_SECTION_OWNERS_OWNER =
        "net/jr/ClientRuntime/runtime/TerrainSectionOwners";
    private static final String TERRAIN_COORDINATOR_OWNER =
        "net/jr/ClientRuntime/runtime/TerrainCoordinator";
    private static final String[] EXCLUDED_CLASS_PREFIXES = {
        "java.",
        "javax.",
        "jdk.",
        "sun.",
        "com.sun.",
        "cpw.mods.modlauncher.",
        "org.objectweb.asm.",
        "org.spongepowered.asm.",
        "net.jr.ClientRuntime.",
        "net.jr.mixin.",
        "net.alnv.javareforged.",
        "net.alnv.mixin."
    };
    /**
     * 26.2 core classes that actually read or write one of the runtime-bound fields.
     * Keeping selection exact avoids asking FML to build an ASM tree for virtually
     * every loaded Minecraft class. The optional broad mode remains available for
     * diagnosing third-party mods that directly access those vanilla fields.
     */
    private static final Set<String> CORE_RUNTIME_FIELD_CONSUMERS = Set.of(
        "com.mojang.blaze3d.platform.FramerateLimitTracker",
        "net.minecraft.client.Camera",
        "net.minecraft.client.KeyboardHandler",
        "net.minecraft.client.Minecraft",
        "net.minecraft.client.MouseHandler",
        "net.minecraft.client.Options",
        "net.minecraft.client.gui.Gui",
        "net.minecraft.client.gui.GuiGraphicsExtractor",
        "net.minecraft.client.gui.Hud",
        "net.minecraft.client.gui.components.AbstractSelectionList",
        "net.minecraft.client.gui.components.CommandSuggestions",
        "net.minecraft.client.gui.components.DebugScreenOverlay",
        "net.minecraft.client.gui.components.PlayerTabOverlay",
        "net.minecraft.client.gui.components.debug.DebugEntryBiome",
        "net.minecraft.client.gui.components.debug.DebugEntryChunkSourceStats",
        "net.minecraft.client.gui.components.debug.DebugEntryHeightmap",
        "net.minecraft.client.gui.components.debug.DebugEntryLight",
        "net.minecraft.client.gui.components.debug.DebugEntryLookingAt",
        "net.minecraft.client.gui.components.debug.DebugEntryLookingAtEntity",
        "net.minecraft.client.gui.components.debug.DebugEntryLookingAtEntityTags",
        "net.minecraft.client.gui.components.debug.DebugEntryPosition",
        "net.minecraft.client.gui.components.debug.DebugEntrySoundMood",
        "net.minecraft.client.gui.components.toasts.RecipeToast",
        "net.minecraft.client.gui.contextualbar.ExperienceBar",
        "net.minecraft.client.gui.contextualbar.JumpableVehicleBar",
        "net.minecraft.client.gui.contextualbar.LocatorBar",
        "net.minecraft.client.gui.screens.ChatScreen",
        "net.minecraft.client.gui.screens.DeathScreen",
        "net.minecraft.client.gui.screens.InBedChatScreen",
        "net.minecraft.client.gui.screens.MenuScreens$ScreenConstructor",
        "net.minecraft.client.gui.screens.MultiplayerOptionsScreen",
        "net.minecraft.client.gui.screens.PauseScreen",
        "net.minecraft.client.gui.screens.Screen",
        "net.minecraft.client.gui.screens.debug.GameModeSwitcherScreen",
        "net.minecraft.client.gui.screens.inventory.AbstractContainerScreen",
        "net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen",
        "net.minecraft.client.gui.screens.inventory.AnvilScreen",
        "net.minecraft.client.gui.screens.inventory.BeaconScreen$BeaconCancelButton",
        "net.minecraft.client.gui.screens.inventory.BeaconScreen$BeaconConfirmButton",
        "net.minecraft.client.gui.screens.inventory.BookViewScreen",
        "net.minecraft.client.gui.screens.inventory.CartographyTableScreen",
        "net.minecraft.client.gui.screens.inventory.CreativeInventoryListener",
        "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen",
        "net.minecraft.client.gui.screens.inventory.EffectsInInventory",
        "net.minecraft.client.gui.screens.inventory.EnchantmentScreen",
        "net.minecraft.client.gui.screens.inventory.InventoryScreen",
        "net.minecraft.client.gui.screens.inventory.LecternScreen",
        "net.minecraft.client.gui.screens.inventory.LoomScreen",
        "net.minecraft.client.gui.screens.inventory.StonecutterScreen",
        "net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen",
        "net.minecraft.client.gui.screens.options.OptionsScreen",
        "net.minecraft.client.gui.screens.options.WorldOptionsScreen",
        "net.minecraft.client.gui.screens.recipebook.RecipeBookComponent",
        "net.minecraft.client.gui.screens.recipebook.RecipeBookPage",
        "net.minecraft.client.gui.screens.social.RemoteFriendListUpdateHandler",
        "net.minecraft.client.gui.screens.social.SocialInteractionsPlayerList",
        "net.minecraft.client.gui.screens.social.SocialInteractionsScreen",
        "net.minecraft.client.multiplayer.ClientAdvancements",
        "net.minecraft.client.multiplayer.ClientLevel",
        "net.minecraft.client.multiplayer.ClientPacketListener",
        "net.minecraft.client.multiplayer.ClientSuggestionProvider",
        "net.minecraft.client.multiplayer.MultiPlayerGameMode",
        "net.minecraft.client.multiplayer.PlayerInfo",
        "net.minecraft.client.multiplayer.chat.ChatListener",
        "net.minecraft.client.particle.SpellParticle",
        "net.minecraft.client.particle.ParticleEngine",
        "net.minecraft.client.player.LocalPlayer",
        "net.minecraft.client.quickplay.QuickPlayLog",
        "net.minecraft.client.renderer.GameRenderer",
        "net.minecraft.client.renderer.ItemInHandRenderer",
        "net.minecraft.client.renderer.LightmapRenderStateExtractor",
        "net.minecraft.client.renderer.ScreenEffectRenderer",
        "net.minecraft.client.renderer.SkyRenderer",
        "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection",
        "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$CompileTask",
        "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$ResortTransparencyTask",
        "net.minecraft.client.renderer.blockentity.AbstractSignRenderer",
        "net.minecraft.client.renderer.blockentity.BeaconRenderer",
        "net.minecraft.client.renderer.blockentity.BlockEntityWithBoundingBoxRenderer",
        "net.minecraft.client.renderer.debug.BeeDebugRenderer",
        "net.minecraft.client.renderer.debug.BrainDebugRenderer",
        "net.minecraft.client.renderer.debug.BreezeDebugRenderer",
        "net.minecraft.client.renderer.debug.ChunkBorderRenderer",
        "net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer",
        "net.minecraft.client.renderer.debug.HeightMapRenderer",
        "net.minecraft.client.renderer.debug.LightDebugRenderer",
        "net.minecraft.client.renderer.debug.LightSectionDebugRenderer",
        "net.minecraft.client.renderer.debug.SolidFaceRenderer",
        "net.minecraft.client.renderer.debug.SupportBlockRenderer",
        "net.minecraft.client.renderer.debug.WaterDebugRenderer",
        "net.minecraft.client.renderer.entity.FishingHookRenderer",
        "net.minecraft.client.renderer.entity.LivingEntityRenderer",
        "net.minecraft.client.renderer.entity.player.AvatarRenderer",
        "net.minecraft.client.renderer.extract.LevelExtractor",
        "net.minecraft.client.renderer.item.properties.conditional.IsViewEntity",
        "net.minecraft.client.server.IntegratedServer",
        "net.minecraft.client.tutorial.CraftPlanksTutorialStep",
        "net.minecraft.client.tutorial.FindTreeTutorialStepInstance",
        "net.minecraft.client.tutorial.PunchTreeTutorialStepInstance",
        "net.minecraft.client.tutorial.Tutorial",
        "net.neoforged.neoforge.client.ClientCommandHandler",
        "net.neoforged.neoforge.client.gui.ConfigurationScreen",
        "net.neoforged.neoforge.client.internal.NeoForgeClientProxy",
        "net.neoforged.neoforge.client.network.ClientPayloadHandler",
        "net.neoforged.neoforge.client.network.handling.ClientPayloadContext",
        "net.neoforged.neoforge.client.registries.ClientRegistryManager"
    );
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
            new FieldKey(GUI_OWNER, "screen", "Lnet/minecraft/client/gui/screens/Screen;"),
            new FieldRedirect(CLIENT_ACCESS_OWNER, "screen")
        ),
        Map.entry(
            new FieldKey(GAME_RENDERER_OWNER, "mainCamera", "Lnet/minecraft/client/Camera;"),
            new FieldRedirect(CLIENT_ACCESS_OWNER, "camera")
        ),
        Map.entry(
            new FieldKey(PARTICLE_ENGINE_OWNER, "particles", "Ljava/util/Map;"),
            new FieldRedirect(PARTICLE_ENGINE_FIELDS_OWNER, "particles")
        ),
        Map.entry(
            new FieldKey(PARTICLE_ENGINE_OWNER, "trackingEmitters", "Ljava/util/Queue;"),
            new FieldRedirect(PARTICLE_ENGINE_FIELDS_OWNER, "trackingEmitters")
        ),
        Map.entry(
            new FieldKey(PARTICLE_ENGINE_OWNER, "particlesToAdd", "Ljava/util/Queue;"),
            new FieldRedirect(PARTICLE_ENGINE_FIELDS_OWNER, "particlesToAdd")
        ),
        Map.entry(
            new FieldKey(
                PARTICLE_ENGINE_OWNER,
                "trackedParticleCounts",
                "Lit/unimi/dsi/fastutil/objects/Object2IntOpenHashMap;"
            ),
            new FieldRedirect(PARTICLE_ENGINE_FIELDS_OWNER, "trackedParticleCounts")
        )
    );

    private static final Map<FieldKey, FieldRedirect> PUTFIELD_REDIRECTS = Map.ofEntries(
        Map.entry(
            new FieldKey(MINECRAFT_OWNER, "gameMode", "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;"),
            new FieldRedirect(CLIENT_ACCESS_OWNER, "setGameMode")
        ),
        Map.entry(
            new FieldKey(MINECRAFT_OWNER, "level", "Lnet/minecraft/client/multiplayer/ClientLevel;"),
            new FieldRedirect(CLIENT_ACCESS_OWNER, "setLevel")
        ),
        Map.entry(
            new FieldKey(MINECRAFT_OWNER, "player", "Lnet/minecraft/client/player/LocalPlayer;"),
            new FieldRedirect(CLIENT_ACCESS_OWNER, "setPlayer")
        ),
        Map.entry(
            new FieldKey(MINECRAFT_OWNER, "crosshairPickEntity", "Lnet/minecraft/world/entity/Entity;"),
            new FieldRedirect(CLIENT_ACCESS_OWNER, "setCrosshairPickEntity")
        ),
        Map.entry(
            new FieldKey(MINECRAFT_OWNER, "hitResult", "Lnet/minecraft/world/phys/HitResult;"),
            new FieldRedirect(CLIENT_ACCESS_OWNER, "setHitResult")
        ),
        Map.entry(
            new FieldKey(GUI_OWNER, "screen", "Lnet/minecraft/client/gui/screens/Screen;"),
            new FieldRedirect(CLIENT_ACCESS_OWNER, "bindScreen")
        )
    );

    private static final Map<MethodKey, MethodRedirect> METHOD_REDIRECTS = Map.ofEntries(
        Map.entry(
            new MethodKey(GAME_RENDERER_OWNER, "mainCamera", "()Lnet/minecraft/client/Camera;"),
            new MethodRedirect(CLIENT_ACCESS_OWNER, "camera", "(L" + GAME_RENDERER_OWNER + ";)Lnet/minecraft/client/Camera;")
        ),
        Map.entry(
            new MethodKey(MINECRAFT_OWNER, "getCameraEntity", "()Lnet/minecraft/world/entity/Entity;"),
            new MethodRedirect(CLIENT_ACCESS_OWNER, "cameraEntity", "(L" + MINECRAFT_OWNER + ";)Lnet/minecraft/world/entity/Entity;")
        ),
        Map.entry(
            new MethodKey(MINECRAFT_OWNER, "setCameraEntity", "(Lnet/minecraft/world/entity/Entity;)V"),
            new MethodRedirect(CLIENT_ACCESS_OWNER, "setCameraEntity", "(L" + MINECRAFT_OWNER + ";Lnet/minecraft/world/entity/Entity;)V")
        )
    );

    @Override
    public ProcessorName name() {
        return new ProcessorName("java_reforged", "runtime_field_redirects");
    }

    @Override
    public Set<ProcessorName> runsAfter() {
        return Set.of(ClassProcessorIds.MIXIN);
    }

    @Override
    public boolean handlesClass(SelectionContext context) {
        return !context.empty() && shouldTransformClass(context.type().getClassName());
    }

    @Override
    public ComputeFlags processClass(TransformationContext context) {
        ClassNode classNode = context.node();
        Type classType = context.type();
        if (!shouldTransformClass(classType.getClassName())) {
            return ComputeFlags.NO_REWRITE;
        }

        int redirectedReads = 0;
        int redirectedWrites = 0;
        int redirectedCalls = 0;
        int terrainHooks = 0;
        for (MethodNode method : classNode.methods) {
            FieldRewriteCount count = rewriteRuntimeFieldAccesses(classNode, method);
            redirectedReads += count.reads();
            redirectedWrites += count.writes();
            redirectedCalls += rewriteRuntimeMethodCalls(classNode, method);
            terrainHooks += rewriteTerrainLifecycle(classNode, method);
        }

        if (redirectedReads == 0 && redirectedWrites == 0 && redirectedCalls == 0 && terrainHooks == 0) {
            return ComputeFlags.NO_REWRITE;
        }

        context.audit(
            "redirected runtime-bound Minecraft state",
            Integer.toString(redirectedReads),
            Integer.toString(redirectedWrites),
            Integer.toString(redirectedCalls),
            Integer.toString(terrainHooks)
        );
        logRewrite(classType.getClassName(), redirectedReads, redirectedWrites, "fml-class-processor");
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
            if (isRawStateBoundRenderMethod(classNode, method) && !isSlotCameraField(key)) {
                insn = nextInsn;
                continue;
            }
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

    private static int rewriteRuntimeMethodCalls(ClassNode classNode, MethodNode method) {
        if (shouldSkipMethod(classNode, method)) {
            return 0;
        }

        int redirectedCalls = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; ) {
            AbstractInsnNode nextInsn = insn.getNext();
            if (!(insn instanceof MethodInsnNode methodInsn)
                || (methodInsn.getOpcode() != Opcodes.INVOKEVIRTUAL && methodInsn.getOpcode() != Opcodes.INVOKEINTERFACE)) {
                insn = nextInsn;
                continue;
            }

            MethodRedirect redirect = METHOD_REDIRECTS.get(new MethodKey(methodInsn.owner, methodInsn.name, methodInsn.desc));
            if (redirect == null) {
                insn = nextInsn;
                continue;
            }

            method.instructions.set(
                methodInsn,
                new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    redirect.accessOwner(),
                    redirect.accessorName(),
                    redirect.accessorDescriptor(),
                    false
                )
            );
            redirectedCalls++;
            insn = nextInsn;
        }
        return redirectedCalls;
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

            InsnList discardCapturedConsumer = new InsnList();
            discardCapturedConsumer.add(new InsnNode(Opcodes.SWAP));
            discardCapturedConsumer.add(new InsnNode(Opcodes.POP));
            method.instructions.insertBefore(methodInsn, discardCapturedConsumer);
            method.instructions.set(methodInsn, new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                TERRAIN_COORDINATOR_OWNER,
                "onSectionCompiled",
                "(L" + RENDER_SECTION_OWNER + ";)V",
                false
            ));
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
            hooks += rewriteTaskCameraReference(method);
            hooks += insertTaskFinishedBeforeReturns(method, Opcodes.ARETURN);
            return hooks;
        }
        if ("cancel".equals(method.name) && "()V".equals(method.desc)) {
            return insertTaskFinishedBeforeReturns(method, Opcodes.RETURN);
        }
        return 0;
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

    private static boolean shouldSkipMethod(ClassNode classNode, MethodNode method) {
        return method.name.startsWith("splitTest$")
            || method.name.startsWith("handler$")
            || method.name.startsWith("redirect$")
            || (GAME_RENDERER_OWNER.equals(classNode.name) && "mainCamera".equals(method.name))
            || (MINECRAFT_OWNER.equals(classNode.name) && "fillReport".equals(method.name))
            || "<init>".equals(method.name)
            || "<clinit>".equals(method.name);
    }

    private static boolean isSlotCameraField(FieldKey key) {
        return GAME_RENDERER_OWNER.equals(key.owner())
            && "mainCamera".equals(key.name())
            && "Lnet/minecraft/client/Camera;".equals(key.desc());
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
        return BROAD_COMPATIBILITY_SCAN || CORE_RUNTIME_FIELD_CONSUMERS.contains(className);
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

    private record MethodKey(String owner, String name, String desc) {
    }

    private record MethodRedirect(String accessOwner, String accessorName, String accessorDescriptor) {
    }

    private record FieldRewriteCount(int reads, int writes) {
    }
}
