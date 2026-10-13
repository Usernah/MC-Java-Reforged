package net.jr.mixin;

import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class ServerPlayerDeathMixin {
    @Shadow
    public int deathTime;

    @Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true)
    private void javaReforged$retainDeadServerPlayerAsObserver(CallbackInfo ci) {
        if (!((Object)this instanceof ServerPlayer player)) {
            return;
        }

        this.deathTime++;
        if (this.deathTime == 20 && !player.isRemoved()) {
            player.level().broadcastEntityEvent(player, (byte)60);
            player.level()
                .getChunkSource()
                .sendToTrackingPlayers(player, new ClientboundRemoveEntitiesPacket(player.getId()));
        }

        // PlayerList.respawn (or disconnect) performs the real removal. Keeping the
        // dead ServerPlayer tracked until then preserves its chunk/entity observer.
        ci.cancel();
    }
}
