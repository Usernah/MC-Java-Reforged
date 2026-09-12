package net.jr.client.input;

import net.jr.client.input.binding.ModKeyBindings;
import net.jr.api.client.local.LocalClients;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.jr.client.sound.action.InputActionSounds;
import net.jr.screens.crafting.Crafting2x2Screen;
import net.jr.screens.inventory.CreativeInventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

public final class GameplayActionHandler {
    private static final long HOTBAR_REPEAT_DELAY_MS = 250L;
    private static final long HOTBAR_REPEAT_INTERVAL_MS = 100L;

    private GameplayActionHandler() {
    }

    public static boolean handle(
        Minecraft minecraft,
        LocalPlayer player,
        boolean gamepadInput,
        boolean wasPauseButtonDown,
        HotbarRepeatState hotbarRepeatState
    ) {
        boolean pauseButtonDown = gamepadInput
            && InputApi.isGamepadConnected()
            && InputApi.isPressed(GamepadDigitalInput.BUTTON_START);
        if (pauseButtonDown) InputApi.markGamepadInput();
        if (pauseButtonDown && !wasPauseButtonDown) minecraft.pauseGame(false);

        if (minecraft.gui.screen() != null) return pauseButtonDown;
        updateHotbarAction(player, HotbarAction.PREVIOUS, hotbarRepeatState.previous);
        updateHotbarAction(player, HotbarAction.NEXT, hotbarRepeatState.next);
        while (ModKeyBindings.GAMEPLAY_CRAFTING.consumeClick()) {
            if (player.hasInfiniteMaterials()) {
                minecraft.gui.setScreen(new CreativeInventoryScreen(
                    player, Component.translatable("container.creative")
                ));
            } else {
                minecraft.gui.setScreen(new Crafting2x2Screen(
                    player.inventoryMenu,
                    player.getInventory(),
                    Component.translatable("container.crafting"),
                    LocalClients.slotId(player)
                ));
            }
        }
        return pauseButtonDown;
    }

    private static void updateHotbarAction(LocalPlayer player, HotbarAction action, RepeatState repeatState) {
        boolean activated = false;
        while (action.mapping.consumeClick()) {
            action.apply(player);
            activated = true;
        }

        long now = Util.getMillis();
        if (!action.mapping.isDown()) {
            repeatState.clear();
            return;
        }
        if (activated || !repeatState.active) {
            repeatState.active = true;
            repeatState.startedMs = now;
            repeatState.lastDispatchMs = now;
            return;
        }
        if (now - repeatState.startedMs < HOTBAR_REPEAT_DELAY_MS
            || now - repeatState.lastDispatchMs < HOTBAR_REPEAT_INTERVAL_MS) {
            return;
        }

        action.apply(player);
        repeatState.lastDispatchMs = now;
    }

    public static boolean updateSounds(Minecraft minecraft, LocalPlayer player, boolean wasDropDown) {
        boolean dropDown = minecraft.options.keyDrop.isDown();
        if (dropDown && !wasDropDown && !player.getInventory().getSelectedItem().isEmpty()) {
            InputActionSounds.playGameplayItemPop();
        }
        return dropDown;
    }

    public static final class HotbarRepeatState {
        private final RepeatState previous = new RepeatState();
        private final RepeatState next = new RepeatState();
    }

    private static final class RepeatState {
        private boolean active;
        private long startedMs;
        private long lastDispatchMs;

        private void clear() {
            this.active = false;
            this.startedMs = 0L;
            this.lastDispatchMs = 0L;
        }
    }

    private enum HotbarAction {
        PREVIOUS(ModKeyBindings.GAMEPLAY_HOTBAR_PREV, -1),
        NEXT(ModKeyBindings.GAMEPLAY_HOTBAR_NEXT, 1);

        private final net.minecraft.client.KeyMapping mapping;
        private final int direction;

        HotbarAction(net.minecraft.client.KeyMapping mapping, int direction) {
            this.mapping = mapping;
            this.direction = direction;
        }

        private void apply(LocalPlayer player) {
            int selectedSlot = player.getInventory().getSelectedSlot();
            player.getInventory().setSelectedSlot((selectedSlot + this.direction + 9) % 9);
        }
    }
}
