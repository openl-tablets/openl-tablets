package org.openl.rules.webstudio.web.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.deploy.DeploymentException;
import org.openl.rules.workspace.deploy.ProductionDeployer;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.RepositoryException;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Deployment manager
 *
 * @author Andrey Naumenko
 */
public class DeploymentManager {
    private static final Log LOG = LogFactory.getLog(DeploymentManager.class);

    private ProductionDeployer deployer;

    public DeployID deploy(ADeploymentProject project) throws RepositoryException, DeploymentException {
        DesignTimeRepository dtr = RepositoryUtils.getWorkspace().getDesignTimeRepository();

        Collection<ProjectDescriptor> projectDescriptors = project.getProjectDescriptors();
        Collection<AProject> projects = new ArrayList<AProject>();

        for (ProjectDescriptor pd : projectDescriptors) {
            projects.add(dtr.getProject(pd.getProjectName(), pd.getProjectVersion()));
        }

        DeployID id = RepositoryUtils.getDeployID(project);
        deployer.deploy(id, projects);
        if (LOG.isDebugEnabled()) {
            String msg = "Project '" + project.getName() + "' successfully deployed with id:" + id.getName();
            LOG.debug(msg);
        }

        return id;
    }

    public void setDeployer(ProductionDeployer deployer) {
        this.deployer = deployer;
    }
}
