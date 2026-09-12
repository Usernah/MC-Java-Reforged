package net.jr.mixin.compat.jade;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import snownee.jade.impl.ui.BoxElementImpl;
import snownee.jade.overlay.OverlayRenderer;

@Mixin(value = OverlayRenderer.class, remap = false)
public interface OverlayRendererAccessor {
    @Accessor("lingerTooltip")
    static BoxElementImpl javaReforged$getLingerTooltip() {
        throw new AssertionError();
    }

    @Accessor("lingerTooltip")
    static void javaReforged$setLingerTooltip(BoxElementImpl value) {
        throw new AssertionError();
    }

    @Accessor("disappearTicks")
    static float javaReforged$getDisappearTicks() {
        throw new AssertionError();
    }

    @Accessor("disappearTicks")
    static void javaReforged$setDisappearTicks(float value) {
        throw new AssertionError();
    }
}
