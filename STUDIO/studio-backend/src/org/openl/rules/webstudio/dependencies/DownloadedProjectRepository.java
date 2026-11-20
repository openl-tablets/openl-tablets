package org.openl.rules.webstudio.dependencies;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.util.List;

import org.openl.rules.project.impl.local.InPlaceRepository;
import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.repository.IgnoredPaths;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;

/**
 * The folder an OpenL project downloaded from the Maven repository is unpacked into, as a repository of the session
 * that holds this one project, read-only.
 *
 * <p>The project is opened where it is unpacked, as a project edited in place is: it is compiled, run and traced as a
 * project of the workspace. Nothing writes its files: every write is refused, whoever asks.
 *
 * @author Yury Molchan
 */
public final class DownloadedProjectRepository extends InPlaceRepository {

    /** The type and the name the repository is told by, as the Maven repository is. */
    public static final String TYPE = "maven";
    private static final String NAME = "Maven";

    /**
     * @param id       the id of the repository: the id of the project it holds
     * @param root     the folder the project folder is in
     * @param registry the metainfo registry of the user, which records nothing about the project
     */
    public DownloadedProjectRepository(String id, Path root, MetainfoRegistry registry) {
        super(root, root, registry, IgnoredPaths.of(null));
        setId(id);
        setName(NAME);
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        throw refused(data.getName());
    }

    @Override
    public List<FileData> save(List<FileItem> fileItems) throws IOException {
        throw refused(getId());
    }

    @Override
    public FileData save(FileData folderData, Iterable<FileItem> files, ChangesetType changesetType)
            throws IOException {
        throw refused(folderData.getName());
    }

    @Override
    public boolean delete(FileData data) throws IOException {
        throw refused(data.getName());
    }

    @Override
    public boolean delete(List<FileData> data) throws IOException {
        throw refused(getId());
    }

    @Override
    public boolean deleteHistory(FileData data) throws IOException {
        throw refused(data.getName());
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
        throw refused(destData.getName());
    }

    private static AccessDeniedException refused(String path) {
        return new AccessDeniedException(path, null, "A project downloaded from the Maven repository is read-only.");
    }
}
