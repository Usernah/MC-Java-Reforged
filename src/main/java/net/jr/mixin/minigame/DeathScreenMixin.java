package net.jr.mixin.minigame;

import net.jr.client.runtime.context.LocalClientResolver;
import net.jr.experimental_features.minigame.battle.BattlePhase;
import net.jr.experimental_features.minigame.client.BattleClientState;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(DeathScreen.class)
public class DeathScreenMixin extends Screen {

    @Shadow private int delayTicker;

    @Shadow
    @Final
    private Component deathScore;

    protected DeathScreenMixin(Component title) {
        super(title);
    }

    @Unique
    private boolean javaReforged$respawnRequested;

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void onInit(CallbackInfo ci) {
        if (BattleClientState.isBattle()) {
            this.delayTicker = 0;
            ci.cancel();
        }
    }
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (!BattleClientState.isBattle() || this.javaReforged$respawnRequested) {
            return;
        }

        BattlePhase phase = BattleClientState.phase();
        boolean winnerSequence =
                phase == BattlePhase.WINNER_ANOUNCEMENT
                        || phase == BattlePhase.FINISH;

        if (this.delayTicker < 40 && !winnerSequence) {
            return;
        }

        LocalPlayer player = LocalClientResolver.requireActive().player();
        if (player != null) {
            this.javaReforged$respawnRequested = true;
            player.respawn();
        }
    }

    @Redirect(
            method = "visitText",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/ActiveTextCollector;accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/network/chat/Component;)V"))
    private void redirectDeathScore(ActiveTextCollector output, TextAlignment alignment, int x, int y, Component text) {
        if (text == this.deathScore && BattleClientState.isBattle()) {
            return;
        }

        output.accept(alignment, x, y, text);
    }
}
