package org.openl.rules.ruleservice.management;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.ModuleConfiguration;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.loader.IRulesLoader;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;

public abstract class RulesBasedServiceConfigurer implements IServiceConfigurer {
    private Log LOG = LogFactory.getLog(RulesBasedServiceConfigurer.class);
    public static String SERVICES_FIELD_NAME = "services";
    private Object rulesInstance;
    private IRuntimeEnv runtimeEnv;
    private IOpenClass rulesOpenClass;

    protected abstract RulesInstantiationStrategy getRulesSource();

    private void init(IRulesLoader loader) {
        runtimeEnv = new SimpleVM().getRuntimeEnv();
        try {
            rulesOpenClass = getRulesSource().compile(ReloadType.NO).getOpenClass();
            rulesInstance = rulesOpenClass.newInstance(runtimeEnv);
            RulesBasedServiceConfigurer.loader.set(loader);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate rules based service configurer.", e);
        }
    }

    private static ThreadLocal<IRulesLoader> loader = new ThreadLocal<IRulesLoader>();

    /**
     * Utility method that helps to get RulesLoader instance from rules.
     * 
     * @return Rules loader instance.
     */
    public static IRulesLoader getLoader() {
        IRulesLoader loader = RulesBasedServiceConfigurer.loader.get();
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
        List<Deployment> deploymentsList = getLoader().getDeployments();
        Deployment[] deployments = new Deployment[deploymentsList.size()];
        return deploymentsList.toArray(deployments);
    }

    public List<ServiceDescription> getServicesToBeDeployed(IRulesLoader loader) {
        List<ServiceDescription> serviceDescriptions = new ArrayList<ServiceDescription>();
        init(loader);
        try {
            IOpenField servicesField = rulesOpenClass.getField(SERVICES_FIELD_NAME);
            Object[] services = (Object[]) servicesField.get(rulesInstance, runtimeEnv);
            for (Object service : services) {
                try {
                    serviceDescriptions.add(createServiceDescription(service));
                } catch (Exception e) {
                    LOG.error("Failed to load service description.", e);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to get services from rules.", e);
        }
        return serviceDescriptions;
    }

    public static String SERVICE_NAME_FIELD = "name";
    public static String SERVICE_URL_FIELD = "url";
    public static String SERVICE_CLASS_NAME_FIELD = "serviceClassName";
    public static String RUNTIME_CONTEXT_FIELD = "provideRuntimeContext";
    public static String MODULES_GETTER_FIELD = "modulesGetter";

    public ServiceDescription createServiceDescription(Object service) {
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setName(getFieldValue(service, SERVICE_NAME_FIELD, String.class));
        serviceDescription.setUrl(getFieldValue(service, SERVICE_URL_FIELD, String.class));
        serviceDescription.setServiceClassName(getFieldValue(service, SERVICE_CLASS_NAME_FIELD, String.class));
        serviceDescription.setProvideRuntimeContext(getFieldValue(service, RUNTIME_CONTEXT_FIELD, boolean.class));
        String modulesGetterName = getFieldValue(service, MODULES_GETTER_FIELD, String.class);
        IOpenMethod modulesGetter = rulesOpenClass.getMethod(modulesGetterName,
            new IOpenClass[] { JavaOpenClass.getOpenClass(Deployment.class),
                    JavaOpenClass.getOpenClass(AProject.class),
                    JavaOpenClass.getOpenClass(Module.class) });
        checkModulesGetter(serviceDescription.getName(), modulesGetter);
        List<ModuleConfiguration> modulesForService = gatherModules(modulesGetter);
        serviceDescription.setModulesToLoad(modulesForService);
        return serviceDescription;
    }

    private List<ModuleConfiguration> gatherModules(IOpenMethod modulesGetter) {
        List<ModuleConfiguration> modulesForService = new ArrayList<ModuleConfiguration>();
        for (Deployment deployment : getDeployments()) {
            for (AProject project : deployment.getProjects()) {
                for (Module module : loader.get().resolveModulesForProject(deployment.getDeploymentName(),
                    deployment.getCommonVersion(),
                    project.getName())) {
                    Object isSuitable = modulesGetter.invoke(rulesInstance,
                            new Object[] { deployment, project, module },
                            runtimeEnv);
                    if (isSuitable != null && (Boolean) isSuitable) {
                        modulesForService.add(new ModuleConfiguration(deployment.getDeploymentName(),
                            deployment.getCommonVersion(),
                            project.getName(),
                            module.getName()));
                    }

                }
            }
        }
        return modulesForService;
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
            LOG.warn(String.format("Failed to get value of field \"%s\" with type \"%s\"",
                fieldName,
                fieldType.getName()),
                e);
            return null;
        }
    }
}
