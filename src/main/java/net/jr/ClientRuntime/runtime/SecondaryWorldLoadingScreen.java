package net.jr.ClientRuntime.runtime;

import net.jr.ClientRuntime.slot.PlayerSlot;
import net.jr.ClientRuntime.slot.PlayerSlots;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Per-viewport loading presentation used only while a secondary local client becomes world-safe. */
final class SecondaryWorldLoadingScreen extends Screen {
    private static final SecondaryWorldLoadingScreen[] SCREENS = new SecondaryWorldLoadingScreen[PlayerSlots.MAX_SLOTS];

    private final int slotId;

    private SecondaryWorldLoadingScreen(int slotId) {
        super(Component.translatable("menu.java_reforged.split.loading_world"));
        this.slotId = slotId;
    }

    static Screen forSlot(int slotId) {
        SecondaryWorldLoadingScreen screen = SCREENS[slotId];
        if (screen == null) {
            screen = new SecondaryWorldLoadingScreen(slotId);
            SCREENS[slotId] = screen;
        }
        return screen;
    }

    static boolean shouldPresent(PlayerSlot slot) {
        return slot.id() > 0
                && slot.drawable()
                && LocalPlayers.INSTANCE.sessions().isJoining(slot.id());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ViewportPanoramas.extract(graphics, this.width, this.height);
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
