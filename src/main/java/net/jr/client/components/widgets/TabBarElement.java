package net.jr.client.components.widgets;

import net.jr.client.components.interfaces.IWidgetRenderer;
import net.jr.client.components.navigation.TabNavigator;
import net.jr.client.components.navigation.UiAction;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class TabBarElement extends JavaBaseWidget implements TabNavigator {

    public enum Orientation {
        HORIZONTAL,
        VERTICAL
    }

    private final List<TabElement> tabs =
            new ArrayList<>();

    private final WidgetElementGroup group =
            new WidgetElementGroup();

    private final float tabWidth;
    private final float tabHeight;

    private Orientation orientation =
            Orientation.HORIZONTAL;

    private float padding;

    private int positionRoleCapacity;

    private int selectedIndex = -1;

    private Consumer<TabElement> onSelectionChange =
            ignored -> {};

    private IWidgetRenderer renderer;

    private TabElement pressedTab;

    public TabBarElement(
            int x,
            int y,
            int tabWidth,
            int tabHeight
    ) {
        super(
                x,
                y,
                tabWidth,
                tabHeight,
                Component.empty()
        );

        this.tabWidth = tabWidth;
        this.tabHeight = tabHeight;

        this.group.onSelectionChange(widget -> {
            TabElement tab =
                    widget instanceof TabElement element
                            ? element
                            : null;

            this.applySelection(tab);
        });
    }

    public void addTab(TabElement tab) {
        if (tab == null || this.tabs.contains(tab)) {
            return;
        }

        tab.setSelectionRequest(this::select);

        this.tabs.add(tab);
        this.group.add(tab);

        this.layoutTabs();

        if (this.selectedIndex < 0) {
            this.select(tab);
        }
    }

    public void removeTab(TabElement tab) {
        int previousIndex =
                this.tabs.indexOf(tab);

        if (previousIndex < 0) {
            return;
        }

        boolean wasSelected =
                this.selected() == tab;

        this.tabs.remove(tab);
        this.group.remove(tab);

        tab.setSelectionRequest(null);

        this.layoutTabs();

        if (wasSelected && !this.tabs.isEmpty()) {
            this.select(
                    Math.min(
                            previousIndex,
                            this.tabs.size() - 1
                    )
            );
        }
    }

    public void clearTabs() {
        for (TabElement tab : this.tabs) {
            tab.setSelectionRequest(null);
        }

        this.tabs.clear();
        this.group.clear();

        this.selectedIndex = -1;
        this.layoutTabs();
    }

    public List<TabElement> tabs() {
        return Collections.unmodifiableList(this.tabs);
    }

    public void select(TabElement tab) {
        if (tab != null) {
            this.group.select(tab);
        }
    }

    public void select(int index) {
        if (index < 0 || index >= this.tabs.size()) {
            return;
        }

        this.select(this.tabs.get(index));
    }

    public TabElement selected() {
        return this.selectedIndex >= 0
                && this.selectedIndex < this.tabs.size()
                ? this.tabs.get(this.selectedIndex)
                : null;
    }

    public int selectedIndex() {
        return this.selectedIndex;
    }

    public void onSelectionChange(
            Consumer<TabElement> action
    ) {
        this.onSelectionChange =
                action == null
                        ? ignored -> {}
                        : action;
    }

    public Orientation orientation() {
        return this.orientation;
    }

    public void setOrientation(
            Orientation orientation
    ) {
        this.orientation =
                orientation == null
                        ? Orientation.HORIZONTAL
                        : orientation;

        this.layoutTabs();
    }

    public float padding() {
        return this.padding;
    }

    public void setPadding(float padding) {
        this.padding = padding;
        this.layoutTabs();
    }

    /**
     * Sets the number of visual positions in a complete tab row. A partially
     * populated row keeps its final present tab as MIDDLE; LAST is reserved for
     * the final position of a complete row.
     */
    public void setPositionRoleCapacity(int capacity) {
        this.positionRoleCapacity = Math.max(0, capacity);
        this.layoutTabs();
    }

    public void setRenderer(IWidgetRenderer renderer) {
        this.renderer = renderer;
    }

    public boolean selectNext() {
        return this.selectRelative(1);
    }

    public boolean selectPrevious() {
        return this.selectRelative(-1);
    }

    private boolean selectRelative(int delta) {
        if (this.tabs.isEmpty()) {
            return false;
        }

        int current =
                this.selectedIndex < 0
                        ? 0
                        : this.selectedIndex;

        int next =
                Math.floorMod(
                        current + delta,
                        this.tabs.size()
                );

        if (next == this.selectedIndex) {
            return false;
        }

        this.select(next);
        return true;
    }

    @Override
    public boolean handleUiAction(UiAction action) {
        if (!this.active || this.tabs.isEmpty()) {
            return false;
        }

        if (this.orientation == Orientation.HORIZONTAL) {
            if (action == UiAction.MOVE_LEFT) {
                return this.selectPrevious();
            }

            if (action == UiAction.MOVE_RIGHT) {
                return this.selectNext();
            }
        } else {
            if (action == UiAction.MOVE_UP) {
                return this.selectPrevious();
            }

            if (action == UiAction.MOVE_DOWN) {
                return this.selectNext();
            }
        }

        return false;
    }

    @Override
    protected void extractWidgetRenderState(
            GuiGraphicsExtractor guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (!this.visible()) {
            return;
        }

        if (this.renderer != null) {
            this.renderer.render(
                    guiGraphics,
                    this,
                    mouseX,
                    mouseY,
                    partialTick
            );
        }

        for (TabElement tab : this.tabs) {
            tab.extractRenderState(
                    guiGraphics,
                    mouseX,
                    mouseY,
                    partialTick
            );
        }
    }

    @Override
    public boolean mouseClicked(
            MouseButtonEvent event,
            boolean doubleClick
    ) {
        for (int i = this.tabs.size() - 1;
             i >= 0;
             i--) {

            TabElement tab = this.tabs.get(i);

            if (tab.mouseClicked(
                    event,
                    doubleClick
            )) {
                this.pressedTab = tab;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.pressedTab == null) {
            return false;
        }

        TabElement tab = this.pressedTab;
        this.pressedTab = null;

        return tab.mouseReleased(event);
    }

    private void applySelection(TabElement selected) {
        this.selectedIndex = -1;

        for (int i = 0; i < this.tabs.size(); i++) {
            TabElement tab = this.tabs.get(i);

            boolean isSelected =
                    tab == selected;

            tab.setSelected(isSelected);

            if (isSelected) {
                this.selectedIndex = i;
            }
        }

        if (selected != null) {
            this.onSelectionChange.accept(selected);
        }
    }

    private void layoutTabs() {
        for (int i = 0; i < this.tabs.size(); i++) {
            TabElement tab = this.tabs.get(i);

            float tabX = this.x();
            float tabY = this.y();

            if (this.orientation == Orientation.HORIZONTAL) {
                tabX +=
                        i * (this.tabWidth + this.padding);
            } else {
                tabY +=
                        i * (this.tabHeight + this.padding);
            }

            tab.setBounds(
                    tabX,
                    tabY,
                    this.tabWidth,
                    this.tabHeight
            );

            tab.setPositionRole(
                    this.resolveRole(i)
            );
        }

        if (this.tabs.isEmpty()) {
            this.setSize(
                    this.tabWidth,
                    this.tabHeight
            );

            return;
        }

        if (this.orientation == Orientation.HORIZONTAL) {
            this.setSize(
                    this.tabs.size() * this.tabWidth
                            + (this.tabs.size() - 1) * this.padding,
                    this.tabHeight
            );
        } else {
            this.setSize(
                    this.tabWidth,
                    this.tabs.size() * this.tabHeight
                            + (this.tabs.size() - 1) * this.padding
            );
        }
    }

    private TabElement.PositionRole resolveRole(
            int index
    ) {
        int capacity = this.positionRoleCapacity > 0
                ? this.positionRoleCapacity
                : this.tabs.size();

        if (capacity == 1 && this.tabs.size() == 1) {
            return TabElement.PositionRole.SINGLE;
        }

        if (index == 0) {
            return TabElement.PositionRole.FIRST;
        }

        if (index == capacity - 1) {
            return TabElement.PositionRole.LAST;
        }

        return TabElement.PositionRole.MIDDLE;
    }

    @Override
    public boolean shouldTakeFocusAfterInteraction() {
        return false;
    }

    @Override
    public boolean participatesInFocusNavigation() {
        return false;
    }

    @Override
    public ComponentPath nextFocusPath(
            FocusNavigationEvent event
    ) {
        return null;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(false);
    }

    public static final class Builder {

        private final TabBarElement instance;

        public Builder(
                int x,
                int y,
                int tabWidth,
                int tabHeight
        ) {
            this.instance =
                    new TabBarElement(
                            x,
                            y,
                            tabWidth,
                            tabHeight
                    );
        }

        public Builder withOrientation(
                Orientation orientation
        ) {
            this.instance.setOrientation(orientation);
            return this;
        }

        public Builder withPadding(float padding) {
            this.instance.setPadding(padding);
            return this;
        }

        public Builder withRenderer(
                IWidgetRenderer renderer
        ) {
            this.instance.setRenderer(renderer);
            return this;
        }

        public TabBarElement build() {
            return this.instance;
        }
    }
}
