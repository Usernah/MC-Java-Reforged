package net.jr.screens;

import java.util.Objects;
import net.jr.api.client.ui.DslScreen;
import net.jr.api.client.ui.UiScreenFile;
import net.jr.client.ui.LoadingTextProvider;
import net.jr.registry.ModUi;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MessageScreen extends Screen implements DslScreen {
    private static final int TIP_INTERVAL_TICKS = 80;

    private Component title;
    private Component content;
    private boolean error;
    private boolean closeWhenGameplayReady;
    private final LoadingTextProvider loadingTexts =
        new LoadingTextProvider(TIP_INTERVAL_TICKS);

    public MessageScreen(Component title, Component content) {
        super(Objects.requireNonNull(title, ""));
        this.setMessages(title, content);
    }

    public MessageScreen(Component title) {
        super(Objects.requireNonNull(title, ""));
        this.setMessage(title);
    }

    public void setMessage(Component title) {
        this.title = Objects.requireNonNull(title, "");
        this.content = Component.empty();
        this.error = false;
    }

    public void setMessages(Component title, Component content) {
        this.title = Objects.requireNonNull(title, "");
        this.content = Objects.requireNonNull(content, "");
        this.error = true;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public UiScreenFile dslFile() {
        return ModUi.MESSAGE_SCREEN;
    }

    @Override
    protected void init() {
        this.dslData().exposeText("message.title", () -> this.getTitle().getString());
        this.dslData().exposeText("message.content", () -> this.content.getString());
        this.dslData().exposeBoolean("message.error", this::isError);
        this.dslData().exposeText(
            "game.tip",
            () -> this.loadingTexts.current().getString()
        );
    }

    @Override
    public void tick() {
        super.tick();
        this.loadingTexts.tick();
    }

    public boolean isError() {
        return this.error;
    }

    public MessageScreen closeWhenGameplayReady() {
        this.closeWhenGameplayReady = true;
        return this;
    }

    public boolean closesWhenGameplayReady() {
        return this.closeWhenGameplayReady;
    }

    public Component currentTip() {
        return this.loadingTexts.current();
    }

    public int currentTipElapsedTicks() {
        return this.loadingTexts.elapsedTicks();
    }

    @Override
    public void extractBackground(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        this.extractPanorama(graphics, partialTick);
        this.extractBlurredBackground(graphics);
        this.extractMenuBackground(graphics);
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
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
