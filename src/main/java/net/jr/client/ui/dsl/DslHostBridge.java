package net.jr.client.ui.dsl;

import net.jr.client.components.widgets.JavaBaseWidget;

/** Internal bridge shared by every Minecraft UI host that accepts DSL widgets. */
public interface DslHostBridge {
    void javaReforged$addDslWidget(JavaBaseWidget widget);
}
