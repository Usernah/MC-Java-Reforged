package net.jr.client.ui.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.jr.api.client.render.Draw;
import net.jr.api.client.resource.Asset;
import org.junit.jupiter.api.Test;

final class UiInterpolationTest {
    private static final Asset SOURCE = Asset.NamespaceAndPatch(
        "test", "ui/screens/interpolation.screen"
    );
    private static final UiDocument.Position POSITION = new UiDocument.Position(3, 8);

    @Test
    void travelsFromZeroToTargetAndBack() {
        assertEquals(0.0F, value(0L));
        assertEquals(10.0F, value(50L));
        assertEquals(20.0F, value(100L));
        assertEquals(10.0F, value(150L));
        assertEquals(0.0F, value(200L));
        assertEquals(10.0F, value(250L));
    }

    @Test
    void supportsAnimationUnitsAndNegativeTargets() {
        assertEquals(
            -5.0F,
            UiInterpolation.pingPong(
                -10.0F, 2.0F, Draw.AnimUnit.TICKS, 50L, SOURCE, POSITION
            )
        );
    }

    @Test
    void rejectsNonPositiveTime() {
        assertThrows(
            IllegalArgumentException.class,
            () -> UiInterpolation.pingPong(
                20.0F, 0.0F, Draw.AnimUnit.MILLISECONDS, 0L, SOURCE, POSITION
            )
        );
    }

    private static float value(long nowMillis) {
        return UiInterpolation.pingPong(
            20.0F,
            100.0F,
            Draw.AnimUnit.MILLISECONDS,
            nowMillis,
            SOURCE,
            POSITION
        );
    }
}
