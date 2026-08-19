package net.jr.client.runtime.bridge;

import javax.annotation.Nullable;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.ui.LocalScreenTransitionHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;

public final class GuiScreenAccess {
    private GuiScreenAccess() {
    }

    @Nullable
    public static Screen screen(Gui gui) {
        return ClientRuntime.INSTANCE.slots().slot(slotId()).screenState().screen();
    }

    public static void setScreen(Gui gui, @Nullable Screen screen) {
        int slotId = slotId();
        ClientRuntime.INSTANCE.slots().slot(slotId).screenState().setScreen(screen);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            LocalScreenTransitionHandler.onSlotScreenChanged(minecraft, slotId, screen);
        }
    }

    private static int slotId() {
        Integer slotId = SlotScope.idOrNull();
        return slotId != null ? slotId : 0;
    }
}
