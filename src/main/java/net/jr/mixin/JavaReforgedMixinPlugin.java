package net.jr.mixin;

import java.util.List;
import java.util.Set;
import net.jr.bootstrap.OptionalModPresence;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class JavaReforgedMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaReforgedMixinPlugin.class);
    private static final Set<String> NATIVE_TERRAIN_MIXINS = Set.of(
        "net.jr.mixin.runtime.NativeTerrainLevelExtractorMixin",
        "net.jr.mixin.runtime.NativeTerrainLevelRendererMixin",
        "net.jr.mixin.runtime.ChunkSectionsToRenderMixin",
        "net.jr.mixin.runtime.RenderSectionBufferGenerationMixin",
        "net.jr.mixin.runtime.SectionToNodeMapMixin",
        "net.jr.mixin.runtime.ViewAreaMixin"
    );

    private boolean sodiumLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        this.sodiumLoaded = OptionalModPresence.sodiumLoaded();
        if (this.sodiumLoaded) {
            LOGGER.info("Sodium detected; Java Reforged native terrain mixins are disabled");
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !this.sodiumLoaded || !NATIVE_TERRAIN_MIXINS.contains(mixinClassName);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(
        String targetClassName,
        ClassNode targetClass,
        String mixinClassName,
        IMixinInfo mixinInfo
    ) {
    }

    @Override
    public void postApply(
        String targetClassName,
        ClassNode targetClass,
        String mixinClassName,
        IMixinInfo mixinInfo
    ) {
    }
}
