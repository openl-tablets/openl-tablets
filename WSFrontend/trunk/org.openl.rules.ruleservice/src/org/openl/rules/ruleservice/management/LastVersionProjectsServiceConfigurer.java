package org.openl.rules.ruleservice.management;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.ModuleDescription;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;

/**
 * Selects the latest deployments and deploys each of their projects as single
 * service.
 * 
 * @author PUdalau
 */
public class LastVersionProjectsServiceConfigurer implements ServiceConfigurer {
    private final Log log = LogFactory.getLog(LastVersionProjectsServiceConfigurer.class);

    private boolean provideRuntimeContext;

    /** {@inheritDoc} */
    public Collection<ServiceDescription> getServicesToBeDeployed(RuleServiceLoader loader) {
        if (loader == null) {
            throw new IllegalArgumentException("loader argument can't be null");
        }

        if (log.isDebugEnabled()) {
            log.debug("Calculate services to be deployed...");
        }

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

    private Collection<ServiceDescription> createServiceDescriptions(Collection<Deployment> latestDeployments,
            RuleServiceLoader loader) {
        Collection<ServiceDescription> serviceDescriptions = new ArrayList<ServiceDescription>();
        for (Deployment deployment : latestDeployments) {
            for (AProject project : deployment.getProjects()) {
                Collection<Module> modulesOfProject = loader.resolveModulesForProject(deployment.getDeploymentName(),
                        deployment.getCommonVersion(), project.getName());
                ServiceDescription.ServiceDescriptionBuilder serviceDescriptionBuilder = new ServiceDescription.ServiceDescriptionBuilder()
                        .setProvideRuntimeContext(provideRuntimeContext);

                for (Module module : modulesOfProject) {
                    ModuleDescription moduleDescription = new ModuleDescription.ModuleDescriptionBuilder()
                            .setDeploymentName(deployment.getDeploymentName())
                            .setDeploymentVersion(deployment.getCommonVersion()).setModuleName(module.getName())
                            .setProjectName(project.getName()).build();
                    serviceDescriptionBuilder.addModule(moduleDescription);
                }

                if (!modulesOfProject.isEmpty()) {
                    String serviceName = String.format("%s_%s", deployment.getDeploymentName(), project.getName());
                    String serviceUrl = String.format("%s/%s", deployment.getDeploymentName(), project.getName());
                    serviceDescriptionBuilder.setName(serviceName).setUrl(serviceUrl);
                    serviceDescriptions.add(serviceDescriptionBuilder.build());
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
