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
import org.openl.rules.common.impl.RepositoryProjectVersionImpl;
import org.openl.rules.repository.api.ArtefactProperties;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.repository.api.ResourceAPI;

import static org.openl.rules.security.AccessManager.isGranted;
import static org.openl.rules.security.Privileges.*;

public class ADeploymentProject extends AProject {
    private List<ProjectDescriptor> descriptors;
    private ProjectVersion projectVersion;

    public ADeploymentProject(FolderAPI api, CommonUser user) {
        super(api, user);
        projectVersion = getLastVersion();
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

    @Override
    public void openVersion(CommonVersion version) throws ProjectException {
        FolderAPI openedProjectVersion = getAPI().getVersion(version);
        if (openedProjectVersion.hasArtefact(ArtefactProperties.DESCRIPTORS_FILE)) {
            InputStream content = null;
            try {
                content = ((ResourceAPI) openedProjectVersion.getArtefact(ArtefactProperties.DESCRIPTORS_FILE))
                        .getContent();
                descriptors = ProjectDescriptorHelper.deserialize(content);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                IOUtils.closeQuietly(content);
            }
        } else {
            descriptors = new ArrayList<ProjectDescriptor>();
        }
        projectVersion = openedProjectVersion.getVersion();
    }

    @Override
    public void close() throws ProjectException {
        if (isCheckedOut()) {
            getAPI().unlock(user);
        }
        openVersion(getLastVersion());
        refresh();
    }

    @Override
    public ProjectVersion getVersion() {
        return projectVersion;
    }

    @Override
    public boolean isOpened() {
        return !projectVersion.equals(getLastVersion()) || isCheckedOut();
    }

    /** is opened other version? (not last) */
    public boolean isOpenedOtherVersion() {
        ProjectVersion max = getLastVersion();
        if (max == null) {
            return false;
        }
        return (!getVersion().equals(max));
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
                if (hasArtefact(ArtefactProperties.DESCRIPTORS_FILE)) {
                    ((AProjectResource) getArtefact(ArtefactProperties.DESCRIPTORS_FILE))
                            .setContent(new ByteArrayInputStream(descriptorsAsString.getBytes("UTF-8")));
                } else {
                    addResource(ArtefactProperties.DESCRIPTORS_FILE,
                            new ByteArrayInputStream(descriptorsAsString.getBytes("UTF-8")));
                }
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        getAPI().commit(user, major, minor);
        close();
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
    public void update(AProjectArtefact artefact) throws ProjectException {
        // super.update(artefact); TODO
        ADeploymentProject deploymentProject = (ADeploymentProject) artefact;
        setProjectDescriptors(deploymentProject.getProjectDescriptors());
    }

    public boolean getCanDeploy() {
        return (!isCheckedOut() && isGranted(PRIVILEGE_DEPLOY));
    }

    private List<ProjectDescriptor> getDescriptors() {
        if (descriptors == null) {
            descriptors = new ArrayList<ProjectDescriptor>();
            if (hasArtefact(ArtefactProperties.DESCRIPTORS_FILE)) {
                InputStream content = null;
                try {
                    content = ((AProjectResource) getArtefact(ArtefactProperties.DESCRIPTORS_FILE)).getContent();
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
}
