package net.jr.api.client.ui;

import java.util.Objects;

public record UiDataId(String namespace, String path) {
    public UiDataId {
        namespace = requirePart(namespace, "namespace");
        path = requirePart(path, "path");
    }

    private static String requirePart(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("UI data ID " + name + " cannot be blank");
        }
        if (normalized.indexOf('\\') >= 0 || normalized.startsWith("/")
            || normalized.endsWith("/")) {
            throw new IllegalArgumentException("Invalid UI data ID " + name + ": " + value);
        }
        for (String segment : normalized.split("/")) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IllegalArgumentException("Invalid UI data ID " + name + ": " + value);
            }
        }
        return normalized;
    }

    @Override
    public String toString() {
        return this.namespace + ":" + this.path;
    }
}
