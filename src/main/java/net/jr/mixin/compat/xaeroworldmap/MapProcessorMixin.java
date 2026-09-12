package net.jr.mixin.compat.xaeroworldmap;

import java.io.File;
import net.jr.client.compat.xaero.XaeroWorldMapPaths;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.MapProcessor;
import xaero.map.MapRunner;
import xaero.map.WorldMap;

@Mixin(value = MapProcessor.class, remap = false)
abstract class MapProcessorMixin {
    @Redirect(
        method = "fixRootFolder(Ljava/lang/String;Ljava/lang/String;)V",
        at = @At(
            value = "FIELD",
            target = "Lxaero/map/WorldMap;saveFolder:Ljava/io/File;"
        )
    )
    private File javaReforged$currentSaveRoot() {
        return XaeroWorldMapPaths.currentRoot(WorldMap.saveFolder);
    }

    @Inject(
        method = "run",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Thread;sleep(J)V"
        ),
        cancellable = true
    )
    private void javaReforged$yieldSharedEngine(MapRunner runner, CallbackInfo ci) {
        if (!((MapProcessor)(Object)this).isFinalizing()) {
            ci.cancel();
        }
    }
}
