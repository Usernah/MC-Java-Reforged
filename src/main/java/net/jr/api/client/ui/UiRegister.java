package net.jr.api.client.ui;

import net.jr.client.ui.registry.UiRegistryStore;

import java.util.Collection;
import java.util.Optional;

public final class UiRegister {
    private final UiRegistryStore store;

    private UiRegister(UiRegistryStore store) {
        this.store = store;
    }

    public static UiRegister create(String modId) {
        return new UiRegister(UiRegistryStore.create(modId));
    }

    public String modId() {
        return this.store.modId();
    }

    public UiScreenFile registerScreen(String path) {
        return (UiScreenFile)wrap(this.store.register(UiFileType.SCREEN, path));
    }

    public UiContainerFile registerContainer(String path) {
        return (UiContainerFile)wrap(this.store.register(UiFileType.CONTAINER, path));
    }

    public UiOverlayFile registerOverlay(String path) {
        return (UiOverlayFile)wrap(this.store.register(UiFileType.OVERLAY, path));
    }

    public Optional<UiFile> get(UiFileType type, String path) {
        return this.store.get(type, path).map(UiRegister::wrap);
    }

    public Collection<UiFile> entries() {
        return this.store.entries().stream().map(UiRegister::wrap).toList();
    }

    public static Collection<UiFile> allRegisteredFiles() {
        return UiRegistryStore.allRegisteredFiles().stream().map(UiRegister::wrap).toList();
    }

    private static UiFile wrap(UiRegistryStore.UiEntry entry) {
        return switch (entry.type()) {
            case SCREEN -> new UiScreenFile(entry.modId(), entry.path());
            case CONTAINER -> new UiContainerFile(entry.modId(), entry.path());
            case OVERLAY -> new UiOverlayFile(entry.modId(), entry.path());
            case STYLE -> throw new IllegalArgumentException(
                    "Style files are resolved through imports and cannot be registered"
            );
        };
    }
}
