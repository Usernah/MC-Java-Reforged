package net.jr.mixin.runtime;

import java.util.Arrays;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.renderer.SectionOcclusionGraph$SectionToNodeMap")
public abstract class SectionToNodeMapMixin {
    private static final int JAVA_REFORGED_PAGE_SHIFT = 10;
    private static final int JAVA_REFORGED_PAGE_SIZE = 1 << JAVA_REFORGED_PAGE_SHIFT;
    private static final int JAVA_REFORGED_PAGE_MASK = JAVA_REFORGED_PAGE_SIZE - 1;

    @Unique
    private Object[][] javaReforged$nodePages = new Object[16][];

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static int javaReforged$omitDuplicateIndexArray(int sectionCount) {
        return 0;
    }

    @Inject(method = "put", at = @At("HEAD"), cancellable = true)
    private void javaReforged$putGlobalSection(
        SectionRenderDispatcher.RenderSection section,
        @Coerce Object node,
        CallbackInfo ci
    ) {
        int index = section.index;
        int pageIndex = index >>> JAVA_REFORGED_PAGE_SHIFT;
        if (pageIndex >= this.javaReforged$nodePages.length) {
            int nextLength = this.javaReforged$nodePages.length;
            while (nextLength <= pageIndex) {
                nextLength <<= 1;
            }
            this.javaReforged$nodePages = Arrays.copyOf(this.javaReforged$nodePages, nextLength);
        }
        Object[] page = this.javaReforged$nodePages[pageIndex];
        if (page == null) {
            page = new Object[JAVA_REFORGED_PAGE_SIZE];
            this.javaReforged$nodePages[pageIndex] = page;
        }
        page[index & JAVA_REFORGED_PAGE_MASK] = node;
        ci.cancel();
    }

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void javaReforged$getGlobalSection(
        SectionRenderDispatcher.RenderSection section,
        CallbackInfoReturnable<Object> cir
    ) {
        int index = section.index;
        int pageIndex = index >>> JAVA_REFORGED_PAGE_SHIFT;
        if (pageIndex >= this.javaReforged$nodePages.length) {
            cir.setReturnValue(null);
            return;
        }
        Object[] page = this.javaReforged$nodePages[pageIndex];
        cir.setReturnValue(page == null ? null : page[index & JAVA_REFORGED_PAGE_MASK]);
    }
}
