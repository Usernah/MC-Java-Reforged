package net.jr.client.runtime.context;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;

/**
 * The only public entry point that installs a local-client execution context.
 * Code below this boundary reads state through {@link LocalClientAcces} and never resolves slots.
 */
public final class LocalClientExecution {
    private LocalClientExecution() {
    }

    public static void runForSlot(LocalClientSlot slot, Runnable action) {
        runResolved(LocalClientResolver.requireSlot(slot), action);
    }

    /** High-level explicit client admission; lower layers only read {@link LocalClientAcces}. */
    public static void runForClient(int clientId, Runnable action) {
        runResolved(ClientRuntime.INSTANCE.slots().slot(clientId), action);
    }

    public static <T> T callForSlot(LocalClientSlot slot, Supplier<T> action) {
        return callResolved(LocalClientResolver.requireSlot(slot), action);
    }

    public static void runPrimary(Runnable action) {
        runResolved(LocalClientResolver.requirePrimary(), action);
    }

    public static void runPrimary(Minecraft minecraft, Runnable action) {
        runResolved(minecraft, LocalClientResolver.requirePrimary(), action);
    }

    public static Scope enterPrimary(Minecraft minecraft) {
        return enterResolved(minecraft, LocalClientResolver.requirePrimary());
    }

    public static Scope enterForSlot(Minecraft minecraft, LocalClientSlot slot) {
        return enterResolved(minecraft, LocalClientResolver.requireSlot(slot));
    }

    public static <T> T callPrimary(Supplier<T> action) {
        return callResolved(LocalClientResolver.requirePrimary(), action);
    }

    public static void runActive(Runnable action) {
        runResolved(LocalClientResolver.requireActive(), action);
    }

    public static <T> T callActive(Supplier<T> action) {
        return callResolved(LocalClientResolver.requireActive(), action);
    }

    public static void runForConnection(Connection connection, Runnable action) {
        runResolved(LocalClientResolver.requireConnection(connection), action);
    }

    public static <T> T callForConnection(Connection connection, Supplier<T> action) {
        return callResolved(LocalClientResolver.requireConnection(connection), action);
    }

    public static void runForListener(ClientPacketListener listener, Runnable action) {
        runResolved(LocalClientResolver.requireListener(listener), action);
    }

    public static <T> T callForListener(ClientPacketListener listener, Supplier<T> action) {
        return callResolved(LocalClientResolver.requireListener(listener), action);
    }

    public static void runForScreen(Screen screen, Runnable action) {
        runResolved(LocalClientResolver.requireScreen(screen), action);
    }

    public static <T> T callForScreen(Screen screen, Supplier<T> action) {
        return callResolved(LocalClientResolver.requireScreen(screen), action);
    }

    public static void runForKeyboardMouse(Runnable action) {
        runResolved(LocalClientResolver.requireKeyboardMouseOwner(), action);
    }

    public static LocalClientToken captureToken() {
        return LocalClientResolver.captureCurrentToken();
    }

    public static boolean isValid(LocalClientToken token) {
        return LocalClientResolver.isTokenValid(token);
    }

    public static void runCaptured(LocalClientToken token, Runnable action) {
        runResolved(LocalClientResolver.requireToken(token), action);
    }

    public static <T> T callCaptured(LocalClientToken token, Supplier<T> action) {
        return callResolved(LocalClientResolver.requireToken(token), action);
    }

    public static Runnable capture(Runnable action) {
        Objects.requireNonNull(action, "action");
        LocalClientToken token = captureToken();
        return () -> {
            if (isValid(token)) {
                runCaptured(token, action);
            }
        };
    }

    public static <T> Callable<T> capture(Callable<T> action) {
        Objects.requireNonNull(action, "action");
        LocalClientToken token = captureToken();
        return () -> {
            if (!isValid(token)) {
                throw new CancellationException("Local client session expired before async execution");
            }
            return callResolved(LocalClientResolver.requireToken(token), action);
        };
    }

    public static Executor contextual(Executor executor) {
        Objects.requireNonNull(executor, "executor");
        return action -> executor.execute(capture(action));
    }

    public static Runnable wrapScheduled(Runnable action) {
        Objects.requireNonNull(action, "action");
        LocalClientToken token = LocalClientResolver.captureScheduledTokenOrNull();
        if (token == null) {
            return action;
        }
        return () -> {
            if (isValid(token)) {
                runCaptured(token, action);
            }
        };
    }

    private static void runResolved(LocalClientSlot slot, Runnable action) {
        Objects.requireNonNull(action, "action");
        try (LocalClientScope ignored = LocalClientScope.enter(slot)) {
            action.run();
        }
    }

    private static void runResolved(Minecraft minecraft, LocalClientSlot slot, Runnable action) {
        Objects.requireNonNull(action, "action");
        try (Scope ignored = enterResolved(minecraft, slot)) {
            action.run();
        }
    }

    private static Scope enterResolved(Minecraft minecraft, LocalClientSlot slot) {
        return new Scope(LocalClientScope.enter(minecraft, slot));
    }

    private static <T> T callResolved(LocalClientSlot slot, Supplier<T> action) {
        Objects.requireNonNull(action, "action");
        try (LocalClientScope ignored = LocalClientScope.enter(slot)) {
            return action.get();
        }
    }

    private static <T> T callResolved(LocalClientSlot slot, Callable<T> action) throws Exception {
        Objects.requireNonNull(action, "action");
        try (LocalClientScope ignored = LocalClientScope.enter(slot)) {
            return action.call();
        }
    }

    public static final class Scope implements AutoCloseable {
        private final LocalClientScope localClientScope;

        private Scope(LocalClientScope localClientScope) {
            this.localClientScope = localClientScope;
        }

        @Override
        public void close() {
            this.localClientScope.close();
        }
    }
}
