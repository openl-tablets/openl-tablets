package org.openl.rules.ruleservice.loader;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.rules.ruleservice.core.RuleServiceRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Wrapper on data source that gives access to data source and resolves the OpenL projects/modules inside the projects.
 * Contains own storage for all projects that is used in services.
 *
 * @author Marat Kamalov
 */
public class RuleServiceLoaderImpl implements RuleServiceLoader {
    private final Logger log = LoggerFactory.getLogger(RuleServiceLoaderImpl.class);

    private DataSource dataSource;

    private ProjectResolver projectResolver;

    private LocalTemporaryDeploymentsStorage storage;

    /**
     * Construct a new RulesLoader for bean usage.
     * <p>
     * Note: The dataSource, storage and projectResolver have to be set before using the instance.
     * </p>
     *
     * @see #setDataSource, #setProjectResolver
     */
    public RuleServiceLoaderImpl() {
    }

    /**
     * Construct a new RulesLoader for bean usage.
     *
     * @see #setDataSource, #setProjectResolver
     */
    public RuleServiceLoaderImpl(DataSource dataSource,
            LocalTemporaryDeploymentsStorage storage,
            ProjectResolver projectResolver) {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource argument must not be null!");
        }
        if (storage == null) {
            throw new IllegalArgumentException("storage argument must not be null!");
        }
        if (projectResolver == null) {
            throw new IllegalArgumentException("projectResolver argument must not be null!");
        }

        this.dataSource = dataSource;
        this.storage = storage;
        this.projectResolver = projectResolver;
    }

    @Override
    public void setListener(DataSourceListener dataSourceListener) {
        dataSource.setListener(dataSourceListener);
    }

    /**
     * Sets data source.
     */
    public void setDataSource(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource argument must not be null!");
        }

        this.dataSource = dataSource;
    }

    /**
     * Gets rules project resolver.
     */
    public ProjectResolver getProjectResolver() {
        return projectResolver;
    }

    /**
     * Sets rules project resolver. Spring bean configuration property.
     *
     * @param projectResolver
     */
    public void setProjectResolver(ProjectResolver projectResolver) {
        if (projectResolver == null) {
            throw new IllegalArgumentException("projectResolver argument must not be null!");
        }

        this.projectResolver = projectResolver;
    }

    /**
     * Gets storage.
     */
    public LocalTemporaryDeploymentsStorage getStorage() {
        return storage;
    }

    /**
     * Sets storage. Spring bean configuration property.
     */
    public void setStorage(LocalTemporaryDeploymentsStorage storage) {
        if (storage == null) {
            throw new IllegalArgumentException("storage argument must not be null!");
        }

        this.storage = storage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Deployment> getDeployments() {
        return dataSource.getDeployments();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Module> resolveModulesForProject(String deploymentName,
            CommonVersion deploymentVersion,
            String projectName) {
        if (deploymentName == null) {
            throw new IllegalArgumentException("deploymentName argument must not be null!");
        }
        if (deploymentVersion == null) {
            throw new IllegalArgumentException("deploymentVersion argument must not be null!");
        }
        if (projectName == null) {
            throw new IllegalArgumentException("projectName argument must not be null!");
        }

        log.debug("Resoliving modules for deployment (name='{}', version='{}', projectName='{}')",
            deploymentName,
            deploymentVersion.getVersionName(),
            projectName);

        Deployment localDeployment = getDeploymentFromStorage(deploymentName, deploymentVersion);
        AProject project = localDeployment.getProject(projectName);
        if (project == null) {
            throw new RuleServiceRuntimeException(
                "Deployment '" + deploymentName + "' doesn't contain a project '" + projectName + "'!");
        }
        String artefactPath = storage.getDirectoryToLoadDeploymentsIn() + project.getArtefactPath().getStringValue();
        File projectFolder = new File(artefactPath);
        List<Module> result = Collections.emptyList();
        try {
            ProjectDescriptor projectDescriptor = projectResolver.resolve(projectFolder);
            if (projectDescriptor != null) {
                List<Module> modules = projectDescriptor.getModules();
                result = Collections.unmodifiableList(modules);
            }
        } catch (ProjectResolvingException e) {
            log.error("Project resolving has been failed!", e);
        }
        return result;
    }

    private Deployment getDeploymentFromStorage(String deploymentName, CommonVersion commonVersion) {
        Deployment localDeployment = storage.getDeployment(deploymentName, commonVersion);
        if (localDeployment == null) {
            Deployment deployment = dataSource.getDeployment(deploymentName, commonVersion);
            localDeployment = storage.loadDeployment(deployment);
        }
        return localDeployment;
    }
}
