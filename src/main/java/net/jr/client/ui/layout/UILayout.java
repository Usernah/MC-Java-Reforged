package net.jr.client.ui.layout;

import net.jr.api.client.resource.Asset;
import net.jr.api.client.ui.UiFile;
import net.jr.api.client.ui.UiFileType;
import net.jr.api.client.ui.UiScreenFile;
import net.jr.api.client.ui.dsl.UiCompiledDocument;
import net.jr.api.client.ui.dsl.UiRenderLayer;
import net.jr.client.ui.UiElement;
import net.jr.client.ui.dsl.UiDataContext;
import net.jr.client.ui.dsl.UiDocumentManager;
import net.jr.client.components.navigation.UiAction;
import net.jr.client.components.navigation.UiActionHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public abstract class UILayout extends AbstractContainerEventHandler implements Renderable, NarratableEntry, UiActionHandler {
    protected final Minecraft minecraft = Minecraft.getInstance();

    protected Screen screen;
    protected UILayoutRuntime runtime;
    protected int width;
    protected int height;

    private final UiFile uiFile;
    private final UiDataContext uiData = new UiDataContext();
    private final List<AbstractWidget> widgets = new ArrayList<>();
    private final EnumMap<UiRenderLayer, UiElement.Layer> uiLayers = new EnumMap<>(UiRenderLayer.class);
    private UiCompiledDocument compiledDocument;
    private GuiEventListener pointerCapture;
    private int pointerCaptureButton = -1;
    private TransitionType transitionType = TransitionType.NONE;
    private int transitionDuration = 350;
    private int slideDistance = 50;
    private boolean initialized;

    protected UILayout(UiScreenFile uiFile) {
        this(uiFile, UiFileType.SCREEN);
    }

    protected UILayout(UiFile uiFile, UiFileType expectedType) {
        this.uiFile = Objects.requireNonNull(uiFile, "uiFile");
        if (uiFile.type() != expectedType) {
            throw new IllegalArgumentException(
                "Expected a " + expectedType + " UI file, received " + uiFile.type()
            );
        }
    }

    final void initialize(Screen screen, UILayoutRuntime runtime, int width, int height) {
        this.disposeUiContent();
        this.screen = screen;
        this.runtime = runtime;
        this.width = width;
        this.height = height;
        this.compiledDocument = UiDocumentManager.getInstance().requireCompiled(this.uiFile);
        this.initLayout();
        this.initialized = true;
    }

    final void resizeIfNeeded(int width, int height) {
        if (!this.initialized) {
            this.initialize(this.screen, this.runtime, width, height);
            return;
        }
        if (this.width != width || this.height != height) {
            this.width = width;
            this.height = height;
            this.resizeLayout(width, height);
        }
    }

    protected abstract void initLayout();

    /**
     * Updates layout geometry without destroying its widgets, visual elements
     * or other stateful resources. Subclasses may reposition Java-owned
     * components here when their geometry depends on the screen size.
     */
    protected void resizeLayout(int width, int height) {
    }

    protected void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    }

    protected void extractForeground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public final void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderUiLayer(UiRenderLayer.BACKGROUND, graphics);
        this.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.nextStratum();

        this.renderUiLayer(UiRenderLayer.CONTENT, graphics);
        for (AbstractWidget widget : this.widgets) {
            widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
        this.extractForeground(graphics, mouseX, mouseY, partialTick);
        graphics.nextStratum();

        this.renderUiLayer(UiRenderLayer.FOREGROUND, graphics);
    }

    protected final <T extends AbstractWidget> T addWidget(T widget) {
        this.widgets.add(widget);
        return widget;
    }

    public final <T extends AbstractWidget> T attachUiWidget(T widget) {
        return this.addWidget(widget);
    }

    public final UiElement.Layer attachUiLayer(UiElement.Layer layer) {
        Objects.requireNonNull(layer, "layer");
        UiElement.Layer previous = this.uiLayers.putIfAbsent(layer.type(), layer);
        if (previous != null) {
            throw new IllegalStateException(
                "UILayout already has an instantiated " + layer.type() + " render layer"
            );
        }
        return layer;
    }

    private void renderUiLayer(UiRenderLayer type, GuiGraphicsExtractor graphics) {
        UiElement.Layer layer = this.uiLayers.get(type);
        if (layer != null) {
            layer.render(graphics);
        }
    }

    public final UiFile uiFile() {
        return this.uiFile;
    }

    public final UiCompiledDocument uiDocument() {
        if (this.compiledDocument == null) {
            throw new IllegalStateException("UILayout has not loaded its compiled UI document");
        }
        return this.compiledDocument;
    }

    public final UiDataContext uiData() {
        return this.uiData;
    }

    protected final void exposeNumber(String path, Supplier<? extends Number> value) {
        this.uiData.exposeNumber(path, value);
    }

    protected final void exposeBoolean(String path, Supplier<Boolean> value) {
        this.uiData.exposeBoolean(path, value);
    }

    protected final void exposeText(String path, Supplier<String> value) {
        this.uiData.exposeText(path, value);
    }

    protected final void exposeAsset(String path, Supplier<Asset> value) {
        this.uiData.exposeAsset(path, value);
    }

    public final List<AbstractWidget> widgets() {
        return Collections.unmodifiableList(this.widgets);
    }

    public void tick() {
    }

    public void onOpen() {
    }

    public void onClose() {
    }

    final void disposeUiContent() {
        this.cancelPointerCapture();
        for (UiElement.Layer layer : this.uiLayers.values()) {
            layer.close();
        }
        for (AbstractWidget widget : this.widgets) {
            if (widget instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception exception) {
                    throw new IllegalStateException("Could not close UI widget renderables", exception);
                }
            }
        }
        this.uiLayers.clear();
        this.widgets.clear();
    }

    public UILayout transition(TransitionType type, int durationMs) {
        this.transitionType = type == null ? TransitionType.NONE : type;
        this.transitionDuration = Math.max(0, durationMs);
        return this;
    }

    public UILayout fade(int durationMs) {
        return this.transition(TransitionType.FADE, durationMs);
    }

    public UILayout slideLeft(int durationMs) {
        return this.transition(TransitionType.SLIDE_LEFT, durationMs);
    }

    public UILayout slideRight(int durationMs) {
        return this.transition(TransitionType.SLIDE_RIGHT, durationMs);
    }

    public UILayout slideUp(int durationMs) {
        return this.transition(TransitionType.SLIDE_UP, durationMs);
    }

    public UILayout slideDown(int durationMs) {
        return this.transition(TransitionType.SLIDE_DOWN, durationMs);
    }

    public UILayout slideDistance(int distance) {
        this.slideDistance = Math.max(0, distance);
        return this;
    }

    final TransitionType transitionType() {
        return this.transitionType;
    }

    final int transitionDuration() {
        return this.transitionDuration;
    }

    final int slideDistance() {
        return this.slideDistance;
    }

    protected final UILayoutRuntime runtime() {
        if (this.runtime == null) {
            throw new IllegalStateException("UILayout has not been attached to a runtime");
        }
        return this.runtime;
    }

    protected final Screen hostScreen() {
        if (this.screen == null) {
            throw new IllegalStateException("UILayout has not been attached to a screen");
        }
        return this.screen;
    }

    protected final void switchLayout(UILayout target, TransitionType type, int durationMs) {
        this.runtime().switchLayout(target, type, durationMs);
    }

    protected final void showOverlay(UIOverlay overlay) {
        this.runtime().showOverlay(overlay);
    }

    protected final void closeOverlay() {
        this.runtime().closeOverlay();
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return this.widgets;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= 0.0D && mouseY >= 0.0D && mouseX < this.width && mouseY < this.height;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        var child = this.getChildAt(event.x(), event.y());
        if (child.isEmpty()) {
            return false;
        }

        GuiEventListener listener = child.get();
        boolean handled = listener.mouseClicked(event, doubleClick);
        boolean stillInteractive = this.runtime == null || this.runtime.interactiveLayout() == this;
        if (handled && stillInteractive && listener.shouldTakeFocusAfterInteraction()) {
            this.setFocused(listener);
            if (event.button() == 0) {
                this.setDragging(true);
            }
            this.capturePointer(listener, event.button());
        } else if (handled && !stillInteractive) {
            this.cancelCapturedInteraction(listener);
        }
        // Preserve vanilla ContainerEventHandler semantics: hitting a child
        // consumes the event even when that child declines the button.
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.pointerCapture != null && event.button() == this.pointerCaptureButton) {
            GuiEventListener captured = this.pointerCapture;
            this.pointerCapture = null;
            this.pointerCaptureButton = -1;
            if (event.button() == 0) {
                this.setDragging(false);
            }
            return captured.mouseReleased(event);
        }

        if (event.button() == 0 && this.isDragging()) {
            this.setDragging(false);
            return this.getFocused() != null && this.getFocused().mouseReleased(event);
        }
        return false;
    }

    private void capturePointer(GuiEventListener listener, int button) {
        if (this.pointerCapture != null && this.pointerCapture != listener) {
            this.cancelCapturedInteraction(this.pointerCapture);
        }
        this.pointerCapture = listener;
        this.pointerCaptureButton = button;
    }

    private void cancelPointerCapture() {
        if (this.pointerCapture != null) {
            this.cancelCapturedInteraction(this.pointerCapture);
        }
        this.pointerCapture = null;
        this.pointerCaptureButton = -1;
        this.setDragging(false);
    }

    final void cancelInteractions() {
        this.cancelPointerCapture();
        for (AbstractWidget widget : this.widgets) {
            if (widget instanceof net.jr.client.components.widgets.JavaBaseWidget javaWidget) {
                javaWidget.cancelInteraction();
            }
        }
    }

    private void cancelCapturedInteraction(GuiEventListener listener) {
        if (listener instanceof net.jr.client.components.widgets.JavaBaseWidget widget) {
            widget.cancelInteraction();
        }
    }

    @Override
    public boolean handleUiAction(UiAction action) {
        return this.getFocused() instanceof UiActionHandler handler && handler.handleUiAction(action);
    }

    @Nullable
    public AbstractWidget focusedWidget() {
        return this.getFocused() instanceof AbstractWidget widget ? widget : null;
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
    }
}
