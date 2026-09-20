package net.jr.client.compat.xaero;

import xaero.map.graphics.ImprovedFramebuffer;
import xaero.map.element.HoveredMapElementHolder;

public final class XaeroWorldMapViewState {
    private double destScale = 3.0D;

    private ImprovedFramebuffer primaryScaleFbo;
    private ImprovedFramebuffer immediateRenderFbo;

    private boolean attachedCamera;
    private boolean hiddenUi;

    private int lastAmountOfRegionsViewed = 1;

    private boolean skipWorldRender;

    private final XaeroWorldMapMinimapState minimapState =
            new XaeroWorldMapMinimapState();

    private HoveredMapElementHolder<?, ?> hoveredMapElement;

    public XaeroWorldMapMinimapState minimapState() {
        return minimapState;
    }

    public HoveredMapElementHolder<?, ?> hoveredMapElement() {
        return hoveredMapElement;
    }

    public void hoveredMapElement(HoveredMapElementHolder<?, ?> value) {
        hoveredMapElement = value;
    }

    public boolean skipWorldRender() {
        return skipWorldRender;
    }

    public void skipWorldRender(boolean value) {
        skipWorldRender = value;
    }

    public double destScale() {
        return destScale;
    }

    public void destScale(double value) {
        destScale = value;
    }

    public ImprovedFramebuffer primaryScaleFbo() {
        return primaryScaleFbo;
    }

    public void primaryScaleFbo(ImprovedFramebuffer value) {
        primaryScaleFbo = value;
    }

    public ImprovedFramebuffer immediateRenderFbo() {
        return immediateRenderFbo;
    }

    public void immediateRenderFbo(ImprovedFramebuffer value) {
        immediateRenderFbo = value;
    }

    public boolean attachedCamera() {
        return attachedCamera;
    }

    public void attachedCamera(boolean value) {
        attachedCamera = value;
    }

    public boolean hiddenUi() {
        return hiddenUi;
    }

    public void hiddenUi(boolean value) {
        hiddenUi = value;
    }

    public int lastAmountOfRegionsViewed() {
        return lastAmountOfRegionsViewed;
    }

    public void lastAmountOfRegionsViewed(int value) {
        lastAmountOfRegionsViewed = value;
    }
}
