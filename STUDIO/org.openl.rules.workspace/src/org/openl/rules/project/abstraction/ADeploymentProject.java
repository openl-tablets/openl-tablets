package org.openl.rules.project.abstraction;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.common.ProjectDescriptorHelper;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.impl.ProjectDescriptorImpl;
import org.openl.rules.repository.api.ArtefactProperties;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ADeploymentProject extends UserWorkspaceProject {
    private final Logger log = LoggerFactory.getLogger(ADeploymentProject.class);

    private List<ProjectDescriptor> descriptors;
    private ADeploymentProject openedVersion;
    /* this button is used for rendering the save button (only for deploy configuration)*/
    private boolean modifiedDescriptors = false;

    public ADeploymentProject(FolderAPI api, WorkspaceUser user) {
        super(api, user);
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

    public void openVersion(CommonVersion version) throws ProjectException {
        modifiedDescriptors = false;
        FolderAPI openedProjectVersion = getAPI().getVersion(version);
        openedVersion = new ADeploymentProject(openedProjectVersion, getUser());
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
            return getAPI().getVersion();
        } else {
            return openedVersion.getVersion();
        }
    }

    public boolean isOpened() {
        return openedVersion != null || isOpenedForEditing();
    }

    public void edit(CommonUser user) throws ProjectException {
        modifiedDescriptors = false;
        super.edit(user);
        open();
    }

    @Override
    public void save(CommonUser user) throws ProjectException {
        if (descriptors == null || descriptors.isEmpty()) {
            if (hasArtefact(ArtefactProperties.DESCRIPTORS_FILE)) {
                deleteArtefact(ArtefactProperties.DESCRIPTORS_FILE);
            }
        } else {
            InputStream inputStream = ProjectDescriptorHelper.serialize(descriptors);
            AProjectResource resource;
            if (hasArtefact(ArtefactProperties.DESCRIPTORS_FILE)) {
                resource = ((AProjectResource) getArtefact(ArtefactProperties.DESCRIPTORS_FILE));
                resource.setContent(inputStream);
            } else {
                resource = addResource(ArtefactProperties.DESCRIPTORS_FILE, inputStream);
            }
            resource.commit(user);
        }

        modifiedDescriptors = false;
        super.save(user);
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
    }

    public boolean isModifiedDescriptors() {
        return modifiedDescriptors;
    }
}
