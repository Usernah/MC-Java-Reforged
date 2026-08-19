package net.jr.client.runtime.context;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClient;
import net.minecraft.client.Minecraft;

public final class LocalClientScope implements AutoCloseable {
    private static final ThreadLocal<LocalClient> CURRENT_CLIENT = new ThreadLocal<>();

    private final LocalClient client;
    private final LocalClient previousClient;
    private final SlotExecution.Scope slotExecutionScope;
    private boolean closed;

    private LocalClientScope(
        LocalClient client,
        LocalClient previousClient,
        SlotExecution.Scope slotExecutionScope
    ) {
        this.client = client;
        this.previousClient = previousClient;
        this.slotExecutionScope = slotExecutionScope;
    }

    public static LocalClientScope enter(int slotId) {
        return enter(
            Minecraft.getInstance(),
            ClientRuntime.INSTANCE.clients().client(slotId)
        );
    }

    public static LocalClientScope enter(LocalClient client) {
        return enter(Minecraft.getInstance(), client);
    }

    static LocalClientScope enter(Minecraft minecraft, LocalClient client) {
        Objects.requireNonNull(minecraft, "minecraft");
        Objects.requireNonNull(client, "client");

        SlotExecution.Scope slotExecutionScope =
            SlotExecution.enterForSlot(minecraft, client.slot());

        try {
            LocalClient previousClient = CURRENT_CLIENT.get();
            CURRENT_CLIENT.set(client);
            return new LocalClientScope(client, previousClient, slotExecutionScope);
        } catch (RuntimeException | Error error) {
            slotExecutionScope.close();
            throw error;
        }
    }

    public static void run(int slotId, Consumer<LocalClient> action) {
        Objects.requireNonNull(action, "action");
        try (LocalClientScope scope = enter(slotId)) {
            action.accept(scope.client());
        }
    }

    public static void run(LocalClient client, Consumer<LocalClient> action) {
        Objects.requireNonNull(action, "action");
        try (LocalClientScope scope = enter(client)) {
            action.accept(scope.client());
        }
    }

    public static <T> T call(int slotId, Function<LocalClient, T> action) {
        Objects.requireNonNull(action, "action");
        try (LocalClientScope scope = enter(slotId)) {
            return action.apply(scope.client());
        }
    }

    public static <T> T call(LocalClient client, Function<LocalClient, T> action) {
        Objects.requireNonNull(action, "action");
        try (LocalClientScope scope = enter(client)) {
            return action.apply(scope.client());
        }
    }

    public static LocalClient currentClient() {
        LocalClient client = CURRENT_CLIENT.get();
        if (client == null) {
            throw new IllegalStateException("No local client is bound to this execution path");
        }
        return client;
    }

    public static LocalClient currentClientOrNull() {
        return CURRENT_CLIENT.get();
    }

    public LocalClient client() {
        return this.client;
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;

        RuntimeException closeError = null;
        try {
            this.restoreCurrentClient();
        } catch (RuntimeException error) {
            closeError = error;
        }

        try {
            this.slotExecutionScope.close();
        } catch (RuntimeException error) {
            if (closeError == null) {
                closeError = error;
            } else {
                closeError.addSuppressed(error);
            }
        }

        if (closeError != null) {
            throw closeError;
        }
    }

    private void restoreCurrentClient() {
        LocalClient currentClient = CURRENT_CLIENT.get();
        if (currentClient != this.client) {
            throw new IllegalStateException(
                "Unbalanced local client scope: expected slot "
                    + this.client.slotId()
                    + ", found "
                    + (currentClient == null ? "none" : currentClient.slotId())
            );
        }

        if (this.previousClient == null) {
            CURRENT_CLIENT.remove();
        } else {
            CURRENT_CLIENT.set(this.previousClient);
        }
    }
}
