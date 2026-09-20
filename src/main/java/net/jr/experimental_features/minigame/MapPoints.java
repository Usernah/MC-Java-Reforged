package net.jr.experimental_features.minigame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapPoints {

    public static final MapPoints INSTANCE = new MapPoints();

    private final Map<PointGroup, List<PointPos>> pointsMap;

    private MapPoints() {
        this.pointsMap = new HashMap<>();

        this.createpointsmaps();
    }

    private void createpointsmaps(){
        List<PointPos> centralSpawns = new ArrayList<>();
                centralSpawns.add(new PointPos(19,-60,5));
                centralSpawns.add(new PointPos(13,-60,6));
                centralSpawns.add(new PointPos(7,-60,5));
                centralSpawns.add(new PointPos(6,-60,-1));
                centralSpawns.add(new PointPos(7,-60,-7));
                centralSpawns.add(new PointPos(13,-60,-8));
                centralSpawns.add(new PointPos(19,-60,-7));
                centralSpawns.add(new PointPos(20,-60,-1));

        List<PointPos> secondarySpawns = new ArrayList<>();
                secondarySpawns.add(new PointPos(13,-60,14));
                secondarySpawns.add(new PointPos(-2,-60,-1));
                secondarySpawns.add(new PointPos(13,-60,-16));
                secondarySpawns.add(new PointPos(28,-60,-1));

        this.pointsMap.put(PointGroup.SPAWN_CENTRAL,centralSpawns);
        this.pointsMap.put(PointGroup.SPAWN_SECONDARY,secondarySpawns);
    }

    public List<PointPos> getPoints(PointGroup group) {
        return this.pointsMap.getOrDefault(group, new ArrayList<>());
    }
}
