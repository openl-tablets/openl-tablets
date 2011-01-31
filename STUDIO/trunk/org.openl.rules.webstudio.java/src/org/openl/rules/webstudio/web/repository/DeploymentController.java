package org.openl.rules.webstudio.web.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.common.impl.ProjectDescriptorImpl;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.uw.UserWorkspace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.model.SelectItem;

/**
 * Deployment controller.
 *
 * @author Andrey Naumenko
 */
public class DeploymentController {
    private static final Log LOG = LogFactory.getLog(DeploymentController.class);
    private List<DeploymentDescriptorItem> items;
    private String projectName;
    private String version;
    private RepositoryTreeState repositoryTreeState;
    private String cachedForProject;
    private DeploymentManager deploymentManager;

    public synchronized String addItem() {
        ADeploymentProject project = getSelectedProject();

        ProjectDescriptorImpl newItem = new ProjectDescriptorImpl(projectName, new CommonVersionImpl(version));
        List<ProjectDescriptor> newDescriptors = replaceDescriptor(project, projectName, newItem);

        try {
            project.setProjectDescriptors(newDescriptors);
        } catch (ProjectException e) {
            LOG.error("Failed to add project descriptor!", e);
            FacesUtils.addErrorMessage("failed to add project descriptor", e.getMessage());
        }

        return null;
    }

    private void checkConflicts(List<DeploymentDescriptorItem> items) throws ProjectException {
        if (items == null) {
            return;
        }

        DependencyChecker checker = new DependencyChecker();
        ADeploymentProject project = getSelectedProject();
        checker.addProjects(project);
        checker.check(items);
    }

    public String checkIn() {
        try {
            getSelectedProject().checkIn();
            items = null;
        } catch (ProjectException e) {
            LOG.error("Failed to check-in!", e);
            FacesUtils.addErrorMessage("failed to check in", e.getMessage());
        }

        return null;
    }

    public String checkOut() {
        try {
            getSelectedProject().checkOut();
            items = null;
        } catch (ProjectException e) {
            LOG.error("Failed to check-out!", e);
            FacesUtils.addErrorMessage("failed to check out", e.getMessage());
        }

        return null;
    }

    public String close() {
        try {
            getSelectedProject().close();
            items = null;
        } catch (ProjectException e) {
            LOG.error("Failed to close!", e);
            FacesUtils.addErrorMessage("failed to close deployment project", e.getMessage());
        }

        return null;
    }

    public String deleteItem() {
        String projectName = FacesUtils.getRequestParameter("key");
        ADeploymentProject project = getSelectedProject();

        try {
            project.setProjectDescriptors(replaceDescriptor(project, projectName, null));
        } catch (ProjectException e) {
            LOG.error("Failed to delete project descriptor!", e);
            FacesUtils.addErrorMessage("failed to add project descriptor", e.getMessage());
        }
        return null;
    }

    public String deploy() {
        ADeploymentProject project = getSelectedProject();
        if (project != null) {
            try {
                DeployID id = deploymentManager.deploy(project);
                FacesUtils.addInfoMessage("Project '" + project.getName()
                                + "' successfully deployed with id: " + id.getName());
            } catch (Exception e) {
                String msg = "Failed to deploy '" + project.getName() + "'";
                LOG.error(msg, e);
                FacesUtils.addErrorMessage(msg, e.getMessage());
            }
        }
        return null;
    }

    public synchronized List<DeploymentDescriptorItem> getItems() {
        ADeploymentProject project = getSelectedProject();
        if (project == null) {
            return null;
        }

        String projectNameWithVersion = project.getName() + project.getVersion().getVersionName();
        if (items != null && projectNameWithVersion.equals(cachedForProject)) {
            return items;
        }

        cachedForProject = projectNameWithVersion;
        Collection<ProjectDescriptor> descriptors = project.getProjectDescriptors();
        items = new ArrayList<DeploymentDescriptorItem>();

        for (ProjectDescriptor descriptor : descriptors) {
            DeploymentDescriptorItem item = new DeploymentDescriptorItem(descriptor.getProjectName(), descriptor
                    .getProjectVersion());
            items.add(item);
        }

        try {
            checkConflicts(items);
        } catch (ProjectException e) {
            LOG.error("Failed to check conflicts!", e);
            FacesUtils.addErrorMessage(e.getMessage());
        }

        return items;
    }

    public String getProjectName() {
        return projectName;
    }

    public SelectItem[] getProjects() {
        UserWorkspace workspace = RepositoryUtils.getWorkspace();
        Collection<RulesProject> workspaceProjects = workspace.getProjects();
        List<SelectItem> selectItems = new ArrayList<SelectItem>();

        List<DeploymentDescriptorItem> existingItems = getItems();
        Set<String> existing = new HashSet<String>();
        if (existingItems != null) {
            for (DeploymentDescriptorItem ddItem : existingItems) {
                existing.add(ddItem.getName());
            }
        }

        for (RulesProject project : workspaceProjects) {
            if (!(existing.contains(project.getName()) || project.isLocalOnly())) {
                selectItems.add(new SelectItem(project.getName()));
            }
        }

        return selectItems.toArray(new SelectItem[selectItems.size()]);
    }

    public SelectItem[] getProjectVersions() {
        UserWorkspace workspace = RepositoryUtils.getWorkspace();
        if (projectName != null) {
            try {
                AProject project = workspace.getProject(projectName);
                // sort project versions in descending order (1.1 -> 0.0)
                List<ProjectVersion> versions = new ArrayList<ProjectVersion>(project.getVersions());
                Collections.sort(versions, RepositoryUtils.VERSIONS_REVERSE_COMPARATOR);

                List<SelectItem> selectItems = new ArrayList<SelectItem>();
                for (ProjectVersion version : versions) {
                    selectItems.add(new SelectItem(version.getVersionName()));
                }
                return selectItems.toArray(new SelectItem[selectItems.size()]);
            } catch (ProjectException e) {
                LOG.error("Failed to get project versions!", e);
            }
        }
        return new SelectItem[0];
    }

    private ADeploymentProject getSelectedProject() {
        AProjectArtefact artefact = repositoryTreeState.getSelectedNode().getData();
        if (artefact instanceof ADeploymentProject) {
            return (ADeploymentProject) artefact;
        }
        return null;
    }

    public String getVersion() {
        return version;
    }

    public boolean isCheckinable() {
        return true;
    }

    public boolean isCheckoutable() {
        return true;
    }

    public String openSelectedProjects() {
        UserWorkspace workspace = RepositoryUtils.getWorkspace();
        for (DeploymentDescriptorItem item : items) {
            if (item.isSelected()) {
                String projectName = item.getName();
                try {
                    RulesProject project = workspace.getProject(projectName);
                    if (!project.isCheckedOut()) {
                        project.openVersion(item.getVersion());
                    }
                } catch (ProjectException e) {
                    LOG.error("Failed to open project '" + projectName + "'!", e);
                }
            }
            item.setSelected(false);
        }
        return null;
    }

    private List<ProjectDescriptor> replaceDescriptor(ADeploymentProject project, String projectName,
            ProjectDescriptorImpl newItem) {
        List<ProjectDescriptor> newDescriptors = new ArrayList<ProjectDescriptor>();

        for (ProjectDescriptor pd : project.getProjectDescriptors()) {
            if (pd.getProjectName().equals(projectName)) {
                if (newItem != null) {
                    newDescriptors.add(newItem);
                    newItem = null;
                }
            } else {
                newDescriptors.add(pd);
            }
        }
        if (newItem != null) {
            newDescriptors.add(newItem);
        }
        items = null;
        return newDescriptors;
    }

    public void setDeploymentManager(DeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setRepositoryTreeState(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
