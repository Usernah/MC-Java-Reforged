package net.jr.api.client.video;

import net.jr.Java_reforged;
import net.jr.api.client.resource.Asset;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

public final class VideoHolder {
    static final Object EXTRACTION_LOCK = new Object();
    static final String CACHE_FOLDER = "video_cache";
    static final String HASH_SUFFIX = ".sha256";

    private final String modId;
    private final String resourcePath;
    private final Asset location;
    private final Asset loader;

    VideoHolder(String modId, String resourcePath) {
        this.modId = Objects.requireNonNull(modId, "modId");
        this.resourcePath = normalizePath(resourcePath);
        this.location = Asset.NamespaceAndPatch(this.modId, this.resourcePath);
        this.loader = Asset.NamespaceAndPatch(this.modId, this.resourcePath);
    }

    public String modId() {
        return modId;
    }

    public String path() {
        return resourcePath;
    }

    public Asset location() {
        return location;
    }

    public Asset loader() {
        return loader;
    }

    public String debugId() {
        return location.toString();
    }

    public File resolveCachedFile() {
        return resolveCachedFile(false);
    }

    public File resolveCachedFile(boolean forceExtract) {
        try {
            Optional<Resource> resourceOptional = location.find(Minecraft.getInstance().getResourceManager());
            if (resourceOptional.isEmpty()) {
                return null;
            }

            File cacheRoot = ensureHiddenCacheRoot();
            File namespaceDir = new File(cacheRoot, location.namespace());
            File targetFile = new File(namespaceDir, location.path().replace('/', File.separatorChar));
            File hashFile = new File(targetFile.getAbsolutePath() + HASH_SUFFIX);

            synchronized (EXTRACTION_LOCK) {
                createParentDirectories(targetFile);

                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                File tempExtract = new File(targetFile.getAbsolutePath() + ".tmp");

                try (InputStream in = new BufferedInputStream(resourceOptional.get().open());
                     DigestInputStream digestIn = new DigestInputStream(in, digest);
                     BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(
                         tempExtract.toPath(),
                         StandardOpenOption.CREATE,
                         StandardOpenOption.TRUNCATE_EXISTING,
                         StandardOpenOption.WRITE
                     ))) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = digestIn.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }

                String currentHash = HexFormat.of().formatHex(digest.digest());
                String cachedHash = readHash(hashFile);
                boolean cacheMatches = targetFile.exists() && currentHash.equals(cachedHash);

                if (forceExtract || !cacheMatches) {
                    Files.move(tempExtract.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    Files.writeString(
                        hashFile.toPath(),
                        currentHash,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE
                    );
                } else {
                    Files.deleteIfExists(tempExtract.toPath());
                }
            }

            return targetFile;
        } catch (Exception exception) {
            Java_reforged.LOGGER.error("Could not extract video {}", debugId(), exception);
            return null;
        }
    }

    private static File ensureHiddenCacheRoot() throws Exception {
        File cacheRoot = new File(Minecraft.getInstance().gameDirectory, CACHE_FOLDER);
        if (!cacheRoot.exists()) {
            Files.createDirectories(cacheRoot.toPath());
            try {
                Path path = cacheRoot.toPath();
                Files.setAttribute(path, "dos:hidden", true);
                Files.setAttribute(path, "dos:system", true);
            } catch (Exception ignored) {
            }
        }
        return cacheRoot;
    }

    private static void createParentDirectories(File targetFile) throws Exception {
        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists()) {
            Files.createDirectories(parent.toPath());
        }
    }

    private static String readHash(File hashFile) {
        try {
            if (!hashFile.exists()) {
                return "";
            }
            return Files.readString(hashFile.toPath()).trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String normalizePath(String resourcePath) {
        Objects.requireNonNull(resourcePath, "resourcePath");
        String normalized = resourcePath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof VideoHolder that)) return false;
        return location.equals(that.location);
    }

    @Override
    public int hashCode() {
        return location.hashCode();
    }

    @Override
    public String toString() {
        return "VideoHolder[" + location + "]";
    }
}
