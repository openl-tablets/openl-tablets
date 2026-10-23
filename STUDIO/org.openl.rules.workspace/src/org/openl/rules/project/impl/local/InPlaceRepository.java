package org.openl.rules.project.impl.local;

import java.nio.file.Path;
import java.util.Map;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import org.openl.rules.repository.api.FileData;

/**
 * The project folders of a design repository on the file system, edited where they are.
 *
 * <p>OpenL Studio keeps no copy of these projects in the user workspace: every edit is written straight into the
 * folder of the design repository. So a project here is always opened and never modified, and nothing is recorded
 * about it in the metainfo registry of the user, which holds the copies of the projects in the workspace.
 *
 * <p>The paths of this repository are the paths of the design repository on the file system.
 */
public class InPlaceRepository extends LocalRepository {

    @Getter
    private final Path workspaceFolder;

    /**
     * @param root            the folder of the design repository on the file system
     * @param workspaceFolder the folder of the user workspace, which keeps the edit history of the projects
     * @param registry        the metainfo registry of the user
     */
    public InPlaceRepository(Path root, Path workspaceFolder, MetainfoRegistry registry) {
        super(root, registry);
        this.workspaceFolder = workspaceFolder;
    }

    @Override
    public ProjectState getProjectState(String pathInProject) {
        return new InPlaceProjectState();
    }

    @Override
    protected void noteModified(String projectName) {
        // The folder is the project: a write leaves nothing to save.
    }

    @Override
    protected void forgetProject(String projectName) {
        // Nothing is recorded about the project, and its edit history stays in the workspace of the user.
    }

    @Override
    protected @Nullable String uniqueIdOf(String projectName, String path, long size, long modifiedAt) {
        return null;
    }

    @Override
    protected ProjectMetainfo.@Nullable FileBaseline baselineOf(String projectName, String path) {
        return null;
    }

    /** The state of a project edited in place: its files on disk are its current version. */
    private final class InPlaceProjectState implements ProjectState {

        @Override
        public void notifyModified() {
            invokeListener();
        }

        @Override
        public boolean isModified() {
            return false;
        }

        @Override
        public @Nullable String getProjectVersion() {
            return null;
        }

        @Override
        public String getRepositoryId() {
            return getId();
        }

        @Override
        public void saveFileData(String repositoryId, FileData fileData) {
            // Nothing is recorded: the folder is the project.
        }

        @Override
        public void saveSnapshot(String repositoryId,
                                 FileData fileData,
                                 Map<String, ProjectMetainfo.FileBaseline> baselines) {
            // Nothing is recorded: the folder is the project.
        }

        @Override
        public @Nullable FileData getFileData() {
            return null;
        }
    }
}
