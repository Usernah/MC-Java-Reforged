package net.jr.mixin.SSM;

import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection")
public interface RenderSectionSSAccessor {
    @Invoker("cancelTasks")
    void splitTest$cancelTasks();

    @Invoker("setSectionMesh")
    SectionMesh splitTest$setSectionMesh(SectionMesh sectionMesh);
}
