package net.jr.client.components.navigation;

public final class SliderInteractionState {
    private double valueBeforePointerUpdate;
    private UiActionFeedback lastFeedback = UiActionFeedback.NONE;

    public double valueBeforePointerUpdate() {
        return this.valueBeforePointerUpdate;
    }

    public void setValueBeforePointerUpdate(double value) {
        this.valueBeforePointerUpdate = value;
    }

    public UiActionFeedback lastFeedback() {
        return this.lastFeedback;
    }

    public void setLastFeedback(UiActionFeedback feedback) {
        this.lastFeedback = feedback;
    }
}
