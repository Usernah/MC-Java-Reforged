package net.jr.client.runtime.state;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.ChatListener;

public final class ChatState {
    @Nullable
    private ChatComponent component;
    @Nullable
    private ChatListener listener;
    private int ticks;
    private boolean clockInitialized;

    public ChatComponent component(Minecraft minecraft, @Nullable ChatComponent primaryComponent) {
        if (this.component == null) {
            this.component = primaryComponent != null ? primaryComponent : new ChatComponent(minecraft);
        }
        return this.component;
    }

    public ChatListener listener(Minecraft minecraft, @Nullable ChatListener primaryListener) {
        if (this.listener == null) {
            if (primaryListener != null) {
                this.listener = primaryListener;
            } else {
                this.listener = new ChatListener(minecraft);
                this.listener.setMessageDelay(minecraft.options.chatDelay().get());
            }
        }
        return this.listener;
    }

    public int ticks(int vanillaTicks) {
        this.initializeClock(vanillaTicks);
        return this.ticks;
    }

    public void tick(int vanillaTicks) {
        if (this.clockInitialized) {
            this.ticks++;
        } else {
            this.initializeClock(vanillaTicks);
        }
    }

    private void initializeClock(int vanillaTicks) {
        if (!this.clockInitialized) {
            this.ticks = vanillaTicks;
            this.clockInitialized = true;
        }
    }

    public void clear() {
        this.component = null;
        this.listener = null;
        this.ticks = 0;
        this.clockInitialized = false;
    }
}
