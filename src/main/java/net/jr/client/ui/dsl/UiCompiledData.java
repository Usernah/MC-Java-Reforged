package net.jr.client.ui.dsl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.jr.api.client.resource.Asset;
import net.jr.client.ui.dsl.UiCompiledDocument.DataContext;
import net.jr.client.ui.dsl.UiCompiledDocument.Expression;

final class UiCompiledData {
    private final Asset source;
    private final Map<String, Expression<Object>> values;
    private final Map<String, UiCompiledDocument.SourceLocation> locations;
    private final boolean usable;

    UiCompiledData(Asset source, Map<String, Expression<Object>> values,
                   Map<String, UiCompiledDocument.SourceLocation> locations) {
        this(source, values, locations, true);
    }

    private UiCompiledData(
        Asset source,
        Map<String, Expression<Object>> values,
        Map<String, UiCompiledDocument.SourceLocation> locations,
        boolean usable
    ) {
        this.source = Objects.requireNonNull(source, "source");
        this.values = Map.copyOf(values);
        this.locations = Map.copyOf(locations);
        this.usable = usable;
    }

    static UiCompiledData empty(Asset source) {
        return new UiCompiledData(source, Map.of(), Map.of(), false);
    }

    boolean isUsable() {
        return this.usable;
    }

    UiCompiledDocument.SourceLocation location(String id) {
        return this.locations.get(id);
    }

    Map<String, Object> resolve(DataContext context) {
        if (!this.usable) {
            return Map.of();
        }
        Map<String, Object> resolved = new LinkedHashMap<>();
        try {
            this.values.forEach((id, expression) ->
                resolved.put(id, expression.resolve(context))
            );
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                "Could not resolve UI data file " + this.source + ": "
                    + (exception.getMessage() == null
                        ? exception.getClass().getSimpleName()
                        : exception.getMessage()),
                exception
            );
        }
        return resolved;
    }
}
