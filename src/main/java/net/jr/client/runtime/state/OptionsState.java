package net.jr.client.runtime.state;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.viewport.GuiScaleCalculator;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;

/**
 * Options owned by one local player.
 * Only explicitly migrated options belong here.
 */
public final class OptionsState {
    private final int slotId;

    private final OptionInstance<Integer> guiScale;
    private final OptionInstance<Integer> fov;

    private CameraType cameraType = CameraType.FIRST_PERSON;

    public OptionsState(int slotId) {
        this.slotId = slotId;

        this.guiScale = new OptionInstance<>(
                "options.guiScale",
                OptionInstance.noTooltip(),
                (caption, value) -> value == 0
                        ? Component.translatable("options.guiScale.auto")
                        : Component.literal(Integer.toString(value)),
                new OptionInstance.ClampingLazyMaxIntRange(
                        0,
                        this::maximumGuiScale,
                        Integer.MAX_VALUE - 1
                ),
                0,
                value -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    if (minecraft.isRunning()) {
                        ClientRuntime.INSTANCE
                                .viewportResize()
                                .refreshViewportOptions(minecraft);
                    }
                }
        );

        this.fov = new OptionInstance<>(
                "options.fov",
                OptionInstance.noTooltip(),
                (caption, value) -> switch (value) {
                    case 70 -> Options.genericValueLabel(
                            caption,
                            Component.translatable("options.fov.min")
                    );
                    case 110 -> Options.genericValueLabel(
                            caption,
                            Component.translatable("options.fov.max")
                    );
                    default -> Options.genericValueLabel(caption, value);
                },
                new OptionInstance.IntRange(30, 110),
                70,
                value -> {
                }
        );
    }

    public OptionInstance<Integer> guiScale() {
        return this.guiScale;
    }

    public int requestedGuiScale() {
        return this.guiScale.get();
    }

    public CameraType cameraType() {
        return this.cameraType;
    }

    public void setCameraType(CameraType cameraType) {
        this.cameraType = cameraType;
    }

    public OptionInstance<Integer> fov() {
        return this.fov;
    }

    /**
     * Seeds local visual options from Vanilla's loaded options.
     *
     * Called after Options.load(), before local clients begin using them.
     */
    public void initializeVisualOptions(
            CameraType cameraType,
            int fov
    ) {
        this.cameraType = cameraType;
        this.fov.set(fov);
    }

    private int maximumGuiScale() {
        Minecraft minecraft = Minecraft.getInstance();

        if (!minecraft.isRunning()) {
            return Integer.MAX_VALUE - 1;
        }

        return GuiScaleCalculator.maximumForSlot(
                this.slotId,
                minecraft.isEnforceUnicode()
        );
    }
}