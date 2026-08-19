package net.jr.client.ui.hint;

import net.jr.Java_reforged;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = Java_reforged.MODID, value = Dist.CLIENT)
public final class ControlHintScreenEvents {
    private ControlHintScreenEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        ControlHintPipeline.renderScreen(event.getScreen(), event.getGuiGraphics());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHudRender(RenderGuiEvent.Post event) {
        ControlHintPipeline.renderHud(event.getGuiGraphics());
    }
}
