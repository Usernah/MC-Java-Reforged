package net.jr.client.ui.navigation;

import java.util.List;

public interface FocusCarouselHost {
    default List<FocusNavigationTarget> focusCarouselTargets(
        FocusNavigationTarget origin,
        List<FocusNavigationTarget> targets,
        int keyCode
    ) {
        return targets;
    }
}
