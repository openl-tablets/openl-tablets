package org.openl.rules.project.abstraction;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.CommonUser;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.lock.LockInfo;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.ProjectMetainfo;
import org.openl.rules.repository.api.AdditionalData;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.ConflictResolveData;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.RepositoryDelegate;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.util.FileUtils;

@Slf4j
public class RulesProject extends UserWorkspaceProject {

    @Getter
    private final LocalRepository localRepository;
    @Getter
    private String localFolderName;

    @Getter
    private Repository designRepository;
    @Getter
    private String designFolderName;
    private final LockEngine lockEngine;
    private final RulesProjectTags localTags;
    private final ProjectTags designTags;

    public RulesProject(WorkspaceUser user,
                        LocalRepository localRepository,
                        FileData localFileData,
                        Repository designRepository,
                        FileData designFileData,
                        LockEngine lockEngine) {
        super(user,
                localFileData != null ? localRepository : designRepository,
                localFileData != null ? localFileData : designFileData);
        this.localRepository = localRepository;
        this.localFolderName = localFileData == null ? null : localFileData.getName();
        this.designRepository = designRepository;
        this.designFolderName = designFileData == null ? null : designFileData.getName();
        this.lockEngine = lockEngine;

        FileData fullLocalFileData;
        if (localFileData != null && designFileData != null) {
            var localVersion = localFileData.getVersion();
            if (localVersion == null || localVersion.equals(designFileData.getVersion())) {
                // Set the path for local repository, other properties are equal to design repository properties
                fullLocalFileData = new FileData();
                fullLocalFileData.setName(localFileData.getName());
                fullLocalFileData.setVersion(designFileData.getVersion());
                fullLocalFileData.setSize(designFileData.getSize());
                fullLocalFileData.setAuthor(designFileData.getAuthor());
                fullLocalFileData.setModifiedAt(designFileData.getModifiedAt());
                fullLocalFileData.setComment(designFileData.getComment());
                fullLocalFileData.setDeleted(designFileData.isDeleted());
                for (AdditionalData data : designFileData.getAdditionalData().values()) {
                    fullLocalFileData.addAdditionalData(data);
                }
                setFileData(fullLocalFileData);
            } else {
                if (localFileData.getAuthor() == null || localFileData.getAuthor().getName() == null || localFileData
                        .getModifiedAt() == null) {
                    // Lazy load properties
                    setFileData(null);
                } else {
                    setFileData(localFileData);
                }
            }
        }

        if (designFileData != null) {
            setLastHistoryVersion(designFileData.getVersion());
        }
        localTags = new RulesProjectTags(this);
        if (designFileData != null) {
            designTags = new ProjectTags(new AProject(designRepository, designFileData));
        } else {
            designTags = localTags;
        }
    }

    @Override
    public void save(CommonUser user) throws ProjectException {
        save(user, null);
    }

    public void save(AdditionalData additionalData) throws ProjectException {
        save(getUser(), additionalData);
    }

    private void save(CommonUser user, AdditionalData additionalData) throws ProjectException {
        var oldVersion = getHistoryVersion();
        var designData = new FileData();
        designData.setName(designFolderName);
        designData.setVersion(oldVersion);

        var fileData = getFileData();
        for (AdditionalData value : fileData.getAdditionalData().values()) {
            designData.addAdditionalData(value);
        }

        designData.addAdditionalData(additionalData);
        designData.setComment(fileData.getComment());

        var designProject = new AProject(designRepository, designData);
        var localProject = new AProject(localRepository, localFolderName);
        var realPath = designProject.getRealPath();
        designProject.update(localProject, user);

        // Process saved data
        if (designRepository.supports().mappedFolders()) {
            // Project can be renamed.
            var mappedName = ((FolderMapper) designRepository).findMappedName(realPath);
            if (mappedName == null) {
                designFolderName = designProject.getFileData().getName();
            } else {
                designFolderName = mappedName;
            }
        }
        var version = designProject.getFileData().getVersion();
        setLastHistoryVersion(version);
        setHistoryVersion(version);

        refresh();

        // If oldVersion is null, then the project was absent before, no need to update workspace. Otherwise update
        // workspace.
        if (oldVersion != null) {
            // If there are additional commits (merge commits) we cannot assume that their hash codes are same as for
            // local files.
            List<FileData> fileDatas = getHistoryFileDatas();
            var extraCommits = (fileDatas.size() > 1 && !fileDatas.get(fileDatas.size() - 2)
                    .getVersion()
                    .equals(oldVersion)) || additionalData instanceof ConflictResolveData;
            if (extraCommits) {
                openVersion(version);
            } else {
                resetLocalFileData();
            }
        }
        unlock();
    }

    @Override
    public void delete(CommonUser user, String comment) throws ProjectException {
        if (isLocalOnly()) {
            try {
                deleteFromLocalRepository();
            } finally {
                refresh();
            }
        } else {
            super.delete(user, comment);
        }
    }

    @Override
    public void close(CommonUser user) throws ProjectException {
        try {
            if (isLockedByUser(user)) {
                unlock();
            }
            if (localFolderName != null) {
                deleteFromLocalRepository();
            }
            if (!isLocalOnly()) {
                setRepository(designRepository);
                setFolderPath(designFolderName);
                setHistoryVersion(null);
            }
        } finally {
            refresh();
        }
    }

    private void deleteFromLocalRepository() throws ProjectException {
        try {
            for (FileData fileData : localRepository.list(localFolderName)) {
                IOException deleteCause = null;
                boolean deleted;
                try {
                    deleted = localRepository.delete(fileData);
                } catch (IOException e) {
                    deleted = false;
                    deleteCause = e;
                }

                if (!deleted) {
                    try {
                        if (localRepository.check(fileData.getName()) != null) {
                            var message = "Cannot close project because resource '%s' is used".formatted(
                                    fileData.getName());
                            if (deleteCause == null) {
                                throw new ProjectException(message);
                            } else {
                                throw new ProjectException(message, deleteCause);
                            }
                        }
                    } catch (IOException e) {
                        throw new ProjectException("Not possible to read the directory", e);
                    }
                }
            }

            // Delete empty folders. They won't be deleted in the code above.
            var folderData = new FileData();
            folderData.setName(localFolderName);
            localRepository.delete(folderData);
        } catch (IOException e) {
            throw new ProjectException("Not possible to read the directory", e);
        }
    }

    @Override
    public LockInfo getLockInfo() {
        try {
            if (isLocalOnly()) {
                return LockInfo.NO_LOCK;
            }
            return lockEngine.getLockInfo(getDesignRepository().getId(), getBranch(), getRealPath());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return LockInfo.NO_LOCK;
        }
    }

    @Override
    public void unlock() {
        if (isLocalOnly()) {
            // No need to unlock local only projects. Other users don't see it.
            return;
        }
        lockEngine.unlock(getDesignRepository().getId(), getBranch(), getRealPath());
    }

    // for ability to unlock project if something went wrong
    public void forceUnlock() {
        lockEngine.forceUnlock(getDesignRepository().getId(), getBranch(), getRealPath());
    }

    /**
     * Try to lock the project if it's not locked already. Does not overwrite lock info if the user was locked already.
     *
     * @return false if the project was locked by other user. true if project wasn't locked before or was locked by me.
     */
    @Override
    public boolean tryLock() {
        if (isLocalOnly()) {
            // No need to lock local only projects. Other users don't see it.
            return true;
        }
        return lockEngine.tryLock(getDesignRepository().getId(), getBranch(), getRealPath(), getUser().getUserName());
    }

    public String getLockedUserName() {
        var lockInfo = getLockInfo();
        return lockInfo.isLocked() ? lockInfo.getLockedBy() : "";
    }

    @Override
    public ProjectVersion getVersion() {
        var historyVersion = getHistoryVersion();
        if (historyVersion == null) {
            if (designFolderName != null) {
                try {
                    return createProjectVersion(designRepository.check(designFolderName));
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }

            return null;
        }
        return super.getVersion();
    }

    @Override
    public List<FileData> getHistoryFileDatas() {
        if (historyFileDatas == null) {
            try {
                if (designFolderName != null) {
                    historyFileDatas = designRepository.listHistory(designFolderName);
                } else {
                    // Local repository does not have versions
                    historyFileDatas = List.of();
                }
            } catch (IOException ex) {
                log.error(ex.getMessage(), ex);
                historyFileDatas = List.of();
            }
        }
        return historyFileDatas;
    }

    public boolean hasArtefactVersions(ArtefactPath artefactPath) {
        var subPath = artefactPath.getStringValue();
        if (subPath.isEmpty() || subPath.equals("/")) {
            return getLastHistoryVersion() != null;
        }
        if (!subPath.startsWith("/")) {
            subPath = "/" + subPath;
        }
        var fullPath = getFolderPath() + subPath;
        try {
            return getRepository().check(fullPath) != null;
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
            return false;
        }
    }

    @Override
    public boolean isLocalOnly() {
        return designFolderName == null;
    }

    private boolean isRepositoryOnly() {
        return localFolderName == null;
    }

    @Override
    public boolean isOpened() {
        return getRepository() == localRepository;
    }

    @Override
    public void openVersion(String version) throws ProjectException {
        var designProject = new AProject(designRepository, designFolderName, version);

        if (localFolderName == null) {
            localFolderName = designProject.getBusinessName();
        }

        // The file copy and the record write are one atomic step for the registry, so a concurrent
        // registry refresh cannot treat the half-open project as garbage.
        localRepository.getMetainfoRegistry().runLocked(localFolderName, () -> {
            var localProject = new AProject(localRepository, localFolderName);
            localProject.update(designProject, getUser());
            setRepository(localRepository);
            setFolderPath(localFolderName);

            var designVersion = Optional.ofNullable(designProject.getFileData())
                    .map(FileData::getVersion)
                    .orElseThrow(() -> new ProjectException("Cannot open. Revision not found."));
            setHistoryVersion(designVersion);
            if (version == null) {
                // version == 0 means that designVersion is last history version
                setLastHistoryVersion(designVersion);
            }

            refresh();
            resetLocalFileData();
            return null;
        });
    }

    @Override
    protected FileData getFileDataForUnversionableRepo(Repository repository) {
        var designRepository = getDesignRepository();
        // Unwrap delegate repository to get real repository, because delegate repository can be secured.
        // Get file data can't be secured, because it's used in security check to build identity.
        while (designRepository instanceof RepositoryDelegate) {
            designRepository = ((RepositoryDelegate) designRepository).getOriginal();
        }
        if (isLocalOnly()) {
            var fileData = super.getFileDataForUnversionableRepo(repository);
            if (designRepository != null && designRepository.supports().branches()) {
                fileData.setBranch(((BranchRepository) designRepository).getBranch());
            }
            return fileData;
        }

        var version = getHistoryVersion();
        String actualVersion = version == null ? getLastHistoryVersion() : version;

        var fileData = new FileData();
        fileData.setName(getFolderPath());
        fileData.setVersion(actualVersion);

        if (designRepository.supports().branches()) {
            fileData.setBranch(((BranchRepository) designRepository).getBranch());
        }

        if (actualVersion != null) {
            try {
                var repoData = designRepository.checkHistory(designFolderName, actualVersion);
                if (repoData != null) {
                    fileData.setAuthor(repoData.getAuthor());
                    fileData.setModifiedAt(repoData.getModifiedAt());
                    fileData.setComment(repoData.getComment());
                    fileData.setSize(repoData.getSize());
                    fileData.setDeleted(repoData.isDeleted());
                    fileData.setUniqueId(repoData.getUniqueId());
                    var mappingData = repoData.getAdditionalData(FileMappingData.class);
                    if (mappingData != null) {
                        fileData.addAdditionalData(mappingData);
                    } else if (!designRepository.supports().mappedFolders()) {
                        // For flat folder structure external (virtual) path is equal to internal (real) path.
                        fileData.addAdditionalData(new FileMappingData(repoData.getName(), repoData.getName()));
                    }
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }

        return fileData;
    }

    @Override
    protected String findLastHistoryVersion() {
        if (designFolderName != null) {
            try {
                var fileData = designRepository.check(designFolderName);
                if (fileData != null) {
                    return fileData.getVersion();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return null;
    }

    private void resetLocalFileData() {
        var fileData = getFileData();
        if (designRepository.supports().branches()) {
            fileData.setBranch(((BranchRepository) designRepository).getBranch());
        }
        localRepository.getProjectState(localFolderName)
                .saveSnapshot(designRepository.getId(), fileData, collectBaselines());
    }

    /**
     * Captures the baselines of all local project files for the synchronization snapshot.
     *
     * <p>Every file records its actual size and modification time. The repository revision id is added
     * when the design repository provides per-file ids.
     */
    private Map<String, ProjectMetainfo.FileBaseline> collectBaselines() {
        var baselines = new HashMap<String, ProjectMetainfo.FileBaseline>();
        try {
            Map<String, String> designUniqueIds = designUniqueIds();
            for (FileData localData : localRepository.list(localFolderName + "/")) {
                var path = localData.getName().substring(localFolderName.length());
                baselines.put(path,
                        new ProjectMetainfo.FileBaseline(designUniqueIds.get(path),
                                localData.getSize(),
                                localData.getModifiedAt().getTime()));
            }
        } catch (IOException e) {
            // An incomplete snapshot would silently corrupt the local-changes detection,
            // so fail the whole operation instead.
            throw new IllegalStateException(
                    "Cannot capture the file baselines of the '" + localFolderName + "' project.", e);
        }
        return baselines;
    }

    private Map<String, String> designUniqueIds() throws IOException {
        if (!designRepository.supports().folders() || !designRepository.supports().uniqueFileId()) {
            return Map.of();
        }
        var fromFilePath = designFolderName + "/";
        var historyVersion = getHistoryVersion();
        List<FileData> designFiles = historyVersion != null
                ? designRepository.listFiles(fromFilePath, historyVersion)
                : designRepository.list(fromFilePath);
        var uniqueIds = new HashMap<String, String>();
        for (FileData designData : designFiles) {
            var uniqueId = designData.getUniqueId();
            if (uniqueId != null) {
                uniqueIds.put(designData.getName().substring(designFolderName.length()), uniqueId);
            }
        }
        return uniqueIds;
    }

    // Is Opened for Editing by me? -- in LW + locked by me
    @Override
    public boolean isOpenedForEditing() {
        return !isLocalOnly() && super.isOpenedForEditing() && !isRepositoryOnly();
    }

    @Override
    public boolean isModified() {
        return !isRepositoryOnly() && localRepository.getProjectState(localFolderName).isModified();

    }

    public void setModified() {
        if (!isRepositoryOnly()) {
            localRepository.getProjectState(localFolderName).notifyModified();
        }
    }

    @Override
    public ArtefactPath getArtefactPath() {
        // Return artefact name inside the project including project name. In the case of project it's just project
        // name.
        if (isOpened()) {
            return super.getArtefactPath();
        } else {
            return new ArtefactPathImpl(getName());
        }
    }

    @Override
    protected void setDesignRepository(Repository repository) {
        this.designRepository = repository;

        if (!isOpened()) {
            setRepository(repository);
        }
    }

    /**
     * Switches to a project entry that was verified in the target branch.
     *
     * <p>The entry supplies both the branch-scoped repository and that branch's mapped project path.
     */
    public void setBranch(Repository repository, FileData fileData) {
        setDesignRepository(repository);
        designFolderName = fileData.getName();
        if (!isOpened()) {
            setFolderPath(designFolderName);
        }
        setHistoryVersion(null);
        refresh();
        getFileData();
    }

    @Override
    public String getRealPath() {
        if (isLocalOnly()) {
            var state = localRepository.getProjectState(getFolderPath());
            if (state.getFileData() != null) {
                var mappingData = state.getFileData().getAdditionalData(FileMappingData.class);
                if (mappingData != null) {
                    return mappingData.getInternalPath();
                }
            }

            return localFolderName;
        }
        var folderPath = getDesignFolderName();
        var repository = getDesignRepository();
        if (repository.supports().mappedFolders()) {
            if (isOpened()) {
                var state = localRepository.getProjectState(getFolderPath());
                FileMappingData mappingData = null;
                if (state.getFileData() != null) {
                    mappingData = state.getFileData().getAdditionalData(FileMappingData.class);
                }
                if (mappingData == null) {
                    final var fileData = getFileData();
                    if (fileData != null) {
                        mappingData = fileData.getAdditionalData(FileMappingData.class);
                    }
                }
                if (mappingData != null) {
                    return mappingData.getInternalPath();
                }
            }
            return ((FolderMapper) repository).getRealPath(folderPath);
        } else {
            return folderPath;
        }
    }

    public String getMainBusinessName() {
        var repository = getDesignRepository();
        if (repository == null) {
            return getBusinessName();
        }
        var folderPath = getDesignFolderName();
        if (repository.supports().mappedFolders()) {
            folderPath = ((FolderMapper) repository).getBusinessName(folderPath);
        }
        return folderPath.substring(folderPath.lastIndexOf('/') + 1);
    }

    /**
     * Returns the external design-repository identity used by the project index.
     *
     * <p>This can differ from {@link #getName()} for an opened project because its local workspace folder uses the
     * business name. Mapped design repositories include a path-derived suffix in the external folder name.
     */
    public String getDesignProjectName() {
        return FileUtils.getName(getDesignFolderName());
    }

    public Map<String, String> getLocalTags() {
        return localTags.getTags();
    }

    public void saveTags(Map<String, String> tags) throws ProjectException {
        localTags.saveTags(tags);
    }

    public Map<String, String> getDesignTags() {
        return designTags.getTags();
    }
}
