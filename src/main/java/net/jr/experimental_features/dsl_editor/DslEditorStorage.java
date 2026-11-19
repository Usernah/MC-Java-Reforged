package net.jr.experimental_features.dsl_editor;

import net.jr.api.client.resource.Asset;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

final class DslEditorStorage {
    private DslEditorStorage() {
    }

    static Path save(Asset asset, String source) throws IOException {
        Path target = locate(asset);
        Files.writeString(target, source, StandardCharsets.UTF_8);
        return target;
    }

    static Path locate(Asset asset) throws IOException {
        Path packs = Minecraft.getInstance()
            .gameDirectory
            .toPath()
            .resolve("resourcepacks")
            .toAbsolutePath()
            .normalize();
        if (!Files.isDirectory(packs)) {
            throw new IOException("No existe el directorio de resource packs: " + packs);
        }
        Path relative = Path.of("assets", asset.namespace()).resolve(asset.path());
        try (var roots = Files.list(packs)) {
            List<Path> matches = roots
                .filter(Files::isDirectory)
                .map(root -> root.resolve(relative).normalize())
                .filter(path -> path.startsWith(packs))
                .filter(Files::isRegularFile)
                .sorted(Comparator.comparing(path ->
                    path.toString().contains("Base textures for JR") ? 0 : 1
                ))
                .toList();
            if (matches.isEmpty()) {
                throw new IOException("No se encontró un archivo físico para " + asset);
            }
            return matches.getFirst();
        }
    }
}
