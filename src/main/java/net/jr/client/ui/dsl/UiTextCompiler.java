package net.jr.client.ui.dsl;

import net.jr.api.client.render.Draw;
import net.jr.api.client.resource.Asset;
import net.jr.client.components.elements.VisualAnchor;
import net.jr.client.ui.dsl.UiCompiledDocument.Expression;
import net.jr.client.ui.dsl.UiCompiledDocument.TextDefinition;
import net.jr.client.ui.dsl.UiDocument.Node;
import net.jr.client.ui.dsl.UiDocument.Property;
import net.jr.client.ui.dsl.UiDocument.Value;

import java.util.List;

final class UiTextCompiler implements UiElementCompiler {
    private final UiElementContext context;

    UiTextCompiler(UiElementContext context) {
        this.context = context;
    }

    @Override
    public String name() {
        return "text";
    }

    @Override
    public TextDefinition compile(UiDocument document, Node node, boolean allowThis)
        throws UiCompileException {
        return this.compileContent(
            document,
            node,
            allowThis,
            this.context.id(document, node),
            true
        );
    }

    TextDefinition compileContent(
        UiDocument document,
        Node node,
        boolean allowThis,
        String id,
        boolean positioned
    ) throws UiCompileException {
        UiNodeBody body = this.context.body(
            document,
            node,
            positioned ? List.of(
                "literal", "translatable", "anchor", "x", "y", "scale", "color", "shadow",
                "shadow_color", "outline", "align", "font", "bold", "italic", "underlined",
                "strikethrough", "obfuscated", "legacy_shadow"
            ) : List.of(
                "literal", "translatable", "scale", "color", "shadow", "shadow_color",
                "outline", "align", "font", "bold", "italic", "underlined",
                "strikethrough", "obfuscated", "legacy_shadow"
            ),
            List.of()
        );
        Value literalValue = body.property("literal");
        Value translatableValue = body.property("translatable");
        if ((literalValue == null) == (translatableValue == null)) {
            throw this.context.error(document, node.position(),
                "Text requires exactly one of 'literal' or 'translatable'");
        }

        Expression<String> literal = literalValue == null
            ? null
            : this.context.expressions().textString(document, literalValue, allowThis, 0);
        Expression<String> translatable = translatableValue == null
            ? null
            : this.context.expressions().textString(document, translatableValue, allowThis, 0);
        Expression<VisualAnchor> anchor = !positioned || body.property("anchor") == null
            ? this.context.expressions().constant(VisualAnchor.NONE)
            : this.context.expressions().visualAnchor(document, body.property("anchor"), allowThis, 0);
        Expression<Float> x = positioned
            ? this.context.expressions().number(document, body.require("x"), allowThis, 0)
            : this.context.expressions().constant(0.0F);
        Expression<Float> y = positioned
            ? this.context.expressions().number(document, body.require("y"), allowThis, 0)
            : this.context.expressions().constant(0.0F);
        Expression<Float> scale = body.property("scale") == null
            ? this.context.expressions().constant(1.0F)
            : this.context.expressions().number(document, body.property("scale"), allowThis, 0);
        Expression<Integer> color = body.property("color") == null
            ? this.context.expressions().constant(0xFFFFFFFF)
            : this.context.expressions().color(document, body.property("color"), allowThis, 0);
        Expression<Boolean> bold = body.property("bold") == null
            ? this.context.expressions().constant(false)
            : this.context.expressions().bool(document, body.property("bold"), allowThis, 0);
        Expression<Boolean> italic = body.property("italic") == null
            ? this.context.expressions().constant(false)
            : this.context.expressions().bool(document, body.property("italic"), allowThis, 0);
        Expression<Boolean> underlined = body.property("underlined") == null
            ? this.context.expressions().constant(false)
            : this.context.expressions().bool(document, body.property("underlined"), allowThis, 0);
        Expression<Boolean> strikethrough = body.property("strikethrough") == null
            ? this.context.expressions().constant(false)
            : this.context.expressions().bool(document, body.property("strikethrough"), allowThis, 0);
        Expression<Boolean> obfuscated = body.property("obfuscated") == null
            ? this.context.expressions().constant(false)
            : this.context.expressions().bool(document, body.property("obfuscated"), allowThis, 0);
        Expression<Boolean> shadow = body.property("shadow") == null
            ? this.context.expressions().constant(false)
            : this.context.expressions().bool(document, body.property("shadow"), allowThis, 0);
        Expression<Integer> shadowColor = body.property("shadow_color") == null
            ? null
            : this.context.expressions().color(document, body.property("shadow_color"), allowThis, 0);
        Expression<Boolean> legacyShadow =
                body.property("legacy_shadow") == null
                        ? this.context.expressions().constant(false)
                        : this.context.expressions().bool(
                        document,
                        body.property("legacy_shadow"),
                        allowThis,
                        0
                );
        Expression<Integer> outline = body.property("outline") == null
            ? null
            : this.context.expressions().color(document, body.property("outline"), allowThis, 0);
        Expression<Draw.TextAlign> align = body.property("align") == null
            ? this.context.expressions().constant(Draw.TextAlign.LEFT)
            : this.context.expressions().textAlign(document, body.property("align"), allowThis, 0);
        Expression<Asset> font = body.property("font") == null
            ? null
            : this.context.expressions().asset(document, body.property("font"), allowThis, 0);

        return new TextDefinition(
            id, literal, translatable, anchor, x, y, scale, color, bold, italic,
            underlined, strikethrough, obfuscated, shadow, shadowColor, outline, align, font, legacyShadow
        );
    }

}

