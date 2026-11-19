package net.jr.client.compat.create;

import java.lang.reflect.Field;
import java.util.List;
import net.jr.api.client.render.gui.GuiRenderObjects;
import net.neoforged.neoforge.client.gui.GuiLayer;

/** Bridges Create's GUI layers into Java Reforged's per-player HUD registry. */
public final class CreateHudRenderObjects {
    private static boolean registered;

    private CreateHudRenderObjects() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        List<GuiLayer> layers = List.of(
            layer("com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer", "OVERLAY"),
            layer("com.simibubi.create.content.equipment.armor.RemainingAirOverlay", "OVERLAY"),
            layer("com.simibubi.create.content.trains.TrainHUD", "OVERLAY"),
            layer("com.simibubi.create.content.trains.track.TrackPlacementOverlay", "INSTANCE"),
            layer("com.simibubi.create.CreateClient", "VALUE_SETTINGS_HANDLER"),
            layer("com.simibubi.create.content.redstone.link.controller.LinkedControllerClientHandler", "OVERLAY"),
            layer("com.simibubi.create.content.equipment.blueprint.BlueprintOverlayRenderer", "OVERLAY"),
            layer("com.simibubi.create.CreateClient", "SCHEMATIC_HANDLER"),
            layer("com.simibubi.create.content.equipment.toolbox.ToolboxHandlerClient", "OVERLAY")
        );
        GuiRenderObjects.registerHud(context -> {
            for (GuiLayer layer : layers) {
                layer.render(context.graphics(), context.deltaTracker());
            }
        });
        registered = true;
    }

    private static GuiLayer layer(String className, String fieldName) {
        try {
            Class<?> type = Class.forName(className);
            Field field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (GuiLayer)field.get(null);
        } catch (ReflectiveOperationException | ClassCastException exception) {
            throw new IllegalStateException(
                "No se pudo registrar la capa HUD de Create " + className + "." + fieldName,
                exception
            );
        }
    }
}
