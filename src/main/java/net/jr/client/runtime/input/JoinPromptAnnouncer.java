package net.jr.client.runtime.input;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import net.jr.api.client.announcement.AnnouncementSource;
import net.jr.api.client.input.IGamepad;
import net.jr.api.client.split.JoinPromptAnnouncement;
import net.jr.api.client.split.SplitScreen;
import net.jr.client.announcement.AnnouncementPublisher;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClientReadinessPolicy;
import net.jr.experimental_features.pre_join.PreJoinWorldLoad;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

/** Owns join-prompt eligibility and announces only meaningful state changes. */
public final class JoinPromptAnnouncer {
    private static final long DISPLAY_TIME_MS = 3_000L;
    private static final AnnouncementPublisher<JoinPromptAnnouncement> ANNOUNCEMENTS =
        new AnnouncementPublisher<>();
    private static final Map<IGamepad, Boolean> ACTIVE_LATCHED = new HashMap<>();

    private static boolean visible;
    private static long visibleUntilMs;
    @Nullable
    private static IGamepad announcedGamepad;
    @Nullable
    private static JoinPromptAnnouncement.Show currentAnnouncement;

    private JoinPromptAnnouncer() {
    }

    public static AnnouncementSource<JoinPromptAnnouncement> announcements() {
        return listener -> {
            var subscriber = Objects.requireNonNull(listener, "listener");
            var subscription = ANNOUNCEMENTS.source().subscribe(subscriber);
            JoinPromptAnnouncement.Show current = currentAnnouncement;
            if (current != null) {
                subscriber.accept(current);
            }
            return subscription;
        };
    }

    public static void tick(Minecraft minecraft) {
        List<IGamepad> connected = ClientRuntime.INSTANCE.input().devices().gamepads();
        ACTIVE_LATCHED.keySet().removeIf(gamepad -> !connected.contains(gamepad));

        IGamepad movedGamepad = null;
        for (IGamepad gamepad : connected) {
            boolean active = gamepad.input().calibrationInputActive();
            boolean wasActive = Boolean.TRUE.equals(ACTIVE_LATCHED.put(gamepad, active));
            if (active && !wasActive && isAssignable(gamepad)) {
                movedGamepad = gamepad;
            }
        }

        if (!canOfferJoin()) {
            dismiss();
            return;
        }

        if (visible) {
            if (!isAssignable(announcedGamepad) || Util.getMillis() >= visibleUntilMs) {
                dismiss();
            }
            return;
        }

        if (movedGamepad != null) {
            show(movedGamepad);
        }
    }

    public static void dismiss() {
        if (!visible) {
            return;
        }
        visible = false;
        visibleUntilMs = 0L;
        announcedGamepad = null;
        currentAnnouncement = null;
        ANNOUNCEMENTS.announce(new JoinPromptAnnouncement.Hide());
    }

    public static void reset() {
        ACTIVE_LATCHED.clear();
        dismiss();
    }

    private static boolean canOfferJoin() {
        return !PreJoinWorldLoad.isActive()
            && LocalClientReadinessPolicy.gameplayReady(0)
            && SplitScreen.hasRequiredControllerCountForJoin()
            && ClientRuntime.INSTANCE.input().join().shouldShowJoinPrompt();
    }

    private static boolean isAssignable(@Nullable IGamepad gamepad) {
        return gamepad != null
            && ClientRuntime.INSTANCE.input().devices().gamepads().contains(gamepad)
            && ClientRuntime.INSTANCE.input().assignments().assignedSlot(gamepad) == null
            && !ClientRuntime.INSTANCE.input().assignments().awaitingNeutral(gamepad)
            && ClientRuntime.INSTANCE.input().join().hasFreeSecondarySlot();
    }

    private static void show(IGamepad gamepad) {
        int playerNumber = Math.min(4, ClientRuntime.INSTANCE.viewports().presentedCount() + 1);
        visible = true;
        visibleUntilMs = Util.getMillis() + DISPLAY_TIME_MS;
        announcedGamepad = gamepad;
        currentAnnouncement = new JoinPromptAnnouncement.Show(gamepad, playerNumber);
        ANNOUNCEMENTS.announce(currentAnnouncement);
    }
}
