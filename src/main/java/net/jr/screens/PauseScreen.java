package net.jr.screens;

import net.jr.api.client.local.Client;
import net.jr.api.client.ui.DslScreen;
import net.jr.api.client.ui.UiScreenFile;
import net.jr.client.components.widgets.Button;
import net.jr.client.runtime.session.DisconnectHandler;
import net.jr.registry.ModUi;
import net.jr.screens.options.OptionsScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;

public class PauseScreen extends Screen implements DslScreen {
    private final boolean showPauseMenu;

    public PauseScreen(boolean showPauseMenu) {
        this(
                Component.translatable(showPauseMenu ? "menu.game" : "menu.paused"),
                showPauseMenu
        );
    }

    public PauseScreen(Component title) {
        this(title, true);
    }

    private PauseScreen(Component title, boolean showPauseMenu) {
        super(title);
        this.showPauseMenu = showPauseMenu;
    }

    @Override
    public UiScreenFile dslFile() {
        return ModUi.PAUSE_SCN;
    }


    protected void primaryClientInit() {
        if (!Client.current().isPrimary()){
            return;
        }

        new Button(this.dsl("advancements"))
                .onPress(button -> {
                    this.minecraft.gui.setScreen(new AdvancementsScreen(Client.currentOrNull().player().connection.getAdvancements(), this));
                });

        new Button(this.dsl("stats"))
                .onPress(button -> {
                    this.minecraft.gui.setScreen(new StatsScreen(this, Client.currentOrNull().player().getStats()));
                });

    }

    @Override
    protected void init() {
        if (!this.showPauseMenu) {
            return;
        }
        primaryClientInit();

        new Button(this.dsl("return_game"))
                .onPress(button -> {
                    this.minecraft.gui.setScreen((Screen) null);
                    this.minecraft.mouseHandler.grabMouse();
                });

            new Button(this.dsl("options"))
                    .onPress(button -> this.minecraft.gui.setScreen(
                            new OptionsScreen(this, this.minecraft.options, true)
                    ));

        new Button(this.dsl("exit_world"))
                .onPress(button -> DisconnectHandler.disconnectCurrentOrWorld(
                        this.minecraft,
                        ClientLevel.DEFAULT_QUIT_MESSAGE
                ));
    }

    public boolean showsPauseMenu() {
        return this.showPauseMenu;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        this.extractDslBackground(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        this.extractDslContent(graphics, mouseX, mouseY, partialTick);
        this.extractDslForeground(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
