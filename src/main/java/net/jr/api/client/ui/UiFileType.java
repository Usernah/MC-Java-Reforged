package net.jr.api.client.ui;

import java.util.Arrays;
import java.util.Optional;

public enum UiFileType {
    SCREEN("screens", "screen"),
    CONTAINER("containers", "container"),
    OVERLAY("overlays", "overlay"),
    STYLE("styles", "style");

    private final String directory;
    private final String extension;

    UiFileType(String directory, String extension) {
        this.directory = directory;
        this.extension = extension;
    }

    public String directory() {
        return this.directory;
    }

    public String extension() {
        return this.extension;
    }

    public String resourcePath(String path) {
        return "ui/" + this.directory + "/" + path + "." + this.extension;
    }

    public static Optional<UiFileType> fromImportPath(String path) {
        return Arrays.stream(values())
            .filter(type -> path.startsWith(type.directory + "/"))
            .findFirst();
    }

    public String stripDirectory(String path) {
        return path.substring(this.directory.length() + 1);
    }
}
