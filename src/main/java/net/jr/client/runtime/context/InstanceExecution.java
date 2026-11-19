package net.jr.client.runtime.context;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.viewport.ViewportArea;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;

public final class InstanceExecution {
    private InstanceExecution() {
    }

    public static void runForClient(int slotId, Runnable action) {
        runResolved(
            Minecraft.getInstance(),
            InstanceResolver.requireClient(slotId),
            action
        );
    }

    public static void runForClient(int slotId, ViewportArea viewport, Runnable action) {
        runResolved(
            Minecraft.getInstance(),
            InstanceResolver.requireClient(slotId),
            viewport,
            action
        );
    }

    public static <T> T callForClient(int slotId, Supplier<T> action) {
        return callResolved(
            Minecraft.getInstance(),
            InstanceResolver.requireClient(slotId),
            action
        );
    }

    public static void runPrimary(Runnable action) {
        runResolved(
            Minecraft.getInstance(),
            InstanceResolver.requirePrimary(),
            action
        );
    }

    public static void runPrimary(Minecraft minecraft, Runnable action) {
        runResolved(minecraft, InstanceResolver.requirePrimary(), action);
    }

    public static Scope enterPrimary(Minecraft minecraft) {
        return enterResolved(minecraft, InstanceResolver.requirePrimary());
    }

    public static Scope enterForClient(Minecraft minecraft, int slotId) {
        return enterResolved(minecraft, InstanceResolver.requireClient(slotId));
    }

    public static void runActive(Runnable action) {
        runResolved(
            Minecraft.getInstance(),
            InstanceResolver.requireActive(),
            action
        );
    }

    public static <T> T callActive(Supplier<T> action) {
        return callResolved(
            Minecraft.getInstance(),
            InstanceResolver.requireActive(),
            action
        );
    }

    public static void runForConnection(Connection connection, Runnable action) {
        runResolved(
            Minecraft.getInstance(),
            InstanceResolver.requireConnection(connection),
            action
        );
    }

    public static <T> T callForConnection(Connection connection, Supplier<T> action) {
        return callResolved(
            Minecraft.getInstance(),
            InstanceResolver.requireConnection(connection),
            action
        );
    }

    public static void runForListener(ClientPacketListener listener, Runnable action) {
        runResolved(
            Minecraft.getInstance(),
            InstanceResolver.requireListener(listener),
            action
        );
    }

    public static <T> T callForListener(ClientPacketListener listener, Supplier<T> action) {
        return callResolved(
            Minecraft.getInstance(),
            InstanceResolver.requireListener(listener),
            action
        );
    }

    public static SessionToken captureToken() {
        return InstanceResolver.captureCurrentToken();
    }

    public static boolean isValid(SessionToken token) {
        return InstanceResolver.isTokenValid(token);
    }

    public static void runCaptured(SessionToken token, Runnable action) {
        runResolved(
            Minecraft.getInstance(),
            InstanceResolver.requireToken(token),
            action
        );
    }

    public static <T> T callCaptured(SessionToken token, Supplier<T> action) {
        return callResolved(
            Minecraft.getInstance(),
            InstanceResolver.requireToken(token),
            action
        );
    }

    public static Runnable capture(Runnable action) {
        Objects.requireNonNull(action, "action");
        SessionToken token = captureToken();
        return () -> {
            if (isValid(token)) {
                runCaptured(token, action);
            }
        };
    }

    public static <T> Callable<T> capture(Callable<T> action) {
        Objects.requireNonNull(action, "action");
        SessionToken token = captureToken();
        return () -> {
            if (!isValid(token)) {
                throw new CancellationException("Local client session expired before async execution");
            }
            return callResolved(
                Minecraft.getInstance(),
                InstanceResolver.requireToken(token),
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
        SessionToken token = InstanceResolver.captureScheduledTokenOrNull();
        if (token == null) {
            return action;
        }
        return () -> {
            if (isValid(token)) {
                runCaptured(token, action);
            }
        };
    }

    private static void runResolved(Minecraft minecraft, ClientInstance client, Runnable action) {
        Objects.requireNonNull(action, "action");
        try (Scope ignored = enterResolved(minecraft, client)) {
            action.run();
        }
    }

    private static void runResolved(
        Minecraft minecraft,
        ClientInstance client,
        ViewportArea viewport,
        Runnable action
    ) {
        Objects.requireNonNull(action, "action");
        try (Scope ignored = enterResolved(minecraft, client, viewport)) {
            action.run();
        }
    }

    private static <T> T callResolved(
        Minecraft minecraft,
        ClientInstance client,
        Supplier<T> action
    ) {
        Objects.requireNonNull(action, "action");
        try (Scope ignored = enterResolved(minecraft, client)) {
            return action.get();
        }
    }

    private static <T> T callResolved(
        Minecraft minecraft,
        ClientInstance client,
        Callable<T> action
    ) throws Exception {
        Objects.requireNonNull(action, "action");
        try (Scope ignored = enterResolved(minecraft, client)) {
            return action.call();
        }
    }

    private static Scope enterResolved(Minecraft minecraft, ClientInstance client) {
        return new Scope(SlotExecution.enterForSlot(minecraft, client.slot()));
    }

    private static Scope enterResolved(
        Minecraft minecraft,
        ClientInstance client,
        ViewportArea viewport
    ) {
        return new Scope(SlotExecution.enterForSlot(minecraft, client.slot(), viewport));
    }

    public static final class Scope implements AutoCloseable {
        private final SlotExecution.Scope slotExecutionScope;

        private Scope(SlotExecution.Scope slotExecutionScope) {
            this.slotExecutionScope = slotExecutionScope;
        }

        @Override
        public void close() {
            this.slotExecutionScope.close();
        }
    }
}
