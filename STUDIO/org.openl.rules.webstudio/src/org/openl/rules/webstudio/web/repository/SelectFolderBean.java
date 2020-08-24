package org.openl.rules.webstudio.web.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openl.rules.repository.api.FolderMapper;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.filter.AllFilter;
import org.openl.rules.webstudio.web.repository.tree.RepositoryFolderNode;
import org.openl.rules.webstudio.web.repository.tree.TreeFolder;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.richfaces.component.UITree;
import org.richfaces.event.TreeSelectionChangeEvent;
import org.richfaces.model.SequenceRowKey;
import org.richfaces.model.TreeNode;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

@Service
@SessionScope
public class SelectFolderBean {
    private final DesignTimeRepository designTimeRepository;
    private String repositoryId;
    private String path;

    private RepositoryFolderNode selectedNode;
    private TreeNode root;

    public SelectFolderBean(DesignTimeRepository designTimeRepository) {
        this.designTimeRepository = designTimeRepository;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        root = null;
        if (path == null) {
            path = "";
        }
        this.path = path;

        if (repositoryId == null) {
            return;
        }

        Repository repository = designTimeRepository.getRepository(repositoryId);
        if (repository.supports().mappedFolders()) {
            repository = ((FolderMapper) repository).getDelegate();
        }
        if (!repository.supports().folders()) {
            return;
        }

        TreeFolder base = new TreeFolder("", "", new AllFilter<>());
        this.root = base;
        RepositoryFolderNode folder = new RepositoryFolderNode((FolderRepository) repository, "/", "/", this);
        base.add(folder);


        String[] folderNames = path.split("/");
        RepositoryFolderNode selection = folder;
        for (String name : folderNames) {
            for (org.openl.rules.webstudio.web.repository.tree.TreeNode node : selection.getChildNodes()) {
                if (name.equals(node.getName())) {
                    selection = (RepositoryFolderNode) node;
                    break;
                }
            }
        }

        setSelectedNode(selection);
    }

    public TreeNode getFolderTree() {
        return root;
    }

    public void processSelection(TreeSelectionChangeEvent event) {
        List<Object> selection = new ArrayList<>(event.getNewSelection());

        /* If there are no selected nodes */
        if (selection.isEmpty()) {
            return;
        }

        Object currentSelectionKey = selection.get(0);
        UITree tree = (UITree) event.getSource();

        Object storedKey = tree.getRowKey();
        tree.setRowKey(currentSelectionKey);
        RepositoryFolderNode rowData = (RepositoryFolderNode) tree.getRowData();
        setSelectedNode(rowData);
        tree.setRowKey(storedKey);
    }

    public Collection<SequenceRowKey> getSelection() {
        org.openl.rules.webstudio.web.repository.tree.TreeNode node = getSelectedNode();

        List<String> ids = new ArrayList<>();
        while (node != null && !node.getName().isEmpty()) {
            ids.add(0, node.getId());
            node = node.getParent();
        }

        return new ArrayList<>(Collections.singletonList(new SequenceRowKey(ids.toArray())));
    }

    public RepositoryFolderNode getSelectedNode() {
        return selectedNode;
    }

    public void setSelectedNode(RepositoryFolderNode selectedNode) {
        this.selectedNode = selectedNode;
        path = selectedNode.getPath();
    }

    public boolean isExpanded(RepositoryFolderNode node) {
        if (selectedNode == null || node == null) {
            return false;
        }

        org.openl.rules.webstudio.web.repository.tree.TreeNode parent = selectedNode.getParent();
        while (parent instanceof RepositoryFolderNode) {
            if (parent == node) {
                return true;
            }

            parent = parent.getParent();
        }

        return false;
    }
}
