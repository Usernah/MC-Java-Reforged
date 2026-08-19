package net.jr.client.runtime.bridge;

import java.util.BitSet;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.state.GlobalToastState;
import net.jr.client.runtime.state.ToastState;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.sounds.SoundEvent;

public final class ToastManagerStateAccess {
    private static final ThreadLocal<PassMode> PASS_MODE = new ThreadLocal<>();
    private static final ToastState BOOTSTRAP_STATE = new ToastState();
    private static final ToastState GLOBAL_PASS_STATE = new ToastState();
    private static final GlobalToastState GLOBAL_STATE = new GlobalToastState();

    private ToastManagerStateAccess() {
    }

    public static List<Object> visibleToasts(ToastManager manager) {
        return state().visibleToasts();
    }

    public static BitSet occupiedSlots(ToastManager manager) {
        return state().occupiedSlots();
    }

    public static Deque<Toast> queued(ToastManager manager) {
        return state().queued();
    }

    public static Set<SoundEvent> playedToastSounds(ToastManager manager) {
        return state().playedToastSounds();
    }

    @Nullable
    public static Object nowPlayingToast(ToastManager manager) {
        return PASS_MODE.get() == PassMode.LOCAL ? null : GLOBAL_STATE.nowPlayingToast();
    }

    public static void setNowPlayingToast(ToastManager manager, @Nullable Object nowPlayingToast) {
        GLOBAL_STATE.setNowPlayingToast(nowPlayingToast);
    }

    public static void bootstrapPrimary() {
        BOOTSTRAP_STATE.drainInto(ClientRuntime.INSTANCE.slots().primary().toastState());
    }

    public static PassScope enterLocalPass() {
        return enter(PassMode.LOCAL);
    }

    public static PassScope enterGlobalPass() {
        GLOBAL_PASS_STATE.clear();
        return enter(PassMode.GLOBAL);
    }

    private static PassScope enter(PassMode mode) {
        PassMode previous = PASS_MODE.get();
        PASS_MODE.set(mode);
        return new PassScope(mode, previous);
    }

    private static ToastState state() {
        PassMode mode = PASS_MODE.get();
        if (mode == PassMode.GLOBAL) {
            return GLOBAL_PASS_STATE;
        }
        Integer slotId = SlotScope.idOrNull();
        if (slotId != null) {
            return ClientRuntime.INSTANCE.slots().slot(slotId).toastState();
        }
        return BOOTSTRAP_STATE;
    }

    private enum PassMode {
        LOCAL,
        GLOBAL
    }

    public static final class PassScope implements AutoCloseable {
        private final PassMode installed;
        private final PassMode previous;
        private boolean closed;

        private PassScope(PassMode installed, PassMode previous) {
            this.installed = installed;
            this.previous = previous;
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            PassMode current = PASS_MODE.get();
            if (current != this.installed) {
                throw new IllegalStateException(
                    "Toast pass scope is unbalanced: expected " + this.installed + ", found " + current
                );
            }
            this.closed = true;
            if (this.installed == PassMode.GLOBAL) {
                GLOBAL_PASS_STATE.clear();
            }
            if (this.previous == null) {
                PASS_MODE.remove();
            } else {
                PASS_MODE.set(this.previous);
            }
        }
    }
}
