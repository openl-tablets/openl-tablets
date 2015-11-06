package org.openl.rules.ruleservice.conf;

import org.apache.commons.lang3.StringUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.xml.XmlRulesDeploySerializer;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Selects the latest deployments and deploys each of their projects as single
 * service.
 *
 * @author PUdalau, Marat Kamalov
 */
public class LastVersionProjectsServiceConfigurer implements ServiceConfigurer {

    public static final String RULES_DEPLOY_XML = "rules-deploy.xml";

    private final Logger log = LoggerFactory.getLogger(LastVersionProjectsServiceConfigurer.class);

    private IRulesDeploySerializer rulesDeploySerializer = new XmlRulesDeploySerializer();
    private boolean provideRuntimeContext = false;
    private boolean supportVariations = false;
    private boolean useRuleServiceRuntimeContext = false;

    private Collection<Deployment> filterDeployments(Collection<Deployment> deployments) {
        Map<String, Map<String, Deployment>> latestDeployments = new HashMap<String, Map<String, Deployment>>();
        for (Deployment deployment : deployments) {
            String deploymentName = deployment.getDeploymentName();
            Map<String, Deployment> internalMap = latestDeployments.get(deploymentName);
            if (internalMap == null) {
                internalMap = new HashMap<String, Deployment>();
                latestDeployments.put(deploymentName, internalMap);
            }
            boolean hasRulesDeployXML = false;
            for (AProject project : deployment.getProjects()) {
                try {
                    InputStream content = null;
                    RulesDeploy rulesDeploy = null;
                    try {
                        AProjectArtefact artifact = project.getArtefact(RULES_DEPLOY_XML);
                        if (artifact instanceof AProjectResource) {
                            AProjectResource resource = (AProjectResource) artifact;
                            content = resource.getContent();
                            rulesDeploy = getRulesDeploySerializer().deserialize(content);
                            hasRulesDeployXML = true;
                            String version = null;
                            if (!StringUtils.isEmpty(rulesDeploy.getVersion())) {
                                version = rulesDeploy.getVersion();
                            }
                            if (latestDeployments.containsKey(deploymentName)) {
                                if (internalMap.containsKey(version)) {
                                    if (internalMap.get(version)
                                        .getCommonVersion()
                                        .compareTo(deployment.getCommonVersion()) < 0) {
                                        internalMap.put(version, deployment);
                                    }
                                } else {
                                    internalMap.put(version, deployment);
                                }
                            } else {
                                internalMap.put(version, deployment);
                            }
                        }
                    } catch (ProjectException e) {
                    } finally {
                        if (content != null) {
                            try {
                                content.close();
                            } catch (IOException e) {
                                log.error(e.getMessage(), e);
                            }
                        }
                    }
                } catch (Throwable e) {
                    log.error(
                        "Project loading from repository was failed! Project with name \"{}\" in deployment \"{}\" was skipped!",
                        project.getName(),
                        deployment.getDeploymentName(),
                        e);
                }
            }
            if (!hasRulesDeployXML) {
                if (internalMap.containsKey(null)) {
                    if (internalMap.get(null).getCommonVersion().compareTo(deployment.getCommonVersion()) < 0) {
                        internalMap.put(null, deployment);
                    }
                } else {
                    internalMap.put(null, deployment);
                }
            }
        }

        Collection<Deployment> ret = new ArrayList<Deployment>();
        for (String key : latestDeployments.keySet()) {
            for (Deployment d : latestDeployments.get(key).values()) {
                ret.add(d);
            }
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Collection<ServiceDescription> getServicesToBeDeployed(RuleServiceLoader ruleServiceLoader) {
        log.debug("Calculate services to be deployed...");

        Collection<Deployment> allDeployments = ruleServiceLoader.getDeployments();
        Collection<Deployment> deployments = filterDeployments(allDeployments);

        Collection<ServiceDescription> serviceDescriptions = new HashSet<ServiceDescription>();
        Set<String> serviceURLs = new HashSet<String>();
        for (Deployment deployment : deployments) {
            DeploymentDescription deploymentDescription = new DeploymentDescription(deployment.getDeploymentName(),
                deployment.getCommonVersion());
            for (AProject project : deployment.getProjects()) {
                try {
                    Collection<Module> modulesOfProject = ruleServiceLoader.resolveModulesForProject(
                        deployment.getDeploymentName(), deployment.getCommonVersion(), project.getName());
                    ServiceDescription.ServiceDescriptionBuilder serviceDescriptionBuilder = new ServiceDescription.ServiceDescriptionBuilder()
                        .setProvideRuntimeContext(provideRuntimeContext)
                        .setProvideVariations(supportVariations)
                        .setDeployment(deploymentDescription)
                        .setUseRuleServiceRuntimeContext(useRuleServiceRuntimeContext);

                    serviceDescriptionBuilder.setModules(modulesOfProject);

                    if (!modulesOfProject.isEmpty()) {
                        InputStream content = null;
                        RulesDeploy rulesDeploy = null;
                        try {
                            AProjectArtefact artifact = project.getArtefact(RULES_DEPLOY_XML);
                            if (artifact instanceof AProjectResource) {
                                AProjectResource resource = (AProjectResource) artifact;
                                content = resource.getContent();
                                rulesDeploy = getRulesDeploySerializer().deserialize(content);
                                if (rulesDeploy.getServiceClass() != null && !rulesDeploy.getServiceClass()
                                    .trim()
                                    .isEmpty()) {
                                    serviceDescriptionBuilder.setServiceClassName(rulesDeploy.getServiceClass().trim());
                                }
                                if (rulesDeploy.getRmiServiceClass() != null && !rulesDeploy.getRmiServiceClass()
                                    .trim()
                                    .isEmpty()) {
                                    serviceDescriptionBuilder
                                        .setRmiServiceClassName(rulesDeploy.getRmiServiceClass().trim());
                                }
                                if (rulesDeploy.isProvideRuntimeContext() != null) {
                                    serviceDescriptionBuilder
                                        .setProvideRuntimeContext(rulesDeploy.isProvideRuntimeContext());
                                }
                                if (rulesDeploy.isProvideVariations() != null) {
                                    serviceDescriptionBuilder.setProvideVariations(rulesDeploy.isProvideVariations());
                                }
                                if (rulesDeploy.isUseRuleServiceRuntimeContext() != null) {
                                    serviceDescriptionBuilder
                                        .setUseRuleServiceRuntimeContext(rulesDeploy.isUseRuleServiceRuntimeContext());
                                }
                                if (rulesDeploy.getPublishers() != null) {
                                    for (RulesDeploy.PublisherType key : rulesDeploy.getPublishers()) {
                                        serviceDescriptionBuilder.addPublisher(key.toString());
                                    }
                                }
                                if (rulesDeploy.getConfiguration() != null) {
                                    serviceDescriptionBuilder.setConfiguration(rulesDeploy.getConfiguration());
                                }
                                if (rulesDeploy.getInterceptingTemplateClassName() != null && !rulesDeploy
                                    .getInterceptingTemplateClassName().trim().isEmpty()) {
                                    serviceDescriptionBuilder.setAnnotationTemplateClassName(
                                        rulesDeploy.getInterceptingTemplateClassName().trim());
                                }
                                if (rulesDeploy.getAnnotationTemplateClassName() != null && !rulesDeploy
                                    .getAnnotationTemplateClassName().trim().isEmpty()) {
                                    serviceDescriptionBuilder.setAnnotationTemplateClassName(
                                        rulesDeploy.getAnnotationTemplateClassName().trim());
                                }
                            }
                        } catch (ProjectException e) {
                        } finally {
                            if (content != null) {
                                try {
                                    content.close();
                                } catch (IOException e) {
                                    log.error(e.getMessage(), e);
                                }
                            }
                        }
                        serviceDescriptionBuilder.setName(buildServiceName(deployment, project, rulesDeploy));
                        serviceDescriptionBuilder.setUrl(buildServiceUrl(deployment, project, rulesDeploy));
                        ServiceDescription serviceDescription = serviceDescriptionBuilder.build();

                        if (!serviceDescriptions.contains(serviceDescription) && !serviceURLs
                            .contains(serviceDescription.getUrl())) {
                            serviceURLs.add(serviceDescription.getUrl());
                            serviceDescriptions.add(serviceDescription);
                        } else {
                            if (serviceDescriptions.contains(serviceDescription)) {
                                log.warn(
                                    "Service \"{}\" has already exists in a deployment list. The second service will be skipped. Please, use unique name for services.",
                                    serviceDescription.getName());
                            }
                            if (serviceURLs.contains(serviceDescription.getUrl())) {
                                log.warn(
                                    "URL \"{}\" has already registered. The second service will be skipped. Please, use unique URLs for services.",
                                    serviceDescription.getUrl());
                            }
                        }
                    }
                } catch (Throwable e) {
                    log.error(
                        "Project loading from repository was failed! Project with name \"{}\" in deployment \"{}\" was skipped!",
                        project.getName(),
                        deployment.getDeploymentName(),
                        e);
                }
            }
        }

        return serviceDescriptions;
    }

    private String buildServiceName(Deployment deployment, AProject project, RulesDeploy rulesDeploy) {
        if (rulesDeploy != null) {
            if (!StringUtils.isEmpty(rulesDeploy.getServiceName())) {
                if (!StringUtils.isEmpty(rulesDeploy.getVersion())) {
                    return rulesDeploy.getServiceName() + "(version=" + rulesDeploy.getVersion() + ")";
                } else {
                    return rulesDeploy.getServiceName();
                }
            } else {
                if (!StringUtils.isEmpty(rulesDeploy.getVersion())) {
                    return deployment.getDeploymentName() + '_' + project.getName() + "(version=" + rulesDeploy
                        .getVersion() + ")";
                }
            }
        }
        return deployment.getDeploymentName() + '_' + project.getName();
    }

    private String buildServiceUrl(Deployment deployment, AProject project, RulesDeploy rulesDeploy) {
        if (rulesDeploy != null) {
            if (!StringUtils.isEmpty(rulesDeploy.getUrl())) {
                if (!StringUtils.isEmpty(rulesDeploy.getVersion())) {
                    if (rulesDeploy.getUrl().startsWith("/")) {
                        return "/" + rulesDeploy.getVersion() + rulesDeploy.getUrl();
                    } else {
                        return "/" + rulesDeploy.getVersion() + "/" + rulesDeploy.getUrl();
                    }
                } else {
                    return rulesDeploy.getUrl();
                }
            } else {
                if (!StringUtils.isEmpty(rulesDeploy.getVersion())) {
                    return "/" + rulesDeploy.getVersion() + "/" + deployment.getDeploymentName() + '/' + project
                        .getName();
                }
            }
        }
        return deployment.getDeploymentName() + '/' + project.getName();
    }

    public final IRulesDeploySerializer getRulesDeploySerializer() {
        return rulesDeploySerializer;
    }

    public final void setRulesDeploySerializer(IRulesDeploySerializer rulesDeploySerializer) {
        if (rulesDeploySerializer == null) {
            throw new IllegalArgumentException("rulesDeploySerializer arg can't be null!");
        }
        this.rulesDeploySerializer = rulesDeploySerializer;
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

    public boolean isUseRuleServiceRuntimeContext() {
        return useRuleServiceRuntimeContext;
    }

    public void setUseRuleServiceRuntimeContext(boolean useRuleServiceRuntimeContext) {
        this.useRuleServiceRuntimeContext = useRuleServiceRuntimeContext;
    }
}
