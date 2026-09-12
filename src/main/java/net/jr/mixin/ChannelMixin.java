package net.jr.mixin;

import com.mojang.blaze3d.audio.Channel;
import net.jr.client.sound.runtime.StreamingChannelHandler;
import net.jr.client.sound.runtime.StreamingChannelState;
import net.minecraft.client.sounds.AudioStream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Channel.class)
public abstract class ChannelMixin {
    @Unique private final StreamingChannelState javaReforged$streamingState = new StreamingChannelState();

    @Inject(method = "attachBufferStream", at = @At("TAIL"))
    private void javaReforged$prepareStream(AudioStream stream, CallbackInfo ci) {
        StreamingChannelHandler.attached((Channel) (Object) this, this.javaReforged$streamingState);
    }

    @Inject(method = "play", at = @At("HEAD"))
    private void javaReforged$markStreamActive(CallbackInfo ci) {
        StreamingChannelHandler.played((Channel) (Object) this, this.javaReforged$streamingState);
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void javaReforged$markStreamStopped(CallbackInfo ci) {
        StreamingChannelHandler.stopped(this.javaReforged$streamingState);
    }

    @Inject(method = "updateStream", at = @At("HEAD"), cancellable = true)
    private void javaReforged$skipStoppedStreamRefill(CallbackInfo ci) {
        if (StreamingChannelHandler.shouldSkipUpdate((Channel) (Object) this, this.javaReforged$streamingState)) {
            ci.cancel();
        }
    }

    @Inject(method = "updateStream", at = @At("TAIL"))
    private void javaReforged$updateStream(CallbackInfo ci) {
        StreamingChannelHandler.updated((Channel) (Object) this, this.javaReforged$streamingState);
    }
}
