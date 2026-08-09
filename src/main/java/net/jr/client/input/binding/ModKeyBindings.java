package net.jr.client.input.binding;

import com.mojang.blaze3d.platform.InputConstants;
import net.jr.Java_reforged;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@EventBusSubscriber(modid = Java_reforged.MODID, value = Dist.CLIENT)
public final class ModKeyBindings {
    public static final KeyMapping.Category CATEGORY_GENERAL = category("general");
    public static final KeyMapping.Category CATEGORY_UI = category("ui");
    public static final KeyMapping.Category CATEGORY_GAMEPLAY = category("gameplay");

    public static final KeyMapping EXAMPLE_KEY = key("example_action", InputConstants.KEY_G, CATEGORY_GENERAL);
    public static final KeyMapping BATTLE_TEST_TOGGLE = key("battle_test_toggle", InputConstants.KEY_F8, CATEGORY_GAMEPLAY);
    public static final KeyMapping UI_CONFIRM = mouse("ui_confirm", GLFW.GLFW_MOUSE_BUTTON_LEFT);
    public static final KeyMapping UI_BACK = key("ui_back", InputConstants.KEY_ESCAPE, CATEGORY_UI);
    public static final KeyMapping UI_ALTERNATE = mouse("ui_alternate", GLFW.GLFW_MOUSE_BUTTON_RIGHT);
    public static final KeyMapping UI_NAV_UP = key("ui_nav_up", InputConstants.KEY_UP, CATEGORY_UI);
    public static final KeyMapping UI_NAV_DOWN = key("ui_nav_down", InputConstants.KEY_DOWN, CATEGORY_UI);
    public static final KeyMapping UI_NAV_LEFT = key("ui_nav_left", InputConstants.KEY_LEFT, CATEGORY_UI);
    public static final KeyMapping UI_NAV_RIGHT = key("ui_nav_right", InputConstants.KEY_RIGHT, CATEGORY_UI);
    public static final KeyMapping UI_QUICK_MOVE =
        mouse("ui_quick_move", KeyModifier.SHIFT, GLFW.GLFW_MOUSE_BUTTON_LEFT);
    public static final KeyMapping UI_TAKE_ALL = key("ui_take_all", InputConstants.KEY_R, CATEGORY_UI);
    public static final KeyMapping UI_STORE_ALL = key("ui_store_all", GLFW.GLFW_KEY_UNKNOWN, CATEGORY_UI);
    public static final KeyMapping GAMEPLAY_HOTBAR_PREV =
        key("gameplay_hotbar_prev", InputConstants.KEY_LBRACKET, CATEGORY_GAMEPLAY);
    public static final KeyMapping GAMEPLAY_HOTBAR_NEXT =
        key("gameplay_hotbar_next", InputConstants.KEY_RBRACKET, CATEGORY_GAMEPLAY);

    private ModKeyBindings() {}

    private static KeyMapping.Category category(String path) {
        return new KeyMapping.Category(Identifier.fromNamespaceAndPath(Java_reforged.MODID, path));
    }

    private static KeyMapping key(String name, int code, KeyMapping.Category category) {
        return new KeyMapping(translationKey(name), InputConstants.Type.KEYSYM, code, category);
    }

    private static KeyMapping mouse(String name, int button) {
        return mouse(name, KeyModifier.NONE, button);
    }

    private static KeyMapping mouse(String name, KeyModifier modifier, int button) {
        return new KeyMapping(
            translationKey(name),
            KeyConflictContext.GUI,
            modifier,
            InputConstants.Type.MOUSE,
            button,
            CATEGORY_UI
        );
    }

    private static String translationKey(String name) {
        return "key." + Java_reforged.MODID + "." + name;
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY_GENERAL);
        event.registerCategory(CATEGORY_UI);
        event.registerCategory(CATEGORY_GAMEPLAY);
        for (Field field : ModKeyBindings.class.getFields()) {
            if (Modifier.isPublic(field.getModifiers())
                && Modifier.isStatic(field.getModifiers())
                && Modifier.isFinal(field.getModifiers())
                && field.getType() == KeyMapping.class) {
                try {
                    event.register((KeyMapping) field.get(null));
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Could not register " + field.getName(), exception);
                }
            }
        }
    }

    /**
     * Repairs the invalid binding written by early 26.2 ports where quick move
     * lost its Shift modifier and became indistinguishable from UI confirm.
     */
    public static void repairLegacyQuickMoveBinding(Minecraft minecraft) {
        if (UI_QUICK_MOVE.getKeyModifier() != KeyModifier.NONE
            || UI_QUICK_MOVE.getDefaultKeyModifier() != KeyModifier.SHIFT
            || !UI_QUICK_MOVE.getKey().equals(UI_CONFIRM.getKey())) {
            return;
        }

        UI_QUICK_MOVE.setKeyModifierAndCode(KeyModifier.SHIFT, UI_QUICK_MOVE.getKey());
        KeyMapping.resetMapping();
        minecraft.options.save();
        Java_reforged.LOGGER.info("Reparado binding UI_QUICK_MOVE: restaurado modificador Shift.");
    }
}
