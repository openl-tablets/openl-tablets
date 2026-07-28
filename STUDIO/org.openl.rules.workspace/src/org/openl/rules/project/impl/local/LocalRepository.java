package org.openl.rules.project.impl.local;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.util.FileUtils;

/**
 * The repository of project copies in the user workspace.
 *
 * <p>Project folders contain only project files. The project metainfo lives in the per-user
 * {@link MetainfoRegistry}: every save or delete of a project file reports a local change to it, and
 * file listings enrich files with the repository revision id from the recorded baselines.
 *
 * <p>Top-level folders with a name starting with a dot are service folders of the workspace, not
 * projects, and are hidden from listings.
 *
 * @author Yury Molchan
 */
@Slf4j
public class LocalRepository extends FileSystemRepository {

    private final MetainfoRegistry registry;

    public LocalRepository(Path location, MetainfoRegistry registry) {
        setRoot(location);
        this.registry = registry;
    }

    @Override
    public List<FileData> list(String path) throws IOException {
        var list = super.list(path);
        list.removeIf(fileData -> fileData.getName().startsWith("."));
        return list;
    }

    @Override
    public List<FileData> listFolders(String path) {
        var list = super.listFolders(path);
        list.removeIf(fileData -> fileData.getName().startsWith("."));
        return list;
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        var fileData = super.save(data, stream);
        return afterFileWrite(fileData);
    }

    @Override
    public List<FileData> save(List<FileItem> fileItems) throws IOException {
        var result = super.save(fileItems);
        for (var i = 0; i < result.size(); i++) {
            result.set(i, afterFileWrite(result.get(i)));
        }
        return result;
    }

    @Override
    public FileData save(FileData folderData,
                         final Iterable<FileItem> files,
                         ChangesetType changesetType) throws IOException {
        var fileData = super.save(folderData, files, changesetType);
        registry.markDirty(projectNameOf(folderData.getName()));
        return fileData;
    }

    @Override
    public boolean delete(FileData data) throws IOException {
        var deleted = super.delete(data);
        var name = data.getName();
        if (name.contains("/")) {
            registry.markDirty(projectNameOf(name));
        } else if (deleted || check(name) == null) {
            // The project root is deleted: the project leaves the workspace together with its record
            // and its local edit history. When the folder deletion failed and the folder is still
            // there, the record is kept, otherwise the project files would be dropped at the next
            // workspace load.
            registry.remove(name);
            FileUtils.deleteQuietly(getRoot().resolve(FolderHelper.HISTORY_FOLDER).resolve(name).toFile());
        }
        return deleted;
    }

    @Override
    public Features supports() {
        return new FeaturesBuilder(this).setSupportsUniqueFileId(true).setVersions(false).setFolders(true).build();
    }

    @Override
    protected FileData getFileData(Path file) throws IOException {
        var fileData = super.getFileData(file);
        var name = fileData.getName();
        var slash = name.indexOf('/');
        if (slash > 0 && Files.isRegularFile(file)) {
            fileData.setUniqueId(registry.uniqueId(name.substring(0, slash),
                    name.substring(slash),
                    fileData.getSize(),
                    fileData.getModifiedAt().getTime()));
        }
        return fileData;
    }

    public MetainfoRegistry getMetainfoRegistry() {
        return registry;
    }

    /**
     * Reports the written file to the registry and keeps the local-changes detection reliable.
     *
     * <p>When the written file accidentally matches its baseline by size and modification time, the
     * modification time is moved forward. Otherwise the change would look like the recorded repository
     * revision after a restart.
     */
    private FileData afterFileWrite(FileData fileData) throws IOException {
        var name = fileData.getName();
        var projectName = projectNameOf(name);
        registry.markDirty(projectName);
        if (name.length() > projectName.length()) {
            var baseline = registry.baseline(projectName, name.substring(projectName.length()));
            if (baseline != null && baseline.size() == fileData.getSize()
                    && baseline.modifiedAt() == fileData.getModifiedAt().getTime()) {
                var bumped = Math.max(System.currentTimeMillis(), baseline.modifiedAt() + 1);
                Files.setLastModifiedTime(getRoot().resolve(name), FileTime.fromMillis(bumped));
                fileData.setModifiedAt(new Date(bumped));
            }
        }
        return fileData;
    }

    public ProjectState getProjectState(String pathInProject) {
        return new RegistryProjectState(projectNameOf(pathInProject));
    }

    private String projectNameOf(String path) {
        var relative = path.replace('\\', '/');
        if (new File(path).isAbsolute()) {
            relative = relativize(path);
        }
        var slash = relative.indexOf('/');
        return slash < 0 ? relative : relative.substring(0, slash);
    }

    private String relativize(String path) {
        Path base;
        Path pathAbsolute;
        try {
            base = getRoot().toAbsolutePath().toRealPath();
        } catch (IOException e) {
            log.debug(e.getMessage(), e);
            base = getRoot().toAbsolutePath().normalize();
        }
        try {
            pathAbsolute = Path.of(path).toRealPath();
        } catch (IOException e) {
            log.debug(e.getMessage(), e);
            pathAbsolute = Path.of(path).normalize();
        }
        return base.relativize(pathAbsolute).toString().replace('\\', '/');
    }

    private final class RegistryProjectState implements ProjectState {
        private final String projectName;

        private RegistryProjectState(String projectName) {
            this.projectName = projectName;
        }

        @Override
        public void notifyModified() {
            registry.markDirty(projectName);
            invokeListener();
        }

        @Override
        public boolean isModified() {
            return registry.isDirty(projectName);
        }

        @Override
        public String getProjectVersion() {
            var metainfo = registry.get(projectName);
            return metainfo == null ? null : metainfo.version();
        }

        @Override
        public String getRepositoryId() {
            var metainfo = registry.get(projectName);
            return metainfo == null ? null : metainfo.repositoryId();
        }

        @Override
        public void saveFileData(String repositoryId, FileData fileData) {
            if (fileData.getVersion() == null || fileData.getModifiedAt() == null) {
                // No need to save empty fileData
                return;
            }
            registry.relink(projectName, toMetainfo(repositoryId, fileData, Map.of()));
        }

        @Override
        public void saveSnapshot(String repositoryId,
                                 FileData fileData,
                                 Map<String, ProjectMetainfo.FileBaseline> baselines) {
            registry.save(projectName, toMetainfo(repositoryId, fileData, baselines));
        }

        @Override
        public FileData getFileData() {
            var metainfo = registry.get(projectName);
            if (metainfo == null || !metainfo.hasRevision()) {
                // Only partial information is available. Cannot fill FileData. Must request from repository.
                return null;
            }

            var fileData = new FileData();
            fileData.setName(projectName);
            fileData.setVersion(metainfo.version());
            fileData.setBranch(metainfo.branch());
            if (metainfo.author() != null) {
                fileData.setAuthor(new UserInfo(metainfo.author()));
            }
            fileData.setModifiedAt(new Date(metainfo.modifiedAt()));
            if (metainfo.size() != null) {
                fileData.setSize(metainfo.size());
            }
            fileData.setComment(metainfo.comment());
            if (metainfo.pathInRepository() != null) {
                fileData.addAdditionalData(new FileMappingData(projectName, metainfo.pathInRepository()));
            }
            return fileData;
        }

        private ProjectMetainfo toMetainfo(String repositoryId,
                                           FileData fileData,
                                           Map<String, ProjectMetainfo.FileBaseline> baselines) {
            var mappingData = fileData.getAdditionalData(FileMappingData.class);
            var author = Optional.ofNullable(fileData.getAuthor()).map(UserInfo::getName).orElse(null);
            return new ProjectMetainfo(repositoryId,
                    mappingData == null ? null : mappingData.getInternalPath(),
                    fileData.getBranch(),
                    fileData.getVersion(),
                    author,
                    fileData.getModifiedAt() == null ? null : fileData.getModifiedAt().getTime(),
                    fileData.getSize(),
                    fileData.getComment(),
                    baselines);
        }
    }
}
