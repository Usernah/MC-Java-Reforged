package net.jr.client.compat.sodium;

import java.util.List;
import net.jr.client.runtime.slot.ClientSlots;
import net.jr.client.runtime.render.world.WorldRenderView;
import net.minecraft.client.multiplayer.ClientLevel;


final class SodiumTaskListScheduler {
    private Object manager;
    private State state;

    void beginFrame(Object manager, List<WorldRenderView> views) {
        State state = this.stateFor(manager);
        state.pending = state.collected;
        state.pendingLevels = state.collectedLevels;
        state.collected = new Object[ClientSlots.MAX_SLOTS];
        state.collectedLevels = new ClientLevel[ClientSlots.MAX_SLOTS];
        state.coordinate = views.size() > 1;
        ClientLevel[] activeLevels = new ClientLevel[ClientSlots.MAX_SLOTS];
        for (WorldRenderView view : views) {
            int slotId = view.slotId();
            ClientSlots.requireSlotId(slotId);
            activeLevels[slotId] = view.level();
        }
        for (int slotId = 0; slotId < state.pending.length; slotId++) {
            if (state.pendingLevels[slotId] != activeLevels[slotId]) {
                state.pending[slotId] = null;
                state.pendingLevels[slotId] = null;
            }
        }
    }

    void capture(Object manager, int slotId, ClientLevel level) {
        ClientSlots.requireSlotId(slotId);
        State state = this.stateFor(manager);
        state.collected[slotId] = SodiumRendererBridge.taskList(manager);
        state.collectedLevels[slotId] = level;
    }

    boolean submit(Object manager, Object collector, Object uploadBudget) {
        State state = this.state;
        if (manager != this.manager || state == null || !state.coordinate || !hasPendingQueue(state)) {
            return false;
        }

        int queueCount = state.pending.length;
        int emptyQueues = 0;
        while (SodiumRendererBridge.collectorHasBudget(collector)
            && SodiumRendererBridge.uploadBudgetAvailable(uploadBudget)
            && emptyQueues < queueCount) {
            int slotId = state.cursor;
            state.cursor = (slotId + 1) % queueCount;
            Object queue = state.pending[slotId];
            if (queue == null || SodiumRendererBridge.taskListIsEmpty(queue)) {
                emptyQueues++;
                continue;
            }

            emptyQueues = 0;
            long sectionPosition = SodiumRendererBridge.dequeueSectionPosition(queue);
            Object section = SodiumRendererBridge.sectionAt(manager, sectionPosition);
            if (section != null) {
                SodiumRendererBridge.submitSectionTask(
                    manager,
                    collector,
                    section,
                    uploadBudget,
                    state.pendingLevels[slotId]
                );
            }
        }
        return true;
    }

    private State stateFor(Object manager) {
        if (manager != this.manager) {
            this.manager = manager;
            this.state = new State();
        }
        return this.state;
    }

    private static boolean hasPendingQueue(State state) {
        for (Object queue : state.pending) {
            if (queue != null && !SodiumRendererBridge.taskListIsEmpty(queue)) {
                return true;
            }
        }
        return false;
    }

    private static final class State {
        private Object[] collected = new Object[ClientSlots.MAX_SLOTS];
        private Object[] pending = new Object[ClientSlots.MAX_SLOTS];
        private ClientLevel[] collectedLevels = new ClientLevel[ClientSlots.MAX_SLOTS];
        private ClientLevel[] pendingLevels = new ClientLevel[ClientSlots.MAX_SLOTS];
        private boolean coordinate;
        private int cursor;
    }
}
