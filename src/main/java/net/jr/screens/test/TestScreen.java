package net.jr.screens.test;

import net.jr.client.components.widgets.Button;
import net.jr.client.components.widgets.ScrollPanelElement;
import net.jr.client.components.widgets.ScrollbarElement;
import net.jr.client.components.widgets.SliderElement;
import net.jr.client.components.widgets.TabBarElement;
import net.jr.client.components.widgets.TabElement;
import net.jr.client.components.widgets.WidgetElementGroup;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class TestScreen extends Screen {

    private final List<WidgetElementGroup> pages =
            new ArrayList<>();

    private TabBarElement tabBar;

    public TestScreen() {
        super(
                Component.literal(
                        "Java Reforged Test Screen"
                )
        );
    }

    @Override
    protected void init() {
        this.pages.clear();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        WidgetElementGroup groupPage =
                this.createGroupPage(
                        centerX,
                        centerY
                );

        WidgetElementGroup scrollbarPage =
                this.createScrollbarPage(
                        centerX,
                        centerY
                );

        WidgetElementGroup panelPage =
                this.createPanelPage(
                        centerX,
                        centerY
                );

        WidgetElementGroup sliderPage =
                this.createSliderPage(
                        centerX,
                        centerY
                );

        this.pages.add(groupPage);
        this.pages.add(scrollbarPage);
        this.pages.add(panelPage);
        this.pages.add(sliderPage);

        this.tabBar =
                FactoryWidget.tabBar(
                        centerX - 86,
                        centerY - 105
                );

        this.tabBar.onSelectionChange(
                tab -> this.showPage(
                        this.tabBar.selectedIndex()
                )
        );

        this.tabBar.addTab(FactoryWidget.tab());
        this.tabBar.addTab(FactoryWidget.tab());
        this.tabBar.addTab(FactoryWidget.tab());
        this.tabBar.addTab(FactoryWidget.tab());

        this.addRenderableWidget(this.tabBar);

        this.showPage(0);
    }

    private WidgetElementGroup createGroupPage(
            int centerX,
            int centerY
    ) {
        WidgetElementGroup page =
                new WidgetElementGroup();

        WidgetElementGroup selectionGroup =
                new WidgetElementGroup();

        int startX = centerX - 85;
        int y = centerY - 35;

        for (int i = 0; i < 4; i++) {
            Button button =
                    FactoryWidget.button(
                            startX + i * 45,
                            y,
                            40,
                            24,
                            () -> {}
                    );

            button.onPress =
                    ignored ->
                            selectionGroup.select(button);

            selectionGroup.add(button);
            page.add(button);

            this.addRenderableWidget(button);
        }

        selectionGroup.onSelectionChange(
                selected ->
                        selectionGroup.forEach(
                                widget ->
                                        widget.setAlpha(
                                                widget == selected
                                                        ? 1.0F
                                                        : 0.45F
                                        )
                        )
        );

        if (!selectionGroup.widgets().isEmpty()) {
            selectionGroup.select(
                    selectionGroup.widgets().getFirst()
            );
        }

        return page;
    }

    private WidgetElementGroup createScrollbarPage(
            int centerX,
            int centerY
    ) {
        WidgetElementGroup page =
                new WidgetElementGroup();

        ScrollbarElement horizontal =
                FactoryWidget.horizontalScrollbar(
                        centerX - 110,
                        centerY - 25,
                        220,
                        16
                );

        ScrollbarElement vertical =
                FactoryWidget.verticalScrollbar(
                        centerX - 8,
                        centerY + 5,
                        16,
                        90
                );

        page.add(horizontal);
        page.add(vertical);

        this.addRenderableWidget(horizontal);
        this.addRenderableWidget(vertical);

        return page;
    }

    private WidgetElementGroup createPanelPage(
            int centerX,
            int centerY
    ) {
        WidgetElementGroup page =
                new WidgetElementGroup();

        ScrollPanelElement vertical =
                FactoryWidget.verticalPanel(
                        centerX - 115,
                        centerY - 55,
                        230,
                        75,
                        30,
                        3
                );

        ScrollPanelElement horizontal =
                FactoryWidget.horizontalPanel(
                        centerX - 115,
                        centerY + 30,
                        230,
                        55,
                        16
                );

        page.add(vertical);
        page.add(horizontal);

        this.addRenderableWidget(vertical);
        this.addRenderableWidget(horizontal);

        return page;
    }

    private WidgetElementGroup createSliderPage(
            int centerX,
            int centerY
    ) {
        WidgetElementGroup page =
                new WidgetElementGroup();

        SliderElement numeric =
                FactoryWidget.numericSlider(
                        centerX - 110,
                        centerY - 50,
                        220,
                        18,
                        0.0D,
                        100.0D,
                        5.0D
                );

        numeric.setValue(50.0D);

        SliderElement list =
                FactoryWidget.listSlider(
                        centerX - 110,
                        centerY - 10,
                        220,
                        18,
                        "low",
                        "medium",
                        "high",
                        "ultra"
                );

        list.setSelectedValue("medium");

        SliderElement smooth =
                FactoryWidget.numericSlider(
                        centerX - 110,
                        centerY + 30,
                        220,
                        18,
                        0.0D,
                        1.0D,
                        0.0D
                );

        smooth.setValue(0.5D);

        SliderElement vertical =
                FactoryWidget.numericSlider(
                        centerX + 125,
                        centerY - 50,
                        18,
                        100,
                        0.0D,
                        10.0D,
                        1.0D
                );

        vertical.scrollbar().setOrientation(
                ScrollbarElement.Orientation.VERTICAL
        );

        vertical.scrollbar().resetTrack();
        vertical.scrollbar().setHandleSize(16.0F);

        page.add(numeric);
        page.add(list);
        page.add(smooth);
        page.add(vertical);

        this.addRenderableWidget(numeric);
        this.addRenderableWidget(list);
        this.addRenderableWidget(smooth);
        this.addRenderableWidget(vertical);

        return page;
    }

    private void showPage(int index) {
        for (int i = 0; i < this.pages.size(); i++) {
            WidgetElementGroup page =
                    this.pages.get(i);

            boolean visible = i == index;

            page.setVisible(visible);
            page.setActive(visible);
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        guiGraphics.fill(
                0,
                0,
                this.width,
                this.height,
                0xFF10131A
        );

        guiGraphics.fill(
                this.width / 2 - 135,
                this.height / 2 - 75,
                this.width / 2 + 135,
                this.height / 2 + 105,
                0xFF151922
        );

        super.extractRenderState(
                guiGraphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}