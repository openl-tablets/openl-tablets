package org.openl.rules.webstudio.web.repository;

import java.util.Collections;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

import org.openl.rules.webstudio.web.repository.tree.TreeNode;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

@ManagedBean
@SessionScoped
public class ProductionRepositoriesTreeController {
    @ManagedProperty(value = "#{repositorySelectNodeStateHolder}")
    private RepositorySelectNodeStateHolder repositorySelectNodeStateHolder;

    @ManagedProperty(value = "#{productionRepositoriesTreeState}")
    private ProductionRepositoriesTreeState productionRepositoriesTreeState;

    public ProductionRepositoriesTreeState getProductionRepositoriesTreeState() {
        return productionRepositoriesTreeState;
    }

    public void setProductionRepositoriesTreeState(ProductionRepositoriesTreeState productionRepositoriesTreeState) {
        this.productionRepositoriesTreeState = productionRepositoriesTreeState;
    }

    /**
     * Gets all rules projects from a rule repository.
     *
     * @return list of rules projects
     */
    public List<TreeNode> getRulesProjects() {
        TreeNode selectedNode = repositorySelectNodeStateHolder.getSelectedNode();
        return selectedNode == null ? Collections.<TreeNode> emptyList() : selectedNode.getChildNodes();
    }

    public String selectRulesProject() {
        String projectName = WebStudioUtils.getRequestParameter("projectName");

        TreeNode selectedNode = repositorySelectNodeStateHolder.getSelectedNode();
        if (selectedNode == null) {
            return null;
        }
        if (selectedNode.getType().equals(UiConst.TYPE_PRODUCTION_REPOSITORY) || selectedNode.getType()
            .equals(UiConst.TYPE_PRODUCTION_DEPLOYMENT_PROJECT)) {
            for (TreeNode node : selectedNode.getChildNodes()) {
                if (node.getName().equals(projectName)) {
                    repositorySelectNodeStateHolder.setSelectedNode(node);
                    break;
                }
            }
        }

        return null;
    }

    public void openTab() {
        productionRepositoriesTreeState.initTree();

        TreeNode node = productionRepositoriesTreeState.getFirstProductionRepo();
        if (node != null) {
            repositorySelectNodeStateHolder.setSelectedNode(node);
        }
    }

    public RepositorySelectNodeStateHolder getRepositorySelectNodeStateHolder() {
        return repositorySelectNodeStateHolder;
    }

    public void setRepositorySelectNodeStateHolder(RepositorySelectNodeStateHolder repositorySelectNodeStateHolder) {
        this.repositorySelectNodeStateHolder = repositorySelectNodeStateHolder;
    }

    public String refreshTree() {
        productionRepositoriesTreeState.invalidateTree();

        return null;
    }

    public void refreshInitTree() {
        productionRepositoriesTreeState.invalidateTree();
        productionRepositoriesTreeState.initTree();
    }

    public void deleteProdRepo(String configName) {
        if (productionRepositoriesTreeState.getRoot() != null) {
            productionRepositoriesTreeState.getRoot().removeChild(configName);
        }
    }
}
