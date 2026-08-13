package net.jr;

import net.jr.api.client.split.SplitOrientation;
import net.jr.client.ui.presentation.InterfaceProfile;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.EnumValue<InterfaceProfile> INTERFACE_PROFILE = BUILDER
        .comment("Interface presentation profile")
        .defineEnum("interfaceProfile", InterfaceProfile.STANDARD);
    public static final ModConfigSpec.EnumValue<SplitOrientation> SPLIT_ORIENTATION = BUILDER
        .comment("Orientation used by the two-player split-screen layout")
        .defineEnum("splitOrientation", SplitOrientation.VERTICAL);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }

    public static InterfaceProfile interfaceProfile() {
        return INTERFACE_PROFILE.get();
    }

    public static void setInterfaceProfile(InterfaceProfile profile) {
        INTERFACE_PROFILE.set(profile);
        INTERFACE_PROFILE.save();
    }

    public static SplitOrientation splitOrientation() {
        return SPLIT_ORIENTATION.get();
    }

    public static void setSplitOrientation(SplitOrientation orientation) {
        SPLIT_ORIENTATION.set(orientation);
        SPLIT_ORIENTATION.save();
    }
}
