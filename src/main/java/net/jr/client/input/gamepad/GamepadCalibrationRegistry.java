package net.jr.client.input.gamepad;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nullable;
import net.jr.client.input.InputJsonFiles;
import net.minecraft.client.Minecraft;

public final class GamepadCalibrationRegistry {
    private static final String FILE_NAME = "controller_calibrations.json";
    private static final GamepadCalibrationRegistry INSTANCE = new GamepadCalibrationRegistry();

    private final Map<String, Profile> profiles = new LinkedHashMap<>();
    private boolean loaded;
    @Nullable
    private Path filePath;

    private GamepadCalibrationRegistry() {
    }

    public static GamepadCalibrationRegistry get() {
        return INSTANCE;
    }

    public void ensureLoaded(Minecraft minecraft) {
        if (this.loaded) {
            return;
        }

        this.filePath = minecraft.gameDirectory.toPath().resolve(FILE_NAME);
        this.loadFromDisk();
        this.loaded = true;
        this.save();
    }

    public void setInput(GamepadIdentity identity, GamepadDigitalInput logicalInput, RawGamepadInput rawInput) {
        Profile profile = this.profiles.computeIfAbsent(identity.key(), ignored -> Profile.fromIdentity(identity));
        profile.refreshIdentity(identity);
        profile.buttons.put(logicalInput, PhysicalInput.from(rawInput));
        this.save();
    }

    public int calibratedButtonCount(GamepadIdentity identity) {
        Profile profile = this.profiles.get(identity.key());
        return profile == null ? 0 : profile.buttons.size();
    }

    @Nullable
    public RawGamepadInput getInput(GamepadIdentity identity, GamepadDigitalInput logicalInput) {
        Profile profile = this.profiles.get(identity.key());
        if (profile == null) {
            return null;
        }

        PhysicalInput input = profile.buttons.get(logicalInput);
        return input == null ? null : input.toRawInput();
    }

    public void save() {
        if (!this.loaded || this.filePath == null) {
            return;
        }

        JsonObject root = new JsonObject();
        JsonObject profilesJson = new JsonObject();
        for (Map.Entry<String, Profile> entry : new TreeMap<>(this.profiles).entrySet()) {
            profilesJson.add(entry.getKey(), entry.getValue().toJson());
        }
        root.add("profiles", profilesJson);

        InputJsonFiles.write(this.filePath, root);
    }

    private void loadFromDisk() {
        JsonObject root = InputJsonFiles.read(this.filePath);
        if (root == null) {
            return;
        }

        JsonObject profilesJson = root.getAsJsonObject("profiles");
        if (profilesJson == null) {
            return;
        }

        for (Map.Entry<String, JsonElement> entry : profilesJson.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            Profile profile = Profile.fromJson(entry.getKey(), entry.getValue().getAsJsonObject());
            if (profile != null) {
                this.profiles.put(entry.getKey(), profile);
            }
        }
    }

    private static final class Profile implements Comparable<Profile> {
        private final String key;
        private String displayName;
        private int vendor;
        private int product;
        private int productVersion;
        private String guid;
        private final EnumMap<GamepadDigitalInput, PhysicalInput> buttons = new EnumMap<>(GamepadDigitalInput.class);

        private Profile(String key, String displayName, int vendor, int product, int productVersion, String guid) {
            this.key = key;
            this.displayName = displayName;
            this.vendor = vendor;
            this.product = product;
            this.productVersion = productVersion;
            this.guid = guid;
        }

        private static Profile fromIdentity(GamepadIdentity identity) {
            return new Profile(identity.key(), identity.displayName(), identity.vendor(), identity.product(), identity.productVersion(), identity.guid());
        }

        @Nullable
        private static Profile fromJson(String key, JsonObject json) {
            Profile profile = new Profile(
                key,
                string(json, "displayName", "SDL Controller"),
                integer(json, "vendor"),
                integer(json, "product"),
                integer(json, "productVersion"),
                string(json, "guid", "")
            );

            JsonObject buttonsJson = json.getAsJsonObject("buttons");
            if (buttonsJson == null) {
                return profile;
            }

            for (Map.Entry<String, JsonElement> entry : buttonsJson.entrySet()) {
                GamepadDigitalInput button = parseInput(entry.getKey());
                PhysicalInput input = entry.getValue().isJsonObject() ? PhysicalInput.fromJson(entry.getValue().getAsJsonObject()) : null;
                if (button != null && input != null) {
                    profile.buttons.put(button, input);
                }
            }
            return profile;
        }

        private void refreshIdentity(GamepadIdentity identity) {
            this.displayName = identity.displayName();
            this.vendor = identity.vendor();
            this.product = identity.product();
            this.productVersion = identity.productVersion();
            this.guid = identity.guid();
        }

        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("displayName", this.displayName);
            json.addProperty("vendor", this.vendor);
            json.addProperty("product", this.product);
            json.addProperty("productVersion", this.productVersion);
            json.addProperty("guid", this.guid);

            JsonObject buttonsJson = new JsonObject();
            for (Map.Entry<GamepadDigitalInput, PhysicalInput> entry : this.buttons.entrySet()) {
                buttonsJson.add(entry.getKey().name(), entry.getValue().toJson());
            }
            json.add("buttons", buttonsJson);
            return json;
        }

        @Override
        public int compareTo(Profile other) {
            return this.key.compareTo(other.key);
        }
    }

    private record PhysicalInput(String type, int index, int value) {
        private static PhysicalInput from(RawGamepadInput rawInput) {
            return new PhysicalInput(rawInput.type(), rawInput.index(), rawInput.value());
        }

        @Nullable
        private static PhysicalInput fromJson(JsonObject json) {
            String type = string(json, "type", "");
            if (type.isBlank()) {
                return null;
            }
            return new PhysicalInput(type, integer(json, "index"), integer(json, "value"));
        }

        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", this.type);
            json.addProperty("index", this.index);
            json.addProperty("value", this.value);
            return json;
        }

        private RawGamepadInput toRawInput() {
            return new RawGamepadInput(this.type, this.index, this.value);
        }
    }

    @Nullable
    private static GamepadDigitalInput parseInput(String name) {
        return GamepadDigitalInput.fromSerializedName(name);
    }

    private static String string(JsonObject json, String name, String fallback) {
        JsonElement element = json.get(name);
        return element == null || !element.isJsonPrimitive() ? fallback : element.getAsString();
    }

    private static int integer(JsonObject json, String name) {
        JsonElement element = json.get(name);
        return element == null || !element.isJsonPrimitive() ? 0 : element.getAsInt();
    }
}

