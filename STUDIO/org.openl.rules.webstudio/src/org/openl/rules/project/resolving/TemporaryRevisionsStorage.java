package org.openl.rules.project.resolving;

import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.impl.local.LocalFolderAPI;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceImpl;
import org.openl.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.io.File;

import static org.apache.commons.io.FilenameUtils.concat;

public class TemporaryRevisionsStorage implements DisposableBean {
    private final Logger log = LoggerFactory.getLogger(TemporaryRevisionsStorage.class);
    private static final String DEFAULT_STORAGE_PATH = concat(FileUtils.getTempDirectoryPath() + "/", "openl_repo/");

    private final Object lock = new Object();

    private volatile String storagePath = DEFAULT_STORAGE_PATH;
    private volatile File storageFolder;

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public File getRevision(FolderAPI project) throws ProjectException {
        File revisionFolder = getRevisionFolder(project.getName(), project.getVersion());
        if (!revisionFolder.exists()) {
            extractRevision(project, revisionFolder);
        }
        return revisionFolder;
    }

    public void deleteRevisions(FolderAPI project) {
        String projectName = project.getName();

        File folder = getStorageFolder();
        String revisionFolders[] = folder.list(new PrefixFileFilter(projectName));

        if (revisionFolders != null) {
            for (String revisionFolder : revisionFolders) {
                // Folder name example: projectName_v123
                if (revisionFolder.substring(projectName.length()).matches("_v\\d+")) {
                    FolderHelper.deleteFolder(new File(folder, revisionFolder));
                }
            }
        }
    }

    public void clear() {
        synchronized (lock) {
            File folder = getStorageFolder();
            if (!FolderHelper.clearFolder(folder)) {
                log.error("Failed to clear a folder \"{}\"!", folder.getAbsolutePath());
            } else {
                log.info("Temprorary folder for revisions was cleared.");
            }
        }
    }

    public void destroy() {
        synchronized (lock) {
            FolderHelper.deleteFolder(getStorageFolder());
            storageFolder = null;
            log.info("Temprorary folder for revisions was deleted.");
        }
    }

    private void extractRevision(FolderAPI project, File revisionFolder) throws ProjectException {
        try {
            if (!revisionFolder.mkdirs() && !revisionFolder.exists()) {
                throw new ProjectException("Failed to create folder " + revisionFolder.getAbsolutePath());
            }

            LocalFolderAPI whereExtract = new LocalFolderAPI(revisionFolder, new ArtefactPathImpl(
                    revisionFolder.getName()), new LocalWorkspaceImpl(null, revisionFolder.getParentFile(),
                    null, null));
            new AProject(whereExtract).update(new AProject(project), null);
        } catch (ProjectException e) {
            FolderHelper.deleteFolder(revisionFolder);
            throw e;
        }
    }

    private File getRevisionFolder(String projectName, CommonVersion version) {
        return new File(new File(getStorageFolder(), getRevisionFolderName(projectName, version)), projectName);
    }

    private String getRevisionFolderName(String projectName, CommonVersion version) {
        return String.format("%s_v%s", projectName, version.getVersionName());
    }

    private File getStorageFolder() {
        if (storageFolder == null) {
            synchronized (lock) {
                if (storageFolder == null) {
                    storageFolder = new File(storagePath);

                    if (!storageFolder.mkdirs()) {
                        if (storageFolder.exists()) {
                            clear();
                        } else {
                            log.error("Failed to create a folder '{}'", storagePath);
                        }
                    }
                }
            }
        }
        return storageFolder;
    }
}
