package net.jr.client.ui.dsl;

import net.jr.api.client.resource.Asset;
import net.jr.client.components.elements.VisualAnchor;
import net.jr.client.ui.dsl.UiCompiledDocument.AnimationDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.Expression;
import net.jr.client.ui.dsl.UiCompiledDocument.ImageDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.NineDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.Vector2Definition;
import net.jr.client.ui.dsl.UiDocument.BooleanValue;
import net.jr.client.ui.dsl.UiDocument.ConditionalValue;
import net.jr.client.ui.dsl.UiDocument.ExposedDataValue;
import net.jr.client.ui.dsl.UiDocument.ElementValue;
import net.jr.client.ui.dsl.UiDocument.ListValue;
import net.jr.client.ui.dsl.UiDocument.NullValue;
import net.jr.client.ui.dsl.UiDocument.Node;
import net.jr.client.ui.dsl.UiDocument.Property;
import net.jr.client.ui.dsl.UiDocument.Value;
import net.jr.client.ui.dsl.UiReferenceResolver.Resolved;

import java.util.ArrayList;
import java.util.List;

final class UiImageCompiler implements UiElementCompiler {
    private final UiElementContext context;
    private final UiNineCompiler nine;
    private final UiAnimationCompiler animation;

    UiImageCompiler(UiElementContext context) {
        this.context = context;
        this.nine = new UiNineCompiler(context);
        this.animation = new UiAnimationCompiler(context);
    }

    @Override
    public String name() {
        return "image";
    }

    @Override
    public ImageDefinition compile(UiDocument document, Node node, boolean allowThis)
        throws UiCompileException {
        String id = this.context.id(document, node);
        UiNodeBody body = this.context.body(
            document,
            node,
            List.of("texture", "anchor", "x", "y", "w", "h", "uv", "alpha", "uv_size", "atlas_size", "meta", "config"),
            List.of("nine", "animation")
        );
        Expression<Asset> texture = this.context.expressions().asset(document, body.require("texture"), allowThis, 0);
        Expression<VisualAnchor> anchor = body.property("anchor") == null
            ? this.context.expressions().constant(VisualAnchor.NONE)
            : this.context.expressions().visualAnchor(document, body.property("anchor"), allowThis, 0);
        Expression<Float> x = this.context.expressions().number(document, body.require("x"), allowThis, 0);
        Expression<Float> y = this.context.expressions().number(document, body.require("y"), allowThis, 0);
        Expression<Float> width = this.context.expressions().number(document, body.require("w"), allowThis, 0);
        Expression<Float> height = this.context.expressions().number(document, body.require("h"), allowThis, 0);
        Expression<Float> alpha = body.property("alpha") == null ? this.context.expressions().constant(1.0f) : this.context.expressions().number(document, body.property("alpha"), allowThis, 0);

        Vector2Definition uv = this.context.expressions().optionalVector(document, body.property("uv"), allowThis);
        Vector2Definition uvSize = this.context.expressions().optionalVector(document, body.property("uv_size"), allowThis);
        Vector2Definition atlasSize = this.context.expressions().optionalVector(document, body.property("atlas_size"), allowThis);
        Expression<Object> meta = body.property("meta") == null
            ? this.context.expressions().constant(body.property("config") != null)
            : this.imageMeta(document, body.property("meta"), allowThis, 0);
        List<Expression<String>> configs = body.property("config") == null
            ? List.of()
            : this.metaConfigs(document, body.property("config"), allowThis, 0);
        Node nineNode = body.node("nine");
        NineDefinition nine = this.context.optional(
            document, nineNode, () -> this.nine.compile(document, nineNode, allowThis)
        );
        Node animationNode = body.node("animation");
        AnimationDefinition animation = this.context.optional(
            document,
            animationNode,
            () -> this.animation.compile(document, animationNode, allowThis)
        );

        if ((nine != null || animation != null) && body.property("meta") == null
            && body.property("config") == null && (uv == null || uvSize == null || atlasSize == null)) {
            throw this.context.error(document, node.position(),
                "An image with Nine or Animation and no Meta requires uv, uv_size and atlas_size");
        }

        return new ImageDefinition(
            id, texture, anchor, x, y, width, height, alpha, uv, uvSize, atlasSize,
            meta, configs, nine, animation
        );
    }

    private Expression<Object> imageMeta(
        UiDocument document,
        Value value,
        boolean allowThis,
        int depth
    ) throws UiCompileException {
        this.context.references().checkDepth(document, value, depth);
        if (value instanceof BooleanValue bool) {
            return this.context.expressions().constant(bool.value());
        }
        if (value instanceof ExposedDataValue data) {
            this.context.validateData(document, data, allowThis);
            return context -> this.context.expressions().requireImageMetaValue(
                context.resolve(data.path()), document.source(), data.position()
            );
        }
        if (value instanceof ElementValue element) {
            Expression<Object> expression = this.context.element(document, element);
            return context -> this.context.expressions().requireImageMetaValue(
                expression.resolve(context), document.source(), element.position()
            );
        }
        if (value instanceof ConditionalValue conditional) {
            Expression<Boolean> condition = this.context.expressions().bool(
                document, conditional.condition(), allowThis, depth + 1
            );
            Expression<Object> whenTrue = this.imageMeta(
                document, conditional.whenTrue(), allowThis, depth + 1
            );
            Expression<Object> whenFalse = this.imageMeta(
                document, conditional.whenFalse(), allowThis, depth + 1
            );
            return context -> condition.resolve(context)
                ? whenTrue.resolve(context)
                : whenFalse.resolve(context);
        }
        Resolved resolved = this.context.references().resolve(document, value, depth + 1);
        if (resolved != null) {
            if (resolved.asset() != null) {
                return this.context.expressions().constant(resolved.asset());
            }
            return this.imageMeta(resolved.document(), resolved.value(), allowThis, depth + 1);
        }
        throw this.context.error(document, value.position(),
            "Expected true, false or an imported metadata Asset reference");
    }

    private List<Expression<String>> metaConfigs(
        UiDocument document,
        Value value,
        boolean allowThis,
        int depth
    ) throws UiCompileException {
        this.context.references().checkDepth(document, value, depth);
        if (value instanceof ListValue list) {
            List<Expression<String>> configs = new ArrayList<>();
            for (Value item : list.values()) {
                configs.add(this.nullableString(document, item, allowThis, depth + 1));
            }
            return List.copyOf(configs);
        }
        Resolved resolved = this.context.references().resolve(document, value, depth + 1);
        if (resolved != null) {
            return this.metaConfigs(
                resolved.document(), resolved.value(), allowThis, depth + 1
            );
        }
        return List.of(this.nullableString(document, value, allowThis, depth + 1));
    }

    private Expression<String> nullableString(
        UiDocument document,
        Value value,
        boolean allowThis,
        int depth
    ) throws UiCompileException {
        this.context.references().checkDepth(document, value, depth);
        if (value instanceof NullValue) {
            return this.context.expressions().constant(null);
        }
        if (value instanceof ConditionalValue conditional) {
            Expression<Boolean> condition = this.context.expressions().bool(
                document, conditional.condition(), allowThis, depth + 1
            );
            Expression<String> whenTrue = this.nullableString(
                document, conditional.whenTrue(), allowThis, depth + 1
            );
            Expression<String> whenFalse = this.nullableString(
                document, conditional.whenFalse(), allowThis, depth + 1
            );
            return context -> condition.resolve(context)
                ? whenTrue.resolve(context)
                : whenFalse.resolve(context);
        }
        return this.context.expressions().string(document, value, allowThis, depth + 1);
    }

}

