package net.jr.mixin.compat.xaerominimap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.hud.minimap.Minimap;

@Mixin(value = Minimap.class, remap = false)
public interface MinimapAccessor {
    @Accessor("crashedWith")
    Throwable javaReforged$getCrashedWith();

    @Accessor("crashedWith")
    void javaReforged$setCrashedWith(Throwable crashedWith);
}
