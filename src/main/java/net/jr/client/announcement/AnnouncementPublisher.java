package net.jr.client.announcement;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.jr.api.client.announcement.AnnouncementSource;
import net.jr.api.client.announcement.AnnouncementSubscription;


public final class AnnouncementPublisher<T> {
    private final CopyOnWriteArrayList<Consumer<? super T>> listeners =
        new CopyOnWriteArrayList<>();
    private final AnnouncementSource<T> source = this::subscribe;

    public AnnouncementSource<T> source() {
        return this.source;
    }

    public void announce(T announcement) {
        T value = Objects.requireNonNull(announcement, "announcement");
        for (Consumer<? super T> listener : this.listeners) {
            listener.accept(value);
        }
    }

    private AnnouncementSubscription subscribe(Consumer<? super T> listener) {
        Consumer<? super T> subscriber = Objects.requireNonNull(listener, "listener");
        this.listeners.add(subscriber);
        AtomicBoolean subscribed = new AtomicBoolean(true);
        return () -> {
            if (subscribed.compareAndSet(true, false)) {
                this.listeners.remove(subscriber);
            }
        };
    }
}
