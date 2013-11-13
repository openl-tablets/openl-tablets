package org.openl.rules.ruleservice.loader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.project.resolving.RulesProjectResolver;
import org.openl.rules.ruleservice.core.ModuleDescription;
import org.openl.rules.ruleservice.core.RuleServiceRuntimeException;
import org.openl.rules.ruleservice.core.ServiceDescription;

/**
 * Wrapper on data source that gives access to data source and resolves the
 * OpenL projects/modules inside the projects. Contains own storage for all
 * projects that is used in services.
 * 
 * @author Marat Kamalov
 * 
 */
public class RuleServiceLoaderImpl implements RuleServiceLoader {
    private final Log log = LogFactory.getLog(RuleServiceLoaderImpl.class);

    private DataSource dataSource;

    private RulesProjectResolver projectResolver;

    private LocalTemporaryDeploymentsStorage storage;

    /**
     * Construct a new RulesLoader for bean usage.
     * <p>
     * Note: The dataSource, storage and projectResolver have to be set before
     * using the instance.
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
            RulesProjectResolver projectResolver) {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource argument can't be null");
        }
        if (storage == null) {
            throw new IllegalArgumentException("storage argument can't be null");
        }
        if (projectResolver == null) {
            throw new IllegalArgumentException("projectResolver argument can't be null");
        }

        this.dataSource = dataSource;
        this.storage = storage;
        this.projectResolver = projectResolver;
    }

    /** {@inheritDoc} */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Sets data source.
     */
    public void setDataSource(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource argument can't be null");
        }

        this.dataSource = dataSource;
    }

    /**
     * Gets rules project resolver.
     */
    public RulesProjectResolver getProjectResolver() {
        return projectResolver;
    }

    /**
     * Sets rules project resolver. Spring bean configuration property.
     * 
     * @param projectResolver
     */
    public void setProjectResolver(RulesProjectResolver projectResolver) {
        if (projectResolver == null) {
            throw new IllegalArgumentException("projectResolver argument can't be null");
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
            throw new IllegalArgumentException("storage argument can't be null");
        }

        this.storage = storage;
    }

    /** {@inheritDoc} */
    public Collection<Deployment> getDeployments() {
        return getDataSource().getDeployments();
    }

    /** {@inheritDoc} */
    public Deployment getDeployment(String deploymentName, CommonVersion deploymentVersion) {
        if (deploymentName == null) {
            throw new IllegalArgumentException("deploymentName argument can't be null");
        }
        if (deploymentVersion == null) {
            throw new IllegalArgumentException("deploymentVersion argument can't be null");
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("Getting deployement with name=\"%s\" and version=\"%s\"",
                deploymentName,
                deploymentVersion.getVersionName()));
        }

        Deployment localDeployment = storage.getDeployment(deploymentName, deploymentVersion);
        if (localDeployment == null) {
            Deployment deployment = getDataSource().getDeployment(deploymentName, deploymentVersion);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Deployement with name=\"%s\" and version=\"%s\" has been returned from data source",
                    deploymentName,
                    deploymentVersion.getVersionName()));
            }
            return deployment;
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("Deployement with name=\"%s\" and version=\"%s\" has been returned from local repository",
                deploymentName,
                deploymentVersion.getVersionName()));
        }
        return localDeployment;
    }

    /** {@inheritDoc} */
    public Collection<Module> resolveModulesForProject(String deploymentName,
            CommonVersion deploymentVersion,
            String projectName) {
        if (deploymentName == null) {
            throw new IllegalArgumentException("deploymentName argument can't be null");
        }
        if (deploymentVersion == null) {
            throw new IllegalArgumentException("deploymentVersion argument can't be null");
        }
        if (projectName == null) {
            throw new IllegalArgumentException("projectName argument can't be null");
        }

        if (log.isDebugEnabled()) {
            log.debug("Resoliving modules for deployment with name=" + deploymentName + " and version=" + deploymentVersion.getVersionName() + " and projectName=" + projectName);
        }

        Deployment localDeployment = storage.getDeployment(deploymentName, deploymentVersion);
        if (localDeployment == null) {
            Deployment deployment = getDataSource().getDeployment(deploymentName, deploymentVersion);
            localDeployment = storage.loadDeployment(deployment);
        }

        AProject project = localDeployment.getProject(projectName);
        String artefactPath = storage.getDirectoryToLoadDeploymentsIn() + project.getArtefactPath().getStringValue();
        File projectFolder = new File(artefactPath);
        ResolvingStrategy resolvingStrategy = projectResolver.isRulesProject(projectFolder);
        if (resolvingStrategy != null) {
            ProjectDescriptor projectDescriptor = null;
            try{
                projectDescriptor= resolvingStrategy.resolveProject(projectFolder);
            }catch(ProjectResolvingException e){
                throw new RuleServiceRuntimeException("Project resolving failed!", e);
            }
            return Collections.unmodifiableList(projectDescriptor.getModules());
        } else {
            return Collections.emptyList();
        }
    }

    /** {@inheritDoc} */
    public Collection<Module> getModulesByServiceDescription(ServiceDescription serviceDescription) {
        if (serviceDescription == null) {
            throw new IllegalArgumentException("serviceDescription argument can't be null");
        }

        if (log.isDebugEnabled()) {
            log.debug("Resoliving modules for service with name=" + serviceDescription.getName());
        }

        Collection<Module> ret = new ArrayList<Module>();
        Collection<ModuleDescription> modulesToLoad = serviceDescription.getModules();
        for (ModuleDescription moduleDescription : modulesToLoad) {
            String deploymentName = serviceDescription.getDeployment().getName();
            CommonVersion commonVersion = serviceDescription.getDeployment().getVersion();
            Deployment deployment = getDataSource().getDeployment(deploymentName, commonVersion);
            Deployment localDeployment = storage.getDeployment(deploymentName, commonVersion);
            if (localDeployment == null) {
                localDeployment = storage.loadDeployment(deployment);
            }
            AProject project = localDeployment.getProject(moduleDescription.getProjectName());
            if (project == null) {
                throw new RuleServiceRuntimeException("Deployment \"" + deploymentName + "\" doesn't contain a project with name \"" + moduleDescription.getProjectName() + "\"!");
            }
            String artefactPath = storage.getDirectoryToLoadDeploymentsIn() + project.getArtefactPath()
                .getStringValue();
            File projectFolder = new File(artefactPath);
            ResolvingStrategy resolvingStrategy = projectResolver.isRulesProject(projectFolder);
            if (resolvingStrategy != null) {
                ProjectDescriptor projectDescriptor = null;
                try{
                    projectDescriptor= resolvingStrategy.resolveProject(projectFolder);
                }catch(ProjectResolvingException e){
                    throw new RuleServiceRuntimeException("Project resolving failed!", e);
                }
                Collection<Module> modules = projectDescriptor.getModules();
                for (Module module : modules) {
                    if (moduleDescription.getModuleName().equals(module.getName())) {
                        ret.add(module);
                    }
                }
            }
        }
        return Collections.unmodifiableCollection(ret);
    }
}
