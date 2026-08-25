package net.jr.screens.test;

import net.jr.client.components.widgets.Button;
import net.jr.client.components.widgets.ScrollPanelElement;
import net.jr.client.components.widgets.ScrollbarElement;
import net.jr.client.components.widgets.SliderElement;
import net.jr.client.components.widgets.TabBarElement;
import net.jr.client.components.widgets.TabElement;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class FactoryWidget {

    private FactoryWidget() {
    }

    public static Button button(
            int x,
            int y,
            int width,
            int height,
            Runnable action
    ) {
        return new Button.Builder(
                x,
                y,
                width,
                height
        )
                .onPress(button -> {
                    if (action != null) {
                        action.run();
                    }
                })
                .withRenderer(
                        (
                                gui,
                                widget,
                                mouseX,
                                mouseY,
                                partialTick
                        ) -> {
                            Button button = (Button)widget;

                            int color =
                                    button.isPressed()
                                            ? 0xFF5480D6
                                            : button.isHoveredOrFocused()
                                            ? 0xFF4568AA
                                            : 0xFF303846;

                            fillWidget(
                                    gui,
                                    button.getX(),
                                    button.getY(),
                                    button.getWidth(),
                                    button.getHeight(),
                                    color
                            );
                        }
                )
                .build();
    }

    public static ScrollbarElement horizontalScrollbar(
            int x,
            int y,
            int width,
            int height
    ) {
        ScrollbarElement scrollbar =
                new ScrollbarElement.Builder(
                        x,
                        y,
                        width,
                        height
                )
                        .withOrientation(
                                ScrollbarElement.Orientation.HORIZONTAL
                        )
                        .withHandleSize(28.0F)
                        .build();

        applyDefaultScrollbarRenderer(scrollbar);

        return scrollbar;
    }

    public static ScrollbarElement verticalScrollbar(
            int x,
            int y,
            int width,
            int height
    ) {
        ScrollbarElement scrollbar =
                new ScrollbarElement.Builder(
                        x,
                        y,
                        width,
                        height
                )
                        .withOrientation(
                                ScrollbarElement.Orientation.VERTICAL
                        )
                        .withHandleSize(28.0F)
                        .build();

        applyDefaultScrollbarRenderer(scrollbar);

        return scrollbar;
    }

    public static ScrollPanelElement verticalPanel(
            int x,
            int y,
            int width,
            int height,
            int children,
            int columns
    ) {
        ScrollPanelElement panel =
                new ScrollPanelElement.Builder(
                        x,
                        y,
                        width,
                        height
                )
                        .withOrientation(
                                ScrollPanelElement.Orientation.VERTICAL
                        )
                        .withPadding(8.0F)
                        .withSpacing(5.0F)
                        .withColumns(columns)
                        .withRenderer(
                                (
                                        gui,
                                        widget,
                                        mouseX,
                                        mouseY,
                                        partialTick
                                ) ->
                                        fillWidget(
                                                gui,
                                                widget.getX(),
                                                widget.getY(),
                                                widget.getWidth(),
                                                widget.getHeight(),
                                                0xFF191D25
                                        )
                        )
                        .build();

        applyDefaultScrollbarRenderer(
                panel.scrollbar()
        );

        for (int i = 0; i < children; i++) {
            panel.addChild(
                    button(
                            0,
                            0,
                            42 + (i % 3) * 6,
                            20,
                            () -> {}
                    )
            );
        }

        return panel;
    }

    public static ScrollPanelElement horizontalPanel(
            int x,
            int y,
            int width,
            int height,
            int children
    ) {
        ScrollPanelElement panel =
                new ScrollPanelElement.Builder(
                        x,
                        y,
                        width,
                        height
                )
                        .withOrientation(
                                ScrollPanelElement.Orientation.HORIZONTAL
                        )
                        .withPadding(8.0F)
                        .withSpacing(5.0F)
                        .withRenderer(
                                (
                                        gui,
                                        widget,
                                        mouseX,
                                        mouseY,
                                        partialTick
                                ) ->
                                        fillWidget(
                                                gui,
                                                widget.getX(),
                                                widget.getY(),
                                                widget.getWidth(),
                                                widget.getHeight(),
                                                0xFF191D25
                                        )
                        )
                        .build();

        applyDefaultScrollbarRenderer(
                panel.scrollbar()
        );

        for (int i = 0; i < children; i++) {
            panel.addChild(
                    button(
                            0,
                            0,
                            45,
                            24,
                            () -> {}
                    )
            );
        }

        return panel;
    }

    public static SliderElement numericSlider(
            int x,
            int y,
            int width,
            int height,
            double min,
            double max,
            double step
    ) {
        SliderElement slider =
                new SliderElement(
                        x,
                        y,
                        width,
                        height
                );

        slider.setRange(min, max);

        if (step > 0.0D) {
            slider.setStep(step);
        }

        slider.scrollbar().setHandleSize(18.0F);

        applyDefaultScrollbarRenderer(
                slider.scrollbar()
        );

        slider.setRenderer(
                (
                        gui,
                        widget,
                        mouseX,
                        mouseY,
                        partialTick
                ) ->
                        fillWidget(
                                gui,
                                widget.getX(),
                                widget.getY(),
                                widget.getWidth(),
                                widget.getHeight(),
                                0xFF151820
                        )
        );

        return slider;
    }

    public static SliderElement listSlider(
            int x,
            int y,
            int width,
            int height,
            String... values
    ) {
        SliderElement slider =
                new SliderElement(
                        x,
                        y,
                        width,
                        height
                );

        slider.setList(values);

        slider.scrollbar().setHandleSize(18.0F);

        applyDefaultScrollbarRenderer(
                slider.scrollbar()
        );

        slider.setRenderer(
                (
                        gui,
                        widget,
                        mouseX,
                        mouseY,
                        partialTick
                ) ->
                        fillWidget(
                                gui,
                                widget.getX(),
                                widget.getY(),
                                widget.getWidth(),
                                widget.getHeight(),
                                0xFF151820
                        )
        );

        return slider;
    }

    public static TabElement tab() {
        return new TabElement.Builder(
                0,
                0,
                42,
                24
        )
                .withRenderer(
                        (
                                gui,
                                widget,
                                mouseX,
                                mouseY,
                                partialTick
                        ) -> {
                            TabElement tab =
                                    (TabElement)widget;

                            int color =
                                    tab.isSelected()
                                            ? 0xFF596AC9
                                            : tab.isHovered()
                                            ? 0xFF404A69
                                            : 0xFF292F3E;

                            fillWidget(
                                    gui,
                                    tab.getX(),
                                    tab.getY(),
                                    tab.getWidth(),
                                    tab.getHeight(),
                                    color
                            );

                            int iconSize = 8;
                            fillWidget(
                                    gui,
                                    tab.getX() + (tab.getWidth() - iconSize) / 2,
                                    tab.getY() + (tab.getHeight() - iconSize) / 2,
                                    iconSize,
                                    iconSize,
                                    0xFFD9DDF0
                            );
                        }
                )
                .build();
    }

    public static TabBarElement tabBar(
            int x,
            int y
    ) {
        return new TabBarElement.Builder(
                x,
                y,
                42,
                24
        )
                .withOrientation(
                        TabBarElement.Orientation.HORIZONTAL
                )
                .withPadding(2.0F)
                .build();
    }

    public static void applyDefaultScrollbarRenderer(
            ScrollbarElement scrollbar
    ) {
        scrollbar.setRenderer(
                (
                        gui,
                        widget,
                        mouseX,
                        mouseY,
                        partialTick
                ) -> {
                    ScrollbarElement bar =
                            (ScrollbarElement)widget;

                    fillWidget(
                            gui,
                            Math.round(bar.trackX()),
                            Math.round(bar.trackY()),
                            Math.max(
                                    1,
                                    Math.round(bar.trackWidth())
                            ),
                            Math.max(
                                    1,
                                    Math.round(bar.trackHeight())
                            ),
                            0xFF343A48
                    );

                    fillWidget(
                            gui,
                            Math.round(bar.handleX()),
                            Math.round(bar.handleY()),
                            Math.max(
                                    1,
                                    Math.round(bar.handleWidth())
                            ),
                            Math.max(
                                    1,
                                    Math.round(bar.handleHeight())
                            ),
                            bar.isDraggingHandle()
                                    ? 0xFF80A4FF
                                    : 0xFF6487DD
                    );
                }
        );
    }

    private static void fillWidget(
            GuiGraphicsExtractor gui,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        gui.fill(
                x,
                y,
                x + width,
                y + height,
                color
        );
    }
}
