package net.jr.client.runtime.context;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import net.jr.client.runtime.client.LocalClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;

public final class LocalClientExecution {
    private LocalClientExecution() {
    }

    public static void runForClient(int slotId, Runnable action) {
        runResolved(
            Minecraft.getInstance(),
            LocalClientResolver.requireClient(slotId),
            action
        );
    }

    public static <T> T callForClient(int slotId, Supplier<T> action) {
        return callResolved(
            Minecraft.getInstance(),
            LocalClientResolver.requireClient(slotId),
            action
        );
    }

    public static void runPrimary(Runnable action) {
        runResolved(
            Minecraft.getInstance(),
            LocalClientResolver.requirePrimary(),
            action
        );
    }

    public static void runPrimary(Minecraft minecraft, Runnable action) {
        runResolved(minecraft, LocalClientResolver.requirePrimary(), action);
    }

    public static Scope enterPrimary(Minecraft minecraft) {
        return enterResolved(minecraft, LocalClientResolver.requirePrimary());
    }

    public static Scope enterForClient(Minecraft minecraft, int slotId) {
        return enterResolved(minecraft, LocalClientResolver.requireClient(slotId));
    }

    public static void runActive(Runnable action) {
        runResolved(
            Minecraft.getInstance(),
            LocalClientResolver.requireActive(),
            action
        );
    }

    public static <T> T callActive(Supplier<T> action) {
        return callResolved(
            Minecraft.getInstance(),
            LocalClientResolver.requireActive(),
            action
        );
    }

    public static void runForConnection(Connection connection, Runnable action) {
        runResolved(
            Minecraft.getInstance(),
            LocalClientResolver.requireConnection(connection),
            action
        );
    }

    public static <T> T callForConnection(Connection connection, Supplier<T> action) {
        return callResolved(
            Minecraft.getInstance(),
            LocalClientResolver.requireConnection(connection),
            action
        );
    }

    public static void runForListener(ClientPacketListener listener, Runnable action) {
        runResolved(
            Minecraft.getInstance(),
            LocalClientResolver.requireListener(listener),
            action
        );
    }

    public static <T> T callForListener(ClientPacketListener listener, Supplier<T> action) {
        return callResolved(
            Minecraft.getInstance(),
            LocalClientResolver.requireListener(listener),
            action
        );
    }

    public static LocalClientToken captureToken() {
        return LocalClientResolver.captureCurrentToken();
    }

    public static boolean isValid(LocalClientToken token) {
        return LocalClientResolver.isTokenValid(token);
    }

    public static void runCaptured(LocalClientToken token, Runnable action) {
        runResolved(
            Minecraft.getInstance(),
            LocalClientResolver.requireToken(token),
            action
        );
    }

    public static <T> T callCaptured(LocalClientToken token, Supplier<T> action) {
        return callResolved(
            Minecraft.getInstance(),
            LocalClientResolver.requireToken(token),
            action
        );
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
            return callResolved(
                Minecraft.getInstance(),
                LocalClientResolver.requireToken(token),
                action
            );
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

    private static void runResolved(Minecraft minecraft, LocalClient client, Runnable action) {
        Objects.requireNonNull(action, "action");
        try (Scope ignored = enterResolved(minecraft, client)) {
            action.run();
        }
    }

    private static <T> T callResolved(
        Minecraft minecraft,
        LocalClient client,
        Supplier<T> action
    ) {
        Objects.requireNonNull(action, "action");
        try (Scope ignored = enterResolved(minecraft, client)) {
            return action.get();
        }
    }

    private static <T> T callResolved(
        Minecraft minecraft,
        LocalClient client,
        Callable<T> action
    ) throws Exception {
        Objects.requireNonNull(action, "action");
        try (Scope ignored = enterResolved(minecraft, client)) {
            return action.call();
        }
    }

    private static Scope enterResolved(Minecraft minecraft, LocalClient client) {
        return new Scope(LocalClientScope.enter(minecraft, client));
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
