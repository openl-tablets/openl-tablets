package org.openl.rules.project.abstraction;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.CommonUser;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.lock.LockInfo;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.ProjectState;
import org.openl.rules.repository.api.AdditionalData;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.ConflictResolveData;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FolderMapper;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RulesProject extends UserWorkspaceProject {
    private final Logger log = LoggerFactory.getLogger(RulesProject.class);

    private final LocalRepository localRepository;
    private String localFolderName;

    private Repository designRepository;
    private String designFolderName;
    private final LockEngine lockEngine;

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
            String localVersion = localFileData.getVersion();
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
                if (localFileData.getAuthor() == null || localFileData.getModifiedAt() == null) {
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
    }

    @Override
    public void save(CommonUser user) throws ProjectException {
        save(user, null);
    }

    public void save(AdditionalData additionalData) throws ProjectException {
        save(getUser(), additionalData);
    }

    private void save(CommonUser user, AdditionalData additionalData) throws ProjectException {
        String oldVersion = getHistoryVersion();
        FileData designData = new FileData();
        designData.setName(designFolderName);
        designData.setVersion(oldVersion);

        FileData fileData = getFileData();
        for (AdditionalData value : fileData.getAdditionalData().values()) {
            designData.addAdditionalData(value);
        }

        designData.addAdditionalData(additionalData);
        designData.setComment(fileData.getComment());

        AProject designProject = new AProject(designRepository, designData);
        AProject localProject = new AProject(localRepository, localFolderName);
        String realPath = designProject.getRealPath();
        designProject.update(localProject, user);

        // Process saved data
        if (designRepository.supports().mappedFolders()) {
            // Project can be renamed.
            String mappedName = ((FolderMapper) designRepository).findMappedName(realPath);
            if (mappedName == null) {
                designFolderName = designProject.getFileData().getName();
            } else {
                designFolderName = mappedName;
            }
        }
        String version = designProject.getFileData().getVersion();
        setLastHistoryVersion(version);
        setHistoryVersion(version);

        refresh();
        
        // If oldVersion is null, then the project was absent before, no need to update workspace. Otherwise update
        // workspace.
        if (oldVersion != null) {
            // If there are additional commits (merge commits) we cannot assume that their hash codes are same as for
            // local files.
            List<FileData> fileDatas = getHistoryFileDatas();
            boolean extraCommits = fileDatas.size() > 1 && !fileDatas.get(fileDatas.size() - 2)
                .getVersion()
                .equals(oldVersion) || additionalData instanceof ConflictResolveData;
            if (extraCommits) {
                openVersion(version);
            } else {
                resetLocalFileData(true);
            }
        }
        unlock();
    }

    @Override
    public void delete(CommonUser user, String comment) throws ProjectException {
        if (isLocalOnly()) {
            erase(user, comment);
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
                if (!isRepositoryOnly()) {
                    localRepository.getProjectState(localFolderName).setProjectVersion(null);
                }
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
                            String message = String.format("Cannot close project because resource '%s' is used",
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
            FileData folderData = new FileData();
            folderData.setName(localFolderName);
            localRepository.delete(folderData);
        } catch (IOException e) {
            throw new ProjectException("Not possible to read the directory", e);
        }
    }

    @Override
    public void erase(CommonUser user, String comment) throws ProjectException {
        try {
            if (designFolderName != null) {
                FileData data = new FileData();
                data.setName(designFolderName);
                data.setVersion(null);
                data.setAuthor(getUser().getUserName());
                data.setComment(comment);
                designRepository.deleteHistory(data);
            } else {
                deleteFromLocalRepository();
            }
        } catch (IOException e) {
            throw new ProjectException(e.getMessage(), e);
        } finally {
            refresh();
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

    //for ability to unlock project if something went wrong
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
        LockInfo lockInfo = getLockInfo();
        return lockInfo.isLocked() ? lockInfo.getLockedBy() : "";
    }

    @Override
    public ProjectVersion getVersion() {
        String historyVersion = getHistoryVersion();
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
    protected List<FileData> getHistoryFileDatas() {
        if (historyFileDatas == null) {
            try {
                if (designFolderName != null) {
                    historyFileDatas = designRepository.listHistory(designFolderName);
                } else {
                    // Local repository does not have versions
                    historyFileDatas = Collections.emptyList();
                }
            } catch (IOException ex) {
                log.error(ex.getMessage(), ex);
                return Collections.emptyList();
            }
        }
        return historyFileDatas;
    }

    public boolean hasArtefactVersions(ArtefactPath artefactPath) {
        String subPath = artefactPath.getStringValue();
        if (subPath.isEmpty() || subPath.equals("/")) {
            return getLastHistoryVersion() != null;
        }
        if (!subPath.startsWith("/")) {
            subPath = "/" + subPath;
        }
        String fullPath = getFolderPath() + subPath;
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
        AProject designProject = new AProject(designRepository, designFolderName, version);

        if (localFolderName == null) {
            localFolderName = designProject.getBusinessName();
        }

        AProject localProject = new AProject(localRepository, localFolderName);
        localProject.update(designProject, getUser());
        setRepository(localRepository);
        setFolderPath(localFolderName);

        String designVersion = designProject.getFileData().getVersion();
        setHistoryVersion(designVersion);
        if (version == null) {
            // version == 0 means that designVersion is last history version
            setLastHistoryVersion(designVersion);
        }

        refresh();
        resetLocalFileData(true);
    }

    @Override
    protected FileData getFileDataForUnversionableRepo(Repository repository) {
        if (isLocalOnly()) {
            FileData fileData = super.getFileDataForUnversionableRepo(repository);
            if (designRepository != null && designRepository.supports().branches()) {
                fileData.setBranch(((BranchRepository) designRepository).getBranch());
            }
            return fileData;
        }

        String version = getHistoryVersion();
        String actualVersion = version == null ? getLastHistoryVersion() : version;

        FileData fileData = new FileData();
        fileData.setName(getFolderPath());
        fileData.setVersion(actualVersion);

        if (designRepository.supports().branches()) {
            fileData.setBranch(((BranchRepository) designRepository).getBranch());
        }

        if (actualVersion != null) {
            try {
                FileData repoData = designRepository.checkHistory(designFolderName, actualVersion);
                if (repoData != null) {
                    fileData.setAuthor(repoData.getAuthor());
                    fileData.setModifiedAt(repoData.getModifiedAt());
                    fileData.setComment(repoData.getComment());
                    fileData.setSize(repoData.getSize());
                    fileData.setDeleted(repoData.isDeleted());
                    fileData.setUniqueId(repoData.getUniqueId());
                    FileMappingData mappingData = repoData.getAdditionalData(FileMappingData.class);
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
                FileData fileData = designRepository.check(designFolderName);
                if (fileData != null) {
                    return fileData.getVersion();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return null;
    }

    private void resetLocalFileData(boolean needUpdateUniqueId) {
        FileData fileData = getFileData();
        if (designRepository.supports().branches()) {
            fileData.setBranch(((BranchRepository) designRepository).getBranch());
        }
        localRepository.getProjectState(localFolderName).clearModifyStatus();
        localRepository.getProjectState(localFolderName).saveFileData(designRepository.getId(), fileData);

        if (needUpdateUniqueId) {
            updateUniqueId();
        }
    }

    private void updateUniqueId() {
        if (designRepository.supports().folders()) {
            FolderRepository fromRepository = (FolderRepository) designRepository;
            if (fromRepository.supports().uniqueFileId()) {
                try {
                    localRepository.deleteAllFileProperties(localFolderName);

                    String fromFilePath = designFolderName + "/";
                    String historyVersion = getHistoryVersion();
                    List<FileData> designFiles = historyVersion != null ? fromRepository.listFiles(fromFilePath,
                        historyVersion) : fromRepository.list(fromFilePath);

                    for (FileData designData : designFiles) {
                        String designDataName = designData.getName();
                        String localName = localFolderName + designDataName.substring(designFolderName.length());

                        // We need to store: 1) unique id 2) file size 3) modified time. Reuse local file data to get
                        // file size and modified time for a file to avoid lazy loading and therefore performance
                        // degradation. Only change unique id that was gotten from design repository.
                        FileData localData = localRepository.check(localName);
                        if (localData != null) {
                            localData.setUniqueId(designData.getUniqueId());

                            localRepository.updateFileProperties(localData);
                        } else {
                            log.warn("Files in local repository for folder {} are not found", localName);
                        }
                    }
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
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

    public String getDesignFolderName() {
        return designFolderName;
    }

    @Override
    protected void setDesignRepository(Repository repository) {
        this.designRepository = repository;

        if (!isOpened()) {
            setRepository(repository);
        }
    }

    @Override
    public Repository getDesignRepository() {
        return designRepository;
    }

    public LocalRepository getLocalRepository() {
        return localRepository;
    }

    @Override
    public String getRealPath() {
        if (isLocalOnly()) {
            ProjectState state = localRepository.getProjectState(getFolderPath());
            if (state.getFileData() != null) {
                FileMappingData mappingData = state.getFileData().getAdditionalData(FileMappingData.class);
                if (mappingData != null) {
                    return mappingData.getInternalPath();
                }
            }

            return localFolderName;
        }
        String folderPath = getDesignFolderName();
        Repository repository = getDesignRepository();
        if (repository.supports().mappedFolders()) {
            if (isOpened()) {
                ProjectState state = localRepository.getProjectState(getFolderPath());
                FileMappingData mappingData = null;
                if (state.getFileData() != null) {
                    mappingData = state.getFileData().getAdditionalData(FileMappingData.class);
                }
                if (mappingData == null) {
                    final FileData fileData = getFileData();
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

    public String getLocalFolderName() {
        return localFolderName;
    }

    public String getMainBusinessName() {
        Repository repository = getDesignRepository();
        if (repository == null) {
            return getBusinessName();
        }
        String folderPath = getDesignFolderName();
        if (repository.supports().mappedFolders()) {
            folderPath = ((FolderMapper) repository).getBusinessName(folderPath);
        }
        return folderPath.substring(folderPath.lastIndexOf('/') + 1);
    }
}
