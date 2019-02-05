package org.openl.rules.project.abstraction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.openl.rules.common.*;
import org.openl.rules.common.impl.ProjectDescriptorImpl;
import org.openl.rules.common.impl.RepositoryProjectVersionImpl;
import org.openl.rules.repository.api.*;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.impl.LockInfoImpl;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class stores only deploy configuration, not deployed projects!
 * For the latter see {@link Deployment} class.
 */
public class ADeploymentProject extends UserWorkspaceProject {
    private final Logger log = LoggerFactory.getLogger(ADeploymentProject.class);

    private List<ProjectDescriptor> descriptors;
    private ADeploymentProject openedVersion;
    /* this button is used for rendering the save button (only for deploy configuration)*/
    private boolean modifiedDescriptors = false;

    private final LockEngine lockEngine;

    private ADeploymentProject(WorkspaceUser user,
            Repository repository,
            String folderPath,
            String version,
            LockEngine lockEngine) {
        super(user, repository, folderPath, version);
        this.lockEngine = lockEngine;
    }

    public ADeploymentProject(WorkspaceUser user,
            Repository repository,
            FileData fileData,
            LockEngine lockEngine) {
        super(user, repository, fileData);
        this.lockEngine = lockEngine;
    }

    public ADeploymentProject(Repository repository, FileData fileData) {
        super(null, repository, fileData);
        lockEngine = null;
    }

    public void addProjectDescriptor(String name, CommonVersion version) {
        if (hasProjectDescriptor(name)) {
            removeProjectDescriptor(name);
        }
        getDescriptors().add(new ProjectDescriptorImpl(name, version));
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
        throw new ProjectException(String.format("Project descriptor '%s' is not found", name));
    }

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
                return new RepositoryProjectVersionImpl(getHistoryVersion(), null);

            }
        } else {
            return openedVersion.getVersion();
        }
    }

    public boolean isOpened() {
        return openedVersion != null; //|| isOpenedForEditing();
    }

    @Override
    public void save(CommonUser user) throws ProjectException {
        InputStream inputStream = ProjectDescriptorHelper.serialize(descriptors);
        if (getRepository() instanceof FolderRepository) {
            FileData fileData = getFileData();
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                IOUtils.copyAndClose(inputStream, out);

                fileData.setAuthor(user == null ? null : user.getUserName());
                fileData.setSize(out.size());

                FileChange change = new FileChange(fileData.getName() + "/" + ArtefactProperties.DESCRIPTORS_FILE,
                        new ByteArrayInputStream(out.toByteArray()));
                setFileData(((FolderRepository) getRepository()).save(fileData, Collections.singletonList(change)));
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

                IOUtils.copy(inputStream, zipOutputStream);

                inputStream.close();
                zipOutputStream.closeEntry();

                zipOutputStream.close();
                fileData.setAuthor(user == null ? null : user.getUserName());
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
        lock();

        getDescriptors().clear();
        getDescriptors().addAll(projectDescriptors);

        modifiedDescriptors = true;
    }

    @Override
    public LockInfo getLockInfo() {
        if (lockEngine == null) {
            return LockInfoImpl.NO_LOCK;
        }
        return lockEngine.getLockInfo(getName());
    }

    @Override
    public void lock() throws ProjectException {
        if (lockEngine != null) {
            lockEngine.lock(getName(), getUser().getUserName());
        }
    }

    @Override
    public void unlock() {
        if (lockEngine != null) {
            lockEngine.unlock(getName());
        }
    }

    @Override
    public void update(AProjectArtefact artefact, CommonUser user) throws ProjectException {
        ADeploymentProject deploymentProject = (ADeploymentProject) artefact;
        setProjectDescriptors(deploymentProject.getProjectDescriptors());
        save(user);
    }

    private List<ProjectDescriptor> getDescriptors() {
        if (descriptors == null) {
            descriptors = new ArrayList<>();
            ADeploymentProject source = openedVersion == null ? this : openedVersion;
            if (source.hasArtefact(ArtefactProperties.DESCRIPTORS_FILE)) {
                InputStream content = null;
                try {
                    content = ((AProjectResource) source.getArtefact(ArtefactProperties.DESCRIPTORS_FILE)).getContent();
                    descriptors = ProjectDescriptorHelper.deserialize(content);
                    if (descriptors == null) {
                        descriptors = new ArrayList<>();
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                } finally {
                    IOUtils.closeQuietly(content);
                }
            }
        }
        return descriptors;
    }

    @Override
    public void refresh() {
        super.refresh();
        descriptors = null;
        modifiedDescriptors = false;
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
            if (repository == null) {
                throw new IllegalArgumentException("Repository must be not null");
            }

            if (folderPath == null) {
                throw new IllegalArgumentException("folderPath must be not null");
            }

            this.repository = repository;
            this.folderPath = folderPath;
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
            if (user == null) {
                throw new IllegalArgumentException("user must be not null");
            }
            if (lockEngine == null) {
                throw new IllegalArgumentException("lockEngine must be not null");
            }

            return new ADeploymentProject(user, repository, folderPath, version, lockEngine);
        }
    }
}
