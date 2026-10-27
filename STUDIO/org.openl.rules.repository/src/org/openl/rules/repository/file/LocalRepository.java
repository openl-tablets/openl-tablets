package org.openl.rules.repository.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FeaturesBuilder;

/**
 * Local File System Repository
 */
public class LocalRepository extends FileSystemRepository {

    /**
     * Compute the hashCode for all files inside folder. The ignored files are left out, so a change of them leaves
     * the version as it is.
     *
     * @param file folder
     * @return hashCode for given folder
     */
    @Override
    protected String getVersion(Path file) {
        if (Files.isDirectory(file)) {
            var hash = 1;
            for (var path : listAllFiles(file)) {
                var f = path.toFile();
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

    private List<Path> listAllFiles(Path dir) {
        var files = new ArrayList<Path>();
        try {
            walkListed(dir, files::add);
        } catch (IOException unused) {
            return List.of();
        }
        return files;
    }

    @Override
    public Features supports() {
        return new FeaturesBuilder(this).setVersions(false).setLocal(true).setFolders(true).setComments(false).build();
    }
}
