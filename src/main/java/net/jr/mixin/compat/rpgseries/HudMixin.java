package net.jr.mixin.compat.rpgseries;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.jr.client.compat.rpgseries.SpellEngineRuntimeState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = Hud.class, priority = 900)
abstract class HudMixin {
    @WrapMethod(method = "extractItemHotbar")
    private void javaReforged$extractCurrentSpellHotbar(
        GuiGraphicsExtractor graphics,
        DeltaTracker deltaTracker,
        Operation<Void> original
    ) {
        try (SpellEngineRuntimeState.Scope ignored = SpellEngineRuntimeState.activateCurrent()) {
            original.call(graphics, deltaTracker);
        }
    }
}
