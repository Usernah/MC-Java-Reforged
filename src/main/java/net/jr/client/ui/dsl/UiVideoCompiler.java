package net.jr.client.ui.dsl;

import net.jr.api.client.resource.Asset;
import net.jr.client.components.elements.VisualAnchor;
import net.jr.client.ui.dsl.UiCompiledDocument.Expression;
import net.jr.client.ui.dsl.UiCompiledDocument.VideoDefinition;
import net.jr.client.ui.dsl.UiDocument.Node;
import net.jr.client.ui.dsl.UiDocument.Property;

import java.util.List;

final class UiVideoCompiler implements UiElementCompiler {
    private final UiElementContext context;

    UiVideoCompiler(UiElementContext context) {
        this.context = context;
    }

    @Override
    public String name() {
        return "video";
    }

    @Override
    public VideoDefinition compile(UiDocument document, Node node, boolean allowThis)
        throws UiCompileException {
        String id = this.context.id(document, node);
        UiNodeBody body = this.context.body(
            document,
            node,
            List.of("source", "anchor", "x", "y", "w", "h", "fullscreen", "loop", "loop_fade"),
            List.of()
        );
        Expression<Asset> source = this.context.expressions().asset(document, body.require("source"), allowThis, 0);
        Expression<VisualAnchor> anchor = body.property("anchor") == null
            ? this.context.expressions().constant(VisualAnchor.NONE)
            : this.context.expressions().visualAnchor(document, body.property("anchor"), allowThis, 0);
        Expression<Float> x = body.property("x") == null
            ? this.context.expressions().constant(0.0F)
            : this.context.expressions().number(document, body.property("x"), allowThis, 0);
        Expression<Float> y = body.property("y") == null
            ? this.context.expressions().constant(0.0F)
            : this.context.expressions().number(document, body.property("y"), allowThis, 0);
        Expression<Float> width = body.property("w") == null
            ? this.context.expressions().constant(0.0F)
            : this.context.expressions().number(document, body.property("w"), allowThis, 0);
        Expression<Float> height = body.property("h") == null
            ? this.context.expressions().constant(0.0F)
            : this.context.expressions().number(document, body.property("h"), allowThis, 0);
        Expression<Boolean> fullscreen = body.property("fullscreen") == null
            ? this.context.expressions().constant(true)
            : this.context.expressions().bool(document, body.property("fullscreen"), allowThis, 0);
        Expression<Boolean> loop = body.property("loop") == null
            ? this.context.expressions().constant(true)
            : this.context.expressions().bool(document, body.property("loop"), allowThis, 0);
        Expression<Integer> loopFadeMillis = body.property("loop_fade") == null
            ? this.context.expressions().constant(0)
            : this.context.expressions().integer(document, body.property("loop_fade"), allowThis, 0);
        return new VideoDefinition(
            id, source, anchor, x, y, width, height, fullscreen, loop, loopFadeMillis
        );
    }

}

