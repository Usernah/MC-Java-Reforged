package net.jr.client.ui.dsl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.jr.api.client.ui.UiDataFile;
import net.minecraft.client.gui.screens.Screen;

/** Runtime read trace used by development tools. Screen keys are weak to avoid retaining menus. */
public final class UiDataUsageTracker {
    private static final Map<Screen, LinkedHashMap<Key, Usage>> BY_SCREEN = new WeakHashMap<>();

    private UiDataUsageTracker() { }

    public static void record(UiDataFile file, String id, Object value) {
        Screen screen = ScreenEditorHooks.editedScreenOrNull();
        if (screen == null) return;
        synchronized (BY_SCREEN) {
            BY_SCREEN.computeIfAbsent(screen, ignored -> new LinkedHashMap<>())
                .put(new Key(file, id), new Usage(file, id, value, System.nanoTime()));
        }
    }

    public static List<Usage> usages(Screen screen) {
        synchronized (BY_SCREEN) {
            Map<Key, Usage> usages = BY_SCREEN.get(screen);
            return usages == null ? List.of() : List.copyOf(new ArrayList<>(usages.values()));
        }
    }

    private record Key(UiDataFile file, String id) { }

    public record Usage(UiDataFile file, String id, Object value, long lastReadNanos) { }
}
