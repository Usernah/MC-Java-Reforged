package net.jr.client.components.widgets;

import net.jr.client.components.elements.VisualElementsInterface;
import net.jr.client.components.elements.VisualState;
import net.jr.client.components.interfaces.IWidgetSoundHandler;
import net.jr.client.components.navigation.UiAction;
import net.jr.client.components.navigation.UiActionHandler;
import net.jr.client.input.InputApi;
import net.jr.client.runtime.input.InputActionHandler;
import net.jr.client.sound.action.InputActionSounds;
import net.jr.client.ui.dsl.DslNode;
import net.jr.client.ui.dsl.DslElement;
import net.jr.client.ui.dsl.UiCompiledDocument.EntryDefinition;
import net.jr.client.ui.dsl.UiDslKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class JavaBaseWidget extends AbstractWidget
    implements UiActionHandler, VisualElementsInterface, DslElement {

    private static final IWidgetSoundHandler DEFAULT_SOUND_HANDLER = (handler, widget, event) -> {
        switch (event) {
            case FOCUS -> InputActionSounds.playHover();
            case PRESS -> {
                InputActionHandler.suppressNextScreenTransitionSound();
                InputActionSounds.playClick();
            }
            case HOVER, UNHOVER, RELEASE -> {
            }
        }
    };

    private boolean pressed = false;
    private boolean wasClicked = false;

    protected IWidgetSoundHandler soundHandler = DEFAULT_SOUND_HANDLER;
    private final VisualState visualState;
    private final DslNode dsl;
    private final List<EntryDefinition> dslRenderables;
    private final boolean dslScissor;
    private float animProgress = 0.0f;

    public JavaBaseWidget(int x, int y, int width, int height, Component message) {
        this(x, y, width, height, message, null);
    }

    protected JavaBaseWidget(DslNode dsl, Component message) {
        this(
            Math.round(dsl.context().originX() + dsl.number("x")),
            Math.round(dsl.context().originY() + dsl.number("y")),
            Math.round(dsl.number("w")),
            Math.round(dsl.number("h")),
            message,
            dsl
        );
    }

    private JavaBaseWidget(
        int x,
        int y,
        int width,
        int height,
        Component message,
        DslNode dsl
    ) {
        super(x, y, width, height, message);
        this.visualState = new VisualState(x, y, width, height);
        this.dsl = dsl;
        this.dslScissor = dsl != null && dsl.booleanOr("scissor", false);
        this.dslRenderables = dsl == null ? List.of() : dsl.block("renderables");
    }

    protected final void completeDslConstruction(UiDslKind<?> kind) {
        if (this.dsl == null) {
            throw new IllegalStateException("This widget was not created from a DSL node");
        }
        this.dsl.finish(kind, this);
    }

    @Override
    public VisualState visualState() {
        return this.visualState;
    }

    @Override
    public void setX(float x) {
        this.visualState.setX(x);
        super.setX(Math.round(x));
    }

    @Override
    public void setY(float y) {
        this.visualState.setY(y);
        super.setY(Math.round(y));
    }

    @Override
    public void setWidth(float width) {
        this.visualState.setWidth(width);
        this.width = Math.round(width);
    }

    @Override
    public void setHeight(float height) {
        this.visualState.setHeight(height);
        this.height = Math.round(height);
    }

    @Override
    public void setPosition(float x, float y) {
        this.setX(x);
        this.setY(y);
    }

    @Override
    public void setSize(float width, float height) {
        this.setWidth(width);
        this.setHeight(height);
    }

    @Override
    public void setBounds(float x, float y, float width, float height) {
        this.setPosition(x, y);
        this.setSize(width, height);
    }

    @Override
    public void setBaseX(float x) {
        this.visualState.setBaseX(x);
    }

    @Override
    public void setBaseY(float y) {
        this.visualState.setBaseY(y);
    }

    @Override
    public void setBaseWidth(float width) {
        this.visualState.setBaseWidth(width);
    }

    @Override
    public void setBaseHeight(float height) {
        this.visualState.setBaseHeight(height);
    }

    @Override
    public void setBasePosition(float x, float y) {
        this.visualState.setBasePosition(x, y);
    }

    @Override
    public void setBaseSize(float width, float height) {
        this.visualState.setBaseSize(width, height);
    }

    @Override
    public void setBaseBounds(float x, float y, float width, float height) {
        this.visualState.setBaseBounds(x, y, width, height);
    }

    @Override
    public void setVisible(boolean visible) {
        this.visualState.setVisible(visible);
        this.visible = visible;
        if (!visible) {
            this.cancelInteraction();
        }
    }

    @Override
    public void setBaseVisible(boolean visible) {
        this.visualState.setBaseVisible(visible);
    }

    @Override
    public void resetVisualState() {
        this.setBounds(this.baseX(), this.baseY(), this.baseWidth(), this.baseHeight());
        this.setRotation(this.baseRotation());
        this.setAlpha(this.baseAlpha());
        this.setVisible(this.baseVisible());
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.dsl == null || this.dslRenderables.isEmpty()) {
            return;
        }
        if (this.dslScissor) {
            guiGraphics.enableScissor(
                this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight()
            );
        }
        try {
            this.dsl.context().extractBlock(
                this.dslRenderables,
                guiGraphics,
                this::resolveDslValue,
                mouseX,
                mouseY,
                partialTick,
                this.getX(),
                this.getY()
            );
        } finally {
            if (this.dslScissor) {
                guiGraphics.disableScissor();
            }
        }
    }

    public final void closeDsl() {
        this.cancelInteraction();
    }

    protected Object resolveDslProperty(String name) {
        throw new IllegalArgumentException("Unknown widget DSL data: ${this." + name + "}");
    }

    private Object resolveDslValue(List<String> path) {
        if (path.isEmpty() || !path.getFirst().equals("this")) {
            return this.dsl.data().resolve(path);
        }
        if (path.size() != 2) {
            throw new IllegalArgumentException("Unknown widget DSL data: ${" + String.join(".", path) + "}");
        }
        return switch (path.get(1)) {
            case "x" -> this.getX();
            case "y" -> this.getY();
            case "w", "width" -> this.getWidth();
            case "h", "height" -> this.getHeight();
            case "focused" -> this.isFocused();
            case "hovered" -> this.isHovered();
            case "pressed" -> this.isPressed();
            case "active" -> this.active;
            case "visible" -> this.visible;
            case "alpha" -> this.getAlpha();
            default -> this.resolveDslProperty(path.get(1));
        };
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        boolean insideBounds = mouseX >= this.getX()
                && mouseY >= this.getY()
                && mouseX < this.getX() + this.getWidth()
                && mouseY < this.getY() + this.getHeight();
        if (!this.isActive() || !this.isValidClickButton(event.buttonInfo()) || !insideBounds) {
            return false;
        }

        this.playDownSound(Minecraft.getInstance().getSoundManager());
        this.setPressed(true);
        this.onClick(mouseX, mouseY);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.isPressed()) {
            this.setPressed(false);
            if (soundHandler != null) {
                soundHandler.handle(Minecraft.getInstance().getSoundManager(), this, IWidgetSoundHandler.Event.RELEASE);
            }

            if (this.active && this.visible
                    && event.x() >= this.getX()
                    && event.y() >= this.getY()
                    && event.x() < this.getX() + this.getWidth()
                    && event.y() < this.getY() + this.getHeight()) {
                this.wasClicked = true;
                return true;
            }
        }
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
    }

    public void onClick(double mouseX, double mouseY) {
    }

    public boolean isClicked() {
        if (this.wasClicked) {
            this.wasClicked = false;
            return true;
        }
        return false;
    }

    public void setSoundHandler(IWidgetSoundHandler handler) {
        this.soundHandler = handler == null ? DEFAULT_SOUND_HANDLER : handler;
    }

    public boolean isPressed() {
        return this.pressed;
    }

    public void setPressed(boolean pressed) {
        this.pressed = pressed;
    }

    public void cancelInteraction() {
        this.pressed = false;
        this.wasClicked = false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
    }

    @Override
    public void playDownSound(@NotNull SoundManager pHandler) {

        if (!InputApi.isGamepadMode() && soundHandler != null) {
            soundHandler.handle(pHandler, this, IWidgetSoundHandler.Event.PRESS);
        }
    }

    public int getBaseX() {
        return Math.round(this.baseX());
    }

    public void setBaseX(int baseX) {
        this.setBaseX((float) baseX);
    }

    public int getBaseY() {
        return Math.round(this.baseY());
    }

    public void setBaseY(int baseY) {
        this.setBaseY((float) baseY);
    }

    public int getBaseWidth() {
        return Math.round(this.baseWidth());
    }

    public void setBaseWidth(int baseWidth) {
        this.setBaseWidth((float) baseWidth);
    }

    public int getBaseHeight() {
        return Math.round(this.baseHeight());
    }

    public void setBaseHeight(int baseHeight) {
        this.setBaseHeight((float) baseHeight);
    }

    public void setBaseBounds(int baseX, int baseY, int baseWidth, int baseHeight) {
        this.setBaseBounds((float) baseX, (float) baseY, (float) baseWidth, (float) baseHeight);
    }

    @Override
    public void setX(int x) {
        this.visualState.setX(x);
        super.setX(x);
    }

    @Override
    public void setY(int y) {
        this.visualState.setY(y);
        super.setY(y);
    }

    @Override
    public void setWidth(int width) {
        this.visualState.setWidth(width);
        super.setWidth(width);
    }

    @Override
    public void setHeight(int height) {
        this.visualState.setHeight(height);
        super.setHeight(height);
    }

    @Override
    public void setSize(int width, int height) {
        this.setWidth(width);
        this.setHeight(height);
    }

    @Override
    public void setAlpha(float alpha) {
        this.visualState.setAlpha(alpha);
        super.setAlpha(alpha);
    }

    @Override
    public float getAlpha() {
        return this.visualState.alpha();
    }

    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            this.cancelInteraction();
        }
    }

    public float getAnimProgress() {
        return this.animProgress;
    }

    public void setAnimProgress(float animProgress) {
        this.animProgress = animProgress;
    }

    @Override
    public boolean handleUiAction(UiAction action) {
        return false;
    }
}
