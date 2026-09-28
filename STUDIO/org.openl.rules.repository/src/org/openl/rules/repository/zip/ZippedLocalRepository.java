package org.openl.rules.repository.zip;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.openl.util.FileTool;
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;

/**
 * Read only implementation of Local Repository to support deploying of zip archives as it is from file system without
 * unzipping to temporary directories.</br>
 *
 * <p>
 * NOTE: This repository type doesn't support write actions!
 * </p>
 *
 * @author Vladyslav Pikus
 */
public class ZippedLocalRepository extends AbstractArchiveRepository {

    private String uri;
    private String[] archives;

    public void initialize() {
        Path root = resolveRoot();
        final var localStorage = new HashMap<String, Path>();
        if (archives != null && archives.length > 0) {
            for (String archive : archives) {
                if (StringUtils.isBlank(archive)) {
                    continue;
                }
                addArchive(localStorage, archive.trim().replace('\\', '/'), root);
            }
            if (root == null) {
                root = findCommonParentPath(localStorage.values());
            }
        }
        if (localStorage.isEmpty() && root != null) {
            addRootContent(localStorage, root);
        }
        if (root == null) {
            try {
                root = FileTool.createTempDirectory("temp");
                root.toFile().deleteOnExit();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to initialize temp root directory!", e);
            }
        }
        setRoot(root);
        setStorage(localStorage);
    }

    /**
     * The root directory the URI names, created when absent, or {@code null} when no URI is set.
     */
    private Path resolveRoot() {
        Path root = null;
        if (uri != null) {
            var rootFile = new File(uri);
            if (!rootFile.exists()) {
                rootFile.mkdirs();
            }
            root = rootFile.toPath();
            if (!Files.isDirectory(root)) {
                throw new IllegalStateException("Failed to initialize the root directory: [%s].".formatted(root));
            }
        }
        return root;
    }

    /**
     * Adds the archive to the storage under its base name. A relative path that does not name an archive is
     * resolved against the root directory.
     */
    private static void addArchive(Map<String, Path> localStorage, String archive, Path root) {
        Path pathToArchive = Path.of(archive);
        var exists = Files.exists(pathToArchive);
        var isArchive = exists && zipArchiveFilter(pathToArchive);
        if (!pathToArchive.isAbsolute() && (!exists || !isArchive) && root != null) {
            // if path is not absolute, try to resolve it from root folder
            pathToArchive = root.resolve(archive);
            exists |= Files.exists(pathToArchive);
        }
        if (!exists) {
            throw new IllegalStateException("The path [%s] does not exist.".formatted(archive));
        }
        if (!zipArchiveFilter(pathToArchive)) {
            throw new IllegalStateException("[%s] is not archive.".formatted(archive));
        }
        String archiveName = FileUtils.getBaseName(pathToArchive.getFileName().toString());
        if (localStorage.containsKey(archiveName)) {
            throw new IllegalStateException("An archive name [%s] is duplicated!".formatted(archiveName));
        }
        localStorage.put(archiveName, pathToArchive);
    }

    /**
     * Adds the folders and the archives directly in the root directory to the storage.
     */
    private static void addRootContent(Map<String, Path> localStorage, Path root) {
        try {
            Files.walkFileTree(root, EnumSet.noneOf(FileVisitOption.class), 1, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path p, BasicFileAttributes attrs) {
                    if (attrs.isDirectory() || zipArchiveFilter(p)) {
                        var archName = p.getFileName().toString();
                        if (!attrs.isDirectory()) {
                            archName = FileUtils.getBaseName(archName);
                        }
                        localStorage.put(archName, p);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize a repository", e);
        }
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setArchives(String... archives) {
        this.archives = archives;
    }

}
