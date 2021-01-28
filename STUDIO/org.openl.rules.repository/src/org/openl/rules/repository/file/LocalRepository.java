package org.openl.rules.repository.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

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
    protected String getVersion(File file) {
        if (file.isDirectory()) {
            File[] files = listAllFiles(file);
            int hash = 1;
            for (File f : files) {
                hash = 31 * hash + Objects.hash(f.getName(), f.lastModified(), f.length());
            }
            return String.valueOf(hash);
        }
        return null;
    }

    private File[] listAllFiles(File dir) {
        try (Stream<Path> stream = Files.walk(dir.toPath())) {
            return stream.filter(Files::isRegularFile).map(Path::toFile).toArray(File[]::new);
        } catch (IOException unused) {
            return EMPTY_FILES;
        }
    }

    @Override
    public Features supports() {
        return new FeaturesBuilder(this).setVersions(false).setLocal(true).build();
    }
}
