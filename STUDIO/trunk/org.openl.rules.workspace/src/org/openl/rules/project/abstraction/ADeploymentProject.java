package org.openl.rules.project.abstraction;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.openl.rules.common.CommonUser;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.common.ProjectDescriptor.ProjectDescriptorHelper;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.impl.ProjectDescriptorImpl;
import org.openl.rules.repository.api.ArtefactProperties;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.workspace.WorkspaceUser;

public class ADeploymentProject extends UserWorkspaceProject {
    private List<ProjectDescriptor> descriptors;
    private ADeploymentProject openedVersion;

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
        for (ProjectDescriptor descriptor : getProjectDescriptors()) {
            if (descriptor.getProjectName().equals(name)) {
                return true;
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
        throw new ProjectException(String.format("Project descriptor with name \"%s\" is not found"));
    }

    
    
    
    public void openVersion(CommonVersion version) throws ProjectException {
        FolderAPI openedProjectVersion = getAPI().getVersion(version);
        openedVersion = new ADeploymentProject(openedProjectVersion, getUser());
        refresh();
    }

    @Override
    public void close(CommonUser user) throws ProjectException {
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
        return openedVersion != null || isCheckedOut();
    }

    public void checkOut(CommonUser user) throws ProjectException {
        super.checkOut(user);
        open();
    }

    @Override
    public void checkIn(CommonUser user, int major, int minor) throws ProjectException {
        if (CollectionUtils.isEmpty(descriptors)) {
            if (hasArtefact(ArtefactProperties.DESCRIPTORS_FILE)) {
                getArtefact(ArtefactProperties.DESCRIPTORS_FILE).delete();
            }
        } else {
            String descriptorsAsString = ProjectDescriptorHelper.serialize(descriptors);
            try {
                AProjectResource resource;
                if (hasArtefact(ArtefactProperties.DESCRIPTORS_FILE)) {
                    resource = ((AProjectResource) getArtefact(ArtefactProperties.DESCRIPTORS_FILE));
                    resource.setContent(new ByteArrayInputStream(descriptorsAsString.getBytes("UTF-8")));
                } else {
                    resource = addResource(ArtefactProperties.DESCRIPTORS_FILE, new ByteArrayInputStream(
                            descriptorsAsString.getBytes("UTF-8")));
                }
                resource.save(user, major, minor);
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        super.checkIn(user, major, minor);
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
    }

    public Collection<ProjectDescriptor> getProjectDescriptors() {
        return getDescriptors();
    }

    public void setProjectDescriptors(Collection<ProjectDescriptor> projectDescriptors) throws ProjectException {
        getDescriptors().clear();
        getDescriptors().addAll(projectDescriptors);
    }

    @Override
    public void update(AProjectArtefact artefact, CommonUser user, int major, int minor) throws ProjectException {
        ADeploymentProject deploymentProject = (ADeploymentProject) artefact;
        setProjectDescriptors(deploymentProject.getProjectDescriptors());
        checkIn(user, major, minor);
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
                    // TODO Auto-generated catch block
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

    public boolean getCanDeploy() {
        return !isCheckedOut();
    }
}
