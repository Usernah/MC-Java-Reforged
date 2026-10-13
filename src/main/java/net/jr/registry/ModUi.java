package net.jr.registry;

import net.jr.api.client.ui.IUiRegistryProvider;
import net.jr.api.client.ui.UiRegister;
import net.jr.api.client.ui.UiContainerFile;
import net.jr.api.client.ui.UiScreenFile;

import static net.jr.Java_reforged.MODID;

public final class ModUi implements IUiRegistryProvider {
    public static final ModUi INSTANCE = new ModUi();
    public static final UiRegister REGISTRY = UiRegister.create(MODID);

    public static final UiScreenFile TEST_SCREEN = REGISTRY.registerScreen("test");
    public static final UiContainerFile GENERIC_27 = REGISTRY.registerContainer("generic_27");
    public static final UiContainerFile GENERIC_54 = REGISTRY.registerContainer("generic_54");

    private ModUi() {
    }

    public static void bootstrap() {

    }

    @Override
    public UiRegister getUiRegister() {
        return REGISTRY;
    }
}
