package org.openl.rules.project.abstraction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openl.rules.project.impl.ProjectArtefactAPI;
import org.openl.rules.project.impl.RepositoryAPI;
import org.openl.rules.project.impl.UserWorkspaceAPI;
import org.openl.rules.repository.CommonUser;
import org.openl.rules.repository.CommonVersion;
import org.openl.rules.workspace.abstracts.ProjectDescriptor;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.uw.impl.ProjectDescriptorImpl;

import static org.openl.rules.security.SecurityUtils.isGranted;
import static org.openl.rules.security.Privileges.*;

public class ADeploymentProject extends AProject {
    private List<ProjectDescriptor> descriptors;
    private ProjectVersion projectVersion;

    public ADeploymentProject(ProjectArtefactAPI api, CommonUser user) {
        super(api, user);
        projectVersion = getLastVersion();
    }

    public ProjectDescriptor addProjectDescriptor(String name, CommonVersion version) throws ProjectException {
        ProjectDescriptorImpl projectDescriptor = new ProjectDescriptorImpl(name, version);
        getDescriptors().add(projectDescriptor);
        return projectDescriptor;
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
        // FIXME
        ADeploymentProject ddProject = ((RepositoryAPI) impl).getRepository().getDDProject(getName(), version);
        getDescriptors().clear();
        getDescriptors().addAll(ddProject.getProjectDescriptors());
        projectVersion = ddProject.getVersion();
    }

    @Override
    public void close() throws ProjectException {
        if (isCheckedOut()) {
            impl.unlock(user);
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
        if(max == null){
            return false;
        }
        return (!getVersion().equals(max));
    }

    @Override
    public void checkIn(int major, int minor) throws ProjectException {
        impl.setProjectDescriptors(getDescriptors());
        impl.commit(user, major, minor);
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
        super.update(artefact);
        ADeploymentProject deploymentProject = (ADeploymentProject) artefact;
        setProjectDescriptors(deploymentProject.getProjectDescriptors());
    }

    public boolean getCanDeploy() {
        return (!isCheckedOut() && isGranted(PRIVILEGE_DEPLOY));
    }

    private List<ProjectDescriptor> getDescriptors() {
        if (descriptors == null) {
            descriptors = new ArrayList<ProjectDescriptor>();
            descriptors.addAll(impl.getProjectDescriptors());
        }
        return descriptors;
    }

    @Override
    public void refresh() {
        descriptors = null;
    }
}
