package net.jr.client.ui.hint;

import net.jr.client.ui.hint.provider.ControlHintProvider;
import net.jr.client.ui.hint.render.ControlHintRenderer;
import net.jr.client.ui.hint.model.ControlHintRequest;
import net.jr.client.ui.hint.model.ResolvedControlHint;
import net.jr.screens.controller.ControllerMenuCaptureAware;
import net.jr.client.ui.hint.provider.ContainerControlHintProvider;
import net.jr.client.ui.hint.provider.HudControlHintProvider;
import net.jr.client.ui.hint.provider.PauseMenuControlHintProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ControlHintPipeline {
    private static final CopyOnWriteArrayList<ControlHintProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    static {
        registerProvider(new ContainerControlHintProvider());
        registerProvider(new PauseMenuControlHintProvider());
        registerProvider(new HudControlHintProvider());
    }

    private ControlHintPipeline() {
    }

    public static void registerProvider(ControlHintProvider provider) {
        PROVIDERS.addIfAbsent(Objects.requireNonNull(provider, "provider"));
    }

    public static void renderHud(GuiGraphicsExtractor gui) {
        Minecraft minecraft = Minecraft.getInstance();
        ControlHintContext context = ControlHintContext.hud(minecraft);
        if (context.screen() != null || context.player() == null) {
            return;
        }

        renderInternal(context, gui);
    }

    public static void renderScreen(Screen screen, GuiGraphicsExtractor gui) {
        renderInternal(ControlHintContext.screen(Minecraft.getInstance(), screen), gui);
    }

    private static void renderInternal(ControlHintContext context, GuiGraphicsExtractor gui) {
        if (context.screen() instanceof ControllerMenuCaptureAware captureAware
            && captureAware.javareforged$isCapturingControllerBinding()) {
            return;
        }

        List<ControlHintRequest> collected = new ArrayList<>();
        for (ControlHintProvider provider : PROVIDERS) {
            if (provider.supports(context)) {
                collected.addAll(provider.buildHints(context));
            }
        }

        if (collected.isEmpty()) {
            return;
        }

        Set<ControlHintRequest> orderedUnique = new LinkedHashSet<>(collected);
        List<ResolvedControlHint> resolved = ControlHintResolver.resolve(context, List.copyOf(orderedUnique));
        if (resolved.isEmpty()) {
            return;
        }

        ControlHintRenderer.render(context, gui, resolved);
    }
}
