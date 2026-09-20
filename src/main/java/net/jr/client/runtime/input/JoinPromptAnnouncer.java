package net.jr.client.runtime.input;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import net.jr.api.client.announcement.AnnouncementSource;
import net.jr.api.client.split.JoinPromptAnnouncement;
import net.jr.api.client.split.SplitScreen;
import net.jr.client.announcement.AnnouncementPublisher;
import net.jr.client.input.sdl.SdlGamepad;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClientJoinPolicy;
import net.jr.client.runtime.client.LocalClientReadinessPolicy;
import net.jr.experimental_features.pre_join.PreJoinWorldLoad;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

/** Owns join-prompt eligibility and announces only meaningful state changes. */
public final class JoinPromptAnnouncer {
    private static final long DISPLAY_TIME_MS = 3_000L;
    private static final AnnouncementPublisher<JoinPromptAnnouncement> ANNOUNCEMENTS =
        new AnnouncementPublisher<>();
    private static final Map<Long, Boolean> ACTIVE_LATCHED = new HashMap<>();

    private static boolean visible;
    private static long visibleUntilMs;
    private static long announcedDeviceId = -1L;
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
        List<Long> connected = SdlGamepad.deviceIds();
        ACTIVE_LATCHED.keySet().removeIf(deviceId -> !connected.contains(deviceId));

        long movedDeviceId = -1L;
        for (long deviceId : connected) {
            boolean active = SdlGamepad.calibrationInputActive(deviceId);
            boolean wasActive = Boolean.TRUE.equals(ACTIVE_LATCHED.put(deviceId, active));
            if (active && !wasActive && isAssignable(deviceId)) {
                movedDeviceId = deviceId;
            }
        }

        if (!canOfferJoin()) {
            dismiss();
            return;
        }

        if (visible) {
            if (!isAssignable(announcedDeviceId) || Util.getMillis() >= visibleUntilMs) {
                dismiss();
            }
            return;
        }

        if (movedDeviceId >= 0L) {
            show(movedDeviceId);
        }
    }

    public static void dismiss() {
        if (!visible) {
            return;
        }
        visible = false;
        visibleUntilMs = 0L;
        announcedDeviceId = -1L;
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
            && LocalClientJoinPolicy.shouldShowJoinPrompt();
    }

    private static boolean isAssignable(long deviceId) {
        return InputDeviceRouter.connectedGamepads().contains(deviceId)
            && InputDeviceRouter.assignedSlot(deviceId) == null
            && !InputDeviceRouter.isAwaitingNeutral(deviceId)
            && LocalClientJoinPolicy.hasFreeSecondarySlot();
    }

    private static void show(long deviceId) {
        int playerNumber = Math.min(4, ClientRuntime.INSTANCE.viewports().presentedCount() + 1);
        visible = true;
        visibleUntilMs = Util.getMillis() + DISPLAY_TIME_MS;
        announcedDeviceId = deviceId;
        currentAnnouncement = new JoinPromptAnnouncement.Show(deviceId, playerNumber);
        ANNOUNCEMENTS.announce(currentAnnouncement);
    }
}
