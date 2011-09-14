package org.openl.rules.ruleservice.management;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.ruleservice.core.ModuleConfiguration;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.loader.IRulesLoader;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;

public abstract class RulesBasedServiceConfigurer implements IServiceConfigurer {
    private Log LOG = LogFactory.getLog(RulesBasedServiceConfigurer.class);
    public static String SERVICES_FIELD_NAME = "services";
    private Object rulesInstance;
    private IRuntimeEnv runtimeEnv;
    private IOpenClass rulesOpenClass;

    protected abstract RulesInstantiationStrategy getRulesSource();

    private void init() {
        runtimeEnv = new SimpleVM().getRuntimeEnv();
        try {
            rulesOpenClass = getRulesSource().compile(ReloadType.NO).getOpenClass();
            rulesInstance = rulesOpenClass.newInstance(runtimeEnv);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate rules based service configurer.", e);
        }
    }

    private Object prepareRules(IRulesLoader loader) {
        try {
            init();
            IOpenField loaderField = rulesOpenClass.getField("loader");
            loaderField.set(rulesInstance, new IRulesLoader[] { loader }, runtimeEnv);
            IOpenField deploymentsField = rulesOpenClass.getField("deployments");
            List<Deployment> deploymentsList = loader.getDeployments();
            Deployment[] deployments = new Deployment[deploymentsList.size()];
            deploymentsField.set(rulesInstance, deploymentsList.toArray(deployments), runtimeEnv);
        } catch (Exception e) {
            LOG.error("Failed to instantiate rules based service configurer", e);
        }
        return rulesInstance;
    }

    public List<ServiceDescription> getServicesToBeDeployed(IRulesLoader loader) {
        List<ServiceDescription> serviceDescriptions = new ArrayList<ServiceDescription>();
        prepareRules(loader);
        try {
            IOpenField servicesField = rulesOpenClass.getField(SERVICES_FIELD_NAME);
            Object[] services = (Object[]) servicesField.get(rulesInstance, runtimeEnv);
            for (Object service : services) {
                serviceDescriptions.add(createServiceDescription(service));
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

    @SuppressWarnings("unchecked")
    public ServiceDescription createServiceDescription(Object service) {
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setName(getFieldValue(service, SERVICE_NAME_FIELD, String.class));
        serviceDescription.setUrl(getFieldValue(service, SERVICE_URL_FIELD, String.class));
        serviceDescription.setServiceClassName(getFieldValue(service, SERVICE_CLASS_NAME_FIELD, String.class));
        serviceDescription.setProvideRuntimeContext(getFieldValue(service, RUNTIME_CONTEXT_FIELD, boolean.class));
        String modulesGetterName = getFieldValue(service, MODULES_GETTER_FIELD, String.class);
        IOpenMethod modulesGetter = rulesOpenClass.getMethod(modulesGetterName, new IOpenClass[] {});
        serviceDescription.setModulesToLoad((List<ModuleConfiguration>) modulesGetter.invoke(rulesInstance,
            new Object[] {},
            runtimeEnv));
        return serviceDescription;
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
