package net.jr.client.ui.dsl;

import net.jr.api.client.render.Draw;
import net.jr.client.ui.dsl.UiCompiledDocument.Expression;
import net.jr.client.ui.dsl.UiCompiledDocument.NineDefinition;
import net.jr.client.ui.dsl.UiDocument.Node;
import net.jr.client.ui.dsl.UiDocument.Property;
import net.jr.client.ui.dsl.UiDocument.Value;

import java.util.List;

final class UiNineCompiler {
    private final UiElementContext context;

    UiNineCompiler(UiElementContext context) {
        this.context = context;
    }

    NineDefinition compile(UiDocument document, Node node, boolean allowThis)
        throws UiCompileException {
        if (node == null) {
            return null;
        }
        UiNodeBody body = this.context.body(document, node, List.of("borders", "mode"), List.of());
        Value borders = body.require("borders");
        List<Value> values = this.context.expressions().tupleValues(borders);
        Expression<Integer> top;
        Expression<Integer> bottom;
        Expression<Integer> left;
        Expression<Integer> right;
        if (values == null) {
            Expression<Integer> border = this.context.expressions().integer(document, borders, allowThis, 0);
            top = bottom = left = right = border;
        } else {
            if (values.size() != 4) {
                throw this.context.error(document, borders.position(), "Nine borders require one or four values");
            }
            top = this.context.expressions().integer(document, values.get(0), allowThis, 0);
            bottom = this.context.expressions().integer(document, values.get(1), allowThis, 0);
            left = this.context.expressions().integer(document, values.get(2), allowThis, 0);
            right = this.context.expressions().integer(document, values.get(3), allowThis, 0);
        }
        Expression<Draw.CenterMode> mode = body.property("mode") == null
            ? this.context.expressions().constant(Draw.CenterMode.STRETCH)
            : this.context.expressions().centerMode(document, body.property("mode"), allowThis, 0);
        return new NineDefinition(top, bottom, left, right, mode);
    }

}

