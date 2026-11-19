package net.jr.screens.options;

import net.jr.api.client.local.Client;
import net.jr.api.client.ui.DslScreen;
import net.jr.api.client.ui.UiScreenFile;
import net.jr.client.components.widgets.Button;
import net.jr.client.runtime.session.DisconnectHandler;
import net.jr.registry.ModUi;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.options.*;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.telemetry.TelemetryInfoScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;

public class OptionsScreen extends Screen implements DslScreen, HasGamemasterPermissionReaction {

    private final Screen lastScreen;
    private final Options options;
    private final boolean inWorld;

    public OptionsScreen(Screen lastScreen, Options options, boolean inWorld) {
        super(Component.literal(""));
        this.lastScreen = lastScreen;
        this.options = options;
        this.inWorld = inWorld;
    }

    @Override
    public void onGamemasterPermissionChanged(boolean hasGamemasterPermission) {
        this.minecraft.gui.setScreen(new net.minecraft.client.gui.screens.options.OptionsScreen(this.lastScreen, this.minecraft.options, true));
    }

    @Override
    public UiScreenFile dslFile() {
        return ModUi.OPTIONS_SCN;
    }

    protected void primaryClientInit() {
        if (!Client.current().isPrimary()){
            return;
        }

        new Button(this.dsl("telemetry")).onPress(button ->  {this.minecraft.gui.setScreen(new TelemetryInfoScreen(this, this.options));});
        new Button(this.dsl("credits_and_attribution")).onPress(button -> {this.minecraft.gui.setScreen(new CreditsAndAttributionScreen(this));});
        new Button(this.dsl("language")).onPress(button -> {this.minecraft.gui.setScreen(new LanguageSelectScreen(this, this.minecraft.options, this.minecraft.getLanguageManager()));});
        new Button(this.dsl("resourcepack")).onPress(button ->  {this.minecraft.gui.setScreen(new PackSelectionScreen(
                this.minecraft.getResourcePackRepository(),
                this::applyPacks,
                this.minecraft.getResourcePackDirectory(),
                Component.translatable("resourcePack.title")
        ));});
    }

    @Override
    protected void init() {
        primaryClientInit();

        new Button(this.dsl("graphics")).onPress(button -> {this.minecraft.gui.setScreen(new VideoSettingsScreen(this ,this.minecraft, this.minecraft.options));});
        new Button(this.dsl("sounds")).onPress(button -> {this.minecraft.gui.setScreen(new SoundOptionsScreen(this, this.minecraft.options));});

        new Button(this.dsl("chat")).onPress(button -> {this.minecraft.gui.setScreen(new ChatOptionsScreen(this, this.minecraft.options ));});
        new Button(this.dsl("controls")).onPress(button -> this.minecraft.gui.setScreen(new ControlsScreen(this, this.minecraft.options)));
        new Button(this.dsl("skinCustomisation")).onPress(button -> {this.minecraft.gui.setScreen(new SkinCustomizationScreen(this , this.minecraft.options));});

        new Button(this.dsl("accessibility")).onPress(button ->  {this.minecraft.gui.setScreen(new AccessibilityOptionsScreen(this, this.options));});
        new Button(this.dsl("interface")).onPress(button ->
                this.minecraft.gui.setScreen(new InterfaceOptionsScreen(this, this.options)));
        new Button(this.dsl("gameplay")).onPress(button ->
                this.minecraft.gui.setScreen(new GameplayOptionsScreen(this, this.options)));


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
    public void onClose() {
        this.minecraft.gui.setScreen(this.lastScreen);
    }

    public Screen getLastScreen() {
        return this.lastScreen;
    }

    private void applyPacks(PackRepository packRepository) {
        this.options.updateResourcePacks(packRepository);
        this.minecraft.gui.setScreen(this);
    }

}
