package net.jr.client.input.binding;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;
import net.jr.util.JsonFileStore;
import net.jr.api.client.input.GamepadDigitalInput;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.ArrayUtils;

public final class GamepadBindingRegistry {
    private static final String FILE_NAME = "controller_mappings.json";
    private static final Set<String> MOVEMENT_KEYS = Set.of(
        "key.forward",
        "key.left",
        "key.back",
        "key.right"
    );
    private static final Map<String, GamepadInputChord> DEFAULT_BINDINGS = createDefaultBindings();

    private static final GamepadBindingRegistry INSTANCE = new GamepadBindingRegistry();

    private final Map<String, KeyMapping> knownKeyMappings = new LinkedHashMap<>();
    private final PlayerBindings[] playerBindings = createPlayerBindings();

    private final GamepadBindingEvaluator[] evaluators = createEvaluators();
    private boolean loaded;
    @Nullable
    private Path filePath;

    private GamepadBindingRegistry() {
    }

    public static GamepadBindingRegistry get() {
        return INSTANCE;
    }

    public void ensureLoaded(Minecraft minecraft) {
        syncKeyMappings(minecraft.options);
        if (loaded) {
            return;
        }

        filePath = minecraft.gameDirectory.toPath().resolve(FILE_NAME);
        loadFromDisk();
        for (PlayerBindings bindings : this.playerBindings) {
            applyMissingDefaults(bindings);
            pruneUnknownBindings(bindings);
        }
        loaded = true;
        save();
    }

    public void syncKeyMappings(Options options) {
        Map<String, KeyMapping> refreshed = new LinkedHashMap<>();
        for (KeyMapping keyMapping : options.keyMappings) {
            refreshed.put(keyMapping.getName(), keyMapping);
        }
        knownKeyMappings.clear();
        knownKeyMappings.putAll(refreshed);
    }

    public List<KeyMapping> sortedKeyboardKeyMappings(Options options) {
        KeyMapping[] cloned = ArrayUtils.clone(options.keyMappings);
        Arrays.sort(cloned);
        return Arrays.asList(cloned);
    }

    public List<KeyMapping> sortedGamepadKeyMappings(Options options) {
        return sortedKeyboardKeyMappings(options).stream()
            .filter(keyMapping -> !isMovementKey(keyMapping))
            .toList();
    }

    public boolean isMovementKey(KeyMapping keyMapping) {
        return MOVEMENT_KEYS.contains(keyMapping.getName());
    }

    @Nullable
    public GamepadInputChord getBinding(KeyMapping keyMapping) {
        return this.bindings().get(keyMapping.getName());
    }

    public void setBinding(KeyMapping keyMapping, @Nullable GamepadDigitalInput input) {
        setBinding(keyMapping, input == null ? null : GamepadInputChord.of(input));
    }

    public void setBinding(KeyMapping keyMapping, @Nullable GamepadInputChord input) {
        String keyName = keyMapping.getName();
        PlayerBindings bindings = this.currentPlayerBindings();
        if (input == null) {
            bindings.digitalBindings.remove(keyName);
            if (DEFAULT_BINDINGS.containsKey(keyName)) {
                bindings.explicitlyUnbound.add(keyName);
            } else {
                bindings.explicitlyUnbound.remove(keyName);
            }
        } else {
            bindings.digitalBindings.put(keyName, input);
            bindings.explicitlyUnbound.remove(keyName);
        }
        save();
    }

    public void resetBindingToDefault(KeyMapping keyMapping) {
        String keyName = keyMapping.getName();
        PlayerBindings bindings = this.currentPlayerBindings();
        bindings.explicitlyUnbound.remove(keyName);
        GamepadInputChord defaultBinding = DEFAULT_BINDINGS.get(keyName);
        if (defaultBinding == null) {
            bindings.digitalBindings.remove(keyName);
        } else {
            bindings.digitalBindings.put(keyName, defaultBinding);
        }
        save();
    }

    public void resetAllToDefaults() {
        PlayerBindings bindings = this.currentPlayerBindings();
        bindings.digitalBindings.clear();
        bindings.explicitlyUnbound.clear();
        bindings.digitalBindings.putAll(DEFAULT_BINDINGS);
        pruneUnknownBindings(bindings);
        save();
    }

    public boolean isDefault(KeyMapping keyMapping) {
        return Objects.equals(getBinding(keyMapping), DEFAULT_BINDINGS.get(keyMapping.getName()));
    }

    public boolean hasAnyCustomBindings() {
        for (KeyMapping keyMapping : knownKeyMappings.values()) {
            if (!isDefault(keyMapping)) {
                return true;
            }
        }
        return false;
    }

    public List<KeyMapping> conflictingBindings(KeyMapping keyMapping) {
        GamepadInputChord input = getBinding(keyMapping);
        if (input == null) {
            return List.of();
        }

        return knownKeyMappings.values().stream()
            .filter(other -> other != keyMapping)
            .filter(other -> !shouldIgnoreConflict(keyMapping, other))
            .filter(other -> Objects.equals(input, getBinding(other)))
            .sorted(Comparator.naturalOrder())
            .toList();
    }

    public boolean hasConflict(KeyMapping keyMapping) {
        return !conflictingBindings(keyMapping).isEmpty();
    }

    public Component describeConflictNames(KeyMapping keyMapping) {
        List<KeyMapping> conflicts = conflictingBindings(keyMapping);
        String joined = conflicts.stream()
            .map(other -> other.getDisplayName().getString())
            .collect(Collectors.joining(", "));
        return Component.literal(joined);
    }

    public void applyMappedBindings(BindingContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        ensureLoaded(minecraft);
        this.evaluator().apply(this, context);
    }

    public boolean isApplied(KeyMapping keyMapping) {
        return this.evaluator().isApplied(keyMapping);
    }

    public void releaseAppliedBindings() {
        this.evaluator().release(this);
    }

    public void resetAppliedBindings() {
        this.evaluator().reset(this);
    }

    public void suppressHeldInputs() {
        this.evaluator().suppressHeldInputs();
    }

    public Map<String, GamepadInputChord> snapshotBindings() {
        return Collections.unmodifiableMap(this.bindings());
    }

    private void applyMissingDefaults(PlayerBindings bindings) {
        for (Map.Entry<String, GamepadInputChord> entry : DEFAULT_BINDINGS.entrySet()) {
            if (!bindings.explicitlyUnbound.contains(entry.getKey())) {
                bindings.digitalBindings.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
    }

    private void pruneUnknownBindings(PlayerBindings bindings) {
        bindings.digitalBindings.keySet().removeIf(keyName -> !knownKeyMappings.containsKey(keyName));
        bindings.explicitlyUnbound.removeIf(keyName -> !knownKeyMappings.containsKey(keyName));
    }

    private void loadFromDisk() {
        JsonObject root = JsonFileStore.read(this.filePath);
        if (root == null) {
            return;
        }

        boolean playerBlocks = false;
        for (int slotId = 0; slotId < this.playerBindings.length; slotId++) {
            JsonElement element = root.get(playerKey(slotId));
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            playerBlocks = true;
            this.loadBindings(element.getAsJsonObject(), this.playerBindings[slotId]);
        }

        if (!playerBlocks) {
            this.loadBindings(root, this.playerBindings[0]);
        }
    }

    private void loadBindings(JsonObject json, PlayerBindings bindings) {
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (entry.getValue().isJsonNull()) {
                bindings.explicitlyUnbound.add(entry.getKey());
                continue;
            }
            GamepadInputChord input = parseChord(entry.getValue());
            if (input != null) {
                bindings.digitalBindings.put(entry.getKey(), input);
            }
        }
    }

    public void save() {
        if (!loaded || filePath == null) {
            return;
        }

        JsonObject root = new JsonObject();
        for (int slotId = 0; slotId < this.playerBindings.length; slotId++) {
            root.add(playerKey(slotId), this.writeBindings(this.playerBindings[slotId]));
        }

        JsonFileStore.write(this.filePath, root);
    }

    private JsonObject writeBindings(PlayerBindings bindings) {
        JsonObject json = new JsonObject();
        Set<String> keys = new java.util.TreeSet<>(bindings.digitalBindings.keySet());
        keys.addAll(bindings.explicitlyUnbound);
        for (String key : keys) {
            if (bindings.explicitlyUnbound.contains(key)) {
                json.add(key, JsonNull.INSTANCE);
                continue;
            }

            List<GamepadDigitalInput> inputs = bindings.digitalBindings.get(key).inputs();
            if (inputs.size() == 1) {
                json.addProperty(key, inputs.getFirst().serializedName());
            } else {
                JsonArray array = new JsonArray();
                for (GamepadDigitalInput input : inputs) {
                    array.add(input.serializedName());
                }
                json.add(key, array);
            }
        }
        return json;
    }

    private static Map<String, GamepadInputChord> createDefaultBindings() {
        Map<String, GamepadInputChord> defaults = new LinkedHashMap<>();
        defaults.put("key.forward", chord(GamepadDigitalInput.STICK_LEFT_MOVE_UP));
        defaults.put("key.back", chord(GamepadDigitalInput.STICK_LEFT_MOVE_DOWN));
        defaults.put("key.left", chord(GamepadDigitalInput.STICK_LEFT_MOVE_LEFT));
        defaults.put("key.right", chord(GamepadDigitalInput.STICK_LEFT_MOVE_RIGHT));
        defaults.put("key.jump", chord(GamepadDigitalInput.BUTTON_DOWN));
        defaults.put("key.inventory", chord(GamepadDigitalInput.BUTTON_UP));
        defaults.put("key.attack", chord(GamepadDigitalInput.TRIGGER_RIGHT));
        defaults.put("key.use", chord(GamepadDigitalInput.TRIGGER_LEFT));
        defaults.put("key.drop", chord(GamepadDigitalInput.BUTTON_RIGHT));
        defaults.put("key.togglePerspective", chord(GamepadDigitalInput.STICK_LEFT_BUTTON));
        defaults.put("key.sneak", chord(GamepadDigitalInput.STICK_RIGHT_BUTTON));
        defaults.put(ModKeyBindings.UI_CONFIRM.getName(), chord(GamepadDigitalInput.BUTTON_DOWN));
        defaults.put(ModKeyBindings.UI_BACK.getName(), chord(GamepadDigitalInput.BUTTON_RIGHT));
        defaults.put(ModKeyBindings.UI_ALTERNATE.getName(), chord(GamepadDigitalInput.BUTTON_LEFT));
        defaults.put(ModKeyBindings.UI_NAV_UP.getName(), chord(GamepadDigitalInput.DPAD_UP));
        defaults.put(ModKeyBindings.UI_NAV_DOWN.getName(), chord(GamepadDigitalInput.DPAD_DOWN));
        defaults.put(ModKeyBindings.UI_NAV_LEFT.getName(), chord(GamepadDigitalInput.DPAD_LEFT));
        defaults.put(ModKeyBindings.UI_NAV_RIGHT.getName(), chord(GamepadDigitalInput.DPAD_RIGHT));
        defaults.put(ModKeyBindings.UI_QUICK_MOVE.getName(), chord(GamepadDigitalInput.BUTTON_UP));
        defaults.put(ModKeyBindings.UI_TAKE_ALL.getName(), chord(GamepadDigitalInput.BUMPER_LEFT));
        defaults.put(ModKeyBindings.UI_STORE_ALL.getName(), chord(GamepadDigitalInput.BUMPER_RIGHT));
        defaults.put(ModKeyBindings.GAMEPLAY_HOTBAR_PREV.getName(), chord(GamepadDigitalInput.BUMPER_LEFT));
        defaults.put(ModKeyBindings.GAMEPLAY_HOTBAR_NEXT.getName(), chord(GamepadDigitalInput.BUMPER_RIGHT));
        defaults.put(ModKeyBindings.GAMEPLAY_CRAFTING.getName(), chord(GamepadDigitalInput.BUTTON_LEFT));
        return defaults;
    }

    private static GamepadInputChord chord(GamepadDigitalInput input) {
        return GamepadInputChord.of(input);
    }

    private static boolean shouldIgnoreConflict(KeyMapping first, KeyMapping second) {
        return isUiMapping(first) != isUiMapping(second);
    }

    private static boolean isMappingActiveInContext(KeyMapping keyMapping, BindingContext context) {
        return switch (context) {

            case GAMEPLAY -> !isUiMapping(keyMapping);
            case UI -> isUiMapping(keyMapping) && !isContainerOnlyMapping(keyMapping);
            case CONTAINER -> isUiMapping(keyMapping);
        };
    }

    private static boolean isUiMapping(KeyMapping keyMapping) {
        return ModKeyBindings.CATEGORY_UI.equals(keyMapping.getCategory());
    }

    private static boolean isContainerOnlyMapping(KeyMapping keyMapping) {
        return keyMapping == ModKeyBindings.UI_QUICK_MOVE
            || keyMapping == ModKeyBindings.UI_TAKE_ALL
            || keyMapping == ModKeyBindings.UI_STORE_ALL;
    }

    public List<GamepadDigitalInput> currentlyPressedInputs() {
        return this.evaluator().currentlyPressedInputs();
    }

    private GamepadBindingEvaluator evaluator() {
        Integer slotId = SlotScope.idOrNull();
        return this.evaluators[slotId != null ? slotId : 0];
    }

    private static GamepadBindingEvaluator[] createEvaluators() {
        GamepadBindingEvaluator[] evaluators = new GamepadBindingEvaluator[LocalClientSlotRegistry.MAX_SLOTS];
        for (int slotId = 0; slotId < evaluators.length; slotId++) {
            evaluators[slotId] = new GamepadBindingEvaluator();
        }
        return evaluators;
    }

    @Nullable
    private static GamepadInputChord parseChord(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }

        if (element.isJsonPrimitive()) {
            GamepadDigitalInput input = parseInput(element);
            return input == null ? null : GamepadInputChord.of(input);
        }

        if (!element.isJsonArray()) {
            return null;
        }

        List<GamepadDigitalInput> inputs = new ArrayList<>();
        for (JsonElement child : element.getAsJsonArray()) {
            GamepadDigitalInput input = parseInput(child);
            if (input != null && !inputs.contains(input)) {
                inputs.add(input);
            }
        }
        return inputs.isEmpty() ? null : GamepadInputChord.of(inputs);
    }

    @Nullable
    private static GamepadDigitalInput parseInput(JsonElement element) {
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return null;
        }

        try {
            return GamepadDigitalInput.fromSerializedName(element.getAsString());
        } catch (ClassCastException | IllegalStateException exception) {
            return null;
        }
    }

    Map<String, GamepadInputChord> bindings() {
        return this.currentPlayerBindings().digitalBindings;
    }

    private PlayerBindings currentPlayerBindings() {
        Integer slotId = SlotScope.idOrNull();
        return this.playerBindings[slotId != null ? slotId : 0];
    }

    private static PlayerBindings[] createPlayerBindings() {
        PlayerBindings[] bindings = new PlayerBindings[LocalClientSlotRegistry.MAX_SLOTS];
        for (int slotId = 0; slotId < bindings.length; slotId++) {
            bindings[slotId] = new PlayerBindings();
        }
        return bindings;
    }

    private static String playerKey(int slotId) {
        return "player_" + (slotId + 1);
    }

    @Nullable
    KeyMapping keyMapping(String name) {
        return this.knownKeyMappings.get(name);
    }

    boolean isActive(KeyMapping keyMapping, BindingContext context) {
        return isMappingActiveInContext(keyMapping, context);
    }

    private static final class PlayerBindings {
        private final Map<String, GamepadInputChord> digitalBindings = new LinkedHashMap<>();
        private final Set<String> explicitlyUnbound = new LinkedHashSet<>();
    }
}
