package org.openl.rules.project.abstraction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openl.rules.common.*;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.common.impl.RepositoryProjectVersionImpl;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.LockEngine;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.RuntimeExceptionWrapper;

public class RulesProject extends UserWorkspaceProject {
    private LocalRepository localRepository;
    private String localFolderName;
    private Repository designRepository;
    private String designFolderName;
    private List<FileData> historyFileDatas;
    private final LockEngine lockEngine;

    public RulesProject(UserWorkspace userWorkspace,
            LocalRepository localRepository,
            FileData localFileData,
            Repository designRepository, FileData designFileData, LockEngine lockEngine) {
        super(userWorkspace.getUser(), localFileData != null ? localRepository : designRepository,
                localFileData != null ? localFileData : designFileData, localFileData != null);
        this.localRepository = localRepository;
        this.localFolderName = localFileData == null ? null : localFileData.getName();
        this.designRepository = designRepository;
        this.designFolderName = designFileData == null ? null : designFileData.getName();
        this.lockEngine = lockEngine;
    }

    @Override
    public void save(CommonUser user) throws ProjectException {
        AProject designProject = new AProject(designRepository, designFolderName, false);
        AProject localProject = new AProject(localRepository, localFolderName, true);
        designProject.getFileData().setComment(getFileData().getComment());
        designProject.update(localProject, user);
        setHistoryVersion(designProject.getFileData().getVersion());
        clearModifyStatus();
        unlock();
        refresh();
    }

    @Override
    public void delete(CommonUser user) throws ProjectException {
        if (isLocalOnly()) {
            erase();
        } else {
            super.delete(user);
        }
    }

    public void close(CommonUser user) throws ProjectException {
        try {
            if (localFolderName != null) {
                deleteFromLocalRepository();
            }
            if (isLockedByUser(user)) {
                unlock();
            }
            if (!isLocalOnly()) {
                setRepository(designRepository);
                setFolderPath(designFolderName);
                setHistoryVersion(null);
                setFolderStructure(false);
            }
        } finally {
            refresh();
        }
    }

    private void deleteFromLocalRepository() throws ProjectException {
        try {
            for (FileData fileData : localRepository.list(localFolderName)) {
                if (!localRepository.delete(fileData)) {
                    throw new ProjectException("Can't close project because some resources are used");
                }
            }
            // Delete properties folder. Workaround for broken empty projects that failed to delete properties folder last time
            localRepository.getProjectState(localFolderName).notifyModified();
        } catch (IOException e) {
            throw new ProjectException("Not possible to read the directory", e);
        }
    }

    @Override
    public void erase() throws ProjectException {
        try {
            if (designFolderName != null) {
                if (!designRepository.deleteHistory(designFolderName, null)) {
                    throw new ProjectException("Can't erase project because it is absent or can't be deleted");
                }
            } else {
                deleteFromLocalRepository();
            }
        } finally {
          refresh();
        }
    }

    @Override
    public void refresh() {
        super.refresh();
        setFileData(null);
    }

    @Override
    public LockInfo getLockInfo() {
        return lockEngine.getLockInfo(getName());
    }

    @Override
    public void lock() throws ProjectException {
        lockEngine.lock(getName());
    }

    @Override
    public void unlock() throws ProjectException {
        lockEngine.unlock(getName());
    }

    public String getLockedUserName() {
        return getLockInfo().getLockedBy().getUserName();
    }

    public ProjectVersion getVersion() {
        String historyVersion = getHistoryVersion();
        if (historyVersion == null) {
            return getLastVersion();
        }
        return new RepositoryProjectVersionImpl(historyVersion, null);
    }

    @Override
    public ProjectVersion getLastVersion() {
        List<FileData> fileDatas = getHistoryFileDatas();
        return fileDatas.isEmpty() ? null : createProjectVersion(fileDatas.get(fileDatas.size() - 1));
    }

    @Override
    protected boolean isLastVersion() {
        if (getHistoryVersion() == null) {
            return true;
        }
        List<FileData> fileDatas = getHistoryFileDatas();
        return fileDatas.isEmpty() || getHistoryVersion().equals(fileDatas.get(fileDatas.size() - 1).getVersion());
    }

    @Override
    public List<ProjectVersion> getVersions() {
        Collection<FileData> fileDatas = getHistoryFileDatas();
        List<ProjectVersion> versions = new ArrayList<ProjectVersion>();
        for (FileData data : fileDatas) {
            versions.add(createProjectVersion(data));
        }
        return versions;
    }

    @Override
    public int getVersionsCount() {
        return getHistoryFileDatas().size();
    }

    @Override
    protected ProjectVersion getVersion(int index) throws RRepositoryException {
        List<FileData> fileDatas = getHistoryFileDatas();
        return fileDatas.isEmpty() ? null : createProjectVersion(fileDatas.get(index));
    }

    private List<FileData> getHistoryFileDatas() {
        if (historyFileDatas == null) {
            try {
                if (designFolderName != null) {
                    historyFileDatas = designRepository.listHistory(designFolderName);
                } else {
                    // Local repository doesn't have versions
                    historyFileDatas = Collections.emptyList();
                }
            } catch (IOException ex) {
                throw RuntimeExceptionWrapper.wrap(ex);
            }
        }
        return historyFileDatas;
    }

    @Override
    public void setFileData(FileData fileData) {
        super.setFileData(fileData);
        historyFileDatas = null;
    }

    public List<ProjectVersion> getArtefactVersions(ArtefactPath artefactPath) {
        String subPath = artefactPath.getStringValue();
        if (subPath.isEmpty() || subPath.equals("/")) {
            return getVersions();
        }
        if (!subPath.startsWith("/")) {
            subPath += "/";
        }
        String fullPath = getFolderPath() + subPath;
        Collection<FileData> fileDatas;
        try {
            fileDatas = getRepository().listHistory(fullPath);
        } catch (IOException ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        }
        List<ProjectVersion> versions = new ArrayList<ProjectVersion>();
        for (FileData data : fileDatas) {
            versions.add(createProjectVersion(data));
        }
        return versions;
    }

    public boolean isLocalOnly() {
        return designRepository == null;
    }

    public boolean isRepositoryOnly() {
        return localFolderName == null;
    }

    public boolean isOpened() {
        return getRepository() == localRepository;
    }

    public void openVersion(String version) throws ProjectException {
        AProject designProject = new AProject(designRepository, designFolderName, version, false);

        if (localFolderName == null) {
            localFolderName = designProject.getName();
        }
        new AProject(localRepository, localFolderName, true).update(designProject, getUser());
        setRepository(localRepository);
        setFolderPath(localFolderName);
        setFolderStructure(true);

        setHistoryVersion(version);

        refresh();

        localRepository.getProjectState(localFolderName).clearModifyStatus();
    }

    // Is Opened for Editing by me? -- in LW + locked by me
    public boolean isOpenedForEditing() {
        return !isLocalOnly() && super.isOpenedForEditing() && !isRepositoryOnly();
    }

    @Override
    public boolean isModified() {
        return !isRepositoryOnly() && localRepository.getProjectState(localFolderName).isModified();

    }

    private void clearModifyStatus() {
        if (!isRepositoryOnly()) {
            localRepository.getProjectState(localFolderName).clearModifyStatus();
        }
    }

    private void setModified() {
        if (!isRepositoryOnly()) {
            localRepository.getProjectState(localFolderName).notifyModified();
        }
    }

    @Override
    public void setHistoryVersion(String historyVersion) {
        super.setHistoryVersion(historyVersion);
        if (isOpened()) {
            localRepository.getProjectState(localFolderName).setProjectVersion(historyVersion);
        }
    }

    @Override
    public ArtefactPath getArtefactPath() {
        // Return artefact name inside the project including project name. In the case of project it's just project name.
        if (isOpened()) {
            return super.getArtefactPath();
        } else {
            return new ArtefactPathImpl(getName());
        }
    }
}
