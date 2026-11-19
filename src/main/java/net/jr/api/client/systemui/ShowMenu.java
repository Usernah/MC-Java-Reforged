package net.jr.api.client.systemui;

import java.util.Objects;
import net.jr.client.systemui.SystemUiManager;

public final class ShowMenu {
    private final SystemUiTarget target;
    private final SystemOverlay menu;
    private SystemUiTarget lastTarget;
    private SystemOverlay lastMenu;
    private boolean useCurrentAsLast;

    private ShowMenu(SystemUiTarget target, SystemOverlay menu) {
        this.target = Objects.requireNonNull(target, "target");
        this.menu = Objects.requireNonNull(menu, "menu");
    }

    public static ShowMenu of(SystemOverlay menu) {
        return of(SystemUiTarget.fullWindow(), menu);
    }

    public static ShowMenu of(SystemUiTarget target, SystemOverlay menu) {
        return new ShowMenu(target, menu);
    }

    public ShowMenu lastMenu() {
        this.useCurrentAsLast = true;
        this.lastTarget = null;
        this.lastMenu = null;
        return this;
    }

    public ShowMenu lastMenu(SystemOverlay menu) {
        return this.lastMenu(this.target, menu);
    }

    public ShowMenu lastMenu(SystemUiTarget target, SystemOverlay menu) {
        this.useCurrentAsLast = false;
        this.lastTarget = Objects.requireNonNull(target, "target");
        this.lastMenu = Objects.requireNonNull(menu, "menu");
        return this;
    }

    public SystemUiHandle show() {
        SystemUiManager manager = SystemUiManager.get();
        SystemUiManager.MenuHistory history = this.useCurrentAsLast
            ? manager.captureMenu(this.target)
            : this.lastMenu == null
                ? null
                : new SystemUiManager.MenuHistory(this.lastTarget, this.lastMenu, null);
        return manager.showMenu(this.target, this.menu, history);
    }
}
