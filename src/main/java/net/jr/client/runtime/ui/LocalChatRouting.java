package net.jr.client.runtime.ui;

import javax.annotation.Nullable;
import net.jr.client.runtime.bridge.ChatComponentRuntimeBridge;
import net.jr.client.runtime.context.LocalClientAcces;
import net.jr.client.runtime.context.LocalClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.ChatListener;

/** Resolves chat ownership and viewport-dependent chat metrics. */
public final class LocalChatRouting {
    private static final int MINIMUM_CHAT_WIDTH = 40;
    private static final int HORIZONTAL_CONTENT_MARGIN = 8;

    private LocalChatRouting() {
    }

    public static ChatComponent component(ChatComponent primaryComponent) {
        LocalClient client = LocalClientAcces.currentOrNull();
        if (client == null) {
            return primaryComponent;
        }
        ChatComponent primaryBinding = client.slotId() == 0 ? primaryComponent : null;
        return client.chat().component(Minecraft.getInstance(), primaryBinding);
    }

    public static ChatListener listener(ChatListener primaryListener) {
        LocalClient client = LocalClientAcces.currentOrNull();
        if (client == null) {
            return primaryListener;
        }
        ChatListener primaryBinding = client.slotId() == 0 ? primaryListener : null;
        return client.chat().listener(Minecraft.getInstance(), primaryBinding);
    }

    /** Resolves every vanilla chat timestamp against the active local client. */
    public static int ticks(int vanillaTicks) {
        LocalClient client = LocalClientAcces.currentOrNull();
        return client == null ? vanillaTicks : client.chat().ticks(vanillaTicks);
    }

    /** Advances and ticks only the chat owned by the active local client. */
    public static void tick(ChatComponent component, int vanillaTicks) {
        LocalClient client = LocalClientAcces.currentOrNull();
        if (client != null) {
            client.chat().tick(vanillaTicks);
        }
        component.tick();
    }

    /** Replaces only the RGB channels while preserving vanilla's opacity. */
    public static int backgroundColor(int vanillaColor) {
        Minecraft minecraft = Minecraft.getInstance();
        ChatComponent activeComponent = component(minecraft.gui.hud.getChat());
        int rgb = ((ChatComponentRuntimeBridge)activeComponent).javaReforged$getBackgroundColor();
        return vanillaColor & 0xFF000000 | rgb & 0xFFFFFF;
    }

    @Nullable
    public static Integer viewportChatWidthOrNull() {
        LocalClient client = LocalClientAcces.currentOrNull();
        if (client == null || !client.hasViewport()) {
            return null;
        }
        return Math.max(MINIMUM_CHAT_WIDTH, client.viewport().guiWidth() - HORIZONTAL_CONTENT_MARGIN);
    }
}
