package net.jr.client.runtime.terrain;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;

public final class TerrainGraphNodes {
    private static final Map<Object, Map<SectionRenderDispatcher.RenderSection, Object>> GRAPHS =
        Collections.synchronizedMap(new WeakHashMap<>());

    private TerrainGraphNodes() {
    }

    public static void put(Object graphMap, SectionRenderDispatcher.RenderSection section, Object node) {
        nodes(graphMap).put(section, node);
    }

    public static Object get(Object graphMap, SectionRenderDispatcher.RenderSection section) {
        return nodes(graphMap).get(section);
    }

    private static Map<SectionRenderDispatcher.RenderSection, Object> nodes(Object graphMap) {
        synchronized (GRAPHS) {
            return GRAPHS.computeIfAbsent(graphMap, ignored -> new ConcurrentHashMap<>());
        }
    }
}
