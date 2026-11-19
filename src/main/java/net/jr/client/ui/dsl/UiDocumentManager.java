package net.jr.client.ui.dsl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.jr.Java_reforged;
import net.jr.api.client.resource.Asset;
import net.jr.api.client.ui.UiDataFile;
import net.jr.api.client.ui.UiDataRegister;
import net.jr.api.client.ui.UiDataValues;
import net.jr.api.client.ui.UiFile;
import net.jr.api.client.ui.UiFileType;
import net.jr.api.client.ui.UiRegister;
import net.jr.client.runtime.ui.SlotScreenManager;
import net.jr.client.ui.presentation.UiPresentation;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public final class UiDocumentManager {
    private static final String STYLE_REGISTRY_FILE = "styles_registry.json";
    private static final Asset RELOAD_LISTENER_ID = Asset.MOD("reload/ui_documents");
    private static final UiDocumentManager INSTANCE = new UiDocumentManager();

    private volatile Map<UiFile, UiDocument> registeredDocuments = Map.of();
    private volatile Map<UiFile, UiCompiledDocument> compiledDocuments = Map.of();
    private volatile Map<Asset, UiCompiledDocument> compiledDocumentsByAsset = Map.of();
    private volatile Map<UiDataFile, UiCompiledData> compiledData = Map.of();
    private volatile UiDataContext dataContext;
    private volatile Map<Asset, UiDocument> loadedDocuments = Map.of();
    private volatile Map<Asset, List<UiDiagnostic>> diagnostics = Map.of();
    private volatile Map<Asset, String> sourceOverrides = Map.of();
    private final Set<String> reportedRuntimeDiagnostics = new HashSet<>();

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
        for (String suffix : UiPresentation.documentVariantSuffixes()) {
            UiCompiledDocument variant = this.compiledDocumentsByAsset.get(variantAsset(file, suffix));
            if (variant != null && variant.isUsable()) {
                return Optional.of(variant);
            }
        }
        UiCompiledDocument base = this.compiledDocuments.get(file);
        return base != null && base.isUsable() ? Optional.of(base) : Optional.empty();
    }

    public Optional<UiCompiledDocument> getCompiled(Asset source) {
        UiCompiledDocument document = this.compiledDocumentsByAsset.get(source);
        return document != null && document.isUsable() ? Optional.of(document) : Optional.empty();
    }

    public UiCompiledDocument requireCompiled(UiFile file) {
        return this.getCompiled(file).orElseGet(() ->
            UiCompiledDocument.empty(file.asset(), file.type())
        );
    }

    public boolean isCompiled(UiFile file) {
        return this.getCompiled(file).isPresent();
    }

    public UiDataValues readData(UiDataFile file) {
        return UiDataValues.live(
            file,
            () -> this.isLoaded(file),
            () -> this.resolveData(file)
        );
    }

    private Map<String, Object> resolveData(UiDataFile file) {
        UiCompiledData data = this.compiledData.get(file);
        if (data == null || !data.isUsable()) {
            return Map.of();
        }
        UiDataContext context = this.dataContext;
        if (context == null) {
            context = UiDataContext.withGlobals();
            this.dataContext = context;
        }
        return data.resolve(context);
    }

    public boolean isLoaded(UiDataFile file) {
        UiCompiledData data = this.compiledData.get(file);
        return data != null && data.isUsable();
    }

    public UiCompiledDocument.SourceLocation dataLocation(UiDataFile file, String id) {
        UiCompiledData data = this.compiledData.get(file);
        return data == null ? null : data.location(id);
    }

    public void reportRuntime(UiDslRuntimeException exception) {
        UiCompiledDocument.SourceLocation location = exception.location();
        UiDocument document = this.loadedDocuments.get(location.source());
        String code = document == null ? "" : document.lineText(location.line());
        String message = exception.getMessage() == null || exception.getMessage().isBlank()
            ? exception.getClass().getSimpleName()
            : exception.getMessage();
        UiDiagnostic diagnostic = new UiDiagnostic(
            UiDiagnostic.Severity.ERROR,
            location.source(),
            location.line(),
            location.column(),
            message,
            code,
            UiDiagnostic.suggest(message)
        );
        synchronized (this.reportedRuntimeDiagnostics) {
            if (!this.reportedRuntimeDiagnostics.add(diagnostic.formatted())) {
                return;
            }
        }
        Java_reforged.LOGGER.error("UI DSL runtime error (rendering was skipped):\n{}", diagnostic.formatted());
    }


    public <T> T recoverRuntime(
        UiCompiledDocument.SourceLocation location,
        T fallback,
        Supplier<T> operation
    ) {
        try {
            return operation.get();
        } catch (UiDslRuntimeException exception) {
            this.reportRuntime(exception);
        } catch (RuntimeException exception) {
            this.reportRuntime(new UiDslRuntimeException(
                location,
                exception.getMessage() == null
                    ? exception.getClass().getSimpleName()
                    : exception.getMessage(),
                exception
            ));
        }
        return fallback;
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

    public List<UiDiagnostic> diagnostics(UiDataFile file) {
        return this.diagnostics(file.asset());
    }

    public List<UiDiagnostic> diagnostics(Asset source) {
        return this.diagnostics.getOrDefault(source, List.of());
    }

    public List<UiDiagnostic> relevantDiagnostics(UiFile file) {
        List<UiDiagnostic> result = new ArrayList<>();
        Set<Asset> visited = new HashSet<>();
        for (String suffix : UiPresentation.documentVariantSuffixes()) {
            Asset source = variantAsset(file, suffix);
            if (visited.add(source)) {
                result.addAll(this.diagnostics(source));
            }
        }
        if (visited.add(file.asset())) {
            result.addAll(this.diagnostics(file));
        }
        return List.copyOf(result);
    }

    public Map<UiFile, UiDocument> registeredDocuments() {
        return this.registeredDocuments;
    }

    public Optional<String> sourceText(Asset source) {
        String override = this.sourceOverrides.get(source);
        if (override != null) {
            return Optional.of(override);
        }
        UiDocument document = this.loadedDocuments.get(source);
        return document == null ? Optional.empty() : Optional.of(document.sourceText());
    }

    public boolean hasSourceOverride(Asset source) {
        return this.sourceOverrides.containsKey(source);
    }

    public synchronized void updateSourceOverride(Asset source, String sourceText) {
        this.updateSourceOverrides(Map.of(source, sourceText));
    }

    public synchronized void updateSourceOverrides(Map<Asset, String> updatedSources) {
        Map<Asset, String> overrides = new LinkedHashMap<>(this.sourceOverrides);
        overrides.putAll(updatedSources);
        this.sourceOverrides = Map.copyOf(overrides);
        this.recompileOverrides();
    }

    public synchronized void resetSourceOverride(Asset source) {
        if (!this.sourceOverrides.containsKey(source)) {
            return;
        }
        Map<Asset, String> overrides = new LinkedHashMap<>(this.sourceOverrides);
        overrides.remove(source);
        this.sourceOverrides = Map.copyOf(overrides);
        this.recompileOverrides();
    }

    private void recompileOverrides() {
        Minecraft minecraft = Minecraft.getInstance();
        this.apply(
            new Loader(minecraft.getResourceManager(), this.sourceOverrides).loadAll(),
            false
        );
    }

    private void apply(Snapshot snapshot, boolean rebuildPresentation) {
        this.registeredDocuments = Map.copyOf(snapshot.registeredDocuments());
        this.compiledDocuments = Map.copyOf(snapshot.compiledDocuments());
        this.compiledDocumentsByAsset = Map.copyOf(snapshot.compiledDocumentsByAsset());
        this.compiledData = Map.copyOf(snapshot.compiledData());
        this.dataContext = UiDataContext.withGlobals();
        this.loadedDocuments = Map.copyOf(snapshot.loadedDocuments());
        synchronized (this.reportedRuntimeDiagnostics) {
            this.reportedRuntimeDiagnostics.clear();
        }

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
            "Loaded {} registered UI documents, {} data files ({} total documents)",
            this.registeredDocuments.size(),
            this.compiledData.values().stream().filter(UiCompiledData::isUsable).count(),
            this.loadedDocuments.size()
        );



        if (rebuildPresentation) {
            SlotScreenManager.resizeAll(Minecraft.getInstance(), true);
        }
    }

    private static final class ReloadListener extends SimplePreparableReloadListener<Snapshot> {
        @Override
        protected Snapshot prepare(ResourceManager manager, ProfilerFiller profiler) {
            // A real resource reload is an explicit return to the pack contents.
            // Editor recompiles use recompileOverrides() and never enter this path.
            return new Loader(manager, Map.of()).loadAll();
        }

        @Override
        protected void apply(Snapshot snapshot, ResourceManager manager, ProfilerFiller profiler) {
            INSTANCE.sourceOverrides = Map.of();
            INSTANCE.apply(snapshot, true);
            ScreenEditorHooks.resourcesReloaded();
        }
    }

    private static final class Loader {
        private final ResourceManager resourceManager;
        private final Map<UiFile, UiDocument> registeredDocuments = new LinkedHashMap<>();
        private final Map<UiFile, UiCompiledDocument> compiledDocuments = new LinkedHashMap<>();
        private final Map<Asset, UiCompiledDocument> compiledDocumentsByAsset = new LinkedHashMap<>();
        private final Map<UiDataFile, UiDocument> registeredDataDocuments = new LinkedHashMap<>();
        private final Map<UiDataFile, UiCompiledData> compiledData = new LinkedHashMap<>();
        private final Map<Asset, UiDocument> loadedDocuments = new LinkedHashMap<>();
        private final Map<String, UiDocument> registeredStyles = new LinkedHashMap<>();
        private final Map<Asset, List<UiDiagnostic>> diagnostics = new LinkedHashMap<>();
        private final Map<Asset, String> sourceTexts = new HashMap<>();
        private final Set<Asset> loading = new HashSet<>();
        private final Set<Asset> failed = new HashSet<>();
        private final Map<Asset, String> sourceOverrides;

        private Loader(ResourceManager resourceManager, Map<Asset, String> sourceOverrides) {
            this.resourceManager = resourceManager;
            this.sourceOverrides = Map.copyOf(sourceOverrides);
        }

        private Snapshot loadAll() {
            for (Map.Entry<String, Asset> entry : this.loadStyleRegistry().entrySet()) {
                this.load(entry.getValue(), UiFileType.STYLE).ifPresent(document ->
                    this.registeredStyles.put(entry.getKey(), document)
                );
            }
            for (UiDataFile file : UiDataRegister.allRegisteredFiles()) {
                this.load(file.asset(), UiFileType.DATA).ifPresent(document ->
                    this.registeredDataDocuments.put(file, document)
                );
            }
            for (UiFile file : UiRegister.allRegisteredFiles()) {
                this.load(file.asset(), file.type()).ifPresent(document ->
                    this.registeredDocuments.put(file, document)
                );
                this.loadOptionalVariant(file, UiPresentation.HD_SUFFIX);
                this.loadOptionalVariant(
                    file,
                    UiPresentation.HD_SUFFIX + UiPresentation.SMALL_SUFFIX
                );
                this.loadOptionalVariant(file, UiPresentation.SMALL_SUFFIX);
            }
            UiCompiler compiler = new UiCompiler(this.loadedDocuments, this.registeredStyles);
            UiDataCompiler dataCompiler = new UiDataCompiler(
                this.loadedDocuments, this.registeredStyles
            );
            for (Map.Entry<UiDataFile, UiDocument> entry : this.registeredDataDocuments.entrySet()) {
                UiDataCompilationResult result = dataCompiler.compile(entry.getValue());
                this.compiledData.put(entry.getKey(), result.data());
                this.recordCompilerDiagnostics(result.diagnostics());
            }
            for (Map.Entry<UiFile, UiDocument> entry : this.registeredDocuments.entrySet()) {
                if (entry.getKey().type() == UiFileType.STYLE
                    || entry.getKey().type() == UiFileType.DATA) {
                    continue;
                }
                UiCompilationResult result = compiler.compile(entry.getValue());
                this.compiledDocuments.put(entry.getKey(), result.document());
                this.recordCompilerDiagnostics(result.diagnostics());
            }
            for (Map.Entry<Asset, UiDocument> entry : this.loadedDocuments.entrySet()) {
                if (entry.getValue().type() == UiFileType.STYLE
                    || entry.getValue().type() == UiFileType.DATA) {
                    continue;
                }
                UiCompilationResult result = compiler.compile(entry.getValue());
                this.compiledDocumentsByAsset.put(entry.getKey(), result.document());
                this.recordCompilerDiagnostics(result.diagnostics());
            }
            return new Snapshot(
                this.registeredDocuments,
                this.compiledDocuments,
                this.compiledDocumentsByAsset,
                this.compiledData,
                this.loadedDocuments,
                this.diagnostics
            );
        }

        private void recordCompilerDiagnostics(List<UiCompileException> diagnostics) {
            for (UiCompileException exception : diagnostics) {
                this.error(
                    exception.source(),
                    exception.position().line(),
                    exception.position().column(),
                    exception.getMessage()
                );
            }
        }

        private Map<String, Asset> loadStyleRegistry() {
            Map<String, Asset> result = new LinkedHashMap<>();
            for (PackResources pack : this.resourceManager.listPacks().toList()) {
                IoSupplier<InputStream> supplier = pack.getRootResource(STYLE_REGISTRY_FILE);
                if (supplier == null) {
                    continue;
                }

                Asset diagnosticSource = registryDiagnosticSource(pack);
                try (InputStream input = supplier.get()) {
                    String sourceText = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                    this.sourceTexts.put(diagnosticSource, sourceText);
                    JsonElement parsed = JsonParser.parseReader(new StringReader(sourceText));
                    if (!parsed.isJsonObject()) {
                        this.error(diagnosticSource, 1, 1,
                            "Style registry in pack '" + pack.packId() + "' must be a JSON object");
                        continue;
                    }

                    JsonObject registry = parsed.getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : registry.entrySet()) {
                        String id = entry.getKey();
                        if (!validStyleId(id)) {
                            this.error(diagnosticSource, 1, 1,
                                "Invalid registered style ID '" + id + "' in pack '"
                                    + pack.packId() + "'");
                            continue;
                        }
                        if (!entry.getValue().isJsonPrimitive()
                            || !entry.getValue().getAsJsonPrimitive().isString()) {
                            this.error(diagnosticSource, 1, 1,
                                "Registered style '" + id + "' in pack '" + pack.packId()
                                    + "' must map to a path string");
                            continue;
                        }

                        String path = entry.getValue().getAsString();
                        if (!validInternalPath(path)) {
                            this.error(diagnosticSource, 1, 1,
                                "Invalid style path '" + path + "' for registry ID '" + id + "'");
                            continue;
                        }
                        this.resolveRegisteredStyle(pack, diagnosticSource, id, path).ifPresent(asset ->
                            result.put(id, asset)
                        );
                    }
                } catch (Exception exception) {
                    this.error(diagnosticSource, 1, 1,
                        "Could not read " + STYLE_REGISTRY_FILE + " from pack '" + pack.packId()
                            + "': " + exception.getMessage());
                }
            }
            return result;
        }

        private Optional<Asset> resolveRegisteredStyle(
            PackResources pack,
            Asset diagnosticSource,
            String id,
            String path
        ) {
            String resourcePath = UiFileType.STYLE.resourcePath(path);
            List<Asset> matches = new ArrayList<>();
            for (String namespace : pack.getNamespaces(PackType.CLIENT_RESOURCES)) {
                Identifier location;
                try {
                    location = Identifier.fromNamespaceAndPath(namespace, resourcePath);
                } catch (RuntimeException exception) {
                    continue;
                }
                if (pack.getResource(PackType.CLIENT_RESOURCES, location) != null) {
                    matches.add(new Asset(location));
                }
            }
            if (matches.isEmpty()) {
                this.error(diagnosticSource, 1, 1,
                    "Registered style '" + id + "' points to missing path '" + path
                        + "' in pack '" + pack.packId() + "'");
                return Optional.empty();
            }
            if (matches.size() > 1) {
                this.error(diagnosticSource, 1, 1,
                    "Registered style '" + id + "' is ambiguous in pack '" + pack.packId()
                        + "': path '" + path + "' exists in multiple namespaces");
                return Optional.empty();
            }
            return Optional.of(matches.getFirst());
        }

        private static Asset registryDiagnosticSource(PackResources pack) {
            String packId = pack.packId().toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9._/-]", "_");
            return Asset.MOD("style_registries/" + packId + ".json");
        }

        private static boolean validStyleId(String id) {
            if (id == null || id.isEmpty()
                || !(Character.isLetter(id.charAt(0)) || id.charAt(0) == '_')) {
                return false;
            }
            for (int index = 1; index < id.length(); index++) {
                char value = id.charAt(index);
                if (!(Character.isLetterOrDigit(value) || value == '_' || value == '-')) {
                    return false;
                }
            }
            return true;
        }

        private void loadOptionalVariant(UiFile file, String suffix) {
            Asset source = variantAsset(file, suffix);
            if (source.find(this.resourceManager).isPresent()) {
                this.load(source, file.type());
            }
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
                String overriddenSource = this.sourceOverrides.get(source);
                Optional<Resource> resource = source.find(this.resourceManager);
                if (overriddenSource == null && resource.isEmpty()) {
                    this.error(source, 0, 0, "Registered UI asset was not found");
                    this.failed.add(source);
                    return Optional.empty();
                }

                String sourceText = overriddenSource;
                if (sourceText == null) {
                    try (InputStream input = resource.orElseThrow().open()) {
                        sourceText = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
                this.sourceTexts.put(source, sourceText);
                UiDocument document;
                try {
                    UiParser.Result parsed = UiParser.parse(
                        new StringReader(sourceText), source, type
                    );
                    document = parsed.document();
                    for (UiParseException diagnostic : parsed.diagnostics()) {
                        this.error(
                            diagnostic.source(),
                            diagnostic.line(),
                            diagnostic.column(),
                            diagnostic.detail()
                        );
                    }
                } catch (RuntimeException exception) {
                    this.error(
                        source,
                        1,
                        1,
                        "Internal DSL parser error: "
                            + (exception.getMessage() == null
                                ? exception.getClass().getSimpleName()
                                : exception.getMessage())
                    );
                    document = new UiDocument(source, type, List.of(), sourceText);
                }

                for (UiDocument.ImportDirective imported : document.imports()) {
                    Optional<UiFileType> importedType = UiFileType.fromImportPath(imported.target().path());
                    if (importedType.isEmpty()) {
                        continue;
                    }
                    if (importedType.get() == UiFileType.STYLE) {
                        this.error(
                            source,
                            imported.position().line(),
                            imported.position().column(),
                            "Style documents cannot be imported; register the style in "
                                + STYLE_REGISTRY_FILE + " and reference it with '::style_id.member'"
                        );
                        continue;
                    }
                    Optional<ImportedSource> resolved = this.resolveImport(source, imported);
                    if (resolved.isEmpty()) {
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
                    }
                }

                this.loadedDocuments.put(source, document);
                return Optional.of(document);
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
            UiDocument document = this.loadedDocuments.get(source);
            String code = document == null
                ? lineText(this.sourceTexts.get(source), line)
                : document.lineText(line);
            UiDiagnostic diagnostic = new UiDiagnostic(
                UiDiagnostic.Severity.ERROR,
                source,
                line,
                column,
                message,
                code,
                UiDiagnostic.suggest(message)
            );
            List<UiDiagnostic> values = this.diagnostics.computeIfAbsent(
                source, ignored -> new ArrayList<>()
            );
            if (!values.contains(diagnostic)) {
                values.add(diagnostic);
            }
        }

        private static String lineText(String sourceText, int line) {
            if (sourceText == null || line < 1) {
                return "";
            }
            String[] lines = sourceText.split("\\R", -1);
            return line <= lines.length ? lines[line - 1] : "";
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
        Map<Asset, UiCompiledDocument> compiledDocumentsByAsset,
        Map<UiDataFile, UiCompiledData> compiledData,
        Map<Asset, UiDocument> loadedDocuments,
        Map<Asset, List<UiDiagnostic>> diagnostics
    ) {
        private Snapshot {
            registeredDocuments = new HashMap<>(registeredDocuments);
            compiledDocuments = new HashMap<>(compiledDocuments);
            compiledDocumentsByAsset = new HashMap<>(compiledDocumentsByAsset);
            compiledData = new HashMap<>(compiledData);
            loadedDocuments = new HashMap<>(loadedDocuments);
            Map<Asset, List<UiDiagnostic>> diagnosticCopy = new HashMap<>();
            diagnostics.forEach((asset, values) -> diagnosticCopy.put(asset, new ArrayList<>(values)));
            diagnostics = diagnosticCopy;
        }
    }

    private static Asset variantAsset(UiFile file, String suffix) {
        return Asset.NamespaceAndPatch(file.modId(), file.type().resourcePath(file.path() + suffix));
    }
}
