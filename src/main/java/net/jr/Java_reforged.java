package net.jr;

import com.mojang.logging.LogUtils;
import net.jr.playerdata.PlayerProfileDataManager;
import net.jr.experimental_features.minigame.network.BattleNetwork;
import net.jr.network.ContainerOriginNetwork;
import net.jr.registry.ModSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Java_reforged.MODID)
public class Java_reforged {

    public static final String MODID = "java_reforged";

    public static final Logger LOGGER = LogUtils.getLogger();

    public Java_reforged(IEventBus modEventBus) {
        ModSounds.REGISTRY.register(modEventBus);
        PlayerProfileDataManager.ATTACHMENTS.register(modEventBus);
        modEventBus.addListener(BattleNetwork::register);
        modEventBus.addListener(ContainerOriginNetwork::register);
    }
}
