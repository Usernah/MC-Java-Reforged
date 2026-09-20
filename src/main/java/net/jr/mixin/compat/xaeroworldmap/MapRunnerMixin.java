package net.jr.mixin.compat.xaeroworldmap;

import net.jr.client.compat.xaero.XaeroWorldMapRuntime;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.MapProcessor;
import xaero.map.MapRunner;

@Mixin(value = MapRunner.class, remap = false)
abstract class MapRunnerMixin {
    @Shadow
    private boolean stopped;

    @Shadow
    public abstract void doTasks(MapProcessor mapProcessor);

    @Inject(method = "run", at = @At("HEAD"), cancellable = true)
    private void javaReforged$runEverySession(CallbackInfo ci) {
        while (
            !this.stopped
                && (Minecraft.getInstance().isRunning() || XaeroWorldMapRuntime.hasSessions())
        ) {
            if (!XaeroWorldMapRuntime.runProcessors((MapRunner)(Object)this)) {
                this.doTasks(null);
            }

            try {
                Thread.sleep(100L);
            } catch (InterruptedException ignored) {
            }
        }
        ci.cancel();
    }
}
