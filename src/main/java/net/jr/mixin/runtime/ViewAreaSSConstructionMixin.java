package net.jr.mixin.runtime;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.jr.client.runtime.terrain.SharedViewAreaConstruction;
import net.minecraft.client.RotatingSectionStorage;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ViewArea.class)
public abstract class ViewAreaSSConstructionMixin {
    @WrapOperation(
        method = "<init>",
        at = @At(value = "NEW", target = "(IIILnet/minecraft/client/RotatingSectionStorage$ValueCreator;)Lnet/minecraft/client/RotatingSectionStorage;")
    )
    private RotatingSectionStorage<SectionRenderDispatcher.RenderSection> splitTest$omitDuplicateRenderSections(
        int radius,
        int minY,
        int maxY,
        RotatingSectionStorage.ValueCreator<SectionRenderDispatcher.RenderSection> valueCreator,
        Operation<RotatingSectionStorage<SectionRenderDispatcher.RenderSection>> original
    ) {
        if (!SharedViewAreaConstruction.active()) {
            return original.call(radius, minY, maxY, valueCreator);
        }
        RotatingSectionStorage.ValueCreator<SectionRenderDispatcher.RenderSection> emptyCreator =
            (index, sectionNode) -> null;
        return original.call(radius, minY, maxY, emptyCreator);
    }
}
