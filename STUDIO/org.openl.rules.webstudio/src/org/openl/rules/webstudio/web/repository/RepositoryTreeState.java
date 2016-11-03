package org.openl.rules.webstudio.web.repository;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.*;
import org.openl.rules.project.resolving.ProjectDescriptorArtefactResolver;
import org.openl.rules.webstudio.web.repository.tree.*;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.dtr.DesignTimeRepositoryListener;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.webstudio.filter.AllFilter;
import org.openl.rules.webstudio.filter.IFilter;
import org.richfaces.component.UITree;
import org.richfaces.event.TreeSelectionChangeEvent;
import org.richfaces.model.SequenceRowKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import java.util.*;

import static org.openl.rules.security.AccessManager.isGranted;
import static org.openl.rules.security.DefaultPrivileges.*;

/**
 * Used for holding information about rulesRepository tree.
 *
 * @author Andrey Naumenko
 */
@ManagedBean
@SessionScoped
public class RepositoryTreeState implements DesignTimeRepositoryListener {
    private static final String ROOT_TYPE = "root";

    @ManagedProperty(value = "#{repositorySelectNodeStateHolder}")
    private RepositorySelectNodeStateHolder repositorySelectNodeStateHolder;
    @ManagedProperty("#{projectDescriptorArtefactResolver}")
    private ProjectDescriptorArtefactResolver projectDescriptorResolver;

    public static final String DEFAULT_TAB = "Properties";
    private final Logger log = LoggerFactory.getLogger(RepositoryTreeState.class);
    private static IFilter<AProjectArtefact> ALL_FILTER = new AllFilter<AProjectArtefact>();

    /**
     * Root node for RichFaces's tree. It is not displayed.
     */
    private TreeRepository root;
    private TreeRepository rulesRepository;
    private TreeRepository deploymentRepository;

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
        log.debug("Starting buildTree()");

        root = new TreeRepository("", "", filter, ROOT_TYPE);

        String projectsTreeId = "1st - Projects";
        String rpName = "Projects";
        rulesRepository = new TreeRepository(projectsTreeId, rpName, filter, UiConst.TYPE_REPOSITORY);
        rulesRepository.setData(null);

        String deploymentsTreeId = "2nd - Deploy Configurations";
        String dpName = "Deploy Configurations";
        deploymentRepository = new TreeRepository(deploymentsTreeId, dpName, filter, UiConst.TYPE_DEPLOYMENT_REPOSITORY);
        deploymentRepository.setData(null);

        //Such keys are used for correct order of repositories.
        root.add(rulesRepository);
        root.add(deploymentRepository);

        Collection<RulesProject> rulesProjects = userWorkspace.getProjects();

        IFilter<AProjectArtefact> filter = this.filter;
        for (AProject project : rulesProjects) {
            if (!(filter.supports(RulesProject.class) && !filter.select(project))) {
                addRulesProjectToTree(project);
            }
        }

        try {
            for (ADeploymentProject project : userWorkspace.getDDProjects()) {
                addDeploymentProjectToTree(project);
            }
        } catch (ProjectException e) {
            log.error("Cannot get deployment projects", e);
        }
        log.debug("Finishing buildTree()");

        if (getSelectedNode() == null || UiConst.TYPE_REPOSITORY.equals(getSelectedNode().getType())) {
            setSelectedNode(rulesRepository);
        } else if (UiConst.TYPE_DEPLOYMENT_REPOSITORY.equals(getSelectedNode().getType())) {
            setSelectedNode(deploymentRepository);
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
        return this.repositorySelectNodeStateHolder.getSelectedNode();
    }

    public Collection<SequenceRowKey> getSelection() {
        TreeNode node = getSelectedNode();

        List<String> ids = new ArrayList<String>();
        while (node != null && !node.getType().equals(ROOT_TYPE)) {
            ids.add(0, node.getId());
            node = node.getParent();
        }

        return new ArrayList<SequenceRowKey>(Arrays.asList(new SequenceRowKey(ids.toArray())));
    }

    public TreeProject getProjectNodeByPhysicalName(String physicalName) {
        for (TreeNode treeNode : getRulesRepository().getChildNodes()) {
            TreeProject project = (TreeProject) treeNode;
            if (project.getName().equals(physicalName)) {
                return project;
            }
        }
        return null;
    }

    public TreeProject getProjectNodeByLogicalName(String logicalName) {
        for (TreeNode treeNode : getRulesRepository().getChildNodes()) {
            TreeProject project = (TreeProject) treeNode;
            if (project.getLogicalName().equals(logicalName)) {
                return project;
            }
        }
        return null;
    }

    public UserWorkspaceProject getSelectedProject() {
        AProjectArtefact artefact = getSelectedNode().getData();
        if (artefact instanceof UserWorkspaceProject) {
            return (UserWorkspaceProject) artefact;
        } else if (artefact != null) {
            if (artefact.getProject() instanceof UserWorkspaceProject)
                return (UserWorkspaceProject) artefact.getProject();
        }

        return null;
    }

    public boolean isSelectedProjectModified() {
        UserWorkspaceProject selectedProject = getSelectedProject();
        return selectedProject != null && selectedProject.isModified();
    }

    public void invalidateSelection() {
        setSelectedNode(rulesRepository);
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
            setSelectedNode(currentNode);
        }
    }

    public void refreshNode(TreeNode node) {
        node.refresh();
    }

    public void deleteNode(TreeNode node) {
        node.getParent().removeChild(node.getId());
    }

    public void deleteSelectedNodeFromTree() {
        TreeNode selectedNode = getSelectedNode();
        if (selectedNode != root && selectedNode != rulesRepository && selectedNode != deploymentRepository) {
            deleteNode(selectedNode);
            moveSelectionToParentNode();
        }
    }

    public void addDeploymentProjectToTree(ADeploymentProject project) {
        String name = project.getName();
        String id = RepositoryUtils.getTreeNodeId(name);
        if (!project.isDeleted() || !hideDeleted) {
            TreeDProject prj = new TreeDProject(id, name);
            prj.setData(project);
            deploymentRepository.add(prj);
        }
    }

    public void addRulesProjectToTree(AProject project) {
        String name = project.getName();
        String id = RepositoryUtils.getTreeNodeId(name);
        if (!project.isDeleted() || !hideDeleted) {
            TreeProject prj = new TreeProject(id, name, filter, projectDescriptorResolver);
            prj.setData(project);
            rulesRepository.add(prj);
        }
    }

    public void addNodeToTree(TreeNode parent, AProjectArtefact childArtefact) {
        String name = childArtefact.getName();
        String id = RepositoryUtils.getTreeNodeId(name);
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
        if (getSelectedNode().getParent() != null) {
            setSelectedNode(getSelectedNode().getParent());
        } else {
            invalidateSelection();
        }
    }

    public void processSelection(TreeSelectionChangeEvent event) {
        List<Object> selection = new ArrayList<Object>(event.getNewSelection());
        
        /*If there are no selected nodes*/
        if (selection.isEmpty()) {
            return;
        }

        Object currentSelectionKey = selection.get(0);
        UITree tree = (UITree) event.getSource();

        Object storedKey = tree.getRowKey();
        tree.setRowKey(currentSelectionKey);
        setSelectedNode((TreeNode) tree.getRowData());
        tree.setRowKey(storedKey);
    }

    public void rulesRepositorySelection() {
        setSelectedNode(rulesRepository);
    }

    /**
     * Refreshes repositoryTreeState.selectedNode.
     */
    public void refreshSelectedNode() {
        refreshNode(getSelectedNode());
    }

    public void setFilter(IFilter<AProjectArtefact> filter) {
        this.filter = filter != null ? filter : ALL_FILTER;
        root = null;
    }

    public void setRoot(TreeRepository root) {
        this.root = root;
    }

    public void setSelectedNode(TreeNode selectedNode) {
        repositorySelectNodeStateHolder.setSelectedNode(selectedNode);
    }

    @PostConstruct
    public void initUserWorkspace() {
        this.userWorkspace = WebStudioUtils.getUserWorkspace(FacesUtils.getSession());
        userWorkspace.getDesignTimeRepository().addListener(this);
    }

    @PreDestroy
    public void destroy() {
        if (userWorkspace != null) {
            userWorkspace.getDesignTimeRepository().removeListener(this);
        }
    }

    @Override
    public void onRepositoryModified() {
        root = null;
    }

    public boolean isHideDeleted() {
        return hideDeleted;
    }

    public void setHideDeleted(boolean hideDeleted) {
        this.hideDeleted = hideDeleted;
    }

    public boolean getCanCreate() {
        return isGranted(PRIVILEGE_CREATE_PROJECTS);
    }

    // For any project
    public boolean getCanEdit() {
        UserWorkspaceProject selectedProject = getSelectedProject();
        if (selectedProject == null || selectedProject.isLocalOnly() || selectedProject.isOpenedForEditing() || selectedProject.isLocked()) {
            return false;
        }

        return isGranted(PRIVILEGE_EDIT_PROJECTS);
    }

    public boolean getCanSave() {
        UserWorkspaceProject selectedProject = getSelectedProject();

        return selectedProject.isOpenedForEditing() && isGranted(PRIVILEGE_EDIT_PROJECTS);
    }

    public boolean getCanCreateDeployment() {
        return isGranted(PRIVILEGE_CREATE_DEPLOYMENT);
    }

    public boolean getCanEditDeployment() {
        UserWorkspaceProject selectedProject = getSelectedProject();
        if (selectedProject.isLocalOnly() || selectedProject.isOpenedForEditing() || selectedProject.isLocked()) {
            return false;
        }

        return isGranted(PRIVILEGE_EDIT_DEPLOYMENT);
    }

    public boolean getCanDeleteDeployment() {
        UserWorkspaceProject selectedProject = getSelectedProject();
        if (selectedProject.isLocalOnly()) {
            // any user can delete own local project
            return true;
        }
        return (!selectedProject.isLocked() || selectedProject.isLockedByUser(userWorkspace.getUser())) && isGranted(PRIVILEGE_DELETE_DEPLOYMENT);
    }

    public boolean getCanSaveDeployment() {
        ADeploymentProject selectedProject = (ADeploymentProject) getSelectedProject();
        return selectedProject.isOpenedForEditing() && selectedProject.isModifiedDescriptors() && isGranted(PRIVILEGE_EDIT_DEPLOYMENT);
    }

    public boolean getCanSaveProject() {
        UserWorkspaceProject selectedProject = getSelectedProject();
        return selectedProject.isModified() && isGranted(PRIVILEGE_EDIT_PROJECTS);
    }

    public boolean getCanClose() {
        UserWorkspaceProject selectedProject = getSelectedProject();

        if (selectedProject != null) {
            return selectedProject.isLockedByMe() || (!selectedProject.isLocalOnly() && selectedProject.isOpened());
        } else {
            return false;
        }
    }

    public boolean getCanDelete() {
        UserWorkspaceProject selectedProject = getSelectedProject();
        if (selectedProject.isLocalOnly()) {
            // any user can delete own local project
            return true;
        }
        return (!selectedProject.isLocked() || selectedProject.isLockedByUser(userWorkspace.getUser())) && isGranted(PRIVILEGE_DELETE_PROJECTS);
    }

    public boolean getCanErase() {
        return getSelectedProject().isDeleted() && isGranted(PRIVILEGE_ERASE_PROJECTS);
    }

    public boolean getCanOpen() {
        UserWorkspaceProject selectedProject = getSelectedProject();
        if (selectedProject == null || selectedProject.isLocalOnly() || selectedProject.isOpenedForEditing() || selectedProject.isOpened()) {
            return false;
        }

        return isGranted(PRIVILEGE_VIEW_PROJECTS);
    }

    public boolean getCanOpenOtherVersion() {
        UserWorkspaceProject selectedProject = getSelectedProject();

        if (selectedProject == null) {
            return false;
        }

        if (!selectedProject.isLocalOnly()) {
            return isGranted(PRIVILEGE_VIEW_PROJECTS);
        }

        return false;
    }

    public boolean getCanExport() {
        return !getSelectedProject().isLocalOnly();
    }

    public boolean getCanCompare() {
        if (getSelectedProject().isLocalOnly()) {
            return false;
        }
        return isGranted(PRIVILEGE_VIEW_PROJECTS);
    }

    public boolean getCanRedeploy() {
        UserWorkspaceProject selectedProject = getSelectedProject();
        if (selectedProject.isLocalOnly() || selectedProject.isOpenedForEditing()) {
            return false;
        }

        return isGranted(PRIVILEGE_DEPLOY_PROJECTS);
    }

    public boolean getCanUndelete() {
        return getSelectedProject().isDeleted() && isGranted(PRIVILEGE_EDIT_PROJECTS);
    }


    //for any project artefact
    public boolean getCanModify() {
        AProjectArtefact selectedArtefact = getSelectedNode().getData();
        String projectName = selectedArtefact.getProject().getName();
        String projectId = RepositoryUtils.getTreeNodeId(projectName);
        RulesProject project = (RulesProject) getRulesRepository().getChild(projectId).getData();
        return (project.isOpenedForEditing() && isGranted(PRIVILEGE_EDIT_PROJECTS));
    }

    //for deployment project
    public boolean getCanDeploy() {
        return !getSelectedProject().isOpenedForEditing() && isGranted(PRIVILEGE_DEPLOY_PROJECTS);
    }

    public String getDefSelectTab() {
        return DEFAULT_TAB;
    }

    public boolean isLocalOnly() {
        return getSelectedProject().isLocalOnly();
    }

    public String clearSelectPrj() {
        buildTree();
        invalidateSelection();

        return "";
    }

    public RepositorySelectNodeStateHolder getRepositorySelectNodeStateHolder() {
        return repositorySelectNodeStateHolder;
    }

    public void setRepositorySelectNodeStateHolder(RepositorySelectNodeStateHolder repositorySelectNodeStateHolder) {
        this.repositorySelectNodeStateHolder = repositorySelectNodeStateHolder;
    }

    public void setProjectDescriptorResolver(ProjectDescriptorArtefactResolver projectDescriptorResolver) {
        this.projectDescriptorResolver = projectDescriptorResolver;
    }
}
