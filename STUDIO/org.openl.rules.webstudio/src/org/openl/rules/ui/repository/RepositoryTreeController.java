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
    private static long lastId;
    private AbstractTreeNode currentNode;
    private TreeRepository repository;
    private UserWorkspace userWorkspace;

    public UserWorkspace getUserWorkspace() {
        return userWorkspace;
    }

    public void setUserWorkspace(UserWorkspace userWorkspace) {
        this.userWorkspace = userWorkspace;
    }

    public synchronized Object getData() {
        if (root == null) {
            initData();
        }

        return root;
    }

    public synchronized TreeRepository getRepositoryNode() {
        if (root == null) {
            initData();
        }
        return repository;
    }

    public void processSelection(NodeSelectedEvent event) throws AbortProcessingException {
        UITree tree = (UITree) event.getComponent();
        AbstractTreeNode node = (AbstractTreeNode) tree.getRowData();

        setSelected(node);
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

    public Boolean adviseNodeSelected(org.richfaces.component.UITree uiTree) {
        AbstractTreeNode node = (AbstractTreeNode) uiTree.getRowData();
        AbstractTreeNode selected = getSelected();

        return (node == selected) ? Boolean.TRUE : Boolean.FALSE;
    }

    public void reInit() {
        root = null;
        currentNode = null;
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

    // ------ protected ------
    protected void setSelected(AbstractTreeNode node) {
        currentNode = node;
    }

    private static synchronized long generateId() {
        return lastId++;
    }

    protected void initData() {
        lastId=0;
        root = new TreeRepository(generateId(), "");
        repository = new TreeRepository(generateId(), "Rules Repository");
        repository.setDataBean(null);
        root.add(repository);

        for (Project project : userWorkspace.getProjects()) {
            TreeProject prj = new TreeProject(generateId(), project.getName());
            prj.setDataBean(project);
            repository.add(prj);
            // redo that
            initFolder(prj, project.getArtefacts());
        }
    }

    private void initFolder(TreeFolder folder,
        Collection<?extends ProjectArtefact> artefacts) {
        for (ProjectArtefact artefact : artefacts) {
            if (artefact instanceof ProjectFolder) {
                TreeFolder tf = new TreeFolder(generateId(), artefact.getName());
                tf.setDataBean(artefact);
                folder.add(tf);

                initFolder(tf, ((ProjectFolder) artefact).getArtefacts());
            } else {
                TreeFile tf = new TreeFile(generateId(), artefact.getName());
                tf.setDataBean(artefact);
                folder.add(tf);
            }
        }
    }

    public static void refreshSessionTree() {
        RepositoryTreeController self = (RepositoryTreeController) FacesUtils
                .getFacesVariable("#{repositoryTree}");
        if (self != null) {
            self.reInit();
        }
    }
}
