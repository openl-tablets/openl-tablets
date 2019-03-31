package org.openl.rules.ruleservice.conf;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openl.rules.common.CommonVersion;
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
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Selects the latest deployments and deploys each of their projects as single service.
 *
 * @author PUdalau, Marat Kamalov
 */
public class LastVersionProjectsServiceConfigurer implements ServiceConfigurer {

    public static final String RULES_DEPLOY_XML = "rules-deploy.xml";

    private final Logger log = LoggerFactory.getLogger(LastVersionProjectsServiceConfigurer.class);

    private IRulesDeploySerializer rulesDeploySerializer = new XmlRulesDeploySerializer();
    private boolean provideRuntimeContext = false;
    private boolean supportVariations = false;
    private String supportedGroups = null;
    private boolean filterDeployments = false;
    private DeploymentNameMatcher deploymentMatcher = DeploymentNameMatcher.DEFAULT;

    private Collection<Deployment> filterDeployments(Collection<Deployment> deployments) {
        if (!filterDeployments) {
            return deployments;
        }

        Map<String, Map<String, Deployment>> latestDeployments = new HashMap<>();
        for (Deployment deployment : deployments) {
            if (deployment.getCommonVersion() == null) {
                throw new IllegalArgumentException(
                    "Can't detect deployment version. Probably 'version in deployment name' parameter in configuration is incorrect.");
            }
            String deploymentName = deployment.getDeploymentName();
            Map<String, Deployment> internalMap = latestDeployments.get(deploymentName);
            if (internalMap == null) {
                internalMap = new HashMap<>();
                latestDeployments.put(deploymentName, internalMap);
            }
            boolean hasRulesDeployXML = false;
            for (AProject project : deployment.getProjects()) {
                try {
                    InputStream content = null;
                    try {
                        AProjectArtefact artifact = project.getArtefact(RULES_DEPLOY_XML);
                        if (artifact instanceof AProjectResource) {
                            AProjectResource resource = (AProjectResource) artifact;
                            content = resource.getContent();
                            RulesDeploy rulesDeploy = getRulesDeploySerializer().deserialize(content);
                            hasRulesDeployXML = true;
                            String version = null;
                            if (StringUtils.isNotEmpty(rulesDeploy.getVersion())) {
                                version = rulesDeploy.getVersion();
                            }

                            if (internalMap.containsKey(version)) {
                                if (internalMap.get(version)
                                    .getCommonVersion()
                                    .compareTo(deployment.getCommonVersion()) < 0) {
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
                } catch (Exception e) {
                    log.error(
                        "Failed to load project from repository! Project '{}' in deployment '{}' has been skipped!",
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

        Collection<Deployment> ret = new ArrayList<>();
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

        Collection<ServiceDescription> serviceDescriptions = new HashSet<>();
        Set<String> serviceURLs = new HashSet<>();
        for (Deployment deployment : deployments) {
            if (!deploymentMatcher.hasMatches(deployment.getDeploymentName())) {
                continue;
            }
            String deploymentName = deployment.getDeploymentName();
            CommonVersion deploymentVersion = deployment.getCommonVersion();
            DeploymentDescription deploymentDescription = new DeploymentDescription(deploymentName, deploymentVersion);
            for (AProject project : deployment.getProjects()) {
                String projectName = project.getName();
                try {
                    Collection<Module> modulesOfProject = ruleServiceLoader
                        .resolveModulesForProject(deploymentName, deploymentVersion, projectName);
                    ServiceDescription.ServiceDescriptionBuilder serviceDescriptionBuilder = new ServiceDescription.ServiceDescriptionBuilder()
                        .setProvideRuntimeContext(provideRuntimeContext)
                        .setProvideVariations(supportVariations)
                        .setDeployment(deploymentDescription);

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
                                if (rulesDeploy
                                    .getServiceClass() != null && !rulesDeploy.getServiceClass().trim().isEmpty()) {
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
                                if (rulesDeploy.getPublishers() != null) {
                                    for (RulesDeploy.PublisherType key : rulesDeploy.getPublishers()) {
                                        serviceDescriptionBuilder.addPublisher(key.toString());
                                    }
                                }
                                if (rulesDeploy.getConfiguration() != null) {
                                    serviceDescriptionBuilder.setConfiguration(rulesDeploy.getConfiguration());
                                }
                                if (rulesDeploy.getInterceptingTemplateClassName() != null && !rulesDeploy
                                    .getInterceptingTemplateClassName()
                                    .trim()
                                    .isEmpty()) {
                                    serviceDescriptionBuilder.setAnnotationTemplateClassName(
                                        rulesDeploy.getInterceptingTemplateClassName().trim());
                                }
                                if (rulesDeploy.getAnnotationTemplateClassName() != null && !rulesDeploy
                                    .getAnnotationTemplateClassName()
                                    .trim()
                                    .isEmpty()) {
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
                        serviceDescriptionBuilder.setName(buildServiceName(deployment, projectName, rulesDeploy));
                        serviceDescriptionBuilder.setUrl(buildServiceUrl(deployment, projectName, rulesDeploy));
                        ServiceDescription serviceDescription = serviceDescriptionBuilder.build();

                        if (!serviceDescriptions.contains(serviceDescription) && !serviceURLs
                            .contains(serviceDescription.getUrl()) && serviceGroupSupported(rulesDeploy)) {
                            serviceURLs.add(serviceDescription.getUrl());
                            serviceDescriptions.add(serviceDescription);
                        } else {
                            if (serviceDescriptions.contains(serviceDescription)) {
                                log.warn(
                                    "Service '{}' already exists in the deployment list. The second service has been skipped. Please, use unique name for services.",
                                    serviceDescription.getName());
                            }
                            if (serviceURLs.contains(serviceDescription.getUrl())) {
                                log.warn(
                                    "URL '{}' has already been registered. The second service has been skipped. Please, use unique URLs for services.",
                                    serviceDescription.getUrl());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error(
                        "Failed to load project from repository! Project '{}' in deployment '{}' has been skipped!",
                        projectName,
                        deploymentName,
                        e);
                }
            }
        }

        return serviceDescriptions;
    }

    private Set<String> getSupportedGroupsSet() {
        if (getSupportedGroups() != null && !getSupportedGroups().trim().isEmpty()) {
            String[] groups = getSupportedGroups().split(",");
            Set<String> supportedGroupSet = new HashSet<>();
            for (String group : groups) {
                supportedGroupSet.add(group.trim());
            }
            return supportedGroupSet;
        }
        return Collections.emptySet();
    }

    private boolean serviceGroupSupported(RulesDeploy rulesDeploy) {
        Set<String> supportedGroupSet = getSupportedGroupsSet();
        if (!supportedGroupSet.isEmpty()) {
            if (rulesDeploy == null || rulesDeploy.getGroups() == null || rulesDeploy.getGroups().trim().isEmpty()) {
                return false;
            }
            String[] groups = rulesDeploy.getGroups().split(",");
            for (String group : groups) {
                if (supportedGroupSet.contains(group)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private String buildServiceName(Deployment deployment, String projectName, RulesDeploy rulesDeploy) {
        if (rulesDeploy != null) {
            if (StringUtils.isNotEmpty(rulesDeploy.getServiceName())) {
                if (StringUtils.isNotEmpty(rulesDeploy.getVersion())) {
                    return rulesDeploy.getServiceName() + "(version=" + rulesDeploy.getVersion() + ")";
                } else {
                    return rulesDeploy.getServiceName();
                }
            } else {
                if (StringUtils.isNotEmpty(rulesDeploy.getVersion())) {
                    return deployment.getDeploymentName() + '_' + projectName + "(version=" + rulesDeploy
                        .getVersion() + ")";
                }
            }
        }
        return deployment.getDeploymentName() + '_' + projectName;
    }

    private String buildServiceUrl(Deployment deployment, String projectName, RulesDeploy rulesDeploy) {
        if (rulesDeploy != null) {
            if (StringUtils.isNotEmpty(rulesDeploy.getUrl())) {
                if (StringUtils.isNotEmpty(rulesDeploy.getVersion())) {
                    if (rulesDeploy.getUrl().startsWith("/")) {
                        return "/" + rulesDeploy.getVersion() + rulesDeploy.getUrl();
                    } else {
                        return "/" + rulesDeploy.getVersion() + "/" + rulesDeploy.getUrl();
                    }
                } else {
                    return rulesDeploy.getUrl();
                }
            } else {
                if (StringUtils.isNotEmpty(rulesDeploy.getVersion())) {
                    return "/" + rulesDeploy.getVersion() + "/" + deployment.getDeploymentName() + '/' + projectName;
                }
            }
        }
        return deployment.getDeploymentName() + '/' + projectName;
    }

    public final IRulesDeploySerializer getRulesDeploySerializer() {
        return rulesDeploySerializer;
    }

    public final void setRulesDeploySerializer(IRulesDeploySerializer rulesDeploySerializer) {
        if (rulesDeploySerializer == null) {
            throw new IllegalArgumentException("rulesDeploySerializer arg must not be null.");
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

    public void setSupportedGroups(String supportedGroups) {
        this.supportedGroups = supportedGroups;
    }

    public String getSupportedGroups() {
        return supportedGroups;
    }

    public void setFilterDeployments(boolean filterDeployments) {
        this.filterDeployments = filterDeployments;
    }

    public void setDatasourceDeploymentPatterns(String deploymentPatterns) {
        this.deploymentMatcher = new DeploymentNameMatcher(deploymentPatterns);
    }

}
