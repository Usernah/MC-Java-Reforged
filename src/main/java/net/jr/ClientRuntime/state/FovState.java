package net.jr.ClientRuntime.state;

public final class FovState {
    private float modifier = 1.0F;
    private float oldModifier = 1.0F;

    public void tick(float targetModifier) {
        this.oldModifier = this.modifier;
        this.modifier += (targetModifier - this.modifier) * 0.5F;
        if (this.modifier > 1.5F) {
            this.modifier = 1.5F;
        }
        if (this.modifier < 0.1F) {
            this.modifier = 0.1F;
        }
    }

    public float modifier() {
        return this.modifier;
    }

    public float oldModifier() {
        return this.oldModifier;
    }

    public void clear() {
        this.modifier = 1.0F;
        this.oldModifier = 1.0F;
    }
}
