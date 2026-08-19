package net.jr.client.runtime.ui;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;
import net.jr.client.runtime.viewport.ViewportPanoramaRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SecondaryWorldLoadingScreen extends Screen {
    private static final SecondaryWorldLoadingScreen[] SCREENS =
        new SecondaryWorldLoadingScreen[LocalClientSlotRegistry.MAX_SLOTS];

    private final int slotId;

    private SecondaryWorldLoadingScreen(int slotId) {
        super(Component.translatable("menu.java_reforged.split.loading_world"));
        this.slotId = slotId;
    }

    public static Screen forSlot(int slotId) {
        SecondaryWorldLoadingScreen screen = SCREENS[slotId];
        if (screen == null) {
            screen = new SecondaryWorldLoadingScreen(slotId);
            SCREENS[slotId] = screen;
        }
        return screen;
    }

    public static boolean shouldPresent(LocalClientSlot slot) {
        int slotId = slot.id();
        return slotId > 0
            && ClientRuntime.INSTANCE.viewports().isPresented(slotId)
            && ClientRuntime.INSTANCE.clients().hasClient(slotId)
            && ClientRuntime.INSTANCE.clients().isJoining(slotId);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ViewportPanoramaRenderer.extract(graphics, this.width, this.height);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x44000000);
        Component title = Component.translatable("menu.java_reforged.split.loading_world");
        Component detail = Component.translatable("menu.java_reforged.split.loading_player", this.slotId + 1);
        graphics.text(this.font, title, (this.width - this.font.width(title)) / 2, this.height / 2 - 10, 0xFFFFFFFF, false);
        graphics.text(this.font, detail, (this.width - this.font.width(detail)) / 2, this.height / 2 + 4, 0xFFAAAAAA, false);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
