package net.jr.client.runtime.options;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import javax.annotation.Nullable;

import net.jr.client.runtime.slot.ClientSlots;
import net.jr.client.runtime.slot.Slots;
import net.jr.util.JsonFileStore;
import net.jr.client.runtime.state.OptionsState;
import net.jr.client.runtime.state.CraftingMode;
import net.jr.client.runtime.state.LegacyBehaviorMode;
import net.jr.client.ui.presentation.UiSizeMode;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;

public final class PlayerOptionsStorage {
    private static final String FILE_NAME = "player_options.json";

    private final ClientSlots slots;
    private boolean loaded;
    @Nullable
    private Path filePath;

    public PlayerOptionsStorage(ClientSlots slots) {
        this.slots = slots;
    }

    public void ensureLoaded(Minecraft minecraft) {
        if (this.loaded) {
            return;
        }

        this.filePath = minecraft.gameDirectory.toPath().resolve(FILE_NAME);
        JsonObject root = JsonFileStore.read(this.filePath);
        if (root != null) {
            for (Slots slot : this.slots.all()) {
                JsonElement element = root.get(playerKey(slot.id()));
                if (element != null && element.isJsonObject()) {
                    this.read(slot, element.getAsJsonObject());
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
        for (Slots slot : this.slots.all()) {
            root.add(playerKey(slot.id()), this.write(slot));
        }
        JsonFileStore.write(this.filePath, root);
    }

    private void read(Slots slot, JsonObject player) {
        OptionsState state = slot.optionsState();
        state.setGuiScale(integer(player, "gui_scale", state.requestedGuiScale()));
        if (slot.isPrimary()) {
            state.setInterfaceMode(UiSizeMode.byName(string(
                    player,
                    "interface_mode",
                    state.interfaceMode().get().serializedName()
            )));
        }
        state.setBottomHudScale(integer(player, "bottom_hud_scale", state.bottomHudScale().get()));
        state.setSplitBottomHudScale(integer(
                player,
                "split_bottom_hud_scale",
                state.splitBottomHudScale().get()
        ));
        state.setFov(integer(player, "fov", state.fov().get()));
        state.setBrightness(decimal(player, "brightness", state.brightness().get()));
        if (slot.isPrimary()) {
            state.setLegacyGamma(decimal(player, "legacy_gamma", state.legacyGamma().get()));
        }
        state.setHudTransparency(decimal(player, "hud_transparency", state.hudTransparency().get()));
        state.setShowPlayerModelHud(bool(
                player,
                "show_player_model_hud",
                state.showPlayerModelHud().get()
        ));
        if (slot.isPrimary()) {
            state.setShowPlayerNameHud(bool(player, "show_player_name_hud", state.showPlayerNameHud().get()));
            state.setShowWorldPlayerNames(bool(player, "show_world_player_names", state.showWorldPlayerNames().get()));
        }
        state.setShowControlHints(bool(player, "show_control_hints", state.showControlHints().get()));
        state.setShowLocatorBar(bool(player, "show_locator_bar", state.showLocatorBar().get()));
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
        state.setAttackIndicator(attackIndicator(string(
                player,
                "attack_indicator",
                state.attackIndicator().get().name()
        )));
        state.setMainHand(mainHand(string(player, "main_hand", state.mainHand().get().name())));
        state.setCraftingMode(CraftingMode.byName(string(
                player,
                "crafting_mode",
                state.craftingMode().get().serializedName()
        )));
        state.setFlightMode(LegacyBehaviorMode.byName(string(
                player,
                "flight_mode",
                state.flightMode().get().serializedName()
        )));
        state.setConstructionMode(LegacyBehaviorMode.byName(string(
                player,
                "construction_mode",
                state.constructionMode().get().serializedName()
        )));
        state.setControllerCursorSensitivity(decimal(
                player,
                "controller_cursor_sensitivity",
                state.controllerCursorSensitivity().get()
        ));
        state.setControllerCameraSensitivity(decimal(
                player,
                "controller_camera_sensitivity",
                state.controllerCameraSensitivity().get()
        ));
        state.setControllerSpyglassSensitivity(decimal(
                player,
                "controller_spyglass_sensitivity",
                state.controllerSpyglassSensitivity().get()
        ));
        state.setInvertControllerMovementY(bool(
                player,
                "invert_controller_movement_y",
                state.invertControllerMovementY().get()
        ));
        state.setInvertControllerCameraX(bool(
                player,
                "invert_controller_camera_x",
                state.invertControllerCameraX().get()
        ));
        state.setInvertControllerCameraY(bool(
                player,
                "invert_controller_camera_y",
                state.invertControllerCameraY().get()
        ));
    }

    private JsonObject write(Slots slot) {
        OptionsState state = slot.optionsState();
        JsonObject player = new JsonObject();
        player.addProperty("gui_scale", state.requestedGuiScale());
        if (slot.isPrimary()) {
            player.addProperty("interface_mode", state.interfaceMode().get().serializedName());
        }
        player.addProperty("bottom_hud_scale", state.bottomHudScale().get());
        player.addProperty("split_bottom_hud_scale", state.splitBottomHudScale().get());
        player.addProperty("fov", state.fov().get());
        player.addProperty("brightness", state.brightness().get());
        if (slot.isPrimary()) {
            player.addProperty("legacy_gamma", state.legacyGamma().get());
        }
        player.addProperty("hud_transparency", state.hudTransparency().get());
        player.addProperty("show_player_model_hud", state.showPlayerModelHud().get());
        if (slot.isPrimary()) {
            player.addProperty("show_player_name_hud", state.showPlayerNameHud().get());
            player.addProperty("show_world_player_names", state.showWorldPlayerNames().get());
        }
        player.addProperty("show_control_hints", state.showControlHints().get());
        player.addProperty("show_locator_bar", state.showLocatorBar().get());
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
        player.addProperty("attack_indicator", state.attackIndicator().get().name());
        player.addProperty("main_hand", state.mainHand().get().name());
        player.addProperty("crafting_mode", state.craftingMode().get().serializedName());
        player.addProperty("flight_mode", state.flightMode().get().serializedName());
        player.addProperty("construction_mode", state.constructionMode().get().serializedName());
        player.addProperty("controller_cursor_sensitivity", state.controllerCursorSensitivity().get());
        player.addProperty("controller_camera_sensitivity", state.controllerCameraSensitivity().get());
        player.addProperty("controller_spyglass_sensitivity", state.controllerSpyglassSensitivity().get());
        player.addProperty("invert_controller_movement_y", state.invertControllerMovementY().get());
        player.addProperty("invert_controller_camera_x", state.invertControllerCameraX().get());
        player.addProperty("invert_controller_camera_y", state.invertControllerCameraY().get());
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

    private static AttackIndicatorStatus attackIndicator(String value) {
        try {
            return AttackIndicatorStatus.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return AttackIndicatorStatus.CROSSHAIR;
        }
    }

    private static HumanoidArm mainHand(String value) {
        try {
            return HumanoidArm.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return HumanoidArm.RIGHT;
        }
    }
}
