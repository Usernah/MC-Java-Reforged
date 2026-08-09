package net.alnv.javareforged.ClientRuntime.runtime;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import net.alnv.javareforged.ClientRuntime.slot.PlayerSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;

/**
 * The only public entry point that installs a local-client execution context.
 * Code below this boundary reads state through {@link Client} and never resolves slots.
 */
public final class ClientBoundary {
    private ClientBoundary() {
    }

    public static void runForSlot(PlayerSlot slot, Runnable action) {
        runResolved(ClientResolver.requireSlot(slot), action);
    }

    /** High-level explicit client admission; lower layers only read {@link Client}. */
    public static void runForClient(int clientId, Runnable action) {
        runResolved(LocalPlayers.INSTANCE.slots().slot(clientId), action);
    }

    public static <T> T callForSlot(PlayerSlot slot, Supplier<T> action) {
        return callResolved(ClientResolver.requireSlot(slot), action);
    }

    public static void runPrimary(Runnable action) {
        runResolved(ClientResolver.requirePrimary(), action);
    }

    public static void runPrimary(Minecraft minecraft, Runnable action) {
        runResolved(minecraft, ClientResolver.requirePrimary(), action);
    }

    public static Scope enterPrimary(Minecraft minecraft) {
        return enterResolved(minecraft, ClientResolver.requirePrimary());
    }

    public static Scope enterForSlot(Minecraft minecraft, PlayerSlot slot) {
        return enterResolved(minecraft, ClientResolver.requireSlot(slot));
    }

    public static <T> T callPrimary(Supplier<T> action) {
        return callResolved(ClientResolver.requirePrimary(), action);
    }

    public static void runActive(Runnable action) {
        runResolved(ClientResolver.requireActive(), action);
    }

    public static <T> T callActive(Supplier<T> action) {
        return callResolved(ClientResolver.requireActive(), action);
    }

    public static void runForConnection(Connection connection, Runnable action) {
        runResolved(ClientResolver.requireConnection(connection), action);
    }

    public static <T> T callForConnection(Connection connection, Supplier<T> action) {
        return callResolved(ClientResolver.requireConnection(connection), action);
    }

    public static void runForListener(ClientPacketListener listener, Runnable action) {
        runResolved(ClientResolver.requireListener(listener), action);
    }

    public static <T> T callForListener(ClientPacketListener listener, Supplier<T> action) {
        return callResolved(ClientResolver.requireListener(listener), action);
    }

    public static void runForScreen(Screen screen, Runnable action) {
        runResolved(ClientResolver.requireScreen(screen), action);
    }

    public static <T> T callForScreen(Screen screen, Supplier<T> action) {
        return callResolved(ClientResolver.requireScreen(screen), action);
    }

    public static void runForKeyboardMouse(Runnable action) {
        runResolved(ClientResolver.requireKeyboardMouseOwner(), action);
    }

    public static ClientToken captureToken() {
        return ClientResolver.captureCurrentToken();
    }

    public static boolean isValid(ClientToken token) {
        return ClientResolver.isTokenValid(token);
    }

    public static void runCaptured(ClientToken token, Runnable action) {
        runResolved(ClientResolver.requireToken(token), action);
    }

    public static <T> T callCaptured(ClientToken token, Supplier<T> action) {
        return callResolved(ClientResolver.requireToken(token), action);
    }

    public static Runnable capture(Runnable action) {
        Objects.requireNonNull(action, "action");
        ClientToken token = captureToken();
        return () -> {
            if (isValid(token)) {
                runCaptured(token, action);
            }
        };
    }

    public static <T> Callable<T> capture(Callable<T> action) {
        Objects.requireNonNull(action, "action");
        ClientToken token = captureToken();
        return () -> {
            if (!isValid(token)) {
                throw new CancellationException("Local client session expired before async execution");
            }
            return callResolved(ClientResolver.requireToken(token), action);
        };
    }

    public static Executor contextual(Executor executor) {
        Objects.requireNonNull(executor, "executor");
        return action -> executor.execute(capture(action));
    }

    public static Runnable wrapScheduled(Runnable action) {
        Objects.requireNonNull(action, "action");
        ClientToken token = ClientResolver.captureScheduledTokenOrNull();
        if (token == null) {
            return action;
        }
        return () -> {
            if (isValid(token)) {
                runCaptured(token, action);
            }
        };
    }

    private static void runResolved(PlayerSlot slot, Runnable action) {
        Objects.requireNonNull(action, "action");
        try (LocalClientScope ignored = LocalClientScope.enter(slot)) {
            action.run();
        }
    }

    private static void runResolved(Minecraft minecraft, PlayerSlot slot, Runnable action) {
        Objects.requireNonNull(action, "action");
        try (Scope ignored = enterResolved(minecraft, slot)) {
            action.run();
        }
    }

    private static Scope enterResolved(Minecraft minecraft, PlayerSlot slot) {
        return new Scope(LocalClientScope.enter(minecraft, slot));
    }

    private static <T> T callResolved(PlayerSlot slot, Supplier<T> action) {
        Objects.requireNonNull(action, "action");
        try (LocalClientScope ignored = LocalClientScope.enter(slot)) {
            return action.get();
        }
    }

    private static <T> T callResolved(PlayerSlot slot, Callable<T> action) throws Exception {
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
