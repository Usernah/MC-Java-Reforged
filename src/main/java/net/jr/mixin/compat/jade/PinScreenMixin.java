package net.jr.mixin.compat.jade;

import net.jr.client.compat.jade.JadeRuntimeState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import snownee.jade.api.ui.TooltipAnimation;
import snownee.jade.gui.PinScreen;

@Mixin(value = PinScreen.class, remap = false)
abstract class PinScreenMixin {
    @Redirect(
        method = {
            "mouseClicked",
            "mouseReleased",
            "mouseScrolled",
            "mouseDragged",
            "mouseMoved",
            "keyPressed"
        },
        at = @At(
            value = "FIELD",
            target = "Lsnownee/jade/overlay/OverlayRenderer;animation:Lsnownee/jade/api/ui/TooltipAnimation;",
            opcode = Opcodes.GETSTATIC
        )
    )
    private TooltipAnimation javaReforged$currentAnimation() {
        return JadeRuntimeState.animation();
    }
}
