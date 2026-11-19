package net.jr.api.client.ui;

import java.util.Objects;
import net.jr.api.client.resource.Asset;
import net.jr.client.ui.dsl.UiDocumentManager;

public final class UiDataFile {
    private final UiDataId id;
    private final Asset asset;

    public UiDataFile(UiDataId id) {
        this.id = Objects.requireNonNull(id, "id");
        this.asset = Asset.NamespaceAndPatch(
            id.namespace(), UiFileType.DATA.resourcePath(id.path())
        );
    }

    public UiDataId id() {
        return this.id;
    }

    public String fileName() {
        return this.id.path();
    }

    public Asset asset() {
        return this.asset;
    }

    public UiDataValues readData() {
        return UiDocumentManager.getInstance().readData(this);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof UiDataFile that && this.id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}
