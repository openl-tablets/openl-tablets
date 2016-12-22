package org.openl.rules.webstudio.web.repository;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.deploy.DeployUtils;
import org.openl.rules.workspace.deploy.DeploymentException;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.springframework.beans.factory.InitializingBean;

/**
 * Deployment manager
 *
 * @author Andrey Naumenko
 */
public class DeploymentManager implements InitializingBean {
    private String[] initialProductionRepositoryConfigNames;
    private DesignTimeRepository designRepository;

    private Set<String> deployers = new HashSet<String>();

    public void addRepository(String repositoryConfigName) {
        deployers.add(repositoryConfigName);
    }

    public void removeRepository(String repositoryConfigName) throws RRepositoryException {
        deployers.remove(repositoryConfigName);
        repositoryFactoryProxy.releaseRepository(repositoryConfigName);
    }

    public Collection<String> getRepositoryConfigNames() {
        return deployers;
    }

    public DeployID deploy(ADeploymentProject project, String repositoryConfigName) throws WorkspaceException,
                                                                                    ProjectException {
        if (!deployers.contains(repositoryConfigName)) {
            throw new IllegalArgumentException("No such repository '" + repositoryConfigName + "'");
        }

        String userName = WebStudioUtils.getRulesUserSession(FacesUtils.getSession()).getUserName();

        @SuppressWarnings("rawtypes")
        Collection<ProjectDescriptor> projectDescriptors = project.getProjectDescriptors();

        try {
            Repository deployRepo = repositoryFactoryProxy.getRepositoryInstance(repositoryConfigName);
            StringBuilder sb = new StringBuilder(project.getName());
            ProjectVersion projectVersion = project.getVersion();
            if (projectVersion != null) {
                int version = DeployUtils.getNextDeploymentVersion(deployRepo, project.getName());
                sb.append('#').append(version);
            }
            DeployID id = new DeployID(sb.toString());

            String deploymentPath = DeployUtils.DEPLOY_PATH + id.getName();
            Repository designRepo = designRepository.getRepository();
            for (ProjectDescriptor<?> pd : projectDescriptors) {
                String version = pd.getProjectVersion().getVersionName();
                String projectName = pd.getProjectName();
                FileItem srcPrj = designRepo.readHistory("DESIGN/rules/" + projectName, version);
                FileData dest = new FileData();
                dest.setName(deploymentPath + "/" + projectName);
                dest.setAuthor(userName);
                dest.setComment(srcPrj.getData().getComment());
                deployRepo.save(dest, srcPrj.getStream());
            }

            // TODO: Some analogue of notifyChanges() possibly will be needed
            // rRepository.notifyChanges();
            return id;
        } catch (Exception e) {
            throw new DeploymentException("Failed to deploy: " + e.getMessage(), e);
        }
    }

    private ProductionRepositoryFactoryProxy repositoryFactoryProxy;

    public void setRepositoryFactoryProxy(ProductionRepositoryFactoryProxy repositoryFactoryProxy) {
        this.repositoryFactoryProxy = repositoryFactoryProxy;
    }

    public void setInitialProductionRepositoryConfigNames(String[] initialProductionRepositoryConfigNames) {
        this.initialProductionRepositoryConfigNames = initialProductionRepositoryConfigNames;
    }

    public void setDesignRepository(DesignTimeRepository designRepository) {
        this.designRepository = designRepository;
    }

    @Override
    public void afterPropertiesSet() {
        if (initialProductionRepositoryConfigNames != null) {
            for (String repositoryConfigName : initialProductionRepositoryConfigNames) {
                addRepository(repositoryConfigName);
            }
        }
    }
}
