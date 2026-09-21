package net.jr.experimental_features.gamepad_keyboard;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import net.jr.api.client.render.font.FontHolder;
import net.jr.client.components.elements.TextElement;
import net.jr.client.components.navigation.UiAction;
import net.jr.client.components.widgets.Button;
import net.jr.client.input.InputApi;
import net.jr.api.client.input.GamepadDigitalInput;
import net.jr.client.sound.action.InputActionSounds;
import net.jr.client.ui.overlay.ScreenOverlayCloseReason;
import net.jr.client.ui.overlay.ScreenOverlayContext;
import net.jr.client.ui.overlay.VanillaScreenOverlay;
import net.jr.registry.ModFonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;

/** Per-local-player editing session for the experimental controller keyboard. */
public final class GamepadKeyboardState implements VanillaScreenOverlay {
    private static final long REPEAT_DELAY_MS = 300L;
    private static final long REPEAT_INTERVAL_MS = 85L;
    private static final int DIM_COLOR = 0x80000000;
    private static final int KEY_COLOR = 0xE615171B;
    private static final int SPECIAL_KEY_COLOR = 0xE6222428;
    private static final int KEY_DISABLED_COLOR = 0xE60E0F12;
    private static final int PANEL_COLOR = 0xF208090A;
    private static final int PANEL_BORDER_COLOR = 0xFF4D4D4D;
    private static final int PANEL_BORDER_THICKNESS = 1;
    private static final int PANEL_PADDING = 10;
    private static final int FIELD_COLOR = 0xE64D4D4D;
    private static final int OUTLINE_COLOR = 0xFFFFFF00;
    private static final int FOCUS_OVERLAY_COLOR = 0x26FFFFFF;
    private static final int CURSOR_COLOR = 0xFFFFFFFF;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_DISABLED_COLOR = 0xFF808080;
    private static final FontHolder QWERTY_FONT = ModFonts.Montserrat_bold;

    private static final String[][] LETTER_ROWS = {
        {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
        {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
        {"a", "s", "d", "f", "g", "h", "j", "k", "l", "ñ"},
        {"z", "x", "c", "v", "b", "n", "m", ",", ".", "?"}
    };
    private static final String[][] SYMBOL_PAGE_ONE_ROWS = {
        {"§", "&", "@", "#", "/", "\\", ":", ";", "=", "!"},
        {"~", "^", "_", "-", "+", "*", "%", "?", "$", "|"},
        {"{", "}", "[", "]", "(", ")", "<", ">", "\"", "'"},
        {"¿", "¡", "€", "£", "¥", "¢", "©", "®", "™", "°"}
    };
    private static final String[][] SYMBOL_PAGE_TWO_ROWS = {
        {"¹", "²", "³", "¼", "½", "¾", "±", "×", "÷", "≠"},
        {"≤", "≥", "≈", "√", "∞", "µ", "¶", "¬", "¦", "¤"},
        {"«", "»", "“", "”", "‘", "’", "–", "—", "´", "`"},
        {"æ", "Æ", "œ", "Œ", "ø", "Ø", "å", "Å", "ç", "Ç"}
    };
    private static final String[][][] SYMBOL_PAGES = {
        SYMBOL_PAGE_ONE_ROWS,
        SYMBOL_PAGE_TWO_ROWS
    };
    private static final int ACCENT_VARIANT_COUNT = 5;

    private final EnumMap<GamepadDigitalInput, ButtonLatch> latches =
        new EnumMap<>(GamepadDigitalInput.class);
    private final List<List<KeyWidget>> rows = new ArrayList<>();
    private TextElement fieldText;
    private TextElement measurementText;
    private boolean active;
    private boolean symbols;
    private boolean uppercase;
    private int symbolPage;
    private int accentVariant;
    private int selectedRow;
    private int selectedColumn;
    private String value = "";
    private int cursor;
    private int maxLength;
    private GamepadTextInput target;

    public boolean isActive() {
        return this.active;
    }

    public void open(GamepadTextInput target) {
        this.ensureTextElements();
        InputApi.releaseBindings();
        InputApi.suppressHeldBindings();
        this.active = true;
        this.target = target;
        this.value = target.gamepadKeyboardValue();
        this.cursor = Math.clamp(target.gamepadKeyboardCursor(), 0, this.value.length());
        this.maxLength = Math.max(0, target.gamepadKeyboardMaxLength());
        this.symbols = false;
        this.uppercase = false;
        this.symbolPage = 0;
        this.accentVariant = 0;
        this.selectedRow = 1;
        this.selectedColumn = 0;
        this.rebuildKeys();
        this.syncInputs();
    }

    public void clear() {
        this.active = false;
        this.target = null;
        this.value = "";
        this.cursor = 0;
        this.maxLength = 0;
        this.rows.clear();
        this.latches.clear();
    }

    @Override
    public void tick(ScreenOverlayContext context) {
        if (!this.active) {
            context.close(ScreenOverlayCloseReason.CANCELLED);
            return;
        }
        if (this.target == null || !InputApi.hasGamepad()) {
            this.cancel();
            context.close(ScreenOverlayCloseReason.CANCELLED);
            return;
        }
        if (!InputApi.isGamepadMode()) {
            // Leave the original field focused so physical keyboard input can resume naturally.
            this.cancel();
            context.close(ScreenOverlayCloseReason.INPUT_MODE_CHANGED);
            return;
        }

        if (this.edge(GamepadDigitalInput.BUTTON_RIGHT)) {
            this.cancel();
            context.close(ScreenOverlayCloseReason.CANCELLED);
            return;
        }
        if (this.edge(GamepadDigitalInput.BUTTON_START)) {
            this.commit();
            context.close(ScreenOverlayCloseReason.COMPLETED);
            return;
        }

        if (this.repeat(GamepadDigitalInput.BUTTON_LEFT)) {
            this.backspace();
        }
        if (this.edge(GamepadDigitalInput.BUTTON_UP)) {
            this.insert(" ");
        }
        if (this.repeat(GamepadDigitalInput.BUMPER_LEFT)) {
            this.moveTextCursor(-1);
        }
        if (this.repeat(GamepadDigitalInput.BUMPER_RIGHT)) {
            this.moveTextCursor(1);
        }
        if (this.edge(GamepadDigitalInput.STICK_LEFT_BUTTON)) {
            this.cycleAccents();
        }
        if (this.edge(GamepadDigitalInput.TRIGGER_LEFT)) {
            this.toggleSymbols();
        }
        if (this.edge(GamepadDigitalInput.TRIGGER_RIGHT)) {
            this.toggleSecondaryPage();
        }

        int vertical = this.direction(
            GamepadDigitalInput.DPAD_UP,
            GamepadDigitalInput.STICK_LEFT_MOVE_UP,
            GamepadDigitalInput.DPAD_DOWN,
            GamepadDigitalInput.STICK_LEFT_MOVE_DOWN
        );
        int horizontal = this.direction(
            GamepadDigitalInput.DPAD_LEFT,
            GamepadDigitalInput.STICK_LEFT_MOVE_LEFT,
            GamepadDigitalInput.DPAD_RIGHT,
            GamepadDigitalInput.STICK_LEFT_MOVE_RIGHT
        );
        if (vertical != 0) {
            this.moveVertical(vertical);
        } else if (horizontal != 0) {
            this.moveHorizontal(horizontal);
        }

        if (this.edge(GamepadDigitalInput.BUTTON_DOWN)) {
            this.selectedKey().button.handleUiAction(UiAction.ACCEPT);
            if (!this.active) {
                context.close(ScreenOverlayCloseReason.COMPLETED);
            }
        }
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int width, int height, float partialTick) {
        if (!this.active || this.rows.isEmpty()) {
            return;
        }
        graphics.fill(0, 0, width, height, DIM_COLOR);

        int keyboardWidth = Math.max(1, Math.min(440, Math.round((width - 12) * 0.71F)));
        int gap = keyboardWidth < 320 ? 2 : 3;
        int rowCount = this.rows.size();
        int keyHeight = Math.max(11, Math.min(24, (height - 56 - gap * rowCount) / rowCount));
        int fieldHeight = Math.max(15, Math.min(20, keyHeight));
        int contentHeight = fieldHeight + 8 + rowCount * keyHeight + (rowCount - 1) * gap;
        int framedHeight = contentHeight + 20;
        if (framedHeight > height) {
            keyHeight = Math.max(10, keyHeight - (framedHeight - height + rowCount - 1) / rowCount);
            contentHeight = fieldHeight + 8 + rowCount * keyHeight + (rowCount - 1) * gap;
            framedHeight = contentHeight + 20;
        }
        int left = (width - keyboardWidth) / 2;
        int panelTop = Math.max(0, (height - framedHeight) / 2);
        int panelBottom = Math.min(height, panelTop + framedHeight);
        int panelLeft = Math.max(0, left - PANEL_PADDING);
        int panelRight = Math.min(width, left + keyboardWidth + PANEL_PADDING);
        int top = panelTop + PANEL_PADDING;

        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, PANEL_COLOR);
        graphics.fill(
            panelLeft,
            panelTop,
            panelRight,
            Math.min(panelBottom, panelTop + PANEL_BORDER_THICKNESS),
            PANEL_BORDER_COLOR
        );
        graphics.fill(
            panelLeft,
            Math.max(panelTop, panelBottom - PANEL_BORDER_THICKNESS),
            panelRight,
            panelBottom,
            PANEL_BORDER_COLOR
        );
        graphics.fill(
            panelLeft,
            Math.min(panelBottom, panelTop + PANEL_BORDER_THICKNESS),
            Math.min(panelRight, panelLeft + PANEL_BORDER_THICKNESS),
            Math.max(panelTop, panelBottom - PANEL_BORDER_THICKNESS),
            PANEL_BORDER_COLOR
        );
        graphics.fill(
            Math.max(panelLeft, panelRight - PANEL_BORDER_THICKNESS),
            Math.min(panelBottom, panelTop + PANEL_BORDER_THICKNESS),
            panelRight,
            Math.max(panelTop, panelBottom - PANEL_BORDER_THICKNESS),
            PANEL_BORDER_COLOR
        );

        graphics.fill(left, top, left + keyboardWidth, top + fieldHeight, FIELD_COLOR);
        String visible = this.visibleValue(keyboardWidth - 16);
        this.fieldText.setText(literalText(visible));
        int textY = top + Math.round((fieldHeight - this.fieldText.height()) / 2.0F);
        this.fieldText.setPosition(left + 8, textY);
        this.fieldText.draw(graphics);
        int visibleCursor = Math.min(visible.length(), this.cursor - this.visibleValueStart(keyboardWidth - 16));
        int cursorX = left + 8 + Math.round(this.measureText(visible.substring(0, Math.max(0, visibleCursor))));
        graphics.fill(cursorX, textY - 1, cursorX + 1, textY + Math.round(this.fieldText.height()) + 1, CURSOR_COLOR);

        int y = top + fieldHeight + 8;
        int sideWidth = Math.max(39, Math.min(54, keyboardWidth / 8));
        for (int rowIndex = 0; rowIndex < this.rows.size(); rowIndex++) {
            List<KeyWidget> row = this.rows.get(rowIndex);
            if (rowIndex < 4) {
                this.layoutCharacterRow(graphics, row, rowIndex, left, y, keyboardWidth, sideWidth, gap, keyHeight, partialTick);
                y += keyHeight + gap;
                continue;
            }
            float totalWeight = 0.0F;
            for (KeyWidget key : row) totalWeight += key.weight;
            int usableWidth = keyboardWidth - gap * (row.size() - 1);
            int x = left;
            float consumedWeight = 0.0F;
            for (int column = 0; column < row.size(); column++) {
                KeyWidget key = row.get(column);
                consumedWeight += key.weight;
                int right = column == row.size() - 1
                    ? left + keyboardWidth
                    : left + Math.round(usableWidth * consumedWeight / totalWeight) + gap * column;
                key.button.setBounds(x, y, Math.max(1, right - x), keyHeight);
                key.button.extractRenderState(graphics, Integer.MIN_VALUE, Integer.MIN_VALUE, partialTick);
                x = right + gap;
            }
            y += keyHeight + gap;
        }

    }

    private void layoutCharacterRow(
        GuiGraphicsExtractor graphics,
        List<KeyWidget> row,
        int rowIndex,
        int left,
        int y,
        int keyboardWidth,
        int sideWidth,
        int gap,
        int keyHeight,
        float partialTick
    ) {
        boolean hasSideActions = rowIndex == 0 || rowIndex == 1 || rowIndex == 3;
        int characterStart = hasSideActions ? 1 : 0;
        int characterEnd = hasSideActions ? row.size() - 1 : row.size();
        int characterLeft = left + sideWidth + gap;
        int characterRight = left + keyboardWidth - sideWidth - gap;
        int characterCount = characterEnd - characterStart;
        int usableWidth = characterRight - characterLeft - gap * (characterCount - 1);

        for (int index = characterStart; index < characterEnd; index++) {
            int characterIndex = index - characterStart;
            int x = characterLeft + Math.round(usableWidth * characterIndex / (float)characterCount) + gap * characterIndex;
            int right = characterIndex == characterCount - 1
                ? characterRight
                : characterLeft + Math.round(usableWidth * (characterIndex + 1) / (float)characterCount) + gap * characterIndex;
            this.renderKey(graphics, row.get(index), x, y, Math.max(1, right - x), keyHeight, partialTick);
        }

        if (hasSideActions) {
            int sideHeight;
            int sideY = y;
            if (rowIndex == 0) {
                sideHeight = keyHeight;
            } else {
                sideHeight = (keyHeight * 3 + gap) / 2;
                if (rowIndex == 3) {
                    sideY = y + keyHeight - sideHeight;
                }
            }
            this.renderKey(graphics, row.get(0), left, sideY, sideWidth, sideHeight, partialTick);
            this.renderKey(
                graphics,
                row.get(row.size() - 1),
                left + keyboardWidth - sideWidth,
                sideY,
                sideWidth,
                sideHeight,
                partialTick
            );
        }
    }

    private void renderKey(
        GuiGraphicsExtractor graphics,
        KeyWidget key,
        int x,
        int y,
        int width,
        int height,
        float partialTick
    ) {
        key.button.setBounds(x, y, width, height);
        key.button.extractRenderState(graphics, Integer.MIN_VALUE, Integer.MIN_VALUE, partialTick);
    }

    private void rebuildKeys() {
        this.rows.clear();
        String[][] source = this.symbols ? SYMBOL_PAGES[this.symbolPage] : LETTER_ROWS;
        for (int sourceRowIndex = 0; sourceRowIndex < source.length; sourceRowIndex++) {
            String[] sourceRow = source[sourceRowIndex];
            List<KeyWidget> row = new ArrayList<>();
            if (sourceRowIndex == 0) {
                row.add(this.key("←", 1.0F, KeyAction.CURSOR_LEFT));
            } else if (sourceRowIndex == 1) {
                row.add(this.key(this.symbols ? "ABC" : "?123", 1.0F, KeyAction.SYMBOLS));
            } else if (sourceRowIndex == 3) {
                row.add(this.key(this.accentPageLabel(), 1.0F, KeyAction.ACCENT));
            }
            for (String character : sourceRow) {
                String display = this.symbols ? character : this.applyAccentVariant(character);
                if (this.uppercase && !this.symbols) {
                    display = display.toUpperCase(Locale.ROOT);
                }
                row.add(this.key(display, 1.0F, KeyAction.CHARACTER));
            }
            if (sourceRowIndex == 0) {
                row.add(this.key("→", 1.0F, KeyAction.CURSOR_RIGHT));
            } else if (sourceRowIndex == 1) {
                row.add(this.key(this.secondaryPageLabel(), 1.0F, KeyAction.SECONDARY_PAGE));
            } else if (sourceRowIndex == 3) {
                row.add(this.key("Listo", 1.0F, KeyAction.DONE));
            }
            this.rows.add(row);
        }
        this.rows.add(List.of(
            this.key("Espacio", 3.0F, KeyAction.SPACE),
            this.key("Borrar", 2.0F, KeyAction.BACKSPACE)
        ));
        this.selectedRow = Math.clamp(this.selectedRow, 0, this.rows.size() - 1);
        this.selectedColumn = Math.clamp(this.selectedColumn, 0, this.rows.get(this.selectedRow).size() - 1);
        this.refreshKeyAvailabilityAndFocus();
    }

    private KeyWidget key(String label, float weight, KeyAction action) {
        final KeyWidget[] holder = new KeyWidget[1];
        TextElement labelText = createTextElement(label);
        Button button = new Button.Builder(0, 0, 1, 1)
            .onPress(ignored -> this.activate(holder[0]))
            .withRenderer((graphics, widget, mouseX, mouseY, partialTick) -> {
                boolean available = holder[0].button.isActive();
                boolean selected = available && holder[0] == this.selectedKey();
                int x = widget.getX();
                int y = widget.getY();
                int right = x + widget.getWidth();
                int bottom = y + widget.getHeight();
                graphics.fill(
                    x,
                    y,
                    right,
                    bottom,
                    available
                        ? holder[0].action == KeyAction.CHARACTER ? KEY_COLOR : SPECIAL_KEY_COLOR
                        : KEY_DISABLED_COLOR
                );
                if (selected) {
                    graphics.fill(x, y, right, bottom, FOCUS_OVERLAY_COLOR);
                    graphics.fill(x, y, right, y + 1, OUTLINE_COLOR);
                    graphics.fill(x, bottom - 1, right, bottom, OUTLINE_COLOR);
                    graphics.fill(x, y + 1, x + 1, bottom - 1, OUTLINE_COLOR);
                    graphics.fill(right - 1, y + 1, right, bottom - 1, OUTLINE_COLOR);
                }
                TextElement text = holder[0].text;
                text.setColor(available ? TEXT_COLOR : TEXT_DISABLED_COLOR);
                text.setPosition(
                    x + (widget.getWidth()  - text.width()) / 2.0F,
                    y + (widget.getHeight() - text.height()) / 2.0F
                );
                text.draw(graphics);
            })
            .withSound((handler, widget, event) -> {})
            .build();
        KeyWidget key = new KeyWidget(label, weight, action, button, labelText);
        holder[0] = key;
        return key;
    }

    private static TextElement createTextElement(String text) {
        TextElement element = new TextElement(literalText(text), 0.0F, 0.0F, null);
        element.setFont(QWERTY_FONT);
        element.setScale(0.25F);
        element.setColor(TEXT_COLOR);
        element.setShadow(false);
        return element;
    }

    private static FormattedCharSequence literalText(String text) {
        return FormattedCharSequence.forward(text, Style.EMPTY);
    }

    private void ensureTextElements() {
        if (this.fieldText == null) {
            this.fieldText = createTextElement("");
        }
        if (this.measurementText == null) {
            this.measurementText = createTextElement("");
        }
    }

    private void activate(KeyWidget key) {
        if (!this.isKeyAvailable(key)) {
            return;
        }
        switch (key.action) {
            case CHARACTER -> this.insert(key.label);
            case SPACE -> this.insert(" ");
            case BACKSPACE -> this.backspace();
            case SYMBOLS -> this.toggleSymbols();
            case SECONDARY_PAGE -> this.toggleSecondaryPage();
            case ACCENT -> this.cycleAccents();
            case CURSOR_LEFT -> this.moveTextCursor(-1);
            case CURSOR_RIGHT -> this.moveTextCursor(1);
            case DONE -> this.commit();
        }
    }

    private void insert(String text) {
        if (text.isEmpty() || this.value.length() >= this.maxLength) {
            return;
        }
        int available = this.maxLength - this.value.length();
        String insertion = text.length() > available ? text.substring(0, available) : text;
        if (!this.acceptsInsertion(insertion)) {
            return;
        }
        this.value = this.value.substring(0, this.cursor) + insertion + this.value.substring(this.cursor);
        this.cursor += insertion.length();
        this.refreshKeyAvailabilityAndFocus();
        InputActionSounds.playChar();
    }

    private void backspace() {
        if (this.cursor <= 0 || this.value.isEmpty()) {
            return;
        }
        int previous = this.value.offsetByCodePoints(this.cursor, -1);
        this.value = this.value.substring(0, previous) + this.value.substring(this.cursor);
        this.cursor = previous;
        this.refreshKeyAvailabilityAndFocus();
        InputActionSounds.playChar();
    }

    private void toggleSymbols() {
        this.symbols = !this.symbols;
        if (this.symbols) {
            this.symbolPage = 0;
        }
        this.rebuildKeys();
    }

    private void toggleSecondaryPage() {
        if (this.symbols) {
            this.symbolPage = (this.symbolPage + 1) % SYMBOL_PAGES.length;
            this.rebuildKeys();
        } else {
            this.uppercase = !this.uppercase;
            this.rebuildKeys();
        }
    }

    private String secondaryPageLabel() {
        if (this.symbols) {
            int nextPage = (this.symbolPage + 1) % SYMBOL_PAGES.length;
            return (nextPage + 1) + "/" + SYMBOL_PAGES.length;
        }
        return this.uppercase ? "abc" : "ABC";
    }

    private String accentPageLabel() {
        return switch ((this.accentVariant + 1) % ACCENT_VARIANT_COUNT) {
            case 0 -> "A";
            case 1 -> "Á";
            case 2 -> "À";
            case 3 -> "Â";
            case 4 -> "Ä";
            default -> throw new IllegalStateException("Unexpected accent variant");
        };
    }

    private void moveTextCursor(int amount) {
        int next = amount < 0
            ? this.cursor > 0 ? this.value.offsetByCodePoints(this.cursor, -1) : this.cursor
            : this.cursor < this.value.length() ? this.value.offsetByCodePoints(this.cursor, 1) : this.cursor;
        if (next == this.cursor) {
            return;
        }
        this.cursor = next;
        this.refreshKeyAvailabilityAndFocus();
        InputActionSounds.playHotbarFocus();
    }

    private void cycleAccents() {
        if (this.symbols) {
            return;
        }
        this.accentVariant = (this.accentVariant + 1) % ACCENT_VARIANT_COUNT;
        this.rebuildKeys();
    }

    private String applyAccentVariant(String character) {
        return switch (character) {
            case "a" -> accentVariant("a", "á", "à", "â", "ä");
            case "e" -> accentVariant("e", "é", "è", "ê", "ë");
            case "i" -> accentVariant("i", "í", "ì", "î", "ï");
            case "o" -> accentVariant("o", "ó", "ò", "ô", "ö");
            case "u" -> accentVariant("u", "ú", "ù", "û", "ü");
            default -> character;
        };
    }

    private String accentVariant(String normal, String acute, String grave, String circumflex, String diaeresis) {
        return switch (this.accentVariant) {
            case 0 -> normal;
            case 1 -> acute;
            case 2 -> grave;
            case 3 -> circumflex;
            case 4 -> diaeresis;
            default -> throw new IllegalStateException("Unexpected accent variant: " + this.accentVariant);
        };
    }

    private void commit() {
        if (this.target != null) {
            this.target.gamepadKeyboardApply(this.value, this.cursor);
        }
        this.clearSession();
    }

    private void cancel() {
        this.clearSession();
    }

    private void clearSession() {
        this.active = false;
        this.target = null;
        this.rows.clear();
    }

    @Override
    public void closed(ScreenOverlayCloseReason reason) {
        this.clearSession();
    }

    private void moveHorizontal(int amount) {
        List<KeyWidget> row = this.rows.get(this.selectedRow);
        for (int offset = 1; offset <= row.size(); offset++) {
            int candidate = Math.floorMod(this.selectedColumn + amount * offset, row.size());
            if (this.isKeyAvailable(row.get(candidate))) {
                this.selectedColumn = candidate;
                this.refreshFocus();
                InputActionSounds.playHotbarFocus();
                return;
            }
        }
    }

    private void moveVertical(int amount) {
        List<KeyWidget> oldRow = this.rows.get(this.selectedRow);
        float horizontalPosition = this.navigationPosition(oldRow, this.selectedRow, this.selectedColumn);
        for (int offset = 1; offset <= this.rows.size(); offset++) {
            int candidateRow = Math.floorMod(this.selectedRow + amount * offset, this.rows.size());
            List<KeyWidget> row = this.rows.get(candidateRow);
            int candidateColumn = this.closestAvailableColumn(row, candidateRow, horizontalPosition);
            if (candidateColumn >= 0) {
                this.selectedRow = candidateRow;
                this.selectedColumn = candidateColumn;
                this.refreshFocus();
                InputActionSounds.playHotbarFocus();
                return;
            }
        }
    }

    private int closestAvailableColumn(List<KeyWidget> row, int rowIndex, float horizontalPosition) {
        int closestColumn = -1;
        float closestDistance = Float.MAX_VALUE;
        for (int column = 0; column < row.size(); column++) {
            if (!this.isKeyAvailable(row.get(column))) {
                continue;
            }
            float distance = Math.abs(this.navigationPosition(row, rowIndex, column) - horizontalPosition);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestColumn = column;
            }
        }
        return closestColumn;
    }

    private float navigationPosition(List<KeyWidget> row, int rowIndex, int column) {
        if (rowIndex < 4) {
            boolean hasSideActions = rowIndex == 0 || rowIndex == 1 || rowIndex == 3;
            if (hasSideActions) {
                if (column == 0) return -0.05F;
                if (column == row.size() - 1) return 1.05F;
                return (column - 0.5F) / 10.0F;
            }
            return (column + 0.5F) / 10.0F;
        }

        float totalWeight = 0.0F;
        float weightBefore = 0.0F;
        for (int index = 0; index < row.size(); index++) {
            float weight = row.get(index).weight;
            totalWeight += weight;
            if (index < column) weightBefore += weight;
        }
        return (weightBefore + row.get(column).weight / 2.0F) / totalWeight;
    }

    private void refreshKeyAvailabilityAndFocus() {
        for (List<KeyWidget> row : this.rows) {
            for (KeyWidget key : row) {
                key.button.setActive(this.isKeyAvailable(key));
            }
        }

        if (!this.selectedKey().button.isActive()) {
            int sameRowColumn = this.closestAvailableColumn(
                this.rows.get(this.selectedRow),
                this.selectedColumn
            );
            if (sameRowColumn >= 0) {
                this.selectedColumn = sameRowColumn;
            } else {
                for (int offset = 1; offset < this.rows.size(); offset++) {
                    int candidateRow = Math.floorMod(this.selectedRow + offset, this.rows.size());
                    int candidateColumn = this.closestAvailableColumn(this.rows.get(candidateRow), 0);
                    if (candidateColumn >= 0) {
                        this.selectedRow = candidateRow;
                        this.selectedColumn = candidateColumn;
                        break;
                    }
                }
            }
        }
        this.refreshFocus();
    }

    private int closestAvailableColumn(List<KeyWidget> row, int preferredColumn) {
        for (int distance = 0; distance < row.size(); distance++) {
            int left = preferredColumn - distance;
            if (left >= 0 && this.isKeyAvailable(row.get(left))) {
                return left;
            }
            int right = preferredColumn + distance;
            if (distance > 0 && right < row.size() && this.isKeyAvailable(row.get(right))) {
                return right;
            }
        }
        return -1;
    }

    private boolean isKeyAvailable(KeyWidget key) {
        return switch (key.action) {
            case CHARACTER -> this.canInsert(key.label);
            case SPACE -> this.canInsert(" ");
            default -> true;
        };
    }

    private boolean canInsert(String text) {
        if (text == null || text.isEmpty() || this.value.length() >= this.maxLength) {
            return false;
        }
        int available = this.maxLength - this.value.length();
        String insertion = text.length() > available ? text.substring(0, available) : text;
        return this.acceptsInsertion(insertion);
    }

    private boolean acceptsInsertion(String insertion) {
        return this.target != null
            && this.target.gamepadKeyboardAcceptsInsertion(this.value, this.cursor, insertion);
    }

    private void refreshFocus() {
        KeyWidget selected = this.selectedKey();
        for (List<KeyWidget> row : this.rows) {
            for (KeyWidget key : row) key.button.setFocused(key == selected);
        }
    }

    private KeyWidget selectedKey() {
        return this.rows.get(this.selectedRow).get(this.selectedColumn);
    }

    private int direction(
        GamepadDigitalInput negative,
        GamepadDigitalInput negativeStick,
        GamepadDigitalInput positive,
        GamepadDigitalInput positiveStick
    ) {
        if (this.repeat(negative) || this.repeat(negativeStick)) return -1;
        if (this.repeat(positive) || this.repeat(positiveStick)) return 1;
        return 0;
    }

    private boolean edge(GamepadDigitalInput input) {
        return this.poll(input, false);
    }

    private boolean repeat(GamepadDigitalInput input) {
        return this.poll(input, true);
    }

    private boolean poll(GamepadDigitalInput input, boolean repeat) {
        boolean down = InputApi.isPressed(input);
        ButtonLatch latch = this.latches.computeIfAbsent(input, ignored -> new ButtonLatch());
        long now = Util.getMillis();
        boolean fire = down && !latch.down;
        if (fire) {
            latch.pressedAt = now;
            latch.lastRepeatAt = now;
        } else if (repeat && down && now - latch.pressedAt >= REPEAT_DELAY_MS
                && now - latch.lastRepeatAt >= REPEAT_INTERVAL_MS) {
            latch.lastRepeatAt = now;
            fire = true;
        }
        latch.down = down;
        return fire;
    }

    private void syncInputs() {
        for (GamepadDigitalInput input : CONTROL_INPUTS) {
            ButtonLatch latch = this.latches.computeIfAbsent(input, ignored -> new ButtonLatch());
            latch.down = InputApi.isPressed(input);
            latch.pressedAt = Util.getMillis();
            latch.lastRepeatAt = latch.pressedAt;
        }
    }

    private int visibleValueStart(int width) {
        int start = 0;
        while (start < this.cursor && this.measureText(this.value.substring(start, this.cursor)) > width) {
            start = this.value.offsetByCodePoints(start, 1);
        }
        return start;
    }

    private String visibleValue(int width) {
        int start = this.visibleValueStart(width);
        int end = start;
        while (end < this.value.length()) {
            int next = this.value.offsetByCodePoints(end, 1);
            if (this.measureText(this.value.substring(start, next)) > width) {
                break;
            }
            end = next;
        }
        return this.value.substring(start, end);
    }

    private float measureText(String text) {
        this.ensureTextElements();
        this.measurementText.setText(literalText(text));
        return this.measurementText.width();
    }

    private enum KeyAction {
        CHARACTER,
        SPACE,
        BACKSPACE,
        SYMBOLS,
        SECONDARY_PAGE,
        ACCENT,
        CURSOR_LEFT,
        CURSOR_RIGHT,
        DONE
    }

    private record KeyWidget(
        String label,
        float weight,
        KeyAction action,
        Button button,
        TextElement text
    ) {}

    private static final class ButtonLatch {
        private boolean down;
        private long pressedAt;
        private long lastRepeatAt;
    }

    private static final GamepadDigitalInput[] CONTROL_INPUTS = {
        GamepadDigitalInput.BUTTON_DOWN,
        GamepadDigitalInput.BUTTON_RIGHT,
        GamepadDigitalInput.BUTTON_LEFT,
        GamepadDigitalInput.BUTTON_UP,
        GamepadDigitalInput.BUTTON_START,
        GamepadDigitalInput.BUMPER_LEFT,
        GamepadDigitalInput.BUMPER_RIGHT,
        GamepadDigitalInput.STICK_LEFT_BUTTON,
        GamepadDigitalInput.TRIGGER_LEFT,
        GamepadDigitalInput.TRIGGER_RIGHT,
        GamepadDigitalInput.DPAD_UP,
        GamepadDigitalInput.DPAD_DOWN,
        GamepadDigitalInput.DPAD_LEFT,
        GamepadDigitalInput.DPAD_RIGHT,
        GamepadDigitalInput.STICK_LEFT_MOVE_UP,
        GamepadDigitalInput.STICK_LEFT_MOVE_DOWN,
        GamepadDigitalInput.STICK_LEFT_MOVE_LEFT,
        GamepadDigitalInput.STICK_LEFT_MOVE_RIGHT
    };
}
