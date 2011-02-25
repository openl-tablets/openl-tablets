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
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.webstudio.web.repository.tree.AbstractTreeNode;
import org.openl.rules.webstudio.web.repository.tree.TreeDProject;
import org.openl.rules.webstudio.web.repository.tree.TreeFile;
import org.openl.rules.webstudio.web.repository.tree.TreeFolder;
import org.openl.rules.webstudio.web.repository.tree.TreeProject;
import org.openl.rules.webstudio.web.repository.tree.TreeRepository;
import org.openl.rules.workspace.abstracts.DeploymentDescriptorProject;
import org.openl.rules.workspace.dtr.DesignTimeRepositoryListener;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.filter.IFilter;
import org.openl.util.filter.AllFilter;
import org.richfaces.component.UITree;

import org.richfaces.event.NodeSelectedEvent;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * Used for holding information about rulesRepository tree.
 *
 * @author Andrey Naumenko
 */
public class RepositoryTreeState implements DesignTimeRepositoryListener{
    private static final Log LOG = LogFactory.getLog(RepositoryTreeState.class);

    /** Root node for RichFaces's tree. It is not displayed. */
    private TreeRepository root;
    private AbstractTreeNode selectedNode;
    private TreeRepository rulesRepository;
    private TreeRepository deploymentRepository;
    private UserWorkspace userWorkspace;
    private IFilter filter = AllFilter.INSTANCE;
    
    public Boolean adviseNodeSelected(UITree uiTree) {
        AbstractTreeNode node = (AbstractTreeNode) uiTree.getTreeNode();

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
        }
        return false;
    }
    
    private void buildTree() {
        if (root != null) {
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Starting buildTree()");
        }

        root = new TreeRepository("", "", "root");

        String rpName = "Rules Projects";
        rulesRepository = new TreeRepository(rpName, rpName, UiConst.TYPE_REPOSITORY);
        rulesRepository.setData(null);

        String dpName = "Deployment Projects";
        deploymentRepository = new TreeRepository(dpName, dpName, UiConst.TYPE_DEPLOYMENT_REPOSITORY);
        deploymentRepository.setData(null);

        //Such keys are used for correct order of repositories.
        root.addChild("1st - RulesProjects", rulesRepository);
        root.addChild("2nd - DeploymentProjects", deploymentRepository);

        Collection<RulesProject> rulesProjects = userWorkspace.getProjects();

        IFilter filter = this.filter;
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

    public IFilter getFilter() {
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

    public AbstractTreeNode getSelectedNode() {
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
        AbstractTreeNode currentNode = getRulesRepository();
        while ((currentNode != null) && it.hasNext()) {
            currentNode = currentNode.getChild(it.next());
        }

        if (currentNode != null) {
            selectedNode = (AbstractTreeNode) currentNode;
        }
    }

    public void refreshNode(AbstractTreeNode node){
        node.refresh();
        if (node.getData().isFolder()) {
            node.removeChildren();
            TreeFolder folder = (TreeFolder) node;
            traverseFolder(folder, ((AProjectFolder) folder.getData()).getArtefacts(), filter);
        }
    }
    
    public void deleteNode(AbstractTreeNode node){
        node.getParent().removeChild(node.getId());
    }
    
    public void deleteSelectedNodeFromTree(){
        deleteNode(selectedNode);
        moveSelectionToParentNode();
    }
    
    public void addDeploymentProjectToTree(ADeploymentProject project) {
        TreeDProject prj = new TreeDProject(project.getName(), project.getName());
        prj.setData(project);
        deploymentRepository.add(prj);
    }

    public void addRulesProjectToTree(AProject project) {
        TreeProject prj = new TreeProject(project.getName(), project.getName());
        prj.setData(project);
        rulesRepository.add(prj);
        traverseFolder(prj, project.getArtefacts(), filter);
    }

    public void addNodeToTree(AbstractTreeNode parent, AProjectArtefact childArtefact) {
        String id = childArtefact.getName();
        if (childArtefact.isFolder()) {
            TreeFolder treeFolder = new TreeFolder(id, childArtefact.getName());
            treeFolder.setData(childArtefact);
            parent.add(treeFolder);
            traverseFolder(treeFolder, ((AProjectFolder) childArtefact).getArtefacts(), filter);
        } else {
            TreeFile treeFile = new TreeFile(id, childArtefact.getName());
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
        if (selectedNode.getParent() instanceof AbstractTreeNode) {
            selectedNode = (AbstractTreeNode) selectedNode.getParent();
        } else {
            invalidateSelection();
        }
    }

    public void processSelection(NodeSelectedEvent event) {
        UITree tree = (UITree) event.getComponent();
        
        try {
            selectedNode = (AbstractTreeNode) tree.getTreeNode();
        } catch (IllegalStateException ex) {
            // If nothing selected in tree then invalidate selection. 
            selectedNode = getSelectedNode();
        }
    }

    /**
     * Refreshes repositoryTreeState.selectedNode.
     */
    public void refreshSelectedNode() {
        refreshNode(selectedNode);
    }

    public void setFilter(IFilter filter) {
        this.filter = filter != null ? filter : AllFilter.INSTANCE;
        root = null;
    }

    public void setRoot(TreeRepository root) {
        this.root = root;
    }

    public void setSelectedNode(AbstractTreeNode selectedNode) {
        this.selectedNode = selectedNode;
    }

    public void setUserWorkspace(UserWorkspace userWorkspace) {
        this.userWorkspace = userWorkspace;
        userWorkspace.getDesignTimeRepository().addListener(this);
    }

    public void traverseFolder(TreeFolder folder, Collection<AProjectArtefact> artefacts, IFilter filter) {

        Collection<AProjectArtefact> filteredArtefacts = new ArrayList<AProjectArtefact>();
        for (AProjectArtefact artefact : artefacts) {
            if (!(filter.supports(artefact.getClass()) && !filter.select(artefact))) {
                filteredArtefacts.add(artefact);
            }
        }

        AProjectArtefact[] sortedArtefacts = new AProjectArtefact[filteredArtefacts.size()];
        sortedArtefacts = filteredArtefacts.toArray(sortedArtefacts);

        Arrays.sort(sortedArtefacts, RepositoryUtils.ARTEFACT_COMPARATOR);

        for (AProjectArtefact artefact : sortedArtefacts) {
            addNodeToTree(folder, artefact);
        }
    }

    public void onRulesProjectModified(DTRepositoryEvent event) {
        String projectName = event.getProjectName();
        AbstractTreeNode rulesProject = getRulesRepository().getChild(projectName);
        if (rulesProject == null) {
            if (userWorkspace.getDesignTimeRepository().hasProject(projectName)) {
                try {
                    addRulesProjectToTree(userWorkspace.getProject(projectName));
                } catch (ProjectException e) {
                    LOG.error("Failed to add new project to the repository tree.", e);
                }
            }
        } else if (!userWorkspace.getDesignTimeRepository().hasProject(projectName)) {
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
        AbstractTreeNode deploymentProject = getDeploymentRepository().getChild(event.getProjectName());
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
    
    
    
    //for any project
    public boolean getCanCheckOut() {
        if (getSelectedProject().isLocalOnly() || getSelectedProject().isCheckedOut() || getSelectedProject().isLocked()) {
            return false;
        }

        return isGranted(PRIVILEGE_EDIT);
    }

    public boolean getCanClose() {
        return getSelectedProject().isLockedByMe() || (!getSelectedProject().isLocalOnly() && getSelectedProject().isOpened());
    }

    public boolean getCanDelete() {
        if (getSelectedProject().isLocalOnly()) {
            // any user can delete own local project
            return true;
        }

        return (!getSelectedProject().isLocked() || getSelectedProject().isLockedByUser(userWorkspace.getUser())) && isGranted(PRIVILEGE_DELETE);
    }

    public boolean getCanErase() {
        return (getSelectedProject().isDeleted() && isGranted(PRIVILEGE_ERASE));
    }

    public boolean getCanExport() {
        return getCanOpen();
    }

    public boolean getCanOpen() {
        if (getSelectedProject().isLocalOnly() || getSelectedProject().isCheckedOut()) {
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
        if (getSelectedProject().isLocalOnly() || getSelectedProject().isCheckedOut()) {
            return false;
        }

        return isGranted(PRIVILEGE_DEPLOY);
    }

    public boolean getCanUndelete() {
        return (getSelectedProject().isDeleted() && isGranted(PRIVILEGE_EDIT));
    }


    //for any project artefact
    public boolean getCanModify() {
        AProjectArtefact selectedArtefact = selectedNode.getData();
        String projectName = selectedArtefact.getProject().getName();
        RulesProject project = (RulesProject) getRulesRepository().getChild(projectName).getData();
        return (project.isCheckedOut() && isGranted(PRIVILEGE_EDIT));
    }

    //for deployment project
    public boolean getCanDeploy() {
        return (!getSelectedProject().isCheckedOut() && isGranted(PRIVILEGE_DEPLOY));
    }
}
