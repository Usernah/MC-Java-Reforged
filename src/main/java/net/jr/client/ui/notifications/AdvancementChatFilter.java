package net.jr.client.ui.notifications;

import net.jr.Java_reforged;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;

@EventBusSubscriber(modid = Java_reforged.MODID, value = Dist.CLIENT)
public final class AdvancementChatFilter {
    private AdvancementChatFilter() {
    }

    @SubscribeEvent
    public static void onSystemMessage(ClientChatReceivedEvent.System event) {


        if (!event.isOverlay() && event.getMessage().getContents() instanceof TranslatableContents contents) {
            String key = contents.getKey();
            if (key.equals("chat.type.advancement.task")
                || key.equals("chat.type.advancement.goal")
                || key.equals("chat.type.advancement.challenge")) {
                event.setCanceled(true);
            }
        }
    }
}
