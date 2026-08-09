package net.jr.client.ui.hint.provider;

import net.jr.client.ui.hint.ControlHintContext;
import net.jr.client.ui.hint.model.ControlHintRequest;
import net.jr.client.input.binding.ModKeyBindings;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.List;

public final class PauseMenuControlHintProvider implements ControlHintProvider {
    @Override
    public boolean supports(ControlHintContext context) {
        if (context.screen() == null || context.screen() instanceof AbstractContainerScreen<?>) {
            return false;
        }

        return context.screen() instanceof PauseScreen
            || context.screen().getClass().getSimpleName().contains("PauseScreen");
    }

    @Override
    public List<ControlHintRequest> buildHints(ControlHintContext context) {
        return List.of(
            new ControlHintRequest(ModKeyBindings.UI_CONFIRM, Component.literal("Seleccionar")),
            new ControlHintRequest(ModKeyBindings.UI_BACK, Component.literal("Volver"))
        );
    }
}
