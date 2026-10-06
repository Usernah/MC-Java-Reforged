package net.jr.experimental_features.minigame;

import net.minecraft.world.phys.Vec3;

import java.util.List;

public record CoordinateData(PointGroup pointGroup, List<Vec3> positions) {
}
