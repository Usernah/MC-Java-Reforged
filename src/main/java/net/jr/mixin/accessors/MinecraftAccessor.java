package net.jr.mixin.accessors;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Invoker("handleKeybinds")
    void javaReforged$handleKeybinds();

    @Invoker("continueAttack")
    void javaReforged$continueAttack(boolean leftClick);

    @Invoker("pick")
    void javaReforged$pick(float partialTicks);
}
