package net.jr.client.runtime.context;

import java.util.Objects;
import java.util.function.Supplier;
import net.jr.client.compat.create.CreateRuntimeState;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class SlotExecution {
    private SlotExecution() {
    }

    public static void runForSlot(int slotId, Runnable action) {
        runResolved(Minecraft.getInstance(), SlotResolver.requireSlot(slotId), action);
    }

    public static void runForSlot(Minecraft minecraft, int slotId, Runnable action) {
        runResolved(minecraft, SlotResolver.requireSlot(slotId), action);
    }

    public static void runForSlot(LocalClientSlot slot, Runnable action) {
        runResolved(Minecraft.getInstance(), SlotResolver.requireSlot(slot), action);
    }

    public static <T> T callForSlot(int slotId, Supplier<T> action) {
        return callResolved(Minecraft.getInstance(), SlotResolver.requireSlot(slotId), action);
    }

    public static <T> T callForSlot(LocalClientSlot slot, Supplier<T> action) {
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

    public static Scope enterForSlot(Minecraft minecraft, LocalClientSlot slot) {
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

    private static void runResolved(Minecraft minecraft, LocalClientSlot slot, Runnable action) {
        Objects.requireNonNull(action, "action");
        try (Scope ignored = enterResolved(minecraft, slot)) {
            action.run();
        }
    }

    private static <T> T callResolved(Minecraft minecraft, LocalClientSlot slot, Supplier<T> action) {
        Objects.requireNonNull(action, "action");
        try (Scope ignored = enterResolved(minecraft, slot)) {
            return action.get();
        }
    }

    private static Scope enterResolved(Minecraft minecraft, LocalClientSlot slot) {
        Objects.requireNonNull(minecraft, "minecraft");
        SlotScope.Scope slotScope = SlotScope.enter(slot.id());
        try {
            MinecraftClientStateScope stateScope = MinecraftClientStateScope.bind(minecraft, slot);
            LocalClient localClient = ClientRuntime.INSTANCE.clients().clientOrNull(slot.id());
            CreateRuntimeState.Scope createStateScope = CreateRuntimeState.activate(localClient);
            return new Scope(slotScope, stateScope, createStateScope);
        } catch (RuntimeException | Error error) {
            slotScope.close();
            throw error;
        }
    }

    public static final class Scope implements AutoCloseable {
        private final SlotScope.Scope slotScope;
        private final MinecraftClientStateScope stateScope;
        private final CreateRuntimeState.Scope createStateScope;
        private boolean closed;

        private Scope(
            SlotScope.Scope slotScope,
            MinecraftClientStateScope stateScope,
            CreateRuntimeState.Scope createStateScope
        ) {
            this.slotScope = slotScope;
            this.stateScope = stateScope;
            this.createStateScope = createStateScope;
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
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
}
