package net.alnv.javareforged.mixin.SSM;

import java.util.Collection;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection")
public interface RenderSectionSSAccessor {
    @Invoker("cancelTasks")
    boolean splitTest$cancelTasks();

    @Invoker("updateGlobalBlockEntities")
    void splitTest$updateGlobalBlockEntities(Collection<BlockEntity> blockEntities);

    @Invoker("setCompiled")
    void splitTest$setCompiled(SectionRenderDispatcher.CompiledSection compiledSection);
}
