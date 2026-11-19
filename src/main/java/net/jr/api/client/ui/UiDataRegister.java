package net.jr.api.client.ui;

import java.util.Collection;
import java.util.Optional;
import net.jr.client.ui.registry.UiDataRegistryStore;

public final class UiDataRegister {
    private final UiDataRegistryStore store;

    private UiDataRegister(UiDataRegistryStore store) {
        this.store = store;
    }

    public static UiDataRegister create(String modId) {
        return new UiDataRegister(UiDataRegistryStore.create(modId));
    }

    public String modId() {
        return this.store.modId();
    }

    public UiDataFile register(String name) {
        return wrap(this.store.register(name));
    }

    public Optional<UiDataFile> get(String id) {
        return this.store.get(id).map(UiDataRegister::wrap);
    }

    public Collection<UiDataFile> entries() {
        return this.store.entries().stream().map(UiDataRegister::wrap).toList();
    }

    public static Collection<UiDataFile> allRegisteredFiles() {
        return UiDataRegistryStore.allRegisteredFiles().stream()
            .map(UiDataRegister::wrap)
            .toList();
    }

    private static UiDataFile wrap(UiDataRegistryStore.Entry entry) {
        return new UiDataFile(new UiDataId(entry.modId(), entry.name()));
    }
}
