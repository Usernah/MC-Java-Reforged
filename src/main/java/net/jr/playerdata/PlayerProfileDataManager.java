package net.jr.playerdata;

import net.jr.Java_reforged;
import net.jr.ClientRuntime.player.SplitPlayerName;
import net.jr.util.LegacyPlayerColors;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@EventBusSubscriber(modid = Java_reforged.MODID)
public final class PlayerProfileDataManager {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Java_reforged.MODID);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerProfileData>> PLAYER_PROFILE =
            ATTACHMENTS.register("player_profile", () -> AttachmentType.builder(() -> PlayerProfileData.DEFAULT)
                    .serialize(PlayerProfileData.CODEC)
                    .copyOnDeath()
                    .sync(PlayerProfileData.STREAM_CODEC)
                    .build());

    private PlayerProfileDataManager() {
    }

    public static PlayerProfileData get(Player player) {
        return player.getData(PLAYER_PROFILE);
    }

    public static int getLegacyColor(Player player) {
        return get(player).legacyColorRgb();
    }

    public static Component getSplitDisplayName(Player player) {
        PlayerProfileData data = player.getExistingDataOrNull(PLAYER_PROFILE);
        int ordinal = data == null ? PlayerProfileData.NOT_A_SPLIT_PLAYER : data.splitOrdinal();
        Component inferredName = SplitPlayerName.visibleName(player.getGameProfile());
        if (ordinal < 0) {
            return inferredName;
        }

        String internalName = player.getGameProfile().name();
        int separator = internalName.lastIndexOf('_');
        if (separator <= 0) {
            return inferredName;
        }
        return Component.literal(internalName.substring(0, separator) + " (" + ordinal + ")");
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            assignSessionColor(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.setData(PLAYER_PROFILE, PlayerProfileData.DEFAULT);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ensureSessionColor(player);
        }
    }

    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        Component displayName = getSplitDisplayName(event.getEntity());
        if (displayName != null) {
            event.setDisplayname(displayName.copy().withStyle(event.getUsername().getStyle()));
        }
    }

    @SubscribeEvent
    public static void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        Component displayName = getSplitDisplayName(event.getEntity());
        if (displayName != null) {
            event.setDisplayName(displayName);
        }
    }

    private static void assignSessionColor(ServerPlayer player) {
        int splitOrdinal = SplitPlayerName.ordinal(player.getGameProfile());
        player.setData(PLAYER_PROFILE, PlayerProfileData.DEFAULT
                .withLegacyColorIndex(findAvailableColorIndex(player))
                .withSplitOrdinal(splitOrdinal));
    }

    private static void ensureSessionColor(ServerPlayer player) {
        PlayerProfileData current = player.getExistingDataOrNull(PLAYER_PROFILE);
        if (current == null || !current.hasLegacyColor()) {
            assignSessionColor(player);
        }
    }

    private static boolean isColorAvailable(ServerPlayer target, int colorIndex) {
        if (colorIndex < 0) {
            return false;
        }
        for (ServerPlayer online : target.level().getServer().getPlayerList().getPlayers()) {
            if (online.getUUID().equals(target.getUUID())) {
                continue;
            }
            PlayerProfileData profile = online.getExistingDataOrNull(PLAYER_PROFILE);
            if (profile != null && profile.hasLegacyColor() && profile.resolvedLegacyColorIndex() == colorIndex) {
                return false;
            }
        }
        return true;
    }

    private static int findAvailableColorIndex(ServerPlayer target) {
        for (int index = 0; index < LegacyPlayerColors.size(); index++) {
            if (isColorAvailable(target, index)) {
                return index;
            }
        }
        return 0;
    }
}
