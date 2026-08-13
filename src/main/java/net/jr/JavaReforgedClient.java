package net.jr;

import net.minecraft.client.Minecraft;
import net.jr.client.input.InputApi;
import net.jr.client.input.binding.ModKeyBindings;
import net.jr.client.meta.MetaManager;
import net.jr.client.render.ModRenderPipelines;
import net.jr.client.ui.dsl.UiDocumentManager;
import net.jr.registry.ModUi;
import net.jr.registry.ModVideos;
import net.jr.api.client.video.VideoRegister;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

@Mod(value = Java_reforged.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Java_reforged.MODID, value = Dist.CLIENT)
public class JavaReforgedClient {
    public JavaReforgedClient(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        ModUi.bootstrap();
        ModVideos.bootstrap();
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        Java_reforged.LOGGER.info("HELLO FROM CLIENT SETUP");
        Java_reforged.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        event.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            ModKeyBindings.repairLegacyQuickMoveBinding(minecraft);
            InputApi.initializeGamepads();
        });
    }

    @SubscribeEvent
    static void onRegisterReloadListeners(AddClientReloadListenersEvent event) {
        MetaManager.registerClientReloadListener(event);
        VideoRegister.registerClientReloadListener(event);
        UiDocumentManager.registerClientReloadListener(event);
    }

    @SubscribeEvent
    static void onRegisterRenderPipelines(RegisterRenderPipelinesEvent event) {
        ModRenderPipelines.register(event);
    }

}
