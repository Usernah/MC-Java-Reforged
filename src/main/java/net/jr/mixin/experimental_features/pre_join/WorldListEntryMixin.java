package net.jr.mixin.experimental_features.pre_join;

import net.jr.client.runtime.input.InputActionHandler;
import net.jr.experimental_features.pre_join.PreJoinWorldEntry;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldSelectionList.WorldListEntry.class)
public abstract class WorldListEntryMixin {
    @Inject(method = "joinWorld", at = @At("HEAD"), cancellable = true)
    private void javaReforged$openPreJoinOverlay(CallbackInfo callback) {
        WorldSelectionList.WorldListEntry entry = (WorldSelectionList.WorldListEntry)(Object)this;
        if (PreJoinWorldEntry.intercept(entry::joinWorld)) {
            callback.cancel();
        }
    }

    @ModifyVariable(
        method = "mouseClicked",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private boolean javaReforged$controllerActivationIsDirect(boolean doubleClick) {
        return doubleClick || InputActionHandler.isDispatchingControllerPointerClick();
    }
}
