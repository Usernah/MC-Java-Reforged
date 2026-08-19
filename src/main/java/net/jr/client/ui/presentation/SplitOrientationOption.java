package net.jr.client.ui.presentation;

import com.mojang.serialization.Codec;
import java.util.List;
import net.jr.ClientConfig;
import net.jr.client.runtime.ClientRuntime;
import net.jr.api.client.split.SplitOrientation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;

public final class SplitOrientationOption {
    public static final OptionInstance<SplitOrientation> INSTANCE = new OptionInstance<>(
        "menu.java_reforged.video.split_orientation",
        OptionInstance.cachedConstantTooltip(
            net.minecraft.network.chat.Component.translatable("menu.java_reforged.video.split_orientation.description")
        ),
        (caption, orientation) -> Options.genericValueLabel(caption, orientation.caption()),
        new OptionInstance.SliderableEnum<>(
            List.of(SplitOrientation.VERTICAL, SplitOrientation.HORIZONTAL),
            Codec.STRING.xmap(SplitOrientationOption::decode, SplitOrientation::serializedName)
        ),
        ClientConfig.splitOrientation(),
        orientation -> {
            ClientConfig.setSplitOrientation(orientation);
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.isRunning()) {
                ClientRuntime.INSTANCE.viewportResize().setTwoPlayerOrientation(minecraft, orientation);
            }
        }
    );

    private SplitOrientationOption() {
    }

    private static SplitOrientation decode(String name) {
        for (SplitOrientation orientation : SplitOrientation.values()) {
            if (orientation.serializedName().equals(name)) {
                return orientation;
            }
        }
        return SplitOrientation.VERTICAL;
    }
}
