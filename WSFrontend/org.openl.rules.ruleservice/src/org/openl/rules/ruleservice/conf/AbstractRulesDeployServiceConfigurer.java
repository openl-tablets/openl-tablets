package org.openl.rules.ruleservice.conf;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
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
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.rules.ruleservice.core.ModuleDescription;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;

public abstract class AbstractRulesDeployServiceConfigurer implements ServiceConfigurer {
    public static final String RULES_DEPLOY_XML = "rules-deploy.xml";

    private final Log log = LogFactory.getLog(AbstractRulesDeployServiceConfigurer.class);

    private IRulesDeploySerializer rulesDeploySerializer = new XmlRulesDeploySerializer();

    private boolean provideRuntimeContext = false;
    private boolean supportVariations = false;
    private boolean useRuleServiceRuntimeContext = false;

    public final IRulesDeploySerializer getRulesDeploySerializer() {
        return rulesDeploySerializer;
    }

    public final void setRulesDeploySerializer(IRulesDeploySerializer rulesDeploySerializer) {
        if (rulesDeploySerializer == null) {
            throw new IllegalArgumentException("rulesDeploySerializer arg can't be null!");
        }
        this.rulesDeploySerializer = rulesDeploySerializer;
    }

    protected abstract Collection<Deployment> getDeploymentsFromRuleServiceLoader(RuleServiceLoader loader);

    /** {@inheritDoc} */
    public final Collection<ServiceDescription> getServicesToBeDeployed(RuleServiceLoader ruleServiceLoader) {
        if (ruleServiceLoader == null) {
            throw new IllegalArgumentException("ruleServiceLoader argument can't be null!");
        }

        if (log.isDebugEnabled()) {
            log.debug("Calculate services to be deployed...");
        }

        Collection<Deployment> deployments = getDeploymentsFromRuleServiceLoader(ruleServiceLoader);

        return createServiceDescriptions(deployments, ruleServiceLoader);
    }

    private Collection<ServiceDescription> createServiceDescriptions(Collection<Deployment> deployments,
            RuleServiceLoader loader) {
        Collection<ServiceDescription> serviceDescriptions = new HashSet<ServiceDescription>();
        Set<String> serviceURLs = new HashSet<String>();
        for (Deployment deployment : deployments) {
            DeploymentDescription deploymentDescription = new DeploymentDescription(deployment.getDeploymentName(),
                deployment.getCommonVersion());
            for (AProject project : deployment.getProjects()) {
                try {
                    Collection<Module> modulesOfProject = loader.resolveModulesForProject(deployment.getDeploymentName(),
                        deployment.getCommonVersion(),
                        project.getName());
                    ServiceDescription.ServiceDescriptionBuilder serviceDescriptionBuilder = new ServiceDescription.ServiceDescriptionBuilder().setProvideRuntimeContext(provideRuntimeContext)
                        .setProvideVariations(supportVariations)
                        .setDeployment(deploymentDescription)
                        .setUseRuleServiceRuntimeContext(useRuleServiceRuntimeContext);

                    for (Module module : modulesOfProject) {
                        ModuleDescription moduleDescription = new ModuleDescription.ModuleDescriptionBuilder().setModuleName(module.getName())
                            .setProjectName(project.getName())
                            .build();
                        serviceDescriptionBuilder.addModule(moduleDescription);
                    }

                    if (!modulesOfProject.isEmpty()) {
                        InputStream content = null;
                        RulesDeploy rulesDeploy = null;
                        try {
                            AProjectArtefact artifact = project.getArtefact(RULES_DEPLOY_XML);
                            if (artifact instanceof AProjectResource) {
                                AProjectResource resource = (AProjectResource) artifact;
                                content = resource.getContent();
                                rulesDeploy = getRulesDeploySerializer().deserialize(content);
                                if (rulesDeploy.getServiceClass() != null && !rulesDeploy.getServiceClass().isEmpty()) {
                                    serviceDescriptionBuilder.setServiceClassName(rulesDeploy.getServiceClass());
                                }
                                if (rulesDeploy.isProvideRuntimeContext() != null) {
                                    serviceDescriptionBuilder.setProvideRuntimeContext(rulesDeploy.isProvideRuntimeContext());
                                }
                                if (rulesDeploy.isProvideVariations() != null) {
                                    serviceDescriptionBuilder.setProvideVariations(rulesDeploy.isProvideVariations());
                                }
                                if (rulesDeploy.isUseRuleServiceRuntimeContext() != null) {
                                    serviceDescriptionBuilder.setUseRuleServiceRuntimeContext(rulesDeploy.isUseRuleServiceRuntimeContext());
                                }
                                if (rulesDeploy.getConfiguration() != null) {
                                    serviceDescriptionBuilder.setConfiguration(rulesDeploy.getConfiguration());
                                }
                                if (rulesDeploy.getInterceptingTemplateClassName() != null) {
                                    serviceDescriptionBuilder.setInterceptingTemplateClassName(rulesDeploy.getInterceptingTemplateClassName());
                                }
                            }
                        } catch (ProjectException e) {
                        } finally {
                            if (content != null) {
                                try {
                                    content.close();
                                } catch (IOException e) {
                                    if (log.isErrorEnabled()) {
                                        log.error(e.getMessage(), e);
                                    }
                                }
                            }
                        }
                        serviceDescriptionBuilder.setName(buildServiceName(deployment, project, rulesDeploy));
                        serviceDescriptionBuilder.setUrl(buildServiceUrl(deployment, project, rulesDeploy));
                        ServiceDescription serviceDescription = serviceDescriptionBuilder.build();
                        if (!serviceDescriptions.contains(serviceDescription) && !serviceURLs.contains(serviceDescription.getUrl())) {
                            serviceURLs.add(serviceDescription.getUrl());
                            serviceDescriptions.add(serviceDescription);
                        } else {
                            if (serviceDescriptions.contains(serviceDescription)) {
                                if (log.isWarnEnabled()) {
                                    log.warn(String.format("Service \"%s\" has already exists in a deployment list. The second service will be skipped. Please, use unique name for services.",
                                        serviceDescription.getName()));
                                }
                            }
                            if (serviceURLs.contains(serviceDescription.getUrl())) {
                                if (log.isWarnEnabled()) {
                                    log.warn(String.format("URL \"%s\" has already registered. The second service will be skipped. Please, use unique URLs for services.",
                                        serviceDescription.getUrl()));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Project loading from repository was failed! Project with name \"" + project.getName() + "\" in deployemtn \"" + deployment.getDeploymentName() + "\" was skipped!", e);
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

    public boolean isUseRuleServiceRuntimeContext() {
        return useRuleServiceRuntimeContext;
    }

    public void setUseRuleServiceRuntimeContext(boolean useRuleServiceRuntimeContext) {
        this.useRuleServiceRuntimeContext = useRuleServiceRuntimeContext;
    }

    protected String buildServiceName(Deployment deployment, AProject project, RulesDeploy rulesDeploy) {
        if (rulesDeploy != null && !StringUtils.isEmpty(rulesDeploy.getServiceName())) {
            return rulesDeploy.getServiceName();
        } else {
            return String.format("%s_%s", deployment.getDeploymentName(), project.getName());
        }
    }

    protected String buildServiceUrl(Deployment deployment, AProject project, RulesDeploy rulesDeploy) {
        if (rulesDeploy != null && !StringUtils.isEmpty(rulesDeploy.getUrl())) {
            return rulesDeploy.getUrl();
        } else {
            return String.format("%s/%s", deployment.getDeploymentName(), project.getName());
        }
    }
}
