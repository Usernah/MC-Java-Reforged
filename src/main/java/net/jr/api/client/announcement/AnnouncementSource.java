package net.jr.api.client.announcement;

import java.util.function.Consumer;

/** Read-only side of a client announcement stream. */
@FunctionalInterface
public interface AnnouncementSource<T> {
    AnnouncementSubscription subscribe(Consumer<? super T> listener);
}
