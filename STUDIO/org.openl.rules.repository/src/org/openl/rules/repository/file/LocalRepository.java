package org.openl.rules.repository.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FeaturesBuilder;

/**
 * Local File System Repository
 */
public class LocalRepository extends FileSystemRepository {

    private static final File[] EMPTY_FILES = new File[0];

    /**
     * Compute the hashCode for all files inside folder
     *
     * @param file folder
     * @return hashCode for given folder
     */
    @Override
    protected String getVersion(Path file) {
        if (Files.isDirectory(file)) {
            var files = listAllFiles(file.toFile());
            var hash = 1;
            for (File f : files) {
                hash = 31 * hash + Objects.hash(f.getName(), f.lastModified(), f.length());
            }
            return String.valueOf(hash);
        }
        return null;
    }

    @Override
    protected String getVersion(String path) throws IOException {
        var data = check(path);
        return data != null ? data.getVersion() : null;
    }

    private File[] listAllFiles(File dir) {
        try (var stream = Files.walk(dir.toPath())) {
            return stream.filter(Files::isRegularFile).map(Path::toFile).toArray(File[]::new);
        } catch (IOException unused) {
            return EMPTY_FILES;
        }
    }

    @Override
    public Features supports() {
        return new FeaturesBuilder(this).setVersions(false).setLocal(true).setFolders(true).build();
    }
}
