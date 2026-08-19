package net.jr.client.ui;

import net.jr.api.client.ui.dsl.UiCompiledDocument;
import net.jr.api.client.ui.dsl.UiCompiledDocument.ButtonDefinition;
import net.jr.client.ui.layout.UILayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;
import java.util.Objects;

public final class UiWidget {
    private UiWidget() {
    }

    public static final class Button extends net.jr.client.components.widgets.Button implements AutoCloseable {
        private final String uiId;
        private final UILayout host;
        private final ButtonDefinition definition;
        private final List<UiElement.ElementRenderable> renderables;
        private final UiCompiledDocument.DataContext widgetContext;

        public Button(
            String id,
            UILayout currentScreen,
            net.jr.client.components.widgets.Button.OnPress onPress
        ) {
            this(prepare(id, currentScreen), currentScreen, onPress);
        }

        private Button(
            Prepared prepared,
            UILayout currentScreen,
            net.jr.client.components.widgets.Button.OnPress onPress
        ) {
            super(
                prepared.x(),
                prepared.y(),
                prepared.width(),
                prepared.height(),
                Objects.requireNonNull(onPress, "onPress"),
                null
            );
            this.uiId = prepared.definition().id();
            this.host = currentScreen;
            this.definition = prepared.definition();
            this.renderables = UiElement.renderables(this.definition.renderables());
            this.widgetContext = this::resolveData;
            this.setRenderer(this::renderDefinition);
            this.host.attachUiWidget(this);
        }

        public String uiId() {
            return this.uiId;
        }

        public ButtonDefinition definition() {
            return this.definition;
        }

        private void renderDefinition(
            GuiGraphicsExtractor graphics,
            net.minecraft.client.gui.components.AbstractWidget ignored,
            int mouseX,
            int mouseY,
            float partialTick
        ) {
            boolean scissor = this.definition.scissor().resolve(this.widgetContext);
            if (scissor) {
                graphics.enableScissor(
                    this.getX(),
                    this.getY(),
                    this.getX() + this.getWidth(),
                    this.getY() + this.getHeight()
                );
            }
            try {
                for (UiElement.ElementRenderable renderable : this.renderables) {
                    renderable.render(graphics, this.widgetContext, this.getX(), this.getY());
                }
            } finally {
                if (scissor) {
                    graphics.disableScissor();
                }
            }
        }

        @Override
        public void close() {
            this.cancelInteraction();
            for (UiElement.ElementRenderable renderable : this.renderables) {
                renderable.close();
            }
        }

        private Object resolveData(List<String> path) {
            if (path.isEmpty() || !path.getFirst().equals("this")) {
                return this.host.uiData().resolve(path);
            }
            if (path.size() != 2) {
                throw new IllegalArgumentException(
                    "Unknown widget data: ${" + String.join(".", path) + "}"
                );
            }
            return switch (path.get(1)) {
                case "x" -> this.getX();
                case "y" -> this.getY();
                case "w", "width" -> this.getWidth();
                case "h", "height" -> this.getHeight();
                case "focused" -> this.isFocused();
                case "hovered" -> this.isHovered();
                case "pressed" -> this.isPressed();
                case "active" -> this.isActive();
                default -> throw new IllegalArgumentException(
                    "Unknown widget data: ${" + String.join(".", path) + "}"
                );
            };
        }

        private static Prepared prepare(String id, UILayout host) {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(host, "currentScreen");
            ButtonDefinition definition = host.uiDocument().requireButton(id);
            UiCompiledDocument.DataContext context = host.uiData();
            return new Prepared(
                definition,
                Math.round(definition.x().resolve(context)),
                Math.round(definition.y().resolve(context)),
                Math.round(definition.width().resolve(context)),
                Math.round(definition.height().resolve(context))
            );
        }

        private record Prepared(ButtonDefinition definition, int x, int y, int width, int height) {
        }
    }
}
