package net.jr.experimental_features.dsl_editor;

import net.jr.Java_reforged;
import net.jr.client.input.binding.ModKeyBindings;
import net.jr.client.runtime.ClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.jr.client.ui.dsl.ScreenEditorHooks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Java_reforged.MODID, value = Dist.CLIENT)
public final class DslEditorKeyHandler {
    static {
        // Register the experimental tool from this package; no production class imports it.
        ScreenEditorHooks.setToggleHandler(DslEditorKeyHandler::toggleOnCurrentScreen);
    }

    private DslEditorKeyHandler() {
    }

    private static boolean toggleOnCurrentScreen(Screen screen) {
        if (ScreenEditorHooks.activeFor(screen)) {
            closeEditor();
        } else {
            ScreenEditorHooks.install(new DslEditorOverlay(screen));
            applyLayoutTransition();
        }
        return true;
    }

    static void closeEditor() {
        ScreenEditorHooks.clear();
        applyLayoutTransition();
    }

    private static void applyLayoutTransition() {
        ClientRuntime.INSTANCE.viewportResize().applyLayoutTransition(
            Minecraft.getInstance()
        );
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        // Drain the legacy key mapping: GUI keypresses are handled by the raw
        // keyboard hook, because Minecraft does not reliably enqueue key mapping
        // clicks while a Screen is active. Never toggle again here.
        while (ModKeyBindings.DSL_EDITOR.consumeClick()) { }
    }
}
