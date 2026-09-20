package net.jr.api.client.split;

import net.jr.api.client.announcement.AnnouncementSource;
import net.jr.client.runtime.input.JoinPromptAnnouncer;

/** Public read-only access to join-prompt announcements. */
public final class JoinPromptAnnouncements {
    private JoinPromptAnnouncements() {
    }

    public static AnnouncementSource<JoinPromptAnnouncement> source() {
        return JoinPromptAnnouncer.announcements();
    }
}
