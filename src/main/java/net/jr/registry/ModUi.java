package net.jr.registry;

import net.jr.api.client.ui.IUiRegistryProvider;
import net.jr.api.client.ui.UiRegister;
import net.jr.api.client.ui.UiScreenFile;

import static net.jr.Java_reforged.MODID;

public final class ModUi implements IUiRegistryProvider {
    public static final ModUi INSTANCE = new ModUi();
    public static final UiRegister REGISTRY = UiRegister.create(MODID);

    public static final UiScreenFile TEST_SCREEN = REGISTRY.registerScreen("test");

    private ModUi() {
    }

    public static void bootstrap() {

    }

    @Override
    public UiRegister getUiRegister() {
        return REGISTRY;
    }
}
