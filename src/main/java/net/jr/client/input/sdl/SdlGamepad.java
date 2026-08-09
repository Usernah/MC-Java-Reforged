package net.jr.client.input.sdl;

import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.FloatByReference;
import dev.isxander.sdl3java.api.SdlInit;
import dev.isxander.sdl3java.api.SdlSubSystemConst;
import dev.isxander.sdl3java.api.error.SdlError;
import dev.isxander.sdl3java.api.events.SDL_EventType;
import dev.isxander.sdl3java.api.events.SdlEventTypes;
import dev.isxander.sdl3java.api.events.SdlEvents;
import dev.isxander.sdl3java.api.gamepad.SDL_Gamepad;
import dev.isxander.sdl3java.api.gamepad.SDL_GamepadButton;
import dev.isxander.sdl3java.api.guid.SDL_GUID;
import dev.isxander.sdl3java.api.hints.SdlHintConsts;
import dev.isxander.sdl3java.api.hints.SdlHints;
import dev.isxander.sdl3java.api.joystick.SDL_Joystick;
import dev.isxander.sdl3java.api.joystick.SDL_JoystickID;
import dev.isxander.sdl3java.api.joystick.SdlJoystick;
import dev.isxander.sdl3java.jna.SdlNativeLibraryLoader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.jr.Java_reforged;
import net.jr.client.input.gamepad.GamepadAxis;
import net.jr.client.input.gamepad.GamepadCalibrationRegistry;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.jr.client.input.gamepad.GamepadIdentity;
import net.jr.client.input.gamepad.RawGamepadInput;
import net.jr.client.input.gamepad.RawGamepadInputPress;
import net.minecraft.client.Minecraft;

public final class SdlGamepad {
    private static final long EVENT_POLL_INTERVAL_MS = 8L;
    private static final long TOUCHPAD_TAP_COORDINATE_GRACE_NANOS = 50_000_000L;
    private static final long DEVICE_SETTLE_DELAY_MS = 1200L;
    private static final long INVENTORY_SCAN_INTERVAL_MS = 2000L;
    private static final long HOTPLUG_RETRY_WINDOW_MS = 6000L;
    private static final long STARTUP_SCAN_DELAY_MS = 150L;
    private static final long INIT_RETRY_DELAY_MS = 2000L;
    private static final long SLOW_NATIVE_CALL_MS = 50L;
    private static final float RAW_AXIS_DIGITAL_THRESHOLD = 0.55F;
    private static final int AXIS_COUNT = 6;
    private static final int BUTTON_COUNT = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_COUNT;
    private static final int MAX_OPEN_DEVICES = 4;
    private static final long FULL_SCAN_DEVICE_ID = -2L;
    private static final long NO_PENDING_DEVICE_ID = -1L;
    private static final boolean LOG_SDL_INVENTORY = Boolean.getBoolean("split.sdlInventoryLog");
    private static final boolean LOG_SDL_RAW_INPUT = Boolean.getBoolean("split.sdlRawInputLog");
    private static final String SDL_NATIVE_RESOURCE = "/natives/windows-x86_64/SDL3.dll";
    private static final String SDL_NATIVE_FILE_NAME = "SDL3.dll";

    private static final Object SDL_LOCK = new Object();
    private static final ConcurrentLinkedQueue<RawGamepadInputPress> rawInputPresses = new ConcurrentLinkedQueue<>();
    private static final Map<Long, OpenDevice> openDevices = new LinkedHashMap<>();
    private static final Map<Long, TouchpadState> touchpadStates = new LinkedHashMap<>();
    private static volatile Map<Long, Snapshot> snapshots = Map.of();
    private static final Map<Long, Snapshot> rawInputBaselines = new LinkedHashMap<>();
    private static volatile boolean initialized = false;
    private static volatile boolean nativeLibraryPrepared = false;
    private static volatile String initializationFailureReason = "";
    private static volatile long nextInitializationRetryMs = 0L;
    private static volatile boolean workerRunning = false;
    private static volatile Thread workerThread;
    private static volatile boolean splitTouchpad = true;

    private static final int TOUCHPAD_SIDE_NONE = 0;
    private static final int TOUCHPAD_SIDE_LEFT = 1;
    private static final int TOUCHPAD_SIDE_RIGHT = 2;

    private static boolean printedDebug = false;
    private static boolean startupScanPending = false;
    private static long pendingOpenDeviceId = NO_PENDING_DEVICE_ID;
    private static long pendingOpenAt = 0L;
    private static long lastHotplugAt = 0L;
    private static long lastInventoryScanAt = 0L;
    private static volatile int lastJoystickCount = -1;

    public static final int AXIS_LEFTX = 0;
    public static final int AXIS_LEFTY = 1;
    public static final int AXIS_RIGHTX = 2;
    public static final int AXIS_RIGHTY = 3;
    public static final int AXIS_TRIGGERLEFT = 4;
    public static final int AXIS_TRIGGERRIGHT = 5;

    public static final int BUTTON_A = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_SOUTH;
    public static final int BUTTON_B = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_EAST;
    public static final int BUTTON_X = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_WEST;
    public static final int BUTTON_Y = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_NORTH;
    public static final int BUTTON_BACK = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_BACK;
    public static final int BUTTON_GUIDE = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_GUIDE;
    public static final int BUTTON_START = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_START;
    public static final int BUTTON_LEFTSTICK = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_LEFT_STICK;
    public static final int BUTTON_RIGHTSTICK = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_RIGHT_STICK;
    public static final int BUTTON_LEFTSHOULDER = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER;
    public static final int BUTTON_RIGHTSHOULDER = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER;
    public static final int BUTTON_DPAD_UP = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_DPAD_UP;
    public static final int BUTTON_DPAD_DOWN = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_DPAD_DOWN;
    public static final int BUTTON_DPAD_LEFT = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_DPAD_LEFT;
    public static final int BUTTON_DPAD_RIGHT = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_DPAD_RIGHT;
    public static final int BUTTON_MISC1 = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_MISC1;
    public static final int BUTTON_TOUCHPAD = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_TOUCHPAD;
    public static final int BUTTON_RIGHT_PADDLE1 = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_RIGHT_PADDLE1;
    public static final int BUTTON_LEFT_PADDLE1 = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_LEFT_PADDLE1;
    public static final int BUTTON_RIGHT_PADDLE2 = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_RIGHT_PADDLE2;
    public static final int BUTTON_LEFT_PADDLE2 = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_LEFT_PADDLE2;
    public static final int BUTTON_MISC2 = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_MISC2;
    public static final int BUTTON_MISC3 = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_MISC3;
    public static final int BUTTON_MISC4 = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_MISC4;
    public static final int BUTTON_MISC5 = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_MISC5;
    public static final int BUTTON_MISC6 = SDL_GamepadButton.SDL_GAMEPAD_BUTTON_MISC6;

    private SdlGamepad() {
    }

    public static void setSplitTouchpad(boolean enabled) {
        splitTouchpad = enabled;
    }

    public static void initIfNeeded() {
        if (initialized) {
            return;
        }

        long nowMs = System.currentTimeMillis();
        if (nowMs < nextInitializationRetryMs) {
            return;
        }

        synchronized (SDL_LOCK) {
            if (initialized) {
                return;
            }

            nowMs = System.currentTimeMillis();
            if (nowMs < nextInitializationRetryMs) {
                return;
            }

            try {
                loadNativeLibraryIfNeeded();
                configureJoystickHints();

                SdlError.SDL_ClearError();
                boolean initializedOk = SdlInit.SDL_Init(
                    SdlSubSystemConst.SDL_INIT_GAMEPAD
                        | SdlSubSystemConst.SDL_INIT_JOYSTICK
                        | SdlSubSystemConst.SDL_INIT_EVENTS
                );
                if (!initializedOk) {
                    initializationFailureReason = "SDL_Init failed, SDL_GetError='" + sdlError() + "'";
                    nextInitializationRetryMs = System.currentTimeMillis() + INIT_RETRY_DELAY_MS;
                    snapshots = Map.of();
                    Java_reforged.LOGGER.error("SDL no pudo inicializar entrada de gamepads. {}. Se reintentara en {} ms.", initializationFailureReason, INIT_RETRY_DELAY_MS);
                    return;
                }

                dev.isxander.sdl3java.api.gamepad.SdlGamepad.SDL_SetGamepadEventsEnabled(true);
                SdlJoystick.SDL_SetJoystickEventsEnabled(true);
            } catch (Throwable throwable) {
                initializationFailureReason = throwable.getClass().getSimpleName() + ": " + safeString(throwable.getMessage(), "sin detalle");
                nextInitializationRetryMs = System.currentTimeMillis() + INIT_RETRY_DELAY_MS;
                snapshots = Map.of();
                Java_reforged.LOGGER.error("SDL no pudo preparar libsdl4j. {}. Se reintentara en {} ms.", initializationFailureReason, INIT_RETRY_DELAY_MS, throwable);
                return;
            }

            initialized = true;
            initializationFailureReason = "";
            nextInitializationRetryMs = 0L;
            printedDebug = false;
            startupScanPending = true;
            pendingOpenDeviceId = NO_PENDING_DEVICE_ID;
            pendingOpenAt = 0L;
            lastHotplugAt = 0L;
            lastInventoryScanAt = 0L;
            lastJoystickCount = -1;
            snapshots = Map.of();
            startWorker();

            Java_reforged.LOGGER.info("SDL inicializado para gamepads. {}", debugStatus());
        }
    }

    public static boolean isConnected() {
        return !snapshots.isEmpty();
    }

    public static boolean isConnected(long deviceId) {
        Snapshot value = snapshots.get(deviceId);
        return value != null && value.connected();
    }

    public static List<Long> deviceIds() {
        return List.copyOf(snapshots.keySet());
    }

    public static GamepadIdentity identity(long deviceId) {
        return snapshot(deviceId).identity();
    }

    public static String debugStatus() {
        if (!initialized) {
            if (!initializationFailureReason.isBlank()) {
                long retryInMs = Math.max(0L, nextInitializationRetryMs - System.currentTimeMillis());
                return "SDL: init failed, retryIn=" + retryInMs + "ms, reason=" + initializationFailureReason;
            }
            return "SDL: not initialized";
        }

        List<String> names = snapshots.values().stream()
            .map(Snapshot::identity)
            .map(identity -> identity == null ? "desconocido" : identity.displayName())
            .toList();
        return "SDL: connected=" + !snapshots.isEmpty()
            + ", devices=" + lastJoystickCount
            + ", opened=" + snapshots.size()
            + ", names=" + names;
    }

    public static void update() {
        initIfNeeded();
        runMainThreadMaintenance();
    }

    public static void requestDeviceRescan() {
        initializationFailureReason = "";
        nextInitializationRetryMs = 0L;
        initIfNeeded();

        boolean scheduled = false;
        synchronized (SDL_LOCK) {
            if (!initialized) {
                Java_reforged.LOGGER.warn("Re-escaneo SDL ignorado porque SDL no esta inicializado. {}", debugStatus());
                return;
            }

            printedDebug = false;
            scheduleOpen(FULL_SCAN_DEVICE_ID, 0L);
            scheduled = true;
            Java_reforged.LOGGER.info("Re-escaneo SDL solicitado. {}", debugStatus());
        }

        if (scheduled) {
            runMainThreadMaintenance();
        }
    }

    public static void maintenanceTick() {
        initIfNeeded();
        runMainThreadMaintenance();
    }

    public static float axis01(int axisConst, float deadzone) {
        float value = firstSnapshot().axis(axisConst);
        if (Math.abs(value) < deadzone) {
            return 0f;
        }
        return clamp(value, -1f, 1f);
    }

    public static float axis01(GamepadAxis axis, float deadzone) {
        return axis01(toSdlAxis(axis), deadzone);
    }

    public static float axis01(long deviceId, GamepadAxis axis, float deadzone) {
        return axis01(snapshot(deviceId), toSdlAxis(axis), deadzone);
    }

    public static float rawAxis(GamepadAxis axis) {
        return firstSnapshot().axis(toSdlAxis(axis));
    }

    public static float rawAxis(long deviceId, GamepadAxis axis) {
        return snapshot(deviceId).axis(toSdlAxis(axis));
    }

    public static long snapshotAgeMs() {
        long readNanos = firstSnapshot().readNanos();
        if (readNanos <= 0L) {
            return -1L;
        }
        return Math.max(0L, (System.nanoTime() - readNanos) / 1_000_000L);
    }

    public static boolean button(int buttonConst) {
        return firstSnapshot().button(buttonConst);
    }

    public static boolean input(GamepadDigitalInput input, float triggerThreshold) {
        return input(firstSnapshot(), input, triggerThreshold);
    }

    public static boolean input(long deviceId, GamepadDigitalInput input, float triggerThreshold) {
        return input(snapshot(deviceId), input, triggerThreshold);
    }

    private static boolean input(Snapshot currentSnapshot, GamepadDigitalInput input, float triggerThreshold) {
        if (splitTouchpad && input == GamepadDigitalInput.TOUCHPAD_BUTTON) {
            // A split click normally resolves to LEFT or RIGHT. If the physical
            // button works but SDL cannot provide a valid finger coordinate,
            // expose the unsplit input instead of guessing from stale data.
            return currentSnapshot.button(BUTTON_TOUCHPAD)
                && currentSnapshot.touchpadPressSide() == TOUCHPAD_SIDE_NONE;
        }
        if (!splitTouchpad
            && (input == GamepadDigitalInput.TOUCHPAD_LEFT_BUTTON
                || input == GamepadDigitalInput.TOUCHPAD_RIGHT_BUTTON)) {
            return false;
        }

        RawGamepadInput calibratedInput = calibratedInput(currentSnapshot.identity(), input);
        if (calibratedInput != null) {
            return currentSnapshot.rawInput(calibratedInput);
        }

        return switch (input) {
            case BUTTON_DOWN -> currentSnapshot.button(BUTTON_A);
            case BUTTON_RIGHT -> currentSnapshot.button(BUTTON_B);
            case BUTTON_LEFT -> currentSnapshot.button(BUTTON_X);
            case BUTTON_UP -> currentSnapshot.button(BUTTON_Y);
            case BUTTON_START -> currentSnapshot.button(BUTTON_START);
            case BUTTON_SELECT -> currentSnapshot.button(BUTTON_BACK);
            case BUTTON_GUIDE -> currentSnapshot.button(BUTTON_GUIDE);
            case BUTTON_SHARE -> currentSnapshot.button(BUTTON_MISC1);
            case MISC_1 -> currentSnapshot.button(BUTTON_MISC1);
            case MISC_2 -> currentSnapshot.button(BUTTON_MISC2);
            case MISC_3 -> currentSnapshot.button(BUTTON_MISC3);
            case MISC_4 -> currentSnapshot.button(BUTTON_MISC4);
            case MISC_5 -> currentSnapshot.button(BUTTON_MISC5);
            case MISC_6 -> currentSnapshot.button(BUTTON_MISC6);
            case TOUCHPAD_BUTTON -> currentSnapshot.button(BUTTON_TOUCHPAD);
            case TOUCHPAD_LEFT_BUTTON -> currentSnapshot.touchpadPressSide() == TOUCHPAD_SIDE_LEFT;
            case TOUCHPAD_RIGHT_BUTTON -> currentSnapshot.touchpadPressSide() == TOUCHPAD_SIDE_RIGHT;
            case STICK_LEFT_BUTTON -> currentSnapshot.button(BUTTON_LEFTSTICK);
            case STICK_RIGHT_BUTTON -> currentSnapshot.button(BUTTON_RIGHTSTICK);
            case BUMPER_LEFT -> currentSnapshot.button(BUTTON_LEFTSHOULDER);
            case BUMPER_RIGHT -> currentSnapshot.button(BUTTON_RIGHTSHOULDER);
            case PADDLE_RIGHT_1 -> currentSnapshot.button(BUTTON_RIGHT_PADDLE1);
            case PADDLE_LEFT_1 -> currentSnapshot.button(BUTTON_LEFT_PADDLE1);
            case PADDLE_RIGHT_2 -> currentSnapshot.button(BUTTON_RIGHT_PADDLE2);
            case PADDLE_LEFT_2 -> currentSnapshot.button(BUTTON_LEFT_PADDLE2);
            case DPAD_UP -> currentSnapshot.button(BUTTON_DPAD_UP);
            case DPAD_DOWN -> currentSnapshot.button(BUTTON_DPAD_DOWN);
            case DPAD_LEFT -> currentSnapshot.button(BUTTON_DPAD_LEFT);
            case DPAD_RIGHT -> currentSnapshot.button(BUTTON_DPAD_RIGHT);
            case TRIGGER_LEFT -> axis01(currentSnapshot, AXIS_TRIGGERLEFT, triggerThreshold) > 0.5F;
            case TRIGGER_RIGHT -> axis01(currentSnapshot, AXIS_TRIGGERRIGHT, triggerThreshold) > 0.5F;
            case STICK_LEFT_MOVE_UP -> isAxisDirectionPressed(currentSnapshot, AXIS_LEFTY, triggerThreshold, -1);
            case STICK_LEFT_MOVE_DOWN -> isAxisDirectionPressed(currentSnapshot, AXIS_LEFTY, triggerThreshold, 1);
            case STICK_LEFT_MOVE_LEFT -> isAxisDirectionPressed(currentSnapshot, AXIS_LEFTX, triggerThreshold, -1);
            case STICK_LEFT_MOVE_RIGHT -> isAxisDirectionPressed(currentSnapshot, AXIS_LEFTX, triggerThreshold, 1);
            case STICK_RIGHT_MOVE_UP -> isAxisDirectionPressed(currentSnapshot, AXIS_RIGHTY, triggerThreshold, -1);
            case STICK_RIGHT_MOVE_DOWN -> isAxisDirectionPressed(currentSnapshot, AXIS_RIGHTY, triggerThreshold, 1);
            case STICK_RIGHT_MOVE_LEFT -> isAxisDirectionPressed(currentSnapshot, AXIS_RIGHTX, triggerThreshold, -1);
            case STICK_RIGHT_MOVE_RIGHT -> isAxisDirectionPressed(currentSnapshot, AXIS_RIGHTX, triggerThreshold, 1);
        };
    }

    public static void clearRawInputPresses() {
        rawInputPresses.clear();
    }

    public static RawGamepadInputPress pollRawInputPress() {
        initIfNeeded();
        return rawInputPresses.poll();
    }

    public static boolean rawInputActive(RawGamepadInput input) {
        return input != null && snapshots.values().stream().anyMatch(value -> value.rawInput(input));
    }

    public static boolean rawInputActive(long deviceId, RawGamepadInput input) {
        return input != null && snapshot(deviceId).rawInput(input);
    }

    public static boolean anyCalibrationInputActive() {
        return snapshots.values().stream().anyMatch(Snapshot::anyCalibrationInputActive);
    }

    public static boolean calibrationInputActive(long deviceId) {
        return snapshot(deviceId).anyCalibrationInputActive();
    }

    public static void shutdown() {
        Thread threadToJoin = null;
        synchronized (SDL_LOCK) {
            if (workerRunning) {
                workerRunning = false;
                threadToJoin = workerThread;
                workerThread = null;
            }
        }

        if (threadToJoin != null) {
            threadToJoin.interrupt();
            try {
                threadToJoin.join(1000L);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        synchronized (SDL_LOCK) {
            closeAllDevices();
            if (initialized) {
                SdlInit.SDL_Quit();
                initialized = false;
            }
            initializationFailureReason = "";
            nextInitializationRetryMs = 0L;
            printedDebug = false;
            startupScanPending = false;
            pendingOpenDeviceId = NO_PENDING_DEVICE_ID;
            pendingOpenAt = 0L;
            lastHotplugAt = 0L;
            lastInventoryScanAt = 0L;
            lastJoystickCount = -1;
            snapshots = Map.of();
            rawInputBaselines.clear();
            touchpadStates.clear();
        }
    }

    private static void startWorker() {
        if (workerRunning) {
            return;
        }

        workerRunning = true;
        Thread thread = new Thread(SdlGamepad::runEventLoop, "JavaReforged-SDL-Events");
        thread.setDaemon(true);
        workerThread = thread;
        thread.start();
    }

    private static void runEventLoop() {
        SdlEventTypes.SDL_Event event = new SdlEventTypes.SDL_Event();
        while (workerRunning) {
            boolean snapshotDirty = false;
            Map<Long, Snapshot> nextSnapshots = new LinkedHashMap<>();
            try {
                synchronized (SDL_LOCK) {
                    if (!initialized) {
                        break;
                    }

                    while (pollEvent(event)) {
                        event.read();
                        snapshotDirty |= handleEvent(event);
                    }

                    if (!openDevices.isEmpty()) {
                        SdlJoystick.SDL_UpdateJoysticks();
                        dev.isxander.sdl3java.api.gamepad.SdlGamepad.SDL_UpdateGamepads();
                        for (OpenDevice device : openDevices.values()) {
                            nextSnapshots.put(device.instanceId(), readSnapshot(device.instanceId(), device.gamepad(), device.joystick()));
                        }
                        snapshotDirty = true;
                    }
                }
            } catch (Throwable throwable) {
                Java_reforged.LOGGER.warn("Error al actualizar SDL del mando.", throwable);
            }

            if (snapshotDirty) {
                publishSnapshots(nextSnapshots);
            }

            try {
                Thread.sleep(EVENT_POLL_INTERVAL_MS);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        snapshots = Map.of();
        rawInputBaselines.clear();
    }

    private static void runMainThreadMaintenance() {
        if (!initialized || !isMinecraftThread()) {
            return;
        }

        Map<Long, Snapshot> nextSnapshots = null;
        synchronized (SDL_LOCK) {
            if (!initialized) {
                return;
            }

            if (startupScanPending) {
                startupScanPending = false;
                scheduleOpen(FULL_SCAN_DEVICE_ID, STARTUP_SCAN_DELAY_MS);
            }

            long now = System.currentTimeMillis();
            if (!hasPendingOpen() && now - lastInventoryScanAt >= INVENTORY_SCAN_INTERVAL_MS) {
                scheduleOpen(FULL_SCAN_DEVICE_ID, 0L);
            }

            if (hasPendingOpen() && now >= pendingOpenAt) {
                tryOpenPendingControllers();
                nextSnapshots = readAllSnapshots();
            }
        }

        if (nextSnapshots != null) {
            publishSnapshots(nextSnapshots);
        }
    }

    private static boolean handleEvent(SdlEventTypes.SDL_Event event) {
        int type = event.type;
        return switch (type) {
            case SDL_EventType.SDL_EVENT_GAMEPAD_ADDED, SDL_EventType.SDL_EVENT_JOYSTICK_ADDED -> {
                scheduleHotplugRescan();
                yield false;
            }
            case SDL_EventType.SDL_EVENT_GAMEPAD_REMOVED -> handleDeviceRemoved(event.gdevice.which);
            case SDL_EventType.SDL_EVENT_JOYSTICK_REMOVED -> handleDeviceRemoved(event.jdevice.which);
            case SDL_EventType.SDL_EVENT_GAMEPAD_AXIS_MOTION -> belongsToOpenDevice(event.gaxis.which);
            case SDL_EventType.SDL_EVENT_GAMEPAD_BUTTON_DOWN, SDL_EventType.SDL_EVENT_GAMEPAD_BUTTON_UP -> belongsToOpenDevice(event.gbutton.which);
            case SDL_EventType.SDL_EVENT_GAMEPAD_TOUCHPAD_DOWN,
                SDL_EventType.SDL_EVENT_GAMEPAD_TOUCHPAD_MOTION,
                SDL_EventType.SDL_EVENT_GAMEPAD_TOUCHPAD_UP -> handleTouchpadEvent(type, event.gtouchpad);
            case SDL_EventType.SDL_EVENT_JOYSTICK_BUTTON_DOWN, SDL_EventType.SDL_EVENT_JOYSTICK_BUTTON_UP -> belongsToOpenDevice(event.jbutton.which);
            case SDL_EventType.SDL_EVENT_JOYSTICK_HAT_MOTION -> belongsToOpenDevice(event.jhat.which);
            // A remap changes SDL's logical mapping, not the physical device.
            // Reopening here creates a remap -> rescan -> reopen loop on PS4 HIDAPI.
            case SDL_EventType.SDL_EVENT_GAMEPAD_REMAPPED -> belongsToOpenDevice(event.gdevice.which);
            default -> false;
        };
    }

    private static boolean handleDeviceRemoved(SDL_JoystickID deviceInstanceId) {
        // Windows/HIDAPI can emit a transient remove while replacing the
        // transport-facing device. The delayed inventory scan is authoritative;
        // closing immediately caused PS4 Bluetooth devices to reopen forever.
        scheduleHotplugRescan();
        return belongsToOpenDevice(deviceInstanceId);
    }

    private static boolean belongsToOpenDevice(SDL_JoystickID instanceId) {
        return instanceId != null && openDevices.containsKey(instanceId.longValue());
    }

    private static boolean handleTouchpadEvent(int type, SdlEventTypes.SDL_GamepadTouchpadEvent event) {
        if (event == null || event.which == null || event.touchpad != 0 || event.finger != 0) {
            return false;
        }
        long deviceId = event.which.longValue();
        if (!openDevices.containsKey(deviceId)) {
            return false;
        }
        touchpadStates.computeIfAbsent(deviceId, ignored -> new TouchpadState()).handleFingerEvent(type, event.x);
        return true;
    }

    private static boolean tryOpenPendingControllers() {
        long deviceId = pendingOpenDeviceId;
        pendingOpenDeviceId = NO_PENDING_DEVICE_ID;
        pendingOpenAt = 0L;

        if (deviceId == FULL_SCAN_DEVICE_ID) {
            boolean opened = openAvailableControllers();
            if (!opened && System.currentTimeMillis() - lastHotplugAt <= HOTPLUG_RETRY_WINDOW_MS) {
                scheduleOpen(FULL_SCAN_DEVICE_ID, DEVICE_SETTLE_DELAY_MS);
            }
            return opened;
        }
        return openControllerByIdValue(deviceId);
    }

    private static void scheduleOpen(long deviceId, long delayMs) {
        if (pendingOpenDeviceId == FULL_SCAN_DEVICE_ID && deviceId != FULL_SCAN_DEVICE_ID) {
            pendingOpenDeviceId = deviceId;
        } else if (pendingOpenDeviceId == NO_PENDING_DEVICE_ID) {
            pendingOpenDeviceId = deviceId;
        }

        pendingOpenAt = System.currentTimeMillis() + Math.max(0L, delayMs);
    }

    private static void scheduleHotplugRescan() {
        lastHotplugAt = System.currentTimeMillis();
        printedDebug = false;
        scheduleOpen(FULL_SCAN_DEVICE_ID, DEVICE_SETTLE_DELAY_MS);
    }

    private static boolean hasPendingOpen() {
        return pendingOpenDeviceId != NO_PENDING_DEVICE_ID;
    }

    private static void configureJoystickHints() {
        if (!isWindows()) {
            return;
        }

        SdlHints.SDL_SetHint(SdlHintConsts.SDL_HINT_AUTO_UPDATE_JOYSTICKS, "1");
        SdlHints.SDL_SetHint(SdlHintConsts.SDL_HINT_JOYSTICK_ALLOW_BACKGROUND_EVENTS, "1");
        SdlHints.SDL_SetHint(SdlHintConsts.SDL_HINT_JOYSTICK_HIDAPI, "1");
        SdlHints.SDL_SetHint(SdlHintConsts.SDL_HINT_JOYSTICK_HIDAPI_XBOX, "1");
        SdlHints.SDL_SetHint(SdlHintConsts.SDL_HINT_JOYSTICK_HIDAPI_PS4, "1");
        SdlHints.SDL_SetHint(SdlHintConsts.SDL_HINT_JOYSTICK_HIDAPI_PS5, "1");
        SdlHints.SDL_SetHint(SdlHintConsts.SDL_HINT_JOYSTICK_HIDAPI_STEAM, "1");
        SdlHints.SDL_SetHint(SdlHintConsts.SDL_HINT_JOYSTICK_HIDAPI_STEAMDECK, "1");
        SdlHints.SDL_SetHint(SdlHintConsts.SDL_HINT_JOYSTICK_HIDAPI_SWITCH, "1");
        SdlHints.SDL_SetHint(SdlHintConsts.SDL_HINT_JOYSTICK_WGI, "1");
        SdlHints.SDL_SetHint(SdlHintConsts.SDL_HINT_JOYSTICK_DIRECTINPUT, "1");
        SdlHints.SDL_SetHint(SdlHintConsts.SDL_HINT_JOYSTICK_RAWINPUT, "1");
        SdlHints.SDL_SetHint(SdlHintConsts.SDL_HINT_JOYSTICK_RAWINPUT_CORRELATE_XINPUT, "1");
    }

    private static boolean openAvailableControllers() {
        long start = System.nanoTime();
        SDL_JoystickID[] joystickIds = SdlJoystick.SDL_GetJoysticks();
        int previousJoystickCount = lastJoystickCount;
        lastInventoryScanAt = System.currentTimeMillis();
        lastJoystickCount = joystickIds.length;
        logSlowCall("SDL_GetJoysticks", start);
        if (!printedDebug || previousJoystickCount != joystickIds.length) {
            logDeviceInventory(joystickIds);
        }
        reconcileOpenDevices(joystickIds);

        boolean openedAny = false;
        for (SDL_JoystickID joystickId : joystickIds) {
            if (openDevices.size() >= MAX_OPEN_DEVICES) {
                break;
            }
            if (!isGameController(joystickId)) {
                continue;
            }
            if (openController(joystickId)) {
                openedAny = true;
            }
        }

        for (SDL_JoystickID joystickId : joystickIds) {
            if (openDevices.size() >= MAX_OPEN_DEVICES) {
                break;
            }
            if (isGameController(joystickId)) {
                continue;
            }
            if (openJoystick(joystickId)) {
                openedAny = true;
            }
        }

        debugPrintOnce(joystickIds.length);
        return openedAny;
    }

    private static void reconcileOpenDevices(SDL_JoystickID[] joystickIds) {
        Set<Long> connectedIds = new HashSet<>();
        for (SDL_JoystickID joystickId : joystickIds) {
            if (joystickId != null) {
                connectedIds.add(joystickId.longValue());
            }
        }
        for (long instanceId : new ArrayList<>(openDevices.keySet())) {
            if (!connectedIds.contains(instanceId)) {
                Java_reforged.LOGGER.info("[SplitSDL] Closing stale gamepad instance={}", instanceId);
                closeDevice(instanceId);
            }
        }
    }

    private static boolean openControllerByIdValue(long deviceIdValue) {
        if (deviceIdValue < 0L) {
            return false;
        }
        SDL_JoystickID id = new SDL_JoystickID(deviceIdValue);
        return isGameController(id) ? openController(id) : openJoystick(id);
    }

    private static boolean openController(SDL_JoystickID deviceId) {
        if (deviceId == null || openDevices.size() >= MAX_OPEN_DEVICES || !isGameController(deviceId)) {
            return false;
        }
        if (openDevices.containsKey(deviceId.longValue())) {
            return false;
        }

        long start = System.nanoTime();
        SdlError.SDL_ClearError();
        Java_reforged.LOGGER.debug("Intentando abrir SDL Gamepad id {} en hilo '{}'.", deviceId.longValue(), Thread.currentThread().getName());
        SDL_Gamepad opened = dev.isxander.sdl3java.api.gamepad.SdlGamepad.SDL_OpenGamepad(deviceId);
        logSlowCall("SDL_OpenGamepad(" + deviceId.longValue() + ")", start);
        if (opened == null) {
            Java_reforged.LOGGER.warn("SDL no pudo abrir Gamepad id {}. SDL_GetError='{}'", deviceId.longValue(), sdlError());
            return false;
        }

        SDL_Joystick openedJoystick = dev.isxander.sdl3java.api.gamepad.SdlGamepad.SDL_GetGamepadJoystick(opened);
        long instanceId = resolveInstanceId(openedJoystick);
        if (instanceId < 0L || openDevices.containsKey(instanceId)) {
            dev.isxander.sdl3java.api.gamepad.SdlGamepad.SDL_CloseGamepad(opened);
            return false;
        }
        openDevices.put(instanceId, new OpenDevice(instanceId, opened, openedJoystick));
        long steamHandle = dev.isxander.sdl3java.api.gamepad.SdlGamepad.SDL_GetGamepadSteamHandle(opened);
        if (steamHandle != 0L) {
            Java_reforged.LOGGER.debug("[SplitSDL] Steam Input handle detectado para gamepad id {}: {}", deviceId.longValue(), steamHandle);
        }
        printedDebug = false;
        Java_reforged.LOGGER.info("[SplitSDL] Opened gamepad instance={}", instanceId);
        return true;
    }

    private static boolean openJoystick(SDL_JoystickID deviceId) {
        if (deviceId == null || openDevices.size() >= MAX_OPEN_DEVICES) {
            return false;
        }
        if (openDevices.containsKey(deviceId.longValue())) {
            return false;
        }

        long start = System.nanoTime();
        SdlError.SDL_ClearError();
        Java_reforged.LOGGER.debug("Intentando abrir SDL Joystick crudo id {} en hilo '{}'.", deviceId.longValue(), Thread.currentThread().getName());
        SDL_Joystick opened = SdlJoystick.SDL_OpenJoystick(deviceId);
        logSlowCall("SDL_OpenJoystick(" + deviceId.longValue() + ")", start);
        if (opened == null) {
            Java_reforged.LOGGER.warn("SDL no pudo abrir Joystick crudo id {}. SDL_GetError='{}'", deviceId.longValue(), sdlError());
            return false;
        }

        long instanceId = resolveInstanceId(opened);
        if (instanceId < 0L || openDevices.containsKey(instanceId)) {
            SdlJoystick.SDL_CloseJoystick(opened);
            return false;
        }
        openDevices.put(instanceId, new OpenDevice(instanceId, null, opened));
        printedDebug = false;
        Java_reforged.LOGGER.info("[SplitSDL] Opened raw joystick instance={}", instanceId);
        return true;
    }

    private static boolean isGameController(SDL_JoystickID deviceId) {
        if (deviceId == null) {
            return false;
        }

        long start = System.nanoTime();
        boolean result = dev.isxander.sdl3java.api.gamepad.SdlGamepad.SDL_IsGamepad(deviceId);
        logSlowCall("SDL_IsGamepad(" + deviceId.longValue() + ")", start);
        return result;
    }

    private static long resolveInstanceId(SDL_Joystick joystick) {
        if (joystick == null) {
            return -1L;
        }
        SDL_JoystickID joystickId = SdlJoystick.SDL_GetJoystickID(joystick);
        return joystickId == null ? -1L : joystickId.longValue();
    }

    private static Snapshot readSnapshot(long deviceId, SDL_Gamepad gameController, SDL_Joystick joystick) {
        float[] axes = new float[AXIS_COUNT];
        boolean[] buttons = new boolean[BUTTON_COUNT];

        if (gameController != null) {
            for (int axis = 0; axis < AXIS_COUNT; axis++) {
                short value = dev.isxander.sdl3java.api.gamepad.SdlGamepad.SDL_GetGamepadAxis(gameController, axis);
                axes[axis] = clamp(value / 32767.0f, -1f, 1f);
            }
            for (int button = 0; button < BUTTON_COUNT; button++) {
                buttons[button] = dev.isxander.sdl3java.api.gamepad.SdlGamepad.SDL_GetGamepadButton(gameController, button);
            }
        } else {
            int rawAxisCount = Math.min(AXIS_COUNT, Math.max(0, SdlJoystick.SDL_GetNumJoystickAxes(joystick)));
            for (int axis = 0; axis < rawAxisCount; axis++) {
                short value = SdlJoystick.SDL_GetJoystickAxis(joystick, axis);
                axes[axis] = clamp(value / 32767.0f, -1f, 1f);
            }
        }

        int rawButtonCount = Math.max(0, SdlJoystick.SDL_GetNumJoystickButtons(joystick));
        boolean[] rawButtons = new boolean[rawButtonCount];
        for (int rawButton = 0; rawButton < rawButtonCount; rawButton++) {
            rawButtons[rawButton] = SdlJoystick.SDL_GetJoystickButton(joystick, rawButton) != 0;
        }

        int rawHatCount = Math.max(0, SdlJoystick.SDL_GetNumJoystickHats(joystick));
        int[] rawHats = new int[rawHatCount];
        for (int rawHat = 0; rawHat < rawHatCount; rawHat++) {
            rawHats[rawHat] = Byte.toUnsignedInt(SdlJoystick.SDL_GetJoystickHat(joystick, rawHat));
        }

        TouchpadState touchpadState = touchpadStates.computeIfAbsent(deviceId, ignored -> new TouchpadState());
        touchpadState.refreshFinger(gameController);
        touchpadState.syncButton(gameController != null && buttons[BUTTON_TOUCHPAD]);
        return new Snapshot(
            true,
            axes,
            buttons,
            rawButtons,
            rawHats,
            touchpadState.pressedSide,
            identityFromJoystick(joystick),
            System.nanoTime()
        );
    }

    private static Map<Long, Snapshot> readAllSnapshots() {
        Map<Long, Snapshot> result = new LinkedHashMap<>();
        SdlJoystick.SDL_UpdateJoysticks();
        dev.isxander.sdl3java.api.gamepad.SdlGamepad.SDL_UpdateGamepads();
        for (OpenDevice device : openDevices.values()) {
            result.put(device.instanceId(), readSnapshot(device.instanceId(), device.gamepad(), device.joystick()));
        }
        return result;
    }

    private static void publishSnapshots(Map<Long, Snapshot> nextSnapshots) {
        synchronized (SDL_LOCK) {
            for (Map.Entry<Long, Snapshot> entry : nextSnapshots.entrySet()) {
                queueRawInputEdges(entry.getKey(), entry.getValue());
            }
            rawInputBaselines.keySet().removeIf(id -> !nextSnapshots.containsKey(id));
            snapshots = Collections.unmodifiableMap(new LinkedHashMap<>(nextSnapshots));
        }
    }

    private static void queueRawInputEdges(long deviceId, Snapshot nextSnapshot) {
        Snapshot previousSnapshot = rawInputBaselines.getOrDefault(deviceId, Snapshot.disconnected());
        if (!nextSnapshot.connected() || nextSnapshot.identity() == null) {
            rawInputBaselines.put(deviceId, nextSnapshot);
            return;
        }

        if (!previousSnapshot.connected()
            || previousSnapshot.identity() == null
            || !previousSnapshot.identity().key().equals(nextSnapshot.identity().key())) {
            rawInputBaselines.put(deviceId, nextSnapshot);
            return;
        }

        GamepadIdentity identity = nextSnapshot.identity();
        int gamepadButtonCount = Math.min(previousSnapshot.buttons().length, nextSnapshot.buttons().length);
        for (int button = 0; button < gamepadButtonCount; button++) {
            if (nextSnapshot.buttons()[button] && !previousSnapshot.buttons()[button]) {
                recordRawInput(deviceId, identity, RawGamepadInput.gamepadButton(button));
            }
        }

        int rawButtonCount = Math.min(previousSnapshot.rawButtons().length, nextSnapshot.rawButtons().length);
        for (int button = 0; button < rawButtonCount; button++) {
            if (nextSnapshot.rawButtons()[button] && !previousSnapshot.rawButtons()[button]) {
                recordRawInput(deviceId, identity, RawGamepadInput.button(button));
            }
        }

        int rawHatCount = Math.min(previousSnapshot.rawHats().length, nextSnapshot.rawHats().length);
        for (int hat = 0; hat < rawHatCount; hat++) {
            int previousValue = previousSnapshot.rawHats()[hat];
            int nextValue = nextSnapshot.rawHats()[hat];
            if (nextValue != 0 && nextValue != previousValue) {
                recordRawInput(deviceId, identity, RawGamepadInput.hat(hat, nextValue));
            }
        }

        int axisCount = Math.min(previousSnapshot.axes().length, nextSnapshot.axes().length);
        for (int axis = 0; axis < axisCount; axis++) {
            int previousDirection = axisDirection(previousSnapshot, axis);
            int nextDirection = axisDirection(nextSnapshot, axis);
            if (nextDirection != 0 && nextDirection != previousDirection) {
                recordRawInput(deviceId, identity, RawGamepadInput.axis(axis, nextDirection));
            }
        }

        rawInputBaselines.put(deviceId, nextSnapshot);
    }

    private static boolean pollEvent(SdlEventTypes.SDL_Event event) {
        return SdlEvents.SDL_PollEvent(event);
    }

    private static void logDeviceInventory(SDL_JoystickID[] joystickIds) {
        if (!LOG_SDL_INVENTORY) {
            return;
        }
        Java_reforged.LOGGER.info("Escaneo SDL: {} dispositivo(s) joystick detectado(s).", joystickIds.length);
        for (SDL_JoystickID joystickId : joystickIds) {
            try {
                String name = safeString(SdlJoystick.SDL_GetJoystickNameForID(joystickId), "SDL Controller");
                String guid = guidToString(SdlJoystick.SDL_GetJoystickGUIDForID(joystickId));
                int vendor = unsigned(SdlJoystick.SDL_GetJoystickVendorForID(joystickId));
                int product = unsigned(SdlJoystick.SDL_GetJoystickProductForID(joystickId));
                int productVersion = unsigned(SdlJoystick.SDL_GetJoystickProductVersionForID(joystickId));
                boolean gameController = isGameController(joystickId);

                Java_reforged.LOGGER.info(
                    "SDL device[{}]: name='{}', gameController={}, vid={}, pid={}, ver={}, guid={}",
                    joystickId.longValue(),
                    name,
                    gameController,
                    String.format(Locale.ROOT, "%04x", vendor),
                    String.format(Locale.ROOT, "%04x", product),
                    String.format(Locale.ROOT, "%04x", productVersion),
                    guid
                );
            } catch (Throwable throwable) {
                Java_reforged.LOGGER.warn("No se pudo describir SDL device[{}].", joystickId == null ? "null" : joystickId.longValue(), throwable);
            }
        }
    }

    private static void debugPrintOnce(int joystickCount) {
        if (printedDebug) {
            return;
        }
        printedDebug = true;
        if (!LOG_SDL_INVENTORY) {
            return;
        }
        if (joystickCount >= 0) {
            Java_reforged.LOGGER.info("[SplitSDL] SDL_GetJoysticks={}", joystickCount);
        }
        if (openDevices.isEmpty()) {
            Java_reforged.LOGGER.info("[SplitSDL] No SDL gamepad opened.");
        } else {
            Java_reforged.LOGGER.info("[SplitSDL] {} SDL gamepad(s) opened: {}", openDevices.size(), openDevices.keySet());
        }
    }

    private static void closeDevice(long instanceId) {
        OpenDevice device = openDevices.remove(instanceId);
        if (device == null) {
            return;
        }
        if (device.gamepad() != null) {
            dev.isxander.sdl3java.api.gamepad.SdlGamepad.SDL_CloseGamepad(device.gamepad());
        } else {
            SdlJoystick.SDL_CloseJoystick(device.joystick());
        }
        Java_reforged.LOGGER.info("[SplitSDL] Closed gamepad instance={}", instanceId);
        rawInputBaselines.remove(instanceId);
        touchpadStates.remove(instanceId);
        Map<Long, Snapshot> remaining = new LinkedHashMap<>(snapshots);
        remaining.remove(instanceId);
        snapshots = Collections.unmodifiableMap(remaining);
    }

    private static void closeAllDevices() {
        for (long instanceId : new ArrayList<>(openDevices.keySet())) {
            closeDevice(instanceId);
        }
        printedDebug = false;
    }

    private static void loadNativeLibraryIfNeeded() throws IOException {
        if (nativeLibraryPrepared) {
            return;
        }

        if (!isWindows()) {
            nativeLibraryPrepared = true;
            return;
        }

        try (InputStream input = SdlGamepad.class.getResourceAsStream(SDL_NATIVE_RESOURCE)) {
            if (input == null) {
                Java_reforged.LOGGER.warn("No se encontro {} en el classpath. libsdl4j intentara cargar SDL3 desde el sistema.", SDL_NATIVE_RESOURCE);
                nativeLibraryPrepared = true;
                return;
            }

            Path nativePath = resolveNativePath();
            Files.createDirectories(nativePath.getParent());
            Files.copy(input, nativePath, StandardCopyOption.REPLACE_EXISTING);
            try {
                SdlNativeLibraryLoader.loadLibSDL3FromFilePathNow(nativePath.toAbsolutePath().toString());
                Java_reforged.LOGGER.debug("SDL3 nativo cargado desde {}.", nativePath.toAbsolutePath());
            } catch (IllegalStateException alreadyLoaded) {
                Java_reforged.LOGGER.debug("SDL3 ya estaba cargado antes de JavaReforged: {}", safeString(alreadyLoaded.getMessage(), "sin detalle"));
            }
            nativeLibraryPrepared = true;
        }
    }

    private static Path resolveNativePath() {
        Minecraft minecraft = Minecraft.getInstance();
        Path baseDir = minecraft == null
            ? Path.of(System.getProperty("java.io.tmpdir"))
            : minecraft.gameDirectory.toPath();
        // Windows mantiene bloqueada una DLL mientras el proceso que la cargo
        // siga vivo. Las instancias split comparten gameDirectory, por eso no
        // pueden copiar todas sobre el mismo SDL3.dll.
        String processId = Long.toString(ProcessHandle.current().pid());
        return baseDir.resolve(".javareforged")
            .resolve("natives")
            .resolve("sdl3")
            .resolve("instances")
            .resolve(processId)
            .resolve(SDL_NATIVE_FILE_NAME);
    }

    private static void logSlowCall(String callName, long startNanos) {
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
        if (elapsedMs >= SLOW_NATIVE_CALL_MS) {
            Java_reforged.LOGGER.warn("Llamada SDL lenta: {} demoro {} ms.", callName, elapsedMs);
        }
    }

    private static String sdlError() {
        String error = SdlError.SDL_GetError();
        return error == null || error.isBlank() ? "sin detalle" : error;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static boolean isMinecraftThread() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null || minecraft.isSameThread();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float axis01(Snapshot snapshot, int axisConst, float deadzone) {
        float value = snapshot.axis(axisConst);
        return Math.abs(value) < deadzone ? 0f : value;
    }

    private static int axisDirection(Snapshot snapshot, int axis) {
        float value = snapshot.axis(axis);
        if (value >= RAW_AXIS_DIGITAL_THRESHOLD) {
            return 1;
        }
        if (value <= -RAW_AXIS_DIGITAL_THRESHOLD) {
            return -1;
        }
        return 0;
    }

    private static boolean isAxisDirectionPressed(Snapshot snapshot, int axisConst, float deadzone, int direction) {
        float value = axis01(snapshot, axisConst, deadzone);
        return direction < 0 ? value < -0.5F : value > 0.5F;
    }

    private static int toSdlAxis(GamepadAxis axis) {
        return switch (axis) {
            case LEFT_STICK_X -> AXIS_LEFTX;
            case LEFT_STICK_Y -> AXIS_LEFTY;
            case RIGHT_STICK_X -> AXIS_RIGHTX;
            case RIGHT_STICK_Y -> AXIS_RIGHTY;
            case TRIGGER_LEFT -> AXIS_TRIGGERLEFT;
            case TRIGGER_RIGHT -> AXIS_TRIGGERRIGHT;
        };
    }

    private static RawGamepadInput calibratedInput(GamepadIdentity identity, GamepadDigitalInput input) {
        if (identity == null) {
            return null;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return null;
        }

        GamepadCalibrationRegistry registry = GamepadCalibrationRegistry.get();
        registry.ensureLoaded(minecraft);
        return registry.getInput(identity, input);
    }

    private static void recordRawInput(long deviceId, GamepadIdentity identity, RawGamepadInput input) {
        if (identity == null || input == null) {
            return;
        }
        rawInputPresses.offer(new RawGamepadInputPress(deviceId, identity, input, System.currentTimeMillis()));
        if (LOG_SDL_RAW_INPUT) {
            Java_reforged.LOGGER.info("SDL raw input capturado: {} desde {}.", input.displayName(), identity.displayName());
        }
    }

    private static GamepadIdentity resolveIdentity(SDL_JoystickID instanceId) {
        long instance = instanceId == null ? -1L : instanceId.longValue();
        SDL_JoystickID deviceId = findDeviceId(instance);
        if (deviceId != null) {
            return identityFromDeviceId(deviceId);
        }

        OpenDevice open = openDevices.get(instance);
        if (open != null) {
            return identityFromJoystick(open.joystick());
        }

        String fallbackName = instance >= 0L ? "SDL Controller " + instance : "Unknown SDL Controller";
        return createIdentity(fallbackName, 0, 0, 0, "");
    }

    private static SDL_JoystickID findDeviceId(long instance) {
        if (instance < 0L) {
            return null;
        }

        SDL_JoystickID[] joystickIds = SdlJoystick.SDL_GetJoysticks();
        for (SDL_JoystickID deviceId : joystickIds) {
            if (deviceId != null && deviceId.longValue() == instance) {
                return deviceId;
            }
        }
        return null;
    }

    private static GamepadIdentity identityFromDeviceId(SDL_JoystickID deviceId) {
        String name = safeString(SdlJoystick.SDL_GetJoystickNameForID(deviceId), "SDL Controller");
        String guid = guidToString(SdlJoystick.SDL_GetJoystickGUIDForID(deviceId));
        int vendor = unsigned(SdlJoystick.SDL_GetJoystickVendorForID(deviceId));
        int product = unsigned(SdlJoystick.SDL_GetJoystickProductForID(deviceId));
        int productVersion = unsigned(SdlJoystick.SDL_GetJoystickProductVersionForID(deviceId));
        return createIdentity(name, vendor, product, productVersion, guid);
    }

    private static GamepadIdentity identityFromJoystick(SDL_Joystick joystick) {
        if (joystick == null) {
            return createIdentity("SDL Controller", 0, 0, 0, "");
        }

        SDL_JoystickID joystickId = SdlJoystick.SDL_GetJoystickID(joystick);
        String name = safeString(SdlJoystick.SDL_GetJoystickName(joystick), "SDL Controller");
        String guid = joystickId == null ? "" : guidToString(SdlJoystick.SDL_GetJoystickGUIDForID(joystickId));
        int vendor = unsigned(SdlJoystick.SDL_GetJoystickVendor(joystick));
        int product = unsigned(SdlJoystick.SDL_GetJoystickProduct(joystick));
        int productVersion = unsigned(SdlJoystick.SDL_GetJoystickProductVersion(joystick));
        return createIdentity(name, vendor, product, productVersion, guid);
    }

    private static GamepadIdentity createIdentity(String name, int vendor, int product, int productVersion, String guid) {
        String safeName = safeString(name, "SDL Controller");
        String sanitizedName = sanitize(safeName);
        String base = vendor != 0 || product != 0
            ? String.format(Locale.ROOT, "vid_%04x_pid_%04x_ver_%04x", vendor, product, productVersion)
            : (!guid.isBlank() ? "guid_" + sanitize(guid) : "name_" + sanitizedName);
        return new GamepadIdentity(base + "_" + sanitizedName, safeName, vendor, product, productVersion, guid);
    }

    private static String guidToString(SDL_GUID guid) {
        if (guid == null || guid.data == null || guid.data.length == 0) {
            return "";
        }

        StringBuilder builder = new StringBuilder(guid.data.length * 2);
        for (byte value : guid.data) {
            builder.append(String.format(Locale.ROOT, "%02x", Byte.toUnsignedInt(value)));
        }
        return builder.toString();
    }

    private static int unsigned(short value) {
        return Short.toUnsignedInt(value);
    }

    private static int unsigned(char value) {
        return value & 0xFFFF;
    }

    private static String safeString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String sanitize(String value) {
        String sanitized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return sanitized.isBlank() ? "unknown" : sanitized;
    }

    private static Snapshot firstSnapshot() {
        return snapshots.values().stream().findFirst().orElseGet(Snapshot::disconnected);
    }

    private static Snapshot snapshot(long deviceId) {
        return snapshots.getOrDefault(deviceId, Snapshot.disconnected());
    }

    private record OpenDevice(long instanceId, SDL_Gamepad gamepad, SDL_Joystick joystick) {
    }

    /** State that belongs to one physical touchpad, never to a player slot. */
    private static final class TouchpadState {
        private final ByteByReference polledFingerState = new ByteByReference();
        private final FloatByReference polledX = new FloatByReference();
        private final FloatByReference polledY = new FloatByReference();
        private final FloatByReference polledPressure = new FloatByReference();
        private float currentX;
        private float lastCoordinateX;
        private long lastCoordinateNanos;
        private long lastButtonReleaseNanos;
        private boolean fingerDown;
        private boolean coordinateValid;
        private boolean buttonDown;
        private int pressedSide = TOUCHPAD_SIDE_NONE;

        private void handleFingerEvent(int type, float x) {
            if (type == SDL_EventType.SDL_EVENT_GAMEPAD_TOUCHPAD_UP) {
                // A very quick tap can finish before SDL exposes the physical
                // button state. Preserve its final coordinate briefly so the
                // rising button snapshot can still resolve the correct side.
                this.recordCoordinate(x);
                this.clearActiveFinger();
                return;
            }

            this.fingerDown = true;
            if (validCoordinate(x)) {
                this.recordCoordinate(x);
            } else {
                this.coordinateValid = false;
            }
        }

        private void refreshFinger(SDL_Gamepad gamepad) {
            if (gamepad == null) {
                this.clearActiveFinger();
                return;
            }

            int touchpadCount = dev.isxander.sdl3java.api.gamepad.SdlGamepad.SDL_GetNumGamepadTouchpads(gamepad);
            if (touchpadCount <= 0) {
                this.clearActiveFinger();
                return;
            }

            int fingerCount = dev.isxander.sdl3java.api.gamepad.SdlGamepad.SDL_GetNumGamepadTouchpadFingers(gamepad, 0);
            if (fingerCount <= 0) {
                this.clearActiveFinger();
                return;
            }

            boolean read = dev.isxander.sdl3java.api.gamepad.SdlGamepad.SDL_GetGamepadTouchpadFinger(
                gamepad,
                0,
                0,
                this.polledFingerState,
                this.polledX,
                this.polledY,
                this.polledPressure
            );
            if (!read) {
                // Keep a coordinate received through the event queue. SDL can
                // occasionally reject the immediate poll while the event is valid.
                return;
            }

            if (this.polledFingerState.getValue() == 0) {
                this.clearActiveFinger();
                return;
            }

            float x = this.polledX.getValue();
            this.fingerDown = true;
            if (validCoordinate(x)) {
                this.recordCoordinate(x);
            } else {
                this.coordinateValid = false;
            }
        }

        private void syncButton(boolean down) {
            long now = System.nanoTime();
            if (down && !this.buttonDown) {
                this.pressedSide = this.resolvePressedSide(now);
            } else if (!down) {
                if (this.buttonDown) {
                    this.lastButtonReleaseNanos = now;
                }
                this.pressedSide = TOUCHPAD_SIDE_NONE;
            }
            this.buttonDown = down;
        }

        private int resolvePressedSide(long now) {
            if (this.fingerDown && this.coordinateValid) {
                return sideFor(this.currentX);
            }

            boolean belongsToCurrentPress = this.lastCoordinateNanos > this.lastButtonReleaseNanos;
            boolean recentEnough = now - this.lastCoordinateNanos <= TOUCHPAD_TAP_COORDINATE_GRACE_NANOS;
            if (belongsToCurrentPress && recentEnough) {
                return sideFor(this.lastCoordinateX);
            }

            // No trustworthy coordinate: expose the unified touchpad fallback.
            return TOUCHPAD_SIDE_NONE;
        }

        private void recordCoordinate(float x) {
            if (!validCoordinate(x)) {
                this.coordinateValid = false;
                return;
            }
            this.currentX = x;
            this.lastCoordinateX = x;
            this.lastCoordinateNanos = System.nanoTime();
            this.coordinateValid = true;
        }

        private void clearActiveFinger() {
            this.fingerDown = false;
            this.coordinateValid = false;
        }

        private static int sideFor(float x) {
            return x < 0.5F ? TOUCHPAD_SIDE_LEFT : TOUCHPAD_SIDE_RIGHT;
        }

        private static boolean validCoordinate(float coordinate) {
            return Float.isFinite(coordinate) && coordinate >= 0.0F && coordinate <= 1.0F;
        }
    }

    private record Snapshot(
        boolean connected,
        float[] axes,
        boolean[] buttons,
        boolean[] rawButtons,
        int[] rawHats,
        int touchpadPressSide,
        GamepadIdentity identity,
        long readNanos
    ) {
        private static Snapshot disconnected() {
            return new Snapshot(false, new float[AXIS_COUNT], new boolean[BUTTON_COUNT], new boolean[0], new int[0], TOUCHPAD_SIDE_NONE, null, 0L);
        }

        private float axis(int axis) {
            return axis >= 0 && axis < this.axes.length ? this.axes[axis] : 0f;
        }

        private boolean button(int button) {
            return button >= 0 && button < this.buttons.length && this.buttons[button];
        }

        private boolean rawInput(RawGamepadInput input) {
            return switch (input.type()) {
                case "controller_button" -> this.button(input.index());
                case "button" -> input.index() >= 0 && input.index() < this.rawButtons.length && this.rawButtons[input.index()];
                case "hat" -> input.index() >= 0 && input.index() < this.rawHats.length && (this.rawHats[input.index()] & input.value()) == input.value();
                case "axis" -> axisDirection(this, input.index()) == input.value();
                default -> false;
            };
        }

        private boolean anyCalibrationInputActive() {
            for (boolean button : this.buttons) {
                if (button) {
                    return true;
                }
            }
            for (boolean button : this.rawButtons) {
                if (button) {
                    return true;
                }
            }
            for (int hat : this.rawHats) {
                if (hat != 0) {
                    return true;
                }
            }
            for (int axis = 0; axis < this.axes.length; axis++) {
                if (axisDirection(this, axis) != 0) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "Snapshot{"
                + "connected=" + this.connected
                + ", axes=" + Arrays.toString(this.axes)
                + ", buttons=" + Arrays.toString(this.buttons)
                + ", rawButtons=" + Arrays.toString(this.rawButtons)
                + ", rawHats=" + Arrays.toString(this.rawHats)
                + ", touchpadPressSide=" + this.touchpadPressSide
                + ", identity=" + this.identity
                + ", readNanos=" + this.readNanos
                + '}';
        }
    }
}

