package net.jr.client.runtime.options;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import javax.annotation.Nullable;
import net.jr.util.JsonFileStore;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;
import net.jr.client.runtime.state.OptionsState;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

public final class PlayerOptionsStorage {
    private static final String FILE_NAME = "player_options.json";

    private final LocalClientSlotRegistry slots;
    private boolean loaded;
    @Nullable
    private Path filePath;

    public PlayerOptionsStorage(LocalClientSlotRegistry slots) {
        this.slots = slots;
    }

    public void ensureLoaded(Minecraft minecraft) {
        if (this.loaded) {
            return;
        }

        this.filePath = minecraft.gameDirectory.toPath().resolve(FILE_NAME);
        JsonObject root = JsonFileStore.read(this.filePath);
        if (root != null) {
            for (LocalClientSlot slot : this.slots.all()) {
                JsonElement element = root.get(playerKey(slot.id()));
                if (element != null && element.isJsonObject()) {
                    this.read(slot.optionsState(), element.getAsJsonObject());
                }
            }
        }

        this.loaded = true;
        this.save();
    }

    public void save() {
        if (!this.loaded || this.filePath == null) {
            return;
        }

        JsonObject root = new JsonObject();
        for (LocalClientSlot slot : this.slots.all()) {
            root.add(playerKey(slot.id()), this.write(slot.optionsState()));
        }
        JsonFileStore.write(this.filePath, root);
    }

    private void read(OptionsState state, JsonObject player) {
        state.setGuiScale(integer(player, "gui_scale", state.requestedGuiScale()));
        state.setFov(integer(player, "fov", state.fov().get()));
        state.setBrightness(decimal(player, "brightness", state.brightness().get()));
        state.setCameraType(cameraType(string(player, "camera_type", cameraTypeId(state.cameraType()))));
        state.setSensitivity(decimal(player, "sensitivity", state.sensitivity().get()));
        state.setMouseWheelSensitivity(decimal(player, "mouse_wheel_sensitivity", state.mouseWheelSensitivity().get()));
        state.setRawMouseInput(bool(player, "raw_mouse_input", state.rawMouseInput().get()));
        state.setAllowCursorChanges(bool(player, "allow_cursor_changes", state.allowCursorChanges().get()));
        state.setInvertMouseX(bool(player, "invert_mouse_x", state.invertMouseX().get()));
        state.setInvertMouseY(bool(player, "invert_mouse_y", state.invertMouseY().get()));
        state.setDiscreteMouseScroll(bool(player, "discrete_mouse_scroll", state.discreteMouseScroll().get()));
        state.setToggleCrouch(bool(player, "toggle_crouch", state.toggleCrouch().get()));
        state.setToggleSprint(bool(player, "toggle_sprint", state.toggleSprint().get()));
        state.setToggleAttack(bool(player, "toggle_attack", state.toggleAttack().get()));
        state.setToggleUse(bool(player, "toggle_use", state.toggleUse().get()));
        state.setSprintWindow(integer(player, "sprint_window", state.sprintWindow().get()));
        state.setAutoJump(bool(player, "auto_jump", state.autoJump().get()));
        state.setOperatorItemsTab(bool(player, "operator_items_tab", state.operatorItemsTab().get()));
    }

    private JsonObject write(OptionsState state) {
        JsonObject player = new JsonObject();
        player.addProperty("gui_scale", state.requestedGuiScale());
        player.addProperty("fov", state.fov().get());
        player.addProperty("brightness", state.brightness().get());
        player.addProperty("camera_type", cameraTypeId(state.cameraType()));
        player.addProperty("sensitivity", state.sensitivity().get());
        player.addProperty("mouse_wheel_sensitivity", state.mouseWheelSensitivity().get());
        player.addProperty("raw_mouse_input", state.rawMouseInput().get());
        player.addProperty("allow_cursor_changes", state.allowCursorChanges().get());
        player.addProperty("invert_mouse_x", state.invertMouseX().get());
        player.addProperty("invert_mouse_y", state.invertMouseY().get());
        player.addProperty("discrete_mouse_scroll", state.discreteMouseScroll().get());
        player.addProperty("toggle_crouch", state.toggleCrouch().get());
        player.addProperty("toggle_sprint", state.toggleSprint().get());
        player.addProperty("toggle_attack", state.toggleAttack().get());
        player.addProperty("toggle_use", state.toggleUse().get());
        player.addProperty("sprint_window", state.sprintWindow().get());
        player.addProperty("auto_jump", state.autoJump().get());
        player.addProperty("operator_items_tab", state.operatorItemsTab().get());
        return player;
    }

    private static String playerKey(int slotId) {
        return "player_" + (slotId + 1);
    }

    private static int integer(JsonObject json, String key, int fallback) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static double decimal(JsonObject json, String key, double fallback) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsDouble();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsBoolean();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static String string(JsonObject json, String key, String fallback) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsString();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static CameraType cameraType(String value) {
        return switch (value) {
            case "third_person_back" -> CameraType.THIRD_PERSON_BACK;
            case "third_person_front" -> CameraType.THIRD_PERSON_FRONT;
            default -> CameraType.FIRST_PERSON;
        };
    }

    private static String cameraTypeId(CameraType cameraType) {
        return switch (cameraType) {
            case THIRD_PERSON_BACK -> "third_person_back";
            case THIRD_PERSON_FRONT -> "third_person_front";
            default -> "first_person";
        };
    }
}
