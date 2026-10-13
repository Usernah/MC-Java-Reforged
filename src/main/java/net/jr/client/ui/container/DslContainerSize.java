package net.jr.client.ui.container;

import net.jr.api.client.ui.UiContainerFile;
import net.jr.client.ui.dsl.UiCompiledDocument;
import net.jr.client.ui.dsl.UiDataContext;
import net.jr.client.ui.dsl.UiDocumentManager;

/** Dimensions passed to AbstractContainerScreen before vanilla calculates its origin. */
public record DslContainerSize(int width, int height) {
    public DslContainerSize {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                "Container dimensions must be positive, received " + width + "x" + height
            );
        }
    }

    public static DslContainerSize resolve(
        UiContainerFile file,
        int fallbackWidth,
        int fallbackHeight
    ) {
        UiCompiledDocument document = UiDocumentManager.getInstance().requireCompiled(file);
        if (!document.hasBounds()) {
            return new DslContainerSize(fallbackWidth, fallbackHeight);
        }
        UiDataContext data = UiDataContext.withGlobals();
        return new DslContainerSize(
            document.boundsWidth(data),
            document.boundsHeight(data)
        );
    }
}
