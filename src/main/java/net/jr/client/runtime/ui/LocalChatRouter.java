package net.jr.client.runtime.ui;

import javax.annotation.Nullable;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.bridge.ChatComponentRuntimeBridge;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.ChatListener;

public final class LocalChatRouter {
    private static final int MINIMUM_CHAT_WIDTH = 40;
    private static final int HORIZONTAL_CONTENT_MARGIN = 8;

    private LocalChatRouter() {
    }

    public static ChatComponent component(ChatComponent primaryComponent) {
        LocalClientSlot slot = activeSlot();
        ChatComponent primaryBinding = slot.id() == 0 ? primaryComponent : null;
        return slot.chatState().component(Minecraft.getInstance(), primaryBinding);
    }

    public static ChatListener listener(ChatListener primaryListener) {
        LocalClientSlot slot = activeSlot();
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
        int slotId = activeSlotId();
        if (!ClientRuntime.INSTANCE.viewports().hasViewport(slotId)) {
            return null;
        }
        return Math.max(
            MINIMUM_CHAT_WIDTH,
            ClientRuntime.INSTANCE.viewports().viewport(slotId).guiWidth() - HORIZONTAL_CONTENT_MARGIN
        );
    }

    private static LocalClientSlot activeSlot() {
        return ClientRuntime.INSTANCE.slots().slot(activeSlotId());
    }

    private static int activeSlotId() {
        Integer slotId = SlotScope.idOrNull();
        return slotId != null ? slotId : 0;
    }
}
