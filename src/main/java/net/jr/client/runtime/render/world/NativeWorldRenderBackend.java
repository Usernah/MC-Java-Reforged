package net.jr.client.runtime.render.world;

import java.util.List;
import net.jr.client.runtime.terrain.TerrainCoordinator;
import net.jr.client.runtime.terrain.TerrainWorkPhase;

final class NativeWorldRenderBackend implements WorldRenderBackend {
    @Override
    public String id() {
        return "minecraft";
    }

    @Override
    public void beginExtractionFrame(List<WorldRenderView> views) {
        TerrainCoordinator.beginExtractionFrame(views.stream().map(WorldRenderView::slot).toList());
    }

    @Override
    public void beginRenderFrame(List<WorldRenderView> views) {
        TerrainCoordinator.prepareFrame();
    }

    @Override
    public void resourcesReloaded() {
        TerrainCoordinator.requestGlobalGeometryRebuild();
    }

    @Override
    public Scope enterRenderView(WorldRenderView view) {
        TerrainWorkPhase.Scope scope = view.updatesTerrain()
            ? TerrainWorkPhase.update()
            : TerrainWorkPhase.drawOnly();
        return scope::close;
    }
}
