package org.openl.rules.project.abstraction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.openl.rules.common.*;
import org.openl.rules.common.impl.ProjectDescriptorImpl;
import org.openl.rules.common.impl.RepositoryProjectVersionImpl;
import org.openl.rules.repository.api.ArtefactProperties;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class stores only deployment configuration, not deployed projects!
 * For the latter see {@link Deployment} class.
 */
public class ADeploymentProject extends UserWorkspaceProject {
    private final Logger log = LoggerFactory.getLogger(ADeploymentProject.class);

    private List<ProjectDescriptor> descriptors;
    private ADeploymentProject openedVersion;
    /* this button is used for rendering the save button (only for deploy configuration)*/
    private boolean modifiedDescriptors = false;

    public ADeploymentProject(WorkspaceUser user, Repository repository, String folderPath, String version) {
        super(user, repository, folderPath, version, false);
    }

    public ADeploymentProject(WorkspaceUser user, Repository repository, FileData fileData) {
        super(user, repository, fileData, false);
    }

    @Override
    public boolean isFolder() {
        return false;
    }

    public ProjectDescriptor addProjectDescriptor(String name, CommonVersion version) throws ProjectException {
        if (hasProjectDescriptor(name)) {
            removeProjectDescriptor(name);
        }
        ProjectDescriptorImpl projectDescriptor = new ProjectDescriptorImpl(name, version);
        getDescriptors().add(projectDescriptor);
        return projectDescriptor;
    }

    public boolean hasProjectDescriptor(String name) throws ProjectException {
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
        throw new ProjectException(String.format("Project descriptor with name \"%s\" is not found", name));
    }

    public void openVersion(String version) throws ProjectException {
        modifiedDescriptors = false;
        openedVersion = new ADeploymentProject(getUser(), getRepository(), getFolderPath(), version);
        openedVersion.setHistoryVersion(version);
        setHistoryVersion(version);
        refresh();
    }

    @Override
    public void close(CommonUser user) throws ProjectException {
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
            setFileData(getRepository().save(fileData, new ByteArrayInputStream(out.toByteArray())));
        } catch (IOException e) {
            throw new ProjectException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(zipOutputStream);
        }

        modifiedDescriptors = false;
        open();
    }

    public void removeProjectDescriptor(String name) throws ProjectException {
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
        getDescriptors().clear();
        getDescriptors().addAll(projectDescriptors);

        modifiedDescriptors = true;
    }

    @Override
    public void update(AProjectArtefact artefact, CommonUser user) throws ProjectException {
        ADeploymentProject deploymentProject = (ADeploymentProject) artefact;
        setProjectDescriptors(deploymentProject.getProjectDescriptors());
        save(user);
    }

    private List<ProjectDescriptor> getDescriptors() {
        if (descriptors == null) {
            descriptors = new ArrayList<ProjectDescriptor>();
            ADeploymentProject source = openedVersion == null ? this : openedVersion;
            if (source.hasArtefact(ArtefactProperties.DESCRIPTORS_FILE)) {
                InputStream content = null;
                try {
                    content = ((AProjectResource) source.getArtefact(ArtefactProperties.DESCRIPTORS_FILE)).getContent();
                    descriptors = ProjectDescriptorHelper.deserialize(content);
                    if (descriptors == null) {
                        descriptors = new ArrayList<ProjectDescriptor>();
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
        descriptors = null;
        modifiedDescriptors = false;
    }

    @Override
    public boolean isModified() {
        return modifiedDescriptors;
    }
}
