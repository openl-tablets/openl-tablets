package org.openl.ruleservice.management;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.model.Module;
import org.openl.ruleservice.ServiceDescription;
import org.openl.ruleservice.ServiceDescription.ModuleConfiguration;
import org.openl.ruleservice.loader.IRulesLoader;

/**
 * Selects the latest deployments and deploys each of their projects as single
 * service.
 * 
 * @author PUdalau
 */
public class LastVersionProjectsServceConfigurer implements IServiceConfigurer {

    private boolean provideRuntimeContext;

    public List<ServiceDescription> getServicesToBeDeployed(IRulesLoader loader) {
        Map<String, Deployment> latestDeployments = new HashMap<String, Deployment>();
        for (Deployment deployment : loader.getDeployments()) {
            if (latestDeployments.containsKey(deployment.getName())) {
                if (latestDeployments.get(deployment.getName()).getCommonVersion()
                        .compareTo(deployment.getCommonVersion()) < 0) {
                    latestDeployments.put(deployment.getName(), deployment);
                }
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
                List<ModuleConfiguration> moduleConfigurations = new ArrayList<ServiceDescription.ModuleConfiguration>();
                for (Module module : modulesOfProject) {
                    moduleConfigurations.add(new ModuleConfiguration(deployment.getDeploymentName(), deployment
                            .getCommonVersion(), project.getName(), module.getName()));
                }
                new ServiceDescription(deployment.getDeploymentName(), deployment.getDeploymentName() + "/"
                        + project.getName(), null, provideRuntimeContext, moduleConfigurations);
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
