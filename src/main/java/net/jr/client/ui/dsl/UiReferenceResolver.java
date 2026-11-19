package net.jr.client.ui.dsl;

import java.util.List;
import net.jr.api.client.resource.Asset;
import net.jr.api.client.ui.UiFileType;
import net.jr.client.ui.dsl.UiDocument.BlueprintParameterValue;
import net.jr.client.ui.dsl.UiDocument.ImportDirective;
import net.jr.client.ui.dsl.UiDocument.ObjectValue;
import net.jr.client.ui.dsl.UiDocument.ReferenceValue;
import net.jr.client.ui.dsl.UiDocument.RegisteredStyleValue;
import net.jr.client.ui.dsl.UiDocument.Value;

final class UiReferenceResolver {
    private static final int MAX_DEPTH = 64;
    private final UiCompilationContext context;

    UiReferenceResolver(UiCompilationContext context) {
        this.context = context;
    }

    Resolved resolve(UiDocument document, Value value, int depth) throws UiCompileException {
        if (value instanceof BlueprintParameterValue parameter) {
            throw this.context.error(document, parameter.position(),
                "Blueprint parameter references are only valid inside a blueprint");
        }
        if (value instanceof RegisteredStyleValue registered) {
            this.context.profile().require(
                UiLanguageCapability.STYLE_REFERENCES, document, registered.position()
            );
            this.checkDepth(document, value, depth);
            List<String> path = registered.path();
            if (path.size() < 2) {
                throw this.context.error(document, registered.position(),
                    "A registered style value reference must be '::style_id.definition'");
            }
            UiDocument style = this.context.styles().get(path.getFirst());
            if (style == null) {
                throw this.context.error(document, registered.position(),
                    "Unknown registered style '::" + path.getFirst() + "'");
            }
            Value definition = style.definition(path.get(1)).orElseThrow(() ->
                this.context.error(document, registered.position(),
                    "Registered style '::" + path.getFirst() + "' has no definition '"
                        + path.get(1) + "'")
            );
            return new Resolved(
                style,
                this.descend(style, definition, path.subList(2, path.size())),
                null
            );
        }
        if (!(value instanceof ReferenceValue reference)) {
            return null;
        }
        this.checkDepth(document, value, depth);
        List<String> path = reference.path();
        String root = path.getFirst();
        Value definition = document.definition(root).orElse(null);
        if (definition != null) {
            return new Resolved(
                document,
                this.descend(document, definition, path.subList(1, path.size())),
                null
            );
        }

        ImportDirective imported = document.imports().stream()
            .filter(candidate -> candidate.alias().equals(root))
            .findFirst()
            .orElseThrow(() -> this.context.error(
                document, reference.position(), "Unknown symbol '@" + root + "'"
            ));
        Asset importedAsset = this.importedAsset(document.source(), imported);
        UiFileType importedType = UiFileType.fromImportPath(imported.target().path()).orElse(null);
        if (importedType == null) {
            this.context.profile().require(
                UiLanguageCapability.ASSET_IMPORTS, document, reference.position()
            );
            if (path.size() != 1) {
                throw this.context.error(document, reference.position(),
                    "A raw Asset reference cannot have nested properties");
            }
            return new Resolved(document, null, importedAsset);
        }
        this.context.profile().require(
            UiLanguageCapability.DOCUMENT_IMPORTS, document, reference.position()
        );
        if (importedType == UiFileType.STYLE) {
            throw this.context.error(document, reference.position(),
                "Style documents cannot be imported; register the style and use '::style_id.definition'");
        }
        UiDocument importedDocument = this.context.documents().get(importedAsset);
        if (importedDocument == null) {
            throw this.context.error(document, reference.position(),
                "Imported UI document is not loaded: " + importedAsset);
        }
        if (path.size() < 2) {
            throw this.context.error(document, reference.position(),
                "A UI document alias must reference one of its definitions");
        }
        Value importedDefinition = importedDocument.definition(path.get(1)).orElseThrow(() ->
            this.context.error(document, reference.position(),
                "Imported definition '" + path.get(1) + "' does not exist in " + importedAsset)
        );
        return new Resolved(
            importedDocument,
            this.descend(importedDocument, importedDefinition, path.subList(2, path.size())),
            null
        );
    }

    Asset importedAsset(Asset owner, ImportDirective imported) {
        String namespace = imported.target().local() ? owner.namespace() : imported.target().namespace();
        String path = imported.target().path();
        UiFileType type = UiFileType.fromImportPath(path).orElse(null);
        String resourcePath = type == null ? path : type.resourcePath(type.stripDirectory(path));
        return Asset.NamespaceAndPatch(namespace, resourcePath);
    }

    Value descend(UiDocument document, Value value, List<String> path)
        throws UiCompileException {
        Value current = value;
        for (String segment : path) {
            if (!(current instanceof ObjectValue object)) {
                throw this.context.error(document, current.position(),
                    "Cannot read property '" + segment + "' from this value");
            }
            current = object.values().get(segment);
            if (current == null) {
                throw this.context.error(document, object.position(),
                    "Unknown property '" + segment + "'");
            }
        }
        return current;
    }

    void checkDepth(UiDocument document, Value value, int depth)
        throws UiCompileException {
        if (depth > MAX_DEPTH) {
            throw this.context.error(
                document, value.position(), "Definition/reference chain is too deep or circular"
            );
        }
    }

    record Resolved(UiDocument document, Value value, Asset asset) {
    }
}
