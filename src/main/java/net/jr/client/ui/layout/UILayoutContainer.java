package net.jr.client.ui.layout;

import net.jr.api.client.ui.UiContainerFile;
import net.jr.api.client.ui.UiFileType;

public abstract class UILayoutContainer extends UILayout {
    protected UILayoutContainer(UiContainerFile uiFile) {
        super(uiFile, UiFileType.CONTAINER);
    }
}
