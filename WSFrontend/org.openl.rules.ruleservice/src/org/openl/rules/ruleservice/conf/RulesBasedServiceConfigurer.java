package org.openl.rules.ruleservice.conf;

import org.apache.commons.beanutils.PropertyUtils;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.rules.ruleservice.core.ModuleDescription;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.core.ServiceDescription.ServiceDescriptionBuilder;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

public abstract class RulesBasedServiceConfigurer implements ServiceConfigurer {

    private final Logger log = LoggerFactory.getLogger(RulesBasedServiceConfigurer.class);

    private static final String SERVICE_NAME_FIELD = "name";
    private static final String SERVICE_URL_FIELD = "url";
    private static final String SERVICE_CLASS_NAME_FIELD = "serviceClassName";
    private static final String RUNTIME_CONTEXT_FIELD = "provideRuntimeContext";
    private static final String MODULES_GETTER_FIELD = "modulesGetter";
    private static final String SERVICES_FIELD_NAME = "services";

    protected abstract RulesInstantiationStrategy getRulesSource();

    public Collection<ServiceDescription> getServicesToBeDeployed(RuleServiceLoader loader) {
        Collection<ServiceDescription> serviceDescriptions = new ArrayList<ServiceDescription>();
        try {
            IOpenClass rulesOpenClass = getRulesSource().compile().getOpenClass();
            IRuntimeEnv runtimeEnv = new SimpleVM().getRuntimeEnv();
            Object rulesInstance = rulesOpenClass.newInstance(runtimeEnv);
            IOpenField servicesField = rulesOpenClass.getField(SERVICES_FIELD_NAME);
            Object[] services = (Object[]) servicesField.get(rulesInstance, runtimeEnv);
            for (Object service : services) {
                try {
                    ServiceDescription serviceDescription = createServiceDescription(service,
                            rulesOpenClass,
                            rulesInstance,
                            runtimeEnv,
                            loader);
                    serviceDescriptions.add(serviceDescription);
                } catch (Exception e) {
                    log.error("Failed to load service description.", e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to get services from rules.", e);
        }
        return serviceDescriptions;
    }

    private ServiceDescription createServiceDescription(Object service,
            IOpenClass rulesOpenClass,
            Object rulesInstance,
            IRuntimeEnv runtimeEnv, RuleServiceLoader loader) {
        final String serviceName = getFieldValue(service, SERVICE_NAME_FIELD, String.class);
        final String serviceUrl = getFieldValue(service, SERVICE_URL_FIELD, String.class);
        final String serviceClassName = getFieldValue(service, SERVICE_CLASS_NAME_FIELD, String.class);
        final boolean provideRuntimeContext = getFieldValue(service, RUNTIME_CONTEXT_FIELD, boolean.class);
        String modulesGetterName = getFieldValue(service, MODULES_GETTER_FIELD, String.class);

        IOpenMethod modulesGetter = rulesOpenClass.getMethod(modulesGetterName,
                new IOpenClass[] { JavaOpenClass.getOpenClass(Deployment.class),
                        JavaOpenClass.getOpenClass(AProject.class),
                        JavaOpenClass.getOpenClass(Module.class) });
        checkModulesGetter(serviceName, modulesGetter);

        ServiceDescriptionBuilder serviceDescriptionBuilder = new ServiceDescription.ServiceDescriptionBuilder().setName(
                serviceName)
                .setUrl(serviceUrl)
                .setServiceClassName(serviceClassName)
                .setProvideRuntimeContext(provideRuntimeContext);
        gatherModules(modulesGetter, serviceDescriptionBuilder, rulesInstance, runtimeEnv, loader);
        return serviceDescriptionBuilder.build();
    }

    private void gatherModules(IOpenMethod modulesGetter,
            ServiceDescriptionBuilder serviceDescriptionBuilder,
            Object rulesInstance,
            IRuntimeEnv runtimeEnv, RuleServiceLoader loader) {
        DeploymentDescription deploymentDescription = null;
        Collection<Deployment> deployments = loader.getDeployments();
        for (Deployment deployment : deployments) {
            for (AProject project : deployment.getProjects()) {
                for (Module module : loader.resolveModulesForProject(deployment.getDeploymentName(),
                        deployment.getCommonVersion(),
                        project.getName())) {
                    Object isSuitable = modulesGetter.invoke(rulesInstance,
                            new Object[] { deployment, project, module },
                            runtimeEnv);
                    ModuleDescription moduleDescription = new ModuleDescription.ModuleDescriptionBuilder().setProjectName(
                            project.getName()).setModuleName(module.getName())
                            .build();
                    if (isSuitable != null && (Boolean) isSuitable) {
                        deploymentDescription = new DeploymentDescription(deployment.getDeploymentName(),
                                deployment.getCommonVersion());
                        serviceDescriptionBuilder.addModule(moduleDescription);
                    }
                }
            }
        }
        if (deploymentDescription != null) {
            serviceDescriptionBuilder.setDeployment(deploymentDescription);
        }
        return;
    }

    private void checkModulesGetter(String serviceName, IOpenMethod modulesGetter) {
        if (modulesGetter == null) {
            throw new RuntimeException(String.format(
                    "Modules getter for service \"%s\" was not found. Make sure that your getter name specified and there are exists rule with params [%s,%s,%s]",
                    serviceName,
                    Deployment.class.getSimpleName(),
                    AProject.class.getSimpleName(),
                    Module.class.getSimpleName()));
        } else if (modulesGetter.getType() != JavaOpenClass.BOOLEAN && modulesGetter.getType() != JavaOpenClass.getOpenClass(
                Boolean.class)) {
            throw new RuntimeException(String.format(
                    "Modules getter for service \"%s\" has a wrong return type. Return type should be \"boolean\"",
                    serviceName,
                    Deployment.class.getSimpleName(),
                    AProject.class.getSimpleName(),
                    Module.class.getSimpleName()));

        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getFieldValue(Object target, String fieldName, Class<T> fieldType) {
        try {
            return (T) PropertyUtils.getProperty(target, fieldName);
        } catch (Exception e) {
            log.warn("Failed to get value of field \"{}\" with type \"{}\"", fieldName, fieldType.getName(), e);
            return null;
        }
    }
}
