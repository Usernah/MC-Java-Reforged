package net.jr.api.client.ui;

import java.util.Collection;

public interface IUiRegistryProvider {
    UiRegister getUiRegister();

    default Collection<UiFile> getRegisteredUiFiles() {
        return this.getUiRegister().entries();
    }
}
