package net.jr.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.jr.client.sound.music.WorldMusicTransitionHandler;
import net.jr.client.sound.music.WorldMusicTransitionState;
import net.jr.screens.inventory.CreativeInventoryHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow @Final private MusicManager musicManager;
    @Shadow @Final private SoundManager soundManager;
    @Unique private final WorldMusicTransitionState javaReforged$musicTransitionState = new WorldMusicTransitionState();

    @Inject(method = "doWorldLoad", at = @At("HEAD"))
    private void javaReforged$beginWorldLoad(CallbackInfo ci) {
        WorldMusicTransitionHandler.worldLoadStarted(this.musicManager, this.javaReforged$musicTransitionState);
    }

    @Inject(method = "setLevel", at = @At("TAIL"))
    private void javaReforged$finishWorldLoad(ClientLevel level, CallbackInfo ci) {
        WorldMusicTransitionHandler.levelSet((Minecraft) (Object) this, this.musicManager, this.javaReforged$musicTransitionState);
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("HEAD"))
    private void javaReforged$beginDisconnect(Screen next, boolean keepPacks, boolean stopSound, CallbackInfo ci) {
        WorldMusicTransitionHandler.disconnectStarted((Minecraft) (Object) this, this.musicManager,
                this.soundManager, this.javaReforged$musicTransitionState);
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("TAIL"))
    private void javaReforged$finishDisconnect(Screen next, boolean keepPacks, boolean stopSound, CallbackInfo ci) {
        WorldMusicTransitionHandler.disconnectFinished(this.musicManager, this.javaReforged$musicTransitionState);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void javaReforged$syncWorldMusic(CallbackInfo ci) {
        WorldMusicTransitionHandler.tick((Minecraft) (Object) this, this.musicManager, this.javaReforged$musicTransitionState);
    }

    @WrapOperation(method = "handleKeybinds", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z"))
    private boolean javaReforged$openCreativeInventory(KeyMapping mapping, Operation<Boolean> original) {
        Minecraft minecraft = (Minecraft) (Object) this;
        if (!CreativeInventoryHandler.replacesVanilla(minecraft, mapping)) return original.call(mapping);
        CreativeInventoryHandler.openIfRequested(minecraft, mapping);
        return false;
    }

    @Redirect(method = "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;stop()V"))
    private void javaReforged$guardLevelTransitionStop(SoundManager soundManager) {
        if (WorldMusicTransitionHandler.allowForcedLevelStop(this.javaReforged$musicTransitionState)) soundManager.stop();
    }

    @Redirect(method = "doWorldLoad", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;disconnectWithProgressScreen()V"))
    private void javaReforged$preserveWorldLoadMusic(Minecraft minecraft) {
        WorldMusicTransitionHandler.runInternalDisconnect(
                this.javaReforged$musicTransitionState,
                minecraft::disconnectWithProgressScreen
        );
    }
}
