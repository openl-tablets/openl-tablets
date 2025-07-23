package org.openl.rules.project.abstraction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import jakarta.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.impl.ProjectDescriptorImpl;
import org.openl.rules.common.impl.RepositoryProjectVersionImpl;
import org.openl.rules.lock.LockInfo;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.util.IOUtils;

/**
 * This class stores only deploy configuration, not deployed projects! For the latter see {@link Deployment} class.
 */
public class ADeploymentProject extends UserWorkspaceProject {
    private static final Logger LOG = LoggerFactory.getLogger(ADeploymentProject.class);

    private List<ProjectDescriptor> descriptors;
    private ADeploymentProject openedVersion;
    /* this button is used for rendering the save button (only for deploy configuration) */
    private volatile boolean modifiedDescriptors;

    private final LockEngine lockEngine;

    private ADeploymentProject(WorkspaceUser user,
                               Repository repository,
                               String folderPath,
                               String version,
                               LockEngine lockEngine) {
        super(user, repository, folderPath, version);
        this.lockEngine = lockEngine;
    }

    public ADeploymentProject(WorkspaceUser user, Repository repository, FileData fileData, LockEngine lockEngine) {
        super(user, repository, fileData);
        this.lockEngine = lockEngine;
    }

    public ADeploymentProject(Repository repository, FileData fileData) {
        super(null, repository, fileData);
        lockEngine = null;
    }

    public void addProjectDescriptor(String repositoryId,
                                     String name,
                                     String path,
                                     String branch,
                                     CommonVersion version) {
        if (hasProjectDescriptor(name)) {
            removeProjectDescriptor(name);
        }
        getDescriptors().add(new ProjectDescriptorImpl(repositoryId, name, path, branch, version));
    }

    public boolean hasProjectDescriptor(String name) {
        Collection<ProjectDescriptor> pgl = getProjectDescriptors();

        if (pgl != null) {
            for (ProjectDescriptor descriptor : pgl) {
                if (descriptor.getProjectName().equals(name)) {
                    return true;
                }
            }
        }

        return false;
    }

    public ProjectDescriptor getProjectDescriptor(String name) throws ProjectException {
        for (ProjectDescriptor descriptor : getProjectDescriptors()) {
            if (descriptor.getProjectName().equals(name)) {
                return descriptor;
            }
        }
        throw new ProjectException(String.format("Project descriptor '%s' is not found.", name));
    }

    @Override
    public void openVersion(String version) {
        modifiedDescriptors = false;
        openedVersion = new ADeploymentProject(getUser(), getRepository(), getFolderPath(), version, lockEngine);
        openedVersion.setHistoryVersion(version);
        setHistoryVersion(version);
        refresh();
    }

    @Override
    public void close(CommonUser user) throws ProjectException {
        if (isLockedByUser(user)) {
            unlock();
        }
        modifiedDescriptors = false;
        super.close(user);
        openedVersion = null;
        refresh();
    }

    @Override
    public ProjectVersion getVersion() {
        if (openedVersion == null) {
            if (getHistoryVersion() == null || getFileData() != null) {
                return super.getVersion();
            } else {
                return new RepositoryProjectVersionImpl(getHistoryVersion(), null, false);

            }
        } else {
            return openedVersion.getVersion();
        }
    }

    @Override
    public boolean isOpened() {
        return openedVersion != null; // || isOpenedForEditing();
    }

    @Override
    public void save(CommonUser user) throws ProjectException {
        synchronized (this) {
            InputStream inputStream;
            try {
                inputStream = new ProjectDescriptorSerializer().serialize(descriptors);
            } catch (IOException | JAXBException e) {
                throw new ProjectException(e.getMessage(), e);
            }
            if (getRepository().supports().folders()) {
                FileData fileData = getFileData();
                try {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    IOUtils.copyAndClose(inputStream, out);

                    fileData.setAuthor(user == null ? null : user.getUserInfo());
                    fileData.setSize(out.size());

                    FileItem change = new FileItem(fileData.getName() + "/" + ArtefactProperties.DESCRIPTORS_FILE,
                            new ByteArrayInputStream(out.toByteArray()));
                    setFileData(getRepository()
                            .save(fileData, Collections.singletonList(change), ChangesetType.FULL));
                } catch (IOException e) {
                    throw new ProjectException(e.getMessage(), e);
                }
            } else {
                // Archive the folder using zip
                FileData fileData = getFileData();
                ZipOutputStream zipOutputStream = null;
                try {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    zipOutputStream = new ZipOutputStream(out);

                    ZipEntry entry = new ZipEntry(ArtefactProperties.DESCRIPTORS_FILE);
                    zipOutputStream.putNextEntry(entry);

                    inputStream.transferTo(zipOutputStream);

                    inputStream.close();
                    zipOutputStream.closeEntry();

                    zipOutputStream.close();
                    fileData.setAuthor(user == null ? null : user.getUserInfo());
                    fileData.setSize(out.size());
                    setFileData(getRepository().save(fileData, new ByteArrayInputStream(out.toByteArray())));
                } catch (IOException e) {
                    throw new ProjectException(e.getMessage(), e);
                } finally {
                    IOUtils.closeQuietly(zipOutputStream);
                }
            }

            modifiedDescriptors = false;
            open();
            unlock();
        }
    }

    private void removeProjectDescriptor(String name) {
        Collection<ProjectDescriptor> projectDescriptors = getDescriptors();
        for (ProjectDescriptor descriptor : projectDescriptors) {
            if (descriptor.getProjectName().equals(name)) {
                projectDescriptors.remove(descriptor);
                break;
            }
        }

        modifiedDescriptors = true;
    }

    public Collection<ProjectDescriptor> getProjectDescriptors() {
        return getDescriptors();
    }

    public void setProjectDescriptors(Collection<ProjectDescriptor> projectDescriptors) throws ProjectException {
        tryLockOrThrow();

        getDescriptors().clear();
        getDescriptors().addAll(projectDescriptors);

        modifiedDescriptors = true;
    }

    @Override
    public LockInfo getLockInfo() {
        if (lockEngine == null) {
            return LockInfo.NO_LOCK;
        }
        return lockEngine.getLockInfo(getDesignRepository().getId(), getBranch(), getName());
    }

    @Override
    public boolean tryLock() {
        if (lockEngine != null) {
            return lockEngine.tryLock(getDesignRepository().getId(), getBranch(), getName(), getUser().getUserName());
        }

        return super.tryLock();
    }

    @Override
    public void unlock() {
        if (lockEngine != null) {
            lockEngine.unlock(getDesignRepository().getId(), getBranch(), getName());
        }
    }

    @Override
    public void update(AProjectArtefact artefact, CommonUser user) throws ProjectException {
        ADeploymentProject deploymentProject = (ADeploymentProject) artefact;
        setProjectDescriptors(deploymentProject.getProjectDescriptors());
        save(user);
    }

    private List<ProjectDescriptor> getDescriptors() {
        synchronized (this) {
            try {
                FileData folder = getRepository().check(getFolderPath());
                if (folder != null) {
                    // Determine whether the deployment project has been modified by other OpenL Studio instances or
                    // manually, if so, then refresh it.
                    // do not refresh the project if it is open
                    Date modifiedAt = folder.getModifiedAt();
                    if (!isOpened() && !modifiedAt.equals(getFileData().getModifiedAt())) {
                        super.refresh();
                        setHistoryVersion(null);
                        descriptors = null;
                        createInternalArtefacts();
                    }
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
            if (descriptors == null) {
                descriptors = new ArrayList<>();
                ADeploymentProject source = openedVersion == null ? this : openedVersion;
                if (source.hasArtefact(ArtefactProperties.DESCRIPTORS_FILE)) {
                    InputStream content = null;
                    try {
                        content = ((AProjectResource) source.getArtefact(ArtefactProperties.DESCRIPTORS_FILE))
                                .getContent();
                        List<ProjectDescriptor> newDescriptors = new ProjectDescriptorSerializer().deserialize(content);
                        if (newDescriptors != null) {
                            descriptors = newDescriptors;
                        }
                    } catch (Exception e) {
                        LOG.error(e.getMessage(), e);
                    } finally {
                        IOUtils.closeQuietly(content);
                    }
                }
            }
            return descriptors;
        }

    }

    @Override
    public void refresh() {
        if (!isModified()) {
            super.refresh();
            descriptors = null;
        }
    }

    @Override
    public boolean isModified() {
        return modifiedDescriptors;
    }

    public static class Builder {
        private final Repository repository;
        private final String folderPath;
        private String version;
        private WorkspaceUser user;
        private LockEngine lockEngine;

        public Builder(Repository repository, String folderPath) {
            this.repository = Objects.requireNonNull(repository, "repository cannot be null");
            this.folderPath = Objects.requireNonNull(folderPath, "folderPath cannot be null");
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder user(WorkspaceUser user) {
            this.user = user;
            return this;
        }

        public Builder lockEngine(LockEngine lockEngine) {
            this.lockEngine = lockEngine;
            return this;
        }

        public ADeploymentProject build() {
            Objects.requireNonNull(user, "user cannot be null");
            Objects.requireNonNull(lockEngine, "lockEngine cannot be null");
            return new ADeploymentProject(user, repository, folderPath, version, lockEngine);
        }
    }
}
