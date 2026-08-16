package net.jr.client.runtime.render.pass;

import java.util.Objects;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.bridge.ToastManagerStateAccess;
import net.jr.client.runtime.context.LocalClientScope;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.slot.LocalClientSlots;
import net.jr.client.runtime.ui.LocalScreenManager;
import net.jr.client.runtime.viewport.GuiViewportScope;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.ToastManager;

public final class ToastRenderPass {
    private ToastRenderPass() {
    }

    public static void update(ToastManager manager) {
        Objects.requireNonNull(manager, "manager");

        ToastManagerStateAccess.bootstrapPrimary();

        for (int slotId = 0; slotId < LocalClientSlots.MAX_SLOTS; slotId++) {
            LocalClientSlot slot = ClientRuntime.INSTANCE.slots().slot(slotId);

            if (!slot.connected()) {
                continue;
            }

            try (
                    LocalClientScope ignoredClient = LocalClientScope.enter(slot);
                    ToastManagerStateAccess.PassScope ignoredToasts =
                            ToastManagerStateAccess.enterLocalPass()
            ) {
                manager.update();
            }
        }

        try (
                ToastManagerStateAccess.PassScope ignoredToasts =
                        ToastManagerStateAccess.enterGlobalPass()
        ) {
            manager.update();
        }
    }

    public static void extract(
            ToastManager manager,
            GuiGraphicsExtractor graphics
    ) {
        Objects.requireNonNull(manager, "manager");
        Objects.requireNonNull(graphics, "graphics");

        ToastManagerStateAccess.bootstrapPrimary();

        if (LocalScreenManager.slotUiPassOwnsScreens()) {
            extractLocalViewports(manager, graphics);
        } else {
            extractBootstrapPrimary(manager, graphics);
        }

        try (
                ToastManagerStateAccess.PassScope ignoredToasts =
                        ToastManagerStateAccess.enterGlobalPass()
        ) {
            manager.extractRenderState(graphics);
        }
    }

    private static void extractLocalViewports(
            ToastManager manager,
            GuiGraphicsExtractor graphics
    ) {
        for (
                LocalClientSlot slot :
                ClientRuntime.INSTANCE.slots().visibleSlots()
        ) {
            if (!LocalScreenManager.slotUiPassCanRender(slot)) {
                continue;
            }

            try (
                    LocalClientScope ignoredClient = LocalClientScope.enter(slot);
                    GuiViewportScope ignoredViewport =
                            GuiViewportScope.enter(graphics);
                    ToastManagerStateAccess.PassScope ignoredToasts =
                            ToastManagerStateAccess.enterLocalPass()
            ) {
                manager.extractRenderState(graphics);
            }
        }
    }

    private static void extractBootstrapPrimary(
            ToastManager manager,
            GuiGraphicsExtractor graphics
    ) {
        LocalClientSlot primary = ClientRuntime.INSTANCE.primarySlot();

        try (
                LocalClientScope ignoredClient =
                        LocalClientScope.enter(primary);
                ToastManagerStateAccess.PassScope ignoredToasts =
                        ToastManagerStateAccess.enterLocalPass()
        ) {
            manager.extractRenderState(graphics);
        }
    }
}