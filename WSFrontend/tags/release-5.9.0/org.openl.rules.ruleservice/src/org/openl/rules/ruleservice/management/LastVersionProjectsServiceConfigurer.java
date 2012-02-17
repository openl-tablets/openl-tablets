package org.openl.rules.ruleservice.management;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.ModuleConfiguration;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.loader.IRulesLoader;

/**
 * Selects the latest deployments and deploys each of their projects as single
 * service.
 * 
 * @author PUdalau
 */
public class LastVersionProjectsServiceConfigurer implements IServiceConfigurer {
    private Log log = LogFactory.getLog(LastVersionProjectsServiceConfigurer.class);
    
    private boolean provideRuntimeContext;

    /** {@inheritDoc} */
    public List<ServiceDescription> getServicesToBeDeployed(IRulesLoader loader) {
        if (loader == null){
            throw new IllegalArgumentException("loader argument can't be null");
        }
        
        log.debug("Calculate services to be deployed...");
        
        Map<String, Deployment> latestDeployments = new HashMap<String, Deployment>();
        for (Deployment deployment : loader.getDeployments()) {
            String deploymentName = deployment.getDeploymentName();
            if (latestDeployments.containsKey(deploymentName)) {
                if (latestDeployments.get(deploymentName).getCommonVersion().compareTo(deployment.getCommonVersion()) < 0) {
                    latestDeployments.put(deploymentName, deployment);
                }
            } else {
                latestDeployments.put(deploymentName, deployment);
            }
        }
        return createServiceDescriptions(latestDeployments.values(), loader);
    }

    private List<ServiceDescription> createServiceDescriptions(Collection<Deployment> latestDeployments,
            IRulesLoader loader) {
        List<ServiceDescription> serviceDescriptions = new ArrayList<ServiceDescription>();
        for (Deployment deployment : latestDeployments) {
            for (AProject project : deployment.getProjects()) {
                List<Module> modulesOfProject = loader.resolveModulesForProject(deployment.getDeploymentName(),
                        deployment.getCommonVersion(), project.getName());
                List<ModuleConfiguration> moduleConfigurations = new ArrayList<ModuleConfiguration>();
                for (Module module : modulesOfProject) {
                    moduleConfigurations.add(new ModuleConfiguration(deployment.getDeploymentName(), deployment
                            .getCommonVersion(), project.getName(), module.getName()));
                }
                if (!moduleConfigurations.isEmpty()) {
                    String serviceName = String.format("%s_%s", deployment.getDeploymentName(), project.getName());
                    String serviceUrl = String.format("%s/%s", deployment.getDeploymentName(), project.getName());
                    serviceDescriptions.add(new ServiceDescription(serviceName,
                        serviceUrl,
                        null,
                        provideRuntimeContext,
                        moduleConfigurations));
                }
            }
        }
        return serviceDescriptions;
    }

    public boolean isProvideRuntimeContext() {
        return provideRuntimeContext;
    }

    public void setProvideRuntimeContext(boolean provideRuntimeContext) {
        this.provideRuntimeContext = provideRuntimeContext;
    }

}
