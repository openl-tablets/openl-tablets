package org.openl.rules.ui.repository;

import javax.faces.event.AbortProcessingException;

import org.openl.rules.ui.repository.beans.Entity;
import org.openl.rules.ui.repository.beans.FolderBean;
import org.openl.rules.ui.repository.beans.ProjectBean;
import org.openl.rules.ui.repository.beans.RepositoryBean;
import org.openl.rules.ui.repository.tree.AbstractTreeNode;
import org.openl.rules.ui.repository.tree.TreeFile;
import org.openl.rules.ui.repository.tree.TreeFolder;
import org.openl.rules.ui.repository.tree.TreeProject;
import org.openl.rules.ui.repository.tree.TreeRepository;
import org.richfaces.component.UITree;
import org.richfaces.event.NodeSelectedEvent;

/**
 * Handler for Repository Tree.
 * 
 * @author Aleh Bykhavets
 *
 */
public class RepositoryTreeHandler {
    private Context context;
    /**
     * Root node for RichFaces's tree.  It won't be displayed. 
     */
    private TreeRepository root;
    private AbstractTreeNode currentNode;
    private TreeRepository repository;

    public RepositoryTreeHandler(Context context) {
        this.context = context;
    }

    public Object getData() {
        if (root == null) {
            initData();
        }

        return root;
    }

    public void processSelection(NodeSelectedEvent event) throws AbortProcessingException {
        UITree tree = (UITree) event.getComponent();
        AbstractTreeNode node =  (AbstractTreeNode) tree.getRowData();

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
    
    // ------ protected ------
    
    protected void reInit() {
        root = null;
    }
    
    protected void setSelected(AbstractTreeNode node) {
        currentNode = node;
    }

    @Deprecated
    private static long lastId;

    @Deprecated
    private static synchronized long generateId() {
        return lastId++;
    }

    protected void initData() {
        root = new TreeRepository(generateId(), "Rules Repository");
        String repName = context.getRepository().getName();
        repository = new TreeRepository(generateId(), repName);
        RepositoryBean rb = new RepositoryBean();
        rb.setName(repName);
        repository.setDataBean(rb);
        root.add(repository);
        
        for (ProjectBean project : context.getRepositoryHandler().getProjects()) {
            TreeProject prj = new TreeProject(generateId(), project.getName());
            prj.setDataBean(project);
            repository.add(prj);
            // redo that
            initProject(prj, project);
        }
    }
    
    private void initProject(TreeProject prj, ProjectBean pb) {
        // not a best way..
        for (Entity eb : pb.getElements()) {
            if (eb instanceof FolderBean) {
                TreeFolder tf = new TreeFolder(generateId(), eb.getName());
                tf.setDataBean(eb);
                prj.add(tf);
                
                initFolder(tf, (FolderBean)eb);
            } else {
                TreeFile tf = new TreeFile(generateId(), eb.getName());
                tf.setDataBean(eb);
                prj.add(tf);
            }
        }
    }

    private void initFolder(TreeFolder folder, FolderBean fb) {
        // not a best way..
        for (Entity eb : fb.getElements()) {
            if (eb instanceof FolderBean) {
                TreeFolder tf = new TreeFolder(generateId(), eb.getName());
                tf.setDataBean(eb);
                folder.add(tf);
                
                initFolder(tf, (FolderBean)eb);
            } else {
                TreeFile tf = new TreeFile(generateId(), eb.getName());
                tf.setDataBean(eb);
                folder.add(tf);
            }
        }
    }
}
