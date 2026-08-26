package net.jr.client.input.sdl;

import dev.isxander.sdl3java.jna.SdlNativeLibraryLoader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import net.jr.Java_reforged;
import net.minecraft.client.Minecraft;

final class SdlNativeLibrary {
    private static final String RESOURCE = "/natives/windows-x86_64/SDL3.dll";
    private static final String FILE_NAME = "SDL3.dll";
    private static boolean prepared;

    private SdlNativeLibrary() {
    }

    static synchronized void prepare() throws IOException {
        if (prepared) {
            return;
        }
        if (!isWindows()) {
            prepared = true;
            return;
        }

        try (InputStream input = SdlGamepad.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                Java_reforged.LOGGER.warn(
                    "No se encontro {} en el classpath. libsdl4j intentara cargar SDL3 desde el sistema.",
                    RESOURCE
                );
                prepared = true;
                return;
            }

            Path nativePath = resolvePath();
            Files.createDirectories(nativePath.getParent());
            extractIfMissing(input, nativePath);
            try {
                SdlNativeLibraryLoader.loadLibSDL3FromFilePathNow(nativePath.toAbsolutePath().toString());
                Java_reforged.LOGGER.debug("SDL3 nativo cargado desde {}.", nativePath.toAbsolutePath());
            } catch (IllegalStateException alreadyLoaded) {
                Java_reforged.LOGGER.debug(
                    "SDL3 ya estaba cargado antes de JavaReforged: {}",
                    safeString(alreadyLoaded.getMessage(), "sin detalle")
                );
            }
            prepared = true;
        }
    }

    private static Path resolvePath() {
        Minecraft minecraft = Minecraft.getInstance();
        Path baseDirectory = minecraft == null
            ? Path.of(System.getProperty("java.io.tmpdir"))
            : minecraft.gameDirectory.toPath();
        return baseDirectory.resolve(".javareforged").resolve("natives").resolve(FILE_NAME);
    }

    private static void extractIfMissing(InputStream input, Path nativePath) throws IOException {
        if (Files.isRegularFile(nativePath)) {
            return;
        }

        Path temporaryPath = Files.createTempFile(nativePath.getParent(), "SDL3-", ".tmp");
        try {
            Files.copy(input, temporaryPath, StandardCopyOption.REPLACE_EXISTING);
            try {
                try {
                    Files.move(temporaryPath, nativePath, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException exception) {
                    Files.move(temporaryPath, nativePath);
                }
            } catch (FileAlreadyExistsException ignored) {
            }
        } finally {
            Files.deleteIfExists(temporaryPath);
        }
    }

    static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static String safeString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
