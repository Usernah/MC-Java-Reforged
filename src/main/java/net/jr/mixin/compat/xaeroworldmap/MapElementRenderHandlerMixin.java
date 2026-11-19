package net.jr.mixin.compat.xaeroworldmap;

import java.util.ArrayDeque;
import java.util.Deque;

import net.jr.client.compat.xaero.XaeroWorldMapRuntime;
import net.jr.client.compat.xaero.XaeroWorldMapViewState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.element.HoveredMapElementHolder;
import xaero.map.element.MapElementRenderHandler;
import xaero.map.element.render.ElementRenderer;

@Mixin(value = MapElementRenderHandler.class, remap = false)
abstract class MapElementRenderHandlerMixin {
    @Shadow
    private HoveredMapElementHolder<?, ?> previousHovered;

    @Shadow
    private boolean previousHoveredPresent;

    @Shadow
    private boolean renderingHovered;

    @Shadow
    private Object workingHovered;

    @Shadow
    private ElementRenderer<?, ?, ?> workingHoveredRenderer;

    @Unique
    private static final ThreadLocal<Deque<HoverScope>> javaReforged$hoverScopes =
            ThreadLocal.withInitial(ArrayDeque::new);


    @Inject(method = "render", at = @At("HEAD"))
    private void javaReforged$enterHoverState(
            CallbackInfoReturnable<HoveredMapElementHolder<?, ?>> cir
    ) {
        XaeroWorldMapViewState state =
                XaeroWorldMapRuntime.currentViewStateOrNull();

        if (state == null) {
            javaReforged$hoverScopes.get().push(HoverScope.INACTIVE);
            return;
        }

        javaReforged$hoverScopes.get().push(
                new HoverScope(
                        state,
                        previousHovered,
                        previousHoveredPresent,
                        renderingHovered,
                        workingHovered,
                        workingHoveredRenderer
                )
        );

        previousHovered = state.hoveredMapElement();
        previousHoveredPresent = false;
        renderingHovered = false;
        workingHovered = null;
        workingHoveredRenderer = null;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void javaReforged$exitHoverState(
            CallbackInfoReturnable<HoveredMapElementHolder<?, ?>> cir
    ) {
        Deque<HoverScope> scopes = javaReforged$hoverScopes.get();
        HoverScope scope = scopes.pop();

        if (scope.state() != null) {
            scope.state().hoveredMapElement(previousHovered);
            previousHovered = scope.previousHovered();
            previousHoveredPresent = scope.previousHoveredPresent();
            renderingHovered = scope.renderingHovered();
            workingHovered = scope.workingHovered();
            workingHoveredRenderer = scope.workingHoveredRenderer();
        }

        if (scopes.isEmpty()) {
            javaReforged$hoverScopes.remove();
        }
    }

    @Unique
    private record HoverScope(
            XaeroWorldMapViewState state,
            HoveredMapElementHolder<?, ?> previousHovered,
            boolean previousHoveredPresent,
            boolean renderingHovered,
            Object workingHovered,
            ElementRenderer<?, ?, ?> workingHoveredRenderer
    ) {
        private static final HoverScope INACTIVE =
                new HoverScope(null, null, false, false, null, null);
    }
}
