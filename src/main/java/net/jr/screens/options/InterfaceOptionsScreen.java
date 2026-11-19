package net.jr.screens.options;

import java.util.ArrayList;
import java.util.List;
import net.jr.api.client.local.Client;
import net.jr.api.client.split.SplitScreen;
import net.jr.client.runtime.options.OptionsStateResolver;
import net.jr.client.runtime.state.OptionsState;
import net.jr.client.ui.presentation.SplitOrientationOption;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

public final class InterfaceOptionsScreen extends OptionsSubScreen {
    private static final Component TITLE = Component.translatable("options.java_reforged.interface");
    private AbstractWidget interfaceModeWidget;

    public InterfaceOptionsScreen(Screen lastScreen, Options options) {
        super(lastScreen, options, TITLE);
    }

    @Override
    protected void addOptions() {
        OptionsState state = OptionsStateResolver.current();
        boolean primary = Client.current().isPrimary();
        List<OptionInstance<?>> entries = new ArrayList<>();
        if (primary) {
            entries.add(state.bottomHudScale());
        }
        entries.add(state.splitBottomHudScale());
        entries.add(state.guiScale());
        if (primary) {
            entries.add(state.interfaceMode());
            entries.add(SplitOrientationOption.INSTANCE);
            entries.add(state.showPlayerNameHud());
        }
        entries.add(state.showPlayerModelHud());
        entries.add(state.hudTransparency());
        entries.add(state.showControlHints());
        entries.add(state.showLocatorBar());
        if (primary) {
            entries.add(state.showWorldPlayerNames());
        }
        entries.add(state.attackIndicator());
        if (primary) {
            entries.add(this.options.showAutosaveIndicator());
        }
        this.list.addSmall(entries.toArray(OptionInstance<?>[]::new));
        this.interfaceModeWidget = primary ? this.list.findOption(state.interfaceMode()) : null;
        this.updateInterfaceModeAvailability();
    }

    @Override
    public void tick() {
        super.tick();
        this.updateInterfaceModeAvailability();
    }

    private void updateInterfaceModeAvailability() {
        if (this.interfaceModeWidget != null) {
            this.interfaceModeWidget.active = !SplitScreen.isActive();
        }
    }
}
