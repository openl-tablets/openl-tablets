package org.openl.rules.webstudio.web.repository;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.repository.CommonVersionImpl;
import org.openl.rules.webstudio.web.repository.tree.AbstractTreeNode;
import org.openl.rules.workspace.abstracts.DeploymentDescriptorProject;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectDescriptor;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceDeploymentProject;
import org.openl.rules.workspace.uw.UserWorkspaceProject;

/**
 *
 * @author Aleh Bykhavets
 *
 */
public class SmartRedeployController {
    private static final Log LOG = LogFactory.getLog(SmartRedeployController.class);

    /** A controller which contains pre-built UI object tree. */
    private RepositoryTreeState repositoryTreeState;

    private List<DeploymentProjectItem> items;

    private DeploymentManager deploymentManager;

    public synchronized List<DeploymentProjectItem> getItems() {
        UserWorkspaceProject project = getSelectedProject();
        if (project == null) {
            items = null;
            return null;
        }

        items = getItems4Project(project);
        return items;
    }

    private List<DeploymentProjectItem> getItems4Project(UserWorkspaceProject project) {
        String projectName = project.getName();
        UserWorkspace workspace = RepositoryUtils.getWorkspace();

        List<DeploymentProjectItem> result = new LinkedList<DeploymentProjectItem>();

        // FIXME take latest deployment projects from DTR not from user scope
        // get all deployment projects
        List<AbstractTreeNode> nodes = repositoryTreeState.getDeploymentRepository().getChildNodes();
        for (AbstractTreeNode node : nodes) {
            ProjectArtefact artefact = node.getDataBean();
            if (!(artefact instanceof UserWorkspaceDeploymentProject)) {
                continue; // should never happen
            }

            UserWorkspaceDeploymentProject deploymentProject = (UserWorkspaceDeploymentProject) artefact;
            if (deploymentProject.isDeleted()) {
                continue; // don't check marked for deletion projects
            }

            DeploymentDescriptorProject latestDeploymentVersion = deploymentProject;
            if (deploymentProject.isOpenedOtherVersion()) {
                try {
                    latestDeploymentVersion = workspace.getDesignTimeRepository().getDDProject(
                            deploymentProject.getName());
                } catch (RepositoryException e) {
                    LOG.error("Failed to get latest version for deployment project '" + deploymentProject.getName()
                            + "'", e);
                }
            }

            ProjectDescriptor projectDescriptor = null;

            // check all descriptors
            // we are interested in all Deployment projects that has the project
            Collection<ProjectDescriptor> descriptors = latestDeploymentVersion.getProjectDescriptors();
            for (ProjectDescriptor descr : descriptors) {
                if (projectName.equals(descr.getProjectName())) {
                    projectDescriptor = descr;
                    break;
                }
            }

            if (projectDescriptor == null) {
                continue;
            }

            // create new item
            DeploymentProjectItem item = new DeploymentProjectItem();
            item.setName(deploymentProject.getName());

            CommonVersionImpl descrVersion = new CommonVersionImpl(projectDescriptor.getProjectVersion());
            int cmp = descrVersion.compareTo(project.getVersion());

            if (cmp == 0) {
                item.setDisabled(true);
                item.setMessages("Up to date");
            } else if (cmp < 0) {
                if (deploymentProject.isCheckedOut()) {
                    // prevent loosing of user's changes
                    item.setDisabled(true);
                    item.setMessages("Checked-out");
                    item.setStyleForMessages(UiConst.STYLE_WARNING);
                    item.setStyleForName(UiConst.STYLE_WARNING);
                } else if (deploymentProject.isLocked()) {
                    // won't be able to modify anyway
                    item.setDisabled(true);
                    item.setMessages("Locked by other user");
                    item.setStyleForMessages(UiConst.STYLE_WARNING);
                    item.setStyleForName(UiConst.STYLE_WARNING);
                } else {
                    DependencyChecker checker = new DependencyChecker();
                    // check against latest version of the deployment project
                    checker.addProjects(latestDeploymentVersion);
                    // overwrite settings
                    checker.addProject(project);
                    if (checker.check()) {
                        item.setMessages("Can be updated to " + project.getVersion().getVersionName() + " from "
                                + descrVersion.getVersionName());
                    } else {
                        item.setMessages("Has dependency conflict!");
                        item.setStyleForMessages(UiConst.STYLE_ERROR);
                    }
                }
            } else {
                item.setDisabled(true);
                item.setMessages("Deployment uses newer version " + descrVersion.getVersionName());
            }

            result.add(item);
        }

        if (!workspace.hasDDProject(projectName)) {
            // there is no deployment project with the same name...
            DeploymentProjectItem item = new DeploymentProjectItem();
            item.setName(projectName);
            item.setMessages("Create deployment project");
            item.setStyleForName(UiConst.STYLE_WARNING);

            // place it first
            result.add(0, item);
        }

        return result;
    }

    private UserWorkspaceProject getSelectedProject() {
        ProjectArtefact artefact = repositoryTreeState.getSelectedNode().getDataBean();
        if (artefact instanceof UserWorkspaceProject) {
            return (UserWorkspaceProject) artefact;
        }
        return null;
    }

    public String redeploy() {
        UserWorkspaceProject project = getSelectedProject();
        if (project == null) {
            return UiConst.OUTCOME_FAILURE;
        }

        List<UserWorkspaceDeploymentProject> successfulyUpdated = new LinkedList<UserWorkspaceDeploymentProject>();
        // update selected deployment projects
        for (DeploymentProjectItem item : items) {
            if (!item.isSelected()) {
                continue;
            }

            UserWorkspaceDeploymentProject deploymentProject = update(item.getName(), project);
            if (deploymentProject != null) {
                // OK, it was updated
                successfulyUpdated.add(deploymentProject);
            }
        }

        // redeploy takes more time
        for (UserWorkspaceDeploymentProject deploymentProject : successfulyUpdated) {
            try {
                DeployID id = deploymentManager.deploy(deploymentProject);
                FacesContext.getCurrentInstance().addMessage(
                        null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Project '" + project.getName()
                                + "' successfully deployed with id: " + id.getName(), null));
            } catch (Exception e) {
                String msg = "Failed to deploy '" + project.getName() + "'";
                LOG.error(msg, e);
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, e.getMessage()));
            }
        }

        return UiConst.OUTCOME_SUCCESS;
    }

    public void setDeploymentManager(DeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
    }

    public void setRepositoryTreeState(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;
    }

    private UserWorkspaceDeploymentProject update(String deploymentName, UserWorkspaceProject project) {
        UserWorkspace workspace = RepositoryUtils.getWorkspace();
        try {
            if (deploymentName.equals(project.getName())) {
                // the same name
                if (!workspace.hasDDProject(deploymentName)) {
                    // create if absent
                    workspace.createDDProject(deploymentName);
                    repositoryTreeState.invalidateTree();
                }
            }

            // get latest version
            UserWorkspaceDeploymentProject deploymentProject = workspace.getDDProject(deploymentName);

            if (deploymentProject.isLocked()) {
                // someone else is locked it while we were thinking
                FacesContext.getCurrentInstance().addMessage(
                        null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Deployment project '" + deploymentName
                                + "' is locked by other user", null));
                return null;
            } else {
                deploymentProject.checkOut();

                // rewrite project->version
                deploymentProject.addProjectDescriptor(project.getName(), project.getVersion());

                deploymentProject.checkIn();

                FacesContext.getCurrentInstance().addMessage(
                        null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Deployment project '" + deploymentName
                                + "' successfully updated", null));
                return deploymentProject;
            }
        } catch (ProjectException e) {
            String msg = "Failed to update deployment project '" + deploymentName + "'";
            LOG.error(msg, e);
            FacesContext.getCurrentInstance()
                    .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
        }

        return null;
    }
}
