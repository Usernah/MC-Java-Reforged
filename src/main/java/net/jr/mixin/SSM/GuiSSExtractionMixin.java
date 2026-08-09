package net.jr.mixin.SSM;

import java.util.Stack;
import net.jr.ClientRuntime.runtime.HudPass;
import net.jr.ClientRuntime.runtime.Screens;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.ClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Gui.class)
public abstract class GuiSSExtractionMixin {
    @Redirect(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Hud;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"
        )
    )
    private void splitTest$extractSlotHudStates(Hud hud, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (Screens.slotUiPassOwnsScreens()) {
            HudPass.extract(hud, graphics, deltaTracker);
        } else {
            hud.extractRenderState(graphics, deltaTracker);
        }
    }

    @Redirect(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/client/ClientHooks;extractScreen(Lnet/minecraft/client/gui/screens/Screen;Ljava/util/Stack;Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"
        )
    )
    private void splitTest$skipGlobalScreenExtraction(
        Screen screen,
        Stack<Screen> backgroundLayers,
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        if (!Screens.slotUiPassOwnsScreens()) {
            ClientHooks.extractScreen(screen, backgroundLayers, graphics, mouseX, mouseY, partialTick);
        }
    }

    @Redirect(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Hud;extractSavingIndicator(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"
        )
    )
    private void splitTest$skipGlobalSavingIndicator(Hud hud, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!Screens.slotUiPassOwnsScreens()) {
            hud.extractSavingIndicator(graphics, deltaTracker);
        }
    }

    @Redirect(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Hud;extractDebugOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"
        )
    )
    private void splitTest$skipGlobalDebugOverlay(Hud hud, GuiGraphicsExtractor graphics) {
        if (!Screens.slotUiPassOwnsScreens()) {
            hud.extractDebugOverlay(graphics);
        }
    }

    @Redirect(
        method = "extractRenderState",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractDeferredSubtitles()V")
    )
    private void splitTest$skipGlobalDeferredSubtitles(Hud hud) {
        if (!Screens.slotUiPassOwnsScreens()) {
            hud.extractDeferredSubtitles();
        }
    }
}
