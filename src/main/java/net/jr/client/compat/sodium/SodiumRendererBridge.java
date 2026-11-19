package net.jr.client.compat.sodium;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;


final class SodiumRendererBridge {
    private static final String SECTION_MANAGER_FIELD = "renderSectionManager";
    private static final String RENDERER_LEVEL_FIELD = "level";
    private static final String UNIFORM_BUFFER_MANAGER_FIELD = "uniformBufferManager";
    private static final String FOG_PARAMETERS_FIELD = "lastFogParameters";
    private static final String CAMERA_POSITION_FIELD = "cameraPosition";
    private static final String FRAME_FIELD = "frame";
    private static final String TASK_LISTS_FIELD = "taskLists";
    private static final String RENDER_SECTIONS_FIELD = "renderSections";
    private static final String SECTION_CACHE_CLASS =
        "net.caffeinemc.mods.sodium.client.world.cloned.ClonedChunkSectionCache";
    private static final String CHUNK_TRACKER_HOLDER_CLASS =
        "net.caffeinemc.mods.sodium.client.render.chunk.map.ChunkTrackerHolder";
    private static final String CHUNK_EVENT_HANDLER_CLASS =
        "net.caffeinemc.mods.sodium.client.render.chunk.map.ChunkTracker$ChunkEventHandler";

    @Nullable
    private static Field sectionManagerField;
    @Nullable
    private static Field rendererLevelField;
    @Nullable
    private static Field uniformBufferManagerField;
    @Nullable
    private static Field fogParametersField;
    @Nullable
    private static Field cameraPositionField;
    @Nullable
    private static Field frameField;
    @Nullable
    private static Field taskListsField;
    @Nullable
    private static Field renderSectionsField;
    @Nullable
    private static Field chunkBuilderField;
    @Nullable
    private static Field chunkBuilderQueueField;
    @Nullable
    private static Field asyncCullExecutorField;
    @Nullable
    private static Field renderDistanceField;
    @Nullable
    private static Field sortBehaviorField;
    @Nullable
    private static Field regionsField;
    @Nullable
    private static Field chunkRendererField;
    @Nullable
    private static Constructor<?> sectionCacheConstructor;
    @Nullable
    private static Method beforeSectionUpdatesMethod;
    @Nullable
    private static Method onChunkAddedMethod;
    @Nullable
    private static Method onChunkRemovedMethod;
    @Nullable
    private static Method sectionPositionMethod;
    @Nullable
    private static Method finalizeRenderListsMethod;
    @Nullable
    private static Method tickVisibleRendersMethod;
    @Nullable
    private static Method chunkTrackerGetMethod;
    @Nullable
    private static Method readyChunksMethod;
    @Nullable
    private static Method toLongArrayMethod;
    @Nullable
    private static Method chunkTrackerEventsMethod;
    @Nullable
    private static Object discardChunkEventHandler;
    @Nullable
    private static Method taskListIsEmptyMethod;
    @Nullable
    private static Method dequeueSectionPositionMethod;
    @Nullable
    private static Method collectorHasBudgetMethod;
    @Nullable
    private static Method uploadBudgetAvailableMethod;
    @Nullable
    private static Method sectionAtMethod;
    @Nullable
    private static Method submitSectionTaskMethod;
    @Nullable
    private static Method sectionCacheCleanupMethod;
    private static final Map<ClientLevel, Object> SECTION_CACHES = new IdentityHashMap<>();
    private static final ThreadLocal<ClientLevel> PENDING_TASK_TERRAIN_SOURCE = new ThreadLocal<>();

    private SodiumRendererBridge() {
    }

    static @Nullable Object sectionManager(Object renderer) {
        try {
            return sectionManagerField(renderer).get(renderer);
        } catch (ReflectiveOperationException exception) {
            throw incompatibleSodium(exception);
        }
    }

    static @Nullable ClientLevel rendererLevel(Object renderer) {
        try {
            return (ClientLevel)rendererLevelField(renderer).get(renderer);
        } catch (ReflectiveOperationException exception) {
            throw incompatibleSodium(exception);
        }
    }

    static @Nullable Object uniformBufferManager(Object renderer) {
        try {
            return uniformBufferManagerField(renderer).get(renderer);
        } catch (ReflectiveOperationException exception) {
            throw incompatibleSodium(exception);
        }
    }

    static void bindRenderer(Object renderer, ClientLevel level, Object manager, Object uniforms) {
        try {
            rendererLevelField(renderer).set(renderer, level);
            sectionManagerField(renderer).set(renderer, manager);
            uniformBufferManagerField(renderer).set(renderer, uniforms);
        } catch (ReflectiveOperationException exception) {
            throw incompatibleSodium(exception);
        }
    }

    static Object chunkBuilder(Object manager) {
        try {
            return chunkBuilderField(manager).get(manager);
        } catch (ReflectiveOperationException exception) {
            throw incompatibleSodium(exception);
        }
    }

    static void shutdownBuilder(Object builder) {
        try {
            if (chunkBuilderRunning(builder)) {
                invoke(builder, publicMethod(builder.getClass(), "shutdown"));
            }
        } catch (ReflectiveOperationException exception) {
            throw incompatibleSodium(exception);
        }
    }

    static void reloadRenderer(Object renderer) {
        invoke(renderer, publicMethod(renderer.getClass(), "reload"));
    }

    static Object createManagerLike(Object reference, ClientLevel level) {
        try {
            int renderDistance = renderDistanceField(reference).getInt(reference);
            Object sortBehavior = sortBehaviorField(reference).get(reference);
            for (Constructor<?> constructor : reference.getClass().getConstructors()) {
                Class<?>[] parameters = constructor.getParameterTypes();
                if (parameters.length == 3
                    && parameters[0].isAssignableFrom(ClientLevel.class)
                    && parameters[1] == int.class
                    && parameters[2].isInstance(sortBehavior)) {
                    return constructor.newInstance(level, renderDistance, sortBehavior);
                }
            }
            throw new NoSuchMethodException("RenderSectionManager(ClientLevel, int, SortBehavior)");
        } catch (ReflectiveOperationException exception) {
            throw incompatibleSodium(exception);
        }
    }

    static int renderDistance(Object manager) {
        try {
            return renderDistanceField(manager).getInt(manager);
        } catch (ReflectiveOperationException exception) {
            throw incompatibleSodium(exception);
        }
    }

    static Object createUniformsLike(Object reference, ClientLevel level, int renderDistance) {
        try {
            for (Constructor<?> constructor : reference.getClass().getConstructors()) {
                Class<?>[] parameters = constructor.getParameterTypes();
                if (parameters.length == 2
                    && parameters[0].isAssignableFrom(ClientLevel.class)
                    && parameters[1] == int.class) {
                    return constructor.newInstance(level, renderDistance);
                }
            }
            throw new NoSuchMethodException("UniformBufferManager(ClientLevel, int)");
        } catch (ReflectiveOperationException exception) {
            throw incompatibleSodium(exception);
        }
    }

    static void replaceDedicatedBuilder(Object manager, Object sharedBuilder) {
        try {
            Object dedicatedBuilder = chunkBuilderField(manager).get(manager);
            if (dedicatedBuilder != sharedBuilder && chunkBuilderRunning(dedicatedBuilder)) {
                invoke(dedicatedBuilder, publicMethod(dedicatedBuilder.getClass(), "shutdown"));
            }
            chunkBuilderField(manager).set(manager, sharedBuilder);
        } catch (ReflectiveOperationException exception) {
            throw incompatibleSodium(exception);
        }
    }

    static void stopDedicatedCullExecutor(Object manager) {
        try {
            Object executor = asyncCullExecutorField(manager).get(manager);
            ((java.util.concurrent.ExecutorService)executor).shutdownNow();
        } catch (ReflectiveOperationException exception) {
            throw incompatibleSodium(exception);
        }
    }

    static void releaseTerrainState(Object manager) {
        try {
            Object executor = asyncCullExecutorField(manager).get(manager);
            ((java.util.concurrent.ExecutorService)executor).shutdownNow();
            Object sections = renderSectionsField(manager).get(manager);
            invoke(sections, publicMethod(sections.getClass(), "deleteAll"));
            Object regions = regionsField(manager).get(manager);
            invoke(regions, publicMethod(regions.getClass(), "delete"));
            Object chunkRenderer = chunkRendererField(manager).get(manager);
            invoke(chunkRenderer, publicMethod(chunkRenderer.getClass(), "delete"));
        } catch (ReflectiveOperationException exception) {
            throw incompatibleSodium(exception);
        }
    }

    static void deleteUniforms(Object uniforms) {
        invoke(uniforms, publicMethod(uniforms.getClass(), "delete"));
    }

    static void clearRendererState(Object renderer) {
        try {
            sectionManagerField(renderer).set(renderer, null);
            uniformBufferManagerField(renderer).set(renderer, null);
        } catch (ReflectiveOperationException exception) {
            throw incompatibleSodium(exception);
        }
    }

    static void activateTerrainSource(Object manager, ClientLevel level) {
        try {
            if (!(manager instanceof SodiumTerrainSourceAccess access)) {
                throw new IllegalStateException(
                    "Sodium RenderSectionManager is missing the Java Reforged terrain-state contract"
                );
            }
            access.javaReforged$setTerrainSource(
                level,
                sectionCache(level, manager.getClass().getClassLoader())
            );
        } catch (ReflectiveOperationException exception) {
            throw incompatibleSodium(exception);
        }
    }

    static void beforeSectionUpdates(Object manager) {
        invoke(manager, beforeSectionUpdatesMethod(manager));
    }

    static void onChunkAdded(Object manager, int x, int z) {
        invoke(manager, onChunkAddedMethod(manager), x, z);
    }

    static void onChunkRemoved(Object manager, int x, int z) {
        invoke(manager, onChunkRemovedMethod(manager), x, z);
    }

    static void populateReadyChunks(Object manager, ClientLevel level) {
        activateTerrainSource(manager, level);
        for (long position : readyChunks(level)) {
            onChunkAdded(manager, net.minecraft.world.level.ChunkPos.getX(position),
                net.minecraft.world.level.ChunkPos.getZ(position));
        }
    }

    static SectionPos sectionPosition(Object section) {
        try {
            Method method = sectionPositionMethod;
            if (method == null) {
                method = section.getClass().getMethod("getPosition");
                sectionPositionMethod = method;
            }
            return (SectionPos)method.invoke(section);
        } catch (ReflectiveOperationException exception) {
            throw incompatibleSodium(exception);
        }
    }

    static void activateView(Object renderer, SodiumViewState state, int listEpoch) {
        try {
            Object manager = sectionManager(renderer);
            ClientLevel level = state.level();
            if (manager == null || level == null || state.camera() == null) {
                return;
            }

            fogParametersField(renderer).set(renderer, state.fogParameters());
            cameraPositionField(manager).set(
                manager,
                new org.joml.Vector3d(state.camera().position().x, state.camera().position().y, state.camera().position().z)
            );
            Field frame = frameField(manager);
            int globalFrame = frame.getInt(manager);
            frame.setInt(manager, listEpoch);
            try {
                invoke(
                    manager,
                    finalizeRenderListsMethod(manager),
                    state.camera(),
                    state.viewport(),
                    state.fogParameters(),
                    true
                );
            } finally {
                frame.setInt(manager, globalFrame);
            }
        } catch (ReflectiveOperationException exception) {
            throw incompatibleSodium(exception);
        }
    }

    static void markVisibleAnimatedSprites(Object manager) {
        Method method = tickVisibleRendersMethod;
        if (method == null) {
            method = publicMethod(manager.getClass(), "tickVisibleRenders");
            tickVisibleRendersMethod = method;
        }
        invoke(manager, method);
    }

    static long[] readyChunks(ClientLevel level) {
        try {
            Method getTracker = chunkTrackerGetMethod;
            if (getTracker == null) {
                Class<?> holder = Class.forName(
                    CHUNK_TRACKER_HOLDER_CLASS,
                    true,
                    SodiumRendererBridge.class.getClassLoader()
                );
                getTracker = holder.getMethod("get", ClientLevel.class);
                chunkTrackerGetMethod = getTracker;
            }

            Object tracker = getTracker.invoke(null, level);
            Method getReadyChunks = readyChunksMethod;
            if (getReadyChunks == null) {
                getReadyChunks = tracker.getClass().getMethod("getReadyChunks");
                readyChunksMethod = getReadyChunks;
            }

            Object readyChunks = getReadyChunks.invoke(tracker);
            Method toArray = toLongArrayMethod;
            if (toArray == null) {
                Class<?> longCollection = Class.forName(
                    "it.unimi.dsi.fastutil.longs.LongCollection",
                    true,
                    SodiumRendererBridge.class.getClassLoader()
                );
                toArray = longCollection.getMethod("toLongArray");
                toLongArrayMethod = toArray;
            }
            long[] snapshot = (long[])toArray.invoke(readyChunks);
            discardChunkEvents(tracker);
            return snapshot;
        } catch (ReflectiveOperationException exception) {
            throw incompatibleSodium(exception);
        }
    }

    private static void discardChunkEvents(Object tracker) throws ReflectiveOperationException {
        Method method = chunkTrackerEventsMethod;
        Object handler = discardChunkEventHandler;
        if (method == null || handler == null) {
            ClassLoader classLoader = tracker.getClass().getClassLoader();
            Class<?> handlerType = Class.forName(CHUNK_EVENT_HANDLER_CLASS, true, classLoader);
            method = tracker.getClass().getMethod("forEachEvent", handlerType, handlerType);
            handler = Proxy.newProxyInstance(
                classLoader,
                new Class<?>[] { handlerType },
                (proxy, invokedMethod, arguments) -> null
            );
            chunkTrackerEventsMethod = method;
            discardChunkEventHandler = handler;
        }
        method.invoke(tracker, handler, handler);
    }

    static void maintainSectionCaches(Iterable<ClientLevel> activeLevels) {
        Set<ClientLevel> active = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (ClientLevel level : activeLevels) {
            active.add(level);
        }
        SECTION_CACHES.keySet().removeIf(level -> !active.contains(level));
        for (Object cache : SECTION_CACHES.values()) {
            Method method = sectionCacheCleanupMethod;
            if (method == null) {
                method = publicMethod(cache.getClass(), "cleanup");
                sectionCacheCleanupMethod = method;
            }
            invoke(cache, method);
        }
    }

    static @Nullable Object taskList(Object manager) {
        try {
            Field field = taskListsField;
            if (field == null) {
                field = accessibleField(manager.getClass(), TASK_LISTS_FIELD);
                taskListsField = field;
            }
            return field.get(manager);
        } catch (ReflectiveOperationException exception) {
            throw incompatibleSodium(exception);
        }
    }

    static boolean taskListIsEmpty(Object taskList) {
        Method method = taskListIsEmptyMethod;
        if (method == null) {
            method = publicMethod(taskList.getClass(), "isEmpty");
            taskListIsEmptyMethod = method;
        }
        return (boolean)invokeResult(taskList, method);
    }

    static long dequeueSectionPosition(Object taskList) {
        Method method = dequeueSectionPositionMethod;
        if (method == null) {
            method = publicMethod(taskList.getClass(), "dequeueNextSectionPos");
            dequeueSectionPositionMethod = method;
        }
        return (long)invokeResult(taskList, method);
    }

    static boolean collectorHasBudget(Object collector) {
        Method method = collectorHasBudgetMethod;
        if (method == null) {
            method = publicMethod(collector.getClass(), "hasBudgetRemaining");
            collectorHasBudgetMethod = method;
        }
        return (boolean)invokeResult(collector, method);
    }

    static boolean uploadBudgetAvailable(Object uploadBudget) {
        Method method = uploadBudgetAvailableMethod;
        if (method == null) {
            method = publicMethod(uploadBudget.getClass(), "isAvailable");
            uploadBudgetAvailableMethod = method;
        }
        return (boolean)invokeResult(uploadBudget, method);
    }

    static @Nullable Object sectionAt(Object manager, long position) {
        try {
            Field field = renderSectionsField;
            if (field == null) {
                field = accessibleField(manager.getClass(), RENDER_SECTIONS_FIELD);
                renderSectionsField = field;
            }
            Object sections = field.get(manager);
            Method method = sectionAtMethod;
            if (method == null) {
                method = publicMethod(sections.getClass(), "getConsistent", long.class);
                sectionAtMethod = method;
            }
            return invokeResult(sections, method, position);
        } catch (ReflectiveOperationException exception) {
            throw incompatibleSodium(exception);
        }
    }

    static void submitSectionTask(
        Object manager,
        Object collector,
        Object section,
        Object uploadBudget,
        ClientLevel terrainSource
    ) {
        Method method = submitSectionTaskMethod;
        if (method == null) {
            for (Method candidate : manager.getClass().getDeclaredMethods()) {
                if (candidate.getName().equals("submitSectionTask") && candidate.getParameterCount() == 3) {
                    candidate.setAccessible(true);
                    submitSectionTaskMethod = candidate;
                    method = candidate;
                    break;
                }
            }
            if (method == null) {
                throw incompatibleSodium(new NoSuchMethodException("RenderSectionManager.submitSectionTask"));
            }
        }
        PENDING_TASK_TERRAIN_SOURCE.set(terrainSource);
        try {
            invoke(manager, method, collector, section, uploadBudget);
        } finally {
            PENDING_TASK_TERRAIN_SOURCE.remove();
        }
    }

    static @Nullable ClientLevel pendingTaskTerrainSource() {
        return PENDING_TASK_TERRAIN_SOURCE.get();
    }

    private static Field sectionManagerField(Object renderer) throws NoSuchFieldException {
        Field field = sectionManagerField;
        if (field == null) {
            field = accessibleField(renderer.getClass(), SECTION_MANAGER_FIELD);
            sectionManagerField = field;
        }
        return field;
    }

    private static Field rendererLevelField(Object renderer) throws NoSuchFieldException {
        Field field = rendererLevelField;
        if (field == null) {
            field = accessibleField(renderer.getClass(), RENDERER_LEVEL_FIELD);
            rendererLevelField = field;
        }
        return field;
    }

    private static Field uniformBufferManagerField(Object renderer) throws NoSuchFieldException {
        Field field = uniformBufferManagerField;
        if (field == null) {
            field = accessibleField(renderer.getClass(), UNIFORM_BUFFER_MANAGER_FIELD);
            uniformBufferManagerField = field;
        }
        return field;
    }

    private static Field chunkBuilderField(Object manager) throws NoSuchFieldException {
        Field field = chunkBuilderField;
        if (field == null) {
            field = accessibleField(manager.getClass(), "builder");
            chunkBuilderField = field;
        }
        return field;
    }

    private static boolean chunkBuilderRunning(Object builder) throws ReflectiveOperationException {
        Field field = chunkBuilderQueueField;
        if (field == null) {
            field = accessibleField(builder.getClass(), "queue");
            chunkBuilderQueueField = field;
        }
        Object queue = field.get(builder);
        return (boolean)publicMethod(queue.getClass(), "isRunning").invoke(queue);
    }

    private static Field asyncCullExecutorField(Object manager) throws NoSuchFieldException {
        Field field = asyncCullExecutorField;
        if (field == null) {
            field = accessibleField(manager.getClass(), "asyncCullExecutor");
            asyncCullExecutorField = field;
        }
        return field;
    }

    private static Field renderDistanceField(Object manager) throws NoSuchFieldException {
        Field field = renderDistanceField;
        if (field == null) {
            field = accessibleField(manager.getClass(), "renderDistance");
            renderDistanceField = field;
        }
        return field;
    }

    private static Field sortBehaviorField(Object manager) throws NoSuchFieldException {
        Field field = sortBehaviorField;
        if (field == null) {
            field = accessibleField(manager.getClass(), "sortBehavior");
            sortBehaviorField = field;
        }
        return field;
    }

    private static Field regionsField(Object manager) throws NoSuchFieldException {
        Field field = regionsField;
        if (field == null) {
            field = accessibleField(manager.getClass(), "regions");
            regionsField = field;
        }
        return field;
    }

    private static Field chunkRendererField(Object manager) throws NoSuchFieldException {
        Field field = chunkRendererField;
        if (field == null) {
            field = accessibleField(manager.getClass(), "chunkRenderer");
            chunkRendererField = field;
        }
        return field;
    }

    private static Field renderSectionsField(Object manager) throws NoSuchFieldException {
        Field field = renderSectionsField;
        if (field == null) {
            field = accessibleField(manager.getClass(), RENDER_SECTIONS_FIELD);
            renderSectionsField = field;
        }
        return field;
    }

    private static Field fogParametersField(Object renderer) throws NoSuchFieldException {
        Field field = fogParametersField;
        if (field == null) {
            field = accessibleField(renderer.getClass(), FOG_PARAMETERS_FIELD);
            fogParametersField = field;
        }
        return field;
    }

    private static Field cameraPositionField(Object manager) throws NoSuchFieldException {
        Field field = cameraPositionField;
        if (field == null) {
            field = accessibleField(manager.getClass(), CAMERA_POSITION_FIELD);
            cameraPositionField = field;
        }
        return field;
    }

    private static Field frameField(Object manager) throws NoSuchFieldException {
        Field field = frameField;
        if (field == null) {
            field = accessibleField(manager.getClass(), FRAME_FIELD);
            frameField = field;
        }
        return field;
    }

    private static Object sectionCache(ClientLevel level, ClassLoader classLoader)
        throws ReflectiveOperationException {
        Object cache = SECTION_CACHES.get(level);
        if (cache == null) {
            Constructor<?> constructor = sectionCacheConstructor;
            if (constructor == null) {
                Class<?> type = Class.forName(SECTION_CACHE_CLASS, true, classLoader);
                constructor = type.getConstructor(Level.class);
                sectionCacheConstructor = constructor;
            }
            cache = constructor.newInstance(level);
            SECTION_CACHES.put(level, cache);
        }
        return cache;
    }

    private static Method beforeSectionUpdatesMethod(Object manager) {
        Method method = beforeSectionUpdatesMethod;
        if (method == null) {
            method = publicMethod(manager.getClass(), "beforeSectionUpdates");
            beforeSectionUpdatesMethod = method;
        }
        return method;
    }

    private static Method onChunkAddedMethod(Object manager) {
        Method method = onChunkAddedMethod;
        if (method == null) {
            method = publicMethod(manager.getClass(), "onChunkAdded", int.class, int.class);
            onChunkAddedMethod = method;
        }
        return method;
    }

    private static Method onChunkRemovedMethod(Object manager) {
        Method method = onChunkRemovedMethod;
        if (method == null) {
            method = publicMethod(manager.getClass(), "onChunkRemoved", int.class, int.class);
            onChunkRemovedMethod = method;
        }
        return method;
    }

    private static Method finalizeRenderListsMethod(Object manager) {
        Method method = finalizeRenderListsMethod;
        if (method == null) {
            for (Method candidate : manager.getClass().getMethods()) {
                if (candidate.getName().equals("finalizeRenderLists") && candidate.getParameterCount() == 4) {
                    finalizeRenderListsMethod = candidate;
                    return candidate;
                }
            }
            throw incompatibleSodium(new NoSuchMethodException("RenderSectionManager.finalizeRenderLists"));
        }
        return method;
    }

    private static Method publicMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        try {
            Method method = owner.getMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException exception) {
            throw incompatibleSodium(exception);
        }
    }

    private static void invoke(Object owner, Method method, Object... arguments) {
        try {
            method.invoke(owner, arguments);
        } catch (IllegalAccessException exception) {
            throw incompatibleSodium(exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Sodium terrain operation failed", cause);
        }
    }

    private static Object invokeResult(Object owner, Method method, Object... arguments) {
        try {
            return method.invoke(owner, arguments);
        } catch (IllegalAccessException exception) {
            throw incompatibleSodium(exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Sodium terrain operation failed", cause);
        }
    }

    private static Field accessibleField(Class<?> owner, String name) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static IllegalStateException incompatibleSodium(ReflectiveOperationException cause) {
        return new IllegalStateException("Unsupported Sodium world renderer layout", cause);
    }
}
