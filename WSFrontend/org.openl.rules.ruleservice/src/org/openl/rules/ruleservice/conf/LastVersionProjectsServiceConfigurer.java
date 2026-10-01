package org.openl.rules.ruleservice.conf;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.IDeployment;
import org.openl.rules.project.abstraction.IProject;
import org.openl.rules.project.abstraction.IProjectResource;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
import org.openl.rules.ruleservice.publish.RuleServicePublisher;
import org.openl.util.StringUtils;

/**
 * Selects the latest deployments and deploys each of their projects as single service.
 *
 * @author PUdalau, Marat Kamalov
 */
@Slf4j
public class LastVersionProjectsServiceConfigurer implements ServiceConfigurer, InitializingBean {

    @Getter
    @Setter
    private boolean provideRuntimeContext;
    @Getter
    @Setter
    private String supportedGroups;
    private DeploymentNameMatcher deploymentMatcher = DeploymentNameMatcher.DEFAULT;
    private Collection<String> defaultPublishers = List.of();

    /**
     * {@inheritDoc}
     */
    @Override
    public final Collection<ServiceDescription> getServicesToBeDeployed(RuleServiceLoader ruleServiceLoader) {
        log.debug("Calculate services to be deployed...");

        Collection<IDeployment> deployments = ruleServiceLoader.getDeployments();

        var serviceDescriptions = new HashSet<ServiceDescription>();
        for (IDeployment deployment : deployments) {
            if (!deploymentMatcher.hasMatches(deployment.getDeploymentName())) {
                continue;
            }
            var deploymentName = deployment.getDeploymentName();
            var deploymentVersion = deployment.getCommonVersion();
            var deploymentDescription = new DeploymentDescription(deploymentName, deploymentVersion);
            for (IProject project : deployment.getProjects()) {
                if (project.isDeleted()) {
                    continue;
                }
                var projectName = project.getName();
                try {
                    var pd = ruleServiceLoader.resolveProject(deploymentName, deploymentVersion, projectName);
                    // A project without a descriptor is not an OpenL project
                    if (pd != null) {
                        Collection<Module> modulesOfProject = pd.getModules();
                        var serviceDescriptionBuilder = new ServiceDescription.ServiceDescriptionBuilder()
                                .setProvideRuntimeContext(isProvideRuntimeContext())
                                .setPublishers(defaultPublishers)
                                .setDeployment(deploymentDescription);

                        serviceDescriptionBuilder.setModules(modulesOfProject);
                        serviceDescriptionBuilder.setProjectDescriptor(pd);
                        var resourceLoader = new ResourceLoaderImpl(project);
                        serviceDescriptionBuilder.setResourceLoader(resourceLoader);
                        var rulesDeploy = applyRulesDeploy(project, serviceDescriptionBuilder);
                        serviceDescriptionBuilder.setManifest(readManifestFile(project));
                        serviceDescriptionBuilder.setName(buildServiceName(deployment, projectName, rulesDeploy));
                        serviceDescriptionBuilder.setUrl(buildServiceUrl(deployment, projectName, rulesDeploy));
                        serviceDescriptionBuilder
                                .setServicePath(ruleServiceLoader.getLogicalProjectFolder(project.getFolderPath()));
                        var serviceDescription = serviceDescriptionBuilder.build();

                        addServiceDescription(serviceDescriptions, serviceDescription, rulesDeploy);
                    }
                } catch (Exception e) {
                    log.error(
                            "Failed to load a project from the repository. Project '{}' in deployment '{}' has been skipped.",
                            projectName,
                            deploymentName,
                            e);
                }
            }
        }

        return serviceDescriptions;
    }

    /**
     * Adds the service unless a service with the same deploy path is added already or the service group is not
     * supported.
     */
    private void addServiceDescription(Set<ServiceDescription> serviceDescriptions,
                                       ServiceDescription serviceDescription,
                                       @Nullable RulesDeploy rulesDeploy) {
        if (!serviceDescriptions.contains(serviceDescription) && serviceGroupSupported(rulesDeploy)) {
            serviceDescriptions.add(serviceDescription);
        } else if (serviceDescriptions.contains(serviceDescription)) {
            log.error(
                    "Service '{}' already exists in the deployment list.",
                    serviceDescription.getDeployPath());
        }
    }

    /**
     * Applies the optional rules deploy file of the project to the service description.
     *
     * @return the rules deploy of the project, or {@code null} when the project has none
     */
    private static @Nullable RulesDeploy applyRulesDeploy(
            IProject project,
            ServiceDescription.ServiceDescriptionBuilder serviceDescriptionBuilder) throws IOException {
        RulesDeploy rulesDeploy = null;
        try {
            var artifact = project.getArtefact(RulesDeploy.FILE_NAME);
            if (artifact instanceof IProjectResource resource) {
                try (var content = resource.getContent()) {
                    rulesDeploy = RulesDeploy.read(content);
                    serviceDescriptionBuilder.setRulesDeploy(rulesDeploy);
                    applyRulesDeploySettings(rulesDeploy, serviceDescriptionBuilder);
                }
            }
        } catch (ProjectException ignored) {
            // rules-deploy.xml is optional; proceed with defaults
        }
        return rulesDeploy;
    }

    private static void applyRulesDeploySettings(
            RulesDeploy rulesDeploy,
            ServiceDescription.ServiceDescriptionBuilder serviceDescriptionBuilder) {
        if (rulesDeploy
                .getServiceClass() != null && !rulesDeploy.getServiceClass().trim().isEmpty()) {
            serviceDescriptionBuilder
                    .setServiceClassName(rulesDeploy.getServiceClass().trim());
        }
        if (rulesDeploy.isProvideRuntimeContext() != null) {
            serviceDescriptionBuilder
                    .setProvideRuntimeContext(rulesDeploy.isProvideRuntimeContext());
        }
        if (rulesDeploy.getPublishers() != null) {
            var publishers = Arrays.stream(rulesDeploy.getPublishers())
                    .map(Enum::toString)
                    .collect(Collectors.toSet());
            serviceDescriptionBuilder.setPublishers(publishers);
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

    private Manifest readManifestFile(IProject project) {
        try {
            var artifact = project.getArtefact(JarFile.MANIFEST_NAME);
            if (artifact instanceof IProjectResource resource) {
                try (var content = resource.getContent()) {
                    return new Manifest(content);
                }
            }
        } catch (IOException | ProjectException ignored) {
            // manifest is optional; return null so callers treat it as absent
        }
        return null;
    }

    private Set<String> getSupportedGroupsSet() {
        if (getSupportedGroups() != null && !getSupportedGroups().trim().isEmpty()) {
            return Arrays.stream(getSupportedGroups().split(",", -1))
                    .map(String::trim)
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }

    private boolean serviceGroupSupported(RulesDeploy rulesDeploy) {
        Set<String> supportedGroupSet = getSupportedGroupsSet();
        if (!supportedGroupSet.isEmpty()) {
            if (rulesDeploy == null || rulesDeploy.getGroups() == null || rulesDeploy.getGroups().trim().isEmpty()) {
                return false;
            }
            var groups = rulesDeploy.getGroups().split(",", -1);
            for (String group : groups) {
                if (supportedGroupSet.contains(group)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private String buildServiceName(IDeployment deployment, String projectName, RulesDeploy rulesDeploy) {
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

    private String buildServiceUrl(IDeployment deployment, String projectName, RulesDeploy rulesDeploy) {
        if (rulesDeploy != null) {
            if (StringUtils.isNotEmpty(rulesDeploy.getUrl())) {
                if (StringUtils.isNotEmpty(rulesDeploy.getVersion())) {
                    return buildVersionedUrl(rulesDeploy);
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

    private static String buildVersionedUrl(RulesDeploy rulesDeploy) {
        if (rulesDeploy.getUrl().startsWith("/")) {
            return "/" + rulesDeploy.getVersion() + rulesDeploy.getUrl();
        } else {
            return "/" + rulesDeploy.getVersion() + "/" + rulesDeploy.getUrl();
        }
    }

    public void setDatasourceDeploymentPatterns(String deploymentPatterns) {
        this.deploymentMatcher = new DeploymentNameMatcher(deploymentPatterns);
    }

    public void setDefaultPublishers(String[] defaultPublishers) {
        if (defaultPublishers != null) {
            this.defaultPublishers = new HashSet<>();
            Collections.addAll(this.defaultPublishers, defaultPublishers);
        } else {
            this.defaultPublishers = List.of();
        }
    }

    /**
     * For validation
     */
    @Setter(onMethod_ = @Autowired)
    private Collection<RuleServicePublisher> supportedPublishers;

    @Override
    public void afterPropertiesSet() throws Exception {
        for (String defPublisher : defaultPublishers) {
            var publisher = supportedPublishers.stream().filter(n -> n.name().equalsIgnoreCase(defPublisher)).findAny();
            if (publisher.isEmpty()) {
                throw new BeanInitializationException(
                        "Default publisher with id '%s' is not found in the map of supported publishers.".formatted(
                                defPublisher));
            }
        }
    }
}
