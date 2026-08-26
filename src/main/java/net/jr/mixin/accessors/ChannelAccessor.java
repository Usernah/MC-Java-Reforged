package net.jr.mixin.accessors;

import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.sounds.AudioStream;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Channel.class)
public interface ChannelAccessor {
    @Accessor("source")
    int javaReforged$getSource();

    @Accessor("stream")
    @Nullable AudioStream javaReforged$getStream();

    @Accessor("streamingBufferSize")
    int javaReforged$getStreamingBufferSize();

    @Accessor("streamingBufferSize")
    void javaReforged$setStreamingBufferSize(int size);

    @Invoker("pumpBuffers")
    void javaReforged$pumpBuffers(int readCount);
}
