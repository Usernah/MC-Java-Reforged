package net.jr.api.client.announcement;

/** A removable subscription to a client announcement stream. */
@FunctionalInterface
public interface AnnouncementSubscription extends AutoCloseable {
    @Override
    void close();
}
