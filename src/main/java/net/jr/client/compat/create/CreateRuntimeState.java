package net.jr.client.compat.create;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.jr.Java_reforged;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

/**
 * Swaps Create's legacy static client state whenever Java Reforged changes the
 * active local client. Create's render engines remain global; only interaction,
 * animation and temporal render values are scoped.
 */
public final class CreateRuntimeState {
    private static final Map<LocalClient, State> STATES = new WeakHashMap<>();
    private static final ThreadLocal<State> ACTIVE_STATE = new ThreadLocal<>();

    private static final String[] STATIC_STATE_CLASSES = {
        "net.createmod.catnip.api.client.gui.ScreenOpener",
            "com.simibubi.create.content.contraptions.actors.seat.ContraptionPlayerPassengerRotation",
        "com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler",
        "com.simibubi.create.content.contraptions.chassis.ChassisRangeDisplay",
        "com.simibubi.create.content.contraptions.minecart.CouplingHandlerClient",
        "com.simibubi.create.content.contraptions.wrench.RadialWrenchHandler",
        "com.simibubi.create.content.equipment.armor.CardboardArmorStealthOverlay",
        "com.simibubi.create.content.equipment.armor.NetheriteBacktankFirstPersonRenderer",
        "com.simibubi.create.content.equipment.blueprint.BlueprintOverlayRenderer",
        "com.simibubi.create.content.equipment.clipboard.ClipboardValueSettingsHandler$ClientEvents",
        "com.simibubi.create.content.equipment.extendoGrip.ExtendoGripRenderHandler",
        "com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer",
        "com.simibubi.create.content.equipment.symmetryWand.SymmetryHandler$Client",
        "com.simibubi.create.content.equipment.toolbox.ToolboxHandlerClient",
        "com.simibubi.create.content.equipment.zapper.terrainzapper.WorldshaperRenderHandler",
        "com.simibubi.create.content.kinetics.belt.item.BeltConnectorHandler",
        "com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorConnectionHandler",
        "com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorInteractionHandler",
        "com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRidingHandler",
        "com.simibubi.create.content.kinetics.fan.AirCurrent$Client",
        "com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointHandler",
        "com.simibubi.create.content.kinetics.turntable.TurntableHandler",
        "com.simibubi.create.content.logistics.depot.EjectorTargetHandler",
        "com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnectionHandler",
        "com.simibubi.create.content.logistics.packagePort.PackagePortTargetSelectionHandler",
        "com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedClientHandler",
        "com.simibubi.create.content.logistics.tableCloth.TableClothOverlayRenderer",
        "com.simibubi.create.content.redstone.link.LinkRenderer",
        "com.simibubi.create.content.redstone.link.controller.LinkedControllerClientHandler",
        "com.simibubi.create.content.trains.TrainHUD",
        "com.simibubi.create.content.trains.entity.TrainRelocator",
        "com.simibubi.create.content.trains.track.CurvedTrackInteraction",
        "com.simibubi.create.content.trains.track.TrackPlacementClient",
        "com.simibubi.create.content.trains.track.TrackTargetingClient",
        "com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer",
        "com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueHandler",
        "com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueLabelRenderer",
        "com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueRenderer",
        "com.simibubi.create.foundation.recipe.CreateRecipeClientCache",
        "com.simibubi.create.foundation.sound.SoundScapes",
        "com.simibubi.create.foundation.utility.ServerSpeedProvider",
        "dev.engine_room.create_flywheel.backend.engine.uniform.FrameUniforms"
    };

    private static final Set<String> CREATE_CLIENT_HANDLERS = Set.of(
        "SCHEMATIC_SENDER",
        "SCHEMATIC_HANDLER",
        "SCHEMATIC_AND_QUILL_HANDLER",
        "GLUE_HANDLER",
        "ZAPPER_RENDER_HANDLER",
        "POTATO_CANNON_RENDER_HANDLER",
        "SOUL_PULSE_EFFECT_HANDLER",
        "RAILWAYS",
        "VALUE_SETTINGS_HANDLER"
    );

    private static final Map<String, Set<String>> SHARED_STATIC_FIELDS = Map.of(
        "com.simibubi.create.foundation.utility.ServerSpeedProvider", Set.of("serverTimer"),
        "dev.engine_room.create_flywheel.backend.engine.uniform.FrameUniforms",
        Set.of("debugMode", "frustumPaused", "frustumCapture")
    );

    private static volatile Schema schema;
    private static volatile Method contraptionTickMethod;
    private static volatile Method soundScapesTickMethod;
    private static volatile Method airCurrentSoundTickMethod;
    private static volatile boolean ready;
    private static volatile boolean failed;

    private CreateRuntimeState() {
    }

    /** Called from Create's completed static initializer. */
    public static void markReady() {
        ready = true;
    }

    public static Scope activate(LocalClient client) {
        // Create owns mutable render collections that are also observed by map
        // worker threads.  Those threads still need JR's Minecraft/session
        // routing, but they must never exchange Create's render state: doing so
        // races the render thread and can invalidate both snapshots.
        if (!ready || failed || client == null || !Minecraft.getInstance().isSameThread()) {
            return Scope.NOOP;
        }

        try {
            Schema currentSchema = schema();
            State state = state(client, currentSchema);
            State previous = ACTIVE_STATE.get();
            boolean switched = previous != state;
            if (switched) {
                if (previous != null) {
                    previous.capture();
                } else {
                    primaryState(currentSchema).capture();
                }
                state.apply();
                ACTIVE_STATE.set(state);
            }
            return new Scope(state, previous, switched);
        } catch (Throwable error) {
            disable(error);
            return Scope.NOOP;
        }
    }

    public static void clear(LocalClient client) {
        synchronized (STATES) {
            STATES.remove(client);
        }
    }

    /**
     * Runs Create's collision pass after a secondary local world's entity
     * update. The normal ClientTick.Post event is too early for secondary
     * worlds because JR ticks those worlds after the primary client tick.
     */
    public static void tickContraptionsAfterWorldTick(Level level) {
        if (!ready || failed || level == null
                || !Minecraft.getInstance().isSameThread()) {
            return;
        }

        try {
            contraptionTickMethod().invoke(null, level);
        } catch (IllegalAccessException error) {
            throw new IllegalStateException(
                    "Unable to invoke Create's contraption collision tick",
                    error
            );
        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error fatal) {
                throw fatal;
            }
            throw new IllegalStateException(
                    "Create's contraption collision tick failed",
                    cause
            );
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(
                    "Create's contraption collision tick is unavailable",
                    error
            );
        }
    }

    /**
     * Restores the client-audio maintenance calls present in upstream Create.
     * Create Adapted 0.996 no longer invokes these from ClientEvents.preTick,
     * leaving ambient soundscapes alive after their sources disappear and
     * preventing fan/player sounds from advancing.
     */
    public static void tickClientAudio() {
        if (!ready || failed || !Minecraft.getInstance().isSameThread()) {
            return;
        }

        try {
            soundScapesTickMethod().invoke(null);
            airCurrentSoundTickMethod().invoke(null);
        } catch (IllegalAccessException error) {
            throw new IllegalStateException("Unable to invoke Create's client audio tick", error);
        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error fatal) {
                throw fatal;
            }
            throw new IllegalStateException("Create's client audio tick failed", cause);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Create's client audio tick is unavailable", error);
        }
    }

    private static Method soundScapesTickMethod() throws ReflectiveOperationException {
        Method cached = soundScapesTickMethod;
        if (cached == null) {
            synchronized (CreateRuntimeState.class) {
                cached = soundScapesTickMethod;
                if (cached == null) {
                    Class<?> type = Class.forName(
                        "com.simibubi.create.foundation.sound.SoundScapes",
                        false,
                        CreateRuntimeState.class.getClassLoader()
                    );
                    cached = type.getMethod("tick");
                    soundScapesTickMethod = cached;
                }
            }
        }
        return cached;
    }

    private static Method airCurrentSoundTickMethod() throws ReflectiveOperationException {
        Method cached = airCurrentSoundTickMethod;
        if (cached == null) {
            synchronized (CreateRuntimeState.class) {
                cached = airCurrentSoundTickMethod;
                if (cached == null) {
                    Class<?> type = Class.forName(
                        "com.simibubi.create.content.kinetics.fan.AirCurrent$Client",
                        false,
                        CreateRuntimeState.class.getClassLoader()
                    );
                    cached = type.getMethod("tickClientPlayerSounds");
                    airCurrentSoundTickMethod = cached;
                }
            }
        }
        return cached;
    }

    private static Method contraptionTickMethod()
            throws ReflectiveOperationException {
        Method cached = contraptionTickMethod;
        if (cached != null) {
            return cached;
        }

        synchronized (CreateRuntimeState.class) {
            cached = contraptionTickMethod;
            if (cached == null) {
                Class<?> handler = Class.forName(
                        "com.simibubi.create.content.contraptions.ContraptionHandler",
                        false,
                        CreateRuntimeState.class.getClassLoader()
                );
                cached = handler.getMethod("tick", Level.class);
                contraptionTickMethod = cached;
            }
            return cached;
        }
    }

    private static Schema schema() throws ReflectiveOperationException {
        Schema result = schema;
        if (result != null) {
            return result;
        }
        synchronized (CreateRuntimeState.class) {
            result = schema;
            if (result == null) {
                result = Schema.discover();
                schema = result;
                Java_reforged.LOGGER.info("Create split-screen state scope enabled with {} state cells", result.cells.size());
            }
            return result;
        }
    }

    private static State state(LocalClient client, Schema stateSchema) {
        synchronized (STATES) {
            return STATES.computeIfAbsent(client, ignored -> new State(stateSchema, stateSchema.baselineCopy()));
        }
    }

    private static State primaryState(Schema stateSchema) {
        LocalClient primary = ClientRuntime.INSTANCE.clients().clientOrNull(0);
        if (primary == null) {
            throw new IllegalStateException("Create state scope requires the primary local client");
        }
        return state(primary, stateSchema);
    }

    private static void disable(Throwable error) {
        if (!failed) {
            failed = true;
            Java_reforged.LOGGER.error("Create split-screen state scope was disabled after an incompatible state layout", error);
        }
    }

    public static final class Scope implements AutoCloseable {
        private static final Scope NOOP = new Scope(null, null, false);

        private final State state;
        private final State previous;
        private final boolean switched;
        private boolean closed;

        private Scope(State state, State previous, boolean switched) {
            this.state = state;
            this.previous = previous;
            this.switched = switched;
        }

        @Override
        public void close() {
            if (closed || !switched || state == null || failed) {
                return;
            }
            closed = true;
            try {
                state.capture();
                if (previous != null) {
                    previous.apply();
                    ACTIVE_STATE.set(previous);
                } else {
                    primaryState(state.schema).apply();
                    ACTIVE_STATE.remove();
                }
            } catch (Throwable error) {
                ACTIVE_STATE.remove();
                disable(error);
            }
        }
    }

    private static final class State {
        private final Schema schema;
        private Object[] values;

        private State(Schema schema, Object[] values) {
            this.schema = schema;
            this.values = values;
        }

        private void capture() throws ReflectiveOperationException {
            values = schema.capture();
        }

        private void apply() throws ReflectiveOperationException {
            schema.apply(values);
        }
    }

    private interface Cell {
        Object capture() throws ReflectiveOperationException;

        void apply(Object value) throws ReflectiveOperationException;

        Object copy(Object value);
    }

    private static final class Schema {
        private final List<Cell> cells;
        private final Object[] baseline;

        private Schema(List<Cell> cells) throws ReflectiveOperationException {
            this.cells = List.copyOf(cells);
            this.baseline = capture();
        }

        private static Schema discover() throws ReflectiveOperationException {
            List<Cell> cells = new ArrayList<>();
            ClassLoader loader = CreateRuntimeState.class.getClassLoader();
            for (String className : STATIC_STATE_CLASSES) {
                Class<?> type = optionalClass(loader, className);
                if (type != null) {
                    discoverStaticCells(type, cells);
                }
            }

            discoverOptionalStaticCells(loader, cells, "xaeroworldmap", "com.simibubi.create.compat.trainmap.XaeroTrainMap");
            discoverOptionalStaticCells(loader, cells, "journeymap", "com.simibubi.create.compat.trainmap.JourneyTrainMap");

            Class<?> createClient = Class.forName("com.simibubi.create.CreateClient", false, loader);
            for (String fieldName : CREATE_CLIENT_HANDLERS) {
                Field field = declaredField(createClient, fieldName);
                Object root = field.get(null);
                if (root != null) {
                    cells.add(ObjectCell.forRoot(root, createClient.getName() + "." + fieldName));
                }
            }

            Class<?> outliner = optionalClass(loader, "net.createmod.catnip.api.client.outliner.Outliner");
            if (outliner != null) {
                Field instance = declaredField(outliner, "instance");
                Object root = instance.get(null);
                cells.add(ObjectCell.forSelectedRoot(root, outliner.getName() + ".instance", Set.of("outlines")));
            }
            return new Schema(cells);
        }

        private static void discoverOptionalStaticCells(
            ClassLoader loader,
            List<Cell> cells,
            String modId,
            String className
        ) throws ReflectiveOperationException {
            if (!net.neoforged.fml.ModList.get().isLoaded(modId)) {
                return;
            }
            Class<?> type = optionalClass(loader, className);
            if (type != null) {
                discoverStaticCells(type, cells);
            }
        }

        private Object[] capture() throws ReflectiveOperationException {
            Object[] values = new Object[cells.size()];
            for (int i = 0; i < cells.size(); i++) {
                values[i] = cells.get(i).capture();
            }
            return values;
        }

        private void apply(Object[] values) throws ReflectiveOperationException {
            if (values.length != cells.size()) {
                throw new IllegalStateException("Create state snapshot schema changed while active");
            }
            for (int i = 0; i < cells.size(); i++) {
                cells.get(i).apply(values[i]);
            }
        }

        private Object[] baselineCopy() {
            Object[] copy = new Object[baseline.length];
            for (int i = 0; i < baseline.length; i++) {
                copy[i] = cells.get(i).copy(baseline[i]);
            }
            return copy;
        }

        private static void discoverStaticCells(Class<?> type, List<Cell> cells) throws ReflectiveOperationException {
            for (Field field : type.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (!Modifier.isStatic(modifiers) || field.isSynthetic()) {
                    continue;
                }
                if (SHARED_STATIC_FIELDS.getOrDefault(type.getName(), Set.of()).contains(field.getName())) {
                    continue;
                }
                field.setAccessible(true);
                if (!Modifier.isFinal(modifiers)) {
                    if (isMutableValueType(field.getType())) {
                        Object root = field.get(null);
                        if (root != null) {
                            cells.add(ObjectCell.forRoot(root, type.getName() + "." + field.getName()));
                        }
                    } else {
                        cells.add(new FieldCell(field));
                    }
                    continue;
                }

                Object root = field.get(null);
                if (root instanceof Map<?, ?> || root instanceof Collection<?>) {
                    if (!field.getName().toLowerCase(java.util.Locale.ROOT).contains("view")) {
                        cells.add(new RootContainerCell(root));
                    }
                } else if (isMutableValueType(field.getType())) {
                    cells.add(ObjectCell.forRoot(root, type.getName() + "." + field.getName()));
                }
            }
        }
    }

    private static final class RootContainerCell implements Cell {
        private final Object root;

        private RootContainerCell(Object root) {
            this.root = root;
        }

        @Override
        public Object capture() {
            return isolatedCopy(root);
        }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public void apply(Object value) {
            try {
                if (root instanceof Map rootMap && value instanceof Map valueMap) {
                    rootMap.clear();
                    rootMap.putAll(valueMap);
                } else if (root instanceof Collection rootCollection && value instanceof Collection valueCollection) {
                    rootCollection.clear();
                    rootCollection.addAll(valueCollection);
                }
            } catch (UnsupportedOperationException ignored) {
                // Immutable lookup tables are shared engine data, not client state.
            }
        }

        @Override
        public Object copy(Object value) {
            return isolatedCopy(value);
        }
    }

    private static final class FieldCell implements Cell {
        private final Field field;

        private FieldCell(Field field) {
            this.field = field;
        }

        @Override
        public Object capture() throws IllegalAccessException {
            return isolatedCopy(field.get(null));
        }

        @Override
        public void apply(Object value) throws IllegalAccessException {
            field.set(null, isolatedCopy(value));
        }

        @Override
        public Object copy(Object value) {
            return isolatedCopy(value);
        }
    }

    private static final class ObjectCell implements Cell {
        private final Object root;
        private final String name;
        private final List<InstanceField> fields;

        private ObjectCell(Object root, String name, Set<String> selectedFields) {
            this.root = root;
            this.name = name;
            this.fields = discoverInstanceFields(root.getClass(), selectedFields);
        }

        private static ObjectCell forRoot(Object root, String name) {
            return new ObjectCell(root, name, Set.of());
        }

        private static ObjectCell forSelectedRoot(Object root, String name, Set<String> selectedFields) {
            return new ObjectCell(root, name, selectedFields);
        }

        @Override
        public Object capture() throws ReflectiveOperationException {
            Object[] values = new Object[fields.size()];
            for (int i = 0; i < fields.size(); i++) {
                values[i] = fields.get(i).capture(root);
            }
            return values;
        }

        @Override
        public void apply(Object value) throws ReflectiveOperationException {
            Object[] values = (Object[])value;
            if (values.length != fields.size()) {
                throw new IllegalStateException("Create object state changed for " + name);
            }
            for (int i = 0; i < fields.size(); i++) {
                fields.get(i).apply(root, values[i]);
            }
        }

        @Override
        public Object copy(Object value) {
            Object[] source = (Object[])value;
            Object[] copy = new Object[source.length];
            for (int i = 0; i < source.length; i++) {
                copy[i] = fields.get(i).copy(source[i]);
            }
            return copy;
        }
    }

    private interface InstanceField {
        Object capture(Object root) throws ReflectiveOperationException;

        void apply(Object root, Object value) throws ReflectiveOperationException;

        Object copy(Object value);
    }

    private static final class AssignableInstanceField implements InstanceField {
        private final Field field;

        private AssignableInstanceField(Field field) {
            this.field = field;
        }

        @Override
        public Object capture(Object root) throws IllegalAccessException {
            return isolatedCopy(field.get(root));
        }

        @Override
        public void apply(Object root, Object value) throws IllegalAccessException {
            field.set(root, isolatedCopy(value));
        }

        @Override
        public Object copy(Object value) {
            return isolatedCopy(value);
        }
    }

    private static final class ContainerInstanceField implements InstanceField {
        private final Field field;
        private final boolean rejectNullElements;

        private ContainerInstanceField(Field field) {
            this.field = field;
            this.rejectNullElements = field.getDeclaringClass().getName()
                .equals("net.createmod.catnip.api.animation.PhysicalFloat")
                && field.getName().equals("forces");
        }

        @Override
        public Object capture(Object root) throws IllegalAccessException {
            return sanitizedCopy(field.get(root));
        }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public void apply(Object root, Object value) throws IllegalAccessException {
            Object target = field.get(root);
            try {
                if (target instanceof Map targetMap && value instanceof Map valueMap) {
                    targetMap.clear();
                    targetMap.putAll(valueMap);
                } else if (target instanceof Collection targetCollection && value instanceof Collection valueCollection) {
                    targetCollection.clear();
                    if (rejectNullElements) {
                        for (Object element : valueCollection) {
                            if (element != null) {
                                targetCollection.add(element);
                            }
                        }
                    } else {
                        targetCollection.addAll(valueCollection);
                    }
                }
            } catch (UnsupportedOperationException ignored) {
                // Immutable lookup tables are shared engine data, not client state.
            }
        }

        @Override
        public Object copy(Object value) {
            return sanitizedCopy(value);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private Object sanitizedCopy(Object value) {
            Object copy = isolatedCopy(value);
            if (rejectNullElements && copy instanceof Collection collection) {
                collection.removeIf(java.util.Objects::isNull);
            }
            return copy;
        }
    }

    private static List<InstanceField> discoverInstanceFields(Class<?> type, Set<String> selectedFields) {
        List<InstanceField> result = new ArrayList<>();
        for (Class<?> cursor = type; cursor != null && cursor != Object.class; cursor = cursor.getSuperclass()) {
            for (Field field : cursor.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || field.isSynthetic()) {
                    continue;
                }
                if (!selectedFields.isEmpty() && !selectedFields.contains(field.getName())) {
                    continue;
                }
                field.setAccessible(true);
                if (!Modifier.isFinal(modifiers)) {
                    result.add(new AssignableInstanceField(field));
                } else if (Map.class.isAssignableFrom(field.getType()) || Collection.class.isAssignableFrom(field.getType())) {
                    if (!field.getName().toLowerCase(java.util.Locale.ROOT).contains("view")) {
                        result.add(new ContainerInstanceField(field));
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    private static boolean isMutableValueType(Class<?> type) {
        String name = type.getName();
        return name.equals("net.createmod.catnip.api.animation.LerpedFloat")
            || name.equals("net.createmod.catnip.api.animation.PhysicalFloat")
            || name.startsWith("org.joml.Matrix")
            || name.startsWith("org.joml.Vector");
    }

    private static Field declaredField(Class<?> type, String name) throws NoSuchFieldException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Class<?> optionalClass(ClassLoader loader, String name) {
        try {
            return Class.forName(name, false, loader);
        } catch (ClassNotFoundException | LinkageError ignored) {
            return null;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object isolatedCopy(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof IdentityHashMap<?, ?> map) {
            return new IdentityHashMap<>(map);
        }
        if (value instanceof WeakHashMap<?, ?> map) {
            return new WeakHashMap<>(map);
        }
        if (value instanceof LinkedHashMap<?, ?> map) {
            return new LinkedHashMap<>(map);
        }
        if (value instanceof Map<?, ?> map) {
            return new HashMap<>(map);
        }
        if (value instanceof LinkedHashSet<?> set) {
            return new LinkedHashSet<>(set);
        }
        if (value instanceof Set<?> set) {
            return new HashSet<>(set);
        }
        if (value instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        if (value instanceof ArrayDeque<?> deque) {
            return new ArrayDeque<>(deque);
        }
        if (value instanceof Collection<?> collection) {
            return new ArrayList<>(collection);
        }
        if (value instanceof WeakReference<?> reference) {
            return new WeakReference<>(reference.get());
        }
        if (value.getClass().isArray()) {
            if (value instanceof Object[] objects) {
                return objects.clone();
            }
            if (value instanceof int[] ints) {
                return ints.clone();
            }
            if (value instanceof float[] floats) {
                return floats.clone();
            }
            if (value instanceof double[] doubles) {
                return doubles.clone();
            }
            if (value instanceof long[] longs) {
                return longs.clone();
            }
            if (value instanceof byte[] bytes) {
                return bytes.clone();
            }
            if (value instanceof short[] shorts) {
                return shorts.clone();
            }
            if (value instanceof char[] chars) {
                return chars.clone();
            }
            if (value instanceof boolean[] booleans) {
                return booleans.clone();
            }
        }
        return value;
    }

    public static boolean isTrainControlsActive() {
        if (!ready || failed) {
            return false;
        }

        try {
            Class<?> controlsHandler = optionalClass(
                    CreateRuntimeState.class.getClassLoader(),
                    "com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler"
            );

            if (controlsHandler == null) {
                return false;
            }

            var method = controlsHandler.getDeclaredMethod("getContraption");
            method.setAccessible(true);

            return method.invoke(null) != null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }
}
