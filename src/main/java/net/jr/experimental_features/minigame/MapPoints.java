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
                centralSpawns.add(new PointPos(213,69,-220));
                centralSpawns.add(new PointPos(215,69,-224));
                centralSpawns.add(new PointPos(219,69,-226));
                centralSpawns.add(new PointPos(223,69,-224));
                centralSpawns.add(new PointPos(225,69,-220));
                centralSpawns.add(new PointPos(223,69,-216));
                centralSpawns.add(new PointPos(219,69,-214));
                centralSpawns.add(new PointPos(215,69,-216));

        List<PointPos> secondarySpawns = new ArrayList<>();
                //secondarySpawns.add(new PointPos(13,68,14));
                //secondarySpawns.add(new PointPos(-2,68,-1));
                //secondarySpawns.add(new PointPos(13,68,-16));
                //secondarySpawns.add(new PointPos(28,68,-1));

        List<PointPos> centralChest = new ArrayList<>();
                centralChest.add(new PointPos(218, 63, -221));
                centralChest.add(new PointPos(218, 63, -219));
                centralChest.add(new PointPos(220, 63, -219));
                centralChest.add(new PointPos(220, 63, -221));
                centralChest.add(new PointPos(219, 64, -220));

        List<PointPos> secondaryChest = new ArrayList<>();
                secondaryChest.add(new PointPos(2, 68, 10));
                secondaryChest.add(new PointPos(24, 68, -12));


        this.pointsMap.put(PointGroup.SPAWN_CENTRAL,centralSpawns);
        this.pointsMap.put(PointGroup.SPAWN_SECONDARY,secondarySpawns);
        this.pointsMap.put(PointGroup.CHEST_CENTRAL,centralChest);
        this.pointsMap.put(PointGroup.CHEST_SECONDARY,secondaryChest);
    }

    public List<PointPos> getPoints(PointGroup group) {
        return this.pointsMap.getOrDefault(group, new ArrayList<>());
    }
}
