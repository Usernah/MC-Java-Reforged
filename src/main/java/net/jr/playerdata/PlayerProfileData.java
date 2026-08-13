package net.jr.playerdata;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.jr.util.LegacyPlayerColors;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record PlayerProfileData(int legacyColorIndex, int splitOrdinal) {
    public static final int UNASSIGNED_COLOR_INDEX = -1;
    public static final int NOT_A_SPLIT_PLAYER = -1;
    public static final PlayerProfileData DEFAULT = new PlayerProfileData(UNASSIGNED_COLOR_INDEX, NOT_A_SPLIT_PLAYER);
    public static final MapCodec<PlayerProfileData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("legacyColorIndex", UNASSIGNED_COLOR_INDEX).forGetter(PlayerProfileData::legacyColorIndex),
            Codec.INT.optionalFieldOf("splitOrdinal", NOT_A_SPLIT_PLAYER).forGetter(PlayerProfileData::splitOrdinal)
    ).apply(instance, PlayerProfileData::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerProfileData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            PlayerProfileData::legacyColorIndex,
            ByteBufCodecs.VAR_INT,
            PlayerProfileData::splitOrdinal,
            PlayerProfileData::new
    );

    public boolean hasLegacyColor() {
        return legacyColorIndex >= 0;
    }

    public PlayerProfileData withLegacyColorIndex(int colorIndex) {
        return new PlayerProfileData(Math.floorMod(colorIndex, LegacyPlayerColors.size()), splitOrdinal);
    }

    public PlayerProfileData withSplitOrdinal(int ordinal) {
        return new PlayerProfileData(legacyColorIndex, ordinal);
    }

    public int resolvedLegacyColorIndex() {
        return hasLegacyColor() ? Math.floorMod(legacyColorIndex, LegacyPlayerColors.size()) : 0;
    }

    public int legacyColorRgb() {
        return LegacyPlayerColors.get(resolvedLegacyColorIndex());
    }
}
