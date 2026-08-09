package net.jr.client.input;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;
import net.jr.Java_reforged;

public final class InputJsonFiles {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private InputJsonFiles() {
    }

    @Nullable
    public static JsonObject read(Path path) {
        if (path == null || !Files.exists(path)) {
            return null;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException | IllegalStateException | JsonParseException exception) {
            Java_reforged.LOGGER.warn("No se pudo leer {}.", path.getFileName(), exception);
            return null;
        }
    }

    public static void write(Path path, JsonObject root) {
        if (path == null) {
            return;
        }

        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException exception) {
            Java_reforged.LOGGER.warn("No se pudo guardar {}.", path.getFileName(), exception);
        }
    }
}

