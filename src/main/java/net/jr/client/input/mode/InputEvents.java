package net.jr.client.input.mode;

import net.jr.Java_reforged;
import net.jr.client.input.InputApi;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Java_reforged.MODID, value = Dist.CLIENT)
public final class InputEvents {
    private InputEvents() {
    }

    @SubscribeEvent
    public static void onKeyboardInput(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS || event.getAction() == GLFW.GLFW_REPEAT) {
            InputApi.markKeyboardMouseInput();
        }
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getAction() == GLFW.GLFW_PRESS || event.getAction() == GLFW.GLFW_RELEASE) {
            InputApi.markKeyboardMouseInput();
        }
    }

    @SubscribeEvent
    public static void onMouseScrolled(InputEvent.MouseScrollingEvent event) {
        if (event.getScrollDeltaX() != 0.0D || event.getScrollDeltaY() != 0.0D) {
            InputApi.markKeyboardMouseInput();
        }
    }

    @SubscribeEvent
    public static void onScreenMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        InputApi.markKeyboardMouseInput();
    }
}

