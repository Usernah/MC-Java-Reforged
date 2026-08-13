package net.jr.ClientRuntime.state;

import net.jr.ClientRuntime.runtime.LocalPlayers;
import net.jr.ClientRuntime.viewport.GuiScaleCalculator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

/** Options owned by one local player. Only explicitly migrated options belong here. */
public final class OptionsState {
    private final int slotId;
    private final OptionInstance<Integer> guiScale;

    public OptionsState(int slotId) {
        this.slotId = slotId;
        this.guiScale = new OptionInstance<>(
            "options.guiScale",
            OptionInstance.noTooltip(),
            (caption, value) -> value == 0
                ? net.minecraft.network.chat.Component.translatable("options.guiScale.auto")
                : net.minecraft.network.chat.Component.literal(Integer.toString(value)),
            new OptionInstance.ClampingLazyMaxIntRange(0, this::maximumGuiScale, Integer.MAX_VALUE - 1),
            0,
            value -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.isRunning()) {
                    LocalPlayers.INSTANCE.refreshViewportOptions(minecraft);
                }
            }
        );
    }

    public OptionInstance<Integer> guiScale() {
        return this.guiScale;
    }

    public int requestedGuiScale() {
        return this.guiScale.get();
    }

    private int maximumGuiScale() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isRunning()) {
            return Integer.MAX_VALUE - 1;
        }
        return GuiScaleCalculator.maximumForSlot(this.slotId, minecraft.isEnforceUnicode());
    }
}
