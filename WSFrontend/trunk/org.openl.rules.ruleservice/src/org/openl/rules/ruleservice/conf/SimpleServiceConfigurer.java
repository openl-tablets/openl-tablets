package org.openl.rules.ruleservice.conf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.ModuleDescription;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;

/**
 * Simple ServiceConfigurer bean is designed for using with data sources with
 * one deployment. Exposes only one project from this deployment. All service
 * properties defined in this configurer.
 * 
 * @author Marat Kamalov
 * 
 */
public class SimpleServiceConfigurer implements ServiceConfigurer {
    private final Log log = LogFactory.getLog(SimpleServiceConfigurer.class);

    private String serviceName;
    private String projectName;
    private String serviceUrl;
    private String serviceClassName;
    private boolean provideRuntimeContext;
    private boolean supportVariations;

    /** {@inheritDoc} */
    public Collection<ServiceDescription> getServicesToBeDeployed(RuleServiceLoader loader) {
        Collection<ServiceDescription> serviceDescriptions = new ArrayList<ServiceDescription>();
        if (loader.getDeployments().size() > 0) {
            if (log.isErrorEnabled()) {
                log.error("This configurer can be used only in case with one deployment.");
            }
        }
        for (Deployment deployment : loader.getDeployments()) {
            Set<ModuleDescription> modulesInDeployment = new HashSet<ModuleDescription>();
            for (AProject project : deployment.getProjects()) {
                Collection<Module> modulesOfProject = loader.resolveModulesForProject(deployment.getDeploymentName(),
                        deployment.getCommonVersion(), project.getName());

                for (Module module : modulesOfProject) {
                    ModuleDescription.ModuleDescriptionBuilder moduleDescriptionBuilder = new ModuleDescription.ModuleDescriptionBuilder();
                    moduleDescriptionBuilder.setDeploymentName(deployment.getDeploymentName());
                    moduleDescriptionBuilder.setDeploymentVersion(deployment.getCommonVersion());
                    moduleDescriptionBuilder.setProjectName(project.getName());
                    moduleDescriptionBuilder.setModuleName(module.getName());
                    ModuleDescription moduleDescription = moduleDescriptionBuilder.build();
                    modulesInDeployment.add(moduleDescription);
                }
            }

            for (AProject project : deployment.getProjects()) {
                Collection<ModuleDescription> modulesForService = new ArrayList<ModuleDescription>();
                for (Module module : loader.resolveModulesForProject(deployment.getDeploymentName(),
                        deployment.getCommonVersion(), project.getName())) {

                    if (module.getProject().getId().equals(projectName)) {
                        ModuleDescription.ModuleDescriptionBuilder moduleDescriptionBuilder = new ModuleDescription.ModuleDescriptionBuilder();
                        moduleDescriptionBuilder.setDeploymentName(deployment.getDeploymentName());
                        moduleDescriptionBuilder.setDeploymentVersion(deployment.getCommonVersion());
                        moduleDescriptionBuilder.setProjectName(project.getName());
                        moduleDescriptionBuilder.setModuleName(module.getName());
                        ModuleDescription moduleDescription = moduleDescriptionBuilder.build();
                        modulesForService.add(moduleDescription);
                    }
                }
                if (!modulesForService.isEmpty()) {
                    ServiceDescription.ServiceDescriptionBuilder serviceDescriptionBuilder = new ServiceDescription.ServiceDescriptionBuilder();
                    serviceDescriptionBuilder.setModules(modulesInDeployment);
                    serviceDescriptionBuilder.setModulesInService(modulesForService);
                    serviceDescriptionBuilder.setName(serviceName);
                    serviceDescriptionBuilder.setUrl(serviceUrl);
                    serviceDescriptionBuilder.setServiceClassName(serviceClassName);
                    serviceDescriptionBuilder.setProvideRuntimeContext(provideRuntimeContext);
                    serviceDescriptionBuilder.setProvideVariations(supportVariations);
                    serviceDescriptions.add(serviceDescriptionBuilder.build());
                }
            }
        }
        return serviceDescriptions;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName arg can't be null");
        }
        this.serviceName = serviceName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        if (projectName == null) {
            throw new IllegalArgumentException("projectName arg can't be null");
        }
        this.projectName = projectName;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public String getServiceClassName() {
        return serviceClassName;
    }

    public void setServiceClassName(String serviceClassName) {
        this.serviceClassName = serviceClassName;
    }

    public boolean isProvideRuntimeContext() {
        return provideRuntimeContext;
    }

    public void setProvideRuntimeContext(boolean provideRuntimeContext) {
        this.provideRuntimeContext = provideRuntimeContext;
    }

    public boolean isSupportVariations() {
        return supportVariations;
    }

    public void setSupportVariations(boolean supportVariations) {
        this.supportVariations = supportVariations;
    }
}
