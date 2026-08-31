package net.jr.client.ui.presentation;

import net.jr.ClientConfig;
import net.jr.client.runtime.render.experimental.ExperimentalRenderController;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class ExperimentalRenderOption {
    private static final OptionInstance.SliderableValueSet<Boolean> BOOLEAN_SLIDER =
        new OptionInstance.IntRange(0, 1).xmap(value -> value == 1, value -> value ? 1 : 0, true);

    public static final OptionInstance<Boolean> INSTANCE = new OptionInstance<>(
        "menu.java_reforged.video.optimize_experimental",
        OptionInstance.cachedConstantTooltip(
            Component.translatable("menu.java_reforged.video.optimize_experimental.description")
        ),
        (caption, enabled) -> Options.genericValueLabel(
            caption,
            enabled ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF
        ),
        BOOLEAN_SLIDER,
        ClientConfig.experimentalRenderOptimizations(),
        enabled -> {
            ClientConfig.setExperimentalRenderOptimizations(enabled);
            ExperimentalRenderController.requestEnabled(enabled);
        }
    );

    private ExperimentalRenderOption() {
    }
}
