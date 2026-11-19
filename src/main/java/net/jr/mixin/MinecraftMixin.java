package net.jr.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.jr.api.client.local.Client;
import net.jr.api.client.split.SplitScreen;
import net.jr.client.sound.music.WorldMusicTransitionHandler;
import net.jr.client.sound.music.WorldMusicTransitionState;
import net.jr.client.runtime.audio.SlotSoundRouter;
import net.jr.client.runtime.render.world.WorldRenderBackends;
import net.jr.client.runtime.session.DisconnectHandler;
import net.jr.client.ui.LoadingTextProvider;
import net.jr.screens.MessageScreen;
import net.jr.screens.TransitionScreen;
import net.jr.screens.inventory.CreativeInventoryHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow @Final private MusicManager musicManager;
    @Shadow @Final private SoundManager soundManager;
    @Unique private final WorldMusicTransitionState javaReforged$musicTransitionState = new WorldMusicTransitionState();

    @ModifyReturnValue(method = "createTitle", at = @At("RETURN"))
    private String javaReforged$showSplitScreenInWindowTitle(String original) {
        if (!SplitScreen.isActive()) {
            return original;
        }
        int separator = original.lastIndexOf(" - ");
        String prefix = separator >= 0 ? original.substring(0, separator) : original;
        return prefix + " - " + I18n.get(
            "title.java_reforged.split_screen",
            Client.count(),
            Client.capacity()
        );
    }

    @WrapOperation(
        method = "lambda$reloadResourcePacks$2",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/extract/LevelExtractor;allChanged()V"
        )
    )
    private void javaReforged$invalidateVirtualTerrainAfterResourceReload(
        LevelExtractor extractor,
        Operation<Void> original
    ) {
        WorldRenderBackends.resourcesReloaded();
        original.call(extractor);
    }

    @Inject(method = "doWorldLoad", at = @At("HEAD"))
    private void javaReforged$beginWorldLoad(CallbackInfo ci) {
        WorldMusicTransitionHandler.worldLoadStarted(this.musicManager, this.javaReforged$musicTransitionState);
    }

    @Inject(method = "setLevel", at = @At("TAIL"))
    private void javaReforged$finishWorldLoad(ClientLevel level, CallbackInfo ci) {
        WorldMusicTransitionHandler.levelSet((Minecraft) (Object) this, this.musicManager, this.javaReforged$musicTransitionState);
    }

    @Inject(
        method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void javaReforged$beginDisconnect(Screen next, boolean keepPacks, boolean stopSound, CallbackInfo ci) {
        Minecraft minecraft = (Minecraft)(Object)this;
        if (DisconnectHandler.disconnectCurrentSecondary(minecraft)) {
            ci.cancel();
            return;
        }
        WorldMusicTransitionHandler.disconnectStarted((Minecraft) (Object) this, this.musicManager,
                this.soundManager, this.javaReforged$musicTransitionState);
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("TAIL"))
    private void javaReforged$finishDisconnect(Screen next, boolean keepPacks, boolean stopSound, CallbackInfo ci) {
        WorldMusicTransitionHandler.disconnectFinished(this.musicManager, this.javaReforged$musicTransitionState);
    }

    @ModifyVariable(
            method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private Screen javaReforged$replaceDisconnectMessage(Screen screen) {
        return javaReforged$replaceGenericMessage(screen);
    }

    @ModifyArg(
            method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Gui;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"
            ),
            index = 0
    )
    private Screen javaReforged$replaceSavingMessage(Screen screen) {
        return javaReforged$replaceGenericMessage(screen);
    }

    @Unique
    private static Screen javaReforged$replaceGenericMessage(Screen screen) {
        if (screen != null && screen.getClass() == GenericMessageScreen.class) {
            return new MessageScreen(screen.getTitle());
        }
        return screen;
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
        if (WorldMusicTransitionHandler.allowForcedLevelStop(this.javaReforged$musicTransitionState)) {
            soundManager.stop();
        } else {
            SlotSoundRouter.stopAllEffects(soundManager);
        }
    }

    @Redirect(method = "doWorldLoad", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;disconnectWithProgressScreen()V"))
    private void javaReforged$preserveWorldLoadMusic(Minecraft minecraft) {
        Screen currentScreen = minecraft.gui.screen();
        MessageScreen messageScreen = currentScreen instanceof MessageScreen currentMessage
                ? currentMessage
                : new MessageScreen(Component.translatable("selectWorld.resource_load"));
        WorldMusicTransitionHandler.runInternalDisconnect(
                this.javaReforged$musicTransitionState,
                () -> minecraft.disconnect(messageScreen, false, true)
        );
    }

    @WrapOperation(
            method = "doWorldLoad",
            at = @At(
                    value = "NEW",
                    target = "Lnet/minecraft/client/gui/screens/LevelLoadingScreen;"
            )
    )
    private LevelLoadingScreen javaReforged$createTransitionScreen(
            LevelLoadTracker loadTracker,
            LevelLoadingScreen.Reason reason,
            Operation<LevelLoadingScreen> original
    ) {
        Minecraft minecraft = (Minecraft)(Object)this;
        if (minecraft.gui.screen() instanceof MessageScreen messageScreen) {
            return new TransitionScreen(
                    loadTracker,
                    reason,
                    messageScreen.currentTip(),
                    messageScreen.currentTipElapsedTicks()
            );
        }

        LoadingTextProvider loadingTexts = new LoadingTextProvider(40);
        return new TransitionScreen(
                loadTracker,
                reason,
                loadingTexts.current(),
                loadingTexts.elapsedTicks()
        );
    }

    @WrapOperation(
            method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;setScreenAndShow(Lnet/minecraft/client/gui/screens/Screen;)V"
            )
    )
    private void javaReforged$keepActiveWorldLoadMessage(
            Minecraft minecraft,
            Screen nextScreen,
            Operation<Void> original
    ) {
        if (nextScreen instanceof MessageScreen && minecraft.gui.screen() == nextScreen) {
            minecraft.renderFrame(false);
            return;
        }
        original.call(minecraft, nextScreen);
    }
}
