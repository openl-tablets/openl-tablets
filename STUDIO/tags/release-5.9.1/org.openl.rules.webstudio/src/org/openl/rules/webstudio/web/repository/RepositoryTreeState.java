package org.openl.rules.webstudio.web.repository;

import static org.openl.rules.security.AccessManager.isGranted;
import static org.openl.rules.security.Privileges.PRIVILEGE_DELETE;
import static org.openl.rules.security.Privileges.PRIVILEGE_DEPLOY;
import static org.openl.rules.security.Privileges.PRIVILEGE_EDIT;
import static org.openl.rules.security.Privileges.PRIVILEGE_ERASE;
import static org.openl.rules.security.Privileges.PRIVILEGE_READ;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.webstudio.web.repository.tree.TreeNode;
import org.openl.rules.webstudio.web.repository.tree.TreeDProject;
import org.openl.rules.webstudio.web.repository.tree.TreeFile;
import org.openl.rules.webstudio.web.repository.tree.TreeFolder;
import org.openl.rules.webstudio.web.repository.tree.TreeProject;
import org.openl.rules.webstudio.web.repository.tree.TreeRepository;
import org.openl.rules.workspace.dtr.DesignTimeRepositoryListener;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.filter.IFilter;
import org.openl.util.filter.AllFilter;
import org.richfaces.component.UITree;

import org.richfaces.event.TreeSelectionChangeEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

/**
 * Used for holding information about rulesRepository tree.
 *
 * @author Andrey Naumenko
 */
@ManagedBean
@SessionScoped
public class RepositoryTreeState implements DesignTimeRepositoryListener{

    private static final Log LOG = LogFactory.getLog(RepositoryTreeState.class);
    private static IFilter<AProjectArtefact> ALL_FILTER = new AllFilter<AProjectArtefact>();

    /** Root node for RichFaces's tree. It is not displayed. */
    private TreeRepository root;
    private TreeNode selectedNode;
    private TreeRepository rulesRepository;
    private TreeRepository deploymentRepository;

    @ManagedProperty(value="#{rulesUserSession.userWorkspace}")
    private UserWorkspace userWorkspace;

    private IFilter<AProjectArtefact> filter = ALL_FILTER;
    private boolean hideDeleted = true;

    public Boolean adviseNodeSelected(UITree uiTree) {
        /*TreeNode node = (TreeNode) uiTree.getTreeNode();

        AProjectArtefact projectArtefact = node.getData();
        AProjectArtefact selected = null;
		if (selectedNode != null) {
            selected = selectedNode.getData();
		}else{
			return false;
		}

        if ((selected == null) || (projectArtefact == null)) {
            return selectedNode.getId().equals(node.getId());
        }

        if (selected.getArtefactPath().equals(projectArtefact.getArtefactPath())) {
            if (projectArtefact instanceof DeploymentDescriptorProject) {
                return selected instanceof DeploymentDescriptorProject;
            }
            return true;
        }*/
        return false;
    }

    private void buildTree() {
        if (root != null) {
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Starting buildTree()");
        }

        root = new TreeRepository("", "", filter, "root");

        String rpName = "Projects";
        rulesRepository = new TreeRepository(rpName, rpName, filter, UiConst.TYPE_REPOSITORY);
        rulesRepository.setData(null);

        String dpName = "Deployments";
        deploymentRepository = new TreeRepository(dpName, dpName, filter, UiConst.TYPE_DEPLOYMENT_REPOSITORY);
        deploymentRepository.setData(null);

        //Such keys are used for correct order of repositories.
        root.addChild("1st - Projects", rulesRepository);
        root.addChild("2nd - Deployments", deploymentRepository);

        Collection<RulesProject> rulesProjects = userWorkspace.getProjects();

        IFilter<AProjectArtefact> filter = this.filter;
        for (AProject project : rulesProjects) {
            if (!(filter.supports(RulesProject.class) && !filter.select(project))) {
                addRulesProjectToTree(project);
            }
        }

        List<ADeploymentProject> deploymentsProjects = null;

        try {
            deploymentsProjects = userWorkspace.getDDProjects();
        } catch (ProjectException e) {
            LOG.error("Cannot get deployment projects", e);
        }

        for (ADeploymentProject project : deploymentsProjects) {
            addDeploymentProjectToTree(project);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Finishing buildTree()");
        }

        if (selectedNode == null || UiConst.TYPE_REPOSITORY.equals(selectedNode.getType())) {
            selectedNode = rulesRepository;
        } else if (UiConst.TYPE_DEPLOYMENT_REPOSITORY.equals(selectedNode.getType())) {
            selectedNode = deploymentRepository;
        } else {
            updateSelectedNode();
        }
    }
    
    public TreeRepository getDeploymentRepository() {
        buildTree();
        return deploymentRepository;
    }

    public IFilter<AProjectArtefact> getFilter() {
        return filter;
    }

    public TreeRepository getRoot() {
        buildTree();
        return root;
    }

    public TreeRepository getRulesRepository() {
        buildTree();
        return rulesRepository;
    }

    public TreeNode getSelectedNode() {
        buildTree();
        return selectedNode;
    }

    public UserWorkspaceProject getSelectedProject() {
        AProjectArtefact artefact = getSelectedNode().getData();
        if (artefact instanceof UserWorkspaceProject) {
            return (UserWorkspaceProject) artefact;
        }
        return null;
    }

    public void invalidateSelection() {
        selectedNode = rulesRepository;
    }
    
    /**
     * Refreshes repositoryTreeState.selectedNode after rebuilding tree.
     */
    public void updateSelectedNode() {
        Iterator<String> it = getSelectedNode().getData().getArtefactPath().getSegments().iterator();
        TreeNode currentNode = getRulesRepository();
        while ((currentNode != null) && it.hasNext()) {
            currentNode = (TreeNode) currentNode.getChild(it.next());
        }

        if (currentNode != null) {
            selectedNode = currentNode;
        }
    }

    public void refreshNode(TreeNode node){
        node.refresh();
    }

    public void deleteNode(TreeNode node){
        node.getParent().removeChild(node.getId());
    }

    public void deleteSelectedNodeFromTree(){
        deleteNode(selectedNode);
        moveSelectionToParentNode();
    }
    
    public void addDeploymentProjectToTree(ADeploymentProject project) {
        String name = project.getName();
        String id = String.valueOf(name.hashCode());
        if (!project.isDeleted() || !hideDeleted) {
            TreeDProject prj = new TreeDProject(id, name);
            prj.setData(project);
            deploymentRepository.add(prj);
        }
    }

    public void addRulesProjectToTree(AProject project) {
        String name = project.getName();
        String id = String.valueOf(name.hashCode());
        if (!project.isDeleted() || !hideDeleted) {
            TreeProject prj = new TreeProject(id, name, filter);
            prj.setData(project);
            rulesRepository.add(prj);
        }
    }

    public void addNodeToTree(TreeNode parent, AProjectArtefact childArtefact) {
        String name = childArtefact.getName();
        String id = String.valueOf(name.hashCode());
        if (childArtefact.isFolder()) {
            TreeFolder treeFolder = new TreeFolder(id, name, filter);
            treeFolder.setData(childArtefact);
            parent.add(treeFolder);
        } else {
            TreeFile treeFile = new TreeFile(id, name);
            treeFile.setData(childArtefact);
            parent.add(treeFile);
        }
    }

    /**
     * Forces tree rebuild during next access.
     */
    public void invalidateTree() {
        invalidateSelection();
        root = null;
    }
    
    /**
     * Moves selection to the parent of the current selected node.
     */
    public void moveSelectionToParentNode() {
        if (selectedNode.getParent() instanceof TreeNode) {
            selectedNode = selectedNode.getParent();
        } else {
            invalidateSelection();
        }
    }

    public void processSelection(TreeSelectionChangeEvent event) {
        List<Object> selection = new ArrayList<Object>(event.getNewSelection());
        Object currentSelectionKey = selection.get(0);
        UITree tree = (UITree) event.getSource();

        Object storedKey = tree.getRowKey();
        tree.setRowKey(currentSelectionKey);
        selectedNode = (TreeNode) tree.getRowData();
        tree.setRowKey(storedKey);
    }

    /**
     * Refreshes repositoryTreeState.selectedNode.
     */
    public void refreshSelectedNode() {
        refreshNode(selectedNode);
    }

    public void setFilter(IFilter<AProjectArtefact> filter) {
        this.filter = filter != null ? filter : ALL_FILTER;
        root = null;
    }

    public void setRoot(TreeRepository root) {
        this.root = root;
    }

    public void setSelectedNode(TreeNode selectedNode) {
        this.selectedNode = selectedNode;
    }

    public void setUserWorkspace(UserWorkspace userWorkspace) {
        this.userWorkspace = userWorkspace;
        userWorkspace.getDesignTimeRepository().addListener(this);
    }


    public void onRulesProjectModified(DTRepositoryEvent event) {
        String projectName = event.getProjectName();
        TreeNode rulesProject = getRulesRepository().getChild(projectName);
        if (rulesProject == null) {
            if (userWorkspace.getDesignTimeRepository().hasProject(projectName)) {
                try {
                    addRulesProjectToTree(userWorkspace.getProject(projectName));
                } catch (ProjectException e) {
                    LOG.error("Failed to add new project to the repository tree.", e);
                }
            }
        } else if (!userWorkspace.getDesignTimeRepository().hasProject(projectName)
                && !userWorkspace.getLocalWorkspace().hasProject(projectName)) {
            deleteNode(rulesProject);
        } else {
            try {
                rulesProject.setData(userWorkspace.getProject(projectName));
                refreshNode(rulesProject);
            } catch (ProjectException e) {
                LOG.error(String.format("Failed to refresh project \"%s\" in the repository tree.", projectName), e);
            }
        }
    }

    public void onDeploymentProjectModified(DTRepositoryEvent event) {
        TreeNode deploymentProject = getDeploymentRepository().getChild(event.getProjectName());
        if(deploymentProject == null){
            if(userWorkspace.getDesignTimeRepository().hasDDProject(event.getProjectName())){
                try {
                    addDeploymentProjectToTree(userWorkspace.getDDProject(event.getProjectName()));
                } catch (ProjectException e) {
                    LOG.error("Failed to add new project to the repository tree.", e);
                }
            }
        }else{
            if (!userWorkspace.getDesignTimeRepository().hasDDProject(event.getProjectName())) {
                deleteNode(deploymentProject);
            } else {
                refreshNode(deploymentProject);
            }
        }
    }
    
    public boolean isHideDeleted() {
        return hideDeleted;
    }

    public void setHideDeleted(boolean hideDeleted) {
        this.hideDeleted = hideDeleted;
    }

    //for any project
    public boolean getCanCheckOut() {
        UserWorkspaceProject selectedProject = getSelectedProject();
        if (selectedProject.isLocalOnly() || selectedProject.isCheckedOut() || selectedProject.isLocked()) {
            return false;
        }

        return isGranted(PRIVILEGE_EDIT);
    }

    public boolean getCanCheckIn() {
        UserWorkspaceProject selectedProject = getSelectedProject();
        return selectedProject.isCheckedOut() /*&& selectedProject.isModified()*/ && isGranted(PRIVILEGE_EDIT);
    }

    public boolean getCanClose() {
        UserWorkspaceProject selectedProject = getSelectedProject();
        return selectedProject.isLockedByMe() || (!selectedProject.isLocalOnly() && selectedProject.isOpened());
    }

    public boolean getCanDelete() {
        UserWorkspaceProject selectedProject = getSelectedProject();
        if (selectedProject.isLocalOnly()) {
            // any user can delete own local project
            return true;
        }
        return (!selectedProject.isLocked() || selectedProject.isLockedByUser(userWorkspace.getUser())) && isGranted(PRIVILEGE_DELETE);
    }

    public boolean getCanErase() {
        return getSelectedProject().isDeleted() && isGranted(PRIVILEGE_ERASE);
    }

    public boolean getCanExport() {
        return getCanOpen();
    }

    public boolean getCanOpen() {
        UserWorkspaceProject selectedProject = getSelectedProject();
        if (selectedProject.isLocalOnly() || selectedProject.isCheckedOut()) {
            return false;
        }

        return isGranted(PRIVILEGE_READ);
    }

    public boolean getCanCompare() {
        if (getSelectedProject().isLocalOnly()) {
            return false;
        }
        return isGranted(PRIVILEGE_READ);
    }

    public boolean getCanRedeploy() {
        UserWorkspaceProject selectedProject = getSelectedProject();
        if (selectedProject.isLocalOnly() || selectedProject.isCheckedOut()) {
            return false;
        }

        return isGranted(PRIVILEGE_DEPLOY);
    }

    public boolean getCanUndelete() {
        return getSelectedProject().isDeleted() && isGranted(PRIVILEGE_EDIT);
    }


    //for any project artefact
    public boolean getCanModify() {
        AProjectArtefact selectedArtefact = selectedNode.getData();
        String projectName = selectedArtefact.getProject().getName();
        String projectId = String.valueOf(projectName.hashCode());
        RulesProject project = (RulesProject) getRulesRepository().getChild(projectId).getData();
        return (project.isCheckedOut() && isGranted(PRIVILEGE_EDIT));
    }

    //for deployment project
    public boolean getCanDeploy() {
        return !getSelectedProject().isCheckedOut() && isGranted(PRIVILEGE_DEPLOY);
    }

}
