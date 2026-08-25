package net.jr.client.ui.hint.provider;

import java.util.List;
import net.jr.client.ui.hint.ControlHintContext;
import net.jr.client.ui.hint.model.ControlHintRequest;
import net.jr.client.ui.navigation.tabs.TabNavigationHost;

public final class TabNavigationControlHintProvider implements ControlHintProvider {
    @Override
    public boolean supports(ControlHintContext context) {
        return context.screen() instanceof TabNavigationHost;
    }

    @Override
    public List<ControlHintRequest> buildHints(ControlHintContext context) {
        return context.screen() instanceof TabNavigationHost host
                ? host.tabNavigationController().hintRequests()
                : List.of();
    }
}
