package org.openl.ruleservice.loader;

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
import org.openl.ruleservice.ServiceDescription;
import org.openl.ruleservice.ServiceDescription.ModuleConfiguration;

public class RulesLoader implements IRulesLoader {
    private IDataSource dataSource;
    private RulesProjectResolver projectResolver;
    private LocalTemporaryDeploymentsStorage storage;

    /**
     * Construct a new RulesLoader for bean usage.
     * <p>Note: The dataSource, storage and projectResolver have to be set before using the instance.
     * @see #setDataSource, #setProjectResolver 
     */
    public RulesLoader() {
    }
    
    public RulesLoader(IDataSource dataSource, LocalTemporaryDeploymentsStorage storage,
            RulesProjectResolver projectResolver) {
        if (dataSource == null || storage == null || projectResolver == null)
            throw new IllegalArgumentException();
        this.dataSource = dataSource;
        this.storage = storage;
        this.projectResolver = projectResolver;
    }

    public IDataSource getDataSource() {
        return dataSource;
    }
    
    public void setDataSource(IDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public RulesProjectResolver getProjectResolver() {
        return projectResolver;
    }
    
    public void setProjectResolver(RulesProjectResolver projectResolver) {
        this.projectResolver = projectResolver;
    }

    public List<Deployment> getDeployments() {
        return getDataSource().getDeployments();
    }
    
    public LocalTemporaryDeploymentsStorage getStorage() {
        return storage;
    }
    
    public void setStorage(LocalTemporaryDeploymentsStorage storage) {
        this.storage = storage;
    }

    public Deployment getDeployment(String deploymentName, CommonVersion deploymentVersion) {
        Deployment deployment = getDataSource().getDeployment(deploymentName, deploymentVersion);
        Deployment localDeployment = storage.getDeployment(deploymentName, deploymentVersion);
        if (localDeployment == null) {
            return deployment;
        }
        return localDeployment;
    }

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
