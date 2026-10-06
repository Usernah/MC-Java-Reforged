package net.jr.mixin;

import net.jr.api.client.render.Draw;
import net.jr.api.client.render.font.FontHolder;
import net.jr.api.client.render.font.FontRegister;
import net.jr.client.components.elements.TextElement;
import net.jr.experimental_features.minigame.client.BattleClientState;
import net.jr.registry.ModFonts;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public class DeathScreenMixin extends Screen {


    protected DeathScreenMixin(Component title) {
        super(title);
    }

    @Inject(
            method = "extractRenderState",
            at = @At(value = "TAIL")
    )
    private void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        TextElement titleText = new TextElement(this.title, this.width / 2, 60, graphics);
        titleText.setOutline(0xFF0f0f0f);
        titleText.setShadow(true);
        titleText.setAlign(Draw.TextAlign.CENTER);
        titleText.setScale(2.0F);
        titleText.draw();
    }
}
