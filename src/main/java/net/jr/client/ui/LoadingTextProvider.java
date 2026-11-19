package net.jr.client.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

public final class LoadingTextProvider {
    private static final int DEFAULT_CHANGE_INTERVAL_TICKS = 120;
    private static final Component[] TEXTS = {
        Component.translatable("loading_tip.java_reforged.1"),
        Component.translatable("loading_tip.java_reforged.5"),
        Component.translatable("loading_tip.java_reforged.21"),
        Component.translatable("loading_tip.java_reforged.51"),
        Component.translatable("loading_tip.java_reforged.52"),
        Component.translatable("loading_tip.java_reforged.53"),
        Component.translatable("loading_tip.java_reforged.54"),
        Component.translatable("loading_tip.java_reforged.55"),
        Component.translatable("loading_tip.java_reforged.57"),
        Component.translatable("loading_tip.java_reforged.58"),
        Component.translatable("loading_tip.java_reforged.59"),
        Component.translatable("loading_tip.java_reforged.61"),
        Component.translatable("loading_tip.java_reforged.62"),
        Component.translatable("loading_tip.java_reforged.63"),
        Component.translatable("loading_tip.java_reforged.65")
    };

    private final RandomSource random = RandomSource.create();
    private final int[] order = new int[TEXTS.length];
    private final int changeIntervalTicks;
    private int nextPosition;
    private int lastTextIndex = -1;
    private int elapsedTicks;
    private Component current;

    public LoadingTextProvider() {
        this(DEFAULT_CHANGE_INTERVAL_TICKS);
    }

    public LoadingTextProvider(int changeIntervalTicks) {
        this(changeIntervalTicks, null, 0);
    }

    public LoadingTextProvider(
        int changeIntervalTicks,
        Component current,
        int elapsedTicks
    ) {
        if (changeIntervalTicks <= 0) {
            throw new IllegalArgumentException("changeIntervalTicks must be positive");
        }

        this.changeIntervalTicks = changeIntervalTicks;
        for (int i = 0; i < this.order.length; i++) {
            this.order[i] = i;
        }
        this.shuffle();
        if (current == null) {
            this.advance();
        } else {
            this.current = current;
            this.elapsedTicks = Math.max(0, Math.min(elapsedTicks, changeIntervalTicks - 1));
        }
    }

    public Component current() {
        return Component.literal(this.current.getString());
    }

    public int elapsedTicks() {
        return this.elapsedTicks;
    }

    public boolean tick() {
        if (++this.elapsedTicks < this.changeIntervalTicks) {
            return false;
        }

        this.elapsedTicks = 0;
        this.advance();
        return true;
    }

    private void advance() {
        if (this.nextPosition >= this.order.length) {
            this.shuffle();
        }

        int textIndex = this.order[this.nextPosition++];
        this.lastTextIndex = textIndex;
        this.current = TEXTS[textIndex];
    }

    private void shuffle() {
        for (int i = this.order.length - 1; i > 0; i--) {
            int swapIndex = this.random.nextInt(i + 1);
            int value = this.order[i];
            this.order[i] = this.order[swapIndex];
            this.order[swapIndex] = value;
        }

        if (this.order.length > 1 && this.order[0] == this.lastTextIndex) {
            int value = this.order[0];
            this.order[0] = this.order[1];
            this.order[1] = value;
        }

        this.nextPosition = 0;
    }
}
