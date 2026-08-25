package net.jr.client.runtime.minimap;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

final class MinimapSurfaceSampler {
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    public Sample sample(ClientLevel level, int blockX, int blockZ) {
        int chunkX = SectionPos.blockToSectionCoord(blockX);
        int chunkZ = SectionPos.blockToSectionCoord(blockZ);

        if (!level.hasChunk(chunkX, chunkZ)) {
            return Sample.MISSING;
        }

        LevelChunk chunk = level.getChunk(chunkX, chunkZ);

        if (chunk.isEmpty()) {
            return Sample.MISSING;
        }

        this.pos.set(blockX, 0, blockZ);

        int height = chunk.getHeight(
                Heightmap.Types.WORLD_SURFACE,
                blockX,
                blockZ
        ) + 1;

        BlockState state;

        if (height <= level.getMinY()) {
            state = Blocks.BEDROCK.defaultBlockState();
        } else {
            do {
                --height;

                this.pos.set(blockX, height, blockZ);
                state = chunk.getBlockState(this.pos);
            } while (
                    state.getMapColor(level, this.pos) == MapColor.NONE
                            && height > level.getMinY()
            );
        }

        MapColor color = state.getMapColor(level, this.pos);

        if (color == MapColor.NONE) {
            return Sample.MISSING;
        }

        return new Sample(color, height, true);
    }

    record Sample(
            MapColor color,
            int height,
            boolean available
    ) {
        static final Sample MISSING =
                new Sample(MapColor.NONE, 0, false);
    }
}