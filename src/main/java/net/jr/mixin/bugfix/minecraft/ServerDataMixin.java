package net.jr.mixin.bugfix.minecraft;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Restores the non-null presentation-state invariant of {@link ServerData} in Minecraft 26.2.
 *
 * <p>The vanilla constructor leaves {@code motd} and {@code status} null. The multiplayer screen
 * asks an online-server entry for narration from {@code Screen.init()} before its first render;
 * that first render is where vanilla normally assigns empty components and starts the ping. A
 * saved server can therefore pass a null MOTD to {@code Component.translatable(...)} and crash
 * while the screen is opening.</p>
 *
 * <p>Keep this fix local to {@code ServerData}: suppressing or delaying narration would alter
 * every screen and would only hide the broken data invariant. Remove this mixin once the supported
 * Minecraft version initializes both fields itself.</p>
 */
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
