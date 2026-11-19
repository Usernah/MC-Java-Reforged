package net.jr.client.systemui.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.jr.api.client.input.GamepadDigitalInput;
import net.jr.api.client.input.IGamepad;
import net.jr.api.client.render.Draw;
import net.jr.api.client.resource.Asset;
import net.jr.api.client.systemui.AbstractSystemPanel;
import net.jr.api.client.systemui.SystemUI;
import net.jr.api.client.systemui.SystemUiCloseReason;
import net.jr.api.client.systemui.SystemUiHandle;
import net.jr.api.client.systemui.SystemUiRenderContext;
import net.jr.api.client.systemui.SystemUiTarget;
import net.jr.client.components.elements.TextElement;
import net.jr.client.components.widgets.Button;
import net.jr.client.components.widgets.ScrollPanelElement;
import net.jr.client.user.UserManager;
import net.jr.client.user.UserProfile;
import net.jr.client.systemui.SystemUiSounds;
import net.jr.registry.ModFonts;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

public final class SignInMenu extends AbstractSystemPanel {
    private static final int PANEL_WIDTH = 1006;
    private static final int PANEL_HEIGHT = 751;
    private static final int LIST_X = 3;
    private static final int LIST_Y = 92;
    private static final int LIST_WIDTH = 1000;
    private static final int LIST_HEIGHT = 656;
    private static final int BUTTON_WIDTH = 1000;
    private static final int BUTTON_HEIGHT = 142;
    private static final int ICON_X = 20;
    private static final int ICON_Y = 8;
    private static final int ICON_SIZE = 124;
    private static final int TEXT_X = 172;
    private static final int TEXT_Y = 47;
    private static final int TEXT_COLOR = 0xFF636060;
    private static final int TEXT_HOVER_COLOR = 0xFFE4E4E4;
    private static final long CONTENT_FADE_DURATION_MS = 150L;
    private static final long SIGN_IN_DISPLAY_DURATION_MS = 1_000L;
    private static final Asset PANEL = Asset.MOD("textures/system/panels/sign_in.png");
    private static final Asset SELECTED_BUTTON =
        Asset.MOD("textures/system/sign_in_menu/sel_bt.png");
    private static final Asset UNSELECTED_BUTTON =
        Asset.MOD("textures/system/sign_in_menu/un_bt.png");
    private static final Asset CREATE_USER_ICON =
        Asset.MOD("textures/system/sign_in_menu/create_usser_icon.png");

    private final IGamepad activator;
    private final Consumer<UserProfile> selection;
    private final ScrollPanelElement userList = new ScrollPanelElement(
        0, 0, LIST_WIDTH, LIST_HEIGHT
    );
    private final List<Entry> entries = new ArrayList<>();
    private SystemUiHandle handle;
    private int selectedIndex;
    private double pointerX = Integer.MIN_VALUE;
    private double pointerY = Integer.MIN_VALUE;
    private boolean upDown;
    private boolean downDown;
    private boolean acceptDown;
    private boolean backDown;
    private UserProfile pendingUser;
    private long signInStartedAtMs;
    private boolean joinRequested;

    public SignInMenu(IGamepad activator, Consumer<UserProfile> selection) {
        super(Component.literal("Iniciar sesión"), PANEL_WIDTH, PANEL_HEIGHT);
        this.activator = Objects.requireNonNull(activator, "activator");
        this.selection = Objects.requireNonNull(selection, "selection");
        this.userList.setOrientation(ScrollPanelElement.Orientation.VERTICAL);
        this.userList.setColumns(1);
        this.userList.setSpacing(0.0F);
        this.userList.setPadding(0.0F);
        this.rebuildEntries(null);
    }

    public static SystemUiHandle open(
        IGamepad activator,
        Consumer<UserProfile> selection
    ) {
        return SystemUI.showMenu(
            SystemUiTarget.fullWindow(0, activator),
            new SignInMenu(activator, selection)
        ).show();
    }

    @Override
    public void opened(SystemUiHandle handle) {
        super.opened(handle);
        this.handle = handle;
        this.syncButtons();
        this.updateSelection();
    }

    @Override
    public void tick() {
        if (!this.activator.connected()) {
            this.handle.close(SystemUiCloseReason.CANCELLED);
            return;
        }
        if (this.isOpening()) {
            this.syncButtons();
            return;
        }
        if (this.pendingUser != null) {
            this.tickSignIn();
            return;
        }

        boolean up = this.pressed(GamepadDigitalInput.DPAD_UP)
            || this.pressed(GamepadDigitalInput.STICK_LEFT_MOVE_UP);
        boolean down = this.pressed(GamepadDigitalInput.DPAD_DOWN)
            || this.pressed(GamepadDigitalInput.STICK_LEFT_MOVE_DOWN);
        boolean accept = this.pressed(GamepadDigitalInput.BUTTON_DOWN);
        boolean back = this.pressed(GamepadDigitalInput.BUTTON_RIGHT);

        if (up && !this.upDown) {
            this.moveSelection(-1);
        } else if (down && !this.downDown) {
            this.moveSelection(1);
        }
        if (accept && !this.acceptDown) {
            this.activateSelected();
        } else if (back && !this.backDown) {
            this.handle.back();
        }

        this.upDown = up;
        this.downDown = down;
        this.acceptDown = accept;
        this.backDown = back;
    }

    @Override
    protected void renderPanel(SystemUiRenderContext context, Bounds bounds) {
        Draw.image(PANEL, bounds.x(), bounds.y(), bounds.width(), bounds.height())
            .pixelSnap()
            .draw(context.graphics());
    }

    @Override
    protected void renderContent(SystemUiRenderContext context, Bounds bounds) {
        float listAlpha = this.listAlpha();
        this.userList.setBounds(
            bounds.x() + LIST_X,
            bounds.y() + LIST_Y,
            LIST_WIDTH,
            LIST_HEIGHT
        );
        this.userList.extractRenderState(
            context.graphics(),
            (int)Math.round(this.pointerX),
            (int)Math.round(this.pointerY),
            context.deltaTracker().getGameTimeDeltaTicks()
        );
        if (this.pendingUser != null) {
            this.renderSigningIn(context, bounds, 1.0F - listAlpha);
        }
    }

    @Override
    protected void onMouseMoved(double x, double y) {
        if (this.pendingUser != null) {
            return;
        }
        this.pointerX = x;
        this.pointerY = y;
    }

    @Override
    protected void onMouseButton(double x, double y, int button, int action) {
        if (this.pendingUser != null) {
            return;
        }
        MouseButtonEvent event = new MouseButtonEvent(
            x,
            y,
            new MouseButtonInfo(button, 0)
        );
        if (action == GLFW.GLFW_PRESS) {
            this.userList.mouseClicked(event, false);
        } else if (action == GLFW.GLFW_RELEASE) {
            this.userList.mouseReleased(event);
        }
    }

    @Override
    protected void onMouseScrolled(
        double x,
        double y,
        double horizontal,
        double vertical
    ) {
        if (this.pendingUser != null) {
            return;
        }
        this.userList.mouseScrolled(x, y, horizontal, vertical);
    }

    private void rebuildEntries(UserProfile preferredSelection) {
        this.userList.clearChildren();
        this.entries.clear();
        for (UserProfile user : UserManager.get().users()) {
            this.addEntry(user, Component.literal(user.displayName()), Asset.PARSE(user.icon().texture()));
        }
        this.addEntry(null, Component.literal("Crear usuario"), CREATE_USER_ICON);

        if (preferredSelection != null) {
            for (int index = 0; index < this.entries.size(); index++) {
                UserProfile user = this.entries.get(index).user;
                if (user != null && user.id().equals(preferredSelection.id())) {
                    this.selectedIndex = index;
                    break;
                }
            }
        }
        this.selectedIndex = Math.clamp(this.selectedIndex, 0, this.entries.size() - 1);
        this.updateSelection();
    }

    private void addEntry(UserProfile user, Component label, Asset icon) {
        TextElement text = new TextElement(label, 0.0F, 0.0F, null);
        text.setFont(ModFonts.Montserrat_bold);
        text.setScale(1.0F);
        text.setAlign(Draw.TextAlign.LEFT);
        text.setShadow(false);

        final Entry[] holder = new Entry[1];
        Button button = new Button.Builder(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT)
            .onPress(ignored -> this.activate(holder[0]))
            .withRenderer((graphics, widget, mouseX, mouseY, partialTick) -> {
                boolean highlighted = widget.isFocused() || widget.isHovered();
                SystemUiSounds.updateFocus(widget, highlighted);
                float alpha = this.listAlpha();
                Draw.image(
                    highlighted ? SELECTED_BUTTON : UNSELECTED_BUTTON,
                    widget.getX(),
                    widget.getY(),
                    widget.getWidth(),
                    widget.getHeight()
                ).alpha(alpha).draw(graphics);
                text.setColor(highlighted ? TEXT_HOVER_COLOR : TEXT_COLOR);
                text.setAlpha(alpha);
                Draw.image(
                    icon,
                    widget.getX() + ICON_X,
                    widget.getY() + ICON_Y,
                    ICON_SIZE,
                    ICON_SIZE
                ).alpha(alpha).draw(graphics);
                text.setPosition(widget.getX() + TEXT_X, widget.getY() + TEXT_Y);
                text.draw(graphics);
            })
            .withSound((handler, widget, event) -> {
            })
            .build();
        Entry entry = new Entry(user, button);
        holder[0] = entry;
        this.entries.add(entry);
        this.userList.addChild(button);
    }

    private void moveSelection(int amount) {
        this.selectedIndex = Math.floorMod(this.selectedIndex + amount, this.entries.size());
        this.updateSelection();
    }

    private void updateSelection() {
        if (this.entries.isEmpty()) {
            return;
        }
        for (int index = 0; index < this.entries.size(); index++) {
            this.entries.get(index).button.setFocused(index == this.selectedIndex);
        }
        this.userList.ensureChildVisible(this.entries.get(this.selectedIndex).button);
    }

    private void activateSelected() {
        this.activate(this.entries.get(this.selectedIndex));
    }

    private void activate(Entry entry) {
        SystemUiSounds.playSelectButton();
        if (entry.user == null) {
            UserProfile created = UserManager.get().createRandomTestUser();
            this.rebuildEntries(created);
            return;
        }
        this.pendingUser = entry.user;
        this.signInStartedAtMs = Util.getMillis();
        this.joinRequested = false;
        this.userList.cancelInteraction();
        this.syncButtons();
    }

    private void tickSignIn() {
        if (this.joinRequested
                || Util.getMillis() - this.signInStartedAtMs
                    < CONTENT_FADE_DURATION_MS + SIGN_IN_DISPLAY_DURATION_MS) {
            return;
        }
        this.joinRequested = true;
        UserProfile selectedUser = this.pendingUser;
        this.selection.accept(selectedUser);
        UserManager.get().markUsed(selectedUser);
        this.handle.close(SystemUiCloseReason.COMPLETED);
    }

    private float listAlpha() {
        if (this.pendingUser == null) {
            return 1.0F;
        }
        return 1.0F - Math.clamp(
            (Util.getMillis() - this.signInStartedAtMs) / (float)CONTENT_FADE_DURATION_MS,
            0.0F,
            1.0F
        );
    }

    private void renderSigningIn(
        SystemUiRenderContext context,
        Bounds bounds,
        float alpha
    ) {
        int textColor = Math.round(Math.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24
            | 0x00636060;
        Draw.TextBuilder text = Draw.text(Component.literal("Iniciando sesión"))
            .font(ModFonts.Montserrat_bold)
            .scale(1.0F)
            .align(Draw.TextAlign.CENTER)
            .shadow(false)
            .color(textColor);
        text.position(
            bounds.x() + PANEL_WIDTH / 2,
            bounds.y() + LIST_Y + (LIST_HEIGHT - text.height()) / 2
        );
        text.draw(context.graphics());
    }

    private boolean pressed(GamepadDigitalInput input) {
        return this.activator.input().pressed(input);
    }

    private void syncButtons() {
        this.upDown = this.pressed(GamepadDigitalInput.DPAD_UP)
            || this.pressed(GamepadDigitalInput.STICK_LEFT_MOVE_UP);
        this.downDown = this.pressed(GamepadDigitalInput.DPAD_DOWN)
            || this.pressed(GamepadDigitalInput.STICK_LEFT_MOVE_DOWN);
        this.acceptDown = this.pressed(GamepadDigitalInput.BUTTON_DOWN);
        this.backDown = this.pressed(GamepadDigitalInput.BUTTON_RIGHT);
    }

    private record Entry(UserProfile user, Button button) {
    }
}
