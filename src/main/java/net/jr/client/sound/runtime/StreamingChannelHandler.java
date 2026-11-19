package net.jr.client.sound.runtime;

import com.mojang.blaze3d.audio.Channel;
import net.jr.mixin.accessors.ChannelAccessor;
import org.lwjgl.openal.AL10;

public final class StreamingChannelHandler {
    private StreamingChannelHandler() {
    }

    public static void attached(Channel channel, StreamingChannelState state) {
        ChannelAccessor accessor = (ChannelAccessor) channel;
        if (!state.boosted()) {
            accessor.javaReforged$setStreamingBufferSize(
                    Math.max(accessor.javaReforged$getStreamingBufferSize() * 8, 262144)
            );
            state.markBoosted();
        }
        state.setBuffersToFill(48);
        state.setRecoveryAllowed(true);
        state.setRecoveryTicks(20);
    }

    public static void played(Channel channel, StreamingChannelState state) {
        if (((ChannelAccessor) channel).javaReforged$getStream() != null) {
            state.setRecoveryAllowed(true);
            state.setRecoveryTicks(20);
        }
    }

    public static void stopped(StreamingChannelState state) {
        state.setRecoveryAllowed(false);
        state.setBuffersToFill(0);
        state.setRecoveryTicks(0);
    }

    public static boolean shouldSkipUpdate(Channel channel, StreamingChannelState state) {


        return !state.recoveryAllowed() && ((ChannelAccessor) channel).javaReforged$getStream() != null;
    }

    public static void updated(Channel channel, StreamingChannelState state) {
        ChannelAccessor accessor = (ChannelAccessor) channel;
        if (accessor.javaReforged$getStream() == null || !state.recoveryAllowed()) return;
        if (state.buffersToFill() > 0) {
            try {
                int batch = Math.min(state.buffersToFill(), 2);
                accessor.javaReforged$pumpBuffers(batch);
                state.setBuffersToFill(state.buffersToFill() - batch);
            } catch (Exception ignored) {
            }
        }
        if (!channel.stopped()) {
            state.setRecoveryTicks(20);
            return;
        }
        if (!state.recoveryAllowed() || state.consumeRecoveryTick() <= 0) return;
        try {
            if (AL10.alGetSourcei(accessor.javaReforged$getSource(), AL10.AL_BUFFERS_QUEUED) == 0) {
                accessor.javaReforged$pumpBuffers(2);
            }
            if (AL10.alGetSourcei(accessor.javaReforged$getSource(), AL10.AL_BUFFERS_QUEUED) > 0) {
                channel.play();
            }
        } catch (Exception ignored) {
        }
    }
}
