package org.openl.rules.webstudio.web.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.richfaces.component.UITree;
import org.richfaces.event.TreeSelectionChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.security.SecureDeploymentRepositoryService;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.repository.tree.TreeNode;
import org.openl.rules.webstudio.web.repository.tree.TreeProductionDProject;
import org.openl.rules.webstudio.web.repository.tree.TreeRepository;

@Service
@SessionScope
public class ProductionRepositoriesTreeState {
    private static final String SEPARATOR = "#";
    @Autowired
    private RepositorySelectNodeStateHolder repositorySelectNodeStateHolder;

    @Autowired
    private RepositoryFactoryProxy productionRepositoryFactoryProxy;

    @Autowired
    private SecureDeploymentRepositoryService secureDeploymentRepositoryService;

    private final Logger log = LoggerFactory.getLogger(ProductionRepositoriesTreeState.class);
    /**
     * Root node for RichFaces's tree. It is not displayed.
     */
    private TreeRepository root;

    private final IFilter<AProjectArtefact> filter = new AllFilter<>();

    private static Collection<Deployment> getLastDeploymentProjects(Repository repository,
                                                                    String deployPath) throws IOException {

        Map<String, Deployment> latestDeployments = new HashMap<>();
        Map<String, Integer> versionsList = new HashMap<>();

        Collection<FileData> fileDatas;
        if (repository.supports().folders()) {
            // All deployments
            fileDatas = repository.listFolders(deployPath);
        } else {
            // Projects inside all deployments
            fileDatas = repository.list(deployPath);
        }
        for (FileData fileData : fileDatas) {
            String deploymentFolderName = fileData.getName().substring(deployPath.length()).split("/")[0];
            int separatorPosition = deploymentFolderName.lastIndexOf(SEPARATOR);

            String deploymentName = deploymentFolderName;
            int version = 0;
            CommonVersionImpl commonVersion;
            if (separatorPosition >= 0) {
                deploymentName = deploymentFolderName.substring(0, separatorPosition);
                version = Integer.parseInt(deploymentFolderName.substring(separatorPosition + 1));
                commonVersion = new CommonVersionImpl(version);
            } else {
                commonVersion = new CommonVersionImpl(fileData.getVersion());
            }
            Integer previous = versionsList.put(deploymentName, version);
            if (previous != null && previous > version) {
                // rollback
                versionsList.put(deploymentName, previous);
            } else {
                // put the latest deployment

                String folderPath = deployPath + deploymentFolderName;
                boolean folderStructure;
                if (repository.supports().folders()) {
                    folderStructure = !repository.listFolders(folderPath + "/").isEmpty();
                } else {
                    folderStructure = false;
                }
                Deployment deployment = new Deployment(repository,
                        folderPath,
                        deploymentName,
                        commonVersion,
                        folderStructure);
                latestDeployments.put(deploymentName, deployment);
            }
        }

        return latestDeployments.values();
    }

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
            List<AProjectFolder> repoList = getPRepositoryProjects(repoConfig);
            repoList.sort(RepositoryUtils.ARTEFACT_COMPARATOR);

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

    private List<AProjectFolder> getPRepositoryProjects(RepositoryConfiguration repoConfig) {
        try {
            Repository repository = productionRepositoryFactoryProxy.getRepositoryInstance(repoConfig.getConfigName());
            String deploymentsPath = productionRepositoryFactoryProxy.getBasePath(repoConfig.getConfigName());
            return new ArrayList<>(getLastDeploymentProjects(repository, deploymentsPath));
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

        Object currentSelectionKey = selection.get(0);
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
