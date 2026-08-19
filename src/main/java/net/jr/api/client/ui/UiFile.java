package net.jr.api.client.ui;

import net.jr.api.client.resource.Asset;

import java.util.Objects;

public abstract sealed class UiFile permits UiScreenFile, UiContainerFile, UiOverlayFile {
    private final String modId;
    private final String path;
    private final UiFileType type;
    private final Asset asset;

    UiFile(String modId, String path, UiFileType type) {
        this.modId = Objects.requireNonNull(modId, "modId");
        this.path = Objects.requireNonNull(path, "path");
        this.type = Objects.requireNonNull(type, "type");
        this.asset = Asset.NamespaceAndPatch(modId, type.resourcePath(path));
    }

    public String modId() {
        return this.modId;
    }

    public String path() {
        return this.path;
    }

    public UiFileType type() {
        return this.type;
    }

    public Asset asset() {
        return this.asset;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UiFile that)) {
            return false;
        }
        return this.modId.equals(that.modId) && this.path.equals(that.path) && this.type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.modId, this.path, this.type);
    }

    @Override
    public String toString() {
        return this.modId + ":" + this.type.directory() + "/" + this.path;
    }
}
