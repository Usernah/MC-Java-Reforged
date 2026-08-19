package net.jr.client.runtime.render.pass;

import java.util.Objects;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.bridge.ToastManagerStateAccess;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.viewport.GuiViewportScope;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.ToastManager;

public final class ToastRenderPass {
    private ToastRenderPass() {
    }

    public static void update(ToastManager manager) {
        Objects.requireNonNull(manager, "manager");
        ToastManagerStateAccess.bootstrapPrimary();

        for (LocalClient client : ClientRuntime.INSTANCE.clients().all()) {
            LocalClientExecution.runForClient(client.slotId(), () -> {
                try (ToastManagerStateAccess.PassScope ignoredToasts = ToastManagerStateAccess.enterLocalPass()) {
                    manager.update();
                }
            });
        }

        try (ToastManagerStateAccess.PassScope ignoredToasts = ToastManagerStateAccess.enterGlobalPass()) {
            manager.update();
        }
    }

    public static void extract(ToastManager manager, GuiGraphicsExtractor graphics) {
        Objects.requireNonNull(manager, "manager");
        Objects.requireNonNull(graphics, "graphics");
        ToastManagerStateAccess.bootstrapPrimary();

        for (LocalClient client : ClientRuntime.INSTANCE.clients().all()) {
            int slotId = client.slotId();
            if (!ClientRuntime.INSTANCE.viewports().hasViewport(slotId)) {
                continue;
            }
            LocalClientExecution.runForClient(slotId, () -> {
                try (
                    GuiViewportScope ignoredViewport = GuiViewportScope.enter(
                        graphics,
                        ClientRuntime.INSTANCE.viewports().viewport(slotId)
                    );
                    ToastManagerStateAccess.PassScope ignoredToasts = ToastManagerStateAccess.enterLocalPass()
                ) {
                    manager.extractRenderState(graphics);
                }
            });
        }

        try (ToastManagerStateAccess.PassScope ignoredToasts = ToastManagerStateAccess.enterGlobalPass()) {
            manager.extractRenderState(graphics);
        }
    }
}
