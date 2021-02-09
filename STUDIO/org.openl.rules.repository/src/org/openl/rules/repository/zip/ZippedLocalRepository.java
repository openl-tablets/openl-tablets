package org.openl.rules.repository.zip;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

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
        Path root = null;
        if (uri != null) {
            File rootFile = new File(uri);
            if (!rootFile.exists()) {
                rootFile.mkdirs();
            }
            root = rootFile.toPath();
            if (!Files.isDirectory(root)) {
                throw new IllegalStateException(String.format("Failed to initialize the root directory: [%s].", root));
            }
        }
        final Map<String, Path> localStorage = new HashMap<>();
        if (archives != null && archives.length > 0) {
            for (String archive : archives) {
                if (StringUtils.isBlank(archive)) {
                    continue;
                }
                archive = archive.trim().replace('\\', '/');
                Path pathToArchive = Paths.get(archive);
                boolean exists = Files.exists(pathToArchive);
                boolean isArchive = exists && zipArchiveFilter(pathToArchive);
                if (!pathToArchive.isAbsolute() && (!exists || !isArchive) && root != null) {
                    // if path is not absolute, try to resolve it from root folder
                    pathToArchive = root.resolve(archive);
                    exists |= Files.exists(pathToArchive);
                }
                if (!exists) {
                    throw new IllegalStateException(String.format("The path [%s] does not exist.", archive));
                }
                if (!zipArchiveFilter(pathToArchive)) {
                    throw new IllegalStateException(String.format("[%s] is not archive.", archive));
                }
                String archiveName = FileUtils.getBaseName(pathToArchive.getFileName().toString());
                if (localStorage.containsKey(archiveName)) {
                    throw new IllegalStateException(String.format("An archive name [%s] is duplicated!", archiveName));
                }
                localStorage.put(archiveName, pathToArchive);
            }
            if (root == null) {
                root = findCommonParentPath(localStorage.values());
            }
        }
        if (root == null) {
            throw new IllegalStateException("Failed to initialize root directory!");
        }
        if (localStorage.isEmpty()) {
            try {
                Files.walkFileTree(root, EnumSet.noneOf(FileVisitOption.class), 1, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path p, BasicFileAttributes attrs) {
                        if (attrs.isDirectory() || zipArchiveFilter(p)) {
                            String archName = p.getFileName().toString();
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
        setRoot(root);
        setStorage(localStorage);
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setArchives(String... archives) {
        this.archives = archives;
    }

}
