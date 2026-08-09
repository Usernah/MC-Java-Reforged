package net.alnv.javareforged.mixin.SSM;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LocalPlayer.class)
public interface LocalPlayerSSAccessor {
    @Invoker("sendPosition")
    void splitTest$sendPosition();
}
