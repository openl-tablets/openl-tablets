package org.openl.rules.webstudio.web.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.richfaces.component.UITree;
import org.richfaces.event.TreeSelectionChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.rest.deployment.service.DeploymentCriteriaQuery;
import org.openl.rules.rest.deployment.service.DeploymentService;
import org.openl.rules.webstudio.security.SecureDeploymentRepositoryService;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.repository.tree.TreeNode;
import org.openl.rules.webstudio.web.repository.tree.TreeProductionDProject;
import org.openl.rules.webstudio.web.repository.tree.TreeRepository;

@Service
@SessionScope
public class ProductionRepositoriesTreeState {

    @Autowired
    private RepositorySelectNodeStateHolder repositorySelectNodeStateHolder;

    @Autowired
    private SecureDeploymentRepositoryService secureDeploymentRepositoryService;

    @Autowired
    private DeploymentService deploymentService;

    private final Logger log = LoggerFactory.getLogger(ProductionRepositoriesTreeState.class);
    /**
     * Root node for RichFaces's tree. It is not displayed.
     */
    private TreeRepository root;

    private final IFilter<AProjectArtefact> filter = new AllFilter<>();

    private void buildTree() {
        if (root != null) {
            return;
        }

        log.debug("Starting buildTree()");

        root = new TreeRepository("", "", filter, "root");

        /* get list of production repos */
        for (RepositoryConfiguration repoConfig : getRepositories()) {
            String prName = repoConfig.getName();
            TreeRepository productionRepository = new TreeRepository(prName,
                    prName,
                    filter,
                    UiConst.TYPE_PRODUCTION_REPOSITORY);
            productionRepository.setData(null);

            root.addChild(prName, productionRepository);

            /* Get repo's deployment configs */
            IFilter<AProjectArtefact> filter = this.filter;
            List<Deployment> repoList = getPRepositoryProjects(repoConfig);

            for (AProjectFolder project : repoList) {
                TreeProductionDProject tpdp = new TreeProductionDProject("" + project.getName().hashCode(),
                        project.getName(),
                        filter);
                tpdp.setData(project);
                tpdp.setParent(productionRepository);
                productionRepository.add(tpdp);
            }

            if (repoList.isEmpty()) {
                // Initialize content of empty node
                productionRepository.getElements();
            }

        }

        log.debug("Finishing buildTree()");
    }

    private List<Deployment> getPRepositoryProjects(RepositoryConfiguration repoConfig) {
        try {
            return deploymentService.getDeployments(DeploymentCriteriaQuery.builder()
                    .repository(repoConfig.getId())
                    .build());
        } catch (Exception e) {
            return new ArrayList<>();
        }

    }

    public Collection<RepositoryConfiguration> getRepositories() {
        return secureDeploymentRepositoryService.getRepositories();
    }

    public TreeRepository getRoot() {
        // buildTree();
        return root;
    }

    public void initTree() {
        buildTree();
    }

    public void processSelection(TreeSelectionChangeEvent event) {
        List<Object> selection = new ArrayList<>(event.getNewSelection());

        /* If there are no selected nodes */
        if (selection.isEmpty()) {
            return;
        }

        Object currentSelectionKey = selection.getFirst();
        UITree tree = (UITree) event.getSource();

        Object storedKey = tree.getRowKey();
        tree.setRowKey(currentSelectionKey);
        repositorySelectNodeStateHolder.setSelectedNode((TreeNode) tree.getRowData());
        tree.setRowKey(storedKey);
    }

    public TreeNode getFirstProductionRepo() {
        try {
            String repoName = getRepositories().iterator().next().getName();
            return root.getElements().get(repoName);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Forces tree rebuild during next access.
     */
    public void invalidateTree() {
        root = null;
    }
}
