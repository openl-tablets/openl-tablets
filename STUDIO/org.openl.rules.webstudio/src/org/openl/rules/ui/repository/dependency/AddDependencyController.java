package org.openl.rules.ui.repository.dependency;

import org.openl.rules.ui.repository.RepositoryTreeController;
import org.openl.rules.ui.repository.UiConst;
import org.openl.rules.ui.repository.tree.AbstractTreeNode;
import org.openl.rules.ui.repository.tree.TreeProject;
import org.openl.rules.workspace.uw.UserWorkspaceProject;

import javax.faces.model.SelectItem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddDependencyController {
    /**
     * A controller which contains pre-built UI object tree.
     */
    private RepositoryTreeController repositoryTree;

    private String project;
    private String lowerVersion;
    private String upperVersion;

    public void setRepositoryTree(RepositoryTreeController repositoryTree) {
        this.repositoryTree = repositoryTree;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getLowerVersion() {
        return lowerVersion;
    }

    public void setLowerVersion(String lowerVersion) {
        this.lowerVersion = lowerVersion;
    }

    public String getUpperVersion() {
        return upperVersion;
    }

    public void setUpperVersion(String upperVersion) {
        this.upperVersion = upperVersion;
    }

    public SelectItem[] getAvailableProjects() {
        AbstractTreeNode selected = repositoryTree.getSelected();
        if (!(selected instanceof TreeProject)) {
            return new SelectItem[0];
        }
        Set<String> existing = new HashSet<String>();
        for (DependencyBean dep : selected.getDependencies()) {
            existing.add(dep.getProjectName());
        }
        existing.add(selected.getName());

        List<String> matching = new ArrayList<String>();
        for (AbstractTreeNode node : repositoryTree.getRepositoryNode().getChildNodes()) {
            if (!existing.contains(node.getName()) && !((UserWorkspaceProject) node.getDataBean()).isLocalOnly()) {
                matching.add(node.getName());
            }
        }

        SelectItem[] result = new SelectItem[matching.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = new SelectItem(matching.get(i));
        }
        
        return result;
    }

    public String add() {
        
        return UiConst.OUTCOME_SUCCESS;
    }
}
