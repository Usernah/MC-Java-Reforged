package net.alnv.javareforged.mixin.SSM;

import java.util.Queue;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SectionRenderDispatcher.class)
public interface SectionRenderDispatcherAccessor {
    @Accessor("toUpload")
    Queue<Runnable> splitTest$getPendingUploads();
}
