package net.jr.client.ui.hint;

import net.jr.client.ui.hint.provider.ControlHintProvider;
import net.jr.client.ui.hint.model.ControlHintRequest;
import net.jr.client.ui.hint.model.ResolvedControlHint;
import net.jr.client.ui.navigation.ControllerBindingCaptureHost;
import net.jr.client.ui.hint.provider.ContainerControlHintProvider;
import net.jr.client.ui.hint.provider.HudControlHintProvider;
import net.jr.client.ui.hint.provider.PauseMenuControlHintProvider;
import net.jr.client.ui.hint.provider.TabNavigationControlHintProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ControlHintPipeline {
    private static final CopyOnWriteArrayList<ControlHintProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    static {
        registerProvider(new TabNavigationControlHintProvider());
        registerProvider(new ContainerControlHintProvider());
        registerProvider(new PauseMenuControlHintProvider());
        registerProvider(new HudControlHintProvider());
    }

    private ControlHintPipeline() {
    }

    public static void registerProvider(ControlHintProvider provider) {
        PROVIDERS.addIfAbsent(Objects.requireNonNull(provider, "provider"));
    }

    @Nullable
    public static Result resolveHud() {
        Minecraft minecraft = Minecraft.getInstance();
        ControlHintContext context = ControlHintContext.hud(minecraft);
        if (context.screen() != null || context.player() == null) {
            return null;
        }
        return resolveInternal(context);
    }

    @Nullable
    public static Result resolveScreen(Screen screen) {
        return resolveInternal(ControlHintContext.screen(Minecraft.getInstance(), screen));
    }

    @Nullable
    private static Result resolveInternal(ControlHintContext context) {
        if (!context.isGamepadMode()) {
            return null;
        }

        if (context.screen() instanceof ControllerBindingCaptureHost captureHost
            && captureHost.isCapturingControllerBinding()) {
            return null;
        }

        List<ControlHintRequest> collected = new ArrayList<>();
        for (ControlHintProvider provider : PROVIDERS) {
            if (provider.supports(context)) {
                collected.addAll(provider.buildHints(context));
            }
        }

        if (collected.isEmpty()) {
            return null;
        }

        Set<ControlHintRequest> orderedUnique = new LinkedHashSet<>(collected);
        List<ResolvedControlHint> resolved = ControlHintResolver.resolve(context, List.copyOf(orderedUnique));
        if (resolved.isEmpty()) {
            return null;
        }
        return new Result(context, resolved);
    }

    public record Result(ControlHintContext context, List<ResolvedControlHint> hints) {
        public Result {
            hints = List.copyOf(hints);
        }
    }
}
