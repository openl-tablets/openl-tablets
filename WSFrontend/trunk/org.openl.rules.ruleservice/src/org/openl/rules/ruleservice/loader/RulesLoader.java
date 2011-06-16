package org.openl.rules.ruleservice.loader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.project.resolving.RulesProjectResolver;
import org.openl.rules.ruleservice.ServiceDescription;
import org.openl.rules.ruleservice.ServiceDescription.ModuleConfiguration;

/**
 * Wrapper on data source that gives access to data source and resolves the
 * OpenL projects/modules inside the projects. Contains own storage for all
 * projects that is used in services.
 * 
 * @author MKamalov
 * 
 */
public class RulesLoader implements IRulesLoader {
    private IDataSource dataSource;
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
    public RulesLoader() {
    }

    /**
     * Construct a new RulesLoader for bean usage.
     * 
     * @see #setDataSource, #setProjectResolver
     */
    public RulesLoader(IDataSource dataSource, LocalTemporaryDeploymentsStorage storage,
            RulesProjectResolver projectResolver) {
        if (dataSource == null || storage == null || projectResolver == null){
            throw new IllegalArgumentException();
        }
        this.dataSource = dataSource;
        this.storage = storage;
        this.projectResolver = projectResolver;
    }

    /** {@inheritDoc} */
    public IDataSource getDataSource() {
        return dataSource;
    }

    /**
     * Sets data source
     */
    public void setDataSource(IDataSource dataSource) {
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
        this.storage = storage;
    }

    /** {@inheritDoc} */
    public List<Deployment> getDeployments() {
        return getDataSource().getDeployments();
    }

    /** {@inheritDoc} */
    public Deployment getDeployment(String deploymentName, CommonVersion deploymentVersion) {
        if (deploymentName == null || deploymentVersion == null)
            throw new IllegalArgumentException();
        Deployment deployment = getDataSource().getDeployment(deploymentName, deploymentVersion);
        Deployment localDeployment = storage.getDeployment(deploymentName, deploymentVersion);
        if (localDeployment == null) {
            return deployment;
        }
        return localDeployment;
    }

    /** {@inheritDoc} */
    public List<Module> resolveModulesForProject(String deploymentName, CommonVersion deploymentVersion,
            String projectName) {
        if (deploymentName == null || deploymentVersion == null || projectName == null)
            throw new IllegalArgumentException();
        Deployment deployment = getDataSource().getDeployment(deploymentName, deploymentVersion);
        Deployment localDeployment = storage.getDeployment(deploymentName, deploymentVersion);
        if (localDeployment == null) {
            localDeployment = storage.loadDeployment(deployment);
        }
        AProject project = localDeployment.getProject(projectName);
        String artefactPath = storage.getDirectoryToLoadDeploymentsIn() + project.getArtefactPath().getStringValue();
        File projectFolder = new File(artefactPath);
        ResolvingStrategy resolvingStrategy = projectResolver.isRulesProject(projectFolder);
        if (resolvingStrategy != null) {
            ProjectDescriptor projectDescriptor = resolvingStrategy.resolveProject(projectFolder);
            return Collections.unmodifiableList(projectDescriptor.getModules());
        } else {
            return Collections.emptyList();
        }
    }

    /** {@inheritDoc} */
    public List<Module> getModulesForService(ServiceDescription serviceDescription) {
        List<Module> ret = new ArrayList<Module>();
        List<ModuleConfiguration> modulesToLoad = serviceDescription.getModulesToLoad();
        for (ModuleConfiguration moduleConfiguration : modulesToLoad) {
            String deploymentName = moduleConfiguration.getDeploymentName();
            CommonVersion commonVersion = moduleConfiguration.getDeploymentVersion();
            Deployment deployment = getDataSource().getDeployment(deploymentName, commonVersion);
            Deployment localDeployment = storage.getDeployment(deploymentName, commonVersion);
            if (localDeployment == null) {
                localDeployment = storage.loadDeployment(deployment);
            }
            AProject project = localDeployment.getProject(moduleConfiguration.getProjectName());
            String artefactPath = storage.getDirectoryToLoadDeploymentsIn()
                    + project.getArtefactPath().getStringValue();
            File projectFolder = new File(artefactPath);
            ResolvingStrategy resolvingStrategy = projectResolver.isRulesProject(projectFolder);
            if (resolvingStrategy != null) {
                ProjectDescriptor projectDescriptor = resolvingStrategy.resolveProject(projectFolder);
                List<Module> modules = projectDescriptor.getModules();
                for (Module module : modules) {
                    if (moduleConfiguration.getModuleName().equals(module.getName())) {
                        ret.add(module);
                    }
                }
            }
        }
        return ret;
    }
}
