package net.jr.client.runtime.bridge;

import javax.annotation.Nullable;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotExecution;
import net.jr.client.runtime.ui.SlotScreenTransitionHandler;
import net.jr.mixin.accessors.GuiAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;

public final class GuiScreenAccess {
    private GuiScreenAccess() {
    }

    @Nullable
    public static Screen screen(Gui gui) {
        if (SlotExecution.isMinecraftBootstrapping()) {
            return ((GuiAccessor)gui).javaReforged$getRawScreen();
        }
        return ClientRuntime.INSTANCE.slots().slot(slotId()).screenState().screen();
    }

    public static void setScreen(Gui gui, @Nullable Screen screen) {
        if (SlotExecution.isMinecraftBootstrapping()) {
            ((GuiAccessor)gui).javaReforged$setRawScreen(screen);
            return;
        }
        int slotId = slotId();
        var slot = ClientRuntime.INSTANCE.slots().slot(slotId);
        Screen previous = slot.screenState().screen();
        slot.screenState().setScreen(screen);
        slot.playerModelHudState().onScreenChanged(previous, screen);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            SlotScreenTransitionHandler.onSlotScreenChanged(minecraft, slotId, screen);
        }
    }

    private static int slotId() {
        return SlotExecution.currentSlotId();
    }
}
