package net.jr.mixin;

import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.sounds.AudioStream;
import org.jspecify.annotations.Nullable;
import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Channel.class)
public abstract class MixinChannel {
    @Shadow @Final private int source;
    @Shadow private @Nullable AudioStream stream;
    @Shadow private int streamingBufferSize;

    @Shadow
    public abstract void play();

    @Shadow
    public abstract boolean stopped();

    @Shadow
    private void pumpBuffers(int readCount) {
        throw new AssertionError();
    }

    @Unique private boolean jf$isBoosted = false;
    @Unique private int jf$buffersToFill = 0;
    @Unique private boolean jf$allowRecovery = false;
    @Unique private int jf$recoveryTicks = 0;

    @Inject(method = "attachBufferStream", at = @At("TAIL"))
    private void jf$prepareGradualBoost(AudioStream stream, CallbackInfo ci) {
        if (!this.jf$isBoosted) {
            this.streamingBufferSize = Math.max(this.streamingBufferSize * 8, 262144);
            this.jf$isBoosted = true;
        }

        this.jf$buffersToFill = 48;
        this.jf$allowRecovery = true;
        this.jf$recoveryTicks = 20;
    }

    @Inject(method = "play", at = @At("HEAD"))
    private void jf$markActive(CallbackInfo ci) {
        if (this.stream != null) {
            this.jf$allowRecovery = true;
            this.jf$recoveryTicks = 20;
        }
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void jf$markStopped(CallbackInfo ci) {
        this.jf$allowRecovery = false;
        this.jf$buffersToFill = 0;
        this.jf$recoveryTicks = 0;
    }

    @Inject(method = "updateStream", at = @At("TAIL"))
    private void jf$progressivePump(CallbackInfo ci) {
        if (this.stream == null) {
            return;
        }

        if (this.jf$buffersToFill > 0) {
            try {
                int batch = Math.min(this.jf$buffersToFill, 2);
                this.pumpBuffers(batch);
                this.jf$buffersToFill -= batch;
            } catch (Exception ignored) {
            }
        }

        if (!this.stopped()) {
            this.jf$recoveryTicks = 20;
            return;
        }

        if (!this.jf$allowRecovery || this.jf$recoveryTicks-- <= 0) {
            return;
        }

        try {
            int queuedBefore = AL10.alGetSourcei(this.source, AL10.AL_BUFFERS_QUEUED);
            if (queuedBefore == 0) {
                this.pumpBuffers(2);
            }

            if (AL10.alGetSourcei(this.source, AL10.AL_BUFFERS_QUEUED) > 0) {
                this.play();
            }
        } catch (Exception ignored) {
        }
    }
}
