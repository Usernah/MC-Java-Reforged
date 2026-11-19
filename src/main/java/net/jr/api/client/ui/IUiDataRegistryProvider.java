package net.jr.api.client.ui;

import java.util.Collection;

public interface IUiDataRegistryProvider {
    UiDataRegister getUiDataRegister();

    default Collection<UiDataFile> getRegisteredUiDataFiles() {
        return this.getUiDataRegister().entries();
    }
}
