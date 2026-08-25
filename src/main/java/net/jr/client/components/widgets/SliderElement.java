package net.jr.client.components.widgets;

import net.jr.client.components.interfaces.IWidgetRenderer;
import net.jr.client.components.navigation.UiAction;
import net.jr.client.components.navigation.UiActionFeedback;
import net.jr.client.sound.action.WidgetInputSounds;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class SliderElement extends JavaBaseWidget {

    public enum ValueMode {
        NONE,
        NUMERIC,
        LIST
    }

    private final ScrollbarElement scrollbar;

    private ValueMode valueMode =
            ValueMode.NONE;

    private double minValue;
    private double maxValue = 1.0D;
    private double numericValue;
    private double step;

    private final List<String> listValues =
            new ArrayList<>();

    private int selectedIndex;

    private boolean editing;

    private UiActionFeedback lastUiActionFeedback = UiActionFeedback.NONE;

    private Consumer<SliderElement> onChange =
            ignored -> {};

    private IWidgetRenderer renderer;

    public SliderElement(
            int x,
            int y,
            int width,
            int height
    ) {
        super(x, y, width, height, Component.empty());

        this.scrollbar =
                new ScrollbarElement(
                        x,
                        y,
                        width,
                        height
                );

        this.scrollbar.setOrientation(
                ScrollbarElement.Orientation.HORIZONTAL
        );

        this.scrollbar.onPositionChange(
                this::applyScrollbarPosition
        );
        this.scrollbar.setSoundHandler((handler, widget, event) -> {});
    }

    public ValueMode valueMode() {
        return this.valueMode;
    }

    public void setRange(
            double min,
            double max
    ) {
        if (!Double.isFinite(min)
                || !Double.isFinite(max)
                || max <= min) {
            throw new IllegalArgumentException(
                    "Slider range requires finite max > min"
            );
        }

        boolean enteringNumeric =
                this.valueMode != ValueMode.NUMERIC;

        this.valueMode = ValueMode.NUMERIC;

        this.minValue = min;
        this.maxValue = max;

        this.listValues.clear();
        this.selectedIndex = 0;

        if (enteringNumeric) {
            this.numericValue = min;
        } else {
            this.numericValue =
                    Mth.clamp(
                            this.numericValue,
                            min,
                            max
                    );
        }

        this.applyStep();
        this.syncScrollbarFromValue();
    }

    public double min() {
        return this.minValue;
    }

    public double max() {
        return this.maxValue;
    }

    public double value() {
        this.requireMode(ValueMode.NUMERIC);
        return this.numericValue;
    }

    public void setValue(double value) {
        this.requireMode(ValueMode.NUMERIC);

        double previous =
                this.numericValue;

        this.numericValue =
                Mth.clamp(
                        value,
                        this.minValue,
                        this.maxValue
                );

        this.applyStep();
        this.syncScrollbarFromValue();

        if (Double.compare(
                previous,
                this.numericValue
        ) != 0) {
            this.onChange.accept(this);
        }
    }

    public double step() {
        return this.step;
    }

    public void setStep(double step) {
        if (!Double.isFinite(step)
                || step < 0.0D) {
            throw new IllegalArgumentException(
                    "Slider step must be >= 0"
            );
        }

        this.step = step;

        if (this.valueMode == ValueMode.NUMERIC) {
            double previous =
                    this.numericValue;

            this.applyStep();
            this.syncScrollbarFromValue();

            if (Double.compare(
                    previous,
                    this.numericValue
            ) != 0) {
                this.onChange.accept(this);
            }
        }
    }

    public void clearStep() {
        this.step = 0.0D;
    }

    public void setList(String... values) {
        this.setList(
                values == null
                        ? List.of()
                        : Arrays.asList(values)
        );
    }

    public void setList(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(
                    "Slider list cannot be empty"
            );
        }

        this.valueMode = ValueMode.LIST;

        this.listValues.clear();

        for (String value : values) {
            this.listValues.add(
                    value == null ? "" : value
            );
        }

        this.selectedIndex =
                Mth.clamp(
                        this.selectedIndex,
                        0,
                        this.listValues.size() - 1
                );

        this.syncScrollbarFromValue();
    }

    public List<String> values() {
        return Collections.unmodifiableList(
                this.listValues
        );
    }

    public int selectedIndex() {
        this.requireMode(ValueMode.LIST);
        return this.selectedIndex;
    }

    public String selectedValue() {
        this.requireMode(ValueMode.LIST);

        return this.listValues.get(
                this.selectedIndex
        );
    }

    public void setSelectedIndex(int index) {
        this.requireMode(ValueMode.LIST);

        int resolved =
                Mth.clamp(
                        index,
                        0,
                        this.listValues.size() - 1
                );

        if (resolved == this.selectedIndex) {
            return;
        }

        this.selectedIndex = resolved;

        this.syncScrollbarFromValue();
        this.onChange.accept(this);
    }

    public void setSelectedValue(String value) {
        this.requireMode(ValueMode.LIST);

        int index =
                this.listValues.indexOf(value);

        if (index >= 0) {
            this.setSelectedIndex(index);
        }
    }

    public ScrollbarElement scrollbar() {
        return this.scrollbar;
    }

    public boolean isEditing() {
        return this.editing;
    }

    public void setRenderer(IWidgetRenderer renderer) {
        this.renderer = renderer;
    }

    public void onChange(
            Consumer<SliderElement> action
    ) {
        this.onChange =
                action == null
                        ? ignored -> {}
                        : action;
    }

    @Override
    protected void extractWidgetRenderState(
            GuiGraphicsExtractor guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        super.extractWidgetRenderState(
                guiGraphics,
                mouseX,
                mouseY,
                partialTick
        );

        if (this.renderer != null) {
            this.renderer.render(
                    guiGraphics,
                    this,
                    mouseX,
                    mouseY,
                    partialTick
            );
        }

        this.scrollbar.extractRenderState(
                guiGraphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    @Override
    public boolean mouseClicked(
            MouseButtonEvent event,
            boolean doubleClick
    ) {
        if (!this.active || !this.visible()) {
            return false;
        }

        double previous = this.valuePosition();
        boolean handled = this.scrollbar.mouseClicked(
                event,
                doubleClick
        );
        if (handled) {
            WidgetInputSounds.reportPointerClickFeedback(
                    this.valueChangedSince(previous)
                            ? UiActionFeedback.CHAR
                            : UiActionFeedback.NONE
            );
        }
        return handled;
    }

    @Override
    public boolean prefersDirectFocusAction() {
        return true;
    }

    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent event) {
        return this.active && this.visible()
            ? ComponentPath.leaf(this)
            : null;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return this.scrollbar.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(
            MouseButtonEvent event,
            double dragX,
            double dragY
    ) {
        double previous = this.valuePosition();
        boolean handled = this.scrollbar.mouseDragged(
                event,
                dragX,
                dragY
        );
        if (handled && this.valueChangedSince(previous)) {
            WidgetInputSounds.playPointerDragFeedback(UiActionFeedback.CHAR);
        }
        return handled;
    }

    @Override
    public void cancelInteraction() {
        super.cancelInteraction();
        this.scrollbar.cancelInteraction();
        this.editing = false;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);

        if (!focused) {
            this.editing = false;
        }
    }

    @Override
    public boolean handleUiAction(UiAction action) {
        this.lastUiActionFeedback = UiActionFeedback.NONE;
        if (!this.active || !this.visible()) {
            return false;
        }

        if (!this.editing) {
            if (action == UiAction.ACCEPT) {
                this.editing = true;
                this.lastUiActionFeedback = UiActionFeedback.CLICK;
                return true;
            }

            return false;
        }

        if (action == UiAction.ACCEPT
                || action == UiAction.CANCEL
                || action == UiAction.BACK) {
            this.editing = false;
            this.lastUiActionFeedback = UiActionFeedback.CLICK;
            return true;
        }

        boolean horizontal =
                this.scrollbar.orientation()
                        == ScrollbarElement.Orientation.HORIZONTAL;

        if (horizontal) {
            if (action == UiAction.MOVE_LEFT) {
                this.lastUiActionFeedback = this.adjustFromFocus(-1)
                        ? UiActionFeedback.CHAR
                        : UiActionFeedback.NONE;
                return true;
            }

            if (action == UiAction.MOVE_RIGHT) {
                this.lastUiActionFeedback = this.adjustFromFocus(1)
                        ? UiActionFeedback.CHAR
                        : UiActionFeedback.NONE;
                return true;
            }

            if (action == UiAction.MOVE_UP
                    || action == UiAction.MOVE_DOWN) {
                this.editing = false;
                return false;
            }
        } else {
            if (action == UiAction.MOVE_UP) {
                this.lastUiActionFeedback = this.adjustFromFocus(-1)
                        ? UiActionFeedback.CHAR
                        : UiActionFeedback.NONE;
                return true;
            }

            if (action == UiAction.MOVE_DOWN) {
                this.lastUiActionFeedback = this.adjustFromFocus(1)
                        ? UiActionFeedback.CHAR
                        : UiActionFeedback.NONE;
                return true;
            }

            if (action == UiAction.MOVE_LEFT
                    || action == UiAction.MOVE_RIGHT) {
                this.editing = false;
                return false;
            }
        }

        return false;
    }

    @Override
    public UiActionFeedback uiActionFeedback(UiAction action) {
        return this.lastUiActionFeedback;
    }

    private boolean adjustFromFocus(int delta) {
        double previous = this.valuePosition();
        if (this.valueMode == ValueMode.LIST) {
            this.setSelectedIndex(
                    this.selectedIndex + delta
            );

            return this.valueChangedSince(previous);
        }

        if (this.valueMode != ValueMode.NUMERIC) {
            return false;
        }

        double amount =
                this.step > 0.0D
                        ? this.step
                        : (this.maxValue - this.minValue)
                        / 100.0D;

        this.setValue(
                this.numericValue + amount * delta
        );
        return this.valueChangedSince(previous);
    }

    private double valuePosition() {
        return switch (this.valueMode) {
            case NUMERIC -> this.numericValue;
            case LIST -> this.selectedIndex;
            case NONE -> Double.NaN;
        };
    }

    private boolean valueChangedSince(double previous) {
        return !Double.isNaN(previous)
                && Double.compare(previous, this.valuePosition()) != 0;
    }

    private void applyScrollbarPosition(
            double position
    ) {
        if (this.valueMode == ValueMode.NUMERIC) {
            double previous =
                    this.numericValue;

            double raw =
                    this.minValue
                            + position
                            * (this.maxValue - this.minValue);

            this.numericValue =
                    Mth.clamp(
                            raw,
                            this.minValue,
                            this.maxValue
                    );

            this.applyStep();

            if (Double.compare(
                    previous,
                    this.numericValue
            ) != 0) {
                this.onChange.accept(this);
            }

            return;
        }

        if (this.valueMode == ValueMode.LIST) {
            int index =
                    this.listValues.size() <= 1
                            ? 0
                            : (int)Math.round(
                            position
                                    * (this.listValues.size() - 1)
                    );

            index =
                    Mth.clamp(
                            index,
                            0,
                            this.listValues.size() - 1
                    );

            if (index != this.selectedIndex) {
                this.selectedIndex = index;
                this.onChange.accept(this);
            }
        }
    }

    private void applyStep() {
        if (this.step <= 0.0D) {
            return;
        }

        double steps =
                Math.round(
                        (this.numericValue - this.minValue)
                                / this.step
                );

        this.numericValue =
                this.minValue + steps * this.step;

        this.numericValue =
                Mth.clamp(
                        this.numericValue,
                        this.minValue,
                        this.maxValue
                );
    }

    private void syncScrollbarFromValue() {
        if (this.valueMode == ValueMode.NUMERIC) {
            double range =
                    this.maxValue - this.minValue;

            this.scrollbar.setPositionSilently(
                    range <= 0.0D
                            ? 0.0D
                            : (this.numericValue - this.minValue)
                            / range
            );

            return;
        }

        if (this.valueMode == ValueMode.LIST) {
            this.scrollbar.setPositionSilently(
                    this.listValues.size() <= 1
                            ? 0.0D
                            : (double)this.selectedIndex
                            / (this.listValues.size() - 1)
            );
        }
    }

    private void requireMode(ValueMode expected) {
        if (this.valueMode != expected) {
            throw new IllegalStateException(
                    "Slider mode is "
                            + this.valueMode
                            + ", expected "
                            + expected
            );
        }
    }
}
