package net.jr.client.runtime.state;

import javax.annotation.Nullable;

public final class GlobalToastState {
    @Nullable
    private Object nowPlayingToast;

    @Nullable
    public Object nowPlayingToast() {
        return this.nowPlayingToast;
    }

    public void setNowPlayingToast(@Nullable Object nowPlayingToast) {
        this.nowPlayingToast = nowPlayingToast;
    }
}