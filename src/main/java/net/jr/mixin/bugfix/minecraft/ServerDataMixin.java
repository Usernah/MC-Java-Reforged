package net.jr.mixin.bugfix.minecraft;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ServerData.class)
public abstract class ServerDataMixin {
    @Shadow public Component status;
    @Shadow public Component motd;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void javaReforged$initializePresentationState(
            String name,
            String ip,
            ServerData.Type type,
            CallbackInfo callback
    ) {
        if (this.status == null) {
            this.status = CommonComponents.EMPTY;
        }
        if (this.motd == null) {
            this.motd = CommonComponents.EMPTY;
        }
    }
}
