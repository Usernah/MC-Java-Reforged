package net.jr.client.input;

import net.jr.client.input.binding.ModKeyBindings;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.jr.client.sound.action.InputActionSounds;
import net.jr.screens.crafting.JavaCrafting2x2Screen;
import net.jr.screens.inventory.JavaCreativeInventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public final class GameplayActionHandler {
    private GameplayActionHandler() {
    }

    public static boolean handle(
        Minecraft minecraft,
        LocalPlayer player,
        boolean gamepadInput,
        boolean wasPauseButtonDown
    ) {
        boolean pauseButtonDown = gamepadInput
            && InputApi.isGamepadConnected()
            && InputApi.isPressed(GamepadDigitalInput.BUTTON_START);
        if (pauseButtonDown) InputApi.markGamepadInput();
        if (pauseButtonDown && !wasPauseButtonDown) minecraft.pauseGame(false);

        if (minecraft.gui.screen() != null) return pauseButtonDown;
        while (ModKeyBindings.GAMEPLAY_HOTBAR_PREV.consumeClick()) {
            player.getInventory().setSelectedSlot((player.getInventory().getSelectedSlot() - 1 + 9) % 9);
        }
        while (ModKeyBindings.GAMEPLAY_HOTBAR_NEXT.consumeClick()) {
            player.getInventory().setSelectedSlot((player.getInventory().getSelectedSlot() + 1) % 9);
        }
        while (ModKeyBindings.GAMEPLAY_CRAFTING.consumeClick()) {
            if (player.hasInfiniteMaterials()) {
                minecraft.gui.setScreen(new JavaCreativeInventoryScreen(
                    player, Component.translatable("container.creative")
                ));
            } else {
                minecraft.gui.setScreen(new JavaCrafting2x2Screen(
                    player.inventoryMenu,
                    player.getInventory(),
                    Component.translatable("container.crafting")
                ));
            }
        }
        return pauseButtonDown;
    }

    public static boolean updateSounds(Minecraft minecraft, LocalPlayer player, boolean wasDropDown) {
        boolean dropDown = minecraft.options.keyDrop.isDown();
        if (dropDown && !wasDropDown && !player.getInventory().getSelectedItem().isEmpty()) {
            InputActionSounds.playGameplayItemPop();
        }
        return dropDown;
    }
}
