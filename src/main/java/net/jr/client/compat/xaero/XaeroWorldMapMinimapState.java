package net.jr.client.compat.xaero;

import java.util.ArrayList;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.map.mods.gui.Waypoint;
import xaero.map.mods.gui.WaypointMenuRenderer;
import xaero.map.mods.minimap.element.MinimapElementGraphicsWrapper;

/**
 * Mutable state owned by Xaero's World Map -> Minimap compatibility bridge.
 *
 * <p>Xaero keeps one {@code SupportXaeroMinimap} singleton, even though Java
 * Reforged can display several independent {@code GuiMap} instances. These
 * values therefore have to travel with the active local-player slot.</p>
 */
public final class XaeroWorldMapMinimapState {
    private boolean deathpoints = true;
    private boolean refreshWaypoints = true;
    private MinimapWorld waypointWorld;
    private MinimapWorld mapWaypointWorld;
    private ResourceKey<Level> mapDimensionId;
    private double dimensionDivision = 1.0D;
    private WaypointSet waypointSet;
    private boolean allSets;
    private ArrayList<Waypoint> waypoints;
    private ArrayList<Waypoint> sortedWaypoints;
    private WaypointMenuRenderer waypointMenuRenderer;
    private MinimapWorld mouseBlockWaypointWorld;
    private MinimapWorld rightClickWaypointWorld;
    private MinimapElementGraphicsWrapper elementGraphicsWrapper;

    public boolean deathpoints() {
        return deathpoints;
    }

    public void deathpoints(boolean value) {
        deathpoints = value;
    }

    public boolean refreshWaypoints() {
        return refreshWaypoints;
    }

    public void refreshWaypoints(boolean value) {
        refreshWaypoints = value;
    }

    public MinimapWorld waypointWorld() {
        return waypointWorld;
    }

    public void waypointWorld(MinimapWorld value) {
        waypointWorld = value;
    }

    public MinimapWorld mapWaypointWorld() {
        return mapWaypointWorld;
    }

    public void mapWaypointWorld(MinimapWorld value) {
        mapWaypointWorld = value;
    }

    public ResourceKey<Level> mapDimensionId() {
        return mapDimensionId;
    }

    public void mapDimensionId(ResourceKey<Level> value) {
        mapDimensionId = value;
    }

    public double dimensionDivision() {
        return dimensionDivision;
    }

    public void dimensionDivision(double value) {
        dimensionDivision = value;
    }

    public WaypointSet waypointSet() {
        return waypointSet;
    }

    public void waypointSet(WaypointSet value) {
        waypointSet = value;
    }

    public boolean allSets() {
        return allSets;
    }

    public void allSets(boolean value) {
        allSets = value;
    }

    public ArrayList<Waypoint> waypoints() {
        return waypoints;
    }

    public void waypoints(ArrayList<Waypoint> value) {
        waypoints = value;
    }

    public ArrayList<Waypoint> sortedWaypoints() {
        return sortedWaypoints;
    }

    public void sortedWaypoints(ArrayList<Waypoint> value) {
        sortedWaypoints = value;
    }

    public WaypointMenuRenderer waypointMenuRenderer() {
        return waypointMenuRenderer;
    }

    public void waypointMenuRenderer(WaypointMenuRenderer value) {
        waypointMenuRenderer = value;
    }

    public MinimapWorld mouseBlockWaypointWorld() {
        return mouseBlockWaypointWorld;
    }

    public void mouseBlockWaypointWorld(MinimapWorld value) {
        mouseBlockWaypointWorld = value;
    }

    public MinimapWorld rightClickWaypointWorld() {
        return rightClickWaypointWorld;
    }

    public void rightClickWaypointWorld(MinimapWorld value) {
        rightClickWaypointWorld = value;
    }

    public MinimapElementGraphicsWrapper elementGraphicsWrapper() {
        return elementGraphicsWrapper;
    }

    public void elementGraphicsWrapper(MinimapElementGraphicsWrapper value) {
        elementGraphicsWrapper = value;
    }
}
