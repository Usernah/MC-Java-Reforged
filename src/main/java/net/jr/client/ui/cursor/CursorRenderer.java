package net.jr.client.ui.cursor;

import net.jr.Java_reforged;
import net.jr.api.client.split.SplitScreen;
import net.jr.api.client.ui.Viewport;
import net.jr.client.input.InputApi;
import net.jr.client.runtime.input.CursorVisibility;
import net.jr.client.runtime.input.GamepadInputHandler;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.ui.render.composition.ScreenFramebufferSpace;
import net.jr.client.ui.navigation.UiInputModeController;
import net.jr.api.client.render.Draw;
import net.jr.api.client.resource.Asset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = Java_reforged.MODID, value = Dist.CLIENT)
public final class CursorRenderer {

    private static final Asset MOUSE_CURSOR_TEXTURE = Asset.NamespaceAndPatch(Java_reforged.MODID, "textures/cursor/mouse.png");
    private static final Asset JOYSTICK_CURSOR_TEXTURE = Asset.NamespaceAndPatch(Java_reforged.MODID, "textures/cursor/joystick.png");
    private static final float CURSOR_SIZE = 32.0f;
    private static final float HOTSPOT_X = 3.75f;
    private static final float HOTSPOT_Y = 1.875f;

    private CursorRenderer() {
    }

    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        CursorVisibility.setReplacementHiddenForSlot(
            InputApi.keyboardMouseSlotId(),
            slotWantsSystemCursorHidden(minecraft)
        );
        CursorVisibility.sync();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (hasLocalScreen(minecraft)) {
            return;
        }
        if (!shouldDrawReplacementCursor(minecraft)) {
            return;
        }

        drawCursor(event.getGuiGraphics(), resolveCursorPosition(minecraft));
    }

    public static void renderForCurrentClient(GuiGraphicsExtractor guiGraphics, Minecraft minecraft) {
        if (minecraft == null || minecraft.gui.screen() == null) {
            return;
        }
        if (!shouldDrawReplacementCursor(minecraft)) {
            return;
        }
        drawCursor(guiGraphics, resolveClientCursorPosition(minecraft));
    }

    private static boolean shouldDrawReplacementCursor(Minecraft minecraft) {
        if (minecraft == null || minecraft.gui.screen() == null || minecraft.gui.overlay() != null) {
            return false;
        }
        return slotWantsReplacementCursor(minecraft)
            && (!UiInputModeController.isFocusNavigationActive() || UiInputModeController.shouldShowCursorDuringFocus());
    }

    private static boolean slotWantsReplacementCursor(Minecraft minecraft) {
        if (minecraft == null || minecraft.gui.screen() == null) {
            return false;
        }
        return GamepadInputHandler.isControllerCursorActive()
            && InputApi.isGamepadConnected();
    }

    private static boolean slotWantsSystemCursorHidden(Minecraft minecraft) {
        if (minecraft == null || minecraft.gui.overlay() != null) {
            return true;
        }

        return GamepadInputHandler.isControllerCursorActive()
                && InputApi.isGamepadConnected();
    }

    private static CursorPosition resolveCursorPosition(Minecraft minecraft) {
        if (GamepadInputHandler.isControllerCursorActive() && hasLocalScreen(minecraft)) {
            return new CursorPosition(GamepadInputHandler.visualCursorX(), GamepadInputHandler.visualCursorY());
        }
        if (!InputApi.canPhysicalMouseDrive()) {
            return null;
        }

        double mouseX = MouseCoordinates.rawMouseToGlobalGuiX(minecraft, minecraft.mouseHandler.xpos());
        double mouseY = MouseCoordinates.rawMouseToGlobalGuiY(minecraft, minecraft.mouseHandler.ypos());
        return new CursorPosition(mouseX, mouseY);
    }

    private static CursorPosition resolveClientCursorPosition(Minecraft minecraft) {
        if (GamepadInputHandler.isControllerCursorActive()) {
            return new CursorPosition(GamepadInputHandler.visualCursorX(), GamepadInputHandler.visualCursorY());
        }
        if (!InputApi.canPhysicalMouseDrive()) {
            return null;
        }

        ViewportArea viewport = Viewport.current();
        return new CursorPosition(
            ScreenFramebufferSpace.windowMouseX(
                minecraft.gui.screen(), viewport, minecraft.mouseHandler.xpos()
            ),
            ScreenFramebufferSpace.windowMouseY(
                minecraft.gui.screen(), viewport, minecraft.mouseHandler.ypos()
            )
        );
    }

    private static void drawCursor(GuiGraphicsExtractor guiGraphics, CursorPosition cursor) {
        if (cursor == null) {
            return;
        }

        //boolean shouldCenterCursor = GamepadInputHandler.isControllerCursorActive();

        //float finalHotspotX = shouldCenterCursor ? CURSOR_WIDTH / 2.0F : HOTSPOT_X;
        //float finalHotspotY = shouldCenterCursor ? CURSOR_HEIGHT / 2.0F : HOTSPOT_Y;

        float finalHotspot = CURSOR_SIZE / 2.0F;

        float drawX = (float) cursor.x() - finalHotspot;
        float drawY = (float) cursor.y() - finalHotspot;
        Asset cursorTexture = JOYSTICK_CURSOR_TEXTURE;
        guiGraphics.nextStratum();
        Draw.image(cursorTexture, drawX, drawY, CURSOR_SIZE, CURSOR_SIZE)
            .draw(guiGraphics);
    }

    //unused, reserved for future DSL-based cursor customizations.
    private static Asset resolveCursorTexture() {
        return GamepadInputHandler.isControllerCursorActive()
            ? JOYSTICK_CURSOR_TEXTURE
            : MOUSE_CURSOR_TEXTURE;
    }

    private static boolean hasLocalScreen(Minecraft minecraft) {
        if (minecraft == null) {
            return false;
        }
        return minecraft.gui.screen() != null;
    }

    private record CursorPosition(double x, double y) {
    }
}
