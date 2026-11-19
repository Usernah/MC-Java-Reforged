package net.jr.screens.options;

import net.jr.client.runtime.options.OptionsStateResolver;
import net.jr.client.runtime.state.OptionsState;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

public final class GameplayOptionsScreen extends OptionsSubScreen {
    private static final Component TITLE = Component.translatable("options.java_reforged.gameplay");

    public GameplayOptionsScreen(Screen lastScreen, Options options) {
        super(lastScreen, options, TITLE);
    }

    @Override
    protected void addOptions() {
        OptionsState state = OptionsStateResolver.current();
        this.list.addSmall(
                state.craftingMode(),
                state.flightMode(),
                state.constructionMode(),
                state.autoJump(),
                state.mainHand(),
                state.operatorItemsTab()
        );
    }
}
