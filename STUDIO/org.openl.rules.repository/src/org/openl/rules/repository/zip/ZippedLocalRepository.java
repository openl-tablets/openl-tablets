package org.openl.rules.repository.zip;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.openl.util.FileUtils;
import org.openl.util.StringUtils;
import org.openl.util.ZipUtils;

/**
 * Read only repository of the zip archives of the file system, which reads an archive without unzipping it to a
 * temporary folder.
 *
 * <p>The repository holds the archives it is given. A relative path of an archive is resolved against the root
 * folder when it does not name an archive as it is. With no archives given, it holds the folders and the archives
 * directly in the root folder.
 *
 * <p>NOTE: This repository type doesn't support write actions!
 *
 * @author Vladyslav Pikus
 */
public class ZippedLocalRepository extends AbstractArchiveRepository {

    /**
     * @param uri      the root folder, created when absent, or {@code null} for none
     * @param archives the paths of the archives, blank ones are skipped
     * @throws IllegalStateException when the root is not a folder, or an archive is absent, is not an archive or
     *                               repeats the name of another one
     */
    ZippedLocalRepository(@Nullable String uri, String... archives) {
        super(findArchives(resolveRoot(uri), archives));
    }

    private static Map<String, Path> findArchives(@Nullable Path root, String... archives) {
        var storage = new HashMap<String, Path>();
        for (var archive : archives) {
            if (StringUtils.isNotBlank(archive)) {
                addArchive(storage, archive.trim().replace('\\', '/'), root);
            }
        }
        if (storage.isEmpty() && root != null) {
            addRootContent(storage, root);
        }
        return storage;
    }

    private static @Nullable Path resolveRoot(@Nullable String uri) {
        if (uri == null) {
            return null;
        }
        var rootFile = new File(uri);
        if (!rootFile.exists()) {
            rootFile.mkdirs();
        }
        var root = rootFile.toPath();
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("Failed to initialize the root directory: [%s].".formatted(root));
        }
        return root;
    }

    /**
     * Adds the archive to the storage under its base name. A relative path that does not name an archive is
     * resolved against the root directory.
     */
    private static void addArchive(Map<String, Path> storage, String archive, @Nullable Path root) {
        var pathToArchive = Path.of(archive);
        var exists = Files.exists(pathToArchive);
        if (!pathToArchive.isAbsolute() && !ZipUtils.isArchive(pathToArchive) && root != null) {
            // if path is not absolute, try to resolve it from root folder
            pathToArchive = root.resolve(archive);
            exists |= Files.exists(pathToArchive);
        }
        if (!exists) {
            throw new IllegalStateException("The path [%s] does not exist.".formatted(archive));
        }
        if (!ZipUtils.isArchive(pathToArchive)) {
            throw new IllegalStateException("[%s] is not archive.".formatted(archive));
        }
        var archiveName = FileUtils.getBaseName(pathToArchive.getFileName().toString());
        if (storage.putIfAbsent(archiveName, pathToArchive) != null) {
            throw new IllegalStateException("An archive name [%s] is duplicated!".formatted(archiveName));
        }
    }

    /**
     * Adds the folders and the archives directly in the root directory to the storage.
     */
    private static void addRootContent(Map<String, Path> storage, Path root) {
        try (var content = Files.list(root)) {
            content.forEach(p -> {
                if (Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)) {
                    storage.put(p.getFileName().toString(), p);
                } else if (ZipUtils.isArchive(p)) {
                    storage.put(FileUtils.getBaseName(p.getFileName().toString()), p);
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize a repository", e);
        }
    }
}
