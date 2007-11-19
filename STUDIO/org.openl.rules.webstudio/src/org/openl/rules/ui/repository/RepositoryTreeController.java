package org.openl.rules.ui.repository;

import org.openl.rules.ui.repository.tree.AbstractTreeNode;
import org.openl.rules.ui.repository.tree.TreeFile;
import org.openl.rules.ui.repository.tree.TreeFolder;
import org.openl.rules.ui.repository.tree.TreeProject;
import org.openl.rules.ui.repository.tree.TreeRepository;
import org.openl.rules.webstudio.util.FacesUtils;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectFolder;
import org.openl.rules.workspace.uw.UserWorkspace;

import org.richfaces.component.UITree;

import org.richfaces.event.NodeSelectedEvent;

import java.util.Collection;

import javax.faces.event.AbortProcessingException;


/**
 * Handler for Repository Tree.
 *
 * @author Aleh Bykhavets
 */
public class RepositoryTreeController {
    /** Root node for RichFaces's tree.  It is not displayed. */
    private TreeRepository root;
    private AbstractTreeNode currentNode;
    private TreeRepository repository;
    private UserWorkspace userWorkspace;

    /**
     * TODO: reimplement properly when AbstractTreeNode.id becomes Object.
     *
     * @param nodeName
     *
     * @return
     */
    private static long generateId(String nodeName) {
        return nodeName.hashCode();
    }

    private void traverseFolder(TreeFolder folder,
        Collection<?extends ProjectArtefact> artefacts) {
        for (ProjectArtefact artefact : artefacts) {
            String path = artefact.getArtefactPath().getStringValue();
            if (artefact instanceof ProjectFolder) {
                TreeFolder tf = new TreeFolder(generateId(path), artefact.getName());
                tf.setDataBean(artefact);
                folder.add(tf);
                traverseFolder(tf, ((ProjectFolder) artefact).getArtefacts());
            } else {
                TreeFile tf = new TreeFile(generateId(path), artefact.getName());
                tf.setDataBean(artefact);
                folder.add(tf);
            }
        }
    }

    private void buildTree() {
        root = new TreeRepository(generateId(""), "");
        repository = new TreeRepository(generateId("Rules Repository"), "Rules Repository");
        repository.setDataBean(null);
        root.add(repository);

        for (Project project : userWorkspace.getProjects()) {
            TreeProject prj = new TreeProject(generateId(project.getName()),
                    project.getName());
            prj.setDataBean(project);
            repository.add(prj);
            // redo that
            traverseFolder(prj, project.getArtefacts());
        }
    }

    public synchronized Object getData() {
        if (root == null) {
            buildTree();
        }

        return root;
    }

    public synchronized TreeRepository getRepositoryNode() {
        if (root == null) {
            buildTree();
        }
        return repository;
    }

    public AbstractTreeNode getSelected() {
        if (currentNode == null) {
            // lazy loading
            getData();
            //
            currentNode = repository;
        }
        return currentNode;
    }

    public void processSelection(NodeSelectedEvent event) throws AbortProcessingException {
        UITree tree = (UITree) event.getComponent();
        AbstractTreeNode node = (AbstractTreeNode) tree.getRowData();
        currentNode = node;
    }

    public Boolean adviseNodeSelected(UITree uiTree) {
        AbstractTreeNode node = (AbstractTreeNode) uiTree.getRowData();
        AbstractTreeNode selected = getSelected();
        return (node.getId() == selected.getId());
    }

    public void reInit() {
        root = null;

//        currentNode = null;
    }

    /**
     * Used for testing DDP. Must be removed in the future.
     *
     * @return
     */
    public String getSecondProjectName() {
        int c = 0;
        for (Project project : userWorkspace.getProjects()) {
            if (c > 0) {
                return project.getName();
            }
            c++;
        }
        return null;
    }

    public static void refreshSessionTree() {
        RepositoryTreeController self = (RepositoryTreeController) FacesUtils
                .getFacesVariable("#{repositoryTree}");
        if (self != null) {
            self.reInit();
        }
    }

    public UserWorkspace getUserWorkspace() {
        return userWorkspace;
    }

    public void setUserWorkspace(UserWorkspace userWorkspace) {
        this.userWorkspace = userWorkspace;
    }
}
