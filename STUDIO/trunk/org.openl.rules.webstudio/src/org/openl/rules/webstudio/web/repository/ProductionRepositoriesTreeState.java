package org.openl.rules.webstudio.web.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.config.ConfigurationManager;
import org.openl.config.ConfigurationManagerFactory;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.repository.RProductionRepository;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.repository.tree.TreeNode;
import org.openl.rules.webstudio.web.repository.tree.TreeProductProject;
import org.openl.rules.webstudio.web.repository.tree.TreeRepository;
import org.openl.util.filter.AllFilter;
import org.openl.util.filter.IFilter;
import org.richfaces.component.UITree;
import org.richfaces.event.TreeSelectionChangeEvent;

@ManagedBean
@SessionScoped
public class ProductionRepositoriesTreeState {
    @ManagedProperty(value="#{repositorySelectNodeStateHolder}")
    private RepositorySelectNodeStateHolder repositorySelectNodeStateHolder;

    @ManagedProperty(value="#{deploymentManager}")
    private DeploymentManager deploymentManager;

    @ManagedProperty(value="#{productionRepositoryConfigManagerFactory}")
    private ConfigurationManagerFactory productionConfigManagerFactory;
    
    @ManagedProperty(value="#{productionRepositoryFactoryProxy}")
    private ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy;

    private final Log log = LogFactory.getLog(ProductionRepositoriesTreeState.class);
    /** Root node for RichFaces's tree. It is not displayed. */
    private TreeRepository root;

    private static IFilter<AProjectArtefact> ALL_FILTER = new AllFilter<AProjectArtefact>();
    private IFilter<AProjectArtefact> filter = ALL_FILTER;
    
    private void buildTree() {
        if (root != null) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Starting buildTree()");
        }

        root = new TreeRepository("", "", filter, "root");

        /*get list of production repos*/
        for(RepositoryConfiguration repoConfig : getRepositories()) {
            String prName = repoConfig.getName();
            TreeRepository productionRepository = new TreeRepository(prName, prName, filter, UiConst.TYPE_PRODUCTION_REPOSITORY);
            productionRepository.setData(null);

            root.addChild(prName, productionRepository);
            
            /*Get repo's projects*/
            IFilter<AProjectArtefact> filter = this.filter;
            List<AProject> repoList = getPRepositoryProjects(repoConfig);
            Collections.sort(repoList, RepositoryUtils.ARTEFACT_COMPARATOR);

            for (AProject project : repoList) {
                Collection<AProjectArtefact> prjList = project.getArtefacts();
                AProjectArtefact[] sortedArtefacts = new AProjectArtefact[prjList.size()];
                sortedArtefacts = prjList.toArray(sortedArtefacts);

                Arrays.sort(sortedArtefacts, RepositoryUtils.ARTEFACT_COMPARATOR);
                
                for (AProjectArtefact apa : sortedArtefacts) {
                    TreeProductProject prj = new TreeProductProject(""+apa.getName().hashCode(), apa.getName(), filter);
                    prj.setData(apa);

                    productionRepository.add(prj);
                }
            }

        }

        if (log.isDebugEnabled()) {
            log.debug("Finishing buildTree()");
        }
    }

    private List<AProject> getPRepositoryProjects(RepositoryConfiguration repoConfig) {
        try {
            RProductionRepository productRepos = productionRepositoryFactoryProxy.getRepositoryInstance(repoConfig.getConfigName());
            List<AProject> prjList = new ArrayList<AProject>();

            for(FolderAPI prj : productRepos.getDeploymentProjects()) {
                prjList.add(new AProject(prj));
            }

            return prjList;
        } catch (RRepositoryException e) {
            return new ArrayList<AProject>();
        }

    }

    public Collection<RepositoryConfiguration> getRepositories() {
        List<RepositoryConfiguration> repos = new ArrayList<RepositoryConfiguration>();
        Collection<String> repositoryConfigNames = deploymentManager.getRepositoryConfigNames();
        for (String configName : repositoryConfigNames) {
            ConfigurationManager productionConfig = productionConfigManagerFactory.getConfigurationManager(configName);
            RepositoryConfiguration config = new RepositoryConfiguration(configName, productionConfig);
            repos.add(config);
        }

        Collections.sort(repos, RepositoryConfiguration.COMPARATOR);

        return repos;
    }

    public TreeRepository getRoot() {
        //buildTree();
        return root;
    }
    
    public String initTree() {
        buildTree();
        
        return null;
    }
    
    public void processSelection(TreeSelectionChangeEvent event) {
        List<Object> selection = new ArrayList<Object>(event.getNewSelection());
        
        /*If there are no selected nodes*/
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
    
    /**
     * Forces tree rebuild during next access.
     */
    public void invalidateTree() {
        root = null;
        initTree();
    }

    public DeploymentManager getDeploymentManager() {
        return deploymentManager;
    }

    public void setDeploymentManager(DeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
    }

    public ConfigurationManagerFactory getProductionConfigManagerFactory() {
        return productionConfigManagerFactory;
    }

    public void setProductionConfigManagerFactory(ConfigurationManagerFactory productionConfigManagerFactory) {
        this.productionConfigManagerFactory = productionConfigManagerFactory;
    }
    
    public ProductionRepositoryFactoryProxy getProductionRepositoryFactoryProxy() {
        return productionRepositoryFactoryProxy;
    }

    public void setProductionRepositoryFactoryProxy(ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy) {
        this.productionRepositoryFactoryProxy = productionRepositoryFactoryProxy;
    }
    
    public RepositorySelectNodeStateHolder getRepositorySelectNodeStateHolder() {
        return repositorySelectNodeStateHolder;
    }

    public void setRepositorySelectNodeStateHolder(RepositorySelectNodeStateHolder repositorySelectNodeStateHolder) {
        this.repositorySelectNodeStateHolder = repositorySelectNodeStateHolder;
    }
}
