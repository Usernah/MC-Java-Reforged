package net.jr.mixin.compat.xaeroworldmap;

import java.util.ArrayList;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.map.mods.SupportXaeroMinimap;
import xaero.map.mods.gui.Waypoint;
import xaero.map.mods.gui.WaypointMenuRenderer;
import xaero.map.mods.minimap.element.MinimapElementGraphicsWrapper;

@Mixin(value = SupportXaeroMinimap.class, remap = false)
public interface SupportXaeroMinimapAccessor {
    @Accessor("deathpoints")
    boolean javaReforged$deathpoints();

    @Accessor("deathpoints")
    void javaReforged$deathpoints(boolean value);

    @Accessor("refreshWaypoints")
    boolean javaReforged$refreshWaypoints();

    @Accessor("refreshWaypoints")
    void javaReforged$refreshWaypoints(boolean value);

    @Accessor("waypointWorld")
    MinimapWorld javaReforged$waypointWorld();

    @Accessor("waypointWorld")
    void javaReforged$waypointWorld(MinimapWorld value);

    @Accessor("mapWaypointWorld")
    MinimapWorld javaReforged$mapWaypointWorld();

    @Accessor("mapWaypointWorld")
    void javaReforged$mapWaypointWorld(MinimapWorld value);

    @Accessor("mapDimId")
    ResourceKey<Level> javaReforged$mapDimensionId();

    @Accessor("mapDimId")
    void javaReforged$mapDimensionId(ResourceKey<Level> value);

    @Accessor("dimDiv")
    double javaReforged$dimensionDivision();

    @Accessor("dimDiv")
    void javaReforged$dimensionDivision(double value);

    @Accessor("waypointSet")
    WaypointSet javaReforged$waypointSet();

    @Accessor("waypointSet")
    void javaReforged$waypointSet(WaypointSet value);

    @Accessor("allSets")
    boolean javaReforged$allSets();

    @Accessor("allSets")
    void javaReforged$allSets(boolean value);

    @Accessor("waypoints")
    ArrayList<Waypoint> javaReforged$waypoints();

    @Accessor("waypoints")
    void javaReforged$waypoints(ArrayList<Waypoint> value);

    @Accessor("waypointsSorted")
    ArrayList<Waypoint> javaReforged$sortedWaypoints();

    @Accessor("waypointsSorted")
    void javaReforged$sortedWaypoints(ArrayList<Waypoint> value);

    @Accessor("waypointMenuRenderer")
    WaypointMenuRenderer javaReforged$waypointMenuRenderer();

    @Accessor("waypointMenuRenderer")
    void javaReforged$waypointMenuRenderer(WaypointMenuRenderer value);

    @Accessor("mouseBlockWaypointWorld")
    MinimapWorld javaReforged$mouseBlockWaypointWorld();

    @Accessor("mouseBlockWaypointWorld")
    void javaReforged$mouseBlockWaypointWorld(MinimapWorld value);

    @Accessor("rightClickWaypointWorld")
    MinimapWorld javaReforged$rightClickWaypointWorld();

    @Accessor("rightClickWaypointWorld")
    void javaReforged$rightClickWaypointWorld(MinimapWorld value);

    @Accessor("elementGraphicsWrapper")
    MinimapElementGraphicsWrapper javaReforged$elementGraphicsWrapper();

    @Accessor("elementGraphicsWrapper")
    void javaReforged$elementGraphicsWrapper(MinimapElementGraphicsWrapper value);
}
