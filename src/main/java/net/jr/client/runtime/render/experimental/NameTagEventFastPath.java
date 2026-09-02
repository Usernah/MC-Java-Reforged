package net.jr.client.runtime.render.experimental;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.jr.Java_reforged;
import net.neoforged.bus.EventBus;
import net.neoforged.bus.ListenerList;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Takes one immutable snapshot of the name-tag listener topology for a complete
 * multi-view extraction frame. NeoForge otherwise allocates and posts one
 * mutable event for every visible entity in every local view, even when its
 * listener array is empty.
 */
public final class NameTagEventFastPath {
    private static final MethodHandle GET_LISTENER_LIST = findListenerLookup();

    private static int extractionDepth;
    private static boolean canRenderHasListeners = true;
    private static boolean listenerLookupFailed;

    private NameTagEventFastPath() {
    }

    public static void beginExtractionFrame() {
        if (extractionDepth++ == 0) {
            canRenderHasListeners = hasListeners(RenderNameTagEvent.CanRender.class);
        }
    }

    public static void endExtractionFrame() {
        if (extractionDepth > 0) {
            extractionDepth--;
        }
        if (extractionDepth == 0) {
            canRenderHasListeners = true;
        }
    }

    public static boolean bypassCanRenderEvent() {
        if (!ExperimentalRenderController.enabled()) {
            return false;
        }
        if (extractionDepth > 0) {
            return !canRenderHasListeners;
        }
        return !hasListeners(RenderNameTagEvent.CanRender.class);
    }

    private static boolean hasListeners(Class<? extends Event> eventType) {
        if (listenerLookupFailed || GET_LISTENER_LIST == null || !(NeoForge.EVENT_BUS instanceof EventBus eventBus)) {
            return true;
        }
        try {
            ListenerList listeners = (ListenerList)GET_LISTENER_LIST.invoke(eventBus, eventType);
            return listeners.getListeners().length != 0;
        } catch (Throwable error) {
            listenerLookupFailed = true;
            Java_reforged.LOGGER.warn("Could not inspect NeoForge event listeners; preserving normal event dispatch", error);
            return true;
        }
    }

    private static MethodHandle findListenerLookup() {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(EventBus.class, MethodHandles.lookup());
            return lookup.findVirtual(
                EventBus.class,
                "getListenerList",
                MethodType.methodType(ListenerList.class, Class.class)
            );
        } catch (ReflectiveOperationException error) {
            Java_reforged.LOGGER.warn("NeoForge listener fast path is unavailable; preserving normal event dispatch", error);
            return null;
        }
    }
}
