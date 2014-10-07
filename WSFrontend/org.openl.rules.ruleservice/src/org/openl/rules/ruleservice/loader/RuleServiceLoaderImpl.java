package org.openl.rules.ruleservice.loader;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wrapper on data source that gives access to data source and resolves the
 * OpenL projects/modules inside the projects. Contains own storage for all
 * projects that is used in services.
 *
 * @author Marat Kamalov
 */
public class RuleServiceLoaderImpl implements RuleServiceLoader {
    private final Logger log = LoggerFactory.getLogger(RuleServiceLoaderImpl.class);

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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    public Collection<Deployment> getDeployments() {
        return getDataSource().getDeployments();
    }

    /**
     * {@inheritDoc}
     */
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

        log.debug("Resoliving modules for deployment with name={} and version={} and projectName={}",
                deploymentName,
                deploymentVersion.getVersionName(),
                projectName);

        Deployment localDeployment = getDeploymentFromStorage(deploymentName, deploymentVersion);

        AProject project = localDeployment.getProject(projectName);
        String artefactPath = storage.getDirectoryToLoadDeploymentsIn() + project.getArtefactPath().getStringValue();
        File projectFolder = new File(artefactPath);
        ResolvingStrategy resolvingStrategy = projectResolver.isRulesProject(projectFolder);
        if (resolvingStrategy != null) {
            ProjectDescriptor projectDescriptor = null;
            try {
                projectDescriptor = resolvingStrategy.resolveProject(projectFolder);
            } catch (ProjectResolvingException e) {
                log.error("Project resolving failed!", e);
                return Collections.emptyList();
            }
            List<Module> modules = projectDescriptor.getModules();
            return Collections.unmodifiableList(modules);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Collection<Module> getModulesByServiceDescription(ServiceDescription serviceDescription) {
        if (serviceDescription == null) {
            throw new IllegalArgumentException("serviceDescription argument can't be null");
        }

        log.debug("Resoliving modules for service with name={}", serviceDescription.getName());

        Map<String, Collection<ModuleDescription>> projectModules = new HashMap<String, Collection<ModuleDescription>>();

        Collection<ModuleDescription> modulesToLoad = serviceDescription.getModules();
        for (ModuleDescription moduleDescription : modulesToLoad) {
            String projectName = moduleDescription.getProjectName();
            if (projectModules.containsKey(projectName)) {
                Collection<ModuleDescription> modules = projectModules.get(projectName);
                modules.add(moduleDescription);
            } else {
                Collection<ModuleDescription> modules = new ArrayList<ModuleDescription>();
                modules.add(moduleDescription);
                projectModules.put(projectName, modules);
            }
        }

        String deploymentName = serviceDescription.getDeployment().getName();
        CommonVersion commonVersion = serviceDescription.getDeployment().getVersion();
        Deployment localDeployment = getDeploymentFromStorage(deploymentName, commonVersion);

        Collection<Module> ret = new ArrayList<Module>();
        for (String projectName : projectModules.keySet()) {
            AProject project = localDeployment.getProject(projectName);
            if (project == null) {
                throw new RuleServiceRuntimeException("Deployment \"" + deploymentName + "\" doesn't contain a project with name \"" + projectName + "\"!");
            }
            String artefactPath = storage.getDirectoryToLoadDeploymentsIn() + project.getArtefactPath()
                    .getStringValue();
            File projectFolder = new File(artefactPath);
            ResolvingStrategy resolvingStrategy = projectResolver.isRulesProject(projectFolder);
            if (resolvingStrategy != null) {
                ProjectDescriptor projectDescriptor = null;
                try {
                    projectDescriptor = resolvingStrategy.resolveProject(projectFolder);
                } catch (ProjectResolvingException e) {
                    log.error("Project resolving failed!", e);
                    return Collections.emptyList();
                }
                Collection<Module> modules = projectDescriptor.getModules();
                for (ModuleDescription moduleDescription : projectModules.get(projectName)) {
                    for (Module module : modules) {
                        if (moduleDescription.getModuleName().equals(module.getName())) {
                            ret.add(module);
                        }
                    }
                }
            }
        }
        return Collections.unmodifiableCollection(ret);
    }

    private Deployment getDeploymentFromStorage(String deploymentName, CommonVersion commonVersion) {
        Deployment localDeployment = storage.getDeployment(deploymentName, commonVersion);
        if (localDeployment == null) {
            Deployment deployment = getDataSource().getDeployment(deploymentName, commonVersion);
            localDeployment = storage.loadDeployment(deployment);
        }
        return localDeployment;
    }
}
