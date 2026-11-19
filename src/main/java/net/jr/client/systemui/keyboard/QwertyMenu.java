package net.jr.client.systemui.keyboard;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.annotation.Nullable;
import net.jr.api.client.input.GamepadDigitalInput;
import net.jr.api.client.input.IGamepad;
import net.jr.api.client.render.Draw;
import net.jr.api.client.resource.Asset;
import net.jr.api.client.systemui.AbstractSystemPanel;
import net.jr.api.client.systemui.ShowMenu;
import net.jr.api.client.systemui.SystemOverlay;
import net.jr.api.client.systemui.SystemUI;
import net.jr.api.client.systemui.SystemUiCloseReason;
import net.jr.api.client.systemui.SystemUiHandle;
import net.jr.api.client.systemui.SystemUiRenderContext;
import net.jr.api.client.systemui.SystemUiTarget;
import net.jr.client.components.elements.TextElement;
import net.jr.client.components.navigation.UiAction;
import net.jr.client.components.widgets.Button;
import net.jr.client.input.InputApi;
import net.jr.client.systemui.SystemUiSounds;
import net.jr.experimental_features.gamepad_keyboard.GamepadTextInput;
import net.jr.registry.ModFonts;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

public final class QwertyMenu extends AbstractSystemPanel {
    public static final int WIDTH = 1443;
    public static final int HEIGHT = 751;

    private static final int DESCRIPTION_X = 31;
    private static final int DESCRIPTION_Y = 135;
    private static final int FIELD_X = 35;
    private static final int FIELD_Y = 176;
    private static final int FIELD_WIDTH = 1371;
    private static final int FIELD_HEIGHT = 59;
    private static final int FIELD_PADDING_X = 20;
    private static final int CHARACTER_X = 231;
    private static final int CHARACTER_Y = 307;
    private static final int CHARACTER_WIDTH = 90;
    private static final int CHARACTER_HEIGHT = 72;
    private static final int KEY_GAP = 9;
    private static final int SIDE_LEFT_X = 48;
    private static final int SIDE_RIGHT_X = 1221;
    private static final int SMALL_Y = 307;
    private static final int SMALL_WIDTH = 174;
    private static final int SMALL_HEIGHT = 72;
    private static final int BIG_TOP_Y = 388;
    private static final int BIG_BOTTOM_Y = 511;
    private static final int BIG_WIDTH = 174;
    private static final int BIG_HEIGHT = 111;
    private static final int BOTTOM_Y = 631;
    private static final int SPACE_X = 48;
    private static final int SPACE_WIDTH = 798;
    private static final int BACKSPACE_X = 855;
    private static final int BACKSPACE_WIDTH = 540;
    private static final long REPEAT_DELAY_MS = 300L;
    private static final long REPEAT_INTERVAL_MS = 85L;

    private static final int NORMAL_TEXT_COLOR = 0xFF636060;
    private static final int HIGHLIGHT_TEXT_COLOR = 0xFFE4E4E4;
    private static final int HIGHLIGHT_FILL_COLOR = 0xFF089E1C;
    private static final Asset PANEL = Asset.MOD("textures/system/panels/qwerty.png");
    private static final Asset KEY = Asset.MOD("textures/system/qwerty_menu/key.png");
    private static final Asset SPECIAL_SMALL = Asset.MOD("textures/system/qwerty_menu/special_small.png");
    private static final Asset SPECIAL_BIG = Asset.MOD("textures/system/qwerty_menu/special_big.png");
    private static final Asset SPACE = Asset.MOD("textures/system/qwerty_menu/space.png");
    private static final Asset BACKSPACE = Asset.MOD("textures/system/qwerty_menu/backspace.png");

    private static final String[][] LETTER_ROWS = {
        {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
        {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
        {"a", "s", "d", "f", "g", "h", "j", "k", "l", "ñ"},
        {"z", "x", "c", "v", "b", "n", "m", ",", ".", "?"}
    };
    private static final String[][] SYMBOL_PAGE_ONE = {
        {"§", "&", "@", "#", "/", "\\", ":", ";", "=", "!"},
        {"~", "^", "_", "-", "+", "*", "%", "?", "$", "|"},
        {"{", "}", "[", "]", "(", ")", "<", ">", "\"", "'"},
        {"¿", "¡", "€", "£", "¥", "¢", "©", "®", "™", "°"}
    };
    private static final String[][] SYMBOL_PAGE_TWO = {
        {"¹", "²", "³", "¼", "½", "¾", "±", "×", "÷", "≠"},
        {"≤", "≥", "≈", "√", "∞", "µ", "¶", "¬", "¦", "¤"},
        {"«", "»", "“", "”", "‘", "’", "–", "—", "´", "`"},
        {"æ", "Æ", "œ", "Œ", "ø", "Ø", "å", "Å", "ç", "Ç"}
    };
    private static final String[][][] SYMBOL_PAGES = {SYMBOL_PAGE_ONE, SYMBOL_PAGE_TWO};

    private final GamepadTextInput target;
    private final @Nullable IGamepad activator;
    private final @Nullable Component description;
    private final EnumMap<GamepadDigitalInput, ButtonLatch> latches =
        new EnumMap<>(GamepadDigitalInput.class);
    private final List<List<Key>> rows = new ArrayList<>();
    private final TextElement fieldText = text("");
    private final TextElement measurementText = text("");
    private String value;
    private int cursor;
    private final int maxLength;
    private boolean symbols;
    private boolean uppercase;
    private int symbolPage;
    private int accentVariant;
    private int selectedRow = 1;
    private int selectedColumn = 1;
    private double pointerX = Integer.MIN_VALUE;
    private double pointerY = Integer.MIN_VALUE;

    public QwertyMenu(Component title, GamepadTextInput target) {
        this(title, null, InputApi.currentGamepad(), target);
    }

    private QwertyMenu(
        Component title,
        @Nullable Component description,
        @Nullable IGamepad activator,
        GamepadTextInput target
    ) {
        super(title, WIDTH, HEIGHT);
        this.description = description;
        this.activator = activator;
        this.target = Objects.requireNonNull(target, "target");
        this.value = target.gamepadKeyboardValue();
        this.cursor = Math.clamp(target.gamepadKeyboardCursor(), 0, this.value.length());
        this.maxLength = Math.max(0, target.gamepadKeyboardMaxLength());
        this.rebuildKeys();
    }

    public static Builder builder(Component title, GamepadTextInput target) {
        return new Builder(title, target);
    }

    @Override
    public void opened(SystemUiHandle handle) {
        super.opened(handle);
        this.syncInputs();
    }

    @Override
    public void tick() {
        if (this.activator == null) {
            return;
        }
        if (!this.activator.connected()) {
            this.handle().close(SystemUiCloseReason.CANCELLED);
            return;
        }
        if (this.isOpening()) {
            this.syncInputs();
            return;
        }
        if (this.edge(GamepadDigitalInput.BUTTON_RIGHT)) {
            this.back();
            return;
        }
        if (this.edge(GamepadDigitalInput.BUTTON_START)) {
            SystemUiSounds.playSelectCharacter();
            this.commit();
            return;
        }
        if (this.repeat(GamepadDigitalInput.BUTTON_LEFT) && this.backspace()) {
            SystemUiSounds.playSelectCharacter();
        }
        if (this.edge(GamepadDigitalInput.BUTTON_UP) && this.insert(" ")) {
            SystemUiSounds.playSelectCharacter();
        }
        if (this.repeat(GamepadDigitalInput.BUMPER_LEFT) && this.moveCursor(-1)) {
            SystemUiSounds.playSelectCharacter();
        }
        if (this.repeat(GamepadDigitalInput.BUMPER_RIGHT) && this.moveCursor(1)) {
            SystemUiSounds.playSelectCharacter();
        }
        if (this.edge(GamepadDigitalInput.STICK_LEFT_BUTTON)) {
            SystemUiSounds.playSelectCharacter();
            this.cycleAccents();
        }
        if (this.edge(GamepadDigitalInput.TRIGGER_LEFT)) {
            SystemUiSounds.playSelectCharacter();
            this.toggleSymbols();
        }
        if (this.edge(GamepadDigitalInput.TRIGGER_RIGHT)) {
            SystemUiSounds.playSelectCharacter();
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
        if (vertical != 0) this.moveVertical(vertical);
        else if (horizontal != 0) this.moveHorizontal(horizontal);

        if (this.edge(GamepadDigitalInput.BUTTON_DOWN)) {
            this.selectedKey().button.handleUiAction(UiAction.ACCEPT);
        }
    }

    @Override
    protected void renderPanel(SystemUiRenderContext context, Bounds bounds) {
        Draw.image(PANEL, bounds.x(), bounds.y(), bounds.width(), bounds.height())
            .pixelSnap()
            .draw(context.graphics());
    }

    @Override
    protected void renderContent(SystemUiRenderContext context, Bounds bounds) {
        if (this.description != null) {
            Draw.text(this.description, bounds.x() + DESCRIPTION_X, bounds.y() + DESCRIPTION_Y)
                .font(ModFonts.Montserrat_bold)
                .scale(1.0F)
                .color(NORMAL_TEXT_COLOR)
                .shadow(false)
                .draw(context.graphics());
        }
        this.renderField(context, bounds);
        int mouseX = (int)Math.round(this.pointerX);
        int mouseY = (int)Math.round(this.pointerY);
        float partialTick = context.deltaTracker().getGameTimeDeltaTicks();
        for (List<Key> row : this.rows) {
            for (Key key : row) {
                key.button.setPosition(
                    bounds.x() + key.button.getBaseX(),
                    bounds.y() + key.button.getBaseY()
                );
                key.button.extractRenderState(context.graphics(), mouseX, mouseY, partialTick);
            }
        }
    }

    private void renderField(SystemUiRenderContext context, Bounds bounds) {
        int availableWidth = FIELD_WIDTH - FIELD_PADDING_X * 2;
        int start = this.visibleStart(availableWidth);
        String visible = this.visibleValue(start, availableWidth);
        this.fieldText.setText(Component.literal(visible));
        this.fieldText.setColor(NORMAL_TEXT_COLOR);
        this.fieldText.setPosition(
            bounds.x() + FIELD_X + FIELD_PADDING_X,
            bounds.y() + FIELD_Y + (FIELD_HEIGHT - this.fieldText.height()) / 2.0F
        );
        this.fieldText.draw(context.graphics());

        int localCursor = Math.clamp(this.cursor - start, 0, visible.length());
        int cursorX = bounds.x() + FIELD_X + FIELD_PADDING_X
            + Math.round(this.measure(visible.substring(0, localCursor)));
        int textY = Math.round(this.fieldText.y());
        if ((Util.getMillis() / 500L & 1L) == 0L) {
            context.graphics().fill(
                cursorX,
                textY - 2,
                cursorX + 2,
                textY + Math.round(this.fieldText.height()) + 2,
                NORMAL_TEXT_COLOR
            );
        }
    }

    @Override
    protected void onMouseMoved(double x, double y) {
        this.pointerX = x;
        this.pointerY = y;
    }

    @Override
    protected void onMouseButton(double x, double y, int button, int action) {
        MouseButtonEvent event = new MouseButtonEvent(x, y, new MouseButtonInfo(button, 0));
        for (List<Key> row : this.rows) {
            for (Key key : row) {
                if (action == GLFW.GLFW_PRESS && key.button.mouseClicked(event, false)) return;
                if (action == GLFW.GLFW_RELEASE) key.button.mouseReleased(event);
            }
        }
    }

    @Override
    protected void onKeyEvent(int action, KeyEvent event) {
        if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_REPEAT) return;
        switch (event.key()) {
            case GLFW.GLFW_KEY_ESCAPE -> this.back();
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                SystemUiSounds.playSelectCharacter();
                this.commit();
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (this.backspace()) SystemUiSounds.playSelectCharacter();
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (this.moveCursor(-1)) SystemUiSounds.playSelectCharacter();
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (this.moveCursor(1)) SystemUiSounds.playSelectCharacter();
            }
            default -> {
            }
        }
    }

    @Override
    protected void onCharTyped(CharacterEvent event) {
        if (this.insert(event.codepointAsString())) {
            SystemUiSounds.playSelectCharacter();
        }
    }

    private void rebuildKeys() {
        this.rows.clear();
        String[][] source = this.symbols ? SYMBOL_PAGES[this.symbolPage] : LETTER_ROWS;
        List<Key> row0 = new ArrayList<>();
        row0.add(this.key("←", Action.CURSOR_LEFT, Style.SMALL, SIDE_LEFT_X, SMALL_Y, SMALL_WIDTH, SMALL_HEIGHT));
        this.addCharacters(row0, source[0], 0);
        row0.add(this.key("→", Action.CURSOR_RIGHT, Style.SMALL, SIDE_RIGHT_X, SMALL_Y, SMALL_WIDTH, SMALL_HEIGHT));
        this.rows.add(row0);

        List<Key> row1 = new ArrayList<>();
        row1.add(this.key(this.symbols ? "ABC" : "?123", Action.SYMBOLS, Style.BIG, SIDE_LEFT_X, BIG_TOP_Y, BIG_WIDTH, BIG_HEIGHT));
        this.addCharacters(row1, source[1], 1);
        row1.add(this.key(this.secondaryLabel(), Action.SECONDARY_PAGE, Style.BIG, SIDE_RIGHT_X, BIG_TOP_Y, BIG_WIDTH, BIG_HEIGHT));
        this.rows.add(row1);

        List<Key> row2 = new ArrayList<>();
        this.addCharacters(row2, source[2], 2);
        this.rows.add(row2);

        List<Key> row3 = new ArrayList<>();
        row3.add(this.key(this.accentLabel(), Action.ACCENT, Style.BIG, SIDE_LEFT_X, BIG_BOTTOM_Y, BIG_WIDTH, BIG_HEIGHT));
        this.addCharacters(row3, source[3], 3);
        row3.add(this.key("Listo", Action.DONE, Style.BIG, SIDE_RIGHT_X, BIG_BOTTOM_Y, BIG_WIDTH, BIG_HEIGHT));
        this.rows.add(row3);

        this.rows.add(new ArrayList<>(List.of(
            this.key("Espacio", Action.SPACE, Style.SPACE, SPACE_X, BOTTOM_Y, SPACE_WIDTH, CHARACTER_HEIGHT),
            this.key("Borrar", Action.BACKSPACE, Style.BACKSPACE, BACKSPACE_X, BOTTOM_Y, BACKSPACE_WIDTH, CHARACTER_HEIGHT)
        )));
        this.selectedRow = Math.clamp(this.selectedRow, 0, this.rows.size() - 1);
        this.selectedColumn = Math.clamp(this.selectedColumn, 0, this.rows.get(this.selectedRow).size() - 1);
        this.refreshAvailability();
    }

    private void addCharacters(List<Key> row, String[] labels, int rowIndex) {
        for (int column = 0; column < labels.length; column++) {
            String label = this.symbols ? labels[column] : this.accented(labels[column]);
            if (this.uppercase && !this.symbols) label = label.toUpperCase(Locale.ROOT);
            row.add(this.key(
                label,
                Action.CHARACTER,
                Style.KEY,
                CHARACTER_X + column * (CHARACTER_WIDTH + KEY_GAP),
                CHARACTER_Y + rowIndex * (CHARACTER_HEIGHT + KEY_GAP),
                CHARACTER_WIDTH,
                CHARACTER_HEIGHT
            ));
        }
    }

    private Key key(String label, Action action, Style style, int x, int y, int w, int h) {
        final Key[] holder = new Key[1];
        TextElement text = text(label);
        Button button = new Button.Builder(x, y, w, h)
            .onPress(ignored -> this.activate(holder[0]))
            .withRenderer((graphics, widget, mouseX, mouseY, partialTick) -> {
                Key key = holder[0];
                boolean highlighted = key.button.isActive()
                    && (key.button.isFocused() || key.button.isHovered());
                SystemUiSounds.updateFocus(widget, highlighted);
                Draw.image(
                    key.style.texture,
                    widget.getX(),
                    widget.getY(),
                    widget.getWidth(),
                    widget.getHeight()
                ).draw(graphics);
                if (highlighted) {
                    graphics.fill(
                        widget.getX(), widget.getY(),
                        widget.getX() + widget.getWidth(),
                        widget.getY() + widget.getHeight(),
                        HIGHLIGHT_FILL_COLOR
                    );
                }
                key.text.setColor(highlighted ? HIGHLIGHT_TEXT_COLOR : NORMAL_TEXT_COLOR);
                key.text.setPosition(
                    widget.getX() + (widget.getWidth() - key.text.width()) / 2.0F,
                    widget.getY() + (widget.getHeight() - key.text.height()) / 2.0F
                );
                key.text.draw(graphics);
            })
            .withSound((handler, widget, event) -> {
            })
            .build();
        Key key = new Key(label, action, style, button, text);
        holder[0] = key;
        return key;
    }

    private void activate(Key key) {
        if (!key.button.isActive()) return;
        switch (key.action) {
            case CHARACTER -> {
                SystemUiSounds.playSelectCharacter();
                this.insert(key.label);
            }
            case CURSOR_LEFT -> {
                if (this.moveCursor(-1)) SystemUiSounds.playSelectCharacter();
            }
            case CURSOR_RIGHT -> {
                if (this.moveCursor(1)) SystemUiSounds.playSelectCharacter();
            }
            case SYMBOLS -> {
                SystemUiSounds.playSelectCharacter();
                this.toggleSymbols();
            }
            case SECONDARY_PAGE -> {
                SystemUiSounds.playSelectCharacter();
                this.toggleSecondaryPage();
            }
            case ACCENT -> {
                SystemUiSounds.playSelectCharacter();
                this.cycleAccents();
            }
            case DONE -> {
                SystemUiSounds.playSelectCharacter();
                this.commit();
            }
            case SPACE -> {
                if (this.insert(" ")) SystemUiSounds.playSelectCharacter();
            }
            case BACKSPACE -> {
                if (this.backspace()) SystemUiSounds.playSelectCharacter();
            }
        }
    }

    private boolean insert(String insertion) {
        if (!this.canInsert(insertion)) return false;
        int available = this.maxLength - this.value.length();
        String accepted = insertion.length() > available
            ? insertion.substring(0, available) : insertion;
        this.value = this.value.substring(0, this.cursor) + accepted + this.value.substring(this.cursor);
        this.cursor += accepted.length();
        this.refreshAvailability();
        return true;
    }

    private boolean canInsert(String insertion) {
        if (insertion == null || insertion.isEmpty() || this.value.length() >= this.maxLength) return false;
        int available = this.maxLength - this.value.length();
        String accepted = insertion.length() > available
            ? insertion.substring(0, available) : insertion;
        return this.target.gamepadKeyboardAcceptsInsertion(this.value, this.cursor, accepted);
    }

    private boolean backspace() {
        if (this.cursor <= 0) return false;
        int previous = this.value.offsetByCodePoints(this.cursor, -1);
        this.value = this.value.substring(0, previous) + this.value.substring(this.cursor);
        this.cursor = previous;
        this.refreshAvailability();
        return true;
    }

    private boolean moveCursor(int amount) {
        int next = amount < 0
            ? this.cursor > 0 ? this.value.offsetByCodePoints(this.cursor, -1) : this.cursor
            : this.cursor < this.value.length() ? this.value.offsetByCodePoints(this.cursor, 1) : this.cursor;
        if (next == this.cursor) return false;
        this.cursor = next;
        return true;
    }

    private void commit() {
        this.target.gamepadKeyboardApply(this.value, this.cursor);
        this.handle().close(SystemUiCloseReason.COMPLETED);
    }

    private void toggleSymbols() {
        this.symbols = !this.symbols;
        if (this.symbols) this.symbolPage = 0;
        this.rebuildKeys();
    }

    private void toggleSecondaryPage() {
        if (this.symbols) this.symbolPage = (this.symbolPage + 1) % SYMBOL_PAGES.length;
        else this.uppercase = !this.uppercase;
        this.rebuildKeys();
    }

    private void cycleAccents() {
        if (this.symbols) return;
        this.accentVariant = (this.accentVariant + 1) % 5;
        this.rebuildKeys();
    }

    private String secondaryLabel() {
        return this.symbols ? ((this.symbolPage + 1) % SYMBOL_PAGES.length + 1) + "/2"
            : this.uppercase ? "abc" : "ABC";
    }

    private String accentLabel() {
        return switch ((this.accentVariant + 1) % 5) {
            case 0 -> "A";
            case 1 -> "Á";
            case 2 -> "À";
            case 3 -> "Â";
            case 4 -> "Ä";
            default -> "A";
        };
    }

    private String accented(String value) {
        return switch (value) {
            case "a" -> this.variant("a", "á", "à", "â", "ä");
            case "e" -> this.variant("e", "é", "è", "ê", "ë");
            case "i" -> this.variant("i", "í", "ì", "î", "ï");
            case "o" -> this.variant("o", "ó", "ò", "ô", "ö");
            case "u" -> this.variant("u", "ú", "ù", "û", "ü");
            default -> value;
        };
    }

    private String variant(String... values) {
        return values[this.accentVariant];
    }

    private void refreshAvailability() {
        for (List<Key> row : this.rows) {
            for (Key key : row) {
                key.button.setActive(key.action != Action.CHARACTER || this.canInsert(key.label));
            }
        }
        this.refreshFocus();
    }

    private void refreshFocus() {
        Key selected = this.selectedKey();
        for (List<Key> row : this.rows) {
            for (Key key : row) key.button.setFocused(key == selected);
        }
    }

    private Key selectedKey() {
        return this.rows.get(this.selectedRow).get(this.selectedColumn);
    }

    private void moveHorizontal(int amount) {
        List<Key> row = this.rows.get(this.selectedRow);
        for (int offset = 1; offset <= row.size(); offset++) {
            int candidate = Math.floorMod(this.selectedColumn + amount * offset, row.size());
            if (row.get(candidate).button.isActive()) {
                this.selectedColumn = candidate;
                this.refreshFocus();
                return;
            }
        }
    }

    private void moveVertical(int amount) {
        float position = this.navigationPosition(this.selectedRow, this.selectedColumn);
        for (int offset = 1; offset <= this.rows.size(); offset++) {
            int rowIndex = Math.floorMod(this.selectedRow + amount * offset, this.rows.size());
            List<Key> row = this.rows.get(rowIndex);
            int closest = -1;
            float distance = Float.MAX_VALUE;
            for (int column = 0; column < row.size(); column++) {
                if (!row.get(column).button.isActive()) continue;
                float candidateDistance = Math.abs(this.navigationPosition(rowIndex, column) - position);
                if (candidateDistance < distance) {
                    distance = candidateDistance;
                    closest = column;
                }
            }
            if (closest >= 0) {
                this.selectedRow = rowIndex;
                this.selectedColumn = closest;
                this.refreshFocus();
                return;
            }
        }
    }

    private float navigationPosition(int row, int column) {
        List<Key> keys = this.rows.get(row);
        Key key = keys.get(column);
        return key.button.getX() + key.button.getWidth() / 2.0F;
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
        boolean down = this.activator != null && this.activator.input().pressed(input);
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
        if (this.activator == null) return;
        long now = Util.getMillis();
        for (GamepadDigitalInput input : CONTROL_INPUTS) {
            ButtonLatch latch = this.latches.computeIfAbsent(input, ignored -> new ButtonLatch());
            latch.down = this.activator.input().pressed(input);
            latch.pressedAt = now;
            latch.lastRepeatAt = now;
        }
    }

    private int visibleStart(int width) {
        int start = 0;
        while (start < this.cursor && this.measure(this.value.substring(start, this.cursor)) > width) {
            start = this.value.offsetByCodePoints(start, 1);
        }
        return start;
    }

    private String visibleValue(int start, int width) {
        int end = start;
        while (end < this.value.length()) {
            int next = this.value.offsetByCodePoints(end, 1);
            if (this.measure(this.value.substring(start, next)) > width) break;
            end = next;
        }
        return this.value.substring(start, end);
    }

    private float measure(String value) {
        this.measurementText.setText(Component.literal(value));
        return this.measurementText.width();
    }

    private static TextElement text(String value) {
        TextElement text = new TextElement(Component.literal(value), 0.0F, 0.0F, null);
        text.setFont(ModFonts.Montserrat_bold);
        text.setScale(1.0F);
        text.setColor(NORMAL_TEXT_COLOR);
        text.setShadow(false);
        return text;
    }

    private enum Action {
        CHARACTER, CURSOR_LEFT, CURSOR_RIGHT, SYMBOLS, SECONDARY_PAGE,
        ACCENT, DONE, SPACE, BACKSPACE
    }

    private enum Style {
        KEY(QwertyMenu.KEY),
        SMALL(SPECIAL_SMALL),
        BIG(SPECIAL_BIG),
        SPACE(QwertyMenu.SPACE),
        BACKSPACE(QwertyMenu.BACKSPACE);

        private final Asset texture;

        Style(Asset texture) {
            this.texture = texture;
        }
    }

    private record Key(String label, Action action, Style style, Button button, TextElement text) {
    }

    private static final class ButtonLatch {
        private boolean down;
        private long pressedAt;
        private long lastRepeatAt;
    }

    public static final class Builder {
        private final Component title;
        private final GamepadTextInput input;
        private @Nullable Component description;
        private @Nullable IGamepad activator;
        private boolean currentAsLast;
        private @Nullable SystemOverlay lastMenu;

        private Builder(Component title, GamepadTextInput input) {
            this.title = Objects.requireNonNull(title, "title");
            this.input = Objects.requireNonNull(input, "input");
            this.activator = InputApi.currentGamepad();
        }

        public Builder description(Component description) {
            this.description = Objects.requireNonNull(description, "description");
            return this;
        }

        public Builder activator(@Nullable IGamepad activator) {
            this.activator = activator;
            return this;
        }

        public Builder lastMenu() {
            this.currentAsLast = true;
            this.lastMenu = null;
            return this;
        }

        public Builder lastMenu(SystemOverlay menu) {
            this.currentAsLast = false;
            this.lastMenu = Objects.requireNonNull(menu, "menu");
            return this;
        }

        public SystemUiHandle show() {
            int slotId = InputApi.currentSlotId();
            SystemUiTarget resolvedTarget = SystemUiTarget.viewport(slotId);
            QwertyMenu menu = new QwertyMenu(
                this.title, this.description, this.activator, this.input
            );
            ShowMenu request = SystemUI.showMenu(resolvedTarget, menu);
            if (this.currentAsLast) request.lastMenu();
            else if (this.lastMenu != null) request.lastMenu(this.lastMenu);
            return request.show();
        }
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
