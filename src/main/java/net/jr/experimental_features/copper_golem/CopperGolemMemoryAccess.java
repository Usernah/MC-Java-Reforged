package net.jr.experimental_features.copper_golem;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

public interface CopperGolemMemoryAccess {

    CopperGolemContainerMemory javaReforged$getContainerMemory();

    @Nullable
    GlobalPos javaReforged$getTransportSource();

    void javaReforged$setTransportSource(
            @Nullable GlobalPos source
    );

    boolean javaReforged$isReturningToSource();

    void javaReforged$setReturningToSource(
            boolean returning
    );

    @Nullable
    Item javaReforged$getLastRejectedItem();

    void javaReforged$setLastRejectedItem(
            @Nullable Item item
    );

    long javaReforged$getRetryAfterGameTime();

    void javaReforged$setRetryAfterGameTime(long gameTime);
}
