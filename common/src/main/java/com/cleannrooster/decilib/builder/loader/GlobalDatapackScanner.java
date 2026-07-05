package com.cleannrooster.decilib.builder.loader;

import com.cleannrooster.decilib.Decilib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scans known global datapack loader directories (Paxi, OpenLoader, Global Packs,
 * Global Datapacks) for mob-profile JSON files before the Minecraft registry freezes.
 *
 * <p>All three major loader mods store their packs in plain filesystem directories under
 * the game directory, making them accessible via {@link Files#walk} at startup — the same
 * technique used to scan mod JARs in {@code ModEntities.registerEntityTypes()}.
 *
 * <p>Usage:
 * <pre>{@code
 *   GlobalDatapackScanner scanner = new GlobalDatapackScanner(gameDir);
 *   List<Path> extraRoots = scanner.collectPackRoots();
 *   // pass extraRoots into ModEntities.registerEntityTypes alongside modJarRoots
 *   scanner.close(); // releases any mounted ZIP FileSystems
 * }</pre>
 *
 * <p>Supported loaders and their scanned directories (relative to game dir):
 * <ul>
 *   <li>Paxi              — {@code config/paxi/datapacks/}</li>
 *   <li>OpenLoader        — {@code config/openloader/data/}</li>
 *   <li>Global Packs      — {@code config/global_packs/required_data/},
 *                           {@code config/global_packs/optional_data/}</li>
 *   <li>Global Datapacks  — {@code datapacks/}</li>
 * </ul>
 *
 * <p>Known limitations:
 * <ul>
 *   <li>World-specific datapacks ({@code saves/<world>/datapacks/}) cannot be scanned —
 *       the world folder is not known at registry-freeze time.</li>
 *   <li>Datapacks added or changed via {@code /reload} cannot register new entity types;
 *       the registry is already frozen by then.</li>
 *   <li>OpenLoader's {@code additionalFolders} (custom paths in {@code advanced_options.json})
 *       are not parsed — only the default path is scanned.</li>
 * </ul>
 */
public final class GlobalDatapackScanner implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalDatapackScanner.class);

    /** Sub-paths relative to the game directory that each supported loader mod uses. */
    private static final List<String> LOADER_DIRS = List.of(
            "config/paxi/datapacks",
            "config/openloader/data",
            "config/global_packs/required_data",
            "config/global_packs/optional_data",
            "datapacks"
    );

    private final Path gameDir;
    /** Keeps track of ZipFileSystems we opened so they can be closed afterwards. */
    private final List<FileSystem> openedFileSystems = new ArrayList<>();

    public GlobalDatapackScanner(Path gameDir) {
        this.gameDir = gameDir;
    }

    /**
     * Returns a list of pack-root {@link Path}s found inside every supported loader directory.
     * Each element is either:
     * <ul>
     *   <li>A plain filesystem directory (folder-based pack root), or</li>
     *   <li>The root of a mounted ZIP/JAR {@link FileSystem}.</li>
     * </ul>
     *
     * <p>The caller should pass these alongside {@code modJarRoots} into
     * {@code ModEntities.registerEntityTypes()}, then call {@link #close()} once registration
     * is complete to release any mounted ZIP FileSystems.
     */
    public List<Path> collectPackRoots() {
        List<Path> roots = new ArrayList<>();

        for (String loaderDir : LOADER_DIRS) {
            Path loaderPath = gameDir.resolve(loaderDir);
            if (!Files.isDirectory(loaderPath)) continue;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(loaderPath)) {
                for (Path entry : stream) {
                    if (Files.isDirectory(entry)) {
                        // Folder-based pack — must contain pack.mcmeta to be valid.
                        if (Files.exists(entry.resolve("pack.mcmeta"))) {
                            roots.add(entry);
                            LOGGER.debug("[deci-lib] GlobalDatapackScanner: found folder pack at {}", entry);
                        }
                    } else {
                        String name = entry.getFileName().toString().toLowerCase();
                        if (name.endsWith(".zip") || name.endsWith(".jar")) {
                            Path root = mountZip(entry);
                            if (root != null) {
                                roots.add(root);
                                LOGGER.debug("[deci-lib] GlobalDatapackScanner: mounted ZIP pack at {}", entry);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.warn("[deci-lib] GlobalDatapackScanner: failed to scan loader dir {}: {}",
                        loaderPath, e.getMessage());
            }
        }

        if (!roots.isEmpty()) {
            LOGGER.info("[deci-lib] GlobalDatapackScanner: found {} pack root(s) across global datapack loaders",
                    roots.size());
        }

        return roots;
    }

    /**
     * Mounts a ZIP or JAR file as a NIO {@link FileSystem} and returns its root {@link Path}.
     * The opened filesystem is tracked in {@link #openedFileSystems} for cleanup in
     * {@link #close()}.
     *
     * @return the root path of the mounted filesystem, or {@code null} on failure
     */
    private Path mountZip(Path zipFile) {
        try {
            URI uri = URI.create("jar:" + zipFile.toUri());
            Map<String, String> env = new HashMap<>();
            env.put("create", "false");
            FileSystem fs = FileSystems.newFileSystem(uri, env);
            openedFileSystems.add(fs);
            return fs.getPath("/");
        } catch (IOException e) {
            LOGGER.warn("[deci-lib] GlobalDatapackScanner: could not mount ZIP {}: {}",
                    zipFile, e.getMessage());
            return null;
        }
    }

    /**
     * Closes all ZIP {@link FileSystem}s opened during {@link #collectPackRoots()}.
     * Call this immediately after {@code ModEntities.registerEntityTypes()} returns.
     */
    @Override
    public void close() {
        for (FileSystem fs : openedFileSystems) {
            try {
                fs.close();
            } catch (IOException e) {
                LOGGER.debug("[deci-lib] GlobalDatapackScanner: error closing ZIP filesystem: {}",
                        e.getMessage());
            }
        }
        openedFileSystems.clear();
    }
}
