package org.openl.rules.webstudio.web.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
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
        AbstractTreeNode node = (AbstractTreeNode) uiTree.getRowData();

        AProjectArtefact projectArtefact = node.getDataBean();
        AProjectArtefact selected = null;
		if (selectedNode != null) {
		    selected = selectedNode.getDataBean();
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
        rulesRepository.setDataBean(null);

        String dpName = "Deployment Projects";
        deploymentRepository = new TreeRepository(dpName, dpName, UiConst.TYPE_DEPLOYMENT_REPOSITORY);
        deploymentRepository.setDataBean(null);

        root.add(rulesRepository);
        root.add(deploymentRepository);

        Collection<AProject> rulesProjects = userWorkspace.getProjects();

        IFilter filter = this.filter;
        for (AProject project : rulesProjects) {
            if (!(filter.supports(AProject.class) && !filter.select(project))) {
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

    public AProject getSelectedProject() {
        AProjectArtefact artefact = getSelectedNode().getDataBean();
        if (artefact instanceof AProject) {
            return (AProject) artefact;
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
        Iterator<String> it = getSelectedNode().getDataBean().getArtefactPath().getSegments().iterator();
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
        if (!node.isLeaf()) {
            node.removeChildren();
            TreeFolder folder = (TreeFolder) node;
            traverseFolder(folder, ((AProjectFolder) folder.getDataBean()).getArtefacts(), filter);
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
        prj.setDataBean(project);
        deploymentRepository.add(prj);
    }

    public void addRulesProjectToTree(AProject project) {
        TreeProject prj = new TreeProject(project.getName(), project.getName());
        prj.setDataBean(project);
        rulesRepository.add(prj);
        traverseFolder(prj, project.getArtefacts(), filter);
    }

    public void addNodeToTree(AbstractTreeNode parent, AProjectArtefact childArtefact) {
        String id = childArtefact.getName();
        if (childArtefact.isFolder()) {
            TreeFolder treeFolder = new TreeFolder(id, childArtefact.getName());
            treeFolder.setDataBean(childArtefact);
            parent.add(treeFolder);
            traverseFolder(treeFolder, ((AProjectFolder) childArtefact).getArtefacts(), filter);
        } else {
            TreeFile treeFile = new TreeFile(id, childArtefact.getName());
            treeFile.setDataBean(childArtefact);
            parent.add(treeFile);
        }
    }

    /**
     * Forces tree rebuild during next access.
     */
    public void invalidateTree() {
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
            selectedNode = (AbstractTreeNode) tree.getRowData();
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
        AbstractTreeNode rulesProject = getRulesRepository().getChild(event.getProjectName());
        if(rulesProject == null){
            if(userWorkspace.getDesignTimeRepository().hasProject(event.getProjectName())){
                try {
                    addRulesProjectToTree(userWorkspace.getProject(event.getProjectName()));
                } catch (ProjectException e) {
                    LOG.error("Failed to add new project to the repository tree.", e);
                }
            }
        }else if (!userWorkspace.getDesignTimeRepository().hasProject(event.getProjectName())){
            deleteNode(rulesProject);
        }else{
            refreshNode(rulesProject);
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
        }else if (!userWorkspace.getDesignTimeRepository().hasDDProject(event.getProjectName())){
            deleteNode(deploymentProject);
        }else{
            refreshNode(deploymentProject);
        }
    }

}
