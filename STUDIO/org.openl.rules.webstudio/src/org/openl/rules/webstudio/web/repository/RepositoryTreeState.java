package org.openl.rules.webstudio.web.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.openl.rules.webstudio.web.repository.tree.AbstractTreeNode;
import org.openl.rules.webstudio.web.repository.tree.TreeDProject;
import org.openl.rules.webstudio.web.repository.tree.TreeFile;
import org.openl.rules.webstudio.web.repository.tree.TreeFolder;
import org.openl.rules.webstudio.web.repository.tree.TreeProject;
import org.openl.rules.webstudio.web.repository.tree.TreeRepository;
import org.openl.rules.workspace.abstracts.DeploymentDescriptorProject;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectFolder;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceDeploymentProject;
import org.openl.rules.workspace.uw.UserWorkspaceProject;

import org.richfaces.component.UITree;

import org.richfaces.event.NodeSelectedEvent;

import org.richfaces.model.TreeNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;


/**
 * Used for holding information about rulesRepository tree.
 *
 * @author Andrey Naumenko
 */
public class RepositoryTreeState {
    private final static Log log = LogFactory.getLog(RepositoryTreeState.class);
    public static final Comparator<ProjectArtefact> ARTEFACT_COMPARATOR = new Comparator<ProjectArtefact>() {
            public int compare(ProjectArtefact o1, ProjectArtefact o2) {
                if (o1.isFolder() == o2.isFolder()) {
                    return o1.getName().compareTo(o2.getName());
                } else {
                    return (o1.isFolder() ? (-1) : 1);
                }
            }
        };

    /** Root node for RichFaces's tree.  It is not displayed. */
    private TreeRepository root;
    private AbstractTreeNode selectedNode;
    private TreeRepository rulesRepository;
    private TreeRepository deploymentRepository;
    private UserWorkspace userWorkspace;

    private void traverseFolder(TreeFolder folder,
        Collection<?extends ProjectArtefact> artefacts) {
        ProjectArtefact[] sortedArtefacts = new ProjectArtefact[artefacts.size()];
        sortedArtefacts = artefacts.toArray(sortedArtefacts);

        Arrays.sort(sortedArtefacts, ARTEFACT_COMPARATOR);

        for (ProjectArtefact artefact : sortedArtefacts) {
            String id = artefact.getName();
            if (artefact.isFolder()) {
                TreeFolder treeFolder = new TreeFolder(id, artefact.getName());
                treeFolder.setDataBean(artefact);
                folder.add(treeFolder);
                traverseFolder(treeFolder, ((ProjectFolder) artefact).getArtefacts());
            } else {
                TreeFile treeFile = new TreeFile(id, artefact.getName());
                treeFile.setDataBean(artefact);
                folder.add(treeFile);
            }
        }
    }

    private void buildTree() {
        if (root != null) {
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Starting buildTree()");
        }

        root = new TreeRepository("", "", "root");

        String rpName = "Rules Projects";
        rulesRepository = new TreeRepository(rpName, rpName, UiConst.TYPE_REPOSITORY);
        rulesRepository.setDataBean(null);

        if (selectedNode == null) {
            selectedNode = rulesRepository;
        }

        String dpName = "Deployment Projects";
        deploymentRepository = new TreeRepository(dpName, dpName,
                UiConst.TYPE_DEPLOYMENT_REPOSITORY);
        deploymentRepository.setDataBean(null);

        root.add(rulesRepository);
        root.add(deploymentRepository);

        Collection<UserWorkspaceProject> rulesProjects = userWorkspace.getProjects();

        for (Project project : rulesProjects) {
            TreeProject prj = new TreeProject(project.getName(), project.getName());
            prj.setDataBean(project);
            rulesRepository.add(prj);
            traverseFolder(prj, project.getArtefacts());
        }

        List<UserWorkspaceDeploymentProject> deploymentsProjects = null;

        try {
            deploymentsProjects = userWorkspace.getDDProjects();
        } catch (RepositoryException e) {
            log.error("Cannot get deployment projects", e);
        }

        for (UserWorkspaceDeploymentProject project : deploymentsProjects) {
            TreeDProject prj = new TreeDProject(dpName + "/" + project.getName(),
                    project.getName());
            prj.setDataBean(project);
            deploymentRepository.add(prj);
        }
        if (log.isDebugEnabled()) {
            log.debug("Finishing buildTree()");
        }
    }

    public void processSelection(NodeSelectedEvent event) {
        UITree tree = (UITree) event.getComponent();
        selectedNode = (AbstractTreeNode) tree.getRowData();
    }

    public Boolean adviseNodeSelected(UITree uiTree) {
        AbstractTreeNode node = (AbstractTreeNode) uiTree.getRowData();

        ProjectArtefact projectArtefact = node.getDataBean();
        ProjectArtefact selected = selectedNode.getDataBean();

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

    /**
     * Forces tree rebuild during next access.
     */
    public void invalidateTree() {
        root = null;
    }

    public void invalidateSelection() {
        selectedNode = null;
    }

    /**
     * Refreshes repositoryTreeState.selectedNode.
     */
    public void refreshSelectedNode() {
        Iterator<String> it = getSelectedNode().getDataBean().getArtefactPath()
                .getSegments().iterator();
        TreeNode currentNode = getRulesRepository();
        while ((currentNode != null) && it.hasNext()) {
            currentNode = currentNode.getChild(it.next());
        }

        if (currentNode != null) {
            selectedNode = (AbstractTreeNode) currentNode;
        }
    }

    /**
     * Moves selection to the parent of the current selected node.
     */
    public void moveSelectionToParentNode() {
        Iterator<String> it = getSelectedNode().getDataBean().getArtefactPath()
                .getSegments().iterator();
        TreeNode node = getRulesRepository();
        TreeNode prevNode = null;
        while ((node != null) && it.hasNext()) {
            prevNode = node;
            node = node.getChild(it.next());
        }

        if (prevNode != null) {
            selectedNode = (AbstractTreeNode) prevNode;
        }
    }

    public TreeRepository getRoot() {
        buildTree();
        return root;
    }

    public void setRoot(TreeRepository root) {
        this.root = root;
    }

    public AbstractTreeNode getSelectedNode() {
        buildTree();
        return selectedNode;
    }

    public void setSelectedNode(AbstractTreeNode selectedNode) {
        this.selectedNode = selectedNode;
    }

    public TreeRepository getRulesRepository() {
        buildTree();
        return rulesRepository;
    }

    public TreeRepository getDeploymentRepository() {
        buildTree();
        return deploymentRepository;
    }

    public void setUserWorkspace(UserWorkspace userWorkspace) {
        this.userWorkspace = userWorkspace;
    }
}
