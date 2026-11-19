package net.jr.client.components.navigation;

public final class SliderInteractionState {
    private double valueBeforePointerUpdate;

    public double valueBeforePointerUpdate() {
        return this.valueBeforePointerUpdate;
    }

    public void setValueBeforePointerUpdate(double value) {
        this.valueBeforePointerUpdate = value;
    }

}
