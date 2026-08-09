package net.alnv.javareforged.ClientRuntime.runtime;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import net.alnv.javareforged.ClientRuntime.slot.PlayerSlot;
import net.minecraft.client.Minecraft;

/**
 * Central execution scope for code that is running as a specific local slot.
 *
 * <p>Think of this as putting Minecraft's global singleton mask on for one
 * player while also carrying a LocalClient for our own code.</p>
 */
public final class LocalClientScope implements AutoCloseable {
    private static final ThreadLocal<LocalClient> CURRENT_CLIENT = new ThreadLocal<>();

    private final LocalClient client;
    private final LocalClient previousClient;
    private final ActiveSlot.Scope activeSlotScope;
    private final RawMinecraftStateScope rawMinecraftStateScope;
    private boolean closed;

    private LocalClientScope(
            LocalClient client,
            LocalClient previousClient,
            ActiveSlot.Scope activeSlotScope,
            RawMinecraftStateScope rawMinecraftStateScope
    ) {
        this.client = client;
        this.previousClient = previousClient;
        this.activeSlotScope = activeSlotScope;
        this.rawMinecraftStateScope = rawMinecraftStateScope;
    }

    public static LocalClientScope enter(int slotId) {
        return enter(LocalPlayers.INSTANCE.slots().slot(slotId));
    }

    public static LocalClientScope enter(PlayerSlot slot) {
        return enter(Minecraft.getInstance(), slot);
    }

    static LocalClientScope enter(Minecraft minecraft, PlayerSlot slot) {
        Objects.requireNonNull(minecraft, "minecraft");
        Objects.requireNonNull(slot, "slot");

        ActiveSlot.Scope activeSlotScope = ActiveSlot.enter(slot.id());
        try {
            RawMinecraftStateScope rawMinecraftStateScope = RawMinecraftStateScope.bind(minecraft, slot);
            LocalClient previousClient = CURRENT_CLIENT.get();
            LocalClient client = new LocalClient(slot);
            CURRENT_CLIENT.set(client);
            return new LocalClientScope(client, previousClient, activeSlotScope, rawMinecraftStateScope);
        } catch (RuntimeException | Error error) {
            activeSlotScope.close();
            throw error;
        }
    }

    public static void run(int slotId, Consumer<ClientSlotContext> action) {
        Objects.requireNonNull(action, "action");
        try (LocalClientScope scope = enter(slotId)) {
            action.accept(scope.slot());
        }
    }

    public static void run(PlayerSlot slot, Consumer<ClientSlotContext> action) {
        Objects.requireNonNull(action, "action");
        try (LocalClientScope scope = enter(slot)) {
            action.accept(scope.slot());
        }
    }

    public static void runClient(int slotId, Consumer<LocalClient> action) {
        Objects.requireNonNull(action, "action");
        try (LocalClientScope scope = enter(slotId)) {
            action.accept(scope.client());
        }
    }

    public static void runClient(PlayerSlot slot, Consumer<LocalClient> action) {
        Objects.requireNonNull(action, "action");
        try (LocalClientScope scope = enter(slot)) {
            action.accept(scope.client());
        }
    }

    public static <T> T call(int slotId, Function<ClientSlotContext, T> action) {
        Objects.requireNonNull(action, "action");
        try (LocalClientScope scope = enter(slotId)) {
            return action.apply(scope.slot());
        }
    }

    public static <T> T call(PlayerSlot slot, Function<ClientSlotContext, T> action) {
        Objects.requireNonNull(action, "action");
        try (LocalClientScope scope = enter(slot)) {
            return action.apply(scope.slot());
        }
    }

    public static <T> T callClient(int slotId, Function<LocalClient, T> action) {
        Objects.requireNonNull(action, "action");
        try (LocalClientScope scope = enter(slotId)) {
            return action.apply(scope.client());
        }
    }

    public static <T> T callClient(PlayerSlot slot, Function<LocalClient, T> action) {
        Objects.requireNonNull(action, "action");
        try (LocalClientScope scope = enter(slot)) {
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

    public ClientSlotContext slot() {
        return this.client.context();
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
            this.rawMinecraftStateScope.close();
        } catch (RuntimeException error) {
            if (closeError == null) {
                closeError = error;
            } else {
                closeError.addSuppressed(error);
            }
        } finally {
            this.activeSlotScope.close();
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
