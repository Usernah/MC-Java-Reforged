package net.jr.api.client.announcement;


@FunctionalInterface
public interface AnnouncementSubscription extends AutoCloseable {
    @Override
    void close();
}
