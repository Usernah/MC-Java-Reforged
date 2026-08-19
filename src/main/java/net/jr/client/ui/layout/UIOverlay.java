package net.jr.client.ui.layout;

import net.jr.api.client.ui.UiFileType;
import net.jr.api.client.ui.UiOverlayFile;

public abstract class UIOverlay extends UILayout {
    private final UILayout parent;

    protected UIOverlay(UiOverlayFile uiFile) {
        this(uiFile, null);
    }

    protected UIOverlay(UiOverlayFile uiFile, UILayout parent) {
        super(uiFile, UiFileType.OVERLAY);
        this.parent = parent;
    }

    public final UILayout parent() {
        return this.parent;
    }

    protected final void returnToParent() {
        this.closeOverlay();
    }
}
