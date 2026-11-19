package net.jr.client.ui.dsl;

import net.jr.client.components.elements.VisualAnchor;
import net.jr.client.ui.dsl.UiCompiledDocument.Expression;
import net.jr.client.ui.dsl.UiCompiledDocument.RectangleDefinition;
import net.jr.client.ui.dsl.UiDocument.Node;
import net.jr.client.ui.dsl.UiDocument.Property;
import net.jr.client.ui.dsl.UiDocument.Value;

import java.util.List;

final class UiRectangleCompiler implements UiElementCompiler {
    private final UiElementContext context;

    UiRectangleCompiler(UiElementContext context) {
        this.context = context;
    }

    @Override
    public String name() {
        return "rectangle";
    }

    @Override
    public RectangleDefinition compile(
        UiDocument document,
        Node node,
        boolean allowThis
    ) throws UiCompileException {
        String id = this.context.id(document, node);
        UiNodeBody body = this.context.body(
            document,
            node,
            List.of("anchor", "x", "y", "w", "h", "color"),
            List.of()
        );
        Expression<VisualAnchor> anchor = body.property("anchor") == null
            ? this.context.expressions().constant(VisualAnchor.NONE)
            : this.context.expressions().visualAnchor(document, body.property("anchor"), allowThis, 0);
        Expression<Float> x = this.context.expressions().number(document, body.require("x"), allowThis, 0);
        Expression<Float> y = this.context.expressions().number(document, body.require("y"), allowThis, 0);
        Expression<Float> width = this.context.expressions().number(document, body.require("w"), allowThis, 0);
        Expression<Float> height = this.context.expressions().number(document, body.require("h"), allowThis, 0);

        Value colorValue = body.require("color");
        List<Value> colors = this.context.expressions().tupleValues(colorValue);
        Expression<Integer> topColor;
        Expression<Integer> bottomColor = null;
        if (colors == null) {
            topColor = this.context.expressions().color(document, colorValue, allowThis, 0);
        } else {
            if (colors.size() != 2) {
                throw this.context.error(document, colorValue.position(),
                    "Rectangle color requires one color or a pair of gradient colors");
            }
            topColor = this.context.expressions().color(document, colors.get(0), allowThis, 0);
            bottomColor = this.context.expressions().color(document, colors.get(1), allowThis, 0);
        }

        return new RectangleDefinition(
            id, anchor, x, y, width, height, topColor, bottomColor
        );
    }

}

