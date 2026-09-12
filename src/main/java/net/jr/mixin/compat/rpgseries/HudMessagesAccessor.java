package net.jr.mixin.compat.rpgseries;

import net.spell_engine.client.gui.HudMessages;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = HudMessages.class, remap = false)
public interface HudMessagesAccessor {
    @Mutable
    @Accessor("INSTANCE")
    static void javaReforged$setInstance(HudMessages instance) {
        throw new AssertionError();
    }
}
