package net.jr.mixin.runtime;

import javax.annotation.Nullable;
import net.jr.client.runtime.viewport.PreciseGuiScissor;
import net.jr.client.runtime.viewport.PreciseScissorAccess;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ScreenRectangle.class)
public abstract class ScreenRectangleMixin
        implements PreciseScissorAccess {

    @Unique
    @Nullable
    private PreciseGuiScissor javaReforged$preciseScissor;

    @Override
    @Nullable
    public PreciseGuiScissor javaReforged$preciseScissor() {
        return this.javaReforged$preciseScissor;
    }

    @Override
    public void javaReforged$setPreciseScissor(
            @Nullable PreciseGuiScissor scissor
    ) {
        this.javaReforged$preciseScissor = scissor;
    }
}
