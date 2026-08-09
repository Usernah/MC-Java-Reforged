package net.jr.client.ui.hint.glyph;

import com.mojang.blaze3d.platform.InputConstants;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public enum KeyboardMouseGlyph {
    KEY("key"),
    KEY_0("key_0"),
    KEY_1("key_1"),
    KEY_2("key_2"),
    KEY_3("key_3"),
    KEY_4("key_4"),
    KEY_5("key_5"),
    KEY_6("key_6"),
    KEY_7("key_7"),
    KEY_8("key_8"),
    KEY_9("key_9"),
    KEY_A("key_a"),
    KEY_B("key_b"),
    KEY_C("key_c"),
    KEY_D("key_d"),
    KEY_E("key_e"),
    KEY_F("key_f"),
    KEY_G("key_g"),
    KEY_H("key_h"),
    KEY_I("key_i"),
    KEY_J("key_j"),
    KEY_K("key_k"),
    KEY_L("key_l"),
    KEY_M("key_m"),
    KEY_N("key_n"),
    KEY_O("key_o"),
    KEY_P("key_p"),
    KEY_Q("key_q"),
    KEY_R("key_r"),
    KEY_S("key_s"),
    KEY_T("key_t"),
    KEY_U("key_u"),
    KEY_V("key_v"),
    KEY_W("key_w"),
    KEY_X("key_x"),
    KEY_Y("key_y"),
    KEY_Z("key_z"),
    CHAR_APOSTROPHE("char_apostrophe"),
    CHAR_ASTERISK("char_asterisk"),
    CHAR_BACKSLASH("char_backslash"),
    CHAR_COMMA("char_comma"),
    CHAR_EQUAL("char_equal"),
    CHAR_GRAVE_ACCENT("char_grave_accent"),
    CHAR_LEFT_BRACKET("char_left_bracket"),
    CHAR_MINUS("char_minus"),
    CHAR_PERIOD("char_period"),
    CHAR_PLUS("char_plus"),
    CHAR_RIGHT_BRACKET("char_right_bracket"),
    CHAR_SEMICOLON("char_semicolon"),
    CHAR_SLASH("char_slash"),
    SPECIAL_ALT_LEFT("special_alt_left"),
    SPECIAL_ALT_RIGHT("special_alt_right"),
    SPECIAL_ARROW_DOWN("special_arrow_down"),
    SPECIAL_ARROW_LEFT("special_arrow_left"),
    SPECIAL_ARROW_RIGHT("special_arrow_right"),
    SPECIAL_ARROW_UP("special_arrow_up"),
    SPECIAL_CAPS_LOCK("special_caps_lock"),
    SPECIAL_CTRL_LEFT("special_ctrl_left"),
    SPECIAL_CTRL_RIGHT("special_ctrl_right"),
    SPECIAL_DELETE("special_delete"),
    SPECIAL_END("special_end"),
    SPECIAL_ENTER("special_enter"),
    SPECIAL_ESCAPE("special_escape"),
    SPECIAL_F1("special_f1"),
    SPECIAL_F2("special_f2"),
    SPECIAL_F3("special_f3"),
    SPECIAL_F4("special_f4"),
    SPECIAL_F5("special_f5"),
    SPECIAL_F6("special_f6"),
    SPECIAL_F7("special_f7"),
    SPECIAL_F8("special_f8"),
    SPECIAL_F9("special_f9"),
    SPECIAL_F10("special_f10"),
    SPECIAL_F11("special_f11"),
    SPECIAL_F12("special_f12"),
    SPECIAL_HOME("special_home"),
    SPECIAL_MENU("special_menu"),
    SPECIAL_NUM_LOCK("special_num_lock"),
    SPECIAL_PAGE_DOWN("special_page_down"),
    SPECIAL_PAGE_UP("special_page_up"),
    SPECIAL_PAUSE("special_pause"),
    SPECIAL_PRINT_SCREEN("special_print_screen"),
    SPECIAL_SCROLL_LOCK("special_scroll_lock"),
    SPECIAL_SHIFT_LEFT("special_shift_left"),
    SPECIAL_SHIFT_RIGHT("special_shift_right"),
    SPECIAL_SPACE("special_space"),
    SPECIAL_SUPER("special_super"),
    SPECIAL_TAB("special_tab"),
    SPECIAL_WORLD_1("special_world_1"),
    SPECIAL_WORLD_2("special_world_2"),
    MOUSE("mouse"),
    MOUSE_LEFT_CLICK("mouse_left_click"),
    MOUSE_MOVE_ALL("mouse_move_all"),
    MOUSE_RIGHT_CLICK("mouse_right_click"),
    MOUSE_SCROLL_ALL("mouse_scroll_all"),
    MOUSE_SCROLL_CLICK("mouse_scroll_click"),
    MOUSE_SCROLL_DOWN("mouse_scroll_down"),
    MOUSE_SCROLL_UP("mouse_scroll_up");

    private static final Map<String, KeyboardMouseGlyph> INPUT_NAMES = new HashMap<>();

    static {
        registerLettersAndNumbers();
        registerSymbols();
        registerSpecialKeys();
        registerKeypadAliases();
        registerMouse();
    }

    private final String fileName;

    KeyboardMouseGlyph(String fileName) {
        this.fileName = fileName;
    }

    public String fileName() {
        return this.fileName;
    }

    public static KeyboardMouseGlyph fallback() {
        return KEY;
    }

    public static KeyboardMouseGlyph fromKey(InputConstants.Key key) {
        KeyboardMouseGlyph glyph = fromInputName(key.getName());
        return glyph == null ? fallback() : glyph;
    }

    @Nullable
    public static KeyboardMouseGlyph fromInputName(String inputName) {
        return INPUT_NAMES.get(inputName);
    }

    private static void registerLettersAndNumbers() {
        register("key.keyboard.0", KEY_0);
        register("key.keyboard.1", KEY_1);
        register("key.keyboard.2", KEY_2);
        register("key.keyboard.3", KEY_3);
        register("key.keyboard.4", KEY_4);
        register("key.keyboard.5", KEY_5);
        register("key.keyboard.6", KEY_6);
        register("key.keyboard.7", KEY_7);
        register("key.keyboard.8", KEY_8);
        register("key.keyboard.9", KEY_9);
        register("key.keyboard.a", KEY_A);
        register("key.keyboard.b", KEY_B);
        register("key.keyboard.c", KEY_C);
        register("key.keyboard.d", KEY_D);
        register("key.keyboard.e", KEY_E);
        register("key.keyboard.f", KEY_F);
        register("key.keyboard.g", KEY_G);
        register("key.keyboard.h", KEY_H);
        register("key.keyboard.i", KEY_I);
        register("key.keyboard.j", KEY_J);
        register("key.keyboard.k", KEY_K);
        register("key.keyboard.l", KEY_L);
        register("key.keyboard.m", KEY_M);
        register("key.keyboard.n", KEY_N);
        register("key.keyboard.o", KEY_O);
        register("key.keyboard.p", KEY_P);
        register("key.keyboard.q", KEY_Q);
        register("key.keyboard.r", KEY_R);
        register("key.keyboard.s", KEY_S);
        register("key.keyboard.t", KEY_T);
        register("key.keyboard.u", KEY_U);
        register("key.keyboard.v", KEY_V);
        register("key.keyboard.w", KEY_W);
        register("key.keyboard.x", KEY_X);
        register("key.keyboard.y", KEY_Y);
        register("key.keyboard.z", KEY_Z);
    }

    private static void registerSymbols() {
        register("key.keyboard.apostrophe", CHAR_APOSTROPHE);
        register("key.keyboard.backslash", CHAR_BACKSLASH);
        register("key.keyboard.comma", CHAR_COMMA);
        register("key.keyboard.equal", CHAR_EQUAL);
        register("key.keyboard.grave.accent", CHAR_GRAVE_ACCENT);
        register("key.keyboard.left.bracket", CHAR_LEFT_BRACKET);
        register("key.keyboard.minus", CHAR_MINUS);
        register("key.keyboard.period", CHAR_PERIOD);
        register("key.keyboard.right.bracket", CHAR_RIGHT_BRACKET);
        register("key.keyboard.semicolon", CHAR_SEMICOLON);
        register("key.keyboard.slash", CHAR_SLASH);
    }

    private static void registerSpecialKeys() {
        register("key.keyboard.down", SPECIAL_ARROW_DOWN);
        register("key.keyboard.left", SPECIAL_ARROW_LEFT);
        register("key.keyboard.right", SPECIAL_ARROW_RIGHT);
        register("key.keyboard.up", SPECIAL_ARROW_UP);
        register("key.keyboard.space", SPECIAL_SPACE);
        register("key.keyboard.tab", SPECIAL_TAB);
        register("key.keyboard.left.alt", SPECIAL_ALT_LEFT);
        register("key.keyboard.left.control", SPECIAL_CTRL_LEFT);
        register("key.keyboard.left.shift", SPECIAL_SHIFT_LEFT);
        register("key.keyboard.left.win", SPECIAL_SUPER);
        register("key.keyboard.right.alt", SPECIAL_ALT_RIGHT);
        register("key.keyboard.right.control", SPECIAL_CTRL_RIGHT);
        register("key.keyboard.right.shift", SPECIAL_SHIFT_RIGHT);
        register("key.keyboard.right.win", SPECIAL_SUPER);
        register("key.keyboard.enter", SPECIAL_ENTER);
        register("key.keyboard.escape", SPECIAL_ESCAPE);
        register("key.keyboard.backspace", SPECIAL_DELETE);
        register("key.keyboard.delete", SPECIAL_DELETE);
        register("key.keyboard.end", SPECIAL_END);
        register("key.keyboard.home", SPECIAL_HOME);
        register("key.keyboard.insert", KEY);
        register("key.keyboard.page.down", SPECIAL_PAGE_DOWN);
        register("key.keyboard.page.up", SPECIAL_PAGE_UP);
        register("key.keyboard.caps.lock", SPECIAL_CAPS_LOCK);
        register("key.keyboard.pause", SPECIAL_PAUSE);
        register("key.keyboard.scroll.lock", SPECIAL_SCROLL_LOCK);
        register("key.keyboard.num.lock", SPECIAL_NUM_LOCK);
        register("key.keyboard.menu", SPECIAL_MENU);
        register("key.keyboard.print.screen", SPECIAL_PRINT_SCREEN);
        register("key.keyboard.world.1", SPECIAL_WORLD_1);
        register("key.keyboard.world.2", SPECIAL_WORLD_2);
        registerFunctionKeys();
    }

    private static void registerFunctionKeys() {
        register("key.keyboard.f1", SPECIAL_F1);
        register("key.keyboard.f2", SPECIAL_F2);
        register("key.keyboard.f3", SPECIAL_F3);
        register("key.keyboard.f4", SPECIAL_F4);
        register("key.keyboard.f5", SPECIAL_F5);
        register("key.keyboard.f6", SPECIAL_F6);
        register("key.keyboard.f7", SPECIAL_F7);
        register("key.keyboard.f8", SPECIAL_F8);
        register("key.keyboard.f9", SPECIAL_F9);
        register("key.keyboard.f10", SPECIAL_F10);
        register("key.keyboard.f11", SPECIAL_F11);
        register("key.keyboard.f12", SPECIAL_F12);
    }

    private static void registerKeypadAliases() {
        register("key.keyboard.keypad.0", KEY_0);
        register("key.keyboard.keypad.1", KEY_1);
        register("key.keyboard.keypad.2", KEY_2);
        register("key.keyboard.keypad.3", KEY_3);
        register("key.keyboard.keypad.4", KEY_4);
        register("key.keyboard.keypad.5", KEY_5);
        register("key.keyboard.keypad.6", KEY_6);
        register("key.keyboard.keypad.7", KEY_7);
        register("key.keyboard.keypad.8", KEY_8);
        register("key.keyboard.keypad.9", KEY_9);
        register("key.keyboard.keypad.add", CHAR_PLUS);
        register("key.keyboard.keypad.decimal", CHAR_PERIOD);
        register("key.keyboard.keypad.enter", SPECIAL_ENTER);
        register("key.keyboard.keypad.equal", CHAR_EQUAL);
        register("key.keyboard.keypad.multiply", CHAR_ASTERISK);
        register("key.keyboard.keypad.divide", CHAR_SLASH);
        register("key.keyboard.keypad.subtract", CHAR_MINUS);
    }

    private static void registerMouse() {
        register("key.mouse.left", MOUSE_LEFT_CLICK);
        register("key.mouse.right", MOUSE_RIGHT_CLICK);
        register("key.mouse.middle", MOUSE_SCROLL_CLICK);
        register("key.mouse.100", MOUSE_SCROLL_UP);
        register("key.mouse.101", MOUSE_SCROLL_DOWN);
        register("key.mouse.4", MOUSE);
        register("key.mouse.5", MOUSE);
        register("key.mouse.6", MOUSE);
        register("key.mouse.7", MOUSE);
        register("key.mouse.8", MOUSE);
    }

    private static void register(String inputName, KeyboardMouseGlyph glyph) {
        INPUT_NAMES.put(inputName, glyph);
    }
}
