package net.jr.client.ui.render.composition;

import net.minecraft.client.renderer.state.gui.GuiRenderState;

import java.util.ArrayList;
import java.util.List;

public final class GuiCompositionQueue {
    private static final List<Job> QUEUED = new ArrayList<>();
    private static final GuiCompositionStateRenderer RENDERER = new GuiCompositionStateRenderer();

    private GuiCompositionQueue() {
    }

    static void enqueue(
        GuiCompositionTarget target,
        GuiRenderState state,
        int guiWidth,
        int guiHeight,
        int guiScale
    ) {
        QUEUED.add(new Job(target, state, guiWidth, guiHeight, guiScale));
    }

    public static void renderQueued() {
        if (QUEUED.isEmpty()) {
            return;
        }

        List<Job> jobs = List.copyOf(QUEUED);
        QUEUED.clear();
        for (Job job : jobs) {
            RENDERER.render(job);
        }
    }

    static void discard(GuiCompositionTarget target) {
        QUEUED.removeIf(job -> {
            if (job.target() != target) {
                return false;
            }
            job.state().reset();
            return true;
        });
    }

    record Job(
        GuiCompositionTarget target,
        GuiRenderState state,
        int guiWidth,
        int guiHeight,
        int guiScale
    ) {
    }
}
