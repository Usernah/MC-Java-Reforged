package net.jr.mixin.accessors;

import net.minecraft.TracingExecutor;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.chunk.SectionTaskDynamicQueue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SectionRenderDispatcher.class)
public interface SectionRenderDispatcherAccessor {
    @Accessor("queue")
    SectionTaskDynamicQueue javaReforged$getQueue();

    @Accessor("executor")
    TracingExecutor javaReforged$getExecutor();

    @Invoker("runTask")
    void javaReforged$runTask();
}
