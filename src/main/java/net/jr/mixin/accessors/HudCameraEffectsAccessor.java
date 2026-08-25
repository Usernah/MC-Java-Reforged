package net.jr.mixin.accessors;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Hud.class)
public interface HudCameraEffectsAccessor {
    @Invoker("extractCameraOverlays")
    void javaReforged$extractCameraEffects(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker);
}
