package net.jr.client.compat.xaero;

import net.jr.mixin.compat.xaeroworldmap.SupportXaeroMinimapAccessor;
import xaero.map.mods.SupportMods;
import xaero.map.mods.SupportXaeroMinimap;
import xaero.map.mods.minimap.element.MinimapElementGraphicsWrapper;

/** Temporarily binds Xaero's singleton minimap bridge to one local slot. */
public final class XaeroWorldMapMinimapScope implements AutoCloseable {
    private static final XaeroWorldMapMinimapScope NOOP =
            new XaeroWorldMapMinimapScope();

    private final SupportXaeroMinimapAccessor access;
    private final XaeroWorldMapMinimapState state;
    private final XaeroWorldMapMinimapState backup;
    private final boolean active;

    private XaeroWorldMapMinimapScope() {
        access = null;
        state = null;
        backup = null;
        active = false;
    }

    private XaeroWorldMapMinimapScope(
            SupportXaeroMinimapAccessor access,
            XaeroWorldMapMinimapState state
    ) {
        this.access = access;
        this.state = state;
        this.backup = new XaeroWorldMapMinimapState();
        this.active = true;

        capture(access, backup);
        apply(access, state);
    }

    public static XaeroWorldMapMinimapScope activate(
            XaeroWorldMapViewState viewState
    ) {
        if (viewState == null) {
            return NOOP;
        }

        SupportXaeroMinimap support = SupportMods.xaeroMinimap;
        if (!(support instanceof SupportXaeroMinimapAccessor access)) {
            return NOOP;
        }

        XaeroWorldMapMinimapState minimapState = viewState.minimapState();
        if (minimapState.elementGraphicsWrapper() == null) {
            minimapState.elementGraphicsWrapper(
                    new MinimapElementGraphicsWrapper()
            );
        }

        return new XaeroWorldMapMinimapScope(
                access,
                minimapState
        );
    }

    @Override
    public void close() {
        if (!active) {
            return;
        }

        capture(access, state);
        apply(access, backup);
    }

    private static void capture(
            SupportXaeroMinimapAccessor access,
            XaeroWorldMapMinimapState target
    ) {
        target.deathpoints(access.javaReforged$deathpoints());
        target.refreshWaypoints(access.javaReforged$refreshWaypoints());
        target.waypointWorld(access.javaReforged$waypointWorld());
        target.mapWaypointWorld(access.javaReforged$mapWaypointWorld());
        target.mapDimensionId(access.javaReforged$mapDimensionId());
        target.dimensionDivision(access.javaReforged$dimensionDivision());
        target.waypointSet(access.javaReforged$waypointSet());
        target.allSets(access.javaReforged$allSets());
        target.waypoints(access.javaReforged$waypoints());
        target.sortedWaypoints(access.javaReforged$sortedWaypoints());
        target.waypointMenuRenderer(access.javaReforged$waypointMenuRenderer());
        target.mouseBlockWaypointWorld(access.javaReforged$mouseBlockWaypointWorld());
        target.rightClickWaypointWorld(access.javaReforged$rightClickWaypointWorld());
        target.elementGraphicsWrapper(access.javaReforged$elementGraphicsWrapper());
    }

    private static void apply(
            SupportXaeroMinimapAccessor access,
            XaeroWorldMapMinimapState source
    ) {
        access.javaReforged$deathpoints(source.deathpoints());
        access.javaReforged$refreshWaypoints(source.refreshWaypoints());
        access.javaReforged$waypointWorld(source.waypointWorld());
        access.javaReforged$mapWaypointWorld(source.mapWaypointWorld());
        access.javaReforged$mapDimensionId(source.mapDimensionId());
        access.javaReforged$dimensionDivision(source.dimensionDivision());
        access.javaReforged$waypointSet(source.waypointSet());
        access.javaReforged$allSets(source.allSets());
        access.javaReforged$waypoints(source.waypoints());
        access.javaReforged$sortedWaypoints(source.sortedWaypoints());
        access.javaReforged$waypointMenuRenderer(source.waypointMenuRenderer());
        access.javaReforged$mouseBlockWaypointWorld(source.mouseBlockWaypointWorld());
        access.javaReforged$rightClickWaypointWorld(source.rightClickWaypointWorld());
        access.javaReforged$elementGraphicsWrapper(source.elementGraphicsWrapper());
    }
}
