package net.jr.client.ui.dsl;

import java.util.Objects;

/** A DSL declaration name and the Java type that owns its meaning. */
public record UiDslKind<E extends DslElement>(String name, Class<E> javaType) {
    public UiDslKind {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(javaType, "javaType");
        if (name.isBlank()) {
            throw new IllegalArgumentException("A DSL element name cannot be blank");
        }
    }
}
