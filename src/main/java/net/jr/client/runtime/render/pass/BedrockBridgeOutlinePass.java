package net.jr.client.runtime.render.pass;

import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.player.BedrockBridgePlacement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public final class BedrockBridgeOutlinePass {
    private BedrockBridgeOutlinePass() {
    }

    public static void extract(
            Minecraft minecraft,
            ClientLevel level,
            LevelRenderState renderState,
            ClientInstance client
    ) {
        LocalPlayer player = client.player();
        if (player == null) {
            return;
        }

        HitResult hitResult = client.hitResult();
        if (hitResult != null && hitResult.getType() != HitResult.Type.MISS) {
            return;
        }

        BedrockBridgePlacement.Candidate candidate = BedrockBridgePlacement.resolveFirst(player);
        if (candidate == null) {
            return;
        }

        BlockState state = candidate.placementState();
        BlockPos pos = candidate.targetPos();
        VoxelShape shape = state.getShape(level, pos, CollisionContext.of(player));
        if (shape.isEmpty()) {
            return;
        }

        BlockStateModel model = minecraft
                .getModelManager()
                .getBlockStateModelSet()
                .get(state);
        boolean translucent = model.hasMaterialFlag(level, pos, state, 1);
        boolean highContrast = minecraft.options.highContrastBlockOutline().get();

        renderState.blockOutlineRenderState = new BlockOutlineRenderState(
                pos,
                translucent,
                highContrast,
                shape,
                List.of()
        );
    }
}
