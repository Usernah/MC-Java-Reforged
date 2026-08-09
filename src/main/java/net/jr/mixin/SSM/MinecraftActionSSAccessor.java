package net.jr.mixin.SSM;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftActionSSAccessor {
    @Invoker("handleKeybinds")
    void splitTest$handleKeybinds();

    @Invoker("continueAttack")
    void splitTest$continueAttack(boolean leftClick);

    @Invoker("pick")
    void splitTest$pick(float partialTicks);
}
