package net.jr.client.ui.hint.provider;

import net.jr.client.ui.hint.ControlHintContext;
import net.jr.client.ui.hint.model.ControlHintRequest;
import net.jr.client.ui.hint.model.HintPlacement;
import net.jr.client.ui.hint.WorldHintRules;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

public final class HudControlHintProvider implements ControlHintProvider {
    @Override
    public boolean supports(ControlHintContext context) {
        return context.isHud() && context.player() != null;
    }

    @Override
    public List<ControlHintRequest> buildHints(ControlHintContext context) {
        List<ControlHintRequest> hints = new ArrayList<>();
        hints.add(new ControlHintRequest(context.minecraft().options.keyInventory, Component.literal("Inventario")));

        if (WorldHintRules.shouldShowAttackHint(context)) {
            hints.add(new ControlHintRequest(context.minecraft().options.keyAttack, Component.literal("Atacar")));
        }

        if (WorldHintRules.shouldShowBreakHint(context)) {
            hints.add(new ControlHintRequest(context.minecraft().options.keyAttack, Component.literal("Romper")));
            if (context.player() != null && context.player().getAbilities().instabuild) {
                hints.add(new ControlHintRequest(
                    context.minecraft().options.keyPickItem,
                    Component.literal("Copiar"),
                    HintPlacement.LEFT
                ));
            }
        }

        Component useLabel = WorldHintRules.resolveUseLabel(context);
        if (useLabel != null) {
            hints.add(new ControlHintRequest(context.minecraft().options.keyUse, useLabel, HintPlacement.LEFT));
        }

        return hints;
    }
}
