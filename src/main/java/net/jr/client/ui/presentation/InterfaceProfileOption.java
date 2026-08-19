package net.jr.client.ui.presentation;

import com.mojang.serialization.Codec;
import java.util.List;
import net.jr.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;

public final class InterfaceProfileOption {
    public static final OptionInstance<InterfaceProfile> INSTANCE = new OptionInstance<>(
        "menu.java_reforged.video.interface_profile",
        OptionInstance.cachedConstantTooltip(
            net.minecraft.network.chat.Component.translatable("menu.java_reforged.video.interface_profile.description")
        ),
        (caption, profile) -> Options.genericValueLabel(caption, profile.caption()),
        new OptionInstance.SliderableEnum<>(
            List.of(InterfaceProfile.STANDARD, InterfaceProfile.PORTABLE),
            Codec.STRING.xmap(InterfaceProfileOption::decode, InterfaceProfile::serializedName)
        ),
        ClientConfig.interfaceProfile(),
        profile -> {
            ClientConfig.setInterfaceProfile(profile);
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.isRunning()) {
                minecraft.resizeGui();
            }
        }
    );

    private InterfaceProfileOption() {
    }

    private static InterfaceProfile decode(String name) {
        for (InterfaceProfile profile : InterfaceProfile.values()) {
            if (profile.serializedName().equals(name)) {
                return profile;
            }
        }
        return InterfaceProfile.STANDARD;
    }
}
