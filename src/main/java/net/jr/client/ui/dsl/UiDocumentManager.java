package net.jr.client.ui.dsl;

import net.jr.Java_reforged;
import net.jr.api.client.resource.Asset;
import net.jr.api.client.ui.UiFile;
import net.jr.api.client.ui.UiFileType;
import net.jr.api.client.ui.UiRegister;
import net.jr.api.client.ui.dsl.UiDocument;
import net.jr.api.client.ui.dsl.UiCompiledDocument;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class UiDocumentManager {
    private static final Asset RELOAD_LISTENER_ID = Asset.MOD("reload/ui_documents");
    private static final UiDocumentManager INSTANCE = new UiDocumentManager();

    private volatile Map<UiFile, UiDocument> registeredDocuments = Map.of();
    private volatile Map<UiFile, UiCompiledDocument> compiledDocuments = Map.of();
    private volatile Map<Asset, UiDocument> loadedDocuments = Map.of();
    private volatile Map<Asset, List<UiDiagnostic>> diagnostics = Map.of();

    private UiDocumentManager() {
    }

    public static UiDocumentManager getInstance() {
        return INSTANCE;
    }

    public static void registerClientReloadListener(AddClientReloadListenersEvent event) {
        RELOAD_LISTENER_ID.registerReloadListener(event, new ReloadListener());
    }

    public Optional<UiDocument> get(UiFile file) {
        return Optional.ofNullable(this.registeredDocuments.get(file));
    }

    public Optional<UiCompiledDocument> getCompiled(UiFile file) {
        return Optional.ofNullable(this.compiledDocuments.get(file));
    }

    public UiCompiledDocument requireCompiled(UiFile file) {
        return this.getCompiled(file).orElseThrow(() ->
            new IllegalStateException("UI document is not compiled: " + file)
        );
    }

    public Optional<UiDocument> get(Asset source) {
        return Optional.ofNullable(this.loadedDocuments.get(source));
    }

    public boolean isLoaded(UiFile file) {
        return this.registeredDocuments.containsKey(file);
    }

    public List<UiDiagnostic> diagnostics(UiFile file) {
        return this.diagnostics(file.asset());
    }

    public List<UiDiagnostic> diagnostics(Asset source) {
        return this.diagnostics.getOrDefault(source, List.of());
    }

    public Map<UiFile, UiDocument> registeredDocuments() {
        return this.registeredDocuments;
    }

    private void apply(Snapshot snapshot) {
        this.registeredDocuments = Map.copyOf(snapshot.registeredDocuments());
        this.compiledDocuments = Map.copyOf(snapshot.compiledDocuments());
        this.loadedDocuments = Map.copyOf(snapshot.loadedDocuments());

        Map<Asset, List<UiDiagnostic>> immutableDiagnostics = new LinkedHashMap<>();
        snapshot.diagnostics().forEach((asset, values) ->
            immutableDiagnostics.put(asset, List.copyOf(values))
        );
        this.diagnostics = Map.copyOf(immutableDiagnostics);

        for (List<UiDiagnostic> values : this.diagnostics.values()) {
            for (UiDiagnostic diagnostic : values) {
                if (diagnostic.severity() == UiDiagnostic.Severity.ERROR) {
                    Java_reforged.LOGGER.error("UI DSL: {}", diagnostic.formatted());
                } else {
                    Java_reforged.LOGGER.warn("UI DSL: {}", diagnostic.formatted());
                }
            }
        }

        Java_reforged.LOGGER.info(
            "Loaded {} registered UI documents ({} total including imports)",
            this.registeredDocuments.size(),
            this.loadedDocuments.size()
        );
    }

    private static final class ReloadListener extends SimplePreparableReloadListener<Snapshot> {
        @Override
        protected Snapshot prepare(ResourceManager manager, ProfilerFiller profiler) {
            return new Loader(manager).loadAll();
        }

        @Override
        protected void apply(Snapshot snapshot, ResourceManager manager, ProfilerFiller profiler) {
            INSTANCE.apply(snapshot);
        }
    }

    private static final class Loader {
        private final ResourceManager resourceManager;
        private final Map<UiFile, UiDocument> registeredDocuments = new LinkedHashMap<>();
        private final Map<UiFile, UiCompiledDocument> compiledDocuments = new LinkedHashMap<>();
        private final Map<Asset, UiDocument> loadedDocuments = new LinkedHashMap<>();
        private final Map<Asset, List<UiDiagnostic>> diagnostics = new LinkedHashMap<>();
        private final Set<Asset> loading = new HashSet<>();
        private final Set<Asset> failed = new HashSet<>();

        private Loader(ResourceManager resourceManager) {
            this.resourceManager = resourceManager;
        }

        private Snapshot loadAll() {
            for (UiFile file : UiRegister.allRegisteredFiles()) {
                this.load(file.asset(), file.type()).ifPresent(document ->
                    this.registeredDocuments.put(file, document)
                );
            }
            UiCompiler compiler = new UiCompiler(this.loadedDocuments);
            for (Map.Entry<UiFile, UiDocument> entry : this.registeredDocuments.entrySet()) {
                if (entry.getKey().type() == UiFileType.STYLE) {
                    continue;
                }
                try {
                    this.compiledDocuments.put(entry.getKey(), compiler.compile(entry.getValue()));
                } catch (UiCompileException exception) {
                    this.error(
                        exception.source(),
                        exception.position().line(),
                        exception.position().column(),
                        exception.getMessage()
                    );
                }
            }
            return new Snapshot(
                this.registeredDocuments,
                this.compiledDocuments,
                this.loadedDocuments,
                this.diagnostics
            );
        }

        private Optional<UiDocument> load(Asset source, UiFileType type) {
            UiDocument existing = this.loadedDocuments.get(source);
            if (existing != null) {
                return Optional.of(existing);
            }
            if (this.failed.contains(source)) {
                return Optional.empty();
            }
            if (!this.loading.add(source)) {
                this.error(source, 0, 0, "Circular UI import detected");
                this.failed.add(source);
                return Optional.empty();
            }

            try {
                Optional<Resource> resource = source.find(this.resourceManager);
                if (resource.isEmpty()) {
                    this.error(source, 0, 0, "Registered UI asset was not found");
                    this.failed.add(source);
                    return Optional.empty();
                }

                UiDocument document;
                try (Reader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                    document = UiParser.parse(reader, source, type);
                }

                boolean importsValid = true;
                for (UiDocument.ImportDirective imported : document.imports()) {
                    if (UiFileType.fromImportPath(imported.target().path()).isEmpty()) {
                        continue;
                    }
                    Optional<ImportedSource> resolved = this.resolveImport(source, imported);
                    if (resolved.isEmpty()) {
                        importsValid = false;
                        continue;
                    }
                    ImportedSource dependency = resolved.get();
                    if (this.load(dependency.asset(), dependency.type()).isEmpty()) {
                        this.error(
                            source,
                            imported.position().line(),
                            imported.position().column(),
                            "Could not load import '" + imported.alias() + "' from " + dependency.asset()
                        );
                        importsValid = false;
                    }
                }

                if (!importsValid) {
                    this.failed.add(source);
                    return Optional.empty();
                }

                this.loadedDocuments.put(source, document);
                return Optional.of(document);
            } catch (UiParseException exception) {
                this.error(source, exception.line(), exception.column(), exception.detail());
                this.failed.add(source);
                return Optional.empty();
            } catch (Exception exception) {
                this.error(source, 0, 0, "Could not read UI asset: " + exception.getMessage());
                this.failed.add(source);
                return Optional.empty();
            } finally {
                this.loading.remove(source);
            }
        }

        private Optional<ImportedSource> resolveImport(Asset owner, UiDocument.ImportDirective imported) {
            UiDocument.ImportTarget target = imported.target();
            String path = target.path();
            Optional<UiFileType> type = UiFileType.fromImportPath(path);
            if (type.isEmpty()) {
                return Optional.empty();
            }

            UiFileType fileType = type.get();
            String internalPath = fileType.stripDirectory(path);
            if (!validInternalPath(internalPath)) {
                this.error(
                    owner,
                    imported.position().line(),
                    imported.position().column(),
                    "Invalid imported UI path '" + path + "'"
                );
                return Optional.empty();
            }

            String namespace = target.local() ? owner.namespace() : target.namespace();
            try {
                return Optional.of(new ImportedSource(
                    Asset.NamespaceAndPatch(namespace, fileType.resourcePath(internalPath)),
                    fileType
                ));
            } catch (RuntimeException exception) {
                this.error(
                    owner,
                    imported.position().line(),
                    imported.position().column(),
                    "Invalid imported UI asset: " + exception.getMessage()
                );
                return Optional.empty();
            }
        }

        private void error(Asset source, int line, int column, String message) {
            this.diagnostics.computeIfAbsent(source, ignored -> new ArrayList<>())
                .add(new UiDiagnostic(UiDiagnostic.Severity.ERROR, source, line, column, message));
        }

        private static boolean validInternalPath(String path) {
            if (path.isBlank() || path.startsWith("/") || path.endsWith("/") || path.indexOf('\\') >= 0) {
                return false;
            }
            for (String segment : path.split("/")) {
                if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                    return false;
                }
            }
            for (UiFileType type : UiFileType.values()) {
                if (path.endsWith("." + type.extension())) {
                    return false;
                }
            }
            return true;
        }
    }

    private record ImportedSource(Asset asset, UiFileType type) {
    }

    private record Snapshot(
        Map<UiFile, UiDocument> registeredDocuments,
        Map<UiFile, UiCompiledDocument> compiledDocuments,
        Map<Asset, UiDocument> loadedDocuments,
        Map<Asset, List<UiDiagnostic>> diagnostics
    ) {
        private Snapshot {
            registeredDocuments = new HashMap<>(registeredDocuments);
            compiledDocuments = new HashMap<>(compiledDocuments);
            loadedDocuments = new HashMap<>(loadedDocuments);
            Map<Asset, List<UiDiagnostic>> diagnosticCopy = new HashMap<>();
            diagnostics.forEach((asset, values) -> diagnosticCopy.put(asset, new ArrayList<>(values)));
            diagnostics = diagnosticCopy;
        }
    }
}
