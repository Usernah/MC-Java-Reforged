package net.jr.api.client.systemui;

import net.jr.client.systemui.SystemUiManager;

public final class SystemUiHandle {
    private final long id;
    private final SystemUiTarget target;

    public SystemUiHandle(long id, SystemUiTarget target) {
        this.id = id;
        this.target = target;
    }

    public long id() {
        return this.id;
    }

    public SystemUiTarget target() {
        return this.target;
    }

    public boolean isOpen() {
        return SystemUiManager.get().isOpen(this);
    }

    public void close() {
        this.close(SystemUiCloseReason.CANCELLED);
    }

    public void close(SystemUiCloseReason reason) {
        SystemUiManager.get().close(this, reason);
    }

    public void back() {
        SystemUiManager.get().back(this);
    }
}
