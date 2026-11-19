package net.jr.client.ui.render.composition;

import java.util.ArrayList;
import java.util.List;

public final class GuiCompositionQueue {
    private static final List<Job> QUEUED = new ArrayList<>();

    private GuiCompositionQueue() {
    }

    static void enqueue(
        GuiCompositionTarget target,
        int guiWidth,
        int guiHeight,
        int guiScale
    ) {
        QUEUED.add(new Job(target, guiWidth, guiHeight, guiScale));
    }

    public static void renderQueued() {
        if (QUEUED.isEmpty()) {
            return;
        }

        List<Job> jobs = List.copyOf(QUEUED);
        QUEUED.clear();
        for (Job job : jobs) {
            job.target().renderQueued(job.guiWidth(), job.guiHeight(), job.guiScale());
        }
    }

    static void discard(GuiCompositionTarget target) {
        QUEUED.removeIf(job -> {
            if (job.target() != target) {
                return false;
            }
            target.discardQueuedState();
            return true;
        });
    }

    record Job(
        GuiCompositionTarget target,
        int guiWidth,
        int guiHeight,
        int guiScale
    ) {
    }
}
