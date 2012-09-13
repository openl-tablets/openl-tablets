package org.openl.rules.ruleservice.conf;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.xml.XmlRulesDeploySerializer;
import org.openl.rules.ruleservice.core.ModuleDescription;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;

/**
 * Selects the latest deployments and deploys each of their projects as single
 * service.
 * 
 * @author PUdalau, Marat Kamalov
 */
public class LastVersionProjectsServiceConfigurer implements ServiceConfigurer {
    private static final String RULES_DEPLOY_XML = "rules-deploy.xml";

    private final Log log = LogFactory.getLog(LastVersionProjectsServiceConfigurer.class);

    private IRulesDeploySerializer rulesDeploySerializer = new XmlRulesDeploySerializer();

    private boolean provideRuntimeContext;
    private boolean supportVariations;

    public IRulesDeploySerializer getRulesDeploySerializer() {
        return rulesDeploySerializer;
    }

    public void setRulesDeploySerializer(IRulesDeploySerializer rulesDeploySerializer) {
        if (rulesDeploySerializer == null) {
            throw new IllegalArgumentException("rulesDeploySerializer arg can't be null");
        }
        this.rulesDeploySerializer = rulesDeploySerializer;
    }

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
        Collection<ServiceDescription> serviceDescriptions = new HashSet<ServiceDescription>();
        Set<String> serviceURLs = new HashSet<String>();
        for (Deployment deployment : latestDeployments) {
            for (AProject project : deployment.getProjects()) {
                Collection<Module> modulesOfProject = loader.resolveModulesForProject(deployment.getDeploymentName(),
                        deployment.getCommonVersion(), project.getName());
                ServiceDescription.ServiceDescriptionBuilder serviceDescriptionBuilder = new ServiceDescription.ServiceDescriptionBuilder()
                        .setProvideRuntimeContext(provideRuntimeContext).setProvideVariations(supportVariations);

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

                    try {
                        AProjectArtefact artifact = project.getArtefact(RULES_DEPLOY_XML);
                        if (artifact instanceof AProjectResource) {
                            AProjectResource resource = (AProjectResource) artifact;
                            InputStream content = resource.getContent();
                            RulesDeploy rulesDeploy = getRulesDeploySerializer().deserialize(content);
                            if (rulesDeploy.getName() != null && !rulesDeploy.getName().isEmpty()) {
                                serviceDescriptionBuilder.setName(rulesDeploy.getName());
                            }
                            if (rulesDeploy.getServiceClass() != null && !rulesDeploy.getServiceClass().isEmpty()) {
                                serviceDescriptionBuilder.setServiceClassName(rulesDeploy.getServiceClass());
                            }
                            if (rulesDeploy.getUrl() != null && !rulesDeploy.getUrl().isEmpty()) {
                                serviceDescriptionBuilder.setUrl(rulesDeploy.getUrl());
                            }
                            if (rulesDeploy.isProvideRuntimeContext() != null) {
                                serviceDescriptionBuilder.setProvideRuntimeContext(rulesDeploy
                                        .isProvideRuntimeContext());
                            }
                            if (rulesDeploy.isProvideVariations() != null) {
                                serviceDescriptionBuilder.setProvideVariations(rulesDeploy.isProvideVariations());
                            }
                        }
                    } catch (ProjectException e) {
                    }
                    ServiceDescription serviceDescription = serviceDescriptionBuilder.build();
                    if (!serviceDescriptions.contains(serviceDescription)
                            && !serviceURLs.contains(serviceDescription.getUrl())) {
                        serviceURLs.add(serviceDescription.getUrl());
                        serviceDescriptions.add(serviceDescription);
                    } else {
                        if (serviceDescriptions.contains(serviceDescription)) {
                            if (log.isWarnEnabled()) {
                                log.warn(String
                                        .format("Service \"%s\" has already exists in a deployment list. The new service will be skipped. Please, use unique name for services.",
                                                serviceDescription.getName()));
                            }
                        }
                        if (serviceURLs.contains(serviceDescription.getUrl())) {
                            if (log.isWarnEnabled()) {
                                log.warn(String
                                        .format("URL \"%s\" has already registered. The new service will be skipped. Please, use unique URLs for services.",
                                                serviceDescription.getUrl()));
                            }
                        }
                    }
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

    public boolean isSupportVariations() {
        return supportVariations;
    }

    public void setSupportVariations(boolean supportVariations) {
        this.supportVariations = supportVariations;
    }
}
