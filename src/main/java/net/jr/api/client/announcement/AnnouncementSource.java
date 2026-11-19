package net.jr.api.client.announcement;

import java.util.function.Consumer;


@FunctionalInterface
public interface AnnouncementSource<T> {
    AnnouncementSubscription subscribe(Consumer<? super T> listener);
}
