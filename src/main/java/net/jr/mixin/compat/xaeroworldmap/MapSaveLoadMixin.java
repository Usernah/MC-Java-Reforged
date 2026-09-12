package net.jr.mixin.compat.xaeroworldmap;

import java.io.File;
import net.jr.client.compat.xaero.XaeroWorldMapPaths;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.map.WorldMap;
import xaero.map.file.MapSaveLoad;

@Mixin(value = MapSaveLoad.class, remap = false)
abstract class MapSaveLoadMixin {
    @Redirect(
        method = {"getRootFolder", "getMainFolder", "getOldFolder"},
        at = @At(
            value = "FIELD",
            target = "Lxaero/map/WorldMap;saveFolder:Ljava/io/File;"
        )
    )
    private static File javaReforged$currentSaveRoot() {
        return XaeroWorldMapPaths.currentRoot(WorldMap.saveFolder);
    }
}
