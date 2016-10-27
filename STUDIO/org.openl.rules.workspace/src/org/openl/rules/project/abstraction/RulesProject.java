package org.openl.rules.project.abstraction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openl.rules.common.*;
import org.openl.rules.common.impl.RepositoryProjectVersionImpl;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.dtr.impl.LockInfoImpl;
import org.openl.rules.workspace.uw.UserWorkspace;

public class RulesProject extends UserWorkspaceProject {
    private LocalRepository localRepository;
    private String localFolderName;
    private Repository designRepository;
    private String designFolderName;

    public RulesProject(UserWorkspace userWorkspace,
            LocalRepository localRepository,
            String localFolderName,
            Repository designRepository, String designFolderName, String version) {
        super(userWorkspace.getUser(),
                localFolderName != null ? localRepository : designRepository,
                localFolderName != null ? localFolderName : designFolderName,
                version);
        this.localRepository = localRepository;
        this.localFolderName = localFolderName;
        this.designRepository = designRepository;
        this.designFolderName = designFolderName;

        if (designFolderName != null) {
            FileData fileData = new FileData();
            fileData.setName(designFolderName);
            fileData.setVersion(version);
            setFileData(fileData);
        }
    }

    public RulesProject(UserWorkspace userWorkspace,
            LocalRepository localRepository,
            FileData localFileData,
            Repository designRepository, FileData designFileData) {
        super(userWorkspace.getUser(), localFileData != null ? localRepository : designRepository,
                localFileData != null ? localFileData : designFileData);
        this.localRepository = localRepository;
        this.localFolderName = localFileData == null ? null : localFileData.getName();
        this.designRepository = designRepository;
        this.designFolderName = designFileData == null ? null : designFileData.getName();
    }

    public void edit(CommonUser user) throws ProjectException {
        super.edit(user);
        open();
    }

    @Override
    public void save(CommonUser user) throws ProjectException {
        clearModifyStatus();
        new AProject(designRepository, designFolderName).update(new AProject(localRepository, localFolderName), user);
//        localRepository.setCurrentVersion(designRepository.getVersion());
//        localRepository.commit(user, 0);// save persistence
        unlock(user);
        refresh();
    }

    @Override
    public void delete(CommonUser user) throws ProjectException {
        if (isLocalOnly()) {
            erase();
        } else {
            if (isOpened()) {
                close();
            }
            super.delete(user);
        }
    }

    public void close(CommonUser user) throws ProjectException {
        if (localFolderName != null) {
            localRepository.delete(localFolderName);
        }
        if (isLockedByUser(user)) {
            unlock(user);
        }
        if (!isLocalOnly()) {
            setRepository(designRepository);
            setFolderPath(designFolderName);
        }
        refresh();
    }

    @Override
    public void erase() throws ProjectException {
        if (designFolderName != null) {
            designRepository.deleteHistory(designFolderName, null);
        } else {
            localRepository.delete(localFolderName);
        }
    }

    @Override
    public void refresh() {
        super.refresh();
        setFileData(null);
    }

    public LockInfo getLockInfo() {
        return LockInfoImpl.NO_LOCK;
    }

    @Override
    public void lock(CommonUser user) throws ProjectException {
        // Do nothing
    }

    @Override
    public void unlock(CommonUser user) throws ProjectException {
        // Do nothing
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
        if (designFolderName != null) {
            return designRepository.listHistory(designFolderName).size();
        } else {
            return localRepository.listHistory(localFolderName).size();
        }
    }

    @Override
    protected ProjectVersion getVersion(int index) throws RRepositoryException {
        List<FileData> fileDatas = getHistoryFileDatas();
        return fileDatas.isEmpty() ? null : createProjectVersion(fileDatas.get(index));
    }

    private List<FileData> getHistoryFileDatas() {
        List<FileData> fileDatas;
        if (designFolderName != null) {
            fileDatas = designRepository.listHistory(designFolderName);
        } else {
            fileDatas = localRepository.list(localFolderName);
        }
        return fileDatas;
    }

    public List<ProjectVersion> getArtefactVersions(ArtefactPath artefactPath) {
        String subPath = artefactPath.getStringValue();
        if (!subPath.startsWith("/")) {
            subPath += "/";
        }
        String fullPath = getFolderPath() + subPath;
        Collection<FileData> fileDatas = getRepository().listHistory(fullPath);
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
        AProject designProject = new AProject(designRepository, designFolderName, version);

        if (localFolderName == null) {
            localFolderName = designProject.getName();
        }
        new AProject(localRepository, localFolderName).update(designProject, getUser());
        setRepository(localRepository);
        setFolderPath(localFolderName);

        setHistoryVersion(version);

        refresh();

        localRepository.clearModifyStatus(localFolderName);
    }

    // Is Opened for Editing by me? -- in LW + locked by me
    public boolean isOpenedForEditing() {
        return !isLocalOnly() && isLockedByMe() && !isRepositoryOnly();
    }

    @Override
    public boolean isModified() {
        return !isRepositoryOnly() && localRepository.isModified(localFolderName);

    }

    private void clearModifyStatus() {
        if (!isRepositoryOnly()) {
            localRepository.clearModifyStatus(localFolderName);
        }
    }

}
