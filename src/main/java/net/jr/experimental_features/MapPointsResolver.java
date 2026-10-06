package net.jr.experimental_features;

import net.jr.experimental_features.minigame.PointGroup;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

public class MapPointsResolver {
    private final Map<PointGroup, List<Vec3>> pointGroupList;

    public MapPointsResolver(Map<PointGroup, List<Vec3>> pointGroupList) {
        this.pointGroupList = pointGroupList;
    }



}
