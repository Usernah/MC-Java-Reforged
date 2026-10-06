package net.jr.client.compat.sodium;

import java.util.EnumSet;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPointForge;
import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.Range;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ModOptionsBuilder;
import net.jr.Java_reforged;
import net.jr.api.client.split.SplitOrientation;
import net.jr.client.runtime.options.OptionsStateResolver;
import net.jr.client.runtime.viewport.GuiScaleCalculator;
import net.jr.client.ui.presentation.ExperimentalRenderOption;
import net.jr.client.ui.presentation.InterfaceProfile;
import net.jr.client.ui.presentation.InterfaceProfileOption;
import net.jr.client.ui.presentation.SplitOrientationOption;
import net.jr.client.ui.presentation.UiPresentation;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

@ConfigEntryPointForge(Java_reforged.MODID)
public final class SodiumConfigIntegration implements ConfigEntryPoint {
    private static final Identifier SODIUM_GUI_SCALE = Identifier.parse("sodium:general.gui_scale");
    private static final StorageEventHandler NO_OP_STORAGE = () -> {
    };

    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        ModOptionsBuilder modOptions = builder.registerOwnModOptions();

        modOptions.registerOptionReplacement(SODIUM_GUI_SCALE, this.guiScaleOption(builder));
        modOptions.addPage(
            builder.createOptionPage()
                .setName(Component.translatable("menu.java_reforged.video.page"))
                .addOptionGroup(
                    builder.createOptionGroup()
                        .setName(Component.translatable("menu.java_reforged.video.group.interface"))
                        .addOption(this.interfaceProfileOption(builder))
                        .addOption(this.splitOrientationOption(builder))
                )
                .addOptionGroup(
                    builder.createOptionGroup()
                        .setName(Component.translatable("menu.java_reforged.video.group.rendering"))
                        .addOption(this.experimentalRenderOption(builder))
                )
        );
    }

    private net.caffeinemc.mods.sodium.api.config.structure.IntegerOptionBuilder guiScaleOption(
        ConfigBuilder builder
    ) {
        return builder.createIntegerOption(id("video.gui_scale"))
            .setName(Component.translatable("options.guiScale"))
            .setTooltip(Component.translatable("sodium.options.gui_scale.tooltip"))
            .setStorageHandler(NO_OP_STORAGE)
            .setDefaultValue(0)
            .setBinding(
                value -> OptionsStateResolver.current().guiScale().set(value),
                () -> OptionsStateResolver.current().requestedGuiScale()
            )
            .setRangeProvider(state -> guiScaleRange(), ConfigState.UPDATE_ON_REBUILD)
            .setValueFormatter(value -> value == 0
                ? Component.translatable("options.guiScale.auto")
                : Component.literal(Integer.toString(value))
            );
    }

    private net.caffeinemc.mods.sodium.api.config.structure.EnumOptionBuilder<InterfaceProfile>
    interfaceProfileOption(ConfigBuilder builder) {
        return builder.createEnumOption(id("video.interface_profile"), InterfaceProfile.class)
            .setName(Component.translatable("menu.java_reforged.video.interface_profile"))
            .setTooltip(Component.translatable("menu.java_reforged.video.interface_profile.description"))
            .setStorageHandler(NO_OP_STORAGE)
            .setDefaultValue(InterfaceProfile.STANDARD)
            .setBinding(InterfaceProfileOption.INSTANCE::set, InterfaceProfileOption.INSTANCE::get)
            .setAllowedValues(EnumSet.allOf(InterfaceProfile.class))
            .setElementNameProvider(InterfaceProfile::caption)
            .setEnabledProvider(
                state -> !UiPresentation.isSplitScreen(),
                ConfigState.UPDATE_ON_REBUILD
            );
    }

    private net.caffeinemc.mods.sodium.api.config.structure.EnumOptionBuilder<SplitOrientation>
    splitOrientationOption(ConfigBuilder builder) {
        return builder.createEnumOption(id("video.split_orientation"), SplitOrientation.class)
            .setName(Component.translatable("menu.java_reforged.video.split_orientation"))
            .setTooltip(Component.translatable("menu.java_reforged.video.split_orientation.description"))
            .setStorageHandler(NO_OP_STORAGE)
            .setDefaultValue(SplitOrientation.VERTICAL)
            .setBinding(SplitOrientationOption.INSTANCE::set, SplitOrientationOption.INSTANCE::get)
            .setAllowedValues(EnumSet.allOf(SplitOrientation.class))
            .setElementNameProvider(SplitOrientation::caption);
    }

    private net.caffeinemc.mods.sodium.api.config.structure.BooleanOptionBuilder experimentalRenderOption(
        ConfigBuilder builder
    ) {
        return builder.createBooleanOption(id("video.experimental_render"))
            .setName(Component.translatable("menu.java_reforged.video.optimize_experimental"))
            .setTooltip(Component.translatable("menu.java_reforged.video.optimize_experimental.description"))
            .setStorageHandler(NO_OP_STORAGE)
            .setDefaultValue(false)
            .setBinding(ExperimentalRenderOption.INSTANCE::set, ExperimentalRenderOption.INSTANCE::get);
    }

    private static Range guiScaleRange() {
        Minecraft minecraft = Minecraft.getInstance();
        int maximum = minecraft.isRunning()
            ? GuiScaleCalculator.maximumForSlot(currentSlotId(), minecraft.isEnforceUnicode())
            : 1;
        return new Range(0, Math.max(1, maximum), 1);
    }

    private static int currentSlotId() {
        Integer slotId = net.jr.client.runtime.context.SlotScope.idOrNull();
        return slotId != null ? slotId : 0;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Java_reforged.MODID, path);
    }
}
