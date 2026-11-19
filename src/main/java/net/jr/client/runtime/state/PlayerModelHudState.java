package net.jr.client.runtime.state;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Per-client visibility and equipment tracking for the HUD player model. */
public final class PlayerModelHudState {
    private static final long VISIBILITY_COOLDOWN_MS = 2_000L;

    private final Map<EquipmentSlot, ItemStack> equipment =
            new EnumMap<>(EquipmentSlot.class);
    @Nullable
    private UUID playerId;
    private long visibleUntilMs;

    public void onScreenChanged(@Nullable Screen previous, @Nullable Screen current) {
        if (previous instanceof AbstractContainerScreen<?>
                && !(current instanceof AbstractContainerScreen<?>)) {
            this.showTemporarily();
        }
    }

    public boolean updateAndIsVisible(LocalPlayer player) {
        this.updateEquipment(player);

        long now = nowMs();
        boolean performingVisibleAction = player.getAbilities().flying
                || player.isFallFlying()
                || player.isSwimming()
                || player.isSprinting()
                || player.isCrouching();
        if (performingVisibleAction) {
            this.visibleUntilMs = now + VISIBILITY_COOLDOWN_MS;
        }
        return performingVisibleAction || now < this.visibleUntilMs;
    }

    private void updateEquipment(LocalPlayer player) {
        UUID currentPlayerId = player.getUUID();
        if (!currentPlayerId.equals(this.playerId)) {
            this.playerId = currentPlayerId;
            this.captureEquipment(player);
            return;
        }

        boolean changed = false;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack current = player.getItemBySlot(slot);
            ItemStack previous = this.equipment.getOrDefault(slot, ItemStack.EMPTY);
            if (!ItemStack.isSameItem(current, previous)) {
                changed = true;
                break;
            }
        }

        if (changed) {
            this.captureEquipment(player);
            this.showTemporarily();
        }
    }

    private void captureEquipment(LocalPlayer player) {
        this.equipment.clear();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.equipment.put(slot, player.getItemBySlot(slot).copy());
        }
    }

    private void showTemporarily() {
        this.visibleUntilMs = Math.max(
                this.visibleUntilMs,
                nowMs() + VISIBILITY_COOLDOWN_MS
        );
    }

    private static long nowMs() {
        return System.nanoTime() / 1_000_000L;
    }
}
