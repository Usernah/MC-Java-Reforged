package net.jr.client.runtime.player;

import javax.annotation.Nullable;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.state.GameplayState;
import net.jr.client.runtime.state.LegacyBehaviorMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public final class LegacyGameplayModes {
    private static final int FLIGHT_ACCELERATION_TICKS = 10;
    private static final double PLACEMENT_DISTANCE = 1.0D;
    private static final float MOVEMENT_EPSILON = 0.001F;

    private LegacyGameplayModes() {
    }

    public static boolean usesLegacyFlight(LocalPlayer player) {
        ClientInstance client = clientFor(player);
        return client != null
                && client.options().flightMode().get() == LegacyBehaviorMode.LEGACY;
    }

    public static boolean usesLegacyConstruction(LocalPlayer player) {
        ClientInstance client = clientFor(player);
        return client != null
                && client.options().constructionMode().get() == LegacyBehaviorMode.LEGACY;
    }

    public static void afterPlayerAiStep(LocalPlayer player) {
        ClientInstance client = clientFor(player);
        if (client == null) {
            return;
        }

        GameplayState state = client.gameplay();
        if (client.options().flightMode().get() != LegacyBehaviorMode.LEGACY
                || !player.getAbilities().flying
                || player.isPassenger()) {
            state.setLegacyFlightSprintTicks(0);
            return;
        }

        Vec2 movement = player.input.getMoveVector();
        float forward = movement.y;
        boolean fastFlight = player.isSprinting() && Math.abs(forward) > MOVEMENT_EPSILON;
        if (fastFlight) {
            int ticks = Math.min(FLIGHT_ACCELERATION_TICKS, state.legacyFlightSprintTicks() + 1);
            state.setLegacyFlightSprintTicks(ticks);
            double progress = ticks / (double)FLIGHT_ACCELERATION_TICKS;
            double speed = progress * progress * forward;
            Vec3 look = player.getLookAngle();
            player.move(MoverType.SELF, look.scale(speed));
        } else {
            state.setLegacyFlightSprintTicks(0);
        }

        boolean hasHorizontalInput = movement.lengthSquared() > MOVEMENT_EPSILON * MOVEMENT_EPSILON;
        boolean hasVerticalInput = player.input.keyPresses.jump() || player.input.keyPresses.shift();
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(
                hasHorizontalInput ? velocity.x : 0.0D,
                hasVerticalInput ? velocity.y : 0.0D,
                hasHorizontalInput ? velocity.z : 0.0D
        );
        player.fallDistance = 0.0F;
    }

    public static void prepareConstructionRepeat(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        ClientInstance client = clientFor(player);
        if (player == null || client == null) {
            return;
        }

        GameplayState state = client.gameplay();
        boolean eligible = client.options().constructionMode().get() == LegacyBehaviorMode.LEGACY
                && minecraft.options.keyUse.isDown()
                && !player.isUsingItem()
                && (player.getMainHandItem().getItem() instanceof BlockItem
                    || player.getOffhandItem().getItem() instanceof BlockItem);
        if (!eligible) {
            state.resetLegacyPlacement();
            return;
        }

        if (!state.legacyPlacementActive()) {
            return;
        }

        boolean movedEnough = Math.abs(player.getX() - state.legacyPlacementX()) >= PLACEMENT_DISTANCE
                || Math.abs(player.getY() - state.legacyPlacementY()) >= PLACEMENT_DISTANCE
                || Math.abs(player.getZ() - state.legacyPlacementZ()) >= PLACEMENT_DISTANCE;
        state.setRightClickDelay(movedEnough ? 0 : Math.max(1, state.rightClickDelay()));
    }

    public static void recordSuccessfulPlacement(LocalPlayer player) {
        ClientInstance client = clientFor(player);
        if (client == null
                || !usesLegacyConstruction(player)) {
            return;
        }
        client.gameplay().advanceLegacyPlacement(player.getX(), player.getY(), player.getZ());
    }

    @Nullable
    private static ClientInstance clientFor(@Nullable LocalPlayer player) {
        if (player == null) {
            return null;
        }
        for (ClientInstance client : ClientRuntime.INSTANCE.clients().all()) {
            if (client.player() == player) {
                return client;
            }
        }
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == player
                ? ClientRuntime.INSTANCE.clients().clientOrNull(0)
                : null;
    }
}
