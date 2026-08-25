package net.jr.client.components.widgets;

import net.jr.client.components.interfaces.IWidgetRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;
import java.util.function.Consumer;

public class EditBoxElement extends JavaBaseWidget {
    private String value = "";
    private int maxLength = 32;
    private int cursorPosition;
    private int highlightPosition;
    private boolean editable = true;
    private boolean canLoseFocus = true;
    private Component hint = Component.empty();
    private String suggestion = "";
    private Consumer<String> responder = ignored -> {};
    private IWidgetRenderer renderer;
    private Font font;
    private long focusedTime = Util.getMillis();

    public EditBoxElement(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    public String value() {
        return this.value;
    }

    public void setValue(String value) {
        String next = value == null ? "" : value;
        if (next.length() > this.maxLength) {
            next = next.substring(0, this.maxLength);
        }
        if (Objects.equals(this.value, next)) {
            this.moveCursorToEnd(false);
            return;
        }
        this.value = next;
        this.moveCursorToEnd(false);
        this.responder.accept(this.value);
    }

    public int cursorPosition() {
        return this.cursorPosition;
    }

    public int selectionStart() {
        return Math.min(this.cursorPosition, this.highlightPosition);
    }

    public int selectionEnd() {
        return Math.max(this.cursorPosition, this.highlightPosition);
    }

    public boolean hasSelection() {
        return this.cursorPosition != this.highlightPosition;
    }

    public String selectedText() {
        return this.value.substring(this.selectionStart(), this.selectionEnd());
    }

    public boolean isCursorVisible() {
        return this.canConsumeInput()
                && (Util.getMillis() - this.focusedTime) % 1000L < 500L;
    }

    public boolean canConsumeInput() {
        return this.isActive() && this.visible && this.isFocused() && this.editable;
    }

    public void insertText(String text) {
        if (!this.editable || text == null || text.isEmpty()) {
            return;
        }
        String insertion = text;
        if (this.hasSelection()) {
            this.deleteSelection();
        }
        int available = this.maxLength - this.value.length();
        if (available <= 0) {
            return;
        }
        if (insertion.length() > available) {
            insertion = insertion.substring(0, available);
        }
        this.value = this.value.substring(0, this.cursorPosition)
                + insertion
                + this.value.substring(this.cursorPosition);
        this.cursorPosition += insertion.length();
        this.highlightPosition = this.cursorPosition;
        this.responder.accept(this.value);
    }

    public void deleteChars(int amount) {
        if (!this.editable) {
            return;
        }
        if (this.hasSelection()) {
            this.deleteSelection();
            this.responder.accept(this.value);
            return;
        }
        int from = amount < 0
                ? Math.max(0, this.cursorPosition + amount)
                : this.cursorPosition;
        int to = amount < 0
                ? this.cursorPosition
                : Math.min(this.value.length(), this.cursorPosition + amount);
        if (from == to) {
            return;
        }
        this.value = this.value.substring(0, from) + this.value.substring(to);
        this.cursorPosition = from;
        this.highlightPosition = from;
        this.responder.accept(this.value);
    }

    public void deleteCharsToPos(int position) {
        int target = Math.clamp(position, 0, this.value.length());
        this.deleteChars(target - this.cursorPosition);
    }

    public void deleteWords(int amount) {
        if (!this.editable) {
            return;
        }
        if (this.hasSelection()) {
            this.deleteChars(0);
            return;
        }
        int target = this.cursorPosition;
        int direction = amount < 0 ? -1 : 1;
        for (int i = 0; i < Math.abs(amount); i++) {
            if (direction < 0) {
                while (target > 0 && Character.isWhitespace(this.value.charAt(target - 1))) {
                    target--;
                }
                while (target > 0 && !Character.isWhitespace(this.value.charAt(target - 1))) {
                    target--;
                }
            } else {
                while (target < this.value.length() && Character.isWhitespace(this.value.charAt(target))) {
                    target++;
                }
                while (target < this.value.length() && !Character.isWhitespace(this.value.charAt(target))) {
                    target++;
                }
            }
        }
        this.deleteCharsToPos(target);
    }

    public void moveCursorTo(int position) {
        this.moveCursorTo(position, false);
    }

    public void moveCursorTo(int position, boolean select) {
        this.cursorPosition = Math.clamp(position, 0, this.value.length());
        if (!select) {
            this.highlightPosition = this.cursorPosition;
        }
    }

    public void moveCursorToStart(boolean select) {
        this.moveCursorTo(0, select);
    }

    public void moveCursorToEnd(boolean select) {
        this.moveCursorTo(this.value.length(), select);
    }

    public void selectAll() {
        this.cursorPosition = this.value.length();
        this.highlightPosition = 0;
    }

    public int maxLength() {
        return this.maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = Math.max(0, maxLength);
        if (this.value.length() > this.maxLength) {
            this.value = this.value.substring(0, this.maxLength);
            this.moveCursorToEnd(false);
            this.responder.accept(this.value);
        }
    }

    public boolean isEditable() {
        return this.editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean canLoseFocus() {
        return this.canLoseFocus;
    }

    public void setCanLoseFocus(boolean canLoseFocus) {
        this.canLoseFocus = canLoseFocus;
    }

    public Component hint() {
        return this.hint;
    }

    public void setHint(Component hint) {
        this.hint = hint == null ? Component.empty() : hint;
    }

    public String suggestion() {
        return this.suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion == null ? "" : suggestion;
    }

    public void setResponder(Consumer<String> responder) {
        this.responder = responder == null ? ignored -> {} : responder;
    }

    public IWidgetRenderer renderer() {
        return this.renderer;
    }

    public void setRenderer(IWidgetRenderer renderer) {
        this.renderer = renderer;
    }

    public Font font() {
        return this.font;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public String visibleText(int pixelWidth) {
        if (this.value.isEmpty() || this.font == null || pixelWidth <= 0) {
            return this.value;
        }
        int start = 0;
        while (start < this.cursorPosition
                && this.font.width(this.value.substring(start, this.cursorPosition)) > pixelWidth) {
            start++;
        }
        String visible = this.value.substring(start);
        return this.font.width(visible) <= pixelWidth
                ? visible
                : this.font.plainSubstrByWidth(visible, pixelWidth);
    }

    @Override
    protected void extractWidgetRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
        if (this.renderer != null && this.isActive() && this.visible) {
            this.renderer.render(graphics, this, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!super.mouseClicked(event, doubleClick)) {
            return false;
        }
        this.setFocused(true);
        this.moveCursorTo(this.findCursorPosition(event.x()), false);
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (!this.isActive() || !this.visible || !this.isPressed()) {
            return false;
        }
        this.moveCursorTo(this.findCursorPosition(event.x()), true);
        return true;
    }

    @Override
    public void setFocused(boolean focused) {
        if (!focused && !this.canLoseFocus) {
            return;
        }
        super.setFocused(focused);
        if (focused) {
            this.focusedTime = Util.getMillis();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!this.canConsumeInput()) {
            return false;
        }
        if (event.isSelectAll()) {
            this.selectAll();
            return true;
        }
        if (event.isLeft()) {
            if (event.hasControlDown()) {
                this.moveCursorTo(this.wordBoundary(-1), event.hasShiftDown());
            } else {
                this.moveCursorTo(this.cursorPosition - 1, event.hasShiftDown());
            }
            return true;
        }
        if (event.isRight()) {
            if (event.hasControlDown()) {
                this.moveCursorTo(this.wordBoundary(1), event.hasShiftDown());
            } else {
                this.moveCursorTo(this.cursorPosition + 1, event.hasShiftDown());
            }
            return true;
        }
        switch (event.key()) {
            case GLFW.GLFW_KEY_HOME -> {
                this.moveCursorToStart(event.hasShiftDown());
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                this.moveCursorToEnd(event.hasShiftDown());
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                this.deleteWords(-1);
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                this.deleteWords(1);
                return true;
            }
            default -> {

                return true;
            }
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!this.canConsumeInput() || !event.isAllowedChatCharacter()) {
            return false;
        }
        this.insertText(event.codepointAsString());
        return true;
    }

    private void deleteSelection() {
        int start = this.selectionStart();
        int end = this.selectionEnd();
        this.value = this.value.substring(0, start) + this.value.substring(end);
        this.cursorPosition = start;
        this.highlightPosition = start;
    }

    private int wordBoundary(int direction) {
        int target = this.cursorPosition;
        if (direction < 0) {
            while (target > 0 && Character.isWhitespace(this.value.charAt(target - 1))) {
                target--;
            }
            while (target > 0 && !Character.isWhitespace(this.value.charAt(target - 1))) {
                target--;
            }
        } else {
            while (target < this.value.length() && Character.isWhitespace(this.value.charAt(target))) {
                target++;
            }
            while (target < this.value.length() && !Character.isWhitespace(this.value.charAt(target))) {
                target++;
            }
        }
        return target;
    }

    private int findCursorPosition(double mouseX) {
        if (this.font == null || this.value.isEmpty()) {
            return mouseX <= this.getX() + this.getWidth() * 0.5 ? 0 : this.value.length();
        }
        int relativeX = Math.max(0, (int)Math.round(mouseX - this.getX() - 3.0));
        for (int position = 0; position <= this.value.length(); position++) {
            int width = this.font.width(this.value.substring(0, position));
            if (relativeX < width) {
                int previous = position == 0 ? 0 : this.font.width(this.value.substring(0, position - 1));
                return relativeX - previous < width - relativeX ? position - 1 : position;
            }
        }
        return this.value.length();
    }
}
