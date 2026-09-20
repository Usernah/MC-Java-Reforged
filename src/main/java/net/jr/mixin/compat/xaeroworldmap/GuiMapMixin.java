package net.jr.mixin.compat.xaeroworldmap;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.jr.client.compat.xaero.XaeroWorldMapRuntime;
import net.jr.client.compat.xaero.XaeroWorldMapMinimapScope;
import net.jr.client.compat.xaero.XaeroWorldMapViewState;
import net.jr.client.compat.xaero.XaeroWorldMapViewport;
import net.jr.client.render.GuiGraphicsExtractorBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.map.graphics.ImprovedFramebuffer;
import xaero.map.gui.GuiMap;
import xaero.map.gui.IRightClickableElement;
import xaero.map.mods.SupportXaeroMinimap;

@Mixin(value = GuiMap.class, remap = false)
abstract class GuiMapMixin {

    /*
     * ============================================================
     * XAERO STATIC STATE
     * ============================================================
     *
     * GuiMap was written assuming that only one fullscreen map can
     * exist at a time.
     *
     * Java Reforged can have one GuiMap per slot, so these fields
     * must be rebound to the state of the active slot while Xaero
     * executes.
     */

    @Shadow
    private static double destScale;

    @Shadow
    private static ImprovedFramebuffer primaryScaleFBO;

    @Shadow
    private static ImprovedFramebuffer immediateRenderFBO;

    @Shadow
    private static boolean attachedCamera;

    @Shadow
    public static boolean hiddenUI;

    @Shadow
    private static int lastAmountOfRegionsViewed;

    @Shadow
    private double screenScale;

    /*
     * The scissor already belongs to Java Reforged's GUI viewport
     * pipeline. We only cache the reference while Xaero is extracting
     * this GuiMap.
     */
    @Unique
    @Nullable
    private ScreenRectangle javaReforged$viewportScissor;

    /*
     * Calls can nest:
     *
     * extractRenderState()
     *     -> changeZoom()
     *     -> applyZoomLimits()
     *
     * Only the outermost entry swaps the Xaero static state.
     */
    @Unique
    private static final ThreadLocal<Integer> javaReforged$stateDepth =
            ThreadLocal.withInitial(() -> 0);

    @Unique
    private static final ThreadLocal<XaeroStaticBackup> javaReforged$backup =
            new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<XaeroWorldMapViewState> javaReforged$activeState =
            new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<XaeroWorldMapMinimapScope> javaReforged$minimapScope =
            new ThreadLocal<>();

    @Unique
    private static void javaReforged$enterState() {
        int depth = javaReforged$stateDepth.get();

        if (depth > 0) {
            javaReforged$stateDepth.set(depth + 1);
            return;
        }

        XaeroWorldMapViewState state =
                XaeroWorldMapRuntime.currentViewStateOrNull();

        javaReforged$stateDepth.set(1);

        if (state == null) {
            return;
        }

        javaReforged$backup.set(
                new XaeroStaticBackup(
                        destScale,
                        primaryScaleFBO,
                        immediateRenderFBO,
                        attachedCamera,
                        hiddenUI,
                        lastAmountOfRegionsViewed
                )
        );

        javaReforged$activeState.set(state);
        javaReforged$minimapScope.set(
                XaeroWorldMapMinimapScope.activate(state)
        );

        destScale = state.destScale();
        primaryScaleFBO = state.primaryScaleFbo();
        immediateRenderFBO = state.immediateRenderFbo();
        attachedCamera = state.attachedCamera();
        hiddenUI = state.hiddenUi();
        lastAmountOfRegionsViewed =
                state.lastAmountOfRegionsViewed();
    }

    @Unique
    private static void javaReforged$exitState() {
        int depth = javaReforged$stateDepth.get();

        if (depth <= 0) {
            return;
        }

        depth--;

        if (depth > 0) {
            javaReforged$stateDepth.set(depth);
            return;
        }

        javaReforged$stateDepth.remove();

        XaeroWorldMapViewState state =
                javaReforged$activeState.get();

        if (state == null) {
            javaReforged$minimapScope.remove();
            javaReforged$activeState.remove();
            javaReforged$backup.remove();
            return;
        }

        /*
         * Persist everything Xaero changed while the slot was active.
         */
        state.destScale(destScale);
        state.primaryScaleFbo(primaryScaleFBO);
        state.immediateRenderFbo(immediateRenderFBO);
        state.attachedCamera(attachedCamera);
        state.hiddenUi(hiddenUI);
        state.lastAmountOfRegionsViewed(
                lastAmountOfRegionsViewed
        );

        XaeroWorldMapMinimapScope minimapScope =
                javaReforged$minimapScope.get();

        if (minimapScope != null) {
            minimapScope.close();
        }

        /*
         * Restore the real global values so code outside the current
         * slot scope does not inherit the slot-local state.
         */
        XaeroStaticBackup backup =
                javaReforged$backup.get();

        if (backup != null) {
            destScale = backup.destScale();
            primaryScaleFBO = backup.primaryScaleFbo();
            immediateRenderFBO = backup.immediateRenderFbo();
            attachedCamera = backup.attachedCamera();
            hiddenUI = backup.hiddenUi();
            lastAmountOfRegionsViewed =
                    backup.lastAmountOfRegionsViewed();
        }

        javaReforged$activeState.remove();
        javaReforged$minimapScope.remove();
        javaReforged$backup.remove();
    }

    /*
     * ============================================================
     * CONSTRUCTOR
     * ============================================================
     *
     * GuiMap constructor reads destScale three times:
     *
     *   userScale = destScale * ...
     *   new SlowingAnimation(userScale, destScale, ..., destScale * ...)
     *
     * The constructor happens before any ordinary method can be wrapped,
     * so redirect those static reads to the current slot state.
     */

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "FIELD",
                    target = "Lxaero/map/gui/GuiMap;destScale:D",
                    opcode = Opcodes.GETSTATIC
            )
    )
    private double javaReforged$constructorDestScale() {
        XaeroWorldMapViewState state =
                XaeroWorldMapRuntime.currentViewStateOrNull();

        return state != null
                ? state.destScale()
                : destScale;
    }

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lxaero/map/mods/SupportXaeroMinimap;"
                                    + "onMapConstruct()V"
            )
    )
    private void javaReforged$constructMinimapBridgePerSlot(
            SupportXaeroMinimap support
    ) {
        XaeroWorldMapViewState state =
                XaeroWorldMapRuntime.currentViewStateOrNull();

        try (XaeroWorldMapMinimapScope ignored =
                     XaeroWorldMapMinimapScope.activate(state)) {
            support.onMapConstruct();
        }
    }

    /*
     * ============================================================
     * INIT
     * ============================================================
     *
     * init() reads/writes attachedCamera and reads the GUI scale.
     */

    @WrapMethod(method = "init")
    private void javaReforged$initPerSlot(
            Operation<Void> original
    ) {
        javaReforged$enterState();

        try {
            original.call();
        } finally {
            javaReforged$exitState();
        }
    }

    @Redirect(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lcom/mojang/blaze3d/platform/Window;getGuiScale()I"
            )
    )
    private int javaReforged$viewportGuiScale(
            Window window
    ) {
        return XaeroWorldMapViewport.guiScale(window);
    }

    /*
     * ============================================================
     * ZOOM
     * ============================================================
     *
     * Both methods directly read/write static destScale.
     */

    @WrapMethod(method = "changeZoom")
    private void javaReforged$changeZoomPerSlot(
            double amount,
            int method,
            Operation<Void> original
    ) {
        javaReforged$enterState();

        try {
            original.call(amount, method);
        } finally {
            javaReforged$exitState();
        }
    }

    @WrapMethod(method = "applyZoomLimits")
    private void javaReforged$applyZoomLimitsPerSlot(
            Operation<Void> original
    ) {
        javaReforged$enterState();

        try {
            original.call();
        } finally {
            javaReforged$exitState();
        }
    }

    /*
     * ============================================================
     * ATTACHED CAMERA
     * ============================================================
     */

    @WrapMethod(method = "activateHopToCoords")
    private void javaReforged$activateHopPerSlot(
            Operation<Void> original
    ) {
        javaReforged$enterState();

        try {
            original.call();
        } finally {
            javaReforged$exitState();
        }
    }

    @WrapMethod(method = "onAttachedCameraButton")
    private void javaReforged$attachedCameraPerSlot(
            Button button,
            Operation<Void> original
    ) {
        javaReforged$enterState();

        try {
            original.call(button);
        } finally {
            javaReforged$exitState();
        }
    }

    /*
     * ============================================================
     * HIDDEN UI / INPUT
     * ============================================================
     *
     * These methods directly touch static hiddenUI.
     */

    @WrapMethod(method = "mouseClicked")
    private boolean javaReforged$mouseClickedPerSlot(
            MouseButtonEvent event,
            boolean doubleClick,
            Operation<Boolean> original
    ) {
        javaReforged$enterState();

        try {
            return original.call(event, doubleClick);
        } finally {
            javaReforged$exitState();
        }
    }

    @WrapMethod(method = "mouseReleased")
    private boolean javaReforged$mouseReleasedPerSlot(
            MouseButtonEvent event,
            Operation<Boolean> original
    ) {
        javaReforged$enterState();

        try {
            return original.call(event);
        } finally {
            javaReforged$exitState();
        }
    }

    @WrapMethod(method = "handleRightClick")
    private void javaReforged$rightClickPerSlot(
            IRightClickableElement element,
            int x,
            int y,
            Operation<Void> original
    ) {
        javaReforged$enterState();

        try {
            original.call(element, x, y);
        } finally {
            javaReforged$exitState();
        }
    }

    @WrapMethod(method = "onInputPress")
    private boolean javaReforged$inputPressPerSlot(
            InputConstants.Type type,
            int key,
            Operation<Boolean> original
    ) {
        javaReforged$enterState();

        try {
            return original.call(type, key);
        } finally {
            javaReforged$exitState();
        }
    }

    @WrapMethod(method = "toggleHiddenUI")
    private void javaReforged$toggleHiddenUiPerSlot(
            Operation<Void> original
    ) {
        javaReforged$enterState();

        try {
            original.call();
        } finally {
            javaReforged$exitState();
        }
    }

    /*
     * ============================================================
     * MAP RENDER EXTRACTION
     * ============================================================
     *
     * This is the critical boundary.
     *
     * During J1:
     *
     *   immediateRenderFBO -> slot 0 FBO
     *
     * During J2:
     *
     *   immediateRenderFBO -> slot 1 FBO
     *
     * Therefore the BlitRenderState extracted for J1 keeps J1's
     * texture and cannot later be overwritten by J2.
     */

    @WrapMethod(method = "extractRenderState")
    private void javaReforged$extractPerSlot(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            Operation<Void> original
    ) {
        javaReforged$enterState();

        /*
         * Do NOT rebuild a viewport/scissor here.
         *
         * GuiViewportScope already installed the exact scissor that
         * belongs to this extraction. Use the Runtime's current value.
         */
        this.javaReforged$viewportScissor =
                ((GuiGraphicsExtractorBridge)(Object)graphics)
                        .javaReforged$currentScissor();

        try {
            /*
             * Xaero caches the vanilla Window gui scale in this field.
             * Refresh it from the viewport API for the active slot.
             */
            this.screenScale =
                    XaeroWorldMapViewport.guiScale(
                            Minecraft.getInstance().getWindow()
                    );

            original.call(
                    graphics,
                    mouseX,
                    mouseY,
                    partialTick
            );
        } finally {
            this.javaReforged$viewportScissor = null;
            javaReforged$exitState();
        }
    }

    /*
     * Xaero asks Window for framebuffer dimensions while building and
     * rendering its private FBO.
     *
     * These values must come from Java Reforged's active viewport.
     */

    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lcom/mojang/blaze3d/platform/Window;getWidth()I"
            )
    )
    private int javaReforged$viewportFramebufferWidth(
            Window window
    ) {
        return XaeroWorldMapViewport.framebufferWidth(window);
    }

    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lcom/mojang/blaze3d/platform/Window;getHeight()I"
            )
    )
    private int javaReforged$viewportFramebufferHeight(
            Window window
    ) {
        return XaeroWorldMapViewport.framebufferHeight(window);
    }

    /*
     * Xaero manually creates the final BlitRenderState with
     * scissor = null.
     *
     * Replace ONLY that argument with the current scissor already
     * supplied by Java Reforged.
     *
     * No coordinates are recalculated here.
     */
    @ModifyArg(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lnet/minecraft/client/renderer/state/gui/BlitRenderState;"
                                    + "<init>("
                                    + "Lcom/mojang/blaze3d/pipeline/RenderPipeline;"
                                    + "Lnet/minecraft/client/gui/render/TextureSetup;"
                                    + "Lorg/joml/Matrix3x2f;"
                                    + "IIIIFFFFI"
                                    + "Lnet/minecraft/client/gui/navigation/ScreenRectangle;"
                                    + ")V"
            ),
            index = 12
    )
    private ScreenRectangle javaReforged$useViewportScissor(
            @Nullable ScreenRectangle xaeroScissor
    ) {
        return this.javaReforged$viewportScissor != null
                ? this.javaReforged$viewportScissor
                : xaeroScissor;
    }

    /*
     * ============================================================
     * XAERO MESSAGE SCREENS
     * ============================================================
     *
     * Same rule: Window dimensions are replaced with dimensions
     * supplied by the active Java Reforged viewport.
     */

    @Redirect(
            method =
                    "renderMessageScreen("
                            + "Lnet/minecraft/client/gui/GuiGraphicsExtractor;"
                            + "Ljava/lang/String;"
                            + "Ljava/lang/String;"
                            + ")V",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lcom/mojang/blaze3d/platform/Window;getWidth()I"
            )
    )
    private int javaReforged$viewportMessageFramebufferWidth(
            Window window
    ) {
        return XaeroWorldMapViewport.guiWidth(window);
    }

    @Redirect(
            method =
                    "renderMessageScreen("
                            + "Lnet/minecraft/client/gui/GuiGraphicsExtractor;"
                            + "Ljava/lang/String;"
                            + "Ljava/lang/String;"
                            + ")V",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lcom/mojang/blaze3d/platform/Window;getHeight()I"
            )
    )
    private int javaReforged$viewportMessageFramebufferHeight(
            Window window
    ) {
        return XaeroWorldMapViewport.guiHeight(window);
    }

    @Redirect(
            method =
                    "renderMessageScreen("
                            + "Lnet/minecraft/client/gui/GuiGraphicsExtractor;"
                            + "Ljava/lang/String;"
                            + "Ljava/lang/String;"
                            + ")V",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lcom/mojang/blaze3d/platform/Window;"
                                    + "getGuiScaledWidth()I"
            )
    )
    private int javaReforged$viewportMessageGuiWidth(
            Window window
    ) {
        return XaeroWorldMapViewport.guiWidth(window);
    }

    @Redirect(
            method =
                    "renderMessageScreen("
                            + "Lnet/minecraft/client/gui/GuiGraphicsExtractor;"
                            + "Ljava/lang/String;"
                            + "Ljava/lang/String;"
                            + ")V",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lcom/mojang/blaze3d/platform/Window;"
                                    + "getGuiScaledHeight()I"
            )
    )
    private int javaReforged$viewportMessageGuiHeight(
            Window window
    ) {
        return XaeroWorldMapViewport.guiHeight(window);
    }

    /*
     * Backup of Xaero's actual static fields while one slot is active.
     */
    @Unique
    private record XaeroStaticBackup(
            double destScale,
            ImprovedFramebuffer primaryScaleFbo,
            ImprovedFramebuffer immediateRenderFbo,
            boolean attachedCamera,
            boolean hiddenUi,
            int lastAmountOfRegionsViewed
    ) {
    }
}
