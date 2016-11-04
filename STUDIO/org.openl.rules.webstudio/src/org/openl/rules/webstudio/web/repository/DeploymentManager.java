package org.openl.rules.webstudio.web.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.deploy.DeploymentException;
import org.openl.rules.workspace.deploy.ProductionDeployer;
import org.openl.rules.workspace.deploy.ProductionDeployerFactory;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.springframework.beans.factory.InitializingBean;

/**
 * Deployment manager
 *
 * @author Andrey Naumenko
 */
public class DeploymentManager implements InitializingBean {
    private ProductionDeployerFactory productionDeployerFactory;
    private String[] initialProductionRepositoryConfigNames;
    private DesignTimeRepository designRepository;
    private boolean deploymentFormatOld;

    private Map<String, ProductionDeployer> deployers = new HashMap<String, ProductionDeployer>();

    public void addRepository(String repositoryConfigName) {
        deployers.put(repositoryConfigName, productionDeployerFactory.getDeployerInstance(repositoryConfigName, deploymentFormatOld));
    }

    public void removeRepository(String repositoryConfigName) throws RRepositoryException {
        if (deployers.containsKey(repositoryConfigName)) {
            deployers.get(repositoryConfigName).destroy();
            deployers.remove(repositoryConfigName);
        }
    }

    public Collection<String> getRepositoryConfigNames() {
        return deployers.keySet();
    }

    public DeployID deploy(ADeploymentProject project, String repositoryConfigName) throws WorkspaceException, ProjectException {
        ProductionDeployer deployer = deployers.get(repositoryConfigName);
        if (deployer == null) {
            throw new IllegalArgumentException("No such repository '" + repositoryConfigName + "'");
        }

        WorkspaceUserImpl user = new WorkspaceUserImpl(WebStudioUtils.getRulesUserSession(FacesUtils.getSession()).getUserName());

        @SuppressWarnings("rawtypes")
        Collection<ProjectDescriptor> projectDescriptors = project.getProjectDescriptors();
        Collection<AProject> projects = new ArrayList<AProject>();

        for (ProjectDescriptor<?> pd : projectDescriptors) {
            try {
                projects.add(designRepository.getProject(pd.getProjectName(), pd.getProjectVersion()));
            } catch (RepositoryException e) {
                throw new DeploymentException(e.getMessage(), e);
            }
        }

        return deployer.deploy(project, projects, user);
    }

    public void setProductionDeployerFactory(ProductionDeployerFactory productionDeployerFactory) {
        this.productionDeployerFactory = productionDeployerFactory;
    }

    public void setInitialProductionRepositoryConfigNames(String[] initialProductionRepositoryConfigNames) {
        this.initialProductionRepositoryConfigNames = initialProductionRepositoryConfigNames;
    }
    
    public void setDesignRepository(DesignTimeRepository designRepository) {
        this.designRepository = designRepository;
    }

    public void reload() throws RRepositoryException {
        for (String repository : deployers.keySet()) {
            removeRepository(repository);
        }
        afterPropertiesSet();
    }
    @Override
    public void afterPropertiesSet() {
        if (initialProductionRepositoryConfigNames != null) {
            for (String repositoryConfigName : initialProductionRepositoryConfigNames) {
                addRepository(repositoryConfigName);
            }
        }
    }

    public boolean isDeploymentFormatOld() {
        return deploymentFormatOld;
    }

    public void setDeploymentFormatOld(boolean deploymentFormatOld) {
        this.deploymentFormatOld = deploymentFormatOld;
    }

}
