package net.jr.client.runtime.context;

import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.jr.client.compat.create.CreateRuntimeState;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.slot.Slots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class SlotExecution {
    private static final ThreadLocal<ExecutionFrame> ACTIVE_EXECUTION = new ThreadLocal<>();

    private SlotExecution() {
    }

    public static Slots currentSlot() {
        Slots slot = currentSlotOrNull();
        if (slot == null) {
            throw new IllegalStateException(
                "No fully bound local client slot is active on this execution path"
            );
        }
        return slot;
    }

    @Nullable
    public static Slots currentSlotOrNull() {
        ExecutionFrame frame = ACTIVE_EXECUTION.get();
        return frame != null ? frame.slot() : null;
    }

    public static void runForSlot(int slotId, Runnable action) {
        runResolved(Minecraft.getInstance(), SlotResolver.requireSlot(slotId), action);
    }

    public static void runForSlot(Minecraft minecraft, int slotId, Runnable action) {
        runResolved(minecraft, SlotResolver.requireSlot(slotId), action);
    }

    public static void runForSlot(Slots slot, Runnable action) {
        runResolved(Minecraft.getInstance(), SlotResolver.requireSlot(slot), action);
    }

    public static <T> T callForSlot(int slotId, Supplier<T> action) {
        return callResolved(Minecraft.getInstance(), SlotResolver.requireSlot(slotId), action);
    }

    public static <T> T callForSlot(Slots slot, Supplier<T> action) {
        return callResolved(Minecraft.getInstance(), SlotResolver.requireSlot(slot), action);
    }

    public static void runPrimary(Runnable action) {
        runResolved(Minecraft.getInstance(), SlotResolver.requirePrimary(), action);
    }

    public static void runPrimary(Minecraft minecraft, Runnable action) {
        runResolved(minecraft, SlotResolver.requirePrimary(), action);
    }

    public static Scope enterPrimary(Minecraft minecraft) {
        return enterResolved(minecraft, SlotResolver.requirePrimary());
    }

    public static Scope enterForSlot(Minecraft minecraft, Slots slot) {
        return enterResolved(minecraft, SlotResolver.requireSlot(slot));
    }

    public static void runActive(Runnable action) {
        runResolved(Minecraft.getInstance(), SlotResolver.requireActive(), action);
    }

    public static void runForScreen(Screen screen, Runnable action) {
        runResolved(Minecraft.getInstance(), SlotResolver.requireScreen(screen), action);
    }

    public static <T> T callForScreen(Screen screen, Supplier<T> action) {
        return callResolved(Minecraft.getInstance(), SlotResolver.requireScreen(screen), action);
    }

    public static void runForKeyboardMouse(Runnable action) {
        runResolved(Minecraft.getInstance(), SlotResolver.requireKeyboardMouseOwner(), action);
    }

    private static void runResolved(Minecraft minecraft, Slots slot, Runnable action) {
        Objects.requireNonNull(action, "action");
        try (Scope ignored = enterResolved(minecraft, slot)) {
            action.run();
        }
    }

    private static <T> T callResolved(Minecraft minecraft, Slots slot, Supplier<T> action) {
        Objects.requireNonNull(action, "action");
        try (Scope ignored = enterResolved(minecraft, slot)) {
            return action.get();
        }
    }

    private static Scope enterResolved(Minecraft minecraft, Slots slot) {
        Objects.requireNonNull(minecraft, "minecraft");
        SlotScope.Scope slotScope = SlotScope.enter(slot.id());
        try {
            MinecraftClientStateScope stateScope = MinecraftClientStateScope.bind(minecraft, slot);
            ClientInstance clientInstance = ClientRuntime.INSTANCE.clients().clientOrNull(slot.id());
            CreateRuntimeState.Scope createStateScope = CreateRuntimeState.activate(clientInstance);
            ExecutionFrame previousExecution = ACTIVE_EXECUTION.get();
            ExecutionFrame execution = new ExecutionFrame(slot);
            ACTIVE_EXECUTION.set(execution);
            return new Scope(
                slotScope,
                stateScope,
                createStateScope,
                execution,
                previousExecution
            );
        } catch (RuntimeException | Error error) {
            slotScope.close();
            throw error;
        }
    }

    public static final class Scope implements AutoCloseable {
        private final SlotScope.Scope slotScope;
        private final MinecraftClientStateScope stateScope;
        private final CreateRuntimeState.Scope createStateScope;
        private final ExecutionFrame execution;
        private final ExecutionFrame previousExecution;
        private boolean closed;

        private Scope(
            SlotScope.Scope slotScope,
            MinecraftClientStateScope stateScope,
            CreateRuntimeState.Scope createStateScope,
            ExecutionFrame execution,
            @Nullable ExecutionFrame previousExecution
        ) {
            this.slotScope = slotScope;
            this.stateScope = stateScope;
            this.createStateScope = createStateScope;
            this.execution = execution;
            this.previousExecution = previousExecution;
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            if (ACTIVE_EXECUTION.get() != this.execution) {
                throw new IllegalStateException("Unbalanced fully bound slot execution scope");
            }
            this.closed = true;
            RuntimeException error = null;
            try {
                this.createStateScope.close();
            } catch (RuntimeException closeError) {
                error = closeError;
            }
            try {
                this.stateScope.close();
            } catch (RuntimeException closeError) {
                if (error == null) {
                    error = closeError;
                } else {
                    error.addSuppressed(closeError);
                }
            }
            try {
                if (this.previousExecution == null) {
                    ACTIVE_EXECUTION.remove();
                } else {
                    ACTIVE_EXECUTION.set(this.previousExecution);
                }
            } finally {
                try {
                    this.slotScope.close();
                } catch (RuntimeException closeError) {
                    if (error == null) {
                        error = closeError;
                    } else {
                        error.addSuppressed(closeError);
                    }
                }
            }
            if (error != null) {
                throw error;
            }
        }
    }

    private record ExecutionFrame(Slots slot) {
    }
}
