package net.jr.client.ui.dsl;

import net.jr.api.client.render.Draw;
import net.jr.client.ui.dsl.UiCompiledDocument.AnimationDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.Expression;
import net.jr.client.ui.dsl.UiCompiledDocument.Vector2Definition;
import net.jr.client.ui.dsl.UiDocument.Node;
import net.jr.client.ui.dsl.UiDocument.Property;
import net.jr.client.ui.dsl.UiDocument.Value;

import java.util.ArrayList;
import java.util.List;

final class UiAnimationCompiler {
    private final UiElementContext context;

    UiAnimationCompiler(UiElementContext context) {
        this.context = context;
    }

    AnimationDefinition compile(UiDocument document, Node node, boolean allowThis)
        throws UiCompileException {
        if (node == null) {
            return null;
        }
        UiNodeBody body = this.context.body(
            document,
            node,
            List.of("frames", "frame_size", "duration", "unit", "mode", "interpolation", "frame_durations"),
            List.of()
        );
        Expression<Integer> frames = this.context.expressions().integer(document, body.require("frames"), allowThis, 0);
        Vector2Definition frameSize = this.context.expressions().vector(document, body.require("frame_size"), allowThis);
        Expression<Integer> duration = this.context.expressions().integer(document, body.require("duration"), allowThis, 0);
        Expression<Draw.AnimUnit> unit = body.property("unit") == null
            ? this.context.expressions().constant(Draw.AnimUnit.MILLISECONDS)
            : this.context.expressions().animationUnit(document, body.property("unit"), allowThis, 0);
        Expression<Draw.AnimationMode> mode = body.property("mode") == null
            ? this.context.expressions().constant(Draw.AnimationMode.LOOP)
            : this.context.expressions().animationMode(document, body.property("mode"), allowThis, 0);
        Expression<Boolean> interpolation = body.property("interpolation") == null
            ? this.context.expressions().constant(false)
            : this.context.expressions().bool(document, body.property("interpolation"), allowThis, 0);

        List<Expression<Integer>> frameDurations = new ArrayList<>();
        Value rawDurations = body.property("frame_durations");
        if (rawDurations != null) {
            List<Value> values = this.context.expressions().tupleValues(rawDurations);
            if (values == null) {
                throw this.context.error(document, rawDurations.position(),
                    "frame_durations must be a list or tuple");
            }
            for (Value value : values) {
                frameDurations.add(this.context.expressions().integer(document, value, allowThis, 0));
            }
        }
        return new AnimationDefinition(frames, frameSize, duration, unit, mode, interpolation, frameDurations);
    }

}

