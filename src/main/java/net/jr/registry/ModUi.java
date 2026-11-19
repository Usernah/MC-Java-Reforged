package net.jr.registry;

import net.jr.api.client.ui.IUiRegistryProvider;
import net.jr.api.client.ui.IUiDataRegistryProvider;
import net.jr.api.client.ui.UiDataFile;
import net.jr.api.client.ui.UiDataRegister;
import net.jr.api.client.ui.UiRegister;
import net.jr.api.client.ui.UiContainerFile;
import net.jr.api.client.ui.UiScreenFile;

import static net.jr.Java_reforged.MODID;

public final class ModUi implements IUiRegistryProvider, IUiDataRegistryProvider {
    public static final ModUi INSTANCE = new ModUi();
    public static final UiRegister REGISTRY = UiRegister.create(MODID);
    public static final UiDataRegister DATA_REGISTRY = UiDataRegister.create(MODID);

    public static final UiScreenFile TEST_SCREEN = REGISTRY.registerScreen("test");
    public static final UiScreenFile MESSAGE_SCREEN = REGISTRY.registerScreen("message");
    public static final UiScreenFile TRANSITION_SCREEN = REGISTRY.registerScreen("transition");
    public static final UiScreenFile PAUSE_SCN = REGISTRY.registerScreen("pause");
    public static final UiScreenFile OPTIONS_SCN = REGISTRY.registerScreen("options");
    public static final UiContainerFile GENERIC_27 = REGISTRY.registerContainer("generic_27");
    public static final UiContainerFile GENERIC_54 = REGISTRY.registerContainer("generic_54");
    public static final UiContainerFile CRAFTING_2X2 = REGISTRY.registerContainer("crafting_2x2");
    public static final UiContainerFile CRAFTING_3X3 = REGISTRY.registerContainer("crafting_3x3");
    public static final UiContainerFile CREATIVE_INVENTORY = REGISTRY.registerContainer("creative_inventory");
    public static final UiContainerFile SURIVAL_INVENTORY = REGISTRY.registerContainer("survival_inventory");
    public static final UiContainerFile CLASSIC_INVENTORY = REGISTRY.registerContainer("classic_inventory");
    public static final UiContainerFile CLASSIC_CRAFTING_3X3 = REGISTRY.registerContainer("classic_crafting_3x3");
    public static final UiDataFile CONTROL_TOOLTIPS_DAT = DATA_REGISTRY.register("control_tooltips");
    public static final UiDataFile CRAFTING_2X2_DAT = DATA_REGISTRY.register("crafting_2x2");
    public static final UiDataFile CRAFTING_3X3_DAT = DATA_REGISTRY.register("crafting_3x3");
    public static final UiDataFile CREATIVE_INVENTORY_DAT = DATA_REGISTRY.register("creative_inventory");
    public static final UiDataFile SURIVAL_INVENTORY_DAT = DATA_REGISTRY.register("survival_inventory");
    public static final UiDataFile PLAYER_MODEL_HUD_DAT = DATA_REGISTRY.register("player_model_hud");


    private ModUi() {
    }

    public static void bootstrap() {

    }

    @Override
    public UiRegister getUiRegister() {
        return REGISTRY;
    }

    @Override
    public UiDataRegister getUiDataRegister() {
        return DATA_REGISTRY;
    }
}
