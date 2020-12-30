package org.openl.rules.repository.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;

/**
 * Local File System Repository
 */
public class LocalRepository extends FileSystemRepository {

    private static final File[] EMPTY_FILES = new File[0];

    private boolean supportDeployments = false;

    public void setSupportDeployments(boolean supportDeployments) {
        this.supportDeployments = supportDeployments;
    }

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
    public List<FileData> listFolders(String path) {
        if (supportDeployments) {
            return super.listFolders(path);
        }
        //!!! NOTE: Backward compatibility to support local repo without deployment folder. Root folder is a deployment !!!
        List<FileData> files = new LinkedList<>();
        boolean isRoot = path.isEmpty();
        final File root = getRoot();
        final String name = root.getName();
        if (isRoot) {
            FileData data = new FileData();
            data.setName(name);
            data.setModifiedAt(new Date(root.lastModified()));
            data.setVersion(getVersion(root));
            files.add(data);
            return files;
        }
        if (path.charAt(0) == '/') {
            path = path.substring(1);
        }
        if (path.startsWith(name)) {
            path = path.substring(name.length());
            File directory = new File(root, path);
            File[] found = directory.listFiles(File::isDirectory);
            if (found != null) {
                for (File file : found) {
                    FileData data = new FileData();
                    data.setName("/" + file.getName());
                    data.setModifiedAt(new Date(file.lastModified()));
                    data.setVersion(getVersion(file));
                    files.add(data);
                }
            }
            return files;
        }
        //should never happen
        throw new IllegalStateException(String.format("Illegal state for LocalRepository. Unable to get list of folders from '%s' path.", path));
    }

    @Override
    public Features supports() {
        return new FeaturesBuilder(this).setVersions(false).setLocal(true).build();
    }
}
