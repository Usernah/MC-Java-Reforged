package net.jr.client.components.widgets;

import net.jr.client.components.interfaces.IWidgetRenderer;
import net.jr.client.components.interfaces.IWidgetSoundHandler;
import net.jr.client.components.navigation.UiAction;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class Button extends JavaBaseWidget {

    public OnPress onPress;
    private IWidgetRenderer renderer;
    private boolean pressEffectApplied = false;

    // 2. Constructor actualizado con soundHandler
    protected Button(int x, int y, int width, int height, OnPress onPress, IWidgetRenderer renderer) {
        super(x, y, width, height, Component.empty());
        this.onPress = onPress;
        this.renderer = renderer;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        super.extractWidgetRenderState(gui, mouseX, mouseY, partialTick); // IMPORTANTE: Para lógica de sonido
        if (this.renderer != null) {
            this.renderer.render(gui, this, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (!this.active) {
            return;
        }
        if (this.onPress != null) {
            this.onPress.onPress(this);
        }
    }

    @Override
    public void setFocused(boolean focused) {
        if (!this.active && focused) {
            return;
        }
        super.setFocused(focused);
    }

    // --- Getters y Setters ---
    // Eliminamos isPressed() y setPressed() porque ya están en JavaBaseWidget

    public boolean isActive() { return this.active; }
    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            this.cancelInteraction();
        }
    }
    public boolean isPressEffectApplied() { return this.pressEffectApplied; }
    public void setPressEffectApplied(boolean applied) { this.pressEffectApplied = applied; }
    public void setRenderer(IWidgetRenderer renderer) { this.renderer = renderer; }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
        this.defaultButtonNarrationText(pNarrationElementOutput);
    }

    @Override
    public boolean handleUiAction(UiAction action) {
        if (action != UiAction.ACCEPT || !this.isActive()) {
            return false;
        }

        this.playDownSound(net.minecraft.client.Minecraft.getInstance().getSoundManager());
        this.onClick(this.getX() + (this.getWidth() / 2.0), this.getY() + (this.getHeight() / 2.0));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return switch (event.key()) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE ->
                this.handleUiAction(UiAction.ACCEPT);
            default -> false;
        };
    }

    @FunctionalInterface
    public interface OnPress {
        void onPress(Button button);
    }

    // --- BUILDER ---
    public static class Builder {
        private final int x, y, width, height;
        private OnPress onPress = b -> {};
        private IWidgetRenderer renderer;
        private IWidgetSoundHandler soundHandler;
        private boolean active = true;

        public Builder(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public Builder onPress(OnPress onPress) {
            this.onPress = onPress;
            return this;
        }

        public Builder withRenderer(IWidgetRenderer renderer) {
            this.renderer = renderer;
            return this;
        }

        // 5. Método para asignar el sonido desde fuera
        public Builder withSound(IWidgetSoundHandler sound) {
            this.soundHandler = sound;
            return this;
        }

        public Builder setActive(boolean active) {
            this.active = active;
            return this;
        }

        public Button build() {
            // 6. Pasar el soundHandler al constructor
            Button button = new Button(x, y, width, height, onPress, renderer);
            button.setActive(this.active);
            button.setSoundHandler(this.soundHandler);
            return button;
        }
    }
}
