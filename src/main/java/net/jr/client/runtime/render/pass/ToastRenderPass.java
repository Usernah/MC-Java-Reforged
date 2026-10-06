package net.jr.client.runtime.render.pass;

import java.util.Objects;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.render.state.ToastStateScope;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.context.InstanceExecution;
import net.jr.client.runtime.viewport.GuiViewportScope;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.ToastManager;

public final class ToastRenderPass {
    private ToastRenderPass() {
    }

    public static void update(ToastManager manager) {
        Objects.requireNonNull(manager, "manager");
        ToastStateScope.bootstrapPrimary();

        for (ClientInstance client : ClientRuntime.INSTANCE.clients().all()) {
            InstanceExecution.runForClient(client.slotId(), () -> {
                try (ToastStateScope.PassScope ignoredToasts = ToastStateScope.enterLocalPass()) {
                    manager.update();
                }
            });
        }

        try (ToastStateScope.PassScope ignoredToasts = ToastStateScope.enterGlobalPass()) {
            manager.update();
        }
    }

    public static void extract(ToastManager manager, GuiGraphicsExtractor graphics) {
        Objects.requireNonNull(manager, "manager");
        Objects.requireNonNull(graphics, "graphics");
        ToastStateScope.bootstrapPrimary();

        for (ClientInstance client : ClientRuntime.INSTANCE.clients().all()) {
            int slotId = client.slotId();
            if (!ClientRuntime.INSTANCE.viewports().hasViewport(slotId)) {
                continue;
            }
            InstanceExecution.runForClient(slotId, () -> {
                try (
                    GuiViewportScope ignoredViewport = GuiViewportScope.enter(
                        graphics,
                        ClientRuntime.INSTANCE.viewports().viewport(slotId)
                    );
                    ToastStateScope.PassScope ignoredToasts = ToastStateScope.enterLocalPass()
                ) {
                    manager.extractRenderState(graphics);
                }
            });
        }

        try (ToastStateScope.PassScope ignoredToasts = ToastStateScope.enterGlobalPass()) {
            manager.extractRenderState(graphics);
        }
    }
}
