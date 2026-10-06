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

    @Unique
    @Nullable
    private ScreenRectangle javaReforged$viewportScissor;


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


    @WrapMethod(method = "extractRenderState")
    private void javaReforged$extractPerSlot(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            Operation<Void> original
    ) {
        javaReforged$enterState();


        this.javaReforged$viewportScissor =
                ((GuiGraphicsExtractorBridge)(Object)graphics)
                        .javaReforged$currentScissor();

        try {

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
