package net.jr.client.ui.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

final class DirectionalFocusResolverTest {
    @Test
    void regularGridMovesAlongTheCurrentRow() {
        Target origin = target("origin", 0, 0);
        Target right = target("right", 18, 0);
        Target diagonal = target("diagonal", 18, 18);

        assertEquals(right, next(List.of(origin, right, diagonal), origin, GLFW.GLFW_KEY_RIGHT));
    }

    @Test
    void alignmentRemainsASoftPreference() {
        Target origin = target("origin", 0, 0);
        Target aligned = target("aligned", 36, 0);
        Target stronglyDiagonal = target("strongly-diagonal", 20, 40);

        assertEquals(
            aligned,
            next(List.of(origin, aligned, stronglyDiagonal), origin, GLFW.GLFW_KEY_RIGHT)
        );
    }

    @Test
    void usesActualRectangleDimensionsForCrossAxisOverlap() {
        Target origin = new Target("origin", new ScreenRectangle(0, 0, 32, 32));
        Target overlapping = new Target("overlapping", new ScreenRectangle(40, 28, 16, 16));
        Target separated = new Target("separated", new ScreenRectangle(38, 40, 16, 16));

        assertEquals(
            overlapping,
            next(List.of(origin, overlapping, separated), origin, GLFW.GLFW_KEY_RIGHT)
        );
    }

    @Test
    void canReachAnIrregularTargetOutsideTheOldCone() {
        Target origin = target("origin", 0, 0);
        Target irregular = target("irregular", 20, 80);

        assertEquals(
            irregular,
            next(List.of(origin, irregular), origin, GLFW.GLFW_KEY_RIGHT)
        );
    }

    @Test
    void neverSelectsATargetBehindTheRequestedDirection() {
        Target origin = target("origin", 20, 0);
        Target behind = target("behind", 0, 0);

        assertNull(next(List.of(origin, behind), origin, GLFW.GLFW_KEY_RIGHT));
    }

    private static Target next(List<Target> targets, Target origin, int keyCode) {
        return DirectionalFocusResolver.nextTarget(
            targets,
            origin,
            Target::rectangle,
            keyCode
        );
    }

    private static Target target(String name, int x, int y) {
        return new Target(name, new ScreenRectangle(x, y, 16, 16));
    }

    private record Target(String name, ScreenRectangle rectangle) {
    }
}
