package net.jr.client.ui.dsl;

import net.jr.client.components.elements.VisualAnchor;
import net.jr.client.ui.dsl.UiCompiledDocument.Expression;
import net.jr.client.ui.dsl.UiCompiledDocument.TextDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.TextBoxDefinition;
import net.jr.client.ui.dsl.UiDocument.Node;
import net.jr.client.ui.dsl.UiDocument.Property;

import java.util.List;

final class UiTextBoxCompiler implements UiElementCompiler {
    private final UiElementContext context;
    private final UiTextCompiler text;

    UiTextBoxCompiler(UiElementContext context, UiTextCompiler text) {
        this.context = context;
        this.text = text;
    }

    @Override
    public String name() {
        return "textbox";
    }

    @Override
    public TextBoxDefinition compile(
        UiDocument document,
        Node node,
        boolean allowThis
    ) throws UiCompileException {
        String id = this.context.id(document, node);
        UiNodeBody body = this.context.body(
            document,
            node,
            List.of("anchor", "x", "y", "w", "h", "max_lines", "line_spacing"),
            List.of("text")
        );
        Node textNode = body.node("text");
        if (textNode == null) {
            throw this.context.error(document, node.position(),
                "TextBox requires exactly one 'text' block");
        }

        Expression<VisualAnchor> anchor = body.property("anchor") == null
            ? this.context.expressions().constant(VisualAnchor.NONE)
            : this.context.expressions().visualAnchor(document, body.property("anchor"), allowThis, 0);
        Expression<Float> x = this.context.expressions().number(document, body.require("x"), allowThis, 0);
        Expression<Float> y = this.context.expressions().number(document, body.require("y"), allowThis, 0);
        Expression<Float> width = this.context.expressions().number(document, body.require("w"), allowThis, 0);
        Expression<Float> height = this.context.expressions().number(document, body.require("h"), allowThis, 0);
        Expression<Integer> maxLines = body.property("max_lines") == null
            ? this.context.expressions().constant(0)
            : this.context.expressions().integer(document, body.property("max_lines"), allowThis, 0);
        Expression<Float> lineSpacing = body.property("line_spacing") == null
            ? this.context.expressions().constant(0.0F)
            : this.context.expressions().number(document, body.property("line_spacing"), allowThis, 0);
        TextDefinition text = this.text.compileContent(
            document,
            textNode,
            allowThis,
            id + ".$text",
            false
        );

        return new TextBoxDefinition(
            id,
            anchor,
            x,
            y,
            width,
            height,
            maxLines,
            lineSpacing,
            text
        );
    }

}

