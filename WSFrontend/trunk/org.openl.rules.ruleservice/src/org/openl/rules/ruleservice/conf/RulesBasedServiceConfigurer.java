package org.openl.rules.ruleservice.conf;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.rules.ruleservice.core.ModuleDescription;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.core.ServiceDescription.ServiceDescriptionBuilder;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;

public abstract class RulesBasedServiceConfigurer implements ServiceConfigurer {

    private final Log log = LogFactory.getLog(RulesBasedServiceConfigurer.class);

    private static final String SERVICE_NAME_FIELD = "name";
    private static final String SERVICE_URL_FIELD = "url";
    private static final String SERVICE_CLASS_NAME_FIELD = "serviceClassName";
    private static final String RUNTIME_CONTEXT_FIELD = "provideRuntimeContext";
    private static final String MODULES_GETTER_FIELD = "modulesGetter";
    private static final String SERVICES_FIELD_NAME = "services";

    private Object rulesInstance;
    private IRuntimeEnv runtimeEnv;
    private IOpenClass rulesOpenClass;

    protected abstract RulesInstantiationStrategy getRulesSource();

    private void init(RuleServiceLoader loader) throws RuleServiceInstantiationException {
        runtimeEnv = new SimpleVM().getRuntimeEnv();
        try {
            rulesOpenClass = getRulesSource().compile().getOpenClass();
            rulesInstance = rulesOpenClass.newInstance(runtimeEnv);
            RulesBasedServiceConfigurer.loader.set(loader);
        } catch (RulesInstantiationException e) {
            throw new RuleServiceInstantiationException("Failed to instantiate rules based service configurer.", e);
        }
    }

    private static ThreadLocal<RuleServiceLoader> loader = new ThreadLocal<RuleServiceLoader>();

    /**
     * Utility method that helps to get RulesLoader instance from rules.
     * 
     * @return Rules loader instance.
     */
    public static RuleServiceLoader getLoader() {
        RuleServiceLoader loader = RulesBasedServiceConfigurer.loader.get();
        if (loader == null) {
            throw new OpenLRuntimeException("Rules loader have not been specified.");
        }
        return loader;
    }

    /**
     * Utility method that helps to get deployments from rules.
     * 
     * @return Deployments from data source.
     */
    public static Deployment[] getDeployments() {
        Collection<Deployment> deploymentsList = getLoader().getDeployments();
        Deployment[] deployments = new Deployment[deploymentsList.size()];
        return deploymentsList.toArray(deployments);
    }

    public Collection<ServiceDescription> getServicesToBeDeployed(RuleServiceLoader loader) {
        Collection<ServiceDescription> serviceDescriptions = new ArrayList<ServiceDescription>();
        try {
            init(loader);
        } catch (RuleServiceInstantiationException e) {
            if (log.isErrorEnabled()) {
                log.error("Failed to Instantiation rule service.", e);
            }
        }
        try {
            IOpenField servicesField = rulesOpenClass.getField(SERVICES_FIELD_NAME);
            Object[] services = (Object[]) servicesField.get(rulesInstance, runtimeEnv);
            for (Object service : services) {
                try {
                    serviceDescriptions.add(createServiceDescription(service));
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Failed to load service description.", e);
                    }
                }
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Failed to get services from rules.", e);
            }
        }
        return serviceDescriptions;
    }

    private ServiceDescription createServiceDescription(Object service) {
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

        ServiceDescriptionBuilder serviceDescriptionBuilder = new ServiceDescription.ServiceDescriptionBuilder().setName(serviceName)
            .setUrl(serviceUrl)
            .setServiceClassName(serviceClassName)
            .setProvideRuntimeContext(provideRuntimeContext);
        gatherModules(modulesGetter, serviceDescriptionBuilder);
        return serviceDescriptionBuilder.build();
    }

    private void gatherModules(IOpenMethod modulesGetter, ServiceDescriptionBuilder serviceDescriptionBuilder) {
        DeploymentDescription deploymentDescription = null;
        for (Deployment deployment : getDeployments()) {
            for (AProject project : deployment.getProjects()) {
                for (Module module : loader.get().resolveModulesForProject(deployment.getDeploymentName(),
                    deployment.getCommonVersion(),
                    project.getName())) {
                    Object isSuitable = modulesGetter.invoke(rulesInstance,
                        new Object[] { deployment, project, module },
                        runtimeEnv);
                    ModuleDescription moduleDescription = new ModuleDescription.ModuleDescriptionBuilder().setProjectName(project.getName()).setModuleName(module.getName())
                        .build();
                    if (isSuitable != null && (Boolean) isSuitable) {
                        deploymentDescription = new DeploymentDescription(deployment.getDeploymentName(), deployment.getCommonVersion());
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
            throw new RuntimeException(String.format("Modules getter for service \"%s\" was not found. Make sure that your getter name specified and there are exists rule with params [%s,%s,%s]",
                serviceName,
                Deployment.class.getSimpleName(),
                AProject.class.getSimpleName(),
                Module.class.getSimpleName()));
        } else if (modulesGetter.getType() != JavaOpenClass.BOOLEAN && modulesGetter.getType() != JavaOpenClass.getOpenClass(Boolean.class)) {
            throw new RuntimeException(String.format("Modules getter for service \"%s\" has a wrong return type. Return type should be \"boolean\"",
                serviceName,
                Deployment.class.getSimpleName(),
                AProject.class.getSimpleName(),
                Module.class.getSimpleName()));

        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getFieldValue(Object target, String fieldName, Class<T> fieldType) {
        try {
            return (T) PropertyUtils.getProperty(target, fieldName);
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Failed to get value of field \"%s\" with type \"%s\"",
                    fieldName,
                    fieldType.getName()),
                    e);
            }
            return null;
        }
    }
}
