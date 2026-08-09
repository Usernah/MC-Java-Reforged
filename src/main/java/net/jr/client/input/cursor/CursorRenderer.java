package net.jr.client.input.cursor;

import net.jr.Java_reforged;
import net.jr.client.input.InputApi;
import net.jr.client.input.runtime.GamepadInputProcessor;
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
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = Java_reforged.MODID, value = Dist.CLIENT)
public final class CursorRenderer {

    private static final Asset MOUSE_CURSOR_TEXTURE = Asset.NamespaceAndPatch(Java_reforged.MODID, "textures/cursor/mouse.png");
    private static final Asset JOYSTICK_CURSOR_TEXTURE = Asset.NamespaceAndPatch(Java_reforged.MODID, "textures/cursor/joystick.png");
    private static final float CURSOR_WIDTH = 16.0f;
    private static final float CURSOR_HEIGHT = 16.0f;
    private static final float HOTSPOT_X = 3.75f;
    private static final float HOTSPOT_Y = 1.875f;

    private CursorRenderer() {
    }

    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        CursorHider.setReplacementHidden(slotWantsSystemCursorHidden(minecraft));
        CursorHider.sync();
    }

    /** Draws the replacement cursor for this process' normal HUD pass. */
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

    /** Draws the replacement cursor over the current vanilla screen. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!shouldDrawReplacementCursor(minecraft)) {
            return;
        }

        drawCursor(event.getGuiGraphics(), resolveScreenCursorPosition(minecraft, event));
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
        return GamepadInputProcessor.isControllerCursorActive()
            ? InputApi.isGamepadConnected()
            : InputApi.canPhysicalMouseDrive();
    }

    private static boolean slotWantsSystemCursorHidden(Minecraft minecraft) {
        if (minecraft == null || minecraft.gui.overlay() != null) {
            return true;
        }

        return slotWantsReplacementCursor(minecraft);
    }

    private static CursorPosition resolveCursorPosition(Minecraft minecraft) {
        if (GamepadInputProcessor.isControllerCursorActive() && hasLocalScreen(minecraft)) {
            return new CursorPosition(GamepadInputProcessor.visualCursorX(), GamepadInputProcessor.visualCursorY());
        }
        if (!InputApi.canPhysicalMouseDrive()) {
            return null;
        }

        double mouseX = MouseCoordinates.rawMouseToGlobalGuiX(minecraft, minecraft.mouseHandler.xpos());
        double mouseY = MouseCoordinates.rawMouseToGlobalGuiY(minecraft, minecraft.mouseHandler.ypos());
        return new CursorPosition(mouseX, mouseY);
    }

    private static CursorPosition resolveScreenCursorPosition(Minecraft minecraft, ScreenEvent.Render.Post event) {
        if (GamepadInputProcessor.isControllerCursorActive()) {
            return new CursorPosition(GamepadInputProcessor.visualCursorX(), GamepadInputProcessor.visualCursorY());
        }
        if (!InputApi.canPhysicalMouseDrive()) {
            return null;
        }

        return new CursorPosition(event.getMouseX(), event.getMouseY());
    }

    private static CursorPosition resolveClientCursorPosition(Minecraft minecraft) {
        if (GamepadInputProcessor.isControllerCursorActive()) {
            return new CursorPosition(GamepadInputProcessor.visualCursorX(), GamepadInputProcessor.visualCursorY());
        }
        if (!InputApi.canPhysicalMouseDrive()) {
            return null;
        }

        return new CursorPosition(
            MouseCoordinates.rawMouseToGlobalGuiX(minecraft, minecraft.mouseHandler.xpos()),
            MouseCoordinates.rawMouseToGlobalGuiY(minecraft, minecraft.mouseHandler.ypos())
        );
    }

    private static void drawCursor(GuiGraphicsExtractor guiGraphics, CursorPosition cursor) {
        if (cursor == null) {
            return;
        }

        boolean shouldCenterCursor = GamepadInputProcessor.isControllerCursorActive();

        float finalHotspotX = shouldCenterCursor ? CURSOR_WIDTH / 2.0F : HOTSPOT_X;
        float finalHotspotY = shouldCenterCursor ? CURSOR_HEIGHT / 2.0F : HOTSPOT_Y;

        float drawX = (float) cursor.x() - finalHotspotX;
        float drawY = (float) cursor.y() - finalHotspotY;
        Asset cursorTexture = resolveCursorTexture();
        guiGraphics.nextStratum();
        Draw.image(cursorTexture, drawX, drawY, CURSOR_WIDTH, CURSOR_HEIGHT)
            .uvSize(32, 32)
            .atlasSize(32, 32)
            .draw(guiGraphics);
    }

    private static Asset resolveCursorTexture() {
        return GamepadInputProcessor.isControllerCursorActive()
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
