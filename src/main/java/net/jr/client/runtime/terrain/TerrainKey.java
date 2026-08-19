package net.jr.client.runtime.terrain;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record TerrainKey(ResourceKey<Level> dimension, int sectionX, int sectionY, int sectionZ) {
}
