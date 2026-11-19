package net.jr.experimental_features.pre_join;

import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nullable;
import net.jr.api.client.input.IGamepad;
import net.jr.Java_reforged;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.user.UserProfile;
import net.jr.client.runtime.ui.SlotScreenTransitionHandler;
import net.jr.client.runtime.client.ReadinessPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;


public final class PreJoinWorldLoad {
    private static final Map<Integer, PendingPlayer> PENDING_PLAYERS = new TreeMap<>();

    private static boolean active;
    private static boolean loadingObserved;
    private static boolean leftSourceScreen;
    private static @Nullable Screen sourceScreen;

    private PreJoinWorldLoad() {
    }

    public static void begin(
        Screen source,
        Map<Integer, PendingPlayer> pendingPlayers,
        Runnable openWorld
    ) {
        reset();
        sourceScreen = source;
        PENDING_PLAYERS.putAll(pendingPlayers);
        active = true;
        try {
            openWorld.run();
        } catch (RuntimeException | Error error) {
            reset();
            throw error;
        }
    }

    public static void tick(Minecraft minecraft) {
        if (!active) {
            return;
        }

        Screen screen = minecraft.gui.screen();
        if (screen instanceof LevelLoadingScreen || minecraft.getSingleplayerServer() != null) {
            loadingObserved = true;
        } else if (!loadingObserved && screen != sourceScreen) {
            leftSourceScreen = true;
        } else if (!loadingObserved && leftSourceScreen && screen == sourceScreen) {

            reset();
            return;
        }

        if (!loadingObserved || !ReadinessPolicy.gameplayReady(0)) {
            return;
        }

        if (!joinNextPlayerInOrder(minecraft)) {
            return;
        }

        ClientRuntime.INSTANCE.input().join().presentJoinedPlayers(minecraft, PENDING_PLAYERS.keySet());
        reset();
    }

    public static boolean shouldHoldLoadingScreen() {
        return active && loadingObserved;
    }

    public static boolean shouldForcePause() {
        return active && loadingObserved;
    }

    public static boolean isActive() {
        return active;
    }


    public static void onMinecraftDisconnect() {
        if (!active || loadingObserved) {
            reset();
        }
    }

    public static void reset() {
        active = false;
        loadingObserved = false;
        leftSourceScreen = false;
        sourceScreen = null;
        PENDING_PLAYERS.clear();
    }

    private static boolean joinNextPlayerInOrder(Minecraft minecraft) {
        ClientRuntime runtime = ClientRuntime.INSTANCE;
        try {
            for (Map.Entry<Integer, PendingPlayer> entry : PENDING_PLAYERS.entrySet()) {
                int slotId = entry.getKey();
                if (runtime.clients().hasClient(slotId)
                    || runtime.clients().sessionOrNull(slotId) != null) {
                    if (!ReadinessPolicy.gameplayReady(slotId)) {
                        return false;
                    }
                    continue;
                }

                PendingPlayer pending = entry.getValue();
                runtime.input().join().joinWithoutPresenting(
                    minecraft,
                    pending.gamepad(),
                    slotId,
                    pending.user()
                );
                return false;
            }
            return true;
        } catch (RuntimeException | Error error) {
            Java_reforged.LOGGER.error("Could not prepare pre-joined local players", error);
            rollbackPreparedPlayers();
            reset();
            return false;
        }
    }

    private static void rollbackPreparedPlayers() {
        ClientRuntime runtime = ClientRuntime.INSTANCE;
        for (Map.Entry<Integer, PendingPlayer> entry : PENDING_PLAYERS.entrySet()) {
            int slotId = entry.getKey();
            if (runtime.clients().hasClient(slotId)
                || runtime.clients().sessionOrNull(slotId) != null) {
                runtime.clients().disconnectSecondary(slotId);
            }
            SlotScreenTransitionHandler.clearSecondaryJoiningScreen(
                Minecraft.getInstance(),
                slotId
            );
            runtime.input().mutableAssignments().unassign(entry.getValue().gamepad());
        }
    }

    public record PendingPlayer(IGamepad gamepad, UserProfile user) {
    }
}
