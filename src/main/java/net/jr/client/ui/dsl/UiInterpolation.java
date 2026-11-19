package net.jr.client.ui.dsl;

import net.jr.api.client.render.Draw;
import net.jr.api.client.resource.Asset;

final class UiInterpolation {
    private UiInterpolation() {
    }

    static float pingPong(
        float target,
        float duration,
        Draw.AnimUnit unit,
        long nowMillis,
        Asset source,
        UiDocument.Position position
    ) {
        if (!Float.isFinite(target)) {
            throw error(source, position, "interpolate() value must be finite");
        }
        if (!Float.isFinite(duration) || duration <= 0.0F) {
            throw error(source, position, "interpolate() time must be greater than zero");
        }

        double legMillis = duration * unit.toMillis(1);
        if (!Double.isFinite(legMillis) || legMillis <= 0.0D) {
            throw error(source, position, "interpolate() duration is outside the supported range");
        }

        double cycleMillis = legMillis * 2.0D;
        double cursor = Math.floorMod(nowMillis, Math.max(1L, (long)Math.ceil(cycleMillis)));
        double progress = cursor <= legMillis
            ? cursor / legMillis
            : (cycleMillis - cursor) / legMillis;
        return target * (float)Math.max(0.0D, Math.min(1.0D, progress));
    }

    private static IllegalArgumentException error(
        Asset source,
        UiDocument.Position position,
        String message
    ) {
        return new IllegalArgumentException(
            source + ":" + position.line() + ":" + position.column() + ": " + message
        );
    }
}
