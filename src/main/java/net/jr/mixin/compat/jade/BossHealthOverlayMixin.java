package net.jr.mixin.compat.jade;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.jr.client.compat.jade.JadeRuntimeState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = BossHealthOverlay.class, priority = 900)
abstract class BossHealthOverlayMixin {
    @WrapMethod(method = "extractRenderState")
    private void javaReforged$extractCurrentBossBar(
        GuiGraphicsExtractor graphics,
        Operation<Void> original
    ) {
        JadeRuntimeState.run(() -> original.call(graphics));
    }
}
