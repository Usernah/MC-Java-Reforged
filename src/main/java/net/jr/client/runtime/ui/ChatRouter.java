package net.jr.client.runtime.ui;

import javax.annotation.Nullable;

import net.jr.api.client.ui.Viewport;
import net.jr.client.runtime.bridge.ChatComponentRuntimeBridge;
import net.jr.client.runtime.context.SlotExecution;
import net.jr.client.runtime.slot.Slots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.ChatListener;

public final class ChatRouter {
    private static final int MINIMUM_CHAT_WIDTH = 40;
    private static final int HORIZONTAL_CONTENT_MARGIN = 8;

    private ChatRouter() {
    }

    public static ChatComponent component(ChatComponent primaryComponent) {
        Slots slot = activeSlot();
        ChatComponent primaryBinding = slot.id() == 0 ? primaryComponent : null;
        return slot.chatState().component(Minecraft.getInstance(), primaryBinding);
    }

    public static ChatListener listener(ChatListener primaryListener) {
        Slots slot = activeSlot();
        ChatListener primaryBinding = slot.id() == 0 ? primaryListener : null;
        return slot.chatState().listener(Minecraft.getInstance(), primaryBinding);
    }

    public static int ticks(int vanillaTicks) {
        return activeSlot().chatState().ticks(vanillaTicks);
    }

    public static void tick(ChatComponent component, int vanillaTicks) {
        activeSlot().chatState().tick(vanillaTicks);
        component.tick();
    }

    public static int backgroundColor(int vanillaColor) {
        Minecraft minecraft = Minecraft.getInstance();
        ChatComponent activeComponent = component(minecraft.gui.hud.getChat());
        int rgb = ((ChatComponentRuntimeBridge)activeComponent).javaReforged$getBackgroundColor();
        return vanillaColor & 0xFF000000 | rgb & 0xFFFFFF;
    }

    @Nullable
    public static Integer viewportChatWidthOrNull() {
        var viewport = Viewport.getOrNull(activeSlot().id());
        if (viewport == null) {
            return null;
        }

        return Math.max(
                MINIMUM_CHAT_WIDTH,
                viewport.logicalGuiWidth() - HORIZONTAL_CONTENT_MARGIN
        );
    }

    private static Slots activeSlot() {
        return SlotExecution.currentSlot();
    }
}
