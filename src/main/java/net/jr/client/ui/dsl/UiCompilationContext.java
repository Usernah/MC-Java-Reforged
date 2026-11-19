package net.jr.client.ui.dsl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.jr.api.client.resource.Asset;
import net.jr.client.ui.dsl.UiDocument.ElementValue;

final class UiCompilationContext {
    private final Map<Asset, UiDocument> documents;
    private final Map<String, UiDocument> styles;
    private final List<UiCompileException> diagnostics = new ArrayList<>();
    private final State state = new State();
    private UiDocument document;
    private UiLanguageProfile profile;

    UiCompilationContext(Map<Asset, UiDocument> documents, Map<String, UiDocument> styles) {
        this.documents = Map.copyOf(documents);
        this.styles = Map.copyOf(styles);
    }

    void begin(UiDocument document) {
        this.document = document;
        this.profile = UiLanguageProfile.forType(document.type());
        this.diagnostics.clear();
        this.state.clear();
    }

    UiDocument document() {
        return this.document;
    }

    UiLanguageProfile profile() {
        return this.profile;
    }

    Map<Asset, UiDocument> documents() {
        return this.documents;
    }

    Map<String, UiDocument> styles() {
        return this.styles;
    }

    List<UiCompileException> diagnostics() {
        return this.diagnostics;
    }

    State state() {
        return this.state;
    }

    UiCompileException error(UiDocument source, UiDocument.Position position, String message) {
        return new UiCompileException(source.source(), position, message);
    }

    final class State {
        private int nextId;
        private final Map<String, SourcePosition> ids = new LinkedHashMap<>();
        private final List<ElementReference> references = new ArrayList<>();
        private final Map<String, UiCompiledDocument.SourceLocation> locations =
            new LinkedHashMap<>();
        private final Map<String, Map<String, UiCompiledDocument.SourceLocation>> propertyLocations =
            new LinkedHashMap<>();

        private void clear() {
            this.nextId = 0;
            this.ids.clear();
            this.references.clear();
            this.locations.clear();
            this.propertyLocations.clear();
        }

        String nextId(String kind) {
            return "$" + kind + "_" + ++this.nextId;
        }

        Checkpoint checkpoint() {
            return new Checkpoint(
                this.nextId,
                new LinkedHashMap<>(this.ids),
                new ArrayList<>(this.references),
                new LinkedHashMap<>(this.locations),
                copyPropertyLocations(this.propertyLocations)
            );
        }

        void rollback(Checkpoint checkpoint) {
            this.nextId = checkpoint.nextId();
            this.ids.clear();
            this.ids.putAll(checkpoint.ids());
            this.references.clear();
            this.references.addAll(checkpoint.references());
            this.locations.clear();
            this.locations.putAll(checkpoint.locations());
            this.propertyLocations.clear();
            this.propertyLocations.putAll(copyPropertyLocations(checkpoint.propertyLocations()));
        }

        void register(UiDocument source, UiDocument.Position position, String id)
            throws UiCompileException {
            SourcePosition previous = this.ids.putIfAbsent(
                id, new SourcePosition(source.source(), position)
            );
            if (previous != null) {
                throw UiCompilationContext.this.error(source, position,
                    "Duplicate UI element id '" + id + "' (first declared at "
                        + previous.source() + ":" + previous.position().line() + ":"
                        + previous.position().column() + ")");
            }
            this.locate(source, position, id);
        }

        void reference(UiDocument source, ElementValue element) {
            this.references.add(new ElementReference(source, element.id(), element.position()));
        }

        void validateReferences() {
            for (ElementReference reference : this.references) {
                if (!this.ids.containsKey(reference.id())) {
                    UiCompilationContext.this.diagnostics.add(
                        UiCompilationContext.this.error(
                            reference.document(),
                            reference.position(),
                            "Unknown UI element id '" + reference.id() + "'"
                        )
                    );
                }
            }
        }

        void locate(UiDocument source, UiDocument.Position position, String id) {
            this.locations.putIfAbsent(
                id,
                new UiCompiledDocument.SourceLocation(
                    source.source(), position.line(), position.column()
                )
            );
        }

        Map<String, UiCompiledDocument.SourceLocation> locations() {
            return Map.copyOf(this.locations);
        }

        void locateProperty(Asset source, UiDocument.Position position, String id, String property, boolean editable) {
            this.locateProperty(source, position, id, property, editable, editable);
        }

        void locateProperty(Asset source, UiDocument.Position position, String id, String property,
                            boolean editable, boolean branchesEditable) {
            this.propertyLocations
                .computeIfAbsent(id, ignored -> new LinkedHashMap<>())
                .putIfAbsent(
                    property,
                    new UiCompiledDocument.SourceLocation(
                        source, position.line(), position.column(), editable, branchesEditable
                    )
                );
        }

        Map<String, Map<String, UiCompiledDocument.SourceLocation>> propertyLocations() {
            return copyPropertyLocations(this.propertyLocations);
        }

        private static Map<String, Map<String, UiCompiledDocument.SourceLocation>> copyPropertyLocations(
            Map<String, Map<String, UiCompiledDocument.SourceLocation>> source
        ) {
            Map<String, Map<String, UiCompiledDocument.SourceLocation>> copy = new LinkedHashMap<>();
            source.forEach((id, values) -> copy.put(id, new LinkedHashMap<>(values)));
            return copy;
        }
    }

    record Checkpoint(
        int nextId,
        Map<String, SourcePosition> ids,
        List<ElementReference> references,
        Map<String, UiCompiledDocument.SourceLocation> locations,
        Map<String, Map<String, UiCompiledDocument.SourceLocation>> propertyLocations
    ) {
    }

    private record SourcePosition(Asset source, UiDocument.Position position) {
    }

    private record ElementReference(
        UiDocument document,
        String id,
        UiDocument.Position position
    ) {
    }
}
