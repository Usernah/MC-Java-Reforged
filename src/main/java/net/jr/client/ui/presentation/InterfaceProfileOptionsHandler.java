package net.jr.client.ui.presentation;

import java.util.Arrays;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.OptionsList;

public final class InterfaceProfileOptionsHandler {
    private InterfaceProfileOptionsHandler() {
    }

    public static OptionInstance<?>[] extend(Options options, OptionInstance<?>[] original) {
        int slotId = SlotScope.idOrNull() != null ? SlotScope.requireId() : 0;
        OptionInstance<?> playerGuiScale = ClientRuntime.INSTANCE.slots().slot(slotId).optionsState().guiScale();
        OptionInstance<?>[] extended = Arrays.copyOf(original, original.length + 2);
        for (int index = 0; index < original.length; index++) {
            if (original[index] == options.guiScale()) {
                extended[index] = playerGuiScale;
                break;
            }
        }
        extended[original.length] = InterfaceProfileOption.INSTANCE;
        extended[original.length + 1] = SplitOrientationOption.INSTANCE;
        return extended;
    }

    public static void updateSplitLock(OptionsList list) {
        if (list == null) {
            return;
        }
        AbstractWidget widget = list.findOption(InterfaceProfileOption.INSTANCE);
        if (widget == null) {
            return;
        }
        boolean split = UiPresentation.isSplitScreen();
        widget.active = !split;
        if (widget instanceof CycleButton<?> rawButton) {
            @SuppressWarnings("unchecked")
            CycleButton<InterfaceProfile> button = (CycleButton<InterfaceProfile>) rawButton;
            button.setValue(split ? InterfaceProfile.STANDARD : InterfaceProfileOption.INSTANCE.get());
        }
    }
}
