package net.jr.mixin.runtime;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.nio.file.Path;
import java.util.List;

import com.mojang.blaze3d.platform.Window;
import net.jr.api.client.ui.Viewport;
import net.jr.client.runtime.input.InputActionHandler;
import net.jr.client.runtime.context.SlotExecution;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.sound.action.WidgetSoundRouter;
import net.jr.client.ui.render.composition.ScreenFramebufferSpace;
import net.jr.client.ui.dsl.ScreenEditorHooks;
import net.jr.client.runtime.ui.SlotScreenManager;
import net.jr.api.client.systemui.SystemUI;
import net.jr.client.input.InputApi;
import net.jr.mixin.accessors.MouseHandlerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = MouseHandler.class, priority = 2000)
public abstract class MouseHandlerMixin {
    @WrapMethod(method = "onButton")
    private void javaReforged$mouseButtonForInputOwner(long windowPointer, MouseButtonInfo buttonInfo, int action, Operation<Void> original) {
        SlotExecution.runForKeyboardMouse(() -> {
            Screen editorScreen = SlotScreenManager.activeScreenOrNull();
            if (editorScreen != null && ScreenEditorHooks.activeFor(editorScreen)) {
                Minecraft mc = Minecraft.getInstance();
                ViewportArea area = net.jr.api.client.ui.Viewport.current();
                double x = ScreenFramebufferSpace.windowMouseX(editorScreen, area, mc.mouseHandler.xpos());
                double y = ScreenFramebufferSpace.windowMouseY(editorScreen, area, mc.mouseHandler.ypos());
                ScreenEditorHooks.mouseButton(editorScreen, x, y, buttonInfo.button(), action);
                return;
            }
            if (!SystemUI.mouseButton(buttonInfo.button(), action)) {
                original.call(windowPointer, buttonInfo, action);
            }
        });
    }

    @WrapMethod(method = "onScroll")
    private void javaReforged$mouseScrollForInputOwner(long windowPointer, double xOffset, double yOffset, Operation<Void> original) {
        SlotExecution.runForKeyboardMouse(() -> {
            Screen editorScreen = SlotScreenManager.activeScreenOrNull();
            if (editorScreen != null && ScreenEditorHooks.activeFor(editorScreen)) {
                Minecraft mc = Minecraft.getInstance();
                ViewportArea area = net.jr.api.client.ui.Viewport.current();
                double x = ScreenFramebufferSpace.windowMouseX(editorScreen, area, mc.mouseHandler.xpos());
                double y = ScreenFramebufferSpace.windowMouseY(editorScreen, area, mc.mouseHandler.ypos());
                ScreenEditorHooks.mouseScroll(editorScreen, x, y, xOffset, yOffset);
                return;
            }
            if (!SystemUI.mouseScrolled(xOffset, yOffset)) {
                original.call(windowPointer, xOffset, yOffset);
            }
        });
    }

    @WrapMethod(method = "onMove")
    private void javaReforged$mouseMoveForInputOwner(long windowPointer, double xpos, double ypos, Operation<Void> original) {
        SlotExecution.runForKeyboardMouse(() -> {
            Screen editorScreen = SlotScreenManager.activeScreenOrNull();
            if (editorScreen != null && ScreenEditorHooks.activeFor(editorScreen)) {
                ViewportArea area = net.jr.api.client.ui.Viewport.current();
                double x = ScreenFramebufferSpace.windowMouseX(editorScreen, area, xpos);
                double y = ScreenFramebufferSpace.windowMouseY(editorScreen, area, ypos);
                ScreenEditorHooks.mouseMove(editorScreen, x, y);
                MouseHandlerAccessor mouse = (MouseHandlerAccessor)Minecraft.getInstance().mouseHandler;
                mouse.javaReforged$setXpos(xpos);
                mouse.javaReforged$setYpos(ypos);
                InputApi.markMouseMove(xpos, ypos);
                return;
            }
            if (SystemUI.mouseMoved(xpos, ypos)) {
                MouseHandlerAccessor mouse = (MouseHandlerAccessor)Minecraft.getInstance().mouseHandler;
                mouse.javaReforged$setXpos(xpos);
                mouse.javaReforged$setYpos(ypos);
                InputApi.markMouseMove(xpos, ypos);
            } else {
                original.call(windowPointer, xpos, ypos);
            }
        });
    }

    @WrapMethod(method = "onDrop")
    private void javaReforged$fileDropForInputOwner(long windowPointer, List<Path> files, int failedCount, Operation<Void> original) {
        SlotExecution.runForKeyboardMouse(() -> {
            if (!SystemUI.blocksKeyboardMouse()) {
                original.call(windowPointer, files, failedCount);
            }
        });
    }

    @WrapMethod(method = "handleAccumulatedMovement")
    private void javaReforged$mouseMovementForInputOwner(Operation<Void> original) {
        SlotExecution.runForKeyboardMouse(() -> {
            if (!SystemUI.blocksKeyboardMouse()) {
                original.call();
            }
        });
    }

    @Redirect(
        method = {"onButton", "onScroll", "handleAccumulatedMovement"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;getScaledXPos(Lcom/mojang/blaze3d/platform/Window;)D")
    )
    private double javaReforged$readLocalMouseX(MouseHandler mouseHandler, com.mojang.blaze3d.platform.Window window) {
        ViewportArea viewport = Viewport.current();
        return ScreenFramebufferSpace.windowMouseX(
            Minecraft.getInstance().gui.screen(), viewport, mouseHandler.xpos()
        );
    }

    @Redirect(
        method = {"onButton", "onScroll", "handleAccumulatedMovement"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;getScaledYPos(Lcom/mojang/blaze3d/platform/Window;)D")
    )
    private double javaReforged$readLocalMouseY(MouseHandler mouseHandler, com.mojang.blaze3d.platform.Window window) {
        ViewportArea viewport = Viewport.current();
        return ScreenFramebufferSpace.windowMouseY(
            Minecraft.getInstance().gui.screen(), viewport, mouseHandler.ypos()
        );
    }

    @Redirect(
            method = "handleAccumulatedMovement",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;getScaledXPos(Lcom/mojang/blaze3d/platform/Window;D)D")
    )
    private double javaReforged$readAccumulatedLocalMouseX(Window window, double x) {
        ViewportArea viewport = Viewport.current();
        return ScreenFramebufferSpace.windowDeltaX(
            Minecraft.getInstance().gui.screen(), viewport, x
        );
    }

    @Redirect(
            method = "handleAccumulatedMovement",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;getScaledYPos(Lcom/mojang/blaze3d/platform/Window;D)D")
    )
    private double javaReforged$readAccumulatedLocalMouseY(Window window, double y) {
        ViewportArea viewport = Viewport.current();
        return ScreenFramebufferSpace.windowDeltaY(
            Minecraft.getInstance().gui.screen(), viewport, y
        );
    }

    @Redirect(
        method = "onButton",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z"
        )
    )
    private boolean javaReforged$playContainerFeedbackFromRealMouseEvent(
        Screen screen,
        MouseButtonEvent event,
        boolean doubleClick
    ) {
        InputActionHandler.playPhysicalMouseContainerActionFeedback(screen, event);
        WidgetSoundRouter.beginPress();
        boolean handled = screen.mouseClicked(event, doubleClick);
        WidgetSoundRouter.finishPress(screen, handled);
        return handled;
    }
}
